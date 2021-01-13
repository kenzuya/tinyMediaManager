package org.tinymediamanager.scraper.theshowdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.BasicTest;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

public class TheShowDBMetadataProviderTest extends BasicTest {

  TheShowDBTvShowMetadataProvider mp;


  @Before
  public void setUp() {

    mp = new TheShowDBTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("apiKey", 1);

  }

  @Test
  public void testSearch() throws ScrapeException {

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.en);
    options.setSearchQuery("Lost");


    List<MediaSearchResult> result = new ArrayList<>(mp.search(options));
    assertThat(result).isNotNull();
    assertThat(result).hasSizeGreaterThan(5);

  }

  @Test
  public void getTvShow() throws MissingIdException, ScrapeException, NothingFoundException {

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.en);
    options.setId("theshowdb","602648");
    MediaMetadata result;

    result = mp.getMetadata(options);
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Lost");
    assertThat(result.getRuntime()).isEqualTo(45);
    assertThat(result.getCertifications()).isNotNull();
    assertThat(result.getGenres()).hasSize(4);
    assertThat(result.getPlot()).isNotNull();
    assertThat(result.getId(MediaMetadata.IMDB)).isEqualTo("tt0411008");
    assertThat(result.getId("zap2it")).isEqualTo("SH672362");

  }
}
