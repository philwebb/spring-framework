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

package org.springframework.web.context.embedded;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.ServletContextInitializer;

/**
 * Abstract base class for {@link EmbeddedServletContainerFactory} implementations.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class AbstractEmbeddedServletContainerFactory implements
		EmbeddedServletContainerFactory {

	private final Log logger = LogFactory.getLog(getClass());


	private String contextPath = "";

	private int port = 8080;

	private List<ServletContextInitializer> initializers = new ArrayList<ServletContextInitializer>();

	private File documentRoot;


	/**
	 * Create a new {@link AbstractEmbeddedServletContainerFactory} instance.
	 */
	public AbstractEmbeddedServletContainerFactory() {
	}

	/**
	 * Create a new {@link AbstractEmbeddedServletContainerFactory} instance with the
	 * specified port.
	 * @param port the port number for the embedded servlet container
	 */
	public AbstractEmbeddedServletContainerFactory(int port) {
		setPort(port);
	}

	/**
	 * Create a new {@link AbstractEmbeddedServletContainerFactory} instance with
	 * the specified context path and port.
	 * @param contextPath the context path for the embedded servlet container
	 * @param port the port number for the embedded servlet container
	 */
	public AbstractEmbeddedServletContainerFactory(String contextPath, int port) {
		setContextPath(contextPath);
		setPort(port);
	}

	/**
	 * Sets the context path for the embedded servlet container. The context should
	 * start with a "/" character but not end with a "/" character. The default
	 * context path can be specified using an empty string.
	 * @param contextPath the contextPath to set
	 * @see #getContextPath
	 */
	public void setContextPath(String contextPath) {
		Assert.notNull(contextPath, "ContextPath must not be null");
		if(contextPath.length() > 0) {
			if("/".equals(contextPath)) {
				throw new IllegalArgumentException("Root ContextPath must be specified using an empty string");
			}
			if(!contextPath.startsWith("/") || contextPath.endsWith("/")) {
				throw new IllegalArgumentException("ContextPath must start with '/ and not end with '/'");
			}
		}
		this.contextPath = contextPath;
	}

	/**
	 * Returns the context path for the embedded servlet container. The path will start
	 * with "/" and not end with "/". The root context is represented by an empty string.
	 */
	public String getContextPath() {
		return this.contextPath;
	}

	/**
	 * Sets the port that the embedded servlet container should listen on. If not
	 * specified port '8080' will be used.
	 * @param port the port to set
	 */
	public void setPort(int port) {
		if(port < 1 || port > 65535) {
			throw new IllegalArgumentException("Port must be between 1 and 65535");
		}
		this.port = port;
	}

	/**
	 * Returns the port that the embedded servlet container should listen on.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Sets {@link ServletContextInitializer} that should be applied in addition to
	 * {@link #getEmbdeddedServletContainer(ServletContextInitializer...)} parameters.
	 * This method will replace any previously set or added initializers.
	 * @param initializers the initializers to set
	 * @see #addInitializers
	 * @see #getInitializers
	 */
	public void setInitializers(List<? extends ServletContextInitializer> initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers = new ArrayList<ServletContextInitializer>(initializers);
	}

	/**
	 * Add {@link ServletContextInitializer}s to those that should be applied in addition
	 * to {@link #getEmbdeddedServletContainer(ServletContextInitializer...)} parameters.
	 * @param initializers the initializers to add
	 * @see #setInitializers
	 * @see #getInitializers
	 */
	public void addInitializers(ServletContextInitializer... initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers.addAll(Arrays.asList(initializers));
	}

	/**
	 * Returns a mutable list of {@link ServletContextInitializer} that should be applied
	 * in addition to {@link #getEmbdeddedServletContainer(ServletContextInitializer...)}
	 * parameters.
	 * @return the initializers
	 */
	public List<ServletContextInitializer> getInitializers() {
		return this.initializers;
	}

	/**
	 * Sets the document root folder which will be used by the web context to serve static
	 * files.
	 * @param documentRoot the document root or {@code null} if not required
	 */
	public void setDocumentRoot(File documentRoot) {
		this.documentRoot = documentRoot;
	}

	/**
	 * Returns the document root which will be used by the web context to serve static
	 * files.
	 */
	public File getDocumentRoot() {
		return this.documentRoot;
	}

	/**
	 * Utility method that can be used by subclasses wishing to combine the specified
	 * {@link ServletContextInitializer} parameters with those defined in this instance.
	 * @param initializers the initializers to merge
	 * @return a complete set of merged initializers (with the specified parameters
	 *         appearing first)
	 */
	protected final ServletContextInitializer[] mergeInitializers(ServletContextInitializer... initializers) {
		List<ServletContextInitializer> mergedInitializers = new ArrayList<ServletContextInitializer>();
		mergedInitializers.addAll(Arrays.asList(initializers));
		mergedInitializers.addAll(getInitializers());
		return mergedInitializers.toArray(new ServletContextInitializer[mergedInitializers.size()]);
	}

	/**
	 * Returns the absolute document root when it points to a valid folder, logging a
	 * warning and returning {@code null} otherwise.
	 */
	protected final File getValidDocumentRoot() {
		File root = getDocumentRoot();
		if(root != null && root.exists() && root.isDirectory()) {
			return root.getAbsoluteFile();
		}
		if(logger.isWarnEnabled()) {
			logger.warn("The document root " + root + " does not point to a directory and will be ignored.");
		}
		return null;
	}

}
