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

package org.springframework.aop.scope;

import java.lang.reflect.Executable;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.beans.factory.aot.registration.BeanRegistrationCodeGenerator;
import org.springframework.beans.factory.aot.registration.BeanRegistrationCodeGeneratorFactory;
import org.springframework.beans.factory.aot.registration.DefaultBeanRegistrationCodeGenerator;
import org.springframework.beans.factory.aot.registration.InnerBeanDefinitionMethodGenerator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 *
 * @author pwebb
 * @since 6.0
 */
class ScopedProxyBeanRegistrationCodeGeneratorFactory implements BeanRegistrationCodeGeneratorFactory {

	private static final Log logger = LogFactory.getLog(ScopedProxyBeanRegistrationCodeGeneratorFactory.class);

	@Override
	public BeanRegistrationCodeGenerator getBeanRegistrationCodeGenerator(RegisteredBean registeredBean,
			Executable constructorOrFactoryMethod, ClassName className, MethodGenerator methodGenerator,
			InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator) {
		Class<?> beanType = registeredBean.getBeanType().toClass();
		if (!beanType.equals(ScopedProxyFactoryBean.class)) {
			return null;
		}
		String targetBeanName = getTargetBeanName(registeredBean.getMergedBeanDefinition());
		BeanDefinition targetBeanDefinition = getTargetBeanDefinition(registeredBean.getBeanFactory(), targetBeanName);
		if (targetBeanDefinition == null) {
			logger.warn("Could not handle " + ScopedProxyFactoryBean.class.getSimpleName()
					+ ": no target bean definition found with name " + targetBeanName);
			return null;
		}
		RegisteredBean processedRegisteredBean = registeredBean.withProcessedMergedBeanDefinition(beanDefinition -> {
			RootBeanDefinition processedBeanDefinition = new RootBeanDefinition(beanDefinition);
			processedBeanDefinition.setTargetType(targetBeanDefinition.getResolvableType());
			processedBeanDefinition.getPropertyValues().removePropertyValue("targetBeanName");
			return processedBeanDefinition;
		});
		return new CodeGenerator(processedRegisteredBean, constructorOrFactoryMethod, className, methodGenerator,
				innerBeanDefinitionMethodGenerator, targetBeanName);
	}

	@Nullable
	private String getTargetBeanName(BeanDefinition beanDefinition) {
		Object value = beanDefinition.getPropertyValues().get("targetBeanName");
		return (value instanceof String) ? (String) value : null;
	}

	@Nullable
	private BeanDefinition getTargetBeanDefinition(ConfigurableBeanFactory beanFactory,
			@Nullable String targetBeanName) {
		if (targetBeanName != null && beanFactory.containsBean(targetBeanName)) {
			return beanFactory.getMergedBeanDefinition(targetBeanName);
		}
		return null;
	}

	private static class CodeGenerator extends DefaultBeanRegistrationCodeGenerator {

		private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

		private final String targetBeanName;

		CodeGenerator(RegisteredBean registeredBean, Executable constructorOrFactoryMethod, ClassName className,
				MethodGenerator methodGenerator,
				InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator,
				String targetBeanName) {
			super(registeredBean, constructorOrFactoryMethod, className, methodGenerator,
					innerBeanDefinitionMethodGenerator);
			this.targetBeanName=targetBeanName;
		}

		@Override
		protected CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				boolean allowDirectSupplierShortcut) {
			RegisteredBean registeredBean = getRegisteredBean();
			GeneratedMethod method = getMethodGenerator().generateMethod("get", "scopedProxyInstance").using((builder) -> {
				Class<?> beanClass = registeredBean.getBeanClass();
				builder.addJavadoc("Create the scoped proxy bean instance for '$L'.", registeredBean.getBeanName());
				builder.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
				builder.returns(beanClass);
				builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
				builder.addStatement("$T factory = new $T()", ScopedProxyFactoryBean.class, ScopedProxyFactoryBean.class);
				builder.addStatement("factory.setTargetBeanName($S)", this.targetBeanName);
				builder.addStatement("factory.setBeanFactory($L.getBeanFactory())", REGISTERED_BEAN_PARAMETER_NAME);
				builder.addStatement("return ($T) factory.getObject()", beanClass);
			});
			return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, getClassName(), method.getName());
		}

	}

}
