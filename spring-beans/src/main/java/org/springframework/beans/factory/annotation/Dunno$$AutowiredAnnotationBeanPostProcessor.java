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

package org.springframework.beans.factory.annotation;

import java.util.Collection;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 *
 * @author pwebb
 * @since 6.0
 */
public class Dunno$$AutowiredAnnotationBeanPostProcessor implements Supplier<InjectionMetadata> {

	@Override
	public InjectionMetadata get() {
		return AutowiredAnnotationBeanPostProcessor.buildAutowiringMetadata(autowire-> {
			autowire.optionalField("fieldName");
			autowire.optionalMethod("methodName", InputStream.class);
			autowire.requiredField("otherFieldName");
		});
	}

	// Or perhaps
	// Dunno$$AutowiredAnnotation extends AotContributedAutowiredAnnotationBeanPostProcessor {

	// setup(autowire -> {autowire.optionalField())};

	// }




	private Collection<InjectedElement> getAutowiredElements() {


	}




	public static void main(String[] args) {
		SpringFactoriesLoader.forNamedItem(AutowiredAnnotationBeanPostProcessor.class,
				MyBean.class.getName()).load(Supplier.class);
	}

	static class MyBean {
	}


}
