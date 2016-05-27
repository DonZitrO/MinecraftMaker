package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.MakerExtent;
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
		if (firstRun) {
			firstRun = false;
			startNanoTime = System.nanoTime();
			level.tryStatusTransition(LevelStatus.CLIPBOARD_PASTE_READY, LevelStatus.PASTING_CLIPBOARD);
			// black box
			//return new DelegateOperation(this, new ResumableOperationQueue(LevelUtils.createRegionFacesOperation(BukkitUtil.toWorld(level.getWorld()), level.getLevelRegion(), new BaseBlock(BlockID.OBSIDIAN)), LevelUtils.createPasteOperation(level.getClipboard(), level.getMakerExtent(), level.getWorldData())));
			return new DelegateOperation(this, LevelUtils.createPasteOperation(level.getClipboard(), new MakerExtent(level.getWorld()), level.getWorldData()));
		}
		level.tryStatusTransition(LevelStatus.PASTING_CLIPBOARD, LevelStatus.CLIPBOARD_PASTED);
		Bukkit.getLogger().info(String.format("LevelClipboardPasteOperation.resume - finished on: [%s] nanoseconds - level: [%s]", System.nanoTime() - startNanoTime, level.getDescription()));
		return null;
	}

	@Override
	public void cancel() {
		level.disable(String.format("Level operation cancelled: [%s]", this), null);
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		// TODO: understand and implement this
	}

}
