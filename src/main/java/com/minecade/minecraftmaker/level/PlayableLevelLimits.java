package com.minecade.minecraftmaker.level;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.core.data.Rank;

public class PlayableLevelLimits {

	public static int getRankDroppedItemsLimit(Rank rank) {
		checkNotNull(rank);
		if (rank.includes(Rank.TITAN)) {
			return 50;
		}
		if (rank.includes(Rank.ELITE)) {
			return 40;
		}
		if (rank.includes(Rank.PRO)) {
			return 30;
		}
		if (rank.includes(Rank.VIP)) {
			return 20;
		}
		return 10;
	}

	public static int getRankGuestEditorsLimit(Rank rank) {
		checkNotNull(rank);
		if (rank.includes(Rank.TITAN)) {
			return Integer.MAX_VALUE;
		}
		if (rank.includes(Rank.ELITE)) {
			return 4;
		}
		if (rank.includes(Rank.PRO)) {
			return 2;
		}
		if (rank.includes(Rank.VIP)) {
			return 1;
		}
		return 0;
	}

	public static int getRankLivingEntitiesLimit(Rank rank) {
		checkNotNull(rank);
		if (rank.includes(Rank.TITAN)) {
			return 50;
		}
		if (rank.includes(Rank.ELITE)) {
			return 40;
		}
		if (rank.includes(Rank.PRO)) {
			return 30;
		}
		if (rank.includes(Rank.VIP)) {
			return 20;
		}
		return 10;
	}

	public static int getRankVehiclesLimit(Rank rank) {
		checkNotNull(rank);
		if (rank.includes(Rank.TITAN)) {
			return 50;
		}
		if (rank.includes(Rank.ELITE)) {
			return 40;
		}
		if (rank.includes(Rank.PRO)) {
			return 30;
		}
		if (rank.includes(Rank.VIP)) {
			return 20;
		}
		return 10;
	}

	public static int getRankPublishedLevelsLimit(Rank rank) {
		checkNotNull(rank);
		if (rank.includes(Rank.TITAN)) {
			return Integer.MAX_VALUE;
		}
		if (rank.includes(Rank.ELITE)) {
			return 100;
		}
		if (rank.includes(Rank.PRO)) {
			return 50;
		}
		if (rank.includes(Rank.VIP)) {
			return 25;
		}
		return 5;
	}

	public static int getRankUnpublishedLevelsLimit(Rank rank) {
		checkNotNull(rank);
		if (rank.equals(Rank.GUEST)) {
			return 5;
		}
		return 28;
//		if (rank.includes(Rank.TITAN)) {
//			return 28;
//		}
//		if (rank.includes(Rank.ELITE)) {
//			return 21;
//		}
//		if (rank.includes(Rank.PRO)) {
//			return 14;
//		}
//		if (rank.includes(Rank.VIP)) {
//			return 7;
//		}
//		return 5;
	}

	private PlayableLevelLimits() {
		super();
	}

}
