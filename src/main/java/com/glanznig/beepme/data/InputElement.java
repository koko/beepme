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

package com.glanznig.beepme.data;

import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An input element represents a method of data entry on a more abstract level and a specific UI
 * component on a more specific level. There are several possibilities: text entry, photos, tags,
 * single- and multi-selection, (Likert-)scale input, etc.
 */
public class InputElement {

    /**
     * Specifies different possible input element types.
     *
     * PHOTO -
     * TEXT - one or multi-line text input element
     * TAGS - multiple either pre-defined only or also user-defined tags (keywords), different from
     *        multi-select in terms of UI presentation
     */
    public enum InputElementType {
        PHOTO, TEXT, TAGS
    }

    private static final String TAG = "InputElement";

    private Long uid;
    private InputElementType type;
    private String name;
    private Boolean mandatory;
    private HashMap<Restriction.RestrictionType, Restriction> restrictions;
    private HashMap<String, String> options;
    private Long vocabularyUid;
    private Long inputGroupUid;

    private HashMap<String, HashMap<TranslationElement.Target, TranslationElement>> translations;
    private Vocabulary vocabulary;

    public InputElement() {
        uid = null;
        type = null;
        name = null;
        mandatory = Boolean.FALSE;
        restrictions = new HashMap<Restriction.RestrictionType, Restriction>();
        options = new HashMap<String, String>();
        vocabularyUid = null;
        inputGroupUid = null;

        translations = new HashMap<String, HashMap<TranslationElement.Target, TranslationElement>>();
        vocabulary = null;
    }

    public InputElement(long uid) {
        setUid(uid);
        type = null;
        name = null;
        mandatory = Boolean.FALSE;
        restrictions = new HashMap<Restriction.RestrictionType, Restriction>();
        options = new HashMap<String, String>();
        vocabularyUid = null;
        inputGroupUid = null;

        translations = new HashMap<String, HashMap<TranslationElement.Target, TranslationElement>>();
        vocabulary = null;
    }

    /**
     * get unique identifier
     * @return uid (primary key)
     */
    public long getUid() {
        if (uid != null) {
            return uid.longValue();
        }
        else {
            return 0L;
        }
    }

    /**
     * set unique identifier
     * @param uid uid (primary key)
     */
    private void setUid(long uid) {
        this.uid = Long.valueOf(uid);
    }

    /**
     * Sets the input element type (text, tags, photo ...)
     * @param type input element type enum
     */
    public void setType(InputElementType type) {
        this.type = type;
    }

    /**
     * Gets the input element type (text, tags, photo ...)
     * @return input element type, or null if not set
     */
    public InputElementType getType() {
        return type;
    }

    /**
     * Sets the name (string id) of this input element
     * @param name the name (string id)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name (string id) of this input element
     * @return the name (string id), or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets whether it is mandatory to fill in this input item.
     * @param mandatory mandatory or not
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = new Boolean(mandatory);
    }

    /**
     * Gets whether it is mandatory to fill in this input item.
     * @return true if mandatory, false otherwise
     */
    public boolean isMandatory() {
        return mandatory.booleanValue();
    }

