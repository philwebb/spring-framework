/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Simple key generator. Returns an empty {@link List} if no parameters are provided, the
 * parameter itself if it is a single non-null value or a {@link List} of
 * {@code parameters}.
 *
 * <p>This implementation assumes that all parameters implement suitable {@code hashCode}
 * and {@code equals} methods and are immutable.
 *
 * @author Phillip Webb
 * @since 3.2
 */
public class SimpleKeyGenerator implements KeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		if(params.length == 1 && params[0] != null) {
			return params[0];
		}
		return Arrays.asList(params);
	}

}
