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
		MergedAnnotation<?> annotation = MergedAnnotations.from(WithGetMapping.class).get(GetMapping.class);
		System.out.println(annotation.getString("path"));
		System.out.println(annotation.getString("value"));
	}

	@Retention(RetentionPolicy.RUNTIME)
	@RequestMapping(method = "get")
	static @interface GetMapping {

		@AliasFor(annotation = RequestMapping.class)
		String path() default "";

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface RequestMapping {

		String method();

		String path() default "";

	}

	@GetMapping("foo")
	static class WithGetMapping {

	}

}
