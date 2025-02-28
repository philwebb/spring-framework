/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ComponentProxyFactory}.
 *
 * @author Phillip Webb
 */
class ComponentProxyFactoryTests {

	private AnnotationMetadata metadata1 = mock();

	private AnnotationMetadata metadata2 = mock();

	private AnnotationMetadata metadata3 = mock();

	private InstanceSupplier<?> supplier1 = mock();

	private InstanceSupplier<?> supplier2 = mock();

	@Test
	void ofWhenEmptyArrayReturnsNone() {
		assertThat(ComponentProxyFactory.of()).isEqualTo(ComponentProxyFactory.NONE);
	}

	@Test
	void ofWhenEmptyCollectionReturnsNone() {
		assertThat(ComponentProxyFactory.of(Collections.emptySet()))
			.isEqualTo(ComponentProxyFactory.NONE);
	}

	@Test
	void ofWhenNullCollectionReturnsNone() {
		assertThat(ComponentProxyFactory.of((Collection<ComponentProxyFactory>) null))
			.isEqualTo(ComponentProxyFactory.NONE);
	}

	@Test
	void ofWhenSingleItemCollectionReturnsItem() {
		ComponentProxyFactory factory = componentMetadata -> null;
		assertThat(ComponentProxyFactory.of(Set.of(factory))).isSameAs(factory);
	}

	@Test
	void ofWhenSingleItemArrayReturnsItem() {
		ComponentProxyFactory factory = componentMetadata -> null;
		assertThat(ComponentProxyFactory.of(factory)).isSameAs(factory);
	}

	@Test
	void ofReturnsComposite() {
		ComponentProxyFactory factory1 = factory(this.metadata1, this.supplier1);
		ComponentProxyFactory factory2 = factory(this.metadata2, this.supplier2);
		ComponentProxyFactory composite = ComponentProxyFactory.of(factory1, factory2);
		assertThat(composite.createProxyInstanceSupplier(this.metadata1)).isEqualTo(this.supplier1);
		assertThat(composite.createProxyInstanceSupplier(this.metadata2)).isEqualTo(this.supplier2);
		assertThat(composite.createProxyInstanceSupplier(this.metadata3)).isNull();
	}

	@Test
	void ofWhenNoMultipleFactoriesReturnInstanceSupplierReturnsCompositeThatThrows() {
		ComponentProxyFactory factory1 = factory(this.metadata1, this.supplier1);
		ComponentProxyFactory factory2a = factory(this.metadata2, this.supplier1);
		ComponentProxyFactory factory2b = factory(this.metadata2, this.supplier2);
		ComponentProxyFactory composite = ComponentProxyFactory.of(factory1, factory2a, factory2b);
		assertThat(composite.createProxyInstanceSupplier(this.metadata1)).isEqualTo(this.supplier1);
		assertThatIllegalStateException().isThrownBy(() -> composite.createProxyInstanceSupplier(this.metadata2))
			.withMessageContaining("Multiple ComponentProxyFactories");
	}

	private ComponentProxyFactory factory(AnnotationMetadata metadata, InstanceSupplier<?> supplier) {
		return componentMetadata -> (metadata.equals(componentMetadata)) ? supplier : null;
	}

}
