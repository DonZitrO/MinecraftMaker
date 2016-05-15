package com.minecade.minecraftmaker.level;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.data.MakerRelativeLocationData;
import com.minecade.minecraftmaker.function.operation.LevelClipboardCopyOperation;
import com.minecade.minecraftmaker.function.operation.LevelClipboardPasteOperation;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitUtil;
import com.minecade.minecraftmaker.schematic.exception.DataException;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.world.BlockVector;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.Vector2D;
import com.minecade.minecraftmaker.schematic.world.WorldData;
import com.minecade.minecraftmaker.util.LevelUtils;
import com.minecade.minecraftmaker.util.Tickable;
import com.minecade.nms.NMSUtils;

public class MakerPlayableLevel extends MakerLevel implements Tickable {

	public static final short MAX_LEVEL_WIDTH = 160;
	public static final short HIGHEST_LEVEL_Y = 63;
	public static final short FLOOR_LEVEL_Y = 16;

	private static final MakerRelativeLocationData RELATIVE_START_LOCATION = new MakerRelativeLocationData(2.5, 17, 6.5, -90f, 0);

	private static final short MAX_LEVEL_ITEMS = 20;
	private static final short MAX_LEVEL_MOBS = 20;

	private Map<BlockVector, LevelRedstoneInteraction> cancelledRedstoneInteractions = new LinkedHashMap<>();

	private final Short chunkZ;

	private Clipboard clipboard;
	private UUID currentPlayerId;
	private long currentTick;
	private int lastItemCount;
	private int lastMobCount;
	private long startTime;
	private LevelStatus status;
	private boolean firstTimeLoaded = true;

	public MakerPlayableLevel(MinecraftMakerPlugin plugin, short chunkZ) {
		super(plugin);
		checkArgument(chunkZ >= 0);
		this.chunkZ = chunkZ;
		this.status = LevelStatus.BLANK;
	}

	public void checkLevelBorder(Location to) {
		if (to.getBlockY() < -1) {
			Bukkit.getScheduler().runTask(plugin, () -> restartPlaying());
		}
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

	private void clearBlocksAboveEndBeacon() throws MinecraftMakerException {
		if (relativeEndLocation == null) {
			return;
		}
		BaseBlock air = new BaseBlock(BlockID.AIR);
		Vector beacon = BukkitUtil.toVector(getEndLocation());
		for (int y = beacon.getBlockY() + 1; y <= clipboard.getMaximumPoint().getBlockY(); y++) {
			Vector above = new Vector(beacon.getBlockX(), y, beacon.getBlockZ());
			if (clipboard.getBlock(above).getType() != BlockID.BARRIER) {
				clipboard.setBlock(above, air);
			}
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
		// TODO: warning - we get here assuming the level is published
		plugin.getDatabaseAdapter().saveLevelClearAsync(getLevelId(), mPlayer.getUniqueId(), clearTimeMillis);
		if (authorId.equals(mPlayer.getUniqueId())) {
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.clearLevel - author cleared published level: [%s]", getLevelName()));
			}
			if (this.clearedByAuthorMillis == 0 || this.clearedByAuthorMillis > clearTimeMillis) {
				this.clearedByAuthorMillis = clearTimeMillis;
				plugin.getDatabaseAdapter().updateLevelAuthorClearTimeAsync(getLevelId(), clearTimeMillis);
			}
		}
		mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.clear.title"), plugin.getMessage("level.clear.subtitle", formatMillis(clearTimeMillis)));
		mPlayer.sendMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
		mPlayer.sendMessage(plugin, "level.clear.options");
	}

	public void continueEditing() {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer == null) {
			disable(String.format("MakerLevel.continueEditing - editor is offline"), null);
			return;
		}
		try {
			tryStatusTransition(LevelStatus.PLAYING, LevelStatus.CLIPBOARD_LOADED);
			this.currentPlayerId = null;
			waitForBusyLevel(mPlayer, true);
		} catch (DataException e) {
			disable(e.getMessage(), e);
			return;
		}
	}

