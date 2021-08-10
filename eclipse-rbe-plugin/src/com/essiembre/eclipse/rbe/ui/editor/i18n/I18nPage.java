/*
 * Copyright (C) 2003-2014 Pascal Essiembre Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.essiembre.eclipse.rbe.ui.editor.i18n;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.essiembre.eclipse.rbe.model.DeltaEvent;
import com.essiembre.eclipse.rbe.model.IDeltaListener;
import com.essiembre.eclipse.rbe.model.bundle.Bundle;
import com.essiembre.eclipse.rbe.model.bundle.BundleEntry;
import com.essiembre.eclipse.rbe.model.bundle.BundleGroup;
import com.essiembre.eclipse.rbe.model.tree.KeyTree;
import com.essiembre.eclipse.rbe.model.tree.KeyTreeItem;
import com.essiembre.eclipse.rbe.model.workbench.RBEPreferences;
import com.essiembre.eclipse.rbe.translators.ITranslator;
import com.essiembre.eclipse.rbe.translators.TranslatorHandler;
import com.essiembre.eclipse.rbe.translators.TranslatorHandler.TranslatorType;
import com.essiembre.eclipse.rbe.ui.editor.i18n.tree.KeyTreeComposite;
import com.essiembre.eclipse.rbe.ui.editor.resources.ResourceManager;

/**
 * Internationalization page where one can edit all resource bundle entries at
 * once for all supported locales.
 *
 * @author Pascal Essiembre
 * @author cuhiodtick
 */
public class I18nPage extends ScrolledComposite {

    private final ResourceManager            resourceMediator;
    private final KeyTreeComposite           keysComposite;
    private final List<BundleEntryComposite> entryComposites = new ArrayList<>();

    private final LocalBehaviour             localBehaviour  = new LocalBehaviour();
    private final ScrolledComposite          editingComposite;

    /* default */BundleEntryComposite        activeEntry;
    /* default */BundleEntryComposite        lastActiveEntry;

    private AutoMouseWheelAdapter            _autoMouseWheelAdapter;
    // boolean _autoAdjustNeeded;
    private Composite                        _rightComposite;

    private TranslatorType                   translator;

    /**
     * Constructor.
     *
     * @param parent
     *            parent component.
     * @param style
     *            style to apply to this component
     * @param resourceMediator
     *            resource manager
     */
    public I18nPage(Composite parent, int style, final ResourceManager resourceMediator) {
        super(parent, style);
        this.resourceMediator = resourceMediator;

        if (RBEPreferences.getNoTreeInEditor()) {
            this.keysComposite = null;
            this.editingComposite = this;
            this.createEditingPart(this);
        } else {
            // Create screen
            SashForm sashForm = new SashForm(this, SWT.NONE);

            this.setContent(sashForm);

            this.keysComposite = new KeyTreeComposite(sashForm, resourceMediator.getKeyTree());
            this.keysComposite.getTreeViewer().addSelectionChangedListener(this.localBehaviour);

            this.editingComposite = new ScrolledComposite(sashForm, SWT.V_SCROLL | SWT.H_SCROLL);
            this.editingComposite.getVerticalBar().setIncrement(10);
            this.editingComposite.getVerticalBar().setPageIncrement(100);
            this.editingComposite.setShowFocusedControl(true);
            this.createSashRightSide();

            sashForm.setWeights(new int[] { 25, 75 });

        }

        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.setMinWidth(400);
        // setMinHeight(600);

        resourceMediator.getKeyTree().addListener(this.localBehaviour);

        this._autoMouseWheelAdapter = new AutoMouseWheelAdapter(parent);

        // if (RBEPreferences.getAutoAdjust()) {
        // // performance optimization: we only auto-adjust every 50 ms
        // getShell().getDisplay().timerExec(50, new Runnable() {
        //
        // @Override
        // public void run() {
        // if (_autoAdjustNeeded) {
        // _autoAdjustNeeded = false;
        // Point newMinSize = _rightComposite.computeSize(
        // editingComposite.getClientArea().width,
        // SWT.DEFAULT);
        // editingComposite.setMinSize(newMinSize);
        // editingComposite.layout();
        // }
        //
        // if (!isDisposed())
        // getShell().getDisplay().timerExec(50, this);
        // }
        // });
        // }

        this.translator = TranslatorType.GOOGLE_TRANSLATOR;
    }


