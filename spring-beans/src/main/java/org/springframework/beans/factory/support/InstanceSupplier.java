/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.function.Supplier;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowableSupplier;

/**
 * Specialized {@link Supplier} that can be set on a
 * {@link AbstractBeanDefinition#setInstanceSupplier(Supplier) BeanDefinition} when
 * details about the {@link RegisteredBean registered bean} are needed to supply the
 * instance, or if additional {@link InstancePostProcessor instance post processing} is
 * required.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @param <T> the type of instance supplied by this supplier
 * @see RegisteredBean
 */
@FunctionalInterface
public interface InstanceSupplier<T> extends ThrowableSupplier<T> {

	@Override
	default T getWithException() throws Exception {
		throw new IllegalStateException("No RegisteredBean parameter provided");
	}

	/**
	 * Gets the supplied instance.
	 * @param registeredBean the registered bean requesting the instance
	 * @return the supplied instance
	 * @throws Exception on error
	 */
	T get(RegisteredBean registeredBean) throws Exception;

	/**
	 * Return a new {@link InstanceSupplier} that applies the given
	 * {@link InstancePostProcessor} when supplying a result.
	 * @param instancePostProcessor the instance post processor to apply
	 * @return a new {@link InstanceSupplier}
	 */
	@SuppressWarnings("unchecked")
	default InstanceSupplier<T> withPostProcessor(InstancePostProcessor<T> instancePostProcessor) {
		Assert.notNull(instancePostProcessor, "'instancePostProcessor' must not be null");
		return new PostProcessingInstanceSupplier<>(this, instancePostProcessor);
	}

	/**
	 * Factory method to create an {@link InstanceSupplier} from a
	 * {@link ThrowableSupplier}.
	 * @param <T> the type of instance supplied by this supplier
	 * @param supplier the source supplier
	 * @return a new {@link InstanceSupplier}
	 */
	static <T> InstanceSupplier<T> of(ThrowableSupplier<T> supplier) {
		Assert.notNull(supplier, "'supplier' must not be null");
		if (supplier instanceof InstanceSupplier<T> instanceSupplier) {
			return instanceSupplier;
		}
		return registeredBean -> supplier.getWithException();
	}

	/**
	 * Lambda friendly method that can be used to create a {@link InstanceSupplier} and
	 * add post processors in a single call. For example: {@code
	 * InstanceSupplier.of(registeredBean -> ...).withPostProcessor(...)}.
	 * @param <T> the type of instance supplied by this supplier
	 * @param supplier the source supplier
	 * @return a new {@link InstanceSupplier}
	 */
	static <T> InstanceSupplier<T> of(InstanceSupplier<T> instanceSupplier) {
		Assert.notNull(instanceSupplier, "'function' must not be null");
		return instanceSupplier;
	}

	/**
	 * Get a supplied instance from the specified {@link Supplier}, casting it to an
	 * {@link InstanceSupplier} if possible.
	 * @param <T> the type of instance supplied by the supplier
	 * @param registeredBean the registered bean calling the supplier
	 * @param supplier the supplier to us
	 * @return the supplied result
	 * @throws Exception on error
	 */
	static <T> T getSuppliedInstance(RegisteredBean registeredBean, Supplier<T> supplier) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(supplier, "'supplier' must not be null");
		try {
			if (supplier instanceof InstanceSupplier<T> instanceSupplier) {
				return instanceSupplier.get(registeredBean);
			}
			if (supplier instanceof ThrowableSupplier<T> throwableSupplier) {
				return throwableSupplier.getWithException();
			}
			return supplier.get();
		}
		catch (Throwable ex) {
			throw new BeanCreationException(registeredBean.getBeanName(), "Instantiation of supplied bean failed", ex);
		}
	}

}
