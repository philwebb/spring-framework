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

package org.springframework.beans.factory.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.aot.generator.ProtectedAccess;
import org.springframework.aot.generator.ResolvableTypeGenerator;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.generator.config.BeanDefinitionRegistrar;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.support.MultiStatement;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Default {@link BeanRegistrationGenerator} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class DefaultBeanRegistrationGenerator implements BeanRegistrationGenerator {

	private static final ResolvableTypeGenerator typeGenerator = new ResolvableTypeGenerator();

	@Nullable
	private final String beanName;

	private final BeanDefinition beanDefinition;

	private final Executable instanceCreator;

	private final List<BeanInstanceContributor> beanInstanceContributors;

	private final BeanParameterGenerator parameterGenerator;

	private int nesting = 0;

	public DefaultBeanRegistrationGenerator(@Nullable String beanName, BeanDefinition beanDefinition,
			Executable instanceCreator, List<BeanInstanceContributor> beanInstanceContributors) {
		this.beanName = beanName;
		this.beanDefinition = beanDefinition;
		this.instanceCreator = instanceCreator;
		this.beanInstanceContributors = List.copyOf(beanInstanceContributors);
		this.parameterGenerator = new BeanParameterGenerator(this::writeNestedBeanDefinition);
	}

	@Override
	public CodeBlock generateBeanRegistration(GeneratedTypeContext context) {
		CodeContribution beanInstanceContribution = generateBeanInstance(context.getRuntimeHints(),
				this.instanceCreator, this.beanInstanceContributors);
		// Write everything in one place
		ProtectedAccess protectedAccess = beanInstanceContribution.protectedAccess();
		protectedAccess.analyze(this.beanDefinition.getResolvableType());
		String targetPackageName = context.getMainGeneratedType().getClassName().packageName();
		String protectedPackageName = protectedAccess.getPrivilegedPackageName(targetPackageName);
		CodeBlock.Builder code = CodeBlock.builder();
		if (protectedPackageName != null) {
			GeneratedType type = context.getGeneratedType(protectedPackageName);
			MethodSpec method = type.addMethod(beanRegistrationMethod(methodBody ->
					writeBeanRegistration(beanInstanceContribution.statements(), methodBody)));
			code.addStatement("$T.$N(beanFactory)", type.getClassName(), method);
		}
		else {
			writeBeanRegistration(beanInstanceContribution.statements(), code);
		}
		return code.build();
	}

	/**
	 * Return the predicate to use to include Bean Definition
	 * {@link AttributeAccessor attributes}.
	 * @return the bean definition's attributes include filter
	 */
	protected Predicate<String> getAttributeFilter() {
		return candidate -> false;
	}

	/**
	 * Specify if the creator {@link Executable} should be defined. By default,
	 * a creator is specified if the {@code instanceSupplier} callback is used
	 * with an {@code instanceContext} callback.
	 * @param instanceCreator the executable to use to instantiate the bean
	 * @return {@code true} to declare the creator
	 */
	protected boolean shouldDeclareCreator(Executable instanceCreator) {
		if (instanceCreator instanceof Method) {
			return true;
		}
		if (instanceCreator instanceof Constructor<?> constructor) {
			int minArgs = isInnerClass(constructor.getDeclaringClass()) ? 2 : 1;
			return instanceCreator.getParameterCount() >= minArgs;
		}
		return false;
	}

	/**
	 * Return the necessary code to instantiate and post-process a bean.
	 * @param runtimeHints the {@link RuntimeHints} to use
	 * @param instanceCreator the {@link Executable} to use to create the instance
	 * @param contributors additional contributors
	 * @return a code contribution that provides an initialized bean instance
	 */
	protected CodeContribution generateBeanInstance(RuntimeHints runtimeHints, Executable instanceCreator,
			List<BeanInstanceContributor> contributors) {

		return new DefaultBeanInstanceGenerator(instanceCreator, contributors).generateBeanInstance(runtimeHints);
	}

	void writeBeanRegistration(MultiStatement beanInstanceStatements, Builder code) {
		initializeBeanDefinitionRegistrar(beanInstanceStatements, code);
		code.addStatement(".register(beanFactory)");
	}

	void writeBeanDefinition(MultiStatement beanInstanceStatements, Builder code) {
		initializeBeanDefinitionRegistrar(beanInstanceStatements, code);
		code.add(".toBeanDefinition()");
	}

	private void initializeBeanDefinitionRegistrar(MultiStatement beanInstanceStatements, Builder code) {
		code.add("$T", BeanDefinitionRegistrar.class);
		if (this.beanName != null) {
			code.add(".of($S, ", this.beanName);
		}
		else {
			code.add(".inner(");
		}
		writeBeanType(code);
		code.add(")");
		boolean shouldDeclareCreator = shouldDeclareCreator(this.instanceCreator);
		if (shouldDeclareCreator) {
			handleCreatorReference(code, this.instanceCreator);
		}
		code.add("\n").indent().indent();
		code.add(".instanceSupplier(");
		code.add(beanInstanceStatements.toCodeBlock());
		code.add(")").unindent().unindent();
		handleBeanDefinitionMetadata(code);
	}

	private static boolean isInnerClass(Class<?> type) {
		return type.isMemberClass() && !java.lang.reflect.Modifier.isStatic(type.getModifiers());
	}

	private void handleCreatorReference(Builder code, Executable creator) {
		if (creator instanceof Method) {
			code.add(".withFactoryMethod($T.class, $S", creator.getDeclaringClass(), creator.getName());
			if (creator.getParameterCount() > 0) {
				code.add(", ");
			}
		}
		else {
			code.add(".withConstructor(");
		}
		code.add(this.parameterGenerator.writeExecutableParameterTypes(creator));
		code.add(")");
	}

	private void handleBeanDefinitionMetadata(Builder code) {
		String bdVariable = determineVariableName("bd");
		MultiStatement statements = new MultiStatement();
		if (this.beanDefinition.isPrimary()) {
			statements.addStatement("$L.setPrimary(true)", bdVariable);
		}
		String scope = this.beanDefinition.getScope();
		if (StringUtils.hasText(scope) && !ConfigurableBeanFactory.SCOPE_SINGLETON.equals(scope)) {
			statements.addStatement("$L.setScope($S)", bdVariable, scope);
		}
		String[] dependsOn = this.beanDefinition.getDependsOn();
		if (!ObjectUtils.isEmpty(dependsOn)) {
			statements.addStatement("$L.setDependsOn($L)", bdVariable,
					this.parameterGenerator.writeParameterValue(dependsOn));
		}
		if (this.beanDefinition.isLazyInit()) {
			statements.addStatement("$L.setLazyInit(true)", bdVariable);
		}
		if (!this.beanDefinition.isAutowireCandidate()) {
			statements.addStatement("$L.setAutowireCandidate(false)", bdVariable);
		}
		if (this.beanDefinition instanceof AbstractBeanDefinition
				&& ((AbstractBeanDefinition) this.beanDefinition).isSynthetic()) {
			statements.addStatement("$L.setSynthetic(true)", bdVariable);
		}
		if (this.beanDefinition.getRole() != BeanDefinition.ROLE_APPLICATION) {
			statements.addStatement("$L.setRole($L)", bdVariable, this.beanDefinition.getRole());
		}
		Map<Integer, ValueHolder> indexedArgumentValues = this.beanDefinition.getConstructorArgumentValues()
				.getIndexedArgumentValues();
		if (!indexedArgumentValues.isEmpty()) {
			handleArgumentValues(statements, bdVariable, indexedArgumentValues);
		}
		if (this.beanDefinition.hasPropertyValues()) {
			handlePropertyValues(statements, bdVariable, this.beanDefinition.getPropertyValues());
		}
		if (this.beanDefinition.attributeNames().length > 0) {
			handleAttributes(statements, bdVariable);
		}
		if (statements.isEmpty()) {
			return;
		}
		code.add(statements.toCodeBlock(".customize((" + bdVariable + ") ->"));
		code.add(")");
	}

	private void handleArgumentValues(MultiStatement statements, String bdVariable,
			Map<Integer, ValueHolder> indexedArgumentValues) {
		if (indexedArgumentValues.size() == 1) {
			Entry<Integer, ValueHolder> entry = indexedArgumentValues.entrySet().iterator().next();
			statements.add(writeArgumentValue(bdVariable + ".getConstructorArgumentValues().",
					entry.getKey(), entry.getValue()));
		}
		else {
			String avVariable = determineVariableName("argumentValues");
			statements.addStatement("$T $L = $L.getConstructorArgumentValues()", ConstructorArgumentValues.class, avVariable, bdVariable);
			statements.addAll(indexedArgumentValues.entrySet(), entry -> writeArgumentValue(avVariable + ".",
					entry.getKey(), entry.getValue()));
		}
	}

	private CodeBlock writeArgumentValue(String prefix, Integer index, ValueHolder valueHolder) {
		Builder code = CodeBlock.builder();
		code.add(prefix);
		code.add("addIndexedArgumentValue($L, ", index);
		Object value = valueHolder.getValue();
		code.add(this.parameterGenerator.writeParameterValue(value));
		code.add(")");
		return code.build();
	}

	private void handlePropertyValues(MultiStatement statements, String bdVariable,
			PropertyValues propertyValues) {
		PropertyValue[] properties = propertyValues.getPropertyValues();
		if (properties.length == 1) {
			statements.add(writePropertyValue(bdVariable + ".getPropertyValues().", properties[0]));
		}
		else {
			String pvVariable = determineVariableName("propertyValues");
			statements.addStatement("$T $L = $L.getPropertyValues()", MutablePropertyValues.class, pvVariable, bdVariable);
			for (PropertyValue property : properties) {
				statements.addStatement(writePropertyValue(pvVariable + ".", property));
			}
		}
	}

	private CodeBlock writePropertyValue(String prefix, PropertyValue property) {
		Builder code = CodeBlock.builder();
		code.add(prefix);
		code.add("addPropertyValue($S, ", property.getName());
		Object value = property.getValue();
		code.add(this.parameterGenerator.writeParameterValue(value));
		code.add(")");
		return code.build();
	}

	private void handleAttributes(MultiStatement statements, String bdVariable) {
		String[] attributeNames = this.beanDefinition.attributeNames();
		Predicate<String> filter = getAttributeFilter();
		for (String attributeName : attributeNames) {
			if (filter.test(attributeName)) {
				Object value = this.beanDefinition.getAttribute(attributeName);
				Builder code = CodeBlock.builder();
				code.add("$L.setAttribute($S, ", bdVariable, attributeName);
				code.add((this.parameterGenerator.writeParameterValue(value)));
				code.add(")");
				statements.add(code.build());
			}
		}
	}

	private void writeNestedBeanDefinition(BeanDefinition beanDefinition, Builder code) {
		throw new IllegalStateException("Not supported yet");
	}

	private void writeBeanType(Builder code) {
		ResolvableType resolvableType = this.beanDefinition.getResolvableType();
		if (resolvableType.hasGenerics() && !hasUnresolvedGenerics(resolvableType)) {
			code.add(typeGenerator.generateTypeFor(resolvableType));
		}
		else {
			code.add("$T.class", getUserBeanClass());
		}
	}

	private Class<?> getUserBeanClass() {
		return ClassUtils.getUserClass(this.beanDefinition.getResolvableType().toClass());
	}

	private boolean hasUnresolvedGenerics(ResolvableType resolvableType) {
		if (resolvableType.hasUnresolvableGenerics()) {
			return true;
		}
		for (ResolvableType generic : resolvableType.getGenerics()) {
			if (hasUnresolvedGenerics(generic)) {
				return true;
			}
		}
		return false;
	}

	private MethodSpec.Builder beanRegistrationMethod(Consumer<Builder> code) {
		String name = registerBeanMethodName();
		MethodSpec.Builder method = MethodSpec.methodBuilder(name)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(DefaultListableBeanFactory.class, "beanFactory");
		CodeBlock.Builder body = CodeBlock.builder();
		code.accept(body);
		method.addCode(body.build());
		return method;
	}

	private String registerBeanMethodName() {
		if (this.instanceCreator instanceof Method method) {
			String target = (isValidName(this.beanName)) ? this.beanName : method.getName();
			return String.format("register%s_%s", method.getDeclaringClass().getSimpleName(), target);
		}
		else if (this.instanceCreator.getDeclaringClass().getEnclosingClass() != null) {
			String target = (isValidName(this.beanName)) ? this.beanName : getUserBeanClass().getSimpleName();
			Class<?> enclosingClass = this.instanceCreator.getDeclaringClass().getEnclosingClass();
			return String.format("register%s_%s", enclosingClass.getSimpleName(), target);
		}
		else {
			String target = (isValidName(this.beanName)) ? this.beanName : getUserBeanClass().getSimpleName();
			return "register" + StringUtils.capitalize(target);
		}
	}

	private boolean isValidName(@Nullable String name) {
		return name != null && SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name);
	}

	private String determineVariableName(String name) {
		return name + "_" .repeat(this.nesting);
	}

}
