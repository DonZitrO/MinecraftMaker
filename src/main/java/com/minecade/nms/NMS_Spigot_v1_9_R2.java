package com.minecade.nms;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_9_R2.CraftServer;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.nmsapi.NMSAdapter;
import com.minecade.mcore.schematic.block.BaseBlock;
import com.minecade.mcore.schematic.bukkit.Constants;
import com.minecade.mcore.schematic.entity.BaseEntity;
import com.minecade.mcore.schematic.item.CustomItemStack;
import com.minecade.mcore.schematic.jnbt.ByteArrayTag;
import com.minecade.mcore.schematic.jnbt.ByteTag;
import com.minecade.mcore.schematic.jnbt.CompoundTag;
import com.minecade.mcore.schematic.jnbt.DoubleTag;
import com.minecade.mcore.schematic.jnbt.EndTag;
import com.minecade.mcore.schematic.jnbt.FloatTag;
import com.minecade.mcore.schematic.jnbt.IntArrayTag;
import com.minecade.mcore.schematic.jnbt.IntTag;
import com.minecade.mcore.schematic.jnbt.ListTag;
import com.minecade.mcore.schematic.jnbt.LongTag;
import com.minecade.mcore.schematic.jnbt.NBTConstants;
import com.minecade.mcore.schematic.jnbt.ShortTag;
import com.minecade.mcore.schematic.jnbt.StringTag;
import com.minecade.mcore.schematic.jnbt.Tag;

import net.minecraft.server.v1_9_R2.BiomeBase;
import net.minecraft.server.v1_9_R2.BlockPosition;
import net.minecraft.server.v1_9_R2.DamageSource;
import net.minecraft.server.v1_9_R2.Entity;
import net.minecraft.server.v1_9_R2.EntityLiving;
import net.minecraft.server.v1_9_R2.EntityTypes;
import net.minecraft.server.v1_9_R2.NBTBase;
import net.minecraft.server.v1_9_R2.NBTTagByte;
import net.minecraft.server.v1_9_R2.NBTTagByteArray;
import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.NBTTagDouble;
import net.minecraft.server.v1_9_R2.NBTTagEnd;
import net.minecraft.server.v1_9_R2.NBTTagFloat;
import net.minecraft.server.v1_9_R2.NBTTagInt;
import net.minecraft.server.v1_9_R2.NBTTagIntArray;
import net.minecraft.server.v1_9_R2.NBTTagList;
import net.minecraft.server.v1_9_R2.NBTTagLong;
import net.minecraft.server.v1_9_R2.NBTTagShort;
import net.minecraft.server.v1_9_R2.NBTTagString;
import net.minecraft.server.v1_9_R2.TileEntity;
import net.minecraft.server.v1_9_R2.World;
import net.minecraft.server.v1_9_R2.WorldServer;

