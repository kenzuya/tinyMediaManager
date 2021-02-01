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
package org.tinymediamanager.thirdparty.trakttv;

import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getAudio;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getAudioChannels;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getHdr;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getMediaType;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.getResolution;
import static org.tinymediamanager.thirdparty.trakttv.TraktTv.printStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.BaseMovie;
import com.uwetrottmann.trakt5.entities.Metadata;
import com.uwetrottmann.trakt5.entities.MovieIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.enums.Extended;

import retrofit2.Response;

/**
 * the movie implementation of the Trakt.tv interface
 * 
 * @author Manuel Laggner
 */
class TraktTvMovie {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraktTvMovie.class);

  private final TraktTv       traktTv;
  private final TraktV2       api;

  TraktTvMovie(TraktTv traktTv) {
    this.traktTv = traktTv;
    this.api = traktTv.getApi();
  }

  /**
   * Syncs Trakt.tv collection (specified movies)<br>
   * Gets all Trakt movies from collection, matches them to ours, and sends ONLY the new ones back to Trakt
   */
  void syncTraktMovieCollection(List<Movie> moviesInTmm) {
    // *****************************************************************************
    // 1) get diff of TMM <-> Trakt collection
    // *****************************************************************************
    LOGGER.debug("got up to {} movies for Trakt.tv collection sync", moviesInTmm.size());

    // get ALL Trakt movies in collection
    List<BaseMovie> traktMovies;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      Response<List<BaseMovie>> response = api.sync().collectionMovies(Extended.METADATA).execute();
      if (!response.isSuccessful() && response.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        response = api.sync().collectionMovies(Extended.METADATA).execute();
      }
      if (!response.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
        return;
      }
      traktMovies = response.body();
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} movies in your Trakt.tv collection", traktMovies.size());

    List<Movie> tmmMovies = new ArrayList<>(moviesInTmm);

    // loop over all movies on trakt and match them with the tmm ones
    for (BaseMovie traktMovie : traktMovies) {
      List<Movie> matchingTmmMovies = getTmmMoviesForTraktMovie(tmmMovies, traktMovie.movie);
      boolean metadataFound = false;

      for (Movie tmmMovie : matchingTmmMovies) {
        // update missing IDs (we get them for free :)
        boolean dirty = updateIDs(tmmMovie, traktMovie.movie);

        if (traktMovie.collected_at != null) {
          Date collectedAt = DateTimeUtils.toDate(traktMovie.collected_at.toInstant());
          if (!collectedAt.equals(tmmMovie.getDateAdded()))
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking movie '{}' as collected on {} (was {})", tmmMovie.getTitle(), collectedAt, tmmMovie.getDateAddedAsString());
          tmmMovie.setDateAdded(collectedAt);
          dirty = true;

        }

        if (dirty) {
          tmmMovie.writeNFO();
          tmmMovie.saveToDb();
        }

        // remove it from our list, if we already have at least a video source (so metadata has also been synced)
        if (matchesMetadata(traktMovie.metadata, tmmMovie)) {
          metadataFound = true;
        }

        tmmMovies.remove(tmmMovie);
      }

      if (!metadataFound && !matchingTmmMovies.isEmpty()) {
        // not at least one movie in tmm found with matching metadata -> sync the first found to trakt
        tmmMovies.add(matchingTmmMovies.get(0));
      }
    }

    if (tmmMovies.isEmpty()) {
      LOGGER.debug("Already up-to-date - no need to add anything :)");
      return;
    }

    // *****************************************************************************
    // 2) add remaining TMM movies to Trakt collection
    // *****************************************************************************
    LOGGER.debug("prepare {} movies for Trakt.tv collection sync", tmmMovies.size());

    List<SyncMovie> movies = new ArrayList<>();
    int nosync = 0;
    for (Movie tmmMovie : tmmMovies) {
      if (tmmMovie.getIdAsInt(Constants.TRAKT) > 0 || MetadataUtil.isValidImdbId(tmmMovie.getImdbId()) || tmmMovie.getTmdbId() > 0) {
        movies.add(toSyncMovie(tmmMovie, false));
      }
      else {
        // do not add to Trakt if we do not have at least one ID
        nosync++;
      }
    }
    if (nosync > 0) {
      LOGGER.debug("skipping {} movies, because they have not been scraped yet!", nosync);
    }

    if (movies.isEmpty()) {
      LOGGER.info("no new movies for Trakt.tv collection sync found.");
      return;
    }

    try {
      LOGGER.info("Adding {} movies to Trakt.tv collection", movies.size());
      SyncItems items = new SyncItems().movies(movies);
      Response<SyncResponse> response = api.sync().addItemsToCollection(items).execute();
      if (!response.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
        return;
      }
      LOGGER.debug("Trakt add-to-library status:");
      printStatus(response.body());
    }
    catch (Exception e) {
      LOGGER.error("failed syncing Trakt.tv: {}", e.getMessage());
    }
  }

  /**
   * Syncs Trakt.tv "seen" flag (all gives you have already marked as watched)<br>
   * Gets all watched movies from Trakt, and sets the "watched" flag on TMM movies.<br>
   * Then update the remaining TMM movies on Trakt as 'seen'.
   */
  void syncTraktMovieWatched(List<Movie> moviesInTmm) {
    // create a local copy of the list
    List<Movie> tmmMovies = new ArrayList<>(moviesInTmm);

    // *****************************************************************************
    // 1) get all Trakt watched movies and update our "watched" status
    // *****************************************************************************
    List<BaseMovie> traktMovies;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      Response<List<BaseMovie>> traktWatchedResponse = api.sync().watchedMovies(null).execute();
      if (!traktWatchedResponse.isSuccessful() && traktWatchedResponse.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        traktWatchedResponse = api.sync().watchedMovies(null).execute();
      }
      if (!traktWatchedResponse.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", traktWatchedResponse.code(), traktWatchedResponse.message());
        return;
      }
      traktMovies = traktWatchedResponse.body();
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt.tv: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} movies marked as 'watched' in your Trakt.tv collection", traktMovies.size());

    // loop over all watched movies on trakt
    for (BaseMovie traktWatched : traktMovies) {
      List<Movie> matchingTmmMovies = getTmmMoviesForTraktMovie(tmmMovies, traktWatched.movie);

      for (Movie tmmMovie : matchingTmmMovies) {
        // update missing IDs (we get them for free :)
        boolean dirty = updateIDs(tmmMovie, traktWatched.movie);

        if (!tmmMovie.isWatched()) {
          // save Trakt watched status
          LOGGER.trace("Marking movie '{}' as watched", tmmMovie.getTitle());
          tmmMovie.setWatched(true);
          dirty = true;
        }
        if (tmmMovie.getPlaycount() != traktWatched.plays) {
          tmmMovie.setPlaycount(traktWatched.plays);
          dirty = true;
        }

        if (dirty) {
          tmmMovie.writeNFO();
          tmmMovie.saveToDb();
        }

        if (traktWatched.last_watched_at != null) {
          Date lastWatchedAt = DateTimeUtils.toDate(traktWatched.last_watched_at.toInstant());
          if (!lastWatchedAt.equals(tmmMovie.getLastWatched())) {
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking movie '{}' as watched on {} (was {})", tmmMovie.getTitle(), lastWatchedAt, tmmMovie.getLastWatched());
            tmmMovie.setLastWatched(lastWatchedAt);
            // dirty = true; // we do not write date to NFO. But just mark for syncing back...
          }
        }
      }
    }

    // *****************************************************************************
    // 2) mark additionally "watched" movies as 'seen' on Trakt.tv
    // *****************************************************************************
    // Now get all TMM watched movies...
    List<Movie> tmmWatchedMovies = moviesInTmm.stream().filter(Movie::isWatched).collect(Collectors.toList());
    LOGGER.info("You have now {} movies marked as 'watched' in your TMM database", tmmWatchedMovies.size());

    // ...and subtract the already watched from Trakt
    for (BaseMovie traktWatched : traktMovies) {
      tmmWatchedMovies.removeAll(getTmmMoviesForTraktMovie(tmmMovies, traktWatched.movie));
    }

    if (tmmWatchedMovies.isEmpty()) {
      LOGGER.debug("no new watched movies for Trakt.tv sync found.");
      return;
    }

    LOGGER.debug("prepare {} movies for Trakt.tv sync", tmmWatchedMovies.size());
    List<SyncMovie> movies = new ArrayList<>();
    int nosync = 0;
    for (Movie tmmMovie : tmmWatchedMovies) {
      if (tmmMovie.getIdAsInt(Constants.TRAKT) > 0 || MetadataUtil.isValidImdbId(tmmMovie.getImdbId()) || tmmMovie.getTmdbId() > 0) {
        movies.add(toSyncMovie(tmmMovie, true));
      }
      else {
        // do not add to Trakt if we do not have at least one ID
        nosync++;
      }
    }
    if (nosync > 0) {
      LOGGER.debug("skipping {} movies, because they have not been scraped yet!", nosync);
    }

    if (movies.isEmpty()) {
      LOGGER.debug("no new watched movies for Trakt.tv sync found.");
      return;
    }

    try {
      LOGGER.info("Marking {} movies as 'watched' to Trakt.tv collection", movies.size());
      SyncItems items = new SyncItems().movies(movies);
      Response<SyncResponse> response = api.sync().addItemsToWatchedHistory(items).execute();
      if (!response.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
        return;
      }
      LOGGER.debug("Trakt mark-as-watched status:");
      printStatus(response.body());
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
    }
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  void clearTraktMovies() {
    // *****************************************************************************
    // 1) get ALL Trakt movies in collection / watched
    // *****************************************************************************
    List<BaseMovie> traktCollection;
    List<BaseMovie> traktWatched;
    try {
      // collection
      Response<List<BaseMovie>> traktCollectionResponse = api.sync().collectionMovies(null).execute();
      if (!traktCollectionResponse.isSuccessful() && traktCollectionResponse.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        traktCollectionResponse = api.sync().collectionMovies(null).execute();
      }
      if (!traktCollectionResponse.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", traktCollectionResponse.code(), traktCollectionResponse.message());
        return;
      }
      traktCollection = traktCollectionResponse.body();

      // watched
      Response<List<BaseMovie>> traktWatchedResponse = api.sync().watchedMovies(null).execute();
      if (!traktWatchedResponse.isSuccessful() && traktWatchedResponse.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        traktWatchedResponse = api.sync().watchedMovies(null).execute();
      }
      if (!traktWatchedResponse.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", traktWatchedResponse.code(), traktWatchedResponse.message());
        return;
      }
      traktWatched = traktWatchedResponse.body();
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} movies in your Trakt.tv collection", traktCollection.size());
    LOGGER.info("You have {} movies watched", traktWatched.size());

    // *****************************************************************************
    // 2) remove every movie from the COLLECTION state
    // *****************************************************************************
    List<SyncMovie> moviesToRemove = new ArrayList<>();
    for (BaseMovie traktMovie : traktCollection) {
      moviesToRemove.add(toSyncMovie(traktMovie));
    }

    if (!moviesToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().movies(moviesToRemove);
        Response<SyncResponse> response = api.sync().deleteItemsFromCollection(items).execute();
        if (!response.isSuccessful()) {
          LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
          return;
        }
        LOGGER.info("removed {} movies from your trakt.tv collection", moviesToRemove.size());
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }

    // *****************************************************************************
    // 3) remove every movie from the WATCHED state
    // *****************************************************************************
    moviesToRemove.clear();
    for (BaseMovie traktMovie : traktWatched) {
      moviesToRemove.add(toSyncMovie(traktMovie));
    }

    if (!moviesToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().movies(moviesToRemove);
        Response<SyncResponse> response = api.sync().deleteItemsFromWatchedHistory(items).execute();
        if (!response.isSuccessful()) {
          LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
          return;
        }
        LOGGER.info("removed {} movies from your trakt.tv watched", moviesToRemove.size());
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
      }
    }
  }

  private List<Movie> getTmmMoviesForTraktMovie(List<Movie> tmmMovies, com.uwetrottmann.trakt5.entities.Movie traktMovie) {
    return tmmMovies.stream().filter(movie -> matches(movie, traktMovie)).collect(Collectors.toList());
  }

  private boolean matches(Movie tmmMovie, com.uwetrottmann.trakt5.entities.Movie traktMovie) {
    if (traktMovie == null || traktMovie.ids == null) {
      return false;
    }

    MovieIds ids = traktMovie.ids;

    int traktId = MetadataUtil.unboxInteger(ids.trakt);
    if (traktId > 0 && traktId == tmmMovie.getIdAsInt(Constants.TRAKT)) {
      return true;
    }

    if (StringUtils.isNotEmpty(ids.imdb) && ids.imdb.equals(tmmMovie.getImdbId())) {
      return true;
    }

    int tmdbId = MetadataUtil.unboxInteger(ids.tmdb);
    if (tmdbId > 0 && tmdbId == tmmMovie.getTmdbId()) {
      return true;
    }

    return false;
  }

  private static boolean matchesMetadata(Metadata metadata, Movie movie) {
    if (metadata == null) {
      return false;
    }

    if (metadata.is3d == null || metadata.is3d != movie.isVideoIn3D()) {
      return false;
    }

    if (metadata.audio != getAudio(movie.getMediaInfoAudioCodec())) {
      return false;
    }

    if (metadata.media_type != getMediaType(movie.getMediaInfoSource())) {
      return false;
    }

    if (metadata.resolution != getResolution(movie.getMainVideoFile())) {
      return false;
    }
    if (metadata.audio_channels == getAudioChannels(movie.getMainVideoFile().getAudioChannelCount())) {
      return false;
    }

    if (metadata.hdr != getHdr(movie.getVideoHDRFormat())) {
      return false;
    }

    return true;
  }

  private boolean updateIDs(Movie tmmMovie, com.uwetrottmann.trakt5.entities.Movie traktMovie) {
    boolean dirty = false;
    if (traktMovie == null || traktMovie.ids == null) {
      return dirty;
    }

    MovieIds ids = traktMovie.ids;

    if (tmmMovie.getIdAsString(Constants.IMDB).isEmpty() && !StringUtils.isEmpty(ids.imdb)) {
      tmmMovie.setId(Constants.IMDB, ids.imdb);
      dirty = true;
    }
    if (tmmMovie.getIdAsInt(Constants.TMDB) == 0 && MetadataUtil.unboxInteger(ids.tmdb) > 0) {
      tmmMovie.setId(Constants.TMDB, ids.tmdb);
      dirty = true;
    }
    if (tmmMovie.getIdAsInt(Constants.TRAKT) == 0 && MetadataUtil.unboxInteger(ids.trakt) > 0) {
      tmmMovie.setId(Constants.TRAKT, ids.trakt);
      dirty = true;
    }
    return dirty;
  }

  private SyncMovie toSyncMovie(Movie tmmMovie, boolean watched) {
    boolean hasId = false;
    SyncMovie movie = null;

    MovieIds ids = new MovieIds();
    if (MetadataUtil.isValidImdbId(tmmMovie.getImdbId())) {
      ids.imdb = tmmMovie.getImdbId();
      hasId = true;
    }
    if (tmmMovie.getTmdbId() > 0) {
      ids.tmdb = tmmMovie.getTmdbId();
      hasId = true;
    }
    if (tmmMovie.getIdAsInt(Constants.TRAKT) > 0) {
      ids.trakt = tmmMovie.getIdAsInt(Constants.TRAKT);
      hasId = true;
    }

    if (!hasId) {
      return movie;
    }

    // we have to decide what we send; trakt behaves differently when sending data to sync collection and sync history.
    movie = new SyncMovie();
    movie.id(ids);
    if (watched) {
      // sync history
      if (tmmMovie.isWatched() && tmmMovie.getLastWatched() == null) {
        // watched in tmm and not in trakt -> sync
        OffsetDateTime watchedAt = OffsetDateTime.ofInstant(DateTimeUtils.toInstant(new Date()), ZoneId.systemDefault());
        movie.watchedAt(watchedAt);
      }
    }
    else {
      // sync collection
      OffsetDateTime collectedAt = OffsetDateTime.ofInstant(DateTimeUtils.toInstant(tmmMovie.getDateAdded()), ZoneId.systemDefault());
      movie.collectedAt(collectedAt);
    }

    // also sync mediainfo
    movie.mediaType(getMediaType(tmmMovie.getMediaInfoSource()));
    movie.resolution(getResolution(tmmMovie.getMainVideoFile()));
    movie.hdr(getHdr(tmmMovie.getVideoHDRFormat()));
    movie.audio(getAudio(tmmMovie.getMediaInfoAudioCodec()));
    movie.audioChannels(getAudioChannels(tmmMovie.getMainVideoFile().getAudioChannelCount()));
    movie.is3d(tmmMovie.isVideoIn3D());

    return movie;
  }

  private SyncMovie toSyncMovie(BaseMovie baseMovie) {
    return new SyncMovie().id(baseMovie.movie.ids).collectedAt(baseMovie.collected_at).watchedAt(baseMovie.last_watched_at);
  }
}
