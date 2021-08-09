/*
 * Copyright (C) 2003-2014 Pascal Essiembre Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.essiembre.eclipse.rbe.ui.editor.i18n.tree;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.essiembre.eclipse.rbe.RBEPlugin;
import com.essiembre.eclipse.rbe.model.bundle.BundleGroup;
import com.essiembre.eclipse.rbe.model.tree.KeyTree;
import com.essiembre.eclipse.rbe.model.tree.KeyTreeItem;
import com.essiembre.eclipse.rbe.model.tree.updater.FlatKeyTreeUpdater;
import com.essiembre.eclipse.rbe.model.tree.updater.GroupedKeyTreeUpdater;
import com.essiembre.eclipse.rbe.model.tree.updater.IncompletionUpdater;
import com.essiembre.eclipse.rbe.model.tree.updater.KeyTreeUpdater;
import com.essiembre.eclipse.rbe.model.workbench.RBEPreferences;
import com.essiembre.eclipse.rbe.ui.UIUtils;

/**
 * Helper class which is used to provide menu functions to the used TreeViewer instances
 * (outline and the treeview within the editor).
 */
public class TreeViewerContributor {

    public static final int  KT_FLAT         = 0; // 0th bit unset
    public static final int  KT_HIERARCHICAL = 1; // 0th bit set
    public static final int  KT_INCOMPLETE   = 2; // 1th bit set

    public static final int  MENU_NEW        = 0;
    public static final int  MENU_RENAME     = 1;
    public static final int  MENU_DELETE     = 2;
    public static final int  MENU_COPY       = 3;
    public static final int  MENU_COMMENT    = 4;
    public static final int  MENU_UNCOMMENT  = 5;
    public static final int  MENU_EXPAND     = 6;
    public static final int  MENU_COLLAPSE   = 7;
    public static final int  MENU_GETKEY     = 8;
    private static final int MENU_COUNT      = 9;

    /** the tree which is controlled through this manager.    */
    private KeyTree          tree;

    /** the component which displays the tree.                */
    private TreeViewer       treeviewer;

    private Separator        separator;

    /** actions for the context menu.                           */
    private Action[]         actions;

    /** the updater which is used for structural information. */
    private KeyTreeUpdater   structuralupdater;

    /** holds the information about the current state.        */
    private int              mode;

    /** some cursors to indicate progress                     */
    private Cursor           waitcursor;
    private Cursor           defaultcursor;

    /**
     * Initializes this contributor using the supplied model structure
     * and the viewer which is used to access the model.
     *
     * @param keytree   Out tree model.
     * @param viewer    The viewer used to display the supplied model.
     */
    public TreeViewerContributor(KeyTree keytree, TreeViewer viewer) {
        this.tree = keytree;
        this.treeviewer = viewer;
        this.actions = new Action[TreeViewerContributor.MENU_COUNT];
        this.mode = TreeViewerContributor.KT_HIERARCHICAL;
        this.waitcursor = UIUtils.createCursor(SWT.CURSOR_WAIT);
        this.defaultcursor = UIUtils.createCursor(SWT.CURSOR_ARROW);
        if (RBEPreferences.getKeyTreeHierarchical()) {
            this.structuralupdater = new GroupedKeyTreeUpdater(RBEPreferences.getKeyGroupSeparator());
        } else {
            this.structuralupdater = new FlatKeyTreeUpdater();
        }
    }


