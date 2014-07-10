/*
This file is part of BeepMe.

BeepMe is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

BeepMe is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with BeepMe. If not, see <http://www.gnu.org/licenses/>.

Copyright 2012-2014 Michael Glanznig
http://beepme.yourexp.at
*/

package com.glanznig.beepme.view.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.glanznig.beepme.R;
import com.glanznig.beepme.data.MultiValue;
import com.glanznig.beepme.data.Restriction;
import com.glanznig.beepme.data.Value;
import com.glanznig.beepme.data.VocabularyItem;
import com.glanznig.beepme.data.db.VocabularyItemTable;
import com.glanznig.beepme.helper.FlowLayout;

/**
 * A tag control provides a UI element to add (free-text) keywords.
 */
public class TagControl extends LinearLayout implements InputControl, View.OnClickListener {
	
	private static final String TAG = "TagControl";

    private Context ctx;
    private Mode mode;
    private String name;
    private boolean mandatory;
    private boolean restrictEdit;
    private long inputElementUid = 0L;
    private MultiValue value;

    private TextView help;
    private AutoCompleteTextView tagInput;
    private TextView title;
    private ImageButton addBtn;
    private FlowLayout tagContainer;

    private ArrayList<OnTagControlChangeListener> listeners;

	
	private final float scale = getResources().getDisplayMetrics().density;
	private long vocabularyUid = 0L;
	private ArrayList<VocabularyItem> vocabularyItems = null;
	
	public TagControl(Context ctx) {
		super(ctx);
        this.ctx = ctx.getApplicationContext();
        listeners = new ArrayList<OnTagControlChangeListener>();

		vocabularyItems = new ArrayList<VocabularyItem>();
        mode = Mode.CREATE;
        name = null;
        mandatory = false;
        value = null;
        restrictEdit = false;

        setupView();
	}

    /**
     * Constructor
     * @param ctx the view context
     * @param mode the view mode
     * @param restrictions access restrictions for this tag control
     */
	public TagControl(Context ctx, Mode mode, Collection<Restriction> restrictions) {
		super(ctx);
        this.ctx = ctx.getApplicationContext();
        listeners = new ArrayList<OnTagControlChangeListener>();

		vocabularyItems = new ArrayList<VocabularyItem>();
        this.mode = mode;
        name = null;
        mandatory = false;
        value = null;
        restrictEdit = false;

        if (restrictions != null) {
            Iterator<Restriction> restrictionIterator = restrictions.iterator();
            while (restrictionIterator.hasNext()) {
                Restriction restriction = restrictionIterator.next();
                // todo if delete allowed render buttons but no input field ??
                if (restriction.getType().equals(Restriction.RestrictionType.EDIT) && restriction.getAllowed() == false) {
                    restrictEdit = true;
                }
                if (restriction.getType().equals(Restriction.RestrictionType.DELETE) && restriction.getAllowed() == false) {
                    restrictEdit = true;
                }
            }
        }

        setupView();
	}
	
	public TagControl(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
        this.ctx = ctx.getApplicationContext();
        listeners = new ArrayList<OnTagControlChangeListener>();

		vocabularyItems = new ArrayList<VocabularyItem>();
        mode = Mode.CREATE;
        name = null;
        mandatory = false;
        value = null;
        restrictEdit = false;

        setupView();
	}

