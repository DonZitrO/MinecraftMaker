package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.util.LevelUtils;

public class LevelClipboardPasteOperation implements Operation {

	private final MakerLevel level;

	private boolean firstRun = true;

	public LevelClipboardPasteOperation(MakerLevel level) {
		this.level = level;
	}

	@Override
	public Operation resume(RunContext runContext) throws MinecraftMakerException {
		if (firstRun) {
			firstRun = false;
			level.tryStatusTransition(LevelStatus.CLIPBOARD_PASTE_READY, LevelStatus.PASTING_CLIPBOARD);
			return new DelegateOperation(this, LevelUtils.createPasteOperation(level.getClipboard(), level.getMakerExtent(), level.getWorldData()));
		}
		level.tryStatusTransition(LevelStatus.PASTING_CLIPBOARD, LevelStatus.CLIPBOARD_PASTED);
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