    private void buildActions() {
        this.actions[TreeViewerContributor.MENU_NEW] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.newKey();
            }
        };
        this.actions[TreeViewerContributor.MENU_NEW].setText(RBEPlugin.getString("key.new"));

        this.actions[TreeViewerContributor.MENU_RENAME] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.renameKeyOrGroup();
            }
        };
        this.actions[TreeViewerContributor.MENU_RENAME].setText(RBEPlugin.getString("key.rename"));

        this.actions[TreeViewerContributor.MENU_DELETE] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.deleteKeyOrGroup();
            }
        };
        this.actions[TreeViewerContributor.MENU_DELETE].setText(RBEPlugin.getString("key.delete"));

        this.actions[TreeViewerContributor.MENU_COPY] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.copyKeyOrGroup();
            }
        };
        this.actions[TreeViewerContributor.MENU_COPY].setText(RBEPlugin.getString("key.duplicate"));

        this.actions[TreeViewerContributor.MENU_COMMENT] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.commentKey();
            }
        };
        this.actions[TreeViewerContributor.MENU_COMMENT].setText(RBEPlugin.getString("key.comment"));

        this.actions[TreeViewerContributor.MENU_UNCOMMENT] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.uncommentKey();
            }
        };
        this.actions[TreeViewerContributor.MENU_UNCOMMENT].setText(RBEPlugin.getString("key.uncomment"));

        this.separator = new Separator();

        this.actions[TreeViewerContributor.MENU_EXPAND] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.treeviewer.expandAll();
            }
        };
        this.actions[TreeViewerContributor.MENU_EXPAND].setText(RBEPlugin.getString("key.expandAll"));

        this.actions[TreeViewerContributor.MENU_COLLAPSE] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.treeviewer.collapseAll();
            }
        };

        this.actions[TreeViewerContributor.MENU_COLLAPSE].setText(RBEPlugin.getString("key.collapseAll"));

        this.actions[TreeViewerContributor.MENU_GETKEY] = new Action() {

            @Override
            public void run() {
                TreeViewerContributor.this.copyKeyID();
            }
        };
        this.actions[TreeViewerContributor.MENU_GETKEY].setText(RBEPlugin.getString("key.getkey"));

    }


    private void fillMenu(IMenuManager manager) {
        KeyTreeItem selectedItem = this.getSelection();
        manager.add(this.actions[TreeViewerContributor.MENU_NEW]);
        manager.add(this.actions[TreeViewerContributor.MENU_RENAME]);
        this.actions[TreeViewerContributor.MENU_RENAME].setEnabled(selectedItem != null);
        manager.add(this.actions[TreeViewerContributor.MENU_DELETE]);
        this.actions[TreeViewerContributor.MENU_DELETE].setEnabled(selectedItem != null);
        manager.add(this.actions[TreeViewerContributor.MENU_COPY]);
        this.actions[TreeViewerContributor.MENU_COPY].setEnabled(selectedItem != null);
        manager.add(this.actions[TreeViewerContributor.MENU_COMMENT]);
        this.actions[TreeViewerContributor.MENU_COMMENT].setEnabled(selectedItem != null);
        manager.add(this.actions[TreeViewerContributor.MENU_UNCOMMENT]);
        this.actions[TreeViewerContributor.MENU_UNCOMMENT].setEnabled(selectedItem != null);
        manager.add(this.separator);
        manager.add(this.actions[TreeViewerContributor.MENU_GETKEY]);
        this.actions[TreeViewerContributor.MENU_GETKEY].setEnabled(selectedItem != null);
        manager.add(this.separator);
        manager.add(this.actions[TreeViewerContributor.MENU_EXPAND]);
        manager.add(this.actions[TreeViewerContributor.MENU_COLLAPSE]);

    }


    /**
     * Creates the menu contribution for the supplied parental component.
     *
     * @param parent   The component which is receiving the menu.
     */
    public void createControl(Composite parent) {
        this.buildActions();
        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                TreeViewerContributor.this.fillMenu(manager);
            }
        });

        this.treeviewer.getTree().setMenu(menuManager.createContextMenu(parent));

    }


    /**
     * Gets the selected key tree item.
     * @return key tree item
     */
    public KeyTreeItem getSelection() {
        IStructuredSelection selection = (IStructuredSelection) this.treeviewer.getSelection();
        return (KeyTreeItem) selection.getFirstElement();
    }


    /**
     * Creates a new key in case it isn't existing yet.
     */
    protected void newKey() {
        KeyTreeItem selectedItem = this.getSelection();
        String key = selectedItem != null ? selectedItem.getId() : "";
        String msgHead = RBEPlugin.getString("dialog.new.head");
        String msgBody = RBEPlugin.getString("dialog.new.body", key);
        InputDialog dialog = new InputDialog(this.getShell(), msgHead, msgBody, key, null);
        dialog.open();
        if (dialog.getReturnCode() == Window.OK) {
            String newKey = dialog.getValue();
            BundleGroup bundleGroup = this.tree.getBundleGroup();
            if (!bundleGroup.containsKey(newKey)) {
                bundleGroup.addKey(newKey);
            }
        }
    }


    /**
     * Renames a key or group of key.
     */
    protected void renameKeyOrGroup() {
        KeyTreeItem selectedItem = this.getSelection();
        String key = selectedItem.getId();
        String msgHead = null;
        String msgBody = null;
        if (selectedItem.getChildren().size() == 0) {
            msgHead = RBEPlugin.getString("dialog.rename.head.single");
            msgBody = RBEPlugin.getString("dialog.rename.body.single", key);
        } else {
            msgHead = RBEPlugin.getString("dialog.rename.head.multiple");
            msgBody = RBEPlugin.getString("dialog.rename.body.multiple", selectedItem.getName());
        }
        // Rename single item
        InputDialog dialog = new InputDialog(this.getShell(), msgHead, msgBody, key, null);
        dialog.open();
        if (dialog.getReturnCode() == Window.OK) {
            String newKey = dialog.getValue();
            BundleGroup bundleGroup = this.tree.getBundleGroup();
            Collection<KeyTreeItem> items = new ArrayList<>();
            items.add(selectedItem);
            items.addAll(selectedItem.getNestedChildren());
            for (Iterator<KeyTreeItem> iter = items.iterator(); iter.hasNext();) {
                KeyTreeItem item = iter.next();
                String oldItemKey = item.getId();
                if (oldItemKey.startsWith(key)) {
                    String newItemKey = newKey + oldItemKey.substring(key.length());
                    bundleGroup.renameKey(oldItemKey, newItemKey);
                }
            }
        }
    }


    /**
     * Uncomments a key or group of key.
     */
    protected void uncommentKey() {
        KeyTreeItem selectedItem = this.getSelection();
        BundleGroup bundleGroup = this.tree.getBundleGroup();
        Collection<KeyTreeItem> items = new ArrayList<>();
        items.add(selectedItem);
        items.addAll(selectedItem.getNestedChildren());
        for (Iterator<KeyTreeItem> iter = items.iterator(); iter.hasNext();) {
            KeyTreeItem item = iter.next();
            bundleGroup.uncommentKey(item.getId());
        }
    }


    /**
     * Deletes a key or group of key.
     */
    protected void deleteKeyOrGroup() {
        KeyTreeItem selectedItem = this.getSelection();
        String key = selectedItem.getId();
        String msgHead = null;
        String msgBody = null;
        if (selectedItem.getChildren().size() == 0) {
            msgHead = RBEPlugin.getString("dialog.delete.head.single");
            msgBody = RBEPlugin.getString("dialog.delete.body.single", key);
        } else {
            msgHead = RBEPlugin.getString("dialog.delete.head.multiple");
            msgBody = RBEPlugin.getString("dialog.delete.body.multiple", selectedItem.getName());
        }
        MessageBox msgBox = new MessageBox(this.getShell(), SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
        msgBox.setMessage(msgBody);
        msgBox.setText(msgHead);
        if (msgBox.open() == SWT.OK) {
            BundleGroup bundleGroup = this.tree.getBundleGroup();
            Collection<KeyTreeItem> items = new ArrayList<>();
            items.add(selectedItem);
            items.addAll(selectedItem.getNestedChildren());
            for (Iterator<KeyTreeItem> iter = items.iterator(); iter.hasNext();) {
                KeyTreeItem item = iter.next();
                bundleGroup.removeKey(item.getId());
            }
        }
    }


    /**
     * Comments a key or group of key.
     */
    protected void commentKey() {
        KeyTreeItem selectedItem = this.getSelection();
        BundleGroup bundleGroup = this.tree.getBundleGroup();
        Collection<KeyTreeItem> items = new ArrayList<>();
        items.add(selectedItem);
        items.addAll(selectedItem.getNestedChildren());
        for (Iterator<KeyTreeItem> iter = items.iterator(); iter.hasNext();) {
            KeyTreeItem item = iter.next();
            bundleGroup.commentKey(item.getId());
        }

    }


    /**
     * Copies a key or group of key.
     */
    protected void copyKeyOrGroup() {
        KeyTreeItem selectedItem = this.getSelection();
        String key = selectedItem.getId();
        String msgHead = null;
        String msgBody = null;
        if (selectedItem.getChildren().size() == 0) {
            msgHead = RBEPlugin.getString("dialog.duplicate.head.single");
            msgBody = RBEPlugin.getString("dialog.duplicate.body.single", key);
        } else {
            msgHead = RBEPlugin.getString("dialog.duplicate.head.multiple");
            msgBody = RBEPlugin.getString("dialog.duplicate.body.multiple", selectedItem.getName());
        }
        // Rename single item
        InputDialog dialog = new InputDialog(this.getShell(), msgHead, msgBody, key, null);
        dialog.open();
        if (dialog.getReturnCode() == Window.OK) {
            String newKey = dialog.getValue();
            BundleGroup bundleGroup = this.tree.getBundleGroup();
            Collection<KeyTreeItem> items = new ArrayList<>();
            items.add(selectedItem);
            items.addAll(selectedItem.getNestedChildren());
            for (Iterator<KeyTreeItem> iter = items.iterator(); iter.hasNext();) {
                KeyTreeItem item = iter.next();
                String origItemKey = item.getId();
                if (origItemKey.startsWith(key)) {
                    String newItemKey = newKey + origItemKey.substring(key.length());
                    bundleGroup.copyKey(origItemKey, newItemKey);
                }
            }
        }
    }


    /**
     * Copies a key or group of key.
     */
    protected void copyKeyID() {
        KeyTreeItem selectedItem = this.getSelection();
        String key = selectedItem.getId();

        StringSelection selectKey = new StringSelection(key);
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(selectKey, selectKey);

    }


    /**
     * Returns the currently used Shell instance.
     *
     * @return   The currently used Shell instance.
     */
    private Shell getShell() {
        return (RBEPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell());
    }


    /**
     * Modifies the current filter according to a selected activity.
     *
     * @param action     One of the KT_??? constants declared above.
     * @param activate   true <=> Enable this activity.
     */
    public void update(int action, boolean activate) {
        this.treeviewer.getTree().setCursor(this.waitcursor);
        if (action == TreeViewerContributor.KT_INCOMPLETE) {
            if (activate) {
                // we're setting a filter which uses the structural updater
                this.tree.setUpdater(new IncompletionUpdater(this.tree.getBundleGroup(), this.structuralupdater));
                this.mode = this.mode | TreeViewerContributor.KT_INCOMPLETE;
            } else {
                // disabled, so we can reuse the structural updater
                this.tree.setUpdater(this.structuralupdater);
                this.mode = this.mode & (~TreeViewerContributor.KT_INCOMPLETE);
            }
            if (this.structuralupdater instanceof GroupedKeyTreeUpdater) {
                if (RBEPreferences.getKeyTreeExpanded()) {
                    this.treeviewer.expandAll();
                }
            }
        } else if (action == TreeViewerContributor.KT_FLAT) {
            this.structuralupdater = new FlatKeyTreeUpdater();
            if ((this.mode & TreeViewerContributor.KT_INCOMPLETE) != 0) {
                // we need to activate the filter
                this.tree.setUpdater(new IncompletionUpdater(this.tree.getBundleGroup(), this.structuralupdater));
            } else {
                this.tree.setUpdater(this.structuralupdater);
            }
            this.mode = this.mode & (~TreeViewerContributor.KT_HIERARCHICAL);
        } else if (action == TreeViewerContributor.KT_HIERARCHICAL) {
            this.structuralupdater = new GroupedKeyTreeUpdater(RBEPreferences.getKeyGroupSeparator());
            if ((this.mode & TreeViewerContributor.KT_INCOMPLETE) != 0) {
                // we need to activate the filter
                this.tree.setUpdater(new IncompletionUpdater(this.tree.getBundleGroup(), this.structuralupdater));
            } else {
                this.tree.setUpdater(this.structuralupdater);
            }
            if (RBEPreferences.getKeyTreeExpanded()) {
                this.treeviewer.expandAll();
            }
            this.mode = this.mode | TreeViewerContributor.KT_HIERARCHICAL;
        }

        this.treeviewer.getTree().setCursor(this.defaultcursor);
    }


    /**
     * Returns the currently used mode.
     * @return   The currently used mode.
     */
    public int getMode() {
        return (this.mode);
    }
}
