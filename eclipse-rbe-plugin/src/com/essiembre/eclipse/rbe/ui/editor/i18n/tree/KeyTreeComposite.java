/*
 * Copyright (C) 2003-2014 Pascal Essiembre Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.essiembre.eclipse.rbe.ui.editor.i18n.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.essiembre.eclipse.rbe.RBEPlugin;
import com.essiembre.eclipse.rbe.model.bundle.BundleGroup;
import com.essiembre.eclipse.rbe.model.tree.KeyTree;
import com.essiembre.eclipse.rbe.model.tree.KeyTreeItem;
import com.essiembre.eclipse.rbe.model.tree.updater.FlatKeyTreeUpdater;
import com.essiembre.eclipse.rbe.model.tree.updater.GroupedKeyTreeUpdater;
import com.essiembre.eclipse.rbe.model.tree.visitors.KeysStartingWithVisitor;
import com.essiembre.eclipse.rbe.model.workbench.RBEPreferences;
import com.essiembre.eclipse.rbe.translators.TranslatorHandler;
import com.essiembre.eclipse.rbe.ui.UIUtils;
import com.essiembre.eclipse.rbe.ui.editor.i18n.I18nPage;

/**
 * Tree for displaying and navigating through resource bundle keys.
 * @author Pascal Essiembre
 * @author cuhiodtick
 */
public class KeyTreeComposite extends Composite {

    /** Image for tree mode toggle button. */
    private Image                  treeToggleImage;
    /** Image for flat mode toggle button. */
    private Image                  flatToggleImage;
    private Image                  translateToggleImage;

    /* default */ Cursor           waitCursor;
    /* default */ Cursor           defaultCursor;

    /** Key Tree Viewer. */
    /* default */ TreeViewer       treeViewer;
    /** TreeViewer label provider. */
    protected KeyTreeLabelProvider labelProvider;

    /** Flat or Tree mode? */
    private boolean                keyTreeHierarchical = RBEPreferences.getKeyTreeHierarchical();

    /** Text box to add a new key. */
    /* default */ Text             addTextBox;

    /** Key tree. */
    /* default */ KeyTree          keyTree;

    /** Whether to synchronize the add text box with tree key selection. */
    /* default */ boolean          syncAddTextBox      = true;

    /** Contributes menu items to the tree viewer. */
    private TreeViewerContributor  treeviewerContributor;

    private Text                   filterTextBox;
    protected I18nPage             page;

    /**
     * Constructor.
     * @param parent parent composite
     * @param keyTree key tree
     */
    public KeyTreeComposite(Composite parent, final KeyTree keyTree, I18nPage page) {
        super(parent, SWT.BORDER);
        this.keyTree = keyTree;
        this.page = page;
        this.treeToggleImage = UIUtils.getImage(UIUtils.IMAGE_LAYOUT_HIERARCHICAL);
        this.flatToggleImage = UIUtils.getImage(UIUtils.IMAGE_LAYOUT_FLAT);
        this.translateToggleImage = UIUtils.getImage(UIUtils.IMAGE_LAYOUT_TRANSLATOR);
        this.waitCursor = UIUtils.createCursor(SWT.CURSOR_WAIT);
        this.defaultCursor = UIUtils.createCursor(SWT.CURSOR_ARROW);

        this.setLayout(new GridLayout(1, false));
        this.createTopSection();
        this.createMiddleSection();
        this.createBottomSection();
    }


    /**
     * Gets the tree viewer.
     * @return tree viewer
     */
    public TreeViewer getTreeViewer() {
        return this.treeViewer;
    }


    public void setFilter(String filter) {
        this.filterTextBox.setText(filter);
    }


    public String getFilter() {
        return this.filterTextBox.getText();
    }


    /**
     * Gets the selected key tree item.
     * @return key tree item
     */
    public KeyTreeItem getSelection() {
        IStructuredSelection selection = (IStructuredSelection) this.treeViewer.getSelection();
        return (KeyTreeItem) selection.getFirstElement();
    }


    /**
     * Gets selected key.
     * @return selected key
     */
    public String getSelectedKey() {
        String key = null;
        KeyTreeItem item = this.getSelection();
        if (item != null) {
            key = item.getId();
        }
        return key;
    }


