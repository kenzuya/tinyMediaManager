/*
 * Copyright 2012 - 2021 Manuel Laggner
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
import java.util.Collections;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.omdb.entities.MediaEntity;
import org.tinymediamanager.scraper.omdb.entities.MediaRating;
import org.tinymediamanager.scraper.omdb.entities.MediaSearch;
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
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions query) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", query);

    initAPI();

    if (query.getSearchResult() != null && query.getSearchResult().getMediaMetadata() != null
        && getId().equals(query.getSearchResult().getMediaMetadata().getProviderId())) {
      return query.getSearchResult().getMediaMetadata();
    }

    MediaMetadata metadata = new MediaMetadata(getId());

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

    MediaEntity result = null;
    try {
      result = controller.getScrapeDataById(imdbId, "movie", true);
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
      org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("imdb");
      rating.setRating(Float.parseFloat(result.imdbRating));
      rating.setVotes(MetadataUtil.parseInt(result.imdbVotes));
      rating.setMaxValue(10);
      metadata.addRating(rating);
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }
    try {
      org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("metacritic");
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
        org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometerallcritics");
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
        org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometerallaudience");
        rating.setRating(Float.parseFloat(result.tomatoUserMeter));
        rating.setMaxValue(100);
        rating.setVotes(MetadataUtil.parseInt(result.tomatoUserReviews));
        metadata.addRating(rating);
      }
    }
    catch (NumberFormatException e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    // use rotten tomates from the Ratings block
    for (MediaRating movieRating : ListUtils.nullSafe(result.ratings)) {
      switch (movieRating.source) {
        case "Rotten Tomatoes":
          try {
            org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometerallcritics");
            rating.setRating(Integer.parseInt(movieRating.value.replace("%", "")));
            rating.setMaxValue(100);
            rating.setVotes(1); // no votes here, but set to 1 to avoid filter out 0 ratings
            metadata.addRating(rating);
          }
          catch (Exception ignored) {
          }
          break;

        case "Metacritic":
          try {
            org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("metacritic");
            rating.setRating(Integer.parseInt(movieRating.value.replace("/100", "")));
            rating.setMaxValue(100);
            rating.setVotes(1); // no votes here, but set to 1 to avoid filter out 0 ratings
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
      org.tinymediamanager.core.entities.MediaRating omdbRating = metadata.getRatings()
          .stream()
          .filter(rating -> MediaMetadata.IMDB.equals(rating.getId()))
          .findFirst()
          .orElse(null);
      org.tinymediamanager.core.entities.MediaRating imdbRating = RatingUtil.getImdbRating((String) metadata.getId(MediaMetadata.IMDB));
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

    initAPI();

    SortedSet<MediaSearchResult> mediaResult = new TreeSet<>();

    // if the imdb id is given, directly fetch the result
    if (MetadataUtil.isValidImdbId(query.getImdbId())) {
      try {
        MediaMetadata md = getMetadata(query);

        // create the search result from the metadata
        MediaSearchResult result = new MediaSearchResult(getId(), MediaType.MOVIE);
        result.setMetadata(md);
        result.setTitle(md.getTitle());
        result.setIMDBId(query.getImdbId());
        result.setYear(md.getYear());

        for (MediaArtwork artwork : md.getMediaArt(MediaArtwork.MediaArtworkType.POSTER)) {
          result.setPosterUrl(artwork.getPreviewUrl());
        }

        result.setScore(1);
        mediaResult.add(result);

        return mediaResult;
      }
      catch (Exception e) {
        LOGGER.debug("could not fetch data with imdb id - '{}'", e.getMessage());
      }
    }

    MediaSearch resultList;
    try {
      resultList = controller.getMovieSearchInfo(query.getSearchQuery(), "movie", null);

      if (resultList == null || ListUtils.isEmpty(resultList.search)) {
        // nothing found - try via direct lookup
        MediaEntity result = controller.getScrapeDataByTitle(query.getSearchQuery(), "movie", false);
        if ("true".equalsIgnoreCase(result.response)) {
          resultList = new MediaSearch();
          resultList.search = Collections.singletonList(result);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (resultList == null) {
      LOGGER.info("no result from omdbapi");
      return mediaResult;
    }

    for (MediaEntity entity : ListUtils.nullSafe(resultList.search)) {
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
