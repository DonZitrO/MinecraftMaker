package com.minecade.minecraftmaker.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import com.minecade.core.data.DatabaseException;
import com.minecade.core.data.MinecadeAccountData;
import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.PlayerLevelsMenu;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.DataException;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.io.ClipboardFormat;
import com.minecade.minecraftmaker.schematic.io.ClipboardReader;
import com.minecade.minecraftmaker.schematic.io.ClipboardWriter;
import com.minecade.minecraftmaker.util.LevelUtils;

public class MakerDatabaseAdapter {

	private final MinecraftMakerPlugin plugin;

	private final String jdbcUrl;
	private final String dbUsername;
	private final String dbpassword;

	private final String networkSchema;
	private final String coinsColumn;

	private Connection connection;

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

	}

	private synchronized void loadLevelBySerialFull(MakerLevel level) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		try (PreparedStatement testQuery = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`levels` where `level_serial` = ?"))) {
			testQuery.setLong(1, level.getLevelSerial());
			ResultSet resultSet = testQuery.executeQuery();
			if (resultSet.next()) {
				loadLevelFromResult(level, resultSet, false);
				loadLevelClipboard(level);
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

	public void loadLevelBySerialFullAsync(MakerLevel level) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadLevelBySerialFull(level));
	}

	private void loadLevelClipboard(MakerLevel level) {
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

	private void loadLevelClipboardFromResult(MakerLevel level, ResultSet resultSet) throws DataException {
		Blob data = null;
		try {
			data = resultSet.getBlob("data");
			try (BufferedInputStream is = new BufferedInputStream(data.getBinaryStream()); ClipboardReader reader = ClipboardFormat.SCHEMATIC.getReader(is)) {
				// note: this particular implementation doesn't need world data for anything. TODO: find other places where WorldData is not needed.
				Clipboard clipboard = reader.read(null);
				if (clipboard.getRegion().getHeight() > 128) {
					clipboard.setOrigin(LevelUtils.getLevelOrigin(level.getChunkZ()).add(0,-48,0));
				} else {
					clipboard.setOrigin(LevelUtils.getLevelOrigin(level.getChunkZ()));
				}
				Bukkit.getLogger().info(String.format("height: [%s]", clipboard.getRegion().getHeight()));
				Bukkit.getLogger().info(String.format("origin: [%s]", clipboard.getOrigin().getBlockY()));
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

	private void loadLevelFromResult(MakerLevel level, ResultSet result, boolean published) throws SQLException, DataException {
		ByteBuffer levelIdBytes = ByteBuffer.wrap(result.getBytes("level_id"));
		ByteBuffer authorIdBytes = ByteBuffer.wrap(result.getBytes("author_id"));
		level.setLevelId(new UUID(levelIdBytes.getLong(), levelIdBytes.getLong()));
		level.setLevelSerial(result.getLong("level_serial"));
		level.setLevelName(result.getString("level_name"));
		level.setAuthorId(new UUID(authorIdBytes.getLong(), authorIdBytes.getLong()));
		level.setAuthorName(result.getString("author_name"));
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

	private synchronized void loadPublishedLevels(LevelSortBy sortBy, int offset, int limit) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadPublishedLevels - sortBy: [%s] - offset: [%s] - limit: [%s]", sortBy.name(), offset, limit));
		}
		checkNotNull(sortBy);
		List<MakerLevel> levels = new ArrayList<>(limit);
		try (PreparedStatement levelPageQuery = getConnection().prepareStatement(String.format(
				"SELECT `levels`.*, SUM(CASE WHEN `dislike` = 0 THEN 1 ELSE 0 END) as likes, SUM(CASE WHEN `dislike` = 1 THEN 1 ELSE 0 END) as dislikes " +
				"FROM `mcmaker`.`levels` LEFT JOIN `mcmaker`.`level_likes` on `levels`.`level_id` = `level_likes`.`level_id` " +
				"WHERE `date_published` IS NOT NULL GROUP BY `level_id` ORDER BY ? DESC LIMIT ?,?"))) {
			levelPageQuery.setString(1, sortBy.name());
			levelPageQuery.setInt(2, offset);
			levelPageQuery.setInt(3, limit);
			ResultSet resultSet = levelPageQuery.executeQuery();
			while (resultSet.next()) {
				MakerLevel level = new MakerLevel(plugin);
				loadLevelFromResult(level, resultSet, true);
				levels.add(level);
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.loadPublishedLevels - loaded levels: [%s]", levels.size()));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		}
		Bukkit.getScheduler().runTask(plugin, () -> LevelBrowserMenu.addOrUpdateLevels(plugin, levels));
	}

	public void loadPublishedLevelsAsync(LevelSortBy sortBy, int offset, int limit) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPublishedLevels(sortBy, offset, limit));
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
		List<MakerLevel> levels = new ArrayList<>();
		// TODO: hardcoded limit
		try (PreparedStatement levelsByAuthorQuery = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`levels` WHERE `author_id` = UNHEX(?) AND `date_published` IS NULL ORDER BY level_serial DESC LIMIT 5"))) {
			levelsByAuthorQuery.setString(1, authorIdString);
			ResultSet resultSet = levelsByAuthorQuery.executeQuery();
			while (resultSet.next()) {
				MakerLevel level = new MakerLevel(plugin);
				loadLevelFromResult(level, resultSet, false);
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

	private synchronized void publishLevel(MakerLevel level) {
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
			if (Bukkit.getLogger().isLoggable(Level.INFO)) {
				Bukkit.getLogger().info(String.format("MakerDatabaseAdapter.publishLevel - level updated without errors: [%s<%s>]", level.getLevelName(), level.getLevelId()));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.publishLevel - error while updating level: [%s<%s>] - ", level.getLevelName(), level.getLevelId(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void publishLevelAsync(MakerLevel level) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> publishLevel(level));
	}

	private synchronized void renameLevel(MakerLevel level, String newName) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		try {
			level.tryStatusTransition(LevelStatus.RENAME_READY, LevelStatus.RENAMING);
			String levelId = level.getLevelId().toString().replace("-", "");
			try (PreparedStatement updateLevelSt = getConnection().prepareStatement("UPDATE `mcmaker`.`levels` SET `level_name` = ? WHERE `level_id` = UNHEX(?)")) {
				updateLevelSt.setString(1, newName);
				updateLevelSt.setString(2, levelId);
				updateLevelSt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				level.tryStatusTransition(LevelStatus.RENAMING, LevelStatus.RENAME_ERROR);
				return;
			}
			level.setLevelName(newName);
			level.tryStatusTransition(LevelStatus.RENAMING, LevelStatus.RENAMED);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.renameLevel - error while renaming level: [%s<%s>] - %s", level.getLevelName(), level.getLevelId(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void renameLevelAsync(MakerLevel level, String newName) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> renameLevel(level, newName));
	}

	public synchronized void saveLevel(MakerLevel level) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		Blob data = null;
		try {
			level.tryStatusTransition(LevelStatus.SAVE_READY, LevelStatus.SAVING);
			String levelId = level.getLevelId().toString().replace("-", "");
			String authorId = level.getAuthorId().toString().replace("-", "");
			String endLocationUUID = null;
			if (level.getRelativeEndLocation() != null) {
				endLocationUUID = insertOrUpdateRelativeLocation(level.getRelativeEndLocation()).toString().replace("-", "");
			}
			if (level.getLevelSerial() > 0) {
				// after every save the author needs to clear the level again in order to publish it
				String updateBase = "UPDATE `mcmaker`.`levels` SET `author_cleared` = 0%s WHERE `level_id` = UNHEX(?)";
				String updateStatement = null;
				if (endLocationUUID != null) {
					updateStatement = String.format(updateBase, ", `end_location_id` = UNHEX(?)");
				} else {
					updateStatement = String.format(updateBase, "");
				}
				try (PreparedStatement updateLevelSt = getConnection().prepareStatement(updateStatement)) {
					updateLevelSt.setString(1, level.getLevelName());
					if (endLocationUUID != null) {
						updateLevelSt.setString(1, endLocationUUID);
						updateLevelSt.setString(2, levelId);
					} else {
						updateLevelSt.setString(1, levelId);
					}
					Bukkit.getLogger().severe(String.format("Affected rows: [%s]", updateLevelSt.executeUpdate()));
				}
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.saveLevel - updated shallow data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
				}
			} else {
				// level does not exist yet, create it
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.saveLevel - inserting data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
				}
				String insertBase = "INSERT INTO `mcmaker`.`levels` (level_id, author_id, level_name, author_name%s) VALUES (UNHEX(?), UNHEX(?), ?, ?%s)";
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
					if (endLocationUUID != null) {
						insertLevelSt.setString(5, endLocationUUID);
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
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.saveLevel - inserted shallow data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
				}
			}
			// insert clipboard
			if (level.getClipboard() != null) {
				data = getConnection().createBlob();
				try (PreparedStatement insertLevelBinaryData = getConnection().prepareStatement("INSERT INTO `mcmaker`.`schematics` (level_id, data) VALUES (UNHEX(?), ?)")) {
					try (BufferedOutputStream bos = new BufferedOutputStream(data.setBinaryStream(1)); ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(bos);) {
						writer.write(level.getClipboard(), null);
					}
					insertLevelBinaryData.setString(1, levelId);
					insertLevelBinaryData.setBlob(2, data);
					insertLevelBinaryData.executeUpdate();
				}
			}
			level.tryStatusTransition(LevelStatus.SAVING, LevelStatus.SAVED);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.saveLevel - level saved without errors: [%s<%s>]", level.getLevelName(), level.getLevelId()));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.saveLevel - error while saving Level with id: [%s] and slot [%s] - %s", level.getLevelId(), level.getChunkZ(), e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
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

	public void updateLevelClearAsync(UUID levelId, UUID uniqueId, long clearTimeMillis) {
		// TODO Auto-generated method stub
	}

}
