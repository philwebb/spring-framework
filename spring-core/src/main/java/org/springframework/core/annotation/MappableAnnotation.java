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

package org.springframework.core.annotation;

import java.lang.annotation.Repeatable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;

/**
 * Source of an annotation declaration that can be handled by
 * {@link MappedAnnotations}. Encapsulates the {@link DeclaredAttributes} as
 * well as the resolved {@link AnnotationType}. The {@code .from(...)} methods
 * also deal with transparently expanding containers of
 * {@link Repeatable @Repeatable} annotations.
 *
 * @author Phillip Webb
 * @since 5.1
 */
final class MappableAnnotation {

	private static final String REPEATABLE = Repeatable.class.getName();

	private final AnnotationTypeResolver resolver;

	private final AnnotationType type;

	private final DeclaredAttributes attributes;

	MappableAnnotation(AnnotationTypeResolver resolver, DeclaredAnnotation annotation) {
		this.resolver = resolver;
		this.type = resolver.resolve(annotation.getClassName());
		this.attributes = annotation.getAttributes();
	}

	MappableAnnotation(AnnotationTypeResolver resolver, AnnotationType type,
			DeclaredAttributes attributes) {
		this.resolver = resolver;
		this.type = type;
		this.attributes = attributes;
	}

	public AnnotationTypeResolver getResolver() {
		return this.resolver;
	}

	public AnnotationType getAnnotationType() {
		return this.type;
	}

	public DeclaredAttributes getAttributes() {
		return this.attributes;
	}

	public static Stream<MappableAnnotation> from(AnnotationTypeResolver resolver,
			DeclaredAnnotations annotations) {
		Assert.notNull(resolver, "Resolver must not be null");
		Assert.notNull(annotations, "Annotations must not be null");
		return StreamSupport.stream(annotations.spliterator(), false).flatMap(
				(annotation) -> MappableAnnotation.from(resolver, annotation));
	}

	public static Stream<MappableAnnotation> from(AnnotationTypeResolver resolver,
			DeclaredAnnotation annotation) {
		Assert.notNull(resolver, "Resolver must not be null");
		Assert.notNull(annotation, "Annotation must not be null");
		AnnotationType type = resolver.resolve(annotation.getClassName());
		if (type == null) {
			return Stream.empty();
		}
		DeclaredAttributes attributes = annotation.getAttributes();
		AnnotationType repeatableType = getRepeatableType(resolver, type, attributes);
		if (repeatableType == null) {
			return Stream.of(new MappableAnnotation(resolver, type, attributes));
		}
		return Stream.of((DeclaredAttributes[]) attributes.get("value")).map(
				repeatAttributes -> new MappableAnnotation(resolver, repeatableType,
						repeatAttributes));
	}

	private static AnnotationType getRepeatableType(AnnotationTypeResolver resolver,
			AnnotationType type, DeclaredAttributes attributes) {
		Object value = attributes.get("value");
		AttributeType valueType = type.getAttributeTypes().get("value");
		if (value != null && value instanceof DeclaredAttributes[]) {
			String elementType = valueType.getClassName().replace("[]", "");
			AnnotationType repeatableType = resolver.resolve(elementType);
			if (hasAnnotation(repeatableType, REPEATABLE)) {
				return repeatableType;
			}
		}
		return null;
	}

	private static boolean hasAnnotation(AnnotationType contained, String name) {
		return contained != null && contained.getDeclaredAnnotations().find(name) != null;
	}

}
