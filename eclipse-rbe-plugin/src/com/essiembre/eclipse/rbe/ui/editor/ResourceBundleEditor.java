/*
 * Copyright (C) 2003-2017 Pascal Essiembre Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.essiembre.eclipse.rbe.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.essiembre.eclipse.rbe.RBEPlugin;
import com.essiembre.eclipse.rbe.model.tree.KeyTree;
import com.essiembre.eclipse.rbe.ui.UIUtils;
import com.essiembre.eclipse.rbe.ui.editor.i18n.I18nPage;
import com.essiembre.eclipse.rbe.ui.editor.i18n.I18nPageEditor;
import com.essiembre.eclipse.rbe.ui.editor.locale.NewLocalePage;
import com.essiembre.eclipse.rbe.ui.editor.resources.ResourceManager;
import com.essiembre.eclipse.rbe.ui.editor.resources.SourceEditor;
import com.essiembre.eclipse.rbe.ui.views.ResourceBundleOutline;

/**
 * Multi-page editor for editing resource bundles.
 * @author Pascal Essiembre
 */
public class ResourceBundleEditor extends MultiPageEditorPart implements IGotoMarker {

    /** Editor ID, as defined in plugin.xml. */
    public static final String     EDITOR_ID              = "com.essiembre.eclipse.rbe.ui.editor.ResourceBundleEditor";

    private ResourceManager        resourceMediator;
    private I18nPage               i18nPage;
    /** New locale page. */
    private NewLocalePage          newLocalePage;

    /** the outline which additionally allows to navigate through the keys. */
    private ResourceBundleOutline  outline;

    private ResourceChangeListener resourceChangeListener = new ResourceChangeListener();
    private List<IPath>            paths                  = new ArrayList<>();
    private SourceEditor           lastEditor;

    /**
     * Creates a multi-page editor example.
     */
    public ResourceBundleEditor() {
        super();
    }


