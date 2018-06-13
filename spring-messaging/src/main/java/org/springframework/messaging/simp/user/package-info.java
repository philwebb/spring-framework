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
 * Support for handling messages to "user" destinations (i.e. destinations that are
 * unique to a user's sessions), primarily translating the destinations and then
 * forwarding the updated message to the broker.
 *
 * <p>Also included is {@link org.springframework.messaging.simp.user.SimpUserRegistry}
 * for keeping track of connected user sessions.
 */
@NonNullApi
@NonNullFields
package org.springframework.messaging.simp.user;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
