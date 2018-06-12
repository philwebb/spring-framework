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

package org.springframework.core.type.classreading;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link AnnotationMetadata} returned from a {@link SimpleMetadataReader}.
 *
 * @author Phillip Webb
 * @since 5.1
 */
public final class SimpleAnnotationMetadata extends SimpleAnnotatedTypeMetadata implements AnnotationMetadata {

	private static final int VERSION = 0;

	private final String className;

	private final EnumSet<Flag> flags;

	@Nullable
	private final String enclosingClassName;

	@Nullable
	private final String superClassName;

	private final String[] interfaceNames;

	private final String[] memberClassNames;

	private final Set<String> annotationTypes;

	private final Set<SimpleMethodMetadata> methodMetadata;


	SimpleAnnotationMetadata(ClassLoader classLoader,
			LinkedMultiValueMap<String, AnnotationAttributes> annotationAttributes,
			Map<String, Set<String>> metaAnnotations, String className,
			EnumSet<Flag> flags, String enclosingClassName, String superClassName,
			String[] interfaceNames, String[] memberClassNames,
			Set<String> annotationTypes, Set<SimpleMethodMetadata> methodMetadata) {
		super(classLoader, annotationAttributes, metaAnnotations);
		this.className = className;
		this.flags = flags;
		this.enclosingClassName = enclosingClassName;
		this.superClassName = superClassName;
		this.interfaceNames = interfaceNames;
		this.memberClassNames = memberClassNames;
		this.annotationTypes = annotationTypes;
		this.methodMetadata = methodMetadata;
	}


	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean isInterface() {
		return this.flags.contains(Flag.INTERFACE);
	}

	@Override
	public boolean isAnnotation() {
		return this.flags.contains(Flag.ANNOTATION);
	}

	@Override
	public boolean isAbstract() {
		return this.flags.contains(Flag.ABSTRACT);

	}

	@Override
	public boolean isFinal() {
		return this.flags.contains(Flag.FINAL);
	}

	@Override
	public boolean isIndependent() {
		return (getEnclosingClassName() == null || this.flags.contains(Flag.INDEPENDENT_INNER_CLASS));
	}

	@Override
	@Nullable
	public String getEnclosingClassName() {
		return this.enclosingClassName;
	}

	@Override
	@Nullable
	public String getSuperClassName() {
		return this.superClassName;
	}

	@Override
	public String[] getInterfaceNames() {
		return this.interfaceNames;
	}

	@Override
	public String[] getMemberClassNames() {
		return this.memberClassNames;
	}


	@Override
	public Set<String> getAnnotationTypes() {
		return this.annotationTypes;
	}

	@Override
	public Set<String> getMetaAnnotationTypes(String annotationName) {
		return super.getMetaAnnotationTypes(annotationName);
	}

	@Override
	public boolean hasMetaAnnotation(String metaAnnotationType) {
		return super.hasMetaAnnotation(metaAnnotationType);
	}

	@Override
	public boolean hasAnnotatedMethods(String annotationName) {
		for (MethodMetadata methodMetadata : this.methodMetadata) {
			if (methodMetadata.isAnnotated(annotationName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<>(4);
		for (MethodMetadata methodMetadata : this.methodMetadata) {
			if (methodMetadata.isAnnotated(annotationName)) {
				annotatedMethods.add(methodMetadata);
			}
		}
		return annotatedMethods;
	}

	@Override
	protected String getDescription() {
		return "class '" + this.className + "'";
	}

	/**
	 * Save the meta-data contents to an output stream.
	 * @param outputStream the destination output stream
	 * @throws IOException on IO error
	 * @see #load(InputStream)
	 */
	public void save(OutputStream outputStream) throws IOException {
		DataOutputStream data = new DataOutputStream(outputStream);
		data.writeByte(VERSION);
		data.writeUTF(this.className);
		saveEnumSet(data, this.flags);
		saveNullableString(data, this.enclosingClassName);
		saveNullableString(data, this.superClassName);
		saveStringArray(data, this.interfaceNames);
		saveStringArray(data, this.memberClassNames);
		saveStringArray(data, StringUtils.toStringArray(this.annotationTypes));
		//saveAnnotationAttributes(getDirectAnnotationAttributes());
		data.writeInt(this.methodMetadata.size());
		for (SimpleMethodMetadata methodMetadata : this.methodMetadata) {
			data.writeUTF(methodMetadata.getMethodName());
			saveEnumSet(data, methodMetadata.getFlags());
			data.writeUTF(methodMetadata.getReturnTypeName());
		//	methodMetadata.
		}
	}

	private <E extends Enum<E>> void saveEnumSet(DataOutputStream data,
			Set<E> enumSet) throws IOException {
		int bitVector = 0;
		for (E element : enumSet) {
			bitVector |= (1L << element.ordinal());
		}
		data.writeInt(bitVector);
	}


	private void saveNullableString(DataOutput data, String string) throws IOException {
		data.writeBoolean(string != null);
		if(string != null) {
			data.writeUTF(string);
		}
	}

	private void saveStringArray(DataOutput data, String[] elements) throws IOException {
		data.writeInt(elements.length);
		for (String element : elements) {
			data.writeUTF(element);
		}
	}

	private void saveAnnotationAttributes(DataOutput data,
			LinkedMultiValueMap<String, AnnotationAttributes> attributes) throws IOException {
		data.writeInt(attributes.size());
		for (Map.Entry<String, List<AnnotationAttributes>> entry : attributes.entrySet()) {
			data.writeUTF(entry.getKey());
		}
	}


	/**
	 * Load previously {@link #save(OutputStream) saved} meta-data from a source input
	 * stream.
	 * @param inputStream the source input stream
	 * @return a new {@link SimpleAnnotationMetadata} instance
	 * @throws IOException on IO error
	 * @see #save(OutputStream)
	 */
	public static SimpleAnnotationMetadata load(ClassLoader classLoader, InputStream inputStream) throws IOException {
		DataInputStream data = new DataInputStream(inputStream);
		byte version = data.readByte();
		if (version != VERSION) {
			throw new IOException("Unsupported version " + version);
		}
		String className = data.readUTF();
		EnumSet<Flag> flags = loadEnumSet(data, Flag.class);
		String enclosingClassName = loadNullableString(data);
		String superClassName = loadNullableString(data);
		String[] interfaceNames = loadStringArray(data);
		String[] memberClassNames = loadStringArray(data);
		Set<String> annotationTypes = new LinkedHashSet<>(Arrays.asList(loadStringArray(data)));
		int methodMetadataSize = data.readInt();
		Set<SimpleMethodMetadata> methodMetadata = new LinkedHashSet<>(methodMetadataSize);
		for(int i=0; i<methodMetadataSize; i++) {
			String methodName = data.readUTF();
			Set<SimpleMethodMetadata.Flag> methodFlags = loadEnumSet(data,
					SimpleMethodMetadata.Flag.class);
			String returnTypeName = data.readUTF();
			//methodMetadata.add(new SimpleMethodMetadata(classLoader, annotationAttributes, metaAnnotations, methodName, flags, declaringClassName, returnTypeName));
		}
		return null;
	}

	/**
	 * @param data
	 * @return
	 */
	private static String[] loadStringArray(DataInputStream data) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}


	/**
	 * @param data
	 * @return
	 */
	private static String loadNullableString(DataInputStream data) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}


	private static <E extends Enum<E>> EnumSet<E> loadEnumSet(DataInput data, Class<E> type) {
		return null;
	}

	public enum Flag {

		INTERFACE, ANNOTATION, ABSTRACT, FINAL, INDEPENDENT_INNER_CLASS

	}

}
