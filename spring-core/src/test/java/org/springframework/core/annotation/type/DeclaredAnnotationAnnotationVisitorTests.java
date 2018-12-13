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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.SpringAsmInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeclaredAnnotationAnnotationVisitor}.
 *
 * @author Phillip Webb
 */
public class DeclaredAnnotationAnnotationVisitorTests {

	@Test
	public void visitShouldConsumeDeclaredAnnotation() throws Exception {
		DeclaredAnnotation annotation = visit(WithExample.class, Example.class.getName());
		assertThat(annotation.getType()).isEqualTo(Example.class.getName());
		assertThat(annotation.getAttributes().get("value")).isEqualTo("test");
	}

	private DeclaredAnnotation visit(Class<?> type, String annotationType)
			throws IOException {
		DeclaredAnnotation[] result = new DeclaredAnnotation[1];
		AnnotationVisitor annotationVisitor = new DeclaredAnnotationAnnotationVisitor(
				annotationType, declaredAnnotation -> result[0] = declaredAnnotation);
		ClassVisitor classVisitor = new ClassVisitor(SpringAsmInfo.ASM_VERSION) {

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return annotationVisitor;
			}

		};
		ClassReader reader = new ClassReader(type.getName());
		reader.accept(classVisitor, ClassReader.SKIP_DEBUG);
		return result[0];
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface Example {

		String value();

	}

	@Example(value = "test")
	private static class WithExample {

	}

}
