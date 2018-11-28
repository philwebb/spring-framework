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
import java.lang.annotation.Inherited;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

/**
 * A single merged annotation returned from a {@link MergedAnnotations}
 * collection.
 *
 * @author Phillip Webb
 * @since 5.1
 * @param <A> the annotation type
 * @see MergedAnnotations
 */
public interface MergedAnnotation<A extends Annotation> {

	/**
	 * Return if the annotation is present on the source. Considers
	 * {@link #isDirectlyPresent() direct annotations},
	 * {@link #isFromInherited() @Inherited annotations} and
	 * {@link #isMetaPresent() meta-annotation} annotations within the context
	 * of the {@link SearchStrategy} used.
	 * @return {@code true} if the annotation is present
	 */
	boolean isPresent();

	/**
	 * Return if the annotation is directly present on the source. A directly
	 * present annotation is one that the user has explicitly defined and not
	 * one that is {@link #isMetaPresent() meta-present} or
	 * {@link Inherited @Inherited}.
	 * @return {@code true} if the annotation is directly present
	 */
	boolean isDirectlyPresent();

	/**
	 * Return if the annotation is meta-present on the source. A meta-present
	 * annotation is an annotation that the user hasn't explicitly defined, but
	 * has been used as a meta-annotation somewhere in the annotation hierarchy.
	 * @return {@code true} if the annotation is meta-present
	 */
	boolean isMetaPresent();

	/**
	 * Return if the annotation is from an inherited source. An inherited source
	 * can be a superclass, an implemented interface or a matching method on
	 * either of those.
	 * @return {@code true} if the annotation is from an inherited source
	 */
	boolean isFromInherited();

	/**
	 * Return this annotation is a parent of the specified annotation
	 * annotation.
	 * @param annotation the annotation to check
	 * @return {@code true} if this annotation is a descendant
	 */
	boolean isParentOf(MergedAnnotation<?> annotation);
	// FIXME name of this. Needs to show it's grandparent etc


	/**
	 * Return the depth of this annotation related to its use as a
	 * meta-annotation. A directly declared annotation has a depth of {@code 0},
	 * a meta-annotation has a depth of {@code 1}, a meta-annotation on a
	 * meta-annotation has a depth of {@code 2}, etc.
	 * @return the annotation depth
	 */
	int getDepth();

	/**
	 * Return the class name of the actual annotation type.
	 * @return the annotation type
	 */
	String getType();

	/**
	 * Return if the specified attribute name as a non-default value when
	 * compared to the annotation declaration.
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is different from the default
	 * value
	 */
	boolean hasNonDefaultValue(String attributeName);

	/**
	 * Return if the specified attribute name as a default value when compared
	 * to the annotation declaration.
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is the same as the default
	 * value
	 */
	boolean hasDefaultValue(String attributeName);

	/**
	 * Return a required byte attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a byte
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte getByte(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required byte array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a byte array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte[] getByteArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required boolean attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a boolean
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean getBoolean(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required boolean array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a boolean array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean[] getBooleanArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required char attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a char
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char getChar(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required char array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a char array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char[] getCharArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required short attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a short
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short getShort(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required short array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a short array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short[] getShortArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required int attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as an int
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int getInt(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required int array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as an int array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int[] getIntArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required long attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a long
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long getLong(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required long array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a long array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long[] getLongArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required double attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a double
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double getDouble(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required double array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a double array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double[] getDoubleArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required float attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a float
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float getFloat(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required float array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a float array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float[] getFloatArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required string attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a string
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String getString(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required string array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a string array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String[] getStringArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required class attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a class
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?> getClass(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required class array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @return the value as a class array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?>[] getClassArray(String attributeName) throws NoSuchElementException;

	/**
	 * Return a required enum attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the enum type
	 * @return the value as a enum
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E getEnum(String attributeName, Class<E> type)
			throws NoSuchElementException;

	/**
	 * Return a required enum array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the enum type
	 * @return the value as a enum array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type)
			throws NoSuchElementException;

	/**
	 * Return a required annotation attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the annotation type
	 * @return the value as a {@link MergedAnnotation}
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException;

	/**
	 * Return a required annotation array attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the annotation type
	 * @return the value as a {@link MergedAnnotation} array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName,
			Class<T> type) throws NoSuchElementException;

	/**
	 * Return an optional attribute value of the specified type.
	 * @param attributeName the attribute name
	 * @param type the attribute type
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	<T> Optional<T> getAttribute(String attributeName, Class<T> type);

	/**
	 * Return an optional non-merged attribute value of the specified type. This
	 * method provides raw access to the original attribute value without
	 * applying any of the merging rules.
	 * @param attributeName the attribute name
	 * @param type the attribute type
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	<T> Optional<T> getNonMergedAttribute(String attributeName, Class<T> type);

	/**
	 * Return a new view of the annotation with all attributes that have default
	 * values removed.
	 * @return a filtered view of the annotation without any attributes that
	 * have a default value
	 * @see #filterAttributes(Predicate)
	 */
	MergedAnnotation<A> filterDefaultValues();

