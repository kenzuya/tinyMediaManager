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
package org.tinymediamanager.scraper.tmdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TVDB;
import static org.tinymediamanager.scraper.util.MetadataUtil.isValidImdbId;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTmdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTvdbMetadataProvider;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.RatingUtil;
import org.tinymediamanager.scraper.util.TvUtils;

import com.uwetrottmann.tmdb2.entities.AppendToResponse;
import com.uwetrottmann.tmdb2.entities.BaseCompany;
import com.uwetrottmann.tmdb2.entities.BaseKeyword;
import com.uwetrottmann.tmdb2.entities.BaseTvEpisode;
import com.uwetrottmann.tmdb2.entities.BaseTvShow;
import com.uwetrottmann.tmdb2.entities.CastMember;
import com.uwetrottmann.tmdb2.entities.ContentRating;
import com.uwetrottmann.tmdb2.entities.CrewMember;
import com.uwetrottmann.tmdb2.entities.FindResults;
import com.uwetrottmann.tmdb2.entities.Genre;
import com.uwetrottmann.tmdb2.entities.TvEpisode;
import com.uwetrottmann.tmdb2.entities.TvSeason;
import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem;
import com.uwetrottmann.tmdb2.enumerations.ExternalSource;
import com.uwetrottmann.tmdb2.exceptions.TmdbNotFoundException;

import retrofit2.Response;

