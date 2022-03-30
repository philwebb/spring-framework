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

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowableFunction;

/**
 * Factory hook that allows for custom modifications of bean instances as they are
 * supplied from an {@link InstanceSupplier}.
 * <p>
 * Typically used in AOT-processed applications as a targeted alternative to a
 * {@link BeanPostProcessor}. For example, an {@link InstancePostProcessor} could perform
 * field injection to a bean, working directly with a fields that were previously
 * discovered using annotation scanning.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see InstanceSupplier#withPostProcessor(InstancePostProcessor)
 * @param <T> the bean type
 */
public interface InstancePostProcessor<T> {

	/**
	 * Apply post processing to the given bean instance before it is made available as a
	 * bean.
	 * @param registeredBean the registered bean to process
	 * @param instance the current bean instance
	 * @return the bean instance to use, either the original or a wrapped one.
	 */
	T postProcessInstance(RegisteredBean registeredBean, T instance);

	/**
	 * Factory method that can be used to create an {@link InstancePostProcessor} from a
	 * function. This method can be used if {@link RegisteredBean} details are not needed.
	 * @param <T> the bean type
	 * @param function the processing function
	 * @return an new {@link InstancePostProcessor} that will delegate to the function
	 */
	static <T> InstancePostProcessor<T> of(ThrowableFunction<T, T> function) {
		Assert.notNull(function, "'function' must not be null");
		return (registeredBean, instance) -> function.apply(instance);
	}

}
