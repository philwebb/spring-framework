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

import java.util.List;

import org.springframework.core.annotation.MergedAnnotationIndex.QueryResult;
import org.springframework.core.annotation.MergedAnnotationIndex.Scope;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link MergedAnnotationIndexes} implementation backed by {@link SpringFactoriesLoader}
 * and used for {@link MergedAnnotationIndexes#get(ClassLoader)}
 *
 * @author Phillip Webb
 * @since 5.2
 */
class SpringFactoriesLoaderMergedAnnotationIndexes implements MergedAnnotationIndexes {

	private static ConcurrentReferenceHashMap<ClassLoader, SpringFactoriesLoaderMergedAnnotationIndexes> cache = new ConcurrentReferenceHashMap<>();

	private final ResourceLoader resourceLoader;

	private final List<MergedAnnotationIndex> indexes;

	SpringFactoriesLoaderMergedAnnotationIndexes(ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
		this.indexes = SpringFactoriesLoader.loadFactories(MergedAnnotationIndex.class,
				classLoader);
	}

	SpringFactoriesLoaderMergedAnnotationIndexes(ResourceLoader resourceLoader,
			List<MergedAnnotationIndex> indexes) {
		this.resourceLoader = resourceLoader;
		this.indexes = indexes;
	}

	@Override
	public QueryResult query(String sourceClass, String annotationType, Scope scope) {
		Assert.notNull(sourceClass, "SourceClass must not be null");
		Assert.notNull(annotationType, "AnnotationType must not be null");
		Assert.notNull(scope, "Scope must not be null");
		for (MergedAnnotationIndex index : this.indexes) {
			QueryResult result = index.query(this.resourceLoader, sourceClass,
					annotationType, scope);
			if (QueryResult.isKnown(result)) {
				return result;
			}
		}
		return QueryResult.UNKNOWN;
	}

	static SpringFactoriesLoaderMergedAnnotationIndexes get(
			@Nullable ClassLoader classLoader) {
		classLoader = classLoader != null ? classLoader
				: ClassUtils.getDefaultClassLoader();
		if (classLoader == null) {
			return new SpringFactoriesLoaderMergedAnnotationIndexes(classLoader);
		}
		return cache.computeIfAbsent(classLoader,
				key -> new SpringFactoriesLoaderMergedAnnotationIndexes(key));
	}

}
