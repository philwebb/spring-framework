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

package org.springframework.beans.factory.support;

import java.util.Arrays;

/**
 * {@link InstanceSupplier} to handle
 * {@link InstanceSupplier#withPostProcessors(InstancePostProcessor...)}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @param <T> the type of instance supplied by this supplier
 */
final class PostProcessingInstanceSupplier<T> implements InstanceSupplier<T> {

	private final InstanceSupplier<T> instanceSupplier;

	private final InstancePostProcessor<T>[] postProcessors;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	PostProcessingInstanceSupplier(InstanceSupplier<T> instanceSupplier, InstancePostProcessor<T> postProcessor) {
		if (instanceSupplier instanceof PostProcessingInstanceSupplier<T> postProcessingInstanceSupplier) {
			InstancePostProcessor<T>[] originalPostProcessors = postProcessingInstanceSupplier.postProcessors;
			this.instanceSupplier = postProcessingInstanceSupplier.instanceSupplier;
			this.postProcessors = Arrays.copyOf(originalPostProcessors, originalPostProcessors.length + 1);
			this.postProcessors[originalPostProcessors.length] = postProcessor;
		}
		else {
			this.instanceSupplier = instanceSupplier;
			this.postProcessors = new InstancePostProcessor[] { postProcessor };
		}
	}

	@Override
	public T get(RegisteredBean registeredBean) throws Exception {
		T result = this.instanceSupplier.get(registeredBean);
		for (InstancePostProcessor<T> postProcessor : this.postProcessors) {
			result = postProcessor.postProcessInstance(registeredBean, result);
		}
		return result;
	}

}
