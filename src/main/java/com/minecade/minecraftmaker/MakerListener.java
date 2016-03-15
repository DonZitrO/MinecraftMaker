package com.minecade.minecraftmaker;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.minecade.core.gamebase.MinigamePlayer.Type;

public class MakerListener implements Listener {
	
	MakerBase base;
	
	public MakerListener(MakerBase base) {
		this.base = base;
	}


	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.setJoinMessage(base.getText("join.message", event.getPlayer().getDisplayName()));
		base.getLobby().onPlayerJoin(event.getPlayer());
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		base.getLobby().onPlayerQuit(event.getPlayer());
		event.setQuitMessage(null);
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onItemDrop(PlayerDropItemEvent event) {
		event.setCancelled(true);
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onEntitySpawn(CreatureSpawnEvent event) {
		if(event.getSpawnReason() == SpawnReason.NATURAL) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onLoseHealth(FoodLevelChangeEvent event) {
		if(event.getEntity() instanceof Player) {
			MakerPlayer mPlayer = (MakerPlayer) base.getMinigamePlayer((Player) event.getEntity());
			if(mPlayer.getArena() == null) {
				if(event.getFoodLevel() < 20) {
					event.setFoodLevel(20);
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onDamageEvent(EntityDamageEvent event) {
		if(event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			MakerPlayer mPlayer = (MakerPlayer) base.getMinigamePlayer(player);
			if(mPlayer.getArena() == null) {
				if(event.getCause() == DamageCause.VOID) {
					if(base.getLobby() != null && base.getLobby().getLobbySpawn() != null) {
						player.setFallDistance(0f);
						player.teleport(base.getLobby().getLobbySpawn().getLocation());
						event.setDamage(0);
						event.setCancelled(true);
					}
				}else {
					event.setDamage(0);
					event.setCancelled(true);
				}
			}else {
				mPlayer.getArena().onEntityDamageEvent(event);
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		MakerPlayer mPlayer = (MakerPlayer) base.getMinigamePlayer(player);
		if(mPlayer.getArena() != null) {
			mPlayer.getArena().onBlockPlace(event);
		}
		
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		MakerPlayer mPlayer = (MakerPlayer) base.getMinigamePlayer(player);
		if(mPlayer.getArena() != null) {
			mPlayer.getArena().onBlockBreak(event);
		}
		
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBlockInteract(PlayerInteractEvent event) {
		if(event.getClickedBlock() != null && event.getClickedBlock().getType() != Material.AIR) {
			Player player = event.getPlayer();
			MakerPlayer mPlayer = (MakerPlayer) base.getMinigamePlayer(player);
			if(mPlayer.getArena() != null) {
				mPlayer.getArena().onBlockInteract(event);
			}
		}
	}
}
