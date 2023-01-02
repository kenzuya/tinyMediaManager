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
package org.tinymediamanager.scraper.tvdbv3;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.MediaMetadata.TVDB;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroup.ABSOLUTE;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroup.AIRED;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroup.DISPLAY;
import static org.tinymediamanager.scraper.entities.MediaEpisodeGroup.EpisodeGroup.DVD;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTvdbMetadataProvider;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.TvUtils;

import com.uwetrottmann.thetvdb.entities.Actor;
import com.uwetrottmann.thetvdb.entities.ActorsResponse;
import com.uwetrottmann.thetvdb.entities.Episode;
import com.uwetrottmann.thetvdb.entities.EpisodesResponse;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResultsResponse;

import retrofit2.Response;

/**
 * the class {@link TvdbV3TvShowMetadataProvider} offers the meta data provider for TheTvDb - legacy v3 API!!
 *
 * @author Manuel Laggner
 */
public class TvdbV3TvShowMetadataProvider extends TvdbV3MetadataProvider implements ITvShowMetadataProvider, ITvShowTvdbMetadataProvider {
  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(TvdbV3TvShowMetadataProvider.class);

  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(600, 5);

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
    return "tvshow";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // do we have an id from the options?
    Integer id = options.getIdAsInteger(MediaMetadata.TVDB);
    if (id == null || id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(MediaMetadata.TVDB);
    }