public final class NMS_Spigot_v1_9_R2 implements NMSAdapter {

    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());

    private final Field nbtListTagListField;
    private final Method nbtCreateTagMethod;

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public NMS_Spigot_v1_9_R2() throws NoSuchFieldException, NoSuchMethodException {
        // A simple test
        CraftServer.class.cast(Bukkit.getServer());

        // The list of tags on an NBTTagList
        nbtListTagListField = NBTTagList.class.getDeclaredField("list");
        nbtListTagListField.setAccessible(true);

        // The method to create an NBTBase tag given its type ID
        nbtCreateTagMethod = NBTBase.class.getDeclaredMethod("createTag", byte.class);
        nbtCreateTagMethod.setAccessible(true);
    }

    /**
     * Read the given NBT data into the given tile entity.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTagIntoTileEntity(NBTTagCompound tag, TileEntity tileEntity) {
        tileEntity.a(tag);
    }

    /**
     * Write the tile entity's NBT data to the given tag.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTileEntityIntoTag(TileEntity tileEntity, NBTTagCompound tag) {
        tileEntity.save(tag);
    }

    /**
     * Get the ID string of the given entity.
     *
     * @param entity the entity
     * @return the entity ID or null if one is not known
     */
    @Nullable
    private static String getEntityId(Entity entity) {
        return EntityTypes.b(entity);
    }

    /**
     * Create an entity using the given entity ID.
     *
     * @param id the entity ID
     * @param world the world
     * @return an entity or null
     */
    @Nullable
    private static Entity createEntityFromId(String id, World world) {
        return EntityTypes.createEntityByName(id, world);
    }

    /**
     * Write the given NBT data into the given entity.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readTagIntoEntity(NBTTagCompound tag, Entity entity) {
        entity.f(tag);
    }

    /**
     * Write the entity's NBT data to the given tag.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readEntityIntoTag(Entity entity, NBTTagCompound tag) {
        entity.e(tag);
    }

    // ------------------------------------------------------------------------
    // Code that is less likely to break
    // ------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public int getBlockId(Material material) {
        return material.getId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Material getMaterial(int id) {
        return Material.getMaterial(id);
    }

    @Override
    public int getBiomeId(Biome biome) {
        BiomeBase mcBiome = CraftBlock.biomeToBiomeBase(biome);
        return mcBiome != null ? BiomeBase.a(mcBiome) : 0;
    }

    @Override
    public Biome getBiome(int id) {
        BiomeBase mcBiome = BiomeBase.getBiome(id);
        return CraftBlock.biomeBaseToBiome(mcBiome); // Defaults to ocean if it's an invalid ID
    }

    @SuppressWarnings("deprecation")
    @Override
    public BaseBlock getBlock(Location location) {
        checkNotNull(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        org.bukkit.block.Block bukkitBlock = location.getBlock();
        BaseBlock block = new BaseBlock(bukkitBlock.getTypeId(), bukkitBlock.getData());

        // Read the NBT data
        TileEntity te = craftWorld.getHandle().getTileEntity(new BlockPosition(x, y, z));
        if (te != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readTileEntityIntoTag(te, tag); // Load data
            block.setNbtData((CompoundTag) toNative(tag));
        }

        return block;
    }

	@Override
    @SuppressWarnings("deprecation")
    public boolean setBlock(Location location, BaseBlock block, boolean notifyAndLight) {
        checkNotNull(location);
        checkNotNull(block);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        BlockState blockState = location.getBlock().getState();
        blockState.setTypeId(block.getId());
        blockState.setRawData((byte)block.getData());
        boolean changed = blockState.update(true, false);

        // Copy NBT data for the block
        CompoundTag nativeTag = block.getNbtData();
        if (nativeTag != null) {
            // We will assume that the tile entity was created for us,
            // though we do not do this on the Forge version
            TileEntity tileEntity = craftWorld.getHandle().getTileEntity(new BlockPosition(x, y, z));
            if (tileEntity != null) {
                NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                tag.set("x", new NBTTagInt(x));
                tag.set("y", new NBTTagInt(y));
                tag.set("z", new NBTTagInt(z));
                readTagIntoTileEntity(tag, tileEntity); // Load data
            }
        }

        return changed |= blockState.update(false, notifyAndLight);
    }

    @Override
    public BaseEntity getEntity(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);

        CraftEntity craftEntity = ((CraftEntity) entity);
        Entity mcEntity = craftEntity.getHandle();

        String id = getEntityId(mcEntity);

        if (id != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readEntityIntoTag(mcEntity, tag);
            return new BaseEntity(id, (CompoundTag) toNative(tag));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public org.bukkit.entity.Entity createEntity(Location location, BaseEntity state) {
        checkNotNull(location);
        checkNotNull(state);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        WorldServer worldServer = craftWorld.getHandle();

        Entity createdEntity = createEntityFromId(state.getTypeId(), craftWorld.getHandle());

        if (createdEntity != null) {
            CompoundTag nativeTag = state.getNbtData();
            if (nativeTag != null) {
                NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.remove(name);
                }
                readTagIntoEntity(tag, createdEntity);
            }

            createdEntity.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            worldServer.addEntity(createdEntity, SpawnReason.CUSTOM);
            return createdEntity.getBukkitEntity();
        } else {
            return null;
        }
    }

    /**
     * Converts from a non-native NMS NBT structure to a native WorldEdit NBT
     * structure.
     *
     * @param foreign non-native NMS NBT structure
     * @return native WorldEdit NBT structure
     */
    private Tag toNative(NBTBase foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof NBTTagCompound) {
            Map<String, Tag> values = new HashMap<String, Tag>();
            Set<String> foreignKeys = ((NBTTagCompound) foreign).c();

            for (String str : foreignKeys) {
                NBTBase base = ((NBTTagCompound) foreign).get(str);
                values.put(str, toNative(base));
            }
            return new CompoundTag(values);
        } else if (foreign instanceof NBTTagByte) {
            return new ByteTag(((NBTTagByte) foreign).f()); // getByte
        } else if (foreign instanceof NBTTagByteArray) {
            return new ByteArrayTag(((NBTTagByteArray) foreign).c()); // data
        } else if (foreign instanceof NBTTagDouble) {
            return new DoubleTag(((NBTTagDouble) foreign).g()); // getDouble
        } else if (foreign instanceof NBTTagFloat) {
            return new FloatTag(((NBTTagFloat) foreign).h()); // getFloat
        } else if (foreign instanceof NBTTagInt) {
            return new IntTag(((NBTTagInt) foreign).d()); // getInt
        } else if (foreign instanceof NBTTagIntArray) {
            return new IntArrayTag(((NBTTagIntArray) foreign).c()); // data
        } else if (foreign instanceof NBTTagList) {
            try {
                return toNativeList((NBTTagList) foreign);
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Failed to convert NBTTagList", e);
                return new ListTag(ByteTag.class, new ArrayList<ByteTag>());
            }
        } else if (foreign instanceof NBTTagLong) {
            return new LongTag(((NBTTagLong) foreign).c()); // getLong
        } else if (foreign instanceof NBTTagShort) {
            return new ShortTag(((NBTTagShort) foreign).e()); // getShort
        } else if (foreign instanceof NBTTagString) {
            return new StringTag(((NBTTagString) foreign).a_()); // data
        } else if (foreign instanceof NBTTagEnd) {
            return new EndTag();
        } else {
            throw new IllegalArgumentException("Don't know how to make native " + foreign.getClass().getCanonicalName());
        }
    }

    /**
     * Convert a foreign NBT list tag into a native WorldEdit one.
     *
     * @param foreign the foreign tag
     * @return the converted tag
     * @throws NoSuchFieldException on error
     * @throws SecurityException on error
     * @throws IllegalArgumentException on error
     * @throws IllegalAccessException on error
     */
    @SuppressWarnings("rawtypes")
    private ListTag toNativeList(NBTTagList foreign) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        List<Tag> values = new ArrayList<Tag>();
        int type = foreign.d();

        List foreignList;
        foreignList = (List) nbtListTagListField.get(foreign);
        for (int i = 0; i < foreign.size(); i++) {
            NBTBase element = (NBTBase) foreignList.get(i);
            values.add(toNative(element)); // List elements shouldn't have names
        }

        Class<? extends Tag> cls = NBTConstants.getClassFromType(type);
        return new ListTag(cls, values);
    }

    /**
     * Converts a WorldEdit-native NBT structure to a NMS structure.
     *
     * @param foreign structure to convert
     * @return non-native structure
     */
    private NBTBase fromNative(Tag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundTag) {
            NBTTagCompound tag = new NBTTagCompound();
            for (Map.Entry<String, Tag> entry : ((CompoundTag) foreign)
                    .getValue().entrySet()) {
                tag.set(entry.getKey(), fromNative(entry.getValue()));
            }
            return tag;
        } else if (foreign instanceof ByteTag) {
            return new NBTTagByte(((ByteTag) foreign).getValue());
        } else if (foreign instanceof ByteArrayTag) {
            return new NBTTagByteArray(((ByteArrayTag) foreign).getValue());
        } else if (foreign instanceof DoubleTag) {
            return new NBTTagDouble(((DoubleTag) foreign).getValue());
        } else if (foreign instanceof FloatTag) {
            return new NBTTagFloat(((FloatTag) foreign).getValue());
        } else if (foreign instanceof IntTag) {
            return new NBTTagInt(((IntTag) foreign).getValue());
        } else if (foreign instanceof IntArrayTag) {
            return new NBTTagIntArray(((IntArrayTag) foreign).getValue());
        } else if (foreign instanceof ListTag) {
            NBTTagList tag = new NBTTagList();
            ListTag foreignList = (ListTag) foreign;
            for (Tag t : foreignList.getValue()) {
                tag.add(fromNative(t));
            }
            return tag;
        } else if (foreign instanceof LongTag) {
            return new NBTTagLong(((LongTag) foreign).getValue());
        } else if (foreign instanceof ShortTag) {
            return new NBTTagShort(((ShortTag) foreign).getValue());
        } else if (foreign instanceof StringTag) {
            return new NBTTagString(((StringTag) foreign).getValue());
        } else if (foreign instanceof EndTag) {
            try {
                return (NBTBase) nbtCreateTagMethod.invoke(null, (byte) 0);
            } catch (Exception e) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS " + foreign.getClass().getCanonicalName());
        }
    }

	@Override
	public ItemStack createSkull(ItemStack item, String uniqueId, String value) {
		if(!Material.SKULL_ITEM.equals(item.getType())) {
			return null;
		}

		net.minecraft.server.v1_9_R2.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

		// Set textures
		NBTTagCompound texture = new NBTTagCompound();
		texture.setString("Value", value);

		NBTTagList textures = new NBTTagList();
		textures.add(texture);

		NBTTagCompound properties = new NBTTagCompound();
		properties.set("textures", textures);

		// Set unique id and textures
		NBTTagCompound owner = new NBTTagCompound();
		owner.setString("Id", uniqueId);
		owner.set("Properties", properties);

		NBTTagCompound tag = nmsItem.getTag();
		if(tag == null) tag = new NBTTagCompound();
		tag.set("SkullOwner", owner);
		nmsItem.setTag(tag);

		return CraftItemStack.asCraftMirror(nmsItem);
	}

	@SuppressWarnings("deprecation")
	@Override
	public ItemStack createSpawnEgg(ItemStack item, EntityType type) {
		if (!Material.MONSTER_EGG.equals(item.getType())) {
			return item;
		}

		try {
			net.minecraft.server.v1_9_R2.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
			NBTTagCompound tag = nmsItem.getTag();
			if (tag == null) {
				tag = new NBTTagCompound();
				NBTTagCompound entityTag = new NBTTagCompound();
				entityTag.setString("id", type.getName());
				entityTag.setString("CustomName", "");
				entityTag.setBoolean("CustomNameVisible", false);
				tag.set("EntityTag", entityTag);
			} else {
				NBTTagCompound existing = tag.getCompound("EntityTag");
				if (existing != null) {
					existing.setString("id", type.getName());
					tag.set("EntityTag", existing);
				} else {
					NBTTagCompound entityTag = new NBTTagCompound();
					entityTag.setString("id", type.getName());
					entityTag.setString("CustomName", "");
					entityTag.setBoolean("CustomNameVisible", false);
					tag.set("EntityTag", entityTag);
				}
			}
			nmsItem.setTag(tag);
			return CraftItemStack.asCraftMirror(nmsItem);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("NMSUtils.createSpawnEgg - error: %s", e.getMessage()));
			e.printStackTrace();
		}
		return item;
	}

	@Override
	public void damageLivingEntity(org.bukkit.entity.LivingEntity entity, DamageCause cause, float amount) {
		CraftLivingEntity craftEntity = ((CraftLivingEntity) entity);
		EntityLiving mcEntity = craftEntity.getHandle();
		mcEntity.damageEntity(fromDamageCause(cause), amount);
	}

	private DamageSource fromDamageCause(DamageCause cause) {
		switch (cause) {
		case VOID:
			return DamageSource.OUT_OF_WORLD;
		default:
			return DamageSource.GENERIC;
		}
	}

	@Override
	public List<CustomItemStack> toCustomItemStackList(List<ItemStack> loadout) {
		List<CustomItemStack> items = new ArrayList<>();
		if (loadout == null) {
			return items;
		}
		for (int i = 0; i < loadout.size(); ++i) {
			if (loadout.get(i) != null) {
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte("Slot", (byte) i);
				CraftItemStack.asNMSCopy(loadout.get(i)).save(nbttagcompound1);
				items.add(new CustomItemStack((CompoundTag) toNative(nbttagcompound1)));
			}
		}
		return items;
	}

	@Override
	public List<ItemStack> toItemStackList(List<CustomItemStack> loadout) {
		List<ItemStack> items = new ArrayList<>();
		if (loadout == null) {
			return items;
		}
		for (CustomItemStack custom : loadout) {
			if (custom != null && custom.getNbtData() != null) {
				byte slot = custom.getNbtData().getByte("Slot");
				ItemStack stack = fromNbtData(custom.getNbtData());
				while (items.size() <= slot) {
					items.add(null);
				}
				items.set(slot, stack);
			}
		}
		return items;
	}

	private ItemStack fromNbtData(CompoundTag tag) {
		try {
			return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_9_R2.ItemStack.createStack((NBTTagCompound) fromNative(tag)));
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("NMS_Spigot_v1_9_R2.fromNbtData - error: %s", e.getMessage()));
		}
		return null;
	}

}
