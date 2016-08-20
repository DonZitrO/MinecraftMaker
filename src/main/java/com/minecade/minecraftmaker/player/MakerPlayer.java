package com.minecade.minecraftmaker.player;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.scoreboard.Scoreboard;

import com.minecade.core.data.Rank;
import com.minecade.core.player.PlayerUtils;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.data.MakerSteveData;
import com.minecade.minecraftmaker.data.MakerUnlockable;
import com.minecade.minecraftmaker.inventory.AbstractMakerMenu;
import com.minecade.minecraftmaker.inventory.CheckTemplateOptionsMenu;
import com.minecade.minecraftmaker.inventory.LevelTemplatesMenu;
import com.minecade.minecraftmaker.inventory.EditLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.EditorPlayLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.GuestEditLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.LevelConfigMenu;
import com.minecade.minecraftmaker.inventory.LevelSearchMenu;
import com.minecade.minecraftmaker.inventory.LevelTimeMenu;
import com.minecade.minecraftmaker.inventory.LevelWeatherMenu;
import com.minecade.minecraftmaker.inventory.MenuClickResult;
import com.minecade.minecraftmaker.inventory.PlayLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.PlayerLevelsMenu;
import com.minecade.minecraftmaker.inventory.ServerBrowserMenu;
import com.minecade.minecraftmaker.inventory.SteveLevelClearOptionsMenu;
import com.minecade.minecraftmaker.inventory.SteveLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.ToolsSkullMenu;
import com.minecade.minecraftmaker.inventory.ToolsSkullTypeMenu;
import com.minecade.minecraftmaker.inventory.VipLevelToolsMenu;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.items.SkullTypeItem;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.level.PlayableLevelLimits;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.scoreboard.MakerScoreboard;
import com.minecade.minecraftmaker.util.Tickable;
import com.minecade.minecraftmaker.util.TickableUtils;
import com.minecade.nms.NMSUtils;

public class MakerPlayer implements Tickable {

	private static final int MIN_TIME_BETWEEN_SEARCHES_MILLIS = 3000;

	private final MinecraftMakerPlugin plugin;
	private final Player player;
	private final MakerPlayerData data;

	private MakerSteveData steveData;
	private MakerPlayableLevel currentLevel;

	private MakerScoreboard makerScoreboard;

	private final Map<String, AbstractMakerMenu> personalMenus = new HashMap<>();

	private final LinkedList<String> pendingActionMessages = new LinkedList<>();
	private long lastActionMessageTick;
	private AbstractMakerMenu inventoryToOpen;
	private boolean closeInventoryRequest;

	private boolean dirtyInventory;
	private long currentTick;
	private boolean disabled = false;

	//private Entity entityTeleportDestination;
	private Location teleportDestination;
	private GameMode requestedGameMode;

	private long levelToDeleteSerial;
	private long levelToUnpublishSerial;

	private long nextAllowedSearchMillis;

	public MakerPlayer(MinecraftMakerPlugin plugin, Player player, MakerPlayerData data) {
		this.plugin = plugin;
		this.player = player;
		this.data = data;
	} 

	public boolean canCreateLevel() {
		if (data.getUnpublishedLevelsCount() < PlayableLevelLimits.getRankUnpublishedLevelsLimit(data.getHighestRank())) {
			return true;
		}
		return false;
	}

	public boolean canPublishLevel() {
		if (data.getPublishedLevelsCount() < PlayableLevelLimits.getRankPublishedLevelsLimit(data.getHighestRank())) {
			return true;
		}
		return false;
	}

	public boolean canSearchAgain() {
		long currentTimeMillis = System.currentTimeMillis();
		if (nextAllowedSearchMillis < currentTimeMillis) {
			nextAllowedSearchMillis = currentTimeMillis + MIN_TIME_BETWEEN_SEARCHES_MILLIS;
			return true;
		} else {
			return false;
		}
	}

	public void clearInventory() {
		player.getInventory().clear();
		updateInventory();
	}

	public void closeInventory() {
		closeInventoryRequest = true;
	}

