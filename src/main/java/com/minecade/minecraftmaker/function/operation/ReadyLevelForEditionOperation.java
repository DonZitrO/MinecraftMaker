package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

// FIXME: experimental class
public class ReadyLevelForEditionOperation implements Operation {

	private final MakerLevel level;

	public ReadyLevelForEditionOperation(MakerLevel level) {
		this.level = level;
	}

	@Override
	public Operation resume(RunContext run) throws MinecraftMakerException {
		level.startEdition();
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
