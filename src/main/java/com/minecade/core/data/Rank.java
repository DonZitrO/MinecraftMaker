package com.minecade.core.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;

import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.i18n.Translatable;

public enum Rank implements Translatable {

	// natural order is declaration order
	GUEST,
	VIP,
	PRO(VIP),
	ELITE(PRO),
	TITAN(ELITE),
	LEGENDARY(TITAN), // obsolete - replaced by TITAN
	YT(TITAN),
	GM(TITAN),
	MGM(GM),
	ADMIN(MGM),
	DEV(ADMIN, TITAN),
	OWNER(DEV); 

	private final Set<Rank> included = Collections.synchronizedSet(new HashSet<>());
 
	// these is configurable/translatable
	private String displayName;

	private Rank(Rank... includes) {
		if (includes != null) {
			includeRanks(Arrays.asList(includes));
		}
		includeRank(this);
		Bukkit.getLogger().severe(String.format("Rank: %s includes: [%s]", this, included));
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

	private void includeRanks(Iterable<Rank> ranks) {
		if (ranks == null) {
			return;
		}
		for (Rank included : ranks) {
			includeRank(included);
		}
	}

	private void includeRank(Rank rank) {
		checkNotNull(rank);
		if (included.contains(rank)){
			return;
		}
		included.add(rank);
		includeRanks(rank.included);
	}

	@Override
	public void translate(Internationalizable plugin) {
		String messageKey = String.format("%s.%s", getTranslationKeyBase(), getName().toLowerCase());
		String displayName = plugin.getMessage(messageKey);
		if (!messageKey.equals(displayName)) {
			setDisplayName(displayName);
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

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean includes(Rank rank) {
		return this == rank || included.contains(rank);
	}

}
