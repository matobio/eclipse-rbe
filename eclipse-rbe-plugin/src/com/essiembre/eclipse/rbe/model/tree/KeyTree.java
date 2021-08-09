/*
 * Copyright (C) 2003-2014 Pascal Essiembre Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.essiembre.eclipse.rbe.model.tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ViewerFilter;

import com.essiembre.eclipse.rbe.model.DeltaEvent;
import com.essiembre.eclipse.rbe.model.IDeltaListener;
import com.essiembre.eclipse.rbe.model.Model;
import com.essiembre.eclipse.rbe.model.bundle.Bundle;
import com.essiembre.eclipse.rbe.model.bundle.BundleEntry;
import com.essiembre.eclipse.rbe.model.bundle.BundleGroup;
import com.essiembre.eclipse.rbe.model.tree.updater.KeyTreeUpdater;

/**
 * Tree representation of a bundle group.
 * @author Pascal Essiembre
 * @author cuhiodtick
 */
public class KeyTree extends Model implements IKeyTreeVisitable {

    /** Caching of key tree items (key=ID; value=KeyTreeItem). **/
    private final Map<String, KeyTreeItem> keyItemsCache = new TreeMap<String, KeyTreeItem>();
    /** Items found at root level. */
    private final Set<KeyTreeItem>         rootKeyItems  = new TreeSet<KeyTreeItem>();
    /** Updater responsible for tree changes. */
    private KeyTreeUpdater                 updater;
    /** Bundle group used to build the tree. */
    private BundleGroup                    bundleGroup;
    /** the currently selected key. */
    private String                         selectedKey;

    /**
     * Constructor.
     * @param bundleGroup bundle group used to build this tree
     * @param updater updater used to handle tree modifications
     */
    public KeyTree(BundleGroup bundleGroup, KeyTreeUpdater updater) {
        super();
        this.bundleGroup = bundleGroup;
        this.updater = updater;

        // Set listeners
        bundleGroup.addListener(new IDeltaListener() {

            @Override
            public void add(DeltaEvent event) {
                KeyTree.this.initBundle((Bundle) event.receiver());
            }


            @Override
            public void remove(DeltaEvent event) {
                // do nothing
            }


            @Override
            public void modify(DeltaEvent event) {
                // do nothing
            }


            @Override
            public void select(DeltaEvent event) {
                // do nothing
            }
        });
        for (Iterator<Bundle> iter = bundleGroup.iterator(); iter.hasNext();) {
            this.initBundle(iter.next());
        }
        // Initial tree creation
        this.load();
    }


    /**
     * Initializes the given bundle by adding propser listeners on it.
     * @param bundle the bundle to initialize
     */
    protected void initBundle(final Bundle bundle) {
        bundle.addListener(new IDeltaListener() {

            @Override
            public void add(DeltaEvent event) {
                // TODO figure out how to filter event that do not add keys.
                // Probably not necessary with plugin rewrite.
                String key = ((BundleEntry) event.receiver()).getKey();
                KeyTree.this.addKey(key);
            }


            @Override
            public void remove(DeltaEvent event) {
                String key = ((BundleEntry) event.receiver()).getKey();
                Collection<BundleEntry> entries = KeyTree.this.bundleGroup.getBundleEntries(key);
                if (entries.size() == 0) {
                    KeyTree.this.removeKey(((BundleEntry) event.receiver()).getKey());
                }
            }


            @Override
            public void modify(DeltaEvent event) {
                // TODO figure out how to filter event that do not modify keys.
                // Probably not necessary with plugin rewrite.
                String key = ((BundleEntry) event.receiver()).getKey();
                KeyTree.this.modifyKey(key);
            }


            @Override
            public void select(DeltaEvent event) {}
        });
    }


    /**
     * Gets a key tree item.
     * @param key key of item to get
     * @return a key tree item
     */
    public KeyTreeItem getKeyTreeItem(String key) {
        return this.keyItemsCache.get(key);
    }


    /**
     * Returns the currently selected key.
     *
     * @return   The currently selected key. null = no selection.
     */
    public String getSelectedKey() {
        return this.selectedKey;
    }


    /**
     * Gets the key tree item cache.
     * @return key tree item cache.
     */
    public Map<String, KeyTreeItem> getKeyItemsCache() {
        return this.keyItemsCache;
    }


