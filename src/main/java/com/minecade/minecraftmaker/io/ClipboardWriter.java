package com.minecade.minecraftmaker.io;

import java.io.Closeable;
import java.io.IOException;

import com.minecade.minecraftmaker.world.WorldData;

/**
 * Writes {@code Clipboard}s.
 *
 * @see Clipboard
 */
public interface ClipboardWriter extends Closeable {

    /**
     * Writes a clipboard.
     *
     * @param clipboard the clipboard
     * @param data the world data instance
     * @throws IOException thrown on I/O error
     */
    void write(Clipboard clipboard, WorldData data) throws IOException;

}
