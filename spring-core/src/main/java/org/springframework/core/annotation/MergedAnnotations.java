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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.lang.Nullable;

/**
 * Provides access to a collection of merged annotations, usually obtained from
 * a from a source such as a {@link Class} or {@link Method}. Each merged
 * annotation represent a view where the attribute values may be "merged" from
 * different source values, typically:
 * <ul>
 * <li>Explicit and Implicit {@link AliasFor @AliasFor} declarations on one or
 * attributes within the annotation.</li>
 * <li>Explicit {@link AliasFor @AliasFor} declarations for a
 * meta-annotation.</li>
 * <li>Convention based attribute aliases for a meta-annotation</li>
 * <li>From a meta-annotation declaration.</li>
 * </ul>
 * <p>
 * For example, a {@code @PostMapping} annotation might be defined as follows:
 *
 * <pre class="code">
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;RequestMapping(method = RequestMethod.POST)
 * public &#064;interface PostMapping {
 *
 * 	&#064;AliasFor(attribute = "path")
 * 	String[] value() default {};
 *
 * 	&#064;AliasFor(attribute = "value")
 * 	String[] path() default {};
 *
 * }
 * </pre>
 *
 * If a method is annotated with {@code @PostMapping("/home")} it will contain
 * merged annotations for both {@code @PostMapping} and the meta-annotation
 * {@code @RequestMapping}. The merged view of the {@code @RequestMapping}
 * annotation will contain the following attributes:
 * <p>
 * <table>
 * <tr>
 * <th>Name</th>
 * <th>Value</th>
 * <th>Source</th>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>"/home"</td>
 * <td>Declared {@code @PostMapping}</td>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>"/home"</td>
 * <td>Explicit {@code @AliasFor}</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>RequestMethod.POST</td>
 * <td>Declared meta-annotation</td>
 * </tr>
 * </table>
 * <p>
 * {@link MergedAnnotations} can be obtained {@link #from(AnnotatedElement)
 * from} any Java {@link AnnotatedElement}. They may also used for sources that
 * don't use reflection (such as those that directly parse bytecode).
 * <p>
 * Different {@link SearchStrategy search strategies} can be used to locate
 * related source elements that contain the annotations to be aggregated
 * together. For example, {@link SearchStrategy#EXHAUSTIVE} will search both
 * superclasses and implemented interfaces.
 * <p>
 * From a {@link MergedAnnotations} instance you can either {@link #get(String)}
 * a single annotation, or {@link #stream() stream all annotations} or just
 * those that match {@link #stream(String) a specific type}. You can also
 * quickly tell if an annotation {@link #isPresent(String) is present}.
 * <p>
 * Here are some typical example:
 *
 * <pre class="code">
 * // is an annotation present or meta-present
 * mergedAnnotations.isPresent(ExampleAnnotation.class);
 *
 * // get the merged "value" attribute of ExampleAnnotation (either direct or
 * // meta-present)
 * mergedAnnotations.get(ExampleAnnotation.class).getString("value");
 *
 * // get all meta-annotations but no direct annotations
 * mergedAnnotations.stream().anyMatch(MergedAnnotation::isMetaPresent);
 *
 * // get all ExampleAnnotation declarations (include any meta-annotations) and
 * // print the merged "value" attributes
 * mergedAnnotations.stream(ExampleAnnotation.class).map(
 * 		a -> a.getString("value")).forEach(System.out::println);
 * </pre>
 *
 * @author Phillip Webb
 * @since 5.2
 * @see MergedAnnotation
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {

	/**
	 * Return if the specified annotation is either directly present, or
	 * meta-present. Equivalent to calling
	 * {@code get(annotationType).isPresent()}.
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is present
	 */
	<A extends Annotation> boolean isPresent(Class<A> annotationType);

	/**
	 * Return if the specified annotation is either directly present, or
	 * meta-present. Equivalent to calling
	 * {@code get(annotationType).isPresent()}.
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is present
	 */
	<A extends Annotation> boolean isPresent(String annotationType);

	/**
	 * Return the nearest matching annotation or meta-annotation of the
	 * specified type, or {@link MergedAnnotation#missing()} if none is present.
	 * @param annotationType the annotation type to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

	/**
	 * Return the nearest matching annotation or meta-annotation of the
	 * specified type, or {@link MergedAnnotation#missing()} if none is present.
	 * @param annotationType the annotation type to get
	 * @param predicate a predicate that must match, or {@code null} if only
	 * type matching is required
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * Return the nearest matching annotation or meta-annotation of the
	 * specified type, or {@link MergedAnnotation#missing()} if none is present.
	 * @param annotationType the annotation type to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType);

	/**
	 * Return the nearest matching annotation or meta-annotation of the
	 * specified type, or {@link MergedAnnotation#missing()} if none is present.
	 * @param annotationType the annotation type to get
	 * @param predicate a predicate that must match, or {@code null} if only
	 * type matching is required
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * Get all annotations and meta-annotations that match the specified
	 * type. The resulting stream follows the same ordering rules are
	 * {@link #getAll()}.
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Set<MergedAnnotation<A>> getAll(Class<A> annotationType);

	/**
	 * Get all annotations and meta-annotations that match the specified
	 * type.The resulting stream follows the same ordering rules are
	 * {@link #getAll()}.
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Set<MergedAnnotation<A>> getAll(String annotationType);

	/**
	 * Get all contained annotations and meta-annotations contained in this
	 * collection. The resulting stream is ordered first by the
	 * {@link MergedAnnotation#getAggregateIndex() aggregate index}, and then by
	 * the annotation depth (with the closest annotations first). This ordering
	 * means that, for most use-cases, the most suitable annotations appear
	 * earliest in the stream.
	 * @return a stream of annotations
	 */
	Set<MergedAnnotation<Annotation>> getAll();

	/**
	 * Stream all annotations and meta-annotations that match the specified
	 * type. The resulting stream follows the same ordering rules are
	 * {@link #stream()}.
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

	/**
	 * Stream all annotations and meta-annotations that match the specified
	 * type.The resulting stream follows the same ordering rules are
	 * {@link #stream()}.
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

	/**
	 * Stream all contained annotations and meta-annotations contained in this
	 * collection. The resulting stream is ordered first by the
	 * {@link MergedAnnotation#getAggregateIndex() aggregate index}, and then by
	 * the annotation depth (with the closest annotations first). This ordering
	 * means that, for most use-cases, the most suitable annotations appear
	 * earliest in the stream.
	 * @return a stream of annotations
	 */
	Stream<MergedAnnotation<Annotation>> stream();

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element. The
	 * resulting instance will not include any inherited annotations, if you
	 * want to include those as well you should use
	 * {@link #from(AnnotatedElement, SearchStrategy)} with an appropriate
	 * {@link SearchStrategy}.
	 * @param element the source element
	 * @return a {@link MergedAnnotations} instance containing the element
	 * annotations
	 */
	static MergedAnnotations from(AnnotatedElement element) {
		return from(element, SearchStrategy.DIRECT);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 * @param element the source element
	 * @param searchStrategy the search strategy to use
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element,
			SearchStrategy searchStrategy) {
		return from(element, searchStrategy,
				RepeatableContainers.standardRepeatables(), AnnotationFilter.PLAIN);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 * @param element the source element
	 * @param searchStrategy the search strategy to use
	 * @param repeatableContainers the repeatable containers that may be used by
	 * the element annotations or the meta-annotations
	 * @param annotationFilter an annotation filter used to restrict the
	 * annotations considered. Note that the filter is used as a cache key so
	 * should ideally be a shared singleton instance
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element,
			SearchStrategy searchStrategy, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, annotationFilter);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see #of(AnnotatedElement, Annotation...)
	 * @see #from(AnnotatedElement)
	 */
	static MergedAnnotations from(Annotation... annotations) {
		return from(null, annotations);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 * @param source the source for the annotations. This source is used only
	 * for information and logging. It does not need to <em>actually</em>
	 * contain the specified annotations and it will not be searched.
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see #of(Annotation...)
	 * @see #from(AnnotatedElement)
	 */
	static MergedAnnotations from(@Nullable AnnotatedElement source,
			Annotation... annotations) {
		return from(source, annotations,
				RepeatableContainers.standardRepeatables(), AnnotationFilter.PLAIN);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 * @param source the source for the annotations. This source is used only
	 * for information and logging. It does not need to <em>actually</em>
	 * contain the specified annotations and it will not be searched.
	 * @param annotations the annotations to include
	 * @param repeatableContainers the repeatable containers that may be used by
	 * meta-annotations
	 * @param annotationFilter an annotation filter used to restrict the
	 * annotations considered. Note that the filter is used as a cache key so
	 * should ideally be a shared singleton instance
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations from(@Nullable AnnotatedElement source,
			Annotation[] annotations, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return TypeMappedAnnotations.from(source, annotations, repeatableContainers,
				annotationFilter);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations of the specified aggregates.
	 * @param aggregates the aggregates that make up the merged annotation in
	 * priority order. Commonly used to represent a complete class hierarchy
	 * @param repeatableContainers the repeatable containers that may be used by
	 * the element annotations or the meta-annotations
	 * @param annotationFilter an annotation filter used to restrict the
	 * annotations considered. Note that the filter is used as a cache key so
	 * should ideally be a shared singleton instance
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * annotations
	 */
	static MergedAnnotations of(Iterable<DeclaredAnnotations> aggregates,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
		return of(null, aggregates, repeatableContainers, annotationFilter);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations of the specified aggregates.
	 * @param classLoader the class loader used to read annotations
	 * @param aggregates the aggregates that make up the merged annotation in
	 * priority order. Commonly used to represent a complete class hierarchy
	 * @param repeatableContainers the repeatable containers that may be used by
	 * the element annotations or the meta-annotations
	 * @param annotationFilter an annotation filter used to restrict the
	 * annotations considered. Note that the filter is used as a cache key so
	 * should ideally be a shared singleton instance
	 * {@link AnnotationFilter#PLAIN} is recommended for most purposes.
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * annotations
	 */
	static MergedAnnotations of(@Nullable ClassLoader classLoader,
			Iterable<DeclaredAnnotations> aggregates, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return TypeMappedAnnotations.of(classLoader, aggregates,
				repeatableContainers, annotationFilter);
	}

	/**
	 * Search strategies supported by
	 * {@link MergedAnnotations#from(AnnotatedElement, SearchStrategy)}. Each
	 * strategy creates a different set of aggregates that will be combined to
	 * create the final {@link MergedAnnotations}.
	 */
	public static enum SearchStrategy {

		/**
		 * Find only directly declared annotations, without considering
		 * {@link Inherited @Inherited} annotations and without searching
		 * super-classes or implemented interfaces.
		 */
		DIRECT,

		/**
		 * Find all directly declared annotations as well any
		 * {@link Inherited @Inherited} super-class annotations. This strategy
		 * is only really useful when used with {@link Class} types since the
		 * {@link Inherited @Inherited} annotation is ignored for all other
		 * {@link AnnotatedElement annotated elements}. This strategy does not
		 * search implemented interfaces.
		 */
		INHERITED_ANNOTATIONS,

		/**
		 * Find all directly declared and super-class annotations. This strategy
		 * is similar to {@link #INHERITED_ANNOTATIONS} except the annotations
		 * do not need to be meta-annotated with {@link Inherited @Inherited}.
		 * This strategy does not search implemented interfaces.
		 */
		SUPER_CLASS,

		/**
		 * Perform a full search of all related elements, include those on any
		 * super-classes or implemented interfaces. Superclass annotations do
		 * not need to be meta-annotated with {@link Inherited @Inherited}.
		 */
		EXHAUSTIVE;

	}

}
