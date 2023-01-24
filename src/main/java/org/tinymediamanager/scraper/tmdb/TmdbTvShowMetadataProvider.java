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
package org.tinymediamanager.scraper.tmdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TVDB;
import static org.tinymediamanager.scraper.util.MediaIdUtil.isValidImdbId;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaIdProvider;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTmdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTvdbMetadataProvider;
import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.BaseCompany;
import org.tinymediamanager.scraper.tmdb.entities.BaseKeyword;
import org.tinymediamanager.scraper.tmdb.entities.BaseTvEpisode;
import org.tinymediamanager.scraper.tmdb.entities.BaseTvShow;
import org.tinymediamanager.scraper.tmdb.entities.CastMember;
import org.tinymediamanager.scraper.tmdb.entities.ContentRating;
import org.tinymediamanager.scraper.tmdb.entities.CrewMember;
import org.tinymediamanager.scraper.tmdb.entities.FindResults;
import org.tinymediamanager.scraper.tmdb.entities.Genre;
import org.tinymediamanager.scraper.tmdb.entities.Image;
import org.tinymediamanager.scraper.tmdb.entities.Network;
import org.tinymediamanager.scraper.tmdb.entities.TvEpisode;
import org.tinymediamanager.scraper.tmdb.entities.TvSeason;
import org.tinymediamanager.scraper.tmdb.entities.TvShow;
import org.tinymediamanager.scraper.tmdb.entities.TvShowResultsPage;
import org.tinymediamanager.scraper.tmdb.enumerations.AppendToResponseItem;
import org.tinymediamanager.scraper.tmdb.enumerations.ExternalSource;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.TvUtils;

import retrofit2.Response;

