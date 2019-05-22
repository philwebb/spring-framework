/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.subpackage.NonPublicAnnotatedClass;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Component;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThat;


import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertNull;
import static org.springframework.core.annotation.AnnotationUtils.VALUE;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotationDeclaringClass;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotationDeclaringClassForTypes;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.getDeclaredRepeatableAnnotations;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.core.annotation.AnnotationUtils.getRepeatableAnnotations;
import static org.springframework.core.annotation.AnnotationUtils.getValue;
import static org.springframework.core.annotation.AnnotationUtils.isAnnotationDeclaredLocally;
import static org.springframework.core.annotation.AnnotationUtils.isAnnotationInherited;
import static org.springframework.core.annotation.AnnotationUtils.isAnnotationMetaPresent;
import static org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation;

/**
 * Unit tests for {@link AnnotationUtils}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Phillip Webb
 * @author Oleg Zhurakousky
 */
@SuppressWarnings("deprecation")
public class AnnotationUtilsTests {

	@Before
	public void clearCacheBeforeTests() {
		AnnotationUtils.clearCache();
	}


	@Test
	public void findMethodAnnotationOnLeaf() throws Exception {
		Method m = Leaf.class.getMethod("annotatedOnLeaf");
		assertNotNull(m.getAnnotation(Order.class));
		assertNotNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	// @since 4.2
	@Test
	public void findMethodAnnotationWithAnnotationOnMethodInInterface() throws Exception {
		Method m = Leaf.class.getMethod("fromInterfaceImplementedByRoot");
		// @Order is not @Inherited
		assertNull(m.getAnnotation(Order.class));
		// getAnnotation() does not search on interfaces
		assertNull(getAnnotation(m, Order.class));
		// findAnnotation() does search on interfaces
		assertNotNull(findAnnotation(m, Order.class));
	}

	// @since 4.2
	@Test
	public void findMethodAnnotationWithMetaAnnotationOnLeaf() throws Exception {
		Method m = Leaf.class.getMethod("metaAnnotatedOnLeaf");
		assertNull(m.getAnnotation(Order.class));
		assertNotNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	// @since 4.2
	@Test
	public void findMethodAnnotationWithMetaMetaAnnotationOnLeaf() throws Exception {
		Method m = Leaf.class.getMethod("metaMetaAnnotatedOnLeaf");
		assertNull(m.getAnnotation(Component.class));
		assertNull(getAnnotation(m, Component.class));
		assertNotNull(findAnnotation(m, Component.class));
	}

	@Test
	public void findMethodAnnotationOnRoot() throws Exception {
		Method m = Leaf.class.getMethod("annotatedOnRoot");
		assertNotNull(m.getAnnotation(Order.class));
		assertNotNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	// @since 4.2
	@Test
	public void findMethodAnnotationWithMetaAnnotationOnRoot() throws Exception {
		Method m = Leaf.class.getMethod("metaAnnotatedOnRoot");
		assertNull(m.getAnnotation(Order.class));
		assertNotNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	@Test
	public void findMethodAnnotationOnRootButOverridden() throws Exception {
		Method m = Leaf.class.getMethod("overrideWithoutNewAnnotation");
		assertNull(m.getAnnotation(Order.class));
		assertNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	@Test
	public void findMethodAnnotationNotAnnotated() throws Exception {
		Method m = Leaf.class.getMethod("notAnnotated");
		assertNull(findAnnotation(m, Order.class));
	}

	@Test
	public void findMethodAnnotationOnBridgeMethod() throws Exception {
		Method bridgeMethod = SimpleFoo.class.getMethod("something", Object.class);
		assertThat(bridgeMethod.isBridge()).isTrue();

		assertNull(bridgeMethod.getAnnotation(Order.class));
		assertNull(getAnnotation(bridgeMethod, Order.class));
		assertNotNull(findAnnotation(bridgeMethod, Order.class));

		boolean runningInEclipse = Arrays.stream(new Exception().getStackTrace())
				.anyMatch(element -> element.getClassName().startsWith("org.eclipse.jdt"));

		// As of JDK 8, invoking getAnnotation() on a bridge method actually finds an
		// annotation on its 'bridged' method [1]; however, the Eclipse compiler will not
		// support this until Eclipse 4.9 [2]. Thus, we effectively ignore the following
		// assertion if the test is currently executing within the Eclipse IDE.
		//
		// [1] https://bugs.openjdk.java.net/browse/JDK-6695379
		// [2] https://bugs.eclipse.org/bugs/show_bug.cgi?id=495396
		//
		if (!runningInEclipse) {
			assertNotNull(bridgeMethod.getAnnotation(Transactional.class));
		}
		assertNotNull(getAnnotation(bridgeMethod, Transactional.class));
		assertNotNull(findAnnotation(bridgeMethod, Transactional.class));
	}

	@Test
	public void findMethodAnnotationOnBridgedMethod() throws Exception {
		Method bridgedMethod = SimpleFoo.class.getMethod("something", String.class);
		assertThat(bridgedMethod.isBridge()).isFalse();

		assertNull(bridgedMethod.getAnnotation(Order.class));
		assertNull(getAnnotation(bridgedMethod, Order.class));
		assertNotNull(findAnnotation(bridgedMethod, Order.class));

		assertNotNull(bridgedMethod.getAnnotation(Transactional.class));
		assertNotNull(getAnnotation(bridgedMethod, Transactional.class));
		assertNotNull(findAnnotation(bridgedMethod, Transactional.class));
	}

	@Test
	public void findMethodAnnotationFromInterface() throws Exception {
		Method method = ImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test  // SPR-16060
	public void findMethodAnnotationFromGenericInterface() throws Exception {
		Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test  // SPR-17146
	public void findMethodAnnotationFromGenericSuperclass() throws Exception {
		Method method = ExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test
	public void findMethodAnnotationFromInterfaceOnSuper() throws Exception {
		Method method = SubOfImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test
	public void findMethodAnnotationFromInterfaceWhenSuperDoesNotImplementMethod() throws Exception {
		Method method = SubOfAbstractImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	// @since 4.1.2
	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverAnnotationsOnInterfaces() {
		Component component = findAnnotation(ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, Component.class);
		assertNotNull(component);
		assertThat(component.value()).isEqualTo("meta2");
	}

	// @since 4.0.3
	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedAnnotations() {
		Transactional transactional = findAnnotation(SubSubClassWithInheritedAnnotation.class, Transactional.class);
		assertNotNull(transactional);
		assertThat(transactional.readOnly()).as("readOnly flag for SubSubClassWithInheritedAnnotation").isTrue();
	}

	// @since 4.0.3
	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedComposedAnnotations() {
		Component component = findAnnotation(SubSubClassWithInheritedMetaAnnotation.class, Component.class);
		assertNotNull(component);
		assertThat(component.value()).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnMetaMetaAnnotatedClass() {
		Component component = findAnnotation(MetaMetaAnnotatedClass.class, Component.class);
		assertNotNull("Should find meta-annotation on composed annotation on class", component);
		assertThat(component.value()).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnMetaMetaMetaAnnotatedClass() {
		Component component = findAnnotation(MetaMetaMetaAnnotatedClass.class, Component.class);
		assertNotNull("Should find meta-annotation on meta-annotation on composed annotation on class", component);
		assertThat(component.value()).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// TransactionalClass is NOT annotated or meta-annotated with @Component
		Component component = findAnnotation(TransactionalClass.class, Component.class);
		assertNull("Should not find @Component on TransactionalClass", component);
	}

	@Test
	public void findClassAnnotationOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		Component component = findAnnotation(MetaCycleAnnotatedClass.class, Component.class);
		assertNull("Should not find @Component on MetaCycleAnnotatedClass", component);
	}

	// @since 4.2
	@Test
	public void findClassAnnotationOnInheritedAnnotationInterface() {
		Transactional tx = findAnnotation(InheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on InheritedAnnotationInterface", tx);
	}

	// @since 4.2
	@Test
	public void findClassAnnotationOnSubInheritedAnnotationInterface() {
		Transactional tx = findAnnotation(SubInheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on SubInheritedAnnotationInterface", tx);
	}

	// @since 4.2
	@Test
	public void findClassAnnotationOnSubSubInheritedAnnotationInterface() {
		Transactional tx = findAnnotation(SubSubInheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on SubSubInheritedAnnotationInterface", tx);
	}

	// @since 4.2
	@Test
	public void findClassAnnotationOnNonInheritedAnnotationInterface() {
		Order order = findAnnotation(NonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on NonInheritedAnnotationInterface", order);
	}

	// @since 4.2
	@Test
	public void findClassAnnotationOnSubNonInheritedAnnotationInterface() {
		Order order = findAnnotation(SubNonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on SubNonInheritedAnnotationInterface", order);
	}

	// @since 4.2
	@Test
	public void findClassAnnotationOnSubSubNonInheritedAnnotationInterface() {
		Order order = findAnnotation(SubSubNonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on SubSubNonInheritedAnnotationInterface", order);
	}

	@Test
	public void findAnnotationDeclaringClassForAllScenarios() {
		// no class-level annotation
		assertNull(findAnnotationDeclaringClass(Transactional.class, NonAnnotatedInterface.class));
		assertNull(findAnnotationDeclaringClass(Transactional.class, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(findAnnotationDeclaringClass(Transactional.class, InheritedAnnotationInterface.class)).isEqualTo(InheritedAnnotationInterface.class);
		assertNull(findAnnotationDeclaringClass(Transactional.class, SubInheritedAnnotationInterface.class));
		assertThat(findAnnotationDeclaringClass(Transactional.class, InheritedAnnotationClass.class)).isEqualTo(InheritedAnnotationClass.class);
		assertThat(findAnnotationDeclaringClass(Transactional.class, SubInheritedAnnotationClass.class)).isEqualTo(InheritedAnnotationClass.class);

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClass() should still find it on classes.
		assertThat(findAnnotationDeclaringClass(Order.class, NonInheritedAnnotationInterface.class)).isEqualTo(NonInheritedAnnotationInterface.class);
		assertNull(findAnnotationDeclaringClass(Order.class, SubNonInheritedAnnotationInterface.class));
		assertThat(findAnnotationDeclaringClass(Order.class, NonInheritedAnnotationClass.class)).isEqualTo(NonInheritedAnnotationClass.class);
		assertThat(findAnnotationDeclaringClass(Order.class, SubNonInheritedAnnotationClass.class)).isEqualTo(NonInheritedAnnotationClass.class);
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithSingleCandidateType() {
		// no class-level annotation
		List<Class<? extends Annotation>> transactionalCandidateList = Collections.singletonList(Transactional.class);
		assertNull(findAnnotationDeclaringClassForTypes(transactionalCandidateList, NonAnnotatedInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(transactionalCandidateList, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(findAnnotationDeclaringClassForTypes(transactionalCandidateList, InheritedAnnotationInterface.class)).isEqualTo(InheritedAnnotationInterface.class);
		assertNull(findAnnotationDeclaringClassForTypes(transactionalCandidateList, SubInheritedAnnotationInterface.class));
		assertThat(findAnnotationDeclaringClassForTypes(transactionalCandidateList, InheritedAnnotationClass.class)).isEqualTo(InheritedAnnotationClass.class);
		assertThat(findAnnotationDeclaringClassForTypes(transactionalCandidateList, SubInheritedAnnotationClass.class)).isEqualTo(InheritedAnnotationClass.class);

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClassForTypes() should still find it on classes.
		List<Class<? extends Annotation>> orderCandidateList = Collections.singletonList(Order.class);
		assertThat(findAnnotationDeclaringClassForTypes(orderCandidateList, NonInheritedAnnotationInterface.class)).isEqualTo(NonInheritedAnnotationInterface.class);
		assertNull(findAnnotationDeclaringClassForTypes(orderCandidateList, SubNonInheritedAnnotationInterface.class));
		assertThat(findAnnotationDeclaringClassForTypes(orderCandidateList, NonInheritedAnnotationClass.class)).isEqualTo(NonInheritedAnnotationClass.class);
		assertThat(findAnnotationDeclaringClassForTypes(orderCandidateList, SubNonInheritedAnnotationClass.class)).isEqualTo(NonInheritedAnnotationClass.class);
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithMultipleCandidateTypes() {
		List<Class<? extends Annotation>> candidates = asList(Transactional.class, Order.class);

		// no class-level annotation
		assertNull(findAnnotationDeclaringClassForTypes(candidates, NonAnnotatedInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(candidates, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(findAnnotationDeclaringClassForTypes(candidates, InheritedAnnotationInterface.class)).isEqualTo(InheritedAnnotationInterface.class);
		assertNull(findAnnotationDeclaringClassForTypes(candidates, SubInheritedAnnotationInterface.class));
		assertThat(findAnnotationDeclaringClassForTypes(candidates, InheritedAnnotationClass.class)).isEqualTo(InheritedAnnotationClass.class);
		assertThat(findAnnotationDeclaringClassForTypes(candidates, SubInheritedAnnotationClass.class)).isEqualTo(InheritedAnnotationClass.class);

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClassForTypes() should still find it on classes.
		assertThat(findAnnotationDeclaringClassForTypes(candidates, NonInheritedAnnotationInterface.class)).isEqualTo(NonInheritedAnnotationInterface.class);
		assertNull(findAnnotationDeclaringClassForTypes(candidates, SubNonInheritedAnnotationInterface.class));
		assertThat(findAnnotationDeclaringClassForTypes(candidates, NonInheritedAnnotationClass.class)).isEqualTo(NonInheritedAnnotationClass.class);
		assertThat(findAnnotationDeclaringClassForTypes(candidates, SubNonInheritedAnnotationClass.class)).isEqualTo(NonInheritedAnnotationClass.class);

		// class hierarchy mixed with @Transactional and @Order declarations
		assertThat(findAnnotationDeclaringClassForTypes(candidates, TransactionalClass.class)).isEqualTo(TransactionalClass.class);
		assertThat(findAnnotationDeclaringClassForTypes(candidates, TransactionalAndOrderedClass.class)).isEqualTo(TransactionalAndOrderedClass.class);
		assertThat(findAnnotationDeclaringClassForTypes(candidates, SubTransactionalAndOrderedClass.class)).isEqualTo(TransactionalAndOrderedClass.class);
	}

	@Test
	public void isAnnotationDeclaredLocallyForAllScenarios() {
		// no class-level annotation
		assertThat(isAnnotationDeclaredLocally(Transactional.class, NonAnnotatedInterface.class)).isFalse();
		assertThat(isAnnotationDeclaredLocally(Transactional.class, NonAnnotatedClass.class)).isFalse();

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(isAnnotationDeclaredLocally(Transactional.class, InheritedAnnotationInterface.class)).isTrue();
		assertThat(isAnnotationDeclaredLocally(Transactional.class, SubInheritedAnnotationInterface.class)).isFalse();
		assertThat(isAnnotationDeclaredLocally(Transactional.class, InheritedAnnotationClass.class)).isTrue();
		assertThat(isAnnotationDeclaredLocally(Transactional.class, SubInheritedAnnotationClass.class)).isFalse();

		// non-inherited class-level annotation; note: @Order is not inherited
		assertThat(isAnnotationDeclaredLocally(Order.class, NonInheritedAnnotationInterface.class)).isTrue();
		assertThat(isAnnotationDeclaredLocally(Order.class, SubNonInheritedAnnotationInterface.class)).isFalse();
		assertThat(isAnnotationDeclaredLocally(Order.class, NonInheritedAnnotationClass.class)).isTrue();
		assertThat(isAnnotationDeclaredLocally(Order.class, SubNonInheritedAnnotationClass.class)).isFalse();
	}

	@Test
	public void isAnnotationInheritedForAllScenarios() {
		// no class-level annotation
		assertThat(isAnnotationInherited(Transactional.class, NonAnnotatedInterface.class)).isFalse();
		assertThat(isAnnotationInherited(Transactional.class, NonAnnotatedClass.class)).isFalse();

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(isAnnotationInherited(Transactional.class, InheritedAnnotationInterface.class)).isFalse();
		// isAnnotationInherited() does not currently traverse interface hierarchies.
		// Thus the following, though perhaps counter intuitive, must be false:
		assertThat(isAnnotationInherited(Transactional.class, SubInheritedAnnotationInterface.class)).isFalse();
		assertThat(isAnnotationInherited(Transactional.class, InheritedAnnotationClass.class)).isFalse();
		assertThat(isAnnotationInherited(Transactional.class, SubInheritedAnnotationClass.class)).isTrue();

		// non-inherited class-level annotation; note: @Order is not inherited
		assertThat(isAnnotationInherited(Order.class, NonInheritedAnnotationInterface.class)).isFalse();
		assertThat(isAnnotationInherited(Order.class, SubNonInheritedAnnotationInterface.class)).isFalse();
		assertThat(isAnnotationInherited(Order.class, NonInheritedAnnotationClass.class)).isFalse();
		assertThat(isAnnotationInherited(Order.class, SubNonInheritedAnnotationClass.class)).isFalse();
	}

	@Test
	public void isAnnotationMetaPresentForPlainType() {
		assertThat(isAnnotationMetaPresent(Order.class, Documented.class)).isTrue();
		assertThat(isAnnotationMetaPresent(NonNullApi.class, Documented.class)).isTrue();
		assertThat(isAnnotationMetaPresent(NonNullApi.class, Nonnull.class)).isTrue();
		assertThat(isAnnotationMetaPresent(ParametersAreNonnullByDefault.class, Nonnull.class)).isTrue();
	}

	@Test
	public void getAnnotationAttributesWithoutAttributeAliases() {
		Component component = WebController.class.getAnnotation(Component.class);
		assertNotNull(component);

		AnnotationAttributes attributes = (AnnotationAttributes) getAnnotationAttributes(component);
		assertNotNull(attributes);
		assertThat(attributes.getString(VALUE)).as("value attribute: ").isEqualTo("webController");
		assertThat(attributes.annotationType()).isEqualTo(Component.class);
	}

	@Test
	public void getAnnotationAttributesWithNestedAnnotations() {
		ComponentScan componentScan = ComponentScanClass.class.getAnnotation(ComponentScan.class);
		assertNotNull(componentScan);

		AnnotationAttributes attributes = getAnnotationAttributes(ComponentScanClass.class, componentScan);
		assertNotNull(attributes);
		assertThat(attributes.annotationType()).isEqualTo(ComponentScan.class);

		Filter[] filters = attributes.getAnnotationArray("excludeFilters", Filter.class);
		assertNotNull(filters);

		List<String> patterns = stream(filters).map(Filter::pattern).collect(toList());
		assertThat(patterns).isEqualTo(asList("*Foo", "*Bar"));
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliases() throws Exception {
		Method method = WebController.class.getMethod("handleMappedWithValueAttribute");
		WebMapping webMapping = method.getAnnotation(WebMapping.class);
		AnnotationAttributes attributes = (AnnotationAttributes) getAnnotationAttributes(webMapping);
		assertNotNull(attributes);
		assertThat(attributes.annotationType()).isEqualTo(WebMapping.class);
		assertThat(attributes.getString("name")).as("name attribute: ").isEqualTo("foo");
		assertThat(attributes.getStringArray(VALUE)).as("value attribute: ").isEqualTo(asArray("/test"));
		assertThat(attributes.getStringArray("path")).as("path attribute: ").isEqualTo(asArray("/test"));

		method = WebController.class.getMethod("handleMappedWithPathAttribute");
		webMapping = method.getAnnotation(WebMapping.class);
		attributes = (AnnotationAttributes) getAnnotationAttributes(webMapping);
		assertNotNull(attributes);
		assertThat(attributes.annotationType()).isEqualTo(WebMapping.class);
		assertThat(attributes.getString("name")).as("name attribute: ").isEqualTo("bar");
		assertThat(attributes.getStringArray(VALUE)).as("value attribute: ").isEqualTo(asArray("/test"));
		assertThat(attributes.getStringArray("path")).as("path attribute: ").isEqualTo(asArray("/test"));
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliasesWithDifferentValues() throws Exception {
		Method method = WebController.class.getMethod("handleMappedWithDifferentPathAndValueAttributes");
		WebMapping webMapping = method.getAnnotation(WebMapping.class);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				getAnnotationAttributes(webMapping))
			.withMessageContaining("attribute 'path' and its alias 'value'")
			.withMessageContaining("values of [{/test}] and [{/enigma}]");
	}

	@Test
	public void getValueFromAnnotation() throws Exception {
		Method method = SimpleFoo.class.getMethod("something", Object.class);
		Order order = findAnnotation(method, Order.class);

		assertThat(getValue(order, VALUE)).isEqualTo(1);
		assertThat(getValue(order)).isEqualTo(1);
	}

	@Test
	public void getValueFromNonPublicAnnotation() throws Exception {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertEquals(1, declaredAnnotations.length);
		Annotation annotation = declaredAnnotations[0];
		assertNotNull(annotation);
		assertThat(annotation.annotationType().getSimpleName()).isEqualTo("NonPublicAnnotation");
		assertThat(getValue(annotation, VALUE)).isEqualTo(42);
		assertThat(getValue(annotation)).isEqualTo(42);
	}

	@Test
	public void getDefaultValueFromAnnotation() throws Exception {
		Method method = SimpleFoo.class.getMethod("something", Object.class);
		Order order = findAnnotation(method, Order.class);

		assertThat(getDefaultValue(order, VALUE)).isEqualTo(Ordered.LOWEST_PRECEDENCE);
		assertThat(getDefaultValue(order)).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	public void getDefaultValueFromNonPublicAnnotation() {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertEquals(1, declaredAnnotations.length);
		Annotation annotation = declaredAnnotations[0];
		assertNotNull(annotation);
		assertThat(annotation.annotationType().getSimpleName()).isEqualTo("NonPublicAnnotation");
		assertThat(getDefaultValue(annotation, VALUE)).isEqualTo(-1);
		assertThat(getDefaultValue(annotation)).isEqualTo(-1);
	}

	@Test
	public void getDefaultValueFromAnnotationType() {
		assertThat(getDefaultValue(Order.class, VALUE)).isEqualTo(Ordered.LOWEST_PRECEDENCE);
		assertThat(getDefaultValue(Order.class)).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	public void findRepeatableAnnotation() {
		Repeatable repeatable = findAnnotation(MyRepeatable.class, Repeatable.class);
		assertNotNull(repeatable);
		assertThat(repeatable.value()).isEqualTo(MyRepeatableContainer.class);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnMethod() throws Exception {
		Method method = InterfaceWithRepeated.class.getMethod("foo");
		Set<MyRepeatable> annotations = getRepeatableAnnotations(method, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(annotations);
		List<String> values = annotations.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(asList("A", "B", "C", "meta1"));
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClassWithMissingAttributeAliasDeclaration() throws Exception {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				getRepeatableAnnotations(BrokenConfigHierarchyTestCase.class, BrokenContextConfig.class, BrokenHierarchy.class))
			.withMessageStartingWith("Attribute 'value' in")
			.withMessageContaining(BrokenContextConfig.class.getName())
			.withMessageContaining("@AliasFor 'location'");

	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClassWithAttributeAliases() {
		final List<String> expectedLocations = asList("A", "B");

		Set<ContextConfig> annotations = getRepeatableAnnotations(ConfigHierarchyTestCase.class, ContextConfig.class, null);
		assertNotNull(annotations);
		assertEquals("size if container type is omitted: ", 0, annotations.size());

		annotations = getRepeatableAnnotations(ConfigHierarchyTestCase.class, ContextConfig.class, Hierarchy.class);
		assertNotNull(annotations);

		List<String> locations = annotations.stream().map(ContextConfig::location).collect(toList());
		assertThat(locations).isEqualTo(expectedLocations);

		List<String> values = annotations.stream().map(ContextConfig::value).collect(toList());
		assertThat(values).isEqualTo(expectedLocations);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClass() {
		final List<String> expectedValuesJava = asList("A", "B", "C");
		final List<String> expectedValuesSpring = asList("A", "B", "C", "meta1");

		// Java 8
		MyRepeatable[] array = MyRepeatableClass.class.getAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		List<String> values = stream(array).map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesJava);

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnSuperclass() {
		final Class<?> clazz = SubMyRepeatableClass.class;
		final List<String> expectedValuesJava = asList("A", "B", "C");
		final List<String> expectedValuesSpring = asList("A", "B", "C", "meta1");

		// Java 8
		MyRepeatable[] array = clazz.getAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		List<String> values = stream(array).map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesJava);

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClassAndSuperclass() {
		final Class<?> clazz = SubMyRepeatableWithAdditionalLocalDeclarationsClass.class;
		final List<String> expectedValuesJava = asList("X", "Y", "Z");
		final List<String> expectedValuesSpring = asList("X", "Y", "Z", "meta2");

		// Java 8
		MyRepeatable[] array = clazz.getAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		List<String> values = stream(array).map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesJava);

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnMultipleSuperclasses() {
		final Class<?> clazz = SubSubMyRepeatableWithAdditionalLocalDeclarationsClass.class;
		final List<String> expectedValuesJava = asList("X", "Y", "Z");
		final List<String> expectedValuesSpring = asList("X", "Y", "Z", "meta2");

		// Java 8
		MyRepeatable[] array = clazz.getAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		List<String> values = stream(array).map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesJava);

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);
	}

	@Test
	public void getDeclaredRepeatableAnnotationsDeclaredOnClass() {
		final List<String> expectedValuesJava = asList("A", "B", "C");
		final List<String> expectedValuesSpring = asList("A", "B", "C", "meta1");

		// Java 8
		MyRepeatable[] array = MyRepeatableClass.class.getDeclaredAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		List<String> values = stream(array).map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesJava);

		// Spring
		Set<MyRepeatable> set = getDeclaredRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);

		// When container type is omitted and therefore inferred from @Repeatable
		set = getDeclaredRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values).isEqualTo(expectedValuesSpring);
	}

	@Test
	public void getDeclaredRepeatableAnnotationsDeclaredOnSuperclass() {
		final Class<?> clazz = SubMyRepeatableClass.class;

		// Java 8
		MyRepeatable[] array = clazz.getDeclaredAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		assertThat(array.length).isEqualTo(0);

		// Spring
		Set<MyRepeatable> set = getDeclaredRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		assertThat(set).hasSize(0);

		// When container type is omitted and therefore inferred from @Repeatable
		set = getDeclaredRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		assertThat(set).hasSize(0);
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithMissingDefaultValues() throws Exception {
		Class<?> clazz = ImplicitAliasesWithMissingDefaultValuesContextConfigClass.class;
		Class<ImplicitAliasesWithMissingDefaultValuesContextConfig> annotationType = ImplicitAliasesWithMissingDefaultValuesContextConfig.class;
		ImplicitAliasesWithMissingDefaultValuesContextConfig config = clazz.getAnnotation(annotationType);
		assertNotNull(config);

		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				synthesizeAnnotation(config, clazz))
			.withMessageStartingWith("Misconfigured aliases:")
			.withMessageContaining("attribute 'location1' in annotation [" + annotationType.getName() + "]")
			.withMessageContaining("attribute 'location2' in annotation [" + annotationType.getName() + "]")
			.withMessageContaining("default values");
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithDifferentDefaultValues() throws Exception {
		Class<?> clazz = ImplicitAliasesWithDifferentDefaultValuesContextConfigClass.class;
		Class<ImplicitAliasesWithDifferentDefaultValuesContextConfig> annotationType = ImplicitAliasesWithDifferentDefaultValuesContextConfig.class;
		ImplicitAliasesWithDifferentDefaultValuesContextConfig config = clazz.getAnnotation(annotationType);
		assertNotNull(config);
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				synthesizeAnnotation(config, clazz))
			.withMessageStartingWith("Misconfigured aliases:")
			.withMessageContaining("attribute 'location1' in annotation [" + annotationType.getName() + "]")
			.withMessageContaining("attribute 'location2' in annotation [" + annotationType.getName() + "]")
			.withMessageContaining("same default value");
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithDuplicateValues() throws Exception {
		Class<?> clazz = ImplicitAliasesWithDuplicateValuesContextConfigClass.class;
		Class<ImplicitAliasesWithDuplicateValuesContextConfig> annotationType = ImplicitAliasesWithDuplicateValuesContextConfig.class;
		ImplicitAliasesWithDuplicateValuesContextConfig config = clazz.getAnnotation(annotationType);
		assertNotNull(config);

		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				synthesizeAnnotation(config, clazz).location1())
			.withMessageStartingWith("Different @AliasFor mirror values")
			.withMessageContaining(annotationType.getName())
			.withMessageContaining("declared on class")
			.withMessageContaining(clazz.getName())
			.withMessageContaining("attribute 'location1' and its alias 'location2'")
			.withMessageContaining("with values of [1] and [2]");
	}

	@Test
	public void synthesizeAnnotationFromMapWithoutAttributeAliases() throws Exception {
		Component component = WebController.class.getAnnotation(Component.class);
		assertNotNull(component);

		Map<String, Object> map = Collections.singletonMap(VALUE, "webController");
		Component synthesizedComponent = synthesizeAnnotation(map, Component.class, WebController.class);
		assertNotNull(synthesizedComponent);

		assertNotSame(component, synthesizedComponent);
		assertThat(component.value()).as("value from component: ").isEqualTo("webController");
		assertThat(synthesizedComponent.value()).as("value from synthesized component: ").isEqualTo("webController");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeAnnotationFromMapWithNestedMap() throws Exception {
		ComponentScanSingleFilter componentScan = ComponentScanSingleFilterClass.class.getAnnotation(ComponentScanSingleFilter.class);
		assertNotNull(componentScan);
		assertThat(componentScan.value().pattern()).as("value from ComponentScan: ").isEqualTo("*Foo");

		AnnotationAttributes attributes = getAnnotationAttributes(
				ComponentScanSingleFilterClass.class, componentScan, false, true);
		assertNotNull(attributes);
		assertThat(attributes.annotationType()).isEqualTo(ComponentScanSingleFilter.class);

		Map<String, Object> filterMap = (Map<String, Object>) attributes.get("value");
		assertNotNull(filterMap);
		assertThat(filterMap.get("pattern")).isEqualTo("*Foo");

		// Modify nested map
		filterMap.put("pattern", "newFoo");
		filterMap.put("enigma", 42);

		ComponentScanSingleFilter synthesizedComponentScan = synthesizeAnnotation(
				attributes, ComponentScanSingleFilter.class, ComponentScanSingleFilterClass.class);
		assertNotNull(synthesizedComponentScan);

		assertNotSame(componentScan, synthesizedComponentScan);
		assertThat(synthesizedComponentScan.value().pattern()).as("value from synthesized ComponentScan: ").isEqualTo("newFoo");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeAnnotationFromMapWithNestedArrayOfMaps() throws Exception {
		ComponentScan componentScan = ComponentScanClass.class.getAnnotation(ComponentScan.class);
		assertNotNull(componentScan);

		AnnotationAttributes attributes = getAnnotationAttributes(ComponentScanClass.class, componentScan, false, true);
		assertNotNull(attributes);
		assertThat(attributes.annotationType()).isEqualTo(ComponentScan.class);

		Map<String, Object>[] filters = (Map[]) attributes.get("excludeFilters");
		assertNotNull(filters);

		List<String> patterns = stream(filters).map(m -> (String) m.get("pattern")).collect(toList());
		assertThat(patterns).isEqualTo(asList("*Foo", "*Bar"));

		// Modify nested maps
		filters[0].put("pattern", "newFoo");
		filters[0].put("enigma", 42);
		filters[1].put("pattern", "newBar");
		filters[1].put("enigma", 42);

		ComponentScan synthesizedComponentScan = synthesizeAnnotation(attributes, ComponentScan.class, ComponentScanClass.class);
		assertNotNull(synthesizedComponentScan);

		assertNotSame(componentScan, synthesizedComponentScan);
		patterns = stream(synthesizedComponentScan.excludeFilters()).map(Filter::pattern).collect(toList());
		assertThat(patterns).isEqualTo(asList("newFoo", "newBar"));
	}

	@Test
	public void synthesizeAnnotationFromDefaultsWithoutAttributeAliases() throws Exception {
		AnnotationWithDefaults annotationWithDefaults = synthesizeAnnotation(AnnotationWithDefaults.class);
		assertNotNull(annotationWithDefaults);
		assertThat(annotationWithDefaults.text()).as("text: ").isEqualTo("enigma");
		assertThat(annotationWithDefaults.predicate()).as("predicate: ").isTrue();
		assertThat(annotationWithDefaults.characters()).as("characters: ").isEqualTo(new char[] { 'a', 'b', 'c' });
	}

	@Test
	public void synthesizeAnnotationFromDefaultsWithAttributeAliases() throws Exception {
		ContextConfig contextConfig = synthesizeAnnotation(ContextConfig.class);
		assertNotNull(contextConfig);
		assertThat(contextConfig.value()).as("value: ").isEqualTo("");
		assertThat(contextConfig.location()).as("location: ").isEqualTo("");
	}

	@Test
	public void synthesizeAnnotationFromMapWithMinimalAttributesWithAttributeAliases() throws Exception {
		Map<String, Object> map = Collections.singletonMap("location", "test.xml");
		ContextConfig contextConfig = synthesizeAnnotation(map, ContextConfig.class, null);
		assertNotNull(contextConfig);
		assertThat(contextConfig.value()).as("value: ").isEqualTo("test.xml");
		assertThat(contextConfig.location()).as("location: ").isEqualTo("test.xml");
	}

	@Test
	public void synthesizeAnnotationFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements() throws Exception {
		Map<String, Object> map = Collections.singletonMap("value", "/foo");
		Get get = synthesizeAnnotation(map, Get.class, null);
		assertNotNull(get);
		assertThat(get.value()).as("value: ").isEqualTo("/foo");
		assertThat(get.path()).as("path: ").isEqualTo("/foo");

		map = Collections.singletonMap("path", "/foo");
		get = synthesizeAnnotation(map, Get.class, null);
		assertNotNull(get);
		assertThat(get.value()).as("value: ").isEqualTo("/foo");
		assertThat(get.path()).as("path: ").isEqualTo("/foo");
	}

	@Test
	public void synthesizeAnnotationFromMapWithImplicitAttributeAliases() throws Exception {
		assertAnnotationSynthesisFromMapWithImplicitAliases("value");
		assertAnnotationSynthesisFromMapWithImplicitAliases("location1");
		assertAnnotationSynthesisFromMapWithImplicitAliases("location2");
		assertAnnotationSynthesisFromMapWithImplicitAliases("location3");
		assertAnnotationSynthesisFromMapWithImplicitAliases("xmlFile");
		assertAnnotationSynthesisFromMapWithImplicitAliases("groovyScript");
	}

	private void assertAnnotationSynthesisFromMapWithImplicitAliases(String attributeNameAndValue) throws Exception {
		Map<String, Object> map = Collections.singletonMap(attributeNameAndValue, attributeNameAndValue);
		ImplicitAliasesContextConfig config = synthesizeAnnotation(map, ImplicitAliasesContextConfig.class, null);
		assertNotNull(config);
		assertThat(config.value()).as("value: ").isEqualTo(attributeNameAndValue);
		assertThat(config.location1()).as("location1: ").isEqualTo(attributeNameAndValue);
		assertThat(config.location2()).as("location2: ").isEqualTo(attributeNameAndValue);
		assertThat(config.location3()).as("location3: ").isEqualTo(attributeNameAndValue);
		assertThat(config.xmlFile()).as("xmlFile: ").isEqualTo(attributeNameAndValue);
		assertThat(config.groovyScript()).as("groovyScript: ").isEqualTo(attributeNameAndValue);
	}

	@Test
	public void synthesizeAnnotationFromMapWithMissingAttributeValue() throws Exception {
		assertMissingTextAttribute(Collections.emptyMap());
	}

	@Test
	public void synthesizeAnnotationFromMapWithNullAttributeValue() throws Exception {
		Map<String, Object> map = Collections.singletonMap("text", null);
		assertThat(map.containsKey("text")).isTrue();
		assertMissingTextAttribute(map);
	}

	private void assertMissingTextAttribute(Map<String, Object> attributes) {
		assertThatIllegalArgumentException().isThrownBy(() ->
				synthesizeAnnotation(attributes, AnnotationWithoutDefaults.class, null))
			.withMessageContaining("No value found for attribute named 'text' in merged annotation");
	}

	@Test
	public void synthesizeAnnotationFromMapWithAttributeOfIncorrectType() throws Exception {
		Map<String, Object> map = Collections.singletonMap(VALUE, 42L);
		assertThatIllegalArgumentException().isThrownBy(() ->
				synthesizeAnnotation(map, Component.class, null))
			.withMessageContaining("Attribute 'value' in annotation org.springframework.stereotype.Component "
					+ "should be compatible with java.lang.String but a java.lang.Long value was returned");
	}

	@Test
	public void synthesizeAnnotationFromAnnotationAttributesWithoutAttributeAliases() throws Exception {
		// 1) Get an annotation
		Component component = WebController.class.getAnnotation(Component.class);
		assertNotNull(component);

		// 2) Convert the annotation into AnnotationAttributes
		AnnotationAttributes attributes = getAnnotationAttributes(WebController.class, component);
		assertNotNull(attributes);

		// 3) Synthesize the AnnotationAttributes back into an annotation
		Component synthesizedComponent = synthesizeAnnotation(attributes, Component.class, WebController.class);
		assertNotNull(synthesizedComponent);

		// 4) Verify that the original and synthesized annotations are equivalent
		assertNotSame(component, synthesizedComponent);
		assertThat(synthesizedComponent).isEqualTo(component);
		assertThat(component.value()).as("value from component: ").isEqualTo("webController");
		assertThat(synthesizedComponent.value()).as("value from synthesized component: ").isEqualTo("webController");
	}

	@Test // gh-22702
	public void findAnnotationWithRepeatablesElements() {
		assertNull(AnnotationUtils.findAnnotation(TestRepeatablesClass.class,
				TestRepeatable.class));
		assertNotNull(AnnotationUtils.findAnnotation(TestRepeatablesClass.class,
				TestRepeatableContainer.class));
	}

	@SafeVarargs
	static <T> T[] asArray(T... arr) {
		return arr;
	}


	@Component("meta1")
	@Order
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Meta1 {
	}

	@Component("meta2")
	@Transactional(readOnly = true)
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
	static class ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
	}

	@Meta1
	static class ClassWithInheritedMetaAnnotation {
	}

	@Meta2
	static class SubClassWithInheritedMetaAnnotation extends ClassWithInheritedMetaAnnotation {
	}

	static class SubSubClassWithInheritedMetaAnnotation extends SubClassWithInheritedMetaAnnotation {
	}

	@Transactional
	static class ClassWithInheritedAnnotation {
	}

	@Meta2
	static class SubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
	}

	static class SubSubClassWithInheritedAnnotation extends SubClassWithInheritedAnnotation {
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
	@interface Transactional {

		boolean readOnly() default false;
	}

	public static abstract class Foo<T> {

		@Order(1)
		public abstract void something(T arg);
	}

	public static class SimpleFoo extends Foo<String> {

		@Override
		@Transactional
		public void something(final String arg) {
		}
	}

	@Transactional
	public interface InheritedAnnotationInterface {
	}

	public interface SubInheritedAnnotationInterface extends InheritedAnnotationInterface {
	}

	public interface SubSubInheritedAnnotationInterface extends SubInheritedAnnotationInterface {
	}

	@Order
	public interface NonInheritedAnnotationInterface {
	}

	public interface SubNonInheritedAnnotationInterface extends NonInheritedAnnotationInterface {
	}

	public interface SubSubNonInheritedAnnotationInterface extends SubNonInheritedAnnotationInterface {
	}

	public static class NonAnnotatedClass {
	}

	public interface NonAnnotatedInterface {
	}

	@Transactional
	public static class InheritedAnnotationClass {
	}

	public static class SubInheritedAnnotationClass extends InheritedAnnotationClass {
	}

	@Order
	public static class NonInheritedAnnotationClass {
	}

	public static class SubNonInheritedAnnotationClass extends NonInheritedAnnotationClass {
	}

	@Transactional
	public static class TransactionalClass {
	}

	@Order
	public static class TransactionalAndOrderedClass extends TransactionalClass {
	}

	public static class SubTransactionalAndOrderedClass extends TransactionalAndOrderedClass {
	}

	public interface InterfaceWithAnnotatedMethod {

		@Order
		void foo();
	}

	public static class ImplementsInterfaceWithAnnotatedMethod implements InterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public static class SubOfImplementsInterfaceWithAnnotatedMethod extends ImplementsInterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public abstract static class AbstractDoesNotImplementInterfaceWithAnnotatedMethod
			implements InterfaceWithAnnotatedMethod {
	}

	public static class SubOfAbstractImplementsInterfaceWithAnnotatedMethod
			extends AbstractDoesNotImplementInterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public interface InterfaceWithGenericAnnotatedMethod<T> {

		@Order
		void foo(T t);
	}

	public static class ImplementsInterfaceWithGenericAnnotatedMethod implements InterfaceWithGenericAnnotatedMethod<String> {

		public void foo(String t) {
		}
	}

	public static abstract class BaseClassWithGenericAnnotatedMethod<T> {

		@Order
		abstract void foo(T t);
	}

	public static class ExtendsBaseClassWithGenericAnnotatedMethod extends BaseClassWithGenericAnnotatedMethod<String> {

		public void foo(String t) {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface MyRepeatableContainer {

		MyRepeatable[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Repeatable(MyRepeatableContainer.class)
	@interface MyRepeatable {

		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@MyRepeatable("meta1")
	@interface MyRepeatableMeta1 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@MyRepeatable("meta2")
	@interface MyRepeatableMeta2 {
	}

	interface InterfaceWithRepeated {

		@MyRepeatable("A")
		@MyRepeatableContainer({@MyRepeatable("B"), @MyRepeatable("C")})
		@MyRepeatableMeta1
		void foo();
	}

	@MyRepeatable("A")
	@MyRepeatableContainer({@MyRepeatable("B"), @MyRepeatable("C")})
	@MyRepeatableMeta1
	static class MyRepeatableClass {
	}

	static class SubMyRepeatableClass extends MyRepeatableClass {
	}

	@MyRepeatable("X")
	@MyRepeatableContainer({@MyRepeatable("Y"), @MyRepeatable("Z")})
	@MyRepeatableMeta2
	static class SubMyRepeatableWithAdditionalLocalDeclarationsClass extends MyRepeatableClass {
	}

	static class SubSubMyRepeatableWithAdditionalLocalDeclarationsClass extends
			SubMyRepeatableWithAdditionalLocalDeclarationsClass {
	}

	enum RequestMethod {
		GET, POST
	}

	/**
	 * Mock of {@code org.springframework.web.bind.annotation.RequestMapping}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface WebMapping {

		String name();

		@AliasFor("path")
		String[] value() default "";

		@AliasFor(attribute = "value")
		String[] path() default "";

		RequestMethod[] method() default {};
	}

	/**
	 * Mock of {@code org.springframework.web.bind.annotation.GetMapping}, except
	 * that the String arrays are overridden with single String elements.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@WebMapping(method = RequestMethod.GET, name = "")
	@interface Get {

		@AliasFor(annotation = WebMapping.class)
		String value() default "";

		@AliasFor(annotation = WebMapping.class)
		String path() default "";
	}

	/**
	 * Mock of {@code org.springframework.web.bind.annotation.PostMapping}, except
	 * that the path is overridden by convention with single String element.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@WebMapping(method = RequestMethod.POST, name = "")
	@interface Post {

		String path() default "";
	}

	@Component("webController")
	static class WebController {

		@WebMapping(value = "/test", name = "foo")
		public void handleMappedWithValueAttribute() {
		}

		@WebMapping(path = "/test", name = "bar", method = { RequestMethod.GET, RequestMethod.POST })
		public void handleMappedWithPathAttribute() {
		}

		@Get("/test")
		public void getMappedWithValueAttribute() {
		}

		@Get(path = "/test")
		public void getMappedWithPathAttribute() {
		}

		@Post(path = "/test")
		public void postMappedWithPathAttribute() {
		}

		/**
		 * mapping is logically "equal" to handleMappedWithPathAttribute().
		 */
		@WebMapping(value = "/test", path = "/test", name = "bar", method = { RequestMethod.GET, RequestMethod.POST })
		public void handleMappedWithSamePathAndValueAttributes() {
		}

		@WebMapping(value = "/enigma", path = "/test", name = "baz")
		public void handleMappedWithDifferentPathAndValueAttributes() {
		}
	}

	/**
	 * Mock of {@code org.springframework.test.context.ContextConfiguration}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextConfig {

		@AliasFor("location")
		String value() default "";

		@AliasFor("value")
		String location() default "";

		Class<?> klass() default Object.class;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface BrokenContextConfig {

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
	@interface Hierarchy {
		ContextConfig[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface BrokenHierarchy {
		BrokenContextConfig[] value();
	}

	@Hierarchy({@ContextConfig("A"), @ContextConfig(location = "B")})
	static class ConfigHierarchyTestCase {
	}

	@BrokenHierarchy(@BrokenContextConfig)
	static class BrokenConfigHierarchyTestCase {
	}

	@ContextConfig("simple.xml")
	static class SimpleConfigTestCase {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface CharsContainer {

		@AliasFor(attribute = "chars")
		char[] value() default {};

		@AliasFor(attribute = "value")
		char[] chars() default {};
	}

	@CharsContainer(chars = { 'x', 'y', 'z' })
	static class GroupOfCharsClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForWithMissingAttributeDeclaration {

		@AliasFor
		String foo() default "";
	}

	@AliasForWithMissingAttributeDeclaration
	static class AliasForWithMissingAttributeDeclarationClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForWithDuplicateAttributeDeclaration {

		@AliasFor(value = "bar", attribute = "baz")
		String foo() default "";
	}

	@AliasForWithDuplicateAttributeDeclaration
	static class AliasForWithDuplicateAttributeDeclarationClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForNonexistentAttribute {

		@AliasFor("bar")
		String foo() default "";
	}

	@AliasForNonexistentAttribute
	static class AliasForNonexistentAttributeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForWithoutMirroredAliasFor {

		@AliasFor("bar")
		String foo() default "";

		String bar() default "";
	}

	@AliasForWithoutMirroredAliasFor
	static class AliasForWithoutMirroredAliasForClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForWithMirroredAliasForWrongAttribute {

		@AliasFor(attribute = "bar")
		String[] foo() default "";

		@AliasFor(attribute = "quux")
		String[] bar() default "";
	}

	@AliasForWithMirroredAliasForWrongAttribute
	static class AliasForWithMirroredAliasForWrongAttributeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForAttributeOfDifferentType {

		@AliasFor("bar")
		String[] foo() default "";

		@AliasFor("foo")
		boolean bar() default true;
	}

	@AliasForAttributeOfDifferentType
	static class AliasForAttributeOfDifferentTypeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForWithMissingDefaultValues {

		@AliasFor(attribute = "bar")
		String foo();

		@AliasFor(attribute = "foo")
		String bar();
	}

	@AliasForWithMissingDefaultValues(foo = "foo", bar = "bar")
	static class AliasForWithMissingDefaultValuesClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasForAttributeWithDifferentDefaultValue {

		@AliasFor("bar")
		String foo() default "X";

		@AliasFor("foo")
		String bar() default "Z";
	}

	@AliasForAttributeWithDifferentDefaultValue
	static class AliasForAttributeWithDifferentDefaultValueClass {
	}

	// @ContextConfig --> Intentionally NOT meta-present
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedComposedContextConfigNotMetaPresent {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String xmlConfigFile();
	}

	@AliasedComposedContextConfigNotMetaPresent(xmlConfigFile = "test.xml")
	static class AliasedComposedContextConfigNotMetaPresentClass {
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String xmlConfigFile();
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ImplicitAliasesContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String xmlFile() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String groovyScript() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String value() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location1() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location2() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location3() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "klass")
		Class<?> configClass() default Object.class;

		String nonAliasedAttribute() default "";
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesContextConfig(groovyScript = "groovyScript")
	static class GroovyImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesContextConfig(xmlFile = "xmlFile")
	static class XmlImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesContextConfig("value")
	static class ValueImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesContextConfig(location1 = "location1")
	static class Location1ImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesContextConfig(location2 = "location2")
	static class Location2ImplicitAliasesContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesContextConfig(location3 = "location3")
	static class Location3ImplicitAliasesContextConfigClass {
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig {

		// intentionally omitted: attribute = "value"
		@AliasFor(annotation = ContextConfig.class)
		String value() default "";

		// intentionally omitted: attribute = "locations"
		@AliasFor(annotation = ContextConfig.class)
		String location() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String xmlFile() default "";
	}

	@ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface TransitiveImplicitAliasesWithImpliedAliasNamesOmittedContextConfig {

		@AliasFor(annotation = ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class, attribute = "xmlFile")
		String xml() default "";

		@AliasFor(annotation = ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class, attribute = "location")
		String groovy() default "";
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig("value")
	static class ValueImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig(location = "location")
	static class LocationsImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig(xmlFile = "xmlFile")
	static class XmlFilesImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass {
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ImplicitAliasesWithMissingDefaultValuesContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location1();

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location2();
	}

	@ImplicitAliasesWithMissingDefaultValuesContextConfig(location1 = "1", location2 = "2")
	static class ImplicitAliasesWithMissingDefaultValuesContextConfigClass {
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ImplicitAliasesWithDifferentDefaultValuesContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location1() default "foo";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location2() default "bar";
	}

	@ImplicitAliasesWithDifferentDefaultValuesContextConfig(location1 = "1", location2 = "2")
	static class ImplicitAliasesWithDifferentDefaultValuesContextConfigClass {
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ImplicitAliasesWithDuplicateValuesContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location1() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String location2() default "";
	}

	@ImplicitAliasesWithDuplicateValuesContextConfig(location1 = "1", location2 = "2")
	static class ImplicitAliasesWithDuplicateValuesContextConfigClass {
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ImplicitAliasesForAliasPairContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String xmlFile() default "";

		@AliasFor(annotation = ContextConfig.class, value = "value")
		String groovyScript() default "";
	}

	@ImplicitAliasesForAliasPairContextConfig(xmlFile = "test.xml")
	static class ImplicitAliasesForAliasPairContextConfigClass {
	}

	@ImplicitAliasesContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface TransitiveImplicitAliasesContextConfig {

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "xmlFile")
		String xml() default "";

		@AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScript")
		String groovy() default "";
	}

	@TransitiveImplicitAliasesContextConfig(xml = "test.xml")
	static class TransitiveImplicitAliasesContextConfigClass {
	}

	@ImplicitAliasesForAliasPairContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface TransitiveImplicitAliasesForAliasPairContextConfig {

		@AliasFor(annotation = ImplicitAliasesForAliasPairContextConfig.class, attribute = "xmlFile")
		String xml() default "";

		@AliasFor(annotation = ImplicitAliasesForAliasPairContextConfig.class, attribute = "groovyScript")
		String groovy() default "";
	}

	@TransitiveImplicitAliasesForAliasPairContextConfig(xml = "test.xml")
	static class TransitiveImplicitAliasesForAliasPairContextConfigClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {
		String pattern();
	}

	/**
	 * Mock of {@code org.springframework.context.annotation.ComponentScan}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComponentScan {
		Filter[] excludeFilters() default {};
	}

	@ComponentScan(excludeFilters = {@Filter(pattern = "*Foo"), @Filter(pattern = "*Bar")})
	static class ComponentScanClass {
	}

	/**
	 * Mock of {@code org.springframework.context.annotation.ComponentScan}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComponentScanSingleFilter {
		Filter value();
	}

	@ComponentScanSingleFilter(@Filter(pattern = "*Foo"))
	static class ComponentScanSingleFilterClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AnnotationWithDefaults {
		String text() default "enigma";
		boolean predicate() default true;
		char[] characters() default {'a', 'b', 'c'};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AnnotationWithoutDefaults {
		String text();
	}

	@ContextConfig(value = "foo", location = "bar")
	interface ContextConfigMismatch {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(TestRepeatableContainer.class)
	static @interface TestRepeatable {

		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface TestRepeatableContainer {

		TestRepeatable[] value();
	}

	@TestRepeatable("a")
	@TestRepeatable("b")
	static class TestRepeatablesClass {
	}
}
