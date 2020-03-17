/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * Method-level annotation for integration tests that need to add properties
 * with dynamic values to the {@code Environment}'s set of
 * {@code PropertySources}. Primarily designed to allow properties from
 * <a href="https://www.testcontainers.org/">Testcontainer</a> based tests to be
 * exposed easily to Spring.
 * <p>
 * Methods annotated with {@code @DynamicPropertySource} must be {@code static}
 * and must have a single {@link DynamicPropertyValues} argument which is used
 * to add name/value property pairs. Values are dynamic and provided via a
 * {@link Supplier} which is only invoked when the property is resolved.
 * Typically, method referenced are used to supply values, for example:
 * <pre class="code">
 * &#064;SpringJUnitConfig(...)
 * &#064;Testcontainers
 * class ExampleIntegrationTests {
 *
 *     &#064;Container
 *     static RedisContainer redis = new RedisContainer();
 *
 *     // ...
 *
 *     &#064;DynamicPropertySource
 *     static void redisProperties(DynamicPropertyValues values) {
 *         values.add("redis.host", redis::getContainerIpAddress);
 *         values.add("redis.port", redis::getMappedPort);
 *     }
 *
 * }
 * </pre>
 *
 *
 *
 * @author Phillip Webb
 * @since 5.2.5
 * @see DynamicPropertyValues
 * @see ContextConfiguration
 * @see org.springframework.core.env.PropertySource
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DynamicPropertySource {

}
