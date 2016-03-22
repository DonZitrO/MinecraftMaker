package com.minecade.minecraftmaker.schematic.world;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;

class SimpleStateValue implements StateValue {

	private SimpleState state;
	private Byte data;
	private Vector direction;

	void setState(SimpleState state) {
		this.state = state;
	}

	@Override
	public boolean isSet(BaseBlock block) {
		return data != null && (block.getData() & state.getDataMask()) == data;
	}

	@Override
	public boolean set(BaseBlock block) {
		if (data != null) {
			block.setData((block.getData() & ~state.getDataMask()) | data);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Vector getDirection() {
		return direction;
	}

}
