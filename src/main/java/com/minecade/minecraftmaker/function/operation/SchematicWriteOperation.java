package com.minecade.minecraftmaker.function.operation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.io.ClipboardFormat;
import com.minecade.minecraftmaker.schematic.io.ClipboardWriter;
import com.minecade.minecraftmaker.schematic.world.WorldData;

public class SchematicWriteOperation implements Operation {

	private final Clipboard clipboard;
	private final File file;
	private final WorldData worldData;

	public SchematicWriteOperation(Clipboard clipboard, WorldData worldData, File file) {
		this.clipboard = clipboard;
		this.file = file;
		this.worldData = worldData;
	}

	@Override
	public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
		long startNanoTime = 0;
		if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | SchematicWriteOperation.resume - start..."));
			startNanoTime = System.nanoTime();
		}
		try (
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(bos)) {
			writer.write(clipboard, worldData);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("SchematicWriteOperation.resume - unable to write schematic data: %s", e.getMessage()));
			e.printStackTrace();
		}
		if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | SchematicWriteOperation.resume - finished on: [%s] nanoseconds", System.nanoTime() - startNanoTime));
		}
		return null;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void addStatusMessages(List<String> messages) {
		// TODO Auto-generated method stub

	}

}
