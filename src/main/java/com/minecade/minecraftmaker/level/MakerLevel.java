package com.minecade.minecraftmaker.level;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import com.minecade.minecraftmaker.function.operation.LevelClipboardCopyOperation;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerLevel implements Tickable {

	private final MinecraftMakerPlugin plugin;
	private final UUID levelId;
	private final UUID authorId;
	private final String authorName;

	private long levelSerial;
	private short chunkZ;
	private LevelStatus status;

	private String levelName;

	//private Region region;
	private Clipboard clipboard;

	private long favs;
	private long likes;
	private long dislikes;

	private long startTime;
	private boolean cleared;

	private UUID currentPlayerId;

	private Location startLocation;
	private Location endLocation;

	private long currentTick;

	public MakerLevel(MinecraftMakerPlugin plugin, MakerPlayer author, short chunkZ) {
		this.plugin = plugin;
		this.levelId = UUID.randomUUID();
		this.authorId = author.getUniqueId();
		this.authorName = author.getName();
		this.chunkZ = chunkZ;
		this.startLocation = new Vector(2.5, 65, (chunkZ * 16) + 6.5).toLocation(plugin.getController().getMainWorld(), -90f, 0f);
		this.status = LevelStatus.LOADING;
	}

	public MakerLevel(MinecraftMakerPlugin plugin, UUID levelId, UUID authorId, String authorName) {
		this.plugin = plugin;
		this.levelId = levelId;
		this.authorId = authorId;
		this.authorName = authorName;
	}


	@Override
	public synchronized void disable() {
		if (!isEnabled()) {
			return;
		}
		// TODO: disable logic
		status = LevelStatus.DISABLED;
	}

	@Override
	public void enable() {
		throw new UnsupportedOperationException("A level is enabled by default");
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public String getAuthorName() {
		return authorName;
	}

	public long getLevelSerial() {
		return levelSerial;
	}

	public void setLevelSerial(long levelSerial) {
		this.levelSerial = levelSerial;
	}

	public short getChunkZ() {
		return chunkZ;
	}

	public Clipboard getClipboard() {
		return clipboard;
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	public long getDislikes() {
		return dislikes;
	}

	public long getFavs() {
		return favs;
	}

	public UUID getLevelId() {
		return levelId;
	}

	public String getLevelName() {
		return levelName != null ? levelName : levelSerial > 0 ? String.valueOf(levelSerial) : levelId.toString().replace("-", "");
	}

	public long getLikes() {
		return likes;
	}

	public Location getStartLocation() {
		return startLocation.clone();
	}

	public LevelStatus getStatus() {
		return status;
	}

	@Override
	public boolean isEnabled() {
		return !LevelStatus.DISABLED.equals(getStatus());
	}

	public void rename(String newName) {
		// TODO: additional name validation?
		this.levelName = newName;
		// TODO: implement
	}

	public synchronized void exitEditing() {
		// TODO: maybe verify EDITING status
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		this.status = LevelStatus.UNLOAD_READY;
	}

	public void exitPlaying() {
		// TODO: maybe verify PLAYING status
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		this.status = LevelStatus.UNLOAD_READY;
	}

	public synchronized void saveLevel() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		// FIXME: re-think status name
		this.status = LevelStatus.EDITED;
		plugin.getController().addPlayerToMainLobby(mPlayer);
	}

	public synchronized void saveAndPlay() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			return;
		}
		if (!isPlayableByEditor()) {
			mPlayer.sendActionMessage(plugin, "level.edit.error.missing-end");
			return;
		}

		mPlayer.sendActionMessage(plugin, "level.loading");
		this.currentPlayerId = authorId;
		saveLevel();
	}

	public void setClipboard(Clipboard clipboard) {
		this.clipboard = clipboard;
	}

	public void startEdition() {
		// TODO: level must be on the right status to allow edition
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null || !mPlayer.isInLobby()) {
			// TODO: disable/unload level
			return;
		}
		mPlayer.sendActionMessage(plugin, "level.create.start");
		if (mPlayer.teleport(startLocation, TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.CREATIVE);
			mPlayer.setFlying(true);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			mPlayer.setCurrentLevel(this);
			status = LevelStatus.EDITING;
		} else {
			// TODO: disable/unload level
		}
	}

	public void startPlaying(MakerPlayer mPlayer) {
		if (!LevelStatus.PLAY_READY.equals(getStatus())) {
			mPlayer.sendMessage(plugin, "level.play.error.status");
			return;
		}
		mPlayer.sendActionMessage(plugin, "level.play.start");
		if (mPlayer.teleport(startLocation, TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.ADVENTURE);
			mPlayer.setFlying(false);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			mPlayer.setCurrentLevel(this);
			status = LevelStatus.PLAYING;
			startTime = System.currentTimeMillis();
		} else {
			// TODO: error message to player
		}
	}

	public void startPlaying(UUID playerId) {
		MakerPlayer mPlayer = plugin.getController().getPlayer(playerId);
		if (mPlayer == null) {
			return;
		}
		startPlaying(mPlayer);
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		tickStatus();
	}

	private void tickEdited() {
		this.status = LevelStatus.CLIPBOARD_COPY_READY;
		plugin.getLevelOperatorTask().offer(new LevelClipboardCopyOperation(plugin, this));
	}

	private void tickEditReady() {
		// TODO Auto-generated method stub

	}

	private void tickSaved() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer!=null) {
			mPlayer.sendActionMessage(plugin, "level.save.success");
		}
		if (currentPlayerId != null) {
			this.status = LevelStatus.PLAY_READY;
			startPlaying(currentPlayerId);
		} else {
			plugin.getController().unloadLevel(this);
		}
	}

	private synchronized void tickStatus() {
		switch (getStatus()) {
		case EDIT_READY:
			tickEditReady();
			break;
		case EDITED:
			tickEdited();
			break;
		case SAVED:
			tickSaved();
			break;
		case CLIPBOARD_COPIED:
			tickClipboardCopied();
			break;
		case UNLOAD_READY:
			tickUnloadReady();
			break;
		default:
			break;
		}
	}

	private void tickUnloadReady() {
		plugin.getController().unloadLevel(this);
	}

	private void tickClipboardCopied() {
		this.status = LevelStatus.SAVE_READY;
		plugin.saveLevelAsync(this);
	}

	public synchronized boolean tryStatusTransition(LevelStatus from, LevelStatus to) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.tryStatusTransition - current status: [%s] - requested from: [%s] - requested to: [%s]", getStatus(), from, to));
		}
		if (!from.equals(getStatus())) {
			return false;
		}
		this.status = to;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((levelId == null) ? 0 : levelId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MakerLevel other = (MakerLevel) obj;
		if (levelId == null) {
			if (other.levelId != null)
				return false;
		} else if (!levelId.equals(other.levelId))
			return false;
		return true;
	}

	public boolean isCleared() {
		return cleared;
	}

	public boolean isPlayableByEditor() {
		return endLocation != null;
	}

	public boolean setupEndLocation(Location location) {
		long startNanos = 0;
		if (plugin.isDebugMode()) {
			startNanos = System.nanoTime();
		}
		if (endLocation != null) {
			Block formerBeacon = endLocation.getBlock();
			formerBeacon.setType(Material.AIR);
			formerBeacon.getState().update();
			updateBeaconBase(formerBeacon.getRelative(BlockFace.DOWN), Material.AIR);
		}
		updateBeaconBase(location.getBlock().getRelative(BlockFace.DOWN), Material.IRON_BLOCK);
		endLocation = location.clone();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.setupEndLocation took: [%s] nanoseconds", System.nanoTime() - startNanos));
		}
		return true;
	}

	private void updateBeaconBase(Block belowFormerBeacon, Material material) {
		belowFormerBeacon.setType(material);
		belowFormerBeacon.getState().update();
		for (BlockFace around : BlockFace.values()) {
			switch (around) {
			case NORTH_WEST:
			case NORTH:
			case NORTH_EAST:
			case WEST:
			case EAST:
			case SOUTH_WEST:
			case SOUTH:
			case SOUTH_EAST:
				Block aroundBlock = belowFormerBeacon.getRelative(around);
				aroundBlock.setType(material);
				aroundBlock.getState().update();
				break;
			default:
				break;
			}
		}
	}

	public void checkLevelEnd(Location location) {
		if (endLocation == null) {
			throw new IllegalStateException("MakerLevel.endlocation cannot be null at this point");
		}
		if (location.getBlockX() == endLocation.getBlockX() && location.getBlockZ() == endLocation.getBlockZ() && location.getBlockY() >= endLocation.getBlockY()) {
			clearLevel();
		}
	}

	private void clearLevel() {
		status = LevelStatus.CLEARED;
		MakerPlayer mPlayer = plugin.getController().getPlayer(currentPlayerId);
		if (mPlayer != null) {
			// FIXME: temporal test stuff obviously
			mPlayer.sendActionMessage(plugin, "level.clear.success", (System.currentTimeMillis() - startTime) / 1000);
		}
	}

}
