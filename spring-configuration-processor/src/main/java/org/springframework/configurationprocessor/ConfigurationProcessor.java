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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.TypeVariableName;

/**
 *
 * @author pwebb
 * @since 5.0
 */
public class ConfigurationProcessor extends AbstractProcessor {

	private Types types;

	private Elements elements;

	private Filer filer;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.types = processingEnv.getTypeUtils();
		this.elements = processingEnv.getElementUtils();
		this.filer = processingEnv.getFiler();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(SpringClassNames.CONFIGURATION.toString());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		process(ElementFilter.typesIn(roundEnv.getRootElements()));
		return true;
	}

	private void process(Set<TypeElement> types) {
		types.stream().filter(this::isConfiguration).forEach(this::process);
	}

	private void process(TypeElement type) {
		write(createType(type), ClassName.get(type).packageName());
	}

	private TypeSpec createType(TypeElement type) {
		ClassName className = ClassName.get(type);
		ClassName beansClassName = className.peerClass(
				className.simpleName() + "$$ConfigurationProxy");
		boolean beanFactoryAware = hasInterface(type,
				SpringClassNames.BEAN_FACTORY_AWARE);
		Builder builder = TypeSpec.classBuilder(beansClassName);
		builder.addModifiers(type.getModifiers().toArray(new Modifier[0]));
		addExtendsAndImplements(builder, className, beanFactoryAware);
		addFields(builder);
		addConstructors(builder, type);
		addSetBeanFactoryMethod(builder, beanFactoryAware);
		addBeanMethods(builder, type);
		addInnerTypes(builder, type);
		TypeSpec type1 = builder.build();
		return type1;
	}

	private void addInnerTypes(Builder builder, TypeElement type) {
		ElementFilter.typesIn(type.getEnclosedElements()).stream().filter(
				this::isConfiguration).map(this::createType).forEach(builder::addType);
	}

	private boolean isConfiguration(TypeElement type) {
		return !isAbstract(type) && isAnnotated(type, SpringClassNames.CONFIGURATION);
	}

	private void addExtendsAndImplements(Builder builder, ClassName className,
			boolean beanFactoryAware) {
		builder.superclass(className);
		if (!beanFactoryAware) {
			builder.addSuperinterface(SpringClassNames.BEAN_FACTORY_AWARE);
		}
	}

	private void addFields(Builder builder) {
		builder.addField(SpringClassNames.PRE_PROCESSED_CONFIGURATION, "configuration",
				Modifier.PRIVATE);
	}

	private void addConstructors(Builder builder, TypeElement type) {
		ElementFilter.constructorsIn(type.getEnclosedElements()).stream().map(
				this::createConstructor).forEach(builder::addMethod);
	}

	private MethodSpec createConstructor(ExecutableElement constructor) {
		List<? extends VariableElement> parameters = constructor.getParameters();
		MethodSpec.Builder builder = MethodSpec.constructorBuilder();
		builder.addModifiers(constructor.getModifiers());
		getTypeVariableNames(constructor).forEach(builder::addTypeVariable);
		getParameters(constructor).forEach(builder::addParameter);
		builder.varargs(constructor.isVarArgs());
		constructor.getThrownTypes().stream().map(TypeName::get).forEach(
				builder::addException);
		if (!parameters.isEmpty()) {
			String args = parameters.stream().map(this::getSimpleName).collect(
					Collectors.joining(", "));
			builder.addStatement("super($L)", args);
		}
		return builder.build();
	}

	private void addSetBeanFactoryMethod(Builder builder, boolean beanFactoryAware) {
		builder.addMethod(createSetBeanFactoryMethod(beanFactoryAware));
	}

	private MethodSpec createSetBeanFactoryMethod(boolean beanFactoryAware) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("setBeanFactory");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(ParameterSpec.builder(SpringClassNames.BEAN_FACTORY,
				"beanFactory").build());
		builder.addException(SpringClassNames.BEANS_EXCEPTION);
		if (beanFactoryAware) {
			builder.addStatement("super.setBeanFactory(beanFactory)");
		}
		builder.addStatement("this.configuration = new $T(beanFactory, this, $T.lookup())",
				SpringClassNames.PRE_PROCESSED_CONFIGURATION,
				ClassName.get(MethodHandles.class));
		return builder.build();
	}

	private void addBeanMethods(Builder builder, TypeElement type) {
		getBeanMethods(type).stream().map(this::createBeanMethod).forEach(
				builder::addMethod);
	}

	private MethodSpec createBeanMethod(ExecutableElement beanMethod) {
		MethodSpec.Builder builder = MethodSpec.overriding(beanMethod);
		String parameterNames = getParameters(beanMethod, this::getSimpleName).collect(
				Collectors.joining(", "));
		Object[] parameterTypes = getParameters(beanMethod,
				p -> TypeName.get(types.erasure(p.asType()))).toArray();
		String parameterVariables = getParameters(beanMethod, p -> "$T.class").collect(
				Collectors.joining(", "));
		builder.addStatement("return this.configuration.beanMethod(\""
				+ beanMethod.getSimpleName() + "\""
				+ (parameterVariables.isEmpty() ? "" : ", " + parameterVariables)
				+ ").invoke(" + parameterNames + ")", parameterTypes);
		return builder.build();
	}

	private String getSimpleName(Element element) {
		return element.getSimpleName().toString();
	}

	private Stream<TypeVariableName> getTypeVariableNames(ExecutableElement element) {
		return element.getTypeParameters().stream().map(this::getTypeVariableName);
	}

	private TypeVariableName getTypeVariableName(TypeParameterElement element) {
		return TypeVariableName.get((TypeVariable) element.asType());
	}

	private Stream<ParameterSpec> getParameters(ExecutableElement method) {
		return getParameters(method, ParameterSpec::get);
	}

	private <T> Stream<T> getParameters(ExecutableElement method,
			Function<VariableElement, T> mapper) {
		return method.getParameters().stream().map(mapper);
	}

	private boolean hasInterface(TypeElement element, ClassName type) {
		return this.types.isAssignable(element.asType(),
				this.elements.getTypeElement(type.toString()).asType());
	}

	private List<ExecutableElement> getBeanMethods(TypeElement type) {
		Set<Name> seen = new HashSet<>();
		List<ExecutableElement> beanMethods = new ArrayList<>();
		while (type != null) {
			for (ExecutableElement candidate : ElementFilter.methodsIn(
					type.getEnclosedElements())) {
				if (isBeanMethod(candidate) && seen.add(candidate.getSimpleName())) {
					beanMethods.add(candidate);
				}
			}
			type = getSuperType(type);
		}
		return beanMethods;
	}

	private boolean isBeanMethod(ExecutableElement element) {
		Set<Modifier> modifiers = element.getModifiers();
		return (isAnnotated(element, SpringClassNames.BEAN)
				&& !modifiers.contains(Modifier.STATIC)
				&& !modifiers.contains(Modifier.PRIVATE));
	}

	private TypeElement getSuperType(TypeElement type) {
		TypeMirror superType = type.getSuperclass();
		return (superType == null ? null : (TypeElement) types.asElement(superType));
	}

	private boolean isAbstract(TypeElement type) {
		return type.getModifiers().contains(Modifier.ABSTRACT);
	}

	private boolean isAnnotated(Element element, ClassName type) {
		for (AnnotationMirror candidate : element.getAnnotationMirrors()) {
			if (type.equals(ClassName.get(candidate.getAnnotationType()))) {
				return true;
			}
		}
		return false;
	}

	private void write(TypeSpec type, String packageName) {
		JavaFile file = JavaFile.builder(packageName, type).build();
		try {
			file.writeTo(this.filer);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
