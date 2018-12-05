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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

/**
 * Tests for unusual things in AnnotationUtils.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class AnnotationQuirk2Tests {

	@Test
	public void alias() {
		System.out.println(InternalAnnotatedElementUtils.getAllAnnotationAttributes(
				WithMeta.class, Root.class.getName()));
//		System.out.println(InternalAnnotatedElementUtils.getMergedAnnotationAttributes(
//				WithMeta.class, Root.class));

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface Root {

		String rootAttribute() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Root
	static @interface Meta {

		@AliasFor(annotation = Root.class, attribute = "rootAttribute")
		String metaAttribute() default "";

		@AliasFor(annotation = Root.class, attribute = "rootAttribute")
		String metaAttribute2() default "";

	}

	@Meta(metaAttribute = "metaValue")
	static class WithMeta {

	}

}
