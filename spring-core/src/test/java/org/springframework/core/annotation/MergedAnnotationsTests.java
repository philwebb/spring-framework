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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.subpackage.NonPublicAnnotatedClass;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link MergedAnnotations} and {@link MergedAnnotation}. These tests cover
 * common usage scenarios and were mainly ported from the original {@code AnnotationUtils}
 * and {@code AnnotatedElementUtils} tests.
 *
 * @author Phillip Webb
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Oleg Zhurakousky
 * @author Rossen Stoyanchev
 * @see MergedAnnotationsRepeatableAnnotationTests
 * @see MergedAnnotationClassLoaderTests
 */
public class MergedAnnotationsTests {

	@Test
	public void getMetaAnnotationTypesOnNonAnnotatedClass() {
		assertThat(MergedAnnotations.from(NonAnnotatedClass.class).stream(
				TransactionalComponent.class)).isEmpty();
	}

	@Test
	public void getMetaAnnotationTypesOnClassWithMetaDepth1() {
		Stream<String> names = MergedAnnotations.from(
				TransactionalComponent.class).stream().map(MergedAnnotation::getType);
		assertThat(names).containsExactly(Transactional.class.getName(),
				Component.class.getName(), Indexed.class.getName());
	}

	@Test
	public void getMetaAnnotationTypesOnClassWithMetaDepth2() {
		Stream<String> names = MergedAnnotations.from(
				ComposedTransactionalComponent.class).stream().map(
						MergedAnnotation::getType);
		assertThat(names).containsExactly(TransactionalComponent.class.getName(),
				Transactional.class.getName(), Component.class.getName(),
				Indexed.class.getName());
	}

	@Test
	public void hasMetaAnnotationTypesOnNonAnnotatedClass() {
		assertThat(MergedAnnotations.from(NonAnnotatedClass.class).isPresent(
				Transactional.class)).isFalse();
	}

	@Test
	public void hasMetaAnnotationTypesOnClassWithMetaDepth0() {
		MergedAnnotations annotations = MergedAnnotations.from(
				TransactionalComponent.class);
		assertThat(annotations.isPresent(TransactionalComponent.class)).isFalse();
	}

	@Test
	public void hasMetaAnnotationTypesOnClassWithMetaDepth1() {
		MergedAnnotations annotations = MergedAnnotations.from(
				TransactionalComponent.class);
		assertThat(annotations.isPresent(Transactional.class)).isTrue();
		assertThat(annotations.isPresent(Component.class)).isTrue();
	}

	@Test
	public void hasMetaAnnotationTypesOnClassWithMetaDepth2() {
		MergedAnnotations annotations = MergedAnnotations.from(
				ComposedTransactionalComponent.class);
		assertThat(annotations.isPresent(Transactional.class)).isTrue();
		assertThat(annotations.isPresent(Component.class)).isTrue();
		assertThat(annotations.isPresent(ComposedTransactionalComponent.class)).isFalse();
	}

	@Test
	public void isAnnotatedOnNonAnnotatedClass() {
		assertThat(MergedAnnotations.from(NonAnnotatedClass.class).isPresent(
				Transactional.class)).isFalse();
	}

	@Test
	public void isAnnotatedOnClassWithMetaDepth0() {
		assertThat(MergedAnnotations.from(TransactionalComponentClass.class).isPresent(
				TransactionalComponent.class)).isTrue();
	}

	@Test
	public void isAnnotatedOnSubclassWithMetaDepth0() {
		// Direct only, no subclass search
		assertThat(MergedAnnotations.from(SubTransactionalComponentClass.class).isPresent(
				TransactionalComponent.class)).isFalse();
	}

	@Test
	public void isAnnotatedOnClassWithMetaDepth1() {
		MergedAnnotations annotations = MergedAnnotations.from(
				TransactionalComponentClass.class);
		assertThat(annotations.isPresent(Transactional.class)).isTrue();
		assertThat(annotations.isPresent(Component.class)).isTrue();
	}

	@Test
	public void isAnnotatedOnClassWithMetaDepth2() {
		MergedAnnotations annotations = MergedAnnotations.from(
				ComposedTransactionalComponentClass.class);
		assertThat(annotations.isPresent(Transactional.class)).isTrue();
		assertThat(annotations.isPresent(Component.class)).isTrue();
		assertThat(annotations.isPresent(ComposedTransactionalComponent.class)).isTrue();
	}

