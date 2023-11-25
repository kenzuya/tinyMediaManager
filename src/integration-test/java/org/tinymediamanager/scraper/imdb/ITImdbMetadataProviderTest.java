/*
 * Copyright 2012 - 2021 Manuel Laggner
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tinymediamanager.scraper.imdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSpan;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

public class ITImdbMetadataProviderTest extends BasicITest {

  @Before
  public void setup() throws Exception {
    super.setup();
    MediaProviders.loadMediaProviders();
  }

  @Test
  public void testDynamicReload() throws Exception {
    Document doc = get("https://www.imdb.com/title/tt0105941/episodes/?season=1");
    Document json = get("https://www.imdb.com/tr/?pt=title&spt=episodes&const=tt0105941&ht=actionOnly&pageAction=ttep-seemore");

    // https://caching.graphql.imdb.com/?operationName=TitleEpisodesSubPagePagination&variables={"after":"dHQwNTEzMjAy","const":"tt0105941","filter":{"includeSeasons":["1"]},"first":50,"locale":"de-DE","originalTitleText":false,"returnUrl":"https://www.imdb.com/close_me"}&extensions={"persistedQuery":{"sha256Hash":"ecdbd1a3df08b6a3903b16662bc636e6cbaf6a6e4fcbf64496ab4da46b8038e2","version":1}}
    Document json2 = opt(
        "https://caching.graphql.imdb.com/?operationName=TitleEpisodesSubPagePagination&variables=%7B%22after%22%3A%22dHQwNTEzMjAy%22%2C%22const%22%3A%22tt0105941%22%2C%22filter%22%3A%7B%22includeSeasons%22%3A%5B%221%22%5D%7D%2C%22first%22%3A50%2C%22locale%22%3A%22de-DE%22%2C%22originalTitleText%22%3Afalse%2C%22returnUrl%22%3A%22https%3A%2F%2Fwww.imdb.com%2Fclose_me%22%7D&extensions=%7B%22persistedQuery%22%3A%7B%22sha256Hash%22%3A%22ecdbd1a3df08b6a3903b16662bc636e6cbaf6a6e4fcbf64496ab4da46b8038e2%22%2C%22version%22%3A1%7D%7D");
    Document json3 = get(
        "https://caching.graphql.imdb.com/?operationName=TitleEpisodesSubPagePagination&variables=%7B%22after%22%3A%22dHQwNTEzMjAy%22%2C%22const%22%3A%22tt0105941%22%2C%22filter%22%3A%7B%22includeSeasons%22%3A%5B%221%22%5D%7D%2C%22first%22%3A50%2C%22locale%22%3A%22de-DE%22%2C%22originalTitleText%22%3Afalse%2C%22returnUrl%22%3A%22https%3A%2F%2Fwww.imdb.com%2Fclose_me%22%7D&extensions=%7B%22persistedQuery%22%3A%7B%22sha256Hash%22%3A%22ecdbd1a3df08b6a3903b16662bc636e6cbaf6a6e4fcbf64496ab4da46b8038e2%22%2C%22version%22%3A1%7D%7D");
    System.out.println(json3.body());
  }

  private Map<String, String> cookies = new HashMap<String, String>();

  public Document get(String url) throws IOException {
    Connection connection = Jsoup.connect(url).ignoreContentType(true);
    for (Entry<String, String> cookie : cookies.entrySet()) {
      connection.cookie(cookie.getKey(), cookie.getValue());
    }
    connection.header("Origin", "https://www.imdb.com");
    connection.header("Referer", "https://www.imdb.com");
    Response response = connection.execute();
    cookies.putAll(response.cookies());
    return response.parse();
  }

  public Document opt(String url) throws IOException {
    Connection connection = Jsoup.connect(url).ignoreContentType(true);
    for (Entry<String, String> cookie : cookies.entrySet()) {
      connection.cookie(cookie.getKey(), cookie.getValue());
    }
    connection.header("Origin", "https://www.imdb.com");
    connection.header("Referer", "https://www.imdb.com");
    connection.method(Method.OPTIONS);
    Response response = connection.execute();
    cookies.putAll(response.cookies());
    return response.parse();
  }

  @Test
  public void webdriver() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    WebClient webClient = new WebClient();
    webClient.getOptions().setThrowExceptionOnScriptError(false); // woah

    HtmlPage myPage = webClient.getPage("https://www.imdb.com/title/tt0105941/episodes/?season=1");

    HtmlSpan span = myPage.getFirstByXPath("//span[contains(@class, 'single-page-see-more-button')]");

    HtmlButton htmlButton = (HtmlButton) span.getFirstElementChild();
    HtmlPage pageAfterClick = (HtmlPage) htmlButton.click();
    webClient.waitForBackgroundJavaScript(5000 * 2);
    System.out.println(pageAfterClick.asXml());

  }

  @Test
  public void testMovieSearch() {
    IMovieMetadataProvider mp = null;
    List<MediaSearchResult> results = null;

    /*
     * test on akas.imdb.com - "9"
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("9");
      options.setSearchYear(2016);
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      results = new ArrayList<>(mp.search(options));

      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertThat(results.size()).isGreaterThanOrEqualTo(15);

      // check if the result is found (9 - 2016 - tt5719388)
      checkSearchResult("9", 2016, "tt5719388", results);

      // check second result (9 - 2009 - tt0472033)
      checkSearchResult("9", 2009, "tt0472033", results);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * test on www.imdb.com - "Inglorious Basterds"
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Inglorious Basterds");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      results = new ArrayList<>(mp.search(options));

      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertThat(results.size()).isGreaterThanOrEqualTo(1);

      // check first result (Inglourious Basterds - 2009 - tt0361748)
      checkSearchResult("Inglourious Basterds", 2009, "tt0361748", results);

      // check second result (The Real Inglorious Bastards - 2012 - tt3320110)
      checkSearchResult("The Real Inglorious Bastards", 2012, "tt3320110", results);

    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * test on www.imdb.com - "Asterix der Gallier" in de
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Asterix der Gallier");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      results = new ArrayList<>(mp.search(options));

      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertThat(results.size()).isGreaterThanOrEqualTo(1);

      // check first result (Asterix der Gallier - 1967 - tt0061369)
      checkSearchResult("Asterix der Gallier", 1967, "tt0061369", results);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowSearch() {
    ITvShowMetadataProvider mp = null;
    List<MediaSearchResult> results = null;

    /*
     * test on akas.imdb.com - "Psych"
     */
    try {
      mp = new ImdbTvShowMetadataProvider();
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setSearchQuery("Psych");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      results = new ArrayList<>(mp.search(options));

      // did we get a result?
      assertNotNull("Result", results);

      // result count
      assertThat(results.size()).isGreaterThanOrEqualTo(1);

      // check first result (Psych - 2006 - tt0491738)
      checkSearchResult("Psych", 2006, "tt0491738", results);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeListing() {
    ITvShowMetadataProvider mp = null;
    List<MediaMetadata> episodes = null;

    /*
     * test on akas.imdb.com - Psych (tt0491738)
     */
    try {
      mp = new ImdbTvShowMetadataProvider();
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setId(mp.getProviderInfo().getId(), "tt0491738");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      episodes = mp.getEpisodeList(options);

      // did we get a result?
      assertNotNull("Episodes", episodes);

      // result count
      assertThat(episodes.size()).isGreaterThanOrEqualTo(120);

    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowScrape() {
    ITvShowMetadataProvider mp = null;
    TvShowSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    /*
     * test on akas.imdb.com - Psych (tt0491738)
     */
    try {
      mp = new ImdbTvShowMetadataProvider();
      options = new TvShowSearchAndScrapeOptions();
      options.setImdbId("tt0491738");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertEquals("Psych", md.getTitle());
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * test on akas.imdb.com - Firefly (tt0303461)
     */
    try {
      mp = new ImdbTvShowMetadataProvider();
      options = new TvShowSearchAndScrapeOptions();
      options.setImdbId("tt0303461");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertThat(md.getTitle()).contains("Firefly", "Der Aufbruch der Serenity");
      assertEquals("Firefly", md.getOriginalTitle());
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testTvShowScrapeWithTmdb() {
    ITvShowMetadataProvider mp = null;
    TvShowSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    MediaProviders.loadMediaProviders();

    /*
     * test on akas.imdb.com - Psych (tt0491738)
     */
    try {
      mp = new ImdbTvShowMetadataProvider();
      mp.getProviderInfo().getConfig().setValue(ImdbParser.USE_TMDB_FOR_TV_SHOWS, Boolean.TRUE);
      options = new TvShowSearchAndScrapeOptions();
      options.setImdbId("tt0491738");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertEquals("Psych", md.getTitle());
      assertThat(md.getIds().size()).isGreaterThanOrEqualTo(1);
      assertThat(md.getPlot()).startsWith("Shawn Spencer ist selbsternannter Detektiv");
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeScrape() {
    ITvShowMetadataProvider mp = null;
    TvShowEpisodeSearchAndScrapeOptions options = null;
    MediaMetadata md = null;
    SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.US);

    /*
     * test on akas.imdb.com - Psych (tt0491738)
     */
    // S1E1
    try {
      mp = new ImdbTvShowMetadataProvider();
      options = new TvShowEpisodeSearchAndScrapeOptions();
      options.setTvShowIds(Collections.singletonMap(MediaMetadata.IMDB, "tt0491738"));
      TvShowModuleManager.getInstance().getSettings().setCertificationCountry(CountryCode.US);
      options.setLanguage(MediaLanguages.en);
      options.setId(MediaMetadata.SEASON_NR, "1");
      options.setId(MediaMetadata.EPISODE_NR, "1");
      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertEquals("Pilot", md.getTitle());
      assertThat(md.getPlot()).contains("police", "Santa Barbara");
      assertEquals("7 July 2006", sdf.format(md.getReleaseDate()));
      assertThat(md.getCastMembers(ACTOR)).isNotEmpty();
      assertThat(md.getCastMembers(DIRECTOR)).isNotEmpty();
      assertEquals("Michael Engler", md.getCastMembers(DIRECTOR).get(0).getName());
      assertThat(md.getCastMembers(WRITER)).isNotEmpty();
      assertEquals("Steve Franks", md.getCastMembers(WRITER).get(0).getName());

      assertThat(md.getRatings()).isNotEmpty();
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);

      assertThat(md.getIds()).containsKeys(MediaMetadata.IMDB);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    // S3E12
    try {
      mp = new ImdbTvShowMetadataProvider();
      options = new TvShowEpisodeSearchAndScrapeOptions();
      options.setTvShowIds(Collections.singletonMap(MediaMetadata.IMDB, "tt0491738"));
      TvShowModuleManager.getInstance().getSettings().setCertificationCountry(CountryCode.US);
      options.setLanguage(MediaLanguages.en);
      options.setId(MediaMetadata.SEASON_NR, "3");
      options.setId(MediaMetadata.EPISODE_NR, "12");
      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertEquals("Earth, Wind and... Wait for It", md.getTitle());
      assertThat(md.getPlot()).isNotEmpty();
      assertEquals("23 January 2009", sdf.format(md.getReleaseDate()));

      assertThat(md.getRatings()).isNotEmpty();
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);

      assertThat(md.getIds()).containsKeys(MediaMetadata.IMDB);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testEpisodeWithoutShowLink() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // uncategorized episode "Misfits: Erazer" which is basically an episode from Doctor Who
    ITvShowMetadataProvider mp = new ImdbTvShowMetadataProvider();
    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    TvShowModuleManager.getInstance().getSettings().setCertificationCountry(CountryCode.US);
    options.setId(MediaMetadata.IMDB, "tt2358073");
    options.setLanguage(MediaLanguages.en);

    MediaMetadata md = mp.getMetadata(options);

    // did we get metadata?
    assertNotNull("MediaMetadata", md);

    assertEquals("Misfits: Erazer", md.getTitle());
    assertThat(md.getPlot()).isNotEmpty();
    assertEquals("2011-12-04", sdf.format(md.getReleaseDate()));

    assertThat(md.getRatings()).isNotEmpty();
    MediaRating mediaRating = md.getRatings().get(0);
    assertThat(mediaRating.getId()).isNotEmpty();
    assertThat(mediaRating.getRating()).isGreaterThan(0);
    assertThat(mediaRating.getVotes()).isGreaterThan(0);
    assertThat(mediaRating.getMaxValue()).isEqualTo(10);

    assertThat(md.getIds()).containsKeys(MediaMetadata.IMDB);

    assertThat(md.getCastMembers()).isNotEmpty();
  }

  @Test
  public void testEpisodeScrapeWithTmdb() {
    ITvShowMetadataProvider mp = null;
    TvShowEpisodeSearchAndScrapeOptions options = null;
    MediaMetadata md = null;
    SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.US);

    MediaProviders.loadMediaProviders();

    /*
     * test on akas.imdb.com - Psych (tt0491738)
     */
    // S1E1
    try {
      mp = new ImdbTvShowMetadataProvider();
      options = new TvShowEpisodeSearchAndScrapeOptions();
      mp.getProviderInfo().getConfig().setValue(ImdbParser.USE_TMDB_FOR_TV_SHOWS, Boolean.TRUE);
      options.setTvShowIds(Collections.singletonMap(MediaMetadata.IMDB, "tt0491738"));
      TvShowModuleManager.getInstance().getSettings().setCertificationCountry(CountryCode.US);
      options.setLanguage(MediaLanguages.de);
      options.setId(MediaMetadata.SEASON_NR, "1");
      options.setId(MediaMetadata.EPISODE_NR, "1");
      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      assertEquals("Mit einer Ausrede fängt es an", md.getTitle());
      assertThat(md.getPlot()).contains("Shawn", "Polizei");
      assertEquals("30 October 2007", sdf.format(md.getReleaseDate())); // DE release, but in EN style, eeew
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThanOrEqualTo(27);
      assertEquals(1, md.getCastMembers(DIRECTOR).size());
      assertEquals("Michael Engler", md.getCastMembers(DIRECTOR).get(0).getName());
      assertEquals(1, md.getCastMembers(WRITER).size());
      assertEquals("Steve Franks", md.getCastMembers(WRITER).get(0).getName());
      assertThat(md.getIds().size()).isGreaterThan(1);

      assertThat(md.getRatings()).isNotEmpty();
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(10);

      assertThat(md.getIds()).containsKeys(MediaMetadata.IMDB);
      assertThat(md.getIds()).containsKeys(MediaMetadata.TMDB);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  private void checkSearchResult(String title, int year, String imdbId, List<MediaSearchResult> results) {
    boolean found = true;

    for (MediaSearchResult result : results) {
      if (result.getTitle().equalsIgnoreCase(title) && result.getYear() == year && result.getIMDBId().equalsIgnoreCase(imdbId)) {
        found = true;
        break;
      }
    }

    assertThat(found).isEqualTo(true);
  }

  @Test
  public void testMovieScrape() {
    IMovieMetadataProvider mp = null;
    MovieSearchAndScrapeOptions options = null;
    MediaMetadata md = null;

    /*
     * scrape www.imdb.com - 9 - tt0472033
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      mp.getProviderInfo().getConfig().addBoolean("scrapeLanguageNames", false);
      mp.getProviderInfo().getConfig().addBoolean("scrapeKeywordsPage", true);
      options = new MovieSearchAndScrapeOptions();
      options.setImdbId("tt0472033");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      // check moviedetails
      checkMovieDetails("9", 2009, "9", 7.0, 63365, "(1) To Protect Us...", 79, "Shane Acker", "Pamela Pettler, Shane Acker, Ben Gluck", "PG-13",
          "09-09-2009", md);

      assertThat(md.getGenres().size()).isGreaterThan(0);
      assertThat(md.getPlot()).isNotEmpty();
      checkCastMembers(md);
      checkMoviePoster(md);
      checkProductionCompany(md);

      // check localized values
      assertThat(md.getCountries()).contains("US");
      assertThat(md.getSpokenLanguages()).containsOnly("en");

      assertThat(md.getTags()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * scrape www.imdb.com - 12 Monkeys - tt0114746
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      mp.getProviderInfo().getConfig().setValue("localReleaseDate", true);
      mp.getProviderInfo().getConfig().setValue("scrapeLanguageNames", false);
      options = new MovieSearchAndScrapeOptions();
      options.setImdbId("tt0114746");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      // check moviedetails
      checkMovieDetails("12 Monkeys", 1995, "Twelve Monkeys", 8.1, 262821, "The future is history.", 129, "Terry Gilliam",
          "Chris Marker, David Webb Peoples, Janet Peoples", "16", "01-02-1996", md);

      assertThat(md.getGenres()).isNotEmpty();
      assertThat(md.getPlot()).contains("Virus");
      checkCastMembers(md);
      checkProductionCompany(md);
      checkMoviePoster(md);

      // check localized values
      assertThat(md.getCountries()).contains("US");
      assertThat(md.getSpokenLanguages()).contains("en", "fr");

      assertThat(md.getTags()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * scrape www.imdb.com - Brave - tt1217209
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setImdbId("tt1217209");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.GB);
      options.setReleaseDateCountry("GB");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      // check moviedetails
      checkMovieDetails("Brave", 2012, "Brave", 7.3, 52871, "Change your fate.", 93, "Mark Andrews, Brenda Chapman, Steve Purcell",
          "Brenda Chapman, Mark Andrews, Steve Purcell, Irene Mecchi", "PG", "02-08-2012", md);

      assertThat(md.getGenres()).isNotEmpty();
      assertThat(md.getPlot()).contains("Merida");
      checkCastMembers(md);
      checkProductionCompany(md);
      checkMoviePoster(md);
      assertThat(md.getTags()).isNotEmpty();

    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * scrape akas.imdb.com - Brave - tt1217209 - in DE
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setImdbId("tt1217209");
      options.setLanguage(MediaLanguages.de);
      options.setCertificationCountry(CountryCode.DE);
      options.setReleaseDateCountry("DE");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      // check moviedetails
      checkMovieDetails("Merida - Legende der Highlands", 2012, "Brave", 7.3, 52871, "Change your fate.", 93,
          "Mark Andrews, Brenda Chapman, Steve Purcell", "Brenda Chapman, Mark Andrews, Steve Purcell, Irene Mecchi", "PG", "02-08-2012", md);

      assertThat(md.getTags()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * scrape www.imdb.com - Winnebago Man - tt1396557
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setImdbId("tt1396557");
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      // check moviedetails
      checkMovieDetails("Winnebago Man", 2009, "Winnebago Man", 7.2, 3890, "", 85, "Ben Steinbauer",
          "Malcolm Pullinger, Ben Steinbauer, Louisa Hall, Joel Heller, Berndt Mader, Natasha Rosow", "", "14-03-2009", md);

      assertThat(md.getGenres()).isNotEmpty();
      assertThat(md.getPlot()).contains("Rebney", "Winnebago");
      checkCastMembers(md);
      checkProductionCompany(md);
      checkMoviePoster(md);

      assertThat(md.getTags()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    /*
     * Despicable Me - tmdb id 20352
     */
    try {
      mp = new ImdbMovieMetadataProvider();
      options = new MovieSearchAndScrapeOptions();
      options.setTmdbId(20352);
      options.setLanguage(MediaLanguages.en);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");

      md = mp.getMetadata(options);

      // did we get metadata?
      assertNotNull("MediaMetadata", md);

      // check moviedetails
      checkMovieDetails("Despicable Me", 2010, "Despicable Me", 7.6, 488112, "Superbad. Superdad.", 95, "Pierre Coffin, Chris Renaud",
          "Cinco Paul, Ken Daurio, Sergio Pablos", "", "09-07-2010", md);

      assertThat(md.getGenres()).isNotEmpty();
      assertThat(md.getPlot()).contains("criminal", "orphan");
      checkCastMembers(md);
      checkProductionCompany(md);
      checkMoviePoster(md);

      assertThat(md.getTags()).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  private void checkMovieDetails(String title, int year, String originalTitle, double rating, int voteCount, String tagline, int runtime,
      String director, String writer, String certification, String releaseDate, MediaMetadata md) {
    // title
    assertEquals("title ", title, md.getTitle());
    // year
    assertEquals("year", year, md.getYear());
    // original title
    assertEquals("originalTitle", originalTitle, md.getOriginalTitle());
    // rating
    assertThat(md.getRatings().size()).isGreaterThanOrEqualTo(1);
    MediaRating mediaRating = md.getRatings().get(0);

    assertEquals("rating", rating, mediaRating.getRating(), 0.5);
    // count (only check if parsed count count is smaller than the given votecount)
    if (voteCount > mediaRating.getVotes()) {
      assertEquals("count", voteCount, (int) mediaRating.getVotes());
    }
    // tagline
    assertEquals("tagline", tagline, md.getTagline());
    // runtime
    assertEquals("runtime", runtime, (int) md.getRuntime());
    // director
    StringBuilder sb = new StringBuilder();
    for (Person cm : md.getCastMembers(DIRECTOR)) {
      if (StringUtils.isNotEmpty(sb)) {
        sb.append(", ");
      }
      sb.append(cm.getName());
    }
    assertEquals("director", director, sb.toString());
    // writer
    sb = new StringBuilder();
    for (Person cm : md.getCastMembers(WRITER)) {
      if (StringUtils.isNotEmpty(sb)) {
        sb.append(", ");
      }
      sb.append(cm.getName());
    }
    assertEquals("writer", writer, sb.toString());

    // date can differ depending on the IP address
    assertNotEquals("release date", null, md.getReleaseDate());
    // certification
    // assertEquals("certification",
    // Certification.getCertification(MovieModuleManager.MOVIE_SETTINGS.getCertificationCountry(),
    // certification), md
    // .getCertifications().get(0));
  }

  private void checkMoviePoster(MediaMetadata md) {
    assertThat(md.getMediaArt()).isNotEmpty();
    assertThat(md.getMediaArt().get(0).getImageSizes()).isNotEmpty();
  }

  private void checkCastMembers(MediaMetadata md) {
    assertThat(md.getCastMembers(ACTOR)).isNotEmpty();
    // at least the must have a name/role
    for (Person member : md.getCastMembers(ACTOR)) {
      assertThat(member.getName()).isNotEmpty();
      assertThat(member.getRole()).isNotEmpty();
    }
  }

  private void checkProductionCompany(MediaMetadata md) {
    assertThat(md.getProductionCompanies()).isNotEmpty();
    assertThat(md.getProductionCompanies().get(0)).isNotEmpty();
  }

  @Test
  public void testMovieArtworkScrapeWithImdbId() throws Exception {
    IMovieArtworkProvider mp = new ImdbMovieArtworkProvider();

    ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.MOVIE);
    options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
    options.setImdbId("tt1217209");

    List<MediaArtwork> artworks = mp.getArtwork(options);

    assertThat(artworks).isNotEmpty();
    assertThat(artworks.get(0).getType()).isEqualTo(MediaArtwork.MediaArtworkType.POSTER);
    assertThat(artworks.get(0).getImageSizes()).isNotEmpty();
    assertThat(artworks.get(0).getPreviewUrl()).isNotEmpty();
  }

  @Test
  public void testMovieArtworkScrapeWithTmdbId() throws Exception {
    IMovieArtworkProvider mp = new ImdbMovieArtworkProvider();

    ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.MOVIE);
    options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
    options.setTmdbId(20352);

    List<MediaArtwork> artworks = mp.getArtwork(options);

    assertThat(artworks).isNotEmpty();
    assertThat(artworks.get(0).getType()).isEqualTo(MediaArtwork.MediaArtworkType.POSTER);
    assertThat(artworks.get(0).getImageSizes()).isNotEmpty();
    assertThat(artworks.get(0).getPreviewUrl()).isNotEmpty();
  }

  @Test
  public void testTvShowArtworkScrapeWithImdbId() throws Exception {
    ITvShowArtworkProvider mp = new ImdbTvShowArtworkProvider();

    ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.TV_SHOW);
    options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
    options.setImdbId("tt0491738");

    List<MediaArtwork> artworks = mp.getArtwork(options);

    assertThat(artworks).isNotEmpty();
    assertThat(artworks.get(0).getType()).isEqualTo(MediaArtwork.MediaArtworkType.POSTER);
    assertThat(artworks.get(0).getImageSizes()).isNotEmpty();
    assertThat(artworks.get(0).getPreviewUrl()).isNotEmpty();
  }
}
