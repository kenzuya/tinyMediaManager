package org.tinymediamanager.scraper.subdb;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import org.tinymediamanager.scraper.SubtitleSearchOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.subdb.service.Controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenSubtitleHashTest {

  private ClassLoader               classLoader = getClass().getClassLoader();
  private SubDBSubtitleProvider     sp          = new SubDBSubtitleProvider();
  private Controller controller                 = new Controller(true, true);
  private File                      dexter      = new File(classLoader.getResource("samples/dexter.mp4").getFile());
  private File                      justified   = new File(classLoader.getResource("samples/justified.mp4").getFile());
  private final String              userAgent   = "SubDB/1.0 (TheSubDB-Scraper/0.1; http://gitlab.com/TinyMediaManager)";

  @Test
  public void testHash() {

    assertThat(sp.subDbGetHash(dexter)).isEqualTo("ffd8d4aa68033dc03d1c8ef373b9028c");
    assertThat(sp.subDbGetHash(justified)).isEqualTo("edc1981d6459c6111fe36205b4aff6c2");

  }

  @Test
  public void testAPISearching() throws IOException {
    String result = controller.getSubtitles(sp.subDbGetHash(justified));
    assertThat(result).isNotNull();
  }

  @Test
  public void testSearch() throws Exception {

    List<SubtitleSearchResult> searchResults;
    SubtitleSearchOptions options = new SubtitleSearchOptions(dexter, "Dexter is cool");
    options.setLanguage(new Locale("en"));
    searchResults = sp.search(options);
    assertThat(searchResults.get(0)).isNotNull();


  }

  @Test
  public void TestStatusCode() throws Exception {

    List<SubtitleSearchResult> searchResults;
    SubtitleSearchOptions options = new SubtitleSearchOptions(dexter, "Dexter is cool");
    options.setLanguage(new Locale("en"));
    searchResults = sp.search(options);
    assertThat(searchResults.get(0)).isNotNull();

    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(searchResults.get(0).getUrl()).addHeader("User-Agent", userAgent).build();
    Response response = client.newCall(request).execute();
    assertThat(response.isSuccessful()).isTrue();
  }

}
