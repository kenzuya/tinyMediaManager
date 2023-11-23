package org.tinymediamanager.scraper.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaGenres;

public class LanguageUtilsTest extends BasicTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    Locale.setDefault(Locale.ENGLISH);
  }

  @Test
  public void asiaLangu() {
    printLangu("en");
    printLangu("fr");
    printLangu("de");

    printLangu("zh"); // simple
    printLangu("zh_"); // invalid -> EN
    printLangu("zh_CN"); // simple
    printLangu("zh_HK"); // traditional (no bundle avail, so fallback to ZH simple!)
    printLangu("zh_SG"); // simple
    printLangu("zh_TW"); // traditional (=Hant)
    printLangu("zh_Hans"); // simple (Java translates this to zh_CN)
    printLangu("zh_Hant"); // traditional (Java translates this to zh_TW)
    printLangu("zh__#Hans"); // simple (no country, but script variant)
    printLangu("zh__#Hant"); // traditional (no country, but script variant)
  }

  private void printLangu(String key) {
    MediaGenres genre = MediaGenres.ANIMAL;
    Locale loc = Utils.getLocaleFromLanguage(key);
    ResourceBundle b = ResourceBundle.getBundle("messages", loc);
    String nfo = genre.getLocalizedName(loc);
    System.out.println(key + "   Bundle:" + b.getString("Genres." + genre.name()) + "   nfo:" + nfo);
  }

  @Test
  public void chin() {
    for (Locale l : Utils.getLanguages()) {
      System.out.println("Name: " + l.getDisplayName() + "   Lang: " + l.getDisplayLanguage() + "   Tag: " + l.toLanguageTag());
    }
    for (Locale l : Utils.getLanguages()) {
      MediaGenres genre = MediaGenres.ANIMAL;
      ResourceBundle b = ResourceBundle.getBundle("messages", l);
      String nfo = genre.getLocalizedName(l);
      System.out.println(l + "   Bundle:" + b.getString("Genres." + genre.name()) + "   nfo:" + nfo);
    }
  }

  @Test
  public void trans() {
    for (String s : Utils.getAllTranslationsFor("metatag.season")) {
      System.out.println(s);
    }
    for (String s : Utils.getAllTranslationsFor("metatag.episode")) {
      System.out.println(s);
    }
  }

  @Test
  public void localizedCountries() {
    assertEqual("", LanguageUtils.getLocalizedCountry());
    System.out.println(LanguageUtils.getLocalizedCountry("German", "dE"));

    // Java 8: Vereinigte Staaten von Amerika
    // Java 9: Vereinigte Staaten
    assertThat(LanguageUtils.getLocalizedCountryForLanguage(Locale.GERMAN, "USA", "en_US", "US")).startsWith("Vereinigte Staaten");
    assertThat(LanguageUtils.getLocalizedCountryForLanguage(Locale.GERMANY, "USA", "en_US", "US")).startsWith("Vereinigte Staaten");
    assertThat(LanguageUtils.getLocalizedCountryForLanguage("de", "USA", "en_US", "US")).startsWith("Vereinigte Staaten");

    assertEqual("United States", LanguageUtils.getLocalizedCountryForLanguage("en", "USA", "en_US", "US"));
    assertEqual("United States", LanguageUtils.getLocalizedCountryForLanguage("en", "Vereinigte Staaten von Amerika", "Vereinigte Staaten"));

    // Java 8: Etats-Unis
    // Java 9: États-Unis
    assertThat(LanguageUtils.getLocalizedCountryForLanguage("fr", "USA", "en_US", "US")).matches("(E|É)tats\\-Unis");
    assertEqual("West Germany", LanguageUtils.getLocalizedCountryForLanguage("de", "West Germany", "XWG"));
  }

  @Test
  public void customTest() {
    assertEqual("Basque", LanguageUtils.getEnglishLanguageNameFromLocalizedString("Basque"));
    assertEqual("Basque", LanguageUtils.getEnglishLanguageNameFromLocalizedString("baq"));
    assertEqual("Baskisch", LanguageUtils.getLocalizedLanguageNameFromLocalizedString(Locale.GERMAN, "Basque"));
    assertEqual("Baskisch", LanguageUtils.getLocalizedLanguageNameFromLocalizedString(Locale.GERMAN, "baq"));
    assertEqual("Basque", LanguageUtils.getLocalizedLanguageNameFromLocalizedString(Locale.ENGLISH, "Baskisch"));

    assertEqual("Telugu", LanguageUtils.getEnglishLanguageNameFromLocalizedString("tel"));
    assertEqual("Tamil", LanguageUtils.getEnglishLanguageNameFromLocalizedString("tam"));
    assertEqual("Telugu", LanguageUtils.getLocalizedLanguageNameFromLocalizedString(Locale.GERMAN, "tel"));
    // ??? assertEqual("Tsongaisch", LanguageUtils.getLocalizedLanguageNameFromLocalizedString(Locale.GERMAN, "tam"));
  }
}
