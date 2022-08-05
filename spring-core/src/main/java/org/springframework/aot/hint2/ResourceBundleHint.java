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

import java.util.ResourceBundle;

/**
 * An immutable hint that describes the need to access a {@link ResourceBundle}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 6.0
 * @see ResourceHints
 */
public final class ResourceBundleHint {

	/**
	 * @param name
	 */
	ResourceBundleHint(String name) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param reachableType
	 * @return
	 */
	ResourceBundleHint andReachableType(TypeReference reachableType) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
