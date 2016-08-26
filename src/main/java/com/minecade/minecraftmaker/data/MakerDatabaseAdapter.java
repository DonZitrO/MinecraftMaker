package com.minecade.minecraftmaker.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.minecade.mcore.util.BukkitUtils.verifyNotPrimaryThread;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import com.minecade.mcore.data.CoinTransaction;
import com.minecade.mcore.data.CoinTransaction.Reason;
import com.minecade.mcore.data.CoinTransaction.SourceType;
import com.minecade.mcore.schematic.exception.DataException;
import com.minecade.mcore.schematic.io.Clipboard;
import com.minecade.mcore.schematic.io.ClipboardFormat;
import com.minecade.mcore.schematic.io.ClipboardReader;
import com.minecade.mcore.schematic.io.ClipboardWriter;
import com.minecade.mcore.data.DatabaseException;
import com.minecade.mcore.data.MinecadeAccountData;
import com.minecade.mcore.data.Rank;
import com.minecade.minecraftmaker.inventory.PlayerLevelsMenu;
import com.minecade.minecraftmaker.level.AbstractMakerLevel;
import com.minecade.minecraftmaker.level.ClipboardWrapper;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.level.MakerLevelTemplate;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.world.WorldTimeAndWeather;

public class MakerDatabaseAdapter {

	private static final Random RANDOM = new Random(System.currentTimeMillis());
	//private static final int MAX_SEARCH_RESULTS = 28;

	private static final String LOAD_LEVEL_WITH_DATA_QUERY_BASE =
			"%s " + // select placeholder
			"FROM `mcmaker`.`levels` " +
			"%s "; // where placeholder

	private static final String SELECT_ALL_FROM_LEVELS = "SELECT `levels`.*";

	// based on:
	// https://medium.com/hacking-and-gonzo/how-reddit-ranking-algorithms-work-ef111e33d0d9#.4vtsrfifk
	private static long calculateTrendingScore(long likes, long dislikes, Date published) {
		if (published == null) {
			return 0;
		}
		double score = likes - dislikes;
		double order = Math.log10(Math.max(Math.abs(score), 1));
		double sign = (score > 0) ? 1 : (score < 0) ? -1 : 0;
		double seconds = TimeUnit.MILLISECONDS.toSeconds(published.getTime()) - 1134028003;
		return Math.round(((sign * order) + (seconds / 45000d)) * 10000000d);
	}

	private final MinecraftMakerPlugin plugin;

	private final String jdbcUrl;
	private final String dbUsername;
	private final String dbpassword;
	private final String networkSchema;
	private final String coinsColumn;

	private Connection connection;

	private int lastCount = 0;

	public MakerDatabaseAdapter(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;

		ConfigurationSection databaseConfig = plugin.getConfig().getConfigurationSection("database");
		this.networkSchema = databaseConfig.getString("network-schema", "test_network");
		this.coinsColumn = databaseConfig.getString("coins-column", "coins");
		this.jdbcUrl = databaseConfig.getString("url");
		this.dbUsername = databaseConfig.getString("username");
		this.dbpassword = databaseConfig.getString("password");
	}

