package com.minecade.core.data;

import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.i18n.Translatable;

public enum Rank implements Translatable {

	// natural order is declaration order
	GUEST,
	VIP,
	PRO(VIP),
	ELITE(PRO),
	LEGENDARY, // obsolete, replaced by TITAN
	TITAN(LEGENDARY, ELITE),
	YT(TITAN),
	CHAMP, // this is a per-game rank, so it's loaded along with game data
	GM(TITAN),
	MGM(GM),
	ADMIN(MGM),
	DEV(ADMIN, TITAN),
	OWNER(DEV); 

	private final Rank[] includes;

	// these is configurable/translatable
	private String displayName;

	private Rank(Rank... includes) {
		this.includes = includes;
	}

	public String getColumnName() {
		switch (this) {
		case GUEST:
			return null;
		case YT:
			return "youtuber";
		default:
			return name();
		}
	}

	public String getDisplayName() {
		if (null == displayName) {
			return name();
		}
		return displayName;
	}

	public Rank[] getIncludes() {
		return includes;
	}

	@Override
	public void translate(Internationalizable plugin) {
		String messageKey = String.format("%s.%s", getTranslationKeyBase(), getName().toLowerCase());
		String displayName = plugin.getMessage(messageKey);
		if (!messageKey.equals(displayName)) {
			this.displayName = displayName;
		}
	}

	@Override
	public String getTranslationKeyBase() {
		return "rank";
	}

	@Override
	public String getName() {
		return name();
	}

}