    /**
     * Adds all the necessary sub-elements for the given view mode.
     */
    private void setupView() {
        if (mode.equals(Mode.CREATE) || (mode.equals(Mode.EDIT) && !restrictEdit)) {
            setOrientation(LinearLayout.VERTICAL);

            RelativeLayout tagsInput = new RelativeLayout(ctx);

            tagInput = new AutoCompleteTextView(ctx); //todo let input fill all the width
            addBtn = new ImageButton(ctx);

            tagInput.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            TagAutocompleteAdapter adapterKeywords = new TagAutocompleteAdapter(ctx, R.layout.tag_autocomplete_list_row, vocabularyUid);
            tagInput.setAdapter(adapterKeywords);
            //after how many chars should auto-complete list appear?
            tagInput.setThreshold(2);

            RelativeLayout.LayoutParams tagInputParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            tagInputParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            tagInputParams.addRule(RelativeLayout.LEFT_OF, addBtn.getId());
            tagsInput.addView(tagInput, tagInputParams);

            addBtn.setImageResource(R.drawable.ic_action_new_label);
            addBtn.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, (int)(40 * scale + 0.5f)));
            addBtn.setAdjustViewBounds(true);
            addBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
            addBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    addTag();
                }
            });

            RelativeLayout.LayoutParams addBtnParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            addBtnParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            tagsInput.addView(addBtn, addBtnParams);

            addView(tagsInput);

            help = new TextView(ctx);
            addView(help);

            tagContainer = new FlowLayout(ctx);
            addView(tagContainer);
        }
        else if (mode.equals(Mode.VIEW) || (mode.equals(Mode.EDIT) && restrictEdit)) {
            setOrientation(LinearLayout.VERTICAL);
            title = new TextView(ctx);
            addView(title);
            tagContainer = new FlowLayout(ctx);
            addView(tagContainer);
        }
    }

    @Override
    public void setHelpText(String help) {
        if (this.help != null) {
            this.help.setText(help);
        }
    }

    @Override
    public void setTitle(String title) {
        if (mode.equals(Mode.CREATE) || (mode.equals(Mode.EDIT) && !restrictEdit)) {
            tagInput.setHint(title);
        }
        else if (mode.equals(Mode.VIEW) || (mode.equals(Mode.EDIT) && restrictEdit)) {
            this.title.setText(title);
        }
    }

    @Override
    public void setInputElementUid(long inputElementUid) {
        this.inputElementUid = inputElementUid;
    }

    @Override
    public long getInputElementUid() {
        return inputElementUid;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public boolean getMandatory() {
        return mandatory;
    }

    @Override
    public void setValue(Value value) {
        if (value instanceof MultiValue) {
            this.value = (MultiValue)value;
            vocabularyItems = new ArrayList<VocabularyItem>();
            tagContainer.removeAllViews(); // remove all present tag buttons

            Iterator<VocabularyItem> valuesIterator = this.value.getValues().iterator();
            while (valuesIterator.hasNext()) {
                VocabularyItem item = valuesIterator.next();

                View tag = null;
                if (mode.equals(Mode.CREATE) || (mode.equals(Mode.EDIT) && !restrictEdit)) {
                    TagButton button = new TagButton(ctx, item);
                    button.setOnClickListener(this);
                    tag = button;
                }
                else if (mode.equals(Mode.VIEW) || (mode.equals(Mode.EDIT) && restrictEdit)) {
                    TagView tagView = new TagView(ctx, item);
                    tag = tagView;
                }

                // maintain sorting
                Comparator<VocabularyItem> compare = new Comparator<VocabularyItem>() {
                    public int compare(VocabularyItem item1, VocabularyItem item2) {
                        return item1.getValue().compareTo(item2.getValue());
                    }
                };
                int pos = Collections.binarySearch(vocabularyItems, item, compare);
                tagContainer.addView(tag, -pos - 1);
                vocabularyItems.add(-pos - 1, item);
            }
        }
    }

    @Override
    public Value getValue() {
        if (value == null) {
            value = new MultiValue();
            value.setInputElementUid(inputElementUid);
        }
        value.resetValue();

        Iterator<VocabularyItem> itemIterator = vocabularyItems.iterator();
        while (itemIterator.hasNext()) {
            VocabularyItem item = itemIterator.next();
            value.setValue(item);
        }

        return value;
    }

    /**
     * Sets the vocabulary uid of this tag control.
     * @param vocabulary
     */
    public void setVocabularyUid(long vocabulary) {
        this.vocabularyUid = vocabulary;
    }

    /**
     * Gets the vocabulary uid of this tag control.
     * @return vocabulary uid, or 0L if not set
     */
    public long getVocabularyUid() {
        return vocabularyUid;
    }

    /**
     * Adds a listener that should be notified if a tag gets added or removed
     * @param listener listener class implementing OnTagControlChangeListener
     */
    public void addOnTagControlChangeListener(OnTagControlChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener that should no longer being notified if a tag gets added or removed
     * @param listener listener class implementing OnTagControlChangeListener
     */
    public void removeOnTagControlChangeListener(OnTagControlChangeListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    /**
     * Adds a tag to this tag control.
     */
    public void addTag() {
        if (tagInput.getText().length() > 0) {
            String itemText = tagInput.getText().toString().toLowerCase(Locale.getDefault());
            VocabularyItem item = new VocabularyItemTable(ctx).getVocabularyItem(vocabularyUid, Locale.getDefault(), itemText);
            // new vocabulary item
            if (item == null) {
                item = new VocabularyItem();
                item.setVocabularyUid(vocabularyUid);
                item.setPredefined(false);
                item.setLanguage(Locale.getDefault());
                item.setValue(itemText);
            }

            // todo checking for equality does not work, also keep in mind that there are translations
            if (!vocabularyItems.contains(item)) {
                TagButton button = new TagButton(ctx, item);
                button.setOnClickListener(this);

                // maintain sorting
                Comparator<VocabularyItem> compare = new Comparator<VocabularyItem>() {
                    public int compare(VocabularyItem item1, VocabularyItem item2) {
                        return item1.getValue().compareTo(item2.getValue());
                    }
                };
                int pos = Collections.binarySearch(vocabularyItems, item, compare);
                tagContainer.addView(button, -pos - 1);
                vocabularyItems.add(-pos - 1, item);

                tagInput.setText("");
            }
            else {
                Toast.makeText(ctx, R.string.new_sample_add_tag_error, Toast.LENGTH_SHORT).show();
            }

            // notify listeners on add
            Iterator<OnTagControlChangeListener> listenerIterator = listeners.iterator();
            while (listenerIterator.hasNext()) {
                OnTagControlChangeListener listener = listenerIterator.next();
                listener.onTagAdded(item);
            }
        }
    }

    /**
     * Removes a tag from this tag control.
     * @param button tag holder button, that should be removed
     */
    public void removeTag(TagButton button) {
        VocabularyItem item = button.getVocabularyItem();

        if (item != null) {
            tagContainer.removeView(button);
            if (vocabularyItems.contains(item)) {
                vocabularyItems.remove(item);
            }

            // notify listeners on remove
            Iterator<OnTagControlChangeListener> listenerIterator = listeners.iterator();
            while (listenerIterator.hasNext()) {
                OnTagControlChangeListener listener = listenerIterator.next();
                listener.onTagRemoved(item);
            }
        }
    }

    public void onClick(View view) {
        if (view instanceof TagButton) {
            removeTag((TagButton)view);
        }
    }

    /**
     * Holder class for tag buttons (extends Button).
     */
    private class TagButton extends Button {

        private VocabularyItem vocabularyItem;

        public TagButton(Context context, VocabularyItem vocabularyItem) {
            super(context);
            this.vocabularyItem = vocabularyItem;

            setText(vocabularyItem.getValue());
            setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(40 * scale + 0.5f)));
        }

        public void setVocabularyItem(VocabularyItem vocabularyItem) {
            this.vocabularyItem = vocabularyItem;

            setText(vocabularyItem.getValue());
        }

        public VocabularyItem getVocabularyItem() {
            return vocabularyItem;
        }

    }

    /**
     * Holder class for tag views (extends TextView).
     */
    private class TagView extends TextView {

        private VocabularyItem vocabularyItem;

        public TagView(Context context, VocabularyItem vocabularyItem) {
            super(context);
            this.vocabularyItem = vocabularyItem;

            setText(vocabularyItem.getValue());
            setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(40 * scale + 0.5f)));
        }

        public void setVocabularyItem(VocabularyItem vocabularyItem) {
            this.vocabularyItem = vocabularyItem;

            setText(vocabularyItem.getValue());
        }

        public VocabularyItem getVocabularyItem() {
            return vocabularyItem;
        }

    }
}
