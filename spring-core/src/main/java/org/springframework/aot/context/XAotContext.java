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

package org.springframework.aot.context;

import org.springframework.aot.generate.GenerationContext;

/**
 * Central interface used with ahead-of-time processing of a Spring application.
 * <p>
 * An AOT context provides:
 * <ul>
 * <li>Access to and management of {@link #getProcessors() AOT processors}.</li>
 * <li>Support for {@link #getClassNameGenerator() class name generation}.</li>
 * <li>Central management of all {@link #getGeneratedFiles() generated files}.</li>
 * <li>Support for the recording of {@link #getRuntimeHints() runtime hints}.</li>
 * <li>Registration of {@link #getGeneratedSpringFactories() spring.factories} content.
 * </ul>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see XAotProcessor
 * @see XAotContribution
 * @see GenerationContext
 */
public interface XAotContext extends GenerationContext {

	/**
	 * Return the {@link XAotProcessors} being used by the context. Typically used to
	 * register additional processors or apply further processing.
	 * @return the AOT processors
	 */
	XAotProcessors getProcessors();

}
