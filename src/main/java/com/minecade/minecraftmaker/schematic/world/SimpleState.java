package com.minecade.minecraftmaker.schematic.world;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;

class SimpleState implements State {

	private Byte dataMask;
	private Map<String, SimpleStateValue> values;

	@Override
	public Map<String, SimpleStateValue> valueMap() {
		return Collections.unmodifiableMap(values);
	}

	@Nullable
	@Override
	public StateValue getValue(BaseBlock block) {
		for (StateValue value : values.values()) {
			if (value.isSet(block)) {
				return value;
			}
		}

		return null;
	}

	byte getDataMask() {
		return dataMask != null ? dataMask : 0xF;
	}

	@Override
	public boolean hasDirection() {
		for (SimpleStateValue value : values.values()) {
			if (value.getDirection() != null) {
				return true;
			}
		}

		return false;
	}

	void postDeserialization() {
		for (SimpleStateValue v : values.values()) {
			v.setState(this);
		}
	}

}
