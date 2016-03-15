package com.minecade.minecraftmaker;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.minecade.core.gamebase.MinigameBasicListener;
import com.minecade.core.serverbrowser.ServerBrowserDriver;
import com.minecade.core.serverbrowser.packets.GameStateUpdatePacket;
import com.minecade.core.serverbrowser.packets.PlayerCountUpdatePacket;
import com.minecade.core.serverweb.NodeDataFactory;
import com.minecade.serverweb.shared.constants.GameState;

public class MinecraftMaker extends JavaPlugin {
	
	MakerBase base;
	int maxPlayers = 32;

	@Override
	public void onDisable() {
		
	}

	@Override
	public void onEnable() {
		base = new MakerBase(this);
		base.setupTranslations();
		base.setPlayerRegistry(new MCMakerPlayerRegistry(this));
		base.setNodeData(NodeDataFactory.newNodeData("MCMaker"));
		base.getNodeData().getServerData().setCurrentPlayers(Bukkit.getOnlinePlayers().size());
		base.getNodeData().getServerData().setMaxPlayers(maxPlayers);
		base.setMaxPlayers(maxPlayers);
		base.getNodeData().getServerData().setGameState(GameState.IN_LOBBY);
		base.getNodeData().pushUpdate();
		ServerBrowserDriver.setServerDataHolder(new MakerServerDataHolder(base));
		getServer().getScheduler().runTaskTimer(this, new Runnable() {
			
			@Override
			public void run() {
				base.getNodeData().getServerData().setCurrentPlayers(Bukkit.getOnlinePlayers().size());
				base.getNodeData().pushUpdate();
				new GameStateUpdatePacket().dispatch();
				new PlayerCountUpdatePacket().dispatch();
				
			}
		}, 10*20, 10*20);
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new MinigameBasicListener(base), this);
	}

}
