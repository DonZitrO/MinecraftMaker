package com.minecade.minecraftmaker.data;

public enum MakerUnlockable {

	RAINY_LEVEL(3000),
	MIDNIGHT_LEVEL(3000);

	private final int cost;

	private MakerUnlockable(int cost) {
		this.cost = cost;
	}

	public int getCost() {
		return cost;
	}

}
