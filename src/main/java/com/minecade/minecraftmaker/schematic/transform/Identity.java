package com.minecade.minecraftmaker.schematic.transform;

import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Makes no transformation to given vectors.
 */
public class Identity implements Transform {

	@Override
	public boolean isIdentity() {
		return true;
	}

	@Override
	public Vector apply(Vector vector) {
		return vector;
	}

	@Override
	public Transform inverse() {
		return this;
	}

	@Override
	public Transform combine(Transform other) {
		if (other instanceof Identity) {
			return this;
		} else {
			return other;
		}
	}

}
