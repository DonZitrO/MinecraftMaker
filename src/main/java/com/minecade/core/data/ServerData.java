package com.minecade.core.data;

import java.util.Comparator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerData {

	private static final String SERVER_NUMBER_KEY = "serverNumber";
	private static final String ONLINE_PLAYERS_KEY = "onlinePlayers";
	private static final String MAX_PLAYERS_KEY = "maxPlayers";
	private static final String STATUS_KEY = "status";
	private static final String UPDATED_KEY = "updated";

	public static final Comparator<ServerData> SORT_BY_PLAYERS, SORT_BY_SERVER_NUMBER;
	static {
		SORT_BY_PLAYERS = new Comparator<ServerData>() {
			@Override
			public int compare(ServerData server1, ServerData server2) {
				return (server1.getMaxPlayers() - server1.getOnlinePlayers()) - (server2.getMaxPlayers() - server2.getOnlinePlayers());
			}
		};
		SORT_BY_SERVER_NUMBER = new Comparator<ServerData>() {
			@Override
			public int compare(ServerData server1, ServerData server2) {
				return server1.getServerNumber() - server2.getServerNumber();
			}
		};
	}

	private final JSONObject json = new JSONObject();
	private final JSONParser parser = new JSONParser();

	public int getServerNumber() {
		Object value = json.get(SERVER_NUMBER_KEY);
		return value != null ? Integer.valueOf(String.valueOf(value)) : 0;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void fromJSON(String jsonString) throws ParseException {
		json.putAll((Map) parser.parse(jsonString));
	}

	public byte[] getBytes() {
		return json.toJSONString().getBytes();
	}

	@SuppressWarnings("unchecked")
	public void setServerNumber(int serverNumber) {
		json.put(SERVER_NUMBER_KEY, serverNumber);
	}

	public int getOnlinePlayers() {
		Object value = json.get(ONLINE_PLAYERS_KEY);
		return value != null ? Integer.valueOf(String.valueOf(value)) : 0;
	}

	@SuppressWarnings("unchecked")
	public void setOnlinePlayers(int onlinePlayers) {
		json.put(ONLINE_PLAYERS_KEY, onlinePlayers);
	}

	public int getMaxPlayers() {
		Object value = json.get(MAX_PLAYERS_KEY);
		return value != null ? Integer.valueOf(String.valueOf(value)) : 0;
	}

	@SuppressWarnings("unchecked")
	public void setMaxPlayers(int maxPlayers) {
		json.put(MAX_PLAYERS_KEY, maxPlayers);
	}

	public ServerStatus getStatus() {
		Object value = json.get(STATUS_KEY);
		return value != null ? ServerStatus.valueOf(String.valueOf(value)) : null;
	}

	@SuppressWarnings("unchecked")
	public void setStatus(ServerStatus status) {
		json.put(STATUS_KEY, status.name());
	}

	@Override
	public String toString() {
		return json.toJSONString();
	}

	public long getTimeUpdated() {
		Object value = json.get(UPDATED_KEY);
		return value != null ? Long.valueOf(String.valueOf(value)) : 0;
	}

	@SuppressWarnings("unchecked")
	public void setTimeUpdated(long timeUpdated) {
		json.put(UPDATED_KEY, timeUpdated);
	}

}
