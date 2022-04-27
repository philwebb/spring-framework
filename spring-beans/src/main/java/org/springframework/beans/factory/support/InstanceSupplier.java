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

import org.springframework.util.Assert;
import org.springframework.util.function.ThrowableBiFunction;
import org.springframework.util.function.ThrowableSupplier;

/**
 * Specialized {@link Supplier} that can be set on a
 * {@link AbstractBeanDefinition#setInstanceSupplier(Supplier) BeanDefinition} when
 * details about the {@link RegisteredBean registered bean} are needed to supply the
 * instance.
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
	default T getWithException() {
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
	 * Return a composed instance supplier that first obtains the instance from this
	 * supplier, and then applied the {@code after} function to obtain the result.
	 * @param <V> the type of output of the {@code after} function, and of the composed
	 * function
	 * @param after the function to apply after the instance is obtained
	 * @return a composed instance supplier
	 */
	default <V> InstanceSupplier<V> andThen(ThrowableBiFunction<RegisteredBean, ? super T, ? extends V> after) {
		Assert.notNull(after, "After must not be null");
		return registeredBean -> after.applyWithException(registeredBean, get(registeredBean));
	}

	/**
	 * Factory method to create an {@link InstanceSupplier} from a
	 * {@link ThrowableSupplier}.
	 * @param <T> the type of instance supplied by this supplier
	 * @param supplier the source supplier
	 * @return a new {@link InstanceSupplier}
	 */
	static <T> InstanceSupplier<T> using(ThrowableSupplier<T> supplier) {
		Assert.notNull(supplier, "Supplier must not be null");
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
	 * @param instanceSupplier the source instance supplier
	 * @return a new {@link InstanceSupplier}
	 */
	static <T> InstanceSupplier<T> of(InstanceSupplier<T> instanceSupplier) {
		Assert.notNull(instanceSupplier, "InstanceSupplier must not be null");
		return instanceSupplier;
	}

}
