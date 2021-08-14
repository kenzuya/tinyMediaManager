package org.tinymediamanager.scraper.hdtrailersnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;

public class ITHDTrailersNetTrailerProviderTest extends BasicTest {

  @Before
  public void setUpBeforeTest() throws Exception {
    BasicTest.setup();
    setLicenseKey();
  }


  @Test
  public void testScrapeTrailer() {
    IMovieTrailerProvider mp;
    try {
      mp = new HdTrailersNetMovieTrailerProvider();

      TrailerSearchAndScrapeOptions options = new TrailerSearchAndScrapeOptions(MediaType.MOVIE);

      MediaMetadata md = new MediaMetadata("foo");
      md.setOriginalTitle("Iron Man 3");
      options.setMetadata(md);

      List<MediaTrailer> trailers = mp.getTrailers(options);
      assertThat(trailers).isNotNull().isNotEmpty();

      MediaTrailer trailer = trailers.get(0);
      assertThat(trailer.getName()).isNotEmpty();
      assertThat(trailer.getUrl()).isNotEmpty();
      assertThat(trailer.getProvider()).isNotEmpty();
      assertThat(trailer.getQuality()).isNotEmpty();
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
