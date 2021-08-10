package com.essiembre.eclipse.rbe.translators.opentrad;

import java.util.HashMap;
import java.util.Map;

public enum Language {

    English("en"), Spanish("es"), Galician("gl"), Portuguese("pt"), Catalan("ca"), French("fr"), Italian("it");

    private String value;

    Language(String value) {
        this.value = value;
    }


    public String getValue() {
        return this.value;
    }

    public static final Map<Language, Language[]> directions = new HashMap<>();
    static {
        Language.directions.put(Spanish, new Language[] { English, Galician, Catalan, Portuguese, French, Italian });
        Language.directions.put(English, new Language[] { Catalan, Galician, Spanish });
    }

    public static boolean isValidLanguages(Language languageFrom, Language languageTo) {
        if (languageFrom == null || languageTo == null) {
            return false;
        }
        Language[] validLanguages = Language.directions.get(languageFrom);
        for (int i = 0; i < validLanguages.length; i++) {
            if (validLanguages[i].equals(languageTo)) {
                return true;
            }
        }
        return false;
    }


    public static Language of(String lang) {
        if (lang == null || lang.isEmpty()) {
            return null;
        }

        if (lang.contains("-")) {
            lang = lang.substring(0, lang.indexOf("-"));
        } else if (lang.contains("_")) {
            lang = lang.substring(0, lang.indexOf("_"));
        }

        for (int i = 0; i < Language.values().length; i++) {
            if (Language.values()[i].getValue().equals(lang)) {
                return Language.values()[i];
            }
        }
        return null;
    }
}
