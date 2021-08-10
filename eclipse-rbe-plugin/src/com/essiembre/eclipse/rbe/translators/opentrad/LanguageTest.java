package com.essiembre.eclipse.rbe.translators.opentrad;

import org.junit.Assert;
import org.junit.Test;

public class LanguageTest {

    @Test
    public void testOf() {
        Assert.assertEquals(Language.Spanish, Language.of("es"));
        Assert.assertEquals(Language.Spanish, Language.of("es_ES"));
        Assert.assertEquals(Language.Spanish, Language.of("es-ES"));

        Assert.assertEquals(Language.Catalan, Language.of("ca"));
        Assert.assertEquals(Language.Catalan, Language.of("ca_ES"));
        Assert.assertEquals(Language.Catalan, Language.of("ca-ES"));

        Assert.assertEquals(Language.English, Language.of("en"));
        Assert.assertEquals(Language.English, Language.of("en_GB"));
        Assert.assertEquals(Language.English, Language.of("en_GB"));
    }


    @Test
    public void testIsValidLanguages() {
        Assert.assertTrue(Language.isValidLanguages(Language.Spanish, Language.English));
        Assert.assertFalse(Language.isValidLanguages(Language.English, Language.Italian));

    }

}