public class TmdbTvShowMetadataProvider extends TmdbMetadataProvider implements ITvShowMetadataProvider, ITvShowTmdbMetadataProvider,
    ITvShowImdbMetadataProvider, ITvShowTvdbMetadataProvider, IRatingProvider, IMediaIdProvider {
  private static final Logger                                LOGGER                      = LoggerFactory.getLogger(TmdbTvShowMetadataProvider.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP      = new CacheMap<>(600, 5);
  private static final CacheMap<Integer, String>             ORIGINAL_LANGUAGE_CACHE_MAP = new CacheMap<>(600, 5);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().addBoolean("includeAdultShows", false);
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

    boolean adult = getProviderInfo().getConfig().getValueAsBool("includeAdultShows");

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
          Response<TvShowResultsPage> httpResponse = api.searchService().tv(searchString, page, language, null, adult).execute();
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
    if (MediaIdUtil.isValidImdbId(options.getImdbId())) {
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
          seasonEpisodes.add(morphTvEpisodeToMediaMetadata(episode, options));
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
    md.setScrapeOptions(options);

    // tmdbId from option
    int tmdbId = options.getTmdbId();

    // try to get via imdb id
    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(options.getImdbId())) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, options.getImdbId());
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
      }
    }

    // try to get via tvdb id
    if (tmdbId == 0 && options.getIdAsIntOrDefault(TVDB, 0) > 0) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, options.getIdAsInteger(TVDB));
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
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

    // scrape networks before all other production companies
    for (Network network : ListUtils.nullSafe(complete.networks)) {
      md.addProductionCompany(network.name.trim());
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
        // do not use any empty certifications
        if (StringUtils.isEmpty(country.rating) && countryCode != null && country.iso_3166_1.equalsIgnoreCase(countryCode.getAlpha2())) {
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

    return md;
  }

  @Override
  public MediaMetadata getMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    int tmdbId = 0;

    // ok, we have 2 flavors here:
    // a) we get the season and episode number -> everything is fine
    // b) we get the episode id (tmdb, imdb or tvdb) -> we need to do the lookup to get the season/episode number

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // if we don't have season/episode yet but a imdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && MediaIdUtil.isValidImdbId(options.getImdbId())) {
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
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByTvdbId(options.getIdAsInt(TVDB));
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
      if (tmdbId == 0 && MediaIdUtil.isValidImdbId(tvShowSearchAndScrapeOptions.getImdbId())) {
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
      if (episode != null && episode.episode_number != null && episode.season_number != null) {
        Response<TvEpisode> episodeResponse = api.tvEpisodesService()
            .episode(tmdbId, episode.season_number, episode.episode_number, language,
                new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS, AppendToResponseItem.TRANSLATIONS, AppendToResponseItem.CREDITS,
                    AppendToResponseItem.IMAGES))
            .execute();

        if (!episodeResponse.isSuccessful()) {
          throw new HttpException(seasonResponse.code(), seasonResponse.message());
        }
        episode = episodeResponse.body();
        injectTranslations(Locale.forLanguageTag(language), episode, tmdbId);
      }
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
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
      if (MediaIdUtil.isValidImdbId(episode.external_ids.imdb_id)) {
        md.setId(IMDB, episode.external_ids.imdb_id);
      }
      if (MetadataUtil.unboxInteger(episode.external_ids.tvrage_id) > 0) {
        md.setId("tvrage", episode.external_ids.tvrage_id);
      }
    }

    md.setTitle(episode.name);
    md.setOriginalTitle(episode.originalName);
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
        }
        else if ("Writing".equals(crewMember.department)) {
          cm.setType(WRITER);
        }
        else if ("Production".equals(crewMember.department)) {
          cm.setType(PRODUCER);
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
      }
      else if ("Writing".equals(crewMember.department)) {
        cm.setType(WRITER);
      }
      else if ("Production".equals(crewMember.department)) {
        cm.setType(PRODUCER);
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

    // Thumbs
    if (episode.images != null && ListUtils.isNotEmpty(episode.images.stills)) {
      for (Image image : episode.images.stills) {
        MediaArtwork ma = new MediaArtwork(getId(), MediaArtworkType.THUMB);
        ma.setPreviewUrl(artworkBaseUrl + "w300" + image.file_path);
        ma.setDefaultUrl(artworkBaseUrl + "original" + image.file_path);
        ma.setOriginalUrl(artworkBaseUrl + "original" + image.file_path);

        // add different sizes
        // original (most of the time 1920x1080)
        ma.addImageSize(image.width, image.height, artworkBaseUrl + "original" + image.file_path);
        // 1280x720
        if (1280 < image.width) {
          ma.addImageSize(1280, image.height * 1280 / image.width, artworkBaseUrl + "w1280" + image.file_path);
        }
        // w300
        if (300 < image.width) {
          ma.addImageSize(300, image.height * 300 / image.width, artworkBaseUrl + "w300" + image.file_path);
        }

        md.addMediaArt(ma);
      }
    }
    else if (StringUtils.isNotBlank(episode.still_path)) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtworkType.THUMB);
      ma.setPreviewUrl(artworkBaseUrl + "w300" + episode.still_path);
      ma.setDefaultUrl(artworkBaseUrl + "original" + episode.still_path);
      ma.setOriginalUrl(artworkBaseUrl + "original" + episode.still_path);
      ma.addImageSize(1920, 1080, artworkBaseUrl + "original" + episode.still_path);
      md.addMediaArt(ma);
    }

    return md;
  }

  @Override
  public List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType == MediaType.TV_SHOW) {
      return getTvShowRatings(ids);
    }
    else if (mediaType == MediaType.TV_EPISODE) {
      return getEpisodeRatings(ids);
    }
    else {
      return Collections.emptyList();
    }
  }

  private List<MediaRating> getTvShowRatings(Map<String, Object> ids) throws ScrapeException {
    LOGGER.debug("getRatings(): {}", ids);

    // lazy initialization of the api
    initAPI();

    int tmdbId = MediaIdUtil.getIdAsInt(ids, MediaMetadata.TMDB);
    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);

    // try to get via imdb id
    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(imdbId)) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
      }
    }

    // try to get via tvdb id
    if (tmdbId == 0 && MediaIdUtil.getIdAsInt(ids, MediaMetadata.TVDB) > 0) {
      try {
        tmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, MediaIdUtil.getIdAsInt(ids, MediaMetadata.TVDB));
      }
      catch (Exception e) {
        LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
      }
    }

    // no tmdb id, no scrape..
    if (tmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    TvShow complete = null;
    try {
      Response<TvShow> httpResponse = api.tvService().tv(tmdbId, "en", null).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      complete = httpResponse.body();
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (complete == null) {
      throw new NothingFoundException();
    }

    if (complete.vote_average != null && complete.vote_count != null) {
      try {
        MediaRating rating = new MediaRating("tmdb");
        rating.setRating(complete.vote_average);
        rating.setVotes(complete.vote_count);
        rating.setMaxValue(10);
        return Collections.singletonList(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    return Collections.emptyList();
  }

  private List<MediaRating> getEpisodeRatings(Map<String, Object> ids) throws ScrapeException {
    LOGGER.debug("getRatings(): {}", ids);

    Map<String, Object> showIds = new HashMap<>();
    int showTmdbId = 0;

    try {
      showIds.putAll((Map<? extends String, ?>) ids.get(MediaMetadata.TVSHOW_IDS));
    }
    catch (Exception e) {
      LOGGER.debug("could not get TV show ids - '{}'", e.getMessage());
    }

    // ok, we have 2 flavors here:
    // a) we get the season and episode number -> everything is fine
    // b) we get the episode id (tmdb, imdb or tvdb) -> we need to do the lookup to get the season/episode number

    // get episode number and season number
    int seasonNr = MediaIdUtil.getIdAsIntOrDefault(ids, MediaMetadata.SEASON_NR, -1);
    int episodeNr = MediaIdUtil.getIdAsIntOrDefault(ids, MediaMetadata.EPISODE_NR, -1);

    // if we don't have season/episode yet but a imdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && MediaIdUtil.isValidImdbId(MediaIdUtil.getIdAsString(ids, IMDB))) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByImdbId(MediaIdUtil.getIdAsString(ids, IMDB));
        if (baseTvEpisode != null) {
          showTmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by imdb id - {}", e.getMessage());
      }
    }

    // if we don't have season/episode yet but a tvdb id -> do the find lookup
    if ((seasonNr == -1 || episodeNr == -1) && MediaIdUtil.getIdAsInt(ids, TVDB) > 0) {
      try {
        BaseTvEpisode baseTvEpisode = getBaseTvEpisodeByTvdbId(MediaIdUtil.getIdAsInt(ids, TVDB));
        if (baseTvEpisode != null) {
          showTmdbId = MetadataUtil.unboxInteger(baseTvEpisode.show_id);
          seasonNr = MetadataUtil.unboxInteger(baseTvEpisode.season_number, -1);
          episodeNr = MetadataUtil.unboxInteger(baseTvEpisode.episode_number, -1);
        }
      }
      catch (Exception e) {
        LOGGER.debug("Could not get episode number by tvdb id - {}", e.getMessage());
      }
    }

    // get the tv show ids
    if (showTmdbId == 0 && ids.get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
      showTmdbId = MediaIdUtil.getIdAsInt(showIds, TMDB);

      // try to get via imdb id
      if (showTmdbId == 0 && MediaIdUtil.isValidImdbId(MediaIdUtil.getIdAsString(showIds, IMDB))) {
        try {
          showTmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, MediaIdUtil.getIdAsString(showIds, IMDB));
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via imdb id - {}", e.getMessage());
        }
      }

      // try to get via tvdb id
      if (showTmdbId == 0 && MediaIdUtil.getIdAsInt(showIds, TVDB) > 0) {
        try {
          showTmdbId = TmdbUtils.getTmdbIdFromTvdbId(api, MediaIdUtil.getIdAsInt(showIds, TVDB));
        }
        catch (Exception e) {
          LOGGER.debug("could not get tmdb id via tvdb id - {}", e.getMessage());
        }
      }
    }

    // no tmdb id, no scrape..
    if (showTmdbId == 0) {
      LOGGER.warn("not possible to scrape from TMDB - no tmdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // if we don't have season/episode yet but a tmdb id -> do the episodelist lookup
    if ((seasonNr == -1 || episodeNr == -1) && MediaIdUtil.getIdAsInt(ids, TMDB) > 0) {
      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setIds(showIds);

      int episodeTmdbId = MediaIdUtil.getIdAsInt(ids, TMDB);

      List<MediaMetadata> episodes = getEpisodeList(options);
      for (MediaMetadata episode : episodes) {
        if (episode.getIdAsInt(TMDB) == episodeTmdbId) {
          seasonNr = episode.getSeasonNumber();
          episodeNr = episode.getEpisodeNumber();
          break;
        }
      }
    }

    // does not work - we cannot scrape w/o season
    if (seasonNr == -1 || episodeNr == -1) {
      LOGGER.warn("season number/episode number found");
      throw new MissingIdException(MediaMetadata.SEASON_NR, MediaMetadata.EPISODE_NR);
    }

    // get the data from tmdb
    TvEpisode episode = null;
    TvSeason fullSeason = null;
    // get episode via season listing -> improves caching performance
    try {
      Response<TvSeason> seasonResponse = api.tvSeasonsService().season(showTmdbId, seasonNr, null, null).execute();
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
    }
    catch (Exception e) {
      LOGGER.debug("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (episode == null) {
      throw new NothingFoundException();
    }

    if (episode.vote_average != null && episode.vote_count != null) {
      try {
        MediaRating rating = new MediaRating("tmdb");
        rating.setRating(MetadataUtil.unboxDouble(episode.vote_average));
        rating.setVotes(MetadataUtil.unboxInteger(episode.vote_count));
        rating.setMaxValue(10);
        return Collections.singletonList(rating);

      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }
    }

    return Collections.emptyList();
  }

  @Override
  public Map<String, Object> getMediaIds(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    if (mediaType != MediaType.TV_SHOW) {
      return Collections.emptyMap();
    }

    LOGGER.debug("getMediaIds(): {}", ids);

    // lazy initialization of the api
    initAPI();

    int tmdbId = MediaIdUtil.getIdAsInt(ids, MediaMetadata.TMDB);
    String imdbId = MediaIdUtil.getIdAsString(ids, MediaMetadata.IMDB);

    if (tmdbId == 0 && !MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId/imdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // TMDB only offers the tmdb id and the imdb id
    Map<String, Object> scrapedIds = new HashMap<>();

    if (tmdbId == 0 && MediaIdUtil.isValidImdbId(imdbId)) {
      // we have the imdb id and just need the tmdb id
      try {
        tmdbId = TmdbUtils.getTmdbIdFromImdbId(api, MediaType.TV_SHOW, imdbId);
      }
      catch (Exception e) {
        LOGGER.debug("problem getting tmdbId from imdbId: {}", e.getMessage());
      }
    }

    if (tmdbId == 0) {
      LOGGER.debug("not possible to scrape from TMDB - no tmdbId/imdbId found");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    // scrape
    TvShow tvShow;

    try {
      Response<TvShow> httpResponse = api.tvService().tv(tmdbId, "en", new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS)).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      tvShow = httpResponse.body();
    }
    catch (Exception e) {
      LOGGER.debug("problem getting data from tmdb: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (tvShow == null) {
      throw new NothingFoundException();
    }

    scrapedIds.put(TMDB, tvShow.id);

    // external IDs
    if (tvShow.external_ids != null) {
      if (tvShow.external_ids.tvdb_id != null && tvShow.external_ids.tvdb_id > 0) {
        scrapedIds.put(TVDB, tvShow.external_ids.tvdb_id);
      }
      if (StringUtils.isNotBlank(tvShow.external_ids.imdb_id)) {
        scrapedIds.put(IMDB, tvShow.external_ids.imdb_id);
      }
      if (tvShow.external_ids.tvrage_id != null && tvShow.external_ids.tvrage_id > 0) {
        scrapedIds.put("tvrage", tvShow.external_ids.tvrage_id);
      }
    }

    return scrapedIds;
  }

  private BaseTvEpisode getBaseTvEpisodeByImdbId(String imdbId) throws IOException {
    FindResults findResults = api.findService().find(imdbId, ExternalSource.IMDB_ID, null).execute().body();
    if (findResults != null && ListUtils.isNotEmpty(findResults.tv_episode_results)) {
      return findResults.tv_episode_results.get(0);
    }

    return null;
  }

  private BaseTvEpisode getBaseTvEpisodeByTvdbId(int tvdbId) throws IOException {
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

      // if the search language equals the original language of the movie, there may be no translation
      if (StringUtils.isBlank(val[0]) && language.getLanguage().equals(show.original_language)) {
        val[0] = show.original_name;
      }

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
   * Fallback Language Mechanism - for direct TMDB lookup<br>
   * Title/Overview always gets returned in the original language, if translation has not been found.<br>
   * So we never know exactly what is missing.. so we just inject everything here by hand if a fallback language has been found
   */
  private void injectTranslations(Locale language, TvEpisode episode, int showId) {
    if (Boolean.TRUE.equals(getProviderInfo().getConfig().getValueAsBool("titleFallback"))) {
      Locale fallbackLanguage = Locale.forLanguageTag(getProviderInfo().getConfig().getValue("titleFallbackLanguage"));
      // get in desired localization
      String[] val = getValuesFromTranslation(episode.translations, language);

      // merge empty ones with fallback
      String[] temp = getValuesFromTranslation(episode.translations, fallbackLanguage);
      if (StringUtils.isBlank(val[0])) {
        val[0] = temp[0];
      }
      if (StringUtils.isBlank(val[1])) {
        val[1] = temp[1];
      }

      // finally SET the values
      // if the original episode title from the response starts with "Episode" is might be not translated
      if ((StringUtils.isBlank(episode.name) || isEpisodesNameDefault(episode, episode.episode_number)) && StringUtils.isNotBlank(val[0])) {
        episode.name = val[0];
      }
      if (StringUtils.isNotBlank(val[1])) {
        episode.overview = val[1];
      }
    }

    // parse the original title
    String originalLanguage = getOriginalLanguage(showId);
    if (StringUtils.isNotBlank(originalLanguage)) {
      String[] val = getValuesFromTranslation(episode.translations, Locale.forLanguageTag(originalLanguage));
      if (StringUtils.isNotBlank(val[0])) {
        episode.originalName = val[0];
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

  private MediaMetadata morphTvEpisodeToMediaMetadata(BaseTvEpisode episode, MediaSearchAndScrapeOptions options) {
    MediaMetadata ep = new MediaMetadata(getId());
    ep.setScrapeOptions(options);
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

  private String getOriginalLanguage(int tmdbId) {
    String cache = ORIGINAL_LANGUAGE_CACHE_MAP.get(tmdbId);
    if (StringUtils.isNotBlank(cache)) {
      return cache;
    }

    String originalLanguage = "";
    try {
      Response<TvShow> httpResponse = api.tvService().tv(tmdbId, null).execute();
      if (!httpResponse.isSuccessful()) {
        return "";
      }
      TvShow complete = httpResponse.body();
      if (StringUtils.isNotBlank(complete.original_language)) {
        originalLanguage = complete.original_language;
      }
    }
    catch (Exception e) {
      originalLanguage = "";
    }
    finally {
      ORIGINAL_LANGUAGE_CACHE_MAP.put(tmdbId, originalLanguage);
    }

    return originalLanguage;
  }

  /**
   * Is i1 != i2 (when >0)
   */
  private boolean yearDiffers(int i1, int i2) {
    return i1 > 0 && i2 > 0 && i1 != i2;
  }

}
