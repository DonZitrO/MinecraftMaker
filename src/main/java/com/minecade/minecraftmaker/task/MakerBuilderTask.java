package com.minecade.minecraftmaker.task;

import java.util.LinkedList;

import org.bukkit.scheduler.BukkitRunnable;

public class MakerBuilderTask extends BukkitRunnable {

	// FIXME define interface for generics
	private LinkedList<?> pendingBuildJobs = new LinkedList<>();
	
	@Override
	public void run() {

	}

}
