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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.core.annotation.InternalAnnotationUtils.DefaultValueHolder;
import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * General utility methods for working with annotations, handling meta-annotations,
 * bridge methods (which the compiler generates for generic declarations) as well
 * as super methods (for optional <em>annotation inheritance</em>).
 *
 * <p>Note that most of the features of this class are not provided by the
 * JDK's introspection facilities themselves.
 *
 * <p>As a general rule for runtime-retained annotations (e.g. for transaction
 * control, authorization, or service exposure), always use the lookup methods
 * on this class (e.g., {@link #findAnnotation(Method, Class)},
 * {@link #getAnnotation(Method, Class)}, and {@link #getAnnotations(Method)})
 * instead of the plain annotation lookup methods in the JDK. You can still
 * explicitly choose between a <em>get</em> lookup on the given class level only
 * ({@link #getAnnotation(Method, Class)}) and a <em>find</em> lookup in the entire
 * inheritance hierarchy of the given method ({@link #findAnnotation(Method, Class)}).
 *
 * <h3>Terminology</h3>
 * The terms <em>directly present</em>, <em>indirectly present</em>, and
 * <em>present</em> have the same meanings as defined in the class-level
 * javadoc for {@link AnnotatedElement} (in Java 8).
 *
 * <p>An annotation is <em>meta-present</em> on an element if the annotation
 * is declared as a meta-annotation on some other annotation which is
 * <em>present</em> on the element. Annotation {@code A} is <em>meta-present</em>
 * on another annotation if {@code A} is either <em>directly present</em> or
 * <em>meta-present</em> on the other annotation.
 *
 * <h3>Meta-annotation Support</h3>
 * <p>Most {@code find*()} methods and some {@code get*()} methods in this class
 * provide support for finding annotations used as meta-annotations. Consult the
 * javadoc for each method in this class for details. For fine-grained support for
 * meta-annotations with <em>attribute overrides</em> in <em>composed annotations</em>,
 * consider using {@link AnnotatedElementUtils}'s more specific methods instead.
 *
 * <h3>Attribute Aliases</h3>
 * <p>All public methods in this class that return annotations, arrays of
 * annotations, or {@link AnnotationAttributes} transparently support attribute
 * aliases configured via {@link AliasFor @AliasFor}. Consult the various
 * {@code synthesizeAnnotation*(..)} methods for details.
 *
 * <h3>Search Scope</h3>
 * <p>The search algorithms used by methods in this class stop searching for
 * an annotation once the first annotation of the specified type has been
 * found. As a consequence, additional annotations of the specified type will
 * be silently ignored.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Oleg Zhurakousky
 * @since 2.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotatedElementUtils
 * @see BridgeMethodResolver
 * @see java.lang.reflect.AnnotatedElement#getAnnotations()
 * @see java.lang.reflect.AnnotatedElement#getAnnotation(Class)
 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()
 */
public abstract class AnnotationUtils {

	/**
	 * The attribute name for annotations with a single element.
	 */
	public static final String VALUE = "value";

	private static Map<Class<? extends Annotation>, Map<String, DefaultValueHolder>> defaultValuesCache = new ConcurrentReferenceHashMap<>();


	/**
	 * Get a single {@link Annotation} of {@code annotationType} from the supplied
	 * annotation: either the given annotation itself or a direct meta-annotation
	 * thereof.
	 * <p>Note that this method supports only a single level of meta-annotations.
	 * For support for arbitrary levels of meta-annotations, use one of the
	 * {@code find*()} methods instead.
	 * @param annotation the Annotation to check
	 * @param annotationType the annotation type to look for, both locally and as a meta-annotation
	 * @return the first matching annotation, or {@code null} if not found
	 * @since 4.0
	 */
	@Nullable
	public static <A extends Annotation> A getAnnotation(Annotation annotation, Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getAnnotation(annotation, annotationType)
		).to(() ->
			MergedAnnotations.from(annotation).get(
					annotationType).withNonMergedAttributes().synthesize(
							AnnotationUtils::isSingleLevelPresent).orElse(null)
		);
	}

	/**
	 * Get a single {@link Annotation} of {@code annotationType} from the supplied
	 * {@link AnnotatedElement}, where the annotation is either <em>present</em> or
	 * <em>meta-present</em> on the {@code AnnotatedElement}.
	 * <p>Note that this method supports only a single level of meta-annotations.
	 * For support for arbitrary levels of meta-annotations, use
	 * {@link #findAnnotation(AnnotatedElement, Class)} instead.
	 * @param annotatedElement the {@code AnnotatedElement} from which to get the annotation
	 * @param annotationType the annotation type to look for, both locally and as a meta-annotation
	 * @return the first matching annotation, or {@code null} if not found
	 * @since 3.1
	 */
	@Nullable
	public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getAnnotation(annotatedElement, annotationType)
		).to(() ->
			MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none(),
					AnnotationFilter.PLAIN).get(
							annotationType).withNonMergedAttributes().synthesize(
									AnnotationUtils::isSingleLevelPresent).orElse(null)
		);
	}

	/**
	 * Get a single {@link Annotation} of {@code annotationType} from the
	 * supplied {@link Method}, where the annotation is either <em>present</em>
	 * or <em>meta-present</em> on the method.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * <p>Note that this method supports only a single level of meta-annotations.
	 * For support for arbitrary levels of meta-annotations, use
	 * {@link #findAnnotation(Method, Class)} instead.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @return the first matching annotation, or {@code null} if not found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see #getAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getAnnotation(method, annotationType)
		).to(() ->
			MergedAnnotations.from(method, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none(),
					AnnotationFilter.PLAIN).get(annotationType).withNonMergedAttributes().synthesize(
							AnnotationUtils::isSingleLevelPresent).orElse(null)		);
	}

	/**
	 * Get all {@link Annotation Annotations} that are <em>present</em> on the
	 * supplied {@link AnnotatedElement}.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * @param annotatedElement the Method, Constructor or Field to retrieve annotations from
	 * @return the annotations found, an empty array, or {@code null} if not
	 * resolvable (e.g. because nested Class values in annotation attributes
	 * failed to resolve at runtime)
	 * @since 4.0.8
	 * @see AnnotatedElement#getAnnotations()
	 */
	@Nullable
	public static Annotation[] getAnnotations(AnnotatedElement annotatedElement) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getAnnotations(annotatedElement)
		).to(() ->
			MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none(),
					AnnotationFilter.NONE).stream().filter(
							MergedAnnotation::isDirectlyPresent).map(
									MergedAnnotation::withNonMergedAttributes).collect(
											MergedAnnotationCollectors.toAnnotationArray())
		);
	}

	/**
	 * Get all {@link Annotation Annotations} that are <em>present</em> on the
	 * supplied {@link Method}.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * @param method the Method to retrieve annotations from
	 * @return the annotations found, an empty array, or {@code null} if not
	 * resolvable (e.g. because nested Class values in annotation attributes
	 * failed to resolve at runtime)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see AnnotatedElement#getAnnotations()
	 */
	@Nullable
	public static Annotation[] getAnnotations(Method method) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getAnnotations(method)
		).to(()->
			MergedAnnotations.from(method, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none(),
					AnnotationFilter.NONE).stream().filter(MergedAnnotation::isDirectlyPresent).map(
							MergedAnnotation::withNonMergedAttributes).collect(
									MergedAnnotationCollectors.toAnnotationArray())
		);
	}

	/**
	 * Get the <em>repeatable</em> {@linkplain Annotation annotations} of
	 * {@code annotationType} from the supplied {@link AnnotatedElement}, where
	 * such annotations are either <em>present</em>, <em>indirectly present</em>,
	 * or <em>meta-present</em> on the element.
	 * <p>This method mimics the functionality of Java 8's
	 * {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}
	 * with support for automatic detection of a <em>container annotation</em>
	 * declared via @{@link java.lang.annotation.Repeatable} (when running on
	 * Java 8 or higher) and with additional support for meta-annotations.
	 * <p>Handles both single annotations and annotations nested within a
	 * <em>container annotation</em>.
	 * <p>Correctly handles <em>bridge methods</em> generated by the
	 * compiler if the supplied element is a {@link Method}.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>present</em> on the supplied element.
	 * @param annotatedElement the element to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @return the annotations found or an empty set (never {@code null})
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
	 */
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {
		return getRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * Get the <em>repeatable</em> {@linkplain Annotation annotations} of
	 * {@code annotationType} from the supplied {@link AnnotatedElement}, where
	 * such annotations are either <em>present</em>, <em>indirectly present</em>,
	 * or <em>meta-present</em> on the element.
	 * <p>This method mimics the functionality of Java 8's
	 * {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}
	 * with additional support for meta-annotations.
	 * <p>Handles both single annotations and annotations nested within a
	 * <em>container annotation</em>.
	 * <p>Correctly handles <em>bridge methods</em> generated by the
	 * compiler if the supplied element is a {@link Method}.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>present</em> on the supplied element.
	 * @param annotatedElement the element to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @param containerAnnotationType the type of the container that holds
	 * the annotations; may be {@code null} if a container is not supported
	 * or if it should be looked up via @{@link java.lang.annotation.Repeatable}
	 * when running on Java 8 or higher
	 * @return the annotations found or an empty set (never {@code null})
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
	 */
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerAnnotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getRepeatableAnnotations(annotatedElement,
					annotationType, containerAnnotationType)
		).to(() ->
			MergedAnnotations.from(annotatedElement, SearchStrategy.SUPER_CLASS, containerAnnotationType != null
							? RepeatableContainers.of(annotationType, containerAnnotationType)
							: RepeatableContainers.standardRepeatables(),
					AnnotationFilter.mostAppropriateFor(annotationType)).stream(annotationType).filter(MergedAnnotationPredicates.firstRunOf(
							MergedAnnotation::getAggregateIndex)).map(
									MergedAnnotation::withNonMergedAttributes).collect(
											MergedAnnotationCollectors.toAnnotationSet())
		);
	}

	/**
	 * Get the declared <em>repeatable</em> {@linkplain Annotation annotations}
	 * of {@code annotationType} from the supplied {@link AnnotatedElement},
	 * where such annotations are either <em>directly present</em>,
	 * <em>indirectly present</em>, or <em>meta-present</em> on the element.
	 * <p>This method mimics the functionality of Java 8's
	 * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)}
	 * with support for automatic detection of a <em>container annotation</em>
	 * declared via @{@link java.lang.annotation.Repeatable} (when running on
	 * Java 8 or higher) and with additional support for meta-annotations.
	 * <p>Handles both single annotations and annotations nested within a
	 * <em>container annotation</em>.
	 * <p>Correctly handles <em>bridge methods</em> generated by the
	 * compiler if the supplied element is a {@link Method}.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>present</em> on the supplied element.
	 * @param annotatedElement the element to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @return the annotations found or an empty set (never {@code null})
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
	 */
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {
		return getDeclaredRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * Get the declared <em>repeatable</em> {@linkplain Annotation annotations}
	 * of {@code annotationType} from the supplied {@link AnnotatedElement},
	 * where such annotations are either <em>directly present</em>,
	 * <em>indirectly present</em>, or <em>meta-present</em> on the element.
	 * <p>This method mimics the functionality of Java 8's
	 * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)}
	 * with additional support for meta-annotations.
	 * <p>Handles both single annotations and annotations nested within a
	 * <em>container annotation</em>.
	 * <p>Correctly handles <em>bridge methods</em> generated by the
	 * compiler if the supplied element is a {@link Method}.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>present</em> on the supplied element.
	 * @param annotatedElement the element to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @param containerAnnotationType the type of the container that holds
	 * the annotations; may be {@code null} if a container is not supported
	 * or if it should be looked up via @{@link java.lang.annotation.Repeatable}
	 * when running on Java 8 or higher
	 * @return the annotations found or an empty set (never {@code null})
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
	 */
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerAnnotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getDeclaredRepeatableAnnotations(annotatedElement,
					annotationType, containerAnnotationType)
		).to(() ->
			MergedAnnotations.from(
					annotatedElement,
							SearchStrategy.DIRECT,
					containerAnnotationType != null
							? RepeatableContainers.of(annotationType, containerAnnotationType)
							: RepeatableContainers.standardRepeatables(), AnnotationFilter.mostAppropriateFor(containerAnnotationType)).stream(annotationType).map(
							MergedAnnotation::withNonMergedAttributes).collect(
									MergedAnnotationCollectors.toAnnotationSet())
		);
	}

	/**
	 * Find a single {@link Annotation} of {@code annotationType} on the
	 * supplied {@link AnnotatedElement}.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>directly present</em> on the supplied element.
	 * <p><strong>Warning</strong>: this method operates generically on
	 * annotated elements. In other words, this method does not execute
	 * specialized search algorithms for classes or methods. If you require
	 * the more specific semantics of {@link #findAnnotation(Class, Class)}
	 * or {@link #findAnnotation(Method, Class)}, invoke one of those methods
	 * instead.
	 * @param annotatedElement the {@code AnnotatedElement} on which to find the annotation
	 * @param annotationType the annotation type to look for, both locally and as a meta-annotation
	 * @return the first matching annotation, or {@code null} if not found
	 * @since 4.2
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.findAnnotation(annotatedElement, annotationType)
		).to(() -> {
			A annotation = annotatedElement.getDeclaredAnnotation(annotationType);
			if (annotation != null) {
				return annotation;
			}
			return MergedAnnotations.from(annotatedElement, SearchStrategy.DIRECT,  RepeatableContainers.none(),
					AnnotationFilter.mostAppropriateFor(annotationType)).get(annotationType).withNonMergedAttributes().synthesize(
							MergedAnnotation::isPresent).orElse(null);
		});
	}

	/**
	 * Find a single {@link Annotation} of {@code annotationType} on the supplied
	 * {@link Method}, traversing its super methods (i.e. from superclasses and
	 * interfaces) if the annotation is not <em>directly present</em> on the given
	 * method itself.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * <p>Meta-annotations will be searched if the annotation is not
	 * <em>directly present</em> on the method.
	 * <p>Annotations on methods are not inherited by default, so we need to handle
	 * this explicitly.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation type to look for
	 * @return the first matching annotation, or {@code null} if not found
	 * @see #getAnnotation(Method, Class)
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(Method method, @Nullable Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.findAnnotation(method, annotationType)
		).to(() ->
			MergedAnnotations.from(method, SearchStrategy.EXHAUSTIVE, RepeatableContainers.none(),
					AnnotationFilter.mostAppropriateFor(annotationType)).get(annotationType).withNonMergedAttributes().synthesize(
							MergedAnnotation::isPresent).orElse(null)
		);
	}

	/**
	 * Find a single {@link Annotation} of {@code annotationType} on the
	 * supplied {@link Class}, traversing its interfaces, annotations, and
	 * superclasses if the annotation is not <em>directly present</em> on
	 * the given class itself.
	 * <p>This method explicitly handles class-level annotations which are not
	 * declared as {@link java.lang.annotation.Inherited inherited} <em>as well
	 * as meta-annotations and annotations on interfaces</em>.
	 * <p>The algorithm operates as follows:
	 * <ol>
	 * <li>Search for the annotation on the given class and return it if found.
	 * <li>Recursively search through all annotations that the given class declares.
	 * <li>Recursively search through all interfaces that the given class declares.
	 * <li>Recursively search through the superclass hierarchy of the given class.
	 * </ol>
	 * <p>Note: in this context, the term <em>recursively</em> means that the search
	 * process continues by returning to step #1 with the current interface,
	 * annotation, or superclass as the class to look for annotations on.
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @return the first matching annotation, or {@code null} if not found
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.findAnnotation(clazz, annotationType)
		).to(() -> {
			A annotation = clazz.getDeclaredAnnotation(annotationType);
			if (annotation != null) {
				return annotation;
		    }
			return MergedAnnotations.from(clazz, SearchStrategy.EXHAUSTIVE, RepeatableContainers.none(),
					AnnotationFilter.mostAppropriateFor(annotationType)).get(annotationType).withNonMergedAttributes().synthesize(
							MergedAnnotation::isPresent).orElse(null);
		});
	}

	/**
	 * Find the first {@link Class} in the inheritance hierarchy of the
	 * specified {@code clazz} (including the specified {@code clazz} itself)
	 * on which an annotation of the specified {@code annotationType} is
	 * <em>directly present</em>.
	 * <p>If the supplied {@code clazz} is an interface, only the interface
	 * itself will be checked; the inheritance hierarchy for interfaces will
	 * not be traversed.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>The standard {@link Class} API does not provide a mechanism for
	 * determining which class in an inheritance hierarchy actually declares
	 * an {@link Annotation}, so we need to handle this explicitly.
	 * @param annotationType the annotation type to look for
	 * @param clazz the class to check for the annotation on (may be {@code null})
	 * @return the first {@link Class} in the inheritance hierarchy that
	 * declares an annotation of the specified {@code annotationType}, or
	 * {@code null} if not found
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @see #findAnnotationDeclaringClassForTypes(List, Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 */
	@Nullable
	public static Class<?> findAnnotationDeclaringClass(Class<? extends Annotation> annotationType, @Nullable Class<?> clazz) {
		return MigrateMethod.<Class<?>> from(() ->
			InternalAnnotationUtils.findAnnotationDeclaringClass(annotationType,
					clazz)
		).to(() ->
			(Class<?>) MergedAnnotations.from(clazz,
					SearchStrategy.SUPER_CLASS, RepeatableContainers.none(), AnnotationFilter.mostAppropriateFor(annotationType)).get(annotationType,
							MergedAnnotation::isDirectlyPresent).getSource()
		);
	}

	/**
	 * Find the first {@link Class} in the inheritance hierarchy of the
	 * specified {@code clazz} (including the specified {@code clazz} itself)
	 * on which at least one of the specified {@code annotationTypes} is
	 * <em>directly present</em>.
	 * <p>If the supplied {@code clazz} is an interface, only the interface
	 * itself will be checked; the inheritance hierarchy for interfaces will
	 * not be traversed.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>The standard {@link Class} API does not provide a mechanism for
	 * determining which class in an inheritance hierarchy actually declares
	 * one of several candidate {@linkplain Annotation annotations}, so we
	 * need to handle this explicitly.
	 * @param annotationTypes the annotation types to look for
	 * @param clazz the class to check for the annotations on, or {@code null}
	 * @return the first {@link Class} in the inheritance hierarchy that
	 * declares an annotation of at least one of the specified
	 * {@code annotationTypes}, or {@code null} if not found
	 * @since 3.2.2
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @see #findAnnotationDeclaringClass(Class, Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 */
	@Nullable
	public static Class<?> findAnnotationDeclaringClassForTypes(
			List<Class<? extends Annotation>> annotationTypes, @Nullable Class<?> clazz) {
		return MigrateMethod.<Class<?>> from(() ->
			InternalAnnotationUtils.findAnnotationDeclaringClassForTypes(
					annotationTypes, clazz)
		).to(() ->
			(Class<?>) MergedAnnotations.from(clazz,
					SearchStrategy.SUPER_CLASS, RepeatableContainers.none(), AnnotationFilter.mostAppropriateFor(annotationTypes)).stream().filter(
							MergedAnnotationPredicates.typeIn(annotationTypes).and(
									MergedAnnotation::isDirectlyPresent)).map(
											MergedAnnotation::getSource).findFirst().orElse(
													null)
		);
	}

	/**
	 * Determine whether an annotation of the specified {@code annotationType}
	 * is declared locally (i.e. <em>directly present</em>) on the supplied
	 * {@code clazz}.
	 * <p>The supplied {@link Class} may represent any type.
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>Note: This method does <strong>not</strong> determine if the annotation
	 * is {@linkplain java.lang.annotation.Inherited inherited}. For greater
	 * clarity regarding inherited annotations, consider using
	 * {@link #isAnnotationInherited(Class, Class)} instead.
	 * @param annotationType the annotation type to look for
	 * @param clazz the class to check for the annotation on
	 * @return {@code true} if an annotation of the specified {@code annotationType}
	 * is <em>directly present</em>
	 * @see java.lang.Class#getDeclaredAnnotations()
	 * @see java.lang.Class#getDeclaredAnnotation(Class)
	 * @see #isAnnotationInherited(Class, Class)
	 */
	public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)
		).withSkippedOriginalExceptionCheck().to(() ->
			MergedAnnotations.from(clazz).get(annotationType).isDirectlyPresent()
		);
	}

	/**
	 * Determine whether an annotation of the specified {@code annotationType}
	 * is <em>present</em> on the supplied {@code clazz} and is
	 * {@linkplain java.lang.annotation.Inherited inherited}
	 * (i.e. not <em>directly present</em>).
	 * <p>Meta-annotations will <em>not</em> be searched.
	 * <p>If the supplied {@code clazz} is an interface, only the interface
	 * itself will be checked. In accordance with standard meta-annotation
	 * semantics in Java, the inheritance hierarchy for interfaces will not
	 * be traversed. See the {@linkplain java.lang.annotation.Inherited javadoc}
	 * for the {@code @Inherited} meta-annotation for further details regarding
	 * annotation inheritance.
	 * @param annotationType the annotation type to look for
	 * @param clazz the class to check for the annotation on
	 * @return {@code true} if an annotation of the specified {@code annotationType}
	 * is <em>present</em> and <em>inherited</em>
	 * @see Class#isAnnotationPresent(Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 */
	public static boolean isAnnotationInherited(Class<? extends Annotation> annotationType, Class<?> clazz) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.isAnnotationInherited(annotationType, clazz)
		).to(() ->
			MergedAnnotations.from(clazz,
					SearchStrategy.INHERITED_ANNOTATIONS).stream(annotationType).filter(
						MergedAnnotation::isDirectlyPresent).findFirst().orElseGet(
								MergedAnnotation::missing).getAggregateIndex() > 0
		);
	}

	/**
	 * Determine if an annotation of type {@code metaAnnotationType} is
	 * <em>meta-present</em> on the supplied {@code annotationType}.
	 * @param annotationType the annotation type to search on
	 * @param metaAnnotationType the type of meta-annotation to search for
	 * @return {@code true} if such an annotation is meta-present
	 * @since 4.2.1
	 */
	public static boolean isAnnotationMetaPresent(Class<? extends Annotation> annotationType,
			@Nullable Class<? extends Annotation> metaAnnotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.isAnnotationMetaPresent(annotationType,
				metaAnnotationType)
		).to(() ->
			MergedAnnotations.from(annotationType, SearchStrategy.EXHAUSTIVE).isPresent(
					annotationType)
		);
	}

	/**
	 * Determine if the supplied {@link Annotation} is defined in the core JDK
	 * {@code java.lang.annotation} package.
	 * @param annotation the annotation to check
	 * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
	 */
	public static boolean isInJavaLangAnnotationPackage(@Nullable Annotation annotation) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.isInJavaLangAnnotationPackage(annotation)
		).to(() ->
			AnnotationFilter.JAVA.matches(annotation)
		);
	}

	/**
	 * Determine if the {@link Annotation} with the supplied name is defined
	 * in the core JDK {@code java.lang.annotation} package.
	 * @param annotationType the name of the annotation type to check
	 * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
	 * @since 4.2
	 */
	public static boolean isInJavaLangAnnotationPackage(@Nullable String annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.isInJavaLangAnnotationPackage(annotationType)
		).to(() ->
			AnnotationFilter.JAVA.matches(annotationType)
		);
	}

	/**
	 * Check the declared attributes of the given annotation, in particular covering
	 * Google App Engine's late arrival of {@code TypeNotPresentExceptionProxy} for
	 * {@code Class} values (instead of early {@code Class.getAnnotations() failure}.
	 * <p>This method not failing indicates that {@link #getAnnotationAttributes(Annotation)}
	 * won't failure either (when attempted later on).
	 * @param annotation the annotation to validate
	 * @throws IllegalStateException if a declared {@code Class} attribute could not be read
	 * @since 4.3.15
	 * @see Class#getAnnotations()
	 * @see #getAnnotationAttributes(Annotation)
	 */
	public static void validateAnnotation(Annotation annotation) {
		MigrateMethod.fromCall(() ->
			InternalAnnotationUtils.validateAnnotation(annotation)
		).to(() ->
			AttributeMethods.validate(annotation)
		);
	}

	/**
	 * Retrieve the given annotation's attributes as a {@link Map}, preserving all
	 * attribute types.
	 * <p>Equivalent to calling {@link #getAnnotationAttributes(Annotation, boolean, boolean)}
	 * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
	 * set to {@code false}.
	 * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
	 * However, the {@code Map} signature has been preserved for binary compatibility.
	 * @param annotation the annotation to retrieve the attributes for
	 * @return the Map of annotation attributes, with attribute names as keys and
	 * corresponding attribute values as values (never {@code null})
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
		return getAnnotationAttributes(null, annotation);
	}

	/**
	 * Retrieve the given annotation's attributes as a {@link Map}.
	 * <p>Equivalent to calling {@link #getAnnotationAttributes(Annotation, boolean, boolean)}
	 * with the {@code nestedAnnotationsAsMap} parameter set to {@code false}.
	 * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
	 * However, the {@code Map} signature has been preserved for binary compatibility.
	 * @param annotation the annotation to retrieve the attributes for
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @return the Map of annotation attributes, with attribute names as keys and
	 * corresponding attribute values as values (never {@code null})
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation, boolean classValuesAsString) {
		return getAnnotationAttributes(annotation, classValuesAsString, false);
	}

	/**
	 * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
	 * <p>This method provides fully recursive annotation reading capabilities on par with
	 * the reflection-based {@link org.springframework.core.type.StandardAnnotationMetadata}.
	 * @param annotation the annotation to retrieve the attributes for
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested annotations into
	 * {@link AnnotationAttributes} maps (for compatibility with
	 * {@link org.springframework.core.type.AnnotationMetadata}) or to preserve them as
	 * {@code Annotation} instances
	 * @return the annotation attributes (a specialized Map) with attribute names as keys
	 * and corresponding attribute values as values (never {@code null})
	 * @since 3.1.1
	 */
	public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, boolean classValuesAsString,
			boolean nestedAnnotationsAsMap) {
		return getAnnotationAttributes(null, annotation, classValuesAsString,
				nestedAnnotationsAsMap);
	}

	/**
	 * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
	 * <p>Equivalent to calling {@link #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)}
	 * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
	 * set to {@code false}.
	 * @param annotatedElement the element that is annotated with the supplied annotation;
	 * may be {@code null} if unknown
	 * @param annotation the annotation to retrieve the attributes for
	 * @return the annotation attributes (a specialized Map) with attribute names as keys
	 * and corresponding attribute values as values (never {@code null})
	 * @since 4.2
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static AnnotationAttributes getAnnotationAttributes(@Nullable AnnotatedElement annotatedElement, Annotation annotation) {
		return getAnnotationAttributes(annotatedElement, annotation, false, false);
	}

	/**
	 * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
	 * <p>This method provides fully recursive annotation reading capabilities on par with
	 * the reflection-based {@link org.springframework.core.type.StandardAnnotationMetadata}.
	 * @param annotatedElement the element that is annotated with the supplied annotation;
	 * may be {@code null} if unknown
	 * @param annotation the annotation to retrieve the attributes for
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested annotations into
	 * {@link AnnotationAttributes} maps (for compatibility with
	 * {@link org.springframework.core.type.AnnotationMetadata}) or to preserve them as
	 * {@code Annotation} instances
	 * @return the annotation attributes (a specialized Map) with attribute names as keys
	 * and corresponding attribute values as values (never {@code null})
	 * @since 4.2
	 */
	public static AnnotationAttributes getAnnotationAttributes(@Nullable AnnotatedElement annotatedElement,
			Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getAnnotationAttributes(annotatedElement,
					annotation, classValuesAsString, nestedAnnotationsAsMap)
		).to(() -> {
			ClassLoader classLoader = annotation.annotationType().getClassLoader();
			return MergedAnnotation.from(annotatedElement,
					annotation).withNonMergedAttributes().asMap(
							getAnnotationAttributesFactory(classLoader),
							MapValues.of(classValuesAsString, nestedAnnotationsAsMap));
		});
	}

	@SuppressWarnings("unchecked")
	private static Function<MergedAnnotation<?>, AnnotationAttributes> getAnnotationAttributesFactory(
			ClassLoader classLoader) {
		return annotation -> {
			Class<Annotation> type = (Class<Annotation>) ClassUtils.resolveClassName(
					annotation.getType(), classLoader);
			return new AnnotationAttributes(type, true);
		};
	}

	/**
	 * Register the annotation-declared default values for the given attributes,
	 * if available.
	 * @param attributes the annotation attributes to process
	 * @since 4.3.2
	 */
	public static void registerDefaultValues(AnnotationAttributes attributes) {
		AnnotationAttributes copy = new AnnotationAttributes(attributes);
		MigrateMethod.fromCall(()->
			InternalAnnotationUtils.registerDefaultValues(copy)
		).withArgumentCheck(copy, attributes).to(()-> {
			Class<? extends Annotation> annotationType = attributes.annotationType();
			if (annotationType != null && Modifier.isPublic(annotationType.getModifiers())) {
				getDefaultValues(annotationType).forEach((name, value) ->
					attributes.putIfAbsent(name, new DefaultValueHolder(value)));
			}
		});
	}

	private static Map<String, DefaultValueHolder> getDefaultValues(
			Class<? extends Annotation> annotationType) {
		return defaultValuesCache.computeIfAbsent(annotationType,
				AnnotationUtils::computeDefaultValues);
	}

	private static Map<String, DefaultValueHolder> computeDefaultValues(
			Class<? extends Annotation> annotationType) {
		AnnotationAttributes attributes = MergedAnnotation.from(annotationType).asMap(
				getAnnotationAttributesFactory(annotationType.getClassLoader()),
				MapValues.ANNOTATION_TO_MAP);
		Map<String, DefaultValueHolder> result = new LinkedHashMap<>(attributes.size());
		for (Map.Entry<String, Object> element : attributes.entrySet()) {
			result.put(element.getKey(), new DefaultValueHolder(element.getValue()));
		}
		return result;
	}

	/**
	 * Post-process the supplied {@link AnnotationAttributes}, preserving nested
	 * annotations as {@code Annotation} instances.
	 * <p>Specifically, this method enforces <em>attribute alias</em> semantics
	 * for annotation attributes that are annotated with {@link AliasFor @AliasFor}
	 * and replaces default value placeholders with their original default values.
	 * @param annotatedElement the element that is annotated with an annotation or
	 * annotation hierarchy from which the supplied attributes were created;
	 * may be {@code null} if unknown
	 * @param attributes the annotation attributes to post-process
	 * @param classValuesAsString whether to convert Class references into Strings (for
	 * compatibility with {@link org.springframework.core.type.AnnotationMetadata})
	 * or to preserve them as Class references
	 * @since 4.3.2
	 * @see #postProcessAnnotationAttributes(Object, AnnotationAttributes, boolean, boolean)
	 * @see #getDefaultValue(Class, String)
	 */
	public static void postProcessAnnotationAttributes(@Nullable Object annotatedElement,
			@Nullable AnnotationAttributes attributes, boolean classValuesAsString) {
		if (attributes == null) {
			return;
		}
		AnnotationAttributes copy = new AnnotationAttributes(attributes);
		MigrateMethod.fromCall(() ->
			InternalAnnotationUtils.postProcessAnnotationAttributes(annotatedElement, copy,
					classValuesAsString)
		).withArgumentCheck(copy, attributes).to(()-> {
			if (attributes == null || attributes.annotationType() == null) {
				return;
			}
			if (!attributes.validated) {
				Class<? extends Annotation> annotationType = attributes.annotationType();
				AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
						annotationType).get(0);
				for (int i = 0; i < mapping.getMirrorSets().size(); i++) {
					MirrorSet mirrorSet = mapping.getMirrorSets().get(i);
					int resolved = mirrorSet.resolve(attributes.displayName, attributes,
							AnnotationUtils::getAttributeValueForMirrorResolution);
					if (resolved != -1) {
						Method attribute = mapping.getAttributes().get(resolved);
						Object value = attributes.get(attribute.getName());
						for (int j = 0; j < mirrorSet.size(); j++) {
							Method mirror = mirrorSet.get(j);
							if (mirror != attribute) {
								attributes.put(mirror.getName(), adaptValue(
										annotatedElement, value, classValuesAsString));
							}
						}
					}
				}
			}
			for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
				String attributeName = attributeEntry.getKey();
				Object value = attributeEntry.getValue();
				if (value instanceof DefaultValueHolder) {
					value = ((DefaultValueHolder) value).defaultValue;
					attributes.put(attributeName,
							adaptValue(annotatedElement, value, classValuesAsString));
				}
			}
		});
	}

	private static Object getAttributeValueForMirrorResolution(Method attribute,
			Object attributes) {
		Object result = ((AnnotationAttributes) attributes).get(attribute.getName());
		return result instanceof DefaultValueHolder
				? ((DefaultValueHolder) result).defaultValue
				: result;
	}

	@Nullable
	private static Object adaptValue(@Nullable Object annotatedElement,
			@Nullable Object value, boolean classValuesAsString) {
		if (classValuesAsString) {
			if (value instanceof Class) {
				return ((Class<?>) value).getName();
			}
			if (value instanceof Class[]) {
				Class<?>[] classes = (Class<?>[]) value;
				String[] names = new String[classes.length];
				for (int i = 0; i < classes.length; i++) {
					names[i] = classes[i].getName();
				}
				return names;
			}
		}
		if (value instanceof Annotation) {
			Annotation annotation = (Annotation) value;
			return MergedAnnotation.from(annotatedElement, annotation).synthesize();
		}
		if (value instanceof Annotation[]) {
			Annotation[] annotations = (Annotation[]) value;
			Annotation[] synthesized = (Annotation[]) Array.newInstance(
					annotations.getClass().getComponentType(), annotations.length);
			for (int i = 0; i < annotations.length; i++) {
				synthesized[i] = MergedAnnotation.from(annotatedElement,
						annotations[i]).synthesize();
			}
			return synthesized;
		}
		return value;
	}

	/**
	 * Retrieve the <em>value</em> of the {@code value} attribute of a
	 * single-element Annotation, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the value
	 * @return the attribute value, or {@code null} if not found unless the attribute
	 * value cannot be retrieved due to an {@link AnnotationConfigurationException},
	 * in which case such an exception will be rethrown
	 * @see #getValue(Annotation, String)
	 */
	@Nullable
	public static Object getValue(Annotation annotation) {
		return getValue(annotation, VALUE);
	}

	/**
	 * Retrieve the <em>value</em> of a named attribute, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the value
	 * @param attributeName the name of the attribute value to retrieve
	 * @return the attribute value, or {@code null} if not found unless the attribute
	 * value cannot be retrieved due to an {@link AnnotationConfigurationException},
	 * in which case such an exception will be rethrown
	 * @see #getValue(Annotation)
	 * @see #rethrowAnnotationConfigurationException(Throwable)
	 */
	@Nullable
	public static Object getValue(@Nullable Annotation annotation, @Nullable String attributeName) {
		return InternalAnnotationUtils.getValue(annotation, attributeName);
	}

	/**
	 * Retrieve the <em>default value</em> of the {@code value} attribute
	 * of a single-element Annotation, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the default value
	 * @return the default value, or {@code null} if not found
	 * @see #getDefaultValue(Annotation, String)
	 */
	@Nullable
	public static Object getDefaultValue(Annotation annotation) {
		return getDefaultValue(annotation, VALUE);
	}

	/**
	 * Retrieve the <em>default value</em> of a named attribute, given an annotation instance.
	 * @param annotation the annotation instance from which to retrieve the default value
	 * @param attributeName the name of the attribute value to retrieve
	 * @return the default value of the named attribute, or {@code null} if not found
	 * @see #getDefaultValue(Class, String)
	 */
	@Nullable
	public static Object getDefaultValue(@Nullable Annotation annotation, @Nullable String attributeName) {
		return annotation != null ? getDefaultValue(annotation.annotationType(), attributeName) : null;
	}

	/**
	 * Retrieve the <em>default value</em> of the {@code value} attribute
	 * of a single-element Annotation, given the {@link Class annotation type}.
	 * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
	 * @return the default value, or {@code null} if not found
	 * @see #getDefaultValue(Class, String)
	 */
	@Nullable
	public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
		return getDefaultValue(annotationType, VALUE);
	}

	/**
	 * Retrieve the <em>default value</em> of a named attribute, given the
	 * {@link Class annotation type}.
	 * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
	 * @param attributeName the name of the attribute value to retrieve.
	 * @return the default value of the named attribute, or {@code null} if not found
	 * @see #getDefaultValue(Annotation, String)
	 */
	@Nullable
	public static Object getDefaultValue(
			@Nullable Class<? extends Annotation> annotationType, @Nullable String attributeName) {
		return MigrateMethod.from(() ->
			InternalAnnotationUtils.getDefaultValue(annotationType, attributeName)
		).to(() -> {
			if (annotationType == null || !StringUtils.hasText(attributeName)) {
				return null;
			}
			return MergedAnnotation.from(annotationType).getDefaultValue(attributeName).orElse(null);
		});
	}

	/**
	 * <em>Synthesize</em> an annotation from the supplied {@code annotation}
	 * by wrapping it in a dynamic proxy that transparently enforces
	 * <em>attribute alias</em> semantics for annotation attributes that are
	 * annotated with {@link AliasFor @AliasFor}.
	 * @param annotation the annotation to synthesize
	 * @param annotatedElement the element that is annotated with the supplied
	 * annotation; may be {@code null} if unknown
	 * @return the synthesized annotation if the supplied annotation is
	 * <em>synthesizable</em>; {@code null} if the supplied annotation is
	 * {@code null}; otherwise the supplied annotation unmodified
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 * @see #synthesizeAnnotation(Class)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(
			A annotation, @Nullable AnnotatedElement annotatedElement) {
		return synthesizeAnnotation(annotation, (Object) annotatedElement);
	}

	/**
	 * <em>Synthesize</em> an annotation from the supplied map of annotation
	 * attributes by wrapping the map in a dynamic proxy that implements an
	 * annotation of the specified {@code annotationType} and transparently
	 * enforces <em>attribute alias</em> semantics for annotation attributes
	 * that are annotated with {@link AliasFor @AliasFor}.
	 * <p>The supplied map must contain a key-value pair for every attribute
	 * defined in the supplied {@code annotationType} that is not aliased or
	 * does not have a default value. Nested maps and nested arrays of maps
	 * will be recursively synthesized into nested annotations or nested
	 * arrays of annotations, respectively.
	 * <p>Note that {@link AnnotationAttributes} is a specialized type of
	 * {@link Map} that is an ideal candidate for this method's
	 * {@code attributes} argument.
	 * @param attributes the map of annotation attributes to synthesize
	 * @param annotationType the type of annotation to synthesize
	 * @param annotatedElement the element that is annotated with the annotation
	 * corresponding to the supplied attributes; may be {@code null} if unknown
	 * @return the synthesized annotation
	 * @throws IllegalArgumentException if a required attribute is missing or if an
	 * attribute is not of the correct type
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 * @see #synthesizeAnnotation(Class)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(Map<String, Object> attributes,
			Class<A> annotationType, @Nullable AnnotatedElement annotatedElement) {
		return MigrateMethod.from(()->
			InternalAnnotationUtils.synthesizeAnnotation(attributes, annotationType,
					annotatedElement)
		).to(()-> {
			try {
				return MergedAnnotation.from(annotatedElement, annotationType,
						attributes).synthesize();
			}
			catch (NoSuchElementException | IllegalStateException ex) {
				throw new IllegalArgumentException(ex);
			}
		});
	}

	/**
	 * <em>Synthesize</em> an annotation from its default attributes values.
	 * <p>This method simply delegates to
	 * {@link #synthesizeAnnotation(Map, Class, AnnotatedElement)},
	 * supplying an empty map for the source attribute values and {@code null}
	 * for the {@link AnnotatedElement}.
	 * @param annotationType the type of annotation to synthesize
	 * @return the synthesized annotation
	 * @throws IllegalArgumentException if a required attribute is missing
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(Class<A> annotationType) {
		return synthesizeAnnotation(Collections.emptyMap(), annotationType, null);
	}

	/**
	 * <em>Synthesize</em> an array of annotations from the supplied array
	 * of {@code annotations} by creating a new array of the same size and
	 * type and populating it with {@linkplain #synthesizeAnnotation(Annotation)
	 * synthesized} versions of the annotations from the input array.
	 * @param annotations the array of annotations to synthesize
	 * @param annotatedElement the element that is annotated with the supplied
	 * array of annotations; may be {@code null} if unknown
	 * @return a new array of synthesized annotations, or {@code null} if
	 * the supplied array is {@code null}
	 * @throws AnnotationConfigurationException if invalid configuration of
	 * {@code @AliasFor} is detected
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 */
	static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, @Nullable Object annotatedElement) {
		if (AnnotationFilter.PLAIN.matches(getDeclaringClass(annotatedElement))) {
			return annotations;
		}
		Annotation[] synthesized = (Annotation[]) Array.newInstance(
				annotations.getClass().getComponentType(), annotations.length);
		for (int i = 0; i < annotations.length; i++) {
			synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
		}
		return synthesized;
	}

	private static <A extends Annotation> A synthesizeAnnotation(A annotation, @Nullable Object annotatedElement) {
		return MigrateMethod.from(()->
			InternalAnnotationUtils.synthesizeAnnotation(annotation, annotatedElement)
		).withSkippedOriginalExceptionCheck().to(() -> {
			if (AnnotationFilter.PLAIN.matches(getDeclaringClass(annotatedElement))) {
				return annotation;
			}
			return MergedAnnotation.from(annotatedElement, annotation).synthesize();
		});
	}

	private static Class<?> getDeclaringClass(@Nullable Object annotatedElement) {
		if (annotatedElement instanceof Class) {
			return (Class<?>) annotatedElement;
		}
		if (annotatedElement instanceof Member) {
			return ((Member) annotatedElement).getDeclaringClass();
		}
		return null;
	}

	/**
	 * Clear the internal annotation metadata cache.
	 * @since 4.3.15
	 */
	public static void clearCache() {
		InternalAnnotationUtils.clearCache();
	}

	private static <A extends Annotation> boolean isSingleLevelPresent(
			MergedAnnotation<A> mergedAnnotation) {
		int depth = mergedAnnotation.getDepth();
		return depth == 0 || depth == 1;
	}

}