    /**
     * The <code>MultiPageEditorExample</code> implementation of this method
     * checks that the input is an instance of <code>IFileEditorInput</code>.
     */
    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        super.init(site, editorInput);
        if (editorInput instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) editorInput).getFile();
            try {
                this.resourceMediator = new ResourceManager(site, file);
            } catch (CoreException e) {
                UIUtils.showErrorDialog(site.getShell(), e, "error.init.ui");
                return;
            }

            ResourcesPlugin.getWorkspace().addResourceChangeListener(this.resourceChangeListener, IResourceChangeEvent.POST_CHANGE);

            this.setPartName(this.resourceMediator.getEditorDisplayName());
            this.setContentDescription(RBEPlugin.getString("editor.content.desc") + this.resourceMediator.getEditorDisplayName() + ".");
            this.setTitleImage(UIUtils.getImage(UIUtils.IMAGE_RESOURCE_BUNDLE));
            this.closeIfAreadyOpen(site, file);
        } else {
            throw new PartInitException("Invalid Input: Must be IFileEditorInput");
        }
    }


    /**
     * Gets the resource manager.
     * @return the resource manager
     */
    public ResourceManager getResourceManager() {
        return this.resourceMediator;
    }


    /**
     * Creates the pages of the multi-page editor.
     */
    @Override
    protected void createPages() {
        // Create I18N page
        int index;
        try {
            I18nPageEditor i18PageEditor = new I18nPageEditor(this.resourceMediator);
            index = this.addPage(i18PageEditor, null);
            this.i18nPage = i18PageEditor.getI18nPage();
            this.setPageText(index, RBEPlugin.getString("editor.properties"));
            this.setPageImage(index, UIUtils.getImage(UIUtils.IMAGE_RESOURCE_BUNDLE));
        } catch (PartInitException argh) {
            ErrorDialog.openError(this.getSite().getShell(), "Error creating i18PageEditor page.", null, argh.getStatus());
        }

        // Create text editor pages for each locales
        try {
            SourceEditor[] sourceEditors = this.resourceMediator.getSourceEditors();
            for (int i = 0; i < sourceEditors.length; i++) {
                SourceEditor sourceEditor = sourceEditors[i];
                index = this.addPage(sourceEditor.getEditor(), sourceEditor.getEditor().getEditorInput());
                this.setPageText(index, UIUtils.getDisplayName(sourceEditor.getLocale()));
                this.setPageImage(index, UIUtils.getImage(UIUtils.IMAGE_PROPERTIES_FILE));

                this.paths.add(sourceEditor.getFile().getFullPath());
            }
            this.outline = new ResourceBundleOutline(this.resourceMediator.getKeyTree());

        } catch (PartInitException e) {
            ErrorDialog.openError(this.getSite().getShell(), "Error creating text editor page.", null, e.getStatus());
        }

        // Add "new locale" page
        this.newLocalePage = new NewLocalePage(this.getContainer(), this.resourceMediator, this);
        index = this.addPage(this.newLocalePage);
        this.setPageText(index, RBEPlugin.getString("editor.new.tab"));
        this.setPageImage(index, UIUtils.getImage(UIUtils.IMAGE_NEW_PROPERTIES_FILE));
    }


    public void addResource(IFile resource, Locale locale) {
        try {
            SourceEditor sourceEditor = this.resourceMediator.addSourceEditor(resource, locale);
            int index = this.getPageCount() - 1;
            this.addPage(index, sourceEditor.getEditor(), sourceEditor.getEditor().getEditorInput());
            this.setPageText(index, UIUtils.getDisplayName(sourceEditor.getLocale()));
            this.setPageImage(index, UIUtils.getImage(UIUtils.IMAGE_PROPERTIES_FILE));
            this.i18nPage.refreshPage();
            this.setActivePage(0);
            // re-set the content to trigger dirty state
            sourceEditor.setContent(sourceEditor.getContent());
        } catch (PartInitException e) {
            ErrorDialog.openError(this.getSite().getShell(), "Error creating resource mediator.", null, e.getStatus());
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        T obj = null;
        try {
            obj = super.getAdapter(adapter);
        } catch (NullPointerException e) {
            RBEPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, RBEPlugin.ID, "Got a NPE from MultiPageEditorPart#getAdapter(Class<T>) " + "for adapter class: " + adapter, e));
        }
        if (obj == null && IContentOutlinePage.class.equals(adapter)) {
            return (T) this.outline;
        }
        return obj;
    }


    /**
     * Saves the multi-page editor's document.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        KeyTree keyTree = this.resourceMediator.getKeyTree();
        String key = keyTree.getSelectedKey();

        this.i18nPage.refreshEditorOnChanges();
        this.resourceMediator.save(monitor);

        keyTree.setUpdater(keyTree.getUpdater());
        if (key != null) {
            keyTree.selectKey(key);
        }
    }


    @Override
    public void doSaveAs() {
        // Save As not allowed.
    }


    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }


    /**
     * Change current page based on locale.  If there is no editors associated
     * with current locale, do nothing.
     * @param locale locale used to identify the page to change to
     */
    public void setActivePage(Locale locale) {
        SourceEditor[] editors = this.resourceMediator.getSourceEditors();
        int index = -1;
        for (int i = 0; i < editors.length; i++) {
            SourceEditor editor = editors[i];
            Locale editorLocale = editor.getLocale();
            if (editorLocale != null && editorLocale.equals(locale) || editorLocale == null && locale == null) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            this.setActivePage(index + 1);
        }
    }


    @Override
    public void gotoMarker(IMarker marker) {
        IPath markerPath = marker.getResource().getProjectRelativePath();
        SourceEditor[] sourceEditors = this.resourceMediator.getSourceEditors();
        for (int i = 0; i < sourceEditors.length; i++) {
            SourceEditor editor = sourceEditors[i];
            IPath editorPath = editor.getFile().getProjectRelativePath();
            if (markerPath.equals(editorPath)) {
                this.setActivePage(editor.getLocale());
                IDE.gotoMarker(editor.getEditor(), marker);
                break;
            }
        }
    }


    /**
     * Calculates the contents of page GUI page when it is activated.
     * @param newPageIndex new page index
     */
    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        KeyTree keyTree = this.resourceMediator.getKeyTree();

        if (this.lastEditor != null) {
            String lastEditorKey = this.lastEditor.getCurrentKey();
            if (lastEditorKey != null) {
                keyTree.selectKey(this.lastEditor.getCurrentKey());
            }
        }

        if (newPageIndex == 0) { // switched to first page
            this.resourceMediator.reloadProperties();
            this.i18nPage.refreshTextBoxes();
            this.lastEditor = null; // reset lastEditor
            return;
        }

        if (newPageIndex == this.getPageCount() - 1) {
            return;
        }

        int editorIndex = newPageIndex - 1; // adjust because first page is tree page
        if (editorIndex >= 0 && editorIndex < this.resourceMediator.getSourceEditors().length) {
            this.lastEditor = this.resourceMediator.getSourceEditors()[editorIndex];
            if (keyTree.getSelectedKey() != null) {
                this.lastEditor.selectKey(keyTree.getSelectedKey());
            }
        }
    }


    /**
     * Is the given file a member of this resource bundle.
     * @param file file to test
     * @return <code>true</code> if file is part of bundle
     */
    public boolean isBundleMember(IFile file) {
        return this.resourceMediator.isResource(file);
    }


    private void closeIfAreadyOpen(final IEditorSite site, final IFile file) {
        IWorkbenchPage[] pages = site.getWorkbenchWindow().getPages();
        for (int i = 0; i < pages.length; i++) {
            final IWorkbenchPage page = pages[i];
            IEditorReference[] editors = page.getEditorReferences();
            for (int j = 0; j < editors.length; j++) {
                final IEditorPart editor = editors[j].getEditor(false);
                if (editor instanceof ResourceBundleEditor) {
                    ResourceBundleEditor rbe = (ResourceBundleEditor) editor;
                    if (rbe.isBundleMember(file)) {
                        // putting the close operation into the queue
                        // closing during opening caused errors.
                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                page.closeEditor(editor, true);
                            }
                        });
                    }
                }
            }
        }
    }


    @Override
    public void dispose() {

        if (this.i18nPage != null) {
            this.i18nPage.dispose();
        }
        if (this.newLocalePage != null) {
            this.newLocalePage.dispose();
        }

        /*
         * fix for a weird memory leak: unless we remove the selectionProvider from our editor, nothing get's GCed.
         */
        this.getSite().setSelectionProvider(null);
        SourceEditor[] sourceEditors = this.resourceMediator.getSourceEditors();
        for (int i = 0; i < sourceEditors.length; i++) {
            SourceEditor editor = sourceEditors[i];
            editor.getEditor().getSite().setSelectionProvider(null);
        }

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.resourceChangeListener);
        super.dispose();
    }

    private class ResourceChangeListener implements IResourceChangeListener {

        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            boolean deltaFound = false;
            for (IPath path : ResourceBundleEditor.this.paths) {
                IResourceDelta delta = event.getDelta().findMember(path);
                deltaFound |= delta != null;
            }
            if (deltaFound) {
                ResourceBundleEditor.this.resourceMediator.reloadProperties();
                ResourceBundleEditor.this.i18nPage.refreshTextBoxes();
            }
        }
    }
}
