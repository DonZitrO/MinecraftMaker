package com.minecade.minecraftmaker.function.operation;

import java.util.List;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Executes a delegate operation, but returns to another operation upon
 * completing the delegate.
 */
public class DelegateOperation implements Operation {

	private final Operation original;
	private Operation delegate;

	/**
	 * Create a new operation delegate.
	 *
	 * @param original
	 *            the operation to return to
	 * @param delegate
	 *            the delegate operation to complete before returning
	 */
	public DelegateOperation(Operation original, Operation delegate) {
		checkNotNull(original);
		checkNotNull(delegate);
		this.original = original;
		this.delegate = delegate;
	}

	@Override
	public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
		if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | DelegateOperation.resume - about to resume operation: [%s]", delegate));
		}
		delegate = delegate.resume(run);
		return delegate != null ? this : original;
	}

	@Override
	public void cancel() {
		delegate.cancel();
		original.cancel();
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		original.addStatusMessages(messages);
		delegate.addStatusMessages(messages);
	}

}
