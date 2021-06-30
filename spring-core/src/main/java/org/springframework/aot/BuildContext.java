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

package org.springframework.aot;

import java.util.ServiceLoader;

/**
 * A build context for use with ahead-of-time processing.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public interface BuildContext {

	/**
	 * Contribute source files to the application.
	 * <p>This additional source code will be compiled and packaged with the
	 * application by a build plugin.
	 * @param generatedFiles the source files to add
	 */
	void addGeneratedFiles(GeneratedFile... generatedFiles);

	/**
	 * Contribute a {@link ServiceLoader} to the application, usually
	 * for a {@link #addGeneratedFiles(GeneratedFile...) generate Java file}.
	 * @param <S> the service type
	 * @param service the service type to add
	 * @param className the name of the class that implements the service
	 */
	<S> void addServiceLoader(Class<S> service, String className);

}
