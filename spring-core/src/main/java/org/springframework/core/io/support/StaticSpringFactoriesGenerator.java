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

package org.springframework.core.io.support;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.AccessVisibility;
import org.springframework.aot.generate.ClassGenerator;
import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.core.log.LogMessage;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;


/**
 * Generate code that programmatically creates and registers {@link SpringFactoriesLoader}
 * instances in the static cache.
 *
 * @author Brian Clozel
 * @since 6.0
 * @see SpringFactoriesLoader#staticFactories(ClassLoader, String)
 */
public class StaticSpringFactoriesGenerator {

	private static final Log logger = LogFactory.getLog(StaticSpringFactoriesGenerator.class);


	private final SpringFactoriesJavaFileGenerator javaFileGenerator;

	private final ClassLoader classLoader;

	private final String resourceLocation;

	/**
	 * Create an instance that uses the given classLoader to load
	 * {@link SpringFactoriesLoader#FACTORIES_RESOURCE_LOCATION} resources and factory classes
	 * during the generation process.
	 *
	 * @param classLoader the class loader to use
	 */
	public StaticSpringFactoriesGenerator(@Nullable ClassLoader classLoader) {
		this(classLoader, SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION);
	}

	/**
	 * Create an instance that uses the given classLoader to load
	 * resources from given location and factory classes during the generation process.
	 *
	 * @param classLoader the class loader to use
	 * @param resourceLocation the resource location to consider for factories files
	 */
	public StaticSpringFactoriesGenerator(@Nullable ClassLoader classLoader, String resourceLocation) {
		this.javaFileGenerator = new SpringFactoriesJavaFileGenerator();
		this.classLoader = (classLoader != null) ? classLoader : SpringFactoriesLoader.class.getClassLoader();
		this.resourceLocation = resourceLocation;
	}

	/**
	 * Contribute generated code to the {@link GenerationContext context} that programmatically
	 * registers a {@link SpringFactoriesLoader} and caches it for later use. The actual registration
	 * only happens when the {@link MethodReference method} returned by this method is executed.
	 * @param generationContext the generation context to use
	 * @return a {@link MethodReference} to a method that needs to be executed before any {@link SpringFactoriesLoader} call is made.
	 */
	public MethodReference generateStaticSpringFactories(GenerationContext generationContext) {
		SpringFactoriesLoader springFactoriesLoader = SpringFactoriesLoader.forResourceLocation(this.classLoader, this.resourceLocation);
		Map<String, List<String>> factories = springFactoriesLoader.loadFactoriesResource(this.classLoader, this.resourceLocation);

		GeneratedClass staticSpringFactoriesClass = generationContext.getClassGenerator()
				.getOrGenerateClass(this.javaFileGenerator, SpringFactoriesLoader.class, "SpringFactories");

		GeneratedMethod initSpringFactoriesMethod = staticSpringFactoriesClass
				.getMethodGenerator()
				.generateMethod("init", "spring", "factories")
				.using(builder -> {
					builder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
					builder.addJavadoc("Initialize {@link $T}", SpringFactoriesLoader.class);
					builder.addParameter(ClassLoader.class, "classLoader");
					builder.addCode(generateSpringFactoriesInitialization(generationContext, factories));
				});
		return MethodReference.ofStatic(staticSpringFactoriesClass.getName(), initSpringFactoriesMethod.getName());
	}

	private CodeBlock generateSpringFactoriesInitialization(GenerationContext generationContext, Map<String, List<String>> factories) {
		CodeBlock.Builder initSpringFactoriesCode = CodeBlock.builder()
				.add("$T.staticFactories($L, $S)\n", SpringFactoriesLoader.class, "classLoader", this.resourceLocation)
				.indent();
		for (String factoryType : factories.keySet()) {
			logger.trace(LogMessage.format("Processing factories for [%s]", factoryType));
			for (String factoryImplementation : factories.get(factoryType)) {
				logger.trace(LogMessage.format("Processing factory type [%s] and implementation [%s]", factoryType, factoryImplementation));
				try {
					SpringFactoriesEntry factoryEntry = resolveSpringFactoryEntry(factoryType, factoryImplementation);
					Constructor<?> factoryImplementationConstructor = factoryEntry.findConstructor();
					if (!isPublic(factoryImplementationConstructor)) {
						GeneratedClass generatedClass = generationContext.getClassGenerator()
								.getOrGenerateClass(this.javaFileGenerator, factoryEntry.implementationClass, "SpringFactories");
						GeneratedMethod createMethod = generateCreateMethodForProtectedFactory(factoryEntry, factoryImplementationConstructor, generatedClass);
						initSpringFactoriesCode.add(".addFactory($T.class, $S, $T::$N)\n", factoryEntry.typeClass,
								factoryEntry.implementationClass.getCanonicalName(), generatedClass.getName(), createMethod.getSpec());
					}
					else {
						if (factoryImplementationConstructor.getParameterCount() == 0) {
							initSpringFactoriesCode.add(".addFactory($T.class, $S, $T::new)\n", factoryEntry.typeClass,
									factoryEntry.implementationClass.getCanonicalName(), factoryEntry.implementationClass);
						}
						else {
							initSpringFactoriesCode.add(".addFactory($T.class, $S, ", factoryEntry.typeClass, factoryEntry.implementationClass.getCanonicalName())
									.add("(argumentResolver) -> ")
									.add(instantiateFactory(factoryEntry.implementationClass, factoryImplementationConstructor))
									.add(")\n");
						}
					}
				}
				catch (ClassNotFoundException|NoClassDefFoundError exc) {
					logger.trace(LogMessage.format("Could not process factory [%s] for type [%s]", factoryImplementation, factoryType), exc);
				}
			}
		}
		return initSpringFactoriesCode.add(".register();").unindent().build();
	}

