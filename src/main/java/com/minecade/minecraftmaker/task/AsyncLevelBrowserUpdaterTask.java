package com.minecade.minecraftmaker.task;

import static com.minecade.mcore.util.BukkitUtils.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.LevelPageResult;
import com.minecade.minecraftmaker.inventory.LevelPageUpdateRequest;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class AsyncLevelBrowserUpdaterTask extends BukkitRunnable {

	private final MinecraftMakerPlugin plugin;
	// WARNING: manual synchronization
	private final Map<LevelPageUpdateRequest, LevelPageResult> pendingRequests = new LinkedHashMap<>();
	private boolean busy = false;

	public AsyncLevelBrowserUpdaterTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		if (busy) {
			return;
		}
		verifyNotPrimaryThread();
		try {
			busy = true;
			LevelPageUpdateRequest request = null;
			LevelPageResult result = null;
			synchronized (pendingRequests) {
				if (pendingRequests.isEmpty()) {
					return;
				}
				request = pendingRequests.keySet().iterator().next();
				result = pendingRequests.remove(request);
			}
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelBrowserUpdaterTask.run - handling request: %s", request));
				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelBrowserUpdaterTask.run - with callback: %s", result));
			}
			result.addLevels(plugin.getDatabaseAdapter().loadPublishedLevelsPage(request.getLevelSortBy(), request.isReverseOrder(), LevelBrowserMenu.getPageOffset(request.getPage()), LevelBrowserMenu.ITEMS_PER_PAGE));
			result.setLevelCount(plugin.getDatabaseAdapter().loadPublishedStagesCount("level"));
			final LevelPageResult finalPageResult = result;
			Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().levelPageResultCallback(finalPageResult));
		} finally {
			busy = false;
		}
	}

	public void requestLevelPageUpdate(LevelSortBy sortBy, boolean reverseSortBy, int currentPage, UUID playerId) {
		synchronized (pendingRequests) {
			pendingRequests.merge(new LevelPageUpdateRequest(sortBy, reverseSortBy, currentPage), new LevelPageResult(playerId), (a, b) -> a.merge(b));
		}
	}

}
