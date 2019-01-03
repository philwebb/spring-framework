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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.subpackage.NonPublicAnnotatedClass;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.annotation.AnnotationUtils.VALUE;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.getAttributeAliasNames;
import static org.springframework.core.annotation.AnnotationUtils.getAttributeOverrideName;
import static org.springframework.core.annotation.AnnotationUtils.getDeclaredRepeatableAnnotations;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.core.annotation.AnnotationUtils.getRepeatableAnnotations;
import static org.springframework.core.annotation.AnnotationUtils.getValue;
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
public class XAnnotationUtilsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Before
	public void clearCacheBeforeTests() {
		AnnotationUtils.clearCache();
	}


	@Test
	public void findMethodAnnotationOnLeaf() throws Exception {
		Method method = Leaf.class.getMethod("annotatedOnLeaf");
		assertThat(method.getAnnotation(Order.class)).isNotNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationWithAnnotationOnMethodInInterface() throws Exception {
		Method method = Leaf.class.getMethod("fromInterfaceImplementedByRoot");
		// @Order is not @Inherited
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationWithMetaAnnotationOnLeaf() throws Exception {
		Method method = Leaf.class.getMethod("metaAnnotatedOnLeaf");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(1);
	}

	@Test
	public void findMethodAnnotationWithMetaMetaAnnotationOnLeaf() throws Exception {
		Method method = Leaf.class.getMethod("metaMetaAnnotatedOnLeaf");
		assertThat(method.getAnnotation(Component.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Component.class).getDepth()).isEqualTo(2);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Component.class).getDepth()).isEqualTo(2);
	}

	@Test
	public void findMethodAnnotationOnRoot() throws Exception {
		Method method = Leaf.class.getMethod("annotatedOnRoot");
		assertThat(method.getAnnotation(Order.class)).isNotNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationWithMetaAnnotationOnRoot() throws Exception {
		Method method = Leaf.class.getMethod("metaAnnotatedOnRoot");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(1);
	}

	@Test
	public void findMethodAnnotationOnRootButOverridden() throws Exception {
		Method method = Leaf.class.getMethod("overrideWithoutNewAnnotation");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationNotAnnotated() throws Exception {
		Method method = Leaf.class.getMethod("notAnnotated");
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(-1);
	}

	@Test
	public void findMethodAnnotationOnBridgeMethod() throws Exception {
		Method method = SimpleFoo.class.getMethod("something", Object.class);
		assertThat(method.isBridge()).isTrue();

		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);

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
			assertThat(method.getAnnotation(Transactional.class)).isNotNull();
		}
		assertThat(MergedAnnotations.from(method).get(Transactional.class).getDepth()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Transactional.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationOnBridgedMethod() throws Exception {
		Method method = SimpleFoo.class.getMethod("something", String.class);
		assertThat(method.isBridge()).isFalse();
		assertThat(method.getAnnotation(Order.class)).isNull();
		assertThat(MergedAnnotations.from(method).get(Order.class).getDepth()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
		assertThat(method.getAnnotation(Transactional.class)).isNotNull();
		assertThat(MergedAnnotations.from(method).get(Transactional.class).getDepth()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Transactional.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationFromInterface() throws Exception {
		Method method = ImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test  // SPR-16060
	public void findMethodAnnotationFromGenericInterface() throws Exception {
		Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test  // SPR-17146
	public void findMethodAnnotationFromGenericSuperclass() throws Exception {
		Method method = ExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationFromInterfaceOnSuper() throws Exception {
		Method method = SubOfImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findMethodAnnotationFromInterfaceWhenSuperDoesNotImplementMethod() throws Exception {
		Method method = SubOfAbstractImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		assertThat(MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class).getDepth()).isEqualTo(0);
	}

	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverAnnotationsOnInterfaces() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, SubSubClassWithInheritedAnnotation.class).get(Transactional.class);
		assertThat(annotation.getBoolean("readOnly")).isTrue();
	}

	@Test
	public void findClassAnnotationFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedComposedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, SubSubClassWithInheritedMetaAnnotation.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnMetaMetaAnnotatedClass() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, MetaMetaAnnotatedClass.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnMetaMetaMetaAnnotatedClass() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, MetaMetaMetaAnnotatedClass.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("meta2");
	}

	@Test
	public void findClassAnnotationOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// TransactionalClass is NOT annotated or meta-annotated with @Component
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, TransactionalClass.class).get(Component.class);
		assertThat(annotation.isPresent()).isFalse();
	}

	@Test
	public void findClassAnnotationOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, MetaCycleAnnotatedClass.class).get(Component.class);
		assertThat(annotation.isPresent()).isFalse();
	}

	@Test
	public void findClassAnnotationOnInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, InheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findClassAnnotationOnSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, SubInheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findClassAnnotationOnSubSubInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, SubSubInheritedAnnotationInterface.class).get(Transactional.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findClassAnnotationOnNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, NonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(0);
	}

	@Test
	public void findClassAnnotationOnSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, SubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(1);
	}

	@Test
	public void findClassAnnotationOnSubSubNonInheritedAnnotationInterface() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, SubSubNonInheritedAnnotationInterface.class).get(Order.class);
		assertThat(annotation.getAggregateIndex()).isEqualTo(2);
	}

	@Test
	public void findAnnotationDeclaringClassForAllScenarios() {
		// no class-level annotation
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				NonAnnotatedInterface.class).get(
						Transactional.class).getSource()).isNull();
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				NonAnnotatedClass.class).get(Transactional.class).getSource()).isNull();
		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				InheritedAnnotationInterface.class).get(
						Transactional.class).getSource()).isEqualTo(
								InheritedAnnotationInterface.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				SubInheritedAnnotationInterface.class).get(
						Transactional.class).getSource()).isNull();
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				InheritedAnnotationClass.class).get(
						Transactional.class).getSource()).isEqualTo(
								InheritedAnnotationClass.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				SubInheritedAnnotationClass.class).get(
						Transactional.class).getSource()).isEqualTo(
								InheritedAnnotationClass.class);
		// non-inherited class-level annotation; note: @Order is not inherited,
		// but we should still find it on classes.
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				NonInheritedAnnotationInterface.class).get(
						Order.class).getSource()).isEqualTo(
								NonInheritedAnnotationInterface.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				SubNonInheritedAnnotationInterface.class).get(
						Order.class).getSource()).isNull();
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				NonInheritedAnnotationClass.class).get(
						Order.class).getSource()).isEqualTo(
								NonInheritedAnnotationClass.class);
		assertThat(MergedAnnotations.from(SearchStrategy.SUPER_CLASS,
				SubNonInheritedAnnotationClass.class).get(
						Order.class).getSource()).isEqualTo(
								NonInheritedAnnotationClass.class);
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithSingleCandidateType() {
		// no class-level annotation
		List<Class<? extends Annotation>> transactionalCandidateList = Collections.singletonList(
				Transactional.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(NonAnnotatedInterface.class,
				transactionalCandidateList)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(NonAnnotatedClass.class,
				transactionalCandidateList)).isNull();
		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(getDirectlyPresentSourceWithTypeIn(InheritedAnnotationInterface.class,
				transactionalCandidateList)).isEqualTo(
						InheritedAnnotationInterface.class);
		assertThat(
				getDirectlyPresentSourceWithTypeIn(SubInheritedAnnotationInterface.class,
						transactionalCandidateList)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(InheritedAnnotationClass.class,
				transactionalCandidateList)).isEqualTo(InheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(SubInheritedAnnotationClass.class,
				transactionalCandidateList)).isEqualTo(InheritedAnnotationClass.class);
		// non-inherited class-level annotation; note: @Order is not inherited,
		// but should still find it on classes.
		List<Class<? extends Annotation>> orderCandidateList = Collections.singletonList(
				Order.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				NonInheritedAnnotationInterface.class, orderCandidateList)).isEqualTo(
						NonInheritedAnnotationInterface.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				SubNonInheritedAnnotationInterface.class, orderCandidateList)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(NonInheritedAnnotationClass.class,
				orderCandidateList)).isEqualTo(NonInheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				SubNonInheritedAnnotationClass.class, orderCandidateList)).isEqualTo(
						NonInheritedAnnotationClass.class);
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithMultipleCandidateTypes() {
		List<Class<? extends Annotation>> candidates = asList(Transactional.class,
				Order.class);

		// no class-level annotation
		assertThat(getDirectlyPresentSourceWithTypeIn(NonAnnotatedInterface.class,
				candidates)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(NonAnnotatedClass.class,
				candidates)).isNull();

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(getDirectlyPresentSourceWithTypeIn(InheritedAnnotationInterface.class,
				candidates)).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				SubInheritedAnnotationInterface.class, candidates)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(InheritedAnnotationClass.class,
				candidates)).isEqualTo(InheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(SubInheritedAnnotationClass.class,
				candidates)).isEqualTo(InheritedAnnotationClass.class);

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClassForTypes() should still find it on
		// classes.
		assertThat(getDirectlyPresentSourceWithTypeIn(
				NonInheritedAnnotationInterface.class, candidates)).isEqualTo(
						NonInheritedAnnotationInterface.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				SubNonInheritedAnnotationInterface.class, candidates)).isNull();
		assertThat(getDirectlyPresentSourceWithTypeIn(NonInheritedAnnotationClass.class,
				candidates)).isEqualTo(NonInheritedAnnotationClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				SubNonInheritedAnnotationClass.class, candidates)).isEqualTo(
						NonInheritedAnnotationClass.class);

		// class hierarchy mixed with @Transactional and @Order declarations
		assertThat(getDirectlyPresentSourceWithTypeIn(TransactionalClass.class,
				candidates)).isEqualTo(TransactionalClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(TransactionalAndOrderedClass.class,
				candidates)).isEqualTo(TransactionalAndOrderedClass.class);
		assertThat(getDirectlyPresentSourceWithTypeIn(
				SubTransactionalAndOrderedClass.class, candidates)).isEqualTo(
						TransactionalAndOrderedClass.class);
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
		assertThat(MergedAnnotations.from(NonAnnotatedInterface.class).get(Transactional.class).isDirectlyPresent()).isFalse();
		assertThat(MergedAnnotations.from(NonAnnotatedClass.class).get(Transactional.class).isDirectlyPresent()).isFalse();

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(MergedAnnotations.from(InheritedAnnotationInterface.class).get(Transactional.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(SubInheritedAnnotationInterface.class).get(Transactional.class).isDirectlyPresent()).isFalse();
		assertThat(MergedAnnotations.from(InheritedAnnotationClass.class).get(Transactional.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(SubInheritedAnnotationClass.class).get(Transactional.class).isDirectlyPresent()).isFalse();

		// non-inherited class-level annotation; note: @Order is not inherited
		assertThat(MergedAnnotations.from(NonInheritedAnnotationInterface.class).get(Order.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(SubNonInheritedAnnotationInterface.class).get(Order.class).isDirectlyPresent()).isFalse();
		assertThat(MergedAnnotations.from(NonInheritedAnnotationClass.class).get(Order.class).isDirectlyPresent()).isTrue();
		assertThat(MergedAnnotations.from(SubNonInheritedAnnotationClass.class).get(Order.class).isDirectlyPresent()).isFalse();
	}

	@Test
	public void isAnnotationInheritedForAllScenarios() {
		// no class-level annotation
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, NonAnnotatedInterface.class).get(Transactional.class).getAggregateIndex()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, NonAnnotatedClass.class).get(Transactional.class).getAggregateIndex()).isEqualTo(-1);

		// inherited class-level annotation; note: @Transactional is inherited
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, InheritedAnnotationInterface.class).get(Transactional.class).getAggregateIndex()).isEqualTo(0);
		// Since we're not traversing interface hierarchies the following, though perhaps counter intuitive, must be false:
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, SubInheritedAnnotationInterface.class).get(Transactional.class).getAggregateIndex()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, InheritedAnnotationClass.class).get(Transactional.class).getAggregateIndex()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, SubInheritedAnnotationClass.class).get(Transactional.class).getAggregateIndex()).isEqualTo(1);

		// non-inherited class-level annotation; note: @Order is not inherited
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, NonInheritedAnnotationInterface.class).get(Order.class).getAggregateIndex()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, SubNonInheritedAnnotationInterface.class).get(Order.class).getAggregateIndex()).isEqualTo(-1);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, NonInheritedAnnotationClass.class).get(Order.class).getAggregateIndex()).isEqualTo(0);
		assertThat(MergedAnnotations.from(SearchStrategy.INHERITED_ANNOTATIONS, SubNonInheritedAnnotationClass.class).get(Order.class).getAggregateIndex()).isEqualTo(-1);
	}

	@Test
	public void getAnnotationAttributesWithoutAttributeAliases() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(WebController.class).get(Component.class);
		assertThat(annotation.getString("value")).isEqualTo("webController");
	}

	@Test
	public void getAnnotationAttributesWithNestedAnnotations() {
		MergedAnnotation<?> annotation = MergedAnnotations.from(ComponentScanClass.class).get(ComponentScan.class);
		MergedAnnotation<Filter>[] filters = annotation.getAnnotationArray("excludeFilters", Filter.class);
		assertThat(Arrays.stream(filters).map(filter->filter.getString("pattern"))).containsExactly("*Foo", "*Bar");
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliases1() throws Exception {
		Method method = WebController.class.getMethod("handleMappedWithValueAttribute");
		MergedAnnotation<?> annotation = MergedAnnotations.from(method).get(WebMapping.class);
		assertThat(annotation.getString("name")).isEqualTo("foo");
		assertThat(annotation.getStringArray("value")).containsExactly("/test");
		assertThat(annotation.getStringArray("path")).containsExactly("/test");
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliases2() throws Exception {
		Method method = WebController.class.getMethod("handleMappedWithPathAttribute");
		MergedAnnotation<?> annotation = MergedAnnotations.from(method).get(WebMapping.class);
		assertThat(annotation.getString("name")).isEqualTo("bar");
		assertThat(annotation.getStringArray("value")).containsExactly("/test");
		assertThat(annotation.getStringArray("path")).containsExactly("/test");
	}

	@Test
	public void getAnnotationAttributesWithAttributeAliasesWithDifferentValues() throws Exception {
		Method method = WebController.class.getMethod(
				"handleMappedWithDifferentPathAndValueAttributes");
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> MergedAnnotations.from(method).get(
						WebMapping.class)).withMessageContaining(
								"attribute 'value' and its alias 'path'").withMessageContaining(
										"values of [{/enigma}] and [{/test}]");
	}

	@Test
	public void getValueFromAnnotation() throws Exception {
		Method method = SimpleFoo.class.getMethod("something", Object.class);
		MergedAnnotation<?> annotation = MergedAnnotations.from(SearchStrategy.EXHAUSTIVE, method).get(Order.class);
		assertThat(annotation.getInt("value")).isEqualTo(1);
	}

	@Test
	public void getValueFromNonPublicAnnotation() throws Exception {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertEquals(1, declaredAnnotations.length);
		Annotation annotation = declaredAnnotations[0];
		MergedAnnotation<Annotation> mergedAnnotation = MergedAnnotation.of(annotation);
		assertThat(mergedAnnotation.getType()).contains("NonPublicAnnotation");
		assertThat(mergedAnnotation.synthesize().annotationType().getSimpleName()).isEqualTo("NonPublicAnnotation");
		assertThat(mergedAnnotation.getInt("value")).isEqualTo(42);
	}

	@Test
	public void getDefaultValueFromAnnotation() throws Exception {

		Method method = SimpleFoo.class.getMethod("something", Object.class);

		MergedAnnotation<Order> annotation = MergedAnnotations.from(
				SearchStrategy.EXHAUSTIVE, method).get(Order.class);

		Order order = findAnnotation(method, Order.class);

		assertEquals(Ordered.LOWEST_PRECEDENCE, getDefaultValue(order, VALUE));
		assertEquals(Ordered.LOWEST_PRECEDENCE, getDefaultValue(order));
	}

	@Test
	public void getDefaultValueFromNonPublicAnnotation() {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertEquals(1, declaredAnnotations.length);
		Annotation annotation = declaredAnnotations[0];
		assertNotNull(annotation);
		assertEquals("NonPublicAnnotation", annotation.annotationType().getSimpleName());
		assertEquals(-1, getDefaultValue(annotation, VALUE));
		assertEquals(-1, getDefaultValue(annotation));
	}

	@Test
	public void getDefaultValueFromAnnotationType() {
		assertEquals(Ordered.LOWEST_PRECEDENCE, getDefaultValue(Order.class, VALUE));
		assertEquals(Ordered.LOWEST_PRECEDENCE, getDefaultValue(Order.class));
	}

	@Test
	public void findRepeatableAnnotationOnComposedAnnotation() {
		Repeatable repeatable = findAnnotation(MyRepeatableMeta1.class, Repeatable.class);
		assertNotNull(repeatable);
		assertEquals(MyRepeatableContainer.class, repeatable.value());
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnMethod() throws Exception {
		Method method = InterfaceWithRepeated.class.getMethod("foo");
		Set<MyRepeatable> annotations = getRepeatableAnnotations(method, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(annotations);
		List<String> values = annotations.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(asList("A", "B", "C", "meta1")));
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClassWithMissingAttributeAliasDeclaration() throws Exception {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Attribute 'value' in"));
		exception.expectMessage(containsString(BrokenContextConfig.class.getName()));
		exception.expectMessage(either(
				containsString("@AliasFor [location]")).or(
				containsString("@AliasFor 'location'")));

		getRepeatableAnnotations(BrokenConfigHierarchyTestCase.class, BrokenContextConfig.class, BrokenHierarchy.class);
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
		assertThat(locations, is(expectedLocations));

		List<String> values = annotations.stream().map(ContextConfig::value).collect(toList());
		assertThat(values, is(expectedLocations));
	}

	@Test
	public void getRepeatableAnnotationsDeclaredOnClass() {
		final List<String> expectedValuesJava = asList("A", "B", "C");
		final List<String> expectedValuesSpring = asList("A", "B", "C", "meta1");

		// Java 8
		MyRepeatable[] array = MyRepeatableClass.class.getAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		List<String> values = stream(array).map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesJava));

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));
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
		assertThat(values, is(expectedValuesJava));

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));
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
		assertThat(values, is(expectedValuesJava));

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));
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
		assertThat(values, is(expectedValuesJava));

		// Spring
		Set<MyRepeatable> set = getRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));

		// When container type is omitted and therefore inferred from @Repeatable
		set = getRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));
	}

	@Test
	public void getDeclaredRepeatableAnnotationsDeclaredOnClass() {
		final List<String> expectedValuesJava = asList("A", "B", "C");
		final List<String> expectedValuesSpring = asList("A", "B", "C", "meta1");

		// Java 8
		MyRepeatable[] array = MyRepeatableClass.class.getDeclaredAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		List<String> values = stream(array).map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesJava));

		// Spring
		Set<MyRepeatable> set = getDeclaredRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));

		// When container type is omitted and therefore inferred from @Repeatable
		set = getDeclaredRepeatableAnnotations(MyRepeatableClass.class, MyRepeatable.class);
		assertNotNull(set);
		values = set.stream().map(MyRepeatable::value).collect(toList());
		assertThat(values, is(expectedValuesSpring));
	}

	@Test
	public void getDeclaredRepeatableAnnotationsDeclaredOnSuperclass() {
		final Class<?> clazz = SubMyRepeatableClass.class;

		// Java 8
		MyRepeatable[] array = clazz.getDeclaredAnnotationsByType(MyRepeatable.class);
		assertNotNull(array);
		assertThat(array.length, is(0));

		// Spring
		Set<MyRepeatable> set = getDeclaredRepeatableAnnotations(clazz, MyRepeatable.class, MyRepeatableContainer.class);
		assertNotNull(set);
		assertThat(set.size(), is(0));

		// When container type is omitted and therefore inferred from @Repeatable
		set = getDeclaredRepeatableAnnotations(clazz, MyRepeatable.class);
		assertNotNull(set);
		assertThat(set.size(), is(0));
	}

	@Test
	public void getAttributeOverrideNameFromWrongTargetAnnotation() throws Exception {
		Method attribute = AliasedComposedContextConfig.class.getDeclaredMethod("xmlConfigFile");
		assertThat("xmlConfigFile is not an alias for @Component.",
				getAttributeOverrideName(attribute, Component.class), is(nullValue()));
	}

	@Test
	public void getAttributeOverrideNameForNonAliasedAttribute() throws Exception {
		Method nonAliasedAttribute = ImplicitAliasesContextConfig.class.getDeclaredMethod("nonAliasedAttribute");
		assertThat(getAttributeOverrideName(nonAliasedAttribute, ContextConfig.class), is(nullValue()));
	}

	@Test
	public void getAttributeOverrideNameFromAliasedComposedAnnotation() throws Exception {
		Method attribute = AliasedComposedContextConfig.class.getDeclaredMethod("xmlConfigFile");
		assertEquals("location", getAttributeOverrideName(attribute, ContextConfig.class));
	}

	@Test
	public void getAttributeAliasNamesFromComposedAnnotationWithImplicitAliases() throws Exception {
		Method xmlFile = ImplicitAliasesContextConfig.class.getDeclaredMethod("xmlFile");
		Method groovyScript = ImplicitAliasesContextConfig.class.getDeclaredMethod("groovyScript");
		Method value = ImplicitAliasesContextConfig.class.getDeclaredMethod("value");
		Method location1 = ImplicitAliasesContextConfig.class.getDeclaredMethod("location1");
		Method location2 = ImplicitAliasesContextConfig.class.getDeclaredMethod("location2");
		Method location3 = ImplicitAliasesContextConfig.class.getDeclaredMethod("location3");

		// Meta-annotation attribute overrides
		assertEquals("location", getAttributeOverrideName(xmlFile, ContextConfig.class));
		assertEquals("location", getAttributeOverrideName(groovyScript, ContextConfig.class));
		assertEquals("location", getAttributeOverrideName(value, ContextConfig.class));

		// Implicit aliases
		assertThat(getAttributeAliasNames(xmlFile), containsInAnyOrder("value", "groovyScript", "location1", "location2", "location3"));
		assertThat(getAttributeAliasNames(groovyScript), containsInAnyOrder("value", "xmlFile", "location1", "location2", "location3"));
		assertThat(getAttributeAliasNames(value), containsInAnyOrder("xmlFile", "groovyScript", "location1", "location2", "location3"));
		assertThat(getAttributeAliasNames(location1), containsInAnyOrder("xmlFile", "groovyScript", "value", "location2", "location3"));
		assertThat(getAttributeAliasNames(location2), containsInAnyOrder("xmlFile", "groovyScript", "value", "location1", "location3"));
		assertThat(getAttributeAliasNames(location3), containsInAnyOrder("xmlFile", "groovyScript", "value", "location1", "location2"));
	}

	@Test
	public void getAttributeAliasNamesFromComposedAnnotationWithImplicitAliasesForAliasPair() throws Exception {
		Method xmlFile = ImplicitAliasesForAliasPairContextConfig.class.getDeclaredMethod("xmlFile");
		Method groovyScript = ImplicitAliasesForAliasPairContextConfig.class.getDeclaredMethod("groovyScript");

		// Meta-annotation attribute overrides
		assertEquals("location", getAttributeOverrideName(xmlFile, ContextConfig.class));
		assertEquals("value", getAttributeOverrideName(groovyScript, ContextConfig.class));

		// Implicit aliases
		assertThat(getAttributeAliasNames(xmlFile), containsInAnyOrder("groovyScript"));
		assertThat(getAttributeAliasNames(groovyScript), containsInAnyOrder("xmlFile"));
	}

	@Test
	public void getAttributeAliasNamesFromComposedAnnotationWithImplicitAliasesWithImpliedAliasNamesOmitted()
			throws Exception {

		Method value = ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class.getDeclaredMethod("value");
		Method location = ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class.getDeclaredMethod("location");
		Method xmlFile = ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class.getDeclaredMethod("xmlFile");

		// Meta-annotation attribute overrides
		assertEquals("value", getAttributeOverrideName(value, ContextConfig.class));
		assertEquals("location", getAttributeOverrideName(location, ContextConfig.class));
		assertEquals("location", getAttributeOverrideName(xmlFile, ContextConfig.class));

		// Implicit aliases
		assertThat(getAttributeAliasNames(value), containsInAnyOrder("location", "xmlFile"));
		assertThat(getAttributeAliasNames(location), containsInAnyOrder("value", "xmlFile"));
		assertThat(getAttributeAliasNames(xmlFile), containsInAnyOrder("value", "location"));
	}

	@Test
	public void getAttributeAliasNamesFromComposedAnnotationWithTransitiveImplicitAliases() throws Exception {
		Method xml = TransitiveImplicitAliasesContextConfig.class.getDeclaredMethod("xml");
		Method groovy = TransitiveImplicitAliasesContextConfig.class.getDeclaredMethod("groovy");

		// Explicit meta-annotation attribute overrides
		assertEquals("xmlFile", getAttributeOverrideName(xml, ImplicitAliasesContextConfig.class));
		assertEquals("groovyScript", getAttributeOverrideName(groovy, ImplicitAliasesContextConfig.class));

		// Transitive meta-annotation attribute overrides
		assertEquals("location", getAttributeOverrideName(xml, ContextConfig.class));
		assertEquals("location", getAttributeOverrideName(groovy, ContextConfig.class));

		// Transitive implicit aliases
		assertThat(getAttributeAliasNames(xml), containsInAnyOrder("groovy"));
		assertThat(getAttributeAliasNames(groovy), containsInAnyOrder("xml"));
	}

	@Test
	public void getAttributeAliasNamesFromComposedAnnotationWithTransitiveImplicitAliasesForAliasPair() throws Exception {
		Method xml = TransitiveImplicitAliasesForAliasPairContextConfig.class.getDeclaredMethod("xml");
		Method groovy = TransitiveImplicitAliasesForAliasPairContextConfig.class.getDeclaredMethod("groovy");

		// Explicit meta-annotation attribute overrides
		assertEquals("xmlFile", getAttributeOverrideName(xml, ImplicitAliasesForAliasPairContextConfig.class));
		assertEquals("groovyScript", getAttributeOverrideName(groovy, ImplicitAliasesForAliasPairContextConfig.class));

		// Transitive implicit aliases
		assertThat(getAttributeAliasNames(xml), containsInAnyOrder("groovy"));
		assertThat(getAttributeAliasNames(groovy), containsInAnyOrder("xml"));
	}

	@Test
	public void getAttributeAliasNamesFromComposedAnnotationWithTransitiveImplicitAliasesWithImpliedAliasNamesOmitted()
			throws Exception {

		Method xml = TransitiveImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class.getDeclaredMethod("xml");
		Method groovy = TransitiveImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class.getDeclaredMethod("groovy");

		// Meta-annotation attribute overrides
		assertEquals("location", getAttributeOverrideName(xml, ContextConfig.class));
		assertEquals("location", getAttributeOverrideName(groovy, ContextConfig.class));

		// Explicit meta-annotation attribute overrides
		assertEquals("xmlFile", getAttributeOverrideName(xml, ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class));
		assertEquals("location", getAttributeOverrideName(groovy, ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class));

		// Transitive implicit aliases
		assertThat(getAttributeAliasNames(groovy), containsInAnyOrder("xml"));
		assertThat(getAttributeAliasNames(xml), containsInAnyOrder("groovy"));
	}

	@Test
	public void synthesizeAnnotationWithoutAttributeAliases() throws Exception {
		Component component = WebController.class.getAnnotation(Component.class);
		assertNotNull(component);
		Component synthesizedComponent = synthesizeAnnotation(component);
		assertNotNull(synthesizedComponent);
		assertSame(component, synthesizedComponent);
		assertEquals("value attribute: ", "webController", synthesizedComponent.value());
	}

	@Test
	public void synthesizeAlreadySynthesizedAnnotation() throws Exception {
		Method method = WebController.class.getMethod("handleMappedWithValueAttribute");
		WebMapping webMapping = method.getAnnotation(WebMapping.class);
		assertNotNull(webMapping);
		WebMapping synthesizedWebMapping = synthesizeAnnotation(webMapping);
		assertNotSame(webMapping, synthesizedWebMapping);
		WebMapping synthesizedAgainWebMapping = synthesizeAnnotation(synthesizedWebMapping);
		assertThat(synthesizedAgainWebMapping, instanceOf(SynthesizedAnnotation.class));
		assertSame(synthesizedWebMapping, synthesizedAgainWebMapping);

		assertEquals("name attribute: ", "foo", synthesizedAgainWebMapping.name());
		assertArrayEquals("aliased path attribute: ", asArray("/test"), synthesizedAgainWebMapping.path());
		assertArrayEquals("actual value attribute: ", asArray("/test"), synthesizedAgainWebMapping.value());
	}

	@Test
	public void synthesizeAnnotationWhereAliasForIsMissingAttributeDeclaration() throws Exception {
		AliasForWithMissingAttributeDeclaration annotation = AliasForWithMissingAttributeDeclarationClass.class.getAnnotation(AliasForWithMissingAttributeDeclaration.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("@AliasFor declaration on attribute 'foo' in annotation"));
		exception.expectMessage(containsString(AliasForWithMissingAttributeDeclaration.class.getName()));
		exception.expectMessage(containsString("points to itself"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWhereAliasForHasDuplicateAttributeDeclaration() throws Exception {
		AliasForWithDuplicateAttributeDeclaration annotation = AliasForWithDuplicateAttributeDeclarationClass.class.getAnnotation(AliasForWithDuplicateAttributeDeclaration.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("In @AliasFor declared on attribute 'foo' in annotation"));
		exception.expectMessage(containsString(AliasForWithDuplicateAttributeDeclaration.class.getName()));
		exception.expectMessage(containsString("attribute 'attribute' and its alias 'value' are present with values of [baz] and [bar]"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForNonexistentAttribute() throws Exception {
		AliasForNonexistentAttribute annotation = AliasForNonexistentAttributeClass.class.getAnnotation(AliasForNonexistentAttribute.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Attribute 'foo' in"));
		exception.expectMessage(containsString(AliasForNonexistentAttribute.class.getName()));
		exception.expectMessage(containsString("is declared as an @AliasFor nonexistent attribute 'bar'"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasWithoutMirroredAliasFor() throws Exception {
		AliasForWithoutMirroredAliasFor annotation =
				AliasForWithoutMirroredAliasForClass.class.getAnnotation(AliasForWithoutMirroredAliasFor.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Attribute 'bar' in"));
		exception.expectMessage(containsString(AliasForWithoutMirroredAliasFor.class.getName()));
		exception.expectMessage(containsString("@AliasFor [foo]"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasWithMirroredAliasForWrongAttribute() throws Exception {
		AliasForWithMirroredAliasForWrongAttribute annotation =
				AliasForWithMirroredAliasForWrongAttributeClass.class.getAnnotation(AliasForWithMirroredAliasForWrongAttribute.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Attribute 'bar' in"));
		exception.expectMessage(containsString(AliasForWithMirroredAliasForWrongAttribute.class.getName()));
		exception.expectMessage(either(containsString("must be declared as an @AliasFor [foo], not [quux]")).
				or(containsString("is declared as an @AliasFor nonexistent attribute 'quux'")));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForAttributeOfDifferentType() throws Exception {
		AliasForAttributeOfDifferentType annotation =
				AliasForAttributeOfDifferentTypeClass.class.getAnnotation(AliasForAttributeOfDifferentType.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Misconfigured aliases"));
		exception.expectMessage(containsString(AliasForAttributeOfDifferentType.class.getName()));
		exception.expectMessage(containsString("attribute 'foo'"));
		exception.expectMessage(containsString("attribute 'bar'"));
		exception.expectMessage(containsString("same return type"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForWithMissingDefaultValues() throws Exception {
		AliasForWithMissingDefaultValues annotation =
				AliasForWithMissingDefaultValuesClass.class.getAnnotation(AliasForWithMissingDefaultValues.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Misconfigured aliases"));
		exception.expectMessage(containsString(AliasForWithMissingDefaultValues.class.getName()));
		exception.expectMessage(containsString("attribute 'foo' in annotation"));
		exception.expectMessage(containsString("attribute 'bar' in annotation"));
		exception.expectMessage(containsString("default values"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForAttributeWithDifferentDefaultValue() throws Exception {
		AliasForAttributeWithDifferentDefaultValue annotation =
				AliasForAttributeWithDifferentDefaultValueClass.class.getAnnotation(AliasForAttributeWithDifferentDefaultValue.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Misconfigured aliases"));
		exception.expectMessage(containsString(AliasForAttributeWithDifferentDefaultValue.class.getName()));
		exception.expectMessage(containsString("attribute 'foo' in annotation"));
		exception.expectMessage(containsString("attribute 'bar' in annotation"));
		exception.expectMessage(containsString("same default value"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasForMetaAnnotationThatIsNotMetaPresent() throws Exception {
		AliasedComposedContextConfigNotMetaPresent annotation =
				AliasedComposedContextConfigNotMetaPresentClass.class.getAnnotation(AliasedComposedContextConfigNotMetaPresent.class);
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("@AliasFor declaration on attribute 'xmlConfigFile' in annotation"));
		exception.expectMessage(containsString(AliasedComposedContextConfigNotMetaPresent.class.getName()));
		exception.expectMessage(containsString("declares an alias for attribute 'location' in meta-annotation"));
		exception.expectMessage(containsString(ContextConfig.class.getName()));
		exception.expectMessage(containsString("not meta-present"));
		synthesizeAnnotation(annotation);
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliases() throws Exception {
		Method method = WebController.class.getMethod("handleMappedWithValueAttribute");
		WebMapping webMapping = method.getAnnotation(WebMapping.class);
		assertNotNull(webMapping);

		WebMapping synthesizedWebMapping1 = synthesizeAnnotation(webMapping);
		assertThat(synthesizedWebMapping1, instanceOf(SynthesizedAnnotation.class));
		assertNotSame(webMapping, synthesizedWebMapping1);

		assertEquals("name attribute: ", "foo", synthesizedWebMapping1.name());
		assertArrayEquals("aliased path attribute: ", asArray("/test"), synthesizedWebMapping1.path());
		assertArrayEquals("actual value attribute: ", asArray("/test"), synthesizedWebMapping1.value());

		WebMapping synthesizedWebMapping2 = synthesizeAnnotation(webMapping);
		assertThat(synthesizedWebMapping2, instanceOf(SynthesizedAnnotation.class));
		assertNotSame(webMapping, synthesizedWebMapping2);

		assertEquals("name attribute: ", "foo", synthesizedWebMapping2.name());
		assertArrayEquals("aliased path attribute: ", asArray("/test"), synthesizedWebMapping2.path());
		assertArrayEquals("actual value attribute: ", asArray("/test"), synthesizedWebMapping2.value());
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliases() throws Exception {
		assertAnnotationSynthesisWithImplicitAliases(ValueImplicitAliasesContextConfigClass.class, "value");
		assertAnnotationSynthesisWithImplicitAliases(Location1ImplicitAliasesContextConfigClass.class, "location1");
		assertAnnotationSynthesisWithImplicitAliases(XmlImplicitAliasesContextConfigClass.class, "xmlFile");
		assertAnnotationSynthesisWithImplicitAliases(GroovyImplicitAliasesContextConfigClass.class, "groovyScript");
	}

	private void assertAnnotationSynthesisWithImplicitAliases(Class<?> clazz, String expected) throws Exception {
		ImplicitAliasesContextConfig config = clazz.getAnnotation(ImplicitAliasesContextConfig.class);
		assertNotNull(config);

		ImplicitAliasesContextConfig synthesizedConfig = synthesizeAnnotation(config);
		assertThat(synthesizedConfig, instanceOf(SynthesizedAnnotation.class));

		assertEquals("value: ", expected, synthesizedConfig.value());
		assertEquals("location1: ", expected, synthesizedConfig.location1());
		assertEquals("xmlFile: ", expected, synthesizedConfig.xmlFile());
		assertEquals("groovyScript: ", expected, synthesizedConfig.groovyScript());
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithImpliedAliasNamesOmitted() throws Exception {
		assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
				ValueImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass.class, "value");
		assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
				LocationsImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass.class, "location");
		assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
				XmlFilesImplicitAliasesWithImpliedAliasNamesOmittedContextConfigClass.class, "xmlFile");
	}

	private void assertAnnotationSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
			Class<?> clazz, String expected) {

		ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig config = clazz.getAnnotation(
				ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig.class);
		assertNotNull(config);

		ImplicitAliasesWithImpliedAliasNamesOmittedContextConfig synthesizedConfig = synthesizeAnnotation(config);
		assertThat(synthesizedConfig, instanceOf(SynthesizedAnnotation.class));

		assertEquals("value: ", expected, synthesizedConfig.value());
		assertEquals("locations: ", expected, synthesizedConfig.location());
		assertEquals("xmlFiles: ", expected, synthesizedConfig.xmlFile());
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesForAliasPair() throws Exception {
		Class<?> clazz = ImplicitAliasesForAliasPairContextConfigClass.class;
		ImplicitAliasesForAliasPairContextConfig config = clazz.getAnnotation(ImplicitAliasesForAliasPairContextConfig.class);
		assertNotNull(config);

		ImplicitAliasesForAliasPairContextConfig synthesizedConfig = synthesizeAnnotation(config);
		assertThat(synthesizedConfig, instanceOf(SynthesizedAnnotation.class));

		assertEquals("xmlFile: ", "test.xml", synthesizedConfig.xmlFile());
		assertEquals("groovyScript: ", "test.xml", synthesizedConfig.groovyScript());
	}

	@Test
	public void synthesizeAnnotationWithTransitiveImplicitAliases() throws Exception {
		Class<?> clazz = TransitiveImplicitAliasesContextConfigClass.class;
		TransitiveImplicitAliasesContextConfig config = clazz.getAnnotation(TransitiveImplicitAliasesContextConfig.class);
		assertNotNull(config);

		TransitiveImplicitAliasesContextConfig synthesizedConfig = synthesizeAnnotation(config);
		assertThat(synthesizedConfig, instanceOf(SynthesizedAnnotation.class));

		assertEquals("xml: ", "test.xml", synthesizedConfig.xml());
		assertEquals("groovy: ", "test.xml", synthesizedConfig.groovy());
	}

	@Test
	public void synthesizeAnnotationWithTransitiveImplicitAliasesForAliasPair() throws Exception {
		Class<?> clazz = TransitiveImplicitAliasesForAliasPairContextConfigClass.class;
		TransitiveImplicitAliasesForAliasPairContextConfig config = clazz.getAnnotation(TransitiveImplicitAliasesForAliasPairContextConfig.class);
		assertNotNull(config);

		TransitiveImplicitAliasesForAliasPairContextConfig synthesizedConfig = synthesizeAnnotation(config);
		assertThat(synthesizedConfig, instanceOf(SynthesizedAnnotation.class));

		assertEquals("xml: ", "test.xml", synthesizedConfig.xml());
		assertEquals("groovy: ", "test.xml", synthesizedConfig.groovy());
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithMissingDefaultValues() throws Exception {
		Class<?> clazz = ImplicitAliasesWithMissingDefaultValuesContextConfigClass.class;
		Class<ImplicitAliasesWithMissingDefaultValuesContextConfig> annotationType = ImplicitAliasesWithMissingDefaultValuesContextConfig.class;
		ImplicitAliasesWithMissingDefaultValuesContextConfig config = clazz.getAnnotation(annotationType);
		assertNotNull(config);

		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Misconfigured aliases:"));
		exception.expectMessage(containsString("attribute 'location1' in annotation [" + annotationType.getName() + "]"));
		exception.expectMessage(containsString("attribute 'location2' in annotation [" + annotationType.getName() + "]"));
		exception.expectMessage(containsString("default values"));

		synthesizeAnnotation(config, clazz);
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithDifferentDefaultValues() throws Exception {
		Class<?> clazz = ImplicitAliasesWithDifferentDefaultValuesContextConfigClass.class;
		Class<ImplicitAliasesWithDifferentDefaultValuesContextConfig> annotationType = ImplicitAliasesWithDifferentDefaultValuesContextConfig.class;
		ImplicitAliasesWithDifferentDefaultValuesContextConfig config = clazz.getAnnotation(annotationType);
		assertNotNull(config);

		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Misconfigured aliases:"));
		exception.expectMessage(containsString("attribute 'location1' in annotation [" + annotationType.getName() + "]"));
		exception.expectMessage(containsString("attribute 'location2' in annotation [" + annotationType.getName() + "]"));
		exception.expectMessage(containsString("same default value"));

		synthesizeAnnotation(config, clazz);
	}

	@Test
	public void synthesizeAnnotationWithImplicitAliasesWithDuplicateValues() throws Exception {
		Class<?> clazz = ImplicitAliasesWithDuplicateValuesContextConfigClass.class;
		Class<ImplicitAliasesWithDuplicateValuesContextConfig> annotationType = ImplicitAliasesWithDuplicateValuesContextConfig.class;
		ImplicitAliasesWithDuplicateValuesContextConfig config = clazz.getAnnotation(annotationType);
		assertNotNull(config);

		ImplicitAliasesWithDuplicateValuesContextConfig synthesizedConfig = synthesizeAnnotation(config, clazz);
		assertNotNull(synthesizedConfig);

		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("In annotation"));
		exception.expectMessage(containsString(annotationType.getName()));
		exception.expectMessage(containsString("declared on class"));
		exception.expectMessage(containsString(clazz.getName()));
		exception.expectMessage(containsString("and synthesized from"));
		exception.expectMessage(either(containsString("attribute 'location1' and its alias 'location2'")).or(
				containsString("attribute 'location2' and its alias 'location1'")));
		exception.expectMessage(either(containsString("are present with values of [1] and [2]")).or(
				containsString("are present with values of [2] and [1]")));

		synthesizedConfig.location1();
	}

	@Test
	public void synthesizeAnnotationFromMapWithoutAttributeAliases() throws Exception {
		Component component = WebController.class.getAnnotation(Component.class);
		assertNotNull(component);

		Map<String, Object> map = Collections.singletonMap(VALUE, "webController");
		Component synthesizedComponent = synthesizeAnnotation(map, Component.class, WebController.class);
		assertNotNull(synthesizedComponent);

		assertNotSame(component, synthesizedComponent);
		assertEquals("value from component: ", "webController", component.value());
		assertEquals("value from synthesized component: ", "webController", synthesizedComponent.value());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeAnnotationFromMapWithNestedMap() throws Exception {
		ComponentScanSingleFilter componentScan = ComponentScanSingleFilterClass.class.getAnnotation(ComponentScanSingleFilter.class);
		assertNotNull(componentScan);
		assertEquals("value from ComponentScan: ", "*Foo", componentScan.value().pattern());

		AnnotationAttributes attributes = getAnnotationAttributes(
				ComponentScanSingleFilterClass.class, componentScan, false, true);
		assertNotNull(attributes);
		assertEquals(ComponentScanSingleFilter.class, attributes.annotationType());

		Map<String, Object> filterMap = (Map<String, Object>) attributes.get("value");
		assertNotNull(filterMap);
		assertEquals("*Foo", filterMap.get("pattern"));

		// Modify nested map
		filterMap.put("pattern", "newFoo");
		filterMap.put("enigma", 42);

		ComponentScanSingleFilter synthesizedComponentScan = synthesizeAnnotation(
				attributes, ComponentScanSingleFilter.class, ComponentScanSingleFilterClass.class);
		assertNotNull(synthesizedComponentScan);

		assertNotSame(componentScan, synthesizedComponentScan);
		assertEquals("value from synthesized ComponentScan: ", "newFoo", synthesizedComponentScan.value().pattern());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeAnnotationFromMapWithNestedArrayOfMaps() throws Exception {
		ComponentScan componentScan = ComponentScanClass.class.getAnnotation(ComponentScan.class);
		assertNotNull(componentScan);

		AnnotationAttributes attributes = getAnnotationAttributes(ComponentScanClass.class, componentScan, false, true);
		assertNotNull(attributes);
		assertEquals(ComponentScan.class, attributes.annotationType());

		Map<String, Object>[] filters = (Map[]) attributes.get("excludeFilters");
		assertNotNull(filters);

		List<String> patterns = stream(filters).map(m -> (String) m.get("pattern")).collect(toList());
		assertEquals(asList("*Foo", "*Bar"), patterns);

		// Modify nested maps
		filters[0].put("pattern", "newFoo");
		filters[0].put("enigma", 42);
		filters[1].put("pattern", "newBar");
		filters[1].put("enigma", 42);

		ComponentScan synthesizedComponentScan = synthesizeAnnotation(attributes, ComponentScan.class, ComponentScanClass.class);
		assertNotNull(synthesizedComponentScan);

		assertNotSame(componentScan, synthesizedComponentScan);
		patterns = stream(synthesizedComponentScan.excludeFilters()).map(Filter::pattern).collect(toList());
		assertEquals(asList("newFoo", "newBar"), patterns);
	}

	@Test
	public void synthesizeAnnotationFromDefaultsWithoutAttributeAliases() throws Exception {
		AnnotationWithDefaults annotationWithDefaults = synthesizeAnnotation(AnnotationWithDefaults.class);
		assertNotNull(annotationWithDefaults);
		assertEquals("text: ", "enigma", annotationWithDefaults.text());
		assertTrue("predicate: ", annotationWithDefaults.predicate());
		assertArrayEquals("characters: ", new char[] { 'a', 'b', 'c' }, annotationWithDefaults.characters());
	}

	@Test
	public void synthesizeAnnotationFromDefaultsWithAttributeAliases() throws Exception {
		ContextConfig contextConfig = synthesizeAnnotation(ContextConfig.class);
		assertNotNull(contextConfig);
		assertEquals("value: ", "", contextConfig.value());
		assertEquals("location: ", "", contextConfig.location());
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasesWithDifferentValues() throws Exception {
		ContextConfig contextConfig = synthesizeAnnotation(ContextConfigMismatch.class.getAnnotation(ContextConfig.class));
		exception.expect(AnnotationConfigurationException.class);
		getValue(contextConfig);
	}

	@Test
	public void synthesizeAnnotationFromMapWithMinimalAttributesWithAttributeAliases() throws Exception {
		Map<String, Object> map = Collections.singletonMap("location", "test.xml");
		ContextConfig contextConfig = synthesizeAnnotation(map, ContextConfig.class, null);
		assertNotNull(contextConfig);
		assertEquals("value: ", "test.xml", contextConfig.value());
		assertEquals("location: ", "test.xml", contextConfig.location());
	}

	@Test
	public void synthesizeAnnotationFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements() throws Exception {
		Map<String, Object> map = Collections.singletonMap("value", "/foo");
		Get get = synthesizeAnnotation(map, Get.class, null);
		assertNotNull(get);
		assertEquals("value: ", "/foo", get.value());
		assertEquals("path: ", "/foo", get.path());

		map = Collections.singletonMap("path", "/foo");
		get = synthesizeAnnotation(map, Get.class, null);
		assertNotNull(get);
		assertEquals("value: ", "/foo", get.value());
		assertEquals("path: ", "/foo", get.path());
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
		assertEquals("value: ", attributeNameAndValue, config.value());
		assertEquals("location1: ", attributeNameAndValue, config.location1());
		assertEquals("location2: ", attributeNameAndValue, config.location2());
		assertEquals("location3: ", attributeNameAndValue, config.location3());
		assertEquals("xmlFile: ", attributeNameAndValue, config.xmlFile());
		assertEquals("groovyScript: ", attributeNameAndValue, config.groovyScript());
	}

	@Test
	public void synthesizeAnnotationFromMapWithMissingAttributeValue() throws Exception {
		assertMissingTextAttribute(Collections.emptyMap());
	}

	@Test
	public void synthesizeAnnotationFromMapWithNullAttributeValue() throws Exception {
		Map<String, Object> map = Collections.singletonMap("text", null);
		assertTrue(map.containsKey("text"));
		assertMissingTextAttribute(map);
	}

	private void assertMissingTextAttribute(Map<String, Object> attributes) {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(startsWith("Attributes map"));
		exception.expectMessage(containsString("returned null for required attribute 'text'"));
		exception.expectMessage(containsString("defined by annotation type [" + AnnotationWithoutDefaults.class.getName() + "]"));
		synthesizeAnnotation(attributes, AnnotationWithoutDefaults.class, null);
	}

	@Test
	public void synthesizeAnnotationFromMapWithAttributeOfIncorrectType() throws Exception {
		Map<String, Object> map = Collections.singletonMap(VALUE, 42L);

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(startsWith("Attributes map"));
		exception.expectMessage(containsString("returned a value of type [java.lang.Long]"));
		exception.expectMessage(containsString("for attribute 'value'"));
		exception.expectMessage(containsString("but a value of type [java.lang.String] is required"));
		exception.expectMessage(containsString("as defined by annotation type [" + Component.class.getName() + "]"));

		synthesizeAnnotation(map, Component.class, null);
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
		assertEquals(component, synthesizedComponent);
		assertEquals("value from component: ", "webController", component.value());
		assertEquals("value from synthesized component: ", "webController", synthesizedComponent.value());
	}

	@Test
	public void toStringForSynthesizedAnnotations() throws Exception {
		Method methodWithPath = WebController.class.getMethod("handleMappedWithPathAttribute");
		WebMapping webMappingWithAliases = methodWithPath.getAnnotation(WebMapping.class);
		assertNotNull(webMappingWithAliases);

		Method methodWithPathAndValue = WebController.class.getMethod("handleMappedWithSamePathAndValueAttributes");
		WebMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(WebMapping.class);
		assertNotNull(webMappingWithPathAndValue);

		WebMapping synthesizedWebMapping1 = synthesizeAnnotation(webMappingWithAliases);
		assertNotNull(synthesizedWebMapping1);
		WebMapping synthesizedWebMapping2 = synthesizeAnnotation(webMappingWithAliases);
		assertNotNull(synthesizedWebMapping2);

		assertThat(webMappingWithAliases.toString(), is(not(synthesizedWebMapping1.toString())));
		assertToStringForWebMappingWithPathAndValue(synthesizedWebMapping1);
		assertToStringForWebMappingWithPathAndValue(synthesizedWebMapping2);
	}

	private void assertToStringForWebMappingWithPathAndValue(WebMapping webMapping) {
		String string = webMapping.toString();
		assertThat(string, startsWith("@" + WebMapping.class.getName() + "("));
		assertThat(string, containsString("value=[/test]"));
		assertThat(string, containsString("path=[/test]"));
		assertThat(string, containsString("name=bar"));
		assertThat(string, containsString("method="));
		assertThat(string, containsString("[GET, POST]"));
		assertThat(string, endsWith(")"));
	}

	@Test
	public void equalsForSynthesizedAnnotations() throws Exception {
		Method methodWithPath = WebController.class.getMethod("handleMappedWithPathAttribute");
		WebMapping webMappingWithAliases = methodWithPath.getAnnotation(WebMapping.class);
		assertNotNull(webMappingWithAliases);

		Method methodWithPathAndValue = WebController.class.getMethod("handleMappedWithSamePathAndValueAttributes");
		WebMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(WebMapping.class);
		assertNotNull(webMappingWithPathAndValue);

		WebMapping synthesizedWebMapping1 = synthesizeAnnotation(webMappingWithAliases);
		assertNotNull(synthesizedWebMapping1);
		WebMapping synthesizedWebMapping2 = synthesizeAnnotation(webMappingWithAliases);
		assertNotNull(synthesizedWebMapping2);

		// Equality amongst standard annotations
		assertThat(webMappingWithAliases, is(webMappingWithAliases));
		assertThat(webMappingWithPathAndValue, is(webMappingWithPathAndValue));

		// Inequality amongst standard annotations
		assertThat(webMappingWithAliases, is(not(webMappingWithPathAndValue)));
		assertThat(webMappingWithPathAndValue, is(not(webMappingWithAliases)));

		// Equality amongst synthesized annotations
		assertThat(synthesizedWebMapping1, is(synthesizedWebMapping1));
		assertThat(synthesizedWebMapping2, is(synthesizedWebMapping2));
		assertThat(synthesizedWebMapping1, is(synthesizedWebMapping2));
		assertThat(synthesizedWebMapping2, is(synthesizedWebMapping1));

		// Equality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1, is(webMappingWithPathAndValue));
		assertThat(webMappingWithPathAndValue, is(synthesizedWebMapping1));

		// Inequality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1, is(not(webMappingWithAliases)));
		assertThat(webMappingWithAliases, is(not(synthesizedWebMapping1)));
	}

	@Test
	public void hashCodeForSynthesizedAnnotations() throws Exception {
		Method methodWithPath = WebController.class.getMethod("handleMappedWithPathAttribute");
		WebMapping webMappingWithAliases = methodWithPath.getAnnotation(WebMapping.class);
		assertNotNull(webMappingWithAliases);

		Method methodWithPathAndValue = WebController.class.getMethod("handleMappedWithSamePathAndValueAttributes");
		WebMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(WebMapping.class);
		assertNotNull(webMappingWithPathAndValue);

		WebMapping synthesizedWebMapping1 = synthesizeAnnotation(webMappingWithAliases);
		assertNotNull(synthesizedWebMapping1);
		WebMapping synthesizedWebMapping2 = synthesizeAnnotation(webMappingWithAliases);
		assertNotNull(synthesizedWebMapping2);

		// Equality amongst standard annotations
		assertThat(webMappingWithAliases.hashCode(), is(webMappingWithAliases.hashCode()));
		assertThat(webMappingWithPathAndValue.hashCode(), is(webMappingWithPathAndValue.hashCode()));

		// Inequality amongst standard annotations
		assertThat(webMappingWithAliases.hashCode(), is(not(webMappingWithPathAndValue.hashCode())));
		assertThat(webMappingWithPathAndValue.hashCode(), is(not(webMappingWithAliases.hashCode())));

		// Equality amongst synthesized annotations
		assertThat(synthesizedWebMapping1.hashCode(), is(synthesizedWebMapping1.hashCode()));
		assertThat(synthesizedWebMapping2.hashCode(), is(synthesizedWebMapping2.hashCode()));
		assertThat(synthesizedWebMapping1.hashCode(), is(synthesizedWebMapping2.hashCode()));
		assertThat(synthesizedWebMapping2.hashCode(), is(synthesizedWebMapping1.hashCode()));

		// Equality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1.hashCode(), is(webMappingWithPathAndValue.hashCode()));
		assertThat(webMappingWithPathAndValue.hashCode(), is(synthesizedWebMapping1.hashCode()));

		// Inequality between standard and synthesized annotations
		assertThat(synthesizedWebMapping1.hashCode(), is(not(webMappingWithAliases.hashCode())));
		assertThat(webMappingWithAliases.hashCode(), is(not(synthesizedWebMapping1.hashCode())));
	}

	/**
	 * Fully reflection-based test that verifies support for
	 * {@linkplain AnnotationUtils#synthesizeAnnotation synthesizing annotations}
	 * across packages with non-public visibility of user types (e.g., a non-public
	 * annotation that uses {@code @AliasFor}).
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void synthesizeNonPublicAnnotationWithAttributeAliasesFromDifferentPackage() throws Exception {
		Class<?> clazz =
				ClassUtils.forName("org.springframework.core.annotation.subpackage.NonPublicAliasedAnnotatedClass", null);
		Class<? extends Annotation> annotationType = (Class<? extends Annotation>)
				ClassUtils.forName("org.springframework.core.annotation.subpackage.NonPublicAliasedAnnotation", null);

		Annotation annotation = clazz.getAnnotation(annotationType);
		assertNotNull(annotation);
		Annotation synthesizedAnnotation = synthesizeAnnotation(annotation);
		assertNotSame(annotation, synthesizedAnnotation);

		assertNotNull(synthesizedAnnotation);
		assertEquals("name attribute: ", "test", getValue(synthesizedAnnotation, "name"));
		assertEquals("aliased path attribute: ", "/test", getValue(synthesizedAnnotation, "path"));
		assertEquals("aliased path attribute: ", "/test", getValue(synthesizedAnnotation, "value"));
	}

	@Test
	public void synthesizeAnnotationWithAttributeAliasesInNestedAnnotations() throws Exception {
		List<String> expectedLocations = asList("A", "B");

		Hierarchy hierarchy = ConfigHierarchyTestCase.class.getAnnotation(Hierarchy.class);
		assertNotNull(hierarchy);
		Hierarchy synthesizedHierarchy = synthesizeAnnotation(hierarchy);
		assertNotSame(hierarchy, synthesizedHierarchy);
		assertThat(synthesizedHierarchy, instanceOf(SynthesizedAnnotation.class));

		ContextConfig[] configs = synthesizedHierarchy.value();
		assertNotNull(configs);
		assertTrue("nested annotations must be synthesized",
				stream(configs).allMatch(c -> c instanceof SynthesizedAnnotation));

		List<String> locations = stream(configs).map(ContextConfig::location).collect(toList());
		assertThat(locations, is(expectedLocations));

		List<String> values = stream(configs).map(ContextConfig::value).collect(toList());
		assertThat(values, is(expectedLocations));
	}

	@Test
	public void synthesizeAnnotationWithArrayOfAnnotations() throws Exception {
		List<String> expectedLocations = asList("A", "B");

		Hierarchy hierarchy = ConfigHierarchyTestCase.class.getAnnotation(Hierarchy.class);
		assertNotNull(hierarchy);
		Hierarchy synthesizedHierarchy = synthesizeAnnotation(hierarchy);
		assertThat(synthesizedHierarchy, instanceOf(SynthesizedAnnotation.class));

		ContextConfig contextConfig = SimpleConfigTestCase.class.getAnnotation(ContextConfig.class);
		assertNotNull(contextConfig);

		ContextConfig[] configs = synthesizedHierarchy.value();
		List<String> locations = stream(configs).map(ContextConfig::location).collect(toList());
		assertThat(locations, is(expectedLocations));

		// Alter array returned from synthesized annotation
		configs[0] = contextConfig;

		// Re-retrieve the array from the synthesized annotation
		configs = synthesizedHierarchy.value();
		List<String> values = stream(configs).map(ContextConfig::value).collect(toList());
		assertThat(values, is(expectedLocations));
	}

	@Test
	public void synthesizeAnnotationWithArrayOfChars() throws Exception {
		CharsContainer charsContainer = GroupOfCharsClass.class.getAnnotation(CharsContainer.class);
		assertNotNull(charsContainer);
		CharsContainer synthesizedCharsContainer = synthesizeAnnotation(charsContainer);
		assertThat(synthesizedCharsContainer, instanceOf(SynthesizedAnnotation.class));

		char[] chars = synthesizedCharsContainer.chars();
		assertArrayEquals(new char[] { 'x', 'y', 'z' }, chars);

		// Alter array returned from synthesized annotation
		chars[0] = '?';

		// Re-retrieve the array from the synthesized annotation
		chars = synthesizedCharsContainer.chars();
		assertArrayEquals(new char[] { 'x', 'y', 'z' }, chars);
	}

	@Test
	public void interfaceWithAnnotatedMethods() {
		assertTrue(AnnotationUtils.getAnnotatedMethodsInBaseType(NonAnnotatedInterface.class).isEmpty());
		assertFalse(AnnotationUtils.getAnnotatedMethodsInBaseType(AnnotatedInterface.class).isEmpty());
		assertTrue(AnnotationUtils.getAnnotatedMethodsInBaseType(NullableAnnotatedInterface.class).isEmpty());
	}

	// FIXME to here

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

}
