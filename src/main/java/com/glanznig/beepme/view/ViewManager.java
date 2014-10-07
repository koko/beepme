package com.glanznig.beepme.view;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.glanznig.beepme.BeepMeApp;
import com.glanznig.beepme.data.InputElement;
import com.glanznig.beepme.data.InputGroup;
import com.glanznig.beepme.data.MultiValue;
import com.glanznig.beepme.data.Project;
import com.glanznig.beepme.data.SingleValue;
import com.glanznig.beepme.data.TranslationElement;
import com.glanznig.beepme.data.Value;
import com.glanznig.beepme.data.db.InputElementTable;
import com.glanznig.beepme.data.db.InputGroupTable;
import com.glanznig.beepme.data.db.TranslationElementTable;
import com.glanznig.beepme.helper.PhotoUtils;
import com.glanznig.beepme.view.input.InputControl;
import com.glanznig.beepme.view.input.PhotoControl;
import com.glanznig.beepme.view.input.TagControl;
import com.glanznig.beepme.view.input.TextControl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The ViewManager instantiates and manages the create, edit and view UI (all the input elements).
 */
public class ViewManager {

    private static final String TAG = "ViewManager";

    private Context ctx;
    private HashMap<String, InputControl> controls;
    private PhotoControl photoControl = null;
    private Project project;
    private final float scale;

    public ViewManager(Context ctx, Project project) {
        this.ctx = ctx;
        this.project = project;
        controls = new HashMap<String, InputControl>();

        scale = ctx.getResources().getDisplayMetrics().density;
    }

    /**
     * Gets a instantiation of a UI layout (input controls) for the specified view mode (create,
     * edit, view).
     * @param mode the view mode
     * @return root view with added input control child elements
     */
    public View getLayout(InputControl.Mode mode) {
        View rootView = null;
        BeepMeApp app = (BeepMeApp)ctx.getApplicationContext();
        Project project = app.getCurrentProject();
        List<InputGroup> inputGroupList = new InputGroupTable(ctx).getInputGroups(project.getUid());

        if (inputGroupList.size() > 1) {
            // we have several input groups and need a ViewPager
            ViewPager pager = new ViewPager(ctx);
            Iterator<InputGroup> inputGroupIterator = inputGroupList.iterator();

            while (inputGroupIterator.hasNext()) {
                pager.addView(addElements(mode, inputGroupIterator.next()));
            }

            rootView = pager;
        }
        else {
            // we have only one input group and can leave out the ViewPager
            rootView = addElements(mode, inputGroupList.get(0));
        }

        return rootView;
    }

    /**
     * Gets all input controls, that are part of this ViewManager instance.
     * @return collection of input controls, part of this ViewManager instance
     */
    public Collection<InputControl> getInputControls() {
        return controls.values();
    }

    /**
     * Gets a input control, which is part of this ViewManager instance, by its display id (name).
     * @param name display id of input control
     * @return the input control with the specified display id, or null if not found
     */
    public InputControl getInputControl(String name) {
        return controls.get(name);
    }

    /**
     * Gets the photo control of this view manager
     * @return photo control or null if none
     */
    public PhotoControl getPhotoControl() {
        return photoControl;
    }

    /**
     * Sets the values for the input controls that are referenced in the value map.
     * @param values HashMap of String to Value containing values referencing input controls
     */
    public void setValues(HashMap<String, Value> values) {
        Iterator<String> keyIterator = values.keySet().iterator();

        while (keyIterator.hasNext()) {
            String name = keyIterator.next();
            InputControl control = getInputControl(name);
            control.setValue(values.get(name));
        }
    }

