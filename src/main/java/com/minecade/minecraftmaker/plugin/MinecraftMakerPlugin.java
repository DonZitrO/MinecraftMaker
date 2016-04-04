package com.minecade.minecraftmaker.plugin;

import java.util.Locale;
import java.util.ResourceBundle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.minecade.core.config.ServerPropertyFilesConfigurator;
import com.minecade.core.data.Rank;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.util.BungeeUtils;
import com.minecade.core.util.EmptyGenerator;
import com.minecade.minecraftmaker.cmd.LevelCommandExecutor;
import com.minecade.minecraftmaker.controller.MakerController;
import com.minecade.minecraftmaker.data.MakerDatabaseAdapter;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelTemplateItem;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.listener.MakerListener;
import com.minecade.minecraftmaker.nms.schematic.Spigot_v1_9_R1;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitImplAdapter;
import com.minecade.minecraftmaker.task.MakerBuilderTask;

public class MinecraftMakerPlugin extends JavaPlugin implements Internationalizable {

	private static MinecraftMakerPlugin instance;

	public static MinecraftMakerPlugin getInstance() {
		return instance;
	}

	private final ChunkGenerator emptyGenerator = new EmptyGenerator();

	private MakerDatabaseAdapter databaseAdapter;
	private MakerController controller;
	private MakerBuilderTask builderTask;
	private BukkitImplAdapter bukkitImplAdapter;
	private ResourceBundle messages;

	public MakerBuilderTask getBuilderTask() {
		return builderTask;
	}

	public BukkitImplAdapter getBukkitImplAdapter() {
		return bukkitImplAdapter;
	}

	public MakerController getController() {
		return controller;
	}

	public MakerDatabaseAdapter getDatabaseAdapter() {
		return databaseAdapter;
	}

	@Override
	public ChunkGenerator getDefaultWorldGenerator(String world, String id) {
		return emptyGenerator;
	}

	@Override
	public String getMessage(String key, Object... args) {
		if (messages.containsKey(key)) {
			return String.format(ChatColor.translateAlternateColorCodes('&', messages.getString(key)), args);
		}
		return key;
	}

	public int getServerId() {
		return getConfig().getInt("server.id", 0);
	}

	public boolean isDebugMode() {
		return getConfig().getBoolean("debug-mode", false);
	}

	@Override
	public void onDisable() {
		if (controller != null) {
			controller.disable();
		}
		this.getServer().getScheduler().cancelTasks(this);
	}

	@Override
	public void onEnable() {
		// BungeeCord communication
		getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeUtils.BUNGEECORD_CHANNEL);
		// register commands
		getCommand("level").setExecutor(new LevelCommandExecutor(this));
		databaseAdapter = new MakerDatabaseAdapter(this);
		// instantiate and init main controller
		controller = new MakerController(this, getConfig().getConfigurationSection("controller"));
		controller.enable();
		// start builder task
		builderTask = new MakerBuilderTask(this);
		builderTask.runTaskTimer(this, 0, 0);
		// register listeners
		getServer().getPluginManager().registerEvents(new MakerListener(this), this);
	}

	@Override
	public void onLoad() {
		instance = this;
		// default config
		saveDefaultConfig();
		// server custom config should override this
		getConfig().options().copyDefaults(false);
		// configure server files first and reboot if necessary
		ServerPropertyFilesConfigurator.configureServerProperties();
		ServerPropertyFilesConfigurator.configureBukkitYML();
		ServerPropertyFilesConfigurator.configureSpigotYML();
		try {
			this.bukkitImplAdapter = new Spigot_v1_9_R1();
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MinecraftMaker.onLoad - Unable to initialize specific Spigot version's NBT tags adapter - %s", e.getMessage()));
			e.printStackTrace();
			// this is an extreme case, so shut the server down
			Bukkit.shutdown();
		}
		// i18n config
		messages = ResourceBundle.getBundle("text", new Locale(getConfig().getString("locale", "en")));
		// translate
		translateGeneralStuff();
	}

	private void translateGeneralStuff() {
		// translate ranks
		for (Rank rank : Rank.values()) {
			rank.translate(this);
		}
		// translate lobby items
		for (MakerLobbyItem item : MakerLobbyItem.values()) {
			item.translate(this);
		}
		// translate level template items
		for (LevelTemplateItem item : LevelTemplateItem.values()) {
			item.translate(this);
		}
		// translate general menu items
		for (GeneralMenuItem item : GeneralMenuItem.values()) {
			item.translate(this);
		}
	}

}
