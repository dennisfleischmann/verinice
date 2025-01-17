/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak[at]sernet[dot]de>.
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 * Alexander Koderman <ak[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.gs.ui.rcp.main.bsi.views;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import sernet.gs.service.GSServiceException;
import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.CnAWorkspace;
import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.ImageCache;
import sernet.gs.ui.rcp.main.StatusLine;
import sernet.gs.ui.rcp.main.bsi.editors.BSIElementEditorInput;
import sernet.gs.ui.rcp.main.bsi.editors.BSIElementEditorMultiPage;
import sernet.gs.ui.rcp.main.bsi.views.listeners.DisableContextMenuListener;
import sernet.gs.ui.rcp.main.bsi.views.listeners.WhiteListLocationListener;
import sernet.gs.ui.rcp.main.preferences.PreferenceConstants;
import sernet.verinice.interfaces.ActionRightIDs;
import sernet.verinice.iso27k.rcp.ILinkedWithEditorView;
import sernet.verinice.iso27k.rcp.ISMView;
import sernet.verinice.iso27k.rcp.LinkWithEditorPartListener;
import sernet.verinice.model.common.CnALink;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.iso27k.IControl;
import sernet.verinice.rcp.RightsEnabledView;

public class BrowserView extends RightsEnabledView implements ILinkedWithEditorView {

    private static final Logger LOG = Logger.getLogger(BrowserView.class);

    public static final String ID = "sernet.gs.ui.rcp.main.bsi.views.browserview"; //$NON-NLS-1$

    private Browser browser;

    private ISelectionListener selectionListener;

    private IPartListener2 linkWithEditorPartListener = new LinkWithEditorPartListener(this);

    private boolean linkingActive = true;

    private SerializeBrowserLoadingListener serializeListener;

    private CnATreeElement selectedInISMView;

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        GridLayout gl = new GridLayout(1, false);
        parent.setLayout(gl);

        try {
            browser = new Browser(parent, SWT.NONE);
            browser.setLayoutData(new GridData(
                    GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

            browser.setJavascriptEnabled(false);
            browser.addMenuDetectListener(new DisableContextMenuListener());
            browser.addLocationListener(
                    new WhiteListLocationListener(Arrays.asList("about:blank", defaultImage())));

            serializeListener = new SerializeBrowserLoadingListener(browser);
            browser.addProgressListener(serializeListener);

            browser.setUrl(defaultImage());

            Action linkWithEditorAction = new Action(Messages.BrowserView_0, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    toggleLinking(isChecked());
                }
            };

            toggleLinking(Activator.getDefault().getPreferenceStore()
                    .getBoolean(PreferenceConstants.LINK_TO_EDITOR));

            linkWithEditorAction.setImageDescriptor(
                    ImageCache.getInstance().getImageDescriptor(ImageCache.LINKED));
            getViewSite().getActionBars().getToolBarManager().add(linkWithEditorAction);
            linkWithEditorAction.setChecked(isLinkingActive());

            hookPageSelection();
        } catch (Exception e) {
            ExceptionUtil.log(e, Messages.BrowserView_3);
        }
    }

    protected void toggleLinking(boolean checked) {
        this.linkingActive = checked;
        if (checked) {
            Optional.ofNullable(getSite().getPage().getActiveEditor())
                    .ifPresent(this::editorActivated);
        }
    }

    protected boolean isLinkingActive() {
        return linkingActive;
    }

    @Override
    public String getRightID() {
        return ActionRightIDs.BSIBROWSER;
    }

    /*
     * @see sernet.verinice.rcp.RightsEnabledView#getViewId()
     */
    @Override
    public String getViewId() {
        return ID;
    }

    private static String defaultImage() {
        return Paths.get(CnAWorkspace.getInstance().getWorkdir()).resolve("html")//$NON-NLS-1$
                .resolve("about.html").toUri().toString();//$NON-NLS-1$
    }

    private void hookPageSelection() {
        selectionListener = this::pageSelectionChanged;
        getSite().getPage().addPostSelectionListener(selectionListener);
        getSite().getPage().addPartListener(linkWithEditorPartListener);
    }

    protected void pageSelectionChanged(IWorkbenchPart part, ISelection selection) {

        if (part != this && selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();

            if (part instanceof ISMView) {
                setSelectedInISMView(element);
            } else if (part instanceof RelationView) {
                if (element instanceof CnALink) {
                    element = determineLinkedElement((CnALink) element);
                }
            } else if (part instanceof BSIElementEditorMultiPage && element instanceof CnALink
                    && isLinkingActive()) {
                element = determineLinkedElement((CnALink) element);
            }
            if (element != null) {
                elementSelected(element);
            }
        }
    }

    private void setSelectedInISMView(Object element) {
        if (element instanceof CnATreeElement) {
            selectedInISMView = (CnATreeElement) element;
        }
    }

    private Object determineLinkedElement(CnALink link) {
        Object dependant = link.getDependant();
        Object dependency = link.getDependency();

        if (dependant instanceof IControl && !dependant.equals(selectedInISMView)) {
            return dependant;
        } else if (dependency instanceof IControl && !dependency.equals(selectedInISMView)
                || dependant.equals(dependency)) {
            return dependency;
        }
        return null;
    }

    protected void elementSelected(Object element) {
        String html = null;
        try {
            StatusLine.setErrorMessage(""); //$NON-NLS-1$
            html = HtmlWriter.getHtml(element);
        } catch (GSServiceException e) {
            StatusLine.setErrorMessage(Messages.BrowserView_4 + Messages.BrowserView_5);
            LOG.error(Messages.BrowserView_4 + Messages.BrowserView_5, e);
        }
        if (!StringUtils.isEmpty(html)) {
            setText(html);
        } else {
            browser.setUrl(defaultImage());
        }
    }

    @Override
    public void setFocus() {
        browser.setFocus();
    }

    /**
     * Sets the contents to be displayed in the browser window.
     * 
     * @param is
     *            The HTML page to be displayed as an input stream
     */
    public void setText(String text) {
        if (text != null) {
            serializeListener.setText(text);
        }
    }

    @Override
    public void dispose() {
        getSite().getPage().removePostSelectionListener(selectionListener);
        getSite().getPage().removePartListener(linkWithEditorPartListener);
        super.dispose();
    }

    /*
     * @see
     * sernet.verinice.iso27k.rcp.ILinkedWithEditorView#editorActivated(org.
     * eclipse.ui.IEditorPart)
     */
    @Override
    public void editorActivated(IEditorPart activeEditor) {
        CnATreeElement element = BSIElementEditorInput.extractElement(activeEditor);
        selectedInISMView = element;
        if (!isLinkingActive() || !getViewSite().getPage().isPartVisible(this)) {
            return;
        }
        if (element == null) {
            return;
        }
        elementSelected(element);
    }

}
