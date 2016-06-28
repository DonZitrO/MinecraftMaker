package com.minecade.minecraftmaker.inventory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.minecade.minecraftmaker.level.MakerDisplayableLevel;

public class LevelPageUpdateCallback {

	private Set<UUID> players;
	private Set<Long> toUpdate;
	private Set<Long> toDelete;
	private Set<MakerDisplayableLevel> levels;
	private long levelCount;

	public LevelPageUpdateCallback(UUID playerId) {
		players = new HashSet<>();
		players.add(playerId);
	}

	public synchronized void addPlayers(Set<UUID> playerId) {
		players.addAll(playerId);
	}

	public synchronized void addToDelete(Set<Long> toDelete) {
		if (toDelete == null) {
			return;
		}
		if (this.toDelete == null) {
			this.toDelete = new HashSet<>();
		}
		this.toDelete.addAll(toDelete);
	}

	public synchronized void addToUpdate(Set<Long> toUpdate) {
		if (toUpdate == null) {
			return;
		}
		if (this.toUpdate == null) {
			this.toUpdate = new HashSet<>();
		}
		this.toUpdate.addAll(toUpdate);
	}

	public long getLevelCount() {
		return levelCount;
	}

	public Set<MakerDisplayableLevel> getLevels() {
		return levels;
	}

	public synchronized Set<UUID> getPlayers() {
		return Collections.unmodifiableSet(players);
	}

	public synchronized Set<Long> getToDelete() {
		return Collections.unmodifiableSet(toDelete);
	}

	public int getToDeleteCount() {
		return toDelete != null ? toDelete.size() : 0;
	}

	public synchronized Set<Long> getToUpdate() {
		return Collections.unmodifiableSet(toUpdate);
	}

	public int getToUpdateCount() {
		return toUpdate != null ? toUpdate.size() : 0;
	}

	public synchronized LevelPageUpdateCallback merge(LevelPageUpdateCallback other) {
		if (other == null) {
			return this;
		}
		if (levels != null || other.levels != null) {
			throw new IllegalStateException("Cannot merge after levels were loaded");
		}
		addPlayers(other.players);
		addToUpdate(other.toUpdate);
		addToDelete(other.toDelete);
		return this;
	}

	public synchronized void removeToDelete(Set<Long> toDelete) {
		if (toDelete == null) {
			return;
		}
		if (this.toDelete == null) {
			this.toDelete = new HashSet<>();
		}
		this.toDelete.removeAll(toDelete);
	}

	public synchronized void removeToUpdate(Set<Long> toUpdate) {
		if (toUpdate == null) {
			return;
		}
		if (this.toUpdate == null) {
			this.toUpdate = new HashSet<>();
		}
		this.toUpdate.removeAll(toUpdate);
	}

	public void setLevelCount(long levelCount) {
		this.levelCount = levelCount;
	}

	public void setLevels(Set<MakerDisplayableLevel> levels) {
		this.levels = levels;
	}

	@Override
	public String toString() {
		return "LevelPageUpdateCallback [players=" + players + ", toUpdate=" + toUpdate + ", toDelete=" + toDelete + ", levelCount=" + levelCount + "]";
	}

}
