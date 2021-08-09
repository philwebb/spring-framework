/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

/**
 *
 * @author pwebb
 * @since 5.2
 */
public class DunnoObjectProvider<T> implements ObjectProvider<T> {

	private ObjectProvider<T> parent;

	private List<FunctionalBeanRegistration<?>> registrations;

	@Override
	public T getObject() throws BeansException {
		// FIXME get or call parent context
		return null;
	}

	@Override
	public T getObject(Object... args) throws BeansException {
		return null;
	}

	@Override
	public T getIfAvailable() throws BeansException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public T getIfUnique() throws BeansException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Iterator<T> iterator() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Stream<T> stream() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Stream<T> orderedStream() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
