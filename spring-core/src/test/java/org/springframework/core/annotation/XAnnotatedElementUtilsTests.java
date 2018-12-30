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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.XAnnotatedElementUtilsTests.ContextConfig;
import org.springframework.core.annotation.XAnnotationUtilsTests.ExtendsBaseClassWithGenericAnnotatedMethod;
import org.springframework.core.annotation.XAnnotationUtilsTests.ImplementsInterfaceWithGenericAnnotatedMethod;
import org.springframework.core.annotation.XAnnotationUtilsTests.WebController;
import org.springframework.core.annotation.XAnnotationUtilsTests.WebMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.core.annotation.AnnotatedElementUtils.*;

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
		MergedAnnotation<?> annotation = MergedAnnotations.from(
				SearchStrategy.INHERITED_ANNOTATIONS,
				InvalidConventionBasedComposedContextConfigClass.class).get(
						ContextConfig.class);

		Class<?> element = InvalidConventionBasedComposedContextConfigClass.class;
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(
				either(containsString("attribute 'value' and its alias 'locations'")).or(
						containsString("attribute 'locations' and its alias 'value'")));
		exception.expectMessage(either(containsString(
				"values of [{duplicateDeclaration}] and [{requiredLocationsDeclaration}]")).or(
						containsString(
								"values of [{requiredLocationsDeclaration}] and [{duplicateDeclaration}]")));
		exception.expectMessage(either(containsString("but only one is permitted")).or(
				containsString("Different @AliasFor mirror values for annotation")));
		getMergedAnnotationAttributes(element, ContextConfig.class);
	}

	@Test
	public void getMergedAnnotationAttributesWithShadowedAliasComposedAnnotation() {
		Class<?> element = ShadowedAliasComposedContextConfigClass.class;
		AnnotationAttributes attributes = getMergedAnnotationAttributes(element,
				ContextConfig.class);

		String[] expected = asArray("test.xml");

		assertNotNull("Should find @ContextConfig on " + element.getSimpleName(),
				attributes);
		assertArrayEquals("locations", expected, attributes.getStringArray("locations"));
		assertArrayEquals("value", expected, attributes.getStringArray("value"));
	}

	@Test
	public void findMergedAnnotationAttributesOnInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				InheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on InheritedAnnotationInterface",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				SubInheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on SubInheritedAnnotationInterface",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubSubInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				SubSubInheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on SubSubInheritedAnnotationInterface",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesOnNonInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				NonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on NonInheritedAnnotationInterface",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubNonInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				SubNonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on SubNonInheritedAnnotationInterface",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesOnSubSubNonInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				SubSubNonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on SubSubNonInheritedAnnotationInterface",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromInterfaceMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
				"handleFromInterface");
		AnnotationAttributes attributes = findMergedAnnotationAttributes(method,
				Order.class);
		assertNotNull(
				"Should find @Order on ConcreteClassWithInheritedAnnotation.handleFromInterface() method",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromAbstractMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handle");
		AnnotationAttributes attributes = findMergedAnnotationAttributes(method,
				Transactional.class);
		assertNotNull(
				"Should find @Transactional on ConcreteClassWithInheritedAnnotation.handle() method",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesInheritedFromBridgedMethod()
			throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
				"handleParameterized", String.class);
		AnnotationAttributes attributes = findMergedAnnotationAttributes(method,
				Transactional.class);
		assertNotNull(
				"Should find @Transactional on bridged ConcreteClassWithInheritedAnnotation.handleParameterized()",
				attributes);
	}

	/**
	 * Bridge/bridged method setup code copied from
	 * {@link org.springframework.core.BridgeMethodResolverTests#testWithGenericParameter()}.
	 * @since 4.2
	 */
	@Test
	public void findMergedAnnotationAttributesFromBridgeMethod() {
		Method[] methods = StringGenericParameter.class.getMethods();
		Method bridgeMethod = null;
		Method bridgedMethod = null;

		for (Method method : methods) {
			if ("getFor".equals(method.getName())
					&& !method.getParameterTypes()[0].equals(Integer.class)) {
				if (method.getReturnType().equals(Object.class)) {
					bridgeMethod = method;
				}
				else {
					bridgedMethod = method;
				}
			}
		}
		assertTrue(bridgeMethod != null && bridgeMethod.isBridge());
		assertTrue(bridgedMethod != null && !bridgedMethod.isBridge());

		AnnotationAttributes attributes = findMergedAnnotationAttributes(bridgeMethod,
				Order.class);
		assertNotNull(
				"Should find @Order on StringGenericParameter.getFor() bridge method",
				attributes);
	}

	@Test
	public void findMergedAnnotationAttributesOnClassWithMetaAndLocalTxConfig() {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				MetaAndLocalTxConfigClass.class, Transactional.class);
		assertNotNull("Should find @Transactional on MetaAndLocalTxConfigClass",
				attributes);
		assertEquals("TX qualifier for MetaAndLocalTxConfigClass.", "localTxMgr",
				attributes.getString("qualifier"));
	}

	@Test
	public void findAndSynthesizeAnnotationAttributesOnClassWithAttributeAliasesInTargetAnnotation() {
		String qualifier = "aliasForQualifier";

		// 1) Find and merge AnnotationAttributes from the annotation hierarchy
		AnnotationAttributes attributes = findMergedAnnotationAttributes(
				AliasedTransactionalComponentClass.class, AliasedTransactional.class);
		assertNotNull("@AliasedTransactional on AliasedTransactionalComponentClass.",
				attributes);

		// 2) Synthesize the AnnotationAttributes back into the target
		// annotation
		AliasedTransactional annotation = AnnotationUtils.synthesizeAnnotation(attributes,
				AliasedTransactional.class, AliasedTransactionalComponentClass.class);
		assertNotNull(annotation);

		// 3) Verify that the AnnotationAttributes and synthesized annotation
		// are equivalent
		assertEquals("TX value via attributes.", qualifier,
				attributes.getString("value"));
		assertEquals("TX value via synthesized annotation.", qualifier,
				annotation.value());
		assertEquals("TX qualifier via attributes.", qualifier,
				attributes.getString("qualifier"));
		assertEquals("TX qualifier via synthesized annotation.", qualifier,
				annotation.qualifier());
	}

	@Test
	public void findMergedAnnotationAttributesOnClassWithAttributeAliasInComposedAnnotationAndNestedAnnotationsInTargetAnnotation() {
		AnnotationAttributes attributes = assertComponentScanAttributes(
				TestComponentScanClass.class, "com.example.app.test");

		Filter[] excludeFilters = attributes.getAnnotationArray("excludeFilters",
				Filter.class);
		assertNotNull(excludeFilters);

		List<String> patterns = stream(excludeFilters).map(Filter::pattern).collect(
				toList());
		assertEquals(asList("*Test", "*Tests"), patterns);
	}

	/**
	 * This test ensures that
	 * {@link AnnotationUtils#postProcessAnnotationAttributes} uses
	 * {@code ObjectUtils.nullSafeEquals()} to check for equality between
	 * annotation attributes since attributes may be arrays.
	 */
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

	private AnnotationAttributes assertComponentScanAttributes(Class<?> element,
			String... expected) {
		AnnotationAttributes attributes = findMergedAnnotationAttributes(element,
				ComponentScan.class);

		assertNotNull("Should find @ComponentScan on " + element, attributes);
		assertArrayEquals("value: ", expected, attributes.getStringArray("value"));
		assertArrayEquals("basePackages: ", expected,
				attributes.getStringArray("basePackages"));

		return attributes;
	}

	private AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "annotationType must not be null");
		return AnnotatedElementUtils.findMergedAnnotationAttributes(element,
				annotationType.getName(), false, false);
	}

	@Test
	public void findMergedAnnotationWithAttributeAliasesInTargetAnnotation() {
		Class<?> element = AliasedTransactionalComponentClass.class;
		AliasedTransactional annotation = findMergedAnnotation(element,
				AliasedTransactional.class);
		assertNotNull("@AliasedTransactional on " + element, annotation);
		assertEquals("TX value via synthesized annotation.", "aliasForQualifier",
				annotation.value());
		assertEquals("TX qualifier via synthesized annotation.", "aliasForQualifier",
				annotation.qualifier());
	}

	@Test
	public void findMergedAnnotationForMultipleMetaAnnotationsWithClashingAttributeNames() {
		String[] xmlLocations = asArray("test.xml");
		String[] propFiles = asArray("test.properties");

		Class<?> element = AliasedComposedContextConfigAndTestPropSourceClass.class;

		ContextConfig contextConfig = findMergedAnnotation(element, ContextConfig.class);
		assertNotNull("@ContextConfig on " + element, contextConfig);
		assertArrayEquals("locations", xmlLocations, contextConfig.locations());
		assertArrayEquals("value", xmlLocations, contextConfig.value());

		// Synthesized annotation
		TestPropSource testPropSource = AnnotationUtils.findAnnotation(element,
				TestPropSource.class);
		assertArrayEquals("locations", propFiles, testPropSource.locations());
		assertArrayEquals("value", propFiles, testPropSource.value());

		// Merged annotation
		testPropSource = findMergedAnnotation(element, TestPropSource.class);
		assertNotNull("@TestPropSource on " + element, testPropSource);
		assertArrayEquals("locations", propFiles, testPropSource.locations());
		assertArrayEquals("value", propFiles, testPropSource.value());
	}

	@Test
	public void findMergedAnnotationWithLocalAliasesThatConflictWithAttributesInMetaAnnotationByConvention() {
		final String[] EMPTY = new String[0];
		Class<?> element = SpringAppConfigClass.class;
		ContextConfig contextConfig = findMergedAnnotation(element, ContextConfig.class);

		assertNotNull("Should find @ContextConfig on " + element, contextConfig);
		assertArrayEquals("locations for " + element, EMPTY, contextConfig.locations());
		// 'value' in @SpringAppConfig should not override 'value' in
		// @ContextConfig
		assertArrayEquals("value for " + element, EMPTY, contextConfig.value());
		assertArrayEquals("classes for " + element, new Class<?>[] { Number.class },
				contextConfig.classes());
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
		WebMapping webMapping = findMergedAnnotation(element, WebMapping.class);
		assertNotNull(webMapping);
		assertArrayEquals("value attribute: ", asArray("/test"), webMapping.value());
		assertArrayEquals("path attribute: ", asArray("/test"), webMapping.path());
	}

	@Test
	public void javaLangAnnotationTypeViaFindMergedAnnotation() throws Exception {
		Constructor<?> deprecatedCtor = Date.class.getConstructor(String.class);
		assertEquals(deprecatedCtor.getAnnotation(Deprecated.class),
				findMergedAnnotation(deprecatedCtor, Deprecated.class));
		assertEquals(Date.class.getAnnotation(Deprecated.class),
				findMergedAnnotation(Date.class, Deprecated.class));
	}

	@Test
	public void javaxAnnotationTypeViaFindMergedAnnotation() throws Exception {
		assertEquals(ResourceHolder.class.getAnnotation(Resource.class),
				findMergedAnnotation(ResourceHolder.class, Resource.class));
		assertEquals(SpringAppConfigClass.class.getAnnotation(Resource.class),
				findMergedAnnotation(SpringAppConfigClass.class, Resource.class));
	}

	@Test
	public void getAllMergedAnnotationsOnClassWithInterface() throws Exception {
		Method m = TransactionalServiceImpl.class.getMethod("doIt");
		Set<Transactional> allMergedAnnotations = getAllMergedAnnotations(m,
				Transactional.class);
		assertTrue(allMergedAnnotations.isEmpty());
	}

	@Test
	public void findAllMergedAnnotationsOnClassWithInterface() throws Exception {
		Method m = TransactionalServiceImpl.class.getMethod("doIt");
		Set<Transactional> allMergedAnnotations = findAllMergedAnnotations(m,
				Transactional.class);
		assertEquals(1, allMergedAnnotations.size());
	}

	@Test // SPR-16060
	public void findMethodAnnotationFromGenericInterface() throws Exception {
		Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod(
				"foo", String.class);
		Order order = findMergedAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test // SPR-17146
	public void findMethodAnnotationFromGenericSuperclass() throws Exception {
		Method method = ExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod("foo",
				String.class);
		Order order = findMergedAnnotation(method, Order.class);
		assertNotNull(order);
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
