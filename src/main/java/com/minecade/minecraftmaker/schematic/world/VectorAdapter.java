package com.minecade.minecraftmaker.schematic.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Deserializes {@code Vector}s for GSON.
 */
public class VectorAdapter implements JsonDeserializer<Vector> {

	@Override
	public Vector deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonArray jsonArray = json.getAsJsonArray();
		if (jsonArray.size() != 3) {
			throw new JsonParseException("Expected array of 3 length for Vector");
		}

		double x = jsonArray.get(0).getAsDouble();
		double y = jsonArray.get(1).getAsDouble();
		double z = jsonArray.get(2).getAsDouble();

		return new Vector(x, y, z);
	}
}
