package org.springframework.javapoet.test;

import java.util.Objects;

/**
 * A class name that can be used for generated source code.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @see GenerationContext#generateClassName
 */
public final class GeneratedClassName {

	private final Class<?> target;

	private final String packageName;

	private final String shortName;

	private final boolean owned;

	private final String fullyQualifiedName;

	GeneratedClassName(Class<?> target, String packageName, String shortName, boolean owned) {
		this.target = target;
		this.packageName = packageName;
		this.shortName = shortName;
		this.owned = owned;
		this.fullyQualifiedName = this.packageName + "." + this.shortName;
	}

	/**
	 * Return the target class that was used to generate the name.
	 * @return the target class
	 */
	public Class<?> getTarget() {
		return this.target;
	}

	/**
	 * Return the package name for this instance. If the target is {{@link #isOwned()
	 * owned} then the package name will be taken from target class.
	 * @return the package name
	 */
	public String getPackageName() {
		return this.packageName;
	}

	/**
	 * Return the short name class (the name without the qualified package name) for this
	 * instance.
	 * @return the short name
	 */
	public String getShortName() {
		return this.shortName;
	}

	/**
	 * Return if the generated name is owned. Names that are owned are in the same package
	 * as the {@code target} and can call package-private methods.
	 * @return if the name is owned
	 */
	public boolean isOwned() {
		return this.owned;
	}

	/**
	 * Return the fully qualified name based on the package, short name, feature and
	 * sequence.
	 * @return the fully qualified name
	 */
	public String getFullyQualifiedName() {
		return this.fullyQualifiedName;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		GeneratedClassName other = (GeneratedClassName) obj;
		boolean result = true;
		result = result && this.owned == other.owned;
		result = result && Objects.equals(this.packageName, other.packageName);
		result = result && Objects.equals(this.shortName, other.shortName);
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.owned, this.packageName, this.shortName);
	}

	@Override
	public String toString() {
		return this.fullyQualifiedName;
	}

}
