package com.minecade.minecraftmaker.player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.scoreboard.Scoreboard;

import com.minecade.core.data.Rank;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.player.PlayerUtils;
import com.minecade.minecraftmaker.data.MakerLevelClearData;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.data.MakerSteveData;
import com.minecade.minecraftmaker.inventory.AbstractMakerMenu;
import com.minecade.minecraftmaker.inventory.EditLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.EditorPlayLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.LevelSearchMenu;
import com.minecade.minecraftmaker.inventory.LevelTemplateMenu;
import com.minecade.minecraftmaker.inventory.LevelTimeMenu;
import com.minecade.minecraftmaker.inventory.LevelToolsMenu;
import com.minecade.minecraftmaker.inventory.LevelWeatherMenu;
import com.minecade.minecraftmaker.inventory.MenuClickResult;
import com.minecade.minecraftmaker.inventory.PlayLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.PlayerLevelsMenu;
import com.minecade.minecraftmaker.inventory.ServerBrowserMenu;
import com.minecade.minecraftmaker.inventory.SteveLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.ToolsSkullMenu;
import com.minecade.minecraftmaker.inventory.ToolsSkullTypeMenu;
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
	//private ChatColor nameColor = ChatColor.RESET;
	private final Map<String, AbstractMakerMenu> personalMenus = new HashMap<>();

	private final LinkedList<String> pendingActionMessages = new LinkedList<>();
	private long lastActionMessageTick;
	private AbstractMakerMenu inventoryToOpen;
	private boolean closeInventoryRequest;

	private boolean dirtyInventory;
	private long currentTick;
	private boolean disabled = false;

	private Location teleportDestination;

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

	public String getPlayerRecordTime(){
        if(this.currentLevel != null && this.currentLevel.getLevelsClear() != null){
            for(MakerLevelClearData makerLevelClear : this.currentLevel.getLevelsClear()){
                if(makerLevelClear.getUniqueId().equals(this.getUniqueId())){
                    return getFormattedTime(makerLevelClear.getTimeCleared());
                }
            }
        }

        return MinecraftMakerPlugin.getInstance().getMessage("player.no-time");
    }

	public int getPublishedLevelsCount() {
		return data.getPublishedLevelsCount();
	}

    public String getRecordTime(){
        if(this.currentLevel != null && this.currentLevel.getLevelsClear() != null &&
                this.currentLevel.getLevelsClear().size() > 0){
            MakerLevelClearData makerLevelClear = this.currentLevel.getLevelsClear().get(0);
            return getFormattedTime(makerLevelClear.getTimeCleared());
        }

        return MinecraftMakerPlugin.getInstance().getMessage("player.no-time");
    }

	public String getRecordUsername(){
        if(this.currentLevel != null && this.currentLevel.getLevelsClear() != null &&
                this.currentLevel.getLevelsClear().size() > 0){
            MakerLevelClearData makerLevelClear = this.currentLevel.getLevelsClear().get(0);
            return makerLevelClear.getUsername();
        }

        return MinecraftMakerPlugin.getInstance().getMessage("general.empty");
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

	public void initScoreboard(MinecraftMakerPlugin plugin) {
		if (makerScoreboard != null) {
			Bukkit.getLogger().warning(String.format("MakerPlayer.initScoreboard - scoreboard already initialized for player: %s", getDescription()));
			return;
		}
		makerScoreboard = new MakerScoreboard(plugin, this);
		makerScoreboard.init();
	}

	@Override
	public boolean isDisabled() {
		return disabled;
	}

	public boolean isEditingLevel() {
		return this.currentLevel != null && LevelStatus.EDITING.equals(this.currentLevel.getStatus());
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

	public MenuClickResult onInventoryClick(Inventory inventory, int slot) {
		if (personalMenus.containsKey(inventory.getName())) {
			return personalMenus.get(inventory.getName()).onClick(this, slot);
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

	public void openEditLevelOptionsMenu() {
		AbstractMakerMenu menu = personalMenus.get(EditLevelOptionsMenu.getInstance().getName());
		if (menu == null) {
			menu = EditLevelOptionsMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openEditorPlayLevelOptionsMenu() {
		AbstractMakerMenu menu = personalMenus.get(EditorPlayLevelOptionsMenu.getInstance().getName());
		if (menu == null) {
			menu = EditorPlayLevelOptionsMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openLevelBrowserMenu() {
		LevelBrowserMenu menu = (LevelBrowserMenu) personalMenus.get(plugin.getMessage(LevelBrowserMenu.getTitleKey()));
		if (menu == null) {
			menu = LevelBrowserMenu.getInstance(plugin, this.getUniqueId());
			personalMenus.put(menu.getName(), menu);
		}
		menu.update();
		inventoryToOpen = menu;
	}

	public void openLevelSearchMenu(Set<MakerDisplayableLevel> levels) {
		LevelSearchMenu menu = (LevelSearchMenu) personalMenus.get(plugin.getMessage(LevelSearchMenu.getTitleKey()));
		if (menu == null) {
			menu = LevelSearchMenu.getInstance(plugin, this.getUniqueId());
			personalMenus.put(menu.getName(), menu);
		}
		menu.update(levels);
		inventoryToOpen = menu;
	}

	public void openLevelTemplateMenu() {
		AbstractMakerMenu menu = personalMenus.get(LevelTemplateMenu.getInstance().getName());
		if (menu == null) {
			menu = LevelTemplateMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openLevelTimeMenu() {
		AbstractMakerMenu menu = personalMenus.get(LevelTimeMenu.getInstance().getName());
		if (menu == null) {
			menu = LevelTimeMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openLevelToolsMenu(){
		AbstractMakerMenu menu = personalMenus.get(LevelToolsMenu.getInstance().getName());
		if (menu == null) {
			menu = LevelToolsMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		
		inventoryToOpen = menu;
	}

	public void openLevelWeatherMenu() {
		AbstractMakerMenu menu = personalMenus.get(LevelWeatherMenu.getInstance().getName());
		if (menu == null) {
			menu = LevelWeatherMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openPlayerLevelsMenu(boolean update) {
		PlayerLevelsMenu menu = (PlayerLevelsMenu) personalMenus.get(plugin.getMessage(PlayerLevelsMenu.getTitleKey()));
		if (menu == null) {
			menu = PlayerLevelsMenu.getInstance(plugin, this.getUniqueId());
			personalMenus.put(menu.getName(), menu);
		}
		if (update) {
			menu.update();
		}
		inventoryToOpen = menu;
	}

	public void openPlayLevelOptionsMenu() {
		AbstractMakerMenu menu = personalMenus.get(PlayLevelOptionsMenu.getInstance().getName());
		if (menu == null) {
			menu = PlayLevelOptionsMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openServerBrowserMenu() {
		AbstractMakerMenu menu = personalMenus.get(ServerBrowserMenu.getInstance().getName());
		if (menu == null) {
			menu = ServerBrowserMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openSteveLevelOptionsMenu() {
		AbstractMakerMenu menu = personalMenus.get(SteveLevelOptionsMenu.getInstance().getName());
		if (menu == null) {
			menu = SteveLevelOptionsMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openToolsSkullMenu(SkullTypeItem skullTypeItem) {
		AbstractMakerMenu menu = personalMenus.get(ToolsSkullMenu.getInstance(skullTypeItem).getName());
		if (menu == null) {
			menu = ToolsSkullMenu.getInstance(skullTypeItem);
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void openToolsSkullTypeMenu() {
		AbstractMakerMenu menu = personalMenus.get(ToolsSkullTypeMenu.getInstance().getName());
		if (menu == null) {
			menu = ToolsSkullTypeMenu.getInstance();
			personalMenus.put(menu.getName(), menu);
		}
		inventoryToOpen = menu;
	}

	public void removeTeamEntryFromScoreboard(String playerName) {
		if (makerScoreboard != null && !makerScoreboard.isDisabled()) {
			makerScoreboard.removeEntryFromTeam(playerName);
		}
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

	public void sendActionMessage(Internationalizable plugin, String key, Object... args) {
		if (!StringUtils.isEmpty(key)) {
			String message = plugin.getMessage(key, args);
			if (!message.equals(pendingActionMessages.peekFirst())) {
				pendingActionMessages.add(message);
			}
		}
	}

	public void sendMessage(Internationalizable plugin, String key, Object... args) {
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

	public void spectate() {
		clearInventory();
		player.setGameMode(GameMode.SPECTATOR);
	}

	public boolean teleport(Location location, TeleportCause cause) {
		return player.teleport(location, cause);
	}

	private void teleportIfRequested() {
		if (teleportDestination != null) {
			if (player.teleport(teleportDestination, TeleportCause.PLUGIN)) {
				teleportDestination = null;
			}
		}
	}

	public void teleportOnNextTick(Location destination) {
		this.teleportDestination = destination;
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
