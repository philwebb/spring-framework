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

package org.springframework.beans.factory.function;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Memory efficient filter used to quickly limit candidates based on the hash
 * codes of attributes extracted from the element. For example, a collection of
 * objects could be filtered based on their type-hierarchy to allow fast
 * retrieval of elements that are likely to match an {@code instanceof} check.
 *
 * @author Phillip Webb
 */
class ConcurrentHashFilter<E, A> {

	private static final int DEFAULT_ATTRIBUTE_VALUES_CAPACITY = 32;

	private static final int DEFAULT_COLLISION_THRESHOLD = 128;

	private static final float DEFAULT_LOAD_FACTOR = 0.7f;

	private static final int DEFAULT_CONCURRENCY_LEVEL = 8;

	private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;

	private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;

	private static final int HASH_TABLE_ENTRY_FREE = 0x00;

	private static final Object COLLISION_THRESHOLD_EXCEEDED = new Object();

	private static final Object DELETED = new Object();

	private static final Candidates<?> EMPTY_CANDIDATES = new Candidates<Object>() {

		@Override
		public Iterator<Object> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

	};

	private final HashCodesExtractor<E, A> hashCodesExtractor;

	private final float loadFactor;

	private final int shift;

	private final Segment[] segments;

	private final int collisionThreshold;

	/**
	 * Create a new {@link ConcurrentHashFilter} instance.
	 * @param hashCodesExtractor the hash codes extractor to apply
	 */
	ConcurrentHashFilter(HashCodesExtractor<E, A> hashCodesExtractor) {
		this(hashCodesExtractor, DEFAULT_ATTRIBUTE_VALUES_CAPACITY,
				DEFAULT_COLLISION_THRESHOLD, DEFAULT_LOAD_FACTOR,
				DEFAULT_CONCURRENCY_LEVEL);
	}

