package com.minecade.minecraftmaker.function.operation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.extent.MakerExtent;
import com.minecade.minecraftmaker.util.LevelUtils;

public class LevelClipboardPasteOperation implements Operation {

	private final MakerPlayableLevel level;

	private boolean firstRun = true;
	private long startNanoTime = 0;

	public LevelClipboardPasteOperation(MakerPlayableLevel level) {
		this.level = level;
	}

	@Override
	public Operation resume(LimitedTimeRunContext runContext) throws MinecraftMakerException {
		if (level.isDisabled()) {
			Bukkit.getLogger().info(String.format("LevelClipboardPasteOperation.resume - operation cancelled because level was disabled: [%s]", level.getDescription()));
			return null;
		}
		// player left before the operation was resumed/completed
		if (!level.hasActivePlayer()) {
			Bukkit.getLogger().info(String.format("LevelClipboardPasteOperation.resume - operation cancelled because level player left: [%s]", level.getDescription()));
			return null;
		}
		if (firstRun) {
			firstRun = false;
			startNanoTime = System.nanoTime();
			level.tryStatusTransition(LevelStatus.CLIPBOARD_PASTE_READY, LevelStatus.CLIPBOARD_PASTING);
			return new DelegateOperation(this, LevelUtils.createPasteOperation(level.getClipboard(), new MakerExtent(level.getWorld(), level), level.getWorldData()));
		}
		level.tryStatusTransition(LevelStatus.CLIPBOARD_PASTE_COMMITTING, LevelStatus.CLIPBOARD_PASTED);
		Bukkit.getLogger().info(String.format("LevelClipboardPasteOperation.resume - finished on: [%s] ms - level: [%s]", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime), level.getDescription()));
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
