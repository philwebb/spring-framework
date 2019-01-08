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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationType}.
 *
 * @author Phillip Webb
 */
public class AnnotationTypeTests {

	@Test
	public void ofReturnsSimpleAnnotationType() {
		AnnotationType annotationType = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE, AttributeTypes.of(AttributeType.of("value",
						"java.lang.String", DeclaredAnnotations.NONE, null)));
		assertThat(annotationType).isInstanceOf(SimpleAnnotationType.class);
	}

	@Test
	public void resolveReturnsSimpleAnnotationType() {
		AnnotationType annotationType = AnnotationType.resolve(
				TestAnnotation.class.getName(), null);
		assertThat(annotationType).isInstanceOf(SimpleAnnotationType.class);
		assertThat(annotationType.getClassName()).isEqualTo(
				TestAnnotation.class.getName());
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface TestAnnotation {

	}

}
