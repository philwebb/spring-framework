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

/**
 * Spring's variant of the
 * <a href="http://commons.apache.org/logging">Commons Logging API</a>:
 * with special support for Log4J 2, SLF4J and {@code java.util.logging}.
 *
 * <p>This {@code impl} package is only present for binary compatibility
 * with existing Commons Logging usage, e.g. in Commons Configuration.
 * {@code NoOpLog} can be used as a {@code Log} fallback instance, and
 * {@code SimpleLog} is not meant to work (issuing a warning when used).
 */
package org.apache.commons.logging.impl;
