package com.minecade.minecraftmaker.schematic.io;

import java.io.Closeable;
import java.io.IOException;

import com.minecade.minecraftmaker.schematic.world.WorldData;

/**
 * Reads {@code Clipboard}s.
 *
 * @see Clipboard
 */
public interface ClipboardReader extends Closeable {

	/**
	 * Read a {@code Clipboard}.
	 *
	 * @param data
	 *            the world data space to convert the blocks to
	 * @return the read clipboard
	 * @throws IOException
	 *             thrown on I/O error
	 */
	Clipboard read(WorldData data) throws IOException;

}
