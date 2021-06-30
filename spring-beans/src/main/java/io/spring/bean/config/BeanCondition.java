/*
 * Copyright 2012-2021 the original author or authors.
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

package io.spring.bean.config;

import io.spring.core.origin.Origin;
import io.spring.core.origin.OriginSupplier;

/**
 * A condition used to determine when one or more {@link BeanRegistration
 * BeanRegistrations} should be active within a {@link BeanRegistry}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
@FunctionalInterface
public interface BeanCondition extends OriginSupplier {

	BeanConditionOutcome evaluate(BeanConditionContext context);

	@Override
	default Origin getOrigin() {
		return null;
	}

	default BeanCondition withOrigin(Origin origin) {
		return null;
	}

	static BeanCondition hasProfile(String string) {
		return null;
	}

}
