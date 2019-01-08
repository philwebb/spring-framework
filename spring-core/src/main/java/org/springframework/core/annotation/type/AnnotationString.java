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

package org.springframework.core.annotation.type;

import java.util.Iterator;
import java.util.function.BiConsumer;

import org.springframework.util.ObjectUtils;

/**
 * Shared {@code toString} code for annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class AnnotationString {

	private AnnotationString() {
	}

	public static String get(AnnotationType annotationType) {
		return get(AnnotationString::appendAnnotationType, annotationType);
	}

	public static String get(AttributeTypes attributeTypes) {
		return get(AnnotationString::appendAttributeTypes, attributeTypes);
	}

	public static String get(AttributeType attributeType) {
		return get(AnnotationString::appendAttributeType, attributeType);
	}

	public static String get(DeclaredAnnotations declaredAnnotations) {
		return get(AnnotationString::appendDeclaredAnnotations, declaredAnnotations);
	}

	public static String get(DeclaredAnnotation declaredAnnotation) {
		return get(AnnotationString::appendDeclaredAnnotation, declaredAnnotation);
	}

	public static String get(DeclaredAttributes declaredAttributes) {
		return get(AnnotationString::appendDeclaredAttributes, declaredAttributes);
	}

	public static String get(DeclaredAttribute declaredAttribute) {
		return get(AnnotationString::appendDeclaredAttribute, declaredAttribute);
	}

	private static <T> String get(BiConsumer<StringBuilder, T> builder, T instance) {
		StringBuilder result = new StringBuilder();
		builder.accept(result, instance);
		return result.toString().trim();
	}

	private static void appendAnnotationType(StringBuilder result,
			AnnotationType annotationType) {
		appendDeclaredAnnotations(result, annotationType.getDeclaredAnnotations());
		result.append("@interface " + annotationType.getClassName() + " {\n");
		appendAttributeTypes(result, "\t", annotationType.getAttributeTypes());
		result.append("}");
	}

	private static void appendAttributeTypes(StringBuilder result,
			AttributeTypes attributeTypes) {
		appendAttributeTypes(result, "", attributeTypes);
	}

	private static void appendAttributeTypes(StringBuilder result, String prefix,
			AttributeTypes attributeTypes) {
		if (attributeTypes.iterator().hasNext()) {
			result.append("\n");
			for (AttributeType attributeType : attributeTypes) {
				appendAttributeType(result, prefix, attributeType);
				result.append("\n\n");
			}
		}
	}

	private static void appendAttributeType(StringBuilder result,
			AttributeType attributeType) {
		appendAttributeType(result, "", attributeType);
	}

	private static void appendAttributeType(StringBuilder result, String prefix,
			AttributeType attributeType) {
		appendDeclaredAnnotations(result, prefix, attributeType.getDeclaredAnnotations());
		Object defaultValue = attributeType.getDefaultValue();
		result.append(prefix);
		result.append(attributeType.getClassName());
		result.append(" ");
		result.append(attributeType.getAttributeName());
		result.append("()");
		if (defaultValue != null) {
			result.append(" default ");
			appendValue(result, defaultValue);
		}
		result.append(";");
	}

	private static void appendDeclaredAnnotations(StringBuilder result,
			DeclaredAnnotations declaredAnnotations) {
		appendDeclaredAnnotations(result, "", declaredAnnotations);
	}

	private static void appendDeclaredAnnotations(StringBuilder result, String prefix,
			DeclaredAnnotations declaredAnnotations) {
		for (DeclaredAnnotation declaredAnnotation : declaredAnnotations) {
			appendDeclaredAnnotation(result, prefix, declaredAnnotation);
			result.append("\n");
		}
	}

	private static void appendDeclaredAnnotation(StringBuilder result,
			DeclaredAnnotation declaredAnnotation) {
		appendDeclaredAnnotation(result, "", declaredAnnotation);
	}

	private static void appendDeclaredAnnotation(StringBuilder result, String prefix,
			DeclaredAnnotation declaredAnnotation) {
		result.append(prefix);
		result.append("@");
		result.append(declaredAnnotation.getType());
		DeclaredAttributes attributes = declaredAnnotation.getAttributes();
		if (!attributes.names().isEmpty()) {
			appendDeclaredAttributes(result, attributes);
		}
	}

	private static void appendDeclaredAttributes(StringBuilder result,
			DeclaredAttributes declaredAttributes) {
		result.append("(");
		Iterator<DeclaredAttribute> iterator = declaredAttributes.iterator();
		while (iterator.hasNext()) {
			appendDeclaredAttribute(result, iterator.next());
			result.append(iterator.hasNext() ? ", " : "");
		}
		result.append(")");
	}

	private static void appendDeclaredAttribute(StringBuilder result,
			DeclaredAttribute declaredAttribute) {
		result.append(declaredAttribute.getName());
		result.append("=");
		appendValue(result, declaredAttribute.getValue());
	}

	private static void appendValue(StringBuilder result, Object value) {
		if (ObjectUtils.isArray(value)) {
			Object[] array = ObjectUtils.toObjectArray(value);
			result.append("{");
			for (int i = 0; i < array.length; i++) {
				result.append(i > 0 ? ", " : "");
				appendValue(result, array[i]);
			}
			result.append("}");
		}
		else if (value instanceof String) {
			result.append('"');
			result.append(value);
			result.append('"');
		}
		else {
			result.append(value);
		}
	}

}
