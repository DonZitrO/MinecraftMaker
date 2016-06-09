package com.minecade.minecraftmaker.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.core.event.AsyncAccountDataLoadEvent;
import com.minecade.core.event.EventUtils;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerListener implements Listener {

	private final MinecraftMakerPlugin plugin;

	public MakerListener(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public final void onAsyncAccountDataLoad(AsyncAccountDataLoadEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncAccountDataLoad - Player: [%s<%s>]", event.getData().getUsername(), event.getData().getUniqueId()));
		if (event.getData() instanceof MakerPlayerData) {
			plugin.getController().onAsyncAccountDataLoad((MakerPlayerData)event.getData());
		}
	}

	@EventHandler
	public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
		plugin.getController().onAsyncPlayerChat(event);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public final void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncPlayerPreLogin - Starting... - Player: [%s<%s>] - Initial result: [%s]", event.getName(), event.getUniqueId(), event.getLoginResult()));
		plugin.getController().onAsyncPlayerPreLogin(event);
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncPlayerPreLogin - Finished - Player: [%s<%s>] - Result: [%s]", event.getName(), event.getUniqueId(), event.getLoginResult()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public final void onPlayerLogin(PlayerLoginEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onPlayerLogin - Starting... - Player: [%s<%s>] - Initial result: [%s]", event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getResult()));
		plugin.getController().onPlayerLogin(event);
		Bukkit.getLogger().info(String.format("MakerListener.onPlayerLogin - Finished - Player: [%s<%s>] - Result: [%s]", event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getResult()));
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockBreak - block type: [%s] - location: [%s] - cancelled: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockBreak(event);
	}

	@EventHandler
	public void onBlockDispense(BlockDispenseEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockDispense - block type: [%s] - location: [%s] - cancelled: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockDispense(event);
	}

	@EventHandler
	public void onBlockFromTo(BlockFromToEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockFromTo - block type: [%s] - to block type: [%s] - location: [%s] - cancelled: [%s]", event.getBlock().getType(), event.getToBlock().getType(), event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockFromTo(event);
	}

	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockIgnite - block type: [%s] - location: [%s] - cancelled: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockIgnite(event);
	}

	@EventHandler
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (plugin.isDebugMode()) {
			//Bukkit.getLogger().severe(String.format("[ERROR] | MakerListener.onBlockPhysics - location: [%s] - cancelled: [%s]", event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockPhysics(event);
	}

	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockPistonExtend - location: [%s] - cancelled: [%s]", event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockPistonExtend(event);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockPlace - block type: [%s] - location: [%s] - cancelled: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockPlace(event);
	}

	@EventHandler
	public void onBlockRedstone(BlockRedstoneEvent event) {
//		if (plugin.isDebugMode()) {
//			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockRedstone - block type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
//		}
		plugin.getController().onBlockRedstone(event);
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().severe(String.format("[DEBUG] | MakerListener.onChunkUnload - chunk: [%s,%s]", event.getChunk().getX(), event.getChunk().getZ()));
		}
	}

	@EventHandler
	public void onCreatureDamageByEntity(final EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			return;
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onCreatureDamageByEntity - Type: [%s] - Causer type: [%s] - Damage: [%s] - Cancelled: [%s]", event.getEntityType(), event.getDamager().getType(), event.getDamage(), event.isCancelled()));
		}
		plugin.getController().onCreatureDamageByEntity(event);
	}

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onCreatureSpawn - Entity type: [%s] - Reason: [%s] - Location: [%s] - Cancelled: [%s]", event.getEntityType(), event.getSpawnReason(), event.getLocation().toVector(), event.isCancelled()));
		}
		// disable naturally spawning creatures
		if (event.getSpawnReason() == SpawnReason.NATURAL) {
			event.setCancelled(true);
			return;
		}
		event.getEntity().setRemoveWhenFarAway(false);
		plugin.getController().onCreatureSpawn(event);
	}

	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		// prevents mobs from burning in the daylight
		if (event.getDuration() == 8 && !(event.getEntity() instanceof Player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onEntityExplode - type: [%s] - location: [%s]", event.getEntityType(), event.getLocation().toVector()));
		}
		plugin.getController().onEntityExplode(event);
	}

	@EventHandler
	public void onEntityTeleport(EntityTeleportEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onEntityTeleport start - entity type: [%s] - from: [%s] - to: [%s] - cancelled: [%s]", event.getEntityType(), event.getFrom().toVector(), event.getTo().toVector(), event.isCancelled()));
		}
		// players have their own event handler
		if (event.getEntityType().equals(EntityType.PLAYER)) {
			return;
		}
		plugin.getController().onEntityTeleport(event);
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onEntityTeleport finish - entity type: [%s] - from: [%s] - to: [%s] - cancelled: [%s]", event.getEntityType(), event.getFrom().toVector(), event.getTo().toVector(), event.isCancelled()));
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MainListener.onInventoryClick - Player: [%s] - Inventory: [%s] - Slot type: [%s] - Slot: [%s]", event.getWhoClicked().getName(), event.getInventory().getName(), event.getSlotType(), event.getSlot()));
		}
		plugin.getController().onInventoryClick(event);
	}

	@EventHandler
	public void onPlayerDamage(final EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerDamage - Player: [%s] - Cause: [%s] - Damage: [%s] - Cancelled: [%s]", event.getEntity().getName(), event.getCause(), event.getDamage(), event.isCancelled()));
		}
		plugin.getController().onPlayerDamage(event);
	}

	@EventHandler (priority = EventPriority.LOW)
	public void onPlayerDeath(PlayerDeathEvent event) {
		plugin.getController().onPlayerDeath(event);
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerDropItem - Player: [%s] - ItemDrop: [%s] - Cancelled: [%s]", event.getPlayer().getName(), event.getItemDrop().getType(), event.isCancelled()));
		}
		plugin.getController().onPlayerDropItem(event);
		if (event.isCancelled()) {
			final Player player = event.getPlayer();
			new BukkitRunnable() {
				public void run() {
					if (player.isOnline()) {
						player.updateInventory();
					}
				}
			}.runTask(plugin);
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerInteract - Player: [%s] -  Action: [%s] - Cancelled: [%s]", event.getPlayer().getName(), event.getAction(), event.isCancelled()));
		}
		// prevent barrier breaking
		if (EventUtils.isBlockLeftClick(event, Material.BARRIER)) {
			event.setCancelled(true);
			return;
		}
		// allow to build around the beacons without interacting with them
		if (EventUtils.isBlockRightClick(event, Material.BEACON)) {
			event.setUseInteractedBlock(Result.DENY);
			event.setUseItemInHand(Result.ALLOW);
		}
		// delegate to controller for specific behavior
		plugin.getController().onPlayerInteract(event);
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerInteract - Exit - Player: [%s] - Action: [%s] - Cancelled: [%s]", event.getPlayer().getName(), event.getAction(), event.isCancelled()));
		}
	}

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerInteractEntity - Player: [%s] - Entity type: [%s]", event.getPlayer().getName(), event.getRightClicked().getType()));
		}
		// disable interactions with item frames // FIXME: uncomment and make it lobby only