	/**
	 * @param hashCodesExtractor the hash codes extractor to apply
	 * @param collisionThreshold the maximum number of elements that can be
	 * stored for a given attribute hash code. Once exceeded,
	 * {@link #findCandidates(Object)} will not return a result and a full scan
	 * will be required.
	 */
	ConcurrentHashFilter(HashCodesExtractor<E, A> hashCodesExtractor,
			int collisionThreshold) {
		this(hashCodesExtractor, collisionThreshold, DEFAULT_COLLISION_THRESHOLD,
				DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
	}

	/**
	 * @param hashCodesExtractor the hash codes extractor to apply
	 * @param collisionThreshold the maximum number of elements that can be
	 * stored for a given attribute hash code. Once exceeded,
	 * {@link #findCandidates(Object)} will not return a result and a full scan
	 * will be required.
	 * @param initialCapacity the initial capacity of the filter
	 * @param loadFactor the load factor. When the average number of references
	 * per table exceeds this value, resize will be attempted
	 * @param concurrencyLevel the expected number of threads that will
	 * concurrently use the filter
	 */
	@SuppressWarnings("unchecked")
	ConcurrentHashFilter(HashCodesExtractor<E, A> hashCodesExtractor,
			int collisionThreshold, int initialCapacity, float loadFactor,
			int concurrencyLevel) {
		Assert.notNull(hashCodesExtractor, "HashCodesExtractor must not be null");
		Assert.isTrue(initialCapacity >= 0, "Initial capacity must not be negative");
		Assert.isTrue(loadFactor > 0f, "Load factor must be positive");
		Assert.isTrue(concurrencyLevel > 0, "Concurrency level must be positive");
		this.hashCodesExtractor = hashCodesExtractor;
		this.collisionThreshold = collisionThreshold;
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

	/**
	 * Add the given element to the filter, applying the
	 * {@link HashCodesExtractor} to create any attribute associations.
	 * @param element the element to add
	 */
	void add(E element) {
		this.hashCodesExtractor.extract(element, (hashCode) -> {
			int hash = getHash(hashCode);
			getSegmentForHash(hash).add(hash, element);
		});
	}

	/**
	 * Add the given element to the filter, applying the
	 * {@link HashCodesExtractor} to remove any attribute associations.
	 * @param element the element to remove
	 */
	void remove(E element) {
		this.hashCodesExtractor.extract(element, (hashCode) -> {
			int hash = getHash(hashCode);
			getSegmentForHash(hash).remove(hash, element);
		});
	}

	/**
	 * Find candidate elements that are likely to have the specified attribute.
	 * @param attribute the attribute that the candidates must have
	 * @return the candidates or {@code null} if a full scan is required
	 */
	@Nullable
	Candidates<E> findCandidates(A attribute) {
		return findCandidatesForHashCode(attribute.hashCode());
	}

	/**
	 * Find candidate elements that are likely to have the specified attribute
	 * based on its hash code.
	 * @param attributeHashCode the hashcode of the attribute that the
	 * candidates must have
	 * @return the candidates or {@code null} if a full scan is required
	 */
	@Nullable
	Candidates<E> findCandidatesForHashCode(int attributeHashCode) {
		int hash = getHash(attributeHashCode);
		return getSegmentForHash(hash).findCandidates(hash);
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

	/**
	 * A segment used to divide the hash table to improve multi-threaded
	 * performance.
	 */
	private class Segment extends ReentrantLock {

		private int resizeThreshold;

		/**
		 * The entry table that contains both the linear probing hash table and
		 * the values. The first entry in the array always an {@code int[]} hash
		 * table. The remaining entries are the values which may be a single
		 * associated value, a {@link Elements} container or
		 * {@link #COLLISION_THRESHOLD_EXCEEDED}.
		 */
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
				if (existing == COLLISION_THRESHOLD_EXCEEDED || value.equals(existing)) {
					return;
				}
				if (existing instanceof Elements) {
					Elements<E> elements = (Elements<E>) existing;
					if (elements.size() >= ConcurrentHashFilter.this.collisionThreshold) {
						this.table[index + 1] = COLLISION_THRESHOLD_EXCEEDED;
						return;
					}
					elements.add(value);
					return;
				}
				this.table[index + 1] = new Elements<E>(existing, value);
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
				if (hashes[index] == HASH_TABLE_ENTRY_FREE) {
					return;
				}
				Object existing = this.table[index + 1];
				if (existing == COLLISION_THRESHOLD_EXCEEDED) {
					return;
				}
				if (existing instanceof Elements) {
					Elements<E> elements = (Elements<E>) existing;
					elements.remove(value);
					if (!elements.isEmpty()) {
						return;
					}
				}
				hashes[index] = HASH_TABLE_ENTRY_FREE;
				this.table[index + 1] = null;
				this.entryCount--;
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

		@SuppressWarnings("unchecked")
		Candidates<E> findCandidates(int hash) {
			Object[] table = this.table;
			int[] hashTable = (int[]) table[0];
			int index = findIndex(hashTable, hash);
			if (index == HASH_TABLE_ENTRY_FREE) {
				return (Candidates<E>) EMPTY_CANDIDATES;
			}
			Object existing = table[index + 1];
			if (existing == COLLISION_THRESHOLD_EXCEEDED) {
				return null;
			}
			if (existing instanceof Elements) {
				return (Elements<E>) existing;
			}
			return new SingleElement<>((E) existing);
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

	/**
	 * Container used when more than one element is associated with an attribute
	 * hash.
	 * @param <E> the element type
	 */
	private static class Elements<E> implements Candidates<E> {

		private static final int DEFAULT_INITIAL_CAPACITY = 16;

		private static final int RESIZE_INCREASE = 16;

		private volatile int size;

		private volatile Object[] values;

		public Elements(Object initial, E additional) {
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
		void remove(E value) {
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

		public boolean isEmpty() {
			return this.size == 0;
		}

		@Override
		public Iterator<E> iterator() {
			return new ElementsIterator<E>(this.values);
		}

	}

	/**
	 * Iterator for {@link Elements}. This iterator requires that elements in
	 * the backing array to not change position.
	 * @param <E> the element type
	 */
	private static class ElementsIterator<E> implements Iterator<E> {

		private final Object[] elements;

		private int index;

		private Object next;

		ElementsIterator(Object[] elements) {
			this.elements = elements;
		}

		@Override
		public boolean hasNext() {
			while (this.index < this.elements.length && this.next == null) {
				this.next = this.elements[this.index];
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

	/**
	 * Container used to return a {@link Candidates} result for a single
	 * element.
	 * @param <E> the element type
	 */
	private static class SingleElement<E> implements Candidates<E> {

		private final E element;

		SingleElement(E element) {
			this.element = element;
		}

		@Override
		public Iterator<E> iterator() {
			return new SingleElementIterator<>(this.element);
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

	}

	private static class SingleElementIterator<E> implements Iterator<E> {

		private E next;

		SingleElementIterator(E element) {
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
	 * Strategy interface used to extract attribute hash codes for an element.
	 * @param <E> the element type
	 * @param <A> the attribute type
	 */
	@FunctionalInterface
	public interface HashCodesExtractor<E, A> {

		/**
		 * Extract attribute hash codes for the given element.
		 * @param element the element to extract attribute hash codes from
		 * @param consumer a hash code consumer called once for each hash code
		 */
		void extract(E element, HashCodeConsumer<A> consumer);

		/**
		 * Factory method that can be used to create a
		 * {@link HashCodesExtractor} for a set of pre-computed hash codes.
		 * @param <E> the element type
		 * @param <A> the attribute type
		 * @param hashCodes the pre-computed hash codes
		 * @return a {@link HashCodesExtractor} instance that provides the
		 * pre-computed hash codes
		 */
		static <E, A> HashCodesExtractor<E, A> preComputed(int... hashCodes) {
			return (value, consumer) -> {
				consumer.acceptHashCodes(hashCodes);
			};
		}

	}

	/**
	 * Consumer used to receive hash codes.
	 * @param <A> the attribute type
	 */
	@FunctionalInterface
	public interface HashCodeConsumer<A> {

		/**
		 * Accept the hash code of the given attribute.
		 * @param attribute the attribute to accept
		 */
		default void accept(A attribute) {
			acceptHashCode(attribute.hashCode());
		}

		/**
		 * Accept the hash code of the given attribute generated by a given
		 * function.
		 * @param attribute the attribute to accept
		 * @param hashFunction the function used to generate the hash code
		 */
		default void accept(A attributes, HashCodeFunction<A> hashFunction) {
			acceptHashCode(hashFunction.apply(attributes));
		}

		/**
		 * Accept an array of hash codes (one for each attribute)
		 * @param hashCodes the hash codes to accept
		 */
		default void acceptHashCodes(int[] hashCodes) {
			for (int hashCode : hashCodes) {
				acceptHashCode(hashCode);
			}
		}

		/**
		 * Accept a single hash code.
		 * @param hashCode the hash code to accept
		 */
		void acceptHashCode(int hashCode);

	}

	/**
	 * Strategy that can be used to provide a custom hashing function.
	 * @param <A> the attribute type
	 */
	@FunctionalInterface
	public interface HashCodeFunction<A> {

		/**
		 * Return the hash code of the given attribute
		 * @param attribute the attribute to hash
		 * @return the hash code of the attribute
		 */
		int apply(A attribute);

	}

	/**
	 * An {@link Iterable} collection of candidates that are likely to have a
	 * specific attribute.
	 * @param <E> the element type
	 */
	public interface Candidates<E> extends Iterable<E> {

		/**
		 * Return {@code true} if there are no candidate.
		 * @return if there are no candidate
		 */
		boolean isEmpty();

	}

}
