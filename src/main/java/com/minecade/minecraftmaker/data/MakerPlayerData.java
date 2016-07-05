package com.minecade.minecraftmaker.data;

import java.util.UUID;

import com.minecade.core.data.MinecadeAccountData;

public class MakerPlayerData extends MinecadeAccountData {

	private int unpublishedLevelsCount;
	private int publishedLevelsCount;
	private long uniqueLevelClearsCount;
	// private AlternativeMakerLevelClearData currentLevelClearData;

	public MakerPlayerData(UUID uniqueId, String username) {
		super(uniqueId, username);
	}

//	public AlternativeMakerLevelClearData getCurrentLevelClearData() {
//		return currentLevelClearData;
//	}

	public int getPublishedLevelsCount() {
		return publishedLevelsCount;
	}

	public long getUniqueLevelClearsCount() {
		return uniqueLevelClearsCount;
	}

	public int getUnpublishedLevelsCount() {
		return unpublishedLevelsCount;
	}

//	public void setCurrentLevelClearData(AlternativeMakerLevelClearData currentLevelClearData) {
//		this.currentLevelClearData = currentLevelClearData;
//	}

	public void setPublishedLevelsCount(int publishedLevelsCount) {
		this.publishedLevelsCount = publishedLevelsCount;
	}

	public void setUniqueLevelClearsCount(long uniqueLevelClearsCount) {
		this.uniqueLevelClearsCount = uniqueLevelClearsCount;
	}

	public void setUnpublishedLevelsCount(int unpublishedLevelsCount) {
		this.unpublishedLevelsCount = unpublishedLevelsCount;
	}

}
