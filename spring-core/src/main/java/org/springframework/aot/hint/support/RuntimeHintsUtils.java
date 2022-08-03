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

package org.springframework.aot.hint.support;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.SynthesizedAnnotation;

/**
 * Utility methods for runtime hints support code.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 */
public abstract class RuntimeHintsUtils {

	// FIXME can we merge with new API

	/**
	 * Register the necessary hints so that the specified annotation is visible
	 * at runtime.
	 * @param hints the {@link RuntimeHints} instance to use
	 * @param annotationType the annotation type
	 * @see SynthesizedAnnotation
	 * @deprecated as annotation attributes are visible without additional hints
	 */
	@Deprecated
	public static void registerAnnotation(RuntimeHints hints, Class<?> annotationType) {
		registerSynthesizedAnnotation(hints, annotationType);
	}

	/**
	 * Register the necessary hints so that the specified annotation can be
	 * synthesized at runtime if necessary. Such hints are usually required
	 * if any of the following apply:
	 * <ul>
	 * <li>Use {@link AliasFor} for local aliases</li>
	 * <li>Has a meta-annotation that uses {@link AliasFor} for attribute overrides</li>
	 * <li>Has nested annotations or arrays of annotations that are synthesizable</li>
	 * </ul>
	 * Consider using {@link #registerAnnotationIfNecessary(RuntimeHints, MergedAnnotation)}
	 * that determines if the hints are required.
	 * @param hints the {@link RuntimeHints} instance to use
	 * @param annotationType the annotation type
	 * @see SynthesizedAnnotation
	 */
	public static void registerSynthesizedAnnotation(RuntimeHints hints, Class<?> annotationType) {
		hints.proxies().registerJavaProxy().forInterfaces(annotationType, SynthesizedAnnotation.class);
	}

	/**
	 * Determine if the specified annotation can be synthesized at runtime, and
	 * register the necessary hints accordingly.
	 * @param hints the {@link RuntimeHints} instance to use
	 * @param annotation the annotation
	 * @see #registerSynthesizedAnnotation(RuntimeHints, Class)
	 */
	public static void registerAnnotationIfNecessary(RuntimeHints hints, MergedAnnotation<?> annotation) {
		if (annotation.isSynthesizable()) {
			registerSynthesizedAnnotation(hints, annotation.getType());
		}
	}

}
