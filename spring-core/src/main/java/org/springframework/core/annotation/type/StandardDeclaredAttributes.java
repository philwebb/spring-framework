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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * {@link DeclaredAttributes} backed by an {@link Annotation} and implemented
 * using standard Java reflection.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class StandardDeclaredAttributes extends AbstractDeclaredAttributes {

	private final Annotation annotation;

	private final Map<String, Method> attributeMethods;

	StandardDeclaredAttributes(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		this.annotation = annotation;
		this.attributeMethods = getAttributeMethods(annotation.annotationType());
	}

	private Map<String, Method> getAttributeMethods(
			Class<? extends Annotation> annotationType) {
		Method[] candidates = annotationType.getDeclaredMethods();
		Map<String, Method> attributeMethods = new LinkedHashMap<>(candidates.length);
		for (Method candidate : candidates) {
			if (isAttributeMethod(candidate)) {
				assertAttibuteMethodIsCallable(candidate);
				attributeMethods.put(candidate.getName(), candidate);
			}
		}
		return Collections.unmodifiableMap(attributeMethods);
	}

	private void assertAttibuteMethodIsCallable(Method candidate) {
		candidate.setAccessible(true);
		get(candidate);
	}

	private boolean isAttributeMethod(Method method) {
		return (method.getParameterCount() == 0 && method.getReturnType() != void.class);
	}

	/**
	 * Return the backing source annotation.
	 * @return the source annotation
	 */
	public Annotation getAnnotation() {
		return this.annotation;
	}

	@Override
	public Set<String> names() {
		return this.attributeMethods.keySet();
	}

	@Override
	public Object get(String name) {
		Method method = this.attributeMethods.get(name);
		if (method == null) {
			return null;
		}
		return get(method);
	}

	private Object get(Method method) {
		try {
			return convert(method.invoke(this.annotation));
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Could not obtain annotation attribute value for " + method, ex);
		}
	}

}
