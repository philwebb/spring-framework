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

import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * Tests for unusual things in AnnotationUtils.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class AnnotationQuirkTests {

	@Test
	public void multipleChildren() {
		MultiValueMap<String,Object> map = AnnotatedElementUtils.getAllAnnotationAttributes(MultiChild.class,
				Root.class.getName());


		System.out.println(map);
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface Root {

		String name() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Root
	static @interface RootAlias {

		@AliasFor(annotation = Root.class)
		String name() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@RootAlias
	static @interface Child1 {

		@AliasFor(annotation = RootAlias.class)
		String name() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@RootAlias
	static @interface Child2 {

		@AliasFor(annotation = RootAlias.class)
		String name() default "";

	}

	@Child1(name = "child1")
	@Child2(name = "child2")
	static class MultiChild {

	}

}
