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
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Test;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets;
import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.lang.UsesSunMisc;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link AnnotationTypeMappings} and {@link AnnotationTypeMapping}.
 *
 * @author Phillip Webb
 */
public class AnnotationTypeMappingsTests {

	@Test
	public void forAnnotationTypeWhenAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(null)).withMessage(
						"AnnotationType must not be null");
	}

	@Test
	public void forAnnotationTypeWhenNoMetaAnnotationsReturnsMappings() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				SimpleAnnotation.class);
		assertThat(mappings.size()).isEqualTo(1);
		assertThat(mappings.get(0).getAnnotationType()).isEqualTo(SimpleAnnotation.class);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						SimpleAnnotation.class);
	}

	@Test
	public void forAnnotationWhenHasSpringAnnotationReturnsFilteredMappings() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				WithSpringLangAnnotation.class);
		assertThat(mappings.size()).isEqualTo(1);
	}

	@Test
	public void forAnnotationTypeWhenMetaAnnotationsReturnsMappings() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				MetaAnnotated.class);
		assertThat(mappings.size()).isEqualTo(6);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						MetaAnnotated.class, A.class, B.class, AA.class, AB.class,
						ABC.class);
	}

	@Test
	public void forAnnotationTypeWhenHasRepeatingMetaAnnotationReturnsMapping() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				WithRepeatedMetaAnnotations.class);
		assertThat(mappings.size()).isEqualTo(3);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						WithRepeatedMetaAnnotations.class, Repeating.class,
						Repeating.class);
	}

	@Test
	public void forAnnotationTypeWhenSelfAnnotatedReturnsMapping() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				SelfAnnotated.class);
		assertThat(mappings.size()).isEqualTo(1);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						SelfAnnotated.class);
	}

	@Test
	public void forAnnotationTypeWhenFormsLoopReturnsMapping() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				LoopA.class);
		assertThat(mappings.size()).isEqualTo(2);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(LoopA.class,
						LoopB.class);
	}

	@Test
	public void forAnnotationTypeWhenHasAliasForWithBothValueAndAttributeThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForWithBothValueAndAttribute.class)).withMessage(
								"In @AliasFor declared on attribute 'test' in annotation ["
										+ AliasForWithBothValueAndAttribute.class.getName()
										+ "], attribute 'attribute' and its alias 'value' are present with values of 'foo' and 'bar', but only one is permitted.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForToSelfNonExistingAttribute() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForToSelfNonExistingAttribute.class)).withMessage(
								"@AliasFor declaration on attribute 'test' in annotation ["
										+ AliasForToSelfNonExistingAttribute.class.getName()
										+ "] declares an alias for 'missing' which is not present.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForToOtherNonExistingAttribute() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForToOtherNonExistingAttribute.class)).withMessage(
								"Attribute 'test' in annotation ["
										+ AliasForToOtherNonExistingAttribute.class.getName()
										+ "] is declared as an @AliasFor nonexistent "
										+ "attribute 'missing' in annotation ["
										+ AliasForToOtherNonExistingAttributeTarget.class.getName()
										+ "].");
	}

	@Test
	public void forAnnotationTypeWhenAliasForToSelf() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForToSelf.class)).withMessage(
								"@AliasFor declaration on attribute 'test' in annotation ["
										+ AliasForToSelf.class.getName()
										+ "] points to itself. Specify 'annotation' to point to "
										+ "a same-named attribute on a meta-annotation.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForWithArrayCompatibleReturnTypes() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				AliasForWithArrayCompatibleReturnTypes.class);
		AnnotationTypeMapping mapping = getMapping(mappings,
				AliasForWithArrayCompatibleReturnTypesTarget.class);
		assertThat(getMappedAttribute(mapping, 0).getName()).isEqualTo("test");
	}

	@Test
	public void forAnnotationTypeWhenAliasForWithIncompatibleReturnTypes() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForWithIncompatibleReturnTypes.class)).withMessage(
								"Misconfigured aliases: attribute 'test' in annotation ["
										+ AliasForWithIncompatibleReturnTypes.class.getName()
										+ "] and attribute 'test' in annotation ["
										+ AliasForWithIncompatibleReturnTypesTarget.class.getName()
										+ "] must declare the same return type.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForToSelfNonAnnotatedAttribute() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForToSelfNonAnnotatedAttribute.class)).withMessage(
								"Attribute 'other' in annotation ["
										+ AliasForToSelfNonAnnotatedAttribute.class.getName()
										+ "] must be declared as an @AliasFor 'test'.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForToSelfAnnotatedToOtherAttribute() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForToSelfAnnotatedToOtherAttribute.class)).withMessage(
								"Attribute 'b' in annotation ["
										+ AliasForToSelfAnnotatedToOtherAttribute.class.getName()
										+ "] must be declared as an @AliasFor 'a', not 'c'.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForNonMetaAnnotated() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForNonMetaAnnotated.class)).withMessage(
								"@AliasFor declaration on attribute 'test' in annotation ["
										+ AliasForNonMetaAnnotated.class.getName()
										+ "] declares an alias for attribute 'test' in annotation ["
										+ AliasForNonMetaAnnotatedTarget.class.getName()
										+ "] which is not meta-present.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForSelfWithDifferentDefaults() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForSelfWithDifferentDefaults.class)).withMessage(
								"Misconfigured aliases: attribute 'a' in annotation ["
										+ AliasForSelfWithDifferentDefaults.class.getName()
										+ "] and attribute 'b' in annotation ["
										+ AliasForSelfWithDifferentDefaults.class.getName()
										+ "] must declare the same default value.");
	}

	@Test
	public void forAnnotationTypeWhenAliasForSelfWithMissingDefault() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForSelfWithMissingDefault.class)).withMessage(
								"Misconfigured aliases: attribute 'a' in annotation ["
										+ AliasForSelfWithMissingDefault.class.getName()
										+ "] and attribute 'b' in annotation ["
										+ AliasForSelfWithMissingDefault.class.getName()
										+ "] must declare default values.");
	}

	@Test
	public void forAnnotationTypeWhenAliasWithExplicitMirrorAndDifferentDefaults() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasWithExplicitMirrorAndDifferentDefaults.class)).withMessage(
								"Misconfigured aliases: attribute 'a' in annotation ["
										+ AliasWithExplicitMirrorAndDifferentDefaults.class.getName()
										+ "] and attribute 'c' in annotation ["
										+ AliasWithExplicitMirrorAndDifferentDefaults.class.getName()
										+ "] must declare the same default value.");
	}

	@Test
	public void getDepthReturnsDepth() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				Mapped.class);
		assertThat(mappings.get(0).getDepth()).isEqualTo(0);
		assertThat(mappings.get(1).getDepth()).isEqualTo(1);
	}

	@Test
	public void getAnnotationTypeReturnsAnnotationType() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				Mapped.class);
		assertThat(mappings.get(0).getAnnotationType()).isEqualTo(Mapped.class);
		assertThat(mappings.get(1).getAnnotationType()).isEqualTo(MappedTarget.class);
	}

	@Test
	public void getAnnotationWhenRootReturnsNull() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				Mapped.class);
		assertThat(mappings.get(0).getAnnotation()).isNull();
	}

	@Test
	public void getAnnotationWhenMetaAnnotationReturnsAnnotation() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				Mapped.class);
		assertThat(mappings.get(1).getAnnotation()).isEqualTo(
				Mapped.class.getAnnotation(MappedTarget.class));

	}

	@Test
	public void getAttributesReturnsAttributes() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				Mapped.class).get(0);
		AttributeMethods attributes = mapping.getAttributes();
		assertThat(attributes.size()).isEqualTo(2);
		assertThat(attributes.get(0).getName()).isEqualTo("alias");
		assertThat(attributes.get(1).getName()).isEqualTo("convention");
	}

	@Test
	public void getMappedAttributeReturnsAttributes() throws Exception {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				Mapped.class).get(1);
		assertThat(getMappedAttribute(mapping, 0)).isEqualTo(
				Mapped.class.getDeclaredMethod("alias"));
		assertThat(getMappedAttribute(mapping, 1)).isEqualTo(
				Mapped.class.getDeclaredMethod("convention"));
	}

	@Test
	public void getMirrorSetWhenAliasPairReturnsMirrors() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				AliasPair.class).get(0);
		MirrorSets mirrorSets = mapping.getMirrorSets();
		assertThat(mirrorSets.size()).isEqualTo(1);
		assertThat(mirrorSets.get(0).size()).isEqualTo(2);
		assertThat(mirrorSets.get(0).get(0).getName()).isEqualTo("a");
		assertThat(mirrorSets.get(0).get(1).getName()).isEqualTo("b");
	}

	@Test
	public void getMirrorSetWhenImplicitMirrorsReturnsMirrors() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				ImplicitMirrors.class).get(0);
		MirrorSets mirrorSets = mapping.getMirrorSets();
		assertThat(mirrorSets.size()).isEqualTo(1);
		assertThat(mirrorSets.get(0).size()).isEqualTo(2);
		assertThat(mirrorSets.get(0).get(0).getName()).isEqualTo("a");
		assertThat(mirrorSets.get(0).get(1).getName()).isEqualTo("b");
	}

	@Test
	public void getMirrorSetWhenThreeDeepReturnsMirrors() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				ThreeDeepA.class);
		AnnotationTypeMapping mappingA = mappings.get(0);
		MirrorSets mirrorSetsA = mappingA.getMirrorSets();
		assertThat(mirrorSetsA.size()).isEqualTo(2);
		assertThat(getNames(mirrorSetsA.get(0))).containsExactly("a1", "a2", "a3");
		AnnotationTypeMapping mappingB = mappings.get(1);
		MirrorSets mirrorSetsB = mappingB.getMirrorSets();
		assertThat(mirrorSetsB.size()).isEqualTo(1);
		assertThat(getNames(mirrorSetsB.get(0))).containsExactly("b1", "b2");
		AnnotationTypeMapping mappingC = mappings.get(2);
		MirrorSets mirrorSetsC = mappingC.getMirrorSets();
		assertThat(mirrorSetsC.size()).isEqualTo(0);
	}

	@Test
	public void getMappedAttributeWhenThreeDeepReturnsMappedAttributes() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				ThreeDeepA.class);
		AnnotationTypeMapping mappingA = mappings.get(0);
		assertThat(getMappedAttribute(mappingA, 0)).isNull();
		assertThat(getMappedAttribute(mappingA, 1)).isNull();
		assertThat(getMappedAttribute(mappingA, 2)).isNull();
		assertThat(getMappedAttribute(mappingA, 3)).isNull();
		assertThat(getMappedAttribute(mappingA, 4)).isNull();
		AnnotationTypeMapping mappingB = mappings.get(1);
		assertThat(getMappedAttribute(mappingB, 0).getName()).isEqualTo("a1");
		assertThat(getMappedAttribute(mappingB, 1).getName()).isEqualTo("a1");
		AnnotationTypeMapping mappingC = mappings.get(2);
		assertThat(getMappedAttribute(mappingC, 0).getName()).isEqualTo("a1");
		assertThat(getMappedAttribute(mappingC, 1).getName()).isEqualTo("a4");
	}

	@Test
	public void getMappedAttributesWhenHasDefinedAttributesReturnsMappedAttributes() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				DefinedAttributes.class).get(1);
		assertThat(getMappedAttribute(mapping, 0)).isNull();
		assertThat(getMappedAttribute(mapping, 1).getName()).isEqualTo("value");
	}

	@Test
	public void resolveMirrorsWhenAliasPairResolves() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				AliasPair.class).get(0);
		Method[] resolvedA = resolveMirrorSets(mapping, WithAliasPairA.class,
				AliasPair.class);
		assertThat(resolvedA[0].getName()).isEqualTo("a");
		assertThat(resolvedA[1].getName()).isEqualTo("a");
		Method[] resolvedB = resolveMirrorSets(mapping, WithAliasPairB.class,
				AliasPair.class);
		assertThat(resolvedB[0].getName()).isEqualTo("b");
		assertThat(resolvedB[1].getName()).isEqualTo("b");
	}

	@Test
	public void resolveMirrorsWhenHasSameValuesUsesFirst() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				AliasPair.class).get(0);
		Method[] resolved = resolveMirrorSets(mapping, WithSameValueAliasPair.class,
				AliasPair.class);
		assertThat(resolved[0].getName()).isEqualTo("a");
		assertThat(resolved[1].getName()).isEqualTo("a");
	}

	@Test
	public void resolveMirrorsWhenHasDefaultValuesUsesFirst() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				AliasPair.class).get(0);
		Method[] resolved = resolveMirrorSets(mapping, WithDefaultValueAliasPair.class,
				AliasPair.class);
		assertThat(resolved[0].getName()).isEqualTo("a");
		assertThat(resolved[1].getName()).isEqualTo("a");
	}

	@Test
	public void resolveMirrorsWhenHasDifferentValuesThrowsException() {
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				AliasPair.class).get(0);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> resolveMirrorSets(mapping, WithDifferentValueAliasPair.class,
						AliasPair.class)).withMessage(
								"Different @AliasFor mirror values for annotation ["
										+ AliasPair.class.getName() + "] declared on "
										+ WithDifferentValueAliasPair.class.getName()
										+ ", attribute 'a' and its alias 'b' are declared with values of [test1] and [test2].");
	}

	@Test
	public void resolveMirrorsWhenHasWithMulipleRoutesToAliasReturnsMirrors() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				MulipleRoutesToAliasA.class);
		AnnotationTypeMapping mappingsA = getMapping(mappings,
				MulipleRoutesToAliasA.class);
		assertThat(mappingsA.getMirrorSets().size()).isZero();
		AnnotationTypeMapping mappingsB = getMapping(mappings,
				MulipleRoutesToAliasB.class);
		assertThat(getNames(mappingsB.getMirrorSets().get(0))).containsExactly("b1", "b2",
				"b3");
		AnnotationTypeMapping mappingsC = getMapping(mappings,
				MulipleRoutesToAliasC.class);
		assertThat(getNames(mappingsC.getMirrorSets().get(0))).containsExactly("c1",
				"c2");
	}

	@Test
	public void getMappedAttributeWhenHasWithMulipleRoutesToAliasReturnsMappedAttributes() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				MulipleRoutesToAliasA.class);
		AnnotationTypeMapping mappingsA = getMapping(mappings,
				MulipleRoutesToAliasA.class);
		assertThat(getMappedAttribute(mappingsA, 0)).isNull();
		AnnotationTypeMapping mappingsB = getMapping(mappings,
				MulipleRoutesToAliasB.class);
		assertThat(getMappedAttribute(mappingsB, 0).getName()).isEqualTo("a1");
		assertThat(getMappedAttribute(mappingsB, 1).getName()).isEqualTo("a1");
		assertThat(getMappedAttribute(mappingsB, 2).getName()).isEqualTo("a1");
		AnnotationTypeMapping mappingsC = getMapping(mappings,
				MulipleRoutesToAliasC.class);
		assertThat(getMappedAttribute(mappingsC, 0).getName()).isEqualTo("a1");
		assertThat(getMappedAttribute(mappingsC, 1).getName()).isEqualTo("a1");
	}

	@Test
	public void getMappedAttributeWhenConventionToExplicitAliasesReturnsMappedAttributes() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(ConventionToExplicitAliases.class);
		AnnotationTypeMapping mapping = getMapping(mappings, ConventionToExplicitAliasesTarget.class);
		assertThat(mapping.getMappedAttribute(0)).isEqualTo(0);
		assertThat(mapping.getMappedAttribute(1)).isEqualTo(0);
	}

	private Method[] resolveMirrorSets(AnnotationTypeMapping mapping, Class<?> element,
			Class<? extends Annotation> annotationClass) {
		Annotation annotation = element.getAnnotation(annotationClass);
		int[] resolved = mapping.getMirrorSets().resolve(element.getName(), annotation,
				ReflectionUtils::invokeMethod);
		Method[] result = new Method[resolved.length];
		for (int i = 0; i < resolved.length; i++) {
			result[i] = mapping.getAttributes().get(resolved[i]);
		}
		return result;
	}

	@Nullable
	private Method getMappedAttribute(AnnotationTypeMapping mapping, int attributeIndex) {
		int mapped = mapping.getMappedAttribute(attributeIndex);
		return mapped != -1 ? mapping.getRoot().getAttributes().get(mapped) : null;
	}

	private AnnotationTypeMapping getMapping(AnnotationTypeMappings mappings,
			Class<? extends Annotation> annotationType) {
		for (AnnotationTypeMapping candidate : mappings) {
			if (candidate.getAnnotationType() == annotationType) {
				return candidate;
			}
		}
		return null;
	}

	private List<String> getNames(MirrorSet mirrorSet) {
		List<String> names = new ArrayList<>(mirrorSet.size());
		for (int i = 0; i < mirrorSet.size(); i++) {
			names.add(mirrorSet.get(i).getName());
		}
		return names;
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface SimpleAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@UsesSunMisc
	static @interface WithSpringLangAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@A
	@B
	static @interface MetaAnnotated {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AA
	@AB
	static @interface A {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AA {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ABC
	static @interface AB {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ABC {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface B {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeating
	@Repeating
	static @interface WithRepeatedMetaAnnotations {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(Repeatings.class)
	static @interface Repeating {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface Repeatings {

		Repeating[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@SelfAnnotated
	static @interface SelfAnnotated {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@LoopB
	static @interface LoopA {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@LoopA
	static @interface LoopB {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForWithBothValueAndAttribute {

		@AliasFor(value = "bar", attribute = "foo")
		String test();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForToSelfNonExistingAttribute {

		@AliasFor("missing")
		String test() default "";

		String other() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AliasForToOtherNonExistingAttributeTarget
	static @interface AliasForToOtherNonExistingAttribute {

		@AliasFor(annotation = AliasForToOtherNonExistingAttributeTarget.class, attribute = "missing")
		String test() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForToOtherNonExistingAttributeTarget {

		String other() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForToSelf {

		@AliasFor("test")
		String test() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AliasForWithArrayCompatibleReturnTypesTarget
	static @interface AliasForWithArrayCompatibleReturnTypes {

		@AliasFor(annotation = AliasForWithArrayCompatibleReturnTypesTarget.class)
		String test() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForWithArrayCompatibleReturnTypesTarget {

		String[] test() default {};

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForWithIncompatibleReturnTypes {

		@AliasFor(annotation = AliasForWithIncompatibleReturnTypesTarget.class)
		String[] test() default {};

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForWithIncompatibleReturnTypesTarget {

		String test() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForToSelfNonAnnotatedAttribute {

		@AliasFor("other")
		String test() default "";

		String other() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForToSelfAnnotatedToOtherAttribute {

		@AliasFor("b")
		String a() default "";

		@AliasFor("c")
		String b() default "";

		@AliasFor("a")
		String c() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForNonMetaAnnotated {

		@AliasFor(annotation = AliasForNonMetaAnnotatedTarget.class)
		String test() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForNonMetaAnnotatedTarget {

		String test() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForSelfWithDifferentDefaults {

		@AliasFor("b")
		String a() default "a";

		@AliasFor("a")
		String b() default "b";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForSelfWithMissingDefault {

		@AliasFor("b")
		String a() default "a";

		@AliasFor("a")
		String b();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AliasWithExplicitMirrorAndDifferentDefaultsTarget
	static @interface AliasWithExplicitMirrorAndDifferentDefaults {

		@AliasFor(annotation = AliasWithExplicitMirrorAndDifferentDefaultsTarget.class, attribute = "a")
		String a() default "x";

		@AliasFor(annotation = AliasWithExplicitMirrorAndDifferentDefaultsTarget.class, attribute = "a")
		String b() default "x";

		@AliasFor(annotation = AliasWithExplicitMirrorAndDifferentDefaultsTarget.class, attribute = "a")
		String c() default "y";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasWithExplicitMirrorAndDifferentDefaultsTarget {

		String a() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MappedTarget
	static @interface Mapped {

		String convention() default "";

		@AliasFor(annotation = MappedTarget.class, attribute = "aliasTarget")
		String alias() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface MappedTarget {

		String convention() default "";

		String aliasTarget() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasPair {

		@AliasFor("b")
		String a() default "";

		@AliasFor("a")
		String b() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImplicitMirrorsTarget
	static @interface ImplicitMirrors {

		@AliasFor(annotation = ImplicitMirrorsTarget.class, attribute = "c")
		String a() default "";

		@AliasFor(annotation = ImplicitMirrorsTarget.class, attribute = "c")
		String b() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ImplicitMirrorsTarget {

		@AliasFor("d")
		String c() default "";

		@AliasFor("c")
		String d() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ThreeDeepB
	static @interface ThreeDeepA {

		@AliasFor(annotation = ThreeDeepB.class, attribute = "b1")
		String a1() default "";

		@AliasFor(annotation = ThreeDeepB.class, attribute = "b2")
		String a2() default "";

		@AliasFor(annotation = ThreeDeepC.class, attribute = "c1")
		String a3() default "";

		@AliasFor(annotation = ThreeDeepC.class, attribute = "c2")
		String a4() default "";

		@AliasFor(annotation = ThreeDeepC.class, attribute = "c2")
		String a5() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ThreeDeepC
	static @interface ThreeDeepB {

		@AliasFor(annotation = ThreeDeepC.class, attribute = "c1")
		String b1() default "";

		@AliasFor(annotation = ThreeDeepC.class, attribute = "c1")
		String b2() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ThreeDeepC {

		String c1() default "";

		String c2() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@DefinedAttributesTarget(a = "test")
	static @interface DefinedAttributes {

		@AliasFor(annotation = DefinedAttributesTarget.class, attribute = "b")
		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface DefinedAttributesTarget {

		String a();

		String b() default "";

	}

	@AliasPair(a = "test")
	static class WithAliasPairA {

	}

	@AliasPair(b = "test")
	static class WithAliasPairB {

	}

	@AliasPair(a = "test", b = "test")
	static class WithSameValueAliasPair {

	}

	@AliasPair(a = "test1", b = "test2")
	static class WithDifferentValueAliasPair {

	}

	@AliasPair
	static class WithDefaultValueAliasPair {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MulipleRoutesToAliasB
	static @interface MulipleRoutesToAliasA {

		@AliasFor(annotation = MulipleRoutesToAliasB.class, attribute = "b2")
		String a1() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MulipleRoutesToAliasC
	static @interface MulipleRoutesToAliasB {

		@AliasFor(annotation = MulipleRoutesToAliasC.class, attribute = "c2")
		String b1() default "";

		@AliasFor(annotation = MulipleRoutesToAliasC.class, attribute = "c2")
		String b2() default "";

		@AliasFor(annotation = MulipleRoutesToAliasC.class, attribute = "c1")
		String b3() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface MulipleRoutesToAliasC {

		@AliasFor("c2")
		String c1() default "";

		@AliasFor("c1")
		String c2() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ConventionToExplicitAliasesTarget
	static @interface ConventionToExplicitAliases {

		String test() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ConventionToExplicitAliasesTarget {

		@AliasFor("test")
		String value() default "";

		@AliasFor("value")
		String test() default "";

	}


}
