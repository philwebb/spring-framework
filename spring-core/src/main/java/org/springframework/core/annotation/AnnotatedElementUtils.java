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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import static org.hamcrest.Matchers.any;

/**
 * General utility methods for finding annotations, meta-annotations, and
 * repeatable annotations on {@link AnnotatedElement AnnotatedElements}.
 *
 * <p>{@code AnnotatedElementUtils} defines the public API for Spring's
 * meta-annotation programming model with support for <em>annotation attribute
 * overrides</em>. If you do not need support for annotation attribute
 * overrides, consider using {@link AnnotationUtils} instead.
 *
 * <p>Note that the features of this class are not provided by the JDK's
 * introspection facilities themselves.
 *
 * <h3>Annotation Attribute Overrides</h3>
 * <p>Support for meta-annotations with <em>attribute overrides</em> in
 * <em>composed annotations</em> is provided by all variants of the
 * {@code getMergedAnnotationAttributes()}, {@code getMergedAnnotation()},
 * {@code getAllMergedAnnotations()}, {@code getMergedRepeatableAnnotations()},
 * {@code findMergedAnnotationAttributes()}, {@code findMergedAnnotation()},
 * {@code findAllMergedAnnotations()}, and {@code findMergedRepeatableAnnotations()}
 * methods.
 *
 * <h3>Find vs. Get Semantics</h3>
 * <p>The search algorithms used by methods in this class follow either
 * <em>find</em> or <em>get</em> semantics. Consult the javadocs for each
 * individual method for details on which search algorithm is used.
 *
 * <p><strong>Get semantics</strong> are limited to searching for annotations
 * that are either <em>present</em> on an {@code AnnotatedElement} (i.e. declared
 * locally or {@linkplain java.lang.annotation.Inherited inherited}) or declared
 * within the annotation hierarchy <em>above</em> the {@code AnnotatedElement}.
 *
 * <p><strong>Find semantics</strong> are much more exhaustive, providing
 * <em>get semantics</em> plus support for the following:
 *
 * <ul>
 * <li>Searching on interfaces, if the annotated element is a class
 * <li>Searching on superclasses, if the annotated element is a class
 * <li>Resolving bridged methods, if the annotated element is a method
 * <li>Searching on methods in interfaces, if the annotated element is a method
 * <li>Searching on methods in superclasses, if the annotated element is a method
 * </ul>
 *
 * <h3>Support for {@code @Inherited}</h3>
 * <p>Methods following <em>get semantics</em> will honor the contract of Java's
 * {@link java.lang.annotation.Inherited @Inherited} annotation except that locally
 * declared annotations (including custom composed annotations) will be favored over
 * inherited annotations. In contrast, methods following <em>find semantics</em>
 * will completely ignore the presence of {@code @Inherited} since the <em>find</em>
 * search algorithm manually traverses type and method hierarchies and thereby
 * implicitly supports annotation inheritance without a need for {@code @Inherited}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotationUtils
 * @see BridgeMethodResolver
 */
public abstract class AnnotatedElementUtils {

	/**
	 * Build an adapted {@link AnnotatedElement} for the given annotations,
	 * typically for use with other methods on {@link AnnotatedElementUtils}.
	 * @param annotations the annotations to expose through the {@code AnnotatedElement}
	 * @since 4.3
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations#of(annotations)}
	 */
	@Deprecated
	public static AnnotatedElement forAnnotations(final Annotation... annotations) {
		return InternalAnnotatedElementUtils.forAnnotations(annotations);
	}

