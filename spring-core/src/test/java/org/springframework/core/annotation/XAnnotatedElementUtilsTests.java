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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.XAnnotationUtilsTests.ExtendsBaseClassWithGenericAnnotatedMethod;
import org.springframework.core.annotation.XAnnotationUtilsTests.ImplementsInterfaceWithGenericAnnotatedMethod;
import org.springframework.core.annotation.XAnnotationUtilsTests.WebController;
import org.springframework.core.annotation.XAnnotationUtilsTests.WebMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * Unit tests for {@link AnnotatedElementUtils}.
 *
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0.3
 * @see AnnotationUtilsTests
 * @see MultipleComposedAnnotationsOnSingleAnnotatedElementTests
 * @see ComposedRepeatableAnnotationsTests
 */
public class XAnnotatedElementUtilsTests {

	private static final String TX_NAME = Transactional.class.getName();

	@Rule
	public final ExpectedException exception = ExpectedException.none();

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
		MultiValueMap<String, Object> map = MergedAnnotations.from(NonAnnotatedClass.class).stream(
				Transactional.class).collect(
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
	 * If the "value" entry contains both "DerivedTxConfig" AND "TxConfig", then
	 * the algorithm is accidentally picking up shadowed annotations of the same
	 * type within the class hierarchy. Such undesirable behavior would cause
	 * the logic in
	 * {@link org.springframework.context.annotation.ProfileCondition} to fail.
	 * @see org.springframework.core.env.EnvironmentSystemIntegrationTests#mostSpecificDerivedClassDrivesEnvironment_withDevEnvAndDerivedDevConfigClass
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				DerivedTxConfig.class).stream(
						Transactional.class).collect(
								MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(entry("value", Arrays.asList("DerivedTxConfig")));
	}

