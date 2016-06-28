package com.minecade.minecraftmaker.world;

public enum WorldTimeAndWeather {

	NOON_CLEAR("mcmaker", 6000, false),
	NOON_RAINY("mcmaker_rainy", 6000, true),
	MIDNIGHT_CLEAR("mcmaker_midnight", 18000, false),
	MIDNIGHT_RAINY("mcmaker_midnight_rainy", 18000, true);

	private final String worldName;
	private final long time;
	private final boolean storm;

	private WorldTimeAndWeather(String worldName, long time, boolean storm) {
		this.worldName = worldName;
		this.time = time;
		this.storm = storm;
	}

	public String getWorldName() {
		return worldName;
	}

	public long getTime() {
		return time;
	}

	public boolean isStorm() {
		return storm;
	}

	public WorldTimeAndWeather toNoon() {
		switch (this) {
		case MIDNIGHT_CLEAR:
			return NOON_CLEAR;
		case MIDNIGHT_RAINY:
			return NOON_RAINY;
		default:
			return this;
		}
	}

	public WorldTimeAndWeather toMidnight() {
		switch (this) {
		case NOON_CLEAR:
			return MIDNIGHT_CLEAR;
		case NOON_RAINY:
			return MIDNIGHT_RAINY;
		default:
			return this;
		}
	}

	public WorldTimeAndWeather toClear() {
		switch (this) {
		case NOON_RAINY:
			return NOON_CLEAR;
		case MIDNIGHT_RAINY:
			return MIDNIGHT_CLEAR;
		default:
			return this;
		}
	}

	public WorldTimeAndWeather toRainy() {
		switch (this) {
		case NOON_CLEAR:
			return NOON_RAINY;
		case MIDNIGHT_CLEAR:
			return MIDNIGHT_RAINY;
		default:
			return this;
		}
	}

}
