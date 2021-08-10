package com.essiembre.eclipse.rbe.translators.opentrad;

public enum Language {

    English, Spanish, Galician;

    public String getValue() {
        switch (this) {
            case English:
                return "en";
            case Spanish:
                return "es";
            case Galician:
                return "gl";
            default:
                return "es";
        }
    }
}