    /**
     * Gets selected key.
     *
     * @return selected key
     */
    private String getSelectedKey() {
        return (this.resourceMediator.getKeyTree().getSelectedKey());
    }


    /**
     * Creates right side of main sash form.
     *
     * @param sashForm
     *            parent sash form
     */
    private void createSashRightSide() {
        this.editingComposite.setExpandHorizontal(true);
        this.editingComposite.setExpandVertical(true);
        this.editingComposite.setSize(SWT.DEFAULT, 100);
        this.createEditingPart(this.editingComposite);
    }


    /**
     * Creates the editing parts which are display within the supplied parental
     * ScrolledComposite instance.
     *
     * @param parent
     *            A container to collect the bundle entry editors.
     */
    private void createEditingPart(ScrolledComposite parent) {
        Control[] children = parent.getChildren();
        for (int i = 0; i < children.length; i++) {
            children[i].dispose();
        }
        this._rightComposite = new Composite(parent, SWT.BORDER);
        parent.setContent(this._rightComposite);
        // if (!RBEPreferences.getAutoAdjust()) {
        parent.setMinSize(this._rightComposite.computeSize(SWT.DEFAULT, this.resourceMediator.getLocales().size() * RBEPreferences.getMinHeight()));
        // }
        this._rightComposite.setLayout(new GridLayout(1, false));
        this.entryComposites.clear();
        for (Iterator<Locale> iter = this.resourceMediator.getLocales().iterator(); iter.hasNext();) {
            Locale locale = iter.next();
            BundleEntryComposite entryComposite = new BundleEntryComposite(this._rightComposite, this.resourceMediator, locale, this);
            entryComposite.addFocusListener(this.localBehaviour);
            this.entryComposites.add(entryComposite);
        }
    }


    /**
     * This method focusses the {@link BundleEntryComposite} corresponding to
     * the given {@link Locale}. If no such composite exists or the locale is
     * null, nothing happens.
     *
     * @param locale
     *            The locale whose {@link BundleEntryComposite} is to be
     *            focussed.
     */
    public void focusBundleEntryComposite(Locale locale) {
        for (BundleEntryComposite bec : this.entryComposites) {
            if ((bec.getLocale() == null) && (locale == null) || (locale != null && locale.equals(bec.getLocale()))) {
                bec.focusTextBox();
            }
        }
    }


    /**
     * Focusses the next {@link BundleEntryComposite}.
     */
    public void focusNextBundleEntryComposite() {
        int index = this.entryComposites.indexOf(this.activeEntry);
        BundleEntryComposite nextComposite;
        if (index < this.entryComposites.size() - 1) {
            nextComposite = this.entryComposites.get(++index);
        } else {
            nextComposite = this.entryComposites.get(0);
        }

        if (nextComposite != null) {
            this.focusComposite(nextComposite);
        }
    }


    /**
     * Focusses the previous {@link BundleEntryComposite}.
     */
    public void focusPreviousBundleEntryComposite() {
        int index = this.entryComposites.indexOf(this.activeEntry);
        BundleEntryComposite nextComposite;
        if (index > 0) {
            nextComposite = this.entryComposites.get(--index);
        } else {
            nextComposite = this.entryComposites.get(this.entryComposites.size() - 1);
        }

        if (nextComposite != null) {
            this.focusComposite(nextComposite);
        }
    }


    /**
     * Focusses the given {@link BundleEntryComposite} and scrolls the
     * surrounding {@link ScrolledComposite} in order to make it visible.
     *
     * @param comp
     *            The {@link BundleEntryComposite} to be focussed.
     */
    private void focusComposite(BundleEntryComposite comp) {
        Point compPos = comp.getLocation();
        Point compSize = comp.getSize();
        Point size = this.editingComposite.getSize();
        Point origin = this.editingComposite.getOrigin();
        if (compPos.y + compSize.y > size.y + origin.y) {
            this.editingComposite.setOrigin(origin.x, origin.y + (compPos.y + compSize.y) - (origin.y + size.y) + 5);
        } else if (compPos.y < origin.y) {
            this.editingComposite.setOrigin(origin.x, compPos.y);
        }
        comp.focusTextBox();
    }


