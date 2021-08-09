package com.essiembre.eclipse.rbe.translators.opentrad;

public enum Language {

    English, Spanish;

    public String getValue() {
        switch (this) {
            case English:
                return "en";
            case Spanish:
                return "es";
            default:
                return "es";
        }
    }
}
