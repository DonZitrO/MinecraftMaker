package com.minecade.minecraftmaker.schematic.io;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.jnbt.ByteArrayTag;
import com.minecade.minecraftmaker.schematic.jnbt.CompoundTag;
import com.minecade.minecraftmaker.schematic.jnbt.DoubleTag;
import com.minecade.minecraftmaker.schematic.jnbt.FloatTag;
import com.minecade.minecraftmaker.schematic.jnbt.IntTag;
import com.minecade.minecraftmaker.schematic.jnbt.ListTag;
import com.minecade.minecraftmaker.schematic.jnbt.NBTOutputStream;
import com.minecade.minecraftmaker.schematic.jnbt.ShortTag;
import com.minecade.minecraftmaker.schematic.jnbt.StringTag;
import com.minecade.minecraftmaker.schematic.jnbt.Tag;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.WorldData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Writes schematic files based that are compatible with MCEdit and other editors.
 */
public class SchematicWriter implements ClipboardWriter {

    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;
    private final NBTOutputStream outputStream;

    /**
     * Create a new schematic writer.
     *
     * @param outputStream the output stream to write to
     */
    public SchematicWriter(NBTOutputStream outputStream) {
        checkNotNull(outputStream);
        this.outputStream = outputStream;
    }

    @Override
    public void write(Clipboard clipboard, WorldData data) throws IOException {
        Region region = clipboard.getRegion();
        Vector origin = clipboard.getOrigin();
        Vector min = region.getMinimumPoint();
        Vector offset = min.subtract(origin);
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        if (width > MAX_SIZE) {
            throw new IllegalArgumentException("Width of region too large for a .schematic");
        }
        if (height > MAX_SIZE) {
            throw new IllegalArgumentException("Height of region too large for a .schematic");
        }
        if (length > MAX_SIZE) {
            throw new IllegalArgumentException("Length of region too large for a .schematic");
        }

        // ====================================================================
        // Metadata
        // ====================================================================

        HashMap<String, Tag> schematic = new HashMap<String, Tag>();
        schematic.put("Width", new ShortTag((short) width));
        schematic.put("Length", new ShortTag((short) length));
        schematic.put("Height", new ShortTag((short) height));
        schematic.put("Materials", new StringTag("Alpha"));
        schematic.put("WEOriginX", new IntTag(min.getBlockX()));
        schematic.put("WEOriginY", new IntTag(min.getBlockY()));
        schematic.put("WEOriginZ", new IntTag(min.getBlockZ()));
        schematic.put("WEOffsetX", new IntTag(offset.getBlockX()));
        schematic.put("WEOffsetY", new IntTag(offset.getBlockY()));
        schematic.put("WEOffsetZ", new IntTag(offset.getBlockZ()));

        // ====================================================================
        // Block handling
        // ====================================================================

        byte[] blocks = new byte[width * height * length];
        byte[] addBlocks = null;
        byte[] blockData = new byte[width * height * length];
        List<Tag> tileEntities = new ArrayList<Tag>();

        for (Vector point : region) {
            Vector relative = point.subtract(min);
            int x = relative.getBlockX();
            int y = relative.getBlockY();
            int z = relative.getBlockZ();

            int index = y * width * length + z * width + x;
            BaseBlock block = clipboard.getBlock(point);

            // Save 4096 IDs in an AddBlocks section
            if (block.getType() > 255) {
                if (addBlocks == null) { // Lazily create section
                    addBlocks = new byte[(blocks.length >> 1) + 1];
                }

                addBlocks[index >> 1] = (byte) (((index & 1) == 0) ?
                        addBlocks[index >> 1] & 0xF0 | (block.getType() >> 8) & 0xF
                        : addBlocks[index >> 1] & 0xF | ((block.getType() >> 8) & 0xF) << 4);
            }

            blocks[index] = (byte) block.getType();
            blockData[index] = (byte) block.getData();

            // Store TileEntity data
            CompoundTag rawTag = block.getNbtData();
            if (rawTag != null) {
                Map<String, Tag> values = new HashMap<String, Tag>();
                for (Entry<String, Tag> entry : rawTag.getValue().entrySet()) {
                    values.put(entry.getKey(), entry.getValue());
                }

                values.put("id", new StringTag(block.getNbtId()));
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));

                CompoundTag tileEntityTag = new CompoundTag(values);
                tileEntities.add(tileEntityTag);
            }
        }

        schematic.put("Blocks", new ByteArrayTag(blocks));
        schematic.put("Data", new ByteArrayTag(blockData));
        schematic.put("TileEntities", new ListTag(CompoundTag.class, tileEntities));

        if (addBlocks != null) {
            schematic.put("AddBlocks", new ByteArrayTag(addBlocks));
        }

        // ====================================================================
        // Entities
        // ====================================================================

        List<Tag> entities = new ArrayList<Tag>();
        for (Entity entity : clipboard.getEntities()) {
            BaseEntity state = entity.getState();

            if (state != null) {
                Map<String, Tag> values = new HashMap<String, Tag>();

                // Put NBT provided data
                CompoundTag rawTag = state.getNbtData();
                if (rawTag != null) {
                    values.putAll(rawTag.getValue());
                }

                // Store our location data, overwriting any
                values.put("id", new StringTag(state.getTypeId()));
                values.put("Pos", writeVector(entity.getLocation().toVector(), "Pos"));
                values.put("Rotation", writeRotation(entity.getLocation(), "Rotation"));

                CompoundTag entityTag = new CompoundTag(values);
                entities.add(entityTag);
            }
        }

        schematic.put("Entities", new ListTag(CompoundTag.class, entities));

        // ====================================================================
        // Output
        // ====================================================================

        CompoundTag schematicTag = new CompoundTag(schematic);
        outputStream.writeNamedTag("Schematic", schematicTag);
    }

    private Tag writeVector(Vector vector, String name) {
        List<DoubleTag> list = new ArrayList<DoubleTag>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    private Tag writeRotation(Location location, String name) {
        List<FloatTag> list = new ArrayList<FloatTag>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }

	@Override
	public void close() throws IOException {
		outputStream.close();
	}

}
