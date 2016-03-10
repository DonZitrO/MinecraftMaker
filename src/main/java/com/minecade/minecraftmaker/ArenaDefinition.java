package com.minecade.minecraftmaker;

import java.util.UUID;

import com.minecade.core.gamebase.MinigameLocation;

public class ArenaDefinition {
	
	private String author;
	private UUID authorUUID;
	private String name;
	private int likes = 0;
	private int dislikes = 0;
	private MinigameLocation spawn = null;
	private MinigameLocation finish = null;
	
	public ArenaDefinition(String author, UUID authorUUID, String name) {
		this.author = author;
		this.authorUUID = authorUUID;
		this.name = name;
	}
	
	public ArenaDefinition(String author, UUID authorUUID, String name, int likes, int dislikes) {
		this.author = author;
		this.authorUUID = authorUUID;
		this.name = name;
		this.likes = likes;
		this.dislikes = dislikes;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLikes() {
		return likes;
	}

	public void setLikes(int likes) {
		this.likes = likes;
	}

	public int getDislikes() {
		return dislikes;
	}

	public void setDislikes(int dislikes) {
		this.dislikes = dislikes;
	}

	public UUID getAuthorUUID() {
		return authorUUID;
	}

	public MinigameLocation getSpawn() {
		return spawn;
	}

	public void setSpawn(MinigameLocation spawn) {
		this.spawn = spawn;
	}

	public MinigameLocation getFinish() {
		return finish;
	}

	public void setFinish(MinigameLocation finish) {
		this.finish = finish;
	}
}
