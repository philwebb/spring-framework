/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.client.support;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ProxyFactoryBeanTests {

	ProxyFactoryBean factoryBean;

	@Before
	public void setUp() {
		this.factoryBean = new ProxyFactoryBean();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noType() {
		this.factoryBean.setType(null);
		this.factoryBean.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noHostname() {
		this.factoryBean.setHostname("");
		this.factoryBean.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noPort() {
		this.factoryBean.setHostname("example.com");
		this.factoryBean.afterPropertiesSet();
	}

	@Test
	public void normal() {
		Proxy.Type type = Proxy.Type.HTTP;
		this.factoryBean.setType(type);
		String hostname = "example.com";
		this.factoryBean.setHostname(hostname);
		int port = 8080;
		this.factoryBean.setPort(port);
		this.factoryBean.afterPropertiesSet();

		Proxy result = this.factoryBean.getObject();

		assertEquals(type, result.type());
		InetSocketAddress address = (InetSocketAddress) result.address();
		assertEquals(hostname, address.getHostName());
		assertEquals(port, address.getPort());
	}

}
