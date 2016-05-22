package com.minecade.minecraftmaker.level;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerDisplayableLevel extends AbstractMakerLevel implements Comparable<MakerDisplayableLevel> {

	public MakerDisplayableLevel(MinecraftMakerPlugin plugin) {
		super(plugin);
	}

	@Override
	public int compareTo(MakerDisplayableLevel o) {
		return Long.valueOf(levelSerial).compareTo(o.levelSerial);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (levelSerial ^ (levelSerial >>> 32));
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
		AbstractMakerLevel other = (AbstractMakerLevel) obj;
		if (levelSerial != other.levelSerial)
			return false;
		return true;
	}

}
