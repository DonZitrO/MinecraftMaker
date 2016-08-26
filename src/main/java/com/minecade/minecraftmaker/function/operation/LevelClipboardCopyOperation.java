package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import com.minecade.mcore.function.operation.DelegateOperation;
import com.minecade.mcore.function.operation.LimitedTimeRunContext;
import com.minecade.mcore.function.operation.Operation;
import com.minecade.mcore.function.operation.ResumableForwardExtentCopy;
import com.minecade.mcore.schematic.exception.MinecraftMakerException;
import com.minecade.mcore.schematic.io.BlockArrayClipboard;
import com.minecade.mcore.schematic.world.Region;
import com.minecade.minecraftmaker.extent.MakerExtent;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;

public class LevelClipboardCopyOperation implements Operation {

	private final MakerPlayableLevel level;

	private boolean firstRun = true;

	public LevelClipboardCopyOperation(MakerPlayableLevel level) {
		this.level = level;
	}

	@Override
	public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
		if (firstRun) {
			firstRun = false;
			level.tryStatusTransition(LevelStatus.CLIPBOARD_COPY_READY, LevelStatus.CLIPBOARD_COPYING);
			Region levelRegion = level.getLevelRegion();
			// we need a fresh clipboard every time
			BlockArrayClipboard clipboard = new BlockArrayClipboard(levelRegion);
			clipboard.setOrigin(levelRegion.getMinimumPoint());
			level.setClipboard(clipboard);
			ResumableForwardExtentCopy copy = new ResumableForwardExtentCopy(new MakerExtent(level.getWorld()), levelRegion, level.getClipboard(), level.getClipboard().getOrigin());
			return new DelegateOperation(this, copy);
		}
		level.tryStatusTransition(LevelStatus.CLIPBOARD_COPYING, LevelStatus.CLIPBOARD_COPIED);
		return null;
	}

	@Override
	public void cancel() {
		// no-op
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		// no-op
	}

}
