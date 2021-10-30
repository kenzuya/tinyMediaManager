package org.tinymediamanager.scraper.davestrailer;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ITDavesTrailerPageProviderTest extends BasicTest {

    @Before
    public void setUpBeforeTest() throws Exception {
        BasicTest.setup();
        setLicenseKey();
    }

    @Test
    public void testScrapeTrailerOneTrailerRow() {
        IMovieTrailerProvider mp;
        try {
            mp = new DavesTrailerPageProvider();
            TrailerSearchAndScrapeOptions options = new TrailerSearchAndScrapeOptions(MediaType.MOVIE);
            MediaMetadata md = new MediaMetadata("foo");
            md.setTitle("Dune");
            options.setId(MediaMetadata.IMDB,"tt1160419");
            options.setMetadata(md);

            List<MediaTrailer> trailers = mp.getTrailers(options);
            assertThat(trailers).isNotNull();

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testScrapeTrailerMoreTrailerRows() {
        IMovieTrailerProvider mp;
        try {
            mp = new DavesTrailerPageProvider();
            TrailerSearchAndScrapeOptions options = new TrailerSearchAndScrapeOptions(MediaType.MOVIE);
            MediaMetadata md = new MediaMetadata("foo");
            md.setTitle("Bears");
            options.setId(MediaMetadata.IMDB,"tt3890160");
            options.setMetadata(md);

            List<MediaTrailer> trailers = mp.getTrailers(options);
            assertThat(trailers).isNotNull();

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
