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

package org.springframework.util.function;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A {@link Consumer} that allows to invoke code that throws a checked
 * exception.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @param <T> the type of the input to the operation
 * @since 6.0
 */
@FunctionalInterface
public interface ThrowableConsumer<T> extends Consumer<T> {

	/**
	 * Performs this operation on the given argument, possibly throwing a
	 * checked exception.
	 * @param t the input argument
	 * @throws Exception on error
	 */
	void acceptWithException(T t) throws Exception;

	/**
	 * Default {@link Consumer#accept(Object)} that wraps any thrown checked
	 * exceptions (by default in a {@link RuntimeException}).
	 * @see java.util.function.Consumer#accept(Object)
	 */
	@Override
	default void accept(T t) {
		accept(t, RuntimeException::new);
	}

	/**
	 * Performs this operation on the given argument, wrapping any any thrown
	 * checked exceptions using the given {@code exceptionWrapper}.
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 */
	default void accept(T t,
			BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		try {
			acceptWithException(t);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw exceptionWrapper.apply(ex.getMessage(), ex);
		}
	}

	/**
	 * Return a new {@link ThrowableConsumer} where the {@link #consume(Object}
	 * method wraps any thrown checked exceptions using the given
	 * {@code exceptionWrapper}.
	 * @param exceptionWrapper {@link BiFunction} that wraps the given message
	 * and checked exception into a runtime exception
	 * @return the replacement {@link ThrowableConsumer} instance
	 */
	default ThrowableConsumer<T> throwing(
			BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
		return new ThrowableConsumer<>() {

			@Override
			public void acceptWithException(T t) throws Exception {
				ThrowableConsumer.this.acceptWithException(t);
			}

			@Override
			public void accept(T t) {
				accept(t, exceptionWrapper);
			}

		};
	}

	/**
	 * Lambda friendly convenience method that can be used to create
	 * {@link ThrowableConsumer} where the {@link #apply(Object)} method wraps
	 * any thrown checked exceptions using the given {@code exceptionWrapper}.
	 * @param <T> the type of the input to the operation
	 * @param consumer the source consumer
	 * @return a new {@link ThrowableConsumer} instance
	 */
	static <T> ThrowableConsumer<T> of(ThrowableConsumer<T> consumer,
			BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

		return consumer.throwing(exceptionWrapper);
	}

}