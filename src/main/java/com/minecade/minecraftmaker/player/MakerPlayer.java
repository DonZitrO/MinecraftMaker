package com.minecade.minecraftmaker.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
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
import com.minecade.minecraftmaker.inventory.LevelTemplateMenu;
import com.minecade.minecraftmaker.inventory.ServerBrowserMenu;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerPlayer implements Tickable {

	private final Player player;
	private final MakerPlayerData data;

	private MakerLevel currentLevel;

	private ChatColor nameColor = ChatColor.RESET;
	private final Map<String, AbstractMakerMenu> personalMenus = new HashMap<>();

	private boolean dirtyInventory;
	private long currentTick;
	private boolean enabled = true;
	private AbstractMakerMenu inventoryToOpen;

	public MakerPlayer(Player player, MakerPlayerData data) {
		this.player = player;
		this.data = data;
	}

	@Override
	public void disable() {
		if (!enabled) {
			return;
		}
		// TODO: disable logic
		enabled = false;
	}

	@Override
	public void enable() {
		throw new UnsupportedOperationException("A Player is enabled by default");
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

	public Player getPlayer() {
		return this.player;
	}

	public UUID getUniqueId() {
		return this.player.getUniqueId();
	}

	public boolean hasInventoryToOpen() {
		return inventoryToOpen != null;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	private void openInventoryIfAvailable() {
		if (inventoryToOpen != null) {
			inventoryToOpen.open(player);
			inventoryToOpen = null;
		}
	}

	public void openLevelTemplateMenu() {
		AbstractMakerMenu menu = personalMenus.get(LevelTemplateMenu.getInstance().getName());
		if (menu == null) {
			menu = LevelTemplateMenu.getInstance();
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
		player.getInventory().addItem(MakerLobbyItem.VIEW_LEVELS.getItem());
		// leave item
		player.getInventory().setItem(8, MakerLobbyItem.QUIT.getItem());
		// update inventory on the next tick
		dirtyInventory = true;
	}

	public void resetPlayer() {
		// reset bukkit player
		PlayerUtils.resetPlayer(getPlayer(), GameMode.ADVENTURE);
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

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		// every tick tasks
		updateInventoryIfDirty();
		openInventoryIfAvailable();
	}

	private void updateInventoryIfDirty() {
		if (dirtyInventory) {
			player.updateInventory();
			dirtyInventory = false;
		}
	}

	public boolean onInventoryClick(Inventory inventory, int slot) {
		if (personalMenus.containsKey(inventory.getName())) {
			personalMenus.get(inventory.getName()).onClick(this, slot);
			return true;
		}
		return false;
	}

	public void updateInventoryOnNextTick() {
		dirtyInventory = true;
	}

	public boolean isEditingLevel() {
		return this.currentLevel != null && GameMode.CREATIVE.equals(player.getGameMode());
	}

	public void closeInventory() {
		player.closeInventory();
	}

	public boolean isInLobby() {
		return this.currentLevel == null;
	}

	public void cancelPendingOperation() {
		// TODO keep track of all operations related to this player and cancel
		// them.
	}

	public boolean hasPendingOperation() {
		// TODO check if the player is currently waiting for an operation to complete
		return false;
	}

	public boolean teleport(Location location, TeleportCause cause) {
		return player.teleport(location, cause);
	}

	public void setGameMode(GameMode mode) {
		player.setGameMode(mode);
	}

	public void setCurrentLevel(MakerLevel level) {
		this.currentLevel = level;
	}

	public void clearInventory() {
		player.getInventory().clear();
		updateInventoryOnNextTick();
	}

	public void setFlying(boolean flying) {
		player.setAllowFlight(true);
		player.setFlying(flying);
	}

}
