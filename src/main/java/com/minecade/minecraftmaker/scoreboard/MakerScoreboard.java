package com.minecade.minecraftmaker.scoreboard;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;

import com.minecade.core.data.Rank;
import com.minecade.core.scoreboard.BaseScoreboard;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerScoreboard extends BaseScoreboard {

	private static final List<String> TITLE = Arrays.asList(
			"§3§LM§B§LinecraftMaker", "§LM§3§Li§B§LnecraftMaker", "§LMi§3§Ln§B§LecraftMaker",
			"§LMin§3§Le§B§LcraftMaker", "§LMine§3§Lc§B§LraftMaker", "§LMinec§3§Lr§B§LaftMaker",
			"§LMinecr§3§La§B§LftMaker", "§LMinecra§3§Lf§B§LtMaker", "§LMinecraf§3§Lt§B§LMaker",
			"§LMinecraft§3§LM§B§Laker", "§LMinecraftM§3§La§B§Lker", "§LMinecraftMa§3§Lk§B§Ler",
			"§LMinecraftMak§3§Le§B§Lr", "§LMinecraftMake§3§Lr", "§LMinecraftMaker",
			"§B§LMinecraftMaker", "§B§LMinecraftMaker", "§B§LMinecraftMaker",
			"§F§LMinecraftMaker", "§B§LMinecraftMaker", "§F§LMinecraftMaker",
			"§B§LMinecraftMaker", "§F§LMinecraftMaker", "§B§LMinecraftMaker");

	private final MakerPlayer makerPlayer;
	private int index;

	public MakerScoreboard(MinecraftMakerPlugin plugin, MakerPlayer makerPlayer) {
		super(plugin);
		this.makerPlayer = makerPlayer;
	}

	@Override
	public void disable() {
		makerPlayer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		super.disable();
	}

	private void displayFlashingTitle(){
		if(getCurrentTick() % 3 == 0){
			this.index = this.index == TITLE.size() - 1 ? 0 : this.index + 1;
			updateDisplayName(DisplaySlot.SIDEBAR, TITLE.get(this.index));
		}
	}

	@Override
	public String getDescription() {
		return String.format("MakerScoreboard for player: %s", makerPlayer.getDescription());
	}

	@Override
	public void init() {
		super.init();
		createObjective(DisplaySlot.SIDEBAR, "sidebar", TITLE.get(index), "dummy");
		makerPlayer.setScoreboard(scoreboard);
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		if (getCurrentTick() % 2 == 1) {
			this.displayFlashingTitle();
		}
		if (getCurrentTick() % 20 == 11) {
			updateCommonTexts();
			if (makerPlayer.isInLobby() || makerPlayer.isInBusyLevel()) {
				updatePlayerTexts();
			} else if (makerPlayer.isInSteve()) {
				updateSteveTexts();
			} else if (makerPlayer.isOnUnpublishedLevel() || makerPlayer.getCurrentTick() % 240 < 120) {
				updateCreateAndPlayLevelTexts();
			} else {
				updatePlayLevelTexts();
			}
		}
	}

	private void updateCommonTexts() {
		this.updateSidebarText(13, "§4");
		this.updateSidebarText(10, "§3");
		this.updateSidebarText(7, "§2");
		this.updateSidebarText(4, "§1");
		this.updateSidebarText(1, "§0");
		this.updateSidebarText(0, plugin.getMessage("scoreboard.url"));
	}

	private void updateCreateAndPlayLevelTexts() {
		this.updateSidebarText(12, plugin.getMessage("scoreboard.server.title"));
		this.updateSidebarText(11, plugin.getMessage("scoreboard.server.name", plugin.getServerId()));
		this.updateSidebarText(9, plugin.getMessage("scoreboard.level-name.title"));
		this.updateSidebarText(8, plugin.getMessage("scoreboard.level-name.name", StringUtils.isEmpty(makerPlayer.getCurrentLevel().getLevelName()) ?
				plugin.getMessage("general.empty") : makerPlayer.getCurrentLevel().getLevelName()));
		this.updateSidebarText(6, plugin.getMessage("scoreboard.level-author.title"));
		this.updateSidebarText(5, plugin.getMessage("scoreboard.level-author.name", makerPlayer.getCurrentLevel().getAuthorName()));
		this.updateSidebarText(3, plugin.getMessage("scoreboard.level-likes.title"));
		this.updateSidebarText(2, plugin.getMessage("scoreboard.level-likes.name", makerPlayer.getCurrentLevel().getLikes()));
	}

	private void updatePlayerTexts() {
		this.updateSidebarText(12, plugin.getMessage("scoreboard.server.title"));
		this.updateSidebarText(11, plugin.getMessage("scoreboard.server.name", plugin.getServerId()));
		this.updateSidebarText(9, plugin.getMessage("scoreboard.coins.title"));
		this.updateSidebarText(8, plugin.getMessage("scoreboard.coins.name", makerPlayer.getData().getCoins()));
		this.updateSidebarText(6, plugin.getMessage("scoreboard.level-clear.title"));
		this.updateSidebarText(5, plugin.getMessage("scoreboard.level-clear.name", makerPlayer.getData().getLevelsClear().size()));
		// TODO: put some stuff there
		this.updateSidebarText(3, plugin.getMessage("scoreboard.rank.title"));
		if (!makerPlayer.getHighestRank().equals(Rank.GUEST)) {
			this.updateSidebarText(2, makerPlayer.getDisplayRank().getDisplayName());
		} else {
			this.updateSidebarText(2, plugin.getMessage("upgrade.rank.at"));
		}
	}

	private void updatePlayLevelTexts() {
		this.updateSidebarText(12, plugin.getMessage("scoreboard.server.title"));
		this.updateSidebarText(11, plugin.getMessage("scoreboard.server.name", plugin.getServerId()));
		this.updateSidebarText(9, plugin.getMessage("scoreboard.level-player.title"));
		this.updateSidebarText(8, plugin.getMessage("scoreboard.level-player.name", makerPlayer.getRecordUsername()));
		this.updateSidebarText(6, plugin.getMessage("scoreboard.level-time.title"));
		this.updateSidebarText(5, plugin.getMessage("scoreboard.level-time.name", makerPlayer.getRecordTime()));
		this.updateSidebarText(3, plugin.getMessage("scoreboard.player-best.title"));
		this.updateSidebarText(2, plugin.getMessage("scoreboard.player-best.name", makerPlayer.getPlayerRecordTime()));
	}

	private void updateSteveTexts() {
		this.updateSidebarText(12, plugin.getMessage("scoreboard.level-name.title"));
		this.updateSidebarText(11, plugin.getMessage("scoreboard.level-name.name", this.makerPlayer.getCurrentLevel().getLevelName()));
		this.updateSidebarText(9, plugin.getMessage("scoreboard.level-author.title"));
		this.updateSidebarText(8, plugin.getMessage("scoreboard.level-author.name", this.makerPlayer.getCurrentLevel().getAuthorName()));
		this.updateSidebarText(6, plugin.getMessage("scoreboard.level-clear.title"));
		this.updateSidebarText(5, plugin.getMessage("scoreboard.level-clear.name", this.makerPlayer.getSteveData().getLevelsClearedCount()));
		this.updateSidebarText(3, plugin.getMessage("scoreboard.player-lives.title"));
		this.updateSidebarText(2, plugin.getMessage("scoreboard.player-lives.name", this.makerPlayer.getSteveData().getLives()));
	}

}
