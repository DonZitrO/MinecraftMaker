package com.minecade.minecraftmaker.data;

import java.util.HashSet;
import java.util.Set;

public class MakerSteveData {

	//public static final Random RANDOM = new Random(System.currentTimeMillis());

	private short lives = 100;
	//private final List<Long> levels;
	private final Set<Long> levelsCleared = new HashSet<>();
	private final Set<Long> levelsSkipped = new HashSet<>();

	public MakerSteveData() {
		//checkArgument(levels.size() > 0);
		//this.levels = new ArrayList<Long>(levels);
	}

	public void clearLevel(Long levelSerial) {
		//levels.remove(levelSerial);
		levelsCleared.add(levelSerial);
	}

//	public long getRandomLevel() {
//		if (levels.size() == 0) {
//			return 0;
//		}
//		return levels.get(RANDOM.nextInt(levels.size()));
//	}
//
//	public boolean hasMoreLevels() {
//		return levels.size() > 0;
//	}

	public Set<Long> getClearedAndSkippedLevels() {
		Set<Long> result = new HashSet<>();
		result.addAll(levelsCleared);
		result.addAll(levelsSkipped);
		return result;
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
		//levels.remove(levelSerial);
		levelsSkipped.add(levelSerial);
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
