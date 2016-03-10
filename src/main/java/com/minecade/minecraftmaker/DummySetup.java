package com.minecade.minecraftmaker;

import org.bukkit.entity.Player;

import com.minecade.core.gamebase.MinigameBase;
import com.minecade.core.gamebase.MinigameSetup;

public class DummySetup extends MinigameSetup {

	public DummySetup(MinigameBase base, Player player) {
		super(base, player);
		player.sendMessage(base.getText("setup.nosetup"));
	}

	@Override
	public boolean messageRecieved(String message) {
		return false;
	}

}
