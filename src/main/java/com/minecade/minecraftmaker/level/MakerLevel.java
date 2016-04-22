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

//	public MakerLevel(MinecraftMakerPlugin plugin, MakerPlayer author, short chunkZ) {
//		this.plugin = plugin;
//		this.levelId = UUID.randomUUID();
//		this.authorId = author.getUniqueId();
//		this.authorName = author.getName();
//		this.chunkZ = chunkZ;
//		// FIXME: refactor this
//		this.startLocation = new Vector(2.5, 65, (chunkZ * 16) + 6.5).toLocation(plugin.getController().getMainWorld(), -90f, 0f);
//		this.status = LevelStatus.PREPARING;
//	}

//	public MakerLevel(MinecraftMakerPlugin plugin, UUID levelId, UUID authorId, String authorName) {
//		this.plugin = plugin;
//		this.levelId = levelId;
//		this.authorId = authorId;
//		this.authorName = authorName;
//		this.status = LevelStatus.PREPARING;
//	}

	public MakerLevel(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
		this.status = LevelStatus.PREPARING;
	}

	public MakerLevel(MinecraftMakerPlugin plugin, short chunkZ) {
		this.plugin = plugin;
		this.chunkZ = chunkZ;
		this.status = LevelStatus.PREPARING;
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
			disable();
			return;
		}
		if (!tryStatusTransition(LevelStatus.PLAYING, LevelStatus.CLEARED)) {
			mPlayer.sendMessage(plugin, "level.clear.error.status");
			return;
		}
		if (authorId.equals(mPlayer.getUniqueId())) {
			clearedByAuthorMillis = clearTimeMillis;
			plugin.getDatabaseAdapter().updateLevelAuthorClearTimeAsync(this);
		} else {
			plugin.getDatabaseAdapter().updateLevelClearAsync(getLevelId(), mPlayer.getUniqueId(), clearTimeMillis);
		}
		mPlayer.sendMessage(plugin, "level.clear.options");
		mPlayer.sendActionMessage(plugin, "level.clear.success", formatMillis(clearTimeMillis));

	}

	private String formatMillis(long millis) {
		return String.format("%02d:%02d:%02d,%03d", 
			    TimeUnit.MILLISECONDS.toHours(millis),
			    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
			    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
			    TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
	}

	public synchronized void clipboardError() {
		// TODO: add some meat
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

	public void setLevelId(UUID levelId) {
		this.levelId = levelId;
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public void setAuthorId(UUID authorId) {
		this.authorId = authorId;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public void setRelativeEndLocation(MakerRelativeLocationData relativeEndLocation) {
		this.relativeEndLocation = relativeEndLocation;
	}

	public String getAuthorName() {
		return authorName;
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

	public long getClearedByAuthorMillis() {
		return clearedByAuthorMillis;
	}

	public UUID getLevelId() {
		return levelId;
	}

	public String getLevelName() {
		return levelName != null ? levelName : levelSerial > 0 ? String.valueOf(levelSerial) : levelId.toString().replace("-", "");
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

	public Location getEndLocation() {
		return relativeEndLocation.toLocation(chunkZ, getWorld());
	}

	public Location getStartLocation() {
		return RELATIVE_START_LOCATION.toLocation(chunkZ, getWorld());
	}

	private World getWorld() {
		return plugin.getController().getMainWorld();
	}

	public LevelStatus getStatus() {
		return status;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((levelId == null) ? 0 : levelId.hashCode());
		return result;
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

	public void rename(String newName) {
		// TODO: additional name validation?
		this.levelName = newName;
		// TODO: implement
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
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		// FIXME: re-think status name
		this.status = LevelStatus.EDITED;
	}

	public void setClipboard(Clipboard clipboard) {
		this.clipboard = clipboard;
	}

	public void setCurrentPlayerId(UUID currentPlayerId) {
		this.currentPlayerId = currentPlayerId;
	}

	public void setLevelSerial(long levelSerial) {
		this.levelSerial = levelSerial;
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

	public void startEdition() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null || !mPlayer.isInLobby()) {
			disable();
			return;
		}
		if (!tryStatusTransition(LevelStatus.EDIT_READY, LevelStatus.EDITING)) {
			mPlayer.sendMessage(plugin, "level.edit.error.status");
			return;
		}
		mPlayer.sendMessage(plugin, "level.create.start");
		mPlayer.sendMessage(plugin, "level.create.creative");
		mPlayer.sendMessage(plugin, "level.create.beacon");
		mPlayer.sendMessage(plugin, "level.create.menu");
		if (mPlayer.teleport(getStartLocation(), TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.CREATIVE);
			mPlayer.setAllowFlight(true);
			mPlayer.setFlying(true);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(0, new ItemStack(Material.BEACON));
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			mPlayer.setCurrentLevel(this);
		} else {
			// TODO: disable/unload level
		}
	}

	public void startPlaying(MakerPlayer mPlayer) {
		if (!tryStatusTransition(LevelStatus.PLAY_READY, LevelStatus.PLAYING)) {
			mPlayer.sendMessage(plugin, "level.play.error.status");
			return;
		}
		mPlayer.sendActionMessage(plugin, "level.play.start");
		if (mPlayer.teleport(getStartLocation(), TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.ADVENTURE);
			mPlayer.setFlying(false);
			mPlayer.setAllowFlight(false);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			mPlayer.setCurrentLevel(this);
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

	private void tickClipboardCopied() {
		this.status = LevelStatus.SAVE_READY;
		plugin.saveLevelAsync(this);
	}

	private void tickClipboardPasted() {
		// TODO: find a better way to control these scenarios
		if (currentPlayerId != null) {
			if (currentPlayerId.equals(authorId) || isPublished()) {
				status = LevelStatus.PLAY_READY;
				return;
			}
		}
		currentPlayerId = null;
		status = LevelStatus.EDIT_READY;
	}

	private boolean isPublished() {
		return datePublished != null;
	}

	private void tickEdited() {
		this.status = LevelStatus.CLIPBOARD_COPY_READY;
		plugin.getLevelOperatorTask().offer(new LevelClipboardCopyOperation(plugin, this));
	}

	private void tickEditReady() {
		startEdition();
	}

	private void tickPlayReady() {
		startPlaying(currentPlayerId);
	}

	private void tickSaved() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			mPlayer.sendActionMessage(plugin, "level.save.success");
		}
		if (currentPlayerId != null) {
			// TODO: find a better way for this status transition (level editor save-play)
			this.status = LevelStatus.PLAY_READY;
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
		case PLAY_READY:
			tickPlayReady();
			break;
		case CLIPBOARD_PASTED:
			tickClipboardPasted();
			break;
		case CLIPBOARD_LOADED:
			tickClipboardLoaded();
			break;
		default:
			break;
		}
	}

	private void tickClipboardLoaded() {
		status = LevelStatus.CLIPBOARD_PASTE_READY;
		plugin.getLevelOperatorTask().offer(new LevelClipboardPasteOperation(this));
	}

	private void tickUnloadReady() {
		plugin.getController().unloadLevel(this);
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

	public void publishLevel() {
		if (!isClearedByAuthor()) {
			MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
			if (mPlayer != null) {
				mPlayer.sendActionMessage(plugin, "level.publish.error.not-cleared");
			}
			return;
		}
		// TODO: finish publishing
	}

	public WorldData getWorldData() {
		return plugin.getController().getMainWorldData();
	}

}
