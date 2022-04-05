/*
 * Copyright 2002-2022 the original author or authors.
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

package com.example;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.aot.context.XAotContribution;
import org.springframework.aot.context.XAotProcessors;
import org.springframework.aot.context.XDefaultAotContext;
import org.springframework.aot.context.XTrackedAotProcessors;
import org.springframework.aot.context.XTrackedAotProcessors.FileTracker;
import org.springframework.aot.generate.DefaultGeneratedSpringFactories;
import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.beans.factory.aot.XAotBeanFactoryProcessor;
import org.springframework.beans.factory.aot.XDefinedBean;
import org.springframework.beans.factory.aot.DefinedBeanExcludeFilter;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.aot.BeanRegistrationsAotBeanFactoryProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AotProcess {

	private void run() throws Exception {
		DefaultListableBeanFactory beanFactory = getSourceBeanFactory();
		GeneratedFiles generatedFiles = getGeneratedFiles();
		FileTracker tracker = new FileTracker();
		Deque<XAotContribution> contributions = new ArrayDeque<>();
		XAotProcessors aotProcessors = new XTrackedAotProcessors(contributions::add, tracker);
		XDefaultAotContext aotContext = new XDefaultAotContext(generatedFiles, aotProcessors);
		aotContext.getProcessors().add(new BeanRegistrationsAotBeanFactoryProcessor());
		aotContext.getProcessors().allOfType(XAotBeanFactoryProcessor.class).processAndApplyContributions(new UniqueBeanFactoryName("default"), beanFactory);
		while(!contributions.isEmpty()) {
			XAotContribution contribution = contributions.removeFirst();
			contribution.applyTo(aotContext);
		}
		DefaultGeneratedSpringFactories springFactories = (DefaultGeneratedSpringFactories) aotContext.getGeneratedSpringFactories();
		springFactories.writeTo(generatedFiles);
		tracker.save(generatedFiles);
	}

	private GeneratedFiles getGeneratedFiles() {
		Path root = new File(".").toPath().resolve("gen").normalize().toAbsolutePath();
		GeneratedFiles generatedFiles = new FileSystemGeneratedFiles(root);
		return generatedFiles;
	}

	@SuppressWarnings("resource")
	private DefaultListableBeanFactory getSourceBeanFactory() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(MyConfiguration.class);
		context.getBeanFactory().registerSingleton("excluder", new DefinedBeanExcludeFilter() {

			@Override
			public boolean isExcluded(XDefinedBean definedBean) {
				return definedBean.getBeanName().equals("excluder") || definedBean.getBeanName().contains("internal");
			}

		});
		context.refreshForAotProcessing();
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
		return beanFactory;
	}

	public static void main(String[] args) throws Exception {
		new AotProcess().run();
	}

}
