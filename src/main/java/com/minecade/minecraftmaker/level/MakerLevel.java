package com.minecade.minecraftmaker.level;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.data.MakerLevelClearData;
import com.minecade.minecraftmaker.data.MakerRelativeLocationData;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerLevel {

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

	private List<MakerLevelClearData> levelsClear;

	public MakerLevel(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
		this.levelsClear = new ArrayList<MakerLevelClearData>();
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

	public long getLikes() {
		return likes;
	}

	public MakerRelativeLocationData getRelativeEndLocation() {
		return relativeEndLocation;
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

	public void setAuthorId(UUID authorId) {
		this.authorId = authorId;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public void setAuthorRank(Rank authorRank) {
		this.authorRank = authorRank;
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

	public void setLikes(long likes) {
		this.likes = likes;
	}

	public void setRelativeEndLocation(MakerRelativeLocationData relativeEndLocation) {
		this.relativeEndLocation = relativeEndLocation;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MakerLevel other = (MakerLevel) obj;
		if (levelId == null) {
			if (other.levelId != null)
				return false;
		} else if (!levelId.equals(other.levelId))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((levelId == null) ? 0 : levelId.hashCode());
		return result;
	}

}
