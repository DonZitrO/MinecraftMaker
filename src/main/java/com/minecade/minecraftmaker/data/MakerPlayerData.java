package com.minecade.minecraftmaker.data;

import java.util.UUID;

import com.minecade.core.data.MinecadeAccountData;

public class MakerPlayerData extends MinecadeAccountData {

	private int unpublishedLevelsCount;
	private int publishedLevelsCount;
	private long uniqueLevelClearsCount;
	private boolean steveClear;
	private boolean rainyLevelUnlocked;
	private boolean midnightLevelUnlocked;

	public MakerPlayerData(UUID uniqueId, String username) {
		super(uniqueId, username);
	}

	public int getPublishedLevelsCount() {
		return publishedLevelsCount;
	}

	public long getUniqueLevelClearsCount() {
		return uniqueLevelClearsCount;
	}

	public int getUnpublishedLevelsCount() {
		return unpublishedLevelsCount;
	}

	public boolean isMidnightLevelUnlocked() {
		return midnightLevelUnlocked;
	}

	public boolean isRainyLevelUnlocked() {
		return rainyLevelUnlocked;
	}

	public boolean isSteveClear() {
		return steveClear;
	}

	public void setMidnightLevelUnlocked(boolean midnightLevelUnlocked) {
		this.midnightLevelUnlocked = midnightLevelUnlocked;
	}

	public void setPublishedLevelsCount(int publishedLevelsCount) {
		this.publishedLevelsCount = publishedLevelsCount;
	}

	public void setRainyLevelUnlocked(boolean rainyLevelUnlocked) {
		this.rainyLevelUnlocked = rainyLevelUnlocked;
	}

	public void setSteveClear(boolean steveClear) {
		this.steveClear = steveClear;
	}

	public void setUniqueLevelClearsCount(long uniqueLevelClearsCount) {
		this.uniqueLevelClearsCount = uniqueLevelClearsCount;
	}

	public void setUnpublishedLevelsCount(int unpublishedLevelsCount) {
		this.unpublishedLevelsCount = unpublishedLevelsCount;
	}

}
