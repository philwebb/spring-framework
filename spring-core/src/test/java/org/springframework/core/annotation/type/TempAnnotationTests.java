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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;

public class TempAnnotationTests {

	@Test
	public void test() {
		MergedAnnotations annotations = MergedAnnotations.from(Example.class);
		annotations.stream(Baz.class).filter(MergedAnnotation::isDirectlyPresent).map(
				a -> a.getBoolean("set")).forEach(System.out::println);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Foo {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Foo
	@interface Bar {

		@AliasFor("barValue")
		int value() default 0;

		@AliasFor("value")
		int barValue() default 0;

		@AliasFor(annotation = Foo.class, attribute = "value")
		String fooValue() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Bazes {

		Baz[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(Bazes.class)
	@interface Baz {

		boolean set() default false;

	}

	@Bar(barValue = 124, fooValue = "hello")
	@Baz(set = true)
	@Baz(set = false)
	static class Example {

	}

}
