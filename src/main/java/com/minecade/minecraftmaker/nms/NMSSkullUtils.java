package com.minecade.minecraftmaker.nms;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_9_R1.NBTTagCompound;
import net.minecraft.server.v1_9_R1.NBTTagList;

public class NMSSkullUtils {

	public static ItemStack createSkull(ItemStack item, String uniqueId, String value) {
		if(!Material.SKULL_ITEM.equals(item.getType())) return null;

		net.minecraft.server.v1_9_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

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

}
