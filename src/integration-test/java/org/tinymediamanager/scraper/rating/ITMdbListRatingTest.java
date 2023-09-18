package org.tinymediamanager.scraper.rating;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;

public class ITMdbListRatingTest {

  @Test
  public void testGetRatings() throws Exception {

    Map<String, Object> ids = new HashMap<>();

    ids.put(MediaMetadata.IMDB, "tt6718170");

    MdbListRating ratings = new MdbListRating();
    List<MediaRating> mediaRatings = ratings.getRatings(ids); // Super Mario Bros Movie

    assertThat(mediaRatings).isNotEmpty();
  }
}
