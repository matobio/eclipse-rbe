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

    public static final Map<Language, Language> directions = new HashMap<>();
    static {
        Language.directions.put(Spanish, English);
        Language.directions.put(Spanish, Galician);
        Language.directions.put(Spanish, Catalan);
        Language.directions.put(Spanish, Portuguese);
        Language.directions.put(Spanish, French);
        Language.directions.put(Spanish, Italian);

        Language.directions.put(English, Catalan);
        Language.directions.put(English, Spanish);
        Language.directions.put(English, Galician);
    }
}
