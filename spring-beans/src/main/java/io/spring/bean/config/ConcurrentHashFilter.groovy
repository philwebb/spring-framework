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

package io.spring.bean.config;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.springframework.util.Assert;

/**
 * @author Phillip Webb
 *
 * Map<BeanRegistration,Bean> beans;
 *
 */
class ConcurrentHashFilter<V, A> {

	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;

	private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;

	private static final int USABLE_HASH_BITS = 0x7fffffff;

	private final AttributesExtractor<V, A> attributesExtractor;

	private float loadFactor;

	private int shift;

	/**
	 * Array of segments indexed using the high order bits from the hash.
	 */
	private Segment[] segments;

	/**
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references
	 * per table exceeds this value, resize will be attempted.
	 * @param concurrencyLevel the expected number of threads that will
	 * concurrently use the index.
	 */
	ConcurrentHashFilter(AttributesExtractor<V, A> attributesExtractor,
			Supplier<Iterator<V>> fallbackValuesSupplier, int initialCapacity,
			float loadFactor, int concurrencyLevel, int attributeValueCapacity) {
		Assert.isTrue(loadFactor > 0f, "Load factor must be positive");
		Assert.isTrue(concurrencyLevel > 0, "Concurrency level must be positive");
		this.attributesExtractor = attributesExtractor;
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

	void add(V value) {
		this.attributesExtractor.extractAttributes(value, (attribute) -> {
			int hash = getHash(attribute);
			Segment segment = getSegmentForHash(hash);
			segment.add(hash, value);
		});
	}

	void remove(V value) {
		this.attributesExtractor.extractAttributes(value, (attribute) -> {
			int hash = getHash(attribute);
			Segment segment = getSegmentForHash(hash);
			segment.remove(hash, value);
		});
	}

	boolean hasCandidates(A attribute) {
		return false;
	}

	Iterator<V> getCandidates(A critera) {
		return null;
	}

	void doWithCandidates(A critera, Consumer<V> consumer) {
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
	private static int getHash(Object o) {
		int hash = (o != null ? o.hashCode() : 0);
		hash += (hash << 15) ^ 0xffffcd7d;
		hash ^= (hash >>> 10);
		hash += (hash << 3);
		hash ^= (hash >>> 6);
		hash += (hash << 2) + (hash << 14);
		hash ^= (hash >>> 16);
		return hash & USABLE_HASH_BITS;
	}

	private class Segment extends ReentrantLock {

		private int resizeThreshold;

		private volatile Object[] table;

		public Segment(int initialSize, int resizeThreshold) {
			this.resizeThreshold = resizeThreshold;
		}

		void add(int hash, V value) {
			lock();
			try {
				int index = getIndex(hash, true);
				this.values[index] = value;
			}
			finally {
				unlock();
			}
		}

		void remove(int hash, V value) {
			lock();
			try {
				int index = getIndex(hash, false);
				if (index != -1) {
					this.values[index] = null;
				}
			}
			finally {
				unlock();
			}
		}

		Iterator<V> get(int hash) {
			return null;
		}

		void forEach(int hash, Consumer<? super V> action) {
		}

		private Object getValue(int hash) {
			lock();
			try {
				int index = getIndex(hash, false);
				if (index != -1) {
					return this.values[index];
				}
			}
			finally {
				unlock();
			}
		}

		private int getIndex(int hash, boolean insertIfMissing) {

		}

		private int getIndex(int hash, int length) {
			return (hash & (length - 1));
		}

	}

	private static class Values {

	}

	@FunctionalInterface
	interface AttributesExtractor<V, A> {

		void extractAttributes(V value, Consumer<?> attributes);

	}

}
