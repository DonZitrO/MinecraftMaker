package com.minecade.minecraftmaker.function.operation;

public class LimitedTimeRunContext extends RunContext implements ResumableRunContext {

	private long endNanoTime;

	public LimitedTimeRunContext(long nanoTimeLimit) {
		this.endNanoTime = System.nanoTime() + nanoTimeLimit;
	}

	@Override
	public boolean shouldContinue() {
		return System.nanoTime() < this.endNanoTime;
	}

}
