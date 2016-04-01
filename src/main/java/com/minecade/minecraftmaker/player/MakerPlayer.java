package com.minecade.minecraftmaker.player;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import com.minecade.core.data.Rank;
import com.minecade.core.player.PlayerUtils;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.items.MakerLobbyItems;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerPlayer implements Tickable {

	private final Player player;
	private final MakerPlayerData data;

	private MakerLevel currentLevel;

	private ChatColor nameColor = ChatColor.RESET;
	private boolean dirtyInventory;
	private long currentTick;
	private boolean enabled = true;

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

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void resetLobbyInventory() {
		// clear inventory
		player.getInventory().clear();
		// server browser
		player.getInventory().addItem(MakerLobbyItems.SERVER_BROWSER.getItem());
		player.getInventory().addItem(MakerLobbyItems.STEVE_CHALLENGE.getItem());
		player.getInventory().addItem(MakerLobbyItems.CREATE_LEVEL.getItem());
		player.getInventory().addItem(MakerLobbyItems.VIEW_LEVELS.getItem());
		// leave item
		player.getInventory().setItem(8, MakerLobbyItems.QUIT.getItem());
		// update inventory on the next tick
		dirtyInventory = true;
	}

	public void resetPlayer() {
		// reset bukkit player
		PlayerUtils.resetPlayer(getPlayer(), GameMode.ADVENTURE);
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		// every tick tasks
		updateInventoryIfDirty();
	}

	private void updateInventoryIfDirty() {
		if (dirtyInventory) {
			player.updateInventory();
			dirtyInventory = false;
		}
	}

}
