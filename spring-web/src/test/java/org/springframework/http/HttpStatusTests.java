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

package org.springframework.http;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** @author Arjen Poutsma. */
public class HttpStatusTests {

	private Map<Integer, String> statusCodes = new LinkedHashMap<>();

	@Before
	public void createStatusCodes() {
		this.statusCodes.put(100, "CONTINUE");
		this.statusCodes.put(101, "SWITCHING_PROTOCOLS");
		this.statusCodes.put(102, "PROCESSING");
		this.statusCodes.put(103, "CHECKPOINT");

		this.statusCodes.put(200, "OK");
		this.statusCodes.put(201, "CREATED");
		this.statusCodes.put(202, "ACCEPTED");
		this.statusCodes.put(203, "NON_AUTHORITATIVE_INFORMATION");
		this.statusCodes.put(204, "NO_CONTENT");
		this.statusCodes.put(205, "RESET_CONTENT");
		this.statusCodes.put(206, "PARTIAL_CONTENT");
		this.statusCodes.put(207, "MULTI_STATUS");
		this.statusCodes.put(208, "ALREADY_REPORTED");
		this.statusCodes.put(226, "IM_USED");

		this.statusCodes.put(300, "MULTIPLE_CHOICES");
		this.statusCodes.put(301, "MOVED_PERMANENTLY");
		this.statusCodes.put(302, "FOUND");
		this.statusCodes.put(303, "SEE_OTHER");
		this.statusCodes.put(304, "NOT_MODIFIED");
		this.statusCodes.put(305, "USE_PROXY");
		this.statusCodes.put(307, "TEMPORARY_REDIRECT");
		this.statusCodes.put(308, "PERMANENT_REDIRECT");

		this.statusCodes.put(400, "BAD_REQUEST");
		this.statusCodes.put(401, "UNAUTHORIZED");
		this.statusCodes.put(402, "PAYMENT_REQUIRED");
		this.statusCodes.put(403, "FORBIDDEN");
		this.statusCodes.put(404, "NOT_FOUND");
		this.statusCodes.put(405, "METHOD_NOT_ALLOWED");
		this.statusCodes.put(406, "NOT_ACCEPTABLE");
		this.statusCodes.put(407, "PROXY_AUTHENTICATION_REQUIRED");
		this.statusCodes.put(408, "REQUEST_TIMEOUT");
		this.statusCodes.put(409, "CONFLICT");
		this.statusCodes.put(410, "GONE");
		this.statusCodes.put(411, "LENGTH_REQUIRED");
		this.statusCodes.put(412, "PRECONDITION_FAILED");
		this.statusCodes.put(413, "PAYLOAD_TOO_LARGE");
		this.statusCodes.put(414, "URI_TOO_LONG");
		this.statusCodes.put(415, "UNSUPPORTED_MEDIA_TYPE");
		this.statusCodes.put(416, "REQUESTED_RANGE_NOT_SATISFIABLE");
		this.statusCodes.put(417, "EXPECTATION_FAILED");
		this.statusCodes.put(418, "I_AM_A_TEAPOT");
		this.statusCodes.put(419, "INSUFFICIENT_SPACE_ON_RESOURCE");
		this.statusCodes.put(420, "METHOD_FAILURE");
		this.statusCodes.put(421, "DESTINATION_LOCKED");
		this.statusCodes.put(422, "UNPROCESSABLE_ENTITY");
		this.statusCodes.put(423, "LOCKED");
		this.statusCodes.put(424, "FAILED_DEPENDENCY");
		this.statusCodes.put(426, "UPGRADE_REQUIRED");
		this.statusCodes.put(428, "PRECONDITION_REQUIRED");
		this.statusCodes.put(429, "TOO_MANY_REQUESTS");
		this.statusCodes.put(431, "REQUEST_HEADER_FIELDS_TOO_LARGE");
		this.statusCodes.put(451, "UNAVAILABLE_FOR_LEGAL_REASONS");

		this.statusCodes.put(500, "INTERNAL_SERVER_ERROR");
		this.statusCodes.put(501, "NOT_IMPLEMENTED");
		this.statusCodes.put(502, "BAD_GATEWAY");
		this.statusCodes.put(503, "SERVICE_UNAVAILABLE");
		this.statusCodes.put(504, "GATEWAY_TIMEOUT");
		this.statusCodes.put(505, "HTTP_VERSION_NOT_SUPPORTED");
		this.statusCodes.put(506, "VARIANT_ALSO_NEGOTIATES");
		this.statusCodes.put(507, "INSUFFICIENT_STORAGE");
		this.statusCodes.put(508, "LOOP_DETECTED");
		this.statusCodes.put(509, "BANDWIDTH_LIMIT_EXCEEDED");
		this.statusCodes.put(510, "NOT_EXTENDED");
		this.statusCodes.put(511, "NETWORK_AUTHENTICATION_REQUIRED");
	}

	@Test
	public void fromMapToEnum() {
		for (Map.Entry<Integer, String> entry : this.statusCodes.entrySet()) {
			int value = entry.getKey();
			HttpStatus status = HttpStatus.valueOf(value);
			assertEquals("Invalid value", value, status.value());
			assertEquals("Invalid name for [" + value + "]", entry.getValue(), status.name());
		}
	}

	@Test
	public void fromEnumToMap() {

		for (HttpStatus status : HttpStatus.values()) {
			int value = status.value();
			if (value == 302 || value == 413 || value == 414) {
				continue;
			}
			assertTrue("Map has no value for [" + value + "]", this.statusCodes.containsKey(value));
			assertEquals("Invalid name for [" + value + "]", this.statusCodes.get(value), status.name());
		}
	}
}
