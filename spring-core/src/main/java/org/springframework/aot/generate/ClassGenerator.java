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

package org.springframework.aot.generate;

/**
 * Generates new {@link GeneratedClass} instances.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see GeneratedMethods
 */
public interface ClassGenerator {

	/**
	 * Get or generate a new {@link GeneratedClass} for a given target and feature name.
	 * @param target the target of the newly generated class
	 * @param featureName the name of the feature that the generated class supports
	 * @return a {@link GeneratedClass} instance
	 */
	GeneratedClass getOrGenerateClass(Class<?> target, String featureName);

	/**
	 * Get or generate a new {@link GeneratedClass} for a given target and feature name.
	 * @param target the target of the newly generated class
	 * @param featureName the name of the feature that the generated class supports
	 * @return a {@link GeneratedClass} instance
	 */
	GeneratedClass getOrGenerateClass(String target, String featureName);

}
