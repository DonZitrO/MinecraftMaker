package com.minecade.minecraftmaker.level;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerLevel implements Tickable {

	public static enum LevelStatus {
		BUSY,
		EDITING,
		PLAYING,
		READY;
	}

	private final MinecraftMakerPlugin plugin;
	private final short chunkZ;

	private UUID levelId;
	private UUID authorId;

	private LevelStatus status;

	private String authorName;
	private String levelName;

	private Region region;
	private Clipboard clipboard;

	private long favs;
	private long likes;
	private long dislikes;

	private UUID currentPlayerId;

	private Location startLocation;

	private long currentTick;
	private boolean enabled = false;

	public MakerLevel(MinecraftMakerPlugin plugin, MakerPlayer creator, short chunkZ) {
		this.plugin = plugin;
		this.levelId = UUID.randomUUID();
		this.status = LevelStatus.BUSY;
		this.authorId = creator.getUniqueId();
		this.chunkZ = chunkZ;
		// TODO Auto-generated constructor stub
	}

	public MakerLevel(MinecraftMakerPlugin plugin, short chunkZ) {
		this.plugin = plugin;
		this.chunkZ = chunkZ;
		this.startLocation = new Vector(2.5, 65, (chunkZ * 16) + 6.5).toLocation(plugin.getController().getMainWorld(), -90f, 0f);
		this.status = LevelStatus.BUSY;
		this.levelId = UUID.randomUUID();
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public void setAuthorId(UUID authorId) {
		this.authorId = authorId;
	}

	public LevelStatus getStatus() {
		return status;
	}

	public short getChunkZ() {
		return chunkZ;
	}

	@Override
	public void disable() {
		if (!enabled) {
			return;
		}
		// TODO: disable logic
		enabled = false;
	}

	@Override
	public void enable() {
		throw new UnsupportedOperationException("An Arena is enabled by default");
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
	}

	public void startEdition() {
		// TODO: level must be on the right status to allow edition
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null || !mPlayer.isInLobby()) {
			// TODO: disable/unload level
			return;
		}
		mPlayer.sendMessage(plugin, "level.create.start");
		if (mPlayer.teleport(startLocation, TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.CREATIVE);
			mPlayer.setFlying(true);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			mPlayer.setCurrentLevel(this);
			status = LevelStatus.EDITING;
		} else {
			// TODO: disable/unload level
		}
	}

	public Location getStartLocation() {
		return startLocation.clone();
	}

	public void startPlaying(MakerPlayer mPlayer) {
		if (!LevelStatus.READY.equals(getStatus())) {
			mPlayer.sendMessage(plugin, "level.play.error.status");
			return;
		}
		mPlayer.sendMessage(plugin, "level.play.start");
		if (mPlayer.teleport(startLocation, TeleportCause.PLUGIN)) {
			mPlayer.setGameMode(GameMode.ADVENTURE);
			mPlayer.setFlying(false);
			mPlayer.clearInventory();
			mPlayer.getPlayer().getInventory().setItem(8, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getItem());
			mPlayer.updateInventoryOnNextTick();
			mPlayer.setCurrentLevel(this);
			status = LevelStatus.PLAYING;
		} else {
			// TODO: error message to player
		}
	}

	public void endEditing() {
		// TODO: maybe verify EDITING status
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer != null) {
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		saveAndUnload();

	}

	private void saveAndUnload() {
		plugin.getController().saveAndUnloadLevel(this);
	}

	public void rename(String newName) {
		// TODO: additional name validation?
		this.levelName = newName;
		save();
	}

	private void save() {
		// TODO Auto-generated method stub

	}

	public UUID getLevelId() {
		return levelId;
	}

	public String getLevelName() {
		return levelName != null ? levelName : levelId.toString();
	}

	public long getFavs() {
		return favs;
	}

	public long getLikes() {
		return likes;
	}

	public long getDislikes() {
		return dislikes;
	}

	public Clipboard getClipboard() {
		return clipboard;
	}

}
