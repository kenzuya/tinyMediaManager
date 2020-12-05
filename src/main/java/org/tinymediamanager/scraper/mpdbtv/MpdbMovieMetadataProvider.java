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
package org.tinymediamanager.scraper.mpdbtv;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.mpdbtv.entities.Actor;
import org.tinymediamanager.scraper.mpdbtv.entities.Director;
import org.tinymediamanager.scraper.mpdbtv.entities.Genre;
import org.tinymediamanager.scraper.mpdbtv.entities.MovieEntity;
import org.tinymediamanager.scraper.mpdbtv.entities.Producer;
import org.tinymediamanager.scraper.mpdbtv.entities.Release;
import org.tinymediamanager.scraper.mpdbtv.entities.SearchEntity;
import org.tinymediamanager.scraper.mpdbtv.entities.Studio;
import org.tinymediamanager.scraper.mpdbtv.entities.Trailer;
import org.tinymediamanager.scraper.mpdbtv.services.Controller;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * The Class {@link MpdbMovieMetadataProvider}. Movie metdata provider for the site MPDB.tv
 * 
 * @author Wolfgang Janes
 */
public class MpdbMovieMetadataProvider extends MpdbMetadataProvider implements IMovieMetadataProvider {
  private static final Logger     LOGGER = LoggerFactory.getLogger(MpdbMovieMetadataProvider.class);

  private final MediaProviderInfo providerInfo;
  private final Controller        controller;

  public MpdbMovieMetadataProvider() {
    providerInfo = createMediaProviderInfo();
    controller = new Controller(false);
  }

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    SortedSet<MediaSearchResult> results = new TreeSet<>();
    List<SearchEntity> searchResult;

    if (StringUtils.isAnyBlank(getAboKey(), getUserName())) {
      LOGGER.warn("no username/ABO Key found");
      throw new ScrapeException(new HttpException(401, "Unauthorized"));
    }

    // due to the poor API of mpdb we have to check if the selected language is FR
    // or EN.
    if (!options.getLanguage().getLanguage().equalsIgnoreCase("FR")) {

      LOGGER.info("Scraper only supports Language FR");
      return results;

    }

    LOGGER.info("========= BEGIN MPDB.tv Scraper Search for Movie: {} ", options.getSearchQuery());

    try {
      searchResult = controller.getSearchInformation(License.getInstance().getApiKey(ID), getEncodedUserName(), getSubscriptionKey(),
          options.getSearchQuery(), options.getLanguage().toLocale(), true, FORMAT);
    }
    catch (Exception e) {
      LOGGER.error("error searching: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResult == null) {
      LOGGER.warn("no result from MPDB.tv");
      return results;
    }

    for (SearchEntity entity : searchResult) {

      MediaSearchResult result = new MediaSearchResult(providerInfo.getId(), MediaType.MOVIE);
      result.setId(providerInfo.getId(), entity.id);
      result.setOriginalTitle(StringEscapeUtils.unescapeHtml4(entity.original_title));
      if (StringUtils.isEmpty(entity.title)) {
        result.setTitle(StringEscapeUtils.unescapeHtml4(entity.original_title));
      }
      else {
        result.setTitle(StringEscapeUtils.unescapeHtml4(entity.title));
      }
      result.setYear(entity.year);
      if (MetadataUtil.isValidImdbId(entity.id_imdb)) {
        result.setId("imdb_id", entity.id_imdb);
      }
      result.setId("allocine_id", entity.id_allocine);
      result.setUrl(entity.url);
      result.setPosterUrl(entity.posterUrl);

      // calcuate the result score
      result.calculateScore(options);
      results.add(result);
    }

    return results;
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions mediaScrapeOptions) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", mediaScrapeOptions);

    MediaMetadata metadata = new MediaMetadata(providerInfo.getId());
    MovieEntity scrapeResult;

    if (StringUtils.isAnyBlank(getAboKey(), getUserName())) {
      LOGGER.warn("no username/ABO Key found");
      throw new ScrapeException(new HttpException(401, "Unauthorized"));
    }

