/*
 * Copyright 2012 - 2020 Manuel Laggner
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

package org.tinymediamanager.scraper.universal_tvshow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

public class ITUniversalTvShowMetadataProviderTest {

  @BeforeClass
  public static void setUp() {
    // load all classpath plugins
    MediaProviders.loadMediaProviders();
  }

  @Test
  public void testSearchTmdb() {
    callSearch("tmdb", "Futurama");
  }

  @Test
  public void testSearchImdb() {
    callSearch("imdb", "Futurama");
  }

  @Test
  public void testSearchTvDb() {
    callSearch("tvdb", "Futurama");
  }

  @Test
  public void testSearchTrakt() {
    callSearch("trakt", "Futurama");
  }

  private void callSearch(String providerId, String searchString) {
    try {
      ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();

      mp.getProviderInfo().getConfig().setValue("search", providerId);

      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setSearchQuery(searchString);
      options.setLanguage(MediaLanguages.en);
      List<MediaSearchResult> results = new ArrayList<>(mp.search(options));

      // did we get a result?
      assertThat(results).isNotEmpty();

      // are our results from the chosen provider?
      for (MediaSearchResult result : results) {
        // check this via the ID which must exist for the given scraper
        assertThat(result.getIdAsString(providerId)).isNotBlank();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTvdbFuturama() {
    try {
      scrapeFromTvdbFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTvdbFuturamaWithTmdbid() {
    try {
      scrapeFromTvdbFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTvdbFuturamaWithImdbid() {
    try {
      scrapeFromTvdbFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void scrapeFromTvdbFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "tvdb");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isEmpty(); // no original title at tvbd
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isNotEqualByComparingTo(MediaAiredStatus.UNKNOWN);
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isEmpty(); // no tags at tvdb
  }

  @Test
  public void testScrapeFromTmdbFuturama() {
    try {
      scrapeFromTmdbFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTmdbFuturamaWithTvdbId() {
    try {
      scrapeFromTmdbFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTmdbFuturamaWithImdbId() {
    try {
      scrapeFromTmdbFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void scrapeFromTmdbFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "tmdb");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isNotEmpty();
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isNotEqualByComparingTo(MediaAiredStatus.UNKNOWN);
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isNotEmpty();
  }

  @Test
  public void testScrapeFromImdbFuturama() {
    try {
      scrapeFromImdbFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromImdbFuturamaWithTvdbId() {
    try {
      scrapeFromImdbFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromImdbFuturamaWithTmdbId() {
    try {
      scrapeFromImdbFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void scrapeFromImdbFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "imdb");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isNotEmpty();
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isEqualByComparingTo(MediaAiredStatus.UNKNOWN); // not available at imdb
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isNotEmpty();
  }

  private void scrapeFromTraktFuturama(String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    createSingleScraperConfig(mp, "trakt");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId("tmdb", "615");
    options.setLanguage(MediaLanguages.en);

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata.getIds()).containsValues(73871, "tt0149460");
    assertThat(mediaMetadata.getTitle()).isEqualTo("Futurama");
    assertThat(mediaMetadata.getOriginalTitle()).isEmpty(); // no original title from trakt
    assertThat(mediaMetadata.getYear()).isEqualTo(1999);
    assertThat(mediaMetadata.getReleaseDate()).isNotNull();
    assertThat(mediaMetadata.getRuntime()).isGreaterThan(0);
    assertThat(mediaMetadata.getPlot()).isNotEmpty();
    assertThat(mediaMetadata.getRatings()).isNotEmpty();
    assertThat(mediaMetadata.getStatus()).isNotEqualByComparingTo(MediaAiredStatus.UNKNOWN);
    assertThat(mediaMetadata.getGenres()).isNotEmpty();
    assertThat(mediaMetadata.getCertifications()).isNotEmpty();
    assertThat(mediaMetadata.getProductionCompanies()).isNotEmpty();
    assertThat(mediaMetadata.getCastMembers()).isNotEmpty();
    assertThat(mediaMetadata.getTags()).isEmpty(); // no tags from trakt
  }

  @Test
  public void testScrapeFromTraktFuturamaWithTmdbId() {
    try {
      scrapeFromTraktFuturama("tmdb", "615");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTraktFuturamaWithTvdbId() {
    try {
      scrapeFromTraktFuturama("tvdb", "73871");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeFromTraktFuturamaWithImdbId() {
    try {
      scrapeFromTraktFuturama("imdb", "tt0149460");
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  private void createSingleScraperConfig(ITvShowMetadataProvider mp, String providerId) {
    mp.getProviderInfo().getConfig().setValue("title", providerId);
    mp.getProviderInfo().getConfig().setValue("originalTitle", providerId);
    mp.getProviderInfo().getConfig().setValue("year", providerId);
    mp.getProviderInfo().getConfig().setValue("releaseDate", providerId);
    mp.getProviderInfo().getConfig().setValue("runtime", providerId);
    mp.getProviderInfo().getConfig().setValue("plot", providerId);
    mp.getProviderInfo().getConfig().setValue("genres", providerId);
    mp.getProviderInfo().getConfig().setValue("certifications", providerId);
    mp.getProviderInfo().getConfig().setValue("ratings", providerId);
    mp.getProviderInfo().getConfig().setValue("productionCompanies", providerId);
    mp.getProviderInfo().getConfig().setValue("castMembers", providerId);
    mp.getProviderInfo().getConfig().setValue("tags", providerId);
    mp.getProviderInfo().getConfig().setValue("status", providerId);
    mp.getProviderInfo().getConfig().setValue("countries", providerId);
  }

  @Test
  public void testGetEpisodeListingFromTvdbFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("tvdb", "tvdb", "73871");
    getEpisodeListingEpisode("tvdb", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromTvdbFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("tvdb", "tmdb", "615");
    getEpisodeListingEpisode("tvdb", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromTvdbFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("tvdb", "imdb", "tt0149460");
    getEpisodeListingEpisode("tvdb", "imdb", "tt0149460");
  }

  @Test
  public void testGetEpisodeListingFromTmdbFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("tmdb", "tvdb", "73871");
    getEpisodeListingEpisode("tmdb", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromTmdbFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("tmdb", "tmdb", "615");
    getEpisodeListingEpisode("tmdb", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromTmdbFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("tmdb", "imdb", "tt0149460");
    getEpisodeListingEpisode("tmdb", "imdb", "tt0149460");
  }

  @Test
  public void testGetEpisodeListingFromImdbFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("imdb", "tvdb", "73871");
    getEpisodeListingEpisode("imdb", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromImdbFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("imdb", "tmdb", "615");
    getEpisodeListingEpisode("imdb", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromImdbFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("imdb", "imdb", "tt0149460");
    getEpisodeListingEpisode("imdb", "imdb", "tt0149460");
  }

  @Test
  public void testGetEpisodeListingFromTraktFuturamaWithTvdbId() throws Exception {
    getEpisodeListingTvShow("trakt", "tvdb", "73871");
    getEpisodeListingEpisode("trakt", "tvdb", "73871");
  }

  @Test
  public void testGetEpisodeListingFromTraktFuturamaWithTmdbId() throws Exception {
    getEpisodeListingTvShow("trakt", "tmdb", "615");
    getEpisodeListingEpisode("trakt", "tmdb", "615");
  }

  @Test
  public void testGetEpisodeListingFromTraktFuturamaWithImdbId() throws Exception {
    getEpisodeListingTvShow("trakt", "imdb", "tt0149460");
    getEpisodeListingEpisode("trakt", "imdb", "tt0149460");
  }

  private void getEpisodeListingTvShow(String scraperId, String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("episodes", scraperId);

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);

    List<MediaMetadata> episodes = mp.getEpisodeList(options);

    assertThat(episodes.size()).isGreaterThan(100);
  }

  private void getEpisodeListingEpisode(String scraperId, String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("episodes", scraperId);

    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setLanguage(MediaLanguages.en);

    List<MediaMetadata> episodes = mp.getEpisodeList(options);

    assertThat(episodes.size()).isGreaterThan(100);
  }

  @Test
  public void testScrapeEpisodeFromTvdbWithTvdbId() throws Exception {
    scrapeEpisode("tvdb", "tvdb", "73871");
  }

  @Test
  public void testScrapeEpisodeFromTvdbWithTmdbId() throws Exception {
    scrapeEpisode("tvdb", "tmdb", "615");
  }

  @Test
  public void testScrapeEpisodeFromTvdbWithImdbId() throws Exception {
    scrapeEpisode("tvdb", "imdb", "tt0149460");
  }

  @Test
  public void testScrapeEpisodeFromTmdbWithTvdbId() throws Exception {
    scrapeEpisode("tmdb", "tvdb", "73871");
  }

  @Test
  public void testScrapeEpisodeFromTmdbWithTmdbId() throws Exception {
    scrapeEpisode("tmdb", "tmdb", "615");
  }

  @Test
  public void testScrapeEpisodeFromTmdbWithImdbId() throws Exception {
    scrapeEpisode("tmdb", "imdb", "tt0149460");
  }

  @Test
  public void testScrapeEpisodeFromImdbWithTvdbId() throws Exception {
    scrapeEpisode("imdb", "tvdb", "73871");
  }

  @Test
  public void testScrapeEpisodeFromImdbWithTmdbId() throws Exception {
    scrapeEpisode("imdb", "tmdb", "615");
  }

  @Test
  public void testScrapeEpisodeFromImdbWithImdbId() throws Exception {
    scrapeEpisode("imdb", "imdb", "tt0149460");
  }

  @Test
  public void testScrapeEpisodeFromTraktWithTvdbId() throws Exception {
    scrapeEpisode("trakt", "tvdb", "73871");
  }

  @Test
  public void testScrapeEpisodeFromTraktWithTmdbId() throws Exception {
    scrapeEpisode("trakt", "tmdb", "615");
  }

  @Test
  public void testScrapeEpisodeFromTraktWithImdbId() throws Exception {
    scrapeEpisode("trakt", "imdb", "tt0149460");
  }

  private void scrapeEpisode(String scraperId, String providerId, String id) throws Exception {
    ITvShowMetadataProvider mp = new UniversalTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("episodes", scraperId);

    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    options.setId(providerId, id);
    options.setId(MediaMetadata.SEASON_NR, "1");
    options.setId(MediaMetadata.EPISODE_NR, "2");
    options.setLanguage(MediaLanguages.en);

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md).isNotNull();
    assertThat(md.getSeasonNumber()).isEqualTo(1);
    assertThat(md.getEpisodeNumber()).isEqualTo(2);
    assertThat(md.getTitle()).isNotEmpty();
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getReleaseDate()).isNotNull();
    assertThat(md.getRatings()).isNotEmpty();

    if (!"trakt".equals(scraperId)) {
      // trakt does not support episode guests
      assertThat(md.getCastMembers()).isNotEmpty();
    }
  }
}
