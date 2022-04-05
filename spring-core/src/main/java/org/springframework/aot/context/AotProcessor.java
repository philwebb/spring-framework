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
 * ahead-of-time and in order to optionally provide an {@link XAotContribution}. This
 * interface should not be directly implemented, but instead should be used as the
 * superclass of a more specialized processor.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @param <T> the type being processed
 * @param <C> the contribution type
 */
@FunctionalInterface
public interface AotProcessor<T, C> {

	/**
	 * Process the given named instance ahead-of-time and return an contribution or
	 * {@code null}.
	 * <p>
	 * Processors are free to use any techniques they like to analyze the given instance.
	 * Most typically use reflection to find fields or methods to use in the contribution.
	 * Contributions typically generate source code or resource files that can be used
	 * when the AOT optimized application runs.
	 * <p>
	 * If the given instance isn't relevant to the processor, it should return a
	 * {@code null} contribution.
	 * @param instance the instance to process
	 * @return an {@link XAotContribution} or {@code null}
	 */
	@Nullable
	C processAheadOfTime(T instance);

}
