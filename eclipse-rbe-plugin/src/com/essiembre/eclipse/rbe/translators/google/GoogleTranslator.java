package com.essiembre.eclipse.rbe.translators.google;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.json.provisonnal.com.eclipsesource.json.JsonValue;

import com.essiembre.eclipse.rbe.translators.ITranslator;

public class GoogleTranslator implements ITranslator {

    public static final String        CHARSET    = StandardCharsets.UTF_8.displayName();

    public static final String        URL_GOOGLE = "https://translate.googleapis.com/translate_a/";

    protected static GoogleTranslator instance;

    protected GoogleTranslator() {}


    public static GoogleTranslator getInstance() {
        if (GoogleTranslator.instance == null) {
            GoogleTranslator.instance = new GoogleTranslator();
        }
        return GoogleTranslator.instance;
    }


    @Override
    public String translate(String langFrom, String langTo, String textToTranslate) {
        StringBuilder sbTranslation = new StringBuilder();

        try {
            StringBuilder url = new StringBuilder(GoogleTranslator.URL_GOOGLE);
            url.append("single?client=gtx");
            url.append("&sl=" + langFrom);
            url.append("&tl=" + langTo);
            url.append("&dt=t");
            url.append("&q=" + URLEncoder.encode(textToTranslate, GoogleTranslator.CHARSET));

            URL obj = new URL(url.toString());
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), GoogleTranslator.CHARSET));
            String inputLine;

            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String inputJson = response.toString();

            JsonValue jsonArray = JsonValue.readFrom(inputJson);
            JsonValue jsonArray2 = jsonArray.asArray().get(0);

            for (int i = 0; i < jsonArray2.asArray().size(); i++) {
                if (jsonArray2.asArray().get(i) != null) {
                    String text = jsonArray2.asArray().get(i).asArray().get(0).toString();
                    if (text != null && text.startsWith("\"")) {
                        text = text.substring(1, text.length());
                    }
                    if (text != null && text.endsWith("\"")) {
                        text = text.substring(0, text.length() - 1);
                    }
                    sbTranslation.append(text);
                }
            }

        } catch (Exception e) {
            Logger.getGlobal().log(Level.WARNING, e.getMessage());
        }

        return sbTranslation.toString();
    }

}
