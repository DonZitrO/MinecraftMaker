package com.minecade.minecraftmaker.inventory;

import com.minecade.minecraftmaker.level.LevelSortBy;

public class LevelBrowserPage implements Comparable<LevelBrowserPage> {

	private final LevelSortBy levelSortBy;
	private final boolean reverseOrder;
	private final long page;

	public LevelBrowserPage(LevelSortBy levelSortBy, boolean reverseOrder, long page) {
		this.levelSortBy = levelSortBy;
		this.reverseOrder = reverseOrder;
		this.page = page;
	}

	public LevelSortBy getLevelSortBy() {
		return levelSortBy;
	}

	public long getPage() {
		return page;
	}

	public boolean isReverseOrder() {
		return reverseOrder;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((levelSortBy == null) ? 0 : levelSortBy.hashCode());
		result = prime * result + (int) (page ^ (page >>> 32));
		result = prime * result + (reverseOrder ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LevelBrowserPage other = (LevelBrowserPage) obj;
		if (levelSortBy != other.levelSortBy)
			return false;
		if (page != other.page)
			return false;
		if (reverseOrder != other.reverseOrder)
			return false;
		return true;
	}

	@Override
	public int compareTo(LevelBrowserPage o) {
		int diff = levelSortBy.compareTo(o.levelSortBy);
		if (diff == 0) {
			diff = Boolean.valueOf(reverseOrder).compareTo(o.reverseOrder);
		}
		if (diff == 0) {
			diff = Long.valueOf(page).compareTo(o.page);
		}
		return diff;
	}

	@Override
	public String toString() {
		return "LevelBrowserPage [levelSortBy=" + levelSortBy + ", reverseOrder=" + reverseOrder + ", page=" + page + "]";
	}

}
