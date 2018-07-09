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

package org.springframework.core.annotation4;

import java.io.IOException;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation4.LegacySanityTests.ArrayAnnotation;
import org.springframework.core.annotation4.LegacySanityTests.ImplicitAlias;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

/**
 *
 * @author pwebb
 * @since 5.0
 */
public class LegacySanityTests {

	@Test
	public void testName() {
		DoubleImplicitAlias annotation = AnnotationUtils.findAnnotation(
				WithDoubleImplicitAlias.class, DoubleImplicitAlias.class);
		System.out.println(annotation);
		ImplicitAlias annotation2 = AnnotationUtils.findAnnotation(
				WithDoubleImplicitAlias.class, ImplicitAlias.class);
		System.out.println(annotation2);
		Example annotation3 = AnnotationUtils.findAnnotation(
				WithDoubleImplicitAlias.class, Example.class);
		System.out.println(annotation3);
	}

	@Test
	public void inherited() throws IOException {
		SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = factory.getMetadataReader(WithDoubleImplicitAlias.class.getName());
		System.out.println(metadataReader.getAnnotationMetadata().getMetaAnnotationTypes(DoubleImplicitAlias.class.getName()));
		System.out.println(new StandardAnnotationMetadata(WithDoubleImplicitAlias.class).getMetaAnnotationTypes(DoubleImplicitAlias.class.getName()));
	}

	@Test
	public void findAnnotation() {
		Example findAnnotation = AnnotationUtils.findAnnotation(WithImplicitAlias.class, Example.class);
		System.out.println(findAnnotation);
		System.err.println(AnnotationUtils.getAnnotationAttributes(WithDoubleImplicitAlias.class, findAnnotation, false, false));
	}

	@Test
	public void arrayCopy() {
		 ArrayAnnotation declaredAnnotation = WithArrayAnnotation.class.getDeclaredAnnotation(ArrayAnnotation.class);
		 System.out.println(Arrays.toString(declaredAnnotation.values()));
		 System.out.println(Arrays.toString(declaredAnnotation.values()));
		 declaredAnnotation.annotationType();
	}


	@Example
	static class BaseClass {

	}

	static class SubClass extends BaseClass {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Foo {
	}


	@Inherited
	@Foo
	@Retention(RetentionPolicy.RUNTIME)
	@interface Example {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Example
	@interface ImplicitAlias {

		@AliasFor(annotation = Example.class, attribute = "value")
		String one() default "";

		@AliasFor(annotation = Example.class, attribute = "value")
		String two() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImplicitAlias
	@interface DoubleImplicitAlias {

		@AliasFor(annotation = ImplicitAlias.class, attribute = "one")
		String done() default "";

		@AliasFor(annotation = ImplicitAlias.class, attribute = "two")
		String dtwo() default "";

	}

	@ImplicitAlias(one = "foo")
	static class WithImplicitAlias {

	}

	@DoubleImplicitAlias(done = "foo")
	static class WithDoubleImplicitAlias {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ArrayAnnotation {

		int[] values() default {1,2,3};

	}

	@ArrayAnnotation
	static class WithArrayAnnotation {

	}

}
