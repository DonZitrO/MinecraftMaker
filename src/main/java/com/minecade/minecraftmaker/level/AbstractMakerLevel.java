package com.minecade.minecraftmaker.level;

import java.util.Date;
import java.util.UUID;

import com.minecade.mcore.data.MRelativeLocationData;
import com.minecade.mcore.data.Rank;
import com.minecade.mcore.world.WorldTimeAndWeather;
import com.minecade.minecraftmaker.data.MakerLevelClearData;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractMakerLevel {

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
	protected MRelativeLocationData relativeEndLocation;
	protected long trendingScore;
	protected MakerLevelClearData levelBestClearData;
	protected boolean unpublished;

	protected WorldTimeAndWeather timeAndWeather = WorldTimeAndWeather.NOON_CLEAR;
	protected WorldTimeAndWeather timeAndWeatherChangeRequest;

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

	public MakerLevelClearData getLevelBestClearData() {
		return levelBestClearData;
	}

	public UUID getLevelId() {
		return levelId;
	}

//	public List<MakerLevelClearData> getLevelsClear() {
//		return levelsClear;
//	}

	public String getLevelName() {
		return levelName;
	}

	public long getLevelSerial() {
		return levelSerial;
	}

	public long getLikes() {
		return likes;
	}

	public MRelativeLocationData getRelativeEndLocation() {
		return relativeEndLocation;
	}

	public WorldTimeAndWeather getTimeAndWeather() {
		return timeAndWeather;
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

	public boolean isUnpublished() {
		return unpublished;
	}

	public void requestTimeAndWeatherChange(WorldTimeAndWeather timeAndWeather) {
		this.timeAndWeatherChangeRequest = timeAndWeather;
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
		levelBestClearData = null;
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

	public void setLevelBestClearData(MakerLevelClearData levelBestClearData) {
		this.levelBestClearData = levelBestClearData;
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

	public void setLikes(long likes) {
		this.likes = likes;
	}

	public void setRelativeEndLocation(MRelativeLocationData relativeEndLocation) {
		this.relativeEndLocation = relativeEndLocation;
	}

	public void setTrendingScore(long trendingScore) {
		this.trendingScore = trendingScore;
	}

	public void setUnpublished(boolean unpublished) {
		this.unpublished = unpublished;
	}

}
