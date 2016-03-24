package com.minecade.minecraftmaker.schematic.block;

import java.util.HashMap;
import java.util.Map;

import com.minecade.minecraftmaker.schematic.jnbt.ByteTag;
import com.minecade.minecraftmaker.schematic.jnbt.CompoundTag;
import com.minecade.minecraftmaker.schematic.jnbt.StringTag;
import com.minecade.minecraftmaker.schematic.jnbt.Tag;

/**
 * A skull block.
 */
public class SkullBlock extends BaseBlock implements TileEntityBlock {

	private String owner = ""; // notchian
	private byte skullType; // stored here for block, in damage value for item
	private byte rot; // only matters if block data == 0x1 (on floor)

	/**
	 * Construct the skull block with a default type of skelton.
	 * 
	 * @param data
	 *            data value to set, controls placement
	 */
	public SkullBlock(int data) {
		this(data, (byte) 0);
	}

	/**
	 * Construct the skull block with a given type. 0 - skeleton 1 - wither
	 * skelly 2 - zombie 3 - human 4 - creeper
	 * 
	 * @param data
	 *            data value to set, controls placement
	 * @param type
	 *            type of skull
	 */
	public SkullBlock(int data, byte type) {
		this(data, type, (byte) 0);
	}

	/**
	 * Construct the skull block with a given type and rotation.
	 * 
	 * @param data
	 *            data value to set, controls placement
	 * @param type
	 *            type of skull
	 * @param rot
	 *            rotation (if on floor)
	 */
	public SkullBlock(int data, byte type, byte rot) {
		super(BlockID.HEAD, data);
		if (type < (byte) 0 || type > (byte) 4) {
			this.skullType = (byte) 0;
		} else {
			this.skullType = type;
		}
		this.rot = rot;
		this.owner = "";
	}

	/**
	 * Construct the skull block with a given rotation and owner. The type is
	 * assumed to be player unless owner is null or empty.
	 * 
	 * @param data
	 *            data value to set, controls placement
	 * @param rot
	 *            rotation of skull
	 * @param owner
	 *            name of player
	 */
	public SkullBlock(int data, byte rot, String owner) {
		super(BlockID.HEAD, data);
		this.rot = rot;
		this.setOwner(owner);
		if (owner == null || owner.isEmpty())
			this.skullType = (byte) 0;
	}

	/**
	 * Set the skull's owner. Automatically sets type to player if not empty or
	 * null.
	 * 
	 * @param owner
	 *            player name to set the skull to
	 */
	public void setOwner(String owner) {
		if (owner == null) {
			this.owner = "";
		} else {
			if (owner.length() > 16 || owner.isEmpty())
				this.owner = "";
			else
				this.owner = owner;
		}
		if (this.owner != null && !this.owner.isEmpty())
			this.skullType = (byte) 3;
	}

	/**
	 * Get the skull's owner. Returns null if unset.
	 * 
	 * @return player name or null
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Get the type of skull.
	 * 
	 * @return the skullType
	 */
	public byte getSkullType() {
		return skullType;
	}

	/**
	 * Set the type of skull;
	 * 
	 * @param skullType
	 *            the skullType to set
	 */
	public void setSkullType(byte skullType) {
		this.skullType = skullType;
	}

	/**
	 * Get rotation of skull. This only means anything if the block data is 1.
	 * 
	 * @return the rotation
	 */
	public byte getRot() {
		return rot;
	}

	/**
	 * Set the rotation of skull.
	 * 
	 * @param rot
	 *            the rotation to set
	 */
	public void setRot(byte rot) {
		this.rot = rot;
	}

	@Override
	public boolean hasNbtData() {
		return true;
	}

	@Override
	public String getNbtId() {
		return "Skull";
	}

	@Override
	public CompoundTag getNbtData() {
		Map<String, Tag> values = new HashMap<String, Tag>();
		values.put("SkullType", new ByteTag(skullType));
		if (owner == null)
			owner = "";
		values.put("ExtraType", new StringTag(owner));
		values.put("Rot", new ByteTag(rot));
		return new CompoundTag(values);
	}

	@Override
	public void setNbtData(CompoundTag rootTag) {
		if (rootTag == null) {
			return;
		}

		Map<String, Tag> values = rootTag.getValue();

		Tag t;

		t = values.get("id");
		if (!(t instanceof StringTag) || !((StringTag) t).getValue().equals("Skull")) {
			throw new RuntimeException("'Skull' tile entity expected");
		}

		t = values.get("SkullType");
		if (t instanceof ByteTag) {
			skullType = ((ByteTag) t).getValue();
		}
		t = values.get("ExtraType");
		if (t != null && t instanceof StringTag) {
			owner = ((StringTag) t).getValue();
		}
		t = values.get("Rot");
		if (t instanceof ByteTag) {
			rot = ((ByteTag) t).getValue();
		}
	}

}
