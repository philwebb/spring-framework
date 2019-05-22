/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.junit4.profile.xml;

import org.junit.Test;

import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.1
 */
@ActiveProfiles("dev")
public class DevProfileXmlConfigTests extends DefaultProfileXmlConfigTests {

	@Test
	@Override
	public void employee() {
		assertThat((Object) employee).as("employee bean should be loaded for the 'dev' profile").isNotNull();
		assertThat(employee.getName()).isEqualTo("John Smith");
	}

}
