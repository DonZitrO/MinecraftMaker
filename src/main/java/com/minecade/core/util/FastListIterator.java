package com.minecade.core.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A fast iterator for lists that uses an internal index integer and caches the
 * size of the list. The size of the list cannot change during iteration and
 * {@link Iterator#remove()} is not supported.
 *
 * <p>
 * The iterator in Java, at least in older Java versions, is very slow, causing
 * a significant amount of time in operations in WorldEdit being spent on
 * {@link Iterator#hasNext()}. In contrast, the iterator implemented by this
 * class is very quick, as long as {@link List#get(int)} is fast.
 * </p>
 *
 * @param <E>
 *            the element
 */
public class FastListIterator<E> implements Iterator<E> {

	private final List<E> list;
	private int index;
	private final int size;
	private final int increment;

	/**
	 * Create a new fast iterator.
	 *
	 * @param list
	 *            the list
	 * @param index
	 *            the index to start from
	 * @param size
	 *            the size of the list
	 * @param increment
	 *            the increment amount (i.e. 1 or -1)
	 */
	private FastListIterator(List<E> list, int index, int size, int increment) {
		checkNotNull(list);
		checkArgument(size >= 0, "size >= 0 required");
		checkArgument(index >= 0, "index >= 0 required");
		this.list = list;
		this.index = index;
		this.size = size;
		this.increment = increment;
	}

	@Override
	public boolean hasNext() {
		return index >= 0 && index < size;
	}

	@Override
	public E next() {
		if (hasNext()) {
			E entry = list.get(index);
			index += increment;
			return entry;
		} else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Not supported");
	}

	/**
	 * Create a new forward iterator for the given list.
	 *
	 * @param list
	 *            the list
	 * @param <E>
	 *            the element
	 * @return an iterator
	 */
	public static <E> Iterator<E> forwardIterator(List<E> list) {
		return new FastListIterator<E>(list, 0, list.size(), 1);
	}

	/**
	 * Create a new reverse iterator for the given list.
	 *
	 * @param list
	 *            the list
	 * @param <E>
	 *            the element
	 * @return an iterator
	 */
	public static <E> Iterator<E> reverseIterator(List<E> list) {
		if (!list.isEmpty()) {
			return new FastListIterator<E>(list, list.size() - 1, list.size(), -1);
		} else {
			return new FastListIterator<E>(list, 0, 0, -1);
		}
	}

}
