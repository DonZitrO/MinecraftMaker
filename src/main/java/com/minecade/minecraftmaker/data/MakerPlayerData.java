package com.minecade.minecraftmaker.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.minecade.core.data.MinecadeAccountData;

public class MakerPlayerData extends MinecadeAccountData {

	private List<MakerLevelClearData> levelsClear;

	public MakerPlayerData(UUID uniqueId, String username) {
		super(uniqueId, username);
		this.levelsClear = new ArrayList<MakerLevelClearData>();
	}

	public List<MakerLevelClearData> getLevelsClear() {
		return levelsClear;
	}
}
