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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
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
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.omdb.entities.EpisodeEntity;
import org.tinymediamanager.scraper.omdb.entities.MediaEntity;
import org.tinymediamanager.scraper.omdb.entities.MediaRating;
import org.tinymediamanager.scraper.omdb.entities.MediaSearch;
import org.tinymediamanager.scraper.omdb.entities.SeasonEntity;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.RatingUtil;

public class OmdbTvShowMetadataProvider extends OmdbMetadataProvider implements ITvShowMetadataProvider {

  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(OmdbTvShowMetadataProvider.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(60, 10);

  @Override
  protected String getSubId() {
    return "tvshow";
  }

  @Override
  public MediaMetadata getMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() - TvShow: {}", options.getSearchQuery());

    initAPI();

    MediaMetadata metadata = new MediaMetadata(getId());
    DateFormat format = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
    MediaEntity result;

    String imdbId = getImdbId(options);

    if (StringUtils.isBlank(imdbId)) {
      throw new MissingIdException(imdbId);
    }

    try {
      result = controller.getScrapeDataById(imdbId, "series", true);
    }
    catch (Exception e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (result == null) {
      LOGGER.warn("no result found");
      throw new NothingFoundException();
    }

    // set ID
    if (MetadataUtil.isValidImdbId(result.imdbID)) {
      metadata.setId(MediaMetadata.IMDB, result.imdbID);
    }

    metadata.setTitle(result.title);
    metadata.addCertification(MediaCertification.findCertification(result.rated));
    metadata.setPlot(result.plot);
    metadata.setSpokenLanguages(getResult(result.language, ","));
    metadata.setCountries(getResult(result.country, ","));

    // Year
    if (result.year != null) {
      try {
        // Get the Year when the season started!
        metadata.setYear(Integer.parseInt(result.year.substring(0, 4)));
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse year: {}", e.getMessage());
      }
    }

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
      if ("Rotten Tomatoes".equals(movieRating.source)) {
        try {
          org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometerallcritics");
          rating.setRating(Integer.parseInt(movieRating.value.replace("%", "")));
          rating.setMaxValue(100);
          metadata.addRating(rating);
        }
        catch (Exception ignored) {
        }
      }
      else if ("Metacritic".equals(movieRating.source)) {
        try {
          org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("metacritic");
          rating.setRating(Integer.parseInt(movieRating.value.replace("/100", "")));
          rating.setMaxValue(100);
          metadata.addRating(rating);
        }
        catch (Exception ignored) {
        }
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

  private String getImdbId(TvShowSearchAndScrapeOptions options) throws MissingIdException {
    // id from the options
    String imdbId = options.getImdbId();

    // id from omdb proxy?
    if (!MetadataUtil.isValidImdbId(imdbId)) {
      imdbId = options.getIdAsString(getProviderInfo().getId());
    }

    // still no imdb id but tmdb id? get it from tmdb
    if (!MetadataUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getMovieImdbIdViaTmdbId(options.getTmdbId());
    }

    // imdbid check
    if (!MetadataUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("no imdb id found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }
    return imdbId;
  }

  @Override
  public MediaMetadata getMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException, MissingIdException, NothingFoundException {
    LOGGER.debug("getMetadata() - episode: {}", options.getSearchQuery());

    initAPI();

    DateFormat format = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
    String imdbID = "";
    MediaMetadata md = new MediaMetadata(getId());

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // first get the base episode metadata which can be gathered via getEpisodeList()
    List<MediaMetadata> episodeList = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

    if (episodeList.isEmpty()) {
      LOGGER.error("EpisodeList is empty, cannot fetch Episode Information");
      throw new NothingFoundException();
    }

    for (MediaMetadata metadata : episodeList) {
      if (seasonNr == metadata.getSeasonNumber() && episodeNr == metadata.getEpisodeNumber()) {
        imdbID = metadata.getId("imdbId").toString();
        break;
      }
    }

    if (StringUtils.isBlank(imdbID)) {
      LOGGER.warn("no imdb id found for season {} episode {}", seasonNr, episodeNr);
      return md;
    }

    MediaEntity mediaEntity = null;

    try {
      mediaEntity = controller.getScrapeDataById(imdbID, "episode", true);
    }
    catch (IOException e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (mediaEntity != null) {

      // Add the Information
      md.setTitle(mediaEntity.title);
      try {
        md.setYear(Integer.parseInt(mediaEntity.year));
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse year: {}", e.getMessage());
      }
      md.addCertification(MediaCertification.findCertification(mediaEntity.rated));

      // ReleaseDate
      try {
        md.setReleaseDate(format.parse(mediaEntity.released));
      }
      catch (Exception ignored) {
      }

      md.setSeasonNumber(mediaEntity.season);
      md.setEpisodeNumber(mediaEntity.episode);

      // Runtime
      Pattern p = Pattern.compile("\\d+");
      Matcher m = p.matcher(mediaEntity.runtime);
      while (m.find()) {
        try {
          md.setRuntime(Integer.parseInt(m.group()));
        }
        catch (NumberFormatException ignored) {
        }
      }

      String[] genres = mediaEntity.genre.split(",");
      for (String genre : genres) {
        genre = genre.trim();
        MediaGenres mediaGenres = MediaGenres.getGenre(genre);
        md.addGenre(mediaGenres);
      }

      String[] directors = mediaEntity.director.split(",");
      for (String d : directors) {
        Person director = new Person(DIRECTOR);
        director.setName(d.trim());
        md.addCastMember(director);
      }

      String[] writers = mediaEntity.writer.split(",");
      for (String w : writers) {
        Person writer = new Person(WRITER);
        writer.setName(w.trim());
        md.addCastMember(writer);
      }

      String[] actors = mediaEntity.actors.split(",");
      for (String a : actors) {
        Person actor = new Person(ACTOR);
        actor.setName(a.trim());
        md.addCastMember(actor);
      }

      md.setPlot(mediaEntity.plot);
      md.setSpokenLanguages(getResult(mediaEntity.language, ","));
      md.setCountries(getResult(mediaEntity.country, ","));

      try {
        org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("imdb");
        rating.setRating(Float.parseFloat(mediaEntity.imdbRating));
        rating.setVotes(MetadataUtil.parseInt(mediaEntity.imdbVotes));
        rating.setMaxValue(10);
        md.addRating(rating);
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
      try {
        org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("metascore");
        rating.setRating(Float.parseFloat(mediaEntity.metascore));
        rating.setMaxValue(100);
        md.addRating(rating);
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }

      // Tomatoratings

      try {
        if (!mediaEntity.tomatoMeter.contains("N/A")) {
          org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometerallcritics");
          rating.setRating(Float.parseFloat(mediaEntity.tomatoMeter));
          rating.setMaxValue(100);
          rating.setVotes(MetadataUtil.parseInt(mediaEntity.tomatoReviews));
          md.addRating(rating);
        }
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }

      try {
        if (!mediaEntity.tomatoUserMeter.contains("N/A")) {
          org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometerallaudience");
          rating.setRating(Float.parseFloat(mediaEntity.tomatoUserMeter));
          rating.setMaxValue(100);
          rating.setVotes(MetadataUtil.parseInt(mediaEntity.tomatoUserReviews));
          md.addRating(rating);
        }
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }

      try {
        if (!mediaEntity.tomatoRating.contains("N/A")) {
          org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometeravgcritics");
          rating.setRating(Float.parseFloat(mediaEntity.tomatoRating));
          rating.setMaxValue(100);
          rating.setVotes(MetadataUtil.parseInt(mediaEntity.tomatoReviews));
          md.addRating(rating);
        }
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }

      try {
        if (!mediaEntity.tomatoUserRating.contains("N/A")) {
          org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("tomatometeravgaudience");
          rating.setRating(Float.parseFloat(mediaEntity.tomatoUserRating));
          rating.setMaxValue(100);
          rating.setVotes(MetadataUtil.parseInt(mediaEntity.tomatoUserReviews));
          md.addRating(rating);
        }
      }
      catch (NumberFormatException e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
      // use rotten tomates from the Ratings block
      for (MediaRating movieRating : ListUtils.nullSafe(mediaEntity.ratings)) {
        if ("Rotten Tomatoes".equals(movieRating.source)) {
          try {
            org.tinymediamanager.core.entities.MediaRating rating = new org.tinymediamanager.core.entities.MediaRating("rottenTomatoes");
            rating.setRating(Integer.parseInt(movieRating.value.replace("%", "")));
            rating.setMaxValue(100);
            md.addRating(rating);
          }
          catch (Exception ignored) {
          }
        }
      }

      // get the imdb rating from the imdb dataset too (and probably replace an
      // outdated rating from omdb)
      if (md.getId(MediaMetadata.IMDB) instanceof String) {
        org.tinymediamanager.core.entities.MediaRating omdbRating = md.getRatings()
            .stream()
            .filter(rating -> MediaMetadata.IMDB.equals(rating.getId()))
            .findFirst()
            .orElse(null);
        org.tinymediamanager.core.entities.MediaRating imdbRating = RatingUtil.getImdbRating((String) md.getId(MediaMetadata.IMDB));
        if (imdbRating != null && (omdbRating == null || imdbRating.getVotes() > omdbRating.getVotes())) {
          md.getRatings().remove(omdbRating);
          md.addRating(imdbRating);
        }
      }

      if (StringUtils.isNotBlank(mediaEntity.poster)) {
        MediaArtwork artwork = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
        artwork.setDefaultUrl(mediaEntity.poster);
        md.addMediaArt(artwork);
      }
    }
    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options.getSearchQuery());

    initAPI();

    SortedSet<MediaSearchResult> mediaResult = new TreeSet<>();

    MediaSearch resultList = null;
    try {
      resultList = controller.getMovieSearchInfo(options.getSearchQuery(), "series", null);
    }
    catch (InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (IOException e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (resultList == null) {
      LOGGER.info("no result from omdbapi");
      return mediaResult;
    }

    for (MediaEntity entity : ListUtils.nullSafe(resultList.search)) {
      MediaSearchResult result = new MediaSearchResult(getId(), MediaType.TV_SHOW);

      result.setTitle(entity.title);
      if (MetadataUtil.isValidImdbId(entity.imdbID)) {
        result.setIMDBId(entity.imdbID);
      }
      // Year
      if (entity.year != null) {
        try {
          // Get the Year when the season started!
          result.setYear(Integer.parseInt(entity.year.substring(0, 4)));
        }
        catch (NumberFormatException e) {
          LOGGER.trace("could not parse year: {}", e.getMessage());
        }
      }
      result.setPosterUrl(entity.poster);

      // calcuate the result score
      result.calculateScore(options);

      mediaResult.add(result);

    }

    return mediaResult;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    LOGGER.debug("getEpisodeList(): {}", options.getSearchQuery());

    initAPI();

    String imdbId = getImdbId(options);

    if (StringUtils.isBlank(imdbId)) {
      throw new MissingIdException("imdbId");
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> mediaMetadataList = EPISODE_LIST_CACHE_MAP.get(imdbId + "_" + options.getLanguage().getLanguage());

    if (ListUtils.isNotEmpty(mediaMetadataList)) {
      // cache hit!
      return mediaMetadataList;
    }

    mediaMetadataList = new ArrayList<>();

    if (StringUtils.isBlank(getApiKey())) {
      LOGGER.warn("no API Key found");
      throw new ScrapeException(new HttpException(401, "Unauthorized"));
    }

    MediaEntity tvShowResult = null;
    try {
      tvShowResult = controller.getScrapeDataById(imdbId, "series", true);
    }
    catch (InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (IOException e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (tvShowResult == null) {
      LOGGER.warn("no result found");
      return mediaMetadataList;
    }

    // First get the total amount of seasons
    if (tvShowResult.totalSeasons.equals("N/A")) {
      LOGGER.error("cannot parse total amount of seasons");
      return mediaMetadataList;
    }
    int seasons = Integer.parseInt(tvShowResult.totalSeasons);

    // Then get Information for every Season and save it in metadata result
    for (int i = 1; i < seasons + 1; i++) {

      SeasonEntity seasonEntity;
      try {
        seasonEntity = controller.getSeasonById(imdbId, "series", i);
      }
      catch (IOException e) {
        LOGGER.error("error scraping season {} information: {}", i, e.getMessage());
        continue;
      }

      if (seasonEntity.episodes == null) {
        LOGGER.error("No Episode Information for Season: {}", i);
        continue;
      }

      for (EpisodeEntity ep : seasonEntity.episodes) {
        MediaMetadata md = new MediaMetadata(getId());

        md.setSeasonNumber(i);
        md.setEpisodeNumber(Integer.parseInt(ep.episode));
        md.setId("imdbId", ep.imdbID);
        mediaMetadataList.add(md);
      }
    }

    // cache for further fast access
    if (!mediaMetadataList.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(imdbId + "_" + options.getLanguage().getLanguage(), mediaMetadataList);
    }

    return mediaMetadataList;
  }
}