    /**
     * Gets options for this input element (string value bundle)
     * @return string in the form "key=value,key=value", or empty string if no options set
     */
    public String getOptions() {
        String optStr = "";
        Iterator<Map.Entry<String, String>> i = options.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, String> entry = i.next();
            optStr += entry.getKey()+"="+entry.getValue();
            if (i.hasNext()) {
                optStr += ",";
            }
        }
        return optStr;
    }

    /**
     * Sets a new option or replaces an existing option
     * @param key key identifier of option
     * @param value value of option
     */
    public void setOption(String key, String value) {
        if (key != null && value != null) {
            options.put(key, value);
        }
    }

    /**
     * Gets an option for this input element
     * @param key key identifier of option
     * @return option value, or null if key does not exist or is null
     */
    public String getOption(String key) {
        if (key != null) {
            return options.get(key);
        }
        return null;
    }

    /**
     * Sets a new restriction or updates an existing one (only one restriction per type can exist)
     * @param restriction restriction to add or update
     */
    public void setRestriction(Restriction restriction) {
        if (restriction != null) {
            restrictions.put(restriction.getType(), restriction);
        }
    }

    /**
     * Gets a restriction according to restriction type
     * @param type restriction type (edit, delete)
     * @return restriction, or null if type is not restricted
     */
    public Restriction getRestriction(Restriction.RestrictionType type) {
        return restrictions.get(type);
    }

    /**
     * Gets restrictions for this input element
     * @return collection of restrictions currently active for this project
     */
    public Collection<Restriction> getRestrictions() {
        return restrictions.values();
    }

    /**
     * Sets (or updates) a translation element for this input element
     * @param element translation element
     */
    public void setTranslation(TranslationElement element) {
        if (element.getLang() != null && element.getTarget() != null) {
            if (!translations.containsKey(element.getLang().getLanguage())) {
                translations.put(element.getLang().getLanguage(), new HashMap<TranslationElement.Target, TranslationElement>());
            }
            HashMap<TranslationElement.Target, TranslationElement> trans = translations.get(element.getLang().getLanguage());
            trans.put(element.getTarget(), element);
        }
    }

    /**
     * Gets a translation element associated to this input element according to the supplied
     * parameters of language and target.
     * @param language language code of the translation
     * @param target target of the translation
     * @return translation element, or null if not set
     */
    public TranslationElement getTranslation(String language, TranslationElement.Target target) {
        if (language != null && target != null) {
            if (translations.containsKey(language)) {
                HashMap<TranslationElement.Target, TranslationElement> trans = translations.get(language);
                if (trans.containsKey(target)) {
                    return trans.get(target);
                }
            }
        }
        return null;
    }

    /**
     * Gets all translation targets for this input element of the specified language
     * @param language language code of the translation
     * @return collection of translation elements, or null if no translations for the specified language
     */
    public Collection<TranslationElement> getTranslations(String language) {
        if (translations.containsKey(language)) {
            return translations.get(language).values();
        }
        return null;
    }

    /**
     * Gets all translations for this input element
     * @return collection of maps mapping targets to translations
     */
    public Collection<HashMap<TranslationElement.Target, TranslationElement>> getTranslations() {
        return translations.values();
    }

    /**
     * Gets the title (input hint)
     * @param language language language code of the translation
     * @return title, or null if not set
     */
    public String getTitle(String language) {
        TranslationElement title = getTranslation(language, TranslationElement.Target.TITLE);
        if (title != null) {
            return title.getContent();
        }
        return null;
    }

    /**
     * Gets a (short) help text that describes what the user has to do
     * @param language language code of the translation
     * @return help text, or null if not set
     */
    public String getHelp(String language) {
        TranslationElement help = getTranslation(language, TranslationElement.Target.HELP);
        if (help != null) {
            return help.getContent();
        }
        return null;
    }

    /**
     * Sets the vocabulary uid which this input item uses for available choices
     * @param vocabularyUid vocabulary uid of source of available choices
     */
    public void setVocabularyUid(long vocabularyUid) {
        this.vocabularyUid = Long.valueOf(vocabularyUid);
    }

    /**
     * Gets the vocabulary uid which this input item uses for available choices
     * @return vocabulary uid of source of available choices, or 0L if not set
     */
    public long getVocabularyUid() {
        if (vocabularyUid != null) {
            return vocabularyUid.longValue();
        }

        return 0L;
    }

    /**
     * Sets the vocabulary which this input item uses for available choices
     * @param vocabulary the vocabulary
     */
    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    /**
     * Gets the vocabulary which this input item uses for available choices
     * @return the vocabulary, or null if not set
     */
    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    /**
     * Sets the input group uid where this input item belongs to
     * @param inputGroupUid input group uid of parent input group
     */
    public void setInputGroupUid(long inputGroupUid) {
        this.inputGroupUid = Long.valueOf(inputGroupUid);
    }

    /**
     * Gets the input group uid where this input item belongs to
     * @return input group uid of parent input group, or 0L if not set
     */
    public long getInputGroupUid() {
        if (inputGroupUid != null) {
            return inputGroupUid.longValue();
        }

        return 0L;
    }

    /**
     * Copies all member variables (except uid) to a new object
     * @param copy copy object
     */
    public void copyTo(InputElement copy) {
        copy.setType(type);
        copy.setName(name);
        copy.setMandatory(mandatory.booleanValue());
        if (vocabularyUid != null) {
            copy.setVocabularyUid(vocabularyUid);
        }
        if (inputGroupUid != null) {
            copy.setInputGroupUid(inputGroupUid);
        }

        Iterator<String> opts = options.keySet().iterator();
        while (opts.hasNext()) {
            String key = opts.next();
            copy.setOption(key, options.get(key));
        }

        Iterator<Restriction.RestrictionType> restr = restrictions.keySet().iterator();
        while (restr.hasNext()) {
            Restriction.RestrictionType key = restr.next();
            copy.setRestriction(restrictions.get(key));
        }

        Iterator<HashMap<TranslationElement.Target, TranslationElement>> transl = translations.values().iterator();
        while (transl.hasNext()) {
            HashMap<TranslationElement.Target, TranslationElement> translMap = transl.next();
            Iterator<TranslationElement> transElemIterator = translMap.values().iterator();
            while (transElemIterator.hasNext()) {
                copy.setTranslation(transElemIterator.next());
            }
        }
    }

    @Override
    public int hashCode() {
        return uid != null ? this.getClass().hashCode() + uid.hashCode() : super.hashCode();
    }
}
