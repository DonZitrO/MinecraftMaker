package com.minecade.core.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MinecadeAccountData {

	private final UUID uniqueId;
	private final String username;

	private long coins;
	private Set<Sellable> purchases = new HashSet<Sellable>();

	private Rank highestRank;
	private Rank displayRank;

	private boolean newPlayer;

	protected MinecadeAccountData(UUID uniqueId, String username) {
		if (null == uniqueId || null == username) {
			throw new NullPointerException();
		}
		this.uniqueId = uniqueId;
		this.username = username;
		this.highestRank = Rank.GUEST;
	}

	public void addPurchase(Sellable purchase) {
		purchases.add(purchase);
	}

	public long addOrRemoveCoins(long amount) {
		coins += amount;
		return coins;
	}

	public long getCoins() {
		return coins;
	}

	public Rank getDisplayRank() {
		if (displayRank == null) {
			return highestRank;
		}
		return displayRank;
	}

	public Rank getHighestRank() {
		return highestRank;
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public String getUsername() {
		return username;
	}

	public boolean isNewPlayer() {
		return newPlayer;
	}

	public void setNewPlayer(boolean newPlayer) {
		this.newPlayer = newPlayer;
	}

	public boolean hasRank(Rank rank) {
		if (rank == null) {
			return false;
		}
		return highestRank.includes(rank);
	}

	public boolean ownsItem(Sellable item) {
		return purchases.contains(item);
	}

	public void removePurchase(Sellable item) {
		purchases.remove(item);
	}

	public void setCoins(long coins) {
		this.coins = coins;
	}

	public boolean setDisplayRank(Rank displayRank) {
		if (highestRank.includes(displayRank)) {
			this.displayRank = displayRank;
			return true;
		}
		return false;
	}

	public void setHighestRank(Rank rank) {
		this.highestRank = rank;
	}

}
