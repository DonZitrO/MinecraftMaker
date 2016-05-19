package com.minecade.minecraftmaker.data;

import java.util.UUID;

public class MakerLevelClearData {

	private long timeCleared;
	private int tries;
	private UUID uniqueId;
	private String username;

	public long getTimeCleared() {
		return timeCleared;
	}

	public int getTries() {
		return tries;
	}

	public String getUsername() {
		return username;
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public void setTries(int tries) {
		this.tries = tries;
	}

	public void setTimeCleared(long timeCleared) {
		this.timeCleared = timeCleared;
	}

	public void setUniqueId(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
