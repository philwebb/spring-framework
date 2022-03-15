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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link InstanceCodeGenerationService} backed by a
 * collection of {@link InstanceCodeGenerator} instances.
 * <p>
 * By default, the following instance value types are supported:
 * <ul>
 * <li>{@link Character}</li>
 * <li>Primitives ({@link Boolean}, {@link Byte}, {@link Short},
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
 * Set the {@code addDefaultGenerators} constructor parameter to {@code false}
 * if these are not required. Additional {@link InstanceCodeGenerator}
 * implementations can be {@link #add(InstanceCodeGenerator) added} to support
 * other types.
 * <p>
 * If no additional {@link InstanceCodeGenerator} implementations are needed
 * then the {@link InstanceCodeGenerationService#getSharedInstance()} may be
 * used.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DefaultInstanceCodeGenerationService
		implements InstanceCodeGenerationService {

	private static final DefaultInstanceCodeGenerationService SHARED_INSTANCE = new DefaultInstanceCodeGenerationService(
			null, true, true);

	static final CodeBlock NULL_INSTANCE_CODE_BLOCK = CodeBlock.of("null");

	@Nullable
	private final GeneratedMethods generatedMethods;

	private final boolean sharedInstance;

	private final List<InstanceCodeGenerator> generators = new ArrayList<>();

	public DefaultInstanceCodeGenerationService() {
		this(null, true);
	}

	public DefaultInstanceCodeGenerationService(
			@Nullable GeneratedMethods generatedMethods) {
		this(generatedMethods, true);
	}

	public DefaultInstanceCodeGenerationService(
			@Nullable GeneratedMethods generatedMethods, boolean addDefaultGenerators) {
		this(generatedMethods, addDefaultGenerators, false);
	}

	private DefaultInstanceCodeGenerationService(
			@Nullable GeneratedMethods generatedMethods, boolean addDefaultGenerators,
			boolean sharedInstance) {
		this.generatedMethods = generatedMethods;
		this.sharedInstance = sharedInstance;
		if (addDefaultGenerators) {
			this.generators.add(CharacterInstanceCodeGenerator.INSTANCE);
			this.generators.add(PrimitiveInstanceCodeGenerator.INSTANCE);
			this.generators.add(StringInstanceCodeGenerator.INSTANCE);
			this.generators.add(EnumInstanceCodeGenerator.INSTANCE);
			this.generators.add(ClassInstanceCodeGenerator.INSTANCE);
			this.generators.add(ResolvableTypeInstanceCodeGenerator.INSTANCE);
			this.generators.add(new ArrayInstanceCodeGenerator(this));
			this.generators.add(new ListInstanceCodeGenerator(this));
			this.generators.add(new SetInstanceCodeGenerator(this));
			this.generators.add(new MapInstanceCodeGenerator(this));
		}
	}

	/**
	 * Return a shared instance of {@link DefaultInstanceCodeGenerationService}
	 * that can be used when no addition {@link InstanceCodeGenerator
	 * InstanceCodeGenerators} are required.
	 * @return the shared {@link DefaultInstanceCodeGenerationService} instance
	 */
	static DefaultInstanceCodeGenerationService getSharedInstance() {
		return SHARED_INSTANCE;
	}

	/**
	 * Add a new {@link InstanceCodeGenerator} to this service. Added generators
	 * take precedence over existing generators.
	 * @param generator the generator to add
	 */
	public void add(InstanceCodeGenerator generator) {
		Assert.state(!this.sharedInstance,
				"'DefaultInstanceCodeGenerationService.sharedInstance()' cannot be modified");
		Assert.notNull(generator, "'generator' must not be null");
		this.generators.add(0, generator);
	}

	@Override
	public boolean supportsGeneratedMethods() {
		return this.generatedMethods != null;
	}

	@Override
	public GeneratedMethods getGeneratedMethods() {
		Assert.state(this.generatedMethods != null,
				"No GeneratedMethods instance available");
		return this.generatedMethods;
	}

	@Override
	public CodeBlock generateCode(@Nullable String name, @Nullable Object value,
			ResolvableType type) {
		if (value == null) {
			return NULL_INSTANCE_CODE_BLOCK;
		}
		for (InstanceCodeGenerator generator : this.generators) {
			CodeBlock code = generator.generateCode(name, value, type);
			if (code != null) {
				return code;
			}
		}
		throw new IllegalArgumentException(
				"'type' " + type + " must be supported for instance code generation");
	}

}
