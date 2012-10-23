/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * @author Keith Donald
 * @since 3.1
 */
abstract class AbstractDescriptor {


	static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];


	private final Class<?> type;


	protected AbstractDescriptor(Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		this.type = type;
	}


	public Class<?> getType() {
		return this.type;
	}

	public AbstractDescriptor getElementDescriptor() {
		if (isCollection()) {
			Class<?> elementType = resolveCollectionElementType();
			return (elementType != null ? nested(elementType, 0) : null);
		}
		else if (isArray()) {
			Class<?> elementType = getType().getComponentType();
			return nested(elementType, 0);
		}
		else {
			return null;
		}
	}

	public AbstractDescriptor getMapKeyDescriptor() {
		if (isMap()) {
			Class<?> keyType = resolveMapKeyType();
			return (keyType != null ? nested(keyType, 0) : null);
		}
		else {
			return null;
		}
	}

	public AbstractDescriptor getMapValueDescriptor() {
		if (isMap()) {
			Class<?> valueType = resolveMapValueType();
			return (valueType != null ? nested(valueType, 1) : null);
		}
		else {
			return null;
		}
	}

	public AbstractDescriptor nested() {
		if (isCollection()) {
			Class<?> elementType = resolveCollectionElementType();
			return (elementType != null ? nested(elementType, 0) : null);
		}
		else if (isArray()) {
			return nested(getType().getComponentType(), 0);
		}
		else if (isMap()) {
			Class<?> mapValueType = resolveMapValueType();
			return (mapValueType != null ? nested(mapValueType, 1) : null);
		}
		else if (Object.class.equals(getType())) {
			// could be a collection type but we don't know about its element type,
			// so let's just assume there is an element type of type Object
			return this;
		}
		else {
			throw new IllegalStateException("Not a collection, array, or map: cannot resolve nested value types");
		}
	}


	// subclassing hooks

	public abstract Annotation[] getAnnotations();

	protected abstract Class<?> resolveCollectionElementType();

	protected abstract Class<?> resolveMapKeyType();

	protected abstract Class<?> resolveMapValueType();

	protected abstract AbstractDescriptor nested(Class<?> type, int typeIndex);


	// internal helpers

	private boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	private boolean isArray() {
		return getType().isArray();
	}

	private boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	static Annotation[] nullSafeAnnotations(Annotation[] annotations) {
		return annotations != null ? annotations : EMPTY_ANNOTATION_ARRAY;
	}

}
