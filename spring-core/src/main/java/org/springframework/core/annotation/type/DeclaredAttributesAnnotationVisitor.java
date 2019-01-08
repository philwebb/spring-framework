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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.util.Assert;

/**
 * ASM {@link AnnotationVisitor} that can be used to create
 * {@link DeclaredAttributes}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class DeclaredAttributesAnnotationVisitor extends AttributeValuesAnnotationVisitor {

	private final Consumer<DeclaredAttributes> consumer;

	private final Map<String, Object> values = new LinkedHashMap<>();

	/**
	 * Create a new {@link DeclaredAttributesAnnotationVisitor} instance with no
	 * consumer.
	 * @see #accept(DeclaredAttributes)
	 */
	protected DeclaredAttributesAnnotationVisitor() {
		this.consumer = attribute -> {
			throw new IllegalStateException("Accept method not overridden");
		};
	}

	/**
	 * Create a new {@link DeclaredAttributesAnnotationVisitor} instance.
	 * @param consumer a consumer that will receive the resulting
	 * {@link DeclaredAttributes}.
	 */
	public DeclaredAttributesAnnotationVisitor(Consumer<DeclaredAttributes> consumer) {
		Assert.notNull(consumer, "DeclaredAttributesConsumer must not be null");
		this.consumer = consumer;
	}

	@Override
	protected void accept(String name, Object value) {
		this.values.put(name, value);
	}

	@Override
	public void visitEnd() {
		accept(new SimpleDeclaredAttributes(this.values));
	}

	protected void accept(DeclaredAttributes attributes) {
		this.consumer.accept(attributes);
	}

}
