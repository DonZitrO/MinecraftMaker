package com.minecade.minecraftmaker.level;

import com.minecade.core.i18n.Translatable;

public enum LevelSortBy implements Translatable {

	LIKES(true),
	LEVEL_NAME,
	AUTHOR_NAME,
	AUTHOR_RANK(true),
	DATE_PUBLISHED(true),
	LEVEL_SERIAL,
	DISLIKES(true);

	private String displayName;
	private boolean reversedDefault = false;

	private LevelSortBy() {
		this.reversedDefault = false;
	}

	private LevelSortBy(boolean reversedDefault) {
		this.reversedDefault = reversedDefault;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getTranslationKeyBase() {
		return "level.sort";
	}

	public boolean isReversedDefault() {
		return reversedDefault;
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
