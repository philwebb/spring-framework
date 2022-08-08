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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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

	private final Map<String, ResourceLocationHint> locationHints = new ConcurrentHashMap<>();

	private final Map<ResourcePattern, ResourcePatternHint> patternHints = new ConcurrentHashMap<>();

	private final Set<TypeReference> classBytecodeHints = Collections
			.newSetFromMap(new ConcurrentHashMap<>());

	private final Map<String, ResourceBundleHint> bundleHints = new ConcurrentHashMap<>();

	LocationCondition updateLocation(String[] locations,
			UnaryOperator<ResourceLocationHint> mapper) {
		for (String location : locations) {
			this.locationHints.compute(location, (key, hint) -> mapper
					.apply((hint != null) ? hint : new ResourceLocationHint(location)));
		}
		Consumer<TypeReference> whenReachableAction = reachableType -> updateLocation(
				locations, hint -> hint.andReachableType(reachableType));
		Runnable whenPresentAction = () -> updateLocation(locations,
				hint -> hint.andOnlyWhenPresent());
		return new LocationCondition(whenReachableAction, whenPresentAction);
	}

	Condition updatePattern(ResourcePattern[] patterns,
			UnaryOperator<ResourcePatternHint> mapper) {
		for (ResourcePattern pattern : patterns) {
			this.patternHints.compute(pattern, (key, hint) -> mapper
					.apply((hint != null) ? hint : new ResourcePatternHint(pattern)));
		}
		return new Condition(reachableType -> updatePattern(patterns,
				hint -> hint.andReachableType(reachableType)));
	}

	Condition updateBundle(String[] names, UnaryOperator<ResourceBundleHint> mapper) {
		for (String name : names) {
			this.bundleHints.compute(name, (key, hint) -> mapper
					.apply((hint != null) ? hint : new ResourceBundleHint(name)));
		}
		return new Condition(reachableType -> updateBundle(names,
				hint -> hint.andReachableType(reachableType)));
	}

	public ResourceRegistration registerResource() {
		return new ResourceRegistration();
	}

	public BundleRegistration registerBundle() {
		return new BundleRegistration();
	}

	public class ResourceRegistration extends ReachableTypeRegistration<ResourceRegistration> {


		public ResourceRegistration whenResourceIsPresent() {

		}

		public ResourceRegistration whenResourceIsPresent(ClassLoader classLoader) {

		}

		public LocationCondition forLocation(String... locations) {
			return updateLocation(locations, UnaryOperator.identity());
		}

		public void forPattern(String includeRegex, String excludeRegex) {
			return forPattern(ResourcePattern.of(includeRegex, excludeRegex));
		}

		public void forPattern(ResourcePattern... patterns) {
			updatePattern(patterns, UnaryOperator.identity());
		}

		public void forClassBytecode(Class<?>... types) {
			forClassBytecode(TypeReference.arrayOf(types));
		}

		public void forClassBytecode(String... types) {
			forClassBytecode(TypeReference.arrayOf(types));
		}

		public void forClassBytecode(TypeReference... types) {
			classBytecodeHints.addAll(Arrays.asList(types));
		}

	}

	public class BundleRegistration extends ReachableTypeRegistration<ResourceRegistration> {

		public void forName(String... names) {
			updateBundle(names, UnaryOperator.identity());
		}

	}


	public static class LocationCondition
			extends ReachableTypeRegistration<LocationCondition> {

		private final Runnable whenPresentAction;

		LocationCondition(Consumer<TypeReference> whenReachableAction,
				Runnable whenPresentAction) {
			super(whenReachableAction);
			this.whenPresentAction = whenPresentAction;
		}

		public LocationCondition whenPresent() {
			whenPresentAction.run();
			return this;
		}

	}

}
