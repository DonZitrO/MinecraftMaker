package com.minecade.minecraftmaker.items;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Maps;
import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum SkullTypeItem implements TranslatableItem {

	CHARACTERS("327722ba-304d-4cc3-a3d7-b037f3990d64", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2NlYmM5Nzc5OGMyZTM2MDU1MWNhYjNkZDVkYjZkNTM0OTdmZTYzMDQwOTQxYzlhYzQ5MWE1OWNiZjM4M2E3YSJ9fX0="),

	COLORS("c8fee7ee-b067-48cb-80c8-337a63edd0b1", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzZmNjlmN2I3NTM4YjQxZGMzNDM5ZjM2NThhYmJkNTlmYWNjYTM2NmYxOTBiY2YxZDZkMGEwMjZjOGY5NiJ9fX0="),

	DEVICES("81c080fd-022b-477c-9f24-f637324d4c4c", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFlNTJhZThjOThhYzE5ZmQwNzYzN2E0NjlmZmEyNTZhYjBiM2IxMGVjZTYyNDMxODYxODhiYTM4ZGYxNTQifX19"),

	FOOD("201bdf0f-79ec-444f-a5ec-1a855fcddaf7", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNjM2Y3ODFjOTIzYTI4ODdmMTRjMWVlYTExMDUwMTY2OTY2ZjI2MDI1Nzg0MDFmMTQ1MWU2MDk3Yjk3OWRmIn19fQ=="),

	GAMES("afbe4c67-a6a5-4559-ad06-78a6ed2ab4e9", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzk3OTU1NDYyZTRlNTc2NjY0NDk5YWM0YTFjNTcyZjYxNDNmMTlhZDJkNjE5NDc3NjE5OGY4ZDEzNmZkYjIifX19"),

	HALLOWEEN("f0f30342-2ce7-41bf-9e5f-5a3fb18a141b", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzk2ZjUzOTg0NTJlY2QxOGZlNmZkZGIyY2ZmYWZiM2UxYmZjN2I2N2ZiNTUzZWU3ZmQ4Mjc3ZGU1Zjg3NjMifX19"),

	INTERIOR("0ceac85e-159d-4f9d-a1c2-c8acde792f23", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjFkZDRmZTRhNDI5YWJkNjY1ZGZkYjNlMjEzMjFkNmVmYTZhNmI1ZTdiOTU2ZGI5YzVkNTljOWVmYWIyNSJ9fX0="),

	MISC("822833e6-c3ec-457e-aeef-1fac97799e5f", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzAyZTIyZjY1MDNjMzYzZGY2OWJmOWU5NDQ4ZmU4OWQyZjA1YmFlMzA1MzRiOGJiMTlkMjY4ZjA5ODliOTYifX19"),

	MOBS("7d3a8ace-e045-4eba-ab71-71dbf525daf1", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTYzODQ2OWE1OTljZWVmNzIwNzUzNzYwMzI0OGE5YWIxMWZmNTkxZmQzNzhiZWE0NzM1YjM0NmE3ZmFlODkzIn19fQ=="),

	POKEMON("84bef394-4bd1-4e65-aafb-2c746cb54a94", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjUzZWJjOTc2Y2I2NzcxZjNlOTUxMTdiMzI2ODQyZmY3ODEyYzc0MGJlY2U5NmJiODg1ODM0NmQ4NDEifX19");

	private final static Map<String, SkullTypeItem> BY_DISPLAY_NAME = Maps.newHashMap();

	public static SkullTypeItem getSkullTypeItemByDisplayName(final String displayName) {
		return BY_DISPLAY_NAME.get(displayName);
	}

	private final ItemStackBuilder builder;

	private SkullTypeItem(String uniqueId, String texture){
		this.builder = new ItemBuilder(uniqueId, texture);
	}

	@Override
	public ItemStackBuilder getBuilder() {
		return builder;
	}

	public String getDisplayName() {
		return builder.getDisplayName() != null ? builder.getDisplayName() : name();
	}

	public ItemStack getItem() {
		return builder.build();
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getTranslationKeyBase() {
		return "menu.skulltype";
	}

	@Override
	public void setDisplayName(String displayName) {
		getBuilder().withDisplayName(displayName);
		BY_DISPLAY_NAME.put(displayName, this);
	}

}