    public IFindReplaceTarget getReplaceTarget() {
        return new FindReplaceTarget();
    }


    /**
     * Selects the next entry in the {@link KeyTree}.
     */
    public void selectNextTreeEntry() {
        this.activeEntry.updateBundleOnChanges();
        String nextKey = this.resourceMediator.getBundleGroup().getNextKey(this.getSelectedKey());
        if (nextKey == null) {
            return;
        }

        Locale currentLocale = this.activeEntry.getLocale();
        this.resourceMediator.getKeyTree().selectKey(nextKey);
        this.focusBundleEntryComposite(currentLocale);
    }


    /**
     * Selects the previous entry in the {@link KeyTree}.
     */
    public void selectPreviousTreeEntry() {
        this.activeEntry.updateBundleOnChanges();
        String prevKey = this.resourceMediator.getBundleGroup().getPreviousKey(this.getSelectedKey());
        if (prevKey == null) {
            return;
        }

        Locale currentLocale = this.activeEntry.getLocale();
        this.resourceMediator.getKeyTree().selectKey(prevKey);
        this.focusBundleEntryComposite(currentLocale);
    }


    /**
     * Refreshes the editor associated with the active text box (if any) if it
     * has changed.
     */
    public void refreshEditorOnChanges() {
        if (this.activeEntry != null) {
            this.activeEntry.updateBundleOnChanges();
        }
    }


    /**
     * Refreshes all value-holding text boxes in this page.
     */
    public void refreshTextBoxes() {
        String key = this.getSelectedKey();
        for (Iterator<BundleEntryComposite> iter = this.entryComposites.iterator(); iter.hasNext();) {
            BundleEntryComposite entryComposite = iter.next();
            entryComposite.refresh(key);
        }
    }


    /**
     * Refreshes the tree and recreates the editing part.
     */
    public void refreshPage() {
        if (this.keysComposite != null) {
            this.keysComposite.getTreeViewer().refresh(true);
        }
        this.createEditingPart(this.editingComposite);
        this.editingComposite.layout(true, true);
    }


