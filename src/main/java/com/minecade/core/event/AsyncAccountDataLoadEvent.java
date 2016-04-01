package com.minecade.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.minecade.core.data.MinecadeAccountData;

public class AsyncAccountDataLoadEvent extends Event {

	private final MinecadeAccountData data;

	private static final HandlerList handlers = new HandlerList();

	public AsyncAccountDataLoadEvent(MinecadeAccountData data) {
		super(true);
		this.data = data;
	}

	public MinecadeAccountData getData() {
		return data;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