    /**
     * Gets all items contained a the root level of this tree.
     * @return a collection of <code>KeyTreeItem</code> objects.
     */
    public Set<KeyTreeItem> getRootKeyItems() {
        return this.rootKeyItems;
    }


    /**
     * Adds a key to this tree.
     * @param key key to add
     */
    public void addKey(String key) {
        this.updater.addKey(this, key);
        this.fireAdd(this.keyItemsCache.get(key));
    }


    /**
     * Removes a key from this tree.
     * @param key key to remove
     */
    public void removeKey(String key) {
        Object item = this.keyItemsCache.get(key);
        this.updater.removeKey(this, key);
        this.fireRemove(item);
    }


    /**
     * Modifies a key on this tree.
     * @param key key to modify
     */
    public void modifyKey(String key) {
        Object item = this.keyItemsCache.get(key);
        this.fireModify(item);
    }


    /**
     * Marks a key as selected on this tree.
     * @param key  key to select.
     */
    public void selectKey(String key) {
        if (key == null) {
            return;
        }

        Object item = this.keyItemsCache.get(key);
        if ((this.selectedKey == null) || (!this.selectedKey.equals(key))) {
            this.selectedKey = key;
            this.fireSelect(item);
        }
    }


    public void selectNextKey() {
        String currentKey = this.getSelectedKey();
        boolean takeNext = false;
        String nextKey = null;
        for (String key : this.keyItemsCache.keySet()) {
            if (takeNext) {
                nextKey = key;
                break;
            }
            if (key.equals(currentKey)) {
                takeNext = true;
            }
        }

        if (nextKey != null) {
            this.selectKey(nextKey);
        }
    }


    /**
     * Gets the key tree updater.
     * @return key tree updater
     */
    public KeyTreeUpdater getUpdater() {
        return this.updater;
    }


    /**
     * Sets the key tree updater. Doing so will automatically refresh the tree,
     * which means, recreating it entirely.
     * @param updater key tree updater
     */
    public void setUpdater(KeyTreeUpdater updater) {
        this.updater = updater;
        this.keyItemsCache.clear();
        this.rootKeyItems.clear();
        this.load();
    }


    /**
     * @see com.essiembre.eclipse.rbe.model.tree.IKeyTreeVisitable#accept(
     *         com.essiembre.eclipse.rbe.model.tree.IKeyTreeVisitor,
     *         java.lang.Object)
     */
    @Override
    public void accept(IKeyTreeVisitor visitor, Object passAlongArgument) {
        for (Iterator<KeyTreeItem> iter = this.keyItemsCache.values().iterator(); iter.hasNext();) {
            visitor.visitKeyTreeItem(iter.next(), passAlongArgument);
        }
        visitor.visitKeyTree(this, passAlongArgument);
    }


    /**
     * Gets the bundle group associated with this tree.
     * @return bundle group
     */
    public BundleGroup getBundleGroup() {
        return this.bundleGroup;
    }


    /**
     * Loads all key tree items, base on bundle group.
     */
    private final void load() {
        for (Iterator<String> iter = this.bundleGroup.getKeys().iterator(); iter.hasNext();) {
            /*
             * Do not call "fireAdd" method from here for extreme performance improvement. This is not an addition in the sense that we are laying out
             * existing keys, not adding any new ones. We will refresh the whole tree after we are done looping.
             */
            this.updater.addKey(this, iter.next());
        }
        if (this.getFilter() != null) {
            this.filterKeyItems(this.getFilter());
        }

        this.fireAdd(this);
    }

    private String filter;

    /**
     * Returns the key filter that was applied using the last call to {@link #filterKeyItems(String)}
     * @return The key filter that was applied using the last call to {@link #filterKeyItems(String)}
     */
    private String getFilter() {
        return this.filter;
    }


    /**
     * Filters the key items by applying a substring-check for the given <code>filter</code> string. This information
     * can be used later in {@link ViewerFilter}s to suppress filtered items.
     * @param filter The <code>filter</code> that should be applied to the key items.
     */
    public void filterKeyItems(String filter) {
        this.filter = filter;
        for (KeyTreeItem item : this.rootKeyItems) {
            item.applyFilter(filter);
        }
    }


    /**
     * Resets the filter that is currently used, i.e. makes all key items visible again.
     */
    public void resetFilter() {
        this.filterKeyItems("");
    }
}
