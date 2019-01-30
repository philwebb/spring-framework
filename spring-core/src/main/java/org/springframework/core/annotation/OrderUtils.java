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

package org.springframework.core.annotation;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

/**
 * General utility for determining the order of an object based on its type declaration.
 * Handles Spring's {@link Order} annotation as well as {@link javax.annotation.Priority}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see Order
 * @see javax.annotation.Priority
 */
public abstract class OrderUtils {


	private static final String JAVAX_PRIORITY_ANNOTATION = "javax.annotation.Priority";


	/**
	 * Return the order on the specified {@code type}, or the specified
	 * default value if none can be found.
	 * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
	 * @param type the type to handle
	 * @return the priority value, or the specified default order if none can be found
	 * @since 5.0
	 * @see #getPriority(Class)
	 */
	public static int getOrder(Class<?> type, int defaultOrder) {
		Integer order = getOrder(type);
		return (order != null ? order : defaultOrder);
	}

	/**
	 * Return the order on the specified {@code type}, or the specified
	 * default value if none can be found.
	 * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
	 * @param type the type to handle
	 * @return the priority value, or the specified default order if none can be found
	 * @see #getPriority(Class)
	 */
	@Nullable
	public static Integer getOrder(Class<?> type, @Nullable Integer defaultOrder) {
		Integer order = getOrder(type);
		return (order != null ? order : defaultOrder);
	}

	/**
	 * Return the order on the specified {@code type}.
	 * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
	 * @param type the type to handle
	 * @return the order value, or {@code null} if none can be found
	 * @see #getPriority(Class)
	 */
	@Nullable
	public static Integer getOrder(Class<?> type) {
		return getOrder(MergedAnnotations.from(type, SearchStrategy.EXHAUSTIVE));
	}

	/**
	 * Return the order from the specified annotations.
	 * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
	 * @param type the source annotations
	 * @return the order value, or {@code null} if none can be found
	 */
	static Integer getOrder(MergedAnnotations annotations) {
		MergedAnnotation<Order> orderAnnotation = annotations.get(Order.class);
		if (orderAnnotation.isPresent()) {
			return orderAnnotation.getInt(MergedAnnotation.VALUE);
		}
		MergedAnnotation<?> priorityAnnotation = annotations.get(JAVAX_PRIORITY_ANNOTATION);
		if (priorityAnnotation.isPresent()) {
			return priorityAnnotation.getInt(MergedAnnotation.VALUE);
		}
		return null;
	}

	/**
	 * Return the value of the {@code javax.annotation.Priority} annotation
	 * declared on the specified type, or {@code null} if none.
	 * @param type the type to handle
	 * @return the priority value if the annotation is declared, or {@code null} if none
	 */
	@Nullable
	public static Integer getPriority(Class<?> type) {
		return MergedAnnotations.from(type, SearchStrategy.EXHAUSTIVE).get(
				JAVAX_PRIORITY_ANNOTATION).getValue(MergedAnnotation.VALUE,
						Integer.class).orElse(null);
	}

}
