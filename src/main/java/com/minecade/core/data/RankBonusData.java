package com.minecade.core.data;

public class RankBonusData {

	private final Rank rank;
	private final int coins;

	public RankBonusData(Rank rank, int coins) {
		this.rank = rank;
		this.coins = coins;
	}

	public Rank getRank() {
		return rank;
	}

	public int getCoins() {
		return coins;
	}

}
