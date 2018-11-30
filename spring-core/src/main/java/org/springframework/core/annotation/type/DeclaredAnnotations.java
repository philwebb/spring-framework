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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * A collection of {@link DeclaredAnnotation} instances.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 5.2
 */
@FunctionalInterface
public interface DeclaredAnnotations extends Iterable<DeclaredAnnotation> {

	/**
	 * {@link DeclaredAnnotation} instances that can be used when there are no
	 * declared annotations.
	 */
	static final DeclaredAnnotations NONE = () -> Collections.emptyIterator();

	/**
	 * Return the class that declared the annotations or {@code null} if the
	 * declaring class is not known.
	 * @return the declaring class or {@code null}
	 */
	default Class<?> getDeclaringClass() {
		// FIXME this is not ideal. Best to drop it here and instead create
		// a sub-interface that exposes AnnotatedElement
		return null;
	}

	/**
	 * Find a declared annotation of the specified type.
	 * @param annotationType the type required
	 * @return a declared annotation or {@code null}
	 */
	@Nullable
	default DeclaredAnnotation find(String annotationType) {
		return StreamSupport.stream(spliterator(), false).filter(
				annotation -> annotation.getClassName().equals(
						annotationType)).findFirst().orElse(null);
	}

	// FIXME make this take an enum to stay what to do on error (LOG, IGNORE, FAIL)

	// FIXME DC
	static DeclaredAnnotations from(AnnotatedElement element, Annotation... annotations) {
		return from(element, Arrays.asList(annotations));
	}

	// FIXME DC
	static DeclaredAnnotations from(AnnotatedElement element, Collection<Annotation> annotations) {
		Class<?> declaringClass = null;
		if (element instanceof Class) {
			declaringClass = (Class<?>) element;
		}
		if (element instanceof Method) {
			declaringClass = ((Method) element).getDeclaringClass();
		}
		List<DeclaredAnnotation> adapted = new ArrayList<>();
		for (Annotation annotation : annotations) {
			try {
				adapted.add(DeclaredAnnotation.from(annotation));
			}
			catch (Throwable ex) {
				AnnotationIntrospectionFailure.log(element, ex);
			}
		}
		return of(declaringClass, adapted);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotations} containing the
	 * specified annotations.
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(DeclaredAnnotation... annotations) {
		return of(null, annotations);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotations} containing the
	 * specified annotations.
	 * @param declaringClass the class that declared the annotations
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(Class<?> declaringClass, DeclaredAnnotation... annotations) {
		return new SimpleDeclaredAnnotations(declaringClass, annotations);
	}


	/**
	 * Create a new in-memory {@link DeclaredAnnotations} containing the
	 * specified annotations.
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(Collection<DeclaredAnnotation> annotations) {
		return of(null, annotations);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotations} containing the
	 * specified annotations.
	 * @param declaringClass the class that declared the annotations
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(Class<?> declaringClass, Collection<DeclaredAnnotation> annotations) {
		return new SimpleDeclaredAnnotations(declaringClass, annotations);
	}

}