	@Test
	public void getAllAnnotationAttributesOnNonAnnotatedClass() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				NonAnnotatedClass.class).stream(Transactional.class).collect(
						MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).isEmpty();
	}

	@Test
	public void getAllAnnotationAttributesOnClassWithLocalAnnotation() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(TxConfig.class).stream(
				Transactional.class).collect(
						MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(entry("value", Arrays.asList("TxConfig")));
	}

	@Test
	public void getAllAnnotationAttributesOnClassWithLocalComposedAnnotationAndInheritedAnnotation() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				SubClassWithInheritedAnnotation.class).stream(
						Transactional.class).collect(
								MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(
				entry("qualifier", Arrays.asList("composed2", "transactionManager")));
	}

	@Test
	public void getAllAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				SubSubClassWithInheritedAnnotation.class).stream(
						Transactional.class).collect(
								MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(entry("qualifier", Arrays.asList("transactionManager")));
	}

	@Test
	public void getAllAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				SubSubClassWithInheritedComposedAnnotation.class).stream(
						Transactional.class).collect(
								MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(entry("qualifier", Arrays.asList("composed1")));
	}

	/**
	 * If the "value" entry contains both "DerivedTxConfig" AND "TxConfig", then the
	 * algorithm is accidentally picking up shadowed annotations of the same type within
	 * the class hierarchy. Such undesirable behavior would cause the logic in
	 * {@link org.springframework.context.annotation.ProfileCondition} to fail.
	 *
	 * @see org.springframework.core.env.EnvironmentSystemIntegrationTests#mostSpecificDerivedClassDrivesEnvironment_withDevEnvAndDerivedDevConfigClass
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS, DerivedTxConfig.class).stream(
						Transactional.class).collect(
								MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(entry("value", Arrays.asList("DerivedTxConfig")));
	}

	/**
	 * Note: this functionality is required by
	 * {@link org.springframework.context.annotation.ProfileCondition}.
	 *
	 * @see org.springframework.core.env.EnvironmentSystemIntegrationTests
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithMultipleComposedAnnotations() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				TxFromMultipleComposedAnnotations.class).stream(
						Transactional.class).collect(
								MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(
				entry("value", Arrays.asList("TxInheritedComposed", "TxComposed")));
	}

	@Test
	public void getMergedAnnotationAttributesOnClassWithLocalAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(TxConfig.class).get(
				Transactional.class);
		assertThat(annotation.getString("value")).isEqualTo("TxConfig");
	}

	@Test
	public void getMergedAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS, DerivedTxConfig.class).get(
						Transactional.class);
		assertThat(annotation.getString("value")).isEqualTo("DerivedTxConfig");
	}

	@Test
	public void getMergedAnnotationAttributesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS, MetaCycleAnnotatedClass.class).get(
						Transactional.class);
		assertThat(annotation.isPresent()).isFalse();
	}

	@Test
	public void getMergedAnnotationAttributesFavorsLocalComposedAnnotationOverInheritedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				SubClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isTrue();
	}

	@Test
	public void getMergedAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				SubSubClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isFalse();
	}

	@Test
	public void getMergedAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				SubSubClassWithInheritedComposedAnnotation.class).get(
						Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isFalse();
	}

	@Test
	public void getMergedAnnotationAttributesFromInterfaceImplementedBySuperclass() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				ConcreteClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isFalse();
	}

	@Test
	public void getMergedAnnotationAttributesOnInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				InheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test
	public void getMergedAnnotationAttributesOnNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				NonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test
	public void getMergedAnnotationAttributesWithConventionBasedComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				ConventionBasedComposedContextConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getStringArray("locations")).containsExactly(
				"explicitDeclaration");
		assertThat(annotation.getStringArray("value")).containsExactly(
				"explicitDeclaration");
	}

	@Test
	public void getMergedAnnotationAttributesWithHalfConventionBasedAndHalfAliasedComposedAnnotation1() {
		// SPR-13554: convention mapping mixed with AlaisFor annotations
		// xmlConfigFiles can be used because it has an AlaisFor annotation
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				HalfConventionBasedAndHalfAliasedComposedContextConfigClassV1.class).get(
						ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).containsExactly(
				"explicitDeclaration");
		assertThat(annotation.getStringArray("value")).containsExactly(
				"explicitDeclaration");
	}

	@Test
	public void getMergedAnnotationAttributesWithHalfConventionBasedAndHalfAliasedComposedAnnotation2() {
		// SPR-13554: convention mapping mixed with AlaisFor annotations
		// locations doesn't apply because it has no AlaisFor annotation
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				HalfConventionBasedAndHalfAliasedComposedContextConfigClassV2.class).get(
						ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).isEmpty();
		assertThat(annotation.getStringArray("value")).isEmpty();
	}

	@Test
	public void getMergedAnnotationAttributesWithAliasedComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				AliasedComposedContextConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
	}

	@Test
	public void getMergedAnnotationAttributesWithAliasedValueComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				AliasedValueComposedContextConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
	}

	@Test
	public void getMergedAnnotationAttributesWithImplicitAliasesInMetaAnnotationOnComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				ComposedImplicitAliasesContextConfigClass.class).get(
						ImplicitAliasesContextConfig.class);
		assertThat(annotation.getStringArray("groovyScripts")).containsExactly("A.xml",
				"B.xml");
		assertThat(annotation.getStringArray("xmlFiles")).containsExactly("A.xml",
				"B.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("A.xml",
				"B.xml");
		assertThat(annotation.getStringArray("value")).containsExactly("A.xml", "B.xml");
	}

	@Test
	public void getMergedAnnotationWithAliasedValueComposedAnnotation() {
		assertGetMergedAnnotation(AliasedValueComposedContextConfigClass.class,
				"test.xml");
	}

	@Test
	public void getMergedAnnotationWithImplicitAliasesForSameAttributeInComposedAnnotation() {
		assertGetMergedAnnotation(ImplicitAliasesContextConfigClass1.class, "foo.xml");
		assertGetMergedAnnotation(ImplicitAliasesContextConfigClass2.class, "bar.xml");
		assertGetMergedAnnotation(ImplicitAliasesContextConfigClass3.class, "baz.xml");
	}

	@Test
	public void getMergedAnnotationWithTransitiveImplicitAliases() {
		assertGetMergedAnnotation(TransitiveImplicitAliasesContextConfigClass.class,
				"test.groovy");
	}

	@Test
	public void getMergedAnnotationWithTransitiveImplicitAliasesWithSingleElementOverridingAnArrayViaAliasFor() {
		assertGetMergedAnnotation(
				SingleLocationTransitiveImplicitAliasesContextConfigClass.class,
				"test.groovy");
	}

	@Test
	public void getMergedAnnotationWithTransitiveImplicitAliasesWithSkippedLevel() {
		assertGetMergedAnnotation(
				TransitiveImplicitAliasesWithSkippedLevelContextConfigClass.class,
				"test.xml");
	}

	@Test
	public void getMergedAnnotationWithTransitiveImplicitAliasesWithSkippedLevelWithSingleElementOverridingAnArrayViaAliasFor() {
		assertGetMergedAnnotation(
				SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfigClass.class,
				"test.xml");
	}

	private void assertGetMergedAnnotation(Class<?> element, String... expected) {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS, element).get(ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).isEqualTo(expected);
		assertThat(annotation.getStringArray("value")).isEqualTo(expected);
		assertThat(annotation.getClassArray("classes")).isEmpty();
	}

	@Test
	public void getMergedAnnotationWithImplicitAliasesInMetaAnnotationOnComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				ComposedImplicitAliasesContextConfigClass.class).get(
						ImplicitAliasesContextConfig.class);
		assertThat(annotation.getStringArray("groovyScripts")).containsExactly("A.xml",
				"B.xml");
		assertThat(annotation.getStringArray("xmlFiles")).containsExactly("A.xml",
				"B.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("A.xml",
				"B.xml");
		assertThat(annotation.getStringArray("value")).containsExactly("A.xml", "B.xml");
	}

	@Test
	public void getMergedAnnotationAttributesWithInvalidConventionBasedComposedAnnotation() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
						InvalidConventionBasedComposedContextConfigClass.class).get(
								ContextConfig.class));
	}

	@Test
	public void getMergedAnnotationAttributesWithShadowedAliasComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				ShadowedAliasComposedContextConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
		assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
	}

	@Test
	public void findMergedAnnotationAttributesOnInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				InheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubInheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubSubInheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findMergedAnnotationAttributesOnNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				NonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubSubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromInterfaceMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
				"handleFromInterface");
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				method).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromAbstractMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handle");
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				method).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromBridgedMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
				"handleParameterized", String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				method).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesFromBridgeMethod() {
		List<Method> methods = new ArrayList<>();
		ReflectionUtils.doWithLocalMethods(StringGenericParameter.class,
				method -> "getFor".equals(method.getName()), methods::add);
		Method bridgeMethod = methods.get(0).getReturnType().equals(Object.class)
				? methods.get(0)
				: methods.get(1);
		Method bridgedMethod = methods.get(0).getReturnType().equals(Object.class)
				? methods.get(1)
				: methods.get(0);
		assertThat(bridgeMethod.isBridge()).isTrue();
		assertThat(bridgedMethod.isBridge()).isFalse();
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				bridgeMethod).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findMergedAnnotationAttributesOnClassWithMetaAndLocalTxConfig() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				MetaAndLocalTxConfigClass.class).get(Transactional.class);
		assertThat(annotation.getString("qualifier")).isEqualTo("localTxMgr");
	}

	@Test
	public void findAndSynthesizeAnnotationAttributesOnClassWithAttributeAliasesInTargetAnnotation() {
		MergedAnnotation<AliasedTransactional> mergedAnnotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE, AliasedTransactionalComponentClass.class).get(
						AliasedTransactional.class);
		AliasedTransactional synthesizedAnnotation = mergedAnnotation.synthesize();
		String qualifier = "aliasForQualifier";
		assertThat(mergedAnnotation.getString("value")).isEqualTo(qualifier);
		assertThat(mergedAnnotation.getString("qualifier")).isEqualTo(qualifier);
		assertThat(synthesizedAnnotation.value()).isEqualTo(qualifier);
		assertThat(synthesizedAnnotation.qualifier()).isEqualTo(qualifier);
	}

	@Test
	public void findMergedAnnotationAttributesOnClassWithAttributeAliasInComposedAnnotationAndNestedAnnotationsInTargetAnnotation() {
		MergedAnnotation<?> annotation = assertComponentScanAttributes(
				TestComponentScanClass.class, "com.example.app.test");
		MergedAnnotation<Filter>[] excludeFilters = annotation.getAnnotationArray(
				"excludeFilters", Filter.class);
		assertThat(Arrays.stream(excludeFilters).map(
				filter -> filter.getString("pattern"))).containsExactly("*Test",
						"*Tests");
	}

	@Test
	public void findMergedAnnotationAttributesOnClassWithBothAttributesOfAnAliasPairDeclared() {
		assertComponentScanAttributes(
				ComponentScanWithBasePackagesAndValueAliasClass.class,
				"com.example.app.test");
	}

	@Test
	public void findMergedAnnotationAttributesWithSingleElementOverridingAnArrayViaConvention() {
		assertComponentScanAttributes(
				ConventionBasedSinglePackageComponentScanClass.class,
				"com.example.app.test");
	}

	@Test
	public void findMergedAnnotationAttributesWithSingleElementOverridingAnArrayViaAliasFor() {
		assertComponentScanAttributes(AliasForBasedSinglePackageComponentScanClass.class,
				"com.example.app.test");
	}

	private MergedAnnotation<?> assertComponentScanAttributes(Class<?> element,
			String... expected) {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				element).get(ComponentScan.class);
		assertThat(annotation.getStringArray("value")).containsExactly(expected);
		assertThat(annotation.getStringArray("basePackages")).containsExactly(expected);
		return annotation;
	}

	@Test
	public void findMergedAnnotationForMultipleMetaAnnotationsWithClashingAttributeNames() {
		MergedAnnotations annotations = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				AliasedComposedContextConfigAndTestPropSourceClass.class);
		MergedAnnotation<?> contextConfig = annotations.get(ContextConfig.class);
		assertThat(contextConfig.getStringArray("locations")).containsExactly("test.xml");
		assertThat(contextConfig.getStringArray("value")).containsExactly("test.xml");
		MergedAnnotation<?> testPropSource = annotations.get(TestPropSource.class);
		assertThat(testPropSource.getStringArray("locations")).containsExactly(
				"test.properties");
		assertThat(testPropSource.getStringArray("value")).containsExactly(
				"test.properties");
	}

	@Test
	public void findMergedAnnotationWithLocalAliasesThatConflictWithAttributesInMetaAnnotationByConvention() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SpringAppConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).isEmpty();
		assertThat(annotation.getStringArray("value")).isEmpty();
		assertThat(annotation.getClassArray("classes")).containsExactly(Number.class);
	}

	@Test
	public void findMergedAnnotationWithSingleElementOverridingAnArrayViaConvention()
			throws Exception {
		assertWebMapping(XWebController.class.getMethod("postMappedWithPathAttribute"));
	}

	@Test
	public void findMergedAnnotationWithSingleElementOverridingAnArrayViaAliasFor()
			throws Exception {
		assertWebMapping(XWebController.class.getMethod("getMappedWithValueAttribute"));
		assertWebMapping(XWebController.class.getMethod("getMappedWithPathAttribute"));
	}

	private void assertWebMapping(AnnotatedElement element)
			throws ArrayComparisonFailure {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				element).get(XWebMapping.class);
		assertThat(annotation.getStringArray("value")).containsExactly("/test");
		assertThat(annotation.getStringArray("path")).containsExactly("/test");
	}

	@Test
	public void javaLangAnnotationTypeViaFindMergedAnnotation() throws Exception {
		Constructor<?> deprecatedCtor = Date.class.getConstructor(String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				RepeatableContainers.standardRepeatables(), AnnotationFilter.NONE,
				SearchStrategy.DIRECT, deprecatedCtor).get(Deprecated.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test
	public void javaxAnnotationTypeViaFindMergedAnnotation() throws Exception {
		assertThat(MergedAnnotations.from(ResourceHolder.class).get(
				Resource.class).getString("name")).isEqualTo("x");
	}

	@Test
	public void getAllMergedAnnotationsOnClassWithInterface() throws Exception {
		Method method = TransactionalServiceImpl.class.getMethod("doIt");
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				method).stream(Transactional.class)).isEmpty();
	}

	@Test
	public void findAllMergedAnnotationsOnClassWithInterface() throws Exception {
		Method method = TransactionalServiceImpl.class.getMethod("doIt");
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).stream(
				Transactional.class)).hasSize(1);
	}

	@Test // SPR-16060
	public void findMethodAnnotationFromGenericInterface1() throws Exception {
		// FIXME duped
		Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod(
				"foo", String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				method).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test // SPR-17146
	public void findMethodAnnotationFromGenericSuperclass1() throws Exception {
		// FIXME duped
		Method method = XExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod("foo",
				String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				method).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@SafeVarargs
	static <T> T[] asArray(T... arr) {
		return arr;
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Inherited
	@interface Transactional {

		String value() default "";

		String qualifier() default "transactionManager";

		boolean readOnly() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Inherited
	@interface AliasedTransactional {

		@AliasFor(attribute = "qualifier")
		String value() default "";

		@AliasFor(attribute = "value")
		String qualifier() default "";
	}

	@Transactional(qualifier = "composed1")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	@interface InheritedComposed {
	}

	@Transactional(qualifier = "composed2", readOnly = true)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Composed {
	}

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposedWithOverride {

		String qualifier() default "txMgr";
	}

	@Transactional("TxInheritedComposed")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxInheritedComposed {
	}

	@Transactional("TxComposed")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposed {
	}

	@Transactional
	@Component
	@Retention(RetentionPolicy.RUNTIME)
	@interface TransactionalComponent {
	}

	@TransactionalComponent
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedTransactionalComponent {
	}

	@AliasedTransactional(value = "aliasForQualifier")
	@Component
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedTransactionalComponent {
	}

	@TxComposedWithOverride
	// Override default "txMgr" from @TxComposedWithOverride with "localTxMgr"
	@Transactional(qualifier = "localTxMgr")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MetaAndLocalTxConfig {
	}

	/**
	 * Mock of {@code org.springframework.test.context.TestPropertySource}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestPropSource {

		@AliasFor("locations")
		String[] value() default {};

		@AliasFor("value")
		String[] locations() default {};
	}

	/**
	 * Mock of {@code org.springframework.test.context.ContextConfiguration}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextConfig {

		@AliasFor(attribute = "locations")
		String[] value() default {};

		@AliasFor(attribute = "value")
		String[] locations() default {};

		Class<?>[] classes() default {};
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ConventionBasedComposedContextConfig {

		String[] locations() default {};
	}

	@ContextConfig(value = "duplicateDeclaration")
	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidConventionBasedComposedContextConfig {

		String[] locations();
	}

	/**
	 * This hybrid approach for annotation attribute overrides with transitive implicit
	 * aliases is unsupported. See SPR-13554 for details.
	 */
	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface HalfConventionBasedAndHalfAliasedComposedContextConfig {

		String[] locations() default {};

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xmlConfigFiles() default {};
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xmlConfigFiles();
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedValueComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "value")
		String[] locations();
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ImplicitAliasesContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] groovyScripts() default {};

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xmlFiles() default {};

		// intentionally omitted: attribute = "locations"
		@AliasFor(annotation = ContextConfig.class)
		String[] locations() default {};

		// intentionally omitted: attribute = "locations" (SPR-14069)
		@AliasFor(annotation = ContextConfig.class)
		String[] value() default {};
	}

	@ImplicitAliasesContextConfig(xmlFiles = { "A.xml", "B.xml" })
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedImplicitAliasesContextConfig {
	}

	@ImplicitAliasesContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface TransitiveImplicitAliasesContextConfig {

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "xmlFiles")
		String[] xml() default {};

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
		String[] groovy() default {};
	}

	@ImplicitAliasesContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface SingleLocationTransitiveImplicitAliasesContextConfig {

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "xmlFiles")
		String xml() default "";

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
		String groovy() default "";
	}

	@ImplicitAliasesContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface TransitiveImplicitAliasesWithSkippedLevelContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xml() default {};

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
		String[] groovy() default {};
	}

	@ImplicitAliasesContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String xml() default "";

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
		String groovy() default "";
	}

	/**
	 * Although the configuration declares an explicit value for 'value' and requires a
	 * value for the aliased 'locations', this does not result in an error since
	 * 'locations' effectively <em>shadows</em> the 'value' attribute (which cannot be set
	 * via the composed annotation anyway).
	 *
	 * If 'value' were not shadowed, such a declaration would not make sense.
	 */
	@ContextConfig(value = "duplicateDeclaration")
	@Retention(RetentionPolicy.RUNTIME)
	@interface ShadowedAliasComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xmlConfigFiles();
	}

	@ContextConfig(locations = "shadowed.xml")
	@TestPropSource(locations = "test.properties")
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedComposedContextConfigAndTestPropSource {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xmlConfigFiles() default "default.xml";
	}

	/**
	 * Mock of {@code org.springframework.boot.test.SpringApplicationConfiguration}.
	 */
	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface SpringAppConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] locations() default {};

		@AliasFor("value")
		Class<?>[] classes() default {};

		@AliasFor("classes")
		Class<?>[] value() default {};
	}

	/**
	 * Mock of {@code org.springframework.context.annotation.ComponentScan}
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComponentScan {

		@AliasFor("basePackages")
		String[] value() default {};

		@AliasFor("value")
		String[] basePackages() default {};

		Filter[] excludeFilters() default {};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {

		String pattern();
	}

	@ComponentScan(excludeFilters = { @Filter(pattern = "*Test"),
		@Filter(pattern = "*Tests") })
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestComponentScan {

		@AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
		String[] packages();
	}

	@ComponentScan
	@Retention(RetentionPolicy.RUNTIME)
	@interface ConventionBasedSinglePackageComponentScan {

		String basePackages();
	}

	@ComponentScan
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForBasedSinglePackageComponentScan {

		@AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
		String pkg();
	}

	// -------------------------------------------------------------------------

	static class NonAnnotatedClass {
	}

	@TransactionalComponent
	static class TransactionalComponentClass {
	}

	static class SubTransactionalComponentClass extends TransactionalComponentClass {
	}

	@ComposedTransactionalComponent
	static class ComposedTransactionalComponentClass {
	}

	@AliasedTransactionalComponent
	static class AliasedTransactionalComponentClass {
	}

	@Transactional
	static class ClassWithInheritedAnnotation {
	}

	@Composed
	static class SubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
	}

	static class SubSubClassWithInheritedAnnotation
			extends SubClassWithInheritedAnnotation {
	}

	@InheritedComposed
	static class ClassWithInheritedComposedAnnotation {
	}

	@Composed
	static class SubClassWithInheritedComposedAnnotation
			extends ClassWithInheritedComposedAnnotation {
	}

	static class SubSubClassWithInheritedComposedAnnotation
			extends SubClassWithInheritedComposedAnnotation {
	}

	@MetaAndLocalTxConfig
	static class MetaAndLocalTxConfigClass {
	}

	@Transactional("TxConfig")
	static class TxConfig {
	}

	@Transactional("DerivedTxConfig")
	static class DerivedTxConfig extends TxConfig {
	}

	@TxInheritedComposed
	@TxComposed
	static class TxFromMultipleComposedAnnotations {
	}

	@Transactional
	static interface InterfaceWithInheritedAnnotation {

		@Order
		void handleFromInterface();
	}

	static abstract class AbstractClassWithInheritedAnnotation<T>
			implements InterfaceWithInheritedAnnotation {

		@Transactional
		public abstract void handle();

		@Transactional
		public void handleParameterized(T t) {
		}
	}

	static class ConcreteClassWithInheritedAnnotation
			extends AbstractClassWithInheritedAnnotation<String> {

		@Override
		public void handle() {
		}

		@Override
		public void handleParameterized(String s) {
		}

		@Override
		public void handleFromInterface() {
		}
	}

	public interface GenericParameter<T> {

		T getFor(Class<T> cls);
	}

	@SuppressWarnings("unused")
	private static class StringGenericParameter implements GenericParameter<String> {

		@Order
		@Override
		public String getFor(Class<String> cls) {
			return "foo";
		}

		public String getFor(Integer integer) {
			return "foo";
		}
	}

	@Transactional
	public interface InheritedAnnotationInterface {
	}

	public interface SubInheritedAnnotationInterface
			extends InheritedAnnotationInterface {
	}

	public interface SubSubInheritedAnnotationInterface
			extends SubInheritedAnnotationInterface {
	}

	@Order
	public interface NonInheritedAnnotationInterface {
	}

	public interface SubNonInheritedAnnotationInterface
			extends NonInheritedAnnotationInterface {
	}

	public interface SubSubNonInheritedAnnotationInterface
			extends SubNonInheritedAnnotationInterface {
	}

	@ConventionBasedComposedContextConfig(locations = "explicitDeclaration")
	static class ConventionBasedComposedContextConfigClass {
	}

	@InvalidConventionBasedComposedContextConfig(locations = "requiredLocationsDeclaration")
	static class InvalidConventionBasedComposedContextConfigClass {
	}

	@HalfConventionBasedAndHalfAliasedComposedContextConfig(xmlConfigFiles = "explicitDeclaration")
	static class HalfConventionBasedAndHalfAliasedComposedContextConfigClassV1 {

	}

	@HalfConventionBasedAndHalfAliasedComposedContextConfig(locations = "explicitDeclaration")
	static class HalfConventionBasedAndHalfAliasedComposedContextConfigClassV2 {

	}

	@AliasedComposedContextConfig(xmlConfigFiles = "test.xml")
	static class AliasedComposedContextConfigClass {

	}

	@AliasedValueComposedContextConfig(locations = "test.xml")
	static class AliasedValueComposedContextConfigClass {

	}

	@ImplicitAliasesContextConfig("foo.xml")
	static class ImplicitAliasesContextConfigClass1 {

	}

	@ImplicitAliasesContextConfig(locations = "bar.xml")
	static class ImplicitAliasesContextConfigClass2 {

	}

	@ImplicitAliasesContextConfig(xmlFiles = "baz.xml")
	static class ImplicitAliasesContextConfigClass3 {

	}

	@TransitiveImplicitAliasesContextConfig(groovy = "test.groovy")
	static class TransitiveImplicitAliasesContextConfigClass {

	}

	@SingleLocationTransitiveImplicitAliasesContextConfig(groovy = "test.groovy")
	static class SingleLocationTransitiveImplicitAliasesContextConfigClass {

	}

	@TransitiveImplicitAliasesWithSkippedLevelContextConfig(xml = "test.xml")
	static class TransitiveImplicitAliasesWithSkippedLevelContextConfigClass {

	}

	@SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfig(xml = "test.xml")
	static class SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfigClass {

	}

	@ComposedImplicitAliasesContextConfig
	static class ComposedImplicitAliasesContextConfigClass {

	}

	@ShadowedAliasComposedContextConfig(xmlConfigFiles = "test.xml")
	static class ShadowedAliasComposedContextConfigClass {

	}

	@AliasedComposedContextConfigAndTestPropSource(xmlConfigFiles = "test.xml")
	static class AliasedComposedContextConfigAndTestPropSourceClass {

	}

	@ComponentScan(value = "com.example.app.test", basePackages = "com.example.app.test")
	static class ComponentScanWithBasePackagesAndValueAliasClass {

	}

	@TestComponentScan(packages = "com.example.app.test")
	static class TestComponentScanClass {

	}

	@ConventionBasedSinglePackageComponentScan(basePackages = "com.example.app.test")
	static class ConventionBasedSinglePackageComponentScanClass {

	}

	@AliasForBasedSinglePackageComponentScan(pkg = "com.example.app.test")
	static class AliasForBasedSinglePackageComponentScanClass {

	}

	@SpringAppConfig(Number.class)
	static class SpringAppConfigClass {

	}

	@Resource(name = "x")
	static class ResourceHolder {

	}

	interface TransactionalService {

		@Transactional
		void doIt();

	}

	class TransactionalServiceImpl implements TransactionalService {

		@Override
		public void doIt() {
		}

	}


	///////

	@Before
	public void clearCacheBeforeTests() {
		AnnotationUtils.clearCache();
	}

	@Test
	public void findMethodAnnotationOnLeaf() throws Exception {
		Method method = Leaf.class.getMethod("annotatedOnLeaf");
		assertThat(method.getAnnotation(Order.class)).isNotNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationWithAnnotationOnMethodInInterface() throws Exception {
		Method method = Leaf.class.getMethod("fromInterfaceImplementedByRoot");
		// @Order is not @Inherited
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationWithMetaAnnotationOnLeaf() throws Exception {
		Method method = Leaf.class.getMethod("metaAnnotatedOnLeaf");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(1);
	}

	@Test
	public void findMethodAnnotationWithMetaMetaAnnotationOnLeaf() throws Exception {
		Method method = Leaf.class.getMethod("metaMetaAnnotatedOnLeaf");
		assertThat(method.getAnnotation(Component.class)).isNull();
		assertThat(
				MergedAnnotations.from(method).get(Component.class).getDepth()).isEqualTo(
						2);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Component.class).getDepth()).isEqualTo(2);
	}

	@Test
	public void findMethodAnnotationOnRoot() throws Exception {
		Method method = Leaf.class.getMethod("annotatedOnRoot");
		assertThat(method.getAnnotation(Order.class)).isNotNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationWithMetaAnnotationOnRoot() throws Exception {
		Method method = Leaf.class.getMethod("metaAnnotatedOnRoot");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(1);
	}

	@Test
	public void findMethodAnnotationOnRootButOverridden() throws Exception {
		Method method = Leaf.class.getMethod("overrideWithoutNewAnnotation");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationNotAnnotated() throws Exception {
		Method method = Leaf.class.getMethod("notAnnotated");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(-1);
	}

	@Test
	public void findMethodAnnotationOnBridgeMethod() throws Exception {
		Method method = XSimpleFoo.class.getMethod("something", Object.class);
		assertThat(method.isBridge()).isTrue();
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
		boolean runningInEclipse = Arrays.stream(
				new Exception().getStackTrace()).anyMatch(
						element -> element.getClassName().startsWith("org.eclipse.jdt"));
		// As of JDK 8, invoking getAnnotation() on a bridge method actually finds an
		// annotation on its 'bridged' method [1]; however, the Eclipse compiler will not
		// support this until Eclipse 4.9 [2]. Thus, we effectively ignore the following
		// assertion if the test is currently executing within the Eclipse IDE.
		// [1] https://bugs.openjdk.java.net/browse/JDK-6695379
		// [2] https://bugs.eclipse.org/bugs/show_bug.cgi?id=495396
		if (!runningInEclipse) {
			assertThat(method.getAnnotation(XTransactional.class)).isNotNull();
		}
		assertThat(MergedAnnotations.from(method).get(
				XTransactional.class).getDepth()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				XTransactional.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationOnBridgedMethod() throws Exception {
		Method method = XSimpleFoo.class.getMethod("something", String.class);
		assertThat(method.isBridge()).isFalse();
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(
				-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
		assertThat(method.getAnnotation(XTransactional.class)).isNotNull();
		assertThat(MergedAnnotations.from(method).get(
				XTransactional.class).getDepth()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				XTransactional.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationFromInterface() throws Exception {
		Method method = XImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test // SPR-16060
	public void findMethodAnnotationFromGenericInterface() throws Exception {
		Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod(
				"foo", String.class);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test // SPR-17146
	public void findMethodAnnotationFromGenericSuperclass() throws Exception {
		Method method = XExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod("foo",
				String.class);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationFromInterfaceOnSuper() throws Exception {
		Method method = XSubOfImplementsInterfaceWithAnnotatedMethod.class.getMethod(
				"foo");
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationFromInterfaceWhenSuperDoesNotImplementMethod()
			throws Exception {
		Method method = XSubOfAbstractImplementsInterfaceWithAnnotatedMethod.class.getMethod(
				"foo");
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(
				Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverAnnotationsOnInterfaces() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class).get(
						Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubSubClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isTrue();
	}

	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedComposedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubSubClassWithInheritedMetaAnnotation.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnMetaMetaAnnotatedClass() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				MetaMetaAnnotatedClass.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnMetaMetaMetaAnnotatedClass() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				MetaMetaMetaAnnotatedClass.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// TransactionalClass is NOT annotated or meta-annotated with @Component
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				XTransactionalClass.class).get(Component.class);
		assertThat(annotation.isPresent()).isFalse();
	}

	@Test
	public void findClassAnnotationOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				MetaCycleAnnotatedClass.class).get(Component.class);
		assertThat(annotation.isPresent()).isFalse();
	}

	@Test
	public void findClassAnnotationOnInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				XInheritedAnnotationInterface.class).get(XTransactional.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findClassAnnotationOnSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				XSubInheritedAnnotationInterface.class).get(XTransactional.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findClassAnnotationOnSubSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubSubInheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findClassAnnotationOnNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				XNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findClassAnnotationOnSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				XSubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findClassAnnotationOnSubSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				SubSubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findAnnotationDeclaringClassForAllScenarios() {
		// no class-level annotation
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XNonAnnotatedInterface.class).get(
						XTransactional.class).getSource()).isNull();
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XNonAnnotatedClass.class).get(XTransactional.class).getSource()).isNull();
		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XInheritedAnnotationInterface.class).get(
						XTransactional.class).getSource()).isEqualTo(
								XInheritedAnnotationInterface.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XSubInheritedAnnotationInterface.class).get(
						XTransactional.class).getSource()).isNull();
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XInheritedAnnotationClass.class).get(
						XTransactional.class).getSource()).isEqualTo(
								XInheritedAnnotationClass.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XSubInheritedAnnotationClass.class).get(
						XTransactional.class).getSource()).isEqualTo(
								XInheritedAnnotationClass.class);
		// non-inherited class-level annotation; note: @Order is not inherited,
		// but we should still find it on classes.
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XNonInheritedAnnotationInterface.class).get(
						Order.class).getSource()).isEqualTo(
								XNonInheritedAnnotationInterface.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XSubNonInheritedAnnotationInterface.class).get(
						Order.class).getSource()).isNull();
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XNonInheritedAnnotationClass.class).get(
						Order.class).getSource()).isEqualTo(
								XNonInheritedAnnotationClass.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				XSubNonInheritedAnnotationClass.class).get(
						Order.class).getSource()).isEqualTo(
								XNonInheritedAnnotationClass.class);
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithSingleCandidateType() {
		// no class-level annotation
		List<Class<? extends Annotation>> transactionalCandidateList = Collections.singletonList(
				XTransactional.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(XNonAnnotatedInterface.class,
				transactionalCandidateList)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(XNonAnnotatedClass.class,
				transactionalCandidateList)).isNull();
		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(getDirectlyPresentSourceWithTypeIn(XInheritedAnnotationInterface.class,
				transactionalCandidateList)).isEqualTo(
						XInheritedAnnotationInterface.class);
		assertThat(
				getDirectlyPresentSourceWithTypeIn(XSubInheritedAnnotationInterface.class,
						transactionalCandidateList)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(XInheritedAnnotationClass.class,
				transactionalCandidateList)).isEqualTo(XInheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(XSubInheritedAnnotationClass.class,
				transactionalCandidateList)).isEqualTo(XInheritedAnnotationClass.class);
		// non-inherited class-level annotation; note: @Order is not inherited,
		// but should still find it on classes.
		List<Class<? extends Annotation>> orderCandidateList = Collections.singletonList(
				Order.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XNonInheritedAnnotationInterface.class, orderCandidateList)).isEqualTo(
						XNonInheritedAnnotationInterface.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XSubNonInheritedAnnotationInterface.class, orderCandidateList)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(XNonInheritedAnnotationClass.class,
				orderCandidateList)).isEqualTo(XNonInheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XSubNonInheritedAnnotationClass.class, orderCandidateList)).isEqualTo(
						XNonInheritedAnnotationClass.class);
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithMultipleCandidateTypes() {
		List<Class<? extends Annotation>> candidates = Arrays.asList(XTransactional.class,
				Order.class);
		// no class-level annotation
		assertThat(getDirectlyPresentSourceWithTypeIn(XNonAnnotatedInterface.class,
				candidates)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(XNonAnnotatedClass.class,
				candidates)).isNull();
		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(getDirectlyPresentSourceWithTypeIn(XInheritedAnnotationInterface.class,
				candidates)).isEqualTo(XInheritedAnnotationInterface.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XSubInheritedAnnotationInterface.class, candidates)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(XInheritedAnnotationClass.class,
				candidates)).isEqualTo(XInheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(XSubInheritedAnnotationClass.class,
				candidates)).isEqualTo(XInheritedAnnotationClass.class);
		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClassForTypes() should still find it on
		// classes.
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XNonInheritedAnnotationInterface.class, candidates)).isEqualTo(
						XNonInheritedAnnotationInterface.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XSubNonInheritedAnnotationInterface.class, candidates)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(XNonInheritedAnnotationClass.class,
				candidates)).isEqualTo(XNonInheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XSubNonInheritedAnnotationClass.class, candidates)).isEqualTo(
						XNonInheritedAnnotationClass.class);
		// class hierarchy mixed with @Transactional and @Order declarations
		assertThat(getDirectlyPresentSourceWithTypeIn(XTransactionalClass.class,
				candidates)).isEqualTo(XTransactionalClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(XTransactionalAndOrderedClass.class,
				candidates)).isEqualTo(XTransactionalAndOrderedClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				XSubTransactionalAndOrderedClass.class, candidates)).isEqualTo(
						XTransactionalAndOrderedClass.class);
	}

	private Object getDirectlyPresentSourceWithTypeIn(Class<?> clazz,
			List<Class<? extends Annotation>> annotationTypes) {
		return MergedAnnotations.from(SearchStrategy.SUPER_CLASS, clazz).stream().filter(
				MergedAnnotationPredicates.typeIn(annotationTypes).and(
						MergedAnnotation::isDirectlyPresent)).map(
								MergedAnnotation::getSource).findFirst().orElse(null);
	}

	@Test
	public void isAnnotationDeclaredLocallyForAllScenarios() throws Exception {
		// no class-level annotation
		assertThat(MergedAnnotations.from(XNonAnnotatedInterface.class).get(
				XTransactional.class).isDirectlyPresent()).isFalse();
		assertThat(MergedAnnotations.from(XNonAnnotatedClass.class).get(
				XTransactional.class).isDirectlyPresent()).isFalse();
		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(MergedAnnotations.from(XInheritedAnnotationInterface.class).get(
				XTransactional.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(XSubInheritedAnnotationInterface.class).get(
				XTransactional.class).isDirectlyPresent()).isFalse();
		assertThat(MergedAnnotations.from(XInheritedAnnotationClass.class).get(
				XTransactional.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(XSubInheritedAnnotationClass.class).get(
				XTransactional.class).isDirectlyPresent()).isFalse();
		// non-inherited class-level annotation; note: @Order is not inherited
		assertThat(MergedAnnotations.from(XNonInheritedAnnotationInterface.class).get(
				Order.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(XSubNonInheritedAnnotationInterface.class).get(
				Order.class).isDirectlyPresent()).isFalse();
		assertThat(MergedAnnotations.from(XNonInheritedAnnotationClass.class).get(
				Order.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(XSubNonInheritedAnnotationClass.class).get(
				Order.class).isDirectlyPresent()).isFalse();
	}

	@Test
	public void isAnnotationInheritedForAllScenarios() {
		// no class-level annotation
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XNonAnnotatedInterface.class).get(
						XTransactional.class).getAggregateIndex()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XNonAnnotatedClass.class).get(
						XTransactional.class).getAggregateIndex()).isEqualTo(-1);
		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XInheritedAnnotationInterface.class).get(
						XTransactional.class).getAggregateIndex()).isEqualTo(0);
		// Since we're not traversing interface hierarchies the following, though perhaps
		// counter intuitive, must be false:
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XSubInheritedAnnotationInterface.class).get(
						XTransactional.class).getAggregateIndex()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XInheritedAnnotationClass.class).get(
						XTransactional.class).getAggregateIndex()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XSubInheritedAnnotationClass.class).get(
						XTransactional.class).getAggregateIndex()).isEqualTo(1);
		// non-inherited class-level annotation; note: @Order is not inherited
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XNonInheritedAnnotationInterface.class).get(
						Order.class).getAggregateIndex()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XSubNonInheritedAnnotationInterface.class).get(
						Order.class).getAggregateIndex()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XNonInheritedAnnotationClass.class).get(
						Order.class).getAggregateIndex()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,
				XSubNonInheritedAnnotationClass.class).get(
						Order.class).getAggregateIndex()).isEqualTo(-1);
	}

	@Test
	public void getAnnotationAttributesWithoutAttributeAliases() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(XWebController.class).get(
				Component.class);
		assertThat(annotation.getString("value")).isEqualTo("webController");
	}

	@Test
	public void getAnnotationAttributesWithNestedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				ComponentScanClass.class).get(XComponentScan.class);
		MergedAnnotation<XFilter>[] filters = annotation.getAnnotationArray(
				"excludeFilters", XFilter.class);
		assertThat(Arrays.stream(filters).map(
				filter -> filter.getString("pattern"))).containsExactly("*Foo", "*Bar");
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliases1() throws Exception {
		Method method = XWebController.class.getMethod("handleMappedWithValueAttribute");
		MergedAnnotation<?> annotation = MergedAnnotations.from(method).get(
				XWebMapping.class);
		assertThat(annotation.getString("name")).isEqualTo("foo");
		assertThat(annotation.getStringArray("value")).containsExactly("/test");
		assertThat(annotation.getStringArray("path")).containsExactly("/test");
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliases2() throws Exception {
		Method method = XWebController.class.getMethod("handleMappedWithPathAttribute");
		MergedAnnotation<?> annotation = MergedAnnotations.from(method).get(
				XWebMapping.class);
		assertThat(annotation.getString("name")).isEqualTo("bar");
		assertThat(annotation.getStringArray("value")).containsExactly("/test");
		assertThat(annotation.getStringArray("path")).containsExactly("/test");
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliasesWithDifferentValues()
			throws Exception {
		Method method = XWebController.class.getMethod(
				"handleMappedWithDifferentPathAndValueAttributes");
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotations.from(method).get(
						XWebMapping.class)).withMessageContaining(
								"attribute 'value' and its alias 'path'").withMessageContaining(
										"values of [{/enigma}] and [{/test}]");
	}

	@Test
	public void getValueFromAnnotation() throws Exception {
		Method method = XSimpleFoo.class.getMethod("something", Object.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				method).get(Order.class);
		assertThat(annotation.getInt("value")).isEqualTo(1);
	}

	@Test
	public void getValueFromNonPublicAnnotation() throws Exception {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertThat(declaredAnnotations).hasSize(1);
		Annotation annotation = declaredAnnotations[0];
		MergedAnnotation<Annotation> mergedAnnotation = MergedAnnotation.from(annotation);
		assertThat(mergedAnnotation.getType()).contains("NonPublicAnnotation");
		assertThat(
				mergedAnnotation.synthesize().annotationType().getSimpleName()).isEqualTo(
						"NonPublicAnnotation");
		assertThat(mergedAnnotation.getInt("value")).isEqualTo(42);
	}

	@Test
	public void getDefaultValueFromAnnotation() throws Exception {
		Method method = XSimpleFoo.class.getMethod("something", Object.class);
		MergedAnnotation<Order> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE, method).get(Order.class);
		assertThat(annotation.getDefaultValue("value")).contains(
				Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	public void getDefaultValueFromNonPublicAnnotation() {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertThat(declaredAnnotations).hasSize(1);
		Annotation declaredAnnotation = declaredAnnotations[0];
		MergedAnnotation<?> annotation = MergedAnnotation.from(declaredAnnotation);
		assertThat(annotation.getType()).isEqualTo(
				"org.springframework.core.annotation.subpackage.NonPublicAnnotation");
		assertThat(annotation.getDefaultValue("value")).contains(-1);
	}

	@Test
	public void getDefaultValueFromAnnotationType() {
		MergedAnnotation<?> annotation = MergedAnnotation.from(Order.class);
		assertThat(annotation.getDefaultValue("value")).contains(
				Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	public void findRepeatableAnnotationOnComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				RepeatableContainers.none(), AnnotationFilter.NONE,
				SearchStrategy.EXHAUSTIVE, XMyRepeatableMeta1.class).get(Repeatable.class);
		assertThat(annotation.getClass("value")).isEqualTo(XMyRepeatableContainer.class);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnMethod() throws Exception {
		Method method = XInterfaceWithRepeated.class.getMethod("foo");
		Stream<MergedAnnotation<XMyRepeatable>> annotations = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE, method).stream(XMyRepeatable.class);
		Stream<String> values = annotations.map(
				annotation -> annotation.getString("value"));
		assertThat(values).containsExactly("A", "B", "C", "meta1");
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClassWithMissingAttributeAliasDeclaration()
			throws Exception {
		RepeatableContainers containers = RepeatableContainers.of(XBrokenHierarchy.class,
				XBrokenContextConfig.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotations.from(containers, AnnotationFilter.PLAIN,
						SearchStrategy.EXHAUSTIVE,
						XBrokenConfigHierarchyTestCase.class)).withMessageStartingWith(
								"Attribute 'value' in").withMessageContaining(
										XBrokenContextConfig.class.getName()).withMessageContaining(
												"@AliasFor 'location'");
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClassWithAttributeAliases() {
		assertThat(MergedAnnotations.from(XConfigHierarchyTestCase.class).stream(
				XContextConfig.class)).isEmpty();
		RepeatableContainers containers = RepeatableContainers.of(XHierarchy.class,
				XContextConfig.class);
		MergedAnnotations annotations = MergedAnnotations.from(containers,
				AnnotationFilter.NONE, SearchStrategy.DIRECT,
				XConfigHierarchyTestCase.class);
		assertThat(annotations.stream(XContextConfig.class).map(
				annotation -> annotation.getString("location"))).containsExactly("A",
						"B");
		assertThat(annotations.stream(XContextConfig.class).map(
				annotation -> annotation.getString("value"))).containsExactly("A", "B");
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClass() {
		Class<?> element = XMyRepeatableClass.class;
		String[] expectedValuesJava = { "A", "B", "C" };
		String[] expectedValuesSpring = { "A", "B", "C", "meta1" };
		testRepeatables(SearchStrategy.SUPER_CLASS, element, expectedValuesJava,
				expectedValuesSpring);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnSuperclass() {
		Class<?> element = XSubMyRepeatableClass.class;
		String[] expectedValuesJava = { "A", "B", "C" };
		String[] expectedValuesSpring = { "A", "B", "C", "meta1" };
		testRepeatables(SearchStrategy.SUPER_CLASS, element, expectedValuesJava,
				expectedValuesSpring);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClassAndSuperclass() {
		Class<?> element = XSubMyRepeatableWithAdditionalLocalDeclarationsClass.class;
		String[] expectedValuesJava = { "X", "Y", "Z" };
		String[] expectedValuesSpring = { "X", "Y", "Z", "meta2" };
		testRepeatables(SearchStrategy.SUPER_CLASS, element, expectedValuesJava,
				expectedValuesSpring);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnMultipleSuperclasses() {
		Class<?> element = XSubSubMyRepeatableWithAdditionalLocalDeclarationsClass.class;
		String[] expectedValuesJava = { "X", "Y", "Z" };
		String[] expectedValuesSpring = { "X", "Y", "Z", "meta2" };
		testRepeatables(SearchStrategy.SUPER_CLASS, element, expectedValuesJava,
				expectedValuesSpring);
	}

	@Test
	public void getDeclaredRepeatableAnnotationsDeclaredOnClass() {
		Class<?> element = XMyRepeatableClass.class;
		String[] expectedValuesJava = { "A", "B", "C" };
		String[] expectedValuesSpring = { "A", "B", "C", "meta1" };
		testRepeatables(SearchStrategy.SUPER_CLASS, element, expectedValuesJava,
				expectedValuesSpring);
	}

	@Test
	public void getDeclaredRepeatableAnnotationsDeclaredOnSuperclass() {
		Class<?> element = XSubMyRepeatableClass.class;
		String[] expectedValuesJava = {};
		String[] expectedValuesSpring = {};
		testRepeatables(SearchStrategy.DIRECT, element, expectedValuesJava,
				expectedValuesSpring);
	}

	private void testRepeatables(SearchStrategy searchStrategy, Class<?> element,
			String[] expectedValuesJava, String[] expectedValuesSpring) {
		testJavaRepeatables(searchStrategy, element, expectedValuesJava);
		testExplicitRepeatables(searchStrategy, element, expectedValuesSpring);
		testStandardRepeatables(searchStrategy, element, expectedValuesSpring);
	}

	private void testJavaRepeatables(SearchStrategy searchStrategy, Class<?> element,
			String[] expected) {
		XMyRepeatable[] annotations = searchStrategy == SearchStrategy.DIRECT
				? element.getDeclaredAnnotationsByType(XMyRepeatable.class)
				: element.getAnnotationsByType(XMyRepeatable.class);
		assertThat(Arrays.stream(annotations).map(XMyRepeatable::value)).containsExactly(
				expected);
	}

	private void testExplicitRepeatables(SearchStrategy searchStrategy, Class<?> element,
			String[] expected) {
		MergedAnnotations annotations = MergedAnnotations.from(
				RepeatableContainers.of(XMyRepeatableContainer.class, XMyRepeatable.class),
				AnnotationFilter.PLAIN, searchStrategy, element);
		assertThat(annotations.stream(XMyRepeatable.class).filter(
				MergedAnnotationPredicates.firstRunOf(
						MergedAnnotation::getAggregateIndex)).map(
								annotation -> annotation.getString(
										"value"))).containsExactly(expected);
	}

	private void testStandardRepeatables(SearchStrategy searchStrategy, Class<?> element,
			String[] expected) {
		MergedAnnotations annotations = MergedAnnotations.from(searchStrategy, element);
		assertThat(annotations.stream(XMyRepeatable.class).filter(
				MergedAnnotationPredicates.firstRunOf(
						MergedAnnotation::getAggregateIndex)).map(
								annotation -> annotation.getString(
										"value"))).containsExactly(expected);
	}

	@Test
	public void synthesizeAnnotationWithoutAttributeAliases() throws Exception {
		Component component = XWebController.class.getAnnotation(Component.class);
		assertThat(component).isNotNull();
		Component synthesizedComponent = MergedAnnotation.from(component).synthesize();
		assertThat(synthesizedComponent).isNotNull();
		assertThat(synthesizedComponent).isEqualTo(component);
		assertThat(synthesizedComponent.value()).isEqualTo("webController");
	}

	@Test
	public void synthesizeAlreadySynthesizedAnnotation() throws Exception {
		Method method = XWebController.class.getMethod("handleMappedWithValueAttribute");
		XWebMapping webMapping = method.getAnnotation(XWebMapping.class);
		assertThat(webMapping).isNotNull();
		XWebMapping synthesizedWebMapping = MergedAnnotation.from(webMapping).synthesize();
		XWebMapping synthesizedAgainWebMapping = MergedAnnotation.from(
				synthesizedWebMapping).synthesize();
		assertThat(synthesizedWebMapping).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesizedAgainWebMapping).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesizedWebMapping).isEqualTo(synthesizedAgainWebMapping);
		assertThat(synthesizedWebMapping.name()).isEqualTo("foo");
		assertThat(synthesizedWebMapping.path()).containsExactly("/test");
		assertThat(synthesizedWebMapping.value()).containsExactly("/test");
	}

	@Test
	public void synthesizeAnnotationWhereAliasForIsMissingAttributeDeclaration()
			throws Exception {
		XAliasForWithMissingAttributeDeclaration annotation = XAliasForWithMissingAttributeDeclarationClass.class.getAnnotation(
				XAliasForWithMissingAttributeDeclaration.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"@AliasFor declaration on attribute 'foo' in annotation").withMessageContaining(
								XAliasForWithMissingAttributeDeclaration.class.getName()).withMessageContaining(
										"points to itself");
	}

	@Test
	public void synthesizeAnnotationWhereAliasForHasDuplicateAttributeDeclaration()
			throws Exception {
		XAliasForWithDuplicateAttributeDeclaration annotation = AliasForWithDuplicateAttributeDeclarationClass.class.getAnnotation(
				XAliasForWithDuplicateAttributeDeclaration.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"In @AliasFor declared on attribute 'foo' in annotation").withMessageContaining(
								XAliasForWithDuplicateAttributeDeclaration.class.getName()).withMessageContaining(
										"attribute 'attribute' and its alias 'value' are present with values of 'bar' and 'baz'");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForNonexistentAttribute()
			throws Exception {
		AliasForNonexistentAttribute annotation = XAliasForNonexistentAttributeClass.class.getAnnotation(
				AliasForNonexistentAttribute.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"@AliasFor declaration on attribute 'foo' in annotation").withMessageContaining(
								AliasForNonexistentAttribute.class.getName()).withMessageContaining(
										"declares an alias for 'bar' which is not present");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasWithoutMirroredAliasFor()
			throws Exception {
		XAliasForWithoutMirroredAliasFor annotation = XAliasForWithoutMirroredAliasForClass.class.getAnnotation(
				XAliasForWithoutMirroredAliasFor.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"Attribute 'bar' in").withMessageContaining(
								XAliasForWithoutMirroredAliasFor.class.getName()).withMessageContaining(
										"@AliasFor 'foo'");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasWithMirroredAliasForWrongAttribute()
			throws Exception {
		XAliasForWithMirroredAliasForWrongAttribute annotation = XAliasForWithMirroredAliasForWrongAttributeClass.class.getAnnotation(
				XAliasForWithMirroredAliasForWrongAttribute.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"Attribute 'bar' in").withMessageContaining(
								XAliasForWithMirroredAliasForWrongAttribute.class.getName()).withMessageContaining(
										"must be declared as an @AliasFor 'foo', not attribute 'quux'");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForAttributeOfDifferentType()
			throws Exception {
		XAliasForAttributeOfDifferentType annotation = XAliasForAttributeOfDifferentTypeClass.class.getAnnotation(
				XAliasForAttributeOfDifferentType.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"Misconfigured aliases").withMessageContaining(
								XAliasForAttributeOfDifferentType.class.getName()).withMessageContaining(
										"attribute 'foo'").withMessageContaining(
												"attribute 'bar'").withMessageContaining(
														"same return type");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForWithMissingDefaultValues()
			throws Exception {
		XAliasForWithMissingDefaultValues annotation = XAliasForWithMissingDefaultValuesClass.class.getAnnotation(
				XAliasForWithMissingDefaultValues.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"Misconfigured aliases").withMessageContaining(
								XAliasForWithMissingDefaultValues.class.getName()).withMessageContaining(
										"attribute 'foo' in annotation").withMessageContaining(
												"attribute 'bar' in annotation").withMessageContaining(
														"default values");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForAttributeWithDifferentDefaultValue()
			throws Exception {
		XAliasForAttributeWithDifferentDefaultValue annotation = XAliasForAttributeWithDifferentDefaultValueClass.class.getAnnotation(
				XAliasForAttributeWithDifferentDefaultValue.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"Misconfigured aliases").withMessageContaining(
								XAliasForAttributeWithDifferentDefaultValue.class.getName()).withMessageContaining(
										"attribute 'foo' in annotation").withMessageContaining(
												"attribute 'bar' in annotation").withMessageContaining(
														"same default value");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForMetaAnnotationThatIsNotMetaPresent()
			throws Exception {
		XAliasedComposedContextConfigNotMetaPresent annotation = XAliasedComposedContextConfigNotMetaPresentClass.class.getAnnotation(
				XAliasedComposedContextConfigNotMetaPresent.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(annotation)).withMessageStartingWith(
						"@AliasFor declaration on attribute 'xmlConfigFile' in annotation").withMessageContaining(
								XAliasedComposedContextConfigNotMetaPresent.class.getName()).withMessageContaining(
										"declares an alias for attribute 'location' in annotation").withMessageContaining(
												XContextConfig.class.getName()).withMessageContaining(
														"not meta-present");
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliases() throws Exception {
		assertAnnotationSynthesisWithImplicitAliases(
				XValueImplicitAliasesContextConfigClass.class, "value");
		assertAnnotationSynthesisWithImplicitAliases(
				XLocation1ImplicitAliasesContextConfigClass.class, "location1");
		assertAnnotationSynthesisWithImplicitAliases(
				XXmlImplicitAliasesContextConfigClass.class, "xmlFile");
		assertAnnotationSynthesisWithImplicitAliases(
				XGroovyImplicitAliasesContextConfigClass.class, "groovyScript");
	}

	private void assertAnnotationSynthesisWithImplicitAliases(Class<?> clazz,
			String expected) throws Exception {
		XImplicitAliasesContextConfig config = clazz.getAnnotation(
				XImplicitAliasesContextConfig.class);
		assertThat(config).isNotNull();
		XImplicitAliasesContextConfig synthesized = MergedAnnotation.from(
				config).synthesize();
		assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesized.value()).isEqualTo(expected);
		assertThat(synthesized.location1()).isEqualTo(expected);
		assertThat(synthesized.xmlFile()).isEqualTo(expected);
		assertThat(synthesized.groovyScript()).isEqualTo(expected);
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithImpliedAliasNamesOmitted()
			throws Exception {
		assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
				XValueImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass.class,
				"value");
		assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
				XLocationsImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass.class,
				"location");
		assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
				XXmlFilesImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass.class,
				"xmlFile");
	}

	private void assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
			Class<?> clazz, String expected) {
		XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig config = clazz.getAnnotation(
				XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class);
		assertThat(config).isNotNull();
		XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig synthesized = MergedAnnotation.from(
				config).synthesize();
		assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesized.value()).isEqualTo(expected);
		assertThat(synthesized.location()).isEqualTo(expected);
		assertThat(synthesized.xmlFile()).isEqualTo(expected);
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesForAliasPair() throws Exception {
		XImplicitAliasesForAliasPairContextConfig config = XImplicitAliasesForAliasPairContextConfigClass.class.getAnnotation(
				XImplicitAliasesForAliasPairContextConfig.class);
		XImplicitAliasesForAliasPairContextConfig synthesized = MergedAnnotation.from(
				config).synthesize();
		assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesized.xmlFile()).isEqualTo("test.xml");
		assertThat(synthesized.groovyScript()).isEqualTo("test.xml");
	}

	@Test
	public void synthesizeAnnotationWithTransitiveImplicitAliases() throws Exception {
		XTransitiveImplicitAliasesContextConfig config = XTransitiveImplicitAliasesContextConfigClass.class.getAnnotation(
				XTransitiveImplicitAliasesContextConfig.class);
		XTransitiveImplicitAliasesContextConfig synthesized = MergedAnnotation.from(
				config).synthesize();
		assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesized.xml()).isEqualTo("test.xml");
		assertThat(synthesized.groovy()).isEqualTo("test.xml");
	}

	@Test
	public void synthesizeAnnotationWithTransitiveImplicitAliasesForAliasPair()
			throws Exception {
		XTransitiveImplicitAliasesForAliasPairContextConfig config = XTransitiveImplicitAliasesForAliasPairContextConfigClass.class.getAnnotation(
				XTransitiveImplicitAliasesForAliasPairContextConfig.class);
		XTransitiveImplicitAliasesForAliasPairContextConfig synthesized = MergedAnnotation.from(
				config).synthesize();
		assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesized.xml()).isEqualTo("test.xml");
		assertThat(synthesized.groovy()).isEqualTo("test.xml");
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithMissingDefaultValues()
			throws Exception {
		Class<?> clazz = XImplicitAliasesWithMissingDefaultValuesContextConfigClass.class;
		Class<XImplicitAliasesWithMissingDefaultValuesContextConfig> annotationType = XImplicitAliasesWithMissingDefaultValuesContextConfig.class;
		XImplicitAliasesWithMissingDefaultValuesContextConfig config = clazz.getAnnotation(
				annotationType);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(clazz, config)).withMessageStartingWith(
						"Misconfigured aliases:").withMessageContaining(
								"attribute 'location1' in annotation ["
										+ annotationType.getName()
										+ "]").withMessageContaining(
												"attribute 'location2' in annotation ["
														+ annotationType.getName()
														+ "]").withMessageContaining(
																"default values");
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithDifferentDefaultValues()
			throws Exception {
		Class<?> clazz = XImplicitAliasesWithDifferentDefaultValuesContextConfigClass.class;
		Class<XImplicitAliasesWithDifferentDefaultValuesContextConfig> annotationType = XImplicitAliasesWithDifferentDefaultValuesContextConfig.class;
		XImplicitAliasesWithDifferentDefaultValuesContextConfig config = clazz.getAnnotation(
				annotationType);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(clazz, config)).withMessageStartingWith(
						"Misconfigured aliases:").withMessageContaining(
								"attribute 'location1' in annotation ["
										+ annotationType.getName()
										+ "]").withMessageContaining(
												"attribute 'location2' in annotation ["
														+ annotationType.getName()
														+ "]").withMessageContaining(
																"same default value");
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithDuplicateValues()
			throws Exception {
		Class<?> clazz = XImplicitAliasesWithDuplicateValuesContextConfigClass.class;
		Class<XImplicitAliasesWithDuplicateValuesContextConfig> annotationType = XImplicitAliasesWithDuplicateValuesContextConfig.class;
		XImplicitAliasesWithDuplicateValuesContextConfig config = clazz.getAnnotation(
				annotationType);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(clazz, config)).withMessageStartingWith(
						"Different @AliasFor mirror values for annotation").withMessageContaining(
								annotationType.getName()).withMessageContaining(
										"declared on class").withMessageContaining(
												clazz.getName()).withMessageContaining(
														"are declared with values of");
	}

	@Test
	public void synthesizeAnnotationFromMapWithoutAttributeAliases() throws Exception {
		Component component = XWebController.class.getAnnotation(Component.class);
		assertThat(component).isNotNull();
		Map<String, Object> map = Collections.singletonMap("value", "webController");
		MergedAnnotation<Component> annotation = MergedAnnotation.from(Component.class,
				map);
		Component synthesizedComponent = annotation.synthesize();
		assertThat(synthesizedComponent).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesizedComponent.value()).isEqualTo("webController");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeAnnotationFromMapWithNestedMap() throws Exception {
		XComponentScanSingleFilter componentScan = XComponentScanSingleFilterClass.class.getAnnotation(
				XComponentScanSingleFilter.class);
		assertThat(componentScan).isNotNull();
		assertThat(componentScan.value().pattern()).isEqualTo("*Foo");
		Map<String, Object> map = MergedAnnotation.from(componentScan).asMap(
				annotation -> new LinkedHashMap<String, Object>(),
				MapValues.ANNOTATION_TO_MAP);
		Map<String, Object> filterMap = (Map<String, Object>) map.get("value");
		assertThat(filterMap.get("pattern")).isEqualTo("*Foo");
		filterMap.put("pattern", "newFoo");
		filterMap.put("enigma", 42);
		MergedAnnotation<XComponentScanSingleFilter> annotation = MergedAnnotation.from(
				XComponentScanSingleFilter.class, map);
		XComponentScanSingleFilter synthesizedComponentScan = annotation.synthesize();
		assertThat(synthesizedComponentScan).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesizedComponentScan.value().pattern()).isEqualTo("newFoo");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeAnnotationFromMapWithNestedArrayOfMaps() throws Exception {
		XComponentScan componentScan = ComponentScanClass.class.getAnnotation(
				XComponentScan.class);
		assertThat(componentScan).isNotNull();
		Map<String, Object> map = MergedAnnotation.from(componentScan).asMap(
				annotation -> new LinkedHashMap<String, Object>(),
				MapValues.ANNOTATION_TO_MAP);
		Map<String, Object>[] filters = (Map[]) map.get("excludeFilters");
		List<String> patterns = Arrays.stream(filters).map(
				m -> (String) m.get("pattern")).collect(Collectors.toList());
		assertThat(patterns).containsExactly("*Foo", "*Bar");
		filters[0].put("pattern", "newFoo");
		filters[0].put("enigma", 42);
		filters[1].put("pattern", "newBar");
		filters[1].put("enigma", 42);
		MergedAnnotation<XComponentScan> annotation = MergedAnnotation.from(
				XComponentScan.class, map);
		XComponentScan synthesizedComponentScan = annotation.synthesize();
		assertThat(synthesizedComponentScan).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(Arrays.stream(synthesizedComponentScan.excludeFilters()).map(
				XFilter::pattern)).containsExactly("newFoo", "newBar");
	}

	@Test
	public void synthesizeAnnotationFromDefaultsWithoutAttributeAliases()
			throws Exception {
		MergedAnnotation<XAnnotationWithDefaults> annotation = MergedAnnotation.from(
				XAnnotationWithDefaults.class);
		XAnnotationWithDefaults synthesized = annotation.synthesize();
		assertThat(synthesized.text()).isEqualTo("enigma");
		assertThat(synthesized.predicate()).isTrue();
		assertThat(synthesized.characters()).containsExactly('a', 'b', 'c');
	}

	@Test
	public void synthesizeAnnotationFromDefaultsWithAttributeAliases() throws Exception {
		MergedAnnotation<XContextConfig> annotation = MergedAnnotation.from(
				XContextConfig.class);
		XContextConfig synthesized = annotation.synthesize();
		assertThat(synthesized.value()).isEqualTo("");
		assertThat(synthesized.location()).isEqualTo("");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasesWithDifferentValues()
			throws Exception {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotation.from(XContextConfigMismatch.class.getAnnotation(
						XContextConfig.class)).synthesize());
	}

	@Test
	public void synthesizeAnnotationFromMapWithMinimalAttributesWithAttributeAliases()
			throws Exception {
		Map<String, Object> map = Collections.singletonMap("location", "test.xml");
		MergedAnnotation<XContextConfig> annotation = MergedAnnotation.from(
				XContextConfig.class, map);
		XContextConfig synthesized = annotation.synthesize();
		assertThat(synthesized.value()).isEqualTo("test.xml");
		assertThat(synthesized.location()).isEqualTo("test.xml");
	}

	@Test
	public void synthesizeAnnotationFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements()
			throws Exception {
		synthesizeAnnotationFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements(
				Collections.singletonMap("value", "/foo"));
		synthesizeAnnotationFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements(
				Collections.singletonMap("path", "/foo"));
	}

	private void synthesizeAnnotationFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements(
			Map<String, Object> map) {
		MergedAnnotation<XGet> annotation = MergedAnnotation.from(XGet.class, map);
		XGet synthesized = annotation.synthesize();
		assertThat(synthesized.value()).isEqualTo("/foo");
		assertThat(synthesized.path()).isEqualTo("/foo");
	}

	@Test
	public void synthesizeAnnotationFromMapWithImplicitAttributeAliases()
			throws Exception {
		assertAnnotationSynthesisFromMapWithImplicitAliases("value");
		assertAnnotationSynthesisFromMapWithImplicitAliases("location1");
		assertAnnotationSynthesisFromMapWithImplicitAliases("location2");
		assertAnnotationSynthesisFromMapWithImplicitAliases("location3");
		assertAnnotationSynthesisFromMapWithImplicitAliases("xmlFile");
		assertAnnotationSynthesisFromMapWithImplicitAliases("groovyScript");
	}

	private void assertAnnotationSynthesisFromMapWithImplicitAliases(
			String attributeNameAndValue) throws Exception {
		Map<String, Object> map = Collections.singletonMap(attributeNameAndValue,
				attributeNameAndValue);
		MergedAnnotation<XImplicitAliasesContextConfig> annotation = MergedAnnotation.from(
				XImplicitAliasesContextConfig.class, map);
		XImplicitAliasesContextConfig synthesized = annotation.synthesize();
		assertThat(synthesized.value()).isEqualTo(attributeNameAndValue);
		assertThat(synthesized.location1()).isEqualTo(attributeNameAndValue);
		assertThat(synthesized.location2()).isEqualTo(attributeNameAndValue);
		assertThat(synthesized.location2()).isEqualTo(attributeNameAndValue);
		assertThat(synthesized.xmlFile()).isEqualTo(attributeNameAndValue);
		assertThat(synthesized.groovyScript()).isEqualTo(attributeNameAndValue);
	}

	@Test
	public void synthesizeAnnotationFromMapWithMissingAttributeValue() throws Exception {
		assertMissingTextAttribute(Collections.emptyMap());
	}

	@Test
	public void synthesizeAnnotationFromMapWithNullAttributeValue() throws Exception {
		Map<String, Object> map = Collections.singletonMap("text", null);
		assertThat(map).containsKey("text");
		assertMissingTextAttribute(map);
	}

	private void assertMissingTextAttribute(Map<String, Object> attributes) {
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> {
			MergedAnnotation<XAnnotationWithoutDefaults> annotation = MergedAnnotation.from(
					XAnnotationWithoutDefaults.class, attributes);
			annotation.synthesize();
		}).withMessage("No value found for attribute named 'text' in merged annotation "
				+ XAnnotationWithoutDefaults.class.getName());
	}

	@Test
	public void synthesizeAnnotationFromMapWithAttributeOfIncorrectType()
			throws Exception {
		Map<String, Object> map = Collections.singletonMap("value", 42L);
		MergedAnnotation<Component> annotation = MergedAnnotation.from(Component.class,
				map);
		assertThatIllegalStateException().isThrownBy(
				() -> annotation.synthesize()).withMessage(
						"Attribute 'value' in annotation org.springframework.stereotype.Component "
								+ "should be of type java.lang.String but a java.lang.Long value was returned");
	}

	@Test
	public void synthesizeAnnotationFromAnnotationAttributesWithoutAttributeAliases()
			throws Exception {
		Component component = XWebController.class.getAnnotation(Component.class);
		assertThat(component).isNotNull();
		Map<String, Object> attributes = MergedAnnotation.from(component).asMap();
		Component synthesized = MergedAnnotation.from(Component.class,
				attributes).synthesize();
		assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(synthesized).isEqualTo(component);
	}

	@Test
	public void toStringForSynthesizedAnnotations() throws Exception {
		Method methodWithPath = XWebController.class.getMethod(
				"handleMappedWithPathAttribute");
		XWebMapping webMappingWithAliases = methodWithPath.getAnnotation(XWebMapping.class);
		assertThat(webMappingWithAliases).isNotNull();
		Method methodWithPathAndValue = XWebController.class.getMethod(
				"handleMappedWithSamePathAndValueAttributes");
		XWebMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(
				XWebMapping.class);
		assertThat(methodWithPathAndValue).isNotNull();
		XWebMapping synthesizedWebMapping1 = MergedAnnotation.from(
				webMappingWithAliases).synthesize();
		XWebMapping synthesizedWebMapping2 = MergedAnnotation.from(
				webMappingWithPathAndValue).synthesize();
		assertThat(webMappingWithAliases.toString()).isNotEqualTo(
				synthesizedWebMapping1.toString());
		assertToStringForWebMappingWithPathAndValue(synthesizedWebMapping1);
		assertToStringForWebMappingWithPathAndValue(synthesizedWebMapping2);
	}

	private void assertToStringForWebMappingWithPathAndValue(XWebMapping webMapping) {
		String prefix = "@" + XWebMapping.class.getName() + "(";
		assertThat(webMapping.toString()).startsWith(prefix).contains("value=[/test]",
				"path=[/test]", "name=bar", "method=", "[GET, POST]").endsWith(")");
	}

	@Test
	public void equalsForSynthesizedAnnotations() throws Exception {
		Method methodWithPath = XWebController.class.getMethod(
				"handleMappedWithPathAttribute");
		XWebMapping webMappingWithAliases = methodWithPath.getAnnotation(XWebMapping.class);
		assertThat(webMappingWithAliases).isNotNull();
		Method methodWithPathAndValue = XWebController.class.getMethod(
				"handleMappedWithSamePathAndValueAttributes");
		XWebMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(
				XWebMapping.class);
		assertThat(webMappingWithPathAndValue).isNotNull();
		XWebMapping synthesizedWebMapping1 = MergedAnnotation.from(
				webMappingWithAliases).synthesize();
		XWebMapping synthesizedWebMapping2 = MergedAnnotation.from(
				webMappingWithPathAndValue).synthesize();
		// Equality amongst standard annotations
		assertThat(webMappingWithAliases).isEqualTo(webMappingWithAliases);
		assertThat(webMappingWithPathAndValue).isEqualTo(webMappingWithPathAndValue);
		// Inequality amongst standard annotations
		assertThat(webMappingWithAliases).isNotEqualTo(webMappingWithPathAndValue);
		assertThat(webMappingWithPathAndValue).isNotEqualTo(webMappingWithAliases);
		// Equality amongst synthesized annotations
		assertThat(synthesizedWebMapping1).isEqualTo(synthesizedWebMapping1);
		assertThat(synthesizedWebMapping2).isEqualTo(synthesizedWebMapping2);
		assertThat(synthesizedWebMapping1).isEqualTo(synthesizedWebMapping2);
		assertThat(synthesizedWebMapping2).isEqualTo(synthesizedWebMapping1);
		// Equality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1).isEqualTo(webMappingWithPathAndValue);
		assertThat(webMappingWithPathAndValue).isEqualTo(synthesizedWebMapping1);
		// Inequality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1).isNotEqualTo(webMappingWithAliases);
		assertThat(webMappingWithAliases).isNotEqualTo(synthesizedWebMapping1);
	}

	@Test
	public void hashCodeForSynthesizedAnnotations() throws Exception {
		Method methodWithPath = XWebController.class.getMethod(
				"handleMappedWithPathAttribute");
		XWebMapping webMappingWithAliases = methodWithPath.getAnnotation(XWebMapping.class);
		assertThat(webMappingWithAliases).isNotNull();
		Method methodWithPathAndValue = XWebController.class.getMethod(
				"handleMappedWithSamePathAndValueAttributes");
		XWebMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(
				XWebMapping.class);
		assertThat(webMappingWithPathAndValue).isNotNull();
		XWebMapping synthesizedWebMapping1 = MergedAnnotation.from(
				webMappingWithAliases).synthesize();
		assertThat(synthesizedWebMapping1).isNotNull();
		XWebMapping synthesizedWebMapping2 = MergedAnnotation.from(
				webMappingWithPathAndValue).synthesize();
		assertThat(synthesizedWebMapping2).isNotNull();
		// Equality amongst standard annotations
		assertThat(webMappingWithAliases.hashCode()).isEqualTo(
				webMappingWithAliases.hashCode());
		assertThat(webMappingWithPathAndValue.hashCode()).isEqualTo(
				webMappingWithPathAndValue.hashCode());
		// Inequality amongst standard annotations
		assertThat(webMappingWithAliases.hashCode()).isNotEqualTo(
				webMappingWithPathAndValue.hashCode());
		assertThat(webMappingWithPathAndValue.hashCode()).isNotEqualTo(
				webMappingWithAliases.hashCode());
		// Equality amongst synthesized annotations
		assertThat(synthesizedWebMapping1.hashCode()).isEqualTo(
				synthesizedWebMapping1.hashCode());
		assertThat(synthesizedWebMapping2.hashCode()).isEqualTo(
				synthesizedWebMapping2.hashCode());
		assertThat(synthesizedWebMapping1.hashCode()).isEqualTo(
				synthesizedWebMapping2.hashCode());
		assertThat(synthesizedWebMapping2.hashCode()).isEqualTo(
				synthesizedWebMapping1.hashCode());
		// Equality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1.hashCode()).isEqualTo(
				webMappingWithPathAndValue.hashCode());
		assertThat(webMappingWithPathAndValue.hashCode()).isEqualTo(
				synthesizedWebMapping1.hashCode());
		// Inequality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1.hashCode()).isNotEqualTo(
				webMappingWithAliases.hashCode());
		assertThat(webMappingWithAliases.hashCode()).isNotEqualTo(
				synthesizedWebMapping1.hashCode());
	}

	/**
	 * Fully reflection-based test that verifies support for synthesizing annotations
	 * across packages with non-public visibility of user types (e.g., a non-public
	 * annotation that uses {@code @AliasFor}).
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeNonPublicAnnotationWithAttributeAliasesFromDifferentPackage()
			throws Exception {
		Class<?> type = ClassUtils.forName(
				"org.springframework.core.annotation.subpackage.NonPublicAliasedAnnotatedClass",
				null);
		Class<? extends Annotation> annotationType = (Class<? extends Annotation>) ClassUtils.forName(
				"org.springframework.core.annotation.subpackage.NonPublicAliasedAnnotation",
				null);
		Annotation annotation = type.getAnnotation(annotationType);
		assertThat(annotation).isNotNull();
		MergedAnnotation<Annotation> mergedAnnotation = MergedAnnotation.from(annotation);
		Annotation synthesizedAnnotation = mergedAnnotation.synthesize();
		assertThat(synthesizedAnnotation).isInstanceOf(SynthesizedAnnotation.class);
		assertThat(mergedAnnotation.getString("name")).isEqualTo("test");
		assertThat(mergedAnnotation.getString("path")).isEqualTo("/test");
		assertThat(mergedAnnotation.getString("value")).isEqualTo("/test");
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasesInNestedAnnotations()
			throws Exception {
		XHierarchy hierarchy = XConfigHierarchyTestCase.class.getAnnotation(
				XHierarchy.class);
		assertThat(hierarchy).isNotNull();
		XHierarchy synthesizedHierarchy = MergedAnnotation.from(hierarchy).synthesize();
		assertThat(synthesizedHierarchy).isInstanceOf(SynthesizedAnnotation.class);
		XContextConfig[] configs = synthesizedHierarchy.value();
		assertThat(configs).isNotNull();
		assertThat(configs).allMatch(SynthesizedAnnotation.class::isInstance);
		assertThat(Arrays.stream(configs).map(XContextConfig::location)).containsExactly(
				"A", "B");
		assertThat(Arrays.stream(configs).map(XContextConfig::value)).containsExactly("A",
				"B");
	}

	@Test
	public void synthesizeAnnotationWithArrayOfAnnotations() throws Exception {
		XHierarchy hierarchy = XConfigHierarchyTestCase.class.getAnnotation(
				XHierarchy.class);
		assertThat(hierarchy).isNotNull();
		XHierarchy synthesizedHierarchy = MergedAnnotation.from(hierarchy).synthesize();
		assertThat(synthesizedHierarchy).isInstanceOf(SynthesizedAnnotation.class);
		XContextConfig contextConfig = XSimpleConfigTestCase.class.getAnnotation(
				XContextConfig.class);
		assertThat(contextConfig).isNotNull();
		XContextConfig[] configs = synthesizedHierarchy.value();
		assertThat(Arrays.stream(configs).map(XContextConfig::location)).containsExactly(
				"A", "B");
		// Alter array returned from synthesized annotation
		configs[0] = contextConfig;
		// Re-retrieve the array from the synthesized annotation
		configs = synthesizedHierarchy.value();
		assertThat(Arrays.stream(configs).map(XContextConfig::location)).containsExactly(
				"A", "B");
	}

	@Test
	public void synthesizeAnnotationWithArrayOfChars() throws Exception {
		XCharsContainer charsContainer = XGroupOfCharsClass.class.getAnnotation(
				XCharsContainer.class);
		assertThat(charsContainer).isNotNull();
		XCharsContainer synthesizedCharsContainer = MergedAnnotation.from(
				charsContainer).synthesize();
		assertThat(synthesizedCharsContainer).isInstanceOf(SynthesizedAnnotation.class);
		char[] chars = synthesizedCharsContainer.chars();
		assertThat(chars).containsExactly('x', 'y', 'z');
		// Alter array returned from synthesized annotation
		chars[0] = '?';
		// Re-retrieve the array from the synthesized annotation
		chars = synthesizedCharsContainer.chars();
		assertThat(chars).containsExactly('x', 'y', 'z');
	}

	@Component("meta1")
	@Order
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Meta1 {
	}

	@Component("meta2")
	@XTransactional(readOnly = true)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta2 {
	}

	@Meta2
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaMeta {
	}

	@MetaMeta
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaMetaMeta {
	}

	@MetaCycle3
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaCycle1 {
	}

	@MetaCycle1
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaCycle2 {
	}

	@MetaCycle2
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaCycle3 {
	}

	@Meta1
	interface InterfaceWithMetaAnnotation {
	}

	@Meta2
	static class ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface
			implements InterfaceWithMetaAnnotation {
	}

	@Meta1
	static class ClassWithInheritedMetaAnnotation {
	}

	@Meta2
	static class SubClassWithInheritedMetaAnnotation
			extends ClassWithInheritedMetaAnnotation {
	}

	static class SubSubClassWithInheritedMetaAnnotation
			extends SubClassWithInheritedMetaAnnotation {
	}

	@XTransactional
	static class XClassWithInheritedAnnotation {
	}

	@Meta2
	static class XSubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
	}

	static class XSubSubClassWithInheritedAnnotation
			extends SubClassWithInheritedAnnotation {
	}

	@MetaMeta
	static class MetaMetaAnnotatedClass {
	}

	@MetaMetaMeta
	static class MetaMetaMetaAnnotatedClass {
	}

	@MetaCycle3
	static class MetaCycleAnnotatedClass {
	}

	public interface AnnotatedInterface {

		@Order(0)
		void fromInterfaceImplementedByRoot();
	}

	public interface NullableAnnotatedInterface {

		@Nullable
		void fromInterfaceImplementedByRoot();
	}

	public static class Root implements AnnotatedInterface {

		@Order(27)
		public void annotatedOnRoot() {
		}

		@Meta1
		public void metaAnnotatedOnRoot() {
		}

		public void overrideToAnnotate() {
		}

		@Order(27)
		public void overrideWithoutNewAnnotation() {
		}

		public void notAnnotated() {
		}

		@Override
		public void fromInterfaceImplementedByRoot() {
		}
	}

	public static class Leaf extends Root {

		@Order(25)
		public void annotatedOnLeaf() {
		}

		@Meta1
		public void metaAnnotatedOnLeaf() {
		}

		@MetaMeta
		public void metaMetaAnnotatedOnLeaf() {
		}

		@Override
		@Order(1)
		public void overrideToAnnotate() {
		}

		@Override
		public void overrideWithoutNewAnnotation() {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface XTransactional {

		boolean readOnly() default false;
	}

	public static abstract class XFoo<T> {

		@Order(1)
		public abstract void something(T arg);
	}

	public static class XSimpleFoo extends XFoo<String> {

		@Override
		@XTransactional
		public void something(final String arg) {
		}
	}

	@XTransactional
	public interface XInheritedAnnotationInterface {
	}

	public interface XSubInheritedAnnotationInterface
			extends XInheritedAnnotationInterface {
	}

	public interface XSubSubInheritedAnnotationInterface
			extends XSubInheritedAnnotationInterface {
	}

	@Order
	public interface XNonInheritedAnnotationInterface {
	}

	public interface XSubNonInheritedAnnotationInterface
			extends XNonInheritedAnnotationInterface {
	}

	public interface XSubSubNonInheritedAnnotationInterface
			extends XSubNonInheritedAnnotationInterface {
	}

	public static class XNonAnnotatedClass {
	}

	public interface XNonAnnotatedInterface {
	}

	@XTransactional
	public static class XInheritedAnnotationClass {
	}

	public static class XSubInheritedAnnotationClass extends XInheritedAnnotationClass {
	}

	@Order
	public static class XNonInheritedAnnotationClass {
	}

	public static class XSubNonInheritedAnnotationClass
			extends XNonInheritedAnnotationClass {
	}

	@XTransactional
	public static class XTransactionalClass {
	}

	@Order
	public static class XTransactionalAndOrderedClass extends XTransactionalClass {
	}

	public static class XSubTransactionalAndOrderedClass
			extends XTransactionalAndOrderedClass {
	}

	public interface XInterfaceWithAnnotatedMethod {

		@Order
		void foo();
	}

	public static class XImplementsInterfaceWithAnnotatedMethod
			implements XInterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public static class XSubOfImplementsInterfaceWithAnnotatedMethod
			extends XImplementsInterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public abstract static class XAbstractDoesNotImplementInterfaceWithAnnotatedMethod
			implements XInterfaceWithAnnotatedMethod {
	}

	public static class XSubOfAbstractImplementsInterfaceWithAnnotatedMethod
			extends XAbstractDoesNotImplementInterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public interface XInterfaceWithGenericAnnotatedMethod<T> {

		@Order
		void foo(T t);
	}

	public static class ImplementsInterfaceWithGenericAnnotatedMethod
			implements XInterfaceWithGenericAnnotatedMethod<String> {

		public void foo(String t) {
		}
	}

	public static abstract class XBaseClassWithGenericAnnotatedMethod<T> {

		@Order
		abstract void foo(T t);
	}

	public static class XExtendsBaseClassWithGenericAnnotatedMethod
			extends XBaseClassWithGenericAnnotatedMethod<String> {

		public void foo(String t) {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface XMyRepeatableContainer {

		XMyRepeatable[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Repeatable(XMyRepeatableContainer.class)
	@interface XMyRepeatable {

		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@XMyRepeatable("meta1")
	@interface XMyRepeatableMeta1 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@XMyRepeatable("meta2")
	@interface XMyRepeatableMeta2 {
	}

	interface XInterfaceWithRepeated {

		@XMyRepeatable("A")
		@XMyRepeatableContainer({ @XMyRepeatable("B"), @XMyRepeatable("C") })
		@XMyRepeatableMeta1
		void foo();
	}

	@XMyRepeatable("A")
	@XMyRepeatableContainer({ @XMyRepeatable("B"), @XMyRepeatable("C") })
	@XMyRepeatableMeta1
	static class XMyRepeatableClass {
	}

	static class XSubMyRepeatableClass extends XMyRepeatableClass {
	}

	@XMyRepeatable("X")
	@XMyRepeatableContainer({ @XMyRepeatable("Y"), @XMyRepeatable("Z") })
	@XMyRepeatableMeta2
	static class XSubMyRepeatableWithAdditionalLocalDeclarationsClass
			extends XMyRepeatableClass {
	}

	static class XSubSubMyRepeatableWithAdditionalLocalDeclarationsClass
			extends XSubMyRepeatableWithAdditionalLocalDeclarationsClass {
	}

	enum XRequestMethod {
		GET, POST
	}

	/**
	 * Mock of {@code org.springframework.web.bind.annotation.RequestMapping}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface XWebMapping {

		String name();

		@AliasFor("path")
		String[] value() default "";

		@AliasFor(attribute = "value")
		String[] path() default "";

		XRequestMethod[] method() default {};
	}

	/**
	 * Mock of {@code org.springframework.web.bind.annotation.GetMapping}, except that the
	 * String arrays are overridden with single String elements.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@XWebMapping(method = XRequestMethod.GET, name = "")
	@interface XGet {

		@AliasFor(annotation = XWebMapping.class)
		String value() default "";

		@AliasFor(annotation = XWebMapping.class)
		String path() default "";
	}

	/**
	 * Mock of {@code org.springframework.web.bind.annotation.PostMapping}, except that
	 * the path is overridden by convention with single String element.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@XWebMapping(method = XRequestMethod.POST, name = "")
	@interface XPost {

		String path() default "";
	}

	@Component("webController")
	static class XWebController {

		@XWebMapping(value = "/test", name = "foo")
		public void handleMappedWithValueAttribute() {
		}

		@XWebMapping(path = "/test", name = "bar", method = { XRequestMethod.GET,
			XRequestMethod.POST })
		public void handleMappedWithPathAttribute() {
		}

		@XGet("/test")
		public void getMappedWithValueAttribute() {
		}

		@XGet(path = "/test")
		public void getMappedWithPathAttribute() {
		}

		@XPost(path = "/test")
		public void postMappedWithPathAttribute() {
		}

		/**
		 * mapping is logically "equal" to handleMappedWithPathAttribute().
		 */
		@XWebMapping(value = "/test", path = "/test", name = "bar", method = {
			XRequestMethod.GET, XRequestMethod.POST })
		public void handleMappedWithSamePathAndValueAttributes() {
		}

		@XWebMapping(value = "/enigma", path = "/test", name = "baz")
		public void handleMappedWithDifferentPathAndValueAttributes() {
		}
	}

	/**
	 * Mock of {@code org.springframework.test.context.ContextConfiguration}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface XContextConfig {

		@AliasFor("location")
		String value() default "";

		@AliasFor("value")
		String location() default "";

		Class<?> klass() default Object.class;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XBrokenContextConfig {

		// Intentionally missing:
		// @AliasFor("location")
		String value() default "";

		@AliasFor("value")
		String location() default "";
	}

	/**
	 * Mock of {@code org.springframework.test.context.ContextHierarchy}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface XHierarchy {

		XContextConfig[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XBrokenHierarchy {

		XBrokenContextConfig[] value();
	}

	@XHierarchy({ @XContextConfig("A"), @XContextConfig(location = "B") })
	static class XConfigHierarchyTestCase {
	}

	@XBrokenHierarchy(@XBrokenContextConfig)
	static class XBrokenConfigHierarchyTestCase {
	}

	@XContextConfig("simple.xml")
	static class XSimpleConfigTestCase {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XCharsContainer {

		@AliasFor(attribute = "chars")
		char[] value() default {};

		@AliasFor(attribute = "value")
		char[] chars() default {};
	}

	@XCharsContainer(chars = { 'x', 'y', 'z' })
	static class XGroupOfCharsClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasForWithMissingAttributeDeclaration {

		@AliasFor
		String foo() default "";
	}

	@XAliasForWithMissingAttributeDeclaration
	static class XAliasForWithMissingAttributeDeclarationClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasForWithDuplicateAttributeDeclaration {

		@AliasFor(value = "bar", attribute = "baz")
		String foo() default "";
	}

	@XAliasForWithDuplicateAttributeDeclaration
	static class AliasForWithDuplicateAttributeDeclarationClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForNonexistentAttribute {

		@AliasFor("bar")
		String foo() default "";
	}

	@AliasForNonexistentAttribute
	static class XAliasForNonexistentAttributeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasForWithoutMirroredAliasFor {

		@AliasFor("bar")
		String foo() default "";

		String bar() default "";
	}

	@XAliasForWithoutMirroredAliasFor
	static class XAliasForWithoutMirroredAliasForClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasForWithMirroredAliasForWrongAttribute {

		@AliasFor(attribute = "bar")
		String[] foo() default "";

		@AliasFor(attribute = "quux")
		String[] bar() default "";
	}

	@XAliasForWithMirroredAliasForWrongAttribute
	static class XAliasForWithMirroredAliasForWrongAttributeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasForAttributeOfDifferentType {

		@AliasFor("bar")
		String[] foo() default "";

		@AliasFor("foo")
		boolean bar() default true;
	}

	@XAliasForAttributeOfDifferentType
	static class XAliasForAttributeOfDifferentTypeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasForWithMissingDefaultValues {

		@AliasFor(attribute = "bar")
		String foo();

		@AliasFor(attribute = "foo")
		String bar();
	}

	@XAliasForWithMissingDefaultValues(foo = "foo", bar = "bar")
	static class XAliasForWithMissingDefaultValuesClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasForAttributeWithDifferentDefaultValue {

		@AliasFor("bar")
		String foo() default "X";

		@AliasFor("foo")
		String bar() default "Z";
	}

	@XAliasForAttributeWithDifferentDefaultValue
	static class XAliasForAttributeWithDifferentDefaultValueClass {
	}

	// @ContextConfig --> Intentionally NOT meta-present
	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasedComposedContextConfigNotMetaPresent {

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String xmlConfigFile();
	}

	@XAliasedComposedContextConfigNotMetaPresent(xmlConfigFile = "test.xml")
	static class XAliasedComposedContextConfigNotMetaPresentClass {
	}

	@XContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XAliasedComposedContextConfig {

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String xmlConfigFile();
	}

	@XContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	public @interface XImplicitAliasesContextConfig {

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String xmlFile() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String groovyScript() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String value() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location1() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location2() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location3() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "klass")
		Class<?> configClass() default Object.class;

		String nonAliasedAttribute() default "";
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesContextConfig(groovyScript = "groovyScript")
	static class XGroovyImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesContextConfig(xmlFile = "xmlFile")
	static class XXmlImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesContextConfig("value")
	static class XValueImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesContextConfig(location1 = "location1")
	static class XLocation1ImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesContextConfig(location2 = "location2")
	static class XLocation2ImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesContextConfig(location3 = "location3")
	static class XLocation3ImplicitAliasesContextConfigClass {
	}

	@XContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig {

		// intentionally omitted: attribute = "value"
		@AliasFor(annotation = XContextConfig.class)
		String value() default "";

		// intentionally omitted: attribute = "locations"
		@AliasFor(annotation = XContextConfig.class)
		String location() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String xmlFile() default "";
	}

	@XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XTransitiveImplicitAliasesWithImpliedAliasNamesOmittedContextConfig {

		@AliasFor(annotation = XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class, attribute = "xmlFile")
		String xml() default "";

		@AliasFor(annotation = XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class, attribute = "location")
		String groovy() default "";
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig("value")
	static class XValueImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig(location = "location")
	static class XLocationsImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@XImplicitAliasesWithImpliedAliasNamesOmittedContextConfig(xmlFile = "xmlFile")
	static class XXmlFilesImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass {
	}

	@XContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XImplicitAliasesWithMissingDefaultValuesContextConfig {

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location1();

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location2();
	}

	@XImplicitAliasesWithMissingDefaultValuesContextConfig(location1 = "1", location2 = "2")
	static class XImplicitAliasesWithMissingDefaultValuesContextConfigClass {
	}

	@XContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XImplicitAliasesWithDifferentDefaultValuesContextConfig {

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location1() default "foo";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location2() default "bar";
	}

	@XImplicitAliasesWithDifferentDefaultValuesContextConfig(location1 = "1", location2 = "2")
	static class XImplicitAliasesWithDifferentDefaultValuesContextConfigClass {
	}

	@XContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XImplicitAliasesWithDuplicateValuesContextConfig {

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location1() default "";

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String location2() default "";
	}

	@XImplicitAliasesWithDuplicateValuesContextConfig(location1 = "1", location2 = "2")
	static class XImplicitAliasesWithDuplicateValuesContextConfigClass {
	}

	@XContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XImplicitAliasesForAliasPairContextConfig {

		@AliasFor(annotation = XContextConfig.class, attribute = "location")
		String xmlFile() default "";

		@AliasFor(annotation = XContextConfig.class, value = "value")
		String groovyScript() default "";
	}

	@XImplicitAliasesForAliasPairContextConfig(xmlFile = "test.xml")
	static class XImplicitAliasesForAliasPairContextConfigClass {
	}

	@XImplicitAliasesContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XTransitiveImplicitAliasesContextConfig {

		@AliasFor(annotation = XImplicitAliasesContextConfig.class, attribute = "xmlFile")
		String xml() default "";

		@AliasFor(annotation = XImplicitAliasesContextConfig.class, attribute = "groovyScript")
		String groovy() default "";
	}

	@XTransitiveImplicitAliasesContextConfig(xml = "test.xml")
	static class XTransitiveImplicitAliasesContextConfigClass {
	}

	@XImplicitAliasesForAliasPairContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface XTransitiveImplicitAliasesForAliasPairContextConfig {

		@AliasFor(annotation = XImplicitAliasesForAliasPairContextConfig.class, attribute = "xmlFile")
		String xml() default "";

		@AliasFor(annotation = XImplicitAliasesForAliasPairContextConfig.class, attribute = "groovyScript")
		String groovy() default "";
	}

	@XTransitiveImplicitAliasesForAliasPairContextConfig(xml = "test.xml")
	static class XTransitiveImplicitAliasesForAliasPairContextConfigClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface XFilter {

		String pattern();

	}

	/**
	 * Mock of {@code org.springframework.context.annotation.ComponentScan}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface XComponentScan {

		XFilter[] excludeFilters() default {};
	}

	@XComponentScan(excludeFilters = { @XFilter(pattern = "*Foo"),
		@XFilter(pattern = "*Bar") })
	static class ComponentScanClass {
	}

	/**
	 * Mock of {@code org.springframework.context.annotation.ComponentScan}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface XComponentScanSingleFilter {

		XFilter value();
	}

	@XComponentScanSingleFilter(@XFilter(pattern = "*Foo"))
	static class XComponentScanSingleFilterClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAnnotationWithDefaults {

		String text() default "enigma";

		boolean predicate() default true;

		char[] characters() default { 'a', 'b', 'c' };
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface XAnnotationWithoutDefaults {

		String text();
	}

	@XContextConfig(value = "foo", location = "bar")
	interface XContextConfigMismatch {
	}




}
