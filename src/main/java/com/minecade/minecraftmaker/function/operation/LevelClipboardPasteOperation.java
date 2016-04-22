package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import org.bukkit.Bukkit;

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
			if (!level.tryStatusTransition(LevelStatus.CLIPBOARD_PASTE_READY, LevelStatus.PASTING_CLIPBOARD)) {
				Bukkit.getLogger().severe(String.format("LevelClipboardCopyOperation.resume - unable to paste level clipboard - invalid status: [%s]", level.getStatus()));
				level.disable();
				return null;
			}
			return new DelegateOperation(this, LevelUtils.createPasteOperation(level.getClipboard(), level.getMakerExtent(), level.getWorldData()));
		}
		level.tryStatusTransition(LevelStatus.PASTING_CLIPBOARD, LevelStatus.CLIPBOARD_PASTED);
		return null;
	}

	@Override
	public void cancel() {
		// TODO possible status messages for debugging
		level.clipboardError();
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		// TODO: understand and implement this
	}

}
