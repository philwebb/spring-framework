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
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.bytebuddy.description.annotation.AnnotationSource;

/**
 *
 * @author pwebb
 * @since 5.1
 */
class StandardMergedAnnotations extends AbstractMergedAnnotations {


	StandardMergedAnnotations() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.core.annotation.MergedAnnotations#get(java.lang.String, java.util.function.Predicate, org.springframework.core.annotation.MergedAnnotationSelector)
	 */
	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			Predicate<? super MergedAnnotation<A>> predicate,
			MergedAnnotationSelector<A> selector) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/* (non-Javadoc)
	 * @see org.springframework.core.annotation.MergedAnnotations#getAll()
	 */
	@Override
	public Set<MergedAnnotation<Annotation>> getAll() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param source
	 * @param annotation
	 * @return
	 */
	static <A extends Annotation> MergedAnnotation<A> from(Object source, A annotation) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param source
	 * @param annotationType
	 * @param attributes
	 * @return
	 */
	static <A extends Annotation> MergedAnnotation<A> from(Object source, Class<A> annotationType,
			Map<String, ?> attributes) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
