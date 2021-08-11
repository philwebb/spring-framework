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

/**
 * A qualifier that can be associated with a {@link FunctionBeanDefinition} in
 * order to disambiguate it from other candidates. Qualifier implementations
 * must have valid {@code hashCode}, {@code equals} and {@code toString}
 * methods.
 * <p>
 * Simple string based qualifiers can be created using the {@link #of(String)}
 * factory method.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public interface Qualifier {

	static Qualifier of(String value) {
		return new SimpleQualifier(value);
	}

}
