package com.minecade.minecraftmaker;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.minecade.core.MinecadeCore;
import com.minecade.core.gamebase.MinigameBase;
import com.minecade.core.gamebase.MinigamePlayer;
import com.minecade.core.gamebase.MinigameSetup;
import com.minecade.core.item.common.ReturnToLobbyItem;
import com.minecade.core.item.common.ServerBrowserItem;

public class MakerBase extends MinigameBase {
	
	MinecraftMaker mplugin;
	ConcurrentHashMap<UUID, ConcurrentHashMap<String, ArenaDefinition>> maps = new ConcurrentHashMap<UUID, ConcurrentHashMap<String, ArenaDefinition>>();

	private ReturnToLobbyItem lobbyItem;
	private ServerBrowserItem serverBrowserItem;

	public MakerBase(MinecraftMaker plugin) {
		super(plugin);
		mplugin = plugin;
		lobbyItem = new ReturnToLobbyItem();
		lobbyItem.getBuilder().setTitle(getText("lobby.item.titleleave"));
		lobbyItem.getBuilder().clearLore().addLore(getText("lobby.item.leave"));
		serverBrowserItem = new ServerBrowserItem();
		serverBrowserItem.getBuilder().setType(Material.WATCH);
		serverBrowserItem.getBuilder().setTitle(getText("lobby.item.serverbrowser"));
		MinecadeCore.getActiveItemRegistry().registerItem(lobbyItem);
		MinecadeCore.getActiveItemRegistry().registerItem(serverBrowserItem);
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

}
