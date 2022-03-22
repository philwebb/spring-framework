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

import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;

public final class MyProcessedApplication {

	private MyProcessedApplication() {
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
		beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
		List<DefaultListableBeanFactoryInitializer> beanFactoryInitializers = SpringFactoriesLoader
				.forNamedItem(BeanFactory.class, "default").load(DefaultListableBeanFactoryInitializer.class);
		beanFactoryInitializers.forEach(beanFactoryInitializer -> beanFactoryInitializer.initialize(beanFactory));
		context.refresh();
		context.getBean(MyApplication.class).run();
	}

}
