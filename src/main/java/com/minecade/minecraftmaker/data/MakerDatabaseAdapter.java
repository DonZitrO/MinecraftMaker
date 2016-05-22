package com.minecade.minecraftmaker.data;

import static com.google.common.base.Preconditions.checkNotNull;

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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import com.minecade.core.data.DatabaseException;
import com.minecade.core.data.MinecadeAccountData;
import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.inventory.PlayerLevelsMenu;
import com.minecade.minecraftmaker.level.AbstractMakerLevel;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.DataException;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.io.ClipboardFormat;
import com.minecade.minecraftmaker.schematic.io.ClipboardReader;
import com.minecade.minecraftmaker.schematic.io.ClipboardWriter;
import com.minecade.minecraftmaker.util.LevelUtils;

public class MakerDatabaseAdapter {

	private static final String LOAD_LEVEL_WITH_DATA_QUERY_BASE =
			"%s, " + // select place holder
			"SUM(CASE WHEN `dislike` = 0 THEN 1 ELSE 0 END) as likes, " +
			"SUM(CASE WHEN `dislike` = 1 THEN 1 ELSE 0 END) as dislikes " +
			"FROM `mcmaker`.`levels` " +
			"LEFT JOIN `mcmaker`.`level_likes` on `levels`.`level_id` = `level_likes`.`level_id` " +
			"%s " + // before grouping clauses placeholder
			"GROUP BY `levels`.`level_id` " + 
			"%s "; // after grouping clauses placeholder
	private static final String SELECT_ALL_FROM_LEVELS = "SELECT `levels`.*";

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

	public synchronized void disconnect() {
		if (null != connection) {
			try {
				connection.close();
			} catch (SQLException e) {
				// no-op
			}
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.insertLevel - inserting shallow data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
		}
		String levelId = level.getLevelId().toString().replace("-", "");
		String authorId = level.getAuthorId().toString().replace("-", "");
		String endLocationUUID = null;
		if (level.getRelativeEndLocation() != null) {
			endLocationUUID = insertOrUpdateRelativeLocation(level.getRelativeEndLocation()).toString().replace("-", "");
		}
		String insertBase = "INSERT INTO `mcmaker`.`levels` (level_id, author_id, level_name, author_name, author_rank%s) VALUES (UNHEX(?), UNHEX(?), ?, ?, ?%s)";
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
			insertLevelSt.setString(4, level.getAuthorName());
			insertLevelSt.setString(5, level.getAuthorRank() != null ? level.getAuthorRank().name() : null);
			if (endLocationUUID != null) {
				insertLevelSt.setString(6, endLocationUUID);
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().severe(String.format("[DEBUG] | MakerDatabaseAdapter.insertLevelClear - inserting level clear time millis: [%s] for level: [%s] and player: [%s]", clearTimeMillis, levelId, playerId));
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
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
			long[] likesAndDislikes = loadLevelLikesAndDislikes(levelId);
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().levelLikeCallback(levelId, playerId, dislike, likesAndDislikes[0], likesAndDislikes[1]));
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
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
					account.addRank(rank);
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
		// FIXME: review this
		this.loadPlayerLevelsCleared(data);
		this.loadPlayerLevelsLikes(data);
	}

