/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.dao.helper;

import java.util.List;

import ca.sqlpower.dao.PersistedSPOProperty;
import ca.sqlpower.dao.PersistedSPObject;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.SPPersister;
import ca.sqlpower.dao.session.SessionPersisterSuperConverter;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.SPAnnotationProcessor;
import ca.sqlpower.object.annotation.SPAnnotationProcessorFactory;

import com.google.common.collect.Multimap;

/**
 * This factory class creates an instance of each kind of generated
 * {@link SPPersisterHelper} by the {@link SPAnnotationProcessor}. It delegates
 * to all the methods within {@link SPPersisterHelper}. Extending classes of
 * this abstract class should be generated by the {@link SPAnnotationProcessor}
 * as the #{@link SPPersisterHelperFactory#getSPPersisterHelper(Class)} method
 * relies on what {@link SPPersisterHelper}s are generated.
 */
public abstract class SPPersisterHelperFactory {

	/**
	 * The {@link SPPersister} that a workspace persister {@link SPListener}
	 * passes {@link SPPersister#persistObject(String, String, String, int)}
	 * calls to.
	 */
	private final SPPersister persister;

	/**
	 * @see #getConverter()
	 */
	private final SessionPersisterSuperConverter converter;

	public SPPersisterHelperFactory(
			SPPersister persister, 
			SessionPersisterSuperConverter converter) {
		this.persister = persister;
		this.converter = converter;
	}

	/**
	 * Returns the {@link SessionPersisterSuperConverter} to use to convert
	 * property values from a complex to basic persistable type and vice versa.
	 */
	public SessionPersisterSuperConverter getConverter() {
		return converter;
	}

	/**
	 * Retrieves the appropriate {@link SPPersisterHelper} given an
	 * {@link SPObject} class simple name.
	 * 
	 * @param simpleName
	 *            The simple name of the {@link SPObject} class.
	 * @return The {@link SPPersisterHelper} that deals with {@link SPObject}s
	 *         with the specified simple name.
	 */
	public abstract SPPersisterHelper<? extends SPObject> 
			getSPPersisterHelper(String simpleName);

	/**
	 * Retrieves the appropriate {@link SPPersisterHelper} given an
	 * {@link SPObject} class and calls the commitObject method on it to create
	 * an {@link SPObject} represented by the given {@link PersistedSPObject}.
	 * This should be called by a session {@link SPPersister}.
	 * 
	 * @param <T>
	 *            An {@link SPObject} type.
	 * @param pso
	 *            The {@link PersistedSPObject} that the {@link SPObject} is
	 *            being created from. The UUID to use for the created
	 *            {@link SPObject} is to be taken from this object and the
	 *            loaded flag should be set the <code>true</code> before
	 *            returning the newly created {@link SPObject}.
	 * @param persistedProperties
	 *            A mutable {@link Multimap} of {@link SPObject} UUIDs to
	 *            persisted properties, each represented by
	 *            {@link PersistedSPOProperty}. Some entries within this
	 *            {@link Multimap} will be removed if the {@link SPObject}
	 *            constructor it calls requires arguments.
	 * @param persistedObjects
	 *            The {@link List} of {@link PersistedSPObject}s that has been
	 *            persisted in an {@link SPPersister}. This is to be used for
	 *            {@link SPObject}s that take children in their constructor,
	 *            where the {@link SPPersisterHelper} factory finds the
	 *            appropriate {@link SPPersisterHelper} for that child type and
	 *            calls commitObject on that as well.
	 * @return The created {@link SPObject} with the given required persisted
	 *         properties.
	 * @see SPPersisterHelper#commitObject(Multimap, PersistedSPObject, List,
	 *      SPPersisterHelperFactory)
	 */
	public <T extends SPObject> T commitObject(
			PersistedSPObject pso, 
			Multimap<String, PersistedSPOProperty> persistedProperties, 
			List<PersistedSPObject> persistedObjects) {
		SPPersisterHelper<T> helper = 
			(SPPersisterHelper<T>) getSPPersisterHelper(pso.getType());
		return helper.commitObject(pso, persistedProperties, persistedObjects, this);
	}

