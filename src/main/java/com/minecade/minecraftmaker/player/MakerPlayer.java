package com.minecade.minecraftmaker.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;

import com.minecade.core.data.Rank;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.player.PlayerUtils;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.inventory.AbstractMakerMenu;
import com.minecade.minecraftmaker.inventory.EditLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.EditorPlayLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.LevelTemplateMenu;
import com.minecade.minecraftmaker.inventory.MenuClickResult;
import com.minecade.minecraftmaker.inventory.PlayLevelOptionsMenu;
import com.minecade.minecraftmaker.inventory.PlayerLevelsMenu;
import com.minecade.minecraftmaker.inventory.ServerBrowserMenu;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.util.Tickable;
import com.minecade.nms.NMSUtils;

public class MakerPlayer implements Tickable {

	private final Player player;
	private final MakerPlayerData data;

	private MakerPlayableLevel currentLevel;

	private ChatColor nameColor = ChatColor.RESET;
	private final Map<String, AbstractMakerMenu> personalMenus = new HashMap<>();

	private AbstractMakerMenu inventoryToOpen;
	private boolean closeInventoryRequest;
	private boolean dirtyInventory;

	private long currentTick;
	private boolean disabled = false;
	private Location teleportDestination;


	public MakerPlayer(Player player, MakerPlayerData data) {
		this.player = player;
		this.data = data;
	}

	public void cancelPendingOperation() {
		// TODO keep track of all operations related to this player and cancel
		// them.
	}

	public void clearInventory() {
		player.getInventory().clear();
		updateInventory();
	}

	public void closeInventory() {
		closeInventoryRequest = true;
	}

	@Override
	public void disable(String reason, Exception exception) {
		Bukkit.getLogger().warning(String.format("MakerPlayer.disable - disable request for player: [%s<%s>]", getName(), getUniqueId()));
		if (reason != null) {
			Bukkit.getLogger().warning(String.format("MakerPlayer.disable - reason: %s", reason));
		}
		StackTraceElement[] stackTrace = exception != null ? exception.getStackTrace() : Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			Bukkit.getLogger().warning(String.format("MakerPlayer.disable - stack trace: %s", element));
		}
		if (isDisabled()) {
			return;
		}
		// TODO: player disable logic (kick, probably)
		disabled = true;
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

	public String getDisplayName() {
		String name = this.player.getName();
		StringBuilder displayName = new StringBuilder();
		if (data.getDisplayRank() != Rank.GUEST) {
			displayName.append(data.getDisplayRank().getDisplayName()).append(ChatColor.RESET).append(" ").append(this.nameColor).append(name);
		} else {
			displayName.append(ChatColor.GRAY).append(name);
		}
		return displayName.toString();
	}

	public String getName() {
		return player.getName();
	}

	public Player getPlayer() {
		return this.player;
	}

	public UUID getUniqueId() {
		return this.player.getUniqueId();
	}

	public boolean hasClearedLevel() {
		return this.currentLevel != null && LevelStatus.CLEARED.equals(this.currentLevel.getStatus());
	}

	public boolean hasInventoryToOpen() {
		return inventoryToOpen != null;
	}

	public boolean hasPendingOperation() {
		// TODO check if the player is currently waiting for an operation to complete
		return false;
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
		return this.currentLevel == null;
	}

	public boolean isPlayingLevel() {
		return this.currentLevel != null && LevelStatus.PLAYING.equals(this.currentLevel.getStatus());
	}

	public MenuClickResult onInventoryClick(Inventory inventory, int slot) {
		if (personalMenus.containsKey(inventory.getName())) {
			return personalMenus.get(inventory.getName()).onClick(this, slot);
		}
		return MenuClickResult.ALLOW;
	}

//	public void openLevelSortbyMenu() {
//		AbstractMakerMenu menu = personalMenus.get(LevelSortByMenu.getInstance().getName());
//		if (menu == null) {
//			menu = LevelSortByMenu.getInstance();
//			personalMenus.put(menu.getName(), menu);
//		}
//		inventoryToOpen = menu;
//	}

