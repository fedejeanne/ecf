/*******************************************************************************
 * Copyright (c) 2015 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.internal.remoteservices.ui;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ecf.discovery.identity.IServiceID;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescriptionReader;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.IEndpointDescriptionLocator;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin.ImportReference;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin.ImportRegistration;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.AbstractEndpointNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointAsyncInterfacesNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointConfigTypesNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointConnectTargetIDNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointContentProvider;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointDiscoveryGroupNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointFrameworkIDNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointHostGroupNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointIDNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointIntentsNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointInterfacesNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointNamespaceNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointPackageVersionNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointPropertyNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointRemoteServiceFilterNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointRemoteServiceIDNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointServiceIDNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.EndpointTimestampNode;
import org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.ImportRegistrationNode;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.runtime.registry.RegistryBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.BundleException;
import org.osgi.service.remoteserviceadmin.EndpointEvent;

public class EndpointDiscoveryView extends ViewPart {

	public static final String ID_VIEW = "org.eclipse.ecf.remoteserviceadmin.ui.views.EndpointDiscoveryView"; //$NON-NLS-1$

	private TreeViewer viewer;
	private Action startRSAAction;
	private Action copyValueAction;
	private Action copyNameAction;
	private Action importAction;
	private Action unimportAction;

	private Action undiscoverAction;

	private Action edefDiscoverAction;

	private Clipboard clipboard;

	private DiscoveryComponent discovery;

	private EndpointContentProvider contentProvider;

	public EndpointDiscoveryView() {
	}

	public void createPartControl(Composite parent) {
		this.discovery = DiscoveryComponent.getDefault();
		this.discovery.setView(this);

		IViewSite viewSite = getViewSite();
		this.contentProvider = new EndpointContentProvider(viewSite,
				Messages.EndpointDiscoveryView_ENDPOINT_ROOT_NAME);

		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(this.contentProvider);
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		viewer.setInput(viewSite);

		makeActions();
		hookContextMenu();
		contributeToActionBars();
		// setup clipboard
		clipboard = new Clipboard(viewer.getControl().getDisplay());
		getSite().setSelectionProvider(viewer);

		// Add any previously discovered endpoints
		viewer.getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				IEndpointDescriptionLocator locator = discovery.getEndpointDescriptionLocator();
				if (locator != null) {
					EndpointDescription[] eds = locator.getDiscoveredEndpoints();
					for(EndpointDescription ed: eds)
						handleEndpointDescription(EndpointEvent.ADDED, ed);
				}
			}});
		
		showServicesInRegistryBrowser();
		
	}

	private int previousRegistryBrowserGroupBy;

	private int invokeShowGroupBy(RegistryBrowser registryBrowser, int groupBy)
			throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		return (int) registryBrowser.getClass()
				.getDeclaredMethod("showGroupBy", int.class)
				.invoke(registryBrowser, groupBy);
	}

	private int showInRegistryBrowser(int groupBy) {
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=270684#c33
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			if (window != null) {
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					IViewPart view = page
							.findView("org.eclipse.pde.runtime.RegistryBrowser");
					if (view != null)
						return invokeShowGroupBy((RegistryBrowser) view,
								RegistryBrowser.SERVICES);
				}
			}
		} catch (Exception e) {
			logWarning("Could not show services in PDE Plugin view", e);
		}
		return RegistryBrowser.BUNDLES;
	}

	private void showServicesInRegistryBrowser() {
		this.previousRegistryBrowserGroupBy = showInRegistryBrowser(RegistryBrowser.SERVICES);
	}

	@Override
	public void dispose() {
		showInRegistryBrowser(previousRegistryBrowserGroupBy);
		super.dispose();
		viewer = null;
		contentProvider = null;
		if (discovery != null) {
			discovery.setView(null);
			discovery = null;
		}
		discoveredEndpointIds.clear();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				EndpointDiscoveryView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		bars.getMenuManager().add(startRSAAction);
		bars.getMenuManager().add(edefDiscoverAction);
		bars.getToolBarManager().add(startRSAAction);
		bars.getToolBarManager().add(edefDiscoverAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		ITreeSelection selection = (ITreeSelection) viewer.getSelection();
		if (selection != null) {
			Object e = selection.getFirstElement();
			if (e instanceof EndpointPropertyNode) {
				manager.add(copyNameAction);
				manager.add(copyValueAction);
			} else if (e instanceof EndpointNode) {
				EndpointNode edNode = (EndpointNode) e;
				ImportRegistration ir = edNode.getImportRegistration();
				if (ir == null) {
					manager.add(importAction);
					manager.add(undiscoverAction);
				} else
					manager.add(unimportAction);
			}
		}
	}

	private void log(int level, String message, Throwable e) {
		Activator
				.getDefault()
				.getLog()
				.log(new Status(level, Activator.PLUGIN_ID, message, e));
	}

	private void logWarning(String message, Throwable e) {
		log(IStatus.WARNING, message, e);
	}
	
	private void logError(String message, Throwable e) {
		log(IStatus.ERROR, message, e);
	}

	private void makeActions() {
		startRSAAction = new Action() {
			public void run() {
				if (discovery != null)
					try {
						discovery.startRSA();
						startRSAAction.setEnabled(false);
					} catch (BundleException e) {
						logError(
								Messages.EndpointDiscoveryView_ERROR_RSA_START_FAILED,
								e);
						showMessage(Messages.EndpointDiscoveryView_ERROR_MSG_RSA_START_PREFIX
								+ e.getMessage()
								+ Messages.EndpointDiscoveryView_ERROR_MSG_SUFFIX);
					}
			}
		};
		startRSAAction.setText(Messages.EndpointDiscoveryView_START_RSA);
		startRSAAction
				.setToolTipText(Messages.EndpointDiscoveryView_START_RSA_SERVICE);
		startRSAAction.setEnabled(discovery.getRSA() == null);

		copyValueAction = new Action() {
			public void run() {
				Object o = ((ITreeSelection) viewer.getSelection())
						.getFirstElement();
				String data = ((EndpointPropertyNode) o).getPropertyValue()
						.toString();
				if (data != null && data.length() > 0) {
					clipboard.setContents(new Object[] { data },
							new Transfer[] { TextTransfer.getInstance() });
				}
			}
		};
		copyValueAction
				.setText(Messages.EndpointDiscoveryView_COPY_PROPERTY_VALUE);
		copyValueAction
				.setToolTipText(Messages.EndpointDiscoveryView_COPY_PROPERTY_VALUE);
		copyValueAction.setImageDescriptor(RSAImageRegistry.DESC_PROPERTY_OBJ);

		copyNameAction = new Action() {
			public void run() {
				Object o = ((ITreeSelection) viewer.getSelection())
						.getFirstElement();
				String data = ((EndpointPropertyNode) o).getPropertyName();
				if (data != null && data.length() > 0) {
					clipboard.setContents(new Object[] { data },
							new Transfer[] { TextTransfer.getInstance() });
				}
			}
		};
		copyNameAction
				.setText(Messages.EndpointDiscoveryView_COPY_PROPERTY_NAME);
		copyNameAction
				.setToolTipText(Messages.EndpointDiscoveryView_COPY_PROPERTY_NAME);
		copyNameAction.setImageDescriptor(RSAImageRegistry.DESC_PROPERTY_OBJ);

		importAction = new Action() {
			public void run() {
				EndpointNode edNode = getEDNodeSelected();
				if (edNode != null) {
					RemoteServiceAdmin rsa = discovery.getRSA();
					if (rsa == null)
						showMessage(Messages.EndpointDiscoveryView_ERROR_MSG_RSA_IS_NULL);
					else {
						// Do import
						EndpointDescription ed = edNode
								.getEndpointDescription();
						ImportRegistration reg = (ImportRegistration) rsa
								.importService(ed);
						// Check if import exception
						Throwable exception = reg.getException();
						if (exception != null) {
							logError(
									Messages.EndpointDiscoveryView_ERROR_MSG_RSA_IMPORTSERVICE_FAILED,
									exception);
							showMessage(Messages.EndpointDiscoveryView_ERROR_MSG_RSA_IMPORTSERVICE_FAILED_PREFIX
									+ exception.getMessage()
									+ Messages.EndpointDiscoveryView_ERROR_MSG_SUFFIX);
						} else {
							// Success! Set registration
							// and refresh
							edNode.setImportReg(new ImportRegistrationNode(reg));
							viewer.refresh();
						}
					}
				}
			}
		};
		importAction
				.setText(Messages.EndpointDiscoveryView_IMPORT_REMOTE_SERVICE);
		importAction
				.setToolTipText(Messages.EndpointDiscoveryView_IMPORT_REMOTE_SERVICE_TT);
		importAction.setImageDescriptor(RSAImageRegistry.DESC_RSPROXY_CO);

		unimportAction = new Action() {
			public void run() {
				EndpointNode edNode = getEDNodeSelected();
				if (edNode != null) {
					ImportRegistration ir = edNode.getImportRegistration();
					if (ir == null)
						return;
					try {
						ir.close();
						edNode.setImportReg(null);
						viewer.refresh();
					} catch (Exception e) {
						logError(
								Messages.EndpointDiscoveryView_ERROR_MSG_CANNOT_CLOSE_IR,
								e);
						showMessage(Messages.EndpointDiscoveryView_ERROR_MSG_CANNOT_CLOSE_IR_PREFIX
								+ e.getMessage()
								+ Messages.EndpointDiscoveryView_ERROR_MSG_SUFFIX);
					}
				}
			}
		};
		unimportAction
				.setText(Messages.EndpointDiscoveryView_CLOSE_IMPORTED_REMOTE_SERVICE);
		unimportAction
				.setToolTipText(Messages.EndpointDiscoveryView_CLOSE_IMPORTED_REMOTE_SERVICE_TT);

		edefDiscoverAction = new Action() {
			public void run() {
				IEndpointDescriptionLocator locator = discovery
						.getEndpointDescriptionLocator();
				if (locator != null) {
					FileDialog dialog = new FileDialog(viewer.getControl()
							.getShell(), SWT.OPEN);
					dialog.setFilterExtensions(new String[] { "*.xml" }); //$NON-NLS-1$
					dialog.setText(Messages.EndpointDiscoveryView_OPEN_EDEF_FILE);
					dialog.setFilterPath(null);
					String result = dialog.open();
					if (result != null)
						try {
							EndpointDescription[] eds = (EndpointDescription[]) new EndpointDescriptionReader()
									.readEndpointDescriptions(new FileInputStream(
											result));
							if (eds != null) {
								for (int i = 0; i < eds.length; i++)
									locator.discoverEndpoint(eds[i]);
							}
						} catch (IOException e) {
							logError(
									Messages.EndpointDiscoveryView_ERROR_MSG_ENDPOINT_PARSING_FAILED,
									e);
							showMessage(Messages.EndpointDiscoveryView_ERROR_MSG_ENDPOINT_PARSING_FAILED_PREFIX
									+ e.getMessage()
									+ Messages.EndpointDiscoveryView_ERROR_MSG_SUFFIX);
						}
				}
			}
		};
		edefDiscoverAction
				.setText(Messages.EndpointDiscoveryView_OPEN_EDEF_FILE_DIALOG);
		edefDiscoverAction
				.setToolTipText(Messages.EndpointDiscoveryView_OPEN_EDEF_FILE_DIALOG_TT);
		edefDiscoverAction.setEnabled(discovery.getRSA() != null);
		edefDiscoverAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

		undiscoverAction = new Action() {
			public void run() {
				EndpointNode endpoint = getEDNodeSelected();
				if (endpoint != null
						&& endpoint.getImportRegistration() == null) {
					IEndpointDescriptionLocator l = discovery
							.getEndpointDescriptionLocator();
					if (l != null
							&& MessageDialog
									.openQuestion(
											viewer.getControl().getShell(),
											Messages.EndpointDiscoveryView_REMOVE_ENDPOINT_QUESTION_TITLE,
											Messages.EndpointDiscoveryView_REMOVE_ENDPOINT_QUESTION))
						l.undiscoverEndpoint(endpoint.getEndpointDescription());

				}
			}
		};
		undiscoverAction
				.setText(Messages.EndpointDiscoveryView_REMOVE_ENDPOINT);
		undiscoverAction
				.setToolTipText(Messages.EndpointDiscoveryView_REMOVE_ENDPOINT_TT);
	}

	EndpointNode getEDNodeSelected() {
		AbstractEndpointNode aen = getNodeSelected();
		return (aen instanceof EndpointNode) ? (EndpointNode) aen : null;
	}

	boolean isRootSelected() {
		return contentProvider.getRootNode().equals(getNodeSelected());
	}

	AbstractEndpointNode getNodeSelected() {
		return ((AbstractEndpointNode) ((ITreeSelection) viewer.getSelection())
				.getFirstElement());
	}

	void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(),
				Messages.EndpointDiscoveryView_ENDPOINT_MSGBOX_TITLE, message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	void handleEndpointDescription(final int type, final EndpointDescription ed) {
		if (ed == null || viewer == null) return;
		viewer.getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				switch (type) {
				case EndpointEvent.ADDED:
					addEndpoint(ed);
					break;
				case EndpointEvent.REMOVED:
					removeEndpoint(ed);
					break;
				}
				viewer.setExpandedState(contentProvider.getRootNode(), true);
				viewer.refresh();
			}
		});
	}
	
	void handleEndpointChanged(final EndpointEvent event) {
		handleEndpointDescription(event.getType(),(EndpointDescription) event.getEndpoint());
	}

	private List<String> discoveredEndpointIds = new ArrayList<String>();
	
	void addEndpoint(EndpointDescription ed) {
		if (EndpointDiscoveryView.this.previousRegistryBrowserGroupBy != RegistryBrowser.SERVICES)
			showServicesInRegistryBrowser();
		String edId = ed.getId();
		if (!discoveredEndpointIds.contains(edId)) {
			discoveredEndpointIds.add(edId);
			contentProvider.getRootNode().addChild(createEndpointDescriptionNode(ed));
		}
	}
	
	void removeEndpoint(EndpointDescription ed) {
		if (discoveredEndpointIds.remove(ed.getId()))
			contentProvider.getRootNode().removeChild(new EndpointNode(ed));
	}
	
	ImportRegistration findImportRegistration(EndpointDescription ed) {
		RemoteServiceAdmin rsa = discovery.getRSA();
		if (rsa == null)
			return null;
		List<ImportRegistration> iRegs = rsa.getImportedRegistrations();
		for (ImportRegistration ir : iRegs) {
			ImportReference importRef = (ImportReference) ir
					.getImportReference();
			if (importRef != null && ed.equals(importRef.getImportedEndpoint()))
				return ir;
		}
		return null;
	}

	EndpointNode createEndpointDescriptionNode(EndpointDescription ed) {
		EndpointNode edo = new EndpointNode(
				ed,
				new org.eclipse.ecf.remoteserviceadmin.ui.endpoint.model.ImportRegistrationNode(
						findImportRegistration(ed)));

		// Interfaces
		EndpointInterfacesNode ein = new EndpointInterfacesNode();
		for (String intf : ed.getInterfaces())
			ein.addChild(new EndpointPackageVersionNode(EndpointNode
					.getPackageName(intf)));
		edo.addChild(ein);
		// Async Interfaces (if present)
		List<String> aintfs = ed.getAsyncInterfaces();
		if (aintfs.size() > 0) {
			EndpointAsyncInterfacesNode ain = new EndpointAsyncInterfacesNode();
			for (String intf : ed.getAsyncInterfaces())
				ain.addChild(new EndpointPackageVersionNode(EndpointNode
						.getPackageName(intf)));
			edo.addChild(ain);
		}
		// ID
		edo.addChild(new EndpointIDNode());
		// Remote Service Host
		EndpointHostGroupNode idp = new EndpointHostGroupNode(
				Messages.EndpointDiscoveryView_REMOTE_HOST_NAME);
		// Host children
		idp.addChild(new EndpointNamespaceNode());
		idp.addChild(new EndpointRemoteServiceIDNode());
		org.eclipse.ecf.core.identity.ID connectTarget = ed
				.getConnectTargetID();
		if (connectTarget != null)
			idp.addChild(new EndpointConnectTargetIDNode());
		idp.addChild(new EndpointServiceIDNode());
		idp.addChild(new EndpointIntentsNode());
		idp.addChild(new EndpointConfigTypesNode());
		idp.addChild(new EndpointFrameworkIDNode());
		idp.addChild(new EndpointTimestampNode());
		String filter = ed.getRemoteServiceFilter();
		if (filter != null)
			idp.addChild(new EndpointRemoteServiceFilterNode());
		edo.addChild(idp);

		IEndpointDescriptionLocator locator = discovery
				.getEndpointDescriptionLocator();
		IServiceID serviceID = (locator == null)?null:locator.getNetworkDiscoveredServiceID(ed);

		if (serviceID != null)
			edo.addChild(new EndpointDiscoveryGroupNode(
					Messages.EndpointDiscoveryView_DISCOVERY_GROUP_NAME,
					serviceID));

		return edo;
	}

}