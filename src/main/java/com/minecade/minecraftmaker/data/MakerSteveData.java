package com.minecade.minecraftmaker.data;

import java.util.HashSet;
import java.util.Set;

public class MakerSteveData {

	private short lives = 100;
	private final Set<Long> levelsCleared = new HashSet<>();
	private final Set<Long> levelsSkipped = new HashSet<>();

	public void clearLevel(Long levelSerial) {
		levelsCleared.add(levelSerial);
	}

	public Set<Long> getClearedAndSkippedLevels() {
		Set<Long> result = new HashSet<>();
		result.addAll(levelsCleared);
		result.addAll(levelsSkipped);
		return result;
	}

	public int getLevelsClearedCount() {
		return levelsCleared.size();
	}

	public short getLives() {
		return lives;
	}

	public boolean hasClearedLevel(Long levelSerial) {
		return levelsCleared.contains(levelSerial);
	}

	public boolean hasSkippedLevel(Long levelSerial) {
		return levelsSkipped.contains(levelSerial);
	}

	public void skipLevel(Long levelSerial, boolean loseLife) {
		if (loseLife) {
			lives--;
		}
		levelsSkipped.add(levelSerial);
	}

	public boolean tryAgain() {
		return --lives > 0;
	}

}
