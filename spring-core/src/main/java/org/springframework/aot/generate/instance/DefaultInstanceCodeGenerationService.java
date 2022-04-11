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

package org.springframework.aot.generate.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.aot.generate.MethodGenerator;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link InstanceCodeGenerationService} backed by a collection
 * of {@link InstanceCodeGenerator} instances.
 * <p>
 * By default, the following instance value types are supported:
 * <ul>
 * <li>{@link Character}</li>
 * <li>Primitives and primitive wrappers ({@link Boolean}, {@link Byte}, {@link Short},
 * {@link Integer}, {@link Long})</li>
 * <li>{@link String}</li>
 * <li>{@link Enum}</li>
 * <li>{@link Class}</li>
 * <li>{@link ResolvableType}</li>
 * <li>All Array Types</li>
 * <li>{@link Set}</li>
 * <li>{@link List}</li>
 * <li>{@link Map}</li>
 * </ul>
 * <p>
 * If these are not required, or if additional {@link InstanceCodeGenerator}
 * implementations are needed use an appropriate constructor to configure the
 * {@link InstanceCodeGenerators}.
 * <p>
 * If no additional {@link InstanceCodeGenerator} implementations are needed then the
 * {@link InstanceCodeGenerationService#getSharedInstance()} may be used.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DefaultInstanceCodeGenerationService implements InstanceCodeGenerationService {

	private static final DefaultInstanceCodeGenerationService SHARED_INSTANCE = new DefaultInstanceCodeGenerationService();

	static final CodeBlock NULL_INSTANCE_CODE_BLOCK = CodeBlock.of("null");

	@Nullable
	private final DefaultInstanceCodeGenerationService parent;

	@Nullable
	private final MethodGenerator methodGenerator;

	private final List<InstanceCodeGenerator> instanceCodeGenerators;

	public DefaultInstanceCodeGenerationService() {
		this(null, null, InstanceCodeGenerators::onlyDefaults);
	}

	public DefaultInstanceCodeGenerationService(@Nullable DefaultInstanceCodeGenerationService parent) {
		this(parent, null, InstanceCodeGenerators::onlyDefaults);
	}

	public DefaultInstanceCodeGenerationService(@Nullable MethodGenerator methodGenerator) {
		this(null, methodGenerator, InstanceCodeGenerators::onlyDefaults);
	}

	public DefaultInstanceCodeGenerationService(Consumer<InstanceCodeGenerators> instanceCodeGenerators) {
		this(null, null, instanceCodeGenerators);
	}

	public DefaultInstanceCodeGenerationService(@Nullable DefaultInstanceCodeGenerationService parent,
			@Nullable MethodGenerator methodGenerator, Consumer<InstanceCodeGenerators> instanceCodeGenerators) {
		Assert.notNull(instanceCodeGenerators, "'instanceCodeGenerators' must not be null");
		this.parent = parent;
		this.methodGenerator = (methodGenerator != null) ? methodGenerator : getGeneratedMethodsFromParent(parent);
		this.instanceCodeGenerators = InstanceCodeGenerators.get(instanceCodeGenerators);
	}

	@Nullable
	private MethodGenerator getGeneratedMethodsFromParent(InstanceCodeGenerationService parent) {
		return (parent != null && parent.supportsMethodGeneration()) ? parent.getMethodGenerator() : null;
	}

	/**
	 * Return a shared instance of {@link DefaultInstanceCodeGenerationService} that can
	 * be used when no addition {@link InstanceCodeGenerator InstanceCodeGenerators} are
	 * required.
	 * @return the shared {@link DefaultInstanceCodeGenerationService} instance
	 */
	static DefaultInstanceCodeGenerationService getSharedInstance() {
		return SHARED_INSTANCE;
	}

	@Override
	public boolean supportsMethodGeneration() {
		return this.methodGenerator != null;
	}

	@Override
	public MethodGenerator getMethodGenerator() {
		Assert.state(this.methodGenerator != null, "No MethodGenerator available");
		return this.methodGenerator;
	}

	@Override
	public CodeBlock generateCode(@Nullable Object value, ResolvableType type) {
		if (value == null) {
			return NULL_INSTANCE_CODE_BLOCK;
		}
		DefaultInstanceCodeGenerationService service = this;
		while (service != null) {
			for (InstanceCodeGenerator generator : service.instanceCodeGenerators) {
				CodeBlock code = generator.generateCode(value, type, service);
				if (code != null) {
					return code;
				}
			}
			service = service.parent;
		}
		throw new IllegalArgumentException("'type' " + type + " must be supported for instance code generation");
	}

	@Override
	public Iterator<InstanceCodeGenerator> iterator() {
		return Collections.unmodifiableList(this.instanceCodeGenerators).iterator();
	}

	/**
	 * Manages the {@link InstanceCodeGenerator} instances used by the service.
	 */
	public static class InstanceCodeGenerators {

		private final List<InstanceCodeGenerator> generators = new ArrayList<>();

		InstanceCodeGenerators() {
		}

		/**
		 * Adds the default generators.
		 */
		public void addDefaults() {
			add(PrimitiveInstanceCodeGenerator.INSTANCE);
			add(StringInstanceCodeGenerator.INSTANCE);
			add(EnumInstanceCodeGenerator.INSTANCE);
			add(ClassInstanceCodeGenerator.INSTANCE);
			add(ResolvableTypeInstanceCodeGenerator.INSTANCE);
			add(ArrayInstanceCodeGenerator.INSTANCE);
			add(ListInstanceCodeGenerator.INSTANCE);
			add(SetInstanceCodeGenerator.INSTANCE);
			add(MapInstanceCodeGenerator.INSTANCE);
		}

		/**
		 * Adds a specific generator.
		 * @param instanceCodeGenerator the generator to add
		 */
		public void add(InstanceCodeGenerator instanceCodeGenerator) {
			Assert.notNull(instanceCodeGenerator, "'instanceCodeGenerator' must not be null");
			this.generators.add(instanceCodeGenerator);
		}

		static List<InstanceCodeGenerator> get(Consumer<InstanceCodeGenerators> customizer) {
			InstanceCodeGenerators instanceCodeGenerators = new InstanceCodeGenerators();
			customizer.accept(instanceCodeGenerators);
			return Collections.unmodifiableList(instanceCodeGenerators.generators);
		}

		public static void onlyDefaults(InstanceCodeGenerators generators) {
			generators.addDefaults();
		}

		public static void none(InstanceCodeGenerators generators) {
		}

	}

}
