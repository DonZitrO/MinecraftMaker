package com.minecade.minecraftmaker.level;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import com.minecade.minecraftmaker.data.MakerRelativeLocationData;
import com.minecade.minecraftmaker.function.operation.LevelClipboardCopyOperation;
import com.minecade.minecraftmaker.function.operation.LevelClipboardPasteOperation;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.DataException;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.WorldData;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerLevel implements Tickable {

	private static final MakerRelativeLocationData RELATIVE_START_LOCATION = new MakerRelativeLocationData(2.5, 65, 6.5, -90f, 0);

	private final MinecraftMakerPlugin plugin;

	private UUID levelId;
	private long levelSerial;
	private String levelName;
	private UUID authorId;
	private String authorName;
	private short chunkZ;
	private LevelStatus status;

	//private Region region;
	private Clipboard clipboard;

	private long clearedByAuthorMillis;
	private Date datePublished;

	private long favs;
	private long likes;
	private long dislikes;

	private long startTime;

	private UUID currentPlayerId;

	private MakerRelativeLocationData relativeEndLocation;

	private long currentTick;

	public MakerLevel(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
		this.status = LevelStatus.BLANK;
	}

	public MakerLevel(MinecraftMakerPlugin plugin, short chunkZ) {
		this.plugin = plugin;
		this.chunkZ = chunkZ;
		this.status = LevelStatus.BLANK;
	}

	public void checkLevelEnd(Location location) {
		if (relativeEndLocation == null) {
			throw new IllegalStateException("MakerLevel.endlocation cannot be null at this point");
		}
		Location endLocation = getEndLocation();
		if (location.getBlockX() == endLocation.getBlockX() && location.getBlockZ() == endLocation.getBlockZ() && location.getBlockY() >= endLocation.getBlockY()) {
			clearLevel();
		}
	}

	private void clearLevel() {
		long clearTimeMillis = System.currentTimeMillis() - startTime;
		MakerPlayer mPlayer = plugin.getController().getPlayer(currentPlayerId);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerLevel.clearLevel - player is offline, unloading level..."));
			status = LevelStatus.UNLOAD_READY;
			return;
		}
		try {
			tryStatusTransition(LevelStatus.PLAYING, LevelStatus.CLEARED);
		} catch (DataException e) {
			Bukkit.getLogger().warning(e.getMessage());
			e.printStackTrace();
			mPlayer.sendMessage(plugin, "level.clear.error.status");
			return;
		}
		if (authorId.equals(mPlayer.getUniqueId())) {
			clearedByAuthorMillis = clearTimeMillis;
			status = LevelStatus.UPDATE_AUTHOR_CLEAR_READY;
			plugin.getDatabaseAdapter().updateLevelAuthorClearTimeAsync(this);
			// waitForBusyLevel(mPlayer, false);
			mPlayer.sendMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
		} else {
			plugin.getDatabaseAdapter().updateLevelClearAsync(getLevelId(), mPlayer.getUniqueId(), clearTimeMillis);
			mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.clear.title"), plugin.getMessage("level.clear.subtitle", formatMillis(clearTimeMillis)));
			mPlayer.sendMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
			mPlayer.sendMessage(plugin, "level.clear.options");
		}
	}

	public synchronized void clipboardError() {
		// TODO: add some meat (general level error with message and possible stacktrace
		status = LevelStatus.CLIPBOARD_ERROR;
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

	private String formatMillis(long millis) {
		return String.format("%02d:%02d:%02d,%03d", 
			    TimeUnit.MILLISECONDS.toHours(millis),
			    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
			    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
			    TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public String getAuthorName() {
		return authorName;
	}

	public short getChunkZ() {
		return chunkZ;
	}

	public long getClearedByAuthorMillis() {
		return clearedByAuthorMillis;
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

	public Location getEndLocation() {
		return relativeEndLocation.toLocation(chunkZ, getWorld());
	}

	public long getFavs() {
		return favs;
	}

	public UUID getLevelId() {
		return levelId;
	}

	public String getLevelName() {
		return levelName != null ? levelName : levelId.toString().replace("-", "");
	}

	public long getLevelSerial() {
		return levelSerial;
	}

	public long getLikes() {
		return likes;
	}

	// TODO: candidate for removal, use full level object when possible
	public Extent getMakerExtent() {
		return plugin.getController().getMakerExtent();
	}

	public MakerRelativeLocationData getRelativeEndLocation() {
		return relativeEndLocation;
	}

	public Location getStartLocation() {
		return RELATIVE_START_LOCATION.toLocation(chunkZ, getWorld());
	}

	public LevelStatus getStatus() {
		return status;
	}

	private World getWorld() {
		return plugin.getController().getMainWorld();
	}

	public WorldData getWorldData() {
		return plugin.getController().getMainWorldData();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((levelId == null) ? 0 : levelId.hashCode());
		return result;
	}

	public boolean isBusy() {
		switch (getStatus()) {
		case EDITING:
		case PLAYING:
		case CLEARED:
			return false;
		default:
			return true;
		}
	}

	public boolean isClearedByAuthor() {
		return clearedByAuthorMillis > 0;
	}

	@Override
	public boolean isEnabled() {
		return !LevelStatus.DISABLED.equals(getStatus());
	}

	public boolean isPlayableByEditor() {
		return relativeEndLocation != null;
	}

	private boolean isPublished() {
		return datePublished != null;
	}

	private boolean playerIsInThisLevel(MakerPlayer mPlayer) {
		return this.equals(mPlayer.getCurrentLevel());
	}

	public void publishLevel() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerLevel.publishLevel - editor is offline, unloading level..."));
			status = LevelStatus.UNLOAD_READY;
			return;
		}
		if (!isClearedByAuthor()) {
			mPlayer.sendActionMessage(plugin, "level.publish.error.not-cleared");
			return;
		}
		if (getLevelName().equalsIgnoreCase(levelId.toString().replace("-",""))) {
			mPlayer.sendActionMessage(plugin, "level.publish.error.rename");
			return;
		}
		status = LevelStatus.PUBLISH_READY;
		plugin.getDatabaseAdapter().publishLevelAsync(this);
		plugin.getController().addPlayerToMainLobby(mPlayer);
	}

	public void rename(String newName) {
		try {
			tryStatusTransition(LevelStatus.EDITING, LevelStatus.RENAME_READY);
		} catch (DataException e) {
			this.status = LevelStatus.UNLOAD_READY;
			Bukkit.getLogger().warning(e.getMessage());
			e.printStackTrace();
			return;
		}
		plugin.getDatabaseAdapter().renameLevelAsync(this, newName);
	}

	public synchronized void restartPlaying() {
		// FIXME: enhance this to allow the player to wait on the level start while the level reloads instead of going back to lobby
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			mPlayer.sendActionMessage(plugin, "level.loading");
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		status = LevelStatus.CLIPBOARD_PASTE_READY;
		plugin.getLevelOperatorTask().offer(new LevelClipboardPasteOperation(this));
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

	public synchronized void saveLevel() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			// FIXME: experimental
			waitForBusyLevel(mPlayer, true);
			//plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		// FIXME: re-think status name
		this.status = LevelStatus.EDITED;
	}

	public void setAuthorId(UUID authorId) {
		this.authorId = authorId;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public void setClipboard(Clipboard clipboard) {
		this.clipboard = clipboard;
	}

	public void setCurrentPlayerId(UUID currentPlayerId) {
		this.currentPlayerId = currentPlayerId;
	}

	public void setDatePublished(Date datePublished) {
		this.datePublished = datePublished;
	}

	public void setLevelId(UUID levelId) {
		this.levelId = levelId;
	}

	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	public void setLevelSerial(long levelSerial) {
		this.levelSerial = levelSerial;
	}

	public void setRelativeEndLocation(MakerRelativeLocationData relativeEndLocation) {
		this.relativeEndLocation = relativeEndLocation;
	}

	public boolean setupEndLocation(Location location) {
		long startNanos = 0;
		if (plugin.isDebugMode()) {
			startNanos = System.nanoTime();
		}
		if (relativeEndLocation != null) {
			Block formerBeacon = getEndLocation().getBlock();
			formerBeacon.setType(Material.AIR);
			formerBeacon.getState().update();
			updateBeaconBase(formerBeacon.getRelative(BlockFace.DOWN), Material.AIR);
		}
		updateBeaconBase(location.getBlock().getRelative(BlockFace.DOWN), Material.IRON_BLOCK);
		// reuse object on DB by inheriting the UUID when possible
		relativeEndLocation = new MakerRelativeLocationData(location, relativeEndLocation != null ? relativeEndLocation.getLocationId() : null);
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.setupEndLocation took: [%s] nanoseconds", System.nanoTime() - startNanos));
		}
		return true;
	}

	public void startEditing() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerLevel.startEditing - editor is offline, unloading level..."));
			status = LevelStatus.UNLOAD_READY;
			return;
		}
		if (!mPlayer.isInLobby() && !playerIsInThisLevel(mPlayer)) {
			Bukkit.getLogger().warning(String.format("MakerLevel.startEditing - editor is busy on another level, unloading level..."));
			status = LevelStatus.UNLOAD_READY;
			return;
		}
		try {
			tryStatusTransition(LevelStatus.EDIT_READY, LevelStatus.EDITING);
		} catch (DataException e) {
			Bukkit.getLogger().warning(e.getMessage());
			e.printStackTrace();
			mPlayer.sendMessage(plugin, "level.edit.error.status");
			status = LevelStatus.UNLOAD_READY;
			return;
		}
		if (mPlayer.teleport(getStartLocation(), TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.CREATIVE);
			mPlayer.setAllowFlight(true);
			mPlayer.setFlying(true);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(0, new ItemStack(Material.BEACON));
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			if (!playerIsInThisLevel(mPlayer)) {
				mPlayer.setCurrentLevel(this);
				mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.create.start.title"), plugin.getMessage("level.create.start.subtitle"));
				mPlayer.sendMessage(plugin, "level.create.creative");
				mPlayer.sendMessage(plugin, "level.create.beacon");
				mPlayer.sendMessage(plugin, "level.create.menu");
			} else {
				mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.create.continue.title"), plugin.getMessage("level.create.continue.subtitle"));
			}
		} else {
			// TODO: maybe get rid of this or send player back to lobby or kick it
			Bukkit.getLogger().severe(String.format("MakerLevel.startEditing - unable to send editor to level..."));
			status = LevelStatus.UNLOAD_READY;
			return;
		}
	}

	public void startPlaying(MakerPlayer mPlayer) {
		if (!mPlayer.isInLobby() && !playerIsInThisLevel(mPlayer)) {
			Bukkit.getLogger().warning(String.format("MakerLevel.startPlaying - player is busy on another level, unloading level..."));
			status = LevelStatus.UNLOAD_READY;
			return;
		}
		try {
			tryStatusTransition(LevelStatus.PLAY_READY, LevelStatus.PLAYING);
		} catch (DataException e) {
			Bukkit.getLogger().warning(e.getMessage());
			e.printStackTrace();
			mPlayer.sendMessage(plugin, "level.play.error.status");
			return;
		}
		if (mPlayer.teleport(getStartLocation(), TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.ADVENTURE);
			mPlayer.setFlying(false);
			mPlayer.setAllowFlight(false);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			mPlayer.setCurrentLevel(this);
			mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.play.start.title"), plugin.getMessage("level.play.start.subtitle"));
			mPlayer.sendActionMessage(plugin, "level.play.start");
			startTime = System.currentTimeMillis();
		} else {
			// TODO: maybe get rid of this or send player back to lobby or kick it
			Bukkit.getLogger().severe(String.format("MakerLevel.startPlaying - unable to send editor to level..."));
			status = LevelStatus.UNLOAD_READY;
			return;
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

	private void tickClipboardCopied() {
		this.status = LevelStatus.SAVE_READY;
		plugin.saveLevelAsync(this);
	}

	private void tickClipboardLoaded() {
		status = LevelStatus.CLIPBOARD_PASTE_READY;
		plugin.getLevelOperatorTask().offer(new LevelClipboardPasteOperation(this));
	}

	private void tickClipboardPasted() {
		// TODO: find a better way to control these scenarios
		if (currentPlayerId != null) {
			status = LevelStatus.PLAY_READY;
		} else {
			status = LevelStatus.EDIT_READY;
		}
	}

	private void tickEdited() {
		this.status = LevelStatus.CLIPBOARD_COPY_READY;
		plugin.getLevelOperatorTask().offer(new LevelClipboardCopyOperation(plugin, this));
	}

	private void tickEditReady() {
		startEditing();
	}

	private void tickPlayReady() {
		startPlaying(currentPlayerId);
	}

	private void tickPublished() {
		status = LevelStatus.UNLOAD_READY;
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			mPlayer.updatePublishedLevelOnLevelBrowser(plugin, this);
			mPlayer.sendActionMessage(plugin, "level.publish.success");
		}
	}

	private void tickRenamed() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			this.status = LevelStatus.UNLOAD_READY;
			return;
		}
		this.status = LevelStatus.EDIT_READY;
		mPlayer.sendActionMessage(plugin, "level.rename.success");
	}

	private void tickRenameError() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			this.status = LevelStatus.UNLOAD_READY;
			return;
		}
		this.status = LevelStatus.EDIT_READY;
		mPlayer.sendActionMessage(plugin, "level.rename.error.name");
	}

	private void tickSaved() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			this.status = LevelStatus.UNLOAD_READY;
			return;
		}
		mPlayer.updateSavedLevelOnLevelBrowser(plugin, this);
		mPlayer.sendActionMessage(plugin, "level.save.success");
		if (playerIsInThisLevel(mPlayer)) {
			if (currentPlayerId != null) {
				// TODO: find a better way for this status transition (level editor save-play)
				this.status = LevelStatus.PLAY_READY;
				return;
			} else {
				this.status = LevelStatus.EDIT_READY;
				return;
			}
		}
		this.status = LevelStatus.UNLOAD_READY;
		return;
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
		case PLAY_READY:
			tickPlayReady();
			break;
		case CLIPBOARD_PASTED:
			tickClipboardPasted();
			break;
		case CLIPBOARD_LOADED:
			tickClipboardLoaded();
			break;
		case UPDATED_AUTHOR_CLEAR:
			tickUpdatedAuthorClear();
			break;
		case RENAME_ERROR:
			tickRenameError();
			break;
		case RENAMED:
			tickRenamed();
			break;
		case PUBLISHED:
			tickPublished();
			break;
		default:
			break;
		}
	}

	private void tickUnloadReady() {
		// TODO: maybe remove author or player
		plugin.getController().unloadLevel(this);
	}

	private void tickUpdatedAuthorClear() {
		// FIXME: refactor this logic for a better state transition
		if (!isPublished()) {
			status = LevelStatus.EDIT_READY;
		} else {
			status = LevelStatus.CLEARED;
		}
	}

	public synchronized void tryStatusTransition(LevelStatus from, LevelStatus to) throws DataException {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.tryStatusTransition - current status: [%s] - requested from: [%s] - requested to: [%s]", getStatus(), from, to));
		}
		if (!from.equals(getStatus())) {
			throw new DataException(String.format("MakerLevel.tryStatusTransition - The current level: [%s<%s>] is not on the requested status: [%s] in order to transition to status: [%s] - current status: [%s]",  getLevelName(), getLevelId(), from, to, getStatus()));
		}
		this.status = to;
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

	public void waitForBusyLevel(MakerPlayer mPlayer, boolean showMessage) {
		mPlayer.setGameMode(GameMode.SPECTATOR);
		mPlayer.teleportOnNextTick(getStartLocation());
		if (showMessage) {
			mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.busy.title"), plugin.getMessage("level.busy.subtitle"));
		}
	}

}
