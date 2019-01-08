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

import java.util.function.Consumer;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;

/**
 * ASM {@link AnnotationVisitor} that can be used to create a
 * {@link DeclaredAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class DeclaredAnnotationAnnotationVisitor extends AnnotationVisitor {

	DeclaredAnnotationAnnotationVisitor(String type,
			Consumer<DeclaredAnnotation> consumer) {
		super(SpringAsmInfo.ASM_VERSION,
				new DeclaredAttributesAnnotationVisitor(attributes -> consumer.accept(
						new SimpleDeclaredAnnotation(type, attributes))));
	}

	public static DeclaredAnnotationAnnotationVisitor forTypeDescriptor(
			String typeDescriptor, Consumer<DeclaredAnnotation> consumer) {
		String type = Type.getType(typeDescriptor).getClassName();
		return new DeclaredAnnotationAnnotationVisitor(type, consumer);
	}

}