    /**
     * Reads (string representations of) values of all controls from a Bundle and populates the
     * corresponding object fields. String representations have the form 'type=[single|multi];value=VALUE'.
     * @param state Bundle, which contains the saved state
     */
    public void deserializeValues(Bundle state) {
        Iterator<String> keyIterator = state.keySet().iterator();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            String valueString = state.getString(key);
            boolean isMultiValue = false;
            Value value = null;

            String[] parts = valueString.split(";");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith("type")) {
                    if (parts[i].substring(5).equals("single")) {
                        isMultiValue = false;
                    }
                    else if (parts[i].substring(5).equals("multi")) {
                        isMultiValue = true;
                    }
                }
                else if (parts[i].startsWith("value")) {
                    if (isMultiValue) {
                        MultiValue multiValue = MultiValue.fromString(parts[i].substring(6));
                        value = multiValue;
                    }
                    else {
                        SingleValue singleValue = SingleValue.fromString(parts[i].substring(6));
                        value = singleValue;
                    }
                }
            }

            if (value != null && controls.containsKey(key)) {
                InputControl control = controls.get(key);
                control.setValue(value);
            }
        }
    }

    /**
     * Writes (string representations of) values of all controls into a Bundle. String representations
     * have the form 'type=[single|multi];value=VALUE'.
     * @param state Bundle, which contains the saved state
     */
    public void serializeValues(Bundle state) {
        Iterator<Map.Entry<String, InputControl>> controlIterator = controls.entrySet().iterator();
        while (controlIterator.hasNext()) {
            Map.Entry<String, InputControl> control = controlIterator.next();
            String valueString = "type=";
            Value value = control.getValue().getValue();

            if (value instanceof SingleValue) {
                valueString += "single;value=";
                valueString += ((SingleValue)value).toString();
            }
            else if (value instanceof MultiValue) {
                valueString += "multi;value=";
                valueString += ((MultiValue)value).toString();
            }

            state.putString(control.getKey(), valueString);
        }
    }

    /**
     * Adds input controls of the specified input group to a ScrollView.
     * @param mode the view mode
     * @param inputGroup the definition of the input group
     * @return ScrollView with input controls of the specified input group as child elements
     */
    private ScrollView addElements(InputControl.Mode mode, InputGroup inputGroup) {
        ScrollView scrollView = new ScrollView(ctx);
        LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding((int)(16 * scale + 0.5f), (int)(16 * scale + 0.5f), (int)(16 * scale + 0.5f), (int)(16 * scale + 0.5f));
        scrollView.addView(linearLayout);

        List<InputElement> inputElements = new InputElementTable(ctx).getInputElementsByGroup(inputGroup.getUid());
        Iterator<InputElement> inputElementIterator = inputElements.iterator();

        View control = null;
        while (inputElementIterator.hasNext()) {
            InputElement inputElement = inputElementIterator.next();

            // get translations
            TranslationElementTable translationElementTable = new TranslationElementTable(ctx);
            Iterator<TranslationElement> translationIterator = translationElementTable.getTranslationElements(inputElement.getUid()).iterator();

            while (translationIterator.hasNext()) {
                TranslationElement translationElement = translationIterator.next();
                inputElement.setTranslation(translationElement);
            }

            switch (inputElement.getType()) {
                case PHOTO:
                    //check if device has camera feature (only in CREATE mode)
                    if (PhotoUtils.isEnabled(ctx) || mode != InputControl.Mode.CREATE) {
                        control = createPhotoControl(inputElement, mode);
                        photoControl = (PhotoControl) control;
                    }
                    break;

                case TAGS:
                    control = createTagControl(inputElement, mode);
                    break;

                case TEXT:
                    control = createTextControl(inputElement, mode);
                    break;
            }

            if (control != null) {
                controls.put(inputElement.getName(), (InputControl) control);
                control.setPadding(0, 0, 0, (int) (16 * scale + 0.5f));
                linearLayout.addView(control);
                control = null;
            }
        }

        return scrollView;
    }

    /**
     * Creates a text control from its definition.
     * @param inputElement definition of the text control
     * @param mode the view mode
     * @return the instantiated text control, or null if the supplied input element did not have
     * the type TEXT
     */
    private TextControl createTextControl(InputElement inputElement, InputControl.Mode mode) {
        Locale locale = Locale.getDefault();
        TextControl textControl = null;
        if (inputElement.getType().equals(InputElement.InputElementType.TEXT)) {
            textControl = new TextControl(ctx, mode, inputElement.getRestrictions());
            textControl.setInputElementUid(inputElement.getUid());
            textControl.setName(inputElement.getName());
            textControl.setMandatory(inputElement.isMandatory());
            if (inputElement.getOption("lines") != null) {
                textControl.setLines(Integer.valueOf(inputElement.getOption("lines")).intValue());
            }

            String title = inputElement.getTitle(locale.getLanguage());
            if (title == null) {
                // fallback to default language
                title = inputElement.getTitle(project.getLanguage().getLanguage());
            }
            textControl.setTitle(title);
            String help = inputElement.getHelp(locale.getLanguage());
            if (help == null) {
                // fallback to default language
                help = inputElement.getHelp(project.getLanguage().getLanguage());
            }
            textControl.setHelpText(help);
        }

        return textControl;
    }

    /**
     * Creates a tag control from its definition.
     * @param inputElement definition of the tag control
     * @param mode the view mode
     * @return the instantiated tag control, or null if the supplied input element did not have
     * the type TAG
     */
    private TagControl createTagControl(InputElement inputElement, InputControl.Mode mode) {
        Locale locale = Locale.getDefault();
        TagControl tagControl = null;
        if (inputElement.getType().equals(InputElement.InputElementType.TAGS)) {
            tagControl = new TagControl(ctx, mode, inputElement.getRestrictions());
            tagControl.setInputElementUid(inputElement.getUid());
            tagControl.setName(inputElement.getName());
            tagControl.setMandatory(inputElement.isMandatory());
            tagControl.setVocabularyUid(inputElement.getVocabularyUid());

            String title = inputElement.getTitle(locale.getLanguage());
            if (title == null) {
                // fallback to default language
                title = inputElement.getTitle(project.getLanguage().getLanguage());
            }
            tagControl.setTitle(title);
            String help = inputElement.getHelp(locale.getLanguage());
            if (help == null) {
                // fallback to default language
                help = inputElement.getHelp(project.getLanguage().getLanguage());
            }
            tagControl.setHelpText(help);
        }
        return tagControl;
    }

    /**
     * Creates a photo control from its definition.
     * @param inputElement definition of the photo control
     * @param mode the view mode
     * @return the instantiated photo control, or null if the supplied input element did not have
     * the type PHOTO
     */
    private PhotoControl createPhotoControl(InputElement inputElement, InputControl.Mode mode) {
        Locale locale = Locale.getDefault();
        PhotoControl photoControl = null;
        if (inputElement.getType().equals(InputElement.InputElementType.PHOTO)) {
            photoControl = new PhotoControl(ctx, mode, inputElement.getRestrictions());
            photoControl.setInputElementUid(inputElement.getUid());
            photoControl.setName(inputElement.getName());
            photoControl.setMandatory(inputElement.isMandatory());
            // todo add specific parameters

            String title = inputElement.getTitle(locale.getLanguage());
            if (title == null) {
                // fallback to default language
                title = inputElement.getTitle(project.getLanguage().getLanguage());
            }
            photoControl.setTitle(title);
            String help = inputElement.getHelp(locale.getLanguage());
            if (help == null) {
                // fallback to default language
                help = inputElement.getHelp(project.getLanguage().getLanguage());
            }
            photoControl.setHelpText(help);
        }

        return photoControl;
    }
}