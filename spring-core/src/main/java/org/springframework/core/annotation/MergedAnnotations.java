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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.util.Assert;

/**
 * Provides access to a collection of merged annotations from a specific source.
 * Merged annotations represent a view of an annotation where the attribute
 * values may be "merged" form different sources, typically:
 * <ul>
 * <li>Explicit and Implicit {@link AliasFor @AliasFor} declarations on one or
 * attributes within the annotations.</li>
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
 * {@link MergedAnnotations} can be obtained {@link #from(AnnotatedElement) for}
 * any Java {@link AnnotatedElement}. They may also used from sources that don't
 * use reflection, but instead directly parse bytecode. From a
 * {@link MergedAnnotations} instance you can either {@link #get(String)} a
 * single annotation, or stream {@link #stream() stream all annotations} or just
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
 * @since 5.1
 * @see MergedAnnotation
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<?>> {

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
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType);

	/**
	 * Stream all matching annotations or meta-annotations that match the
	 * specified type.
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

	/**
	 * Stream all matching annotations or meta-annotations that match the
	 * specified type.
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

	/**
	 * Stream all contained annotations and meta-annotations contained in this
	 * collection.
	 * @return a stream of annotations
	 */
	Stream<MergedAnnotation<?>> stream();

	/**
	 * Create a new {@link MergedAnnotations} instance containing the specified
	 * annotations.
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations of(Annotation... annotations) {
		Assert.notNull(annotations, "Annotations must not be null");
		List<MappableAnnotation> mappableAnnotations = new ArrayList<>(
				annotations.length);
		for (Annotation annotation : annotations) {
			AnnotationTypeResolver resolver = AnnotationTypeResolver.get(
					annotation.annotationType().getClassLoader());
			DeclaredAnnotation declaredAnnotation = DeclaredAnnotation.from(annotation);
			MappableAnnotation mappableAnnotation = new MappableAnnotation(resolver,
					RepeatableContainers.standardRepeatables(), declaredAnnotation, null);
			mappableAnnotations.add(mappableAnnotation);
		}
		return new MappedAnnotations(mappableAnnotations);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element. The
	 * resulting instance will not include inherited annotations, if you want to
	 * include those as well you should use
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
	 * @param repeatableContainers the strategy used to find repeatable
	 * annotation containers
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element,
			SearchStrategy searchStrategy) {
		return from(element, searchStrategy, RepeatableContainers.standardRepeatables());
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 * @param element the source element
	 * @param searchStrategy the search strategy to use
	 * @param repeatableContainers the strategy used to find repeatable
	 * annotation containers
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers) {
		Assert.notNull(element, "Element must not be null");
		AnnotationTypeResolver resolver = AnnotationTypeResolver.get(element);
		AnnotationsScanner annotations = new AnnotationsScanner(
				element, searchStrategy);
		return from(annotations, resolver, repeatableContainers);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified source. The source
	 * may return one immediate set of {@link DeclaredAnnotations} as well as
	 * any number of additional {@link MergedAnnotation#isFromInherited()
	 * inherited} annotations.
	 * @param annotations the source of the annotations
	 * @param resolver an annotation type resolver
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * annotations
	 */
	static MergedAnnotations from(Iterable<DeclaredAnnotations> annotations,
			AnnotationTypeResolver resolver) {
		return from(annotations, resolver, RepeatableContainers.standardRepeatables());
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified source. The source
	 * may return one immediate set of {@link DeclaredAnnotations} as well as
	 * any number of additional {@link MergedAnnotation#isFromInherited()
	 * inherited} annotations.
	 * @param annotations the source of the annotations
	 * @param resolver an annotation type resolver
	 * @param repeatableContainers the strategy used to find repeatable
	 * annotation containers
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * annotations
	 */
	static MergedAnnotations from(Iterable<DeclaredAnnotations> annotations,
			AnnotationTypeResolver resolver, RepeatableContainers repeatableContainers) {
		return new MappedAnnotations(annotations, resolver, repeatableContainers);
	}

	/**
	 * Search strategies supported by
	 * {@link MergedAnnotations#from(AnnotatedElement, SearchStrategy)}.
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