    /**
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if (this.keysComposite != null) {
            this.keysComposite.dispose();
        }
        for (Iterator<BundleEntryComposite> iter = this.entryComposites.iterator(); iter.hasNext();) {
            iter.next().dispose();
        }
        this._autoMouseWheelAdapter.dispose();
        super.dispose();
    }


    // void setAutoAdjustNeeded(boolean b) {
    // _autoAdjustNeeded = b;
    // }
    //
    void findActionStart() {
        if (!this.keysComposite.getFilter().isEmpty()) {
            this.keysComposite.setFilter("");
        }
    }

    /**
     * Implementation of custom behaviour.
     */
    private class LocalBehaviour implements FocusListener, IDeltaListener, ISelectionChangedListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void focusGained(FocusEvent event) {
            I18nPage.this.activeEntry = (BundleEntryComposite) event.widget;
            I18nPage.this.lastActiveEntry = I18nPage.this.activeEntry;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void focusLost(FocusEvent event) {
            I18nPage.this.activeEntry = null;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            I18nPage.this.refreshTextBoxes();
            String selected = I18nPage.this.getSelectedKey();
            if (selected != null) {
                I18nPage.this.resourceMediator.getKeyTree().selectKey(selected);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void add(DeltaEvent event) {}


        /**
         * {@inheritDoc}
         */
        @Override
        public void remove(DeltaEvent event) {}


        /**
         * {@inheritDoc}
         */
        @Override
        public void modify(DeltaEvent event) {}


        /**
         * {@inheritDoc}
         */
        @Override
        public void select(DeltaEvent event) {
            KeyTreeItem item = (KeyTreeItem) event.receiver();
            if (I18nPage.this.keysComposite != null) {
                if (item != null) {
                    I18nPage.this.keysComposite.getTreeViewer().setSelection(new StructuredSelection(item));
                }
            } else {
                I18nPage.this.refreshTextBoxes();
            }
        }

    } /* ENDCLASS */

    private class FindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension3 {

        @Override
        public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord) {
            // replaced by findAndSelect(.,.)
            return -1;
        }


        @Override
        public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch) {
            if (I18nPage.this.lastActiveEntry != null) {
                StyledText textWidget = I18nPage.this.lastActiveEntry.getTextViewer().getTextWidget();
                String text = textWidget.getText();
                IRegion region = this.find(text, findString, textWidget.getSelection().x + (searchForward ? 1 : -1), searchForward, caseSensitive, wholeWord, regExSearch);
                if (region != null) {
                    I18nPage.this.focusBundleEntryComposite(I18nPage.this.lastActiveEntry.locale);
                    textWidget.setSelection(region.getOffset(), region.getOffset() + region.getLength());
                    return region.getOffset();
                }
            }
            BundleGroup bundleGroup = I18nPage.this.resourceMediator.getBundleGroup();
            ArrayList<String> keys = new ArrayList<String>(bundleGroup.getKeys());
            String activeKey = I18nPage.this.lastActiveEntry != null ? I18nPage.this.lastActiveEntry.activeKey : keys.get(0);
            int activeKeyIndex = Math.max(keys.indexOf(activeKey), 0);

            List<Locale> locales = I18nPage.this.resourceMediator.getLocales();
            Locale activeLocale = I18nPage.this.lastActiveEntry != null ? I18nPage.this.lastActiveEntry.locale : locales.get(0);
            int activeLocaleIndex = locales.indexOf(activeLocale) + (searchForward ? 1 : -1);

            for (int i = 0, length = keys.size(); i < length; i++) {
                String key = keys.get((activeKeyIndex + (searchForward ? i : -i)) % length);
                int j = (i == 0 ? activeLocaleIndex : (searchForward ? 0 : locales.size() - 1));
                while (j < locales.size() && j >= 0) {
                    Locale locale = locales.get(j);
                    Bundle bundle = bundleGroup.getBundle(locale);
                    BundleEntry value = bundle.getEntry(key);
                    if (value != null && value.getValue() != null) {
                        IRegion region = this.find(value.getValue(), findString, searchForward ? 0 : value.getValue().length() - 1, searchForward, caseSensitive, wholeWord, regExSearch);
                        if (region != null) {
                            I18nPage.this.keysComposite.selectKeyTreeItem(key);
                            I18nPage.this.focusBundleEntryComposite(locale);
                            StyledText textWidget = I18nPage.this.activeEntry.getTextViewer().getTextWidget();
                            textWidget.setSelection(region.getOffset(), region.getOffset() + region.getLength());
                            return region.getOffset();
                        }
                    }
                    if (searchForward) {
                        ++j;
                    } else {
                        --j;
                    }
                }
            }
            return -1;
        }


        private IRegion find(String text, String findString, int offset, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch) {
            Document document = new Document(text);
            FindReplaceDocumentAdapter documentAdapter = new FindReplaceDocumentAdapter(document);
            try {
                return documentAdapter.find(offset, findString, searchForward, caseSensitive, wholeWord, regExSearch);
            } catch (BadLocationException argh) {
                return null;
            }
        }


        @Override
        public void replaceSelection(String text) {
            // replaced by replaceSelection(.,.)
        }


        @Override
        public void replaceSelection(String text, boolean regExReplace) {

        }


        @Override
        public boolean isEditable() {
            return false;
        }


        @Override
        public String getSelectionText() {
            return I18nPage.this.activeEntry != null ? I18nPage.this.activeEntry.getTextViewer().getTextWidget().getSelectionText() : "";
        }


        @Override
        public Point getSelection() {
            return I18nPage.this.activeEntry != null ? I18nPage.this.activeEntry.getTextViewer().getSelectedRange() : new Point(0, 0);
        }


        @Override
        public boolean canPerformFind() {
            return true;
        }

    }

    public ITranslator getTranslator() {
        return TranslatorHandler.getTranslator(this.translator);
    }


    public List<BundleEntryComposite> getEntryComposites() {
        return this.entryComposites;
    }
}
