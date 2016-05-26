package com.minecade.minecraftmaker.task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.LevelPageUpdateCallback;
import com.minecade.minecraftmaker.inventory.LevelPageUpdateRequest;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class AsyncLevelBrowserUpdaterTask extends BukkitRunnable {

	private final MinecraftMakerPlugin plugin;

	private final ConcurrentSkipListMap<LevelPageUpdateRequest, LevelPageUpdateCallback> pendingRequests = new ConcurrentSkipListMap<>();
	private final Set<LevelPageUpdateRequest> completedRequests = Collections.synchronizedSet(new HashSet<LevelPageUpdateRequest>());

	private boolean busy = false;

	public AsyncLevelBrowserUpdaterTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		if (busy) {
			return;
		}
		if (Bukkit.isPrimaryThread()) {
			Bukkit.getLogger().severe(String.format("AsyncLevelBrowserMenuUpdaterTask.run - running in primary thread!"));
			return;
		}
		try {
			busy = true;
			if (pendingRequests.isEmpty()) {
				return;
			}
			LevelPageUpdateRequest request = pendingRequests.firstKey();
			LevelPageUpdateCallback callback = pendingRequests.remove(request);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelBrowserMenuUpdaterTask.run - handling request: %s", request));
				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelBrowserMenuUpdaterTask.run - with callback: %s", callback));
			}
			// load page from db
			Set<Long> dbPage = plugin.getDatabaseAdapter().loadPublishedLevelSerialsPage(request.getLevelSortBy(), request.isReverseOrder(), LevelBrowserMenu.getPageOffset(request.getPage()), LevelBrowserMenu.getLevelsPerPage());
			// load page from menu
			Set<Long> menuPage = LevelBrowserMenu.getLevelPageSerials(request.getLevelSortBy(), request.isReverseOrder(), LevelBrowserMenu.getPageOffset(request.getPage()), LevelBrowserMenu.getLevelsPerPage());
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelBrowserMenuUpdaterTask.run - dbPage: %s", dbPage));
				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelBrowserMenuUpdaterTask.run - menuPage: %s", menuPage));
			}
			// the ones from the db less the ones from the menu
			callback.addToUpdate(dbPage);
			callback.removeToUpdate(menuPage);
			// the ones from the menu less the ones from db
			callback.addToDelete(menuPage);
			callback.removeToDelete(dbPage);
			if (callback.getToUpdateCount() == 0 && callback.getToDeleteCount() == 0) {
				// pages are equals, nothing to update
				completedRequests.add(request);
				return;
			}
			// pages are different, we have to start finding differences backwards
			if (request.getPage() == 1) {
				if (callback.getToUpdateCount() > 0) {
					callback.setLevels(plugin.getDatabaseAdapter().loadPublishedLevelsBySerials(callback.getToUpdate()));
				} else {
					// TODO: verify that delete candidates are actually deleted
				}
				callback.setLevelCount(plugin.getDatabaseAdapter().loadPublishedLevelsCount());
				// the callback is ready, send it
				Bukkit.getScheduler().runTask(plugin, () -> plugin.getController().levelPageUpdateCallback(callback));
				completedRequests.add(request);
				return;
			}
			// merge data with previous callback data
			pendingRequests.merge(new LevelPageUpdateRequest(request.getLevelSortBy(), request.isReverseOrder(), request.getPage() - 1), callback, (e, n) -> e.merge(n));
			completedRequests.add(request);	
		} finally {
			busy = false;
		}
	}

	public synchronized void requestLevelPageUpdate(LevelSortBy sortBy, boolean reverseSortBy, int currentPage, UUID playerId) {
		LevelPageUpdateRequest request = new LevelPageUpdateRequest(sortBy, reverseSortBy, currentPage);
		if (completedRequests.contains(request)) {
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | AsyncLevelBrowserUpdaterTask.requestLevelPageUpdate - request already completed", request));
			}
			return;
		}
		pendingRequests.merge(request, new LevelPageUpdateCallback(playerId), (a, b) -> a.merge(b));
	}

	public synchronized void resetCompleted() {
		completedRequests.clear();
	}

}
