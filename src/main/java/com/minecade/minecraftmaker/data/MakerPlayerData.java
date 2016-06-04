package com.minecade.minecraftmaker.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.minecade.core.data.MinecadeAccountData;

public class MakerPlayerData extends MinecadeAccountData {

	private int unpublishedLevelsCount;
	private int publishedLevelsCount;

	// FIXME: review this
	private List<MakerLevelClearData> levelsClear;

	public MakerPlayerData(UUID uniqueId, String username) {
		super(uniqueId, username);
		this.levelsClear = new ArrayList<MakerLevelClearData>();
	}

	public List<MakerLevelClearData> getLevelsClear() {
		return levelsClear;
	}

	public int getUnpublishedLevelsCount() {
		return unpublishedLevelsCount;
	}

	public void setUnpublishedLevelsCount(int unpublishedLevelsCount) {
		this.unpublishedLevelsCount = unpublishedLevelsCount;
	}

	public int getPublishedLevelsCount() {
		return publishedLevelsCount;
	}

	public void setPublishedLevelsCount(int publishedLevelsCount) {
		this.publishedLevelsCount = publishedLevelsCount;
	}

}
