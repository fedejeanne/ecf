/*******************************************************************************
 * Copyright (c) 2007 Remy Suen, Composent, Inc., and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Remy Suen <remy.suen@gmail.com> - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.internal.ui.actions;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ecf.core.ContainerFactory;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.ui.IConfigurationWizard;
import org.eclipse.ecf.ui.IConnectWizard;
import org.eclipse.ecf.ui.wizards.EmptyConfigurationWizard;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;

public class SelectProviderAction implements IWorkbenchWindowActionDelegate,
		IWorkbenchWindowPulldownDelegate {

	private IWorkbenchWindow window;

	private Menu menu;

	private HashMap map = new HashMap();

	public SelectProviderAction() {
		try {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtension[] configurationWizards = registry.getExtensionPoint(
					"org.eclipse.ecf.ui.configurationWizards").getExtensions();
			IExtension[] connectWizards = registry.getExtensionPoint(
					"org.eclipse.ecf.ui.connectWizards").getExtensions();
			for (int i = 0; i < connectWizards.length; i++) {
				final IConfigurationElement[] ices = connectWizards[i]
						.getConfigurationElements();
				for (int j = 0; j < ices.length; j++) {
					final String factoryName = ices[j]
							.getAttribute("containerFactoryName");
					final IConfigurationWizard wizard = getWizard(
							configurationWizards, factoryName);
					if (wizard == null
							|| wizard.getClass().equals(
									EmptyConfigurationWizard.class)) {
						final IConfigurationElement ice = ices[j];
						map.put(ices[j].getAttribute("name"),
								new SelectionAdapter() {
									public void widgetSelected(SelectionEvent e) {
										openConnectWizard(ice, factoryName);
									}
								});
					} else {
						System.out.println(wizard.getClass() + ":" + wizard);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void openConnectWizard(IConfigurationElement element,
			String factoryName) {
		try {
			IContainer container = ContainerFactory.getDefault()
					.createContainer(factoryName);
			IConnectWizard icw = (IConnectWizard) element
					.createExecutableExtension("class");
			icw.init(window.getWorkbench(), container);
			new WizardDialog(window.getShell(), icw).open();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void dispose() {
		// dispose of the menu
		if (menu != null && !menu.isDisposed()) {
			menu.dispose();
		}
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// nothing to do
	}

	private static IConfigurationWizard getWizard(IExtension[] extensions,
			String containerFactoryName) throws Exception {
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i]
					.getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (containerFactoryName.equals(elements[j]
						.getAttribute("containerFactoryName"))) {
					return (IConfigurationWizard) elements[j]
							.createExecutableExtension("class");
				}
			}
		}
		return null;
	}

	public Menu getMenu(Control parent) {
		if (menu == null) {
			menu = new Menu(parent);
			for (Iterator it = map.keySet().iterator(); it.hasNext();) {
				String name = (String) it.next();
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setText(name);
				item.addSelectionListener((SelectionListener) map.get(name));
			}
		}
		return menu;
	}

}
