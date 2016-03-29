package com.minecade.core.data;

public enum Visibility {
	ALL, RANK, NONE;

	private String displayName;

	public String getDisplayName() {
		if (null == displayName) {
			return name();
		}
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
