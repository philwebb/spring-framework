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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Collector implementations that provide various reduction operations for
 * {@link MergedAnnotation MergedAnnotations}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public final class MergedAnnotationCollectors {

	private static final Characteristics[] NO_CHARACTERISTICS = {};

	private static final Characteristics[] IDENTITY_FINISH_CHARACTERISTICS = {
		Characteristics.IDENTITY_FINISH };

	private MergedAnnotationCollectors() {
	}

	/**
	 * Returns a new {@link Collector} that accumulates merged annotations to a
	 * {@link LinkedHashSet} containing {@link MergedAnnotation#synthesize()
	 * synthesized} versions.
	 * @param <A> the annotation type
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into a {@link Set}
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
		return Collector.of(ArrayList<A>::new,
				(list, annotation) -> list.add(annotation.synthesize()),
				MergedAnnotationCollectors::addAll, (list) -> new LinkedHashSet<>(list));
	}

	/**
	 * Returns a new {@link Collector} that accumulates merged annotations to an
	 * {@link Annotation} array containing {@link MergedAnnotation#synthesize()
	 * synthesized} versions.
	 * @param <A> the annotation type
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into an {@code Annotation[]}
	 * @see #toAnnotationArray(IntFunction)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Annotation[]> toAnnotationArray() {
		return toAnnotationArray(Annotation[]::new);
	}

	/**
	 * Returns a new {@link Collector} that accumulates merged annotations to an
	 * {@link Annotation} array containing {@link MergedAnnotation#synthesize()
	 * synthesized} versions.
	 * @param <A> the annotation type
	 * @param <R> the resulting array type
	 * @param generator a function which produces a new array of the desired
	 * type and the provided length
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into an annotation array
	 * @see #toAnnotationArray
	 */
	public static <A extends Annotation, R extends Annotation> Collector<MergedAnnotation<A>, ?, R[]> toAnnotationArray(
			IntFunction<R[]> generator) {
		return Collector.of(ArrayList::new,
				(list, annotation) -> list.add(annotation.synthesize()),
				MergedAnnotationCollectors::addAll,
				list -> list.toArray(generator.apply(list.size())));
	}

	/**
	 * Returns a new {@link Collector} that accumulates merged annotations to an
	 * {@link MultiValueMap} with items {@link MultiValueMap#add(Object, Object)
	 * added} from each merged annotation
	 * {@link MergedAnnotation#asMap(MapValues...) as a map}.
	 * @param <A> the annotation type
	 * @param options the map conversion options
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into a {@link LinkedMultiValueMap}
	 * @see #toMultiValueMap(Function, MapValues...)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			MapValues... options) {
		return toMultiValueMap(Function.identity(), options);
	}

	/**
	 * Returns a new {@link Collector} that accumulates merged annotations to an
	 * {@link MultiValueMap} with items {@link MultiValueMap#add(Object, Object)
	 * added} from each merged annotation
	 * {@link MergedAnnotation#asMap(MapValues...) as a map}.
	 * @param <A> the annotation type
	 * @param options the map conversion options
	 * @param finisher the finisher function for the new {@link MultiValueMap}
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into a {@link LinkedMultiValueMap}
	 * @see #toMultiValueMap(Function, MapValues...)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			Function<MultiValueMap<String, Object>, MultiValueMap<String, Object>> finisher,
			MapValues... options) {
		Characteristics[] characteristics = isSameInstance(finisher, Function.identity())
				? IDENTITY_FINISH_CHARACTERISTICS
				: NO_CHARACTERISTICS;
		return Collector.of(
				(Supplier<MultiValueMap<String, Object>>) LinkedMultiValueMap::new,
				(map, annotation) -> annotation.asMap(options).forEach(map::add),
				MergedAnnotationCollectors::merge, finisher, characteristics);
	}

	private static boolean isSameInstance(Object instance, Object candidate) {
		return instance == candidate;
	}

	private static <E, L extends List<E>> L addAll(L list, L additions) {
		list.addAll(additions);
		return list;
	}

	private static <K, V> MultiValueMap<K, V> merge(MultiValueMap<K, V> map,
			MultiValueMap<K, V> additions) {
		map.addAll(additions);
		return map;
	}

}
