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

package org.springframework.beans.factory.aot;

import java.util.Objects;

import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import org.springframework.aot.context.XTrackedAotProcessors;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * A unique name associated with a {@link BeanFactory}. Used to disambiguate when an
 * {@link XAotBeanFactoryProcessor} has already been run.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see XAotBeanFactoryProcessor
 * @see XTrackedAotProcessors
 */
public final class XUniqueBeanFactoryName {

	@Nullable
	private final UniqueBeanFactoryName parent;

	private final String value;

	public UniqueBeanFactoryName(String value) {
		this(null, value);
	}

	public UniqueBeanFactoryName(@Nullable UniqueBeanFactoryName parent, String value) {
		Assert.hasText(value, "'value' must not be empty");
		this.parent = parent;
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parent, this.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		UniqueBeanFactoryName other = (UniqueBeanFactoryName) obj;
		return Objects.equals(this.parent, other.parent) && Objects.equals(this.value, other.value);
	}

	@Override
	public String toString() {
		return (this.parent != null) ? this.parent + ":" + this.value : this.value;
	}

}