    LOGGER.info("========= BEGIN MPDB.tv scraping");
    try {
      scrapeResult = controller.getScrapeInformation(License.getInstance().getApiKey(ID), getEncodedUserName(), getSubscriptionKey(),
          mediaScrapeOptions.getIdAsString(providerInfo.getId()), mediaScrapeOptions.getLanguage().toLocale(), null, FORMAT);
    }
    catch (Exception e) {
      LOGGER.error("error searching: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    if (scrapeResult == null) {
      LOGGER.warn("no result from MPDB.tv");
      return metadata;
    }

    // Rating
    if (scrapeResult.rating != null) {
      MediaRating rating = new MediaRating("mpdb.tv");
      rating.setRating(scrapeResult.rating.floatValue());
      rating.setVotes(scrapeResult.ratingVotes);
      rating.setMaxValue(10);

      metadata.addRating(rating);
    }

    // Genres
    ArrayList<MediaGenres> mediaGenres = new ArrayList<>();

    for (Genre genre : scrapeResult.genres) {
      mediaGenres.add(MediaGenres.getGenre(genre.name));
    }
    metadata.setGenres(mediaGenres);

    // Trailers
    ArrayList<MediaTrailer> mediaTrailers = new ArrayList<>();

    for (Trailer trailer : scrapeResult.trailers) {
      MediaTrailer mt = new MediaTrailer();
      mt.setName(scrapeResult.title);
      mt.setUrl(trailer.url);
      mt.setQuality(trailer.quality);

      mediaTrailers.add(mt);
    }
    metadata.setTrailers(mediaTrailers);

    // Studios
    ArrayList<String> productionCompanies = new ArrayList<>();

    for (Studio studio : scrapeResult.studios) {
      productionCompanies.add(studio.name);
    }

    metadata.setProductionCompanies(productionCompanies);

    // Cast
    ArrayList<Person> castMembers = new ArrayList<>();

    for (Director director : scrapeResult.directors) {
      Person mediaCastMember = new Person(DIRECTOR);
      mediaCastMember.setId(providerInfo.getId(), director.id);
      mediaCastMember.setName(director.name);
      mediaCastMember.setRole(director.departement);
      mediaCastMember.setThumbUrl(director.thumb);
      mediaCastMember.setId(providerInfo.getId(), director.id);
      castMembers.add(mediaCastMember);
    }

    for (Actor actor : scrapeResult.actors) {
      Person mediaCastMember = new Person(ACTOR);
      mediaCastMember.setId(providerInfo.getId(), actor.id);
      mediaCastMember.setName(actor.name);
      mediaCastMember.setRole(actor.role);
      mediaCastMember.setThumbUrl(actor.thumb);
      castMembers.add(mediaCastMember);
    }

    for (Producer producer : scrapeResult.producers) {
      Person mediaCastMember = new Person(PRODUCER);
      mediaCastMember.setId(providerInfo.getId(), producer.id);
      mediaCastMember.setName(producer.name);
      mediaCastMember.setRole(producer.departement);
      mediaCastMember.setThumbUrl(producer.thumb);
      castMembers.add(mediaCastMember);
    }
    metadata.setCastMembers(castMembers);

    // Year
    // Get Year from Release Information for given Language
    // I see no other possibility :(
    for (Release release : scrapeResult.releases) {
      if (release.countryId.equals(mediaScrapeOptions.getLanguage().getLanguage().toUpperCase())) {
        metadata.setYear(release.year);
      }
    }

    metadata.setId("allocine", scrapeResult.idAllocine);
    if (MetadataUtil.isValidImdbId(scrapeResult.idImdb)) {
      metadata.setId("imdb", scrapeResult.idImdb);
    }
    metadata.setId("tmdb", scrapeResult.idTmdb);
    metadata.setTagline(scrapeResult.tagline);
    metadata.setReleaseDate(new Date(scrapeResult.firstRelease));
    metadata.setTitle(scrapeResult.title);
    metadata.setOriginalTitle(scrapeResult.originalTitle);
    metadata.setRuntime(scrapeResult.runtime);
    metadata.setPlot(scrapeResult.plot);

    return metadata;
  }
}
