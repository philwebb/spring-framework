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

/**
 * A managed collection of {@link XAotProcessor} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public interface XAotProcessors {

	/**
	 * Add the given AOT processor to this collection.
	 * @param <N> the name type used by the processor
	 * @param <T> the item type used by the processor
	 * @param aotProcessor the AOT processor to add
	 */
	<N, T> void add(XAotProcessor<N, T> aotProcessor);

	/**
	 * Remove the given AOT processor to this collection.
	 * @param <N> the name type used by the processor
	 * @param <T> the item type used by the processor
	 * @param aotProcessor the AOT processor to remove
	 */
	<N, T> void remove(XAotProcessor<N, T> aotProcessor);

	/**
	 * Return if the collection contains the given processor.
	 * @param <N> the name type used by the processor
	 * @param <T> the item type used by the processor
	 * @param aotProcessor the AOT processor to check
	 */
	<N, T> boolean contains(XAotProcessor<N, T> aotProcessor);

	/**
	 * Return a {@link Subset} of processors matching the given processor type.
	 * @param <P> the processor type
	 * @param <N> the name type used by the processor
	 * @param <T> the item type used by the processor
	 * @param processorType the processor type
	 * @return a {@link Subset} of processors the match the processor type
	 */
	<P extends XAotProcessor<N, T>, N, T> Subset<P, N, T> allOfType(Class<P> processorType);

	/**
	 * A subset of processors contained in the collection.
	 * @param <P> the processor type
	 * @param <N> the name type used by the processor
	 * @param <T> the item type used by the processor
	 */
	interface Subset<P extends XAotProcessor<N, T>, N, T> {

		/**
		 * Extend the subset with the specified processors.
		 * @param processors the processors to add
		 * @return a new subset with both processors
		 */
		Subset<P, N, T> and(Iterable<? extends P> processors);

		/**
		 * Call each processor in the subset with the given named instance and apply all
		 * resulting {@link XAotContribution contributions}.
		 * @param name the name of the item being processed.
		 * @param instance the instance to process
		 */
		void processAndApplyContributions(N name, T instance);

	}

}
