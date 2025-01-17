/*
 * Copyright (C) 2003-2014 Pascal Essiembre Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.essiembre.eclipse.rbe.ui.editor.i18n;

import java.awt.ComponentOrientation;
import java.awt.GraphicsEnvironment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.essiembre.eclipse.rbe.RBEPlugin;
import com.essiembre.eclipse.rbe.model.bundle.BundleEntry;
import com.essiembre.eclipse.rbe.model.bundle.BundleGroup;
import com.essiembre.eclipse.rbe.model.bundle.visitors.DuplicateValuesVisitor;
import com.essiembre.eclipse.rbe.model.bundle.visitors.SimilarValuesVisitor;
import com.essiembre.eclipse.rbe.model.utils.LevenshteinDistanceAnalyzer;
import com.essiembre.eclipse.rbe.model.utils.ProximityAnalyzer;
import com.essiembre.eclipse.rbe.model.utils.WordCountAnalyzer;
import com.essiembre.eclipse.rbe.model.workbench.RBEPreferences;
import com.essiembre.eclipse.rbe.translators.ITranslator;
import com.essiembre.eclipse.rbe.ui.UIUtils;
import com.essiembre.eclipse.rbe.ui.editor.ResourceBundleEditor;
import com.essiembre.eclipse.rbe.ui.editor.resources.ResourceManager;
import com.essiembre.eclipse.rbe.ui.editor.resources.SourceEditor;

/**
 * Represents a data entry section for a bundle entry.
 * @author Pascal Essiembre
 * @author cuhiodtick
 */
public class BundleEntryComposite extends Composite {

    /* default */ final ResourceManager  resourceManager;
    /* default */ final Locale           locale;
    /* default */ final I18nPage         page;
    private final Font                   boldFont;
    private final Font                   smallFont;

    // /*default*/ Text textBox;
    private ITextViewer                  textViewer;
    private IUndoManager                 undoManager;

    private Button                       commentedCheckbox;
    private Button                       gotoButton;
    private Button                       duplButton;
    private Button                       translateButton;
    private Button                       simButton;

    /* default */ String                 activeKey;
    /* default */ String                 textBeforeUpdate;

    /* default */ DuplicateValuesVisitor duplVisitor;
    /* default */ SimilarValuesVisitor   similarVisitor;

    private FocusListener                internalFocusListener = new FocusListener() {

                                                                   @Override
                                                                   public void focusGained(FocusEvent e) {
                                                                       e.widget = BundleEntryComposite.this;
                                                                       for (FocusListener listener : BundleEntryComposite.this.focusListeners) {
                                                                           listener.focusGained(e);
                                                                       }
                                                                   }


                                                                   @Override
                                                                   public void focusLost(FocusEvent e) {
                                                                       e.widget = BundleEntryComposite.this;
                                                                       for (FocusListener listener : BundleEntryComposite.this.focusListeners) {
                                                                           listener.focusLost(e);
                                                                           // textViewer.setSelectedRange(0, 0);
                                                                       }
                                                                   }
                                                               };

