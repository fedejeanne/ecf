/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/

package org.eclipse.ecf.presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.ecf.core.identity.ID;

/**
 * Roster entry base class implementing {@link IRosterEntry}. Subclasses may be
 * created as appropriate
 * 
 * @deprecated  in favor of interfaces/classes in <code>org.eclipse.ecf.presence.roster</code> package
 */
public class RosterEntry implements IRosterEntry {

	protected ID serviceID;

	protected ID userID;

	protected String name;

	protected IPresence presence;

	protected InterestType interestType;

	protected List groups;

	public RosterEntry(ID svcID, ID userID, String name,
			IPresence presenceState, InterestType interestType, Collection grps) {
		if (svcID == null)
			throw new NullPointerException("svcID cannot be null");
		this.serviceID = svcID;
		if (userID == null)
			throw new NullPointerException("userID cannot be null");
		this.userID = userID;
		this.name = name;
		this.presence = presenceState;
		this.interestType = interestType;
		this.groups = new ArrayList();
		if (grps != null)
			this.groups.addAll(groups);
	}

	public RosterEntry(ID svcID, ID userID, String name) {
		this(svcID, userID, name, null, InterestType.BOTH, null);
	}

	public RosterEntry(ID svcID, ID userID, String name, IPresence presenceState) {
		this(svcID, userID, name, presenceState, InterestType.BOTH, null);
	}

	public RosterEntry(ID svcID, ID userID, String name,
			InterestType interestType) {
		this(svcID, userID, name, null, interestType, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.ui.presence.IRosterEntry#getGroups()
	 */
	public Collection getGroups() {
		return groups;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.ui.presence.IRosterEntry#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.ui.presence.IRosterEntry#getUserID()
	 */
	public ID getUserID() {
		return userID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.ui.presence.IRosterEntry#getInterestType()
	 */
	public InterestType getInterestType() {
		return interestType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.ui.presence.IRosterEntry#getPresenceState()
	 */
	public IPresence getPresence() {
		return presence;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.ui.presence.IRosterEntry#addGroup(org.eclipse.ecf.ui.presence.IRosterGroup)
	 */
	public boolean add(IRosterGroup group) {
		if (group == null)
			return false;
		return groups.add(group);
	}

	public void addAll(Collection grps) {
		if (grps == null)
			return;
		groups.addAll(grps);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.ui.presence.IRosterEntry#remvoe(org.eclipse.ecf.ui.presence.IRosterGroup)
	 */
	public boolean remove(IRosterGroup group) {
		if (group == null)
			return false;
		return groups.remove(group);
	}

	public ID getServiceID() {
		return serviceID;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("RosterEntry["); //$NON-NLS-1$
		synchronized (sb) {
			sb.append("userID=").append(userID).append(';'); //$NON-NLS-1$
			sb.append("name=").append(name).append(';'); //$NON-NLS-1$
			sb.append("presence=").append(presence).append(';'); //$NON-NLS-1$
			sb.append("interest=").append(interestType).append(';'); //$NON-NLS-1$
			sb.append("groups="); //$NON-NLS-1$
			if (!groups.isEmpty()) {
				for (int i = 0; i < groups.size(); i++) {
					sb.append(((IRosterGroup) groups.get(i)).getName());
					if (i < (groups.size()-1)) sb.append(',');
				}
			}
			sb.append(']');
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

}
