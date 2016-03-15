package com.minecade.minecraftmaker;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;

import com.minecade.core.gamebase.MinigameArena;
import com.minecade.core.gamebase.MinigamePlayer.Type;
import com.minecade.core.item.Loadout;
import com.minecade.serverweb.shared.constants.GameState;

public class MakerArena extends MinigameArena {
	
	enum ArenaType {
		CREATING,
		PLAYING;
	}
	
	private ArenaType arenaType = ArenaType.PLAYING;
	private ArenaDefinition arenaDef = null;
	private MakerSchematic schematic = null;
	private UUID id = UUID.randomUUID();
	private SlotBoundaries slot = null;
	
	private MakerPlayer player = null;
	private ArrayList<MakerPlayer> spectators = new ArrayList<MakerPlayer>();
	
	private int lives = 5;
	
	private int arenaWidth = 7;
	
	private static final int MAX_CHUNKS = 10;
	
	private Loadout arenaLoadout = new Loadout();

	/**
	 * Use this method for a player who is just going to be creating a blank arena.
	 * @param base
	 * @param name the name can be blank, as the user will fill it in during arena creation.
	 * @param minPlayers this should be set to 1
	 * @param maxPlayers this should also be set to 1, unless we want more than 1 editor.
	 */
	public MakerArena(MakerBase base, String name, int minPlayers, int maxPlayers) {
		super(base, name, minPlayers, maxPlayers);
		arenaType = ArenaType.CREATING;
	}
	
	public MakerArena(MakerBase base, ArenaDefinition arenaDef, int minPlayers, int maxPlayers) {
		super(base, arenaDef.getName(), minPlayers, maxPlayers);
		this.arenaDef = arenaDef;
	}
	
	public MakerArena(MakerBase base, ArenaDefinition arenaDef, int minPlayers, int maxPlayers, ArenaType arenaType) {
		super(base, arenaDef.getName(), minPlayers, maxPlayers);
		this.arenaDef = arenaDef;
		this.arenaType = arenaType;
	}

	@Override
	public boolean addPlayer(Player player, Type type) {
		if(type == Type.Player) {
			if(this.player != null) {
				return false;
			}
			this.player = (MakerPlayer) base.getMinigamePlayer(player);
			return true;
		}else {
			//TODO: add in spectator support
		}
		return false;
	}

	@Override
	public void removePlayer(Player player) {
		if(player == this.player.getPlayer()) {
			this.player = null;
			endGame();
		}else {
			//TODO: spectator code
		}
	}

	@Override
	public void setStatus(GameState status) {
		this.status = status;
	}

	@Override
	public boolean startGame() {
		setStatus(GameState.IN_GAME);
		if(arenaType == ArenaType.PLAYING) {
			((MakerBase)base).loadArenaSchematic(this, arenaDef);
		}else if(arenaType == ArenaType.CREATING) {
			
		}
		return true;
	}
	
	public void endGame() {
		//TODO:
	}

	@Override
	public boolean isArenaReady() {
		return player != null && schematic != null && slot != null && arenaDef != null;
	}

	public void onEntityDamageEvent(EntityDamageEvent event) {
		// TODO Auto-generated method stub
		if(event.getEntity() instanceof Player) {
			if(event.getCause() == DamageCause.VOID) {
				if(arenaType == ArenaType.PLAYING) {
					if(--lives < 1) {
						endGame();
					}else {
						//Reset player
						pasteSchematic();
					}
				}else {
					//TODO: return to default spawn point
				}
			}
		}
	}
	
