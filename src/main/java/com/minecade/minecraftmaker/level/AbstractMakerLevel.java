package com.minecade.minecraftmaker.level;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bukkit.WeatherType;

import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.data.MakerLevelClearData;
import com.minecade.minecraftmaker.data.MakerRelativeLocationData;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class AbstractMakerLevel {

	protected final MinecraftMakerPlugin plugin;

	protected UUID authorId;
	protected String authorName;
	protected Rank authorRank;
	protected long clearedByAuthorMillis;
	protected Date datePublished;
	protected long dislikes;
	protected long favs;
	protected UUID levelId;
	protected String levelName;
	protected long levelSerial;
	protected long likes;
	protected MakerRelativeLocationData relativeEndLocation;
	protected long trendingScore;

	protected long levelTime = 6000;
	protected WeatherType levelWeather = WeatherType.CLEAR;

	protected List<MakerLevelClearData> levelsClear = new ArrayList<MakerLevelClearData>();;

	public AbstractMakerLevel(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public String getAuthorName() {
		return authorName;
	}

	public Rank getAuthorRank() {
		return authorRank;
	}

	public long getClearedByAuthorMillis() {
		return clearedByAuthorMillis;
	}

	public Date getDatePublished() {
		return datePublished;
	}

	public long getDislikes() {
		return dislikes;
	}

	public long getFavs() {
		return favs;
	}

	public UUID getLevelId() {
		return levelId;
	}

	public String getLevelName() {
		return levelName;
	}

	public List<MakerLevelClearData> getLevelsClear() {
		return levelsClear;
	}

	public long getLevelSerial() {
		return levelSerial;
	}

	public long getLevelTime() {
		return levelTime;
	}

	public WeatherType getLevelWeather() {
		return levelWeather;
	}

	public long getLikes() {
		return likes;
	}

	public MakerRelativeLocationData getRelativeEndLocation() {
		return relativeEndLocation;
	}

	public long getTrendingScore() {
		return trendingScore;
	}

	public boolean isClearedByAuthor() {
		return clearedByAuthorMillis > 0;
	}

	public boolean isPlayableByEditor() {
		return relativeEndLocation != null;
	}

	public boolean isPublished() {
		return datePublished != null;
	}

	protected void reset() {
		authorId = null;
		authorName = null;
		authorRank = null;
		clearedByAuthorMillis = 0;
		datePublished = null;
		dislikes = 0;
		favs = 0;
		levelId = null;
		levelName = null;
		levelSerial = 0;
		likes = 0;
		relativeEndLocation = null;
		// FIXME
		levelsClear.clear();
	}

	public void setAuthorId(UUID authorId) {
		this.authorId = authorId;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public void setAuthorRank(Rank authorRank) {
		this.authorRank = authorRank;
	}

	public void setClearedByAuthorMillis(long clearedByAuthorMillis) {
		this.clearedByAuthorMillis = clearedByAuthorMillis;
	}

	public void setDatePublished(Date datePublished) {
		this.datePublished = datePublished;
	}

	public void setDislikes(long dislikes) {
		this.dislikes = dislikes;
	}

	public void setLevelId(UUID levelId) {
		this.levelId = levelId;
	}

	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	public void setLevelSerial(long levelSerial) {
		this.levelSerial = levelSerial;
	}

	public void setLevelTime(long levelTime) {
		this.levelTime = levelTime;
	}

	public void setLevelWeather(WeatherType levelWeather) {
		this.levelWeather = levelWeather;
	}

	public void setLikes(long likes) {
		this.likes = likes;
	}

	public void setRelativeEndLocation(MakerRelativeLocationData relativeEndLocation) {
		this.relativeEndLocation = relativeEndLocation;
	}

	public void setTrendingScore(long trendingScore) {
		this.trendingScore = trendingScore;
	}

}