	/**
	 * Get the fully qualified class names of all meta-annotation types
	 * <em>present</em> on the annotation (of the specified {@code annotationType})
	 * on the supplied {@link AnnotatedElement}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type on which to find meta-annotations
	 * @return the names of all meta-annotations present on the annotation,
	 * or {@code null} if not found
	 * @since 4.2
	 * @see #getMetaAnnotationTypes(AnnotatedElement, String)
	 * @see #hasMetaAnnotationTypes
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMetaAnnotationTypes(element, annotationType)
		).withDescription(() -> element + " " + annotationType
		).to(() ->
			getMetaAnnotationTypes(element, element.getAnnotation(annotationType))
		);
	}

	/**
	 * Get the fully qualified class names of all meta-annotation
	 * types <em>present</em> on the annotation (of the specified
	 * {@code annotationName}) on the supplied {@link AnnotatedElement}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation
	 * type on which to find meta-annotations
	 * @return the names of all meta-annotations present on the annotation,
	 * or an empty set if none found
	 * @see #getMetaAnnotationTypes(AnnotatedElement, Class)
	 * @see #hasMetaAnnotationTypes
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMetaAnnotationTypes(element, annotationName)
		).withDescription(() -> element + " " + annotationName
		).to(() -> {
			for (Annotation annotation : element.getAnnotations()) {
				if (annotation.annotationType().getName().equals(annotationName)) {
					return getMetaAnnotationTypes(element, annotation);
				}
			}
			return Collections.emptySet();
		});
	}

	private static Set<String> getMetaAnnotationTypes(AnnotatedElement element, Annotation annotation) {
		if (annotation == null) {
			return Collections.emptySet();
		}
		return MergedAnnotations.from(annotation.annotationType(),
				SearchStrategy.INHERITED_ANNOTATIONS,
				RepeatableContainers.none()).stream().map(
						MergedAnnotation::getType).collect(
								Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Determine if the supplied {@link AnnotatedElement} is annotated with
	 * a <em>composed annotation</em> that is meta-annotated with an
	 * annotation of the specified {@code annotationType}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the meta-annotation type to find
	 * @return {@code true} if a matching meta-annotation is present
	 * @since 4.2.3
	 * @see #getMetaAnnotationTypes
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.hasMetaAnnotationTypes(element, annotationType)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).stream(annotationType).anyMatch(
							MergedAnnotation::isMetaPresent)
		);
	}

	/**
	 * Determine if the supplied {@link AnnotatedElement} is annotated with a
	 * <em>composed annotation</em> that is meta-annotated with an annotation
	 * of the specified {@code annotationName}.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the
	 * meta-annotation type to find
	 * @return {@code true} if a matching meta-annotation is present
	 * @see #getMetaAnnotationTypes
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.hasMetaAnnotationTypes(element, annotationName)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).stream(annotationName).anyMatch(
							MergedAnnotation::isMetaPresent)
		);
	}

	/**
	 * Determine if an annotation of the specified {@code annotationType}
	 * is <em>present</em> on the supplied {@link AnnotatedElement} or
	 * within the annotation hierarchy <em>above</em> the specified element.
	 * <p>If this method returns {@code true}, then {@link #getMergedAnnotationAttributes}
	 * will return a non-null value.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return {@code true} if a matching annotation is present
	 * @since 4.2.3
	 * @see #hasAnnotation(AnnotatedElement, Class)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.isAnnotated(element, annotationType)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).isPresent(annotationType)
		);
	}

	/**
	 * Determine if an annotation of the specified {@code annotationName} is
	 * <em>present</em> on the supplied {@link AnnotatedElement} or within the
	 * annotation hierarchy <em>above</em> the specified element.
	 * <p>If this method returns {@code true}, then {@link #getMergedAnnotationAttributes}
	 * will return a non-null value.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @return {@code true} if a matching annotation is present
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static boolean isAnnotated(AnnotatedElement element, String annotationName) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.isAnnotated(element, annotationName))
		.to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).isPresent(annotationName)
		);
	}

	/**
	 * Get the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(
			AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMergedAnnotationAttributes(element,
				annotationType))
		.to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).get(annotationType).asMap(
							AnnotationAttributes::createIfAnnotationPresent)
		);
	}

	/**
	 * Get the first annotation of the specified {@code annotationName} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)},
	 * supplying {@code false} for {@code classValuesAsString} and {@code nestedAnnotationsAsMap}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element, String annotationName) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMergedAnnotationAttributes(element, annotationName)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).get(annotationName).asMap(
							AnnotationAttributes::createIfAnnotationPresent)
		);
	}

	/**
	 * Get the first annotation of the specified {@code annotationName} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>Attributes from lower levels in the annotation hierarchy override attributes
	 * of the same name from higher levels, and {@link AliasFor @AliasFor} semantics are
	 * fully supported, both within a single annotation and within the annotation hierarchy.
	 * <p>In contrast to {@link #getAllAnnotationAttributes}, the search algorithm used by
	 * this method will stop searching the annotation hierarchy once the first annotation
	 * of the specified {@code annotationName} has been found. As a consequence,
	 * additional annotations of the specified {@code annotationName} will be ignored.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @param classValuesAsString whether to convert Class references into Strings or to
	 * preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances
	 * into {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMergedAnnotationAttributes(element,
				annotationName, classValuesAsString, nestedAnnotationsAsMap)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).get(annotationName).asMap(
							AnnotationAttributes::createIfAnnotationPresent,
							MapValues.get(classValuesAsString, nestedAnnotationsAsMap, false))
		);
	}

	/**
	 * Get the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element},
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy, and synthesize
	 * the result back into an annotation of the specified {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>This method delegates to {@link #getMergedAnnotationAttributes(AnnotatedElement, Class)}
	 * and {@link AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, Class)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see AnnotationUtils#synthesizeAnnotation(Map, Class, AnnotatedElement)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	@Nullable
	public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMergedAnnotation(element, annotationType)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).get(annotationType).synthesize(
							MergedAnnotation::isPresent).orElse(null)
		);
	}

	/**
	 * Get <strong>all</strong> annotations of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getAllMergedAnnotations(AnnotatedElement element, Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getAllMergedAnnotations(element,
					annotationType)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).stream(annotationType).map(
							MergedAnnotation::synthesize).collect(
									Collectors.toCollection(LinkedHashSet::new))
		);
	}

	/**
	 * Get <strong>all</strong> annotations of the specified {@code annotationTypes}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the
	 * annotation hierarchy and synthesize the results back into an annotation
	 * of the corresponding {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationTypes the annotation types to find
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 5.1
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static Set<Annotation> getAllMergedAnnotations(AnnotatedElement element, Set<Class<? extends Annotation>> annotationTypes) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getAllMergedAnnotations(element,
					annotationTypes)
		).to(() -> {
			Predicate<MergedAnnotation<?>> filter = typeNameFilter(annotationTypes);
			return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).stream().filter(filter).map(
							MergedAnnotation::synthesize).collect(
									Collectors.toCollection(LinkedHashSet::new));
		});
	}

	/**
	 * Get all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>The container type that holds the repeatable annotations will be looked up
	 * via {@link java.lang.annotation.Repeatable}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMergedRepeatableAnnotations(element,
					annotationType)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.standardRepeatables()).stream(annotationType).sorted(
							MergedAnnotation.comparingDepth()).map(
									MergedAnnotation::synthesize).collect(
											Collectors.toCollection(LinkedHashSet::new))
		);
	}

	/**
	 * Get all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @param containerType the type of the container that holds the annotations;
	 * may be {@code null} if the container type should be looked up via
	 * {@link java.lang.annotation.Repeatable}
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @throws AnnotationConfigurationException if the supplied {@code containerType}
	 * is not a valid container annotation for the supplied {@code annotationType}
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerType) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getMergedRepeatableAnnotations(element,
					annotationType, containerType)
		).to(() -> {
			RepeatableContainers repeatableContainers = containerType != null
					? RepeatableContainers.of(containerType, annotationType)
					: RepeatableContainers.standardRepeatables();
			return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					repeatableContainers).stream(annotationType).map(
							MergedAnnotation::synthesize).collect(
									Collectors.toCollection(LinkedHashSet::new));
		});
	}

	/**
	 * Get the annotation attributes of <strong>all</strong> annotations of the specified
	 * {@code annotationName} in the annotation hierarchy above the supplied
	 * {@link AnnotatedElement} and store the results in a {@link MultiValueMap}.
	 * <p>Note: in contrast to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)},
	 * this method does <em>not</em> support attribute overrides.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @return a {@link MultiValueMap} keyed by attribute name, containing the annotation
	 * attributes from all annotations found, or {@code null} if not found
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @deprecated since 5.2 in favor of {@link MergedAnnotations}
	 */
	@Deprecated
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element, String annotationName) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getAllAnnotationAttributes(element,
					annotationName)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).stream(annotationName).filter(
							oncePerParent()).peek(System.out::println).collect(
									allAnnotationAttributes(MapValues.NON_MERGED))
		);
	}

	/**
	 * Get the annotation attributes of <strong>all</strong> annotations of
	 * the specified {@code annotationName} in the annotation hierarchy above
	 * the supplied {@link AnnotatedElement} and store the results in a
	 * {@link MultiValueMap}.
	 * <p>Note: in contrast to {@link #getMergedAnnotationAttributes(AnnotatedElement, String)},
	 * this method does <em>not</em> support attribute overrides.
	 * <p>This method follows <em>get semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @param classValuesAsString whether to convert Class references into Strings or to
	 * preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances into
	 * {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return a {@link MultiValueMap} keyed by attribute name, containing the annotation
	 * attributes from all annotations found, or {@code null} if not found
	 */
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
			String annotationName, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {
		return MigrateMethod.from(() ->
			InternalAnnotatedElementUtils.getAllAnnotationAttributes(element,
					annotationName, classValuesAsString, nestedAnnotationsAsMap)
		).to(() ->
			MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS,
					RepeatableContainers.none()).stream(annotationName).filter(
							oncePerParent()).peek(System.out::println).collect(
									allAnnotationAttributes(MapValues.get(classValuesAsString,
											nestedAnnotationsAsMap, true)))
		);
	}

	/**
	 * Determine if an annotation of the specified {@code annotationType}
	 * is <em>available</em> on the supplied {@link AnnotatedElement} or
	 * within the annotation hierarchy <em>above</em> the specified element.
	 * <p>If this method returns {@code true}, then {@link #findMergedAnnotationAttributes}
	 * will return a non-null value.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return {@code true} if a matching annotation is present
	 * @since 4.3
	 * @see #isAnnotated(AnnotatedElement, Class)
	 */
	public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return InternalAnnotatedElementUtils.hasAnnotation(element, annotationType);
	}

	/**
	 * Find the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>Attributes from lower levels in the annotation hierarchy override
	 * attributes of the same name from higher levels, and
	 * {@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>In contrast to {@link #getAllAnnotationAttributes}, the search algorithm
	 * used by this method will stop searching the annotation hierarchy once the
	 * first annotation of the specified {@code annotationType} has been found.
	 * As a consequence, additional annotations of the specified
	 * {@code annotationType} will be ignored.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param classValuesAsString whether to convert Class references into
	 * Strings or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances into
	 * {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			Class<? extends Annotation> annotationType, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
		return InternalAnnotatedElementUtils.findMergedAnnotationAttributes(element,
				annotationType, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * Find the first annotation of the specified {@code annotationName} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element} and
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy.
	 * <p>Attributes from lower levels in the annotation hierarchy override
	 * attributes of the same name from higher levels, and
	 * {@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>In contrast to {@link #getAllAnnotationAttributes}, the search
	 * algorithm used by this method will stop searching the annotation
	 * hierarchy once the first annotation of the specified
	 * {@code annotationName} has been found. As a consequence, additional
	 * annotations of the specified {@code annotationName} will be ignored.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationName the fully qualified class name of the annotation type to find
	 * @param classValuesAsString whether to convert Class references into Strings or to
	 * preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to convert nested Annotation instances into
	 * {@code AnnotationAttributes} maps or to preserve them as Annotation instances
	 * @return the merged {@code AnnotationAttributes}, or {@code null} if not found
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
		return InternalAnnotatedElementUtils.findMergedAnnotationAttributes(element,
				annotationName, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * Find the first annotation of the specified {@code annotationType} within
	 * the annotation hierarchy <em>above</em> the supplied {@code element},
	 * merge that annotation's attributes with <em>matching</em> attributes from
	 * annotations in lower levels of the annotation hierarchy, and synthesize
	 * the result back into an annotation of the specified {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both
	 * within a single annotation and within the annotation hierarchy.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @return the merged, synthesized {@code Annotation}, or {@code null} if not found
	 * @since 4.2
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		return InternalAnnotatedElementUtils.findMergedAnnotation(element,
				annotationType);
	}

	/**
	 * Find <strong>all</strong> annotations of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findAllMergedAnnotations(AnnotatedElement element, Class<A> annotationType) {
		return InternalAnnotatedElementUtils.findAllMergedAnnotations(element,
				annotationType);
	}

	/**
	 * Find <strong>all</strong> annotations of the specified {@code annotationTypes}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the
	 * annotation hierarchy and synthesize the results back into an annotation
	 * of the corresponding {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationTypes the annotation types to find
	 * @return the set of all merged, synthesized {@code Annotations} found,
	 * or an empty set if none were found
	 * @since 5.1
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static Set<Annotation> findAllMergedAnnotations(AnnotatedElement element, Set<Class<? extends Annotation>> annotationTypes) {
		return InternalAnnotatedElementUtils.findAllMergedAnnotations(element,
				annotationTypes);
	}

	/**
	 * Find all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>The container type that holds the repeatable annotations will be looked up
	 * via {@link java.lang.annotation.Repeatable}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {
		return InternalAnnotatedElementUtils.findMergedRepeatableAnnotations(element,
				annotationType);
	}

	/**
	 * Find all <em>repeatable annotations</em> of the specified {@code annotationType}
	 * within the annotation hierarchy <em>above</em> the supplied {@code element};
	 * and for each annotation found, merge that annotation's attributes with
	 * <em>matching</em> attributes from annotations in lower levels of the annotation
	 * hierarchy and synthesize the results back into an annotation of the specified
	 * {@code annotationType}.
	 * <p>{@link AliasFor @AliasFor} semantics are fully supported, both within a
	 * single annotation and within annotation hierarchies.
	 * <p>This method follows <em>find semantics</em> as described in the
	 * {@linkplain AnnotatedElementUtils class-level javadoc}.
	 * @param element the annotated element (never {@code null})
	 * @param annotationType the annotation type to find (never {@code null})
	 * @param containerType the type of the container that holds the annotations;
	 * may be {@code null} if the container type should be looked up via
	 * {@link java.lang.annotation.Repeatable}
	 * @return the set of all merged repeatable {@code Annotations} found,
	 * or an empty set if none were found
	 * @throws IllegalArgumentException if the {@code element} or {@code annotationType}
	 * is {@code null}, or if the container type cannot be resolved
	 * @throws AnnotationConfigurationException if the supplied {@code containerType}
	 * is not a valid container annotation for the supplied {@code annotationType}
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerType) {
		return InternalAnnotatedElementUtils.findMergedRepeatableAnnotations(element,
				annotationType, containerType);
	}

	private static Predicate<MergedAnnotation<?>> typeNameFilter(Set<Class<? extends Annotation>> annotationTypes) {
		Assert.notNull(annotationTypes, "AnnotationTypes must not be null");
		Set<String> annotationNames = annotationTypes.stream().map(
				Class::getName).collect(Collectors.toSet());
		return annotation -> annotationNames.contains(annotation.getType());
	}

	private static Collector<MergedAnnotation<?>, ?, MultiValueMap<String, Object>> allAnnotationAttributes(
			MapValues... options) {
		Supplier<MultiValueMap<String, Object>> supplier = LinkedMultiValueMap::new;
		return Collector.of(supplier,
				(map, annotation) -> annotation.asMap(options).forEach(map::add),
				AnnotatedElementUtils::merge, AnnotatedElementUtils::mapOrNull);
	}

	private static <K, V> MultiValueMap<K, V> merge(MultiValueMap<K, V> map,
			MultiValueMap<K, V> additions) {
		map.addAll(additions);
		return map;
	}

	private static <K, V> MultiValueMap<K, V> mapOrNull(MultiValueMap<K, V> map) {
		return map.isEmpty() ? null : map;
	}

	private static Predicate<MergedAnnotation<?>> oncePerParent() {
		Set<PerParentReference> seen = new HashSet<>();
		return annotaion -> seen.add(new PerParentReference(annotaion));
	}

	/**
	 * Reference to a annotation type and parent type used for once-per-parent
	 * filtering.
	 */
	private static class PerParentReference {

		private final String type;

		private final String parentType;

		public PerParentReference(MergedAnnotation<?> annotaion) {
			this.type = annotaion.getType();
			this.parentType = annotaion.getParent() != null
					? annotaion.getParent().getType()
					: null;
		}

		@Override
		public int hashCode() {
			return this.type.hashCode() * 31
					+ ObjectUtils.nullSafeHashCode(this.parentType);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			PerParentReference other = (PerParentReference) obj;
			return this.type.equals(other.type)
					&& ObjectUtils.nullSafeEquals(this.parentType, other.parentType);
		}

	}

}
