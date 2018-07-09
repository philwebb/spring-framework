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

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.ClassUtils;

/**
 * Tests for {@link MappedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public class MappedAnnotationTest extends AbstractMergedAnnotationTests {

	private AnnotationTypeResolver resolver = AnnotationTypeResolver.get(
			ClassUtils.getDefaultClassLoader());

	@Override
	protected MergedAnnotation<?> create(AnnotationType type,
			DeclaredAttributes attributes) {
		MappableAnnotation source = new MappableAnnotation(this.resolver, type,
				attributes);
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(this.resolver, null,
				source);
		return new MappedAnnotation<>(mapping, attributes, false, null);
	}

}
