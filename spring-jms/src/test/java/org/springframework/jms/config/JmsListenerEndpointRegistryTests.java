/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jms.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Stephane Nicoll
 */
public class JmsListenerEndpointRegistryTests {

	private final JmsListenerEndpointRegistry registry = new JmsListenerEndpointRegistry();

	private final JmsListenerContainerTestFactory containerFactory = new JmsListenerContainerTestFactory();


	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void createWithNullEndpoint() {
		this.thrown.expect(IllegalArgumentException.class);
		this.registry.registerListenerContainer(null, this.containerFactory);
	}

	@Test
	public void createWithNullEndpointId() {
		this.thrown.expect(IllegalArgumentException.class);
		this.registry.registerListenerContainer(new SimpleJmsListenerEndpoint(), this.containerFactory);
	}

	@Test
	public void createWithNullContainerFactory() {
		this.thrown.expect(IllegalArgumentException.class);
		this.registry.registerListenerContainer(createEndpoint("foo", "myDestination"), null);
	}

	@Test
	public void createWithDuplicateEndpointId() {
		this.registry.registerListenerContainer(createEndpoint("test", "queue"), this.containerFactory);

		this.thrown.expect(IllegalStateException.class);
		this.registry.registerListenerContainer(createEndpoint("test", "queue"), this.containerFactory);
	}


	private SimpleJmsListenerEndpoint createEndpoint(String id, String destinationName) {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setId(id);
		endpoint.setDestination(destinationName);
		return endpoint;
	}

}
