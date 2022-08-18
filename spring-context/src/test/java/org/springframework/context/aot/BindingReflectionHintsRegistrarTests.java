/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.aot;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.JavaReflectionHint.Category;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindingReflectionHintsRegistrar}.
 *
 * @author Sebastien Deleuze
 */
public class BindingReflectionHintsRegistrarTests {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void registerTypeForSerializationWithEmptyClass() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleEmptyClass.class);
		assertThat(this.hints.reflection().javaReflection()).singleElement()
				.satisfies(javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleEmptyClass.class));
					assertThat(javaReflectionHint.getCategories()).containsExactlyInAnyOrder(
							Category.DECLARED_FIELDS, Category.INVOKE_DECLARED_CONSTRUCTORS);
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				});
	}

	@Test
	void registerTypeForSerializationWithNoProperty() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithNoProperty.class);
		assertThat(this.hints.reflection().javaReflection()).singleElement()
				.satisfies(javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassWithNoProperty.class)));
	}

	@Test
	void registerTypeForSerializationWithGetter() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithGetter.class);
		assertThat(this.hints.reflection().javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassWithGetter.class));
					assertThat(javaReflectionHint.methods()).singleElement().satisfies(methodHint -> {
						assertThat(methodHint.getName()).isEqualTo("getName");
						assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
					});
				});
	}

	@Test
	void registerTypeForSerializationWithSetter() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithSetter.class);
		assertThat(this.hints.reflection().javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassWithSetter.class));
					assertThat(javaReflectionHint.methods()).singleElement().satisfies(methodHint -> {
						assertThat(methodHint.getName()).isEqualTo("setName");
						assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
					});
				});
	}

	@Test
	void registerTypeForSerializationWithListProperty() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithListProperty.class);
		assertThat(this.hints.reflection().javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(List.class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassWithListProperty.class));
					assertThat(javaReflectionHint.methods()).satisfiesExactlyInAnyOrder(
							methodHint -> {
								assertThat(methodHint.getName()).isEqualTo("setNames");
								assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
							},
							methodHint -> {
								assertThat(methodHint.getName()).isEqualTo("getNames");
								assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
							});
				});
	}

	@Test
	void registerTypeForSerializationWithCycles() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithCycles.class);
		assertThat(this.hints.reflection().javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassWithCycles.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(List.class)));
	}

	@Test
	void registerTypeForSerializationWithResolvableType() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithResolvableType.class);
		assertThat(this.hints.reflection().javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(ResolvableType[].class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(Type.class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(Class.class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(ResolvableType.class));
					assertThat(javaReflectionHint.getCategories()).containsExactlyInAnyOrder(
							Category.DECLARED_FIELDS, Category.INVOKE_DECLARED_CONSTRUCTORS);
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).hasSizeGreaterThan(1);
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassWithResolvableType.class));
					assertThat(javaReflectionHint.methods()).singleElement().satisfies(
							methodHint -> {
								assertThat(methodHint.getName()).isEqualTo("getResolvableType");
								assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
							});
				});
	}

	@Test
	void registerTypeForSerializationWithMultipleLevelsAndCollection() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassA.class);
		assertThat(this.hints.reflection().javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassA.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassB.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleClassC.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class)),
				javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(Set.class)));
	}

	@Test
	void registerTypeForSerializationWithEnum() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleEnum.class);
		assertThat(this.hints.reflection().javaReflection()).singleElement()
				.satisfies(javaReflectionHint -> assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleEnum.class)));
	}

	@Test
	void registerTypeForSerializationWithRecord() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleRecord.class);
		assertThat(this.hints.reflection().javaReflection()).satisfiesExactlyInAnyOrder(
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(javaReflectionHint.getCategories()).isEmpty();
					assertThat(javaReflectionHint.constructors()).isEmpty();
					assertThat(javaReflectionHint.fields()).isEmpty();
					assertThat(javaReflectionHint.methods()).isEmpty();
				},
				javaReflectionHint -> {
					assertThat(javaReflectionHint.getType()).isEqualTo(TypeReference.of(SampleRecord.class));
					assertThat(javaReflectionHint.methods()).singleElement().satisfies(methodHint -> {
						assertThat(methodHint.getName()).isEqualTo("name");
						assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
					});
				});
	}


	static class SampleEmptyClass {
	}

	static class SampleClassWithNoProperty {

		String name() {
			return null;
		}
	}

	static class SampleClassWithGetter {

		public String getName() {
			return null;
		}

		public SampleEmptyClass unmanaged() {
			return null;
		}
	}

	static class SampleClassWithSetter {

		public void setName(String name) {
		}

		public SampleEmptyClass unmanaged() {
			return null;
		}
	}

	static class SampleClassWithListProperty {

		public List<String> getNames() {
			return null;
		}

		public void setNames(List<String> names) {
		}
	}

	static class SampleClassWithCycles {

		public SampleClassWithCycles getSampleClassWithCycles() {
			return null;
		}

		public List<SampleClassWithCycles> getSampleClassWithCyclesList() {
			return null;
		}
	}

	static class SampleClassWithResolvableType {

		public ResolvableType getResolvableType() {
			return null;
		}
	}

	static class SampleClassA {
		public Set<SampleClassB> getB() {
			return null;
		}
	}

	static class SampleClassB {
		public SampleClassC getC() {
			return null;
		}
	}

	class SampleClassC {
		public String getString() {
			return "";
		}
	}

	enum SampleEnum {
		value1, value2
	}

	record SampleRecord(String name) {}

}
