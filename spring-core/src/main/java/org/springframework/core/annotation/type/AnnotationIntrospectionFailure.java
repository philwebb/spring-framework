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

package org.springframework.core.annotation.type;

import java.lang.reflect.AnnotatedElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * Class to log annotation introspection failures.
 */
class AnnotationIntrospectionFailure {

	@Nullable
	private static Log logger;

	static void log(AnnotatedElement element, Throwable ex) {
		Log logger = getLogger();
		if (logger.isInfoEnabled()) {
			logger.info("Failed to introspect annotations"
					+ (element != null ? " on " + element : "") + ": " + ex);
		}
	}

	private static Log getLogger() {
		Log logger = AnnotationIntrospectionFailure.logger;
		if (logger == null) {
			logger = LogFactory.getLog(DeclaredAnnotation.class);
			AnnotationIntrospectionFailure.logger = logger;
		}
		return logger;
	}

}
