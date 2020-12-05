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
package org.tinymediamanager.scraper.omdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.omdb.entities.MovieEntity;
import org.tinymediamanager.scraper.omdb.entities.MovieRating;
import org.tinymediamanager.scraper.omdb.entities.MovieSearch;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.RatingUtil;

/**
 * the class {@link OmdbMovieMetadataProvider} is used to provide meta data for movies
 *
 * @author Manuel Laggner
 */
public class OmdbMovieMetadataProvider extends OmdbMetadataProvider implements IMovieMetadataProvider, IMovieImdbMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OmdbMovieMetadataProvider.class);

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  public boolean isActive() {
    return StringUtils.isNotBlank(getApiKey());
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions query) throws ScrapeException, MissingIdException, NothingFoundException {
    LOGGER.debug("getMetadata(): {}", query);

    MediaMetadata metadata = new MediaMetadata(getId());

    String apiKey = getApiKey();
    if (StringUtils.isBlank(apiKey)) {
      LOGGER.warn("no API key found");
      return metadata;
    }

    // id from the options
    String imdbId = query.getImdbId();

    // id from omdb proxy?
    if (!MetadataUtil.isValidImdbId(imdbId)) {
      imdbId = query.getIdAsString(getProviderInfo().getId());
    }

    // still no imdb id but tmdb id? get it from tmdb
    if (!MetadataUtil.isValidImdbId(imdbId) && query.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getMovieImdbIdViaTmdbId(query.getTmdbId());
    }

    // imdbid check
    if (!MetadataUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("no imdb id found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    DateFormat format = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
    LOGGER.info("========= BEGIN OMDB Scraping");

    MovieEntity result = null;
    try {
      result = controller.getScrapeDataById(apiKey, imdbId, "movie", true);
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (result == null) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    // set ids
    if (MetadataUtil.isValidImdbId(result.imdbID)) {
      metadata.setId(MediaMetadata.IMDB, result.imdbID);
    }

    metadata.setTitle(result.title);
    try {
      metadata.setYear(Integer.parseInt(result.year));
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse year: {}", e.getMessage());
    }

    metadata.addCertification(MediaCertification.findCertification(result.rated));
    try {
      metadata.setReleaseDate(format.parse(result.released));
    }
    catch (Exception ignored) {
    }

    Pattern p = Pattern.compile("\\d+");
    Matcher m = p.matcher(result.runtime);
    while (m.find()) {
      try {
        metadata.setRuntime(Integer.parseInt(m.group()));
      }
      catch (NumberFormatException ignored) {
      }
    }

    String[] genres = result.genre.split(",");
    for (String genre : genres) {
      genre = genre.trim();
      MediaGenres mediaGenres = MediaGenres.getGenre(genre);
      metadata.addGenre(mediaGenres);
    }

    metadata.setPlot(result.plot);

    String[] directors = result.director.split(",");
    for (String d : directors) {
      Person director = new Person(DIRECTOR);
      director.setName(d.trim());
      metadata.addCastMember(director);
    }

    String[] writers = result.writer.split(",");
    for (String w : writers) {
      Person writer = new Person(WRITER);
      writer.setName(w.trim());
      metadata.addCastMember(writer);
    }

    String[] actors = result.actors.split(",");
    for (String a : actors) {
      Person actor = new Person(ACTOR);
      actor.setName(a.trim());
      metadata.addCastMember(actor);
    }

    metadata.setSpokenLanguages(getResult(result.language, ","));
    metadata.setCountries(getResult(result.country, ","));

    try {
      MediaRating rating = new MediaRating("imdb");
      rating.setRating(Float.parseFloat(result.imdbRating));
      rating.setVotes(MetadataUtil.parseInt(result.imdbVotes));
      rating.setMaxValue(10);
      metadata.addRating(rating);
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }
    try {
      MediaRating rating = new MediaRating("metascore");
      rating.setRating(Float.parseFloat(result.metascore));
      rating.setMaxValue(100);
      metadata.addRating(rating);
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    // Tomatoratings

    try {
      if (!result.tomatoMeter.contains("N/A")) {
        MediaRating rating = new MediaRating("tomatometerallcritics");
        rating.setRating(Float.parseFloat(result.tomatoMeter));
        rating.setMaxValue(100);
        rating.setVotes(MetadataUtil.parseInt(result.tomatoReviews));
        metadata.addRating(rating);
      }
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    try {
      if (!result.tomatoUserMeter.contains("N/A")) {
        MediaRating rating = new MediaRating("tomatometerallaudience");
        rating.setRating(Float.parseFloat(result.tomatoUserMeter));
        rating.setMaxValue(100);
        rating.setVotes(MetadataUtil.parseInt(result.tomatoUserReviews));
        metadata.addRating(rating);
      }
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    try {
      if (!result.tomatoRating.contains("N/A")) {
        MediaRating rating = new MediaRating("tomatometeravgcritics");
        rating.setRating(Float.parseFloat(result.tomatoRating));
        rating.setMaxValue(100);
        rating.setVotes(MetadataUtil.parseInt(result.tomatoReviews));
        metadata.addRating(rating);
      }
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    try {
      if (!result.tomatoUserRating.contains("N/A")) {
        MediaRating rating = new MediaRating("tomatometeravgaudience");
        rating.setRating(Float.parseFloat(result.tomatoUserRating));
        rating.setMaxValue(100);
        rating.setVotes(MetadataUtil.parseInt(result.tomatoUserReviews));
        metadata.addRating(rating);
      }
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }
    // use rotten tomates from the Ratings block
    for (MovieRating movieRating : ListUtils.nullSafe(result.ratings)) {
      switch (movieRating.source) {
        case "Rotten Tomatoes":
          try {
            MediaRating rating = new MediaRating("rottenTomatoes");
            rating.setRating(Integer.parseInt(movieRating.value.replace("%", "")));
            rating.setMaxValue(100);
            metadata.addRating(rating);
          }
          catch (Exception ignored) {
          }
          break;
      }
    }

    // get the imdb rating from the imdb dataset too (and probably replace an
    // outdated rating from omdb)
    if (metadata.getId(MediaMetadata.IMDB) instanceof String) {
      MediaRating omdbRating = metadata.getRatings().stream().filter(rating -> MediaMetadata.IMDB.equals(rating.getId())).findFirst().orElse(null);
      MediaRating imdbRating = RatingUtil.getImdbRating((String) metadata.getId(MediaMetadata.IMDB));
      if (imdbRating != null && (omdbRating == null || imdbRating.getVotes() > omdbRating.getVotes())) {
        metadata.getRatings().remove(omdbRating);
        metadata.addRating(imdbRating);
      }
    }

    if (StringUtils.isNotBlank(result.poster)) {
      MediaArtwork artwork = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
      artwork.setDefaultUrl(result.poster);
      metadata.addMediaArt(artwork);
    }

    return metadata;

  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions query) throws ScrapeException {
    LOGGER.debug("search(): {}", query);
    SortedSet<MediaSearchResult> mediaResult = new TreeSet<>();

    String apiKey = getApiKey();

    if (StringUtils.isBlank(apiKey)) {
      LOGGER.warn("no API key found");
      return mediaResult;
    }

    MovieSearch resultList;
    try {
      resultList = controller.getMovieSearchInfo(apiKey, query.getSearchQuery(), "movie", null);
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (resultList == null) {
      LOGGER.info("no result from omdbapi");
      return mediaResult;
    }

    for (MovieEntity entity : ListUtils.nullSafe(resultList.search)) {
      MediaSearchResult result = new MediaSearchResult(getId(), MediaType.MOVIE);

      result.setTitle(entity.title);
      if (MetadataUtil.isValidImdbId(entity.imdbID)) {
        result.setIMDBId(entity.imdbID);
      }
      try {
        result.setYear(Integer.parseInt(entity.year));
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse year: {}", e.getMessage());
      }
      result.setPosterUrl(entity.poster);

      // calcuate the result score
      result.calculateScore(query);
      mediaResult.add(result);
    }

    return mediaResult;
  }
}
