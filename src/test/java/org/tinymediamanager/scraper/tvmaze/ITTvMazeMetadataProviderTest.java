package org.tinymediamanager.scraper.tvmaze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

public class ITTvMazeMetadataProviderTest extends BasicTest {

  @Before
  public void setUpBeforeTest() throws Exception {
    BasicTest.setup();
    setLicenseKey();
  }

  /**
   * Testing ProviderInfo
   */
  @Test
  public void testProviderInfo() {
    try {
      TvMazeTvShowMetadataProvider mp = new TvMazeTvShowMetadataProvider();
      MediaProviderInfo providerInfo = mp.getProviderInfo();

      assertThat(providerInfo.getDescription()).isNotNull();
      assertThat(providerInfo.getId()).isNotNull();
      assertThat(providerInfo.getName()).isNotNull();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testSearch() throws ScrapeException {
    TvMazeTvShowMetadataProvider mp = new TvMazeTvShowMetadataProvider();
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();

    options.setSearchQuery("Batman");

    SortedSet<MediaSearchResult> result = new TreeSet<>(mp.search(options));

    assertThat(result).isNotNull();
    assertThat(result.size()).isGreaterThan(5);

  }

  @Test
  public void testScrapeTvShow() throws ScrapeException {
    TvMazeTvShowMetadataProvider mp = new TvMazeTvShowMetadataProvider();
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();

    options.setId("tvmaze", "1");
    options.setLanguage(MediaLanguages.en);

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md.getTitle()).isEqualTo("Under the Dome");
    assertThat(md.getOriginalLanguage()).isEqualTo("English");
    assertThat(md.getGenres()).isNotNull();
    assertThat(md.getRuntime()).isEqualTo(60);
    assertThat(md.getYear()).isEqualTo(2013);
    assertThat(md.getRatings().get(0).getRating()).isEqualTo(6.5f);
    assertThat(md.getId("tvrage")).isEqualTo(25988);
    assertThat(md.getId("tvdb")).isEqualTo(264492);
    assertThat(md.getId("imdb")).isEqualTo("tt1553656");
  }

  @Test
  public void testScrapeEpisode() throws ScrapeException {
    TvMazeTvShowMetadataProvider mp = new TvMazeTvShowMetadataProvider();
    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();

    Map<String, Object> tvShowIds = new HashMap<>();
    tvShowIds.put("tvmaze", 1);
    options.setTvShowIds(tvShowIds);
    options.setId(MediaMetadata.EPISODE_NR, 1);
    options.setId(MediaMetadata.SEASON_NR, 1);
    options.setLanguage(MediaLanguages.en);

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md).isNotNull();
    assertThat(md.getTitle()).isEqualTo("Pilot");
    assertThat(md.getPlot()).isNotEmpty();
    assertThat(md.getReleaseDate()).isEqualTo("2013-06-24");
    assertThat(md.getRuntime()).isGreaterThan(0);
  }

  @Test
  public void testEpisodeList() throws ScrapeException {
    TvMazeTvShowMetadataProvider mp = new TvMazeTvShowMetadataProvider();
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();

    options.setId("tvmaze", "1");
    options.setLanguage(MediaLanguages.en);

    List<MediaMetadata> episodes = mp.getEpisodeList(options);
    assertThat(episodes).isNotEmpty();
  }
}
