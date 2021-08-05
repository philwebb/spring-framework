/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Filter that can be used to quickly limit candidates based on the hashcodes of
 * attributes associated with the element. For example, a collection of objects
 * could be filtered based on their type-hierarchy to allow fast retrieval of
 * elements that are likely to implement a given interface.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class ConcurrentHashFilter<E, A> {

	private static final int HASH_TABLE_ENTRY_FREE = 0x00;

	private static final int DEFAULT_ATTRIBUTE_VALUES_CAPACITY = 32;

	private static final int DEFAULT_INITIAL_CAPACITY = 256;

	private static final float DEFAULT_LOAD_FACTOR = 0.7f;

	private static final int DEFAULT_CONCURRENCY_LEVEL = 8;

	private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;

	private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;

	private static final Object OVER_CAPACITY = new Object();

	private static final Object DELETED = new Object();

	private final Source<E> source;

	private final HashCodesExtractor<E, A> hashCodesExtractor;

	private float loadFactor;

	private final int shift;

	private final Segment[] segments;

	private final int attributeValuesCapacity;

	public ConcurrentHashFilter(Source<E> source,
			HashCodesExtractor<E, A> hashCodesExtractor) {
		this(source, hashCodesExtractor, DEFAULT_ATTRIBUTE_VALUES_CAPACITY,
				DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}

	ConcurrentHashFilter(Source<E> source, HashCodesExtractor<E, A> hashCodesExtractor,
			int attributeValueCapacity) {
		this(source, hashCodesExtractor, attributeValueCapacity, DEFAULT_INITIAL_CAPACITY,
				DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}

	/**
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references
	 * per table exceeds this value, resize will be attempted.
	 * @param concurrencyLevel the expected number of threads that will
	 * concurrently use the index.
	 */
	@SuppressWarnings("unchecked")
	ConcurrentHashFilter(Source<E> source, HashCodesExtractor<E, A> hashCodesExtractor,
			int attributeValueCapacity, int initialCapacity, float loadFactor,
			int concurrencyLevel) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(hashCodesExtractor, "HashCodesExtractor must not be null");
		Assert.isTrue(initialCapacity >= 0, "Initial capacity must not be negative");
		Assert.isTrue(loadFactor > 0f, "Load factor must be positive");
		Assert.isTrue(concurrencyLevel > 0, "Concurrency level must be positive");
		this.source = source;
		this.hashCodesExtractor = hashCodesExtractor;
		this.attributeValuesCapacity = attributeValueCapacity;
		this.loadFactor = loadFactor;
		this.shift = calculateShift(concurrencyLevel, MAXIMUM_CONCURRENCY_LEVEL);
		int size = 1 << this.shift;
		int roundedUpSegmentCapacity = (int) ((initialCapacity + size - 1L) / size);
		int initialSize = 1 << calculateShift(roundedUpSegmentCapacity,
				MAXIMUM_SEGMENT_SIZE);
		Segment[] segments = (Segment[]) Array.newInstance(Segment.class, size);
		int resizeThreshold = (int) (initialSize * this.loadFactor);
		for (int i = 0; i < segments.length; i++) {
			segments[i] = new Segment(initialSize, resizeThreshold);
		}
		this.segments = segments;
	}

	public void add(E value) {
		this.hashCodesExtractor.extract(value, (hashCode) -> {
			int hash = getHash(hashCode);
			getSegmentForHash(hash).add(hash, value);
		});
	}

	void remove(E value) {
		this.hashCodesExtractor.extract(value, (hashCode) -> {
			int hash = getHash(hashCode);
			getSegmentForHash(hash).remove(hash, value);
		});
	}

	Candidates getCandidates(A attribute) {
		return null;
	}

	private Object find(A attribute) {
		int hash = getHash(attribute.hashCode());
		return getSegmentForHash(hash).find(hash);
	}

	private Segment getSegmentForHash(int hash) {
		return this.segments[(hash >>> (32 - this.shift)) & (this.segments.length - 1)];
	}

	/**
	 * Calculate a shift value that can be used to create a power-of-two value
	 * between the specified maximum and minimum values.
	 * @param minimumValue the minimum value
	 * @param maximumValue the maximum value
	 * @return the calculated shift (use {@code 1 << shift} to obtain a value)
	 */
	private static int calculateShift(int minimumValue, int maximumValue) {
		int shift = 0;
		int value = 1;
		while (value < minimumValue && value < maximumValue) {
			value <<= 1;
			shift++;
		}
		return shift;
	}

	/**
	 * Get the hash for a given object, apply an additional Wang/Jenkins hash
	 * function to reduce collisions.
	 * @param o the object to hash (may be null)
	 * @return the resulting hash code
	 */
	private static int getHash(int hashCode) {
		int hash = hashCode;
		hash += (hash << 15) ^ 0xffffcd7d;
		hash ^= (hash >>> 10);
		hash += (hash << 3);
		hash ^= (hash >>> 6);
		hash += (hash << 2) + (hash << 14);
		hash ^= (hash >>> 16);
		return (hash != HASH_TABLE_ENTRY_FREE ? hash : hash + 1);
	}

	private class Segment extends ReentrantLock {

		private int resizeThreshold;

		private volatile Object[] table;

		private volatile int entryCount;

		Segment(int initialSize, int resizeThreshold) {
			this.resizeThreshold = resizeThreshold;
			this.table = createTable(initialSize);
		}

		@SuppressWarnings("unchecked")
		void add(int hash, E value) {
			lock();
			try {
				if (this.entryCount >= this.resizeThreshold) {
					expandTable();
				}
				int[] hashes = (int[]) this.table[0];
				int index = findIndex(hashes, hash);
				if (hashes[index] == HASH_TABLE_ENTRY_FREE) {
					hashes[index] = hash;
					this.table[index + 1] = value;
					this.entryCount++;
					return;
				}
				Object existing = this.table[index + 1];
				if (existing == OVER_CAPACITY || value.equals(existing)) {
					return;
				}
				if (existing instanceof Values) {
					Values<E> values = (Values<E>) existing;
					if (values.size() >= ConcurrentHashFilter.this.attributeValuesCapacity) {
						this.table[index + 1] = OVER_CAPACITY;
						return;
					}
					values.add(value);
					return;
				}
				this.table[index + 1] = new Values<E>(existing, value);
			}
			finally {
				unlock();
			}
		}

		@SuppressWarnings("unchecked")
		void remove(int hash, E value) {
			lock();
			try {
				int[] hashes = (int[]) this.table[0];
				int index = findIndex(hashes, hash);
				if (hashes[index] != HASH_TABLE_ENTRY_FREE) {
					Object existing = this.table[index + 1];
					if (existing == OVER_CAPACITY) {
						return;
					}
					if (existing instanceof Values) {
						Values<E> candidates = (Values<E>) existing;
						candidates.remove(value);
						if (candidates.isEmpty()) {
							hashes[index] = HASH_TABLE_ENTRY_FREE;
							this.table[index + 1] = null;
						}
					}
					hashes[index] = HASH_TABLE_ENTRY_FREE;
					this.table[index + 1] = null;
					this.entryCount--;
				}
			}
			finally {
				unlock();
			}
		}

		private void expandTable() {
			Object[] previousTable = this.table;
			int[] previousHashes = (int[]) previousTable[0];
			int size = previousHashes.length << 1;
			Assert.state(size <= MAXIMUM_SEGMENT_SIZE, "Maximum table size exceeded");
			Object[] table = createTable(size);
			int[] hashes = (int[]) table[0];
			for (int i = 0; i < previousHashes.length; i++) {
				int hash = previousHashes[i];
				int index = findIndex(hashes, hash);
				hashes[index] = hash;
				table[index + 1] = previousTable[i + 1];
			}
			this.table = table;
			this.resizeThreshold = (int) (size * ConcurrentHashFilter.this.loadFactor);
		}

		private Object[] createTable(int size) {
			Object[] table = new Object[size + 1];
			table[0] = new int[size];
			return table;
		}

		Object find(int hash) {
			Object[] table = this.table;
			int[] hashTable = (int[]) table[0];
			int index = findIndex(hashTable, hash);
			return (hashTable[index] != HASH_TABLE_ENTRY_FREE) ? table[index + 1] : null;
		}

		private int findIndex(int[] hashes, int hash) {
			int mask = hashes.length - 1;
			int offset = hash & mask;
			for (int i = 0; i < hashes.length; i++) {
				int index = (i + offset) & mask;
				int entry = hashes[index];
				if (entry == hash || entry == HASH_TABLE_ENTRY_FREE) {
					return index;
				}
			}
			throw new IndexOutOfBoundsException("Hashtable capacity reached");
		}

	}

	private static class Values<V> implements Iterable<V> {

		private static final int DEFAULT_INITIAL_CAPACITY = 16;

		private static final int RESIZE_INCREASE = 16;

		private volatile int size;

		private volatile Object[] values;

		public Values(Object initial, V additional) {
			this.values = (Object[]) Array.newInstance(Object.class,
					DEFAULT_INITIAL_CAPACITY);
			this.values[0] = initial;
			this.values[1] = additional;
		}

		/**
		 * Add a new value if it isn't already in the collection. This method is
		 * not thread-safe and must only be called when the segment is locked.
		 * @param value the value to add
		 */
		void add(Object value) {
			Object[] values = this.values;
			boolean containsDeleted = false;
			for (int i = 0; i < values.length; i++) {
				if (value.equals(values[i])) {
					return;
				}
				if (values[i] == null) {
					values[i] = value;
					this.size++;
					return;
				}
				containsDeleted = containsDeleted || values[i] == DELETED;
			}
			// We must create a new array whenever elements are moved so that we
			// don't break the iterator.
			if (containsDeleted) {
				values = (Object[]) Array.newInstance(Object.class, values.length);
				int i = 0;
				for (Object existing : this.values) {
					if (existing != DELETED) {
						values[i] = existing;
						i++;
					}
				}
				values[i] = value;
			}
			else {
				values = (Object[]) Array.newInstance(Object.class,
						values.length + RESIZE_INCREASE);
				System.arraycopy(this.values, 0, values, 0, this.values.length);
				values[this.values.length] = value;
			}
			this.values = values;
			this.size++;
		}

		/**
		 * Remove an existing value if it's in the collection. This method is
		 * not thread-safe and must only be called when the segment is locked.
		 * @param value the value to remove
		 */
		void remove(V value) {
			Object[] values = this.values;
			for (int i = 0; i < values.length; i++) {
				if (value.equals(values[i])) {
					values[i] = DELETED;
					this.size--;
					return;
				}
			}
		}

		public int size() {
			return this.size;
		}

		boolean isEmpty() {
			return this.size == 0;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void forEach(Consumer<? super V> action) {
			Object[] values = this.values;
			for (Object value : values) {
				if (value != null && value != DELETED) {
					action.accept((V) value);
				}
			}
		}

		@Override
		public Iterator<V> iterator() {
			return new ValuesIterator<V>(this.values);
		}

	}

	private static class SingleCandidateIterator<E> implements Iterator<E> {

		private E next;

		SingleCandidateIterator(E element) {
			this.next = element;
		}

		@Override
		public boolean hasNext() {
			return this.next != null;
		}

		@Override
		public E next() {
			E next = this.next;
			this.next = null;
			return next;
		}

	}

	/**
	 * Iterator for {@link Values}. This iterator requires that elements in the
	 * backing array to not change position.
	 * @param <E>
	 */
	private static class ValuesIterator<E> implements Iterator<E> {

		private final Object[] values;

		private int index;

		private Object next;

		ValuesIterator(Object[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			while (this.index < this.values.length && this.next == null) {
				this.next = this.values[this.index];
				this.next = (this.next != DELETED) ? this.next : null;
				this.index++;
			}
			return this.next != null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public E next() {
			if (this.next == null) {
				throw new NoSuchElementException();
			}
			Object next = this.next;
			this.next = null;
			return (E) next;
		}

	}

	interface Source<E> {

		Iterator<E> getAll();

		boolean isPresent(E value);

		static <K, V> Source<K> fromMap(Map<K, V> map) {
			return new MapSource<>(map);
		}

		static <E> Source<E> fromSet(Set<E> set) {
			return new CollectionSource(set);
		}

	}

	@FunctionalInterface
	public interface HashCodesExtractor<V, A> {

		void extract(V value, HashCodeConsumer<A> consumer);

		static <V, A> HashCodesExtractor<V, A> preComputed(int... hashCodes) {
			return (value, consumer) -> {
				consumer.acceptHashCodes(hashCodes);
			};
		}

	}

	@FunctionalInterface
	public interface HashCodeConsumer<T> {

		default void accept(T instance) {
			acceptHashCode(instance.hashCode());
		}

		default void accept(T instance, HashCodeFunction<T> hashFunction) {
			acceptHashCode(hashFunction.apply(instance));
		}

		default void acceptHashCodes(int[] hashCodes) {
			for (int hashCode : hashCodes) {
				acceptHashCode(hashCode);
			}
		}

		void acceptHashCode(int hashCode);

	}

	@FunctionalInterface
	public interface HashCodeFunction<T> {

		int apply(T instance);

	}

	public interface Candidates<T> extends Iterable<T> {

		Candidates<T> and(Candidates<T> other);

	}

	private static class MapSource<K, V> implements Source<K> {

		private final Map<K, V> map;

		MapSource(Map<K, V> map) {
			this.map = map;
		}

		@Override
		public Iterator<K> getAll() {
			return this.map.keySet().iterator();
		}

		@Override
		public boolean isPresent(K value) {
			return this.map.keySet().contains(value);
		}

	}

	private static class CollectionSource<E> implements Source<E> {

		private final Collection<E> collection;

		CollectionSource(Set<E> set) {
			this.collection = set;
		}

		@Override
		public Iterator<E> getAll() {
			return this.collection.iterator();
		}

		@Override
		public boolean isPresent(E value) {
			return this.collection.contains(value);
		}

	}

}
