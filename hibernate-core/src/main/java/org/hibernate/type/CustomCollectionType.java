/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.LoggableUserType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A custom type for mapping user-written classes that implement {@code PersistentCollection}
 *
 * @see PersistentCollection
 * @see UserCollectionType
 * @author Gavin King
 */
public class CustomCollectionType extends CollectionType {

	private final UserCollectionType userType;
	private final boolean customLogging;

	public CustomCollectionType(
			ManagedBean<? extends UserCollectionType> userTypeBean,
			String role,
			String foreignKeyPropertyName) {
		super(role, foreignKeyPropertyName );

		userType = userTypeBean.getBeanInstance();
		customLogging = userType instanceof LoggableUserType;
	}

	@Override
	public Class<?> getReturnedClass() {
		return userType.instantiate( -1 ).getClass();
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return userType.getClassification();
	}

	@Override
	public PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key)
	throws HibernateException {
		return userType.instantiate( session, persister );
	}

	@Override
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		return userType.wrap( session, collection );
	}

	@Override
	public Object instantiate(int anticipatedType) {
		return userType.instantiate( anticipatedType );
	}

	@Override
	public Iterator<?> getElementsIterator(Object collection) {
		return userType.getElementsIterator(collection);
	}

	@Override
	public boolean contains(Object collection, Object entity, SharedSessionContractImplementor session) {
		return userType.contains(collection, entity);
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		return userType.indexOf(collection, entity);
	}

	@Override
	public Object replaceElements(Object original, Object target, Object owner, Map copyCache, SharedSessionContractImplementor session)
			throws HibernateException {
		CollectionPersister cp = session.getFactory().getMetamodel().collectionPersister( getRole() );
		return userType.replaceElements(original, target, cp, owner, copyCache, session);
	}

	@Override
	protected String renderLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if ( customLogging ) {
			return ( (LoggableUserType) userType ).toLoggableString( value, factory );
		}
		else {
			return super.renderLoggableString( value, factory );
		}
	}

	public UserCollectionType getUserType() {
		return userType;
	}
}
