package com.essiembre.eclipse.rbe.translators;

import com.essiembre.eclipse.rbe.translators.google.GoogleTranslator;
import com.essiembre.eclipse.rbe.translators.opentrad.OpenTradTranslator;

public abstract class TranslatorHandler {

    public enum TranslatorType {

        GOOGLE_TRANSLATOR("Google Translator"), OPENTRAD_TRANSLATOR("Traductor OpenTrad");

        private String text;

        TranslatorType(String text) {
            this.text = text;
        }


        public String getText() {
            return this.text;
        }

    };

    public static ITranslator getTranslator(TranslatorType translatorType) {

        switch (translatorType) {
            case GOOGLE_TRANSLATOR:
                return GoogleTranslator.getInstance();
            case OPENTRAD_TRANSLATOR:
                return OpenTradTranslator.getInstance();
            default:
                return GoogleTranslator.getInstance();

        }
    }
}
