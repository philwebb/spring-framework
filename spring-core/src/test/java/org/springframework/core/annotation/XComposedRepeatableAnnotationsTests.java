/*
 * Copyright 2002-2016 the original author or authors.
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
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Iterator;
import java.util.Set;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.XComposedRepeatableAnnotationsTests.PeteRepeat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;

/**
 * Unit tests that verify support for getting and finding all composed,
 * repeatable annotations on a single annotated element.
 *
 * <p>
 * See <a href="https://jira.spring.io/browse/SPR-13973">SPR-13973</a>.
 *
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class XComposedRepeatableAnnotationsTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void getNonRepeatableAnnotation() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> getMergedRepeatableAnnotations(getClass(), null,
						NonRepeatable.class)).satisfies(this::nonRepeatableRequirements);
	}

	@Test
	public void getInvalidRepeatableAnnotationContainerMissingValueAttribute() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> getMergedRepeatableAnnotations(getClass(),
						ContainerMissingValueAttribute.class,
						InvalidRepeatable.class)).satisfies(
								this::missingValueAttributeRequirements);
	}

	@Test
	public void getInvalidRepeatableAnnotationContainerWithNonArrayValueAttribute() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> getMergedRepeatableAnnotations(getClass(),
						ContainerWithNonArrayValueAttribute.class,
						InvalidRepeatable.class)).satisfies(
								this::nonArrayValueAttributeRequirements);
	}

	@Test
	public void getInvalidRepeatableAnnotationContainerWithArrayValueAttributeButWrongComponentType() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> getMergedRepeatableAnnotations(getClass(),
						ContainerWithArrayValueAttributeButWrongComponentType.class,
						InvalidRepeatable.class)).satisfies(
								this::wrongComponentTypeRequirements);
	}

	@Test
	public void getRepeatableAnnotationsOnClass() {
		Set<PeteRepeat> annotations = getMergedRepeatableAnnotations(
				RepeatableClass.class, null, PeteRepeat.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void getRepeatableAnnotationsOnSuperclass() {
		Set<PeteRepeat> annotations = getMergedRepeatableAnnotations(
				SubRepeatableClass.class, null, PeteRepeat.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void getComposedRepeatableAnnotationsOnClass() {
		Set<PeteRepeat> annotations = getMergedRepeatableAnnotations(
				ComposedRepeatableClass.class, null, PeteRepeat.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void getComposedRepeatableAnnotationsMixedWithContainerOnClass() {
		Set<PeteRepeat> annotations = getMergedRepeatableAnnotations(
				ComposedRepeatableMixedWithContainerClass.class, null, PeteRepeat.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void getComposedContainerForRepeatableAnnotationsOnClass() {
		Set<PeteRepeat> annotations = getMergedRepeatableAnnotations(
				ComposedContainerClass.class, null, PeteRepeat.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void getNoninheritedComposedRepeatableAnnotationsOnClass() {
		Class<?> element = NoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = getMergedRepeatableAnnotations(element, null,
				Noninherited.class);
		assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
				"B", "C");
	}

	@Test
	public void getNoninheritedComposedRepeatableAnnotationsOnSuperclass() {
		Class<?> element = SubNoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = getMergedRepeatableAnnotations(element, null,
				Noninherited.class);
		assertNotNull(annotations);
		assertEquals(0, annotations.size());
	}

	@Test
	public void findNonRepeatableAnnotation() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> findMergedRepeatableAnnotations(getClass(),
						NonRepeatable.class)).satisfies(this::nonRepeatableRequirements);
	}

	@Test
	public void findInvalidRepeatableAnnotationContainerMissingValueAttribute() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class,
						ContainerMissingValueAttribute.class)).satisfies(
								this::missingValueAttributeRequirements);
	}

	@Test
	public void findInvalidRepeatableAnnotationContainerWithNonArrayValueAttribute() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class,
						ContainerWithNonArrayValueAttribute.class)).satisfies(
								this::nonArrayValueAttributeRequirements);
	}

	@Test
	public void findInvalidRepeatableAnnotationContainerWithArrayValueAttributeButWrongComponentType() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class,
						ContainerWithArrayValueAttributeButWrongComponentType.class)).satisfies(
								this::wrongComponentTypeRequirements);
	}

	@Test
	public void findRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(RepeatableClass.class);
	}

	@Test
	public void findRepeatableAnnotationsOnSuperclass() {
		assertFindRepeatableAnnotations(SubRepeatableClass.class);
	}

	@Test
	public void findComposedRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(ComposedRepeatableClass.class);
	}

	@Test
	public void findComposedRepeatableAnnotationsMixedWithContainerOnClass() {
		assertFindRepeatableAnnotations(ComposedRepeatableMixedWithContainerClass.class);
	}

	@Test
	public void findComposedContainerForRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(ComposedContainerClass.class);
	}

	private void assertFindRepeatableAnnotations(AnnotatedElement element) {
		assertNotNull(element);

		Set<PeteRepeat> peteRepeats = findMergedRepeatableAnnotations(element,
				PeteRepeat.class);
		assertNotNull(peteRepeats);
		assertEquals(3, peteRepeats.size());

		Iterator<PeteRepeat> iterator = peteRepeats.iterator();
		assertEquals("A", iterator.next().value());
		assertEquals("B", iterator.next().value());
		assertEquals("C", iterator.next().value());
	}

	@Test
	public void findNoninheritedComposedRepeatableAnnotationsOnClass() {
		Class<?> element = NoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = findMergedRepeatableAnnotations(element,
				Noninherited.class);
		assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
				"B", "C");
	}

	@Test
	public void findNoninheritedComposedRepeatableAnnotationsOnSuperclass() {
		Class<?> element = SubNoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = findMergedRepeatableAnnotations(element,
				Noninherited.class);
		assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
				"B", "C");
	}

	private <A extends Annotation> Set<A> getMergedRepeatableAnnotations(
			AnnotatedElement element, Class<? extends Annotation> container,
			Class<A> repeatable) {
		RepeatableContainers containers = RepeatableContainers.of(container, repeatable);
		MergedAnnotations annotations = MergedAnnotations.from(containers,
				AnnotationFilter.PLAIN, SearchStrategy.INHERITED_ANNOTATIONS, element);
		return annotations.stream(repeatable).collect(
				MergedAnnotationCollectors.toAnnotationSet());
	}

	private void nonRepeatableRequirements(Exception ex) {
		assertThat(ex.getMessage()).startsWith(
				"Annotation type must be a repeatable annotation").contains(
						"failed to resolve container type for",
						NonRepeatable.class.getName());
	}

	private void missingValueAttributeRequirements(Exception ex) {
		assertThat(ex.getMessage()).startsWith(
				"Invalid declaration of container type").contains(
						ContainerMissingValueAttribute.class.getName(),
						"for repeatable annotation", InvalidRepeatable.class.getName());
		assertThat(ex).hasCauseInstanceOf(NoSuchMethodException.class);
	}

	private void nonArrayValueAttributeRequirements(Exception ex) {
		assertThat(ex.getMessage()).startsWith("Container type").contains(
				ContainerWithNonArrayValueAttribute.class.getName(),
				"must declare a 'value' attribute for an array of type",
				InvalidRepeatable.class.getName());
	}

	private void wrongComponentTypeRequirements(Exception ex) {
		assertThat(ex.getMessage()).startsWith("Container type").contains(
				ContainerWithArrayValueAttributeButWrongComponentType.class.getName(),
				"must declare a 'value' attribute for an array of type",
				InvalidRepeatable.class.getName());
	}



	private static ThrowableTypeAssert<AnnotationConfigurationException> assertThatAnnotationConfigurationException() {
		return assertThatExceptionOfType(AnnotationConfigurationException.class);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NonRepeatable {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ContainerMissingValueAttribute {

		// InvalidRepeatable[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ContainerWithNonArrayValueAttribute {

		InvalidRepeatable value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ContainerWithArrayValueAttributeButWrongComponentType {

		String[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidRepeatable {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface PeteRepeats {

		PeteRepeat[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Repeatable(PeteRepeats.class)
	@interface PeteRepeat {

		String value();

	}

	@PeteRepeat("shadowed")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface ForPetesSake {

		@AliasFor(annotation = PeteRepeat.class)
		String value();

	}

	@PeteRepeat("shadowed")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface ForTheLoveOfFoo {

		@AliasFor(annotation = PeteRepeat.class)
		String value();

	}

	@PeteRepeats({ @PeteRepeat("B"), @PeteRepeat("C") })
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface ComposedContainer {

	}

	@PeteRepeat("A")
	@PeteRepeats({ @PeteRepeat("B"), @PeteRepeat("C") })
	static class RepeatableClass {

	}

	static class SubRepeatableClass extends RepeatableClass {

	}

	@ForPetesSake("B")
	@ForTheLoveOfFoo("C")
	@PeteRepeat("A")
	static class ComposedRepeatableClass {

	}

	@ForPetesSake("C")
	@PeteRepeats(@PeteRepeat("A"))
	@PeteRepeat("B")
	static class ComposedRepeatableMixedWithContainerClass {

	}

	@PeteRepeat("A")
	@ComposedContainer
	static class ComposedContainerClass {

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Noninheriteds {

		Noninherited[] value();

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(Noninheriteds.class)
	@interface Noninherited {

		@AliasFor("name")
		String value() default "";

		@AliasFor("value")
		String name() default "";

	}

	@Noninherited(name = "shadowed")
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedNoninherited {

		@AliasFor(annotation = Noninherited.class)
		String name() default "";

	}

	@ComposedNoninherited(name = "C")
	@Noninheriteds({ @Noninherited(value = "A"), @Noninherited(name = "B") })
	static class NoninheritedRepeatableClass {

	}

	static class SubNoninheritedRepeatableClass extends NoninheritedRepeatableClass {

	}

}
