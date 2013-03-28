/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * Internal marker for a null singleton object:
	 * used as marker value for concurrent Maps (which don't support null values).
	 */
	protected static final Object NULL_OBJECT = new Object();


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Counter used to record the order that singletons are added */
	private final AtomicLong singletonCounter = new AtomicLong();

	/** Map of {@link Singleton}s (can hold singletonObjects, factories or earlySingletonObjects) **/
	private final ConcurrentMap<String, Singleton> singletons = new ConcurrentHashMap<String, Singleton>();

	/** List of suppressed Exceptions, available for associating related causes */
	private ThreadLocal<Set<Exception>> suppressedExceptions = new ThreadLocal<Set<Exception>>();

	/** Names of beans that are currently in creation */
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

	/** Names of beans currently excluded from in creation checks */
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

	/** Flag that indicates whether we're currently within destroySingletons */
	private volatile boolean singletonsCurrentlyInDestruction = false;

	/** Disposable bean instances: bean name --> disposable instance */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();

	/** Map between containing bean names: bean name --> Set of bean names that the bean contains */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>(16);

	/** Map between dependent bean names: bean name --> Set of dependent bean names */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

	/** Map between depending bean names: bean name --> Set of bean names for the bean's dependencies */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

	/** Cache of current singleton names, blown away when singletons change */
	private volatile String[] registeredSingletonNames;


	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Singleton singleton = getOrCreateSingleton(beanName);
		synchronized (singleton) {
			if (singleton.getObject() != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + singleton.getObject() + "] bound");
			}
			singleton.setObject((singletonObject != null ? singletonObject : NULL_OBJECT));
			singletonsChanged();
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		Singleton singleton = getOrCreateSingleton(beanName);
		synchronized (singleton) {
			singleton.setObject((singletonObject != null ? singletonObject : NULL_OBJECT));
			singletonsChanged();
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		Singleton singleton = getOrCreateSingleton(beanName);
		synchronized (singleton) {
			singleton.setFactory(singletonFactory);
		}
	}

	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 */
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		Singleton singleton = getOrCreateSingleton(beanName);
		Object singletonObject = singleton.getObject();
		if(singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			singletonObject = (singletonObject != null ? singletonObject : singleton.getEarlyObject());
			if(singletonObject == null && allowEarlyReference) {
				synchronized (singleton) {
					if(singleton.hasObjectFactory()) {
						singletonObject = singleton.createEarlyObjectFromFactory();
					}
				}
			}
		}
		return (singletonObject != NULL_OBJECT ? singletonObject : null);
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	public Object getSingleton(String beanName, ObjectFactory singletonFactory) {
		Singleton singleton = getOrCreateSingleton(beanName);
		Object singletonObject = singleton.getObject();
		if(singletonObject == null) {
			synchronized (singleton) {
				singletonObject = singleton.getObject();
				if (singletonObject == null) {
					if (this.singletonsCurrentlyInDestruction) {
						throw new BeanCreationNotAllowedException(beanName,
								"Singleton bean creation not allowed while the singletons of this factory are in destruction " +
								"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
					}
					beforeSingletonCreation(beanName);
					boolean recordSuppressedExceptions = (this.suppressedExceptions.get() == null);
					if (recordSuppressedExceptions) {
						this.suppressedExceptions.set(new LinkedHashSet<Exception>());
					}
					try {
						singletonObject = singletonFactory.getObject();
					}
					catch (BeanCreationException ex) {
						if (recordSuppressedExceptions) {
							for (Exception suppressedException : this.suppressedExceptions.get()) {
								ex.addRelatedCause(suppressedException);
							}
						}
						throw ex;
					}
					finally {
						if (recordSuppressedExceptions) {
							this.suppressedExceptions.remove();
						}
						afterSingletonCreation(beanName);
					}
					singleton.setObject((singletonObject != null ? singletonObject : NULL_OBJECT));
					singletonsChanged();
				}
			}
		}
		return (singletonObject != NULL_OBJECT ? singletonObject : null);
	}

	/**
	 * Register an Exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * @param ex the Exception to register
	 */
	protected void onSuppressedException(Exception ex) {
		Set<Exception> suppressedExceptions = this.suppressedExceptions.get();
		if(suppressedExceptions != null) {
			suppressedExceptions.add(ex);
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex(String)
	 */
	protected void removeSingleton(String beanName) {
		this.singletons.remove(beanName);
		singletonsChanged();
	}

	public boolean containsSingleton(String beanName) {
		Singleton singleton = this.singletons.get(beanName);
		return (singleton != null && singleton.getObject() != null);
	}

	public String[] getSingletonNames() {
		// Use a local copy in case another thread changes
		String[] registeredSingletonNames = this.registeredSingletonNames;
		if (registeredSingletonNames == null) {
			List<Map.Entry<String, Singleton>> entries = getOrderedSingletonEntries();
			registeredSingletonNames = getRegisteredSingletonNames(entries);
			this.registeredSingletonNames = registeredSingletonNames;
		}
		return registeredSingletonNames;
	}

	private List<Map.Entry<String, Singleton>> getOrderedSingletonEntries() {
		List<Map.Entry<String, Singleton>> entries =
				new ArrayList<Map.Entry<String, Singleton>>(singletons.entrySet());

		Collections.sort(entries, new Comparator<Map.Entry<String, Singleton>>() {
			public int compare(Entry<String, Singleton> o1,
					Entry<String, Singleton> o2) {
				long p1 = o1.getValue().getPosition();
				long p2 = o2.getValue().getPosition();
				return (p1 < p2) ? -1 : ((p1 == p2) ? 0 : 1);
			}
		});
		return entries;
	}

	private String[] getRegisteredSingletonNames(List<Map.Entry<String, Singleton>> entries) {
		List<String> names = new ArrayList<String>(entries.size());
		for (Map.Entry<String, Singleton> entry : entries) {
			if(entry.getValue().isRegistered()) {
				names.add(entry.getKey());
			}
		}
		return names.toArray(new String[names.size()]);
	}

	public int getSingletonCount() {
		return getSingletonNames().length;
	}

	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) &&
				!this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) &&
				!this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans = this.containedBeanMap.get(containingBeanName);
			if (containedBeans == null) {
				containedBeans = new LinkedHashSet<String>(8);
				this.containedBeanMap.put(containingBeanName, containedBeans);
			}
			containedBeans.add(containedBeanName);
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);
		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
			if (dependentBeans == null) {
				dependentBeans = new LinkedHashSet<String>(8);
				this.dependentBeanMap.put(canonicalName, dependentBeans);
			}
			dependentBeans.add(dependentBeanName);
		}
		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
			if (dependenciesForBean == null) {
				dependenciesForBean = new LinkedHashSet<String>(8);
				this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
			}
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		return StringUtils.toStringArray(dependentBeans);
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		return dependenciesForBean.toArray(new String[dependenciesForBean.size()]);
	}

	public void destroySingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Destroying singletons in " + this);
		}
		this.singletonsCurrentlyInDestruction = true;
		try {
			String[] disposableBeanNames;
			synchronized (this.disposableBeans) {
				disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
			}
			for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
				destroySingleton(disposableBeanNames[i]);
			}

			this.containedBeanMap.clear();
			this.dependentBeanMap.clear();
			this.dependenciesForBeanMap.clear();
			this.singletons.clear();
			singletonsChanged();
		}
		finally {
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		Set<String> dependencies = this.dependentBeanMap.remove(beanName);
		if (dependencies != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				logger.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
			}
		}

		// Trigger destruction of contained beans...
		Set<String> containedBeans = this.containedBeanMap.remove(beanName);
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Expose the singleton mutex to subclasses.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 * @deprecated use getSingletonMutex(beanName), remains only for binary compatibility
	 */
	protected final Object getSingletonMutex() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Expose the singleton mutex to subclasses.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	protected final Object getSingletonMutex(String beanName) {
		return getOrCreateSingleton(beanName);
	}

	private Singleton getOrCreateSingleton(String beanName) {
		Assert.notNull(beanName, "'beanName' must not be null");
		// Quick check to save creating throw away singleton
		Singleton singleton = this.singletons.get(beanName);
		if(singleton != null) {
			return singleton;
		}

		singleton = new Singleton(this.singletonCounter.getAndIncrement());
		Singleton existing = this.singletons.putIfAbsent(beanName, singleton);
		if(existing == null) { // We added a new singeton
			return singleton;
		}
		return existing;
	}

	protected void singletonsChanged() {
		this.registeredSingletonNames = null;
	}


	private static class Singleton {


		private long position;

		private Object object;

		private ObjectFactory factory;

		private Object earlyObject;


		public Singleton(long position) {
			this.position = position;
		}

		public long getPosition() {
			return this.position;
		}

		public Object getObject() {
			return this.object;
		}

		public void setObject(Object object) {
			this.object = object;
			this.factory = null;
			this.earlyObject = null;
		}

		public void setFactory(ObjectFactory factory) {
			this.factory = factory;
			this.object = null;
			this.earlyObject = null;
		}

		public Object createEarlyObjectFromFactory() {
			Assert.state(this.factory != null, "Missing ObjectFactory");
			this.earlyObject = this.factory.getObject();
			this.object = null;
			this.factory = null;
			return earlyObject;
		}

		public boolean hasObjectFactory() {
			return this.factory != null;
		}

		public Object getEarlyObject() {
			return this.earlyObject;
		}

		public boolean isRegistered() {
			return this.object != null || this.earlyObject != null || this.factory != null;
		}
	}

}