	/**
	 * Retrieves the appropriate {@link SPPersisterHelper} given an
	 * {@link SPObject} class and calls the commitProperty method on it to set a
	 * value to a specific persistable property. This should be called by a
	 * session {@link SPPersister}.
	 * 
	 * @param <T>
	 *            An {@link SPObject} type.
	 * @param spo
	 *            The {@link SPObject} of type T to apply the property change
	 *            on.
	 * @param propertyName
	 *            The JavaBean property name.
	 * @param newValue
	 *            The new property value. This value must be converted through
	 *            the {@link SessionPersisterSuperConverter} from simple type
	 *            into a complex type before setting the property value.
	 * @throws SPPersistenceException
	 *             Thrown if the property is not a persistable property. The
	 *             setter method for this property in the {@link SPObject} class
	 *             must be annotated with {@link Mutator}.
	 * @see {@link SPPersisterHelper#commitProperty(SPObject, String, Object, SessionPersisterSuperConverter)}
	 */
	public <T extends SPObject> void commitProperty(
			T spo, 
			String propertyName, 
			Object newValue) throws SPPersistenceException {
		SPPersisterHelper<T> helper = 
			(SPPersisterHelper<T>) getSPPersisterHelper(spo.getClass().getSimpleName());
		helper.commitProperty(spo, propertyName, newValue, converter);
	}

	/**
	 * Retrieves the appropriate {@link SPPersisterHelper} given an
	 * {@link SPObject} class and calls the findProperty method on it to return
	 * the value of the given property. This should be called by a session
	 * {@link SPPersister}.
	 * 
	 * @param <T>
	 *            An {@link SPObject} type.
	 * @param spo
	 *            The {@link SPObject} of type T to retrieve the property from.
	 * @param propertyName
	 *            The property name that needs to be retrieved and converted.
	 *            This is the name of the property in the class itself based on
	 *            the property fired by the setter for the event which is
	 *            enforced by tests using JavaBeans methods. If changes are made
	 *            to an {@link SPObject} class such that one or more properties
	 *            are changed (i.e. changed property name or property type), the
	 *            {@link SPAnnotationProcessorFactory} should be executed to
	 *            generate an updated {@link SPPersisterHelper} class.
	 * @return The value stored in the variable of the object we are given at
	 *         the property name after it has been converted to a type that can
	 *         be stored. The conversion is based on the
	 *         {@link SessionPersisterSuperConverter}.
	 * @throws SPPersistenceException
	 *             Thrown if the property is not a persistable property. The
	 *             getter method for this property in the {@link SPObject} class
	 *             must be annotated with {@link Accessor}.
	 * @see SPPersisterHelper#findProperty(SPObject, String,
	 *      SessionPersisterSuperConverter)
	 */
	public <T extends SPObject> Object findProperty(T spo, String propertyName) throws SPPersistenceException {
		SPPersisterHelper<T> helper = 
			(SPPersisterHelper<T>) getSPPersisterHelper(spo.getClass().getSimpleName());
		return helper.findProperty(spo, propertyName, converter);
	}

	/**
	 * Retrieves the appropriate {@link SPPersisterHelper} given an
	 * {@link SPObject} class and calls the persistObject method on it to
	 * persist into an {@link SPPersister}. This should be called by a workspace
	 * persister {@link SPListener}.
	 * 
	 * @param <T>
	 *            An {@link SPObject} type.
	 * @param spo
	 *            The {@link SPObject} of type T to persist along with its
	 *            required properties.
	 * @param index
	 *            The index of the {@link SPObject} to persist relative to its
	 *            siblings of the same type.
	 * @throws SPPersistenceException
	 *             Thrown if the {@link SPPersister} cannot persist the object
	 *             or any one of its properties.
	 * @see {@link SPPersisterHelper#persistObject(SPObject, int, SPPersister, SessionPersisterSuperConverter)}
	 */
	public <T extends SPObject> void persistObject(T spo, int index) throws SPPersistenceException {
		SPPersisterHelper<T> helper = 
			(SPPersisterHelper<T>) getSPPersisterHelper(spo.getClass().getSimpleName());
		helper.persistObject(spo, index, persister, converter);
	}
	
}
