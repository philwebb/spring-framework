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

package org.springframework.core.annotation;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link MergedAnnotations}.
 *
 * @author Phillip Webb
 */
@Ignore
public class XMergedAnnotationsTests {

	// FIXME revisit

	// FIXME revisit again

	@Rule
	public ExpectedException thrown = ExpectedException.none();
//
//	private final AnnotationTypeResolver resolver = AnnotationTypeResolver.get(
//			ClassUtils.getDefaultClassLoader());
//
//	@Test
//	public void createWhenResolverIsNullShouldThrowException() {
//		this.thrown.expect(IllegalArgumentException.class);
//		this.thrown.expectMessage("Resolver must not be null");
//		new AnnotationTypeMappings(null, RepeatableContainers.standardRepeatables(),
//				this.resolver.resolve(Example.class.getName()));
//	}
//
//	@Test
//	public void createWhenSourceIsNullShouldThrowException() {
//		this.thrown.expect(IllegalArgumentException.class);
//		this.thrown.expectMessage("Source must not be null");
//		new AnnotationTypeMappings(this.resolver,
//				RepeatableContainers.standardRepeatables(), null);
//	}
//
//	@Test
//	public void getMetaAnnotationWhenNoneReturnsNull() {
//		AnnotationTypeMappings mappings = getMappings(Example.class);
//		assertThat(mappings.getMapping(Inherited.class.getName())).isNull();
//	}
//
//	@Test
//	public void getMetaAnnotationReturnsImmediateMetaAnnotation() {
//		AnnotationTypeMappings mappings = getMappings(MetaAnnotatedExample.class);
//		AnnotationTypeMapping mapping = mappings.getMapping(Example.class.getName());
//		assertThat(mapping.getAnnotationType().getClassName()).isEqualTo(
//				Example.class.getName());
//		assertThat(mapping.getAttributes().get("value")).isEqualTo("meta-annotated");
//		assertThat(mapping.getDepth()).isEqualTo(1);
//		assertThat(mapping.getParent().getAnnotationType().getClassName()).isEqualTo(
//				MetaAnnotatedExample.class.getName());
//	}
//
//	@Test
//	public void getMetaAnnotationReturnsInheritedMetaAnnotation() {
//		AnnotationTypeMappings mappings = getMappings(MetaAnnotatedExample.class);
//		AnnotationTypeMapping mapping = mappings.getMapping(Example.class.getName());
//		assertThat(mapping.getAnnotationType().getClassName()).isEqualTo(
//				Example.class.getName());
//		assertThat(mapping.getAttributes().get("value")).isEqualTo("meta-annotated");
//		assertThat(mapping.getDepth()).isEqualTo(1);
//		assertThat(mapping.getParent().getAnnotationType().getClassName()).isEqualTo(
//				MetaAnnotatedExample.class.getName());
//	}
//
//	@Test
//	public void getMetaAnnotationWhenHasOverrideReturnsOverrideMetaAnnotation() {
//		AnnotationTypeMappings mappings = getMappings(MetaMetaAnnotationExample.class);
//		AnnotationTypeMapping mapping = mappings.getMapping(Example.class.getName());
//		assertThat(mapping.getAnnotationType().getClassName()).isEqualTo(
//				Example.class.getName());
//		assertThat(mapping.getAttributes().get("value")).isEqualTo("meta-annotated");
//		assertThat(mapping.getDepth()).isEqualTo(2);
//		AnnotationTypeMapping parent = mapping.getParent();
//		assertThat(parent.getAnnotationType().getClassName()).isEqualTo(
//				MetaAnnotatedExample.class.getName());
//	}
//
//	@Test
//	public void getMetaAnnotationWhenHasOverrideReturnsNearest() {
//		AnnotationTypeMappings mappings = getMappings(
//				MetaMetaAnnotatedOverrideExample.class);
//		AnnotationTypeMapping mapping = mappings.getMapping(Example.class.getName());
//		assertThat(mapping.getAnnotationType().getClassName()).isEqualTo(
//				Example.class.getName());
//		assertThat(mapping.getAttributes().get("value")).isEqualTo("meta-override");
//		assertThat(mapping.getDepth()).isEqualTo(1);
//		assertThat(mapping.getParent().getAnnotationType().getClassName()).isEqualTo(
//				MetaMetaAnnotatedOverrideExample.class.getName());
//	}
//
//	@Test
//	public void createWhenAliasForExplicitMirrorTargetAttributeDoesNotExistShouldThrowException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage(
//				"@AliasFor declaration on attribute 'one' in annotation ["
//						+ AlaisForExplicitMirrorTargetAttributeDoesNotExist.class.getName()
//						+ "] declares an alias for 'two' which is not present.");
//		getMappings(AlaisForExplicitMirrorTargetAttributeDoesNotExist.class);
//	}
//
//	@Test
//	public void createWhenAliasForAnnotationTargetNotMetaPresentThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage(
//				"@AliasFor declaration on attribute 'one' in annotation ["
//						+ AliasForAnnotationTargetNotMetaPresent.class.getName()
//						+ "] declares an alias for attribute 'one' in annotation ["
//						+ Example.class.getName() + "] which is not meta-present.");
//		getMappings(AliasForAnnotationTargetNotMetaPresent.class);
//	}
//
//	@Test
//	public void createWhenAliasForAnnotationTargetAttributeNotPresentThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Attribute 'one' in annotation ["
//				+ AliasForAnnotationTargetAttributeNotPresent.class.getName()
//				+ "] is declared as an @AliasFor nonexistent attribute 'nope' in annotation ["
//				+ Example.class.getName() + "].");
//		getMappings(AliasForAnnotationTargetAttributeNotPresent.class);
//	}
//
//	@Test
//	public void createWhenAliasForExplicitMirrorTargetHasNoAliasForAnnotationThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Attribute 'two' in annotation ["
//				+ AliasForExplicitMirrorTargetHasNoAliasForAnnotation.class.getName()
//				+ "] must be declared as an @AliasFor 'one'.");
//		getMappings(AliasForExplicitMirrorTargetHasNoAliasForAnnotation.class);
//	}
//
//	@Test
//	public void createWhenAliasForExplicitMirrorTargetHasAliasForDifferentAnnotationThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Attribute 'value' in annotation ["
//				+ AliasForExplicitMirrorTargetHasAliasForDifferentAnnotation.class.getName()
//				+ "] must be declared as an @AliasFor 'one', not attribute 'value' in annotation ["
//				+ Example.class.getName() + "].");
//		getMappings(AliasForExplicitMirrorTargetHasAliasForDifferentAnnotation.class);
//	}
//
//	@Test
//	public void createWhenAliasForExplicitMirrorReturnTypesDoNotMatchThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Misconfigured aliases: attribute 'one' in annotation ["
//				+ AliasForExplicitMirrorReturnTypesDoNotMatch.class.getName()
//				+ "] and attribute 'two' in annotation ["
//				+ AliasForExplicitMirrorReturnTypesDoNotMatch.class.getName()
//				+ "] must declare the same return type.");
//		getMappings(AliasForExplicitMirrorReturnTypesDoNotMatch.class);
//	}
//
//	@Test
//	public void createWhenAliasForReturnTypesDoNotMatchThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Misconfigured aliases: attribute 'one' in annotation ["
//				+ AliasForReturnTypesDoNotMatch.class.getName()
//				+ "] and attribute 'value' in annotation [" + Example.class.getName()
//				+ "] must declare the same return type.");
//		getMappings(AliasForReturnTypesDoNotMatch.class);
//	}
//
//	@Test
//	public void createWhenAlaisForReturnTypeIsElementOfArrayAllowsOverride() {
//		AnnotationType type = resolve(AliasForExplicitTargetArrayToElement.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		AnnotationTypeMapping mapping = mappings.getMapping(ExampleArray.class.getName());
//		assertThat(mapping.getAnnotationType().getClassName()).isEqualTo(
//				ExampleArray.class.getName());
//		assertThat(mapping.getDepth()).isEqualTo(1);
//		assertThat(mapping.getParent().getAnnotationType().getClassName()).isEqualTo(
//				AliasForExplicitTargetArrayToElement.class.getName());
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("arrayValue", "test"));
//		MergedAnnotation<Annotation> mapped = mapping.map(annotation, false);
//		assertThat(mapped.getStringArray("value")).containsExactly("test");
//	}
//
//	// FIXME default values with array?
//
//	@Test
//	public void createWhenAlaisForMirrorTypeIsElementOfArrayThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Misconfigured aliases: attribute 'array' in annotation ["
//				+ AliasForExplicitMirrorTargetArrayToElement.class.getName()
//				+ "] and attribute 'element' in annotation ["
//				+ AliasForExplicitMirrorTargetArrayToElement.class.getName()
//				+ "] must declare the same return type.");
//		getMappings(AliasForExplicitMirrorTargetArrayToElement.class);
//	}
//
//	@Test
//	public void createWhenAliasForReferencesSelfThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage(
//				"@AliasFor declaration on attribute 'one' in annotation ["
//						+ AliasForReferencesSelf.class.getName() + "] points to itself. "
//						+ "Specify 'annotation' to point to a same-named attribute on a "
//						+ "meta-annotation.");
//		getMappings(AliasForReferencesSelf.class);
//	}
//
//	@Test
//	public void createWhenAliasForValueAndAttributeArePresentThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage(
//				"In @AliasFor declared on attribute 'one' in annotation ["
//						+ AliasForValueAndAttributeArePresent.class.getName()
//						+ "], attribute 'attribute' and its alias 'value' are "
//						+ "present with values of 'two' and 'three', but only "
//						+ "one is permitted.");
//		getMappings(AliasForValueAndAttributeArePresent.class);
//	}
//
//	@Test
//	public void createWhenAliasForExplicitMirrorHasNoDefaultValueThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Misconfigured aliases: attribute 'two' in annotation ["
//				+ AliasForExplicitMirrorHasNoDefaultValue.class.getName()
//				+ "] and attribute 'one' in annotation ["
//				+ AliasForExplicitMirrorHasNoDefaultValue.class.getName()
//				+ "] must declare default values.");
//		getMappings(AliasForExplicitMirrorHasNoDefaultValue.class);
//	}
//
//	@Test
//	public void createWhenAliasForExplicitMirrorHasDifferentDefaultValuesThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Misconfigured aliases: attribute 'two' in annotation ["
//				+ AliasForExplicitMirrorHasDifferentDefaultValues.class.getName()
//				+ "] and attribute 'one' in annotation ["
//				+ AliasForExplicitMirrorHasDifferentDefaultValues.class.getName()
//				+ "] must declare the same default value.");
//		getMappings(AliasForExplicitMirrorHasDifferentDefaultValues.class);
//	}
//
//	@Test
//	public void createWhenAliasForImplicitMirrorHasNoDefaultValueThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Misconfigured aliases: attribute 'two' in annotation ["
//				+ AliasForImplicitMirrorHasNoDefaultValue.class.getName()
//				+ "] and attribute 'one' in annotation ["
//				+ AliasForImplicitMirrorHasNoDefaultValue.class.getName()
//				+ "] must declare default values.");
//		getMappings(AliasForImplicitMirrorHasNoDefaultValue.class);
//	}
//
//	@Test
//	public void createWhenAliasForImplicitMirrorHasDifferentDefaultValuesThrowsException() {
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Misconfigured aliases: attribute 'two' in annotation ["
//				+ AliasForImplicitMirrorHasDifferentDefaultValues.class.getName()
//				+ "] and attribute 'one' in annotation ["
//				+ AliasForImplicitMirrorHasDifferentDefaultValues.class.getName()
//				+ "] must declare the same default value.");
//		getMappings(AliasForImplicitMirrorHasDifferentDefaultValues.class);
//	}
//
//	@Test
//	public void mapAttributeWhenHasDuplicateExplicitMirrorAttributesThrowsException() {
//		AnnotationType type = resolve(AliasForExplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("one", "1", "two", "2"));
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Different @AliasFor mirror values for annotation ["
//				+ AliasForExplicitMirror.class.getName()
//				+ "], attribute 'one' and its alias 'two' are declared with values of [1] and [2]");
//		mappings.getMapping(type.getClassName()).map(annotation, false);
//	}
//
//	@Test
//	public void getMappingWhenHasDuplicateImplicitMirrorAttributesThrowsException() {
//		AnnotationType type = resolve(AliasForImplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("one", "1", "two", "2"));
//		this.thrown.expect(AnnotationConfigurationException.class);
//		this.thrown.expectMessage("Different @AliasFor mirror values for annotation ["
//				+ AliasForImplicitMirror.class.getName()
//				+ "], attribute 'one' and its alias 'two' are declared with values of [1] and [2]");
//		mappings.getMapping(type.getClassName()).map(annotation, false);
//	}
//
//	@Test
//	public void getMappingWhenHasExplicitMirrorAttributesMapsEachMirror() {
//		AnnotationType type = resolve(AliasForExplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotationOne = createMappable(type,
//				DeclaredAttributes.of("one", "1"));
//		MergedAnnotation<?> mappedOne = mappings.getMapping(type.getClassName()).map(
//				annotationOne, false);
//		assertThat(mappedOne.getString("one")).isEqualTo("1");
//		assertThat(mappedOne.getString("two")).isEqualTo("1");
//		MappableAnnotation annotationTwo = createMappable(type,
//				DeclaredAttributes.of("two", "2"));
//		MergedAnnotation<?> mappedTwo = mappings.getMapping(type.getClassName()).map(
//				annotationTwo, false);
//		assertThat(mappedTwo.getString("one")).isEqualTo("2");
//		assertThat(mappedTwo.getString("two")).isEqualTo("2");
//	}
//
//	@Test
//	public void getMappingWhenHasExplicitMirrorAttributesWithDefaultMapsEachMirror() {
//		AnnotationType type = resolve(AliasForExplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotationOne = createMappable(type,
//				DeclaredAttributes.of("one", "1", "two", ""));
//		MergedAnnotation<?> mappedOne = mappings.getMapping(type.getClassName()).map(
//				annotationOne, false);
//		assertThat(mappedOne.getString("one")).isEqualTo("1");
//		assertThat(mappedOne.getString("two")).isEqualTo("1");
//		MappableAnnotation annotationTwo = createMappable(type,
//				DeclaredAttributes.of("one", "", "two", "2"));
//		MergedAnnotation<?> mappedTwo = mappings.getMapping(type.getClassName()).map(
//				annotationTwo, false);
//		assertThat(mappedTwo.getString("one")).isEqualTo("2");
//		assertThat(mappedTwo.getString("two")).isEqualTo("2");
//	}
//
//	@Test
//	public void getMappingWhenHasAliasForImplicitValueMirrorWithDifferentAttributeAndAnnotationValuesMapsAttributes() {
//		AnnotationType type = resolve(AliasForImplicitValueMirrorWithDifferentAttributeAndAnnotationValues.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("value", "on-attribute"));
//		MergedAnnotation<Annotation> mapped = mappings.getMapping(
//				AliasForExplicitValueMirror.class.getName()).map(annotation, false);
//		// Should not get attribute since convention restricted and not an explicit alias
//		assertThat(mapped.getString("value")).isEqualTo("on-annotation");
//		assertThat(mapped.getString("alais")).isEqualTo("on-annotation");
//	}
//
//	@Test
//	public void getMappingWhenHasAliasForExplcitMirrorWithShadowedAttributeMapsAttributes() {
//		// See SPR-14069 and commit d22480b0ebc9bb65fd297c69a44ef963661acca5
//		AnnotationType type = resolve(AliasForExplcitMirrorWithShadowedAttribute.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("shadow", "on-attribute"));
//		MergedAnnotation<Annotation> mapped = mappings.getMapping(
//				AliasForExplicitValueMirror.class.getName()).map(annotation, false);
//		assertThat(mapped.getString("value")).isEqualTo("on-attribute");
//		assertThat(mapped.getString("alais")).isEqualTo("on-attribute");
//	}
//
//	@Test
//	public void getMappingWhenHasMissingValueShouldReturnNull() {
//		AnnotationType type = resolve(Example.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type, DeclaredAttributes.of());
//		MergedAnnotation<?> mapped = mappings.getMapping(type.getClassName()).map(
//				annotation, false);
//		assertThat(mapped.getString("value")).isEmpty();
//	}
//
//	@Test
//	public void getMappingWhenHasImplicitMirrorAttributesMapsEachMirror() {
//		AnnotationType type = resolve(AliasForImplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotationOne = createMappable(type,
//				DeclaredAttributes.of("one", "1"));
//		MergedAnnotation<?> mappedOne = mappings.getMapping(type.getClassName()).map(
//				annotationOne, false);
//		assertThat(mappedOne.getString("one")).isEqualTo("1");
//		assertThat(mappedOne.getString("two")).isEqualTo("1");
//		assertThat(mappedOne.getString("three")).isEqualTo("1");
//		MappableAnnotation annotationTwo = createMappable(type,
//				DeclaredAttributes.of("two", "2"));
//		MergedAnnotation<?> mappedTwo = mappings.getMapping(type.getClassName()).map(
//				annotationTwo, false);
//		assertThat(mappedTwo.getString("one")).isEqualTo("2");
//		assertThat(mappedTwo.getString("two")).isEqualTo("2");
//		assertThat(mappedTwo.getString("three")).isEqualTo("2");
//	}
//
//	@Test
//	public void getMappingWhenHasImplicitIndirectMirrorAttributesMapsEachMirror() {
//		AnnotationType type = resolve(AliasForIndirectImplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotationOne = createMappable(type,
//				DeclaredAttributes.of("alpha", "1"));
//		MergedAnnotation<?> mappedOne = mappings.getMapping(type.getClassName()).map(
//				annotationOne, false);
//		assertThat(mappedOne.getString("alpha")).isEqualTo("1");
//		assertThat(mappedOne.getString("beta")).isEqualTo("1");
//		MappableAnnotation annotationTwo = createMappable(type,
//				DeclaredAttributes.of("beta", "2"));
//		MergedAnnotation<?> mappedTwo = mappings.getMapping(type.getClassName()).map(
//				annotationTwo, false);
//		assertThat(mappedTwo.getString("alpha")).isEqualTo("2");
//		assertThat(mappedTwo.getString("beta")).isEqualTo("2");
//	}
//
//	@Test
//	public void getMappingWhenHasImplicitMirrorAttributesMapsParent() {
//		AnnotationType type = resolve(AliasForImplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("one", "1"));
//		MergedAnnotation<?> mapped = mappings.getMapping(Example.class.getName()).map(
//				annotation, false);
//		assertThat(mapped.getString("value")).isEqualTo("1");
//	}
//
//	@Test
//	public void getMappingWhenHasImplicitIndirectMirrorAttributesMapsParents() {
//		AnnotationType type = resolve(AliasForIndirectImplicitMirror.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("alpha", "1"));
//		MergedAnnotation<?> mappedImplicit = mappings.getMapping(
//				AliasForImplicitMirror.class.getName()).map(annotation, false);
//		assertThat(mappedImplicit.getString("one")).isEqualTo("1");
//		assertThat(mappedImplicit.getString("two")).isEqualTo("1");
//		assertThat(mappedImplicit.getString("three")).isEqualTo("1");
//		MergedAnnotation<?> mapped = mappings.getMapping(Example.class.getName()).map(
//				annotation, false);
//		assertThat(mapped.getString("value")).isEqualTo("1");
//	}
//
//	@Test
//	public void getMappingWhenHasImplicitIndirectMirrorToSameAttributeMapsParents() {
//		AnnotationType type = resolve(AliasForIndirectImplicitMirrorToSameAttribute.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("alpha", "1"));
//		MergedAnnotation<?> mappedInitial = mappings.getMapping(
//				AliasForIndirectImplicitMirrorToSameAttribute.class.getName()).map(annotation, false);
//		assertThat(mappedInitial.getString("alpha")).isEqualTo("1");
//		assertThat(mappedInitial.getString("beta")).isEqualTo("1");
//		MergedAnnotation<?> mappedImplicit = mappings.getMapping(
//				AliasForImplicitMirror.class.getName()).map(annotation, false);
//		assertThat(mappedImplicit.getString("one")).isEqualTo("1");
//		assertThat(mappedImplicit.getString("two")).isEqualTo("1");
//		assertThat(mappedImplicit.getString("three")).isEqualTo("1");
//		MergedAnnotation<?> mapped = mappings.getMapping(Example.class.getName()).map(
//				annotation, false);
//		assertThat(mapped.getString("value")).isEqualTo("1");
//	}
//
//	@Test
//	public void getMappingWhenHasDeducedAttributeName() {
//		AnnotationType type = resolve(AliasForDeducedAttributeName.class);
//		AnnotationTypeMappings mappings = getMappings(AliasForDeducedAttributeName.class);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("value", "test"));
//		MergedAnnotation<?> attributes = mappings.getMapping(Example.class.getName()).map(
//				annotation, false);
//		assertThat(attributes.getString("value")).isEqualTo("test");
//	}
//
//	@Test
//	public void getMappingWhenHasConventionAttributesMapsAttributes() {
//		AnnotationType type = resolve(ConventionMapping.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("value", "a", "other", "b"));
//		MergedAnnotation<?> mapped = mappings.getMapping(
//				ConventionMappingMeta.class.getName()).map(annotation, false);
//		assertThat(mapped.getString("value")).isEqualTo("");
//		assertThat(mapped.getString("other")).isEqualTo("b");
//	}
//
//	@Test
//	public void getMappingWhenHasConventionAttributesWithoutValueMapsAttributes() {
//		AnnotationType type = resolve(ConventionMappingWithoutValue.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("other", "b"));
//		MergedAnnotation<?> mapped = mappings.getMapping(
//				ConventionMappingMeta.class.getName()).map(annotation, false);
//		assertThat(mapped.getString("more")).isEqualTo("y");
//		assertThat(mapped.getString("value")).isEqualTo("a");
//		assertThat(mapped.getString("other")).isEqualTo("b");
//	}
//
//	@Test
//	public void getAnnotationsShouldIncludeRepeatedAnnotations() {
//		String type = MultipleAnnotations.class.getName();
//		AnnotationTypeMappings mappings = getMappings(type);
//		Stream<String> annotations = mappings.getAllMappings().map(
//				mapping -> mapping.getAnnotationType().getClassName()).filter(
//						name -> name.equals(RepeatableExample.class.getName()));
//		assertThat(annotations).hasSize(2);
//	}
//
//	@Test
//	public void getAnnotationsShouldReturnAllAnnotations() {
//		String type = MultipleAnnotations.class.getName();
//		AnnotationTypeMappings mappings = getMappings(type);
//		Stream<AnnotationTypeMapping> annotations = mappings.getAllMappings();
//		assertThat(annotations).hasSize(7);
//	}
//
//	@Test
//	public void getMappingAllowAccessToUnmapped() {
//		AnnotationType type = resolve(ConventionMapping.class);
//		AnnotationTypeMappings mappings = getMappings(type);
//		MappableAnnotation annotation = createMappable(type,
//				DeclaredAttributes.of("other", "a"));
//		MergedAnnotation<?> mapped = mappings.getMapping(
//				ConventionMappingMeta.class.getName()).map(annotation, false);
//		assertThat(mapped.getNonMergedAttribute("other", String.class)).contains("x");
//	}
//
//	private MappableAnnotation createMappable(AnnotationType type,
//			DeclaredAttributes attributes) {
//		return new MappableAnnotation(this.resolver,
//				RepeatableContainers.standardRepeatables(), type, attributes);
//	}
//
//	private AnnotationType resolve(Class<?> type) {
//		return this.resolver.resolve(type.getName());
//	}
//
//	private AnnotationTypeMappings getMappings(Class<?> type) {
//		return getMappings(type.getName());
//	}
//
//	private AnnotationTypeMappings getMappings(String type) {
//		return new AnnotationTypeMappings(this.resolver,
//				RepeatableContainers.standardRepeatables(), this.resolver.resolve(type));
//	}
//
//	private AnnotationTypeMappings getMappings(AnnotationType type) {
//		return new AnnotationTypeMappings(this.resolver,
//				RepeatableContainers.standardRepeatables(), type);
//	}

//	@Retention(RetentionPolicy.RUNTIME)
//	@interface Example {
//
//		String value() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface ExampleArray {
//
//		String[] value() default {};
//
//	}
//
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example(value = "meta-annotated")
//	@interface MetaAnnotatedExample {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@MetaAnnotatedExample
//	@interface MetaMetaAnnotationExample {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@MetaAnnotatedExample
//	@Example("meta-override")
//	@interface MetaMetaAnnotatedOverrideExample {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AlaisForExplicitMirrorTargetAttributeDoesNotExist {
//
//		@AliasFor("two")
//		String one() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForAnnotationTargetNotMetaPresent {
//
//		@AliasFor(annotation = Example.class)
//		String one() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface AliasForAnnotationTargetAttributeNotPresent {
//
//		@AliasFor(annotation = Example.class, attribute = "nope")
//		String one();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForExplicitMirrorTargetHasNoAliasForAnnotation {
//
//		@AliasFor("two")
//		String one();
//
//		String two();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForExplicitMirrorTargetHasAliasForDifferentAnnotation {
//
//		@AliasFor("value")
//		String one();
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String value();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForExplicitMirrorReturnTypesDoNotMatch {
//
//		@AliasFor("two")
//		String one();
//
//		@AliasFor("one")
//		int two();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface AliasForReturnTypesDoNotMatch {
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		int one();
//
//	}
//
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@ExampleArray
//	@interface AliasForExplicitTargetArrayToElement {
//
//		@AliasFor(annotation = ExampleArray.class, attribute = "value")
//		String arrayValue();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@ExampleArray
//	@interface AliasForExplicitMirrorTargetArrayToElement {
//
//		@AliasFor("element")
//		String[] array();
//
//		@AliasFor("array")
//		String element();
//
//	}
//
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForReferencesSelf {
//
//		@AliasFor("one")
//		String one();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForValueAndAttributeArePresent {
//
//		@AliasFor(value = "two", attribute = "three")
//		String one();
//
//		@AliasFor("one")
//		int two();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForExplicitMirrorHasNoDefaultValue {
//
//		@AliasFor("two")
//		String one();
//
//		@AliasFor("one")
//		String two();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForExplicitMirrorHasDifferentDefaultValues {
//
//		@AliasFor("two")
//		int[] one() default { 1, 2, 3 };
//
//		@AliasFor("one")
//		int[] two() default { 3, 4, 5 };
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface AliasForImplicitMirrorHasNoDefaultValue {
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String one();
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String two();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface AliasForImplicitMirrorHasDifferentDefaultValues {
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String one() default "1";
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String two() default "2";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForExplicitMirror {
//
//		@AliasFor("two")
//		String one() default "";
//
//		@AliasFor("one")
//		String two() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface AliasForExplicitValueMirror {
//
//		@AliasFor("alais")
//		String value() default "";
//
//		@AliasFor("value")
//		String alais() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@AliasForExplicitValueMirror("on-annotation")
//	@interface AliasForImplicitValueMirrorWithDifferentAttributeAndAnnotationValues {
//
//		String value() default "on-attribute";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@AliasForExplicitValueMirror("on-annotation")
//	@interface AliasForExplcitMirrorWithShadowedAttribute {
//
//		@AliasFor(annotation = AliasForExplicitValueMirror.class, attribute = "alais")
//		String shadow() default "on-attribute";
//
//	}
//
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface AliasForImplicitMirror {
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String one() default "x";
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String two() default "x";
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String three() default "x";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@AliasForImplicitMirror
//	@interface AliasForIndirectImplicitMirror {
//
//		@AliasFor(annotation = AliasForImplicitMirror.class, attribute = "one")
//		String alpha() default "y";
//
//		@AliasFor(annotation = AliasForImplicitMirror.class, attribute = "two")
//		String beta() default "y";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@AliasForImplicitMirror
//	@interface AliasForIndirectImplicitMirrorToSameAttribute {
//
//		@AliasFor(annotation = AliasForImplicitMirror.class, attribute = "one")
//		String alpha() default "y";
//
//		@AliasFor(annotation = AliasForImplicitMirror.class, attribute = "one")
//		String beta() default "y";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface AliasForDeducedAttributeName {
//
//		@AliasFor(annotation = Example.class)
//		String value();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface ConventionMappingMeta {
//
//		String value() default "";
//
//		String other() default "";
//
//		String more() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@ConventionMappingMeta(other = "x")
//	@interface ConventionMapping {
//
//		String value();
//
//		String other();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@ConventionMappingMeta(value = "a", other = "x", more = "y")
//	@interface ConventionMappingWithoutValue {
//
//		String other();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Repeatable(RepeatableExampleContainer.class)
//	@interface RepeatableExample {
//
//		String value();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface RepeatableExampleContainer {
//
//		RepeatableExample[] value();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example("1")
//	@RepeatableExample("2")
//	@RepeatableExample("3")
//	@AliasForIndirectImplicitMirror
//	@interface MultipleAnnotations {
//
//	}
//


//	@Test
//	public void isAnnotationPresent() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.isPresent(Other.class)).isTrue();
//	}
//
//	@Test
//	public void isMetaAnnotationPresent() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.isPresent(Example.class)).isTrue();
//	}
//
//	@Test
//	public void getDirectlyDeclaredAnnotationNames() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		Stream<MergedAnnotation<?>> result = annotations.stream().filter(
//				MergedAnnotation::isDirectlyPresent);
//		assertThat(result.map(MergedAnnotation::getType)).containsExactly(
//				MetaExample.class.getName(), Other.class.getName());
//	}
//
//	@Test
//	@Ignore
//	public void getAllMetaAnnotationsOnADirectlyDeclaredAnnotation() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		MergedAnnotation<MetaExample> annotation = annotations.get(MetaExample.class);
//		Stream<MergedAnnotation<?>> result = annotations.stream().filter(
//				annotation::isParentOf);
//		assertThat(result.map(MergedAnnotation::getType)).containsExactly(
//				Example.class.getName());
//		// FIXME
//	}
//
//	@Test
//	public void hasDirectAnnitation() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(MetaExample.class).isDirectlyPresent()).isTrue();
//		assertThat(annotations.get(Example.class).isDirectlyPresent()).isFalse();
//		assertThat(annotations.get(Missing.class).isDirectlyPresent()).isFalse();
//	}
//
//	@Test
//	public void hasDirectOrMetaAnnotation() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.isPresent(MetaExample.class)).isTrue();
//		assertThat(annotations.isPresent(Example.class)).isTrue();
//		assertThat(annotations.isPresent(Missing.class)).isFalse();
//	}
//
//	@Test
//	public void hasMetaAnnotationOnly() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(MetaExample.class).isMetaPresent()).isFalse();
//		assertThat(annotations.get(Example.class).isMetaPresent()).isTrue();
//		assertThat(annotations.get(Missing.class).isMetaPresent()).isFalse();
//	}
//
//	@Test
//	public void getMapWithClassesAsStrings() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		Map<String, Object> map = annotations.get(Example.class).asMap(
//				MapValues.CLASS_TO_STRING);
//		// FIXME assert
//	}
//
//	@Test
//	public void getMultiValueMap() {
//		MultiValueMap<String, Object> multiMap = new LinkedMultiValueMap<>();
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndExample.class);
//		annotations.stream(Example.class).map(
//				annotation -> annotation.asMap(MapValues.CLASS_TO_STRING)).forEach(
//						map -> map.forEach(multiMap::add));
//		// FIXME assert
//		// FIXME do we want a collector to help with this?
//		// annotations.stream("annotationType").collect(AnnotationCollectors.toMultiValueMap());
//	}
//
//	@Test
//	public void getAnAttributeValueThatMustExist() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndExample.class);
//		assertThat(annotations.get(Example.class).getString("value")).isEqualTo("b");
//	}
//
//	@Test
//	public void getAnAttributeValueThatMightNotExist() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndExample.class);
//		assertThat(annotations.get(Example.class).getAttribute("value",
//				String.class)).contains("b");
//		assertThat(
//				annotations.get("Missing").getAttribute("value", String.class)).isEmpty();
//	}
//
//	@Test
//	public void testIfAnAnnotationHasDefaultValue() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(Other.class).hasDefaultValue("value")).isTrue();
//	}
//
//	@Test
//	public void filterDefaultValues() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(Other.class).filterDefaultValues().getAttribute(
//				"value", String.class).orElse("-")).isEqualTo("-");
//	}
//
//	@Test
//	public void getAttributeValueWithAnWithExplicitAlaisMirror() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithExplicitAlaisMirror.class);
//		MergedAnnotation<?> annotation = annotations.get(ExplicitAlaisMirror.class);
//		assertThat(annotation.getString("one")).isEqualTo("a");
//		assertThat(annotation.getString("two")).isEqualTo("a");
//	}
//
//	@Test
//	public void getNestedAttributeValueWithAnWithExplicitAlaisMirror() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithNestedExplicitAlaisMirror.class);
//		MergedAnnotation<?> annotation = annotations.get(NestedExplicitAlaisMirror.class);
//		MergedAnnotation<?> nested = annotation.getAnnotation("mirror",
//				ExplicitAlaisMirror.class);
//		MergedAnnotation<?>[] nestedArray = annotation.getAnnotationArray("mirrors",
//				ExplicitAlaisMirror.class);
//		assertThat(nested.getString("one")).isEqualTo("b");
//		assertThat(nested.getString("two")).isEqualTo("b");
//		assertThat(nestedArray[0].getString("one")).isEqualTo("c");
//		assertThat(nestedArray[0].getString("two")).isEqualTo("c");
//	}
//
//	@Test
//	public void toStringMatches() {
//		String expected = WithNestedExplicitAlaisMirror.class.getDeclaredAnnotation(
//				NestedExplicitAlaisMirror.class).toString();
//		expected = expected.replace("two=x, one=b", "one=b, two=b");
//		expected = expected.replace("two=c, one=x", "one=c, two=c");
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithNestedExplicitAlaisMirror.class);
//		MergedAnnotation<?> annotation = annotations.get(NestedExplicitAlaisMirror.class);
//		assertThat(annotation.toString()).isEqualTo(expected);
//	}
//
//	@Test
//	public void getFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedAnnotations() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndInheritedExample.class);
//		MergedAnnotation<Example> annotation = annotations.get(Example.class);
//		assertThat(annotation.getString("value")).isEqualTo("b");
//	}
//
//	@Test
//	public void selfAnnotated() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				SelfAnnotatedExample.class);
//		MergedAnnotation<SelfAnnotatedExample> annotation = annotations.get(
//				SelfAnnotatedExample.class);
//		assertThat(annotation.getString("value")).isEqualTo("test");
//		assertThat(annotations.stream(SelfAnnotatedExample.class)).hasSize(1);
//	}
//
//	//
//	// // AnnotatedElementUtils.forAnnotations
//	//
//	// // Annotations.of(Annotation...)
//	//
//	// // AnnotatedElementUtils.getMetaAnnotationTypes
//	//
//	// //
//	// Annotations.from(element).stream().map(Annotation::getType).collect(..)
//	//
//	// // AnnotatedElementUtils.hasMetaAnnotationTypes
//	//
//	// //
//	// Annotations.from(element).stream("annotationType").anyMatch(Annotation::isMetaAnnotation);
//	//
//	// // AnnotatedElementUtils.isAnnotated
//	//
//	// // annotations.get("annotationType").isPresent();
//	//
//	// // AnnotatedElementUtils.getMergedAnnotationAttributes
//	//
//	// // annotation = annotations.get("annotationType");
//	// // return (annotation.isPresent ? annotation.asMap(synth+asclass) :
//	// // null;
//	// // (asMap will have different options depending on flavor of call).
//	//
//	// // AnnotatedElementUtils.getMergedAnnotation(
//	//
//	// // annotation.get("type").synthesize(); // FIXME if present
//	//
//	// // AnnotatedElementUtils.getAllMergedAnnotations
//	//
//	// // FIXME filter out repeatable
//	// // annotation.stream("type").map(Annotation::synthesize).collect(toSet);
//	//
//	// // FIXME merge ?
//	// // annotation.stream("type").map(Annotation::synthesize).collect(toSet);
//	//
//	// // getAllAnnotationAttributes (as above)
//	//
//	// // AnnotationElementUtils.hasAnnotation
//	//
//	// // Annotations.from(element,
//	// // SearchStrategy.FULL).getAnnotation("").isPresent();
//	//
//	// // AnnotationElementUtils.findMergedAnnotationAttributes
//	//
//	// // FIXME merge ?
//	// // Annotations.from(element,
//	// // SearchStrategy.FULL).get("type").asMap(...);
//	//
//	// // findAllMergedAnnotations
//	//
//	// // FIXME filter out repeatable
//	// // Annotations.from(element, SearchStrategy.FULL)
//	// // annotation.stream("type").map(Annotation::synthesize).collect(toSet);
//	//
//	// // AnnotationUtils.isAnnotationInherited
//	// // Annotations.from(element, SearchStrategy.INHERITED).get("type");
//	// // isDirectlyDeclared && !isInherited
//	//
//	// // AnnotationUtils.isAnnotationMetaPresent
//	// // Annotation.of(annotation).isMetaAnnotation();
//	//
//	// }
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface Examples {
//
//		Example[] value();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Repeatable(Examples.class)
//	@interface Example {
//
//		String value() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface Missing {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface Other {
//
//		String value() default "default";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface MetaExample {
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String exampleValue() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example(value = "b")
//	@interface ExampleB {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface ExplicitAlaisMirror {
//
//		@AliasFor("two")
//		String one() default "x";
//
//		@AliasFor("one")
//		String two() default "x";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface NestedExplicitAlaisMirror {
//
//		ExplicitAlaisMirror mirror();
//
//		ExplicitAlaisMirror[] mirrors();
//
//	}
//
//	@MetaExample(exampleValue = "a")
//	@Other
//	static class WithMetaExampleAndOther {
//
//	}
//
//	@MetaExample(exampleValue = "a")
//	@Example("b")
//	static class WithMetaExampleAndExample {
//
//	}
//
//	@ExplicitAlaisMirror(one = "a")
//	static class WithExplicitAlaisMirror {
//
//	}
//
//	@NestedExplicitAlaisMirror(mirror = @ExplicitAlaisMirror(one = "b"), mirrors = @ExplicitAlaisMirror(two = "c"))
//	static class WithNestedExplicitAlaisMirror {
//
//	}
//
//	@Example("a")
//	static class InheritedWithExample {
//
//	}
//
//	@ExampleB
//	static class WithMetaExampleAndInheritedExample extends InheritedWithExample {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface ExampleArray {
//
//		String[] test() default {};
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@ExampleArray
//	@interface MataArrayOverrideWithSingle {
//
//		@AliasFor(annotation = ExampleArray.class, attribute = "test")
//		String test() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@SelfAnnotatedExample("test")
//	@interface SelfAnnotatedExample {
//
//		String value() default "";
//
//	}
//
//	// FIXME MapAnnotationAttributeExtractor.java#L136
//
//	// FIXME test repeated

	// FIXME test default value of meta-annotation
//			AnnotatedElementUtils.findMergedAnnotationAttributes(
//				MetaMetaConfigDefaultsTests.class, ContextConfiguration.class.getName(),
//				false, false);


}
