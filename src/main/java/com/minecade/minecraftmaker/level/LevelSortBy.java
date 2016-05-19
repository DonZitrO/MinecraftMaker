package com.minecade.minecraftmaker.level;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public enum LevelSortBy {

	LEVEL_NAME(" level_name "),
	LEVEL_SERIAL(" level_serial "),
	LIKES(" likes "),
	DATE_PUBLISHED(" date_published "),
	RANK(" if(youtuber = 0, 1, 0), if(admin = 0, 1, 0), if(`owner` = 0, 1, 0), " +
			" if(vip = 0, 1, 0), if(pro = 0, 1, 0), if(legendary = 0, 1, 0), " +
			" if(elite = 0, 1, 0), if(titan = 0, 1, 0), if(gm = 0, 1, 0), " +
			" if(mgm = 0, 1, 0), if(dev = 0, 1, 0) ");

	private String dataCriteria;
	private String displayName;

	private LevelSortBy(String dataCriteria){
		this.dataCriteria = dataCriteria;
		this.displayName = MinecraftMakerPlugin.getInstance().getMessage(
				String.format("level.sort.%s", this.name().toLowerCase()));
	}

	public String getDataCriteria(){
		return dataCriteria;
	}

	public String getDisplayName(LevelSortBy levelSortBy){
		if(levelSortBy == null || !this.equals(levelSortBy)){
			return String.format("§F%s", this.displayName);
		} else {
			return String.format("§E%s", this.displayName);
		}
	}
}
