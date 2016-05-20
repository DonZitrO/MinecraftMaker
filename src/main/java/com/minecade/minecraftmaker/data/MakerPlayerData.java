package com.minecade.minecraftmaker.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.minecade.core.data.MinecadeAccountData;

public class MakerPlayerData extends MinecadeAccountData {

	private List<MakerLevelClearData> levelsClear;
	private int levelsLikes;

	public MakerPlayerData(UUID uniqueId, String username) {
		super(uniqueId, username);
		this.levelsClear = new ArrayList<MakerLevelClearData>();
	}

	public List<MakerLevelClearData> getLevelsClear() {
		return levelsClear;
	}

	public int getLevelsLikes(){
		return levelsLikes;
	}

	public void setLevelsLikes(int levelsLikes){
		this.levelsLikes = levelsLikes;
	}
}
