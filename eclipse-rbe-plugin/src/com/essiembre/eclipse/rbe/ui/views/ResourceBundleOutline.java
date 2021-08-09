/*
 * Copyright (C) 2003-2014 Pascal Essiembre Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.essiembre.eclipse.rbe.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.essiembre.eclipse.rbe.RBEPlugin;
import com.essiembre.eclipse.rbe.model.DeltaEvent;
import com.essiembre.eclipse.rbe.model.IDeltaListener;
import com.essiembre.eclipse.rbe.model.tree.KeyTree;
import com.essiembre.eclipse.rbe.model.tree.KeyTreeItem;
import com.essiembre.eclipse.rbe.model.workbench.RBEPreferences;
import com.essiembre.eclipse.rbe.ui.UIUtils;
import com.essiembre.eclipse.rbe.ui.editor.i18n.tree.KeyTreeContentProvider;
import com.essiembre.eclipse.rbe.ui.editor.i18n.tree.KeyTreeLabelProvider;
import com.essiembre.eclipse.rbe.ui.editor.i18n.tree.TreeViewerContributor;

/**
 * This outline provides a view for the property keys coming with with a
 * ResourceBundle
 */
public class ResourceBundleOutline extends ContentOutlinePage {

    private KeyTree                tree;
    private KeyTreeContentProvider contentprovider;
    private ToggleAction           filterincomplete;
    private ToggleAction           flataction;
    private ToggleAction           hierarchicalaction;
    private boolean                hierarchical;
    private TreeViewerContributor  contributor;

    /**
     * Initializes this outline while using the mediator which provides all
     * necessary informations.
     *
     * @param mediator
     *            The mediator which comes with all necessary informations.
     */
    public ResourceBundleOutline(KeyTree keytree) {
        super();
        this.tree = keytree;
        this.contentprovider = new KeyTreeContentProvider();
        this.hierarchical = RBEPreferences.getKeyTreeHierarchical();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        this.getTreeViewer().setContentProvider(this.contentprovider);
        this.getTreeViewer().setLabelProvider(new KeyTreeLabelProvider());
        this.getTreeViewer().setUseHashlookup(true);
        this.getTreeViewer().setInput(this.tree);
        if (RBEPreferences.getKeyTreeExpanded()) {
            ((Tree) this.getTreeViewer().getControl()).setRedraw(false);
            this.getTreeViewer().expandAll();
            ((Tree) this.getTreeViewer().getControl()).setRedraw(true);
        }
        this.contributor = new TreeViewerContributor(this.tree, this.getTreeViewer());
        this.contributor.createControl(parent);
        LocalBehaviour localbehaviour = new LocalBehaviour();
        this.getTreeViewer().addSelectionChangedListener(localbehaviour);
        this.getTreeViewer().getTree().addMouseListener(localbehaviour);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        // contributor.dispose();
        super.dispose();
    }


    /**
     * Gets the selected key tree item.
     *
     * @return key tree item
     */
    public KeyTreeItem getTreeSelection() {
        IStructuredSelection selection = (IStructuredSelection) this.getTreeViewer().getSelection();
        return ((KeyTreeItem) selection.getFirstElement());
    }


    /**
     * Gets selected key.
     *
     * @return selected key
     */
    private String getSelectedKey() {
        String key = null;
        KeyTreeItem item = this.getTreeSelection();
        if (item != null) {
            key = item.getId();
        }
        return (key);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setActionBars(IActionBars actionbars) {
        super.setActionBars(actionbars);
        this.filterincomplete = new ToggleAction(UIUtils.IMAGE_INCOMPLETE_ENTRIES);
        this.filterincomplete.setToolTipText(RBEPlugin.getString("key.filter.incomplete"));

        this.flataction = new ToggleAction(UIUtils.IMAGE_LAYOUT_FLAT);
        this.flataction.setToolTipText(RBEPlugin.getString("key.layout.flat"));
        this.flataction.setChecked(!this.hierarchical);

        this.hierarchicalaction = new ToggleAction(UIUtils.IMAGE_LAYOUT_HIERARCHICAL);
        this.hierarchicalaction.setToolTipText(RBEPlugin.getString("key.layout.tree"));
        this.hierarchicalaction.setChecked(this.hierarchical);

        actionbars.getToolBarManager().add(this.flataction);
        actionbars.getToolBarManager().add(this.hierarchicalaction);
        actionbars.getToolBarManager().add(this.filterincomplete);
    }


    /**
     * Invokes this functionality according to the toggled action.
     *
     * @param action
     *            The action that has been toggled.
     */
    private void update(ToggleAction action) {
        int actioncode = 0;
        if (action == this.filterincomplete) {
            actioncode = TreeViewerContributor.KT_INCOMPLETE;
        } else if (action == this.flataction) {
            actioncode = TreeViewerContributor.KT_FLAT;
        } else if (action == this.hierarchicalaction) {
            actioncode = TreeViewerContributor.KT_HIERARCHICAL;
        }
        this.contributor.update(actioncode, action.isChecked());
        this.flataction.setChecked((this.contributor.getMode() & TreeViewerContributor.KT_HIERARCHICAL) == 0);
        this.hierarchicalaction.setChecked((this.contributor.getMode() & TreeViewerContributor.KT_HIERARCHICAL) != 0);
    }

    /**
     * Simple toggle action which delegates it's invocation to the method
     * {@link #update(ToggleAction)}.
     */
    private class ToggleAction extends Action {

        /**
         * Initializes this action using the supplied icon.
         *
         * @param icon
         *            The icon which shall be displayed.
         */
        public ToggleAction(String icon) {
            super(null, IAction.AS_CHECK_BOX);
            this.setImageDescriptor(RBEPlugin.getImageDescriptor(icon));
        }


        @Override
        public void run() {
            ResourceBundleOutline.this.update(this);
        }
    }

    /**
     * Implementation of custom behaviour.
     */
    private class LocalBehaviour extends MouseAdapter implements IDeltaListener, ISelectionChangedListener {

        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            String selected = ResourceBundleOutline.this.getSelectedKey();
            if (selected != null) {
                ResourceBundleOutline.this.tree.selectKey(selected);
            }
        }


        @Override
        public void add(DeltaEvent event) {}


        @Override
        public void remove(DeltaEvent event) {}


        @Override
        public void modify(DeltaEvent event) {}


        @Override
        public void select(DeltaEvent event) {
            KeyTreeItem item = (KeyTreeItem) event.receiver();
            if (item != null) {
                ResourceBundleOutline.this.getTreeViewer().setSelection(new StructuredSelection(item));
            }
        }


        @Override
        public void mouseDoubleClick(MouseEvent event) {
            Object element = ResourceBundleOutline.this.getSelection();
            if (ResourceBundleOutline.this.getTreeViewer().isExpandable(element)) {
                if (ResourceBundleOutline.this.getTreeViewer().getExpandedState(element)) {
                    ResourceBundleOutline.this.getTreeViewer().collapseToLevel(element, 1);
                } else {
                    ResourceBundleOutline.this.getTreeViewer().expandToLevel(element, 1);
                }
            }
        }

    }

}