	public void onQuit() {
		for (AbstractMakerMenu menu : personalMenus.values()) {
			menu.destroy();
		}
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

	public void openLevelBrowserMenu(MinecraftMakerPlugin plugin, LevelSortBy sortBy, boolean update) {
		LevelBrowserMenu menu = (LevelBrowserMenu) personalMenus.get(plugin.getMessage(LevelBrowserMenu.getTitleKey()));
		if (menu == null) {
			menu = LevelBrowserMenu.getInstance(plugin, this.getUniqueId());
			personalMenus.put(menu.getName(), menu);
		}
		if (sortBy != null) {
			menu.sortBy(sortBy);
		}
		if (update) {
			menu.update();
		}
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

	public void openPlayerLevelsMenu(MinecraftMakerPlugin plugin, LevelSortBy sortBy, boolean update) {
		PlayerLevelsMenu menu = (PlayerLevelsMenu) personalMenus.get(plugin.getMessage(PlayerLevelsMenu.getTitleKey()));
		if (menu == null) {
			menu = PlayerLevelsMenu.getInstance(plugin, this.getUniqueId());
			personalMenus.put(menu.getName(), menu);
		}
		if (sortBy != null) {
			menu.sortBy(sortBy);
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

	public void resetLobbyInventory() {
		// clear inventory
		player.getInventory().clear();
		// server browser
		player.getInventory().addItem(MakerLobbyItem.SERVER_BROWSER.getItem());
		player.getInventory().addItem(MakerLobbyItem.STEVE_CHALLENGE.getItem());
		player.getInventory().addItem(MakerLobbyItem.CREATE_LEVEL.getItem());
		player.getInventory().addItem(MakerLobbyItem.PLAYER_LEVELS.getItem());
		player.getInventory().addItem(MakerLobbyItem.LEVEL_BROWSER.getItem());
		// leave item
		player.getInventory().setItem(8, MakerLobbyItem.QUIT.getItem());
		player.getInventory().setHeldItemSlot(4);
		// update inventory on the next tick
		dirtyInventory = true;
	}

	public void resetPlayer() {
		// reset bukkit player
		PlayerUtils.resetPlayer(getPlayer(), GameMode.ADVENTURE);
	}

	public void sendActionMessage(Internationalizable plugin, String key, Object... args) {
		if (player.isOnline()) {
			if (!StringUtils.isEmpty(key)) {
				NMSUtils.sendActionMessage(player, plugin.getMessage(key, args));
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

	public void sendTitleAndSubtitle(String title, String subtitle) {
		if (player.isOnline()) {
			NMSUtils.sendTitle(player, 10, 40, 10, title, subtitle);
		}
	}

	public void setAllowFlight(boolean allowFlight) {
		player.setAllowFlight(allowFlight);
	}

	public void setCurrentLevel(MakerPlayableLevel level) {
		this.currentLevel = level;
	}

	public void setFlying(boolean flying) {
		player.setAllowFlight(true);
		player.setFlying(flying);
	}

	public void setGameMode(GameMode mode) {
		player.setGameMode(mode);
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
	}

	public void executeRequestedInventoryOperations() {
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

	public void updateInventory() {
		dirtyInventory = true;
	}

	public void updatePublishedLevelOnPlayerLevelsMenu(MinecraftMakerPlugin plugin, MakerLevel makerLevel) {
		LevelBrowserMenu.addOrUpdateLevel(plugin, makerLevel);
		PlayerLevelsMenu menu = (PlayerLevelsMenu) personalMenus.get(plugin.getMessage(PlayerLevelsMenu.getTitleKey()));
		if (menu != null) {
			menu.removeLevel(makerLevel);
		}
	}

	public void updateSavedLevelOnPlayerLevelsMenu(MinecraftMakerPlugin plugin, MakerLevel makerLevel) {
		PlayerLevelsMenu menu = (PlayerLevelsMenu) personalMenus.get(plugin.getMessage(PlayerLevelsMenu.getTitleKey()));
		if (menu != null) {
			menu.updateOwnedLevel(makerLevel);
		}
	}

}