	@Override
	public synchronized void disable(String reason, Exception exception) {
		Bukkit.getLogger().warning(String.format("MakerLevel.disable - disable request for level: [%s(%s)<%s>] on slot: [%s]", getLevelName(), getLevelSerial(), getLevelId(), getChunkZ()));
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

	public Location getEndLocation() {
		return relativeEndLocation.toLocation(chunkZ, getWorld());
	}

	public List<org.bukkit.entity.Entity> getEntities() {
		List<org.bukkit.entity.Entity> entities = new ArrayList<>();
		lastItemCount = 0;
		lastMobCount = 0;
		Region region = getLevelRegion();
		if (region == null) {
			return entities;
		}
		for (Vector2D chunkVector : region.getChunks()) {
			org.bukkit.Chunk chunk = plugin.getController().getMainWorld().getChunkAt(chunkVector.getBlockX(), chunkVector.getBlockZ());
			for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
				switch (entity.getType()) {
				case PLAYER:
					break;
				case DROPPED_ITEM:
					lastItemCount++;
					entities.add(entity);
					break;
				default:
					if (entity instanceof LivingEntity) {
						lastMobCount++;
					}
					entities.add(entity);
					break;
				}
			}
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.getEntities - current entity count: [%s] for level: [%s] - items: [%s] - mobs: [%s]", entities.size(), getLevelName(), lastItemCount, lastMobCount));
		}
		return entities;
	}

	public Region getLevelRegion() {
		return LevelUtils.getLevelRegion(chunkZ, getLevelWidth());
	}

	public int getLevelWidth() {
		return clipboard != null ? clipboard.getDimensions().getBlockX() : MAX_LEVEL_WIDTH;
	}

	// TODO: candidate for removal, use full level object when possible
	public Extent getMakerExtent() {
		return plugin.getController().getMakerExtent();
	}

