package com.minecade.minecraftmaker.level;

import com.minecade.core.i18n.Translatable;

public enum LevelSortBy implements Translatable {

	TRENDING_SCORE(false, true),
	LIKES(false, true),
	LEVEL_NAME(true, false),
	AUTHOR_NAME(true, false),
	AUTHOR_RANK(true, true),
	DATE_PUBLISHED(true, true),
	LEVEL_SERIAL(true, false),
	DISLIKES(false, true);

	private String displayName;
	private boolean reversible = false;
	private boolean reversedDefault = false;

	private LevelSortBy(boolean reversible, boolean reversedDefault) {
		this.reversible = reversible;
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

	public boolean isReversible() {
		return reversible;
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
