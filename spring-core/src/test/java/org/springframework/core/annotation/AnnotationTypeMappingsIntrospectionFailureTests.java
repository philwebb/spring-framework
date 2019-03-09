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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.core.OverridingClassLoader;

/**
 * Tests for {@link AnnotationTypeMappings} when introspection fails.
 *
 * @author Phillip Webb
 */
@Ignore
public class AnnotationTypeMappingsIntrospectionFailureTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testName() throws Exception {
		TestClassLoader classLoader = new TestClassLoader(
				this.getClass().getClassLoader());
		Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) classLoader.loadClass(
				Example.class.getName());
		System.out.println(Arrays.asList(annotationClass.getAnnotations()));
		Annotation annotation = annotationClass.getAnnotations()[1];
		System.out.println(annotation.getClass());
		System.out.println(annotation.getClass().getClassLoader());
		Method method = annotation.getClass().getMethod("value");
		method.setAccessible(true);
		Object invoke = method.invoke(annotation);
		System.out.println(invoke);
		System.out.println(invoke.getClass());
		System.out.println(invoke.getClass().getClassLoader());
	}

	private static class TestClassLoader extends OverridingClassLoader {

		public TestClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			if (ExampleEnum.class.getName().equals(name)) {
				throw new ClassNotFoundException(name);
			}
			System.out.println(name);
			return super.loadClass(name, resolve);
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MissingAnnotation(ExampleEnum.class)
	@interface Example {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface MissingAnnotation {

		Class<?> value() default void.class;

	}

	enum ExampleEnum {
		FOO
	}

}
