package com.minecade.minecraftmaker.data;

import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import com.minecade.core.data.DatabaseException;
import com.minecade.core.data.MinecadeAccountData;
import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.io.ClipboardFormat;
import com.minecade.minecraftmaker.schematic.io.ClipboardWriter;

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

	public void loadLevels(LevelSortBy sortBy, int offset, int limit) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should NOT be called from the main thread");
		}
		Map<UUID, MakerLevel> levels = new HashMap<>(); 
		try (PreparedStatement testQuery = getConnection().prepareStatement(String.format("SELECT * FROM `mcmaker`.`levels` order by ? desc limit ?,?", networkSchema))) {
			testQuery.setString(1, sortBy.name());
			testQuery.setInt(2, offset);
			testQuery.setInt(3, limit);
			ResultSet resultSet = testQuery.executeQuery();
			while (resultSet.next()) {
				ByteBuffer levelIdBytes = ByteBuffer.wrap(resultSet.getBytes("level_id"));
				Bukkit.getLogger().info(String.format("[DEVELOPMENT] | MakerDatabaseAdapter.loadLevel - level id buffer remaining bytes [%s]", levelIdBytes.remaining()));
				ByteBuffer authorIdBytes = ByteBuffer.wrap(resultSet.getBytes("author_id"));
				MakerLevel level = new MakerLevel(plugin, new UUID(levelIdBytes.getLong(), levelIdBytes.getLong()), new UUID(authorIdBytes.getLong(), authorIdBytes.getLong()), resultSet.getString("author_name"));
				level.setLevelSerial(resultSet.getLong("level_serial"));
				levels.put(level.getLevelId(), level);
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.loadLevel - error while loading levels - %s", e.getMessage()));
			e.printStackTrace();
		} 
		plugin.getController().addServerBrowserLevels(levels, sortBy);
	}

	public void loadLevelsAsync(LevelSortBy sortBy, int offset, int limit) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadLevels(sortBy, offset, limit));
	}

	public synchronized void saveLevel(MakerLevel level) {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
		if (!level.tryStatusTransition(LevelStatus.SAVE_READY, LevelStatus.SAVING)) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.saveLevel - Level with id: [%s] and slot [%s] is not ready for saving!", level.getLevelId(), level.getChunkZ()));
			level.disable();
			return;
		}
		// TODO: enhance this to try the update first and then insert if zero rows are updated and try to remove nesting
		String levelId = level.getLevelId().toString().replace("-", "");
		String authorId = level.getAuthorId().toString().replace("-", "");
		Blob data = null;
		try (PreparedStatement byBinaryUUID = getConnection().prepareStatement(String.format("SELECT level_id FROM mcmaker.levels WHERE level_id = UNHEX(?)"))) {
			byBinaryUUID.setString(1, levelId);
			ResultSet byBinaryUUIDResult = byBinaryUUID.executeQuery();
			if (!byBinaryUUIDResult.first()) {
				// level does not exist yet, create it.
				if (Bukkit.getLogger().isLoggable(Level.INFO)) {
					Bukkit.getLogger().info(String.format("MakerDatabaseAdapter.saveLevel - inserting data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
				}
				try (PreparedStatement insertLevelSt = getConnection().prepareStatement(String.format("INSERT INTO mcmaker.levels(level_id, author_id, level_name, author_name) VALUES (UNHEX(?), UNHEX(?), ?, ?)"))) {
					insertLevelSt.setString(1, levelId);
					insertLevelSt.setString(2, authorId);
					insertLevelSt.setString(3, level.getLevelName());
					insertLevelSt.setString(4, level.getAuthorName());
					insertLevelSt.executeUpdate();
				}
				if (Bukkit.getLogger().isLoggable(Level.INFO)) {
					Bukkit.getLogger().info(String.format("MakerDatabaseAdapter.saveLevel - inserted data for level: [%s<%s>]", level.getLevelName(), level.getLevelId()));
				}
			} else {
				// update level name only (FIXME: test again after modification)
				try (PreparedStatement updateLevelSt = getConnection().prepareStatement("UPDATE `mcmaker`.`levels` SET `level_name` =  ? WHERE `level_id` = UNHEX(?)")) {
					updateLevelSt.setString(1, level.getLevelName());
					updateLevelSt.setString(2, levelId);
					updateLevelSt.executeUpdate();
				}
			}
			if (level.getClipboard() != null) {
				data = getConnection().createBlob();
				try (BufferedOutputStream bos = new BufferedOutputStream(data.setBinaryStream(1));
				        ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(bos);
				        PreparedStatement insertLevelBinaryData = getConnection().prepareStatement("INSERT INTO `mcmaker`.`schematics` (level_id, data) VALUES (UNHEX(?), ?)")) {
					writer.write(level.getClipboard(), plugin.getController().getMainWorldData());
					insertLevelBinaryData.setString(1, levelId);
					insertLevelBinaryData.setBlob(2, data);
					insertLevelBinaryData.executeUpdate();
				}
			}
			level.tryStatusTransition(LevelStatus.SAVING, LevelStatus.SAVED);
			if (Bukkit.getLogger().isLoggable(Level.INFO)) {
				Bukkit.getLogger().info(String.format("MakerDatabaseAdapter.saveLevel - level saved without errors: [%s<%s>]", level.getLevelName(), level.getLevelId()));
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerDatabaseAdapter.saveLevel - error while saving Level with id: [%s] and slot [%s] - %s", level.getLevelId(), level.getChunkZ(), e.getMessage()));
			e.printStackTrace();
			level.disable();
		} finally {
			if (data!= null) {
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

}