    String language = options.getLanguage().getLanguage();
    String fallbackLanguage = MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE)).getLanguage(); // just 2 char

    Series show = null;
    try {
      Response<SeriesResponse> httpResponse = tvdb.series().series(id, options.getLanguage().getLanguage()).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      show = httpResponse.body().data;
      fillFallbackLanguages(language, fallbackLanguage, show);
    }
    catch (Exception e) {
      LOGGER.error("failed to get meta data: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (show == null) {
      throw new NothingFoundException();
    }

    // populate metadata
    md.setId(TVDB, show.id);
    md.setTitle(show.seriesName);
    if (MediaIdUtil.isValidImdbId(show.imdbId)) {
      md.setId(MediaMetadata.IMDB, show.imdbId);
    }
    if (StringUtils.isNotBlank(show.zap2itId)) {
      md.setId("zap2it", show.zap2itId);
    }
    md.setPlot(show.overview);

    try {
      md.setRuntime(Integer.parseInt(show.runtime));
    }
    catch (Exception e) {
      LOGGER.trace("could not parse runtime: {}", e.getMessage());
      md.setRuntime(0);
    }

    try {
      MediaRating rating = new MediaRating(MediaMetadata.TVDB);
      rating.setRating(show.siteRating.floatValue());
      rating.setVotes(TvUtils.parseInt(show.siteRatingCount));
      rating.setMaxValue(10);
      md.addRating(rating);
    }
    catch (Exception e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    try {
      md.setReleaseDate(StrgUtils.parseDate(show.firstAired));
    }
    catch (ParseException e) {
      LOGGER.debug("could not parse date: {}", e.getMessage());
    }

    try {
      Date date = StrgUtils.parseDate(show.firstAired);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      int y = calendar.get(Calendar.YEAR);
      md.setYear(y);
      if (y != 0 && md.getTitle().contains(String.valueOf(y))) {
        LOGGER.debug("Weird TVDB entry - removing date {} from title", y);
        md.setTitle(clearYearFromTitle(md.getTitle(), y));
      }
    }
    catch (Exception e) {
      LOGGER.debug("could not parse date: {}", e.getMessage());
    }

    md.setStatus(show.status);
    md.addProductionCompany(show.network);

    List<Actor> actors = new ArrayList<>();
    try {
      Response<ActorsResponse> httpResponse = tvdb.series().actors(id).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      actors.addAll(httpResponse.body().data);
    }
    catch (Exception e) {
      LOGGER.error("failed to get actors: {}", e.getMessage());
    }

    for (Actor actor : actors) {
      Person member = new Person(ACTOR);
      member.setId(MediaMetadata.TVDB, actor.id);
      member.setName(actor.name);
      member.setRole(actor.role);
      if (StringUtils.isNotBlank(actor.image)) {
        member.setThumbUrl(ARTWORK_URL + actor.image);
      }

      md.addCastMember(member);
    }

    md.addCertification(MediaCertification.findCertification(show.rating));

    // genres
    for (String genreAsString : ListUtils.nullSafe(show.genre)) {
      md.addGenre(MediaGenres.getGenre(genreAsString));
    }

    return md;
  }

  @Override
  public MediaMetadata getMetadata(@NotNull TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    // do we have an id from the options?
    int showId = options.createTvShowSearchAndScrapeOptions().getIdAsIntOrDefault(MediaMetadata.TVDB, 0);

    if (showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(MediaMetadata.TVDB);
    }

    int episodeTvdbId = options.getIdAsIntOrDefault(TVDB, 0);

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    if (seasonNr == -1 || episodeNr == -1) {
      seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR_DVD, -1);
      episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR_DVD, -1);
    }

    Date releaseDate = null;
    if (options.getMetadata() != null && options.getMetadata().getReleaseDate() != null) {
      releaseDate = options.getMetadata().getReleaseDate();
    }
    if (releaseDate == null && (seasonNr == -1 || episodeNr == -1) && episodeTvdbId == 0) {
      LOGGER.warn("no aired date/season number/episode number found");
      throw new MissingIdException(MediaMetadata.EPISODE_NR, MediaMetadata.SEASON_NR);
    }

    // get the episode via the episodesList() (is cached and contains all data with 1 call per 100 eps)
    List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

    // now search for the right episode in this list
    MediaMetadata foundEpisode = null;
    // first run - search with EP number
    for (MediaMetadata episode : episodes) {
      if (episodeTvdbId == (Integer) episode.getId(TVDB)) {
        foundEpisode = episode;
        break;
      }

      MediaEpisodeNumber episodeNumber = episode.getEpisodeNumber(AIRED);
      if (episodeNumber == null) {
        continue;
      }

      if (seasonNr == episodeNumber.season() && episodeNr == episodeNumber.episode()) {
        foundEpisode = episode;
        break;
      }
    }
    if (foundEpisode == null && releaseDate != null) {
      // we did not find the episode via season/episode number - search via release date
      for (MediaMetadata episode : episodes) {
        if (episode.getReleaseDate() == releaseDate) {
          foundEpisode = episode;
          break;
        }
      }
    }

    if (foundEpisode == null) {
      throw new NothingFoundException();
    }

    return foundEpisode;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("search() {}", options);
    SortedSet<MediaSearchResult> results = new TreeSet<>();

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = options.getSearchQuery();
    }

    String imdbId = options.getImdbId().isEmpty() ? null : options.getImdbId(); // do not submit empty string!
    if (MediaIdUtil.isValidImdbId(searchString)) {
      imdbId = searchString; // search via IMDBid only
      searchString = null; // by setting empty searchterm
    }
    if (MediaIdUtil.isValidImdbId(imdbId)) {
      searchString = null; // null-out search string, when searching with IMDB, else 405
    }

    int tvdbId = options.getIdAsInt(MediaMetadata.TVDB);
    String language = options.getLanguage().getLanguage();
    String fallbackLanguage = MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE)).getLanguage(); // just 2 char

    List<Series> series = new ArrayList<>();

    // FALLBACK:
    // Accept-Language:
    // Records are returned with the Episode name and Overview in the desired language, if it exists.
    // If there is no translation for the given language, then the record is still returned but with empty values for the translated fields.

    // if we have an TVDB id, use that!
    if (tvdbId != 0) {
      LOGGER.debug("found TvDb ID {} - getting direct", tvdbId);
      try {
        // check with submitted language
        Response<SeriesResponse> httpResponse = tvdb.series().series(tvdbId, language).execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        Series res = httpResponse.body().data;
        fillFallbackLanguages(language, fallbackLanguage, res);
        series.add(res);
      }
      catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb via ID: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    // only search when we did not find something by ID (and search string or IMDB is present)
    if (series.isEmpty() && !(StringUtils.isEmpty(searchString) && StringUtils.isEmpty(imdbId))) {
      try {
        Response<SeriesResultsResponse> httpResponse = tvdb.search().series(searchString, imdbId, null, null, language).execute();
        if (!httpResponse.isSuccessful()) {
          // when not found in language -> 404
          if (!fallbackLanguage.equals(language)) {
            LOGGER.debug("not found - trying with fallback language {}", fallbackLanguage);
            httpResponse = tvdb.search().series(searchString, imdbId, null, null, fallbackLanguage).execute();
          }
          if (!httpResponse.isSuccessful()) {
            if (!fallbackLanguage.equals("en") && !language.equals("en")) {
              LOGGER.debug("not found - trying with EN language");
              httpResponse = tvdb.search().series(searchString, imdbId, null, null, "en").execute();
            }
            if (!httpResponse.isSuccessful()) {
              throw new HttpException(httpResponse.code(), httpResponse.message());
            }
          }
        }
        List<Series> res = httpResponse.body().data;
        for (Series s : res) {
          fillFallbackLanguages(language, fallbackLanguage, s);
        }
        series.addAll(res);
      }
      catch (Exception e) {
        LOGGER.error("problem getting data vom tvdb: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    if (series.isEmpty()) {
      return results;
    }

    // make sure there are no duplicates (e.g. if a show has been found in both languages)
    Map<Integer, MediaSearchResult> resultMap = new HashMap<>();

    for (Series show : series) {
      // check if that show has already a result
      if (resultMap.containsKey(show.id)) {
        continue;
      }

      // build up a new result
      MediaSearchResult result = new MediaSearchResult(MediaMetadata.TVDB, options.getMediaType());
      result.setId(show.id.toString());
      result.setTitle(show.seriesName);
      result.setOverview(show.overview);
      try {
        int year = Integer.parseInt(show.firstAired.substring(0, 4));
        result.setYear(year);
        if (year != 0 && result.getTitle().contains(String.valueOf(year))) {
          LOGGER.debug("Weird TVDB entry - removing date {} from title", year);
          result.setTitle(clearYearFromTitle(result.getTitle(), year));
        }
      }
      catch (Exception ignored) {
        // ignore
      }

      if (StringUtils.isNotBlank(show.poster)) {
        // sometimes the API results the artwork path with /banner/, sometimes without !?
        result.setPosterUrl(ARTWORK_URL + show.poster.replace("/banners/", ""));
      }

      // calculate score
      result.calculateScore(options);
      resultMap.put(show.id, result);
    }

    // and convert all entries from the map to a list
    results.addAll(resultMap.values());

    return results;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options);

    // lazy initialization of the api
    initAPI();

    // do we have an show id from the options?
    Integer showId = options.getIdAsInteger(MediaMetadata.TVDB);
    if (showId == null || showId == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(MediaMetadata.TVDB);
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> episodes = EPISODE_LIST_CACHE_MAP.get(showId + "_" + options.getLanguage().getLanguage());
    if (ListUtils.isNotEmpty(episodes)) {
      // cache hit!
      return episodes;
    }

    episodes = new ArrayList<>();

    List<Episode> eps = new ArrayList<>();
    try {
      String language = options.getLanguage().getLanguage();
      String fallbackLanguage = MediaLanguages.get(getProviderInfo().getConfig().getValue(FALLBACK_LANGUAGE)).getLanguage();

      // 100 results per page
      int counter = 1;
      while (true) {
        Response<EpisodesResponse> httpResponse = tvdb.series().episodes(showId, counter, language).execute();
        if (!httpResponse.isSuccessful() && counter == 1) {
          // error at the first fetch will result in an exception
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        else if (!httpResponse.isSuccessful() && counter > 1) {
          // we got at least one page with results - maybe the episode count is the same as the pagination count
          break;
        }
        EpisodesResponse response = httpResponse.body();

        // fallback language
        for (Episode ep : response.data) {
          fillFallbackLanguages(language, fallbackLanguage, ep);
        }

        eps.addAll(response.data);
        if (response.data.size() < 100) {
          break;
        }

        counter++;
      }
    }
    catch (Exception e) {
      LOGGER.error("failed to get episode list: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    for (Episode ep : eps) {
      MediaMetadata episode = new MediaMetadata(MediaMetadata.TVDB);
      episode.setScrapeOptions(options);
      episode.setId(MediaMetadata.TVDB, ep.id);
      if (MediaIdUtil.isValidImdbId(ep.imdbId)) {
        episode.setId(MediaMetadata.IMDB, ep.imdbId);
      }
      episode.setEpisodeNumber(AIRED, TvUtils.getSeasonNumber(ep.airedSeason), TvUtils.getEpisodeNumber(ep.airedEpisodeNumber));
      episode.setEpisodeNumber(ABSOLUTE, 1, TvUtils.getEpisodeNumber(ep.absoluteNumber));
      episode.setEpisodeNumber(DVD, TvUtils.getSeasonNumber(ep.dvdSeason), TvUtils.getEpisodeNumber(ep.dvdEpisodeNumber));

      if (MetadataUtil.unboxInteger(ep.airsBeforeSeason, -1) > -1 || MetadataUtil.unboxInteger(ep.airsBeforeEpisode, -1) > -1) {
        episode.setEpisodeNumber(DISPLAY, MetadataUtil.unboxInteger(ep.airsBeforeSeason), MetadataUtil.unboxInteger(ep.airsBeforeEpisode));
      }

      if (MetadataUtil.unboxInteger(ep.airsAfterSeason, -1) > -1) {
        episode.setEpisodeNumber(DISPLAY, MetadataUtil.unboxInteger(ep.airsAfterSeason), 4096); // like emm
      }

      episode.setTitle(ep.episodeName);
      episode.setPlot(ep.overview);

      try {
        episode.setReleaseDate(StrgUtils.parseDate(ep.firstAired));
      }
      catch (Exception ignored) {
        LOGGER.trace("Could not parse date: {}", ep.firstAired);
      }

      try {
        MediaRating rating = new MediaRating(MediaMetadata.TVDB);
        rating.setRating(ep.siteRating.floatValue());
        rating.setVotes(TvUtils.parseInt(ep.siteRatingCount));
        rating.setMaxValue(10);
        episode.addRating(rating);
      }
      catch (Exception e) {
        LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
      }

      // directors
      if (ep.directors != null && !ep.directors.isEmpty()) {
        for (String director : ep.directors) {
          String[] multiple = director.split(",");
          for (String g2 : multiple) {
            Person cm = new Person(DIRECTOR);
            cm.setName(g2.trim());
            episode.addCastMember(cm);
          }
        }
      }

      // writers
      if (ep.writers != null && !ep.writers.isEmpty()) {
        for (String writer : ep.writers) {
          String[] multiple = writer.split(",");
          for (String g2 : multiple) {
            Person cm = new Person(WRITER);
            cm.setName(g2.trim());
            episode.addCastMember(cm);
          }
        }
      }

      // actors (guests?)
      if (ep.guestStars != null && !ep.guestStars.isEmpty()) {
        for (String guest : ep.guestStars) {
          String[] multiple = guest.split(",");
          for (String g2 : multiple) {
            Person cm = new Person(ACTOR);
            cm.setName(g2.trim());
            episode.addCastMember(cm);
          }
        }
      }

      // Thumb
      if (StringUtils.isNotBlank(ep.filename)) {
        MediaArtwork ma = new MediaArtwork(getProviderInfo().getId(), MediaArtwork.MediaArtworkType.THUMB);
        ma.setPreviewUrl(ARTWORK_URL + ep.filename);
        ma.setDefaultUrl(ARTWORK_URL + ep.filename);
        ma.setOriginalUrl(ARTWORK_URL + ep.filename);
        if (StringUtils.isNoneBlank(ep.thumbWidth, ep.thumbHeight)) {
          try {
            ma.addImageSize(Integer.parseInt(ep.thumbWidth), Integer.parseInt(ep.thumbHeight), ARTWORK_URL + ep.filename);
          }
          catch (Exception e) {
            ma.addImageSize(0, 0, ARTWORK_URL + ep.filename);
          }
        }
        episode.addMediaArt(ma);
      }

      episodes.add(episode);
    }

    // cache for further fast access
    if (!episodes.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(showId + "_" + options.getLanguage().getLanguage(), episodes);
    }

    return episodes;
  }
}
