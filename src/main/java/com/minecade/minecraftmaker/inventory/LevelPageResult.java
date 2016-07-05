package com.minecade.minecraftmaker.inventory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.minecade.minecraftmaker.level.MakerDisplayableLevel;

public class LevelPageResult {

	private final Set<UUID> players;
	private final Set<MakerDisplayableLevel> levels = new LinkedHashSet<>();
	private long levelCount;

	public LevelPageResult(UUID playerId) {
		players = new HashSet<>();
		players.add(playerId);
	}

	public synchronized void addPlayers(Set<UUID> playerId) {
		players.addAll(playerId);
	}

	public synchronized void addLevels(Collection<MakerDisplayableLevel> levels) {
		this.levels.addAll(levels);
	}

	public long getLevelCount() {
		return levelCount;
	}

	public Set<MakerDisplayableLevel> getLevels() {
		return Collections.unmodifiableSet(levels);
	}

	public synchronized Set<UUID> getPlayers() {
		return Collections.unmodifiableSet(players);
	}

	public synchronized LevelPageResult merge(LevelPageResult other) {
		if (other == null) {
			return this;
		}
		addPlayers(other.players);
		addLevels(other.levels);
		return this;
	}

	public void setLevelCount(long levelCount) {
		this.levelCount = levelCount;
	}

	@Override
	public String toString() {
		return "LevelPageResult [players=" + players.size() + ", levels=" + levels.size() + ", levelCount=" + levelCount + "]";
	}

}
