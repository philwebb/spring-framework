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

package org.springframework.context.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.AotFactoriesLoader;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * A collection of {@link BeanFactoryInitializationAotContribution AOT contributions}
 * obtained from {@link BeanFactoryInitializationAotProcessor AOT processors}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanFactoryInitializationContributions {

	private List<BeanFactoryInitializationAotContribution> contributions;

	BeanFactoryInitializationContributions(DefaultListableBeanFactory beanFactory) {
		this(beanFactory, new AotFactoriesLoader(beanFactory));
	}

	BeanFactoryInitializationContributions(DefaultListableBeanFactory beanFactory, AotFactoriesLoader loader) {
		this.contributions = getContributions(beanFactory, getProcessors(loader));
	}

	private List<BeanFactoryInitializationAotProcessor> getProcessors(AotFactoriesLoader loader) {
		List<BeanFactoryInitializationAotProcessor> processors = new ArrayList<>(
				loader.load(BeanFactoryInitializationAotProcessor.class));
		processors.add(new RuntimeHintsBeanFactoryInitializationAotProcessor());
		return Collections.unmodifiableList(processors);
	}

	private List<BeanFactoryInitializationAotContribution> getContributions(DefaultListableBeanFactory beanFactory,
			List<BeanFactoryInitializationAotProcessor> processors) {
		List<BeanFactoryInitializationAotContribution> contributions = new ArrayList<>();
		for (BeanFactoryInitializationAotProcessor processor : processors) {
			BeanFactoryInitializationAotContribution contribution = processor.processAheadOfTime(beanFactory);
			if (contribution != null) {
				contributions.add(contribution);
			}
		}
		return Collections.unmodifiableList(contributions);
	}

	void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
		for (BeanFactoryInitializationAotContribution contribution : this.contributions) {
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
		}
	}

}