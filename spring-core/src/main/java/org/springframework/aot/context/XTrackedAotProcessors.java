/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link XAotProcessors} implementation that tracks calls to ensure that processors are
 * only invoked once-per-name.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class XTrackedAotProcessors implements XAotProcessors {

	private static final Log logger = LogFactory.getLog(XTrackedAotProcessors.class);

	private final List<AotProcessor<?, ?>> processors = new ArrayList<>();

	private final Consumer<XAotContribution> applyAction;

	private final Tracker tracker;

	/**
	 * Create a new {@link XTrackedAotProcessors} implementation backed by the given
	 * {@link Tracker}.
	 * @param applyAction the action call when applying AOT contributions
	 * @param tracker the tracker used to ensure once-per-name invocation (usually a
	 * {@link FileTracker})
	 */
	public XTrackedAotProcessors(Consumer<XAotContribution> applyAction, Tracker tracker) {
		Assert.notNull(applyAction, "'applyAction' must not be null");
		Assert.notNull(tracker, "'tracker' must not be null");
		this.applyAction = applyAction;
		this.tracker = tracker;
	}

	@Override
	public <N, T> void add(AotProcessor<N, T> aotProcessor) {
		Assert.notNull(aotProcessor, "'aotProcessor' must not be null");
		this.processors.add(aotProcessor);
	}

	@Override
	public <N, T> void remove(AotProcessor<N, T> aotProcessor) {
		Assert.notNull(aotProcessor, "'aotProcessor' must not be null");
		this.processors.remove(aotProcessor);
	}

	@Override
	public <N, T> boolean contains(AotProcessor<N, T> aotProcessor) {
		return this.processors.contains(aotProcessor);
	}

	@Override
	public <P extends AotProcessor<N, T>, N, T> Subset<P, N, T> allOfType(Class<P> processorType) {
		Assert.notNull(processorType, "'processorType' must not be null");
		Assert.isTrue(processorType.isInterface() && !AotProcessor.class.equals(processorType),
				"'processorType' must be a subinterface of AotProcessor");
		return new TypedSubset<>(processorType, this.processors);
	}

	/**
	 * A typed {@link Subset} that can be returned from this instance.
	 * @param <P> the processor type
	 * @param <N> the name type
	 * @param <T> the instance type
	 */
	class TypedSubset<P extends AotProcessor<N, T>, N, T> implements Subset<P, N, T> {

		private final Class<P> processorType;

		private final Iterable<AotProcessor<?, ?>> candidates;

		TypedSubset(Class<P> processorType, Iterable<AotProcessor<?, ?>> candidates) {
			this.processorType = processorType;
			this.candidates = candidates;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Subset<P, N, T> and(Iterable<? extends P> processors) {
			Assert.notNull(processors, "'processors' must not be null");
			return new TypedSubset<>(this.processorType, new CompoundIterable<AotProcessor<?, ?>>(this.candidates,
					(Iterable<AotProcessor<?, ?>>) processors));
		}

		@Override
		@SuppressWarnings("unchecked")
		public void processAndApplyContributions(N name, T instance) {
			Assert.notNull(name, "'name' must not be null");
			Assert.notNull(instance, "'instance' must not be null");
			for (AotProcessor<?, ?> candidate : this.candidates) {
				if (this.processorType.isInstance(candidate)) {
					processAndApplyContributions((P) candidate, name, instance);
				}
			}
		}

		private void processAndApplyContributions(P processor, N name, T instance) {
			Class<?> processorImplementationType = processor.getClass();
			String nameAsString = toString(name);
			if (XTrackedAotProcessors.this.tracker.shouldSkip(processorImplementationType, nameAsString)) {
				logger.trace(LogMessage.format("Skipped '%s' with AOT processor '%s'", name,
						processorImplementationType.getName()));
				return;
			}
			XAotContribution contribution = processor.processAheadOfTime(name, instance);
			XTrackedAotProcessors.this.tracker.markProcessed(processorImplementationType, nameAsString);
			logger.trace(LogMessage.format("Processed '%s' with AOT '%s'%s", name,
					processorImplementationType.getName(), (contribution != null) ? " (no contribution)" : ""));
			apply(contribution);
		}

		private void apply(@Nullable XAotContribution contribution) {
			if (contribution != null) {
				XTrackedAotProcessors.this.applyAction.accept(contribution);
			}
		}

		private String toString(N name) {
			if (name instanceof Class<?>) {
				return ((Class<?>) name).getName();
			}
			return name.toString();
		}

	}

	/**
	 * Compound {@link Iterable} build from two other {@link Iterable} instance.
	 *
	 * @param <T> the type of elements returned by the iterator
	 */
	private static class CompoundIterable<T> implements Iterable<T> {

		private final Iterable<T> first;

		private final Iterable<T> second;

		CompoundIterable(Iterable<T> first, Iterable<T> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public Iterator<T> iterator() {
			return new CompoundIterator<>(this.first.iterator(), this.second.iterator());
		}

	}

	/**
	 * Compound {@link Iterator} build from two other {@link Iterator} instance.
	 *
	 * @param <T> the type of elements returned by the iterator
	 */
	private static class CompoundIterator<T> implements Iterator<T> {

		private final Iterator<T> first;

		private final Iterator<T> second;

		public CompoundIterator(Iterator<T> first, Iterator<T> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public boolean hasNext() {
			return this.first.hasNext() || this.second.hasNext();
		}

		@Override
		public T next() {
			return (this.first.hasNext()) ? this.first.next() : this.second.next();
		}

	}

	/**
	 * Strategy used to determine when processors should be skipped.
	 * @see FileTracker
	 */
	public interface Tracker {

		/**
		 * Return if the given processor implementation should be skipped.
		 * @param processorImplementationType the processor implementation class
		 * @param name the name of the item
		 * @return {@code true} if processing should be skipped
		 */
		boolean shouldSkip(Class<?> processorImplementationType, String name);

		/**
		 * Mark that the given processor has been called with a specific name and should
		 * be skipped if the same name is presented again in the future.
		 * @param processorImplementationType the processor implementation class
		 * @param name the name of the item
		 */
		void markProcessed(Class<?> processorImplementationType, String name);

	}

	/**
	 * {@link Tracker} implementation that uses persistent files to track if a processor
	 * should be skipped. By default all
	 * {@code META-INF/spring/org.springframework.aot.context.AotProcessor/<processorType>.processed}
	 * files on the classpath are checked. The {@link #save(GeneratedFiles)} method should
	 * be used at the end of AOT processing to persist any processors marked during the
	 * session.
	 */
	public static class FileTracker implements Tracker {

		private final ClassLoader classLoader;

		private final Map<Class<?>, TrackedResource> trackedResources = new HashMap<>();

		private final Function<Class<?>, String> resourceName;

		/**
		 * Create a new {@link FileTracker} instance using the default classloader.
		 */
		public FileTracker() {
			this(null);
		}

		/**
		 * Create a new {@link FileTracker} instance using the specified classloader.
		 * @param classLoader the classloader to use or {@code null} to use the default
		 * classloader
		 */
		public FileTracker(@Nullable ClassLoader classLoader) {
			this(classLoader, FileTracker::getResourceLocation);
		}

		/**
		 * Create a new {@link FileTracker} instance using the specified classloader.
		 * @param classLoader the classloader to use or {@code null} to use the default
		 * classloader
		 * @param resourceName a function the should return the resource location to use
		 * for the given {@code processorImplementationType}
		 */
		public FileTracker(@Nullable ClassLoader classLoader, Function<Class<?>, String> resourceName) {
			this.classLoader = (classLoader != null) ? classLoader : ClassUtils.getDefaultClassLoader();
			this.resourceName = resourceName;
		}

		private static String getResourceLocation(Class<?> processorImplementationType) {
			return String.format("META-INF/spring/%s/%s.processed", AotProcessor.class.getName(),
					processorImplementationType.getName());
		}

		@Override
		public boolean shouldSkip(Class<?> processorImplementationType, String name) {
			return getTrackedResource(processorImplementationType).shouldSkip(name);
		}

		@Override
		public void markProcessed(Class<?> processorImplementationType, String name) {
			getTrackedResource(processorImplementationType).markProcessed(name);
		}

		/**
		 * Add tracking files to the specified {@link GeneratedFiles}.
		 * @param generatedFiles the generated files to add to
		 */
		public void save(GeneratedFiles generatedFiles) {
			this.trackedResources.values().forEach(processed -> processed.save(generatedFiles));
		}

		private TrackedResource getTrackedResource(Class<?> processorImplementationType) {
			return this.trackedResources.computeIfAbsent(processorImplementationType,
					key -> new TrackedResource(this.classLoader,
							this.resourceName.apply(processorImplementationType)));
		}

	}

	/**
	 * A single resource tracked by a {@link FileTracker}.
	 */
	private static class TrackedResource {

		private final ClassLoader classLoader;

		private final String resourceName;

		private final Set<String> processed = new TreeSet<>();

		private SoftReference<Set<String>> loadCache = new SoftReference<>(null);

		TrackedResource(ClassLoader classLoader, String resourceName) {
			this.classLoader = classLoader;
			this.resourceName = resourceName;
		}

		boolean shouldSkip(String name) {
			return this.processed.contains(name) || loadResources().contains(name);
		}

		void markProcessed(String name) {
			this.processed.add(name);
		}

		void save(GeneratedFiles generatedFiles) {
			if (!this.processed.isEmpty()) {
				generatedFiles.addResourceFile(this.resourceName, appendable -> {
					for (String line : this.processed) {
						appendable.append(line).append("\n");
					}
				});
			}
		}

		private Set<String> loadResources() {
			Set<String> resources = this.loadCache.get();
			if (resources != null) {
				return resources;
			}
			try {
				resources = loadResources(this.classLoader.getResources(this.resourceName));
				this.loadCache = new SoftReference<>(resources);
				return resources;
			}
			catch (IOException ex) {
				throw new IllegalArgumentException(
						"Unable to load AOT processor tracking from location [" + this.resourceName + "]", ex);
			}
		}

		private Set<String> loadResources(Enumeration<URL> urls) throws IOException {
			if (!urls.hasMoreElements()) {
				return Collections.emptySet();
			}
			Set<String> contents = new LinkedHashSet<>();
			while (urls.hasMoreElements()) {
				UrlResource url = new UrlResource(urls.nextElement());
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
					String line = reader.readLine();
					if (StringUtils.hasText(line)) {
						contents.add(line);
					}
				}
				logger.trace(LogMessage.format("Loaded %s previously tracked AOT %s from %s", contents.size(),
						(contents.size()) == 1 ? "processor" : "processors", url));
			}
			return Collections.unmodifiableSet(contents);
		}

	}

}