	@Override
	public void disable() {
		disabled = true;
		player.kickPlayer(plugin.getMessage("server.error.internal"));
	}

	private void executeRequestedInventoryOperations() {
		if (inventoryToOpen != null) {
			inventoryToOpen.open(player);
			inventoryToOpen = null;
			closeInventoryRequest = false;
			dirtyInventory = false;
			return;
		}
		if (closeInventoryRequest) {
			player.closeInventory();
			closeInventoryRequest = false;
			dirtyInventory = false;
			return;
		}
		if (dirtyInventory) {
			player.updateInventory();
			dirtyInventory = false;
		}
	}

	public MakerPlayableLevel getCurrentLevel() {
		return currentLevel;
	}

	public String getCurrentLevelBestClearData() {
		if (getCurrentLevel() == null || getCurrentLevel().getLevelBestClearData() == null) {
			return plugin.getMessage("player.no-time");
		}
		if (getCurrentLevel().getLevelBestClearData().getBestTimeCleared() > 0) {
			return getFormattedTime(getCurrentLevel().getLevelBestClearData().getBestTimeCleared());
		}
		return plugin.getMessage("player.no-time");
	}

	public String getCurrentLevelCurrentPlayerBestClearData() {
		if (getCurrentLevel() == null || getCurrentLevel().getCurrentPlayerBestClearData() == null) {
			return plugin.getMessage("player.no-time");
		}
		if (getCurrentLevel().getCurrentPlayerBestClearData().getBestTimeCleared() > 0) {
			return getFormattedTime(getCurrentLevel().getCurrentPlayerBestClearData().getBestTimeCleared());
		}
		return plugin.getMessage("player.no-time");
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	public MakerPlayerData getData() {
		return data;
	}

	@Override
	public String getDescription() {
		return String.format("MakerPlayer: [%s<%s>] with rank: [%s]", getName(), getUniqueId(), getDisplayRank());
	}

	public String getDisplayName() {
		String name = this.player.getName();
		StringBuilder displayName = new StringBuilder();
		if (!data.getDisplayRank().equals(Rank.GUEST)) {
			displayName.append(data.getDisplayRank().getDisplayName()).append(" ").append(name);
		} else {
			displayName.append(ChatColor.GRAY).append(name);
		}
		return displayName.toString();
	}

	public Rank getDisplayRank() {
		return getData().getDisplayRank();
	}

	private String getFormattedTime(long millis){
		return String.format("%sm %ss %sms",
				TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
				TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
				TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
	}

	public Rank getHighestRank() {
		return data.getHighestRank();
	}

	public String getLevelRecordUsername() {
		if (getCurrentLevel() == null || getCurrentLevel().getLevelBestClearData() == null) {
			return plugin.getMessage("general.empty");
		}
		if (getCurrentLevel().getLevelBestClearData().getPlayerName() != null) {
			return getCurrentLevel().getLevelBestClearData().getPlayerName();
		}
		return plugin.getMessage("general.empty");
	}

	public long getLevelToDeleteSerial() {
		return levelToDeleteSerial;
	}

	public long getLevelToUnpublishSerial() {
		return levelToUnpublishSerial;
	}

	public String getName() {
		return player.getName();
	}

	public Player getPlayer() {
		return this.player;
	}

	public int getPublishedLevelsCount() {
		return data.getPublishedLevelsCount();
	}

	public MakerSteveData getSteveData(){
		return this.steveData;
	}

	public UUID getUniqueId() {
		return this.player.getUniqueId();
	}

	public int getUnpublishedLevelsCount() {
		return data.getUnpublishedLevelsCount();
	}

	public boolean hasClearedLevel() {
		return this.currentLevel != null && LevelStatus.CLEARED.equals(this.currentLevel.getStatus());
	}

	public boolean hasInventoryToOpen() {
		return inventoryToOpen != null;
	}

	public boolean hasRank(Rank rank) {
		return getHighestRank().includes(rank);
	}

	public boolean hasUnlockable(MakerUnlockable unlockable) {
		return getData().hasUnlockable(unlockable);
	}

	public void initScoreboard(MinecraftMakerPlugin plugin) {
		if (makerScoreboard != null) {
			Bukkit.getLogger().warning(String.format("MakerPlayer.initScoreboard - scoreboard already initialized for player: %s", getDescription()));
			return;
		}
		makerScoreboard = new MakerScoreboard(plugin, this);
		makerScoreboard.init();
	}

	public boolean isAuthorEditingLevel() {
		return currentLevel != null && LevelStatus.EDITING.equals(currentLevel.getStatus()) && getUniqueId().equals(currentLevel.getAuthorId());
	}

	public boolean isCheckingTemplate() {
		return currentLevel != null && LevelStatus.CHECKING.equals(currentLevel.getStatus()) && getUniqueId().equals(currentLevel.getTemplateCheckerId());
	}

	@Override
	public boolean isDisabled() {
		return disabled;
	}

	public boolean isEditingLevel() {
		return isAuthorEditingLevel() || isGuestEditingLevel();
	}

	public boolean isGuestEditingLevel() {
		return currentLevel != null && LevelStatus.EDITING.equals(currentLevel.getStatus()) && currentLevel.isGuestEditor(getName());
	}

	public boolean isInBusyLevel() {
		return currentLevel != null && currentLevel.isBusy();
	}

	public boolean isInLobby() {
		return this.currentLevel == null && !isSpectating();
	}

	public boolean isInSteve() {
		return steveData != null;
	}

	public boolean isOnLevel() {
		return this.currentLevel != null;
	}

	public boolean isOnPublishedLevel() {
		return this.currentLevel != null && currentLevel.getDatePublished() != null;
	}

	public boolean isOnUnpublishedLevel() {
		return this.currentLevel != null && currentLevel.getDatePublished() == null;
	}

	public boolean isPlayingLevel() {
		return this.currentLevel != null && LevelStatus.PLAYING.equals(this.currentLevel.getStatus());
	}

	public boolean isSpectating() {
		return player.getGameMode().equals(GameMode.SPECTATOR);
	}

	public MenuClickResult onInventoryClick(Inventory inventory, int slot, ClickType clickType) {
		checkNotNull(inventory);
		AbstractMakerMenu menu = personalMenus.get(inventory.getTitle());
		if (menu != null) {
			return menu.onClick(this, slot, clickType);
		}
		return MenuClickResult.ALLOW;
	}

	public void onQuit() {
		// disable custom menus
		for (AbstractMakerMenu menu : personalMenus.values()) {
			menu.disable();
		}
		// disable scoreboard
		this.makerScoreboard.disable();
	}

	public void openCheckTemplateOptionsMenu() {
		openMakerInventory(CheckTemplateOptionsMenu.getInstance());
	}

	public void openConfigLevelMenu() {
		openMakerInventory(LevelConfigMenu.getInstance());
	}

	public void openEditLevelOptionsMenu() {
		openMakerInventory(EditLevelOptionsMenu.getInstance());
	}

	public void openEditorPlayLevelOptionsMenu() {
		openMakerInventory(EditorPlayLevelOptionsMenu.getInstance());
	}

	public void openGuestEditLevelOptionsMenu() {
		openMakerInventory(GuestEditLevelOptionsMenu.getInstance());
	}

	public void openLevelBrowserMenu() {
		LevelBrowserMenu menu = LevelBrowserMenu.getInstance(plugin, this.getUniqueId());
		menu.update();
		openMakerInventory(menu);
	}

	public void openLevelSearchMenu(String searchString, int totalResults, Collection<MakerDisplayableLevel> levelsPage) {
		LevelSearchMenu menu = LevelSearchMenu.getInstance(plugin, this.getUniqueId());
		menu.update(searchString, totalResults, levelsPage);
		openMakerInventory(menu);
	}

	public void openLevelTemplateMenu() {
		LevelTemplatesMenu menu = LevelTemplatesMenu.getInstance(plugin, this.getUniqueId());
		menu.update();
		openMakerInventory(menu);
	}

	public void openLevelTimeMenu() {
		LevelTimeMenu menu = LevelTimeMenu.getInstance(plugin, getUniqueId());
		menu.update();
		openMakerInventory(menu);
	}

	public void openLevelWeatherMenu() {
		LevelWeatherMenu menu = LevelWeatherMenu.getInstance(plugin, getUniqueId());
		menu.update();
		openMakerInventory(menu);
	}

	private void openMakerInventory(AbstractMakerMenu toOpen) {
		checkNotNull(toOpen);
		personalMenus.put(toOpen.getTitle(), toOpen);
		inventoryToOpen = toOpen;
	}

	public void openPlayerLevelsMenu(boolean update) {
		PlayerLevelsMenu menu = PlayerLevelsMenu.getInstance(plugin, this.getUniqueId());
		if (update) {
			menu.update();
		}
		openMakerInventory(menu);
	}

	public void openPlayLevelOptionsMenu() {
		openMakerInventory(PlayLevelOptionsMenu.getInstance());
	}

	public void openServerBrowserMenu() {
		openMakerInventory(ServerBrowserMenu.getInstance());
	}

	public void openSteveClearLevelOptionsMenu() {
		openMakerInventory(SteveLevelClearOptionsMenu.getInstance());
	}

	public void openSteveLevelOptionsMenu() {
		openMakerInventory(SteveLevelOptionsMenu.getInstance());
	}

	public void openToolsSkullMenu(SkullTypeItem skullTypeItem) {
		checkNotNull(skullTypeItem);
		openMakerInventory(ToolsSkullMenu.getInstance(skullTypeItem));
	}

	public void openToolsSkullTypeMenu() {
		openMakerInventory(ToolsSkullTypeMenu.getInstance());
	}

	public void openVIPLevelToolsMenu() {
		openMakerInventory(VipLevelToolsMenu.getInstance());
	}

	public void removeTeamEntryFromScoreboard(String playerName) {
		if (makerScoreboard != null && !makerScoreboard.isDisabled()) {
			makerScoreboard.removeEntryFromTeam(playerName);
		}
	}

	public void resetLevelSearchMenu() {
		LevelSearchMenu.getInstance(plugin, this.getUniqueId()).reset();
	}

	public void resetLobbyInventory() {
		// clear inventory
		player.getInventory().clear();
		// server browser
		player.getInventory().addItem(MakerLobbyItem.SERVER_BROWSER.getItem());
		player.getInventory().addItem(MakerLobbyItem.STEVE_CHALLENGE.getItem());
		player.getInventory().addItem(MakerLobbyItem.CREATE_LEVEL.getItem());
		player.getInventory().addItem(MakerLobbyItem.PLAYER_LEVELS.getItem());
		player.getInventory().addItem(MakerLobbyItem.LEVEL_BROWSER.getItem());
		player.getInventory().addItem(MakerLobbyItem.SPECTATE.getItem());
		// leave item
		player.getInventory().setItem(8, MakerLobbyItem.QUIT.getItem());
		player.getInventory().setHeldItemSlot(4);
		// update inventory on the next tick
		dirtyInventory = true;
	}

	public void resetPlayer() {
		resetPlayer(player.getGameMode());
	}

	public void resetPlayer(GameMode gamemode) {
		PlayerUtils.resetPlayer(getPlayer(), gamemode);
	}

	public void sendActionMessage(String key, Object... args) {
		if (!StringUtils.isEmpty(key)) {
			String message = plugin.getMessage(key, args);
			if (!message.equals(pendingActionMessages.peekFirst())) {
				pendingActionMessages.add(message);
			}
		}
	}

	public void sendMessage(String key, Object... args) {
		if (player.isOnline()) {
			if (StringUtils.isEmpty(key)) {
				player.sendMessage("");
			} else {
				player.sendMessage(plugin.getMessage(key, args));
			}
		}
	}

	private void sendPendingActionMessageIfAvailable() {
		if (!pendingActionMessages.isEmpty() && getCurrentTick() - lastActionMessageTick > 40) {
			NMSUtils.sendActionMessage(player, pendingActionMessages.pollFirst());
			lastActionMessageTick = getCurrentTick();
		}
	}

	public void sendTitleAndSubtitle(String title, String subtitle) {
		if (player.isOnline()) {
			NMSUtils.sendTitle(player, 10, 40, 10, title, subtitle);
		}
	}

	public void setAllowFlight(boolean allowFlight) {
		player.setAllowFlight(allowFlight);
	}

	public void setCoins(long balance) {
		getData().setCoins(balance);
	}

	public void setCurrentLevel(MakerPlayableLevel level) {
		//Bukkit.getLogger().severe(String.format("level: %s", level));
		//if (level != null) {
		//	Bukkit.getLogger().severe(String.format("level hash: %s", level.hashCode()));
		//}
		this.currentLevel = level;
	}

	public void setFireTicks(int fireTicks) {
		player.setFireTicks(fireTicks);
	}

	public void setFlying(boolean flying) {
		player.setAllowFlight(true);
		player.setFlying(flying);
	}

	public void setGameMode(GameMode mode) {
		player.setGameMode(mode);
	}

	public void setInvulnerable(boolean invulnerable) {
		if (player.isInvulnerable() == invulnerable) {
			return;
		}
		player.setInvulnerable(invulnerable);
	}

	public void setLevelToDeleteSerial(long levelToDeleteSerial) {
		this.levelToDeleteSerial = levelToDeleteSerial;
	}

	public void setLevelToUnpublishSerial(long levelToUnpublishSerial) {
		this.levelToUnpublishSerial = levelToUnpublishSerial;
	}

	public void setPublishedLevelsCount(int publishedCount) {
		data.setPublishedLevelsCount(publishedCount);
	}

	public void setScoreboard(Scoreboard scoreboard) {
		if (player.isOnline()) {
			player.setScoreboard(scoreboard);
		}
	}

	public void setSteveData(MakerSteveData steveData) {
		this.steveData = steveData;
	}

	public void setUnblishedLevelsCount(int unpublishedCount) {
		data.setUnpublishedLevelsCount(unpublishedCount);
	}

	public void setUniqueLevelClearsCount(long count) {
		getData().setUniqueLevelClearsCount(count);
	}

	public void spectate() {
		//setCurrentLevel(null);
		clearInventory();
		player.setGameMode(GameMode.SPECTATOR);
	}

	public boolean teleport(Location location, TeleportCause cause) {
		return player.teleport(location, cause);
	}

//	public void teleportOnNextTick(Entity destination, GameMode gameMode) {
//		entityTeleportDestination = destination;
//		requestedGameMode = gameMode;
//	}

	private void teleportIfRequested() {
//		if (entityTeleportDestination != null) {
//			teleportDestination = entityTeleportDestination.getLocation().clone();
//			entityTeleportDestination = null;
//		}
		if (teleportDestination != null) {
			if (player.teleport(teleportDestination, TeleportCause.PLUGIN)) {
				teleportDestination = null;
				if (requestedGameMode != null){
					player.setGameMode(requestedGameMode);
					requestedGameMode = null;
				}
			}
		}
	}

	public void teleportOnNextTick(Location destination) {
		this.teleportDestination = destination;
	}

	public void teleportOnNextTick(Location destination, GameMode gameMode) {
		teleportDestination = destination;
		requestedGameMode = gameMode;
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		// every tick tasks
		teleportIfRequested();
		executeRequestedInventoryOperations();
		sendPendingActionMessageIfAvailable();
		TickableUtils.tickSafely(makerScoreboard, currentTick);
	}

	public void updateInventory() {
		dirtyInventory = true;
	}

	public void updateScoreboardPlayerEntry(Rank rank, String playerName) {
		if (makerScoreboard != null && !makerScoreboard.isDisabled()) {
			makerScoreboard.addEntryToTeam(rank.name(), playerName);
		}
	}

}
