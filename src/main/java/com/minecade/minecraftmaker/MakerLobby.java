package com.minecade.minecraftmaker;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.minecade.core.MinecadeCore;
import com.minecade.core.gamebase.MinigameLobby;
import com.minecade.core.gamebase.MinigameLocation;
import com.minecade.core.item.Loadout;
import com.minecade.core.scoreboard.DisplayBoard;
import com.minecade.core.scoreboard.DisplayBoardManager;
import com.minecade.core.scoreboard.DisplayBoardValue;
import com.minecade.serverweb.shared.constants.GameState;

public class MakerLobby extends MinigameLobby {
	
	MakerBase base;
	MinecraftMaker plugin;

	private DisplayBoardManager displayboardmanager = null;
	private DisplayBoardValue server;
	private DisplayBoardValue servername;
	private DisplayBoardValue coins;
	private DisplayBoardValue playertitle;
	private DisplayBoardValue playercount;
	private DisplayBoardValue boardurl;
	
	Loadout lobbyloadout = new Loadout();
	
	private ConcurrentHashMap<UUID, DisplayBoardValue> playercoins = new ConcurrentHashMap<UUID, DisplayBoardValue>();
	
	public MakerLobby(MinecraftMaker plugin, MakerBase base) {
		this.plugin = plugin;
		this.base = base;
	}


	
	public void onPlayerJoin(Player player) {
		if(displayboardmanager == null) {
			displayboardmanager = new DisplayBoardManager();
			server = new DisplayBoardValue(base.getText("scoreboard.server.title"));
			servername = new DisplayBoardValue(base.getText("scoreboard.server.name", String.valueOf(MinecadeCore.getServerNum())));
			coins = new DisplayBoardValue(base.getText("scoreboard.coins.title"));
			playertitle = new DisplayBoardValue(base.getText("scoreboard.players.title"));
			boardurl = new DisplayBoardValue(base.getText("scoreboard.url"));
			playercount = new DisplayBoardValue(base.getText("scoreboard.players.format", "0", String.valueOf(base.getMaxPlayers())));
			lobbyloadout.setItemStack(base.getServerBrowserItem().getBuilder().build(), 0);
			lobbyloadout.setItemStack(base.getLobbyItem().getBuilder().build(), 8);
		}
		if(getLobbySpawn() != null && getLobbySpawn().getLocation() != null) {
			plugin.getServer().getScheduler().runTaskLater(plugin, new DelayedTeleport(player, getLobbySpawn()), 10);
			player.teleport(getLobbySpawn().getLocation());
			World sworld = getLobbySpawn().getLocation().getWorld();
			if(sworld.hasStorm()) {
				sworld.setStorm(false);
			}
			sworld.setWeatherDuration(Integer.MAX_VALUE);
			playercount.updateFieldName(base.getText("scoreboard.players.format", String.valueOf(plugin.getServer().getOnlinePlayers().size()), String.valueOf(base.getMaxPlayers())));
		}
		base.getNodeData().getServerData().setCurrentPlayers(Bukkit.getOnlinePlayers().size() + 1);
		base.getNodeData().getServerData().setGameState(GameState.IN_LOBBY);
		base.getNodeData().pushUpdate();
	}
	
	public void onPlayerQuit(Player player) {
		MakerPlayer mPlayer = (MakerPlayer) base.getMinigamePlayer(player);
		if(mPlayer.getArena() != null) {
			mPlayer.getArena().removePlayer(player);
		}
		base.getNodeData().getServerData().setGameState(GameState.IN_LOBBY);
		base.getNodeData().getServerData().setCurrentPlayers(Bukkit.getOnlinePlayers().size() - 1);
		base.getNodeData().pushUpdate();
		playercoins.remove(player.getUniqueId());
		playercount.updateFieldName(base.getText("scoreboard.players.format", String.valueOf(plugin.getServer().getOnlinePlayers().size() - 1), String.valueOf(base.getMaxPlayers())));
	}
	
	private class DelayedTeleport implements Runnable {
		
		Player player;
		MinigameLocation loc;
		
		public DelayedTeleport(Player player, MinigameLocation loc) {
			this.player = player;
			this.loc = loc;
		}

		@Override
		public void run() {
			if(!player.isOnline()) {
				return;
			}
			player.teleport(loc.getLocation());
			player.setGameMode(GameMode.ADVENTURE);
			player.setMaxHealth(20);
			player.setHealth(20);
			player.setFoodLevel(20);
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(" ");
			player.sendMessage(base.getText("motd.line1"));
			player.sendMessage(" ");
			player.sendMessage(base.getText("motd.line2"));
			player.sendMessage(" ");
			player.sendMessage(base.getText("motd.line4"));
			player.sendMessage(" ");
			player.sendMessage(base.getText("motd.line5"));
			player.setCanPickupItems(true);
			MakerPlayer fplayer = (MakerPlayer) base.getMinigamePlayer(player);
			fplayer.wipeState(true);
			lobbyloadout.equip(player);
			DisplayBoard pboard = displayboardmanager.getNewBoard(base.getText("scoreboard.title"), player.getUniqueId());
			DisplayBoardValue coinsvalue = new DisplayBoardValue(base.getText("scoreboard.coins.name", "0"));
			if(fplayer.getCoreData() != null) {
				coinsvalue.updateFieldName(base.getText("scoreboard.coins.name", String.valueOf(fplayer.getCoreData().getLocalCoins())));
			}
			playercoins.put(fplayer.getUUID(), coinsvalue);
			pboard.putSpace();
			pboard.put(server);
			pboard.put(servername);
			pboard.putSpace();
			pboard.put(coins);
			pboard.put(coinsvalue);
			pboard.putSpace();
			pboard.put(playertitle);
			pboard.put(playercount);
			pboard.putSpace();
			pboard.put(boardurl);
			pboard.startAnimation(base.getText("scoreboard.title.animated"), false);
			pboard.enableDisplayRanks();
			pboard.show(player.getPlayer());
			//if(isMapVotingEnabled()) {
				//showPlayerVotingDialog(player);
			//}
			/*
			if(!npcsregistered) {
				//em = plugin.getEffectManager();
				registerNPCs();
			}*/
		}
	}
}