    /**
     * Constructor.
     * @param parent parent composite
     * @param resourceManager resource manager
     * @param locale locale for this bundle entry
     */
    public BundleEntryComposite(final Composite parent, final ResourceManager resourceManager, final Locale locale, final I18nPage page) {

        super(parent, SWT.NONE);
        this.resourceManager = resourceManager;
        this.locale = locale;
        this.page = page;

        this.boldFont = UIUtils.createFont(this, SWT.BOLD, 0);
        this.smallFont = UIUtils.createFont(SWT.NONE, -1);

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 2;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;

        this.createLabelRow();
        // createTextRow();
        this.createTextViewerRow();

        this.setLayout(gridLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        this.setLayoutData(gd);
    }


    /**
     * Update bundles if the value of the active key changed.
     */
    public void updateBundleOnChanges() {
        if (this.activeKey != null) {
            BundleGroup bundleGroup = this.resourceManager.getBundleGroup();
            BundleEntry entry = bundleGroup.getBundleEntry(this.locale, this.activeKey);
            boolean commentedSelected = this.commentedCheckbox.getSelection();
            String textBoxValue = this.textViewer.getDocument().get();

            if (entry == null || !textBoxValue.equals(entry.getValue()) || entry.isCommented() != commentedSelected) {
                String comment = null;
                if (entry != null) {
                    comment = entry.getComment();
                }
                bundleGroup.addBundleEntry(this.locale, new BundleEntry(this.activeKey, this.textViewer.getDocument().get(), comment, commentedSelected));
            }
        }
    }


    /**
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        this.boldFont.dispose();
        this.smallFont.dispose();

        // Addition by Eric Fettweis
        for (Iterator<Font> it = this.swtFontCache.values().iterator(); it.hasNext();) {
            Font font = it.next();
            font.dispose();
        }
        this.swtFontCache.clear();
    }


    /**
     * Gets the locale associated with this bundle entry
     * @return a locale
     */
    public Locale getLocale() {
        return this.locale;
    }


    /**
     * Sets a selection in the text box.
     * @param start starting position to select
     * @param end ending position to select
     */
    public void setTextSelection(int start, int end) {
        this.textViewer.setSelectedRange(start, end - start);
    }


    public ITextViewer getTextViewer() {
        return this.textViewer;
    }


    /**
     * Refreshes the text field value with value matching given key.
     * @param key key used to grab value
     */
    public void refresh(String key) {
        this.activeKey = key;
        BundleGroup bundleGroup = this.resourceManager.getBundleGroup();
        StyledText textBox = this.textViewer.getTextWidget();

        IDocument document = new Document();

        if (key != null && bundleGroup.isKey(key)) {
            BundleEntry bundleEntry = bundleGroup.getBundleEntry(this.locale, key);
            SourceEditor sourceEditor = this.resourceManager.getSourceEditor(this.locale);
            if (bundleEntry == null) {
                document.set("");
                this.commentedCheckbox.setSelection(false);
            } else {
                this.commentedCheckbox.setSelection(bundleEntry.isCommented());
                String value = bundleEntry.getValue();
                document.set(value);
            }
            this.commentedCheckbox.setEnabled(!sourceEditor.isReadOnly());
            textBox.setEnabled(!sourceEditor.isReadOnly());
            textBox.setEditable(true);
            // textBox.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
            this.gotoButton.setEnabled(true);
            if (RBEPreferences.getReportDuplicateValues()) {
                this.findDuplicates(bundleEntry);
            } else {
                this.duplVisitor = null;
            }
            if (RBEPreferences.getReportSimilarValues()) {
                this.findSimilar(bundleEntry);
            } else {
                this.similarVisitor = null;
            }
        } else {
            this.commentedCheckbox.setSelection(false);
            this.commentedCheckbox.setEnabled(false);
            document.set("");
            textBox.setEnabled(false);
            this.gotoButton.setEnabled(false);
            this.duplButton.setVisible(false);
            this.simButton.setVisible(false);
            textBox.setEditable(false);
            // textBox.setBackground(new Color(getDisplay(), 245, 245, 245));
        }

        this.textViewer.setDocument(document);
        this.resetCommented();
    }


    private void findSimilar(BundleEntry bundleEntry) {
        ProximityAnalyzer analyzer;
        if (RBEPreferences.getReportSimilarValuesLevensthein()) {
            analyzer = LevenshteinDistanceAnalyzer.getInstance();
        } else {
            analyzer = WordCountAnalyzer.getInstance();
        }
        BundleGroup bundleGroup = this.resourceManager.getBundleGroup();
        if (this.similarVisitor == null) {
            this.similarVisitor = new SimilarValuesVisitor();
        }
        this.similarVisitor.setProximityAnalyzer(analyzer);
        this.similarVisitor.clear();
        bundleGroup.getBundle(this.locale).accept(this.similarVisitor, bundleEntry);
        if (this.duplVisitor != null) {
            this.similarVisitor.getSimilars().removeAll(this.duplVisitor.getDuplicates());
        }
        this.simButton.setVisible(this.similarVisitor.getSimilars().size() > 0);
    }


    private void findDuplicates(BundleEntry bundleEntry) {
        BundleGroup bundleGroup = this.resourceManager.getBundleGroup();
        if (this.duplVisitor == null) {
            this.duplVisitor = new DuplicateValuesVisitor();
        }
        this.duplVisitor.clear();
        bundleGroup.getBundle(this.locale).accept(this.duplVisitor, bundleEntry);
        this.duplButton.setVisible(this.duplVisitor.getDuplicates().size() > 0);
    }


    /**
     * Creates the text field label, icon, and commented check box.
     */
    private void createLabelRow() {
        Composite labelComposite = new Composite(this, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 7;
        gridLayout.horizontalSpacing = 5;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        labelComposite.setLayout(gridLayout);
        labelComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Locale text
        Label txtLabel = new Label(labelComposite, SWT.NONE);
        txtLabel.setText(" " + UIUtils.getDisplayName(this.locale) + " ");
        txtLabel.setFont(this.boldFont);
        GridData gridData = new GridData();

        // Similar button
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.END;
        gridData.grabExcessHorizontalSpace = true;
        this.simButton = new Button(labelComposite, SWT.PUSH | SWT.FLAT);
        this.simButton.setImage(UIUtils.getImage("similar.gif"));
        this.simButton.setLayoutData(gridData);
        this.simButton.setVisible(false);
        this.simButton.setToolTipText(RBEPlugin.getString("value.similar.tooltip"));
        this.simButton.addSelectionListener(this.getSimButtonSelectionListener());

        // Duplicate button
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.END;
        this.duplButton = new Button(labelComposite, SWT.PUSH | SWT.FLAT);
        this.duplButton.setImage(UIUtils.getImage("duplicate.gif"));
        this.duplButton.setLayoutData(gridData);
        this.duplButton.setVisible(false);
        this.duplButton.setToolTipText(RBEPlugin.getString("value.duplicate.tooltip"));
        this.duplButton.addSelectionListener(this.getDuplButtonSelectionListener());

        gridData = new GridData();
        gridData.horizontalAlignment = GridData.END;
        // gridData.grabExcessHorizontalSpace = true;

        // Translate button
        this.translateButton = new Button(labelComposite, SWT.TOGGLE);
        this.translateButton.setImage(UIUtils.getImage(UIUtils.IMAGE_LAYOUT_TRANSLATOR));
        this.translateButton.setToolTipText(RBEPlugin.getString("key.layout.translate"));
        this.translateButton.addSelectionListener(this.getTranslateButtonSelectionListener());

        // Commented checkbox
        this.commentedCheckbox = new Button(labelComposite, SWT.CHECK);
        this.commentedCheckbox.setText("#");
        this.commentedCheckbox.setFont(this.smallFont);
        this.commentedCheckbox.setLayoutData(gridData);
        this.commentedCheckbox.addSelectionListener(this.getCommentedCheckboxSelectionListener());
        this.commentedCheckbox.setEnabled(false);

        // Country flag
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.END;
        Label imgLabel = new Label(labelComposite, SWT.NONE);
        imgLabel.setLayoutData(gridData);
        imgLabel.setImage(this.loadCountryIcon(this.locale));

        // Goto button
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.END;
        this.gotoButton = new Button(labelComposite, SWT.ARROW | SWT.RIGHT);
        this.gotoButton.setToolTipText(RBEPlugin.getString("value.goto.tooltip"));
        this.gotoButton.setEnabled(false);
        this.gotoButton.addSelectionListener(this.getGotToButtonSelectionListener());
        this.gotoButton.setLayoutData(gridData);
    }


    private SelectionAdapter getSimButtonSelectionListener() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                String head = RBEPlugin.getString("dialog.similar.head");
                String body = RBEPlugin.getString("dialog.similar.body", BundleEntryComposite.this.activeKey, UIUtils.getDisplayName(BundleEntryComposite.this.locale));
                body += "\n\n";
                for (Iterator<BundleEntry> iter = BundleEntryComposite.this.similarVisitor.getSimilars().iterator(); iter.hasNext();) {
                    body += "        " + iter.next().getKey() + "\n";
                }
                MessageDialog.openInformation(BundleEntryComposite.this.getShell(), head, body);
            }
        };
    }


    private SelectionListener getTranslateButtonSelectionListener() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {

                ITranslator translator = BundleEntryComposite.this.page.getTranslator();
                if (translator == null) {
                    return;
                }
                String textToTranslate = BundleEntryComposite.this.getTextViewer().getTextWidget().getText();
                if (textToTranslate == null || textToTranslate.trim().isEmpty()) {
                    return;
                }

                List<BundleEntryComposite> bundles = BundleEntryComposite.this.page.getEntryComposites();
                for (BundleEntryComposite bundle : bundles) {
                    if (bundle != BundleEntryComposite.this) {

                        String text = bundle.getTextViewer().getTextWidget().getText();
                        if (text == null || text.trim().isEmpty()) {

                            String langFrom = BundleEntryComposite.this.getLocale().toLanguageTag();
                            String langTo = bundle.getLocale().toLanguageTag();

                            String translatedText = translator.translate(langFrom, langTo, textToTranslate);

                            if (translatedText != null && !translatedText.isEmpty()) {
                                bundle.getTextViewer().getDocument().set(translatedText);
                                bundle.updateBundleOnChanges();

                            }
                        }
                    }
                }

            }
        };
    }


    private SelectionAdapter getDuplButtonSelectionListener() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                String head = RBEPlugin.getString("dialog.identical.head");
                String body = RBEPlugin.getString("dialog.identical.body", BundleEntryComposite.this.activeKey, UIUtils.getDisplayName(BundleEntryComposite.this.locale));
                body += "\n\n";
                for (Iterator<BundleEntry> iter = BundleEntryComposite.this.duplVisitor.getDuplicates().iterator(); iter.hasNext();) {
                    body += "        " + iter.next().getKey() + "\n";
                }
                MessageDialog.openInformation(BundleEntryComposite.this.getShell(), head, body);
            }
        };
    }


    private SelectionAdapter getCommentedCheckboxSelectionListener() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                BundleEntryComposite.this.resetCommented();
                BundleEntryComposite.this.updateBundleOnChanges();
            }
        };
    }


    private SelectionAdapter getGotToButtonSelectionListener() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                ITextEditor editor = BundleEntryComposite.this.resourceManager.getSourceEditor(BundleEntryComposite.this.locale).getEditor();
                Object activeEditor = editor.getSite().getPage().getActiveEditor();
                if (activeEditor instanceof ResourceBundleEditor) {
                    ((ResourceBundleEditor) activeEditor).setActivePage(BundleEntryComposite.this.locale);
                }
            }
        };
    }

    private Collection<FocusListener> focusListeners = new LinkedList<FocusListener>();

    @Override
    public void addFocusListener(FocusListener listener) {
        if (!this.focusListeners.contains(listener)) {
            this.focusListeners.add(listener);
        }
    }
    // /**
    // * Creates the text row.
    // */
    // private void createTextRow() {
    // textBox = new Text(this, SWT.MULTI | SWT.WRAP |
    // SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    // textBox.setEnabled(false);
    // //Addition by Eric FETTWEIS
    // //Note that this does not seem to work... It would however be usefull
    // // for arabic and some other languages
    // textBox.setOrientation(getOrientation(locale));
    //
    // GridData gridData = new GridData();
    // gridData.verticalAlignment = GridData.FILL;
    // gridData.grabExcessVerticalSpace = true;
    // gridData.horizontalAlignment = GridData.FILL;
    // gridData.grabExcessHorizontalSpace = true;
    // gridData.heightHint = UIUtils.getHeightInChars(textBox, 3);
    // textBox.setLayoutData(gridData);
    // textBox.addFocusListener(new FocusListener() {
    // public void focusGained(FocusEvent event) {
    // textBeforeUpdate = textBox.getText();
    // }
    // public void focusLost(FocusEvent event) {
    // updateBundleOnChanges();
    // }
    // });
    // //TODO add a preference property listener and add/remove this listener
    // textBox.addTraverseListener(new TraverseListener() {
    // public void keyTraversed(TraverseEvent event) {
    // if (event.character == SWT.TAB
    // && !RBEPreferences.getFieldTabInserts()) {
    // event.doit = true;
    // event.detail = SWT.TRAVERSE_NONE;
    // if (event.stateMask == 0)
    // page.focusNextBundleEntryComposite();
    // else if (event.stateMask == SWT.SHIFT)
    // page.focusPreviousBundleEntryComposite();
    // } else if (event.character == SWT.CR) {
    // if (event.stateMask == SWT.CTRL) {
    // event.doit = false;
    // } else if (event.stateMask == 0) {
    // event.doit = true;
    // event.detail = SWT.TRAVERSE_NONE;
    // page.selectNextTreeEntry();
    // } else if (event.stateMask == SWT.SHIFT) {
    // event.doit = true;
    // event.detail = SWT.TRAVERSE_NONE;
    // page.selectPreviousTreeEntry();
    // }
    // }
    //// } else if (event.keyCode == SWT.ARROW_DOWN
    //// && event.stateMask == SWT.CTRL) {
    //// event.doit = true;
    //// event.detail = SWT.TRAVERSE_NONE;
    //// page.selectNextTreeEntry();
    //// } else if (event.keyCode == SWT.ARROW_UP
    //// && event.stateMask == SWT.CTRL) {
    //// event.doit = true;
    //// event.detail = SWT.TRAVERSE_NONE;
    //// page.selectPreviousTreeEntry();
    //// }
    //
    // }
    // });
    // textBox.addKeyListener(new KeyAdapter() {
    // public void keyReleased(KeyEvent event) {
    // Text eventBox = (Text) event.widget;
    // final ITextEditor editor = resourceManager.getSourceEditor(
    // locale).getEditor();
    // // Text field has changed: make editor dirty if not already
    // if (textBeforeUpdate != null
    // && !textBeforeUpdate.equals(eventBox.getText())) {
    // // Make the editor dirty if not already. If it is,
    // // we wait until field focus lost (or save) to
    // // update it completely.
    // if (!editor.isDirty()) {
    // int caretPosition = eventBox.getCaretPosition();
    // updateBundleOnChanges();
    // eventBox.setSelection(caretPosition);
    // }
    // //autoDetectRequiredFont(eventBox.getText());
    // }
    // }
    // });
    // // Eric Fettweis : new listener to automatically change the font
    // textBox.addModifyListener(new ModifyListener() {
    //
    // public void modifyText(ModifyEvent e) {
    // String text = textBox.getText();
    // Font f = textBox.getFont();
    // String fontName = getBestFont(
    // f.getFontData()[0].getName(), text);
    // if(fontName!=null){
    // f = getSWTFont(f, fontName);
    // textBox.setFont(f);
    // }
    // }
    //
    // });
    //
    // textBox.addFocusListener(internalFocusListener);
    // }


    /**
     * Creates the text row.
     */
    private void createTextViewerRow() {
        // int vscroll = RBEPreferences.getAutoAdjust() ? 0 : SWT.V_SCROLL;
        this.textViewer = new TextViewer(this, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

        this.textViewer.setDocument(new Document());
        this.undoManager = new TextViewerUndoManager(20);
        this.textViewer.setUndoManager(this.undoManager);
        this.textViewer.activatePlugins();
        final StyledText textBox = this.textViewer.getTextWidget();

        textBox.setEnabled(false);
        // Addition by Eric FETTWEIS
        // Note that this does not seem to work... It would however be useful
        // for arabic and some other languages
        textBox.setOrientation(BundleEntryComposite.getOrientation(this.locale));

        FontRegistry fontRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
        Font font = fontRegistry.get("com.essiembre.eclipse.rbe.ui.preferences.fontDefinition");
        if (font != null) {
            textBox.setFont(font);
        }

        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        // gridData.heightHint = UIUtils.getHeightInChars(textBox, 3);
        textBox.setLayoutData(gridData);
        textBox.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent event) {
                BundleEntryComposite.this.textBeforeUpdate = textBox.getText();
            }


            @Override
            public void focusLost(FocusEvent event) {
                BundleEntryComposite.this.updateBundleOnChanges();
            }
        });

        textBox.addTraverseListener(new TraverseListener() {

            @Override
            public void keyTraversed(TraverseEvent event) {
                if (event.character == SWT.TAB && !RBEPreferences.getFieldTabInserts()) {
                    event.doit = true;
                    event.detail = SWT.TRAVERSE_NONE;
                    if (event.stateMask == 0) {
                        BundleEntryComposite.this.textViewer.setSelectedRange(0, 0);
                        BundleEntryComposite.this.page.focusNextBundleEntryComposite();
                    } else if (event.stateMask == SWT.SHIFT) {
                        BundleEntryComposite.this.textViewer.setSelectedRange(0, 0);
                        BundleEntryComposite.this.page.focusPreviousBundleEntryComposite();
                    }
                } else if (event.keyCode == SWT.ARROW_DOWN && event.stateMask == SWT.CTRL) {
                    event.doit = true;
                    event.detail = SWT.TRAVERSE_NONE;
                    BundleEntryComposite.this.page.selectNextTreeEntry();
                } else if (event.keyCode == SWT.ARROW_UP && event.stateMask == SWT.CTRL) {
                    event.doit = true;
                    event.detail = SWT.TRAVERSE_NONE;
                    BundleEntryComposite.this.page.selectPreviousTreeEntry();
                }
            }
        });

        textBox.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent event) {
                if (BundleEntryComposite.isKeyCombination(event, SWT.CTRL, 'z')) {
                    BundleEntryComposite.this.undoManager.undo();
                } else if (BundleEntryComposite.isKeyCombination(event, SWT.CTRL, 'y')) {
                    BundleEntryComposite.this.undoManager.redo();
                } else if (BundleEntryComposite.isKeyCombination(event, SWT.CTRL, 'a')) {
                    BundleEntryComposite.this.textViewer.setSelectedRange(0, BundleEntryComposite.this.textViewer.getDocument().getLength());
                } else {
                    StyledText eventBox = (StyledText) event.widget;
                    final ITextEditor editor = BundleEntryComposite.this.resourceManager.getSourceEditor(BundleEntryComposite.this.locale).getEditor();
                    // Text field has changed: make editor dirty if not already
                    if (BundleEntryComposite.this.textBeforeUpdate != null && !BundleEntryComposite.this.textBeforeUpdate.equals(eventBox.getText())) {
                        // Make the editor dirty if not already. If it is,
                        // we wait until field focus lost (or save) to
                        // update it completely.
                        if (!editor.isDirty()) {
                            int caretPosition = eventBox.getCaretOffset();
                            BundleEntryComposite.this.updateBundleOnChanges();
                            eventBox.setSelection(caretPosition);
                        }
                        // autoDetectRequiredFont(eventBox.getText());
                    }
                }
            }
        });
        // Eric Fettweis : new listener to automatically change the font
        this.textViewer.addTextListener(new ITextListener() {

            String _oldText = null;

            @Override
            public void textChanged(TextEvent event) {
                String text = textBox.getText();
                if (text.equals(this._oldText)) {
                    return;
                }
                this._oldText = text;

                Font f = textBox.getFont();
                String fontName = BundleEntryComposite.getBestFont(f.getFontData()[0].getName(), text);
                if (fontName != null) {
                    f = BundleEntryComposite.this.getSWTFont(f, fontName);
                    textBox.setFont(f);
                }

                // ScrolledComposite scrolledComposite =
                // (ScrolledComposite)getParent().getParent();
                // Point newMinSize = getParent().computeSize(
                // scrolledComposite.getClientArea().width, SWT.DEFAULT);
                // if ( !newMinSize.equals(new Point(
                // scrolledComposite.getMinWidth(),
                // scrolledComposite.getMinHeight())) ) {
                // page.setAutoAdjustNeeded(true);
                // }
            }
        });

        textBox.addFocusListener(this.internalFocusListener);
    }


    private static boolean isKeyCombination(KeyEvent event, int modifier1, int keyCode) {
        return (event.keyCode == keyCode && event.stateMask == modifier1);
    }


    /**
     * Loads country icon based on locale country.
     * @param countryLocale the locale on which to grab the country
     * @return an image, or <code>null</code> if no match could be made
     */
    private Image loadCountryIcon(Locale countryLocale) {
        Image image = null;
        String countryCode = null;
        if (countryLocale != null && countryLocale.getCountry() != null) {
            countryCode = countryLocale.getCountry().toLowerCase();
        }
        if (countryCode != null && countryCode.length() > 0) {
            String imageName = "countries/" + countryCode.toLowerCase() + ".gif";
            image = UIUtils.getImage(imageName);
        }
        if (image == null) {
            image = UIUtils.getImage("countries/blank.gif");
        }
        return image;
    }


    /* default */ void resetCommented() {
        final StyledText textBox = this.textViewer.getTextWidget();
        if (this.commentedCheckbox.getSelection()) {
            this.commentedCheckbox.setToolTipText(RBEPlugin.getString("value.uncomment.tooltip"));
            textBox.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        } else {
            this.commentedCheckbox.setToolTipText(RBEPlugin.getString("value.comment.tooltip"));
            textBox.setForeground(null);
        }
    }


    public void focusTextBox() {
        StyledText textBox = this.textViewer.getTextWidget();
        textBox.setFocus();
        this.textViewer.setSelectedRange(0, this.textViewer.getDocument().getLength());
    }

    /** Additions by Eric FETTWEIS */
    /*
     * private void autoDetectRequiredFont(String value) { Font f = getFont(); FontData[] data = f.getFontData(); boolean resetFont = true; for (int i
     * = 0; i < data.length; i++) { java.awt.Font test = new java.awt.Font(data[i].getName(), java.awt.Font.PLAIN, 12);
     * if(test.canDisplayUpTo(value)==-1){ resetFont = false; break; } } if(resetFont){ String[] fonts =
     * GraphicsEnvironment.getLocalGraphicsEnvironment(). getAvailableFontFamilyNames(); for (int i = 0; i < fonts.length; i++) { java.awt.Font fnt =
     * new java.awt.Font( fonts[i],java.awt.Font.PLAIN,12); if(fnt.canDisplayUpTo(value)==-1){ textBox.setFont(createFont(fonts[i])); break; } } } }
     */
    /**
     * Holds swt fonts used for the textBox.
     */
    private Map<String, Font> swtFontCache = new HashMap<>();

    /**
     * Gets a font by its name. The resulting font is build based on the
     * baseFont parameter.
     * The font is retrieved from the swtFontCache, or created if needed.
     * @param baseFont the current font used to build the new one.
     * Only the name of the new font will differ fromm the original one.
     * @parama baseFont a font
     * @param name the new font name
     * @return a font with the same style and size as the original.
     */
    private Font getSWTFont(Font baseFont, String name) {
        Font font = this.swtFontCache.get(name);
        if (font == null) {
            font = BundleEntryComposite.createFont(baseFont, this.getDisplay(), name);
            this.swtFontCache.put(name, font);
        }
        return font;
    }


    /**
     * Gets the name of the font which will be the best to display a String.
     * All installed fonts are searched. If a font can display the entire
     * string, then it is retuned immediately.
     * Otherwise, the font returned is the one which can display properly the
     * longest substring possible from the argument value.
     * @param baseFontName a font to be tested before any other. It will be
     * the current font used by a widget.
     * @param value the string to be displayed.
     * @return a font name
     */
    private static String getBestFont(String baseFontName, String value) {
        if (BundleEntryComposite.canFullyDisplay(baseFontName, value)) {
            return baseFontName;
        }
        String[] fonts = BundleEntryComposite.getAvailableFontNames();
        String fontName = null;
        int currentScore = 0;
        for (int i = 0; i < fonts.length; i++) {
            int score = BundleEntryComposite.canDisplayUpTo(fonts[i], value);
            if (score == -1) {// no need to loop further
                fontName = fonts[i];
                break;
            }
            if (score > currentScore) {
                fontName = fonts[i];
                currentScore = score;
            }
        }

        return fontName;
    }


    private static String[] getAvailableFontNames() {
        if (BundleEntryComposite._fontFamilyNames == null) {
            String[] fontFamilyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            BundleEntryComposite._fontFamilyNames = fontFamilyNames;
        }
        return BundleEntryComposite._fontFamilyNames;
    }

    /**
     * A cache holding an instance of every AWT font tested.
     */
    private static Map<String, java.awt.Font> awtFontCache = new HashMap<>();
    private static String[]                   _fontFamilyNames;

    /**
     * Creates a variation from an original font, by changing the face name.
     * @param baseFont the original font
     * @param display the current display
     * @param name the new font face name
     * @return a new Font
     */
    private static Font createFont(Font baseFont, Display display, String name) {
        FontData[] fontData = baseFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setName(name);
        }
        return new Font(display, fontData);
    }


    /**
     * Can a font display correctly an entire string ?
     * @param fontName the font name
     * @param value the string to be displayed
     * @return
     */
    private static boolean canFullyDisplay(String fontName, String value) {
        return BundleEntryComposite.canDisplayUpTo(fontName, value) == -1;
    }


    /**
     * Test the number of characters from a given String that a font can
     * display correctly.
     * @param fontName the name of the font
     * @param value the value to be displayed
     * @return the number of characters that can be displayed, or -1 if the
     * entire string can be displayed successfuly.
     * @see java.aw.Font#canDisplayUpTo(String)
     */
    private static int canDisplayUpTo(String fontName, String value) {
        java.awt.Font font = BundleEntryComposite.getAWTFont(fontName);
        return font.canDisplayUpTo(value);
    }


    /**
     * Returns a cached or new AWT font by its name.
     * If the font needs to be created, its style will be Font.PLAIN and its
     * size will be 12.
     * @param name teh font name
     * @return an AWT Font
     */
    private static java.awt.Font getAWTFont(String name) {
        java.awt.Font font = BundleEntryComposite.awtFontCache.get(name);
        if (font == null) {
            font = new java.awt.Font(name, java.awt.Font.PLAIN, 12);
            BundleEntryComposite.awtFontCache.put(name, font);
        }
        return font;
    }


    /**
     * Gets the orientation suited for a given locale.
     * @param locale the locale
     * @return <code>SWT.RIGHT_TO_LEFT</code> or <code>SWT.LEFT_TO_RIGHT</code>
     */
    private static int getOrientation(Locale locale) {
        if (locale != null) {
            ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
            if (orientation == ComponentOrientation.RIGHT_TO_LEFT) {
                return SWT.RIGHT_TO_LEFT;
            }
        }
        return SWT.LEFT_TO_RIGHT;
    }
}
