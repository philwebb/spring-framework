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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSet;
import org.springframework.core.annotation.AnnotationTypeMapping.Reference;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.AttributeTypes;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AnnotationTypeMapping}.
 *
 * @author Phillip Webb
 */
public class AnnotationTypeMappingTests {

	private final ClassLoader classLoader = getClass().getClassLoader();

	private final RepeatableContainers repeatableContainers = RepeatableContainers.standardRepeatables();

	private AnnotationFilter annotationFilter = AnnotationFilter.PLAIN;

	private final AttributeType componentNameAttribute = AttributeType.of("componentName",
			"java.lang.String", DeclaredAnnotations.NONE, "");

	private final AttributeType componentTextAttribute = AttributeType.of("componentText",
			"java.lang.String", DeclaredAnnotations.NONE, "");

	private final AttributeType componentNestedAttribute = AttributeType.of(
			"componentNested", Nested.class.getName(), DeclaredAnnotations.NONE, "");

	private final AttributeType componentNestedWithMirrorAttribute = AttributeType.of(
			"componentNested", NestedWithMirror.class.getName(), DeclaredAnnotations.NONE,
			"");

	private final AnnotationType componentType = AnnotationType.of(
			"com.example.Component", DeclaredAnnotations.NONE,
			AttributeTypes.of(this.componentNameAttribute, this.componentTextAttribute,
					this.componentNestedAttribute));

	private final AnnotationTypeMapping componentMapping = new AnnotationTypeMapping(
			this.classLoader, this.repeatableContainers, this.annotationFilter,
			this.componentType);

	private final AttributeType serviceNameAttribute = AttributeType.of("serviceName",
			"java.lang.String", DeclaredAnnotations.NONE, "");

	private final AttributeType serviceTextAttribute = AttributeType.of("serviceText",
			"java.lang.String", DeclaredAnnotations.NONE, "");

	private final AnnotationType serviceType = AnnotationType.of("com.example.Service",
			DeclaredAnnotations.NONE,
			AttributeTypes.of(this.serviceNameAttribute, this.serviceTextAttribute));

	private final AnnotationTypeMapping serviceMapping = new AnnotationTypeMapping(
			this.classLoader, this.repeatableContainers, this.annotationFilter,
			this.serviceType);

