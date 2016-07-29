package com.minecade.minecraftmaker.task;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.Iterators;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class AnnouncerTask extends BukkitRunnable {

	private static final Random RANDOM = new Random(System.currentTimeMillis());
	private static final String BASE_ANNOUNCEMENT_KEY = "announcements.everyone.%s";
	private static final String BASE_DEFAULT_ANNOUNCEMENT_KEY = "announcements.default.%s";

	private MinecraftMakerPlugin plugin;

	private final Set<String> announcements = new HashSet<>();
	private final Set<String> defaultPlayerAnnouncements = new HashSet<>();

	private Iterator<String> announcementsCycle;
	private Iterator<String> defaultPlayerAnnouncementsCycle;

	public AnnouncerTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	public void init() {
		for (int i = 1;; i++) {
			String key = String.format(BASE_ANNOUNCEMENT_KEY, i);
			String announcement = plugin.getMessage(key);
			if (announcement.equals(key)) {
				break;
			}
			announcements.add(announcement);
		}
		announcementsCycle = Iterators.cycle(announcements);
		for (int i = 1;; i++) {
			String key = String.format(BASE_DEFAULT_ANNOUNCEMENT_KEY, i);
			String announcement = plugin.getMessage(key);
			if (announcement.equals(key)) {
				break;
			}
			defaultPlayerAnnouncements.add(announcement);
		}
		defaultPlayerAnnouncementsCycle = Iterators.cycle(defaultPlayerAnnouncements);
	}

	@Override
	public void run() {
		if (RANDOM.nextInt(2) == 0) {
			if (announcementsCycle.hasNext()) {
				Bukkit.broadcastMessage(announcementsCycle.next());
			}
		} else {
			if (defaultPlayerAnnouncementsCycle.hasNext()) {
				plugin.getController().broadcastToDefaultPlayers(defaultPlayerAnnouncementsCycle.next());
			}
		}
	}
}