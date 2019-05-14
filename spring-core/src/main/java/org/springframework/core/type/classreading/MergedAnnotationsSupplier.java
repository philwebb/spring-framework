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

package org.springframework.core.type.classreading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;

/**
 * Internal class used to provide a {@link MergedAnnotations} instance as late
 * as possible.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class MergedAnnotationsSupplier {

	private static final MergedAnnotationsSupplier NONE = new MergedAnnotationsSupplier(null, Collections.emptyList());

	@Nullable
	private final Object source;

	private final List<MergedAnnotationSupplier<?>> annotations;

	private MergedAnnotationsSupplier(@Nullable Object source,
			List<MergedAnnotationSupplier<?>> annotations) {

		this.source = source;
		this.annotations = annotations;
	}

	MergedAnnotations get() {
		if (this.annotations.isEmpty()) {
			return MergedAnnotations.of(Collections.emptySet());
		}
		List<MergedAnnotation<?>> annotations = new ArrayList<>(this.annotations.size());
		for (MergedAnnotationSupplier<?> supplier : this.annotations) {
			annotations.add(supplier.get(this.source));
		}
		return MergedAnnotations.of(annotations);
	}

	static MergedAnnotationsSupplier from(Supplier<Object> source,
			List<MergedAnnotationSupplier<?>> annotations) {

		if (annotations.isEmpty()) {
			return NONE;
		}
		return new MergedAnnotationsSupplier(source.get(), annotations);
	}

}