	/**
	 * Return a new view of the annotation with matching attributes removed.
	 * @param predicate a predicate used to filter attribute names
	 * @return a filtered view of the annotation
	 * @see #filterDefaultValues()
	 */
	MergedAnnotation<A> filterAttributes(Predicate<String> predicate);

	/**
	 * Return an immutable {@link Map} that contains all the annotation
	 * attributes. The {@link MapValues} options may be used to change the way
	 * that values are added.
	 * @param options map value options
	 * @return a map containing the attributes and values
	 */
	Map<String, Object> asMap(MapValues... options);

	/**
	 * Return a {@link Map} of the supplied type that contains all the
	 * annotation attributes. The {@link MapValues} options may be used to
	 * change the way that values are added.
	 * @param factory a map factory or null to return an immutable map
	 * @param options map value options
	 * @return a map containing the attributes and values
	 */
	<T extends Map<String, Object>> T asMap(
			@Nullable Function<MergedAnnotation<?>, T> factory, MapValues... options);

	/**
	 * Return a type-safe synthesized version of this annotation that can be
	 * used directly in code. The result is synthesized using a JDK
	 * {@link Proxy} and as a result may incur a computational cost when first
	 * invoked.
	 * @return a sythesized version of the annotation.
	 */
	A synthesize();

	/**
	 * Return an {@link MergedAnnotation} that represents a missing annotation
	 * (i.e. one that is not present).
	 * @return an instance representing a missing annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> missing() {
		return MissingMergedAnnotation.getInstance();
	}

	static <A extends Annotation> Finder<A> find(String annotationType) {
		return null;
	}

	static <A extends Annotation> Finder<A> find(Class<A> annotationType) {
		return null;
	}

	/**
	 * Return a {@link Comparator} that compares merged annotations using
	 * {@link #getDepth()}.
	 * @return a depth based comparator
	 */
	static <A extends Annotation> Comparator<? super MergedAnnotation<A>> comparingDepth() {
		return Comparator.comparingInt(MergedAnnotation::getDepth);
	}

	/**
	 * Options that effect the way map values are
	 * {@link MergedAnnotation#asMap(MapValues...) converted}.
	 */
	enum MapValues {

		/**
		 * Add class or class array attributes as strings.
		 */
		CLASS_TO_STRING,

		/**
		 * Convert any nested annotation or annotation arrays to maps rather
		 * than synthesizing the values.
		 */
		ANNOTATION_TO_MAP;

		protected boolean isIn(MapValues... options) {
			for (MapValues candidate : options) {
				if (candidate == this) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Factory method to create a {@link MapValues} array from a set of
		 * boolean flags.
		 * @param classToString if {@link MapValues#CLASS_TO_STRING} is included
		 * @param annotationsToMap if {@link MapValues#ANNOTATION_TO_MAP} is included
		 * @return a new {@link MapValues} array
		 */
		static MapValues[] get(boolean classToString, boolean annotationsToMap) {
			EnumSet<MapValues> result = EnumSet.noneOf(MapValues.class);
			if (classToString) {
				result.add(MapValues.CLASS_TO_STRING);
			}
			if (annotationsToMap) {
				result.add(MapValues.ANNOTATION_TO_MAP);
			}
			return result.toArray(new MapValues[0]);
		}

	}

	/**
	 * Finds annotations of a specific type by searching on a source.
	 *
	 * @param <A> the annotation type to find
	 */
	static interface Finder<A extends Annotation> {

		// Map<Method, MergedAnnotation<A>> fromMethods(Class<?> type);

		// Map<Method, MergedAnnotation<A>> fromLocalMethods(Class<?> type);

		// FIXME will return something that allows access to the method and MergedAnnotation
		// something a bit like a Map<Method, MergedAnnotation<T>> but perhaps not exactly that

	}

}
