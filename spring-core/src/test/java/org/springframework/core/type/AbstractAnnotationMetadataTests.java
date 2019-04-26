/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.*;

/**
 * Base class for {@link AnnotationMetadata} tests.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public abstract class AbstractAnnotationMetadataTests {

	protected abstract AnnotationMetadata getAnnotationMetadata(Class<?> source);

	@Test
	@Ignore
	public void getAnnotationsReturnsDirectAnnotations() {
		// FIXME enable when ASM work is done
		AnnotationMetadata metadata = getAnnotationMetadata(WithDirectAnnotations.class);
		assertThat(metadata.getAnnotations().stream().filter(
				MergedAnnotation::isDirectlyPresent).map(
						a -> a.getType().getName())).containsExactlyInAnyOrder(
								DirectAnnotation1.class.getName(),
								DirectAnnotation2.class.getName());
	}

	@Test
	public void getAnnotationTypesReturnsDirectAnnotations() {
		AnnotationMetadata metadata = getAnnotationMetadata(WithDirectAnnotations.class);
		assertThat(metadata.getAnnotationTypes()).containsExactlyInAnyOrder(
				DirectAnnotation1.class.getName(), DirectAnnotation2.class.getName());
	}

	@Test
	public void getMetaAnnotationTypesReturnsMetaAnnotations() {
		AnnotationMetadata metadata = getAnnotationMetadata(WithMetaAnnotations.class);
		assertThat(metadata.getMetaAnnotationTypes(
				MetaAnnotationRoot.class.getName())).containsExactlyInAnyOrder(
						MetaAnnotation1.class.getName(), MetaAnnotation2.class.getName());
	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface DirectAnnotation1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface DirectAnnotation2 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MetaAnnotation1
	public static @interface MetaAnnotationRoot {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MetaAnnotation2
	public static @interface MetaAnnotation1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface MetaAnnotation2 {

	}

	@DirectAnnotation1
	@DirectAnnotation2
	public static class WithDirectAnnotations {

	}

	@MetaAnnotationRoot
	public static class WithMetaAnnotations {

	}

}
