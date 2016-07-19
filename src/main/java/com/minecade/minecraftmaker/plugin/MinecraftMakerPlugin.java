package com.minecade.minecraftmaker.plugin;

import java.util.Locale;
import java.util.ResourceBundle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.minecade.core.config.ServerPropertyFilesConfigurator;
import com.minecade.core.data.Rank;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.util.BungeeUtils;
import com.minecade.core.util.EmptyGenerator;
import com.minecade.minecraftmaker.cmd.LevelCommandExecutor;
import com.minecade.minecraftmaker.cmd.MakerCommandExecutor;
import com.minecade.minecraftmaker.cmd.MakerLobbyCommandExecutor;
import com.minecade.minecraftmaker.cmd.ReportCommandExecutor;
import com.minecade.minecraftmaker.controller.MakerController;
import com.minecade.minecraftmaker.data.MakerDatabaseAdapter;
import com.minecade.minecraftmaker.items.EditLevelOptionItem;
import com.minecade.minecraftmaker.items.EditorPlayLevelOptionItem;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.GuestEditLevelOptionItem;
import com.minecade.minecraftmaker.items.LevelTemplateItem;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.items.PlayLevelOptionItem;
import com.minecade.minecraftmaker.items.SkullItem;
import com.minecade.minecraftmaker.items.SkullTypeItem;
import com.minecade.minecraftmaker.items.SteveLevelClearOptionItem;
import com.minecade.minecraftmaker.items.SteveLevelOptionItem;
import com.minecade.minecraftmaker.items.TimeItem;
import com.minecade.minecraftmaker.items.WeatherItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.listener.MakerListener;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitImplAdapter;
import com.minecade.minecraftmaker.task.AsyncLevelBrowserUpdaterTask;
import com.minecade.minecraftmaker.task.AsyncLevelSaverTask;
import com.minecade.minecraftmaker.task.AsyncPlayerCounterUpdaterTask;
import com.minecade.minecraftmaker.task.LevelOperatorTask;
import com.minecade.nms.Spigot_v1_9_R2;

public class MinecraftMakerPlugin extends JavaPlugin implements Internationalizable {

	private static MinecraftMakerPlugin instance;

	public static MinecraftMakerPlugin getInstance() {
		return instance;
	}

	private final ChunkGenerator emptyGenerator = new EmptyGenerator();

	private MakerDatabaseAdapter databaseAdapter;
	private MakerController controller;
	private AsyncLevelSaverTask asyncLevelSaver;
	private AsyncLevelBrowserUpdaterTask asyncLevelBrowserUpdater;
	private LevelOperatorTask levelOperatorTask;
	private BukkitImplAdapter bukkitImplAdapter;
	private ResourceBundle messages;

	private boolean debugMode;

	public LevelOperatorTask getLevelOperatorTask() {
		return levelOperatorTask;
	}

	public BukkitImplAdapter getBukkitImplAdapter() {
		return bukkitImplAdapter;
	}

	public AsyncLevelBrowserUpdaterTask getAsyncLevelBrowserUpdater() {
		return asyncLevelBrowserUpdater;
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
		return debugMode; 
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
		getCommand("maker").setExecutor(new MakerCommandExecutor(this));
		getCommand("report").setExecutor(new ReportCommandExecutor(this));
		getCommand("makerlobby").setExecutor(new MakerLobbyCommandExecutor(this));
		databaseAdapter = new MakerDatabaseAdapter(this);
		// async player data saver
		asyncLevelSaver = new AsyncLevelSaverTask(this);
		asyncLevelSaver.runTaskTimerAsynchronously(this, 0, 0);
		// asyc level browser updater
		asyncLevelBrowserUpdater = new AsyncLevelBrowserUpdaterTask(this);
		asyncLevelBrowserUpdater.runTaskTimerAsynchronously(this, 0, 0);
		// instantiate and init main controller
		controller = new MakerController(this, getConfig().getConfigurationSection("controller"));
		controller.init();
		// load the first page of the server browser
		// controller.requestLevelPageUpdate(LevelSortBy.TRENDING_SCORE, LevelSortBy.TRENDING_SCORE.isReversedDefault(), 1, null);
		// start builder task
		levelOperatorTask = new LevelOperatorTask(this);
		levelOperatorTask.runTaskTimer(this, 0, 0);
		// register listeners
		getServer().getPluginManager().registerEvents(new MakerListener(this), this);
		// TODO: remove this after fixed
		if (getConfig().getBoolean("fix-trending-scores", false)) {
			databaseAdapter.fixTrendingScoresAsync();
		}
		// TODO: remove this after rabbit
		if (getServerId() > 100) {
			return;
		}
		new AsyncPlayerCounterUpdaterTask(this).runTaskTimerAsynchronously(this, 100L, 100L);
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
		//ServerPropertyFilesConfigurator.configurePermissionsYML();
		// server specific config
		getServer().setDefaultGameMode(GameMode.ADVENTURE);
		try {
			this.bukkitImplAdapter = new Spigot_v1_9_R2();
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MinecraftMakerPlugin.onLoad - Unable to initialize specific Spigot version's NBT tags adapter - %s", e.getMessage()));
			e.printStackTrace();
			// this is an extreme case, so shut the server down
			Bukkit.shutdown();
		}
		// i18n config
		messages = ResourceBundle.getBundle("messages", new Locale(getConfig().getString("locale", "en")));
		// translate
		translateGeneralStuff();
		// debug mode
		debugMode = getConfig().getBoolean("debug-mode", false);
	}

	public void saveLevelAsync(MakerPlayableLevel level) {
		asyncLevelSaver.saveLevelAsync(level);
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
		// translate edit level option menu items
		for (EditLevelOptionItem item : EditLevelOptionItem.values()) {
			item.translate(this);
		}
		// translate guest edit level option menu items
		for (GuestEditLevelOptionItem item : GuestEditLevelOptionItem.values()) {
			item.translate(this);
		}
		// translate editor play level option menu items
		for (EditorPlayLevelOptionItem item : EditorPlayLevelOptionItem.values()) {
			item.translate(this);
		}
		// translate play level option menu items
		for (PlayLevelOptionItem item : PlayLevelOptionItem.values()) {
			item.translate(this);
		}
		// translate steve level option menu items
		for (SteveLevelOptionItem item : SteveLevelOptionItem.values()) {
			item.translate(this);
		}
		// translate steve level clear option menu items
		for (SteveLevelClearOptionItem item : SteveLevelClearOptionItem.values()) {
			item.translate(this);
		}
		// translate level order by options
		for (LevelSortBy sortBy : LevelSortBy.values()) {
			sortBy.translate(this);
		}
		// translate level tools options
		for (LevelToolsItem item : LevelToolsItem.values()) {
			item.translate(this);
		}
		// translate skull items
		for (SkullItem item : SkullItem.values()) {
			item.translate(this);
		}
		// translate skull type items
		for (SkullTypeItem item : SkullTypeItem.values()) {
			item.translate(this);
		}
		// translate time items
		for (TimeItem item : TimeItem.values()) {
			item.translate(this);
		}
		// translate weather items
		for (WeatherItem item : WeatherItem.values()) {
			item.translate(this);
		}
	}

}
