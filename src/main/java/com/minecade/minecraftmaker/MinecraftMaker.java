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
import com.minecade.minecraftmaker.bukkit.BukkitImplAdapter;
import com.minecade.minecraftmaker.cmd.LevelCommandExecutor;
import com.minecade.minecraftmaker.controller.MakerController;
import com.minecade.minecraftmaker.nms.schematic.Spigot_v1_9_R1;
import com.minecade.minecraftmaker.task.MakerBuilderTask;

public class MinecraftMaker extends JavaPlugin {

	private static MinecraftMaker instance;

	public static MinecraftMaker getInstance() {
		return instance;
	}

	MakerBase base;
	int maxPlayers = 32;

	private MakerController controller;
	private MakerBuilderTask builderTask;
	private BukkitImplAdapter bukkitImplAdapter;

	@Override
	public void onLoad() {
		instance = this;
		// default config
		saveDefaultConfig();
		// server custom config should override this
		getConfig().options().copyDefaults(false);
		try {
			this.bukkitImplAdapter = new Spigot_v1_9_R1();
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MinecraftMaker.onLoad - Unable to initialize specific Spigot version's NBT tags adapter - %s", e.getMessage()));
			e.printStackTrace();
			// this is an extreme case, so shut the server down
			Bukkit.shutdown();
		}
	}

	@Override
	public void onEnable() {
//		base = new MakerBase(this);
//		base.setupTranslations();
//		base.setPlayerRegistry(new MCMakerPlayerRegistry(this));
//		base.setNodeData(NodeDataFactory.newNodeData("MCMaker"));
//		base.getNodeData().getServerData().setCurrentPlayers(Bukkit.getOnlinePlayers().size());
//		base.getNodeData().getServerData().setMaxPlayers(maxPlayers);
//		base.setMaxPlayers(maxPlayers);
//		base.getNodeData().getServerData().setGameState(GameState.IN_LOBBY);
//		base.getNodeData().pushUpdate();
//		ServerBrowserDriver.setServerDataHolder(new MakerServerDataHolder(base));
//		getServer().getScheduler().runTaskTimer(this, new Runnable() {
//			
//			@Override
//			public void run() {
//				base.getNodeData().getServerData().setCurrentPlayers(Bukkit.getOnlinePlayers().size());
//				base.getNodeData().pushUpdate();
//				new GameStateUpdatePacket().dispatch();
//				new PlayerCountUpdatePacket().dispatch();
//				
//			}
//		}, 10*20, 10*20);
//		PluginManager pm = getServer().getPluginManager();
//		pm.registerEvents(new MinigameBasicListener(base), this);
//		pm.registerEvents(new MakerListener(base), this);
		// register commands
		getCommand("level").setExecutor(new LevelCommandExecutor(this));
		// instantiate and init main controller
		controller = new MakerController(this, getConfig().getConfigurationSection("controller"));
		controller.enable();
		// start builder task
		builderTask = new MakerBuilderTask(this);
		builderTask.runTaskTimer(this, 0, 0);
	}

	@Override
	public void onDisable() {
		if (controller != null) {
			controller.disable();
		}
		this.getServer().getScheduler().cancelTasks(this);
	}

	public MakerController getController() {
		return controller;
	}

	public boolean isDebugMode() {
		return getConfig().getBoolean("debug-mode", false);
	}

	public MakerBuilderTask getBuilderTask() {
		return builderTask;
	}

	public BukkitImplAdapter getBukkitImplAdapter() {
		return bukkitImplAdapter;
	}

}
