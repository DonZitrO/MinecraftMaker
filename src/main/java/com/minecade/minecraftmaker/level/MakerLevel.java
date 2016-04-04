package com.minecade.minecraftmaker.level;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerLevel implements Tickable {

	public static enum LevelStatus {
		BUSY,
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
		this.startLocation = new Vector(2.5, 64, (chunkZ * 16) + 6.5).toLocation(plugin.getController().getMainWorld(), -90f, 0f);
		this.status = LevelStatus.BUSY;
		this.levelId = UUID.randomUUID();
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public void setAuthorId(UUID authorId) {
		this.authorId = authorId;
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
		MakerPlayer mPlayer = plugin.getController().getPlayer(authorId);
		if (mPlayer == null || !mPlayer.isInLobby()) {
			// TODO: disable/unload level
			return;
		}
		mPlayer.sendMessage(plugin, "level.create.start");
		if (mPlayer.teleport(startLocation, TeleportCause.PLUGIN)) {
			mPlayer.setFlying(true);
			mPlayer.clearInventory();
			mPlayer.setCurrentLevel(this);
			mPlayer.setGameMode(GameMode.CREATIVE);
			status = LevelStatus.READY;
		} else {
			// TODO: disable/unload level
		}
	}

}
