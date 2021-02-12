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
package org.tinymediamanager.scraper.theshowdb;

import static org.tinymediamanager.core.TmmDateFormat.LOGGER;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.theshowdb.entities.Episode;
import org.tinymediamanager.scraper.theshowdb.entities.Episodes;
import org.tinymediamanager.scraper.theshowdb.entities.Show;
import org.tinymediamanager.scraper.theshowdb.entities.Shows;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * the class {@link TheShowDBTvShowMetadataProvider} provides a meta data provider for TV shows
 * 
 * @author Wolfgang Janes
 */
public class TheShowDBTvShowMetadataProvider extends TheShowDBProvider implements ITvShowMetadataProvider {

  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(60, 10);

  @Override
  public MediaMetadata getMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() TvShow: {}", options);

    initAPI();

    String showId = options.getIdAsString("theshowdb");
    MediaMetadata md = new MediaMetadata(getId());
    Shows shows = null;

    if (StringUtils.isBlank(showId)) {
      throw new MissingIdException("theshowdb");
    }

    try {
      shows = controller.getShowDetailsByShowId(showId);
    }
    catch (InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (IOException e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (shows == null) {
      throw new NothingFoundException();
    }

    if (!shows.getShows().isEmpty()) {

      for (Show show : shows.getShows()) {
        md.setTitle(show.getTitle());
        md.setPlot(show.getPlot());
        md.setRuntime(show.getRuntime());
        md.setId("zap2it", show.getZap2itId());
        md.setId(getId(), show.getId());

        if (MetadataUtil.parseInt(show.getTvdbId(), 0) > 0) {
          md.setId(MediaMetadata.TVDB, MetadataUtil.parseInt(show.getTvdbId(), 0));
        }

        if (MetadataUtil.isValidImdbId(show.getImdbId())) {
          md.setId(MediaMetadata.IMDB, show.getImdbId());
        }

        if (MetadataUtil.parseInt(show.getTmdbId(), 0) > 0) {
          md.setId(MediaMetadata.TMDB, MetadataUtil.parseInt(show.getTmdbId(), 0));
        }

        if (MetadataUtil.parseInt(show.getTraktId(), 0) > 0) {
          md.setId(MediaMetadata.TRAKT_TV, MetadataUtil.parseInt(show.getTraktId(), 0));
        }

        // Genre
        for (String genre : show.getGenre().split("\\|")) {
          if (!genre.isBlank()) {
            MediaGenres mg = MediaGenres.getGenre(genre);
            md.addGenre(mg);
          }
        }

        // Certification
        MediaCertification mc = MediaCertification.findCertification(show.getCertification());
        md.addCertification(mc);

        md.setReleaseDate(show.getAired());
        md.setYear(show.getAiredYear());

        // Artwork
        MediaArtwork poster = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.SEASON_POSTER);
        poster.setDefaultUrl(show.getPosterUrl());
        md.addMediaArt(poster);

        MediaArtwork banner = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.SEASON_BANNER);
        banner.setDefaultUrl(show.getPosterUrl());
        md.addMediaArt(banner);

        MediaArtwork thumb = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.SEASON_THUMB);
        thumb.setDefaultUrl(show.getPosterUrl());
        md.addMediaArt(thumb);
      }
    }

    return md;
  }

  @Override
  public MediaMetadata getMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() Episode: {}", options);

    initAPI();

    MediaMetadata md = new MediaMetadata(getId());

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // first get the base episode metadata which can be gathered via getEpisodeList()
    List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

    MediaMetadata wantedEpisode = null;
    for (MediaMetadata metadata : episodes) {
      if (seasonNr == metadata.getSeasonNumber() && episodeNr == metadata.getEpisodeNumber()) {
        wantedEpisode = metadata;
        break;
      }
    }

    // we did not find the episode; return
    if (wantedEpisode == null) {
      LOGGER.warn("episode not found");
      throw new NothingFoundException();
    }

    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    initAPI();

    SortedSet<MediaSearchResult> results = new TreeSet<>();
    Shows searchResult;

    try {
      searchResult = controller.getShowByName(options.getSearchQuery());
    }
    catch (Exception e) {
      LOGGER.error("error searching: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResult == null) {
      LOGGER.debug("no result from TheShowDB.com");
      return results;
    }

    for (Show show : searchResult.getShows()) {
      MediaSearchResult result = new MediaSearchResult(getProviderInfo().getId(), MediaType.TV_SHOW);
      result.setTitle(show.getTitle());

      if (MetadataUtil.parseInt(show.getTvdbId(), 0) > 0) {
        result.setId(MediaMetadata.TVDB, show.getTvdbId());
      }

      if (MetadataUtil.isValidImdbId(show.getImdbId())) {
        result.setId(MediaMetadata.IMDB, show.getImdbId());
      }

      if (MetadataUtil.parseInt(show.getTmdbId(), 0) > 0) {
        result.setId(MediaMetadata.TMDB, show.getTmdbId());
      }

      if (MetadataUtil.parseInt(show.getTraktId(), 0) > 0) {
        result.setId(MediaMetadata.TRAKT_TV, show.getTraktId());
      }

      result.setId(getId(), show.getId());
      result.setProviderId(getProviderInfo().getId());
      result.setPosterUrl(show.getPosterUrl());
      result.setOverview(show.getPlot());
      result.setYear(show.getAiredYear());

      results.add(result);
    }

    return results;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options);

    initAPI();

    // Get TvShow ID From the options
    String tvShowId = options.getIdAsString("theshowdb");

    if (StringUtils.isBlank(tvShowId)) {
      throw new MissingIdException("theshowdb");
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> episodeList = EPISODE_LIST_CACHE_MAP.get(tvShowId + "_" + options.getLanguage().getLanguage());
    if (ListUtils.isNotEmpty(episodeList)) {
      // cache hit!
      return episodeList;
    }

    episodeList = new ArrayList<>();

    Episodes episodes = null;
    try {
      episodes = controller.getAllEpisodesByShowId(tvShowId);
    }
    catch (InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("error getting episodes: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    if (episodes == null) {
      LOGGER.debug("nothing found");
      return Collections.emptyList();
    }

    for (Episode episode : episodes.getEpisodes()) {

      MediaMetadata md = new MediaMetadata(getProviderInfo().getId());
      md.setEpisodeNumber(episode.getEpisodeNumber());
      md.setSeasonNumber(episode.getSeasonNumber());
      md.setPlot(episode.getEpisodePlot());
      md.setTitle(episode.getEpisodeTitle());
      md.setReleaseDate(episode.getFirstAired());
      md.setAbsoluteNumber(episode.getAbsoluteNumber());
      md.setDvdSeasonNumber(episode.getSeasonNumber());
      md.setDvdEpisodeNumber(episode.getDvdEpisodeNumber());

      // Thumb
      if (episode.getThumbUrl() != null && episode.getThumbUrl().isBlank()) {
        MediaArtwork artwork = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.THUMB);
        artwork.setDefaultUrl(episode.getThumbUrl());
        md.addMediaArt(artwork);
      }

      // Id's
      md.setId("theshowdb", episode.getEpisodeId());
      md.setId(MediaMetadata.TVDB, episode.getTvdbEpisodeId());
      md.setId(MediaMetadata.IMDB, episode.getImdbId());

      // Guest Stars
      for (String guestStars : episode.getGuestStars()) {
        Person person = new Person(Person.Type.ACTOR);
        person.setName(guestStars);
        md.addCastMember(person);
      }
      // Director
      for (String director : episode.getDirectorName()) {
        Person person = new Person(Person.Type.DIRECTOR);
        person.setName(director);
        md.addCastMember(person);
      }

      // Writer
      for (String writer : episode.getWriterName()) {
        Person person = new Person(Person.Type.WRITER);
        person.setName(writer);
        md.addCastMember(person);
      }

      episodeList.add(md);
    }

    // cache for further fast access
    if (!episodeList.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(tvShowId + "_" + options.getLanguage().getLanguage(), episodeList);
    }

    return episodeList;
  }

  @Override
  protected String getSubId() {
    return "tvshow";
  }
}
