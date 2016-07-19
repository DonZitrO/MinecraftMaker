package com.minecade.minecraftmaker.data;

import java.util.UUID;

public class MakerLevelClearData {

	private final UUID levelId;
	private final UUID playerId;
	private String levelName;
	private String playerName;
	private long totalClears;
	private long bestTimeCleared;

	public MakerLevelClearData(UUID levelId, UUID playerId) {
		this.levelId = levelId;
		this.playerId = playerId;
	}

	public long getBestTimeCleared() {
		return bestTimeCleared;
	}

	public UUID getLevelId() {
		return levelId;
	}

	public String getLevelName() {
		return levelName;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	public String getPlayerName() {
		return playerName;
	}

	public long getTotalClears() {
		return totalClears;
	}

	public void setBestTimeCleared(long bestTimeCleared) {
		this.bestTimeCleared = bestTimeCleared;
	}

	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public void setTotalClears(long totalClears) {
		this.totalClears = totalClears;
	}

}
