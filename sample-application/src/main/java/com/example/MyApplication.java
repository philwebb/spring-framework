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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyApplication implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void run() {
		System.err.println("Hello World!");
		System.err.println(this.applicationContext.getBean(MyBean.class));
		System.err.println(this.applicationContext.getBean(MyService.class));
		System.err.println(this.applicationContext.getBean(MyNaughtyComponent.class));
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new AnnotationConfigApplicationContext(MyConfiguration.class).getBean(MyApplication.class).run();
	}

}
