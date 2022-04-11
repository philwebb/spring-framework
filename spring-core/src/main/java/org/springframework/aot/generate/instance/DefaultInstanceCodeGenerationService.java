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
 * Set the {@code addDefaultGenerators} constructor parameter to {@code false} if these
 * are not required. Additional {@link InstanceCodeGenerator} implementations can be
 * {@link #add(InstanceCodeGenerator) added} to support other types.
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

	private static final DefaultInstanceCodeGenerationService SHARED_INSTANCE = new DefaultInstanceCodeGenerationService(
			null, null, true, true);

	static final CodeBlock NULL_INSTANCE_CODE_BLOCK = CodeBlock.of("null");

	@Nullable
	private final DefaultInstanceCodeGenerationService parent;

	@Nullable
	private final MethodGenerator methodGenerator;

	private final boolean sharedInstance;

	private final List<InstanceCodeGenerator> generators = new ArrayList<>();

	/**
	 * Crate a new {@link DefaultInstanceCodeGenerationService} with a default set of
	 * generators.
	 */
	public DefaultInstanceCodeGenerationService() {
		this(null, null, true, false);
	}

	/**
	 * Create a new {@link DefaultInstanceCodeGenerationService} with a parent
	 * {@link InstanceCodeGenerationService}.
	 * @param parent the parent {@link InstanceCodeGenerationService}
	 */
	public DefaultInstanceCodeGenerationService(DefaultInstanceCodeGenerationService parent) {
		this(parent, null, false, false);
	}

	/**
	 * Create a new {@link DefaultInstanceCodeGenerationService} with the specified
	 * {@link MethodGenerator} and a default set of instance code generators.
	 * @param methodGenerator the method generator instance to use
	 */
	public DefaultInstanceCodeGenerationService(@Nullable MethodGenerator methodGenerator) {
		this(null, methodGenerator, true, false);
	}

	/**
	 * Create a new {@link DefaultInstanceCodeGenerationService} with the specified
	 * {@link MethodGenerator} and default or empty set of instance code generators.
	 * @param methodGenerator the method generator instance to use
	 * @param addDefaultGenerators if default generates should be added
	 */
	public DefaultInstanceCodeGenerationService(@Nullable MethodGenerator methodGenerator,
			boolean addDefaultGenerators) {
		this(null, methodGenerator, addDefaultGenerators, false);
	}

	private DefaultInstanceCodeGenerationService(@Nullable DefaultInstanceCodeGenerationService parent,
			@Nullable MethodGenerator methodGenerator, boolean addDefaultGenerators, boolean sharedInstance) {
		this.parent = parent;
		this.methodGenerator = (methodGenerator != null) ? methodGenerator : getGeneratedMethodsFromParent(parent);
		this.sharedInstance = sharedInstance;
		if (addDefaultGenerators) {
			this.generators.add(CharacterInstanceCodeGenerator.INSTANCE);
			this.generators.add(PrimitiveInstanceCodeGenerator.INSTANCE);
			this.generators.add(StringInstanceCodeGenerator.INSTANCE);
			this.generators.add(EnumInstanceCodeGenerator.INSTANCE);
			this.generators.add(ClassInstanceCodeGenerator.INSTANCE);
			this.generators.add(ResolvableTypeInstanceCodeGenerator.INSTANCE);
			this.generators.add(ArrayInstanceCodeGenerator.INSTANCE);
			this.generators.add(ListInstanceCodeGenerator.INSTANCE);
			this.generators.add(SetInstanceCodeGenerator.INSTANCE);
			this.generators.add(MapInstanceCodeGenerator.INSTANCE);
		}
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

	/**
	 * Add a new {@link InstanceCodeGenerator} to this service. Added generators take
	 * precedence over existing generators.
	 * @param generator the generator to add
	 */
	public void add(InstanceCodeGenerator generator) {
		Assert.state(!this.sharedInstance,
				"'DefaultInstanceCodeGenerationService.sharedInstance()' cannot be modified");
		Assert.notNull(generator, "'generator' must not be null");
		this.generators.add(0, generator);
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
			for (InstanceCodeGenerator generator : service.generators) {
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
		return Collections.unmodifiableList(this.generators).iterator();
	}
}
