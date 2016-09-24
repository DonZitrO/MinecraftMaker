package com.minecade.minecraftmaker.plugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import com.minecade.mcore.cmd.GameLobbyCommandExecutor;
import com.minecade.mcore.cmd.ReportCommandExecutor;
import com.minecade.mcore.config.ServerPropertyFilesConfigurator;
import com.minecade.mcore.data.Rank;
import com.minecade.mcore.item.CommonMenuItem;
import com.minecade.mcore.nmsapi.NMS;
import com.minecade.mcore.plugin.MPlugin;
import com.minecade.minecraftmaker.cmd.LevelCommandExecutor;
import com.minecade.minecraftmaker.cmd.MakerCommandExecutor;
import com.minecade.minecraftmaker.cmd.MakerTestCommandExecutor;
import com.minecade.minecraftmaker.cmd.UnlockCommandExecutor;
import com.minecade.minecraftmaker.controller.MakerController;
import com.minecade.minecraftmaker.data.MakerDatabaseAdapter;
import com.minecade.minecraftmaker.items.CheckTemplateOptionItem;
import com.minecade.minecraftmaker.items.EditLevelOptionItem;
import com.minecade.minecraftmaker.items.EditorPlayLevelOptionItem;
import com.minecade.minecraftmaker.items.MakerMenuItem;
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
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.task.AnnouncerTask;
import com.minecade.minecraftmaker.task.AsyncLevelBrowserUpdaterTask;
import com.minecade.minecraftmaker.task.AsyncLevelSaverTask;
import com.minecade.minecraftmaker.task.AsyncPlayerCounterUpdaterTask;
import com.minecade.minecraftmaker.task.LevelOperatorTask;
import com.minecade.nms.NMS_Spigot_v1_9_R2;

public class MinecraftMakerPlugin extends MPlugin<MakerController, MakerPlayer> {

	private static MinecraftMakerPlugin instance;

	public static MinecraftMakerPlugin getInstance() {
		return instance;
	}

	private MakerDatabaseAdapter databaseAdapter;
	private MakerController controller;
	private AsyncLevelSaverTask asyncLevelSaver;
	private AsyncLevelBrowserUpdaterTask asyncLevelBrowserUpdater;
	private LevelOperatorTask levelOperatorTask;

	public AsyncLevelBrowserUpdaterTask getAsyncLevelBrowserUpdater() {
		return asyncLevelBrowserUpdater;
	}

	@Override
	public final MakerController getController() {
		return controller;
	}

	@Override
	public MakerDatabaseAdapter getDatabaseAdapter() {
		return databaseAdapter;
	}


	
	public LevelOperatorTask getLevelOperatorTask() {
		return levelOperatorTask;
	}

	@Override
	protected void onMPluginDisable() {
		if (controller != null) {
			controller.disable();
		}
	}

	@Override
	public void onMPluginEnable() {
		// register commands
		getCommand("level").setExecutor(new LevelCommandExecutor(this));
		getCommand("maker").setExecutor(new MakerCommandExecutor(this));
		getCommand("report").setExecutor(new ReportCommandExecutor(this));
		getCommand("makerlobby").setExecutor(new GameLobbyCommandExecutor(this));
		getCommand("makertest").setExecutor(new MakerTestCommandExecutor(this));
		getCommand("unlock").setExecutor(new UnlockCommandExecutor(this));
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
		// announcer
		AnnouncerTask announcer = new AnnouncerTask(this);
		announcer.init();
		announcer.runTaskTimer(this, 2400L, 2400L);
		// register listeners
		getServer().getPluginManager().registerEvents(new MakerListener(this), this);
		// load level templates
		databaseAdapter.loadLevelTemplatesAsync();
		// TODO: remove this after rabbit
		if (getServerBungeeId() > 100) {
			return;
		}
		new AsyncPlayerCounterUpdaterTask(this).runTaskTimerAsynchronously(this, 100L, 100L);
	}

	@Override
	public void onMPluginLoad() {
		instance = this;
		// configure server files first and reboot if necessary
		ServerPropertyFilesConfigurator.configureServerProperties();
		ServerPropertyFilesConfigurator.configureBukkitYML();
		ServerPropertyFilesConfigurator.configureSpigotYML();
		//ServerPropertyFilesConfigurator.configurePermissionsYML();
		// server specific config
		getServer().setDefaultGameMode(GameMode.ADVENTURE);
		// translate
		translateGeneralStuff();
	}

	public void saveLevelAsync(MakerPlayableLevel level) {
		asyncLevelSaver.saveLevelAsync(level);
	}

	private void translateGeneralStuff() {
		// translate ranks
		for (Rank rank : Rank.values()) {
			rank.translate(this);
		}
		// translate common menu items
		for (CommonMenuItem item: CommonMenuItem.values()) {
			item.translate(this);
		}
		// translate maker general menu items
		for (MakerMenuItem item : MakerMenuItem.values()) {
			item.translate(this);
		}
		// translate lobby items
		for (MakerLobbyItem item : MakerLobbyItem.values()) {
			item.translate(this);
		}
		// translate level template items
		for (LevelTemplateItem item : LevelTemplateItem.values()) {
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
		// translate check template option items
		for (CheckTemplateOptionItem item : CheckTemplateOptionItem.values()) {
			item.translate(this);
		}
	}

	@Override
	protected void setupNMSAdapter() {
		try {
			NMS.setAdapter(new NMS_Spigot_v1_9_R2());
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MinecraftMakerPlugin.setupNMSAdapter - unable to setup NSM adapter - shutting down server - %s", e.getMessage()));
			e.printStackTrace();
			// this is an extreme case, so shut the server down
			Bukkit.shutdown();
		}
	}

}
