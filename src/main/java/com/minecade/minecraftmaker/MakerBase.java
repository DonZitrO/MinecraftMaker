package com.minecade.minecraftmaker;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.minecade.core.MinecadeCore;
import com.minecade.core.gamebase.MinigameBase;
import com.minecade.core.gamebase.MinigamePlayer;
import com.minecade.core.gamebase.MinigameSetup;
import com.minecade.core.item.common.ReturnToLobbyItem;
import com.minecade.core.item.common.ServerBrowserItem;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerBase extends MinigameBase {
	
	MinecraftMakerPlugin mplugin;
	ConcurrentHashMap<UUID, ConcurrentHashMap<String, ArenaDefinition>> maps = new ConcurrentHashMap<UUID, ConcurrentHashMap<String, ArenaDefinition>>();
	ConcurrentHashMap<UUID, MakerArena> runningArenas = new ConcurrentHashMap<UUID, MakerArena>();
	
	ArrayList<SlotBoundaries> openSlots = new ArrayList<SlotBoundaries>();
	int nextSlotID = 0;

	private ReturnToLobbyItem lobbyItem;
	private ServerBrowserItem serverBrowserItem;
	
	private World arenaWorld = null;
	
	private static MakerBase instance;

	public MakerBase(MinecraftMakerPlugin plugin) {
		super(plugin);
		mplugin = plugin;
		instance = this;
		lobbyItem = new ReturnToLobbyItem();
		lobbyItem.getBuilder().setTitle(getText("lobby.item.titleleave"));
		lobbyItem.getBuilder().clearLore().addLore(getText("lobby.item.leave"));
		serverBrowserItem = new ServerBrowserItem();
		serverBrowserItem.getBuilder().setType(Material.WATCH);
		serverBrowserItem.getBuilder().setTitle(getText("lobby.item.serverbrowser"));
		MinecadeCore.getActiveItemRegistry().registerItem(lobbyItem);
		MinecadeCore.getActiveItemRegistry().registerItem(serverBrowserItem);
	}
	
	public static MakerBase getMakerBase() {
		return instance;
	}

	@Override
	public String getMetadataArenaString() {
		return "MakerPlayer";
	}

	@Override
	public void addMinigamePlayer(Player player) {
		MakerPlayer fplayer = new MakerPlayer(player, MinigamePlayer.Type.Player, MakerPlayer.MakerType.Lobby);
		MinecadeCore.getCoreDatabase().requestDataLoad(fplayer, false);
		addMinigamePlayer(fplayer);
	}

	@Override
	public String getPluginPrefix() {
		return null;
	}

	@Override
	public String getCommandPrefix() {
		return "maker";
	}

	@Override
	public String getBaseAdminPermission() {
		return "mcmaker.admin";
	}

	@Override
	public MinigameSetup getNewMinigameArenaSetup(Player player) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveLobby() {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveArenas() {
		// TODO Auto-generated method stub

	}
	
	public ArenaDefinition getArenaDefinition(UUID maker, String name) {
		ArenaDefinition arenaDef = null;
		ConcurrentHashMap<String, ArenaDefinition> arenas = maps.get(maker);
		if(arenas != null) {
			arenaDef = arenas.get(name);
		}
		return arenaDef;
	}
	
	public void saveArenaDefinition(ArenaDefinition arenaDef) {
		ConcurrentHashMap<String, ArenaDefinition> arenas = maps.get(arenaDef.getAuthorUUID());
		if(arenas == null) {
			arenas = new ConcurrentHashMap<String, ArenaDefinition>();
			maps.put(arenaDef.getAuthorUUID(), arenas);
		}
		arenas.put(arenaDef.getName(), arenaDef);
		//TODO: save to mysql
		
	}

	@Override
	public MakerLobby getLobby() {
		return (MakerLobby)super.getLobby();
	}

	public ReturnToLobbyItem getLobbyItem() {
		return lobbyItem;
	}

	public ServerBrowserItem getServerBrowserItem() {
		return serverBrowserItem;
	}

	public void loadArenaSchematic(MakerArena makerArena, ArenaDefinition arenaDef) {
		
	}
	
	public void addRunningArena(MakerArena arena) {
		runningArenas.put(arena.getUniqueID(), arena);
	}
	
	public void removeArena(MakerArena arena) {
		runningArenas.remove(arena.getUniqueID());
		if(arena.getSlot() != null) 
			openSlots.add(arena.getSlot());
	}
	
	public MakerArena getArena(UUID id) {
		return runningArenas.get(id);
	}
	
	/**
	 * Gets the next open slot, removing it from the list
	 * of available slots. This function dynamically
	 * generates more slots as needed.
	 * @return
	 */
	public SlotBoundaries getOpenSlot() {
		if(openSlots.size() > 0) {
			return openSlots.remove(0);
		}else {
			return new SlotBoundaries(nextSlotID++);
		}
	}

	public World getArenaWorld() {
		return arenaWorld;
	}

	public void setArenaWorld(World arenaWorld) {
		this.arenaWorld = arenaWorld;
	}
	
	
}
