/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.StringUtils;

/**
 * Holds the values extracted from {@code @AliasFor} and checks that they are
 * valid.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationTypeMappings
 */
class AliasForDescriptor {

	private final String annotation;

	private final String attribute;

	AliasForDescriptor(String sourceAnnotation, String sourceAttribute,
			DeclaredAnnotation aliasFor) {
		Reference source = new Reference(sourceAnnotation, sourceAttribute);
		this.annotation = deduceAnnotation(source, aliasFor);
		this.attribute = deduceAttribute(source, aliasFor);
		validateNotSelfPointing(source);
	}

	private String deduceAnnotation(Reference source, DeclaredAnnotation aliasFor) {
		DeclaredAttributes attributes = aliasFor.getAttributes();
		ClassReference target = (ClassReference) attributes.get("annotation");
		if (target == null || target.getClassName().equals(Annotation.class.getName())) {
			return source.getAnnotation();
		}
		return target.getClassName();
	}

	private String deduceAttribute(Reference source, DeclaredAnnotation annotation) {
		DeclaredAttributes attributes = annotation.getAttributes();
		String target = (String) attributes.get("attribute");
		String targetAlias = (String) attributes.get("value");
		validateOnlyOne(source, targetAlias, target);
		target = (StringUtils.hasText(target) ? target : targetAlias);
		if (StringUtils.hasText(target)) {
			return target.trim();
		}
		return source.getAttribute();
	}

	private void validateNotSelfPointing(Reference source) {
		if (Objects.equals(this.annotation, source.getAnnotation())
				&& Objects.equals(this.attribute, source.getAttribute())) {
			throw new AnnotationConfigurationException(String.format(
					"@AliasFor declaration on %s points to itself. Specify 'annotation' "
							+ "to point to a same-named attribute on a meta-annotation.",
					source));
		}
	}

	private void validateOnlyOne(Reference source, String target, String targetAlias) {
		if (StringUtils.hasText(target) && StringUtils.hasText(targetAlias)) {
			throw new AnnotationConfigurationException(String.format(
					"In @AliasFor declared on %s, attribute 'attribute' and its alias "
							+ "'value' are present with values of '%s' and '%s', but "
							+ "only one is permitted.",
					source, target, targetAlias));
		}
	}

	public String getAnnotation() {
		return this.annotation;
	}

	public String getAttribute() {
		return this.attribute;
	}

	@Override
	public String toString() {
		return Reference.toString(this.attribute, this.annotation);
	}

	/**
	 * An annotation/attribute reference pair.
	 */
	private static class Reference {

		private final String annotation;

		private final String attribute;

		public Reference(String annotation, String attribute) {
			this.annotation = annotation;
			this.attribute = attribute;
		}

		public String getAnnotation() {
			return this.annotation;
		}

		public String getAttribute() {
			return this.attribute;
		}

		@Override
		public String toString() {
			return toString(this.attribute, this.annotation);
		}

		static String toString(String attribute, String annotation) {
			return String.format("attribute '%s' in annotation [%s]", attribute,
					annotation);
		}

	}

}
