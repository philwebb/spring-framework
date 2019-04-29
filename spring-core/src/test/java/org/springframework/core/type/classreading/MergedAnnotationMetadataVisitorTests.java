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

package org.springframework.core.type.classreading;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link MergedAnnotationMetadataVisitor}.
 *
 * @author Phillip Webb
 */
public class MergedAnnotationMetadataVisitorTests {

	protected MergedAnnotation<?> annotation;

	@Test
	public void visitWhenHasSimpleTypesCreatesAnnotation() {
		loadFrom(WithSimpleTypesAnnotation.class);
		assertThat(annotation.getType()).isEqualTo(SimpleTypesAnnotation.class);
		assertThat(annotation.getValue("stringValue")).contains("string");
		assertThat(annotation.getValue("byteValue")).contains((byte) 1);
		assertThat(annotation.getValue("shortValue")).contains((short) 2);
		assertThat(annotation.getValue("intValue")).contains(3);
		assertThat(annotation.getValue("longValue")).contains(4L);
		assertThat(annotation.getValue("booleanValue")).contains(true);
		assertThat(annotation.getValue("charValue")).contains('c');
		assertThat(annotation.getValue("doubleValue")).contains(5.0);
		assertThat(annotation.getValue("floatValue")).contains(6.0f);
	}

	private void loadFrom(Class<?> type) {
		ClassVisitor visitor = new ClassVisitor(SpringAsmInfo.ASM_VERSION) {

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				return MergedAnnotationMetadataVisitor.get(getClass().getClassLoader(),
						this, descriptor, visible,
						annotation -> MergedAnnotationMetadataVisitorTests.this.annotation = annotation);
			}

		};
		try {
			new ClassReader(type.getName()).accept(visitor, ClassReader.SKIP_DEBUG
					| ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface SimpleTypesAnnotation {

		String stringValue();

		byte byteValue();

		short shortValue();

		int intValue();

		long longValue();

		boolean booleanValue();

		char charValue();

		double doubleValue();

		float floatValue();

	}

	@SimpleTypesAnnotation(stringValue = "string", byteValue = 1, shortValue = 2, intValue = 3, longValue = 4, booleanValue = true, charValue = 'c', doubleValue = 5.0, floatValue = 6.0f)
	static class WithSimpleTypesAnnotation {

	}

}
