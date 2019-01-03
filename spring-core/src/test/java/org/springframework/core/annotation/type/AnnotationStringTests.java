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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationString}.
 *
 * @author Phillip Webb
 */
public class AnnotationStringTests {

	@Test
	public void getForAnnotationTypeReturnsString() {
		assertThat(AnnotationString.get(newAnnotationType())).isEqualTo(
				"@com.example.Service(name=\"test\")\n"
						+ "@interface com.example.Component {\n\n"
						+ "\t@com.example.Repository(name=\"repository\")\n"
						+ "\tjava.lang.String name() default \"defaultName\";\n\n" + "}");
	}

	@Test
	public void getForAttributeTypesReturnsString() {
		assertThat(AnnotationString.get(newAttributeTypes())).isEqualTo(
				"@com.example.Repository(name=\"repository\")\n"
						+ "java.lang.String name() default \"defaultName\";");
	}

	@Test
	public void getForAttributeTypeReturnsString() {
		assertThat(AnnotationString.get(newAttributeType())).isEqualTo(
				"@com.example.Repository(name=\"repository\")\n"
						+ "java.lang.String name() default \"defaultName\";");
	}

	@Test
	public void getForDeclaredAnnotationsReturnsString() {
		assertThat(AnnotationString.get(newDeclaredAnnotations())).isEqualTo(
				"@com.example.Service(name=\"test\")");
	}

	@Test
	public void getForDeclaredAnnotationReturnsString() {
		assertThat(AnnotationString.get(newDeclaredAnnotation())).isEqualTo(
				"@com.example.Service(name=\"test\")");
	}

	@Test
	public void getForDecalredAttributesReturnsString() {
		assertThat(AnnotationString.get(newDeclaredAttributes())).isEqualTo(
				"(name=\"test\")");
	}

	@Test
	public void getForDeclaredAttributeReturnsString() {
		assertThat(AnnotationString.get(newDeclaredAttribute())).isEqualTo(
				"name=\"test\"");
	}

	@Test
	public void getForDeclaredAttributeWhenEnumArrayReturnsString() {
		DeclaredAttribute attribute = DeclaredAttribute.of("name",
				new EnumValueReference[] { EnumValueReference.of("com.example", "ONE") });
		assertThat(AnnotationString.get(attribute)).isEqualTo(
				"name={ONE}");
	}

	private AnnotationType newAnnotationType() {
		return AnnotationType.of("com.example.Component", newDeclaredAnnotations(),
				newAttributeTypes());
	}

	private DeclaredAnnotations newDeclaredAnnotations() {
		return DeclaredAnnotations.of(null, newDeclaredAnnotation());
	}

	private DeclaredAnnotation newDeclaredAnnotation() {
		return DeclaredAnnotation.of("com.example.Service", newDeclaredAttributes());
	}

	private DeclaredAttributes newDeclaredAttributes() {
		return DeclaredAttributes.of(newDeclaredAttribute());
	}

	private DeclaredAttribute newDeclaredAttribute() {
		return DeclaredAttribute.of("name", "test");
	}

	private AttributeTypes newAttributeTypes() {
		return AttributeTypes.of(newAttributeType());
	}

	private AttributeType newAttributeType() {
		return AttributeType.of("name", "java.lang.String",
				DeclaredAnnotations.of(null,
						DeclaredAnnotation.of("com.example.Repository",
								DeclaredAttributes.of("name", "repository"))),
				"defaultName");
	}

}
