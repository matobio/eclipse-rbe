package com.essiembre.eclipse.rbe.translators.opentrad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.essiembre.eclipse.rbe.translators.ITranslator;

public class OpenTradTranslator implements ITranslator {

    public static final String          CHARSET      = StandardCharsets.UTF_8.displayName();

    private static final String         URL_OPENTRAD = "http://www.opentrad.com:80/es/opentrad/traducir";

    protected static OpenTradTranslator instance;

    protected OpenTradTranslator() {}


    public static OpenTradTranslator getInstance() {
        if (OpenTradTranslator.instance == null) {
            OpenTradTranslator.instance = new OpenTradTranslator();
        }
        return OpenTradTranslator.instance;
    }


    @Override
    public String translate(String langFrom, String langTo, String textToTranslate) {
        // TODO
        return null;
    }


    public String translate(Language sourceLanguage, Language destLanguage, String text) {

        String translatedText = "";

        try {
            translatedText = this.sendHttpRequest(new GetData(sourceLanguage, destLanguage, text));
        } catch (Exception e) {
            Logger.getGlobal().log(Level.WARNING, e.getMessage());
        }
        return translatedText;
    }


    public String sendHttpRequest(GetData getData) throws IOException {
        final String contentType = "application/x-www-form-urlencoded";

        URLConnection conn = this.getUrlWithParameters(getData).openConnection();
        conn.setRequestProperty("Accept-Charset", OpenTradTranslator.CHARSET);
        conn.setRequestProperty("Content-Type", contentType + ";charset=" + OpenTradTranslator.CHARSET);
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setDoInput(true);
        return this.readResponse(conn);

    }


    private URL getUrlWithParameters(GetData getData) throws MalformedURLException, UnsupportedEncodingException {
        return new URL(OpenTradTranslator.URL_OPENTRAD + "?" + getData.getDataUrlEncoded());
    }


    private String readResponse(URLConnection conn) throws IOException {
        StringBuilder response = new StringBuilder();

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();

        return URLDecoder.decode(response.toString(), OpenTradTranslator.CHARSET);
    }
}
