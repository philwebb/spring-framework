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

package org.springframework.aot.hint2;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Hints for runtime resource needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see ResourceLocationHint
 * @see ResourcePatternHint
 * @see ResourceBundleHint
 * @see RuntimeHints
 */
public class ResourceHints {

	private final Set<ResourcePatternHint> includeResourcePatternHints = Collections
			.newSetFromMap(new ConcurrentHashMap<>());

	private final Set<ResourcePatternHint> excludeResourcePatternHints = Collections
			.newSetFromMap(new ConcurrentHashMap<>());

	private final Set<ResourceBundleHint> resourceBundleHints = Collections.newSetFromMap(new ConcurrentHashMap<>());


	/**
	 * Registration methods for include pattern hints.
	 * @return include pattern hint registration methods
	 */
	public PatternRegistration registerInclude() {
		return new PatternRegistration(this.includeResourcePatternHints);
	}

	/**
	 * Registration methods for exclude pattern hints.
	 * @return exclude pattern hint registration methods
	 */
	public PatternRegistration registerExclude() {
		return new PatternRegistration(this.excludeResourcePatternHints);
	}

	/**
	 * Registration methods for resource bundles.
	 * @return resource bundle registration methods
	 */
	public BundleRegistration registerBundle() {
		return new BundleRegistration();
	}

	/**
	 * Return an unordered {@link Stream} of the {@link ResourcePatternHint
	 * resource pattern include hints} that have been registered.
	 * @return a stream of {@link ResourcePatternHint}
	 */
	public Stream<ResourcePatternHint> includeResourcePatterns() {
		return this.includeResourcePatternHints.stream();
	}

	/**
	 * Return an unordered {@link Stream} of the {@link ResourcePatternHint
	 * resource pattern exclude hints} that have been registered.
	 * @return a stream of {@link ResourcePatternHint}
	 */
	public Stream<ResourcePatternHint> excludeResourcePatterns() {
		return this.excludeResourcePatternHints.stream();
	}

	/**
	 * Return an unordered {@link Stream} of the {@link ResourceBundleHint
	 * resource bundle hints} that have been registered.
	 * @return a stream of {@link ResourceBundleHint}
	 */
	public Stream<ResourceBundleHint> resourceBundles() {
		return this.resourceBundleHints.stream();
	}


	/**
	 * Registration methods for patterns.
	 */
	public class PatternRegistration extends ReachableTypeRegistration<PatternRegistration> {

		private final Set<ResourcePatternHint> patternHints;

		private Predicate<String> predicate = type -> true;


		PatternRegistration(Set<ResourcePatternHint> patternHints) {
			this.patternHints = patternHints;
		}


		/**
		 * Only register patterns if the a resource at the given location is
		 * present in the {@link ClassUtils#getDefaultClassLoader() default
		 * class loader}.
		 * @param location the location to check
		 * @return this instance
		 */
		public PatternRegistration whenResourceIsPresent(String location) {
			return whenResourceIsPresent(null, location);
		}

		/**
		 * Only register patterns if the a resource at the given location is
		 * present in given class loader
		 * @param classLoader the class loader to check or {@code null} to use
		 * the {@link ClassUtils#getDefaultClassLoader() default class loader}.
		 * @param location the location to check
		 * @return this instance
		 */
		public PatternRegistration whenResourceIsPresent(@Nullable ClassLoader classLoader, String location) {
			ClassLoader classLoaderToUse = (classLoader != null) ? classLoader : ClassUtils.getDefaultClassLoader();
			return when(pattern -> classLoaderToUse.getResource(location) != null);
		}

		/**
		 * Only register when the target pattern matches the given predicate.
		 * @param predicate the predicate used to test the target type
		 * @return this instance
		 */
		public PatternRegistration when(Predicate<String> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			this.predicate = predicate.and(predicate);
			return self();
		}

		/**
		 * Complete the hint registration for the given patterns.
		 * @param patterns the patterns to register
		 */
		public void forPattern(String... patterns) {
			for (String pattern : patterns) {
				add(pattern);
			}
		}

		/**
		 * Complete the hint registration with patterns that match the
		 * {@code .class} bytecode of the given types.
		 * @param the types to register
		 */
		public void forClassBytecode(Class<?>... types) {
			forClassBytecode(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration with patterns that match the
		 * {@code .class} bytecode of the given type names.
		 * @param the type names to register
		 */
		public void forClassBytecode(String... types) {
			forClassBytecode(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration with patterns that match the
		 * {@code .class} bytecode of the given types.
		 * @param the types to register
		 */
		public void forClassBytecode(TypeReference... types) {
			for (TypeReference type : types) {
				String pattern = type.getName().replace(".", "/") + ".class";
				add(pattern);
			}
		}

		private void add(String pattern) {
			if (this.predicate.test(pattern)) {
				this.patternHints.add(new ResourcePatternHint(pattern, getReachableType()));
			}
		}

	}


	/**
	 * Registration methods for resource bundles.
	 */
	public class BundleRegistration extends ReachableTypeRegistration<PatternRegistration> {

		public void forBaseName(String... baseNames) {
			for (String baseName : baseNames) {
				ResourceHints.this.resourceBundleHints.add(new ResourceBundleHint(baseName, getReachableType()));
			}
		}

	}

}
