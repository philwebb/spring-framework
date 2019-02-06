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

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

/**
 *
 * @author pwebb
 * @since 5.1
 */
public class QuickTests {

	@Test
	public void testName() {
		System.out.println(
				InternalAnnotatedElementUtils.findMergedAnnotation(WithA.class, A.class));

		//		System.out.println(
//				InternalAnnotatedElementUtils.findMergedAnnotation(WithA.class, A.class));
//		System.out.println(
//				InternalAnnotatedElementUtils.findMergedAnnotation(WithA.class, B.class));
//		System.out.println(
//				InternalAnnotatedElementUtils.findMergedAnnotation(WithA.class, C.class));
//		System.out.println(
//				InternalAnnotatedElementUtils.findMergedAnnotation(WithA.class, D.class));
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface A {

		@AliasFor("bar")
		String foo() default "";

		@AliasFor("baz")
		String bar() default "";

		@AliasFor("bar")
		String baz() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@D
	static @interface B {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface D {

		String foo() default "";

	}

	@A(foo="foo")
	private static class WithA {
	}

}
