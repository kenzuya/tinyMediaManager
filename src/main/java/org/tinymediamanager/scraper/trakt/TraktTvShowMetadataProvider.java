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
package org.tinymediamanager.scraper.trakt;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TVDB;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.RatingUtil;
import org.tinymediamanager.scraper.util.TvUtils;

import com.uwetrottmann.trakt5.entities.CastMember;
import com.uwetrottmann.trakt5.entities.Credits;
import com.uwetrottmann.trakt5.entities.CrewMember;
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.entities.Season;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.Translation;
import com.uwetrottmann.trakt5.enums.Extended;

import retrofit2.Response;

/**
 * The class TraktTvShowMetadataProvider is used to provide metadata for movies from trakt.tv
 */

public class TraktTvShowMetadataProvider extends TraktMetadataProvider implements ITvShowMetadataProvider, ITvShowImdbMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraktTvShowMetadataProvider.class);

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
    LOGGER.debug("search() - {}", options);

    // lazy initialization of the api
    initAPI();

    String searchString = "";
    if (StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = options.getSearchQuery();
    }

    SortedSet<MediaSearchResult> results = new TreeSet<>();
    List<SearchResult> searchResults = null;

    // pass NO language here since trakt.tv returns less results when passing a language :(
    try {
      Response<List<SearchResult>> response = api.search()
          .textQueryShow(searchString, null, null, null, null, null, null, null, null, null, Extended.FULL, 1, 25)
          .execute();
      if (!response.isSuccessful()) {
        LOGGER.warn("request was NOT successful: HTTP/{} - {}", response.code(), response.message());
        throw new HttpException(response.code(), response.message());
      }
      searchResults = response.body();

    }
    catch (Exception e) {
      LOGGER.debug("failed to search: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResults == null || searchResults.isEmpty()) {
      LOGGER.info("nothing found");
      return results;
    }

    for (SearchResult result : searchResults) {
      MediaSearchResult m = TraktUtils.morphTraktResultToTmmResult(options, result);
      results.add(m);
    }

    return results;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options);

    // lazy initialization of the api
    initAPI();

    List<MediaMetadata> episodes = new ArrayList<>();

    String id = options.getIdAsString(getId());
    if (StringUtils.isBlank(id)) {
      // alternatively we can take the imdbid
      id = options.getIdAsString(IMDB);
    }
    if (StringUtils.isBlank(id)) {
      LOGGER.warn("no id available");
      throw new MissingIdException(IMDB, getId());
    }

    // the API does not provide a complete access to all episodes, so we have to
    // fetch the show summary first and every season afterwards..
    List<Season> seasons = null;
    synchronized (api) {
      try {
        Response<List<Season>> response = api.seasons().summary(id, Extended.FULLEPISODES).execute();
        if (!response.isSuccessful()) {
          LOGGER.warn("request was NOT successful: HTTP/{} - {}", response.code(), response.message());
          throw new HttpException(response.code(), response.message());
        }
        seasons = response.body();
      }
      catch (Exception e) {
        LOGGER.debug("failed to get episode list: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    for (Season season : ListUtils.nullSafe(seasons)) {
      for (Episode episode : ListUtils.nullSafe(season.episodes)) {
        MediaMetadata ep = new MediaMetadata(getId());
        ep.setEpisodeNumber(TvUtils.getEpisodeNumber(episode.number));
        ep.setSeasonNumber(TvUtils.getSeasonNumber(episode.season));
        ep.setTitle(episode.title);

        if (episode.rating != null) {
          try {
            MediaRating rating = new MediaRating(getId());
            rating.setRating(Math.round(episode.rating * 10.0) / 10.0); // hack to round to 1 decimal
            rating.setVotes(episode.votes);
            rating.setMaxValue(10);
            ep.addRating(rating);
          }
          catch (Exception e) {
            LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
          }
        }

        if (episode.first_aired != null) {
          ep.setReleaseDate(TraktUtils.toDate(episode.first_aired));
        }

        if (episode.ids != null) {
          ep.setId(getId(), episode.ids.trakt);
          if (episode.ids.tvdb != null && episode.ids.tvdb > 0) {
            ep.setId(TVDB, episode.ids.tvdb);
          }
          if (episode.ids.tmdb != null && episode.ids.tmdb > 0) {
            ep.setId(TMDB, episode.ids.tmdb);
          }
          if (episode.ids.tvrage != null && episode.ids.tvrage > 0) {
            ep.setId("tvrage", episode.ids.tvrage);
          }
          if (StringUtils.isNotBlank(episode.ids.imdb)) {
            ep.setId(IMDB, episode.ids.imdb);
          }
        }

        episodes.add(ep);
      }
    }

    return episodes;
  }

  @Override
  public MediaMetadata getMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    // lazy initialization of the api
    initAPI();

    MediaMetadata md = new MediaMetadata(getId());

    String id = options.getIdAsString(getId());

    // alternatively we can take the imdbid
    if (StringUtils.isBlank(id)) {
      id = options.getIdAsString(IMDB);
    }

    if (StringUtils.isBlank(id)) {
      LOGGER.warn("no id available");
      throw new MissingIdException(IMDB, getId());
    }

    String lang = options.getLanguage().getLanguage();
    List<Translation> translations = null;
    Show show = null;
    Credits credits = null;
    synchronized (api) {
      try {
        Response<Show> response = api.shows().summary(id, Extended.FULL).execute();
        if (!response.isSuccessful()) {
          LOGGER.warn("request was NOT successful: HTTP/{} - {}", response.code(), response.message());
          throw new HttpException(response.code(), response.message());
        }
        show = response.body();
        if (!"en".equals(lang)) {
          // only call translation when we're not already EN ;)
          translations = api.shows().translation(id, lang).execute().body();
        }
        credits = api.shows().people(id).execute().body();
      }
      catch (Exception e) {
        LOGGER.debug("failed to get meta data: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    if (show == null) {
      LOGGER.warn("nothing found");
      throw new NothingFoundException();
    }

    // show meta data
    if (show.ids != null) {
      md.setId(getId(), show.ids.trakt);
      if (show.ids.tvdb != null && show.ids.tvdb > 0) {
        md.setId(TVDB, show.ids.tvdb);
      }
      if (show.ids.tmdb != null && show.ids.tmdb > 0) {
        md.setId(TMDB, show.ids.tmdb);
      }
      if (show.ids.tvrage != null && show.ids.tvrage > 0) {
        md.setId("tvrage", show.ids.tvrage);
      }
      if (StringUtils.isNotBlank(show.ids.imdb)) {
        md.setId(IMDB, show.ids.imdb);
      }
    }

    // if foreign language, get new values and overwrite
    Translation trans = translations == null || translations.isEmpty() ? null : translations.get(0);
    if (trans != null) {
      md.setTitle(StringUtils.isBlank(trans.title) ? show.title : trans.title);
      md.setPlot(StringUtils.isBlank(trans.overview) ? show.overview : trans.overview);
    }
    else {
      md.setTitle(show.title);
      md.setPlot(show.overview);
    }

    md.setYear(show.year);

    try {
      MediaRating rating = new MediaRating("trakt");
      rating.setRating(show.rating);
      rating.setVotes(show.votes);
      rating.setMaxValue(10);
      md.addRating(rating);
    }
    catch (Exception e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    md.addCertification(MediaCertification.findCertification(show.certification));
    md.addCountry(show.country);
    md.setReleaseDate(TraktUtils.toDate(show.first_aired));
    if (show.status != null) {
      md.setStatus(show.status.toString());
    }
    md.setRuntime(show.runtime);
    md.addProductionCompany(show.network);

    for (String genreAsString : ListUtils.nullSafe(show.genres)) {
      md.addGenre(MediaGenres.getGenre(genreAsString));
    }

    // cast&crew
    if (credits != null) {
      for (CastMember cast : ListUtils.nullSafe(credits.cast)) {
        md.addCastMember(TraktUtils.toTmmCast(cast, ACTOR));
      }
      if (credits.crew != null) {
        for (CrewMember crew : ListUtils.nullSafe(credits.crew.directing)) {
          md.addCastMember(TraktUtils.toTmmCast(crew, DIRECTOR));
        }
        for (CrewMember crew : ListUtils.nullSafe(credits.crew.production)) {
          md.addCastMember(TraktUtils.toTmmCast(crew, PRODUCER));
        }
        for (CrewMember crew : ListUtils.nullSafe(credits.crew.writing)) {
          md.addCastMember(TraktUtils.toTmmCast(crew, WRITER));
        }
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

    // ok, we have 2 flavors here:
    // a) we get the season and episode number -> everything is fine
    // b) we get the episode id (tmdb, imdb or tvdb) -> we need to do the lookup to get the season/episode number

    // get the tv show ids
    TvShowSearchAndScrapeOptions tvShowSearchAndScrapeOptions = options.createTvShowSearchAndScrapeOptions();
    String showId = tvShowSearchAndScrapeOptions.getIdAsString(getId());

    if (StringUtils.isBlank(showId)) {
      // alternatively we can take the imdbid
      showId = tvShowSearchAndScrapeOptions.getIdAsString(IMDB);
    }
    if (StringUtils.isBlank(showId)) {
      LOGGER.warn("no id available");
      throw new MissingIdException(IMDB, getId());
    }

    String episodeImdbId = options.getIdAsString(IMDB);
    if (!MetadataUtil.isValidImdbId(episodeImdbId)) {
      episodeImdbId = "";
    }
    int episodeTmdbId = options.getIdAsIntOrDefault(TMDB, 0);
    int episodeTvdbId = options.getIdAsIntOrDefault(TVDB, 0);
    int episodeTraktId = options.getIdAsIntOrDefault(getId(), 0);

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // parsed valid episode number/season number?
    String aired = "";
    if (options.getMetadata() != null && options.getMetadata().getReleaseDate() != null) {
      Format formatter = new SimpleDateFormat("yyyy-MM-dd");
      aired = formatter.format(options.getMetadata().getReleaseDate());
    }
    if (aired.isEmpty() && (seasonNr == -1 || episodeNr == -1) && StringUtils.isBlank(episodeImdbId) && episodeTmdbId == 0 && episodeTvdbId == 0
        && episodeTraktId == 0) {
      throw new MissingIdException(MediaMetadata.SEASON_NR, MediaMetadata.EPISODE_NR); // not even date set? return
    }

    // fetch all episode data - this results in less connections, but the initial connection is _bigger_
    Episode episode = null;
    List<Season> seasons = null;
    synchronized (api) {
      try {
        Response<List<Season>> response = api.seasons().summary(showId, Extended.FULLEPISODES).execute();
        if (!response.isSuccessful()) {
          LOGGER.warn("request was NOT successful: HTTP/{} - {}", response.code(), response.message());
          throw new HttpException(response.code(), response.message());
        }
        seasons = response.body();
      }
      catch (Exception e) {
        LOGGER.debug("failed to get meta data: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    for (Season season : ListUtils.nullSafe(seasons)) {
      for (Episode ep : ListUtils.nullSafe(season.episodes)) {
        if (ep.ids != null) {
          // possible match by external id
          if (StringUtils.isNotBlank(episodeImdbId) && episodeImdbId.equals(ep.ids.imdb)) {
            episode = ep;
            break;
          }
          else if (episodeTraktId != 0 && episodeTraktId == MetadataUtil.unboxInteger(ep.ids.trakt)) {
            episode = ep;
            break;
          }
          else if (episodeTmdbId != 0 && episodeTmdbId == MetadataUtil.unboxInteger(ep.ids.tmdb)) {
            episode = ep;
            break;
          }
          else if (episodeTvdbId != 0 && episodeTvdbId == MetadataUtil.unboxInteger(ep.ids.tvdb)) {
            episode = ep;
            break;
          }
        }

        if (MetadataUtil.unboxInteger(ep.season, -1) == seasonNr && MetadataUtil.unboxInteger(ep.number, -1) == episodeNr) {
          episode = ep;
          break;
        }
      }
    }

    // not found? try to match by date
    if (episode == null && !aired.isEmpty()) {
      for (Season season : ListUtils.nullSafe(seasons)) {
        for (Episode ep : ListUtils.nullSafe(season.episodes)) {
          if (ep.first_aired != null) {
            Format formatter = new SimpleDateFormat("yyyy-MM-dd");
            String epAired = formatter.format(TraktUtils.toDate(ep.first_aired));
            if (epAired.equals(aired)) {
              episode = ep;
              break;
            }
          }
        }
      }
    }

    if (episode == null) {
      LOGGER.warn("nothing found");
      throw new NothingFoundException();
    }

    md.setEpisodeNumber(TvUtils.getEpisodeNumber(episode.number));
    md.setAbsoluteNumber(TvUtils.getEpisodeNumber(episode.number_abs));
    md.setSeasonNumber(TvUtils.getSeasonNumber(episode.season));

    if (episode.ids != null) {
      md.setId(getId(), episode.ids.trakt);
      if (episode.ids.tvdb != null && episode.ids.tvdb > 0) {
        md.setId(TVDB, episode.ids.tvdb);
      }
      if (episode.ids.tmdb != null && episode.ids.tmdb > 0) {
        md.setId(TMDB, episode.ids.tmdb);
      }
      if (MetadataUtil.isValidImdbId(episode.ids.imdb)) {
        md.setId(IMDB, episode.ids.imdb);
      }
      if (episode.ids.tvrage != null && episode.ids.tvrage > 0) {
        md.setId("tvrage", episode.ids.tvrage);
      }
    }

    md.setTitle(episode.title);
    md.setPlot(episode.overview);

    try {
      MediaRating rating = new MediaRating("trakt");
      rating.setRating(MetadataUtil.unboxDouble(episode.rating));
      rating.setVotes(MetadataUtil.unboxInteger(episode.votes));
      rating.setMaxValue(10);
      md.addRating(rating);
    }
    catch (Exception e) {
      LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
    }

    md.setReleaseDate(TraktUtils.toDate(episode.first_aired));

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
