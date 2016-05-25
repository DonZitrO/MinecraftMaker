package com.minecade.minecraftmaker.level;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.data.MakerRelativeLocationData;
import com.minecade.minecraftmaker.data.MakerSteveData;
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
import com.minecade.minecraftmaker.schematic.world.CuboidRegion;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.Vector2D;
import com.minecade.minecraftmaker.schematic.world.WorldData;
import com.minecade.minecraftmaker.util.LevelUtils;
import com.minecade.minecraftmaker.util.Tickable;
import com.minecade.nms.NMSUtils;

public class MakerPlayableLevel extends AbstractMakerLevel implements Tickable {

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
	private boolean firstTimeEdited = true;
	private MakerSteveData steveData;

	public MakerPlayableLevel(MinecraftMakerPlugin plugin, short chunkZ) {
		super(plugin);
		checkArgument(chunkZ >= 0);
		this.chunkZ = chunkZ;
		this.status = LevelStatus.BLANK;
	}

	public void checkLevelVoidBorder(Location to) {
		if (to.getBlockY() < -1) {
			restartPlaying();
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
		if (clipboard.getRegion().getHeight() > 128) {
			beacon = beacon.add(0, 48, 0);
		}
		for (int y = beacon.getBlockY() + 1; y <= clipboard.getMaximumPoint().getBlockY(); y++) {
			Vector above = new Vector(beacon.getBlockX(), y, beacon.getBlockZ());
			if (clipboard.getBlock(above).getType() != BlockID.BARRIER) {
				clipboard.setBlock(above, air);
			}
		}
	}

	// TODO: this method is too complex: clean and simplify
	private void clearLevel() {
		long clearTimeMillis = System.currentTimeMillis() - startTime;
		MakerPlayer mPlayer = plugin.getController().getPlayer(currentPlayerId);
		if (mPlayer == null) {
			disable(String.format("MakerLevel.clearLevel - player is offline: [%s]", currentPlayerId), null);
			return;
		}
		try {
			// TODO: maybe allow creator to stay for a bit for some after-clear surprises testing
			if (!isPublished() && authorId.equals(mPlayer.getUniqueId())) {
				if (plugin.isDebugMode()) {
					Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.clearLevel - author cleared unpublished level: [%s]", getLevelName()));
				}
				this.currentPlayerId = null;
				tryStatusTransition(LevelStatus.PLAYING, LevelStatus.CLIPBOARD_LOADED);
				removeEntities();
				waitForBusyLevel(mPlayer, false);
				if (this.clearedByAuthorMillis == 0 || this.clearedByAuthorMillis > clearTimeMillis) {
					this.clearedByAuthorMillis = clearTimeMillis;
					plugin.getDatabaseAdapter().updateLevelAuthorClearTimeAsync(getLevelId(), clearTimeMillis);
				}
				mPlayer.sendMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
				return;
			}
			removeEntities(false);
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
		if (isSteve()) {
			mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.clear.title"), plugin.getMessage("level.clear.subtitle", formatMillis(clearTimeMillis)));
			mPlayer.sendActionMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
			clearSteveLevel(mPlayer);
			return;
		} else {
			mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.clear.title"), plugin.getMessage("level.clear.subtitle", formatMillis(clearTimeMillis)));
			mPlayer.sendMessage(plugin, "level.clear.time", formatMillis(clearTimeMillis));
			mPlayer.sendMessage(plugin, "level.clear.options");
			Bukkit.getScheduler().runTaskLater(plugin, () -> openLevelOptionsAfterClear(mPlayer.getUniqueId()), 60);
			return;
		}
	}

	private void clearSteveLevel(MakerPlayer mPlayer) {
		steveData.clearLevel(getLevelSerial());
		if (steveData.getLevelsClearedCount() == 16) {
			finishSteveChallenge();
		} else {
			loadNextSteveLevel(mPlayer);
		}
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

	public void finishSteveChallenge() {
		if (isBusy()) {
			disable("attemped to skip a busy a level", null);
			return;
		}
		MakerPlayer mPlayer = getPlayerIsInThisLevel(currentPlayerId);
		if (mPlayer == null) {
			disable("player is no longer on this level", null);
			return;
		}
		String title = null;
		String subtitle = null;
		if (steveData.getLevelsClearedCount() == 16) {
			title = plugin.getMessage("steve.completed.title");
			subtitle = plugin.getMessage("steve.level.start.subtitle", steveData.getLevelsClearedCount(), steveData.getLives());
		} else {
			title = plugin.getMessage("steve.failed.title");
			if (steveData.getLives() == 0) {
				subtitle = plugin.getMessage("steve.failed.lives.subtitle");
			}
		}
		// TODO: persist steve data
		steveData = null;
		mPlayer.sendTitleAndSubtitle(title, subtitle);
		plugin.getController().addPlayerToMainLobby(mPlayer);
		status = LevelStatus.DISABLE_READY;
	}

	public void fixClipboard() throws MinecraftMakerException {
		removeCrystalFromBorders();
		clearBlocksAboveEndBeacon();
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

	public CuboidRegion getLevelRegion() {
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

	public MakerSteveData getSteveData() {
		return this.steveData;
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

	private boolean isSteve() {
		return steveData != null;
	}

	private void loadNextSteveLevel(MakerPlayer mPlayer) {
		if (!steveData.hasMoreLevels()) {
			finishSteveChallenge();
			return;
		}
		reset();
		waitForBusyLevel(mPlayer, false);
		setLevelSerial(steveData.getRandomLevel());
		plugin.getDatabaseAdapter().loadPlayableLevelBySerialAsync(this);
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
		if (isDisabled()) {
			event.setNewCurrent(event.getOldCurrent());
			return;
		}
		if (isBusy()) {
			Material newMaterial = null;
			// TODO: replace for stage 3 pasting when implemented
			if (LevelStatus.PASTING_CLIPBOARD.equals(getStatus())) {
				if (event.getOldCurrent() == 15 && event.getNewCurrent() == 0 && StringUtils.endsWith(event.getBlock().getType().name(), "_ON")) {
					newMaterial = Material.valueOf(StringUtils.removeEnd(event.getBlock().getType().name(), "ON").concat("OFF"));
				} else if (event.getOldCurrent() == 0 && event.getNewCurrent() == 15 && StringUtils.endsWith(event.getBlock().getType().name(), "_OFF")) {
					newMaterial = Material.valueOf(StringUtils.removeEnd(event.getBlock().getType().name(), "OFF").concat("ON"));
				}
				if (newMaterial != null) {
					LevelRedstoneInteraction cancelled = new LevelRedstoneInteraction(BukkitUtil.toVector(event.getBlock()), newMaterial, event.getBlock().getState().getData(), getCurrentTick(), event.getOldCurrent(), event.getNewCurrent());
					cancelledRedstoneInteractions.put(cancelled.getLocation(), cancelled);
					if (plugin.isDebugMode()) {
						Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.onBlockRedstone - saved cancelled redstone interaction: %s", cancelled));
					}
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
			// horse taming
			if (org.bukkit.entity.EntityType.HORSE.equals(event.getEntityType())) {
				if (((org.bukkit.entity.Horse) event.getEntity()).isAdult()) {
					plugin.getController().sendActionMessageToPlayerIfPresent(authorId, "level.create.horse.tame");
				}
			}
			return;
		}
		Bukkit.getLogger().warning(String.format("MakerLevel.onCreatureSpawn - illegal creature spawn on level: [%s] with status: [%s]", getLevelName(), getStatus()));
		event.setCancelled(true);
	}

	public void onEntityExplode(EntityExplodeEvent event) {
		if (isBusy()) {
			event.setCancelled(true);
			event.blockList().clear();
			return;
		}
		ListIterator<Block> iterator = event.blockList().listIterator();
		// protect beacons and their power blocks
		while (iterator.hasNext()) {
			Block block = iterator.next();
			if(Material.BEACON.equals(block.getType()) || LevelUtils.isBeaconPowerBlock(block)) {
				iterator.remove();
			}
		}
	}

	public void onEntityTeleport(EntityTeleportEvent event) {
		if (isBusy()) {
			event.setCancelled(true);
			return;
		}
		CuboidRegion region = getLevelRegion();
		if (region == null) {
			event.setCancelled(true);
			return;
		}
		region.contract(new Vector(3, 3, 3), new Vector(-3, -3, -3));
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

	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (isBusy()) {
			event.setCancelled(true);
			return;
		}
		CuboidRegion region = getLevelRegion();
		if (region == null) {
			event.setCancelled(true);
			return;
		}
		region.contract(new Vector(3, 3, 3), new Vector(-3, -3, -3));
		if (!region.contains(BukkitUtil.toVector(event.getFrom())) || !region.contains(BukkitUtil.toVector(event.getTo()))) {
			event.setCancelled(true);
			return;
		}
	}

	private void openLevelOptionsAfterClear(UUID playerId) {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(playerId);
		if (mPlayer!=null && LevelStatus.CLEARED.equals(getStatus())) {
			mPlayer.openPlayLevelOptionsMenu();
		}
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
		Region region = clipboard.getRegion();
		//final int startX = region.getMinimumPoint().getBlockX() % 16 == 0 ? region.getMinimumPoint().getBlockX() : region.getMinimumPoint().getBlockX() + (16 - (region.getMinimumPoint().getBlockX() % 16));
		//final int startY = region.getMinimumPoint().getBlockY() % 16 == 0 ? region.getMinimumPoint().getBlockY() : region.getMinimumPoint().getBlockY() + (16 - (region.getMinimumPoint().getBlockY() % 16));
		//Bukkit.getLogger().severe(String.format("startX: %s - startY: %s", startX, startY));
		for (int x = region.getMinimumPoint().getBlockX(); x <= region.getMaximumPoint().getBlockX(); x++) {
			for (int y = region.getMinimumPoint().getBlockY(); y <= region.getMaximumPoint().getBlockY(); y++) {
				//Bukkit.getLogger().severe(String.format("x: %s - y: %s - z1: %s - z2: %s", x, y,  region.getMinimumPoint().getBlockZ() + 1,  region.getMinimumPoint().getBlockZ() -1));
				clipboard.setBlock(new Vector(x, y, region.getMinimumPoint().getBlockZ() + 1), barrier);
				clipboard.setBlock(new Vector(x, y, region.getMaximumPoint().getBlockZ() - 1), barrier);
			}
		}
	}

	private void removeEntities() {
		removeEntities(true);
	}

	private void removeEntities(boolean all) {

		for (org.bukkit.entity.Entity entity : getEntities()) {
			switch (entity.getType()) {
			case PLAYER:
				break;
			default:
				if (all || entity instanceof LivingEntity || entity instanceof Projectile) {
					entity.remove();
					if (plugin.isDebugMode()) {
						Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.removeEntities - removed entity type: [%s]  from level: [%s]", entity.getType(), getLevelName()));
					}
				} else {
					if (plugin.isDebugMode()) {
						Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.removeEntities - skipped removal of entity type: [%s]  from level: [%s] - with status: [%s]", entity.getType(), getLevelName(), getStatus()));
					}
				}
				break;
			}
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

	@Override
	protected void reset() {
		super.reset();
		this.firstTimeLoaded = true;
		this.status = LevelStatus.BLANK;
	}

	public void restartPlaying() {
		if (!isBusy()) {
			status = LevelStatus.RESTART_PLAY_READY;
		} else {
			disable(String.format("failed to restart a busy level: [%s], - player: [%s]", getLevelName(), getStatus()), null);
		}
	}

	public void restoreRedstoneInteraction(LevelRedstoneInteraction cancelled) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerLevel.restoreRedstoneInteraction - tick: [%s] - interaction: [%s]", getCurrentTick(), cancelled));
		}
		@SuppressWarnings("deprecation")
		boolean changed = BukkitUtil.toLocation(getWorld(), cancelled.getLocation()).getBlock().setTypeIdAndData(cancelled.getMaterial().getId(), cancelled.getMaterialData().getData(), true);
		if (!changed) {
			Bukkit.getLogger().warning(String.format("MakerLevel.restoreRedstoneInteraction - unchanged redstone interaction - tick: [%s] - interaction: [%s]", getCurrentTick(), cancelled));
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

	public void setSteveData(MakerSteveData steveData) {
		this.steveData = steveData;
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

	public void skipSteveLevel() {
		if (isBusy()) {
			disable("attemped to skip a busy a level", null);
			return;
		}
		skipSteveLevel(true);
	}

	private void skipSteveLevel(boolean loseLife) {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(currentPlayerId);
		if (mPlayer == null) {
			steveData = null;
			disable("player is no longer on this level", null);
			return;
		}
		steveData.skipLevel(getLevelSerial(), loseLife);
		removeEntities();
		loadNextSteveLevel(mPlayer);
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
			if (firstTimeEdited) {
				firstTimeEdited = false;
				mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.create.start.title"), plugin.getMessage("level.create.start.subtitle"));
				mPlayer.sendActionMessage(plugin, "level.create.creative");
				mPlayer.sendActionMessage(plugin, "level.create.beacon");
				mPlayer.sendActionMessage(plugin, "level.create.menu");
				if (StringUtils.isBlank(this.levelName)) {
					mPlayer.sendActionMessage(plugin, "level.create.rename");
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
			mPlayer.sendActionMessage(plugin, "level.play.error.status");
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
				if (isSteve()) {
					mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.STEVE_LEVEL_OPTIONS.getItem());
				} else {
					mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getItem());
				}
			} else {
				mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.EDITOR_PLAY_LEVEL_OPTIONS.getItem());
			}
			mPlayer.updateInventory();
			if (isSteve()) {
				mPlayer.sendTitleAndSubtitle(plugin.getMessage("steve.level.start.title"), plugin.getMessage("steve.level.start.subtitle", steveData.getLevelsClearedCount(), steveData.getLives()));
			} else {
				mPlayer.sendTitleAndSubtitle(plugin.getMessage("level.play.start.title"), plugin.getMessage("level.play.start.subtitle"));
			}
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
			if (firstTimeLoaded) {
				fixClipboard();
			}
			setupCliboardFloor();
		} catch (Exception e) {
			disable(e.getMessage(), e);
			return;
		}
		if (currentPlayerId != null) {
			MakerPlayer mPlayer = getPlayerIsInThisLevel(currentPlayerId);
			if (mPlayer != null) {
				mPlayer.sendActionMessage(plugin, "steve.level.loading", getLevelName(), getAuthorName());
			}
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

	private void tickDisableReady() {
		if (isSteve() && steveData.hasMoreLevels()) {
			skipSteveLevel(false);
			return;
		}
		this.status = LevelStatus.DISABLED;
		this.cancelledRedstoneInteractions.clear();
		this.cancelledRedstoneInteractions = null;
		this.clipboard = null;
		this.steveData = null;
		MakerPlayer author = getPlayerIsInThisLevel(authorId);
		if (author != null) {
			author.sendMessage(plugin, "level.error.disable");
			author.sendMessage(plugin, "level.error.report");
			plugin.getController().addPlayerToMainLobby(author);
		}
		MakerPlayer currentLevelPlayer = getPlayerIsInThisLevel(currentPlayerId);
		if (currentLevelPlayer != null) {
			currentLevelPlayer.sendMessage(plugin, "level.error.disable");
			currentLevelPlayer.sendMessage(plugin, "level.error.report");
			plugin.getController().addPlayerToMainLobby(currentLevelPlayer);
		}
		this.currentPlayerId = null;
		removeEntities();
		plugin.getController().removeLevelFromSlot(this);
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
		mPlayer.sendMessage(plugin, "level.rename.success");
		mPlayer.sendMessage(plugin, "level.rename.save-reminder");
	}

	private void tickRenameError() {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(authorId);
		if (mPlayer == null) {
			this.status = LevelStatus.DISABLE_READY;
			return;
		}
		this.status = LevelStatus.EDIT_READY;
		mPlayer.sendMessage(plugin, "level.rename.error.name");
	}

	private void tickRestartPlayReady() {
		MakerPlayer mPlayer = getPlayerIsInThisLevel(currentPlayerId);
		if (mPlayer == null) {
			disable(String.format("Player: [%s] is no longer in this level: [%s]", currentPlayerId, getLevelName()), null);
			return;
		}
		if (isSteve()) {
			if (!steveData.tryAgain()) {
				finishSteveChallenge();
				return;
			} else {
				waitForBusyLevel(mPlayer, false);
				//mPlayer.sendTitleAndSubtitle(plugin.getMessage("steve.level.start.title"), plugin.getMessage("steve.level.start.subtitle", steveData.getLevelsClearedCount(), steveData.getLives()));
			}
		} else {
			waitForBusyLevel(mPlayer, true);
		}
		status = LevelStatus.CLIPBOARD_LOADED;
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
			// FIXME:
			//mPlayer.updateSavedLevelOnPlayerLevelsMenu(plugin, this);
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
		case RESTART_PLAY_READY:
			tickRestartPlayReady();
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

	@Override
	public void disable() {
		status = LevelStatus.DISABLE_READY;
	}

	@Override
	public String getDescription() {
		return String.format("MakerPlayableLevel: [%s(%s)<%s>] on slot: [%s]", getLevelName(), getLevelSerial(), getLevelId(), getChunkZ());
	}

	public boolean contains(org.bukkit.util.Vector position) {
		CuboidRegion region = getLevelRegion();
		region.contract(new Vector(2, 0, 3), new Vector(-2, -2, -2));
		return region.contains(BukkitUtil.toVector(position));
	}

}
