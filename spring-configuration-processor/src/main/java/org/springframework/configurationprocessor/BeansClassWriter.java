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

import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;

/**
 *
 * @author pwebb
 * @since 5.0
 */
public class BeansClassWriter {

	private final Types types;

	private final PrintStream out;

	private int nesting;

	public BeansClassWriter(Types types, PrintStream out) {
		this.types = types;
		this.out = out;
	}

	public void startClass(TypeElement type, boolean alreadyBeanFactoryAware) {
		println("package " + getPackage(type).getQualifiedName() + ";");
		println();
		println("import java.lang.invoke.MethodHandles;");
		println("import org.springframework.beans.BeansException;");
		println("import org.springframework.beans.factory.BeanFactory;");
		println("import org.springframework.beans.factory.BeanFactoryAware;");
		println("import org.springframework.context.annotation.ConfigurationClassBeanMethods;");
		println();
		println("class " + type.getSimpleName() + "$$Beans extends "
				+ type.getQualifiedName() + (alreadyBeanFactoryAware ? " {"
						: "\n\t\timplements BeanFactoryAware {"));
	}

	private PackageElement getPackage(Element element) {
		if (element == null || element instanceof PackageElement) {
			return (PackageElement) element;
		}
		return getPackage(element.getEnclosingElement());
	}

	public void endClass() {
		println("}");
	}

	public void startInnerClass(TypeElement type, boolean alreadyBeanFactoryAware) {
		this.nesting++;
		String className = type.getSimpleName().toString();
		boolean staticClass = type.getModifiers().contains(Modifier.STATIC);
		println("public " + (staticClass ? "static " : "") + "class " + className
				+ "$$Beans extends " + className + (alreadyBeanFactoryAware ? " {"
						: "\n\t\timplements BeanFactoryAware {"));
	}

	public void endInnerClass() {
		println("}");
		println();
		this.nesting--;
	}

	public void beanMethodsField() {
		println();
		println("\tprivate ConfigurationClassBeanMethods beanMethods;");
		println();
	}

	public void constructors(List<ExecutableElement> constructors) {
		if (constructors.size() == 1 && constructors.get(0).getParameters().isEmpty()) {
			return;
		}
		constructors.forEach(this::constructor);
	}

	private void constructor(ExecutableElement constructor) {
		String name = constructor.getEnclosingElement().getSimpleName().toString()
				+ "$$Beans";
		String modifiers = constructor.getModifiers().stream().map(
				Object::toString).collect(Collectors.joining(" "));
		modifiers = modifiers.isEmpty() ? "" : modifiers + " ";
		println("\t" + modifiers + name + parameters(constructor.getParameters()) + " {");
		println("\t\tsuper(" + constructor.getParameters().stream().map(
				(p) -> p.getSimpleName().toString()).collect(Collectors.joining(", "))
				+ ");");
		println("\t}");
		println();
	}

	public void setBeanFactory(boolean alreadyBeanFactoryAware) {
		println("\t@Override");
		println("\tpublic void setBeanFactory(BeanFactory beanFactory) throws BeansException {");
		if (alreadyBeanFactoryAware) {
			println("\t\tsuper.setBeanFactory(beanFactory);");
		}
		println("\t\tthis.beanMethods = new ConfigurationClassBeanMethods(beanFactory, this, MethodHandles.lookup());");
		println("\t}");
		println();
	}

	public void beanMethods(List<ExecutableElement> beanMethods) {
		beanMethods.forEach(this::beanMethod);
	}

	private void beanMethod(ExecutableElement beanMethod) {
		String parameterTypes = beanMethod.getParameters().stream().map(
				(p) -> types.asElement(p.asType()) + ".class").collect(Collectors.joining(", "));
		String args = beanMethod.getParameters().stream().map(
				(p) -> p.getSimpleName().toString()).collect(Collectors.joining(", "));
		println("\t@Override");
		println("\tpublic " + beanMethod.getReturnType() + " "
				+ beanMethod.getSimpleName() + parameters(beanMethod.getParameters())
				+ " {");
		println("\t\treturn this.beanMethods.get(\""
				+ beanMethod.getSimpleName().toString() + "\""
				+ (parameterTypes.isEmpty() ? "" : ", " + parameterTypes) + ").invoke("
				+ args + ");");
		println("\t}");
		println();
	}

	private String parameters(List<? extends VariableElement> parameters) {
		return "(" + parameters.stream().map(
				(p) -> p.asType() + " " + p.getSimpleName().toString()).collect(
						Collectors.joining(","))
				+ ")";
	}

	private void println() {
		this.out.println();
	}

	private void println(String s) {
		for (int i = 0; i < nesting; i++) {
			s = "\t" + s;
		}
		this.out.println(s);
	}

}
