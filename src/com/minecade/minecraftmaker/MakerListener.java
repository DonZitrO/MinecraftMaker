package com.minecade.minecraftmaker;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
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

}