	private synchronized void loadPlayableLevelBySerial(MakerPlayableLevel level) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		//String query = 
		String query = "SELECT * FROM `mcmaker`.`levels` where `level_serial` = ?";
		try (PreparedStatement loadLevelQuery = getConnection().prepareStatement(String.format(query))) {
			loadLevelQuery.setLong(1, level.getLevelSerial());
			ResultSet resultSet = loadLevelQuery.executeQuery();
			if (resultSet.next()) {
				loadLevelFromResult(level, resultSet, false);
				loadLevelClipboard(level);
				loadLevelRecords(level);
			} else {
				level.disable(String.format("Unable to find level with serial: [%s]", level.getLevelSerial()), null);
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("loadLevelBySerial.loadLevel - error while loading level: %s", e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
			return;
		}
	}

	public void loadPlayableLevelBySerialAsync(MakerPlayableLevel level) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayableLevelBySerial(level));
	}

	private void loadLevelClipboard(MakerPlayableLevel level) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		try {
			level.tryStatusTransition(LevelStatus.BLANK, LevelStatus.CLIPBOARD_LOADING);
			String levelId = level.getLevelId().toString().replace("-", "");
			try (PreparedStatement testQuery = getConnection().prepareStatement(
			        String.format("SELECT * FROM `mcmaker`.`schematics` where `level_id` = UNHEX(?) order by `updated` desc limit 1"))) {
				testQuery.setString(1, levelId);
				ResultSet resultSet = testQuery.executeQuery();
				if (!resultSet.next()) {
					throw new DataException(String.format("Unable to find schematic for level with id: [%s]", level.getLevelId()));
				}
				loadLevelClipboardFromResult(level, resultSet);
				level.tryStatusTransition(LevelStatus.CLIPBOARD_LOADING, LevelStatus.CLIPBOARD_LOADED);
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadLevelClipboard - level clipboard loaded without errors: [%s<%s>]", level.getLevelName(), level.getLevelId()));
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("loadLevelBySerial.loadLevelClipboard - error while loading level clipboard: %s", e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	private void loadLevelClipboardFromResult(MakerPlayableLevel level, ResultSet resultSet) throws DataException {
		Blob data = null;
		try {
			data = resultSet.getBlob("data");
			try (BufferedInputStream is = new BufferedInputStream(data.getBinaryStream()); ClipboardReader reader = ClipboardFormat.SCHEMATIC.getReader(is)) {
				// note: this particular implementation doesn't need world data for anything. TODO: find other places where WorldData is not needed.
				Clipboard clipboard = reader.read(null);
				if (clipboard.getRegion().getHeight() > 128) {
						Bukkit.getLogger().severe(String.format("remove this level: [%s]", level.getLevelName()));
						clipboard.setOrigin(LevelUtils.getLevelOrigin(level.getChunkZ()).add(0, -48, 0));
					if (level.getRelativeEndLocation() != null) {
						level.setRelativeEndLocation(new MakerRelativeLocationData(level.getEndLocation().add(0,-48,0),level.getRelativeEndLocation().getLocationId()));
					}
				} else {
					clipboard.setOrigin(LevelUtils.getLevelOrigin(level.getChunkZ()));
				}
				level.setClipboard(clipboard);
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

	private void loadLevelFromResult(AbstractMakerLevel level, ResultSet result, boolean published) throws SQLException, DataException {
		ByteBuffer levelIdBytes = ByteBuffer.wrap(result.getBytes("level_id"));
		ByteBuffer authorIdBytes = ByteBuffer.wrap(result.getBytes("author_id"));
		level.setLevelId(new UUID(levelIdBytes.getLong(), levelIdBytes.getLong()));
		level.setLevelSerial(result.getLong("level_serial"));
		level.setLevelName(result.getString("level_name"));
		level.setAuthorId(new UUID(authorIdBytes.getLong(), authorIdBytes.getLong()));
		level.setAuthorName(result.getString("author_name"));
		level.setAuthorRank(loadRank(result));
		byte[] locationId = result.getBytes("end_location_id");
		if (locationId != null) {
			level.setRelativeEndLocation(loadRelativeLocationById(locationId));
		}
		level.setDatePublished(result.getDate("date_published"));
		if (published) {
			level.setLikes(result.getLong("likes"));
			level.setDislikes(result.getLong("dislikes"));
		}
	}

	private synchronized long[] loadLevelLikesAndDislikes(UUID levelId) throws SQLException {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		String levelIdString = levelId.toString().replace("-", "");
		try (PreparedStatement selectLikesDislikesSt = getConnection().prepareStatement(
				"SELECT SUM(CASE WHEN `dislike` = 0 THEN 1 ELSE 0 END) as likes, SUM(CASE WHEN `dislike` = 1 THEN 1 ELSE 0 END) as dislikes FROM `mcmaker`.`level_likes` WHERE `level_id` = UNHEX(?)")) {
			selectLikesDislikesSt.setString(1, levelIdString);
			ResultSet result = selectLikesDislikesSt.executeQuery();
			result.next();
			return new long[] { result.getLong("likes"), result.getLong("dislikes") };
		}
	}

	@Deprecated // FIXME: integrate this in an unique level load query
	private synchronized void loadLevelRecords(AbstractMakerLevel makerLevel) throws SQLException {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}

		String levelId = makerLevel.getLevelId().toString().replace("-", "");
		try (PreparedStatement preparedStatement = getConnection().prepareStatement(
				" SELECT a.unique_id, a.username, COUNT(lc.tries) tries, MIN(lc.time_cleared) time_cleared" +
				" FROM mcmaker.level_clears lc " +
				" INNER JOIN minecade.accounts a ON a.unique_id = lc.player_id " +
				" WHERE lc.time_cleared > 0 AND lc.level_id = UNHEX(?) " +
				" GROUP BY level_id, a.unique_id, a.username " +
				" ORDER BY lc.time_cleared ")) {
			preparedStatement.setString(1, levelId);
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				ByteBuffer uniqueId = ByteBuffer.wrap(resultSet.getBytes("unique_id"));

				MakerLevelClearData levelClear = new MakerLevelClearData();
				levelClear.setUniqueId(new UUID(uniqueId.getLong(), uniqueId.getLong()));
				levelClear.setUsername(resultSet.getString("username"));
				levelClear.setTries(resultSet.getInt("tries"));
				levelClear.setTimeCleared(resultSet.getLong("time_cleared"));
				makerLevel.getLevelsClear().add(levelClear);
			}
		}
	}

	public synchronized Set<Long> loadPublishedLevelSerialsPage(LevelSortBy sortBy, boolean reverse, int offset, int limit) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		
		String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
				"SELECT `levels`.`level_serial`",
				"WHERE `levels`.`date_published` IS NOT NULL",
				"ORDER BY ? %s, `levels`.`level_serial` %s " +
				"LIMIT ?, ?");
		Set<Long> serials = new LinkedHashSet<Long>();
		try (PreparedStatement levelPageQuery = getConnection().prepareStatement(String.format(query, reverse ? "DESC" : "ASC", reverse ? "DESC" : "ASC"))) {
			levelPageQuery.setString(1, sortBy.name());
			levelPageQuery.setInt(2, offset);
			levelPageQuery.setInt(3, limit);
			ResultSet resultSet = levelPageQuery.executeQuery();
			while (resultSet.next()) {
				serials.add(resultSet.getLong(1));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		}
		return serials;
	}

	private synchronized void loadPlayerLevelsCleared(MakerPlayerData data) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		String uniqueId = data.getUniqueId().toString().replace("-", "");
		// Cleared Levels
		try (PreparedStatement query = getConnection().prepareStatement(
				String.format("SELECT tries, time_cleared FROM mcmaker.level_clears WHERE player_id = UNHEX(?)"))) {
			query.setString(1, uniqueId);
			ResultSet resultSet = query.executeQuery();

			while (resultSet.next()) {
				MakerLevelClearData levelClear = new MakerLevelClearData();
				levelClear.setUniqueId(data.getUniqueId());
				levelClear.setUsername(data.getUsername());
				levelClear.setTries(resultSet.getInt("tries"));
				levelClear.setTimeCleared(resultSet.getInt("time_cleared"));
				data.getLevelsClear().add(levelClear);
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String
					.format("loadLevelsCleared - error while getting player's cleared levels: %s", e.getMessage()));
			e.printStackTrace();
		}
	}

	private synchronized void loadPlayerLevelsLikes(MakerPlayerData data){
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		String uniqueId = data.getUniqueId().toString().replace("-", "");
		// Cleared Levels
		try (PreparedStatement query = getConnection().prepareStatement(String.format(
				"SELECT COUNT(ll.level_id) likes FROM levels l INNER JOIN level_likes ll ON ll.level_id = l.level_id WHERE ll.dislike = 0 AND l.author_id = UNHEX(?)"))) {
			query.setString(1, uniqueId);
			ResultSet resultSet = query.executeQuery();

			if (resultSet.next()) {
				data.setLevelsLikes(resultSet.getInt("likes"));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("loadPlayerLevelsLikes - error while getting player's levels likes: %s", e.getMessage()));
			e.printStackTrace();
		}
	}

	private synchronized void loadPublishedLevelByLevelId(String levelId) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		checkNotNull(levelId);
		try {
			int levelCount = loadPublishedLevelsCount(false);
			String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
					SELECT_ALL_FROM_LEVELS,
					"WHERE `levels`.`level_id` = UNHEX(?) AND `levels`.`date_published` IS NOT NULL", "");
			try (PreparedStatement loadLevelById = getConnection().prepareStatement(String.format(query))) {
				loadLevelById.setString(1, levelId);
				ResultSet resultSet = loadLevelById.executeQuery();
				if (resultSet.next()) {
					MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
					loadLevelFromResult(level, resultSet, true);
					loadLevelRecords(level);
					Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().loadPublishedLevelCallback(level, levelCount));
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadPublishedLevel - error while loading level - %s", e.getMessage()));
			e.printStackTrace();
		}
	}

	@Deprecated
	private synchronized void loadPublishedLevelsPage(LevelSortBy sortBy, boolean reverse, int offset, int limit, final UUID playerId) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadPublishedLevels - sortBy: [%s] - offset: [%s] - limit: [%s]", sortBy.name(), offset, limit));
		}
		checkNotNull(sortBy);
		int levelCount=0;
		List<MakerDisplayableLevel> levels = new ArrayList<>(limit);
		try {
			levelCount = loadPublishedLevelsCount(false);
			String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
					SELECT_ALL_FROM_LEVELS,
					"WHERE `date_published` IS NOT NULL ",
					"ORDER BY ? %s, `levels`.`level_serial` %s " +
					"LIMIT ?, ?");
			try (PreparedStatement loadLevelPageSt = getConnection().prepareStatement(String.format(query, reverse ? "DESC" : "ASC", reverse ? "DESC" : "ASC"))) {
				loadLevelPageSt.setString(1, sortBy.name());
				loadLevelPageSt.setInt(2, offset);
				loadLevelPageSt.setInt(3, limit);
				ResultSet resultSet = loadLevelPageSt.executeQuery();
				while (resultSet.next()) {
					MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
					loadLevelFromResult(level, resultSet, true);
					loadLevelRecords(level);
					levels.add(level);
				}
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadPublishedLevels - loaded levels: [%s]", levels.size()));
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		}
		final int finalLevelCount = levelCount;
		Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().loadPublishedLevelsCallback(levels, finalLevelCount, playerId));
	}

	public void loadPublishedLevelsPageAsync(LevelSortBy sortBy, boolean reverse, int offset, int limit, final UUID playerId) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPublishedLevelsPage(sortBy, reverse, offset, limit, playerId));
	}

	public synchronized Set<MakerDisplayableLevel> loadPublishedLevelsBySerials(Set<Long> serials) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		checkNotNull(serials);
		Set<MakerDisplayableLevel> levels = new HashSet<MakerDisplayableLevel>();
		if (serials.isEmpty()) {
			return levels;
		}
		String query = String.format(LOAD_LEVEL_WITH_DATA_QUERY_BASE,
				SELECT_ALL_FROM_LEVELS,
				"WHERE `levels`.`level_serial` IN (%s) AND `levels`.`date_published` IS NOT NULL", "");
		try (PreparedStatement loadLevelById = getConnection().prepareStatement(String.format(query, StringUtils.chop(StringUtils.join(serials, ","))))) {
			ResultSet resultSet = loadLevelById.executeQuery();
			while (resultSet.next()) {
				MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
				loadLevelFromResult(level, resultSet, true);
				loadLevelRecords(level);
				levels.add(level);
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadPublishedLevelsBySerials - error while loading level - %s", e.getMessage()));
			e.printStackTrace();
		}
		return levels;
	}

	private int loadPublishedLevelsCount(boolean callback) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		int levelCount = 0;
		try (PreparedStatement levelPageQuery = getConnection().prepareStatement(String.format("SELECT count(1) FROM `mcmaker`.`levels` WHERE `date_published` IS NOT NULL"))) {
			ResultSet resultSet = levelPageQuery.executeQuery();
			if (resultSet.next()) {
				levelCount = resultSet.getInt(1);
				if (callback) {
					final int finalLevelCount = levelCount;
					Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().loadPublishedLevelsCountCallback(finalLevelCount));
				}
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

	public void loadPublishedLevelsCountAsync() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPublishedLevelsCount(true));
	}

	private Rank loadRank(ResultSet result) {
		Rank rank = Rank.GUEST;
		try {
			rank = Rank.valueOf(result.getString("author_rank"));
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadAuthorRank - unable to load level author rank - %s", e.getMessage()));
		}
		return rank;
	}

	private MakerRelativeLocationData loadRelativeLocationById(byte[] locationId) throws SQLException, DataException {
		ByteBuffer locationIdBytes = ByteBuffer.wrap(locationId);
		UUID locationUUID = new UUID(locationIdBytes.getLong(), locationIdBytes.getLong());
		try (PreparedStatement testQuery = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`locations` where `location_id` = ?"))) {
			testQuery.setBytes(1, locationId);
			ResultSet resultSet = testQuery.executeQuery();
			if (!resultSet.next()) {
				throw new DataException(String.format("Unable to find maker location with id: [%s]", locationUUID));
			}
			MakerRelativeLocationData location = new MakerRelativeLocationData(resultSet.getDouble("location_x"), resultSet.getDouble("location_y"), resultSet.getDouble("location_z"), resultSet.getFloat("location_yaw"), resultSet.getFloat("location_pitch"));
			location.setLocationId(locationUUID);
			return location;
		}
	}

	private synchronized void loadUnpublishedLevelsByAuthorId(PlayerLevelsMenu levelBrowserMenu) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		checkNotNull(levelBrowserMenu);
		String authorIdString = levelBrowserMenu.getViewerId().toString().replace("-", "");
		List<MakerDisplayableLevel> levels = new ArrayList<>();
		// TODO: hardcoded limit
		try (PreparedStatement levelsByAuthorQuery = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`levels` WHERE `author_id` = UNHEX(?) AND `date_published` IS NULL ORDER BY level_serial DESC LIMIT 5"))) {
			levelsByAuthorQuery.setString(1, authorIdString);
			ResultSet resultSet = levelsByAuthorQuery.executeQuery();
			while (resultSet.next()) {
				MakerDisplayableLevel level = new MakerDisplayableLevel(plugin);
				loadLevelFromResult(level, resultSet, false);
				loadLevelRecords(level);
				levels.add(level);
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		}
		if (levels.size() > 0) {
			Bukkit.getScheduler().runTask(plugin, () -> levelBrowserMenu.updateOwnedLevels(levels));
		}
	}

	public void loadUnpublishedLevelsByAuthorIdAsync(PlayerLevelsMenu playerLevelsMenu) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadUnpublishedLevelsByAuthorId(playerLevelsMenu));
	}

	private synchronized void publishLevel(MakerPlayableLevel level) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
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
					throw new DataException(String.format("Unable to find level published date for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
				}
				level.setDatePublished(resultSet.getDate("date_published"));
			}
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
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
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		String levelId = level.getLevelId().toString().replace("-", "");
		try {
			level.tryStatusTransition(LevelStatus.SAVE_READY, LevelStatus.SAVING);
			if (level.getLevelSerial() > 0) {
				String endLocationUUID = null;
				if (level.getRelativeEndLocation() != null) {
					endLocationUUID = insertOrUpdateRelativeLocation(level.getRelativeEndLocation()).toString().replace("-", "");
				}
				// after every save the author needs to clear the level again in order to publish it
				String updateBase = "UPDATE `mcmaker`.`levels` SET `author_cleared` = 0%s WHERE `level_id` = UNHEX(?)";
				String updateStatement = null;
				if (endLocationUUID != null) {
					updateStatement = String.format(updateBase, ", `end_location_id` = UNHEX(?)");
				} else {
					updateStatement = String.format(updateBase, "");
				}
				int changed = 0;
				try (PreparedStatement updateLevelSt = getConnection().prepareStatement(updateStatement)) {
					updateLevelSt.setString(1, level.getLevelName());
					if (endLocationUUID != null) {
						updateLevelSt.setString(1, endLocationUUID);
						updateLevelSt.setString(2, levelId);
					} else {
						updateLevelSt.setString(1, levelId);
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

	public boolean testConnection() {
		try {
			getConnection();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private synchronized void updateLevelAuthorClearTime(UUID levelId, long clearTimeMillis) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		try {
			String levelIdString = levelId.toString().replace("-", "");
			try (PreparedStatement updateLevelSt = getConnection().prepareStatement("UPDATE `mcmaker`.`levels` SET `author_cleared` = LEAST(`author_cleared`, ?) WHERE `level_id` = UNHEX(?)")) {
				updateLevelSt.setLong(1, clearTimeMillis);
				updateLevelSt.setString(2, levelIdString);
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

	@Deprecated
	// use rabbit instead
	public synchronized void updatePlayersCount() {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		final int count = plugin.getController().getPlayerCount();
		if (count == this.lastCount) {
			return;
		}
		this.lastCount = count;
		try (PreparedStatement stmt = getConnection().prepareStatement("UPDATE `mcmaker`.`servers` SET `players` = ? WHERE `serverid` = ?")) {
			stmt.setInt(1, count);
			stmt.setString(2, String.valueOf(plugin.getServerId()));
			stmt.executeUpdate();
		} catch (final SQLException e) {
			Bukkit.getLogger().severe("Error while updating players: ");
			e.printStackTrace();
		}
	}

}