public class TmdbTvShowMetadataProvider extends TmdbMetadataProvider
    implements ITvShowMetadataProvider, ITvShowTmdbMetadataProvider, ITvShowImdbMetadataProvider, ITvShowTvdbMetadataProvider {
  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(TmdbTvShowMetadataProvider.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(600, 5);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().addBoolean("scrapeLanguageNames", true);
    info.getConfig().addBoolean("titleFallback", false);
    info.getConfig().addSelect("titleFallbackLanguage", PT, "en-US");
    info.getConfig().load();

    return info;
  }

  @Override
  protected String getSubId() {
    return "tvshow";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    // lazy initialization of the api
    initAPI();

    Exception savedException = null;

    SortedSet<MediaSearchResult> results = new TreeSet<>();

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = Utils.removeSortableName(options.getSearchQuery());
    }
    searchString = MetadataUtil.removeNonSearchCharacters(searchString);

    String imdbId = options.getImdbId();
    if (!isValidImdbId(imdbId)) {
      imdbId = "";
    }
    if (isValidImdbId(searchString)) {
      imdbId = searchString;
    }

    int tmdbId = options.getTmdbId();
    int tvdbId = options.getIdAsInt(TVDB);

    String language = getRequestLanguage(options.getLanguage());

    // begin search
    LOGGER.info("========= BEGIN TMDB Scraper Search for: {}", searchString);
    // 1. try with TMDB id
    if (tmdbId != 0) {
      LOGGER.debug("found TMDB ID {} - getting direct", tmdbId);
      try {
        Response<TvShow> httpResponse = api.tvService().tv(tmdbId, language, new AppendToResponse(AppendToResponseItem.TRANSLATIONS)).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        TvShow show = httpResponse.body();
        injectTranslations(Locale.forLanguageTag(language), show);
        results.add(morphTvShowToSearchResult(show, options));
        LOGGER.debug("found {} results with TMDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 2. try with IMDB id
    if (results.isEmpty() && StringUtils.isNotEmpty(imdbId)) {
      LOGGER.debug("found IMDB ID {} - getting direct", imdbId);
      try {
        Response<FindResults> httpResponse = api.findService().find(imdbId, ExternalSource.IMDB_ID, language).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        for (BaseTvShow show : httpResponse.body().tv_results) { // should be only one
          injectTranslations(Locale.forLanguageTag(language), show);
          results.add(morphTvShowToSearchResult(show, options));
        }
        LOGGER.debug("found {} results with IMDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 3. try with TVDB id
    if (results.isEmpty() && tvdbId > 0) {
      LOGGER.debug("found TVDB ID {} - getting direct", tvdbId);
      try {
        Response<FindResults> httpResponse = api.findService().find(tvdbId, ExternalSource.TVDB_ID, language).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        for (BaseTvShow show : httpResponse.body().tv_results) { // should be only one
          injectTranslations(options.getLanguage().toLocale(), show);
          results.add(morphTvShowToSearchResult(show, options));
        }
        LOGGER.debug("found {} results with TVDB id", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tvdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // 4. try with search string and year
    if (results.isEmpty()) {
      try {
        int page = 1;
        int maxPage = 1;

        // get all result pages
        do {
          Response<TvShowResultsPage> httpResponse = api.searchService().tv(searchString, page, language, null).execute();
          if (!httpResponse.isSuccessful() || httpResponse.body() == null) {
            throw new HttpException(httpResponse.code(), httpResponse.message());
          }

          for (BaseTvShow show : ListUtils.nullSafe(httpResponse.body().results)) {
            injectTranslations(Locale.forLanguageTag(language), show);
            results.add(morphTvShowToSearchResult(show, options));
          }

          maxPage = httpResponse.body().total_pages;
          page++;
        } while (page <= maxPage);

        LOGGER.debug("found {} results with search string", results.size());
      }
      catch (Exception e) {
        LOGGER.warn("problem getting data from tmdb: {}", e.getMessage());
        savedException = e;
      }
    }

    // if we have not found anything and there is a saved Exception, throw it to indicate a problem
    if (results.isEmpty() && savedException != null) {
      throw new ScrapeException(savedException);
    }

    return results;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {} ", options);

    // lazy initialization of the api
    initAPI();

    // tmdbId from option
    int tmdbId = options.getTmdbId();

    // try to get via imdb id
    if (MetadataUtil.isValidImdbId(options.getImdbId())) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, options.getImdbId());
      }
      catch (Exception e) {
        LOGGER.warn("could not get tmdb id via imdb id - {}", e.getMessage());
      }
    }

    // try to get via tvdb id
    if (options.getIdAsIntOrDefault(TVDB, 0) > 0) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, options.getIdAsInteger(TVDB));
      }
      catch (Exception e) {
        LOGGER.warn("could not get tmdb id via tvdb id - {}", e.getMessage());
      }
    }

    // no tmdb id, no search..
    if (tmdbId == 0) {
      throw new MissingIdException(MediaMetadata.TMDB);
    }

    String language = getRequestLanguage(options.getLanguage());

    // look in the cache map if there is an entry
    List<MediaMetadata> episodes = EPISODE_LIST_CACHE_MAP.get(tmdbId + "_" + language);
    if (ListUtils.isNotEmpty(episodes)) {
      // cache hit!
      return episodes;
    }

    episodes = new ArrayList<>();

    // the API does not provide a complete access to all episodes, so we have to
    // fetch the show summary first and every season afterwards..
    try {
      Response<TvShow> showResponse = api.tvService().tv(tmdbId, language).execute();
      if (!showResponse.isSuccessful()) {
        throw new HttpException(showResponse.code(), showResponse.message());
      }

      for (TvSeason season : ListUtils.nullSafe(showResponse.body().seasons)) {
        List<MediaMetadata> seasonEpisodes = new ArrayList<>();
        Response<TvSeason> seasonResponse = api.tvSeasonsService()
            .season(tmdbId, season.season_number, language, new AppendToResponse(AppendToResponseItem.TRANSLATIONS))
            .execute();
        if (!seasonResponse.isSuccessful()) {
          throw new HttpException(seasonResponse.code(), seasonResponse.message());
        }
        for (TvEpisode episode : ListUtils.nullSafe(seasonResponse.body().episodes)) {
          // season does not send translations, get em only with full episode scrape
          // verifyTvEpisodeTitleLanguage(options, season, episode)) {
          seasonEpisodes.add(morphTvEpisodeToMediaMetadata(episode));
        }
        episodes.addAll(seasonEpisodes);
      }
    }
    catch (Exception e) {
      LOGGER.debug("failed to get episode list: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    // cache the episode list
    if (!episodes.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(tmdbId + "_" + language, episodes);
    }

    return episodes;
  }

  @Override
  public MediaMetadata getMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());

    // tmdbId from option
    int tmdbId = options.getTmdbId();

    // try to get via imdb id
    if (MetadataUtil.isValidImdbId(options.getImdbId())) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, options.getImdbId());
      }
      catch (Exception e) {
        LOGGER.warn("could not get tmdb id via imdb id - {}", e.getMessage());
      }
    }

    // try to get via tvdb id
    if (options.getIdAsIntOrDefault(TVDB, 0) > 0) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, options.getIdAsInteger(TVDB));
      }
      catch (Exception e) {
        LOGGER.warn("could not get tmdb id via tvdb id - {}", e.getMessage());
      }
    }

    // no tmdb id, no scrape..
    if (tmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    String language = getRequestLanguage(options.getLanguage());

    TvShow complete = null;
    try {
      Response<TvShow> httpResponse = api.tvService()
          .tv(tmdbId, language,
              new AppendToResponse(AppendToResponseItem.TRANSLATIONS, AppendToResponseItem.CREDITS, AppendToResponseItem.EXTERNAL_IDS,
                  AppendToResponseItem.CONTENT_RATINGS, AppendToResponseItem.KEYWORDS))
          .execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      complete = httpResponse.body();
      injectTranslations(Locale.forLanguageTag(language), complete);
    }
    catch (TmdbNotFoundException e) {
      LOGGER.info("nothing found");
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (complete == null) {
      throw new NothingFoundException();
    }

    md.setId(getId(), tmdbId);
    md.setTitle(complete.name);
    md.setOriginalTitle(complete.original_name);

    if (MetadataUtil.unboxInteger(complete.vote_count, 0) > 0) {
      try {
        MediaRating rating = new MediaRating("tmdb");
        rating.setRating(complete.vote_average);
        rating.setVotes(complete.vote_count);
        rating.setMaxValue(10);
        md.addRating(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    md.setReleaseDate(complete.first_air_date);
    md.setPlot(complete.overview);
    for (String country : ListUtils.nullSafe(complete.origin_country)) {
      if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("scrapeLanguageNames"))) {
        md.addCountry(LanguageUtils.getLocalizedCountryForLanguage(options.getLanguage().toLocale(), country));
      }
      else {
        md.addCountry(country);
      }
    }

    if (complete.episode_run_time != null && !complete.episode_run_time.isEmpty()) {
      md.setRuntime(complete.episode_run_time.get(0));
    }

    // Poster
    if (StringUtils.isNotBlank(complete.poster_path)) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
      ma.setPreviewUrl(artworkBaseUrl + "w342" + complete.poster_path);
      ma.setDefaultUrl(artworkBaseUrl + "original" + complete.poster_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + complete.poster_path);
      ma.setLanguage(options.getLanguage().getLanguage());
      ma.setTmdbId(complete.id);
      md.addMediaArt(ma);
    }

    for (BaseCompany company : ListUtils.nullSafe(complete.production_companies)) {
      md.addProductionCompany(company.name.trim());
    }
    md.setStatus(complete.status);

    if (complete.first_air_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(complete.first_air_date);
      md.setYear(calendar.get(Calendar.YEAR));
    }

    if (complete.credits != null) {
      for (CastMember castMember : ListUtils.nullSafe(complete.credits.cast)) {
        Person cm = new Person(ACTOR);
        cm.setId(getProviderInfo().getId(), castMember.id);
        cm.setName(castMember.name);
        cm.setRole(castMember.character);
        if (castMember.id != null) {
          cm.setProfileUrl("https://www.themoviedb.org/person/" + castMember.id);
        }

        if (StringUtils.isNotBlank(castMember.profile_path)) {
          cm.setThumbUrl(artworkBaseUrl + "h632" + castMember.profile_path);
        }

        md.addCastMember(cm);
      }
    }

    // external IDs
    if (complete.external_ids != null) {
      if (complete.external_ids.tvdb_id != null && complete.external_ids.tvdb_id > 0) {
        md.setId(TVDB, complete.external_ids.tvdb_id);
      }
      if (StringUtils.isNotBlank(complete.external_ids.imdb_id)) {
        md.setId(IMDB, complete.external_ids.imdb_id);
      }
      if (complete.external_ids.tvrage_id != null && complete.external_ids.tvrage_id > 0) {
        md.setId("tvrage", complete.external_ids.tvrage_id);
      }
    }

    // content ratings
    if (complete.content_ratings != null) {
      // only use the certification of the desired country (if any country has been chosen)
      CountryCode countryCode = options.getCertificationCountry();

      for (ContentRating country : ListUtils.nullSafe(complete.content_ratings.results)) {
        if (countryCode == null || countryCode.getAlpha2().compareToIgnoreCase(country.iso_3166_1) == 0) {
          // do not use any empty certifications
          if (StringUtils.isEmpty(country.rating)) {
            continue;
          }
          md.addCertification(MediaCertification.getCertification(country.iso_3166_1, country.rating));
        }
      }
    }

    // Genres
    for (Genre genre : ListUtils.nullSafe(complete.genres)) {
      md.addGenre(TmdbMetadataProvider.getTmmGenre(genre));
    }

    // season titles
    for (TvSeason season : ListUtils.nullSafe(complete.seasons)) {
      if (season.season_number != null && StringUtils.isNotBlank(season.name)) {
        md.addSeasonName(season.season_number, season.name);
      }
    }

    // add some special keywords as tags
    // see http://forum.kodi.tv/showthread.php?tid=254004
    if (complete.keywords != null) {
      for (BaseKeyword kw : ListUtils.nullSafe(complete.keywords.keywords)) {
        md.addTag(kw.name);
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

  @Override
  public MediaMetadata getMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());

    int tmdbId = 0;

    // ok, we have 2 flavors here:
    // a) we get the season and episode number -> everything is fine
    // b) we get the episode id (tmdb, imdb or tvdb) -> we need to do the lookup to get the season/episode number

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // if we don't have season/episode yet but a imdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && MetadataUtil.isValidImdbId(options.getImdbId())) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByImdbId(options.getImdbId());
        if (baseTvEpisode != null) {
          tmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by imdb id - {}", e.getMessage());
      }
    }

    // if we don't have season/episode yet but a tvdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && options.getIds().containsKey(TVDB)) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByTvdbId(options.getIdAsString(TVDB));
        if (baseTvEpisode != null) {
          tmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by tvdb id - {}", e.getMessage());
      }
    }

    // get the tv show ids
    if (tmdbId == 0) {
      TvShowSearchAndScrapeOptions tvShowSearchAndScrapeOptions = options.createTvShowSearchAndScrapeOptions();
      tmdbId = tvShowSearchAndScrapeOptions.getTmdbId();

      // try to get via imdb id
      if (tmdbId == 0 && MetadataUtil.isValidImdbId(tvShowSearchAndScrapeOptions.getImdbId())) {
        try {
          tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, tvShowSearchAndScrapeOptions.getImdbId());
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
        }
      }

      // try to get via tvdb id
      if (tmdbId == 0 && tvShowSearchAndScrapeOptions.getIdAsIntOrDefault(TVDB, 0) > 0) {
        try {
          tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, tvShowSearchAndScrapeOptions.getIdAsInteger(TVDB));
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
        }
      }
    }

    // no tmdb id, no scrape..
    if (tmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // if we don't have season/episode yet but a tmdb id -> do the episodelist lookup
    if ((seasonNr == -1 || episodeNr == -1) && options.getIds().containsKey(TMDB)) {
      List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());
      for (MediaMetadata episode : episodes) {
        if ((Integer) episode.getId(TMDB) == options.getIdAsInt(TMDB)) {
          seasonNr = episode.getSeasonNumber();
          episodeNr = episode.getEpisodeNumber();
          break;
        }
      }
    }

    // parsed valid episode number/season number?
    String aired = "";
    if (options.getMetadata() != null && options.getMetadata().getReleaseDate() != null) {
      Format formatter = new SimpleDateFormat("yyyy-MM-dd");
      aired = formatter.format(options.getMetadata().getReleaseDate());
    }
    // does not work - we cannot scrape w/o season
    // if (aired.isEmpty() && (seasonNr == -1 || episodeNr == -1)) {
    if (seasonNr == -1 || episodeNr == -1) {
      LOGGER.warn("season number/episode number found");
      throw new MissingIdException(MediaMetadata.SEASON_NR, MediaMetadata.EPISODE_NR);
    }

    String language = getRequestLanguage(options.getLanguage());

    // get the data from tmdb
    TvEpisode episode = null;
    TvSeason fullSeason = null;
    synchronized (api) {
      // get episode via season listing -> improves caching performance
      try {
        Response<TvSeason> seasonResponse = api.tvSeasonsService()
            .season(tmdbId, seasonNr, language, new AppendToResponse(AppendToResponseItem.CREDITS))
            .execute();
        if (!seasonResponse.isSuccessful()) {
          throw new HttpException(seasonResponse.code(), seasonResponse.message());
        }
        fullSeason = seasonResponse.body();
        for (TvEpisode ep : ListUtils.nullSafe(fullSeason.episodes)) {
          if (MetadataUtil.unboxInteger(ep.season_number, -1) == seasonNr && MetadataUtil.unboxInteger(ep.episode_number, -1) == episodeNr) {
            episode = ep;
            break;
          }
        }

        // not found? try to match by date
        if (episode == null && !aired.isEmpty()) {
          for (TvEpisode ep : ListUtils.nullSafe(fullSeason.episodes)) {
            if (ep.air_date != null) {
              Format formatter = new SimpleDateFormat("yyyy-MM-dd");
              String epAired = formatter.format(ep.air_date);
              if (epAired.equals(aired)) {
                episode = ep;
                break;
              }
            }
          }
        }

        // get full episode data
        if (episode != null) {
          Response<TvEpisode> episodeResponse = api.tvEpisodesService()
              .episode(tmdbId, MetadataUtil.unboxInteger(episode.season_number, -1), MetadataUtil.unboxInteger(episode.episode_number, -1), language,
                  new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS, AppendToResponseItem.TRANSLATIONS, AppendToResponseItem.CREDITS))
              .execute();

          if (!episodeResponse.isSuccessful()) {
            throw new HttpException(seasonResponse.code(), seasonResponse.message());
          }
          episode = episodeResponse.body();
          verifyTvEpisodeTitleLanguage(episode, options);
        }
      }
      catch (TmdbNotFoundException e) {
        LOGGER.info("nothing found");
      }
      catch (Exception e) {
        LOGGER.debug("failed to get meta data: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    if (episode == null || fullSeason == null) {
      throw new NothingFoundException();
    }

    md.setEpisodeNumber(TvUtils.getEpisodeNumber(episode.episode_number));
    md.setSeasonNumber(TvUtils.getSeasonNumber(episode.season_number));
    md.setId(getId(), episode.id);

    // external IDs
    if (episode.external_ids != null) {
      if (MetadataUtil.unboxInteger(episode.external_ids.tvdb_id) > 0) {
        md.setId(TVDB, episode.external_ids.tvdb_id);
      }
      if (MetadataUtil.isValidImdbId(episode.external_ids.imdb_id)) {
        md.setId(IMDB, episode.external_ids.imdb_id);
      }
      if (MetadataUtil.unboxInteger(episode.external_ids.tvrage_id) > 0) {
        md.setId("tvrage", episode.external_ids.tvrage_id);
      }
    }

    md.setTitle(episode.name);
    md.setPlot(episode.overview);

    if (MetadataUtil.unboxInteger(episode.vote_count, 0) > 0) {
      try {
        MediaRating rating = new MediaRating("tmdb");
        rating.setRating(MetadataUtil.unboxDouble(episode.vote_average));
        rating.setVotes(MetadataUtil.unboxInteger(episode.vote_count));
        rating.setMaxValue(10);
        md.addRating(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    md.setReleaseDate(episode.air_date);

    if (fullSeason.credits != null) {
      // season cast
      for (CastMember castMember : ListUtils.nullSafe(fullSeason.credits.cast)) {
        Person cm = new Person(ACTOR);
        cm.setId(getProviderInfo().getId(), castMember.id);
        cm.setName(castMember.name);
        cm.setRole(castMember.character);
        if (castMember.id != null) {
          cm.setProfileUrl("https://www.themoviedb.org/person/" + castMember.id);
        }

        if (StringUtils.isNotBlank(castMember.profile_path)) {
          cm.setThumbUrl(artworkBaseUrl + "h632" + castMember.profile_path);
        }

        md.addCastMember(cm);
      }

      // season crew
      for (CrewMember crewMember : ListUtils.nullSafe(fullSeason.credits.crew)) {
        Person cm = new Person();
        if ("Director".equals(crewMember.job)) {
          cm.setType(DIRECTOR);
          cm.setRole(crewMember.department);
        }
        else if ("Writing".equals(crewMember.department)) {
          cm.setType(WRITER);
          cm.setRole(crewMember.department);
        }
        else if ("Production".equals(crewMember.department)) {
          cm.setType(PRODUCER);
          cm.setRole(crewMember.job);
        }
        else {
          continue;
        }
        cm.setId(getProviderInfo().getId(), crewMember.id);
        cm.setName(crewMember.name);

        if (StringUtils.isNotBlank(crewMember.profile_path)) {
          cm.setThumbUrl(artworkBaseUrl + "h632" + crewMember.profile_path);
        }
        if (crewMember.id != null) {
          cm.setProfileUrl("https://www.themoviedb.org/person/" + crewMember.id);
        }

        md.addCastMember(cm);
      }
    }

    // episode guests
    for (CastMember castMember : ListUtils.nullSafe(episode.guest_stars)) {
      Person cm = new Person(ACTOR);
      cm.setId(getProviderInfo().getId(), castMember.id);
      cm.setName(castMember.name);
      cm.setRole(castMember.character);
      if (castMember.id != null) {
        cm.setProfileUrl("https://www.themoviedb.org/person/" + castMember.id);
      }

      if (StringUtils.isNotBlank(castMember.profile_path)) {
        cm.setThumbUrl(artworkBaseUrl + "h632" + castMember.profile_path);
      }

      md.addCastMember(cm);
    }

    // crew
    for (CrewMember crewMember : ListUtils.nullSafe(episode.crew)) {
      Person cm = new Person();
      if ("Director".equals(crewMember.job)) {
        cm.setType(DIRECTOR);
        cm.setRole(crewMember.department);
      }
      else if ("Writing".equals(crewMember.department)) {
        cm.setType(WRITER);
        cm.setRole(crewMember.department);
      }
      else if ("Production".equals(crewMember.department)) {
        cm.setType(PRODUCER);
        cm.setRole(crewMember.job);
      }
      else {
        continue;
      }
      cm.setId(getProviderInfo().getId(), crewMember.id);
      cm.setName(crewMember.name);

      if (StringUtils.isNotBlank(crewMember.profile_path)) {
        cm.setThumbUrl(artworkBaseUrl + "h632" + crewMember.profile_path);
      }
      if (crewMember.id != null) {
        cm.setProfileUrl("https://www.themoviedb.org/person/" + crewMember.id);
      }

      md.addCastMember(cm);
    }

    // Thumb
    if (StringUtils.isNotBlank(episode.still_path)) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtworkType.THUMB);
      ma.setPreviewUrl(artworkBaseUrl + "w300" + episode.still_path);
      ma.setDefaultUrl(artworkBaseUrl + "original" + episode.still_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + episode.still_path);
      md.addMediaArt(ma);
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

  private BaseTvEpisode getBaseTvEpisodeByImdbId(String imdbId) throws IOException {
    FindResults findResults = api.findService().find(imdbId, ExternalSource.IMDB_ID, null).execute().body();
    if (findResults != null && ListUtils.isNotEmpty(findResults.tv_episode_results)) {
      return findResults.tv_episode_results.get(0);
    }

    return null;
  }

  private BaseTvEpisode getBaseTvEpisodeByTvdbId(String tvdbId) throws IOException {
    FindResults findResults = api.findService().find(tvdbId, ExternalSource.TVDB_ID, null).execute().body();
    if (findResults != null && ListUtils.isNotEmpty(findResults.tv_episode_results)) {
      return findResults.tv_episode_results.get(0);
    }

    return null;
  }

  /**
   * Fallback Language Mechanism - for direct TMDB lookup<br>
   * Title/Overview always gets returned in the original language, if translation has not been found.<br>
   * So we never know exactly what is missing.. so we just inject everything here by hand if a fallback language has been found
   */
  private void injectTranslations(Locale language, TvShow show) {
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      Locale fallbackLanguage = Locale.forLanguageTag(getProviderInfo().getConfig().getValue("titleFallbackLanguage"));
      // get in desired localization
      String[] val = getValuesFromTranslation(show.translations, language);

      // merge empty ones with fallback
      String[] temp = getValuesFromTranslation(show.translations, fallbackLanguage);
      if (StringUtils.isBlank(val[0])) {
        val[0] = temp[0];
      }
      if (StringUtils.isBlank(val[1])) {
        val[1] = temp[1];
      }

      // finally SET the values
      if (StringUtils.isNotBlank(val[0])) {
        show.name = val[0];
      }
      if (StringUtils.isNotBlank(val[1])) {
        show.overview = val[1];
      }
    }
  }

  /**
   * Fallback Language Mechanism - For IMDB & searches
   *
   * @param language
   *          the language to search for
   * @param show
   *          the show
   * @throws IOException
   *           any IOException occurred
   */
  private void injectTranslations(Locale language, BaseTvShow show) throws IOException {
    // NOT doing a fallback scrape when overview empty, used only for SEARCH - unneeded!
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      Locale fallbackLanguage = Locale.forLanguageTag(getProviderInfo().getConfig().getValue("titleFallbackLanguage"));

      // tmdb provides title = originalTitle if no title in the requested language has been found,
      // so get the title in a alternative language
      if ((show.name.equals(show.original_name) && !show.original_language.equals(language.getLanguage())) && !language.equals(fallbackLanguage)) {
        LOGGER.debug("checking for title fallback {}", fallbackLanguage);
        String lang = getProviderInfo().getConfig().getValue("titleFallbackLanguage").replace("_", "-");
        Response<TvShow> httpResponse = api.tvService().tv(show.id, lang, new AppendToResponse(AppendToResponseItem.TRANSLATIONS)).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        TvShow s = httpResponse.body();

        // get in desired localization
        String[] val = getValuesFromTranslation(s.translations, language);

        // merge empty ones with fallback
        String[] temp = getValuesFromTranslation(s.translations, fallbackLanguage);
        if (StringUtils.isBlank(val[0])) {
          val[0] = temp[0];
        }
        if (StringUtils.isBlank(val[1])) {
          val[1] = temp[1];
        }

        // finally SET the values
        if (StringUtils.isNotBlank(val[0])) {
          show.name = val[0];
        }
        if (StringUtils.isNotBlank(val[1])) {
          show.overview = val[1];
        }
      }
    }
  }

  /**
   * Language Fallback Mechanism - For TV Episode
   *
   * @param query
   *          the query options
   * @param episode
   *          the original tv episode
   */
  private void verifyTvEpisodeTitleLanguage(BaseTvEpisode episode, MediaSearchAndScrapeOptions query) {
    int seasonNr = query.getIdAsInt(MediaMetadata.SEASON_NR);
    int episodeNr = query.getIdAsInt(MediaMetadata.EPISODE_NR);

    if (episode != null && (StringUtils.isAnyBlank(episode.name, episode.overview) || isEpisodesNameDefault(episode, episodeNr)
        || getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {

      String languageFallback = MediaLanguages.get(getProviderInfo().getConfig().getValue("titleFallbackLanguage")).name().replace("_", "-");

      try {
        TvEpisode ep = api.tvEpisodesService()
            .episode(query.getTmdbId(), episode.season_number, episode.episode_number, languageFallback)
            .execute()
            .body();
        if (ep != null) {
          if ((ep.season_number == seasonNr || ep.episode_number.equals(episode.season_number))
              && (ep.episode_number == episodeNr || ep.episode_number.equals(episode.episode_number))) {

            if (StringUtils.isBlank(episode.name) || (isEpisodesNameDefault(episode, episodeNr) && !isEpisodesNameDefault(ep, episodeNr))) {
              episode.name = ep.name;
            }
            if (StringUtils.isBlank(episode.overview)) {
              episode.overview = ep.overview;
            }
          }
        }
      }
      catch (Exception ignored) {

      }
    }
  }

  private Integer toInteger(String str) {
    try {
      return Integer.parseInt(str);
    }
    catch (Exception exc) {
      return null;
    }
  }

  private Boolean isEpisodesNameDefault(BaseTvEpisode episode, Integer episodeNr) {
    Integer potentialEpisodeNumber;
    String[] originalEpisodeName;
    return (originalEpisodeName = episode.name.split(" ")).length == 2 && (potentialEpisodeNumber = toInteger(originalEpisodeName[1])) != null
        && (potentialEpisodeNumber.equals(episode.episode_number) || potentialEpisodeNumber.equals(episodeNr));
  }

  private MediaMetadata morphTvEpisodeToMediaMetadata(BaseTvEpisode episode) {
    MediaMetadata ep = new MediaMetadata(getId());
    ep.setId(getProviderInfo().getId(), episode.id);
    ep.setEpisodeNumber(episode.episode_number);
    ep.setSeasonNumber(episode.season_number);
    ep.setTitle(episode.name);
    ep.setPlot(episode.overview);

    if (episode.vote_average != null && MetadataUtil.unboxInteger(episode.vote_count, 0) > 0) {
      MediaRating rating = new MediaRating(getProviderInfo().getId());
      rating.setRating(episode.vote_average);
      rating.setVotes(episode.vote_count);
      rating.setMaxValue(10);
      ep.addRating(rating);
    }
    if (episode.air_date != null) {
      ep.setReleaseDate(episode.air_date);
    }

    return ep;
  }

  private MediaSearchResult morphTvShowToSearchResult(BaseTvShow tvShow, TvShowSearchAndScrapeOptions query) {

    MediaSearchResult result = new MediaSearchResult(getId(), MediaType.TV_SHOW);
    result.setId(Integer.toString(tvShow.id));
    result.setTitle(tvShow.name);
    result.setOriginalTitle(tvShow.original_name);
    result.setOverview(tvShow.overview);

    if (tvShow.poster_path != null && !tvShow.poster_path.isEmpty()) {
      result.setPosterUrl(artworkBaseUrl + "w342" + tvShow.poster_path);
    }

    // parse release date to year
    if (tvShow.first_air_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(tvShow.first_air_date);
      result.setYear(calendar.get(Calendar.YEAR));
    }

    // calculate score
    if ((StringUtils.isNotBlank(query.getImdbId()) && query.getImdbId().equals(result.getIMDBId()))
        || String.valueOf(query.getTmdbId()).equals(result.getId())) {
      LOGGER.debug("perfect match by ID - set score to 1");
      result.setScore(1);
    }
    else {
      // calculate the score by comparing the search result with the search options
      result.calculateScore(query);
    }

    return result;
  }

  /**
   * Is i1 != i2 (when >0)
   */
  private boolean yearDiffers(int i1, int i2) {
    return i1 > 0 && i2 > 0 && i1 != i2;
  }

}
