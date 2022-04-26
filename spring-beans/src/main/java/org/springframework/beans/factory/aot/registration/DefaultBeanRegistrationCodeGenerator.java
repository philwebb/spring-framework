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

package org.springframework.beans.factory.aot.registration;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;

/**
 * Default implementation of {@link BeanRegistrationCode} that should work for most beans.
 * Generates code in the following form:<blockquote><pre class="code">
 * Class&lt;?&gt; beanType = MyBean.class;
 * RootBeanDefinition beanDefintion = new RootBeanDefinition(beanType);
 * beanDefintion.setScope(...);
 * ...
 * InstanceSupplier&lt;...&gt; instanceSupplier = InstanceSupplier.of(...);
 * instanceSupplier = instanceSupplier.withPostProcessor(...);
 * beanDefintion.setInstanceSupplier(instanceSupplier);
 * return beanDefinition;
 * </pre></blockquote>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCodeGeneratorFactory
 */
public class DefaultBeanRegistrationCodeGenerator extends AbstractBeanRegistrationCodeGenerator {

	protected static final String BEAN_DEFINITION_VARIABLE = BeanRegistrationCodeGenerator.BEAN_DEFINITION_VARIABLE;

	protected static final String BEAN_TYPE_VARIABLE = "beanType";

	protected static final String INSTANCE_SUPPLIER_VARIABLE = "instanceSupplier";

	/**
	 * Create a new {@link DefaultBeanRegistrationCodeGenerator} instance.
	 * @param registeredBean the registered bean
	 * @param constructorOrFactoryMethod the constructor or factory method that creates
	 * the bean
	 * @param className the name of the class being used for registrations
	 * @param methodGenerator the method generator to use
	 * @param innerBeanDefinitionMethodGenerator the inner-bean definition method
	 * generator to use
	 */
	public DefaultBeanRegistrationCodeGenerator(RegisteredBean registeredBean, Executable constructorOrFactoryMethod,
			ClassName className, MethodGenerator methodGenerator,
			InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator) {
		super(registeredBean, constructorOrFactoryMethod, className, methodGenerator,
				innerBeanDefinitionMethodGenerator);
	}

	@Override
	public CodeBlock generateCode(GenerationContext generationContext) {
		CodeBlock.Builder builder = createBuilder();
		builder.add(generatedNewBeanDefinitionCode(generationContext));
		builder.add(generateSetBeanDefinitionPropertiesCode(generationContext));
		builder.add(generateSetBeanInstanceSupplierCode(generationContext));
		builder.add(generateReturnCode(generationContext));
		return build(generationContext, builder);
	}

	/**
	 * Return the {@link CodeBlock} builder that will be used to generate code. Subclasses
	 * can override this method to add custom code before the builder is used.
	 * @return the {@link CodeBlock} builder.
	 */
	protected CodeBlock.Builder createBuilder() {
		return CodeBlock.builder();
	}

