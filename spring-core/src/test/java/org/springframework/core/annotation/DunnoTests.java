/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.net.JarURLConnection;
import java.net.URLConnection;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 * @author pwebb
 * @since 5.0
 */
public class DunnoTests {

	@Test
	public void test() throws Exception {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		String className = getClass().getName(); //Mockito.class.getName();
		String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX
				+ ClassUtils.convertClassNameToResourcePath(className)
				+ ClassUtils.CLASS_FILE_SUFFIX;
		Resource resource = loader.getResource(resourcePath);
		System.out.println(className);
		System.out.println(resource.getURI());
		System.out.println(resource.getURL());
		URLConnection connection = resource.getURL().openConnection();
		JarURLConnection jarConnection = (JarURLConnection) connection;
		System.out.println(jarConnection.getJarFile().getName());
		//ResourceUtils.getURL(resourceLocation)
	}

}
