package com.minecade.minecraftmaker.level;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
import com.minecade.minecraftmaker.schematic.world.Vector2D;
import com.minecade.minecraftmaker.schematic.world.WorldData;
import com.minecade.minecraftmaker.util.Tickable;
import com.minecade.nms.NMSUtils;

public class MakerLevel implements Tickable {

	private static final MakerRelativeLocationData RELATIVE_START_LOCATION = new MakerRelativeLocationData(2.5, 65, 6.5, -90f, 0);

	private static final int MAX_LEVEL_ENTITIES = 10;

	private final MinecraftMakerPlugin plugin;

	private UUID levelId;
	private long levelSerial;
	private String levelName;
	private UUID authorId;
	private String authorName;
	private Short chunkZ;
	private LevelStatus status;

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
		removeEntities();
		MakerPlayer mPlayer = plugin.getController().getPlayer(currentPlayerId);
		if (mPlayer == null) {
			disable(String.format("MakerLevel.clearLevel - player is offline: [%s]", currentPlayerId), null);
			return;
		}
		try {
			if (!isPublished() && authorId.equals(mPlayer.getUniqueId())) {
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.clearLevel - author cleared unpublished level: [%s]", getLevelName()));
				}
				this.currentPlayerId = null;
				tryStatusTransition(LevelStatus.PLAYING, LevelStatus.CLIPBOARD_LOADED);
				waitForBusyLevel(mPlayer, false);
				if (this.clearedByAuthorMillis == 0 || this.clearedByAuthorMillis > clearTimeMillis) {
					this.clearedByAuthorMillis = clearTimeMillis;
					plugin.getDatabaseAdapter().updateLevelAuthorClearTimeAsync(getLevelId(), clearTimeMillis);
				}
				mPlayer.sendMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
				return;
			}
			tryStatusTransition(LevelStatus.PLAYING, LevelStatus.CLEARED);
		} catch (DataException e) {
			mPlayer.sendMessage(plugin, "level.clear.error.status");
			disable(e.getMessage(), e);
			return;
		}
		if (authorId.equals(mPlayer.getUniqueId())) {
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.clearLevel - author cleared published level: [%s]", getLevelName()));
			}
			if (this.clearedByAuthorMillis == 0 || this.clearedByAuthorMillis > clearTimeMillis) {
				this.clearedByAuthorMillis = clearTimeMillis;
				plugin.getDatabaseAdapter().updateLevelAuthorClearTimeAsync(getLevelId(), clearTimeMillis);
			}
		} else {
			plugin.getDatabaseAdapter().updateLevelClearAsync(getLevelId(), mPlayer.getUniqueId(), clearTimeMillis);
		}
		mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.clear.title"), plugin.getMessage("level.clear.subtitle", formatMillis(clearTimeMillis)));
		mPlayer.sendMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
		mPlayer.sendMessage(plugin, "level.clear.options");
	}

	@Override
	public synchronized void disable(String reason, Exception exception) {
		Bukkit.getLogger().warning(String.format("MakerLevel.disable - disable request for level: [%s<%s>]", getLevelName(), getLevelId()));
		if (reason != null) {
			Bukkit.getLogger().warning(String.format("MakerLevel.disable - reason: %s", reason));
		}
		StackTraceElement[] stackTrace = exception != null ? exception.getStackTrace() : Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			Bukkit.getLogger().warning(String.format("MakerLevel.disable - stack trace: %s", element));
		}
		if (isDisabled()) {
			return;
		}
		status = LevelStatus.DISABLE_READY;
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
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer != null) {
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		this.status = LevelStatus.DISABLE_READY;
	}

	public synchronized void exitPlaying() {
		// TODO: maybe verify PLAYING status
		MakerPlayer mPlayer = getPlayerIsInThisLevel(currentPlayerId);
		if (mPlayer != null) {
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		this.status = LevelStatus.DISABLE_READY;
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

	public List<org.bukkit.entity.Entity> getEntities() {
		List<org.bukkit.entity.Entity> entities = new ArrayList<>();
		if (clipboard == null) {
			return entities;
		}
		for (Vector2D chunkVector : clipboard.getRegion().getChunks()) {
			org.bukkit.Chunk chunk = plugin.getController().getMainWorld().getChunkAt(chunkVector.getBlockX(), chunkVector.getBlockZ());
			for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
				if (EntityType.PLAYER.equals(entity.getType())) {
					continue;
				}
				entities.add(entity);
			}
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.getEntities - current entity count: [%s] for level: [%s]", entities.size(), getLevelName()));
		}
		return entities;
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
	public synchronized boolean isDisabled() {
		return LevelStatus.DISABLED.equals(getStatus());
	}

	public boolean isPlayableByEditor() {
		return relativeEndLocation != null;
	}

	private boolean isPublished() {
		return datePublished != null;
	}

	// FIXME: experimental
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		// loading level for edition, stop mobs from moving/attacking
		if (LevelStatus.PASTING_CLIPBOARD.equals(getStatus()) && currentPlayerId == null) {
			NMSUtils.stopMobFromMovingAndAttacking(event.getEntity());
			return;
		}
		if (LevelStatus.EDITING.equals(getStatus())) {
			// not supported entity types
			switch (event.getEntityType()) {
			case BAT:
			case GUARDIAN:
				event.setCancelled(true);
				sendActionMessageToAuthor("level.create.error.not-supported-mob", event.getEntityType().toString());
				return;
			default:
				break;
			}
			// entity count restriction
			int entityCount = getEntities().size();
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.onCreatureSpawn - current entity count: [%s]", entityCount));
			}
			if (entityCount >= MAX_LEVEL_ENTITIES) {
				event.setCancelled(true);
				sendActionMessageToAuthor("level.create.error.mob-limit", entityCount);
				return;
			}
			NMSUtils.stopMobFromMovingAndAttacking(event.getEntity());
			return;
		}
	}

	public synchronized void onPlayerQuit() {
		disable(String.format("MakerLevel.onPlayerQuit - player quit server"), null);
	}

	private MakerPlayer getPlayerIsInThisLevel(UUID playerId) {
		MakerPlayer mPlayer = plugin.getController().getPlayer(playerId);
		if (mPlayer != null && playerIsInThisLevel(mPlayer)) {
			return mPlayer;
		}
		return null;
	}

	private boolean playerIsInThisLevel(MakerPlayer mPlayer) {
		return this.equals(mPlayer.getCurrentLevel());
	}

	public void publishLevel() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			disable(String.format("MakerLevel.publishLevel - editor is offline"), null);
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

	private void removeEntities() {
		for (org.bukkit.entity.Entity entity : getEntities()) {
			if (EntityType.PLAYER.equals(entity.getType())) {
				continue;
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.removeEntities - removing entity type: [%s]  from level: [%s]", entity.getType(), getLevelName()));
			}
			entity.remove();
		}
	}

	public void rename(String newName) {
		try {
			tryStatusTransition(LevelStatus.EDITING, LevelStatus.RENAME_READY);
		} catch (DataException e) {
			disable(e.getMessage(), e);
			return;
		}
		plugin.getDatabaseAdapter().renameLevelAsync(this, newName);
	}

	public synchronized void restartPlaying() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(currentPlayerId);
		if (mPlayer != null) {
			waitForBusyLevel(mPlayer, true);
		}
		status = LevelStatus.CLIPBOARD_LOADED;
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
			waitForBusyLevel(mPlayer, true);
		}
		// FIXME: re-think status name
		this.status = LevelStatus.EDITED;
	}

	private void sendActionMessageToAuthor(String key, Object... args) {
		MakerPlayer author = plugin.getController().getPlayer(authorId);
		if (author != null) {
			author.sendActionMessage(plugin, key, args);
		}
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
			disable(String.format("MakerLevel.startEditing - editor is offline"), null);
			return;
		}
		if (!mPlayer.isInLobby() && !playerIsInThisLevel(mPlayer)) {
			disable(String.format("MakerLevel.startEditing - editor with id: [%s] is busy on another level", authorId), null);
			return;
		}
		try {
			tryStatusTransition(LevelStatus.EDIT_READY, LevelStatus.EDITING);
		} catch (DataException e) {
			mPlayer.sendMessage(plugin, "level.edit.error.status");
			disable(e.getMessage(), e);
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
			return;
		} else {
			disable(String.format("MakerLevel.startEditing - unable to teleport editor with id: [%s] to level: [%s]", authorId, getLevelName()), null);
			return;
		}
	}

	public void startPlaying(MakerPlayer mPlayer) {
		if (!mPlayer.isInLobby() && !playerIsInThisLevel(mPlayer)) {
			disable(String.format("MakerLevel.startPlaying - player with id: [%s] is busy on another level", mPlayer.getUniqueId()), null);
			return;
		}
		try {
			tryStatusTransition(LevelStatus.PLAY_READY, LevelStatus.PLAYING);
		} catch (DataException e) {
			mPlayer.sendMessage(plugin, "level.play.error.status");
			disable(e.getMessage(), e);
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
			disable(String.format("MakerLevel.startPlaying - unable to send player with id: [%s] to level: [%s]", currentPlayerId, getLevelName()), null);
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
		removeEntities();
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
		status = LevelStatus.DISABLE_READY;
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			mPlayer.updatePublishedLevelOnLevelBrowser(plugin, this);
			mPlayer.sendActionMessage(plugin, "level.publish.success");
		}
	}

	private void tickRenamed() {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer == null) {
			this.status = LevelStatus.DISABLE_READY;
			return;
		}
		this.status = LevelStatus.EDIT_READY;
		mPlayer.sendActionMessage(plugin, "level.rename.success");
	}

	private void tickRenameError() {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer == null) {
			this.status = LevelStatus.DISABLE_READY;
			return;
		}
		this.status = LevelStatus.EDIT_READY;
		mPlayer.sendActionMessage(plugin, "level.rename.error.name");
	}

	private void tickSaved() {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer != null) {
			if (currentPlayerId != null) {
				this.status = LevelStatus.CLIPBOARD_LOADED;
			} else {
				this.status = LevelStatus.EDIT_READY;
			}
		} else {
			this.status = LevelStatus.DISABLE_READY;
			mPlayer = plugin.getController().getPlayer(authorId);
		}
		if (mPlayer != null) {
			mPlayer.updateSavedLevelOnLevelBrowser(plugin, this);
			mPlayer.sendActionMessage(plugin, "level.save.success");
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
		case DISABLE_READY:
			tickDisableReady();
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

	private void tickDisableReady() {
		this.status = LevelStatus.DISABLED;
		plugin.getController().removeLevelFromSlot(this);
		MakerPlayer author = getPlayerIsInThisLevel(authorId);
		if (author!=null) {
			plugin.getController().addPlayerToMainLobby(author);
		}
		MakerPlayer currentLevelPlayer = getPlayerIsInThisLevel(currentPlayerId);
		if (currentLevelPlayer != null) {
			plugin.getController().addPlayerToMainLobby(currentLevelPlayer);
		}
		removeEntities();
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