	/**
	 * Generate code to create a new {@link BeanDefinition} of the appropriate type.
	 * @param generationContext the generate context
	 * @return the code to create the {@link BeanDefinition}
	 */
	protected CodeBlock generatedNewBeanDefinitionCode(GenerationContext generationContext) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.addStatement(generateBeanTypeCode(generationContext));
		builder.addStatement("$T $L = new $T($L)", RootBeanDefinition.class, BEAN_DEFINITION_VARIABLE,
				RootBeanDefinition.class, BEAN_TYPE_VARIABLE);
		return builder.build();
	}

	/**
	 * Generate code to define the bean type.
	 * @return the bean type code
	 */
	protected CodeBlock generateBeanTypeCode(GenerationContext generationContext) {
		ResolvableType beanType = getRegisteredBean().getBeanType();
		if (!beanType.hasGenerics()) {
			return CodeBlock.of("$T<?> $L = $L", Class.class, BEAN_TYPE_VARIABLE,
					InstanceCodeGenerationService.getSharedInstance().generateCode(beanType.toClass()));
		}
		return CodeBlock.of("$T $L = $L", ResolvableType.class, BEAN_TYPE_VARIABLE,
				InstanceCodeGenerationService.getSharedInstance().generateCode(beanType));
	}

	/**
	 * Generate code to set the properties on the {@link BeanDefinition}.
	 * @param generationContext the generation context
	 * @return the code to set properties
	 * @see #generateBeanDefinitionPropertiesCode(Predicate, MethodGenerator, Function,
	 * BeanDefinition)
	 */
	protected CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext) {
		RuntimeHints hints = generationContext.getRuntimeHints();
		RegisteredBean registeredBean = getRegisteredBean();
		BeanDefinition beanDefintion = registeredBean.getMergedBeanDefinition();
		Predicate<String> attributeFilter = this::isAttributeIncluded;
		return generateBeanDefinitionPropertiesCode(hints, attributeFilter, getMethodGenerator(),
				(name, value) -> generateValueCode(generationContext, name, value), beanDefintion);
	}

	/**
	 * Determines if the given attribute is to be included in the generated code. By
	 * default this method always returns {@code true}. Subclasses can override this
	 * method if custom attribute filtering is required.
	 * @param attributeName the attribute name
	 * @return if the attribute is filtered
	 */
	protected boolean isAttributeIncluded(String attributeName) {
		return true;
	}

	/**
	 * Generate custom code for a bean property or constructor value. By default this
	 * method is used to support inner-beans.
	 * @param generationContext the generation context
	 * @param name the name of the property or constructor argument
	 * @param value the property or constructor argument value
	 * @return generated code or {@code null} to use default generation
	 */
	@Nullable
	protected CodeBlock generateValueCode(GenerationContext generationContext, String name, Object value) {
		RegisteredBean innerRegisteredBean = getInnerRegisteredBean(value);
		if (innerRegisteredBean != null) {
			MethodReference generatedMethod = getInnerBeanDefinitionMethodGenerator()
					.generateInnerBeanDefinitionMethod(generationContext, innerRegisteredBean, name);
			return generatedMethod.toInvokeCodeBlock();
		}
		return null;
	}

	@Nullable
	private RegisteredBean getInnerRegisteredBean(Object value) {
		if (value instanceof BeanDefinitionHolder beanDefinitionHolder) {
			return RegisteredBean.ofInnerBean(getRegisteredBean(), beanDefinitionHolder);
		}
		if (value instanceof BeanDefinition beanDefinition) {
			return RegisteredBean.ofInnerBean(getRegisteredBean(), beanDefinition);
		}
		return null;
	}

	/**
	 * Generate the code to set the instance supplier on the bean definition.
	 * @param generationContext the generation context
	 * @return the set instance supplier code
	 */
	private CodeBlock generateSetBeanInstanceSupplierCode(GenerationContext generationContext) {
		CodeBlock.Builder builder = CodeBlock.builder();
		List<MethodReference> postProcessors = getInstancePostProcessorMethodReferences();
		CodeBlock instanceSupplierCode = generateInstanceSupplierCode(generationContext, getClassName(),
				getMethodGenerator(), getRegisteredBean(), getConstructorOrFactoryMethod(), postProcessors.isEmpty());
		if (postProcessors.isEmpty()) {
			builder.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE, instanceSupplierCode);
			return builder.build();
		}
		builder.addStatement("$T $L = $L",
				ParameterizedTypeName.get(InstanceSupplier.class, getRegisteredBean().getBeanClass()),
				INSTANCE_SUPPLIER_VARIABLE, instanceSupplierCode);
		for (MethodReference postProcessor : postProcessors) {
			builder.addStatement("$L = $L.andThen($L)", INSTANCE_SUPPLIER_VARIABLE, INSTANCE_SUPPLIER_VARIABLE,
					postProcessor.toCodeBlock());
		}
		builder.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE, INSTANCE_SUPPLIER_VARIABLE);
		return builder.build();
	}

	/**
	 * Generate the code that returns the bean definition.
	 * @param generationContext the generation context
	 * @return the return code
	 */
	protected CodeBlock generateReturnCode(GenerationContext generationContext) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.addStatement("return $L", BEAN_DEFINITION_VARIABLE);
		return builder.build();
	}

	/**
	 * Build the generated code. Subclasses can override this method to add custom code
	 * before the final result is returned.
	 * @param generationContext the generation context
	 * @param builder the builder
	 * @return the built code
	 */
	protected CodeBlock build(GenerationContext generationContext, Builder builder) {
		return builder.build();
	}

	/**
	 * Generates standard instance supplier code for a {@link RegisteredBean}.
	 * @param generationContext the generation context
	 * @param className the name of the class being used
	 * @param methodGenerator the method generator to use
	 * @param registeredBean the registered bean
	 * @param allowDirectSupplierShortcut if a direct {@link Supplier} shortcut can be
	 * returned rather than always needing an {@link InstanceSupplier}
	 * @return a code block containing the {@link InstanceSupplier} code
	 */
	protected final CodeBlock generateInstanceSupplierCode(GenerationContext generationContext, ClassName className,
			MethodGenerator methodGenerator, RegisteredBean registeredBean, Executable constructorOrFactoryMethod,
			boolean allowDirectSupplierShortcut) {
		return new InstanceSupplierCodeGenerator(generationContext, className, methodGenerator,
				allowDirectSupplierShortcut).generateCode(registeredBean, constructorOrFactoryMethod);
	}

	/**
	 * Generates standard {@link BeanDefinition} property setter code.
	 * @param attributeFilter the attribute filter to use
	 * @param methodGenerator the method generator to use
	 * @param valueCodeGenerator the value code generator to use
	 * @param beanDefinition the source bean definition
	 * @return a code block containing property setter code
	 */
	protected final CodeBlock generateBeanDefinitionPropertiesCode(RuntimeHints hints,
			Predicate<String> attributeFilter, MethodGenerator methodGenerator,
			BiFunction<String, Object, CodeBlock> valueCodeGenerator, BeanDefinition beanDefinition) {
		return new BeanDefinitionPropertiesCodeGenerator(hints, attributeFilter, methodGenerator, valueCodeGenerator)
				.generateCode(beanDefinition);
	}

}
