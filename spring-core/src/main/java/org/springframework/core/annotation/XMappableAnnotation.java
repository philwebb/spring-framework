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
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;

/**
 * Source of an annotation declaration that can be handled by
 * {@link TypeMappedAnnotations}. Encapsulates the {@link DeclaredAttributes} as
 * well as the resolved {@link AnnotationType}. The {@code .from(...)} methods
 * also deal with transparently expanding containers of
 * {@link Repeatable @Repeatable} annotations.
 *
 * @author Phillip Webb
 * @since 5.1
 */
@Deprecated
final class XMappableAnnotation {

	// FIXME this class is very odd. I think repeatable stuff should move then
	// it becomes much lighter

	private final AnnotationTypeResolver resolver;

	private final RepeatableContainers repeatableContainers;

	private final AnnotationType type;

	private final DeclaredAttributes attributes;

	MappableAnnotation(AnnotationTypeResolver resolver,
			RepeatableContainers repeatableContainers, DeclaredAnnotation annotation) {
		this.resolver = resolver;
		this.repeatableContainers = repeatableContainers;
		this.type = resolver.resolve(annotation.getClassName()); //FIXME could be null
		this.attributes = annotation.getAttributes();
	}

	MappableAnnotation(AnnotationTypeResolver resolver,
			RepeatableContainers repeatableContainers, AnnotationType type,
			DeclaredAttributes attributes) {
		this.resolver = resolver;
		this.repeatableContainers = repeatableContainers;
		this.type = type;
		this.attributes = attributes;
	}

	public AnnotationTypeResolver getResolver() {
		return this.resolver;
	}

	public RepeatableContainers getRepeatableContainers() {
		return this.repeatableContainers;
	}

	public AnnotationType getAnnotationType() {
		return this.type;
	}

	public DeclaredAttributes getAttributes() {
		return this.attributes;
	}

	public static Stream<MappableAnnotation> from(AnnotationTypeResolver resolver,
			RepeatableContainers repeatableContainers, DeclaredAnnotations annotations) {
		Assert.notNull(resolver, "Resolver must not be null");
		Assert.notNull(annotations, "Annotations must not be null");
		return StreamSupport.stream(annotations.spliterator(), false).flatMap(
				annotation -> MappableAnnotation.from(resolver, repeatableContainers,
						annotation));
	}

	public static Stream<MappableAnnotation> from(AnnotationTypeResolver resolver,
			RepeatableContainers repeatableContainers, DeclaredAnnotation annotation) {
		Assert.notNull(resolver, "Resolver must not be null");
		Assert.notNull(annotation, "Annotation must not be null");
		AnnotationType type = resolver.resolve(annotation.getClassName());
		if (type == null) {
			return Stream.empty();
		}
		DeclaredAttributes attributes = annotation.getAttributes();
		AnnotationType containedRepeatable = repeatableContainers.findContainedRepeatableType(
				resolver, type, attributes);
		if (containedRepeatable == null) {
			return Stream.of(new MappableAnnotation(resolver, repeatableContainers, type,
					attributes));
		}
		return Stream.of((DeclaredAttributes[]) attributes.get("value")).map(
				repeatAttributes -> new MappableAnnotation(resolver, repeatableContainers,
						containedRepeatable, repeatAttributes));
	}

}
