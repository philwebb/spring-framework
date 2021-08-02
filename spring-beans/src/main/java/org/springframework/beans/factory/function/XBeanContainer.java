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

/*
 * DESIGN NOTES
 *
 * This is DefaultListableBeanFactory which combines BeanFactory and BeanDefinitionRegistry.
 * It seem sensible to keep separate read/register interfaces to keep intentions clearer
 */

/**
 * Dependency injection container used for accessing and registering beans.
 * Provides both {@link FunctionalBeanRegistry} and {@link XBeanRepository} functionality.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public interface XBeanContainer extends FunctionalBeanRegistry, XBeanRepository {

}
