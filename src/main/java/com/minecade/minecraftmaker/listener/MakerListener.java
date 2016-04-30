package com.minecade.minecraftmaker.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.core.event.AsyncAccountDataLoadEvent;
import com.minecade.core.event.EventUtils;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.nms.NMSUtils;

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

	@EventHandler(priority = EventPriority.LOWEST)
	public final void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncPlayerPreLogin - Starting... - Player: [%s<%s>] - Initial result: [%s]", event.getName(), event.getUniqueId(), event.getLoginResult()));
		plugin.getController().onAsyncPlayerPreLogin(event);
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncPlayerPreLogin - Finished - Player: [%s<%s>] - Result: [%s]", event.getName(), event.getUniqueId(), event.getLoginResult()));
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockBreak - block type: [%s] - location: [%s] - cancelled: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockBreak(event);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onBlockPlace - block type: [%s] - location: [%s] - cancelled: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector(), event.isCancelled()));
		}
		plugin.getController().onBlockPlace(event);
	}

	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		// prevents mobs from burning in the daylight
		if (event.getDuration() == 8 && !(event.getEntity() instanceof Player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(final EntityDamageEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onEntityDamage - Entity: [%s] - Cause: [%s] - Damage: [%s] - Cancelled: [%s]", event.getEntity().getName(), event.getCause(), event.getDamage(), event.isCancelled()));
		}
		plugin.getController().onEntityDamage(event);
	}

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onCreatureSpawn - Entity: [%s] - Reason: [%s] - Location: [%s] - Cancelled: [%s]", event.getEntity().getName(), event.getSpawnReason(), event.getLocation().toVector(), event.isCancelled()));
		}
		if (event.getSpawnReason() == SpawnReason.NATURAL) {
			event.setCancelled(true);
		}
		NMSUtils.stopMobFromMovingAndAttacking(event.getEntity());
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MainListener.onInventoryClick - Player: [%s] - Inventory: [%s] - Slot: [%s]", event.getWhoClicked().getName(), event.getInventory().getName(), event.getSlot()));
		}
		plugin.getController().onInventoryClick(event);
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
		event.setCancelled(true);
		final Player player = event.getPlayer();
		new BukkitRunnable() {
			public void run() {
				if (player.isOnline()) {
					player.updateInventory();
				}
			}
		}.runTask(plugin);
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
		plugin.getController().onPlayerMove(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public final void onPlayerQuit(PlayerQuitEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onPlayerQuit - Player: [%s<%s>]", event.getPlayer().getName(), event.getPlayer().getUniqueId()));
		event.setQuitMessage(null);
		plugin.getController().onPlayerQuit(event.getPlayer());
	}

}
