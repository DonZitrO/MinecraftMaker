package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.util.LevelUtils;

public class LevelClipboardPasteOperation implements Operation {

	private final MakerPlayableLevel level;

	private boolean firstRun = true;

	public LevelClipboardPasteOperation(MakerPlayableLevel level) {
		this.level = level;
	}

	@Override
	public Operation resume(LimitedTimeRunContext runContext) throws MinecraftMakerException {
		if (firstRun) {
			firstRun = false;
			level.tryStatusTransition(LevelStatus.CLIPBOARD_PASTE_READY, LevelStatus.PASTING_CLIPBOARD);
			return new DelegateOperation(this, LevelUtils.createPasteOperation(level.getClipboard(), level.getMakerExtent(), level.getWorldData()));
		}
		level.tryStatusTransition(LevelStatus.PASTING_CLIPBOARD, LevelStatus.CLIPBOARD_PASTED);
		return null;
		// this is no longer needed as the previous level is now cleared before the first load
		//return LevelUtils.createPasteOperation(LevelUtils.createLevelRemainingEmptyClipboard(level.getChunkZ(), level.getLevelWidth()), level.getMakerExtent(), level.getWorldData());
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
