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

import java.io.Serializable;

/**
 * Gather the need for Java serialization at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see RuntimeHints
 * @see Serializable
 */
public class SerializationHints {

	// FIXME

	public ConditionRegistration registerType(Class<?>... types) {
		return null;
	}


	public static class ConditionRegistration {

		ConditionRegistration whenReachable(Class<?> type) {
			return this;
		}

	}

}
