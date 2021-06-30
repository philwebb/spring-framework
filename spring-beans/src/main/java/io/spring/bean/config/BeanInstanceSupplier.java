/*
 * Copyright 2012-2021 the original author or authors.
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

package io.spring.bean.config;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.util.Assert;

/*
 * DESIGN NOTES
 *
 * The supplier needs to be able to get other things it needs to inject from the BeanRepository.
 * The via methods are for quick shortcuts to common adapters
 */

/**
 * Strategy interface used to supply fully-wired bean instances.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> The type
 */
@FunctionalInterface
public interface BeanInstanceSupplier<T> {

	/**
	 * Return the full-wired bean instance with dependencies supplied via the
	 * given {@link BeanRegistry}.
	 * @param repository the registry used to obtain dependencies
	 * @return the supplied bean instance
	 */
	T get(BeanRepository repository) throws Exception;

	/**
	 * Create a new {@link BeanInstanceSupplier} where the instance is supplied
	 * via a {@link java.util.instance.Supplier}. This method is often used to
	 * adapt a standard constructor, e.g.
	 * {@code BeanInstanceSupplier.via(MyBean::new)}.
	 * @param <T> the bean type
	 * @param supplier the supplier to use
	 * @return a new {@link BeanInstanceSupplier}
	 */
	static <T> BeanInstanceSupplier<T> via(Supplier<T> supplier) {
		Assert.notNull(supplier, "'supplier' must not be null");
		return (repository) -> supplier.get();
	}

	/**
	 * Create a new {@link BeanInstanceSupplier} where the instance is supplied
	 * via a mapper applied another bean. This method can be used to implement a
	 * factory method pattern, e.g.
	 * {@code BeanInstanceSupplier.via(MyBean.class, MyBean::createAnotherBean)}.
	 * @param <S> the bean type to select
	 * @param <T> the bean type
	 * @param selectedBeanType the bean type to select
	 * @param mapper the mapper applied to the selected bean
	 * @return a new {@link BeanInstanceSupplier}
	 */
	static <S, T> BeanInstanceSupplier<T> via(Class<S> selectedBeanType,
			Function<? super S, ? extends T> mapper) {
		return via(BeanSelector.havingType(selectedBeanType), mapper);
	}

	/**
	 * Create a new {@link BeanInstanceSupplier} where the instance is supplied
	 * via a mapper applied another bean. This method can be used to implement a
	 * factory method pattern, e.g.
	 * {@code BeanInstanceSupplier.via(BeanSelector.havingName("myBean"), MyBean::createAnotherBean)}.
	 * @param <S> the bean selector
	 * @param <T> the bean type
	 * @param selectedBeanType the bean type to select
	 * @param mapper the mapper applied to the selected bean
	 * @return a new {@link BeanInstanceSupplier}
	 */
	static <S, T> BeanInstanceSupplier<T> via(BeanSelector<S> selector,
			Function<? super S, ? extends T> mapper) {
		return (repository) -> repository.select(selector).map(mapper).get();
	}

}
