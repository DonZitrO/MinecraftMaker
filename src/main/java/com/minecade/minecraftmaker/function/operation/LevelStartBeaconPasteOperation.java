package com.minecade.minecraftmaker.function.operation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import com.minecade.mcore.function.operation.DelegateOperation;
import com.minecade.mcore.function.operation.LimitedTimeRunContext;
import com.minecade.mcore.function.operation.Operation;
import com.minecade.mcore.schematic.bukkit.BukkitWorld;
import com.minecade.mcore.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.util.LevelUtils;

public class LevelStartBeaconPasteOperation implements Operation {

	private final MakerPlayableLevel level;

	private boolean firstRun = true;
	private long startNanoTime = 0;

	public LevelStartBeaconPasteOperation(MakerPlayableLevel level) {
		this.level = level;
	}

	@Override
	public Operation resume(LimitedTimeRunContext runContext) throws MinecraftMakerException {
		if (level.isDisabled()) {
			Bukkit.getLogger().info(String.format("LevelStartBeaconPasteOperation.resume - operation cancelled because level was disabled: [%s]", level.getDescription()));
			return null;
		}
		// player left before the operation was resumed/completed
		if (!level.hasActivePlayer()) {
			Bukkit.getLogger().info(String.format("LevelStartBeaconPasteOperation.resume - operation cancelled because level player left: [%s]", level.getDescription()));
			level.disable("LevelStartBeaconPasteOperation.resume - player left");
			return null;
		}
		if (firstRun) {
			firstRun = false;
			startNanoTime = System.nanoTime();
			level.tryStatusTransition(LevelStatus.START_BEACON_PLACE_READY, LevelStatus.START_BEACON_PLACING);
			return new DelegateOperation(this, LevelUtils.createPasteOperation(LevelUtils.createLevelStartClipboard(level.getChunkZ()), new BukkitWorld(level.getWorld()), level.getWorldData()));
		}
		level.tryStatusTransition(LevelStatus.START_BEACON_PLACING, LevelStatus.START_BEACON_PLACED);
		Bukkit.getLogger().info(String.format("LevelStartBeaconPasteOperation.resume - finished on: [%s] ms - level: [%s]", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime), level.getDescription()));
		return null;
	}

	@Override
	public void cancel() {
		level.disable(String.format("LevelClipboardPasteOperation.cancel - Level: {%s}", level.getDescription()));
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		// TODO: understand and implement this
	}

}
