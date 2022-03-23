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

import org.springframework.lang.Nullable;

/**
 * Base interface for all AOT processor interfaces. Allows named instances to be analyzed
 * ahead-of-time and in order to optionally provide an {@link AotContribution}. This
 * interface should not be directly implemented, but instead should be used as the
 * superclass of a more specialized processor.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @param <N> the name type
 * @param <T> the instance type
 */
@FunctionalInterface
public interface AotProcessor<N, T> {

	/**
	 * Process the given named instance ahead-of-time and return an
	 * {@link AotContribution} or {@code null}. The provided name is unique within the
	 * context of the processor. It can be a {@link Class} or any object that has an
	 * appropriate {@link Object#toString() toString()} method.
	 * <p>
	 * Processors are free to use any techniques they like to analyze the given instance.
	 * Most typically use reflection to find fields or methods to use in the
	 * {@link AotContribution}. Contributions typically generate source code or resource
	 * files that can be used when the AOT optimized application runs.
	 * <p>
	 * If the given instance isn't relevant to the processor, it should return a
	 * {@code null} contribution.
	 * @param name the name of the item being processed.
	 * @param instance the instance to process
	 * @return an {@link AotContribution} or {@code null}
	 */
	@Nullable
	AotContribution processAheadOfTime(N name, T instance);

}
