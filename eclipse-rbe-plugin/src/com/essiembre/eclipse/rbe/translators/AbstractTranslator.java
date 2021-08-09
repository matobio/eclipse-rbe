package com.essiembre.eclipse.rbe.translators;

import com.essiembre.eclipse.rbe.translators.google.GoogleTranslator;

public abstract class AbstractTranslator {

    public static final String KEY_GOOGLE_API_TRANSLATOR = "Google Translator API";

    public static ITranslator getTranslator(String translatorKey) {

        switch (translatorKey) {

            case KEY_GOOGLE_API_TRANSLATOR:
                return GoogleTranslator.getInstance();

            default:
                return GoogleTranslator.getInstance();

        }
    }
}
