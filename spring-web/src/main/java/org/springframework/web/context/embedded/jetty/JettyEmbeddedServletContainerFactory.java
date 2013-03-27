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

package org.springframework.web.context.embedded.jetty;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.ServletContextInitializer;
import org.springframework.web.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.web.context.embedded.EmbeddedServletContainerFactory;

/**
 * {@link EmbeddedServletContainerFactory} that can be used to create
 * {@link JettyEmbeddedServletContainer}s. Can be initialized using Spring's
 * {@link ServletContextInitializer}s or Jetty {@link Configuration}s.
 *
 * <p>Unless explicitly configured otherwise this factory will created containers that
 * listen for HTTP requests on port 8080.
 *
 * @author Phillip Webb
 * @since 4.0
 * @see #setPort(int)
 * @see #setConfigurations(Collection)
 * @see JettyEmbeddedServletContainer
 */
public class JettyEmbeddedServletContainerFactory extends
		AbstractEmbeddedServletContainerFactory {

	private List<Configuration> configurations = new ArrayList<Configuration>();

	private boolean registerDefaultServlet = true;


	/**
	 * Create a new {@link JettyEmbeddedServletContainerFactory} instance.
	 */
	public JettyEmbeddedServletContainerFactory() {
		super();
	}

	/**
	 * Create a new {@link JettyEmbeddedServletContainerFactory} that listens for requests
	 * using the specified port.
	 * @param port the port to listen on
	 */
	public JettyEmbeddedServletContainerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new {@link JettyEmbeddedServletContainerFactory} with the specified
	 * context path and port.
	 * @param contextPath root the context path
	 * @param port the port to listen on
	 */
	public JettyEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
	}


	public JettyEmbeddedServletContainer getEmbdeddedServletContainer(
			ServletContextInitializer... initializers) {
		Server server = new Server(getPort());

		WebAppContext context = new WebAppContext();
		String contextPath = getContextPath();
		context.setContextPath(StringUtils.hasLength(contextPath) ? contextPath : "/");
		configureDocumentRoot(context);
		if(this.registerDefaultServlet) {
			addDefaultServlet(context);
		}

		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		Configuration[] configurations = getWebAppContextConfigurations(context, initializersToUse);
		context.setConfigurations(configurations);
		postProcessWebAppContext(context);

		server.setHandler(context);
		return getJettyEmbeddedServletContainer(server);
	}

	private void configureDocumentRoot(WebAppContext handler) {
		if(getDocumentRoot() != null) {
			File root = getValidDocumentRoot();
			try {
				if(root != null) {
					handler.setBaseResource(FileResource.newResource(root));
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	private void addDefaultServlet(WebAppContext context) {
		ServletHolder holder = new ServletHolder();
		holder.setName("default");
		holder.setClassName("org.eclipse.jetty.servlet.DefaultServlet");
		holder.setInitParameter("dirAllowed", "false");
		holder.setInitOrder(1);
		context.getServletHandler().addServletWithMapping(holder, "/");
	}

	/**
	 * Return the Jetty {@link Configuration}s that should be applied to the server.
	 * @param webAppContext the Jetty {@link WebAppContext}
	 * @param initializers the {@link ServletContextInitializer}s to apply
	 * @return configurations to apply
	 */
	protected Configuration[] getWebAppContextConfigurations(WebAppContext webAppContext,
			ServletContextInitializer... initializers) {
		List<Configuration> configurations = new ArrayList<Configuration>();
		configurations.add(getServletContextInitializerConfiguration(webAppContext, initializers));
		configurations.addAll(getConfigurations());
		return configurations.toArray(new Configuration[configurations.size()]);
	}

	/**
	 * Return a Jetty {@link Configuration} that will invoke the specified
	 * {@link ServletContextInitializer}s. By default this method will return a
	 * {@link ServletContextInitializerConfiguration}.
	 * @param webAppContext the Jetty {@link WebAppContext}
	 * @param initializers the {@link ServletContextInitializer}s to apply
	 * @return the {@link Configuration} instance
	 */
	protected Configuration getServletContextInitializerConfiguration(WebAppContext webAppContext,
			ServletContextInitializer... initializers) {
		return new ServletContextInitializerConfiguration(webAppContext, initializers);
	}

	/**
	 * Post process the Jetty {@link WebAppContext} before it used with the Jetty Server.
	 * Subclasses can override this method to apply additional processing to the
	 * {@link WebAppContext}.
	 * @param webAppContext the Jetty {@link WebAppContext}
	 */
	protected void postProcessWebAppContext(WebAppContext webAppContext) {
	}

	/**
	 * Factory method called to create the {@link JettyEmbeddedServletContainer}.
	 * Subclasses can override this method to return a different
	 * {@link JettyEmbeddedServletContainer} or apply additional processing to the
	 * Jetty server.
	 * @param server the Jetty server.
	 * @return a new {@link JettyEmbeddedServletContainer} instance
	 */
	protected JettyEmbeddedServletContainer getJettyEmbeddedServletContainer(Server server) {
		return new JettyEmbeddedServletContainer(server);
	}

	/**
	 * Set if the Jetty DefaultServlet should be registered. Defaults to {@code true}
	 * so that files from the {@link #setDocumentRoot(File) document root} will be
	 * served.
	 * @param registerDefaultServlet if the Jetty default servlet should be registered
	 */
	public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
		this.registerDefaultServlet = registerDefaultServlet;
	}

	/**
	 * Sets Jetty {@link Configuration}s that will be applied to the
	 * {@link WebAppContext} before the server is created. Calling this method will
	 * replace any existing configurations.
	 * @param configurations the Jetty configurations to apply
	 */
	public void setConfigurations(Collection<? extends Configuration> configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations = new ArrayList<Configuration>(configurations);
	}

	/**
	 * Returns a mutable collection of Jetty {@link Configuration}s that will be applied
	 * to the {@link WebAppContext} before the server is created.
	 * @return the Jetty {@link Configuration}s
	 */
	public Collection<Configuration> getConfigurations() {
		return configurations;
	}

	/**
	 * Add {@link Configuration}s that will be applied to the {@link WebAppContext}
	 * before the server is create.
	 * @param configurations the configurations to add
	 */
	public void addConfigurations(Configuration... configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations.addAll(Arrays.asList(configurations));
	}
}
