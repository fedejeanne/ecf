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

import java.util.HashMap;
import java.util.Map;

/**
 * Base presence class implementing {@link IPresence}. Subclasses may be
 * created as appropriate
 * 
 * @deprecated  in favor of interfaces/classes in <code>org.eclipse.ecf.presence.roster</code> package
 */
public class Presence implements IPresence {

	private static final long serialVersionUID = 3906369346107618354L;

	protected Type type;

	protected Mode mode;

	protected String status;

	protected Map properties;

	public Presence() {
		this(Type.AVAILABLE);
	}

	public Presence(Type type) {
		this(type, "", Mode.AVAILABLE);
	}

	public Presence(Type type, String status, Mode mode, Map props) {
		this.type = type;
		this.status = status;
		this.mode = mode;
		this.properties = (props == null) ? new HashMap() : props;
	}

	public Presence(Type type, String status, Mode mode) {
		this(type, status, mode, null);
	}

	public Mode getMode() {
		return mode;
	}

	public Map getProperties() {
		return properties;
	}

	public String getStatus() {
		return status;
	}

	public Type getType() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("Presence[");
		sb.append("type=").append(type).append(";");
		sb.append("mode=").append(mode).append(";");
		sb.append("status=").append(status).append(";");
		sb.append("props=").append(properties).append(";");
		sb.append("]");
		return sb.toString();
	}
}
