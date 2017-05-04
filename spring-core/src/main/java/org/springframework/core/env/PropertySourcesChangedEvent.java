/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.env;

import java.util.EventObject;

/**
 * Event published by {@link MutablePropertySources} whenever the underlying sources are
 * changed.
 *
 * @author Phillip Webb
 * @since 5.0
 * @see MutablePropertySources
 */
public class PropertySourcesChangedEvent extends EventObject {

	/**
	 * Create a new {@link PropertySourcesChangedEvent} instance.
	 * @param source the source {@link PropertySources}
	 */
	public PropertySourcesChangedEvent(PropertySources source) {
		super(source);
	}

	@Override
	public PropertySources getSource() {
		return (PropertySources) super.getSource();
	}

}