    /**
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();

        this.waitCursor.dispose();
        this.defaultCursor.dispose();
        // treeviewerContributor.dispose();
        this.labelProvider.dispose();
        this.addTextBox.dispose();

        this.keyTree = null;
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
            BundleGroup bundleGroup = this.keyTree.getBundleGroup();
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
     * Creates the top section (toggle buttons) of this composite.
     */
    private void createTopSection() {
        Composite topComposite = new Composite(this, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        topComposite.setLayout(gridLayout);
        topComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.filterTextBox = new Text(topComposite, SWT.BORDER);
        // filterTextBox.setText("");
        this.filterTextBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.filterTextBox.addModifyListener(this.getFilterTextBoxModifyListener());

        Composite topRightComposite = new Composite(topComposite, SWT.NONE);
        // ToolBar topRightComposite = new ToolBar(topComposite, SWT.NONE);
        gridLayout = new GridLayout(3, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        topRightComposite.setLayout(gridLayout);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalAlignment = GridData.END;
        // gridData.verticalAlignment = GridData.CENTER;
        // gridData.grabExcessHorizontalSpace = true;
        topRightComposite.setLayoutData(gridData);

        final Button hierModeButton = new Button(topRightComposite, SWT.TOGGLE);
        hierModeButton.setImage(this.treeToggleImage);
        hierModeButton.setToolTipText(RBEPlugin.getString("key.layout.tree"));

        final Button flatModeButton = new Button(topRightComposite, SWT.TOGGLE);
        flatModeButton.setImage(this.flatToggleImage);
        flatModeButton.setToolTipText(RBEPlugin.getString("key.layout.flat"));

        if (this.keyTreeHierarchical) {
            hierModeButton.setSelection(true);
            hierModeButton.setEnabled(false);
        } else {
            flatModeButton.setSelection(true);
            flatModeButton.setEnabled(false);
        }

        // TODO merge the two listeners into one
        hierModeButton.addSelectionListener(this.getHierButtonSelectionListener(hierModeButton, flatModeButton));
        flatModeButton.addSelectionListener(this.getFlatButtonSelectionListener(hierModeButton, flatModeButton));

        final ToolBar toolBar = new ToolBar(topComposite, SWT.NONE);

        final ToolItem translatorButton = new ToolItem(toolBar, SWT.DROP_DOWN);
        translatorButton.setImage(this.translateToggleImage);
        translatorButton.setToolTipText(RBEPlugin.getString("key.layout.translator"));
        final Menu menu = new Menu(translatorButton.getParent().getShell());
        MenuItem menuItemGoogle = new MenuItem(menu, SWT.PUSH);
        menuItemGoogle.setText(TranslatorHandler.TranslatorType.GOOGLE_TRANSLATOR.getText());
        menuItemGoogle.addSelectionListener(new TranslatorMenuSelectionAdapter(TranslatorHandler.TranslatorType.GOOGLE_TRANSLATOR));
        MenuItem menuItemOpenTrad = new MenuItem(menu, SWT.PUSH);
        menuItemOpenTrad.setText(TranslatorHandler.TranslatorType.OPENTRAD_TRANSLATOR.getText());
        menuItemOpenTrad.addSelectionListener(new TranslatorMenuSelectionAdapter(TranslatorHandler.TranslatorType.OPENTRAD_TRANSLATOR));
        translatorButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (event.detail == SWT.ARROW) {
                    Rectangle rect = translatorButton.getBounds();
                    Point pt = new Point(rect.x - (rect.width * 3), rect.y + rect.height);
                    pt = toolBar.toDisplay(pt);
                    menu.setLocation(pt.x, pt.y);
                    menu.setVisible(true);
                }
            }
        });

    }

    class TranslatorMenuSelectionAdapter extends SelectionAdapter {

        TranslatorHandler.TranslatorType translatorType;

        public TranslatorMenuSelectionAdapter(TranslatorHandler.TranslatorType translatorType) {
            this.translatorType = translatorType;
        }


        @Override
        public void widgetSelected(SelectionEvent event) {
            KeyTreeComposite.this.page.setTranslator(this.translatorType);
        }
    }

    private ModifyListener getFilterTextBoxModifyListener() {
        return new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                KeyTreeComposite.this.keyTree.filterKeyItems(KeyTreeComposite.this.filterTextBox.getText());
                KeyTreeComposite.this.treeViewer.getControl().setRedraw(false);
                KeyTreeComposite.this.treeViewer.refresh();
                if (!KeyTreeComposite.this.filterTextBox.getText().isEmpty()) {
                    KeyTreeComposite.this.treeViewer.expandAll();
                }
                KeyTreeComposite.this.treeViewer.getControl().setRedraw(true);
            }
        };
    }


    private SelectionAdapter getFlatButtonSelectionListener(final Button hierModeButton, final Button flatModeButton) {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (flatModeButton.getSelection()) {
                    hierModeButton.setSelection(false);
                    hierModeButton.setEnabled(true);
                    flatModeButton.setEnabled(false);
                    KeyTreeComposite.this.setCursor(KeyTreeComposite.this.waitCursor);
                    KeyTreeComposite.this.setVisible(false);
                    KeyTreeComposite.this.keyTree.setUpdater(new FlatKeyTreeUpdater());
                    // treeviewerContributor.getMenuItem(
                    // TreeViewerContributor.MENU_EXPAND).setEnabled(false);
                    // treeviewerContributor.getMenuItem(
                    // TreeViewerContributor.MENU_COLLAPSE).setEnabled(false);
                    KeyTreeComposite.this.selectKeyTreeItem(KeyTreeComposite.this.addTextBox.getText());
                    KeyTreeComposite.this.setVisible(true);
                    KeyTreeComposite.this.setCursor(KeyTreeComposite.this.defaultCursor);
                }
            }
        };
    }


    private SelectionAdapter getHierButtonSelectionListener(final Button hierModeButton, final Button flatModeButton) {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (hierModeButton.getSelection()) {
                    flatModeButton.setSelection(false);
                    flatModeButton.setEnabled(true);
                    hierModeButton.setEnabled(false);
                    KeyTreeComposite.this.setCursor(KeyTreeComposite.this.waitCursor);
                    KeyTreeComposite.this.setVisible(false);
                    KeyTreeComposite.this.keyTree.setUpdater(new GroupedKeyTreeUpdater(RBEPreferences.getKeyGroupSeparator()));
                    // treeviewerContributor.getMenuItem(
                    // TreeViewerContributor.MENU_EXPAND).setEnabled(true);
                    // treeviewerContributor.getMenuItem(
                    // TreeViewerContributor.MENU_COLLAPSE).setEnabled(true);
                    if (RBEPreferences.getKeyTreeExpanded()) {
                        KeyTreeComposite.this.treeViewer.getControl().setRedraw(false);
                        KeyTreeComposite.this.treeViewer.expandAll();
                        KeyTreeComposite.this.treeViewer.getControl().setRedraw(true);
                    }
                    KeyTreeComposite.this.selectKeyTreeItem(KeyTreeComposite.this.addTextBox.getText());
                    KeyTreeComposite.this.setVisible(true);
                    KeyTreeComposite.this.setCursor(KeyTreeComposite.this.defaultCursor);
                }
            }
        };
    }


    /**
     * Creates the middle (tree) section of this composite.
     */
    private void createMiddleSection() {

        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;

        this.treeViewer = new TreeViewer(this, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        this.treeViewer.setContentProvider(new KeyTreeContentProvider());
        this.labelProvider = new KeyTreeLabelProvider();
        this.treeViewer.setLabelProvider(this.labelProvider);
        this.treeViewer.setUseHashlookup(true);
        this.treeViewer.setInput(this.keyTree);
        if (RBEPreferences.getKeyTreeExpanded()) {
            this.treeViewer.expandAll();
        }
        this.treeViewer.getTree().setLayoutData(gridData);
        this.treeViewer.getTree().addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent event) {
                if (event.character == SWT.DEL) {
                    KeyTreeComposite.this.deleteKeyOrGroup();
                }
            }
        });
        this.treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if (KeyTreeComposite.this.syncAddTextBox && KeyTreeComposite.this.getSelectedKey() != null) {
                    KeyTreeComposite.this.addTextBox.setText(KeyTreeComposite.this.getSelectedKey());
                    KeyTreeComposite.this.keyTree.selectKey(KeyTreeComposite.this.getSelectedKey());
                }
                KeyTreeComposite.this.syncAddTextBox = true;
            }
        });
        this.treeViewer.getTree().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(MouseEvent event) {
                Object element = KeyTreeComposite.this.getSelection();
                if (KeyTreeComposite.this.treeViewer.isExpandable(element)) {
                    if (KeyTreeComposite.this.treeViewer.getExpandedState(element)) {
                        KeyTreeComposite.this.treeViewer.collapseToLevel(element, 1);
                    } else {
                        KeyTreeComposite.this.treeViewer.expandToLevel(element, 1);
                    }
                }
            }
        });

        ViewerFilter filter = new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                // if (parentElement instanceof KeyTreeItem) {
                // KeyTreeItem parent = (KeyTreeItem) parentElement;
                // if (parent.isSelected())
                // return true;
                // }
                if (element instanceof KeyTreeItem) {
                    KeyTreeItem item = (KeyTreeItem) element;
                    return item.isVisible();
                }
                return true;
                // String text = filterTextBox.getText();
                // if (element instanceof KeyTreeItem) {
                // KeyTreeItem item = (KeyTreeItem) element;
                // if (item.getId().indexOf(text) != -1)
                // return true;
                // }
                // return true;
            }
        };
        this.treeViewer.addFilter(filter);

        this.treeviewerContributor = new TreeViewerContributor(this.keyTree, this.treeViewer);
        this.treeviewerContributor.createControl(this);

    }


    /**
     * Creates the botton section (add field/button) of this composite.
     */
    private void createBottomSection() {
        Composite bottomComposite = new Composite(this, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        bottomComposite.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.verticalAlignment = GridData.CENTER;
        gridData.grabExcessHorizontalSpace = true;
        bottomComposite.setLayoutData(gridData);

        // Text box
        this.addTextBox = new Text(bottomComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        this.addTextBox.setLayoutData(gridData);

        // Add button
        final Button addButton = new Button(bottomComposite, SWT.PUSH);
        addButton.setText(RBEPlugin.getString("key.add"));
        addButton.setEnabled(false);
        addButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                KeyTreeComposite.this.addKey();
            }
        });

        this.addTextBox.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent event) {
                if (event.character == SWT.CR && addButton.isEnabled()) {
                    KeyTreeComposite.this.addKey();
                }
            }
        });
        this.addTextBox.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent event) {
                String key = KeyTreeComposite.this.addTextBox.getText();
                boolean keyExist = KeyTreeComposite.this.keyTree.getBundleGroup().isKey(key);
                if (keyExist || key.length() == 0) {
                    addButton.setEnabled(false);
                } else {
                    addButton.setEnabled(true);
                }
                if (key.length() > 0 && !key.equals(KeyTreeComposite.this.getSelectedKey())) {
                    KeysStartingWithVisitor visitor = new KeysStartingWithVisitor();
                    KeyTreeComposite.this.keyTree.accept(visitor, key);
                    KeyTreeItem item = visitor.getKeyTreeItem();
                    if (item != null) {
                        KeyTreeComposite.this.syncAddTextBox = false;
                        KeyTreeComposite.this.selectKeyTreeItem(item);

                        if (key.equals(KeyTreeComposite.this.getSelectedKey())) {
                            KeyTreeComposite.this.keyTree.selectKey(KeyTreeComposite.this.getSelectedKey());
                        }
                    }
                }
            }
        });
    }


    /**
     * Adds a key to the tree, based on content from add field.
     */
    /* default */ void addKey() {
        String key = this.addTextBox.getText();
        this.keyTree.getBundleGroup().addKey(key);
        this.selectKeyTreeItem(key);
    }


    /**
     * Selected the key tree item matching given key.
     * @param key key to select
     */
    public void selectKeyTreeItem(String key) {
        this.selectKeyTreeItem(this.keyTree.getKeyTreeItem(key));
    }


    /**
     * Selected the key tree item matching given key tree item.
     * @param item key tree item to select
     */
    /* default */ void selectKeyTreeItem(KeyTreeItem item) {
        if (item != null) {
            this.treeViewer.setSelection(new StructuredSelection(item), true);
        }
    }

    // public KeyTreeItem getNextKeyTreeItem() {
    // // Either find the next sibbling
    // KeyTreeItem currentItem = keyTree.getKeyTreeItem(keyTree.getSelectedKey());
    // return currentItem.getNextLeaf();
    // }
}
