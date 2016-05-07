package com.minecade.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * An {@link ArrayList} that takes {@link Map.Entry}-like tuples. This class
 * exists for legacy reasons.
 *
 * @param <A>
 *            the first type in the tuple
 * @param <B>
 *            the second type in the tuple
 */
public class TupleArrayList<A, B> extends ArrayList<Map.Entry<A, B>> {

	private static final long serialVersionUID = 1L;

	/**
	 * Add an item to the list.
	 *
	 * @param a
	 *            the 'key'
	 * @param b
	 *            the 'value'
	 */
	public void put(A a, B b) {
		add(new Tuple<A, B>(a, b));
	}

	/**
	 * Return an entry iterator that traverses in the reverse direction.
	 *
	 * @param reverse
	 *            true to return the reverse iterator
	 * @return an entry iterator
	 */
	public Iterator<Map.Entry<A, B>> iterator(boolean reverse) {
		return reverse ? reverseIterator() : iterator();
	}

	@Override
	public Iterator<Map.Entry<A, B>> iterator() {
		return FastListIterator.forwardIterator(this);
	}

	/**
	 * Return an entry iterator that traverses in the reverse direction.
	 *
	 * @return an entry iterator
	 */
	public Iterator<Map.Entry<A, B>> reverseIterator() {
		return FastListIterator.reverseIterator(this);
	}

	private static class Tuple<A, B> implements Map.Entry<A, B> {
		private A key;
		private B value;

		private Tuple(A key, B value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public A getKey() {
			return key;
		}

		@Override
		public B getValue() {
			return value;
		}

		@Override
		public B setValue(B value) {
			throw new UnsupportedOperationException();
		}
	}

}
