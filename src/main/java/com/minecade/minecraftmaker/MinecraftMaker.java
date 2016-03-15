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
import com.minecade.minecraftmaker.cmd.LevelCommandExecutor;
import com.minecade.minecraftmaker.controller.MakerController;

public class MinecraftMaker extends JavaPlugin {
	
	MakerBase base;
	int maxPlayers = 32;

	private MakerController controller;

	@Override
	public void onDisable() {
		if (controller != null) {
			controller.disable();
		}
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
		pm.registerEvents(new MakerListener(base), this);
		// register commands
		getCommand("level").setExecutor(new LevelCommandExecutor());
		// instantiate and init main controller
		controller = new MakerController(this, getConfig().getConfigurationSection("controller"));
		controller.enable();
	}

	public MakerController getController() {
		return controller;
	}

}
