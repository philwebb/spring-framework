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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Collector implementations exposed via {@link MergedAnnotation} or
 * {@link MergedAnnotations}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public final class MergedAnnotationCollectors {

	private MergedAnnotationCollectors() {
	}

	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
		return Collector.of(ArrayList<MergedAnnotation<A>>::new, List::add,
				MergedAnnotationCollectors::addAll,
				MergedAnnotationCollectors::toSynthesizedAnnotationSet);
	}

	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Annotation[]> toAnnotationArray() {
		return Collector.of(ArrayList::new,
				(list, annotation) -> list.add(annotation.synthesize()),
				MergedAnnotationCollectors::addAll, list -> list.toArray(new Annotation[0]));
	}

	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			MapValues... options) {
		Supplier<MultiValueMap<String, Object>> supplier = LinkedMultiValueMap::new;
		return Collector.of(supplier,
				(map, annotation) -> annotation.asMap(options).forEach(map::add),
				MergedAnnotationCollectors::merge, Function.identity(),
				Characteristics.IDENTITY_FINISH);
	}

	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			Function<MultiValueMap<String, Object>, MultiValueMap<String, Object>> finisher,
			MapValues... options) {
		Supplier<MultiValueMap<String, Object>> supplier = LinkedMultiValueMap::new;
		return Collector.of(supplier,
				(map, annotation) -> annotation.asMap(options).forEach(map::add),
				MergedAnnotationCollectors::merge, finisher);
	}

	private static <E, L extends List<E>> L addAll(L list, L additions) {
		list.addAll(additions);
		return list;
	}

	private static <A extends Annotation, M extends A> Set<A> toSynthesizedAnnotationSet(
			Collection<? extends MergedAnnotation<M>> collection) {
		Set<A> result = new LinkedHashSet<>(collection.size());
		for (MergedAnnotation<M> annotation : collection) {
			result.add(annotation.synthesize());
		}
		return result;
	}

	private static <K, V> MultiValueMap<K, V> merge(MultiValueMap<K, V> map,
			MultiValueMap<K, V> additions) {
		map.addAll(additions);
		return map;
	}




}
