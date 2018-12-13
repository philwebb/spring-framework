/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.annotation.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple in-memory {@link DeclaredAnnotations} implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class SimpleDeclaredAnnotations implements DeclaredAnnotations {

	private final Object source;

	private final Collection<DeclaredAnnotation> annotations;

	SimpleDeclaredAnnotations(@Nullable Object source, DeclaredAnnotation[] annotations) {
		Assert.notNull(annotations, "Annotations must not be null");
		this.source = source;
		this.annotations = Collections.unmodifiableCollection(Arrays.asList(annotations));
	}

	SimpleDeclaredAnnotations(@Nullable Object source,
			Collection<DeclaredAnnotation> annotations) {
		Assert.notNull(annotations, "Annotations must not be null");
		this.source = source;
		this.annotations = Collections.unmodifiableCollection(annotations);
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	@Override
	public int size() {
		return this.annotations.size();
	}

	@Override
	public Iterator<DeclaredAnnotation> iterator() {
		return this.annotations.iterator();
	}

	@Override
	public String toString() {
		return AnnotationString.get(this);
	}

}
