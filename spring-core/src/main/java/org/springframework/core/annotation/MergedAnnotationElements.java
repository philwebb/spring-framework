/*
 * Copyright 2002-2019 the original author or authors.
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

/**
 * Provides access to a elements that are all annotated or meta-annotated with the same
 * {@link MergedAnnotation} type.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public interface MergedAnnotationElements<E, A extends Annotation>
		extends Iterable<MergedAnnotationElements.Item<E, A>> {

	interface Item<E, A extends Annotation> {

		E getElement();

		MergedAnnotation<A> getAnnotation();

	}

}
