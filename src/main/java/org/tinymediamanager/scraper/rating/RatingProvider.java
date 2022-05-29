/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.scraper.rating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * the class {@link RatingProvider} is a utility to have quick access to several various ratings
 * 
 * @author Manuel Laggner
 */
public class RatingProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(RatingProvider.class);

  public enum RatingSource {
    IMDB("IMDb"),
    TMDB("TMDB"),
    METACRITIC("Metacritic"),
    ROTTEN_TOMATOES_TOMATOMETER("Rotten Tomatoes - Tomatometer"),
    ROTTEN_TOMATOES_AVG_RATING("Rotten Tomatoes - Audience Score"),
    TRAKT_TV("Trakt.tv");

    private final String title;

    RatingSource(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }

    /**
     * get all relevant {@link RatingSource}s for movies
     * 
     * @return a {@link List} of all relevant {@link RatingSource}s
     */
    public static List<RatingSource> getRatingSourcesForMovies() {
      return Arrays.asList(values());
    }

    /**
     * get all relevant {@link RatingSource}s for TV shows
     * 
     * @return a {@link List} of all relevant {@link RatingSource}s
     */
    public static List<RatingSource> getRatingSourcesForTvShows() {
      List<RatingSource> ratingSources = new ArrayList<>();

      for (RatingSource ratingSource : values()) {
        if (ratingSource == METACRITIC || ratingSource == ROTTEN_TOMATOES_AVG_RATING || ratingSource == ROTTEN_TOMATOES_TOMATOMETER) {
          continue;
        }
        ratingSources.add(ratingSource);
      }

      return ratingSources;
    }
  }

  private RatingProvider() {
    throw new IllegalAccessError();
  }

  /**
   * get the IMDb rating
   * 
   * @param imdbId
   *          the IMDb ID
   * @return the {@link MediaRating} from IMDb
   */
  public static MediaRating getImdbRating(String imdbId) {
    List<MediaRating> ratings = getRatings(Collections.singletonMap(MediaMetadata.IMDB, imdbId), Collections.singletonList(RatingSource.IMDB), null);
    if (!ratings.isEmpty()) {
      return ratings.get(0);
    }
    return null;
  }

  /**
   * get a {@link List} of all supported rating sources
   * 
   * @param ids
   *          the IDs to get the {@link MediaRating}s for
   * @param mediaType
   *          the {@link MediaType}
   * @return a {@link List} of all found {@link MediaRating}s
   */
  public static List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) {
    return getRatings(ids, Arrays.asList(RatingSource.values()), mediaType);
  }

  /**
   * get a {@link List} of all supported ratings from various sources for
   *
   * @param ids
   *          the IDs to get the {@link MediaRating}s for
   * @param sources
   *          the {@link RatingSource}s to get the ratings for
   * @param mediaType
   *          the {@link MediaType}
   * @return a {@link List} of all found {@link MediaRating}s
   */
  public static List<MediaRating> getRatings(Map<String, Object> ids, List<RatingSource> sources, MediaType mediaType) {
    List<MediaRating> ratings = new ArrayList<>();
    List<RatingSource> missingRatings = new ArrayList<>(sources);

    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      // no valid imdb id here - try to fetch all relevant ids via trakt.tv
      MediaIdUtil.injectMissingIds(ids, mediaType);
    }

    // IMDB comes directly from the IMDB data dump
    if (missingRatings.contains(RatingSource.IMDB) && MediaIdUtil.isValidImdbId(imdbId)) {
      MediaRating rating = new ImdbRating().getImdbRating(imdbId);
      if (rating != null) {
        ratings.add(rating);
        missingRatings.remove(RatingSource.IMDB);
      }
    }

    // Metacritic can be fetched from the IMDB scraper
    if (missingRatings.contains(RatingSource.METACRITIC)) {
      callScraper(MediaMetadata.IMDB, mediaType, missingRatings, ids, ratings);
    }

    // OMDB offers Rotten Tomatoes
    if (ListUtils.containsAny(missingRatings, RatingSource.ROTTEN_TOMATOES_AVG_RATING, RatingSource.ROTTEN_TOMATOES_AVG_RATING)
        && MediaIdUtil.isValidImdbId(imdbId)) {
      callScraper("omdbapi", mediaType, missingRatings, ids, ratings);
    }

    // Wikidata offers Metacritic, Rotten Tomatoes and IMDB
    if (ListUtils.containsAny(missingRatings, RatingSource.METACRITIC, RatingSource.ROTTEN_TOMATOES_AVG_RATING,
        RatingSource.ROTTEN_TOMATOES_AVG_RATING) && MediaIdUtil.isValidImdbId(imdbId)) {
      List<MediaRating> ratingsFromWikidata = new WikidataRating().getRatings(imdbId);

      for (MediaRating rating : ratingsFromWikidata) {
        RatingSource source = parseRatingSource(rating.getId());
        if (missingRatings.contains(source) && !ratings.contains(rating)) {
          ratings.add(rating);
          missingRatings.remove(source);
        }
      }
    }

    // TMDB rating comes directly from TMDB
    if (missingRatings.contains(RatingSource.TMDB)) {
      callScraper(MediaMetadata.TMDB, mediaType, missingRatings, ids, ratings);
    }

    // Trakt.tv rating comes directly form Trakt.tv
    if (missingRatings.contains(RatingSource.TRAKT_TV)) {
      callScraper(MediaMetadata.TRAKT_TV, mediaType, missingRatings, ids, ratings);
    }

    return ratings;
  }

  private static void callScraper(String scraperId, MediaType mediaType, List<RatingSource> sources, Map<String, Object> ids,
      List<MediaRating> ratings) {
    MediaScraper scraper = MediaScraper.getMediaScraperById(scraperId, MediaType.getScraperTypeForMediaType(mediaType));
    if (scraper != null && scraper.isEnabled() && scraper.getMediaProvider() instanceof IRatingProvider) {
      try {
        List<MediaRating> ratingsFromScraper = ((IRatingProvider) scraper.getMediaProvider()).getRatings(ids, mediaType);

        for (MediaRating rating : ratingsFromScraper) {
          RatingSource source = parseRatingSource(rating.getId());
          if (sources.contains(source) && !ratings.contains(rating)) {
            ratings.add(rating);
            sources.remove(source);
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not get {} ratings - '{}'", scraperId, e.getMessage());
      }
    }
  }

  /**
   * shutdown of the internal caches
   */
  public static synchronized void shutdown() {
    ImdbRating.shutdown();
  }

  private static RatingSource parseRatingSource(String id) {
    switch (id) {
      case "metacritic":
        return RatingSource.METACRITIC;

      case MediaMetadata.IMDB:
        return RatingSource.IMDB;

      case MediaMetadata.TMDB:
        return RatingSource.TMDB;

      case "tomatometerallcritics":
        return RatingSource.ROTTEN_TOMATOES_TOMATOMETER;

      case "tomatometeravgcritics":
        return RatingSource.ROTTEN_TOMATOES_AVG_RATING;

      case "trakt":
        return RatingSource.TRAKT_TV;

      default:
        return null;
    }
  }
}
