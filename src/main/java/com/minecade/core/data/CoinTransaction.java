package com.minecade.core.data;

import java.util.Date;
import java.util.UUID;

public class CoinTransaction {

	public static enum Reason {
		WON_DUEL,
		LOST_DUEL,
		QUIT_DUEL,
		VICTORY,
		FLAWLESS_VICTORY,
		SECOND_PLACE,
		PLAYER_KILLS,
		RANK_BONUS,
		OTHER,
	}

	public static enum SourceType {
		SERVER,
		PLAYER,
		OTHER;
	}

	private final UUID transactionId;
	private final UUID playerId;
	private final long amount;
	private final UUID source;
	private final SourceType sourceType;
	private final Reason reason;
	private final Date created;

	public CoinTransaction(UUID transactionId, UUID playerId, long amount, UUID source, SourceType sourceType, Reason reason) {
		this.transactionId = transactionId;
		this.playerId = playerId;
		this.amount = amount;
		this.source = source;
		this.sourceType = sourceType;
		this.reason = reason;
		this.created = new Date();
	}

	public UUID getTransactionId() {
		return transactionId;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	public long getAmount() {
		return amount;
	}

	public UUID getSource() {
		return source;
	}

	public SourceType getSourceType() {
		return sourceType;
	}

	public Reason getReason() {
		return reason;
	}

	public Date getCreated() {
		return created;
	}

	@Override
	public String toString() {
		return "CoinTransaction: {transactionId=" + transactionId + "], [playerId=[" + playerId + "], amount=[" + amount + "], source=[" + source + "], sourceType=[" + sourceType + "], reason=[" + reason + "], created=[" + created + "]}";
	}

}