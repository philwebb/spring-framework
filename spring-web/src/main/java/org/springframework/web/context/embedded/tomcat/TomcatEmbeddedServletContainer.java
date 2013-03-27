/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.embedded.tomcat;

import org.apache.catalina.startup.Tomcat;
import org.springframework.util.Assert;
import org.springframework.web.context.embedded.EmbeddedServletContainer;
import org.springframework.web.context.embedded.EmbeddedServletContainerException;

/**
 * {@link EmbeddedServletContainer} that can be used to control an embedded Tomcat server.
 * Usually this class should be created using the
 * {@link TomcatEmbeddedServletContainerFactory} and not directly.
 *
 * @author Phillip Webb
 * @since 4.0
 * @see TomcatEmbeddedServletContainerFactory
 */
public class TomcatEmbeddedServletContainer implements EmbeddedServletContainer {

	private final Tomcat tomcat;


	/**
	 * Create a new {@link TomcatEmbeddedServletContainer} instance.
	 * @param tomcat the underlying Tomcat server
	 */
	public TomcatEmbeddedServletContainer(Tomcat tomcat) {
		Assert.notNull(tomcat, "Tomcat Server must not be null");
		this.tomcat = tomcat;
	}


	public synchronized void start() throws EmbeddedServletContainerException {
		try {
			this.tomcat.start();
			// Unlike Jetty, all Tomcat threads are daemon threads. We create a
			// blocking non-daemon to stop immediate shutdown
			Thread awaitThread = new Thread() {
				public void run() {
					tomcat.getServer().await();
				};
			};
			awaitThread.setDaemon(false);
			awaitThread.start();
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embdedded Tomcat", ex);
		}
	}

	public synchronized void stop() throws EmbeddedServletContainerException {
		try {
			this.tomcat.stop();
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException("Unable to start embdedded Tomcat", ex);
		}
	}


	/**
	 * Returns access to the underlying Tomcat server.
	 */
	public Tomcat getTomcat() {
		return this.tomcat;
	}

}
