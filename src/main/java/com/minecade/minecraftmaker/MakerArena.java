package com.minecade.minecraftmaker;

import java.util.ArrayList;
import java.util.UUID;

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
import com.minecade.serverweb.shared.constants.GameState;
import com.sk89q.worldedit.event.platform.BlockInteractEvent;
import com.sk89q.worldedit.event.platform.Interaction;

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
	
	private static final int MAX_CHUNKS = 10;

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
				if(--lives < 1) {
					endGame();
				}else {
					//Reset player
					pasteSchematic();
				}
			}
		}
	}
	
	public void onBlockPlace(BlockPlaceEvent event) {
		//Don't let them build outside of their arena
		int z = event.getBlock().getZ();
		if(z < slot.getZ() || z > slot.getZ() + 9) {
			event.setCancelled(true);
			return;
		}
		int x = event.getBlock().getX();
		if(x < slot.getX() || x > 16*MAX_CHUNKS + slot.getX()) {
			event.setCancelled(true);
		}

	}
	
	public void onBlockBreak(BlockBreakEvent event) {
		//Don't let them break anything outside of their arena
		int z = event.getBlock().getZ();
		if(z < slot.getZ() || z > slot.getZ() + 9) {
			event.setCancelled(true);
			return;
		}
		int x = event.getBlock().getX();
		if(x < slot.getX() || x > 16*MAX_CHUNKS + slot.getX()) {
			event.setCancelled(true);
		}
	}
	
	public void onBlockInteract(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		//Don't let them interact with anything outside of their arena
		if(block != null) {
			int z = block.getZ();
			if(z < slot.getZ() || z > slot.getZ() + 9) {
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