	private MakerPlayer getPlayerIsInThisLevel(UUID playerId) {
		MakerPlayer mPlayer = plugin.getController().getPlayer(playerId);
		if (mPlayer != null && playerIsInThisLevel(mPlayer)) {
			return mPlayer;
		}
		return null;
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

	@Override
	public synchronized boolean isDisabled() {
		return LevelStatus.DISABLED.equals(getStatus());
	}

	public void onBlockFromTo(BlockFromToEvent event) {
		if (isBusy()) {
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | MakerLevel.onBlockFromTo - cancelled liquid block flowing on busy level: [%s<%s>] with status: [%s]", getLevelName(), getLevelId(), getStatus()));
			}
			event.setCancelled(true);
			return;
		}
		if (relativeEndLocation != null && LevelUtils.isAboveLocation(event.getToBlock().getLocation().toVector(), getEndLocation().toVector())) {
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | MakerLevel.onBlockFromTo - cancelled liquid block flowing on end beacon: [%s<%s>] with status: [%s]", getLevelName(), getLevelId(), getStatus()));
			}
			event.setCancelled(true);
			return;
		}
	}

	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		if (isBusy()) {
			event.setCancelled(true);
			return;
		}
		if (this.relativeEndLocation != null) {
			Location end = getEndLocation();
			for (Block toMove : event.getBlocks()) {
				if (LevelUtils.isAboveLocation(toMove.getRelative(event.getDirection()).getLocation().toVector(), end.toVector())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	public void onBlockRedstone(BlockRedstoneEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.onBlockRedstone - level: [%s] - status: [%s] - tick: [%s] - block type: [%s] - location: [%s] - old current: [%s] - new current: [%s]", getLevelName(), getStatus(), getCurrentTick(), event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.getOldCurrent(), event.getNewCurrent()));
		}
		if (isBusy()) {
			Material newMaterial = null;
			if (event.getOldCurrent() == 15 && event.getNewCurrent() == 0 && StringUtils.endsWith(event.getBlock().getType().name(), "ON")) {
				newMaterial = Material.valueOf(StringUtils.removeEnd(event.getBlock().getType().name(), "ON").concat("OFF"));
			} else if (event.getOldCurrent() == 0 && event.getNewCurrent() == 15 && StringUtils.endsWith(event.getBlock().getType().name(), "OFF")) {
				newMaterial = Material.valueOf(StringUtils.removeEnd(event.getBlock().getType().name(), "OFF").concat("ON"));
			}
			if (newMaterial != null) {
				LevelRedstoneInteraction cancelled = new LevelRedstoneInteraction(BukkitUtil.toVector(event.getBlock()), newMaterial, event.getBlock().getState().getData(), getCurrentTick(), event.getOldCurrent(), event.getNewCurrent());
				cancelledRedstoneInteractions.put(cancelled.getLocation(), cancelled);
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.onBlockRedstone - saved cancelled redstone interaction: %s", cancelled));
				}
			} else {

				Bukkit.getLogger().warning(String.format("[DEBUG] | MakerLevel.onBlockRedstone - ignored cancelled interaction - level: [%s] - status: [%s] - tick: [%s] - block type: [%s] - location: [%s] - old current: [%s] - new current: [%s]", getLevelName(), getStatus(), getCurrentTick(), event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.getOldCurrent(), event.getNewCurrent()));
			}
			event.setNewCurrent(event.getOldCurrent());
			return;
		}
	}

	public void onCreatureSpawn(CreatureSpawnEvent event) {
		// loading level for edition, stop mobs from moving/attacking
		if (LevelStatus.PASTING_CLIPBOARD.equals(getStatus())) {
			NMSUtils.disableMobAI(event.getEntity(), currentPlayerId == null);
			return;
		}
		if (LevelStatus.EDITING.equals(getStatus())) {
			// rules for specific entity types
			switch (event.getEntityType()) {
			case GUARDIAN:
				Material blockType = event.getLocation().getBlock().getType();
				if (!Material.WATER.equals(blockType) && !Material.STATIONARY_WATER.equals(blockType)) {
					event.setCancelled(true);
					plugin.getController().sendActionMessageToPlayerIfPresent(authorId, "level.create.error.water-mob", event.getEntityType().toString());
					return;
				}
				break;
			default:
				break;
			}
			// mob count restriction
			getEntities();
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.onCreatureSpawn - current mob count: [%s]", lastMobCount));
			}
			if (lastMobCount >= MAX_LEVEL_MOBS) {
				event.setCancelled(true);
				plugin.getController().sendActionMessageToPlayerIfPresent(authorId, "level.create.error.mob-limit", lastMobCount);
				return;
			}
			NMSUtils.disableMobAI(event.getEntity(), true);
			return;
		}
		Bukkit.getLogger().warning(String.format("MakerLevel.onCreatureSpawn - illegal creature spawn on level: [%s] with status: [%s]", getLevelName(), getStatus()));
		event.setCancelled(true);
	}

	public void onEntityTeleport(EntityTeleportEvent event) {
		if (isBusy()) {
			event.setCancelled(true);
			return;
		}
		Region region = getLevelRegion();
		if (region == null) {
			event.setCancelled(true);
			return;
		}
		if (!region.contains(BukkitUtil.toVector(event.getFrom())) || !region.contains(BukkitUtil.toVector(event.getTo()))) {
			event.setCancelled(true);
			return;
		}
	}

	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (LevelStatus.EDITING.equals(getStatus())) {
			if (ItemUtils.itemNameEquals(event.getItemDrop().getItemStack(), GeneralMenuItem.EDIT_LEVEL_OPTIONS.getDisplayName())) {
				event.setCancelled(true);
				return;
			}
			// item count restriction
			getEntities();
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.onPlayerDropItem - current item count: [%s]", lastItemCount));
			}
			if (lastItemCount >= MAX_LEVEL_ITEMS) {
				event.setCancelled(true);
				plugin.getController().sendActionMessageToPlayerIfPresent(authorId, "level.create.error.item-limit", lastItemCount);
				return;
			}
			NMSUtils.stopItemFromDespawning(event.getItemDrop());
			return;
		}
		Bukkit.getLogger().warning(String.format("MakerLevel.onPlayerDropItem - illegal item drop on level: [%s] with status: [%s]", getLevelName(), getStatus()));
		event.setCancelled(true);
	}

	public synchronized void onPlayerQuit() {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("MakerLevel.onPlayerQuit - Level will be disabled after player quit: [%s<%s>]", getLevelName(), getLevelId()));
		}
		status = LevelStatus.DISABLE_READY;
	}

	private boolean playerIsInThisLevel(MakerPlayer mPlayer) {
		// exact same level instance
		return this == mPlayer.getCurrentLevel();
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
		status = LevelStatus.PUBLISH_READY;
		plugin.getDatabaseAdapter().publishLevelAsync(this);
		plugin.getController().addPlayerToMainLobby(mPlayer);
	}

	private void removeCrystalFromBorders() throws MinecraftMakerException {
		BaseBlock barrier = new BaseBlock(BlockID.BARRIER);
		Region region = getLevelRegion();
		final int startX = region.getMinimumPoint().getBlockX() % 16 == 0 ? region.getMinimumPoint().getBlockX() : region.getMinimumPoint().getBlockX() + (16 - (region.getMinimumPoint().getBlockX() % 16));
		final int startY = region.getMinimumPoint().getBlockY() % 16 == 0 ? region.getMinimumPoint().getBlockY() : region.getMinimumPoint().getBlockY() + (16 - (region.getMinimumPoint().getBlockY() % 16));
		for (int x = startX; x <= region.getMaximumPoint().getBlockX(); x += 16) {
			for (int y = startY; y <= region.getMaximumPoint().getBlockY(); y += 16) {
				clipboard.setBlock(new Vector(x, y, region.getMinimumPoint().getBlockZ() + 1), barrier);
				clipboard.setBlock(new Vector(x, y, region.getMaximumPoint().getBlockZ() - 1), barrier);
			}
		}
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
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer == null) {
			disable(String.format("MakerLevel.saveLevel - editor is not in level"), null);
			return;
		}
		if (this.levelName == null) {
			mPlayer.sendActionMessage(plugin, "level.create.rename");
			return;
		}
		if (!isPlayableByEditor()) {
			mPlayer.sendActionMessage(plugin, "level.edit.error.missing-end");
			return;
		}
		this.currentPlayerId = authorId;
		waitForBusyLevel(mPlayer, true);
		this.status = LevelStatus.EDITED;
	}

	public synchronized void saveLevel() {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer == null) {
			disable(String.format("MakerLevel.saveLevel - editor is not in level"), null);
			return;
		}
		if (this.levelName == null) {
			mPlayer.sendActionMessage(plugin, "level.create.rename");
			return;
		}
		waitForBusyLevel(mPlayer, true);
		this.status = LevelStatus.EDITED;
	}

	public void setClipboard(Clipboard clipboard) {
		this.clipboard = clipboard;
	}

	public void setCurrentPlayerId(UUID currentPlayerId) {
		this.currentPlayerId = currentPlayerId;
	}

	private void setupCliboardFloor() throws MinecraftMakerException {
		BaseBlock limit = new BaseBlock(currentPlayerId != null ? BlockID.AIR : BlockID.BARRIER);
		Vector minimum = clipboard.getMinimumPoint();
		Vector maximum = clipboard.getMaximumPoint();
		for (int x = minimum.getBlockX(); x <= maximum.getBlockX(); x++) {
			for (int z = minimum.getBlockZ(); z <= maximum.getBlockZ(); z++) {
				clipboard.setBlock(new Vector(x, minimum.getBlockY(), z), limit);
			}
		}
	}

	// TODO: is this heavy enough to need a specific level state or a separate task for the level operator?
	public void setupEndLocation(Location location) {
		checkNotNull(location);
		long startNanos = 0;
		if (plugin.isDebugMode()) {
			startNanos = System.nanoTime();
		}
		if (relativeEndLocation != null) {
			Block formerBeacon = getEndLocation().getBlock();
			formerBeacon.setType(Material.AIR);
			formerBeacon.getState().update(true, false);
			updateBeaconBase(formerBeacon.getRelative(BlockFace.DOWN), Material.AIR);
		}
		updateBeaconBase(location.getBlock().getRelative(BlockFace.DOWN), Material.IRON_BLOCK);
		updateBeaconTop(location.getBlock().getRelative(BlockFace.UP));
		// reuse object on DB by inheriting the UUID when possible
		relativeEndLocation = new MakerRelativeLocationData(location, relativeEndLocation != null ? relativeEndLocation.getLocationId() : null);
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.setupEndLocation took: [%s] nanoseconds", System.nanoTime() - startNanos));
		}
	}

	public void startEditing() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null) {
			disable(String.format("MakerLevel.startEditing - editor is offline"), null);
			return;
		}
		if (!playerIsInThisLevel(mPlayer)) {
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
			mPlayer.getPlayer().setHealth(mPlayer.getPlayer().getMaxHealth());
			for (PotionEffect effect :mPlayer.getPlayer().getActivePotionEffects()) {
				mPlayer.getPlayer().removePotionEffect(effect.getType());;
			}
			mPlayer.setAllowFlight(true);
			mPlayer.setFlying(true);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(0, new ItemStack(Material.BEACON));
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventory();
			if (!playerIsInThisLevel(mPlayer)) {
				mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.create.start.title"), plugin.getMessage("level.create.start.subtitle"));
				mPlayer.sendMessage(plugin, "level.create.creative");
				mPlayer.sendMessage(plugin, "level.create.beacon");
				mPlayer.sendMessage(plugin, "level.create.menu");
				if (StringUtils.isBlank(this.levelName)) {
					mPlayer.sendMessage(plugin, "level.create.rename");
				}
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
		if (!playerIsInThisLevel(mPlayer)) {
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
			mPlayer.getPlayer().setHealth(mPlayer.getPlayer().getMaxHealth());
			for (PotionEffect effect :mPlayer.getPlayer().getActivePotionEffects()) {
				mPlayer.getPlayer().removePotionEffect(effect.getType());;
			}
			mPlayer.setFlying(false);
			mPlayer.setAllowFlight(false);
			mPlayer.clearInventory();
			// FIXME: re-think this control
			if (isPublished()) {
				mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getItem());
			} else {
				mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.EDITOR_PLAY_LEVEL_OPTIONS.getItem());
			}
			mPlayer.updateInventory();
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
		try {
			removeEntities();
			removeCrystalFromBorders();
			setupCliboardFloor();
			clearBlocksAboveEndBeacon();
		} catch (Exception e) {
			disable(e.getMessage(), e);
			return;
		}
		status = LevelStatus.CLIPBOARD_PASTE_READY;
		if (firstTimeLoaded) {
			firstTimeLoaded = false;
			plugin.getLevelOperatorTask().offer(LevelUtils.createPasteOperation(LevelUtils.createEmptyLevelClipboard(getChunkZ(), 0), getMakerExtent(), getWorldData()));
		}
		plugin.getLevelOperatorTask().offer(new LevelClipboardPasteOperation(this));
	}

	private void tickClipboardPasted() {
		restoreRedstoneInteractions();
		if (currentPlayerId != null) {
			status = LevelStatus.PLAY_READY;
		} else {
			status = LevelStatus.EDIT_READY;
		}
	}

	private void restoreRedstoneInteractions() {
		long firstTick = 0;
		for (LevelRedstoneInteraction cancelled : cancelledRedstoneInteractions.values()) {
			if (firstTick == 0) {
				firstTick = cancelled.getTick();
			}
			if (cancelled.getTick() == firstTick) {
				restoreRedstoneInteraction(cancelled);
			} else {
				Bukkit.getScheduler().runTaskLater(plugin, () -> restoreRedstoneInteraction(cancelled), cancelled.getTick() - firstTick);
			}
		}
		cancelledRedstoneInteractions.clear();
	}

	public void restoreRedstoneInteraction(LevelRedstoneInteraction cancelled) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.restoreRedstoneInteraction - tick: [%s] - interaction: [%s]", getCurrentTick(), cancelled));
		}
		@SuppressWarnings("deprecation")
		boolean changed = BukkitUtil.toLocation(getWorld(), cancelled.getLocation()).getBlock().setTypeIdAndData(cancelled.getMaterial().getId(), cancelled.getMaterialData().getData(), true);
		if (!changed) {
			Bukkit.getLogger().severe(String.format("[DEBUG] | MakerLevel.restoreRedstoneInteraction - unable to restore interaction - tick: [%s] - interaction: [%s]", getCurrentTick(), cancelled));
		}
	}

	private void tickDisableReady() {
		this.status = LevelStatus.DISABLED;
		plugin.getController().removeLevelFromSlot(this);
		MakerPlayer author = getPlayerIsInThisLevel(authorId);
		if (author != null) {
			plugin.getController().addPlayerToMainLobby(author);
		}
		MakerPlayer currentLevelPlayer = getPlayerIsInThisLevel(currentPlayerId);
		if (currentLevelPlayer != null) {
			plugin.getController().addPlayerToMainLobby(currentLevelPlayer);
		}
		removeEntities();
		cancelledRedstoneInteractions.clear();
		this.currentPlayerId = null;
		this.clipboard = null;
	}

	private void tickEdited() {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.tickEdited - level: [%s] - status: [%s] - tick: [%s]", getLevelName(), getStatus(), getCurrentTick()));
		}
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
			mPlayer.updatePublishedLevelOnPlayerLevelsMenu(plugin, this);
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
		mPlayer.sendMessage(plugin, "level.rename.save-reminder");
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
			mPlayer.updateSavedLevelOnPlayerLevelsMenu(plugin, this);
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
		belowFormerBeacon.getState().update(true, false);
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
				aroundBlock.getState().update(true, false);
				break;
			default:
				break;
			}
		}
	}

	private void updateBeaconTop(Block aboveBeacon) {
		while (!aboveBeacon.getType().equals(Material.BARRIER) && aboveBeacon.getLocation().getBlockY() < 128) {
			aboveBeacon.setType(Material.AIR);
			aboveBeacon.getState().update(true, false);
			aboveBeacon = aboveBeacon.getRelative(BlockFace.UP);
		}
	}

	public void waitForBusyLevel(MakerPlayer mPlayer, boolean showMessage) {
		mPlayer.setGameMode(GameMode.SPECTATOR);
		mPlayer.setCurrentLevel(this);
		mPlayer.teleportOnNextTick(getStartLocation());
		if (showMessage) {
			mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.busy.title"), plugin.getMessage("level.busy.subtitle"));
		}
	}

	public void setupStartLocation() {
		Vector mp = getLevelRegion().getMinimumPoint();
		Block startLocation = BukkitUtil.toLocation(getWorld(), mp.add(2, FLOOR_LEVEL_Y, 6)).getBlock();
		startLocation.setType(Material.BEACON);
		startLocation.getState().update(true, false);
		Block aboveStart = startLocation.getRelative(BlockFace.UP);
		aboveStart.setType(Material.AIR);
		aboveStart.getState().update(true, false);
		aboveStart = startLocation.getRelative(BlockFace.UP);
		aboveStart.setType(Material.AIR);
		aboveStart.getState().update(true, false);
	}

}
