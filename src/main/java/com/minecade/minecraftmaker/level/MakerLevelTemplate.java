package com.minecade.minecraftmaker.level;

import java.util.UUID;

import com.minecade.mcore.data.MRelativeLocationData;
import com.minecade.mcore.schematic.io.Clipboard;
import com.minecade.mcore.schematic.io.ClipboardWrapper;
import com.minecade.mcore.world.WorldTimeAndWeather;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerLevelTemplate implements ClipboardWrapper {

	public static final int DEFAULT_COIN_COST = 1000;

	protected final MinecraftMakerPlugin plugin;

	protected UUID authorId;
	protected String authorName;
	protected UUID templateId;
	protected String templateName;
	protected MRelativeLocationData relativeEndLocation;
	protected WorldTimeAndWeather timeAndWeather = WorldTimeAndWeather.NOON_CLEAR;
	//protected int coinCost;
	protected boolean free;
	protected boolean vipOnly;
	protected Clipboard clipboard;

	public MakerLevelTemplate(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public String getAuthorName() {
		return authorName;
	}

//	public int getCoinCost() {
//		return coinCost;
//	}

	public Clipboard getClipboard() {
		return clipboard;
	}

	public MRelativeLocationData getRelativeEndLocation() {
		return relativeEndLocation;
	}

	public UUID getTemplateId() {
		return templateId;
	}

	public String getTemplateName() {
		return templateName;
	}

	public WorldTimeAndWeather getTimeAndWeather() {
		return timeAndWeather;
	}

	public boolean isFree() {
		return free;
	}

	public boolean isVipOnly() {
		return vipOnly;
	}

	public void setAuthorId(UUID authorId) {
		this.authorId = authorId;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

//	public void setCoinCost(int coinCost) {
//		this.coinCost = coinCost;
//	}

	public void setClipboard(Clipboard clipboard) {
		this.clipboard = clipboard;
	}

	public void setFree(boolean free) {
		this.free = free;
	}

	public void setRelativeEndLocation(MRelativeLocationData relativeEndLocation) {
		this.relativeEndLocation = relativeEndLocation;
	}

	public void setTemplateId(UUID templateId) {
		this.templateId = templateId;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public void setTimeAndWeather(WorldTimeAndWeather timeAndWeather) {
		this.timeAndWeather = timeAndWeather;
	}

	public void setVipOnly(boolean vipOnly) {
		this.vipOnly = vipOnly;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((templateName == null) ? 0 : templateName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MakerLevelTemplate other = (MakerLevelTemplate) obj;
		if (templateName == null) {
			if (other.templateName != null)
				return false;
		} else if (!templateName.equals(other.templateName))
			return false;
		return true;
	}

}