	/**
	 * Note: this functionality is required by
	 * {@link org.springframework.context.annotation.ProfileCondition}.
	 * @see org.springframework.core.env.EnvironmentSystemIntegrationTests
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithMultipleComposedAnnotations() {
		MultiValueMap<String, Object> map = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				TxFromMultipleComposedAnnotations.class).stream(
						Transactional.class).collect(
								MergedAnnotationCollectors.toMultiValueMap());
		assertThat(map).contains(entry("value", Arrays.asList("TxInheritedComposed", "TxComposed")));
	}

	@Test
	public void getMergedAnnotationAttributesOnClassWithLocalAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(TxConfig.class).get(Transactional.class);
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
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, SubClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isTrue();
	}

	@Test
	public void getMergedAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, SubSubClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isFalse();
	}

	@Test
	public void getMergedAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,SubSubClassWithInheritedComposedAnnotation.class).get(Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isFalse();
	}

	@Test
	public void getMergedAnnotationAttributesFromInterfaceImplementedBySuperclass() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,ConcreteClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isFalse();
	}

	@Test
	public void getMergedAnnotationAttributesOnInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,InheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test
	public void getMergedAnnotationAttributesOnNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,NonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test
	public void getMergedAnnotationAttributesWithConventionBasedComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,ConventionBasedComposedContextConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getStringArray("locations")).containsExactly("explicitDeclaration");
		assertThat(annotation.getStringArray("value")).containsExactly("explicitDeclaration");
	}

	@Test
	public void getMergedAnnotationAttributesWithHalfConventionBasedAndHalfAliasedComposedAnnotation1() {
		// SPR-13554: convention mapping mixed with AlaisFor annotations
		// xmlConfigFiles can be used because it has an AlaisFor annotation
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, HalfConventionBasedAndHalfAliasedComposedContextConfigClassV1.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).containsExactly("explicitDeclaration");
		assertThat(annotation.getStringArray("value")).containsExactly("explicitDeclaration");
	}

	@Test
	public void getMergedAnnotationAttributesWithHalfConventionBasedAndHalfAliasedComposedAnnotation2() {
		// SPR-13554: convention mapping mixed with AlaisFor annotations
		// locations doesn't apply because it has no AlaisFor annotation
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, HalfConventionBasedAndHalfAliasedComposedContextConfigClassV2.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).isEmpty();
		assertThat(annotation.getStringArray("value")).isEmpty();
	}

	@Test
	public void getMergedAnnotationAttributesWithAliasedComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,AliasedComposedContextConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
	}

	@Test
	public void getMergedAnnotationAttributesWithAliasedValueComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,AliasedValueComposedContextConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
	}

	@Test
	public void getMergedAnnotationAttributesWithImplicitAliasesInMetaAnnotationOnComposedAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS,ComposedImplicitAliasesContextConfigClass.class).get(ImplicitAliasesContextConfig.class);
		assertThat(annotation.getStringArray("groovyScripts")).containsExactly("A.xml", "B.xml");
		assertThat(annotation.getStringArray("xmlFiles")).containsExactly("A.xml", "B.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("A.xml", "B.xml");
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
				SearchStrategy.INHERITED_ANNOTATIONS, ComposedImplicitAliasesContextConfigClass.class).get(ImplicitAliasesContextConfig.class);
		assertThat(annotation.getStringArray("groovyScripts")).containsExactly("A.xml", "B.xml");
		assertThat(annotation.getStringArray("xmlFiles")).containsExactly("A.xml", "B.xml");
		assertThat(annotation.getStringArray("locations")).containsExactly("A.xml", "B.xml");
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
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				InheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				SubInheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				SubSubInheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findMergedAnnotationAttributesOnNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				NonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				SubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				SubSubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromInterfaceMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
				"handleFromInterface");
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				method).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromAbstractMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handle");
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
				method).get(Transactional.class);
		assertThat(annotation.isPresent()).isTrue();
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromBridgedMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
				"handleParameterized", String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
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
		assertTrue(bridgeMethod != null && bridgeMethod.isBridge());
		assertTrue(bridgedMethod != null && !bridgedMethod.isBridge());
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE,
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
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, element).get(ComponentScan.class);
		assertThat(annotation.getStringArray("value")).containsExactly(expected);
		assertThat(annotation.getStringArray("basePackages")).containsExactly(expected);
		return annotation;
	}

	@Test
	public void findMergedAnnotationForMultipleMetaAnnotationsWithClashingAttributeNames() {
		MergedAnnotations annotations = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, AliasedComposedContextConfigAndTestPropSourceClass.class);
		MergedAnnotation<?> contextConfig = annotations.get(ContextConfig.class);
		assertThat(contextConfig.getStringArray("locations")).containsExactly("test.xml");
		assertThat(contextConfig.getStringArray("value")).containsExactly("test.xml");
		MergedAnnotation<?> testPropSource = annotations.get(TestPropSource.class);
		assertThat(testPropSource.getStringArray("locations")).containsExactly("test.properties");
		assertThat(testPropSource.getStringArray("value")).containsExactly("test.properties");
	}

	@Test
	public void findMergedAnnotationWithLocalAliasesThatConflictWithAttributesInMetaAnnotationByConvention() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, SpringAppConfigClass.class).get(ContextConfig.class);
		assertThat(annotation.getStringArray("locations")).isEmpty();
		assertThat(annotation.getStringArray("value")).isEmpty();
		assertThat(annotation.getClassArray("classes")).containsExactly(Number.class);
	}

	@Test
	public void findMergedAnnotationWithSingleElementOverridingAnArrayViaConvention()
			throws Exception {
		assertWebMapping(WebController.class.getMethod("postMappedWithPathAttribute"));
	}

	@Test
	public void findMergedAnnotationWithSingleElementOverridingAnArrayViaAliasFor()
			throws Exception {
		assertWebMapping(WebController.class.getMethod("getMappedWithValueAttribute"));
		assertWebMapping(WebController.class.getMethod("getMappedWithPathAttribute"));
	}

	private void assertWebMapping(AnnotatedElement element)
			throws ArrayComparisonFailure {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, element).get(WebMapping.class);
		assertThat(annotation.getStringArray("value")).containsExactly("/test");
		assertThat(annotation.getStringArray("path")).containsExactly("/test");
	}

	@Test
	public void javaLangAnnotationTypeViaFindMergedAnnotation() throws Exception {
		Constructor<?> deprecatedCtor = Date.class.getConstructor(String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(RepeatableContainers.standardRepeatables(),
				AnnotationFilter.NONE, SearchStrategy.DIRECT, deprecatedCtor).get(
						Deprecated.class);
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
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE,
				method).stream(Transactional.class)).hasSize(1);
	}

	@Test // SPR-16060
	public void findMethodAnnotationFromGenericInterface() throws Exception {
		Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod(
				"foo", String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@Test // SPR-17146
	public void findMethodAnnotationFromGenericSuperclass() throws Exception {
		Method method = ExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod(
				"foo", String.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class);
		assertThat(annotation.isPresent()).isTrue();
	}

	@SafeVarargs
	static <T> T[] asArray(T... arr) {
		return arr;
	}

	// -------------------------------------------------------------------------

	@MetaCycle3
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@interface MetaCycle1 {
	}

	@MetaCycle1
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@interface MetaCycle2 {
	}

	@MetaCycle2
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MetaCycle3 {
	}

	@MetaCycle3
	static class MetaCycleAnnotatedClass {
	}

	// -------------------------------------------------------------------------

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
	 * This hybrid approach for annotation attribute overrides with transitive
	 * implicit aliases is unsupported. See SPR-13554 for details.
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
	 * Although the configuration declares an explicit value for 'value' and
	 * requires a value for the aliased 'locations', this does not result in an
	 * error since 'locations' effectively <em>shadows</em> the 'value'
	 * attribute (which cannot be set via the composed annotation anyway).
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
	 * Mock of
	 * {@code org.springframework.boot.test.SpringApplicationConfiguration}.
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

}
