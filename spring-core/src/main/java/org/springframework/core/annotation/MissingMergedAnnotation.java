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
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.DeclaredAttributes;

/**
 * A {@link MappableAnnotation} used as the implementation of
 * {@link MergedAnnotation#missing()}.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @since 5.1
 */
class MissingMergedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	private static final MissingMergedAnnotation<?> INSTANCE = new MissingMergedAnnotation<>();

	@SuppressWarnings("unchecked")
	static <A extends Annotation> MergedAnnotation<A> getInstance() {
		return (MergedAnnotation<A>) INSTANCE;
	}

	MissingMergedAnnotation() {
		super(null, null, null);
	}

	@Override
	public Map<String, Object> asMap(MapValues... options) {
		return Collections.emptyMap();
	}

	@Override
	public String toString() {
		return "(missing)";
	}

	@Override
	protected Object getAttributeValue(String attributeName, boolean merged) {
		return null;
	}

	@Override
	protected AbstractMergedAnnotation<A> cloneWithAttributeFilter(
			Predicate<String> predicate) {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends Annotation> MergedAnnotation<T> createNested(AnnotationType type,
			DeclaredAttributes attributes) {
		return (MergedAnnotation<T>) this;
	}

}
