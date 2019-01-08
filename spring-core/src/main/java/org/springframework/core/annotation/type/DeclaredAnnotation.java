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

/**
 * An annotation directly declared on a a source. Similar to Java's
 * {@link Annotation} type, but may be backed by something that doesn't
 * necessarily use reflection.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public interface DeclaredAnnotation {

	/**
	 * Return the class name of the annotation type.
	 * @return the class name of the annotation
	 * @see AnnotationType#resolve(String, ClassLoader)
	 */
	String getType();

	/**
	 * Return the attributes of of the declared annotation.
	 * @return the annotation attributes
	 */
	DeclaredAttributes getAttributes();

	/**
	 * Check the declared attributes of the given annotation, in particular
	 * covering Google App Engine's late arrival of
	 * {@code TypeNotPresentExceptionProxy} for {@code Class} values (instead of
	 * early {@code Class.getAnnotations() failure}.
	 * @param annotation the annotation to validate
	 * @throws IllegalStateException if a declared {@code Class} attribute could
	 * not be read
	 */
	static void validate(Annotation annotation) {
		new StandardDeclaredAttributes(annotation).forEach(DeclaredAttribute::getValue);
	}

	/**
	 * Create a new {@link DeclaredAnnotation} instance from the source Java
	 * {@link Annotation}.
	 * @param annotation the source annotation
	 * @return a new {@link DeclaredAnnotation} instance
	 */
	static DeclaredAnnotation from(Annotation annotation) {
		return new StandardDeclaredAnnotation(annotation);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotation} with the specific
	 * values.
	 * @param className the class name of the declared annotation
	 * @param attributes the annotation attributes
	 * @return a new {@link DeclaredAnnotation} instance
	 */
	static DeclaredAnnotation of(String className, DeclaredAttributes attributes) {
		return new SimpleDeclaredAnnotation(className, attributes);
	}

}
