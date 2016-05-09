package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.io.BlockArrayClipboard;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.util.LevelUtils;

public class LevelClipboardCopyOperation implements Operation {

	private final MinecraftMakerPlugin plugin;
	private final MakerLevel level;

	private boolean firstRun = true;

	public LevelClipboardCopyOperation(MinecraftMakerPlugin plugin, MakerLevel level) {
		this.plugin = plugin;
		this.level = level;
	}

	@Override
	public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
		if (firstRun) {
			firstRun = false;
			level.tryStatusTransition(LevelStatus.CLIPBOARD_COPY_READY, LevelStatus.COPYING_CLIPBOARD);
			if (level.getClipboard() == null) {
				Region levelRegion = LevelUtils.getLevelRegion(level.getChunkZ());
				BlockArrayClipboard clipboard = new BlockArrayClipboard(levelRegion);
				clipboard.setOrigin(levelRegion.getMinimumPoint());
				level.setClipboard(clipboard);
			}
			ResumableForwardExtentCopy copy = new ResumableForwardExtentCopy(plugin.getController().getMakerExtent(), level.getClipboard().getRegion(), level.getClipboard(), level.getClipboard().getOrigin());
			return new DelegateOperation(this, copy);
		}
		level.tryStatusTransition(LevelStatus.COPYING_CLIPBOARD, LevelStatus.CLIPBOARD_COPIED);
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
