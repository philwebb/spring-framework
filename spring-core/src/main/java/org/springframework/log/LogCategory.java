/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.log;

/**
 * Marker interface that indicate a particular class is intended to be used a a log
 * category. A log category provides a logical way of grouping related log output on top
 * of the log name (the log name is usually the name of the {@code Class} that created the
 * {@code Log}). For example, a {@code LogCategory} might be used to group all {@code SQL}
 * statements.
 *
 * <p>It is recommended (but not not mandatory) that log category classes implement this
 * interface.
 *
 * @author Phillip Webb
 */
public interface LogCategory {
}
