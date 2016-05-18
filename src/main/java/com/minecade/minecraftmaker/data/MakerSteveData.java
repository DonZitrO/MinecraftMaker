package com.minecade.minecraftmaker.data;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MakerSteveData {

	public static final Random RANDOM = new Random(System.currentTimeMillis());

	private short lives = 100;
	private final List<Long> levels;
	private final Set<Long> levelsCleared = new HashSet<>();
	private final Set<Long> levelsSkipped = new HashSet<>();
	// private Long currentLevelId;

	public MakerSteveData(Set<Long> levels) {
		checkArgument(levels.size() > 0);
		this.levels = new ArrayList<Long>(levels);
	}

	public void clearLevel(Long levelId) {
		levels.remove(levelId);
		levelsCleared.add(levelId);
	}

	public long getRandomLevel() {
		if (levels.size() == 0) {
			return 0;
		}
		return levels.get(RANDOM.nextInt(levels.size()));
	}

	public boolean hasClearedLevel(Long levelId) {
		return levelsCleared.contains(levelId);
	}

	public boolean hasSkippedLevel(Long levelId) {
		return levelsSkipped.contains(levelId);
	}

	public void skipLevel(Long levelId) {
		lives--;
		levels.remove(levelId);
		levelsSkipped.add(levelId);
	}

	public boolean tryAgain() {
		return --lives > 0;
	}

	public short getLives() {
		return lives;
	}

	public int getLevelsClearedCount() {
		return levelsCleared.size();
	}

}
