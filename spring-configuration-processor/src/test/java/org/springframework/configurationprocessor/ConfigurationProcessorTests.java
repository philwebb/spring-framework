/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.configurationprocessor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.configurationprocessor.example.BaseConfig;
import org.springframework.configurationprocessor.example.BeanFactoryAwareConfig;
import org.springframework.configurationprocessor.example.ConstructorArgsConfig;
import org.springframework.configurationprocessor.example.ExtendsConfig;
import org.springframework.configurationprocessor.example.InnerConfig;
import org.springframework.configurationprocessor.example.MethodParameterConfig;
import org.springframework.configurationprocessor.example.SimplestPossibleConfig;

import static org.junit.Assert.*;

/**
 * Tests for {@link ConfigurationProcessor}.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public class ConfigurationProcessorTests {

	public static final File SOURCE_FOLDER = new File("src/test/java");

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private JavaCompiler compiler;

	private StandardJavaFileManager fileManager;

	private File outputLocation;

	@Before
	public void setup() throws Exception {
		this.compiler = ToolProvider.getSystemJavaCompiler();
		this.fileManager = this.compiler.getStandardFileManager(null, null, null);
		this.outputLocation = temporaryFolder.newFolder();
		Iterable<? extends File> temp = Arrays.asList(this.outputLocation);
		this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, temp);
		this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, temp);
	}
//FIXME static @Bean
	@Test
	public void simplestPossibleConfig() throws Exception {
		compile(SimplestPossibleConfig.class);
	}

	@Test
	public void extendsConfig() throws Exception {
		compile(BaseConfig.class, ExtendsConfig.class);
	}

	@Test
	public void methodParameterConfig() throws Exception {
		compile(MethodParameterConfig.class);
	}

	@Test
	public void beanFactoryAwareConfig() throws Exception {
		compile(BeanFactoryAwareConfig.class);
	}

	@Test
	public void constructorArgs() throws Exception {
		compile(ConstructorArgsConfig.class);
	}

	@Test
	public void innerConfig() throws Exception {
		compile(InnerConfig.class);
	}

	private void compile(Class<?>... types) throws IOException {
		Iterable<? extends JavaFileObject> compilationUnits = getJavaFileObjects(types);
		CompilationTask task = this.compiler.getTask(null, fileManager, null, null, null,
				compilationUnits);
		task.setProcessors(Arrays.asList(new ConfigurationProcessor()));
		assertTrue("Compile failed", task.call());
	}

	private Iterable<? extends JavaFileObject> getJavaFileObjects(Class<?>... types) {
		File[] files = new File[types.length];
		for (int i = 0; i < types.length; i++) {
			files[i] = new File(SOURCE_FOLDER, sourcePathFor(types[i]));
		}
		return this.fileManager.getJavaFileObjects(files);
	}

	private String sourcePathFor(Class<?> type) {
		return type.getName().replace('.', '/') + ".java";
	}
}
