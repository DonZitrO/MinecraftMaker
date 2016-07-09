package com.minecade.minecraftmaker.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.core.data.Rank;
import com.minecade.core.data.ServerData;
import com.minecade.core.data.ServerStatus;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;
import com.minecade.core.util.BungeeUtils;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class ServerBrowserMenu extends AbstractMakerMenu {

	private static final int MAX_SERVERS = 8;
	private static final long MAX_UPDATE_TIME_MILLIS = 10000;

	private static ServerBrowserMenu instance;

	private static ItemStackBuilder offlineServerIconBuilder = new ItemBuilder(Material.REDSTONE_BLOCK);

	public static ServerBrowserMenu getInstance() {
		if (instance == null) {
			instance = new ServerBrowserMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private Map<Integer, ServerData> serverData = new HashMap<>();

	private ServerBrowserMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 18);
		init();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.serverbrowser.title";
	}

	private ServerData createOfflineServerInfo(int serverId) {
		ServerData info = new ServerData();
		info.setServerNumber(serverId);
		info.setStatus(ServerStatus.OFFLINE);
		info.setTimeUpdated(System.currentTimeMillis());
		return info;
	}

	private void init() {
		for (int i = 0; i < MAX_SERVERS; i++) {
			ServerData info = serverData.get(i + 1);
			if (info == null) {
				info = createOfflineServerInfo(i + 1);
				serverData.put(info.getServerNumber(), info);
			}
			items[i] = updateItemStack(null, info);
		}
	}

	@Override
	public boolean isShared() {
		return true;
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		ServerData info = serverData.get(slot + 1);
		if (info == null) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		if (info.getServerNumber() == plugin.getServerId()) {
			mPlayer.sendMessage(plugin, "server.error.already-connected");
			return MenuClickResult.CANCEL_UPDATE;
		}
		switch (info.getStatus()) {
		case FULL:
			if (!mPlayer.getData().hasRank(Rank.VIP)) {
				mPlayer.sendMessage(plugin, "server.error.vip-only", Rank.VIP.getDisplayName());
				return MenuClickResult.CANCEL_UPDATE;
			}
			break;
		case OPEN:
			String bungeeId = String.format("maker%s", info.getServerNumber());
			BungeeUtils.switchServer(plugin, mPlayer.getPlayer(), bungeeId, plugin.getMessage("menu.serverbrowser.connecting", bungeeId));
			return MenuClickResult.CANCEL_CLOSE;
		default:
			mPlayer.sendMessage(plugin, "server.error.not-available");
			break;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

	@Override
	public void update() {
		boolean dirty = false;
		for (ServerData info :serverData.values()) {
			if (System.currentTimeMillis() - info.getTimeUpdated() > MAX_UPDATE_TIME_MILLIS) {
				info.setStatus(ServerStatus.OFFLINE);
				info.setTimeUpdated(System.currentTimeMillis());
				items[info.getServerNumber()-1] = updateItemStack(items[info.getServerNumber()-1], info);
				dirty = true;
			}
		}
		if (dirty) {
			inventory.setContents(items);
		}
	}

	public void update(ServerData newInfo) {
		if (!Bukkit.isPrimaryThread()) {
			throw new IllegalStateException("This code must run on the primary thread");
		}
		if (newInfo.getServerNumber() > MAX_SERVERS) {
			return;
		}
		newInfo.setTimeUpdated(System.currentTimeMillis());
		serverData.put(newInfo.getServerNumber(), newInfo);
		items[newInfo.getServerNumber()-1] = updateItemStack(items[newInfo.getServerNumber()-1], newInfo);
		inventory.setContents(items);
	}

	private ItemStack updateItemStack(ItemStack icon, ServerData info) {
		if (icon == null) {
			icon = offlineServerIconBuilder.withDisplayName(plugin.getMessage("menu.serverbrowser.server-icon.display-name", info.getServerNumber())).build();
		}
		ItemMeta meta = icon.getItemMeta();
		List<String> newLines = new ArrayList<String>();
		//These three components are always needed
		newLines.add("");
		newLines.add(plugin.getMessage("menu.serverbrowser.status", plugin.getMessage(String.format("menu.serverbrowser.status-%s", String.valueOf(info.getStatus()).toLowerCase()))));
		switch (info.getStatus()) {
		case OPEN:
			newLines.add(plugin.getMessage("menu.serverbrowser.players", info.getOnlinePlayers(), info.getMaxPlayers()));
			newLines.add("");
			newLines.add(plugin.getMessage("menu.serverbrowser.click-message"));
			icon.setType(Material.EMERALD_BLOCK);
			break;
		case FULL:
			icon.setType(Material.GOLD_BLOCK);
			break;
		default:
			icon.setType(Material.REDSTONE_BLOCK);
			break;
		}
		meta.setLore(newLines);
		icon.setItemMeta(meta);
		return icon;
	}

}
