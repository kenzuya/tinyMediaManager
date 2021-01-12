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

import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.entities.MediaGenres;
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
import org.tinymediamanager.scraper.theshowdb.entities.Episode;
import org.tinymediamanager.scraper.theshowdb.entities.Episodes;
import org.tinymediamanager.scraper.theshowdb.entities.Show;
import org.tinymediamanager.scraper.theshowdb.entities.Shows;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.ListUtils;

/**
 * the class {@link TheShowDBTvShowMetadataProvider} provides a meta data provider for TV shows
 * 
 * @author Wolfgang Janes
 */
public class TheShowDBTvShowMetadataProvider extends TheShowDBProvider implements ITvShowMetadataProvider {

  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(60, 10);

  @Override
  public boolean isActive() {
    return StringUtils.isNotBlank(getApiKey());
  }

  @Override
  public MediaMetadata getMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException, MissingIdException, NothingFoundException {
    LOGGER.debug("getMetadata() TvShow: {}", options);

    String showId = options.getIdAsString("theshowdb");
    MediaMetadata md = new MediaMetadata(getId());
    Shows shows = null;

    try {
      shows = controller.getShowDetailsByShowId(getApiKey(), showId);
    }
    catch (Exception e) {
      LOGGER.trace("could not get Main TvShow information: {}", e.getMessage());
    }

    if (!shows.getShows().isEmpty()) {

      for (Show show : shows.getShows()) {
        md.setTitle(show.getTitle());
        md.setPlot(show.getPlot());
        md.setRuntime(show.getRuntime());
        md.setId("zap2it", show.getZap2itId());
        md.setId("theshowdb", show.getId());
        md.setId(MediaMetadata.TVDB, show.getTvdbId());
        md.setId(MediaMetadata.IMDB, show.getImdbId());

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
  public MediaMetadata getMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException, MissingIdException, NothingFoundException {

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

    // TODO scrape the rest of the episode data
    // controller.getEpisodeDetailsByEpisodeId()

    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {

    LOGGER.debug("search(): {}", options);

    SortedSet<MediaSearchResult> results = new TreeSet<>();
    Shows searchResult;

    if (StringUtils.isBlank(getApiKey())) {
      LOGGER.error("API Key not found");
      throw new ScrapeException(new HttpException(401, "Unauthorized"));
    }

    if (!options.getLanguage().getLanguage().equalsIgnoreCase("EN")) {
      LOGGER.info("Scraper only supports Language EN");
      return results;
    }

    LOGGER.info("========= BEGIN TheShowDB Scraper Search for TvShow: {} ", options.getSearchQuery());

    try {
      searchResult = controller.getShowByName(getApiKey(), options.getSearchQuery());
    }
    catch (Exception e) {
      LOGGER.error("error searching: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResult == null) {
      LOGGER.warn("no result from TheShowDB.com");
      return results;
    }

    for (Show show : searchResult.getShows()) {

      MediaSearchResult result = new MediaSearchResult(getProviderInfo().getId(), MediaType.TV_SHOW);
      result.setTitle(show.getTitle());

      result.setId("tvdb", show.getTvdbId());
      result.setId("tvcom", show.getTvComId());
      result.setId("imdb", show.getImdbId());
      result.setId("zap2it", show.getZap2itId());
      result.setId("theshowdb", show.getId());
      result.setProviderId(getProviderInfo().getId());
      result.setPosterUrl(show.getPosterUrl());
      result.setOverview(show.getPlot());
      result.setYear(show.getAiredYear());

      results.add(result);
    }

    return results;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    LOGGER.debug("getEpisodeList(): {}", options);

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

    Episodes episodes = null;
    try {
      episodes = controller.getAllEpisodesByShowId(getApiKey(), tvShowId);
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
