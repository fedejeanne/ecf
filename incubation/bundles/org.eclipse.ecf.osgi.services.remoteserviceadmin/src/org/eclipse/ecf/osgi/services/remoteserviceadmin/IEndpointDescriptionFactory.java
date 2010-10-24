/*******************************************************************************
* Copyright (c) 2010 Composent, Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Composent, Inc. - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.osgi.services.remoteserviceadmin;

import org.eclipse.ecf.discovery.IDiscoveryLocator;
import org.eclipse.ecf.discovery.IServiceInfo;
import org.eclipse.ecf.discovery.identity.IServiceID;

public interface IEndpointDescriptionFactory {

	/**
	 * Create an EndpointDescription for a discovered remote service.  Implementers of this
	 * factory service may return the type of EndpointDescription appropriate for the
	 * associated distribution system (e.g. ECFEndpointDescription).  Implementers should return
	 * <code>null</code> if no notification should occur.
	 * 
	 * @param locator the locator responsible for the discoveredServiceInfo.  Must not be <code>null</code>.
	 * @param discoveredServiceInfo the discovered service info.  Must not be <code>null</code>.
	 * @return EndpointDescription that will be used to notify EndpointListeners
	 * about a new EndpointDescription.  If <code>null</code> is returned, no
	 * notification should be performed by the calling code.  
	 */
	public EndpointDescription createDiscoveredEndpointDescription(IDiscoveryLocator locator, IServiceInfo discoveredServiceInfo);
	/**
	 * Get an EndpointDescription for an undiscovered remote service.  Implementers of this
	 * factory service may return the type of EndpointDescription appropriate for the
	 * associated distribution system (e.g. ECFEndpointDescription).  Implementers should return
	 * <code>null</code> if no notification should occur.
	 * 
	 * @param locator the locator responsible for the discoveredServiceInfo.  Must not be <code>null</code>.
	 * @param serviceId the discovered service ID.  Must not be <code>null</code>.
	 * @return EndpointDescription that will be used to notify EndpointListeners
	 * about an undiscovered EndpointDescription.  If <code>null</code> is returned, no
	 * notification should be performed by the calling code.  
	 */
	public EndpointDescription getUndiscoveredEndpointDescription(IDiscoveryLocator locator, IServiceID serviceID);
	
}