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

/**
 * Simple in-memory {@link DeclaredAnnotations} implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class SimpleDeclaredAnnotations implements DeclaredAnnotations {

	private final Class<?> declaringClass;

	private final Collection<DeclaredAnnotation> declaredAnnotations;

	SimpleDeclaredAnnotations(Class<?> declaringClass, DeclaredAnnotation[] annotations) {
		this(declaringClass, Arrays.asList(annotations));
	}

	SimpleDeclaredAnnotations(Class<?> declaringClass,
			Collection<DeclaredAnnotation> declaredAnnotations) {
		this.declaringClass = declaringClass;
		this.declaredAnnotations = Collections.unmodifiableCollection(
				declaredAnnotations);
	}

	@Override
	public Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	@Override
	public Iterator<DeclaredAnnotation> iterator() {
		return this.declaredAnnotations.iterator();
	}

	@Override
	public String toString() {
		return AnnotationString.get(this);
	}

}
