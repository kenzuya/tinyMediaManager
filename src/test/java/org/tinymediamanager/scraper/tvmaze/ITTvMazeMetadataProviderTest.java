package org.tinymediamanager.scraper.tvmaze;

import org.junit.Test;
import org.tinymediamanager.BasicTest;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ITTvMazeMetadataProviderTest extends BasicTest {

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
  public void testScrape() throws ScrapeException, MissingIdException, NothingFoundException {
    TvMazeTvShowMetadataProvider mp = new TvMazeTvShowMetadataProvider();
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();

    options.setId("tvmaze","1");
    options.setLanguage(MediaLanguages.en);
    MediaMetadata md;

    md = mp.getMetadata(options);

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

}
