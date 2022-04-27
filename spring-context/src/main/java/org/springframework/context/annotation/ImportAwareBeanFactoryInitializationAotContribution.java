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

package org.springframework.context.annotation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanFactoryInitializationAotContribution} from
 * {@link ConfigurationClassPostProcessor} to add {@link ImportAwareAotBeanPostProcessor}
 * support.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class ImportAwareBeanFactoryInitializationAotContribution implements BeanFactoryInitializationAotContribution {

	private static final String BEAN_FACTORY_VARIABLE = BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE;

	private static final ParameterizedTypeName STRING_STRING_MAP = ParameterizedTypeName.get(Map.class, String.class,
			String.class);

	private static final String IMPORT_REGISTRY_BEAN_NAME = ConfigurationClassPostProcessor.IMPORT_REGISTRY_BEAN_NAME;

	private static final String MAPPINGS_VARIABLE = "mappings";

	private final ConfigurableListableBeanFactory beanFactory;

	public ImportAwareBeanFactoryInitializationAotContribution(ConfigurableListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode) {
		Map<String, String> mappings = buildImportAwareMappings();
		if (!mappings.isEmpty()) {
			GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethodGenerator()
					.generateMethod("addImportAwareBeanPostProcessors")
					.using(builder -> generateAddPostProcessorMethod(builder, mappings));
			beanFactoryInitializationCode.addInitializer(MethodReference.of(generatedMethod.getName()));
			ResourceHints hints = generationContext.getRuntimeHints().resources();
			mappings.forEach((target, from) -> hints.registerType(TypeReference.of(from)));
		}
	}

	private void generateAddPostProcessorMethod(MethodSpec.Builder builder, Map<String, String> mappings) {
		builder.addJavadoc("Add ImportAwareBeanPostProcessor to support ImportAware beans");
		builder.addModifiers(Modifier.PRIVATE);
		builder.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_VARIABLE);
		builder.addCode(generateAddPostProcessorCode(mappings));
	}

	private CodeBlock generateAddPostProcessorCode(Map<String, String> mappings) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.addStatement("$T $L = new $T<>()", STRING_STRING_MAP, MAPPINGS_VARIABLE, HashMap.class);
		mappings.forEach((type, from) -> builder.addStatement("$L.put($S, $S)", MAPPINGS_VARIABLE, type, from));
		builder.addStatement("$L.addBeanPostProcessor(new $T($L))", BEAN_FACTORY_VARIABLE,
				ImportAwareAotBeanPostProcessor.class, MAPPINGS_VARIABLE);
		return builder.build();
	}

	private Map<String, String> buildImportAwareMappings() {
		ImportRegistry importRegistry = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
		Map<String, String> mappings = new LinkedHashMap<>();
		for (String name : this.beanFactory.getBeanDefinitionNames()) {
			Class<?> beanType = this.beanFactory.getType(name);
			if (beanType != null && ImportAware.class.isAssignableFrom(beanType)) {
				String target = ClassUtils.getUserClass(beanType).getName();
				AnnotationMetadata from = importRegistry.getImportingClassFor(target);
				if (from != null) {
					mappings.put(target, from.getClassName());
				}
			}
		}
		return mappings;
	}

}
