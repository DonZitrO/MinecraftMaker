package com.minecade.core.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class MinecadeAccountData {

	private final UUID uniqueId;
	private final String username;

	private long coins;
	protected TreeSet<Rank> ranks = new TreeSet<Rank>();
	private Set<Sellable> purchases = new HashSet<Sellable>();

	private Rank displayRank;

	private boolean newPlayer;

	protected MinecadeAccountData(UUID uniqueId, String username) {
		if (null == uniqueId || null == username) {
			throw new NullPointerException();
		}
		this.uniqueId = uniqueId;
		this.username = username;
		addRank(Rank.GUEST);
	}

	public void addPurchase(Sellable purchase) {
		purchases.add(purchase);
	}

	public void addRank(Rank rank) {
		if (rank == null) {
			return;
		}
		if (Rank.LEGENDARY.equals(rank) && !ranks.contains(Rank.TITAN)) {
			rank = Rank.TITAN;
		}
		ranks.add(rank);
		if (null != rank.getIncludes()) {
			// recurse on included ranks
			for (Rank included : rank.getIncludes()) {
				if (!ranks.contains(included)) {
					addRank(included);
				}
			}
		}
	}

	public Set<Rank> getRanks() {
		return new HashSet<Rank>(ranks);
	}

	public long addOrRemoveCoins(long amount) {
		coins += amount;
		return coins;
	}

	public long getCoins() {
		return coins;
	}

	public Rank getDisplayRank() {
		if (null == displayRank) {
			return ranks.last();
		}
		return displayRank;
	}

	public Rank getHighestRank() {
		return ranks.last();
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
		return ranks.contains(rank);
	}

	public boolean ownsItem(Sellable item) {
		return purchases.contains(item);
	}

	public void removePurchase(Sellable item) {
		purchases.remove(item);
	}

	public void removeRank(Rank rank) {
		ranks.remove(rank);
		if (null != rank.getIncludes()) {
			// recurse on included ranks
			for (Rank included : rank.getIncludes()) {
				if (!ranks.contains(included)) {
					removeRank(included);
				}
			}
		}
	}

	public void replaceRanks(List<Rank> newRanks) {
		ranks.clear();
		ranks.add(Rank.GUEST);
		displayRank = null;
		for (Rank rank : newRanks) {
			addRank(rank);
		}
	}

	public void setCoins(long coins) {
		this.coins = coins;
	}

	public void setDisplayRank(Rank displayRank) {
		if (ranks.contains(displayRank)) {
			this.displayRank = displayRank;
		} else {
			throw new IllegalArgumentException(String.format("Rank [%s] is not available for this account", displayRank));
		}
	}

}
