/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.env;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * The default implementation of the {@link PropertySources} interface.
 * Allows manipulation of contained property sources and provides a constructor
 * for copying an existing {@code PropertySources} instance.
 *
 * <p>Where <em>precedence</em> is mentioned in methods such as {@link #addFirst}
 * and {@link #addLast}, this is with regard to the order in which property sources
 * will be searched when resolving a given property with a {@link PropertyResolver}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1
 * @see PropertySourcesPropertyResolver
 */
public class MutablePropertySources implements PropertySources {

	private volatile PropertySource<?>[] sources = new PropertySource<?>[0];

	private final ReentrantLock lock = new ReentrantLock();


	/**
	 * Create a new {@link MutablePropertySources} object.
	 */
	public MutablePropertySources() {
	}

	/**
	 * Create a new {@code MutablePropertySources} from the given propertySources
	 * object, preserving the original order of contained {@code PropertySource} objects.
	 */
	public MutablePropertySources(PropertySources propertySources) {
		if (propertySources instanceof MutablePropertySources) {
			this.sources = ((MutablePropertySources) propertySources).sources;
		}
		else {
			propertySources.forEach(this::addLast);
		}
	}


	@Override
	public Iterator<PropertySource<?>> iterator() {
		return new PropertySourcesIterator(this.sources);
	}

	@Override
	public Spliterator<PropertySource<?>> spliterator() {
		return Arrays.spliterator(this.sources);
	}

	@Override
	public Stream<PropertySource<?>> stream() {
		return Arrays.stream(this.sources);
	}

	@Override
	public boolean contains(String name) {
		return indexOf(this.sources, name) != -1;
	}

	@Override
	@Nullable
	public PropertySource<?> get(String name) {
		PropertySource<?>[] sources = this.sources;
		int index = indexOf(sources, name);
		return (index != -1) ? sources[index] : null;
	}

	/**
	 * Add the given property source object with highest precedence.
	 */
	public void addFirst(PropertySource<?> propertySource) {
		this.lock.lock();
		try {
			PropertySource<?>[] sources = removeIfPresent(this.sources, propertySource);
			this.sources = add(sources, 0, propertySource);
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Add the given property source object with lowest precedence.
	 */
	public void addLast(PropertySource<?> propertySource) {
		this.lock.lock();
		try {
			PropertySource<?>[] sources = removeIfPresent(this.sources, propertySource);
			this.sources = add(sources, sources.length, propertySource);
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Add the given property source object with precedence immediately higher
	 * than the named relative property source.
	 */
	public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		this.lock.lock();
		try {
			PropertySource<?>[] sources = removeIfPresent(this.sources, propertySource);
			int index = assertPresentAndGetIndex(sources, relativePropertySourceName);
			this.sources = add(sources, index, propertySource);
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Add the given property source object with precedence immediately lower
	 * than the named relative property source.
	 */
	public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		this.lock.lock();
		try {
			PropertySource<?>[] sources = removeIfPresent(this.sources, propertySource);
			int index = assertPresentAndGetIndex(sources, relativePropertySourceName);
			this.sources = add(sources, index + 1, propertySource);
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Return the precedence of the given property source, {@code -1} if not found.
	 */
	public int precedenceOf(PropertySource<?> propertySource) {
		return indexOf(this.sources, propertySource);
	}


	/**
	 * Remove and return the property source with the given name, {@code null} if not found.
	 * @param name the name of the property source to find and remove
	 */
	@Nullable
	public PropertySource<?> remove(String name) {
		this.lock.lock();
		try {
			PropertySource<?>[] sources = this.sources;
			int index = indexOf(sources, name);
			if (index != -1) {
				this.sources = remove(sources, index);
				return sources[index];
			}
			return null;
		}
		finally {
			this.lock.unlock();
		}
	}


	/**
	 * Replace the property source with the given name with the given property source object.
	 * @param name the name of the property source to find and replace
	 * @param propertySource the replacement property source
	 * @throws IllegalArgumentException if no property source with the given name is present
	 * @see #contains
	 */
	public void replace(String name, PropertySource<?> propertySource) {
		this.lock.lock();
		try {
			PropertySource<?>[] sources = this.sources;
			int index = assertPresentAndGetIndex(sources, name);
			this.sources[index] = propertySource;
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Return the number of {@link PropertySource} objects contained.
	 */
	public int size() {
		return this.sources.length;
	}

	@Override
	public String toString() {
		return Arrays.toString(this.sources);
	}

	/**
	 * Ensure that the given property source is not being added relative to itself.
	 */
	protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
		String newPropertySourceName = propertySource.getName();
		if (relativePropertySourceName.equals(newPropertySourceName)) {
			throw new IllegalArgumentException(
					"PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
		}
	}

	/**
	 * Remove the given property source if it is present.
	 */
	protected void removeIfPresent(PropertySource<?> propertySource) {
		this.lock.lock();
		try {
			this.sources = removeIfPresent(this.sources, propertySource);
		}
		finally {
			this.lock.unlock();
		}
	}

	private PropertySource<?>[] removeIfPresent(PropertySource<?>[] sources, PropertySource<?> source) {
		int index = indexOf(sources, source);
		return (index != -1) ? remove(sources, index) : sources;
	}

	private int assertPresentAndGetIndex(PropertySource<?>[] sources, String name) {
		int index = indexOf(sources, name);
		Assert.isTrue(index != -1, () -> "PropertySource named '" + name + "' does not exist");
		return index;
	}

	private PropertySource<?>[] add(PropertySource<?>[] sources, int index, PropertySource<?> source) {
		assertIndexInBounds(sources, index);
		PropertySource<?>[] updated = new PropertySource<?>[sources.length + 1];
		System.arraycopy(sources, 0, updated, 0, index);
		System.arraycopy(sources, index, updated, index + 1, sources.length - index);
		updated[index] = source;
		return updated;
	}

	private PropertySource<?>[] remove(PropertySource<?>[] sources, int index) {
		assertIndexInBounds(sources, index);
		PropertySource<?>[] updated = new PropertySource<?>[sources.length - 1];
		System.arraycopy(sources, 0, updated, 0, index);
		System.arraycopy(sources, index + 1, updated, index, sources.length - index - 1);
		return updated;
	}

	private void assertIndexInBounds(PropertySource<?>[] sources, int index) {
		if (index > sources.length || index < 0) {
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + sources.length);
		}
	}

	private int indexOf(PropertySource<?>[] sources, PropertySource<?> source) {
		for (int i = 0; i < sources.length; i++) {
			if (source.equals(sources[i])) {
				return i;
			}
		}
		return -1;
	}

	private int indexOf(PropertySource<?>[] sources, String name) {
		for (int i = 0; i < sources.length; i++) {
			if (sources[i].getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}


	/**
	 * Iterator that works directly from a copy of the array.
	 */
	private final static class PropertySourcesIterator implements Iterator<PropertySource<?>> {

		private final PropertySource<?>[] sources;

		private int cursor = 0;


		private PropertySourcesIterator(PropertySource<?>[] sources) {
			this.sources = sources;
		}


		@Override
		public boolean hasNext() {
			return this.cursor < this.sources.length;
		}

		@Override
		public PropertySource<?> next() {
			if (this.cursor >= this.sources.length) {
				throw new NoSuchElementException();
			}
			return this.sources[this.cursor++];
		}

	}

}