	private SpringFactoriesEntry resolveSpringFactoryEntry(String factoryType, String factoryImplementation) throws ClassNotFoundException {
		Class<?> factoryTypeClass = ClassUtils.forName(factoryType, this.classLoader);
		Class<?> factoryImplementationClass = ClassUtils.forName(factoryImplementation, this.classLoader);
		return new SpringFactoriesEntry(factoryTypeClass, factoryImplementationClass);
	}

	private GeneratedMethod generateCreateMethodForProtectedFactory(SpringFactoriesEntry springFactoriesEntry, Constructor<?> constructor, GeneratedClass generatedClass) {
		return generatedClass.getMethodGenerator()
				.generateMethod("create", springFactoriesEntry.implementationClass.getSimpleName())
				.using(builder -> {
					CodeBlock methodBody = CodeBlock.builder().add("return ")
							.addStatement(instantiateFactory(springFactoriesEntry.implementationClass, constructor))
							.build();
					builder.addModifiers(Modifier.STATIC, Modifier.PUBLIC)
							.addParameter(SpringFactoriesLoader.ArgumentResolver.class, "argumentResolver")
							.returns(springFactoriesEntry.typeClass)
							.addCode(methodBody);
				});
	}

	private CodeBlock instantiateFactory(Class<?> factoryImplementationClass, Constructor<?> constructor) {
		return CodeBlock.builder().add("new $T(", factoryImplementationClass)
				.add(CodeBlock.join(resolveArguments(constructor), ", "))
				.add(")").build();
	}

	private List<CodeBlock> resolveArguments(Constructor<?> constructor) {
		List<CodeBlock> parameterValues = new ArrayList<>();
		for (Class<?> parameterType : constructor.getParameterTypes()) {
			parameterValues.add(CodeBlock.of("argumentResolver.resolve($T.class)", parameterType));
		}
		return parameterValues;
	}

	private boolean isPublic(Constructor<?> constructor) {
		return AccessVisibility.forClass(constructor.getDeclaringClass()) == AccessVisibility.PUBLIC
				&& AccessVisibility.forMember(constructor) == AccessVisibility.PUBLIC;
	}

	/**
	 * Record representing a parsed entry for a Spring Factory.
	 *
	 * @param typeClass the factory type class
	 * @param implementationClass the factory implementation class
	 */
	private record SpringFactoriesEntry(Class<?> typeClass, Class<?> implementationClass) {

		Constructor<?> findConstructor() {
			Constructor<?> constructor = SpringFactoriesLoader.FactoryInstantiator.findConstructor(this.implementationClass);
			Assert.state(constructor != null, "Class [" + this.implementationClass.getName() + "] has no suitable constructor");
			return constructor;
		}
	}

	/**
	 * {@link SpringFactoriesJavaFileGenerator} to create file for the {@code SpringFactoriesLoader}
	 * initialization and the files for per-package factory methods for.
	 */
	private static class SpringFactoriesJavaFileGenerator implements ClassGenerator.JavaFileGenerator {

		@Override
		public JavaFile generateJavaFile(ClassName className, GeneratedMethods methods) {
			TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
			if (SpringFactoriesLoader.class.getPackageName().equals(className.packageName())) {
				classBuilder.addJavadoc("{@link $T} initialization", SpringFactoriesLoader.class);
			}
			else {
				classBuilder.addJavadoc("{@link $T} factory methods for factories located in {@code $S}",
						SpringFactoriesLoader.class, className.packageName());
			}
			classBuilder.addModifiers(Modifier.PUBLIC);
			methods.doWithMethodSpecs(classBuilder::addMethod);
			return JavaFile.builder(className.packageName(), classBuilder.build())
					.build();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return getClass() == obj.getClass();
		}

	}

}
