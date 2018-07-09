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

package org.springframework.core.annotation.type;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * A collection of {@link DeclaredAnnotation} instances.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 5.1
 */
@FunctionalInterface
public interface DeclaredAnnotations extends Iterable<DeclaredAnnotation> {

	/**
	 * {@link DeclaredAnnotation} instances that can be used when there are no
	 * declared annotations.
	 */
	static final DeclaredAnnotations NONE = () -> Collections.emptyIterator();

	/**
	 * Find a declared annotation of the specified type.
	 * @param annotationType the type required
	 * @return a declared annotation or {@code null}
	 */
	@Nullable
	default DeclaredAnnotation find(String annotationType) {
		return StreamSupport.stream(spliterator(), false).filter(
				annotation -> annotation.getClassName().equals(
						annotationType)).findFirst().orElse(null);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotations} containing the
	 * specified annotations.
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(DeclaredAnnotation... annotations) {
		return new SimpleDeclaredAnnotations(annotations);
	}

	/**
	 * Create a new in-memory {@link DeclaredAnnotations} containing the
	 * specified annotations.
	 * @param annotations the contained annotations
	 * @return a new {@link DeclaredAnnotations} instance
	 */
	static DeclaredAnnotations of(Collection<DeclaredAnnotation> annotations) {
		return new SimpleDeclaredAnnotations(annotations);
	}

}