	@Test
	public void addAlaisWhenFromDifferentMappingThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> this.componentMapping.addAlias(
				new Reference(this.serviceMapping, this.componentNameAttribute),
				new Reference(this.serviceMapping,
						this.serviceNameAttribute))).withMessage(
								"Invalid mirror mapping reference");
	}

	@Test
	public void addAliasAddsAlais() {
		this.componentMapping.addAlias(
				new Reference(this.componentMapping, this.componentNameAttribute),
				new Reference(this.serviceMapping, this.serviceNameAttribute));
		Reference aliasTo = this.componentMapping.getAliases().get("componentName");
		assertThat(this.componentMapping.getAliases()).hasSize(1);
		assertThat(aliasTo.getMapping()).isEqualTo(this.serviceMapping);
		assertThat(aliasTo.getAttribute()).isEqualTo(this.serviceNameAttribute);
	}

	@Test
	public void addAliasToMappingAndAttributeNameAddsAlais() {
		this.componentMapping.addAlias("componentName", this.serviceMapping,
				"serviceName");
		Reference aliasTo = this.componentMapping.getAliases().get("componentName");
		assertThat(this.componentMapping.getAliases()).hasSize(1);
		assertThat(aliasTo.getMapping()).isEqualTo(this.serviceMapping);
		assertThat(aliasTo.getAttribute()).isEqualTo(this.serviceNameAttribute);
	}

	@Test
	public void allAliasWhenAlreadyHasAliasDoesNotAddAlias() {
		this.componentMapping.addAlias(
				new Reference(this.componentMapping, this.componentNameAttribute),
				new Reference(this.serviceMapping, this.serviceNameAttribute));
		this.componentMapping.addAlias(
				new Reference(this.componentMapping, this.componentNameAttribute),
				new Reference(this.serviceMapping, AttributeType.of("anotherAttribute",
						"java.lang.String", DeclaredAnnotations.NONE, null)));
		Reference aliasTo = this.componentMapping.getAliases().get("componentName");
		assertThat(this.componentMapping.getAliases()).hasSize(1);
		assertThat(aliasTo.getMapping()).isEqualTo(this.serviceMapping);
		assertThat(aliasTo.getAttribute()).isEqualTo(this.serviceNameAttribute);
	}

	@Test
	public void addMirrorSetFromNamesAddMirrorSet() {
		this.componentMapping.addMirrorSet("componentName", "componentText");
		assertThat(this.componentMapping.getMirrorSets()).hasSize(1);
		MirrorSet mirrorSet = this.componentMapping.getMirrorSets().get(0);
		List<Reference> mirrors = toList(mirrorSet);
		assertThat(mirrors).hasSize(2);
		assertThat(mirrors.get(0).getMapping()).isEqualTo(this.componentMapping);
		assertThat(mirrors.get(0).getAttribute()).isEqualTo(this.componentNameAttribute);
		assertThat(mirrors.get(1).getMapping()).isEqualTo(this.componentMapping);
		assertThat(mirrors.get(1).getAttribute()).isEqualTo(this.componentTextAttribute);
	}

	@Test
	public void addMirrorSetFromNamesWhenMissingAttributeThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.componentMapping.addMirrorSet("componentName",
						"componentMissing")).withMessage(
								"Missing attribute componentMissing");
	}

	@Test
	public void addMirrorSetWhenFromDifferentMappingThrowsException() {
		List<Reference> references = Arrays.asList(
				new Reference(this.componentMapping, this.componentNameAttribute),
				new Reference(this.serviceMapping, this.serviceNameAttribute));
		MirrorSet mirrorSet = new MirrorSet(references);
		assertThatIllegalStateException().isThrownBy(
				() -> this.componentMapping.addMirrorSet(mirrorSet)).withMessage(
						"Invalid mirror mapping reference");
	}

	@Test
	public void addMirrorSetWhenExistingAliasToSelfRemovesAlaias() {
		this.componentMapping.addAlias("componentName", this.componentMapping,
				"componentText");
		assertThat(this.componentMapping.getAliases()).isNotEmpty();
		this.componentMapping.addMirrorSet("componentName", "componentText");
		assertThat(this.componentMapping.getAliases()).isEmpty();
	}

	@Test
	public void getClassLoaderReturnsClassLoader() {
		assertThat(this.componentMapping.getClassLoader()).isEqualTo(
				getClass().getClassLoader());
	}

	@Test
	public void getParentReturnsParent() {
		AnnotationTypeMapping child = new AnnotationTypeMapping(
				getClass().getClassLoader(), this.repeatableContainers,
				this.annotationFilter, this.componentMapping, this.serviceType,
				DeclaredAttributes.NONE);
		assertThat(child.getParent()).isEqualTo(this.componentMapping);
	}

	@Test
	public void getAnnotationAttributesReturnsAnnotationAttributes() {
		DeclaredAttributes attributes = DeclaredAttributes.of("componentName", "test");
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(this.classLoader,
				this.repeatableContainers, this.annotationFilter, null,
				this.componentType, attributes);
		assertThat(mapping.getAnnotationAttributes()).isEqualTo(attributes);
	}

	@Test
	public void getAliasesReturnsAlaises() {
		assertThat(this.componentMapping.getAliases()).isEmpty();
		this.componentMapping.addAlias("componentName", this.serviceMapping,
				"serviceName");
		assertThat(this.componentMapping.getAliases()).isNotEmpty();
	}

	@Test
	public void getMirrorSetsReturnsMirrorSets() {
		assertThat(this.componentMapping.getMirrorSets()).isEmpty();
		this.componentMapping.addMirrorSet("componentName", "componentText");
		assertThat(this.componentMapping.getMirrorSets()).isNotEmpty();
	}

	@Test
	public void addMirrorSetWhenHasNoDefaultValueThrowsException() {
		AnnotationType type = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE,
				AttributeTypes.of(this.componentNameAttribute, AttributeType.of("test",
						"java.lang.String", DeclaredAnnotations.NONE, null)));
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(this.classLoader,
				this.repeatableContainers, this.annotationFilter, type);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> mapping.addMirrorSet("componentName", "test")).withMessage(
						"Misconfigured aliases: attribute 'test' in annotation "
								+ "[com.example.Component] and attribute 'componentName' "
								+ "in annotation [com.example.Component] must declare "
								+ "default values.");
	}

	@Test
	public void addMirrorSetWhenHasDifferentDefaultValuesThrowsException() {
		AnnotationType type = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE,
				AttributeTypes.of(this.componentNameAttribute, AttributeType.of("test",
						"java.lang.String", DeclaredAnnotations.NONE, "different")));
		AnnotationTypeMapping mapping = new AnnotationTypeMapping(this.classLoader,
				this.repeatableContainers, this.annotationFilter, type);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> mapping.addMirrorSet("componentName", "test")).withMessage(
						"Misconfigured aliases: attribute 'test' in annotation "
								+ "[com.example.Component] and attribute 'componentName' in annotation "
								+ "[com.example.Component] must declare the same default value.");
	}

	@Test
	public void addMirrorWhenJustOneItemThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.componentMapping.addMirrorSet("componentName")).withMessage(
						"Mirrors must contain more than one reference");
	}

	@Test
	public void referenceGetMappingReturnsMapping() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		assertThat(reference.getMapping()).isEqualTo(this.componentMapping);
	}

	@Test
	public void referenceGetAttributeTypeReturnsAttributeType() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		assertThat(reference.getAttribute()).isEqualTo(this.componentNameAttribute);
	}

	@Test
	public void referenceIsForSameAnnotationWhenForSameAnnotationReturnsTrue() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		AnnotationTypeMapping sameType = new AnnotationTypeMapping(
				getClass().getClassLoader(), this.repeatableContainers,
				this.annotationFilter, AnnotationType.of("com.example.Component",
						DeclaredAnnotations.NONE, AttributeTypes.NONE));
		Reference other = new Reference(sameType, this.componentNameAttribute);
		assertThat(reference.isForSameAnnotation(other)).isTrue();
	}

	@Test
	public void referenceIsForSameAnnotationWhenForDifferentAnnotationReturnsFalse() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		Reference other = new Reference(this.serviceMapping, this.serviceNameAttribute);
		assertThat(reference.isForSameAnnotation(other)).isFalse();
	}

	@Test
	public void referenceIsForSameAnnotationWhenReferenceIsNullReturnsFalse() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		assertThat(reference.isForSameAnnotation(null)).isFalse();
	}

	@Test
	public void referenceToStringReturnsString() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		assertThat(reference.toString()).isEqualTo(
				"attribute 'componentName' in annotation [com.example.Component]");
	}

	@Test
	public void referenceToCapitalizedStringReturnsString() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		assertThat(reference.toCapitalizedString()).isEqualTo(
				"Attribute 'componentName' in annotation [com.example.Component]");
	}

	@Test
	public void referenceEqualsAndHashCode() {
		Reference reference = new Reference(this.componentMapping,
				this.componentNameAttribute);
		AnnotationTypeMapping sameType = new AnnotationTypeMapping(
				getClass().getClassLoader(), this.repeatableContainers,
				this.annotationFilter, AnnotationType.of("com.example.Component",
						DeclaredAnnotations.NONE, AttributeTypes.NONE));
		Reference other = new Reference(sameType, this.componentNameAttribute);
		assertThat(reference.hashCode()).isEqualTo(other.hashCode());
		assertThat(reference).isEqualTo(other);
		assertThat(reference).isNotEqualTo(
				new Reference(this.componentMapping, this.componentTextAttribute));
		assertThat(reference).isNotEqualTo(
				new Reference(this.serviceMapping, this.serviceNameAttribute));
	}

	@Test
	public void getNestedReturnsNestedMapping() {
		AnnotationTypeMapping nested = this.componentMapping.getNested(
				Nested.class.getName());
		assertThat(nested.getAnnotationType().getClassName()).isEqualTo(
				Nested.class.getName());
	}

	@Test
	public void canSkipSynthesizeWhenHasMirrorSetReturnsFalse() {
		this.componentMapping.addMirrorSet("componentName", "componentText");
		assertThat(this.componentMapping.canSkipSynthesize()).isFalse();
	}

	@Test
	public void canSkipSynthesizeWhenHasAliasesReturnsFalse() {
		this.componentMapping.addAlias("componentName", this.serviceMapping,
				"serviceName");
		assertThat(this.componentMapping.canSkipSynthesize()).isFalse();
	}

	@Test
	public void canSkipSynthesizeWhenHasNestedThatCantBeSkippedReturnsTrue() {
		AnnotationType componentType = AnnotationType.of("com.example.Component",
				DeclaredAnnotations.NONE,
				AttributeTypes.of(this.componentNameAttribute,
						this.componentTextAttribute,
						this.componentNestedWithMirrorAttribute));
		AnnotationTypeMapping componentMapping = new AnnotationTypeMapping(
				this.classLoader, this.repeatableContainers, this.annotationFilter,
				componentType);
		assertThat(componentMapping.canSkipSynthesize()).isFalse();
	}

	@Test
	public void canSkipSynthesizeWhenHasNoMirrosOrAliasesOrNestedReturnsTrue() {
		assertThat(this.componentMapping.canSkipSynthesize()).isTrue();
	}

	private List<Reference> toList(MirrorSet mirrorSet) {
		List<Reference> list = new ArrayList<>();
		mirrorSet.iterator().forEachRemaining(list::add);
		return list;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Nested {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedWithMirror {

		@AliasFor("two")
		String one() default "";

		@AliasFor("one")
		String two() default "";

	}

}
