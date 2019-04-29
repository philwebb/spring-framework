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

package org.springframework.core.type;

import java.io.IOException;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

/**
 * Tests for {@link AnnotationMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 */
public class AnnotationMetadataReadingVisitorTests
		extends AbstractAnnotationMetadataTests {

	// FIXME move to correct package

	@Override
	protected AnnotationMetadata get(Class<?> source) {
		try {
			return new SimpleMetadataReaderFactory(
					getClass().getClassLoader()).getMetadataReader(
							source.getName()).getAnnotationMetadata();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
