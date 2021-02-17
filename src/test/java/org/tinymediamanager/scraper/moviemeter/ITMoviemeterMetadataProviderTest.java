package org.tinymediamanager.scraper.moviemeter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.BasicTest;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;

public class ITMoviemeterMetadataProviderTest extends BasicTest {

  @Before
  public void setUpBeforeTest() throws Exception {
    setLicenseKey();
  }

  @Test
  public void testSearch() {
    try {
      IMovieMetadataProvider rt = new MovieMeterMovieMetadataProvider();
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setSearchQuery("Avatar");

      List<MediaSearchResult> results = new ArrayList<>(rt.search(options));
      assertThat(results.size()).isGreaterThanOrEqualTo(3);
      for (MediaSearchResult result : results) {
        assertThat(result.getTitle()).isNotNull().isNotEmpty();
        assertThat(result.getId()).isNotNull();
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testScrape() throws Exception {
    try {
      IMovieMetadataProvider rt = new MovieMeterMovieMetadataProvider();

      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setLanguage(MediaLanguages.nl);
      options.setCertificationCountry(CountryCode.US);
      options.setReleaseDateCountry("US");
      options.setId(rt.getProviderInfo().getId(), "17552");

      MediaMetadata md = rt.getMetadata(options);

      assertThat(md).isNotNull();

      assertThat(md.getTitle()).isEqualTo("Avatar");
      assertThat(md.getYear()).isEqualTo(2009);

      assertThat(md.getRatings().size()).isGreaterThanOrEqualTo(1);
      MediaRating mediaRating = md.getRatings().get(0);
      assertThat(mediaRating.getId()).isNotEmpty();
      assertThat(mediaRating.getRating()).isGreaterThan(0);
      assertThat(mediaRating.getVotes()).isGreaterThan(0);
      assertThat(mediaRating.getMaxValue()).isEqualTo(5);

      assertThat(md.getPlot()).startsWith("Jake Sully (Sam Worthington) is een verlamde oorlogsveteraan in de toekomst, die met enkele");
      assertThat(md.getProductionCompanies()).isEmpty();
      assertThat(md.getId(MediaMetadata.IMDB)).isEqualTo("tt0499549");
      assertThat(md.getRuntime()).isEqualTo(162);
      assertThat(md.getGenres().size()).isGreaterThanOrEqualTo(1);
      assertThat(md.getCastMembers(ACTOR).size()).isGreaterThanOrEqualTo(1);
      assertThat(md.getCastMembers(DIRECTOR).size()).isGreaterThanOrEqualTo(1);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

}
