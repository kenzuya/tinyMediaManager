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
package org.tinymediamanager.scraper.thetvdb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.CompanyBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.ContentRating;
import org.tinymediamanager.scraper.thetvdb.entities.GenreBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.MovieExtendedRecord;
import org.tinymediamanager.scraper.thetvdb.entities.MovieExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.entities.Release;
import org.tinymediamanager.scraper.thetvdb.entities.RemoteID;
import org.tinymediamanager.scraper.thetvdb.entities.SearchResultRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SearchResultResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SearchType;
import org.tinymediamanager.scraper.thetvdb.entities.Translation;
import org.tinymediamanager.scraper.thetvdb.entities.TranslationResponse;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.RatingUtil;
import org.tinymediamanager.scraper.util.StrgUtils;

import retrofit2.Response;

/**
 * the class {@link TheTvDbMovieMetadataProvider} offers the movie metadata provider for TheTvDb
 *
 * @author Manuel Laggner
 */
public class TheTvDbMovieMetadataProvider extends TheTvDbMetadataProvider implements IMovieMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TheTvDbMovieMetadataProvider.class);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addText("apiKey", "", true);

    ArrayList<String> fallbackLanguages = new ArrayList<>();
    for (MediaLanguages mediaLanguages : MediaLanguages.values()) {
      fallbackLanguages.add(mediaLanguages.toString());
    }
    info.getConfig().addSelect(FALLBACK_LANGUAGE, fallbackLanguages.toArray(new String[0]), MediaLanguages.en.toString());
    info.getConfig().load();

    return info;
  }

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("search() {}", options);
    SortedSet<MediaSearchResult> results = new TreeSet<>();

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotBlank(options.getSearchQuery())) {
      searchString = options.getSearchQuery();
    }

    int tvdbId = options.getIdAsInt(getId());
    // if we have an TVDB id, use that!
    if (tvdbId != 0) {
      LOGGER.debug("found TvDb ID {} - getting direct", tvdbId);
      try {
        MediaMetadata md = getMetadata(options);
        if (md != null) {
          results.add(morphMediaMetadataToSearchResult(md, MediaType.MOVIE));
          return results;
        }
      }
      catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb via ID: {}", e.getMessage());
      }
    }

    List<SearchResultRecord> searchResults = null;

    // only search when we did not find something by ID (and search string or IMDB is present)
    if (StringUtils.isNotBlank(searchString)) {
      try {
        Response<SearchResultResponse> httpResponse = tvdb.getSearchService().getSearch(searchString, SearchType.MOVIE).execute();

        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }

        searchResults = httpResponse.body().data;

        // nothing found - but maybe only the tvdb id entered?
        if (ListUtils.isEmpty(searchResults) && ID_PATTERN.matcher(searchString).matches()) {
          LOGGER.debug("nothing found, but search term '{}' looks like a TvDb ID - getting direct", searchString);
          try {
            MediaMetadata md = getMetadata(options);
            if (md != null) {
              results.add(morphMediaMetadataToSearchResult(md, MediaType.MOVIE));
              return results;
            }
          }
          catch (Exception e) {
            LOGGER.error("problem getting data vom tvdb via ID: {}", e.getMessage());
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    if (ListUtils.isEmpty(searchResults)) {
      return results;
    }

    // make sure there are no duplicates (e.g. if a movie has been found in both languages)
    Map<String, MediaSearchResult> resultMap = new HashMap<>();

    for (SearchResultRecord searchResultRecord : searchResults) {
      // build up a new result
      MediaSearchResult result = new MediaSearchResult(getId(), options.getMediaType());

      String id = "";
      if (StringUtils.isNotBlank(searchResultRecord.tvdbId)) {
        // the TVDB should be here
        id = searchResultRecord.tvdbId;
      }
      else if (StringUtils.isNotBlank(searchResultRecord.id)) {
        // we can parse it out here too
        id = searchResultRecord.id.replace("movie-", "");
      }

      if (StringUtils.isBlank(id)) {
        // no valid if found? need to go to the next result
        continue;
      }

      result.setId(id);

      MediaLanguages baseLanguage = options.getLanguage();
      MediaLanguages fallbackLanguage = null;
      if (StringUtils.isNotBlank(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE))) {
        fallbackLanguage = MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE));
      }

      String title = parseLocalizedText(baseLanguage, searchResultRecord.nameTranslated);
      if (StringUtils.isNotBlank(title)) {
        result.setTitle(title);
      }
      else {
        // try fallback
        title = parseLocalizedText(fallbackLanguage, searchResultRecord.nameTranslated);
        if (StringUtils.isNotBlank(title)) {
          result.setTitle(title);
        }
        else {
          result.setTitle(searchResultRecord.name);
        }
      }

      String overview = parseLocalizedText(baseLanguage, searchResultRecord.overviewTranslated);
      if (StringUtils.isNotBlank(overview)) {
        result.setOverview(overview);
      }
      else {
        // try fallback
        overview = parseLocalizedText(fallbackLanguage, searchResultRecord.overviewTranslated);
        if (StringUtils.isNotBlank(overview)) {
          result.setOverview(overview);
        }
        else {
          result.setOverview(searchResultRecord.overview);
        }
      }
      result.setYear(MetadataUtil.parseInt(searchResultRecord.year, 0));
      result.setPosterUrl(searchResultRecord.imageUrl);

      // calculate score
      result.calculateScore(options);
      resultMap.put(id, result);
    }

    // and convert all entries from the map to a list
    results.addAll(resultMap.values());

    return results;
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getMetadata(): {}", options);
    MediaMetadata md = new MediaMetadata(getId());

    // do we have an id from the options?
    Integer id = options.getIdAsInteger(getId());
    if (id == null || id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(getId());
    }

    MovieExtendedRecord movie;
    Translation baseTranslation = null;
    Translation fallbackTranslation = null;

    try {
      // language in 3 char
      String baseLanguage = LanguageUtils.getIso3Language(options.getLanguage().toLocale());
      String fallbackLanguage = LanguageUtils
          .getIso3Language(MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE)).toLocale());

      // pt-BR is pt at tvdb...
      if ("pob".equals(baseLanguage)) {
        baseLanguage = "pt";
      }
      if ("pob".equals(fallbackLanguage)) {
        fallbackLanguage = "pt";
      }

      Response<MovieExtendedResponse> httpResponse = tvdb.getMoviesService().getMovieExtended(id).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      movie = httpResponse.body().data;

      // base translation (needed for title and overview)
      if (movie.overviewTranslations.contains(baseLanguage)) {
        Response<TranslationResponse> translationResponse = tvdb.getMoviesService().getMoviesTranslation(id, baseLanguage).execute();
        if (translationResponse.isSuccessful()) {
          baseTranslation = translationResponse.body().data;
        }
      }

      // also get fallback is either title or overview of the base translation is missing
      if ((baseTranslation == null || StringUtils.isAnyBlank(baseTranslation.name, baseTranslation.overview))
          && movie.overviewTranslations.contains(fallbackLanguage)) {
        Response<TranslationResponse> translationResponse = tvdb.getMoviesService().getMoviesTranslation(id, fallbackLanguage).execute();
        if (translationResponse.isSuccessful()) {
          fallbackTranslation = translationResponse.body().data;
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (movie == null) {
      throw new NothingFoundException();
    }

    // populate metadata
    md.setId(getId(), movie.id);

    if (baseTranslation != null && StringUtils.isNotBlank(baseTranslation.name)) {
      md.setTitle(baseTranslation.name);
    }
    else if (fallbackTranslation != null && StringUtils.isNotBlank(fallbackTranslation.overview)) {
      md.setTitle(fallbackTranslation.name);
    }
    else {
      md.setTitle(movie.name);
    }

    md.setOriginalTitle(movie.name);

    if (baseTranslation != null && StringUtils.isNotBlank(baseTranslation.overview)) {
      md.setPlot(baseTranslation.overview);
    }
    else if (fallbackTranslation != null && StringUtils.isNotBlank(fallbackTranslation.overview)) {
      md.setPlot(fallbackTranslation.overview);
    }

    for (RemoteID remoteID : ListUtils.nullSafe(movie.remoteIds)) {
      if (StringUtils.isAnyBlank(remoteID.sourceName, remoteID.id)) {
        continue;
      }

      switch (remoteID.sourceName) {
        case "IMDB":
          if (MetadataUtil.isValidImdbId(remoteID.id)) {
            md.setId(MediaMetadata.IMDB, remoteID.id);
          }
          break;

        case "Zap2It":
          md.setId("zap2it", remoteID.id);
          break;

        case "TheMovieDB.com":
          md.setId(MediaMetadata.TMDB, MetadataUtil.parseInt(remoteID.id, 0));
          break;

        default:
          break;
      }
    }

    Date localReleaseDate = null;
    Date globalReleaseDate = null;
    for (Release release : ListUtils.nullSafe(movie.releases)) {
      try {
        Date date = StrgUtils.parseDate(release.date);

        if ("global".equalsIgnoreCase(release.country)) {
          globalReleaseDate = date;
        }

        if (options.getReleaseDateCountry().equals(release.country)) {
          localReleaseDate = date;
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not parse release date - '{}'", e.getMessage());
      }
    }

    Date releaseDate = null;
    if (localReleaseDate != null) {
      releaseDate = localReleaseDate;
    }
    else if (globalReleaseDate != null) {
      releaseDate = globalReleaseDate;
    }

    if (releaseDate != null) {
      md.setReleaseDate(releaseDate);
    }

    if (globalReleaseDate != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(globalReleaseDate);
      int y = calendar.get(Calendar.YEAR);
      md.setYear(y);
      if (y != 0 && md.getTitle().contains(String.valueOf(y))) {
        LOGGER.debug("Weird TVDB entry - removing date {} from title", y);
        md.setTitle(clearYearFromTitle(md.getTitle(), y));
      }
    }

    md.setRuntime(MetadataUtil.unboxInteger(movie.runtime, 0));

    if (movie.companies != null) {
      for (CompanyBaseRecord baseRecord : ListUtils.nullSafe(movie.companies.production)) {
        md.addProductionCompany(baseRecord.name);
      }
      for (CompanyBaseRecord baseRecord : ListUtils.nullSafe(movie.companies.studio)) {
        md.addProductionCompany(baseRecord.name);
      }
      for (CompanyBaseRecord baseRecord : ListUtils.nullSafe(movie.companies.network)) {
        md.addProductionCompany(baseRecord.name);
      }
      for (CompanyBaseRecord baseRecord : ListUtils.nullSafe(movie.companies.specialEffects)) {
        md.addProductionCompany(baseRecord.name);
      }
      for (CompanyBaseRecord baseRecord : ListUtils.nullSafe(movie.companies.distributor)) {
        md.addProductionCompany(baseRecord.name);
      }
    }

    for (Person member : parseCastMembers(movie.characters)) {
      md.addCastMember(member);
    }

    // genres
    for (GenreBaseRecord genreBaseRecord : ListUtils.nullSafe(movie.genres)) {
      md.addGenre(MediaGenres.getGenre(genreBaseRecord.name));
    }

    // artwork
    for (ArtworkBaseRecord artworkBaseRecord : ListUtils.nullSafe(movie.artworks)) {
      MediaArtwork mediaArtwork = parseArtwork(artworkBaseRecord);
      if (mediaArtwork != null) {
        md.addMediaArt(mediaArtwork);
      }
    }

    // certifications
    for (ContentRating contentRating : ListUtils.nullSafe(movie.contentRatings)) {
      MediaCertification certification = MediaCertification.findCertification(contentRating.name);
      if (certification != null && certification != MediaCertification.UNKNOWN) {
        md.addCertification(certification);
      }
    }

    // also try to get the IMDB rating
    if (md.getId(MediaMetadata.IMDB) instanceof String) {
      MediaRating imdbRating = RatingUtil.getImdbRating((String) md.getId(MediaMetadata.IMDB));
      if (imdbRating != null) {
        md.addRating(imdbRating);
      }
    }

    return md;
  }
}