	public void onBlockPlace(BlockPlaceEvent event) {
		//Don't let them build outside of their arena
		int z = event.getBlock().getZ();
		if(z < slot.getZ() + 1 || z > slot.getZ() + arenaWidth) {
			event.setCancelled(true);
			return;
		}
		int x = event.getBlock().getX();
		if(x < slot.getX() || x > 16*MAX_CHUNKS + slot.getX()) {
			event.setCancelled(true);
			return;
		}
		if(arenaType == ArenaType.CREATING) {
			Block block = event.getBlock();
			if(block.getType() == Material.BEACON) {
				if(z < slot.getZ() + 2 || z > slot.getZ() + arenaWidth - 1) {
					event.setCancelled(true);
					event.getPlayer().sendMessage(base.getText("arena.beacon.tooclose"));
					return;
				}
				MakerRelativeLocation finish = arenaDef.getFinish();
				if(finish != null) {
					finish.getLocation(slot).getBlock().setType(Material.AIR);
					int goldx = finish.getBlockX() - 1;
					int goldy = finish.getBlockY() - 1;
					int goldz = finish.getBlockZ() - 1;
					for(int x1 = 0; x < 3; x++) {
						for(int z1 = 0; z < 3; z++) {
							Block gblock = new Location(((MakerBase)base).getArenaWorld(), x1 + goldx, goldy, z1 + goldz).getBlock();
							gblock.setType(Material.STONE);
						}
					}
				}
				arenaDef.setFinish(new MakerRelativeLocation(block.getLocation(), slot));
				int goldx = block.getX() - 1;
				int goldy = block.getY() - 1;
				int goldz = block.getZ() - 1;
				for(int x1 = 0; x < 3; x++) {
					for(int z1 = 0; z < 3; z++) {
						Block gblock = new Location(((MakerBase)base).getArenaWorld(), x1 + goldx, goldy, z1 + goldz).getBlock();
						gblock.setType(Material.GOLD_BLOCK);
					}
				}
			}
		}
	}
	
	public void onBlockBreak(BlockBreakEvent event) {
		//Don't let them break anything outside of their arena
		int z = event.getBlock().getZ();
		if(z < slot.getZ() + 1 || z > slot.getZ() + arenaWidth) {
			event.setCancelled(true);
			return;
		}
		int x = event.getBlock().getX();
		if(x < slot.getX() || x > 16*MAX_CHUNKS + slot.getX()) {
			event.setCancelled(true);
			return;
		}
		if(arenaType == ArenaType.CREATING) {
			Block block = event.getBlock();
			if(block.getType() == Material.BEACON) {
				MakerRelativeLocation finish = arenaDef.getFinish();
				if(finish != null && finish.getBlockX() == block.getX() && finish.getBlockY() == block.getY() &&
						finish.getBlockZ() == block.getZ()) {
					arenaDef.setFinish(null);
					int goldx = block.getX() - 1;
					int goldy = block.getY() - 1;
					int goldz = block.getZ() - 1;
					for(int x1 = 0; x < 3; x++) {
						for(int z1 = 0; z < 3; z++) {
							Block gblock = new Location(((MakerBase)base).getArenaWorld(), x1 + goldx, goldy, z1 + goldz).getBlock();
							gblock.setType(Material.STONE);
						}
					}
				}
			}
		}
	}
	
	public void onBlockInteract(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		//Don't let them interact with anything outside of their arena
		if(block != null) {
			int z = block.getZ();
			if(z < slot.getZ() + 1 || z > slot.getZ() + arenaWidth) {
				event.setCancelled(true);
				return;
			}
			int x = block.getX();
			if(x < slot.getX() || x > 16*MAX_CHUNKS + slot.getX()) {
				event.setCancelled(true);
				return;
			}
		}
		//Let's not let a player pull stuff out of a dispenser or hopper
		if(block != null && (block.getType() == Material.DISPENSER || block.getType() == Material.HOPPER) 
				&& arenaType == ArenaType.PLAYING) {
			event.setCancelled(true);
		}
	}
	
	public void setArenaSchematic(MakerSchematic schematic) {
		this.schematic = schematic;
		if(getStatus() == GameState.IN_GAME) {
			pasteSchematic();
			//TODO: Let's get the player set up to play the game!
		}
	}
	
	public void pasteSchematic() {
		schematic.pasteSchematic(slot);
	}
	
	public void arenaPasted() {
		//TODO:
		if(arenaType == ArenaType.PLAYING) {
			Player bukkitPlayer = player.getPlayer();
			bukkitPlayer.teleport(arenaDef.getSpawn().getLocation(slot));
			bukkitPlayer.setGameMode(GameMode.ADVENTURE);
			bukkitPlayer.getInventory().clear();
			arenaLoadout.equip(bukkitPlayer);
		}else {
			//TODO: Not sure?
		}
	}
	
	public UUID getUniqueID() {
		return id;
	}
	
	public void setSlot(SlotBoundaries slot) {
		this.slot = slot;
	}
	
	public SlotBoundaries getSlot() {
		return slot;
	}

	public int getLives() {
		return lives;
	}

	public void setLives(int lives) {
		this.lives = lives;
	}
}