//		if (event.getRightClicked() instanceof ItemFrame) {
//			event.setCancelled(true);
//			return;
//		}
		// delegate to controller for specific behavior
		plugin.getController().onPlayerInteractEntity(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public final void onPlayerJoin(PlayerJoinEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onPlayerJoin - Player: [%s<%s>]", event.getPlayer().getName(), event.getPlayer().getUniqueId()));
		event.setJoinMessage(null);
		plugin.getController().onPlayerJoin(event.getPlayer());
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.getPlayer().isDead()) {
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerMove - ignoring movement of dead player: [%s<%s>]", event.getPlayer().getName(), event.getPlayer().getUniqueId()));
			}
			return;
		}
		plugin.getController().onPlayerMove(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public final void onPlayerQuit(PlayerQuitEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onPlayerQuit - Player: [%s<%s>]", event.getPlayer().getName(), event.getPlayer().getUniqueId()));
		event.setQuitMessage(null);
		plugin.getController().onPlayerQuit(event.getPlayer());
	}

	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerRespawn - Player: [%s] - Respawn location: [%s]", event.getPlayer().getName(), event.getRespawnLocation().toVector()));
		}
		plugin.getController().onPlayerRespawn(event);
	}

	@EventHandler
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerSwapHandItems - Player: [%s<%s>] hand: [%s] - off hand: [%s]", event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getMainHandItem() != null ? event.getMainHandItem().getType() : "AIR", event.getOffHandItem() != null ? event.getOffHandItem().getType() : "AIR"));
		}
		if (event.getOffHandItem() != null && event.getOffHandItem().getType().equals(Material.ENDER_CHEST)) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerTeleport start - player: [%s] - from: [%s] - to: [%s] - cause: [%s] - cancelled: [%s]", event.getPlayer().getName(), event.getFrom().toVector(), event.getTo().toVector(), event.getCause(), event.isCancelled()));
		}
		switch (event.getCause()) {
		case PLUGIN:
		case UNKNOWN:
		case COMMAND:
		case SPECTATE:
			break;
		default:
			plugin.getController().onPlayerTeleport(event);
			break;
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerTeleport finish - player: [%s] - from: [%s] - to: [%s] - cause: [%s] - cancelled: [%s]", event.getPlayer().getName(), event.getFrom().toVector(), event.getTo().toVector(), event.getCause(), event.isCancelled()));
		}
	}

	@EventHandler
	public void onPrepareAnvil(PrepareAnvilEvent event) {
		if (event.getResult() == null) {
			return;
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPrepareAnvil - result item type: [%s] - location: [%s]", event.getResult().getType(), event.getInventory().getLocation()));
		}
		switch (event.getResult().getType()) {
		// disable renaming on this items
		case ENDER_CHEST:
			event.setResult(null);
			return;
		default:
			break;
		}
	}

	@EventHandler
	public void onProjectileHit(final ProjectileHitEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onProjectileHit - type: [%s] - location: [%s] - bounce: [%s] - velocity: [%s]", event.getEntityType(), event.getEntity().getLocation().toVector(), event.getEntity().doesBounce(), event.getEntity().getVelocity()));
		}
		// remove projectiles 3 seconds after they hit
		Bukkit.getScheduler().runTaskLater(plugin, () -> event.getEntity().remove(), 60);
	}

	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		plugin.getController().onVehicleMove(event);
	}

}
