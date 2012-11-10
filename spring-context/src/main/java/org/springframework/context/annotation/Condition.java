/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A single {@code condition} that must be {@linkplain #matches matched} in order for a
 * component to be registered.
 *
 * <p>Conditions are checked immediately before a component bean-definition is due to be
 * registered and are free to veto registration based on any criteria that can be
 * determined at that point.
 *
 * <p>Conditions are create via {@link AutowireCapableBeanFactory#createBean(Class)}
 * and as such will receive all standard bean initialization callbacks. Conditions must
 * follow the same restrictions as {@link BeanFactoryPostProcessor} and take care to never
 * interact with bean instances.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see Conditional
 */
public interface Condition {

	/**
	 * Determine if the condition matches.
	 * @param metadata the meta-data of the annotated type being considered.
	 * @return {@code true} if the condition matches and the component can be registered
	 *         or {@code false} to veto registration.
	 */
	boolean matches(AnnotatedTypeMetadata metadata);

	//FIXME should we consider a ConditionContext that can grow over time?
	//FIXME matches should perhaps be rename to shouldSkip or could return
	//      and enum to make it cleared when the bean is loaded

}
