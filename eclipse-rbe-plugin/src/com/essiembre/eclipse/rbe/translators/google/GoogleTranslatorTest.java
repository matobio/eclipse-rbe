package com.essiembre.eclipse.rbe.translators.google;

import org.junit.Assert;
import org.junit.Test;

public class GoogleTranslatorTest {

    @Test
    public void testTranslate() {

        String text = GoogleTranslator.getInstance().translate("en_GB", "es_ES", "Hello");
        Assert.assertEquals("\"Hola\"", text);

        // text = GoogleTranslator.getInstance().translate("en_GB", "es_ES", "Hello\nGoodbye");
        // Assert.assertEquals("\"Hola\nAdiós\"", text);
    }

}