	private synchronized void clearSteveChallenge(UUID playerId, String playerName) {
		verifyNotPrimaryThread();
		checkNotNull(playerId);
		try {
			String playerIdString = playerId.toString().replace("-", "");
			try (PreparedStatement clearSteveSt = getConnection().prepareStatement("UPDATE `mcmaker`.`players` SET `steve_clear` = 1 WHERE `player_id` = UNHEX(?)")) {
				clearSteveSt.setString(1, playerIdString);
				clearSteveSt.executeUpdate();
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.clearSteveChallenge - steve challenge cleared for player: [%s<%s>]", playerName, playerId));
			}
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().clearSteveChallengeCallback(playerId, playerName));
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.clearSteveChallenge - error - player: [%s<%s>] - %s ", playerName, playerId, e.getMessage()));
			e.printStackTrace();
		}
	}

	public void clearSteveChallengeAsync(UUID playerId, String playerName) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> clearSteveChallenge(playerId, playerName));
	}

	private synchronized MakerRelativeLocationData copyEndLocationByLevelId(UUID copyFromLocationId) throws DataException, SQLException {
		verifyNotPrimaryThread();
		MakerRelativeLocationData copy = loadRelativeLocationById(copyFromLocationId);
		// it's a copy, will have a new id assigned when saved
		copy.setLocationId(null);
		return copy;
	}

	private synchronized void copyLevelBySerial(MakerPlayableLevel level, long copyFromSerial) {
		verifyNotPrimaryThread();
		try {
			level.tryStatusTransition(LevelStatus.LEVEL_COPY_READY, LevelStatus.LEVEL_COPYING);
			UUID[] result = loadLevelIdAndEndLocationIdBySerial(copyFromSerial);
			UUID copyFromLevelId = result[0];
			if (copyFromLevelId == null) {
				level.disable("No data found to copy from");
				return;
			}
			UUID copyFromLocationId = result[1];
			if (copyFromLocationId != null) {
				level.setRelativeEndLocation(copyEndLocationByLevelId(copyFromLocationId));
			}
			level.tryStatusTransition(LevelStatus.LEVEL_COPYING, LevelStatus.CLIPBOARD_LOAD_READY);
			loadLevelClipboard(level, copyFromLevelId);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.copyLevelBySerial - error while copying level from serial: [%s] and slot [%s] - %s", copyFromSerial, level.getChunkZ(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void copyLevelBySerialAsync(MakerPlayableLevel level, long copyFromSerial) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> copyLevelBySerial(level, copyFromSerial));
	}

	private synchronized void deleteLevelBySerial(long levelSerial, MakerPlayer mPlayer) {
		verifyNotPrimaryThread();
		UUID authorId = null;
		String levelName = null;
		Integer levelCount = null;
		Long balance = null;
		LevelOperationResult result = LevelOperationResult.ERROR;
		try {
			String findQuery = "SELECT `levels`.`author_id`, `levels`.`level_name` FROM `mcmaker`.`levels` WHERE `levels`.`level_serial` = ? AND `levels`.`deleted` = 0";
			try (PreparedStatement findAuthorQuery = getConnection().prepareStatement(String.format(findQuery))) {
				findAuthorQuery.setLong(1, levelSerial);
				ResultSet resultSet = findAuthorQuery.executeQuery();
				if (!resultSet.next()) {
					result = LevelOperationResult.NOT_FOUND;
					return;
				}
				ByteBuffer authorIdBytes = ByteBuffer.wrap(resultSet.getBytes("author_id"));
				authorId = new UUID(authorIdBytes.getLong(), authorIdBytes.getLong());
				if (!authorId.equals(mPlayer.getUniqueId()) && !mPlayer.hasRank(Rank.ADMIN)){
					result = LevelOperationResult.PERMISSION_DENIED;
					return;
				}
				levelName = resultSet.getString("level_name");
			}
			if (!mPlayer.hasRank(Rank.ADMIN)) {
				String description = plugin.getMessage("coin.transaction.level-delete.description", mPlayer.getName(), levelName);
				CoinTransaction transaction = new CoinTransaction(UUID.randomUUID(), mPlayer.getUniqueId(), -500, plugin.getServerUniqueId(), SourceType.SERVER, Reason.LEVEL_DELETE, description);
				switch (executeCoinTransaction(transaction, false)) {
				case COMMITTED:
					balance = getCoinBalance(mPlayer.getUniqueId());
					break;
				case INSUFFICIENT_COINS:
					result = LevelOperationResult.INSUFFICIENT_COINS;
					return;
				default:
					return;
				};
			}
			String deleteQuery = "UPDATE `mcmaker`.`levels` SET `levels`.`deleted` = 1 WHERE `levels`.`level_serial` = ? AND `levels`.`author_id` = UNHEX(?) AND `levels`.`deleted` = 0";
			try (PreparedStatement deleteLevelSt = getConnection().prepareStatement(String.format(deleteQuery))) {
				deleteLevelSt.setLong(1, levelSerial);
				deleteLevelSt.setString(2, authorId.toString().replace("-", ""));
				deleteLevelSt.executeUpdate();
				result = LevelOperationResult.SUCCESS;
				levelCount = loadPublishedLevelsCount();
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.deleteLevelBySerial - error while deleting level: %s", e.getMessage()));
			e.printStackTrace();
		} finally {
			final LevelOperationResult finalResult = result;
			final Long finalBalance = balance;
			final Integer finalLevelCount = levelCount;
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().deleteLevelBySerialCallback(levelSerial, mPlayer.getUniqueId(), finalResult, finalBalance, finalLevelCount));
		}
	}

	public void deleteLevelBySerialAsync(long levelSerial, MakerPlayer mPlayer) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteLevelBySerial(levelSerial, mPlayer));
	}

	public synchronized void disconnect() {
		if (null != connection) {
			try {
				connection.close();
			} catch (SQLException e) {
				// no-op
			}
		}
	}

	private synchronized CoinTransactionResult executeCoinTransaction(final CoinTransaction transaction, final boolean callback) {
		verifyNotPrimaryThread();
		CoinTransactionResult result = CoinTransactionResult.ERROR;
		long balance = -1;
		try {
			result = tryToCommitCoinTransaction(transaction.getPlayerId(), transaction.getAmount());
			Bukkit.getLogger().info(String.format("MakerDatabaseAdapter.executeCoinsTransaction - transaction: [%s] - result: [%s]", transaction, result));
			if (callback) {
				balance = getCoinBalance(transaction.getPlayerId());
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.executeCoinsTransaction - error on transaction: [%s] - %s", transaction, e.getMessage()));
			e.printStackTrace();
		} finally {
			if (callback) {
				final CoinTransactionResult finalResult = result;
				final long finalBalance = balance;
				Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().coinTransactionCallback(transaction, finalResult, finalBalance));
			}
		}
		return result;
	}

	public void executeCoinTransactionAsync(final CoinTransaction transaction) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> executeCoinTransaction(transaction, true));
	}

	private synchronized long getCoinBalance(final UUID uniqueId) throws SQLException {
		// returns the new coin balance
		String uuid = uniqueId.toString().replace("-", "");
		try (PreparedStatement getBalanceSt = getConnection().prepareStatement(String.format("SELECT %s FROM %s.accounts WHERE unique_id = UNHEX(?)", coinsColumn, networkSchema))) {
			getBalanceSt.setString(1, uuid);
			ResultSet result = getBalanceSt.executeQuery();
			if (!result.next()) {
				return 0;
			}
			return result.getLong(coinsColumn);
		}
	}

	private synchronized Connection getConnection() throws SQLException {
		if (null != connection) {
			if (connection.isValid(1)) {
				return connection;
			} else {
				connection.close();
			}
		}
		connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbpassword);
		return connection;
	}

	private synchronized void insertClipboard(String levelId, Clipboard clipboard) throws SQLException, IOException {
		verifyNotPrimaryThread();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.insertClipboard - inserting clipboard data for level: <%s>", levelId));
		}
		checkNotNull(levelId);
		checkNotNull(clipboard);
		Blob data = getConnection().createBlob();
		try (PreparedStatement insertLevelBinaryData = getConnection().prepareStatement("INSERT INTO `mcmaker`.`schematics` (level_id, data) VALUES (UNHEX(?), ?)")) {
			try (BufferedOutputStream bos = new BufferedOutputStream(data.setBinaryStream(1)); ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(bos);) {
				writer.write(clipboard, null);
			}
			insertLevelBinaryData.setString(1, levelId);
			insertLevelBinaryData.setBlob(2, data);
			insertLevelBinaryData.executeUpdate();
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.insertClipboard - inserted clipboard data for level: <%s>", levelId));
			}
		} finally {
			if (data != null) {
				try {
					data.free();
				} catch (SQLException sqle) {
					// no-op
				}
			}
		}
	}

	private synchronized void insertLevel(AbstractMakerLevel level) throws SQLException {
		verifyNotPrimaryThread();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.insertLevel - inserting shallow data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
		}
		String levelId = level.getLevelId().toString().replace("-", "");
		String authorId = level.getAuthorId().toString().replace("-", "");
		String endLocationUUID = null;
		if (level.getRelativeEndLocation() != null) {
			endLocationUUID = insertOrUpdateRelativeLocation(level.getRelativeEndLocation()).toString().replace("-", "");
		}
		String insertBase = "INSERT INTO `mcmaker`.`levels` (level_id, author_id, level_name, level_time_and_weather, author_name, author_rank%s) VALUES (UNHEX(?), UNHEX(?), ?, ?, ?, ?%s)";
		String insertStatement = null;
		if (endLocationUUID != null) {
			insertStatement = String.format(insertBase, ", end_location_id", ", UNHEX(?)");
		} else {
			insertStatement = String.format(insertBase, "", "");
		}
		// insert level
		try (PreparedStatement insertLevelSt = getConnection().prepareStatement(insertStatement)) {
			insertLevelSt.setString(1, levelId);
			insertLevelSt.setString(2, authorId);
			insertLevelSt.setString(3, level.getLevelName());
			insertLevelSt.setString(4, level.getTimeAndWeather() != null ? level.getTimeAndWeather().name() : WorldTimeAndWeather.NOON_CLEAR.name());
			insertLevelSt.setString(5, level.getAuthorName());
			insertLevelSt.setString(6, level.getAuthorRank() != null ? level.getAuthorRank().name() : Rank.GUEST.name());
			if (endLocationUUID != null) {
				insertLevelSt.setString(7, endLocationUUID);
			}
			insertLevelSt.executeUpdate();
		}
		// get auto-incremented level serial
		try (PreparedStatement byBinaryUUID = getConnection().prepareStatement(String.format("SELECT level_serial FROM mcmaker.levels WHERE level_id = UNHEX(?)"))) {
			byBinaryUUID.setString(1, levelId);
			ResultSet byBinaryUUIDResult = byBinaryUUID.executeQuery();
			byBinaryUUIDResult.next();
			level.setLevelSerial(byBinaryUUIDResult.getLong("level_serial"));
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.insertLevel - inserted shallow data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
		}
	}

	private synchronized void insertLevelClear(UUID levelId, UUID playerId, long clearTimeMillis) {
		verifyNotPrimaryThread();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.insertLevelClear - inserting level clear time millis: [%s] for level: [%s] and player: [%s]", clearTimeMillis, levelId, playerId));
		}
		String levelIdString = levelId.toString().replace("-", "");
		String playerIdString = playerId.toString().replace("-", "");
		try (PreparedStatement insertLevelClearSt = getConnection().prepareStatement("INSERT INTO `mcmaker`.`level_clears` (`level_id`, `player_id`, `time_cleared`) VALUES (UNHEX(?), UNHEX(?), ?)")) {
			insertLevelClearSt.setString(1, levelIdString);
			insertLevelClearSt.setString(2, playerIdString);
			insertLevelClearSt.setLong(3, clearTimeMillis);
			insertLevelClearSt.executeUpdate();
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("[DEBUG] | MakerDatabaseAdapter.insertLevelClear - unable to insert level clear time millis: [%s] for level: [%s] and player: [%s] - %s", clearTimeMillis, levelId, playerId, e.getMessage()));
			e.printStackTrace();
		}
	}

	private UUID insertOrUpdateRelativeLocation(MakerRelativeLocationData relativeEndLocation) throws SQLException {
		if (relativeEndLocation.getLocationId() == null) {
			relativeEndLocation.setLocationId(UUID.randomUUID());
			String locationId = relativeEndLocation.getLocationId().toString().replace("-", "");
			try (PreparedStatement insterLocationData = getConnection().prepareStatement(
			        "INSERT INTO `mcmaker`.`locations` (location_id, location_x, location_y, location_z, location_yaw, location_pitch) VALUES (UNHEX(?), ?, ?, ?, ?, ?)")) {
				insterLocationData.setString(1, locationId);
				insterLocationData.setDouble(2, relativeEndLocation.getX());
				insterLocationData.setDouble(3, relativeEndLocation.getY());
				insterLocationData.setDouble(4, relativeEndLocation.getZ());
				insterLocationData.setFloat(5, relativeEndLocation.getYaw());
				insterLocationData.setFloat(6, relativeEndLocation.getPitch());
				insterLocationData.executeUpdate();
			}
		} else {
			try (PreparedStatement insterLocationData = getConnection().prepareStatement(
			        "UPDATE `mcmaker`.`locations` set `location_x` = ?, `location_y`= ?, `location_z` = ?, `location_yaw` = ?, `location_pitch` = ? WHERE location_id = UNHEX(?)")) {
				insterLocationData.setDouble(1, relativeEndLocation.getX());
				insterLocationData.setDouble(2, relativeEndLocation.getY());
				insterLocationData.setDouble(3, relativeEndLocation.getZ());
				insterLocationData.setFloat(4, relativeEndLocation.getYaw());
				insterLocationData.setFloat(5, relativeEndLocation.getPitch());
				String locationId = relativeEndLocation.getLocationId().toString().replace("-", "");
				insterLocationData.setString(6, locationId);
				insterLocationData.executeUpdate();
			}
		}
		return relativeEndLocation.getLocationId();
	}

	private synchronized void insertReport(UUID playerId, String playerName, String report) {
		verifyNotPrimaryThread();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().severe(String.format("[DEBUG] | MakerDatabaseAdapter.insertReport - inserting report from player: [%s<%s>] - %s", playerId, playerName, report));
		}
		String playerIdString = playerId.toString().replace("-", "");
		try (PreparedStatement insertReportSt = getConnection().prepareStatement("INSERT INTO `mcmaker`.`reports` (player_id, player_name, report) VALUES (UNHEX(?), ?, ?)")) {
			insertReportSt.setString(1, playerIdString);
			insertReportSt.setString(2, playerName);
			insertReportSt.setString(3, report);
			insertReportSt.executeUpdate();
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("[DEBUG] | MakerDatabaseAdapter.insertReport - unable to insert report: [%s] - from player: [%s<%s>] - %s", report, playerId, playerName, e.getMessage()));
			e.printStackTrace();
		}
	}

	private synchronized void likeLevel(UUID levelId, UUID playerId, boolean dislike) {
		verifyNotPrimaryThread();
		try {
			String levelIdString = levelId.toString().replace("-", "");
			String playerIdString = playerId.toString().replace("-", "");
			int affected = 0;
			try (PreparedStatement updateLikeSt = getConnection().prepareStatement("UPDATE `mcmaker`.`level_likes` SET `dislike` = ? WHERE `level_id` = UNHEX(?) AND `player_id` = UNHEX(?)")) {
				updateLikeSt.setBoolean(1, dislike);
				updateLikeSt.setString(2, levelIdString);
				updateLikeSt.setString(3, playerIdString);
				affected = updateLikeSt.executeUpdate();
			}
			if (affected == 0) {
				try (PreparedStatement insertLikeSt = getConnection().prepareStatement("INSERT INTO `mcmaker`.`level_likes` (`level_id`, `player_id`, `dislike`) VALUES (UNHEX(?), UNHEX(?), ?)")) {
					insertLikeSt.setString(1, levelIdString);
					insertLikeSt.setString(2, playerIdString);
					insertLikeSt.setBoolean(3, dislike);
					insertLikeSt.executeUpdate();
				}
			}
			long[] result = loadLevelLikesAndUnlikesAndUpdateTrendingScore(levelId);
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().levelLikeCallback(levelId, playerId, dislike, result[0], result[1], result[2]));
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.likeLevel - level like/dislike operation completed without errors for level: [%s] and player: [%s] - dislike: [%s]", levelId, playerId, dislike));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.likeLevel - error while updating level: [%s] - ", levelId, e.getMessage()));
			e.printStackTrace();
		}
	}

	public void likeLevelAsync(UUID levelId, UUID playerId, boolean dislike) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> likeLevel(levelId, playerId, dislike));
	}

	public MakerPlayerData loadAccountData(UUID uniqueId, String username) throws DatabaseException {
		verifyNotPrimaryThread();
		try {
			return loadAccountDataInternal(new MakerPlayerData(uniqueId, username));
		} catch (Exception e) {
			Bukkit.getLogger().severe(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private synchronized void loadAccountDataFromResult(MinecadeAccountData account, ResultSet result, boolean createColumnIfNotFound) throws SQLException {
		// load coins if not already loaded
		if (0 == account.getCoins()) {
			try {
				account.setCoins(result.getLong(coinsColumn));
			} catch (SQLException e) {
				Bukkit.getLogger().severe(String.format("Coins column not found: %s - %s", coinsColumn, e.getMessage()));
				throw new DatabaseException(e);
			}
		}
		// load ranks
		for (Rank rank : Rank.values()) {
			try {
				if (null == rank.getColumnName()) {
					continue;
				}
				if (result.getBoolean(rank.getColumnName())) {
					account.setHighestRank(rank);
				}
			} catch (SQLException e) {
				Bukkit.getLogger().warning(String.format("Rank column not found: %s - %s", rank.getColumnName(), e.getMessage()));
			}
		}
	}

	/**
	 * This loads general Minecade Account data or creates it
	 */
	protected synchronized <T extends MinecadeAccountData> T loadAccountDataInternal(T data) throws SQLException {
		long start = 0;
		if (plugin.isDebugMode()) {
			start = System.nanoTime();
		}
		String uuid = data.getUniqueId().toString().replace("-", "");
		// try to find player by binary UUID first
		try (PreparedStatement byBinaryUUID = getConnection().prepareStatement(String.format("SELECT * FROM %s.accounts WHERE unique_id = UNHEX(?)", networkSchema))) {
			byBinaryUUID.setString(1, uuid);
			ResultSet byBinaryUUIDResult = byBinaryUUID.executeQuery();
			if (!byBinaryUUIDResult.first()) {
				// not found by binary UUID - let's try to find player by the UUID as string (say thanks to n00b devs for this)
				try (PreparedStatement byStringUUID = getConnection().prepareStatement(String.format("SELECT * FROM %s.accounts WHERE uuid = ?", networkSchema))) {
					byStringUUID.setString(1, uuid);
					ResultSet byStringUUIDResult = byStringUUID.executeQuery();
					if (!byStringUUIDResult.first()) {
						// not found - create player on the database from scratch
						if (Bukkit.getLogger().isLoggable(Level.INFO)) {
							Bukkit.getLogger().info(String.format("No Minecade data - inserting Minecade data for first time Player: [%s<%s>]", data.getUsername(), data.getUniqueId()));
						}
						try (PreparedStatement insertAccount = getConnection().prepareStatement(String.format("INSERT INTO %s.accounts(unique_id, uuid, username) VALUES (UNHEX(?), ?, ?)", networkSchema))) {
							insertAccount.setString(1, uuid);
							insertAccount.setString(2, uuid);
							insertAccount.setString(3, data.getUsername());
							insertAccount.executeUpdate();
						}
						if (Bukkit.getLogger().isLoggable(Level.INFO)) {
							Bukkit.getLogger().info(String.format("Inserted Minecade data for first time Player: [%s<%s>]", data.getUsername(), data.getUniqueId()));
						}
					} else {
						// found by string UUID - update binary UUID column
						if (Bukkit.getLogger().isLoggable(Level.INFO)) {
							Bukkit.getLogger().info(String.format("Minecade data found by STRING UUID - Updating binary UUID for Player: [%s<%s>]", data.getUsername(), data.getUniqueId()));
						}
						try (PreparedStatement updateUUID = getConnection().prepareStatement(String.format("UPDATE %s.accounts SET unique_id = UNHEX(?) WHERE uuid = ?", networkSchema))) {
							updateUUID.setString(1, uuid);
							updateUUID.setString(2, uuid);
							updateUUID.executeUpdate();
						}
						if (Bukkit.getLogger().isLoggable(Level.INFO)) {
							Bukkit.getLogger().info(String.format("Updated BINARY UUID for Player: [%s<%s>]", data.getUsername(), data.getUniqueId()));
						}
						// load existing data
						loadAccountDataFromResult(data, byStringUUIDResult, true);
					}
				}
			} else {
				if (Bukkit.getLogger().isLoggable(Level.INFO)) {
					Bukkit.getLogger().info(String.format("Minecade data found by BINARY UUID - Player: [%s<%s>]", data.getUsername(), data.getUniqueId()));
				}
				loadAccountDataFromResult(data, byBinaryUUIDResult, true);
			}
			loadAdditionalData((MakerPlayerData)data);
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | SCBDatabase.loadAccountDataInternal - loading player: [%s<%s>] data from DB took: [%s] nanoseconds", data.getUsername(), data.getUniqueId(), System.nanoTime() - start));
		}
		return data;
	}

	/**
	 * This loads specific MCMaker Account data or creates it
	 */
	protected synchronized void loadAdditionalData(MakerPlayerData data) throws SQLException {
		loadOrCreateMakerPlayerData(data);
		loadPlayerLevelsCounts(data);
		loadUniqueLevelClearsCount(data);
		// FIXME: review this
		//loadPlayerLevelsLikes(data);
	}

	private void loadClipboardIntoWrapper(ClipboardWrapper wrapper, UUID clipboardId) throws SQLException, DataException {
		checkNotNull(wrapper);
		checkNotNull(clipboardId);
		String clipboardIdString = clipboardId.toString().replace("-", "");
		try (PreparedStatement testQuery = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`schematics` where `level_id` = UNHEX(?) order by `updated` desc limit 1"))) {
			testQuery.setString(1, clipboardIdString);
			ResultSet resultSet = testQuery.executeQuery();
			if (!resultSet.next()) {
				throw new DataException(String.format("Unable to find schematic with id: [%s]", clipboardId));
			}
			loadLevelClipboardFromResult(wrapper, resultSet);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadClipboardIntoWrapper - level clipboard loaded without errors: [%s]", clipboardId));
			}
		}
	}

	private synchronized void loadDisplayableLevelsPageByAuthorId(PlayerLevelsMenu levelBrowserMenu, int pageOffset, int levelsPerPage) {
		verifyNotPrimaryThread();
		checkNotNull(levelBrowserMenu);
		String authorIdString = levelBrowserMenu.getViewerId().toString().replace("-", "");
		int levelCount = 0;
		final List<MakerDisplayableLevel> levels = new LinkedList<>();
		try {
			try (PreparedStatement levelsCountByAuthorQuery = getConnection().prepareStatement(String.format("SELECT COUNT(`levels`.`level_serial`) FROM `mcmaker`.`levels` WHERE `author_id` = UNHEX(?) AND `levels`.`deleted` = 0"))) {
				levelsCountByAuthorQuery.setString(1, authorIdString);
				ResultSet resultSet = levelsCountByAuthorQuery.executeQuery();
				if (resultSet.next()) {
					levelCount = resultSet.getInt(1);
				}
			}
			try (PreparedStatement levelsByAuthorQuery = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`levels` WHERE `author_id` = UNHEX(?) AND `levels`.`deleted` = 0 ORDER BY level_serial DESC LIMIT ?, ?"))) {
				levelsByAuthorQuery.setString(1, authorIdString);
				levelsByAuthorQuery.setInt(2, pageOffset);
				levelsByAuthorQuery.setInt(3, levelsPerPage);
				ResultSet resultSet = levelsByAuthorQuery.executeQuery();
				while (resultSet.next()) {
					MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
					loadLevelFromResult(level, resultSet);
					loadlLevelBestClearData(level);
					levels.add(level);
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		} finally {
			final int finalLevelCount = levelCount;
			Bukkit.getScheduler().runTask(plugin, () -> levelBrowserMenu.loadLevelsCallback(finalLevelCount, levels));
		}
	}

	public void loadDisplayableLevelsPageByAuthorIdAsync(PlayerLevelsMenu playerLevelsMenu, int pageOffset, int levelsPerPage) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadDisplayableLevelsPageByAuthorId(playerLevelsMenu, pageOffset, levelsPerPage));
	}

	private synchronized void loadLevelClipboard(MakerPlayableLevel level) {
		verifyNotPrimaryThread();
		checkNotNull(level);
		loadLevelClipboard(level, level.getLevelId());
	}

	private synchronized void loadLevelClipboard(MakerPlayableLevel level, UUID clipboardId) {
		verifyNotPrimaryThread();
		checkNotNull(level);
		checkNotNull(clipboardId);
		try {
			level.tryStatusTransition(LevelStatus.CLIPBOARD_LOAD_READY, LevelStatus.CLIPBOARD_LOADING);
			loadClipboardIntoWrapper(level, clipboardId);
			level.tryStatusTransition(LevelStatus.CLIPBOARD_LOADING, LevelStatus.CLIPBOARD_LOADED);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadLevelClipboard - level clipboard loaded without errors: [%s<%s>]", level.getLevelName(), level.getLevelId()));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("loadLevelBySerial.loadLevelClipboard - error while loading level clipboard for level: [%s<%s>] - %s", level.getLevelName(), level.getLevelId(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	private void loadLevelClipboardFromResult(ClipboardWrapper wrapper, ResultSet resultSet) throws DataException {
		Blob data = null;
		try {
			data = resultSet.getBlob("data");
			try (BufferedInputStream is = new BufferedInputStream(data.getBinaryStream()); ClipboardReader reader = ClipboardFormat.SCHEMATIC.getReader(is)) {
				// note: this particular implementation doesn't need world data for anything. TODO: find other places where WorldData is not needed.
				wrapper.setClipboard(reader.read(null));
			}
		} catch (Exception e) {
			throw new DataException(e);
		} finally {
			if (data != null) {
				try {
					data.free();
				} catch (SQLException sqle) {
					// no-op
				}
			}
		}
	}

	private synchronized void loadLevelFromResult(AbstractMakerLevel level, ResultSet result) throws SQLException, DataException {
		ByteBuffer levelIdBytes = ByteBuffer.wrap(result.getBytes("level_id"));
		ByteBuffer authorIdBytes = ByteBuffer.wrap(result.getBytes("author_id"));
		level.setLevelId(new UUID(levelIdBytes.getLong(), levelIdBytes.getLong()));
		level.setLevelSerial(result.getLong("level_serial"));
		level.setLevelName(result.getString("level_name"));
		level.requestTimeAndWeatherChange(loadTimeAndWeather(result, "level_time_and_weather"));
		level.setAuthorId(new UUID(authorIdBytes.getLong(), authorIdBytes.getLong()));
		level.setAuthorName(result.getString("author_name"));
		level.setAuthorRank(loadRank(result));
		level.setTrendingScore(result.getLong("trending_score"));
		level.setClearedByAuthorMillis(result.getLong("author_cleared"));
		level.setUnpublished(result.getBoolean("unpublished"));
		byte[] locationId = result.getBytes("end_location_id");
		if (locationId != null) {
			ByteBuffer buffer = ByteBuffer.wrap(locationId);
			level.setRelativeEndLocation(loadRelativeLocationById(new UUID(buffer.getLong(), buffer.getLong())));
		}
		level.setDatePublished(result.getTimestamp("date_published"));
		if (level.getDatePublished() != null) {
			level.setLikes(result.getLong("level_likes"));
			level.setDislikes(result.getLong("level_dislikes"));
		}
	}

	private synchronized UUID[] loadLevelIdAndEndLocationIdBySerial(long levelSerial) {
		verifyNotPrimaryThread();
		UUID[] result = new UUID[2];
		String query = "SELECT `levels`.`level_id`, `levels`.`end_location_id`  FROM `mcmaker`.`levels` WHERE `levels`.`level_serial` = ?";
		try (PreparedStatement loadLevelQuery = getConnection().prepareStatement(String.format(query))) {
			loadLevelQuery.setLong(1, levelSerial);
			ResultSet resultSet = loadLevelQuery.executeQuery();
			if (resultSet.next()) {
				ByteBuffer levelIdBytes = ByteBuffer.wrap(resultSet.getBytes("level_id"));
				result[0] = new UUID(levelIdBytes.getLong(), levelIdBytes.getLong());
				byte[] endLocationIdBytes = resultSet.getBytes("end_location_id");
				if (endLocationIdBytes != null) {
					ByteBuffer endLocationIdBytesBuffer = ByteBuffer.wrap(endLocationIdBytes);
					result[1] = new UUID(endLocationIdBytesBuffer.getLong(), endLocationIdBytesBuffer.getLong());
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevelIdAndEndLocationIdBySerial - error while loading: %s", e.getMessage()));
			e.printStackTrace();
		}
		return result;
	}

	private synchronized long[] loadLevelLikesAndUnlikesAndUpdateTrendingScore(UUID levelId) throws SQLException {
		verifyNotPrimaryThread();
		String levelIdString = levelId.toString().replace("-", "");
		String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
				"SELECT `levels`.`level_likes`,`levels`.`level_dislikes`, `levels`.`date_published`, `levels`.`trending_score`, `levels`.`level_name` ",
				"WHERE `levels`.`date_published` IS NOT NULL AND `levels`.`level_id` = UNHEX(?) ");
		try (PreparedStatement selectLikesDislikesSt = getConnection().prepareStatement(query)) {
			selectLikesDislikesSt.setString(1, levelIdString);
			ResultSet result = selectLikesDislikesSt.executeQuery();
			if (result.next()) {
				long likes = result.getLong("level_likes");
				long dislikes = result.getLong("level_dislikes");
				long formerTrendingScore = result.getLong("trending_score");
				long expectedTrendingScore = calculateTrendingScore(likes, dislikes, result.getTimestamp("date_published"));
				if (expectedTrendingScore != formerTrendingScore) {
					Bukkit.getLogger().warning(String.format("MakerDatabaseAdapter.loadLevelLikesAndUnlikesAndUpdateTrendingScore - updating trending score from: [%s] to [%s] on level: [%s]", formerTrendingScore, expectedTrendingScore, result.getString("level_name")));
					updateLevelTrendingScore(levelId, expectedTrendingScore);
				}
				return new long[] { likes, dislikes, expectedTrendingScore };
			}
			return new long[] {0,0,0};
		}
	}

	private synchronized void loadLevelPlayerBestClearData(MakerPlayableLevel level) throws SQLException {
		verifyNotPrimaryThread();
		if (level.getLevelId() == null || level.getCurrentPlayerId() == null) {
			return;
		}
		String levelIdString = level.getLevelId().toString().replace("-", "");
		String playerIdString = level.getCurrentPlayerId().toString().replace("-", "");
		String query = 	"SELECT lc.player_id, lc.time_cleared, a.username " +
						"FROM mcmaker.level_clears lc " +
						"INNER JOIN %s.accounts a " +
						"ON a.unique_id = lc.player_id " +
						"WHERE lc.level_id = UNHEX(?) AND lc.player_id = UNHEX(?) " +
						"ORDER BY lc.time_cleared ASC LIMIT 1";
		try (PreparedStatement selectBestLevelClear = getConnection().prepareStatement(String.format(query, networkSchema))) {
			selectBestLevelClear.setString(1, levelIdString);
			selectBestLevelClear.setString(2, playerIdString);
			ResultSet resultSet = selectBestLevelClear.executeQuery();
			if (resultSet.next()) {
				MakerLevelClearData data = new MakerLevelClearData(level.getLevelId(), level.getCurrentPlayerId());
				data.setPlayerName(resultSet.getString("username"));
				data.setBestTimeCleared(resultSet.getLong("time_cleared"));
				level.setCurrentPlayerBestClearData(data);
			}
		}
	}

	private MakerLevelTemplate loadLevelTemplateFromResult(ResultSet result) throws SQLException, DataException {
		MakerLevelTemplate template = new MakerLevelTemplate(plugin);
		ByteBuffer templateIdBytes = ByteBuffer.wrap(result.getBytes("template_id"));
		ByteBuffer authorIdBytes = ByteBuffer.wrap(result.getBytes("author_id"));
		template.setTemplateId(new UUID(templateIdBytes.getLong(), templateIdBytes.getLong()));
		template.setAuthorId(new UUID(authorIdBytes.getLong(), authorIdBytes.getLong()));
		template.setTemplateName(result.getString("template_name"));
		template.setAuthorName(result.getString("author_name"));
		//template.setCoinCost(result.getInt("coin_cost"));
		template.setFree(result.getBoolean("free"));
		template.setVipOnly(result.getBoolean("vip_only"));
		template.setTimeAndWeather(loadTimeAndWeather(result, "template_time_and_weather"));
		byte[] locationId = result.getBytes("end_location_id");
		if (locationId != null) {
			ByteBuffer buffer = ByteBuffer.wrap(locationId);
			template.setRelativeEndLocation(loadRelativeLocationById(new UUID(buffer.getLong(), buffer.getLong())));
		}
		loadClipboardIntoWrapper(template, template.getTemplateId());
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadLevelTemplateFromResult - template loaded without errors: [%s]", template.getTemplateName()));
		}
		return template;
	}

	private synchronized void loadLevelTemplates() {
		verifyNotPrimaryThread();
		final List<MakerLevelTemplate> templates = new ArrayList<>();
		String query = "SELECT * from `mcmaker`.`level_templates` ORDER BY vip_only ASC, free DESC, template_name ASC";
		try (PreparedStatement templateQuery = getConnection().prepareStatement(query)) {
			ResultSet resultSet = templateQuery.executeQuery();
			while (resultSet.next()) {
				templates.add(loadLevelTemplateFromResult(resultSet));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevelTemplates - error: %s", e.getMessage()));
			e.printStackTrace();
		} finally {
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().loadLevelTemplatesCallback(templates));
		}
	}

//	public synchronized Set<MakerDisplayableLevel> loadPublishedLevelsBySerials(Set<Long> serials) {
//		if (Bukkit.isPrimaryThread()) {
//			throw new RuntimeException("This method should not be called from the main thread");
//		}
//		checkNotNull(serials);
//		Set<MakerDisplayableLevel> levels = new LinkedHashSet<MakerDisplayableLevel>();
//		if (serials.isEmpty()) {
//			return levels;
//		}
//		String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
//				SELECT_ALL_FROM_LEVELS,
//				"WHERE `levels`.`level_serial` IN (%s) AND `levels`.`date_published` IS NOT NULL AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0", "");
//		try (PreparedStatement loadLevelById = getConnection().prepareStatement(String.format(query, StringUtils.join(serials, ",")))) {
//			ResultSet resultSet = loadLevelById.executeQuery();
//			while (resultSet.next()) {
//				MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
//				loadLevelFromResult(level, resultSet);
//				loadLevelRecords(level);
//				levels.add(level);
//			}
//		} catch (Exception e) {
//			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadPublishedLevelsBySerials - error while loading level - %s", e.getMessage()));
//			e.printStackTrace();
//		}
//		return levels;
//	}


	public void loadLevelTemplatesAsync() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadLevelTemplates());
	}

	private synchronized void loadlLevelBestClearData(AbstractMakerLevel level) throws SQLException {
		verifyNotPrimaryThread();
		String levelIdString = level.getLevelId().toString().replace("-", "");
		String query = 	"SELECT lc.player_id, lc.time_cleared, a.username " +
						"FROM mcmaker.level_clears lc " +
						"INNER JOIN %s.accounts a " +
						"ON a.unique_id = lc.player_id " +
						"WHERE lc.level_id = UNHEX(?) " +
						"ORDER BY lc.time_cleared ASC LIMIT 1";
		try (PreparedStatement selectBestLevelClear = getConnection().prepareStatement(String.format(query, networkSchema))) {
			selectBestLevelClear.setString(1, levelIdString);
			ResultSet resultSet = selectBestLevelClear.executeQuery();
			if (resultSet.next()) {
				ByteBuffer playerIdBytes = ByteBuffer.wrap(resultSet.getBytes("player_id"));
				MakerLevelClearData data = new MakerLevelClearData(level.getLevelId(), new UUID(playerIdBytes.getLong(), playerIdBytes.getLong()));
				data.setPlayerName(resultSet.getString("username"));
				data.setBestTimeCleared(resultSet.getLong("time_cleared"));
				level.setLevelBestClearData(data);
			}
		}
	}

	private void loadMakerPlayerUnlockables(MakerPlayerData data, ResultSet result) throws SQLException {
		for (MakerUnlockable unlockable: MakerUnlockable.values()) {
			if (result.getBoolean(unlockable.name().toLowerCase())) {
				data.addUnlockable(unlockable);
			}
		}
	}

	private synchronized void loadNextSteveLevel(MakerPlayableLevel level, Set<Long> clearedAndSkipped) {
		verifyNotPrimaryThread();
		try {
			long levelSerial = 0;
			do {
				level.tryStatusTransition(LevelStatus.STEVE_LEVEL_LOAD_READY, LevelStatus.STEVE_LEVEL_LOADING);
				int random = RANDOM.nextInt(100);
				if (random < 50) {
					// last 50 from trending
					levelSerial = loadRandomPlayableLevelSerialFromTop50Trending();
				} else if (random < 75) {
					// more than 100 likes/unlikes difference
					levelSerial = loadRandomPlayableLevelSerialWithMoreThan100LikesUnlikesDifference();
				} else if (random < 90) {
					// premium with positive likes/unlikes difference
					levelSerial = loadRandomPlayableLevelSerialFromPremiumMakerWithPositiveLikesUnlikesDifference();
				} else {
					// anything
					levelSerial = loadRandomPlayableLevelSerial();
				}
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadNextSteveLevel - obtained next level serial: [%s] for random: [%s]", levelSerial, random));
				}
				if (levelSerial == 0) {
					levelSerial = loadRandomPlayableLevelSerial();
				}
			} while (levelSerial != 0 && clearedAndSkipped.contains(levelSerial));
			level.setLevelSerial(levelSerial);
			level.tryStatusTransition(LevelStatus.STEVE_LEVEL_LOADING, LevelStatus.LEVEL_LOAD_READY);
			loadPlayableLevelBySerial(level);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadNextSteveLevel - error: %s", e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void loadNextSteveLevelAsync(MakerPlayableLevel makerPlayableLevel, Set<Long> clearedAndSkipped) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadNextSteveLevel(makerPlayableLevel, clearedAndSkipped));
	}

	private synchronized void loadOrCreateMakerPlayerData(MakerPlayerData data) throws SQLException {
		String uuid = data.getUniqueId().toString().replace("-", "");
		// try to find player by binary UUID first
		try (PreparedStatement makerPlayerDataSt = getConnection().prepareStatement(String.format("SELECT * FROM mcmaker.players WHERE player_id = UNHEX(?)"))) {
			makerPlayerDataSt.setString(1, uuid);
			ResultSet dataResult = makerPlayerDataSt.executeQuery();
			if (dataResult.first()) {
				data.setSteveClear(dataResult.getBoolean("steve_clear"));
				loadMakerPlayerUnlockables(data, dataResult);
			} else {
				// not found - create maker player data
				if (Bukkit.getLogger().isLoggable(Level.INFO)) {
					Bukkit.getLogger().info(String.format("No MCMaker data - inserting MCMaker data for first time - player: [%s<%s>]", data.getUsername(), data.getUniqueId()));
				}
				try (PreparedStatement insertPlayerDataSt = getConnection().prepareStatement(String.format("INSERT INTO mcmaker.players(player_id) VALUES (UNHEX(?))"))) {
					insertPlayerDataSt.setString(1, uuid);
					insertPlayerDataSt.executeUpdate();
				}
				if (Bukkit.getLogger().isLoggable(Level.INFO)) {
					Bukkit.getLogger().info(String.format("Inserted MCMaker data for first time Player: [%s<%s>]", data.getUsername(), data.getUniqueId()));
				}
			}
		}
	}

	private synchronized void loadPlayableLevelBySerial(MakerPlayableLevel level) {
		verifyNotPrimaryThread();
		try {
			level.tryStatusTransition(LevelStatus.LEVEL_LOAD_READY, LevelStatus.LEVEL_LOADING);
			String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE, SELECT_ALL_FROM_LEVELS, "WHERE `levels`.`level_serial` = ? AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0");
			try (PreparedStatement loadLevelQuery = getConnection().prepareStatement(String.format(query))) {
				loadLevelQuery.setLong(1, level.getLevelSerial());
				ResultSet resultSet = loadLevelQuery.executeQuery();
				if (resultSet.next()) {
					loadLevelFromResult(level, resultSet);
					loadlLevelBestClearData(level);
					loadLevelPlayerBestClearData(level);
					level.tryStatusTransition(LevelStatus.LEVEL_LOADING, LevelStatus.CLIPBOARD_LOAD_READY);
					loadLevelClipboard(level);
				} else {
					level.disable(String.format("Unable to find level with serial: [%s]", level.getLevelSerial()));
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadPlayableLevelBySerial - error while loading level: %s", e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
			return;
		}
	}

	public void loadPlayableLevelBySerialAsync(MakerPlayableLevel level) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayableLevelBySerial(level));
	}

	private synchronized int[] loadPlayerLevelsCount(UUID authorId, boolean callback) {
		verifyNotPrimaryThread();
		checkNotNull(authorId);
		String uniqueId = authorId.toString().replace("-", "");
		String query =  "SELECT " +
							"SUM(CASE WHEN `levels`.`date_published` IS NOT NULL AND `levels`.`unpublished` = 0 THEN 1 ELSE 0 END) as published, " +
							"SUM(CASE WHEN `levels`.`date_published` IS NULL THEN 1 ELSE 0 END) as unpublished " +
						"FROM `mcmaker`.`levels` " +
						"WHERE `levels`.`author_id` = UNHEX(?) AND `levels`.`deleted` = 0 " +
						"GROUP BY `levels`.`author_id` ";
		int[] results = new int[2];
		try (PreparedStatement levelCountsSt = getConnection().prepareStatement(query)) {
			levelCountsSt.setString(1, uniqueId);
			ResultSet resultSet = levelCountsSt.executeQuery();
			if (resultSet.next()) {
				results[0] = resultSet.getInt("published");
				results[1] = resultSet.getInt("unpublished");
			}
			if (callback) {
				Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().playerLevelsCountCallback(authorId, results[0], results[1]));
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadPlayerLevelsCounts - player id: [%s] - published: [%s] - unpublished: [%s]", authorId, results[0], results[1]));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("loadPlayerLevelsCounts - error while getting player levels counts: %s", e.getMessage()));
			e.printStackTrace();
		}
		return results;
	}

	public void loadPlayerLevelsCountAsync(UUID authorId) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayerLevelsCount(authorId, true));
	}

	private synchronized void loadPlayerLevelsCounts(MakerPlayerData data) {
		verifyNotPrimaryThread();
		int[] levelCounts = loadPlayerLevelsCount(data.getUniqueId(), false);
		data.setPublishedLevelsCount(levelCounts[0]);
		data.setUnpublishedLevelsCount(levelCounts[1]);
	}

	private synchronized void loadPublishedLevelByLevelId(String levelId) {
		verifyNotPrimaryThread();
		checkNotNull(levelId);
		try {
			int levelCount = loadPublishedLevelsCount();
			String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
					SELECT_ALL_FROM_LEVELS,
					"WHERE `levels`.`level_id` = UNHEX(?) AND `levels`.`date_published` IS NOT NULL AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0");
			try (PreparedStatement loadLevelById = getConnection().prepareStatement(String.format(query))) {
				loadLevelById.setString(1, levelId);
				ResultSet resultSet = loadLevelById.executeQuery();
				if (resultSet.next()) {
					MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
					loadLevelFromResult(level, resultSet);
					loadlLevelBestClearData(level);
					Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().loadPublishedLevelCallback(level, levelCount));
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadPublishedLevel - error while loading level - %s", e.getMessage()));
			e.printStackTrace();
		}
	}

	public int loadPublishedLevelsCount() {
		verifyNotPrimaryThread();
		int levelCount = 0;
		try (PreparedStatement levelCountQuery = getConnection().prepareStatement(String.format("SELECT count(1) FROM `mcmaker`.`levels` WHERE `date_published` IS NOT NULL AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0"))) {
			ResultSet resultSet = levelCountQuery.executeQuery();
			if (resultSet.next()) {
				levelCount = resultSet.getInt(1);
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadPublishedLevelCount - total level count: [%s]", levelCount));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		}
		return levelCount;
	}

	public synchronized Set<MakerDisplayableLevel> loadPublishedLevelsPage(LevelSortBy levelSortBy, boolean reverseOrder, int pageOffset, int levelsPerPage) {
		verifyNotPrimaryThread();
		Set<MakerDisplayableLevel> levels = new LinkedHashSet<MakerDisplayableLevel>();
		String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
				SELECT_ALL_FROM_LEVELS,
				"WHERE `levels`.`date_published` IS NOT NULL AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0 ORDER BY `%s` %s, `levels`.`level_serial` %s LIMIT ?, ?");
		try (PreparedStatement levelPageQuery = getConnection().prepareStatement(String.format(query, levelSortBy.name(), reverseOrder ? "DESC" : "ASC", reverseOrder ? "DESC" : "ASC"))) {
			levelPageQuery.setInt(1, pageOffset);
			levelPageQuery.setInt(2, levelsPerPage);
			ResultSet resultSet = levelPageQuery.executeQuery();
			while (resultSet.next()) {
				MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
				loadLevelFromResult(level, resultSet);
				//loadLevelRecords(level);
				levels.add(level);
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadPublishedLevelsPage - error while loading levels page - %s", e.getMessage()));
			e.printStackTrace();
		}
		return levels;
	}

	private long loadRandomPlayableLevelSerial() {
		verifyNotPrimaryThread();
		String queryBase =
				"%s " +
				"FROM `mcmaker`.`levels` " +
				"WHERE `levels`.`date_published` IS NOT NULL " +
				"AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0 " +
				"%s";
		String countQuery = String.format(queryBase, "SELECT COUNT(`levels`.`level_serial`)", "");
		//Bukkit.getLogger().severe(String.format("[REMOVE] | MakerDatabaseAdapter.loadRandomPlayableLevelSerial - query: [%s]", countQuery));
		int count = 0;
		try (PreparedStatement countSt = getConnection().prepareStatement(countQuery)) {
			ResultSet resultSet = countSt.executeQuery();
			resultSet.next();
			count = resultSet.getInt(1);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRandomPlayableLevelSerial - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		if (count == 0) {
			return 0;
		}
		int offset = RANDOM.nextInt(count);
		String query = String.format(queryBase, "SELECT `levels`.`level_serial`", String.format("LIMIT %s, 1", offset));
		try (PreparedStatement loadRandomPublishedLevelSerialSt = getConnection().prepareStatement(query)) {
			ResultSet resultSet = loadRandomPublishedLevelSerialSt.executeQuery();
			if (resultSet.next()) {
				return resultSet.getLong("level_serial");
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRandomPlayableLevelSerialNotIn - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		return 0;
	}

	private long loadRandomPlayableLevelSerialFromPremiumMakerWithPositiveLikesUnlikesDifference() {
		verifyNotPrimaryThread();
		String queryBase =
				"%s " +
				"FROM `mcmaker`.`levels` " +
				"WHERE `levels`.`date_published` IS NOT NULL " +
				"AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0 " +
				"AND `levels`.`author_rank` != 'GUEST' " +
				"AND `levels`.`level_likes` - `levels`.`level_dislikes` > 0 " +
				"%s";
		String countQuery = String.format(queryBase, "SELECT COUNT(`levels`.`level_serial`)", "");
		int count = 0;
		try (PreparedStatement countSt = getConnection().prepareStatement(countQuery)) {
			ResultSet resultSet = countSt.executeQuery();
			resultSet.next();
			count = resultSet.getInt(1);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRandomPlayableLevelSerialFromPremiumMakerWithPositiveLikesUnlikesDifference - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		if (count == 0) {
			return 0;
		}
		int offset = RANDOM.nextInt(count);
		String query = String.format(queryBase, "SELECT `levels`.`level_serial`", String.format("LIMIT %s, 1", offset));
		try (PreparedStatement loadRandomPublishedLevelSerialSt = getConnection().prepareStatement(query)) {
			ResultSet resultSet = loadRandomPublishedLevelSerialSt.executeQuery();
			if (resultSet.next()) {
				return resultSet.getLong("level_serial");
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRandomPlayableLevelSerialFromPremiumMakerWithPositiveLikesUnlikesDifference - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		return 0;
	}

	private long loadRandomPlayableLevelSerialFromTop50Trending() {
		verifyNotPrimaryThread();
		int offset = RANDOM.nextInt(50);
		String query = String.format(	"SELECT `levels`.`level_serial` " +
										"FROM `mcmaker`.`levels` " +
										"WHERE `levels`.`date_published` IS NOT NULL " +
										"AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0 " +
										"ORDER BY `levels`.`trending_score` DESC " +
										"LIMIT %s, 1", offset);
		//Bukkit.getLogger().severe(String.format("[REMOVE] | MakerDatabaseAdapter.loadRandomPlayableLevelSerialFromTop50Trending - query: [%s]", query));
		try (PreparedStatement loadRandomPublishedLevelSerialSt = getConnection().prepareStatement(query)) {
			ResultSet resultSet = loadRandomPublishedLevelSerialSt.executeQuery();
			if (resultSet.next()) {
				return resultSet.getLong("level_serial");
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRandomPlayableLevelSerialFromTop50Trending - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		return 0;
	}

	private long loadRandomPlayableLevelSerialWithMoreThan100LikesUnlikesDifference() {
		verifyNotPrimaryThread();
		String queryBase =
				"%s " +
				"FROM `mcmaker`.`levels` " +
				"WHERE `levels`.`date_published` IS NOT NULL " +
				"AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0 " +
				"AND `levels`.`level_likes` - `levels`.`level_dislikes` > 100 " +
				"%s";
		String countQuery = String.format(queryBase, "SELECT COUNT(`levels`.`level_serial`)", "");
		int count = 0;
		try (PreparedStatement countSt = getConnection().prepareStatement(countQuery)) {
			ResultSet resultSet = countSt.executeQuery();
			resultSet.next();
			count = resultSet.getInt(1);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRandomPlayableLevelSerialWithMoreThan100LikesUnlikesDifference - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		if (count == 0) {
			return 0;
		}
		int offset = RANDOM.nextInt(count);
		String query = String.format(queryBase, "SELECT `levels`.`level_serial`", String.format("LIMIT %s, 1", offset));
		try (PreparedStatement loadRandomPublishedLevelSerialSt = getConnection().prepareStatement(query)) {
			ResultSet resultSet = loadRandomPublishedLevelSerialSt.executeQuery();
			if (resultSet.next()) {
				return resultSet.getLong("level_serial");
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRandomPlayableLevelSerialWithMoreThan100LikesUnlikesDifference - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		return 0;
	}

	private Rank loadRank(ResultSet result) {
		Rank rank = Rank.GUEST;
		try {
			rank = Rank.valueOf(result.getString("author_rank"));
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadRank - unable to load level author rank - %s", e.getMessage()));
		}
		return rank;
	}

	private MakerRelativeLocationData loadRelativeLocationById(UUID locationUUID) throws SQLException, DataException {
		checkNotNull(locationUUID);
		String locationIdString = locationUUID.toString().replace("-", "");
		try (PreparedStatement loadLocationByIdSt = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`locations` where `location_id` = UNHEX(?)"))) {
			loadLocationByIdSt.setString(1, locationIdString);
			ResultSet resultSet = loadLocationByIdSt.executeQuery();
			if (!resultSet.next()) {
				throw new DataException(String.format("Unable to find maker location with id: [%s]", locationUUID));
			}
			MakerRelativeLocationData location = new MakerRelativeLocationData(resultSet.getDouble("location_x"), resultSet.getDouble("location_y"), resultSet.getDouble("location_z"), resultSet.getFloat("location_yaw"), resultSet.getFloat("location_pitch"));
			location.setLocationId(locationUUID);
			return location;
		}
	}

	private WorldTimeAndWeather loadTimeAndWeather(ResultSet result, String columnName) {
		checkNotNull(result);
		checkNotNull(columnName);
		WorldTimeAndWeather timeAndWeather = WorldTimeAndWeather.NOON_CLEAR;
		try {
			timeAndWeather = WorldTimeAndWeather.valueOf(result.getString(columnName));
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadTimeAndWeather - unable to load level time and weather - error: %s", e.getMessage()));
		}
		return timeAndWeather;
	}

	private synchronized void loadUniqueLevelClearsCount(MakerPlayerData data) {
		verifyNotPrimaryThread();
		data.setUniqueLevelClearsCount(loadUniqueLevelClearsCount(data.getUniqueId(), false));
	}

	private synchronized long loadUniqueLevelClearsCount(UUID playerId, boolean callback) {
		verifyNotPrimaryThread();
		checkNotNull(playerId);
		String uniqueId = playerId.toString().replace("-", "");
		String query =  "SELECT COUNT(DISTINCT `level_clears`.`level_id`) as unique_clears_count " +
						"FROM `mcmaker`.`level_clears` " +
						"WHERE `level_clears`.`player_id` = UNHEX(?) ";
		long result = 0;
		try (PreparedStatement levelCountsSt = getConnection().prepareStatement(query)) {
			levelCountsSt.setString(1, uniqueId);
			ResultSet resultSet = levelCountsSt.executeQuery();
			if (resultSet.next()) {
				result = resultSet.getLong("unique_clears_count");
			}
			if (callback) {
				final long finalResult = result;
				Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().playerLevelClearsCountCallback(playerId, finalResult));
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadUniqueLevelClearsCount - player id: [%s] - unique clears count: [%s]", playerId, result));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadUniqueLevelClearsCount - error while getting player level clears count: %s", e.getMessage()));
			e.printStackTrace();
		}
		return result;
	}

	private synchronized void publishLevel(MakerPlayableLevel level) {
		verifyNotPrimaryThread();
		try {
			level.tryStatusTransition(LevelStatus.PUBLISH_READY, LevelStatus.PUBLISHING);
			String levelId = level.getLevelId().toString().replace("-", "");
			try (PreparedStatement updateLevelSt = getConnection().prepareStatement("UPDATE `mcmaker`.`levels` SET `date_published` = CURRENT_TIMESTAMP WHERE `level_id` = UNHEX(?)")) {
				updateLevelSt.setString(1, levelId);
				updateLevelSt.executeUpdate();
			}
			try (PreparedStatement getPublishedDateSt = getConnection().prepareStatement("SELECT `date_published` FROM `mcmaker`.`levels` WHERE `level_id` = UNHEX(?)")) {
				getPublishedDateSt.setString(1, levelId);
				ResultSet resultSet = getPublishedDateSt.executeQuery();
				if (!resultSet.next()) {
					level.disable(String.format("Unable to find level published date for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
					return;
				}
				level.setDatePublished(resultSet.getTimestamp("date_published"));
			}
			loadPlayerLevelsCount(level.getAuthorId(), true);
			updateLevelTrendingScore(level.getLevelId(), calculateTrendingScore(0, 0, level.getDatePublished()));
			level.tryStatusTransition(LevelStatus.PUBLISHING, LevelStatus.PUBLISHED);
			loadPublishedLevelByLevelId(levelId);
			if (Bukkit.getLogger().isLoggable(Level.INFO)) {
				Bukkit.getLogger().info(String.format("MakerDatabaseAdapter.publishLevel - level updated without errors: [%s<%s>]", level.getLevelName(), level.getLevelId()));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.publishLevel - error while updating level: [%s<%s>] - ", level.getLevelName(), level.getLevelId(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void publishLevelAsync(MakerPlayableLevel level) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> publishLevel(level));
	}

	private synchronized void renameLevel(MakerPlayableLevel level, String newName) {
		verifyNotPrimaryThread();
		try {
			level.tryStatusTransition(LevelStatus.RENAME_READY, LevelStatus.RENAMING);
			String levelId = level.getLevelId().toString().replace("-", "");
			String currentLevelName = level.getLevelName();
			int affected = 0;
			try (PreparedStatement updateLevelSt = getConnection().prepareStatement("UPDATE `mcmaker`.`levels` SET `level_name` = ? WHERE `level_id` = UNHEX(?)")) {
				updateLevelSt.setString(1, newName);
				updateLevelSt.setString(2, levelId);
				affected = updateLevelSt.executeUpdate();
				if (affected == 0) {
					level.setLevelName(newName);
					insertLevel(level);
					if (level.getClipboard() != null) {
						insertClipboard(levelId, level.getClipboard());
					}
					loadPlayerLevelsCountAsync(level.getAuthorId());
				}
			} catch (SQLException e) {
				e.printStackTrace();
				level.setLevelName(currentLevelName);
				level.tryStatusTransition(LevelStatus.RENAMING, LevelStatus.RENAME_ERROR);
				return;
			}
			level.tryStatusTransition(LevelStatus.RENAMING, LevelStatus.RENAMED);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.renameLevel - error while renaming level: [%s<%s>] - %s", level.getLevelName(), level.getLevelId(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void renameLevelAsync(MakerPlayableLevel level, String newName) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> renameLevel(level, newName));
	}

	public synchronized void saveLevel(MakerPlayableLevel level) {
		verifyNotPrimaryThread();
		String levelId = level.getLevelId().toString().replace("-", "");
		try {
			level.tryStatusTransition(LevelStatus.SAVE_READY, LevelStatus.SAVING);
			if (level.getLevelSerial() > 0) {
				String endLocationUUID = null;
				if (level.getRelativeEndLocation() != null) {
					endLocationUUID = insertOrUpdateRelativeLocation(level.getRelativeEndLocation()).toString().replace("-", "");
				}
				// after every save the author needs to clear the level again in order to publish it
				String updateBase = "UPDATE `mcmaker`.`levels` SET `level_time_and_weather` = ?, `author_rank` = ?, `author_name` = ?, `author_cleared` = 0%s WHERE `level_id` = UNHEX(?)";
				String updateStatement = null;
				if (endLocationUUID != null) {
					updateStatement = String.format(updateBase, ", `end_location_id` = UNHEX(?)");
				} else {
					updateStatement = String.format(updateBase, "");
				}
				int changed = 0;
				try (PreparedStatement updateLevelSt = getConnection().prepareStatement(updateStatement)) {
					updateLevelSt.setString(1, level.getTimeAndWeather() != null ? level.getTimeAndWeather().name() : WorldTimeAndWeather.NOON_CLEAR.name());
					updateLevelSt.setString(2, level.getAuthorRank() != null ? level.getAuthorRank().name() : Rank.GUEST.name());
					updateLevelSt.setString(3, level.getAuthorName());
					if (endLocationUUID != null) {
						updateLevelSt.setString(4, endLocationUUID);
						updateLevelSt.setString(5, levelId);
					} else {
						updateLevelSt.setString(4, levelId);
					}
					changed = updateLevelSt.executeUpdate();
				}
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.saveLevel - updated shallow data for level: [%s<%s>] - changed: [%s]", level.getLevelName(), level.getLevelId(), changed > 0));
				}
			} else {
				insertLevel(level);
			}
			// insert clipboard
			if (level.getClipboard() != null) {
				insertClipboard(levelId, level.getClipboard());
			}
			level.tryStatusTransition(LevelStatus.SAVING, LevelStatus.SAVED);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.saveLevel - level saved without errors: [%s<%s>]", level.getLevelName(), level.getLevelId()));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.saveLevel - error while saving Level with id: [%s] and slot [%s] - %s", level.getLevelId(), level.getChunkZ(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void saveLevelClearAsync(UUID levelId, UUID playerId, long clearTimeMillis) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> insertLevelClear(levelId, playerId, clearTimeMillis));
	}

	public void saveReportAsync(UUID playerId, String playerName, String report) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> insertReport(playerId, playerName, report));
	}

	private int searchPublishedLevelsCountByName(String searchString) {
		verifyNotPrimaryThread();
		int levelCount = 0;
		String query = 
				"SELECT count(`level_serial`) " +
				"FROM `mcmaker`.`levels` " +
				"WHERE `date_published` IS NOT NULL AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0 AND `levels`.`level_name` like ?";
		try (PreparedStatement searchCountQuery = getConnection().prepareStatement(String.format(query))) {
			searchCountQuery.setString(1, String.format("%%%s%%", searchString));
			ResultSet resultSet = searchCountQuery.executeQuery();
			if (resultSet.next()) {
				levelCount = resultSet.getInt(1);
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadPublishedLevelCount - total level count: [%s]", levelCount));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		}
		return levelCount;
	}

	private synchronized Set<MakerDisplayableLevel> searchPublishedLevelsPageByName(String searchString, int pageOffset, int levelsPerPage) {
		verifyNotPrimaryThread();
		checkNotNull(searchString);
		Set<MakerDisplayableLevel> levels = new LinkedHashSet<MakerDisplayableLevel>();
		if (StringUtils.isBlank(searchString)) {
			return levels;
		}
		String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
				SELECT_ALL_FROM_LEVELS,
				"WHERE `levels`.`date_published` IS NOT NULL AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0 AND `levels`.`level_name` like ? " +
				"ORDER BY `levels`.`level_name` ASC LIMIT ?, ?");
		try (PreparedStatement searchByNameQuery = getConnection().prepareStatement(query)) {
			searchByNameQuery.setString(1, String.format("%%%s%%", searchString));
			searchByNameQuery.setInt(2, pageOffset);
			searchByNameQuery.setInt(3, levelsPerPage);
			ResultSet resultSet = searchByNameQuery.executeQuery();
			while (resultSet.next()) {
				MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
				loadLevelFromResult(level, resultSet);
				loadlLevelBestClearData(level);
				levels.add(level);
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.searchPublishedLevelsPageByName - error while loading level - %s", e.getMessage()));
			e.printStackTrace();
		}
		return levels;
	}

	private void searchPublishedLevelsPageByName(final UUID playerId, final String searchString, int pageOffset, int levelsPerPage) {
		checkNotNull(searchString);
		verifyNotPrimaryThread();
		int levelCount = 0;
		Set<MakerDisplayableLevel> levelSet = null;
		try {
			levelCount = searchPublishedLevelsCountByName(searchString);
			levelSet = searchPublishedLevelsPageByName(searchString, pageOffset, levelsPerPage);
		} finally {
			final int finalCount = levelCount;
			final Set<MakerDisplayableLevel> finalLevelSet = levelSet;
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().searchLevelsCallback(playerId, searchString, finalCount, finalLevelSet));
		}
	}

	public void searchPublishedLevelsPageByNameAsync(UUID playerId, String searchString, int pageOffset, int levelsPerPage) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> searchPublishedLevelsPageByName(playerId, searchString, pageOffset, levelsPerPage));
	}

	public boolean testConnection() {
		try {
			getConnection();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private synchronized CoinTransactionResult tryToCommitCoinTransaction(final UUID uniqueId, long coins) throws SQLException {
		verifyNotPrimaryThread();
		String uuid = uniqueId.toString().replace("-", "");
		try (PreparedStatement findAccountSt = getConnection().prepareStatement(String.format("SELECT %s FROM %s.accounts WHERE unique_id = UNHEX(?)", coinsColumn, networkSchema))) {
			findAccountSt.setString(1, uuid);
			ResultSet result = findAccountSt.executeQuery();
			if (!result.first()) {
				return CoinTransactionResult.ACCOUNT_NOT_FOUND;
			}
			try (PreparedStatement updateCoinsSt = getConnection().prepareStatement(String.format("UPDATE %s.accounts SET %s = %s + ? WHERE (%s + ?) >= 0 AND unique_id = UNHEX(?) ", networkSchema, coinsColumn, coinsColumn, coinsColumn))) {
				updateCoinsSt.setLong(1, coins);
				updateCoinsSt.setLong(2, coins);
				updateCoinsSt.setString(3, uuid);
				if (updateCoinsSt.executeUpdate() == 0) {
					return CoinTransactionResult.INSUFFICIENT_COINS;
				}
			}
		}
		return CoinTransactionResult.COMMITTED;
	}

	private void unlock(final MakerPlayer mPlayer, final MakerUnlockable unlockable) {
		verifyNotPrimaryThread();
		checkNotNull(mPlayer);
		checkNotNull(unlockable);
		Long balance = null;
		UnlockOperationResult result = UnlockOperationResult.ERROR;
		try {
			String isUnlockedQuery = "SELECT `players`.`%s` FROM `mcmaker`.`players` WHERE `players`.`player_id` = UNHEX(?)";
			try (PreparedStatement isUnlockedSt = getConnection().prepareStatement(String.format(isUnlockedQuery, unlockable.name().toLowerCase()))) {
				isUnlockedSt.setString(1, mPlayer.getUniqueId().toString().replace("-", ""));
				ResultSet resultSet = isUnlockedSt.executeQuery();
				if (!resultSet.next()) {
					result = UnlockOperationResult.NOT_FOUND;
					return;
				}
				if (resultSet.getBoolean(unlockable.name().toLowerCase())) {
					result = UnlockOperationResult.ALREADY_UNLOCKED;
					return;
				}
			}
			String description = plugin.getMessage("coin.transaction.unlock.description", mPlayer.getName(), unlockable.name().toLowerCase());
			CoinTransaction transaction = new CoinTransaction(UUID.randomUUID(), mPlayer.getUniqueId(), -unlockable.getCost(), plugin.getServerUniqueId(), SourceType.SERVER, Reason.UNLOCKABLE, description);
			switch (executeCoinTransaction(transaction, false)) {
			case COMMITTED:
				balance = getCoinBalance(mPlayer.getUniqueId());
				break;
			case INSUFFICIENT_COINS:
				result = UnlockOperationResult.INSUFFICIENT_COINS;
				return;
			default:
				return;
			};
			String unlockUpdate = "UPDATE `mcmaker`.`players` SET `players`.`%s` = 1 WHERE `players`.`player_id` = UNHEX(?)";
			try (PreparedStatement unlockSt = getConnection().prepareStatement(String.format(unlockUpdate, unlockable.name().toLowerCase()))) {
				unlockSt.setString(1, mPlayer.getUniqueId().toString().replace("-", ""));
				unlockSt.executeUpdate();
				result = UnlockOperationResult.SUCCESS;
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.unlock - error while unlocking: [%s] for player: [%s] - %s", unlockable.name(), mPlayer.getName(), e.getMessage()));
			e.printStackTrace();
		} finally {
			final UnlockOperationResult finalResult = result;
			final Long finalBalance = balance;
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().unlockCallback(mPlayer.getUniqueId(), unlockable, finalResult, finalBalance));
		}
	}

	public void unlockAsync(MakerPlayer mPlayer, MakerUnlockable unlockable) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> unlock(mPlayer, unlockable));
	}

	private synchronized void unpublishLevelBySerial(long levelSerial, MakerPlayer mPlayer) {
		verifyNotPrimaryThread();
		UUID authorId = null;
		String levelName = null;
		Long balance = null;
		Integer levelCount = null;
		LevelOperationResult result = LevelOperationResult.ERROR;
		try {
			String findQuery = "SELECT `levels`.`author_id`, `levels`.`level_name`, `levels`.`date_published`, `levels`.`unpublished` FROM `mcmaker`.`levels` WHERE `levels`.`level_serial` = ? AND `levels`.`deleted` = 0";
			try (PreparedStatement findAuthorQuery = getConnection().prepareStatement(String.format(findQuery))) {
				findAuthorQuery.setLong(1, levelSerial);
				ResultSet resultSet = findAuthorQuery.executeQuery();
				if (!resultSet.next()) {
					result = LevelOperationResult.NOT_FOUND;
					return;
				}
				ByteBuffer authorIdBytes = ByteBuffer.wrap(resultSet.getBytes("author_id"));
				authorId = new UUID(authorIdBytes.getLong(), authorIdBytes.getLong());
				if (!authorId.equals(mPlayer.getUniqueId()) && !mPlayer.hasRank(Rank.ADMIN)){
					result = LevelOperationResult.PERMISSION_DENIED;
					return;
				}
				Date datePublished = resultSet.getTimestamp("date_published");
				if (datePublished == null) {
					result = LevelOperationResult.NOT_PUBLISHED;
					return;
				}
				if (resultSet.getBoolean("unpublished")) {
					result = LevelOperationResult.ALREADY_UNPUBLISHED;
					return;
				}
				levelName = resultSet.getString("level_name");
			}
			if (!mPlayer.hasRank(Rank.ADMIN)) {
				String description = plugin.getMessage("coin.transaction.level-unpublish.description", mPlayer.getName(), levelName);
				CoinTransaction transaction = new CoinTransaction(UUID.randomUUID(), mPlayer.getUniqueId(), -500, plugin.getServerUniqueId(), SourceType.SERVER, Reason.LEVEL_UNPUBLISH, description);
				switch (executeCoinTransaction(transaction, false)) {
				case COMMITTED:
					balance = getCoinBalance(mPlayer.getUniqueId());
					break;
				case INSUFFICIENT_COINS:
					result = LevelOperationResult.INSUFFICIENT_COINS;
					return;
				default:
					return;
				};
			}
			String unpublishQuery = "UPDATE `mcmaker`.`levels` SET `levels`.`unpublished` = 1 WHERE `levels`.`level_serial` = ? AND `levels`.`author_id` = UNHEX(?) AND `levels`.`deleted` = 0 AND `levels`.`unpublished` = 0";
			try (PreparedStatement unpublishLevelSt = getConnection().prepareStatement(String.format(unpublishQuery))) {
				unpublishLevelSt.setLong(1, levelSerial);
				unpublishLevelSt.setString(2, authorId.toString().replace("-", ""));
				unpublishLevelSt.executeUpdate();
				result = LevelOperationResult.SUCCESS;
				levelCount = loadPublishedLevelsCount();
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.unpublishLevelBySerial - error while unpublishing level: %s", e.getMessage()));
			e.printStackTrace();
		} finally {
			final LevelOperationResult finalResult = result;
			final Long finalBalance = balance;
			final Integer finalLevelCount = levelCount;
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().unpublishLevelBySerialCallback(levelSerial, mPlayer.getUniqueId(), finalResult, finalBalance, finalLevelCount));
		}
	}

	public void unpublishLevelBySerialAsync(long levelSerial, MakerPlayer mPlayer) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> unpublishLevelBySerial(levelSerial, mPlayer));
	}

	private synchronized void updateLevelAuthorClearTime(UUID levelId, long clearTimeMillis) {
		verifyNotPrimaryThread();
		try {
			String levelIdString = levelId.toString().replace("-", "");
			try (PreparedStatement updateLevelSt = getConnection().prepareStatement("UPDATE `mcmaker`.`levels` SET `author_cleared` = CASE WHEN `author_cleared` = 0 THEN ? ELSE LEAST(`author_cleared`, ?) END WHERE `level_id` = UNHEX(?)")) {
				updateLevelSt.setLong(1, clearTimeMillis);
				updateLevelSt.setLong(2, clearTimeMillis);
				updateLevelSt.setString(3, levelIdString);
				updateLevelSt.executeUpdate();
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.updateLevelAuthorClearTime - level updated without errors: [%s]", levelId));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.updateLevelAuthorClearTime - error while updating level: [%s] - ", levelId, e.getMessage()));
			e.printStackTrace();
		}
	}

	public void updateLevelAuthorClearTimeAsync(UUID levelId, long clearTimeMillis) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> updateLevelAuthorClearTime(levelId, clearTimeMillis));
	}

	private synchronized void updateLevelTrendingScore(UUID levelId, long trendingScore) {
		try {
			String levelIdString = levelId.toString().replace("-", "");
			try (PreparedStatement updateLevelSt = getConnection().prepareStatement("UPDATE `mcmaker`.`levels` SET `trending_score` = ? WHERE `level_id` = UNHEX(?)")) {
				updateLevelSt.setLong(1, trendingScore);
				updateLevelSt.setString(2, levelIdString);
				updateLevelSt.executeUpdate();
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.updateLevelTrendingScore - level updated without errors: [%s]", levelId));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.updateLevelTrendingScore - error while updating level: [%s] - ", levelId, e.getMessage()));
			e.printStackTrace();
		}
	}

	@Deprecated
	// use rabbit instead
	public synchronized void updatePlayersCount() {
		verifyNotPrimaryThread();
		final int count = plugin.getController().getPlayerCount();
		if (count == this.lastCount) {
			return;
		}
		this.lastCount = count;
		try (PreparedStatement stmt = getConnection().prepareStatement("UPDATE `mcmaker`.`servers` SET `players` = ? WHERE `serverid` = ?")) {
			stmt.setInt(1, count);
			stmt.setString(2, String.valueOf(plugin.getServerBungeeId()));
			stmt.executeUpdate();
		} catch (SQLException e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.updatePlayersCount - error: %s", e.getMessage()));
			e.printStackTrace();
		}
	}

}
