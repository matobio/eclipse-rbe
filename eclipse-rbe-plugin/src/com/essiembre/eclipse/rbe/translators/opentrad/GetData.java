package com.essiembre.eclipse.rbe.translators.opentrad;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class GetData {

    private final String   sourceText;

    private final String   destText = "";

    private final Language sourceLanguage;

    private final Language destLanguage;

    public GetData(Language sourceLanguage, Language destLanguage, String text) {
        this.sourceText = text;
        this.sourceLanguage = sourceLanguage;
        this.destLanguage = destLanguage;
    }


    public String getDataUrlEncoded() throws UnsupportedEncodingException {
        final StringBuilder data = new StringBuilder();

        data.append(URLEncoder.encode("cuadrotexto", "UTF-8") + "=" + URLEncoder.encode(this.sourceText, "UTF-8"));
        data.append("&");
        data.append(URLEncoder.encode("idioma-original_texto", "UTF-8") + "=" + URLEncoder.encode(this.sourceLanguage.getValue(), "UTF-8"));
        data.append("&");
        data.append(URLEncoder.encode("idioma-traducir_texto", "UTF-8") + "=" + URLEncoder.encode(this.destLanguage.getValue(), "UTF-8"));
        data.append("&");
        data.append(URLEncoder.encode("cuadrotexto2", "UTF-8") + "=" + URLEncoder.encode(this.destText, "UTF-8"));

        return data.toString();
    }

}
