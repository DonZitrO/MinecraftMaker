package com.minecade.core.data;

import java.util.Date;
import java.util.UUID;

public class CoinTransaction {

	public static enum Reason {
		FIRST_TIME_LEVEL_CLEAR,
		FIRST_TIME_STEVE_CHALLENGE_CLEAR,
		STEVE_CHALLENGE_CLEAR,
		POPULAR_LEVEL_RECORD_BEAT,
		LEVEL_DELETE,
		LEVEL_UNPUBLISH,
		OTHER;
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
	private final String reasonDescription;
	private Date dateCommitted;

	public CoinTransaction(UUID transactionId, UUID playerId, long amount, UUID source, SourceType sourceType, Reason reason, String reasonDescription) {
		this.transactionId = transactionId;
		this.playerId = playerId;
		this.amount = amount;
		this.source = source;
		this.sourceType = sourceType;
		this.reason = reason;
		this.reasonDescription = reasonDescription;
	}

	public long getAmount() {
		return amount;
	}

	public Date getDateCommitted() {
		return dateCommitted;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	public Reason getReason() {
		return reason;
	}

	public String getReasonDescription() {
		return reasonDescription;
	}

	public UUID getSource() {
		return source;
	}


	public SourceType getSourceType() {
		return sourceType;
	}

	public UUID getTransactionId() {
		return transactionId;
	}

	public void setDateCommitted(Date dateCommitted) {
		this.dateCommitted = dateCommitted;
	}

	@Override
	public String toString() {
		return "CoinTransaction [transactionId=" + transactionId + ", playerId=" + playerId + ", amount=" + amount + ", source=" + source + ", sourceType=" + sourceType + ", reason=" + reason + ", reasonDescription=" + reasonDescription + ", dateCommitted=" + dateCommitted + "]";
	}

}
