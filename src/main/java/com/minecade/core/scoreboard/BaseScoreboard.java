package com.minecade.core.scoreboard;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.util.Tickable;

public abstract class BaseScoreboard implements Tickable {

	protected final MinecraftMakerPlugin plugin;

	protected Scoreboard scoreboard;

	protected DynamicText displayName;

	protected Map<Integer, String> sidebarTexts = new HashMap<>();

	protected long currentTick;

	public BaseScoreboard(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	public void init() {
		this.scoreboard = this.plugin.getServer().getScoreboardManager().getNewScoreboard();
		for (Rank rank : Rank.values()) {
			String prefix = rank.getDisplayName();
			if (prefix.length() >= 15) {
				prefix = prefix.substring(0, 15) + " ";
			} else {
				prefix += " ";
			}
			scoreboard.registerNewTeam(rank.name()).setPrefix(prefix);
		}
	}

	public void removeFromTeam(Rank rank, String playerName) {
		Team team = this.scoreboard.getTeam(rank.name());
		team.removeEntry(playerName);
	}

	public void removeEntryFromTeam(String entry) {
		Team team = this.scoreboard.getEntryTeam(entry);
		if (team != null) {
			team.removeEntry(entry);
		}
	}

	public void addEntryToTeam(String teamName, String entry) {
		Team team = this.scoreboard.getTeam(teamName);
		if (team != null) {
			team.addEntry(entry);
		}
	}

	public void assignTeam(Rank rank, String playerName) {
		Team team = this.scoreboard.getTeam(rank.name());
		team.addEntry(playerName);
	}

	public void createObjective(DisplaySlot slot, String name, String displayName, String criteria) {
		name = name.length() > 16 ? name.substring(0, 16) : name;
		Objective existing = scoreboard.getObjective(slot);
		if (null != existing) {
			existing.unregister();
		}
		existing = scoreboard.getObjective(name);
		if (null != existing) {
			existing.unregister();
		}
		Objective newObjective = scoreboard.registerNewObjective(name, criteria);
		newObjective.setDisplayName(displayName);
		newObjective.setDisplaySlot(slot);
	}

	public void updateScore(DisplaySlot slot, String name, int value) {
		Objective existing = scoreboard.getObjective(slot);
		if (null != existing) {
			existing.getScore(name).setScore(value);
		} else {
			Bukkit.getLogger().warning(String.format("BaseScoreboard.updateScore - Failed to update score, objective not found on slot: [%s]", slot.name()));
		}
	}

	public void updateDisplayName(DisplaySlot slot, String displayName) {
		Objective existing = scoreboard.getObjective(slot);
		if (null != existing) {
			existing.setDisplayName(displayName);
		} else {
			Bukkit.getLogger().warning(String.format("BaseScoreboard.updateDisplayName - Failed to update display name, objective not found on slot: [%s]", slot.name()));
		}
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	@Override
	public boolean isDisabled() {
		return scoreboard == null;
	}

	public void updateSidebarText(int slot, String text) {
		// use color codes for empty slots
		if (StringUtils.isBlank(text)) {
			text = ChatColor.values()[slot].toString();
		}
		text = trimScoreboardText(text);
		// find old text for slot
		String oldText = sidebarTexts.put(slot, text);
		if (null != oldText) {
			if (oldText.equals(text)) {
				return;
			}
			clearScore(oldText);
		}
		updateScore(DisplaySlot.SIDEBAR, text, slot);
	}

	public void clearScore(String name) {
		scoreboard.resetScores(name);
	}

	public void resetAllScores() {
		for (String entry : scoreboard.getEntries()) {
			scoreboard.resetScores(entry);
		}
	}

	public void unregisterAllTeams() {
		for (Team team : scoreboard.getTeams()) {
			team.unregister();
		}
	}

	public void unregisterAllObjectives() {
		for (Objective objective : scoreboard.getObjectives()) {
			objective.unregister();
		}
	}

	@Override
	public void disable() {
		resetAllScores();
		unregisterAllObjectives();
		unregisterAllTeams();
		sidebarTexts.clear();
		sidebarTexts = null;
		displayName = null;
		scoreboard = null;
	}

	protected static String trimScoreboardText(String text) {
		// trim text
		if (text.length() > 16) {
			text = ChatColor.stripColor(text);
			if (text.length() > 16) {
				text = text.substring(0, 16);
			}
		}
		return text;
	}

}
