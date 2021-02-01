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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.BaseEpisode;
import com.uwetrottmann.trakt5.entities.BaseSeason;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.Metadata;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Extended;

import retrofit2.Response;

/**
 * the TV show implementation of the Trakt.tv interface
 *
 * @author Manuel Laggner
 */
class TraktTvTvShow {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraktTvTvShow.class);

  private final TraktTv       traktTv;
  private final TraktV2       api;

  public TraktTvTvShow(TraktTv traktTv) {
    this.traktTv = traktTv;
    this.api = traktTv.getApi();
  }

  /**
   * Syncs Trakt.tv collection (gets all IDs & dates, and adds all TMM shows to Trakt)<br>
   * Do not send diffs, since this is too complicated currently :|
   */
  void syncTraktTvShowCollection(List<TvShow> tvShowsInTmm) {

    // create a local copy of the list
    List<TvShow> tvShows = new ArrayList<>(tvShowsInTmm);

    // *****************************************************************************
    // 1) sync ALL missing show IDs & dates from trakt
    // *****************************************************************************
    List<BaseShow> traktShows;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      Response<List<BaseShow>> response = api.sync().collectionShows(Extended.METADATA).execute();
      if (!response.isSuccessful() && response.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        response = api.sync().collectionShows(Extended.METADATA).execute();
      }
      if (!response.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
        return;
      }
      traktShows = response.body();
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} TvShows in your Trakt.tv collection", traktShows.size());

    // remember which episodes are already in trakt
    Set<TvShowEpisode> episodesInTrakt = new HashSet<>();

    for (BaseShow traktShow : traktShows) {
      List<TvShow> matchingTmmTvShows = getTmmTvShowForTraktShow(tvShows, traktShow.show);

      for (TvShow tmmShow : matchingTmmTvShows) {
        // update show IDs from trakt
        boolean dirty = updateIDs(tmmShow, traktShow.show);

        // update collection date from trakt (show)
        if (traktShow.last_collected_at != null) {
          Date collectedAt = DateTimeUtils.toDate(traktShow.last_collected_at.toInstant());
          if (!collectedAt.equals(tmmShow.getDateAdded())) {
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking TvShow '{}' as collected on {} (was {})", tmmShow.getTitle(), collectedAt, tmmShow.getDateAddedAsString());
            tmmShow.setDateAdded(collectedAt);
            dirty = true;
          }
        }

        // update collection date from trakt (episodes)
        for (BaseSeason bs : ListUtils.nullSafe(traktShow.seasons)) {
          for (BaseEpisode be : ListUtils.nullSafe(bs.episodes)) {
            List<TvShowEpisode> matchingEpisodes = tmmShow.getEpisode(MetadataUtil.unboxInteger(bs.number, -1),
                MetadataUtil.unboxInteger(be.number, -1));
            for (TvShowEpisode tmmEp : matchingEpisodes) {

              // remove it from our list, if we already have at least a video source (so metadata has also been synced)
              if (matchesMetadata(be.metadata, tmmEp)) {
                episodesInTrakt.add(tmmEp);
              }

              if (be.collected_at != null) {
                Date collectedAt = DateTimeUtils.toDate(be.collected_at.toInstant());
                if (!collectedAt.equals(tmmEp.getDateAdded())) {
                  tmmEp.setDateAdded(collectedAt);
                  tmmEp.writeNFO();
                  tmmEp.saveToDb();
                }
              }
            }
          }
        }

        if (dirty) {
          tmmShow.writeNFO();
          tmmShow.saveToDb();
        }
      }
    }

    // *****************************************************************************
    // 2) add all our shows to Trakt collection (we have the physical file)
    // *****************************************************************************
    LOGGER.info("Adding {} TvShows to Trakt.tv collection", tvShows.size());
    // send show per show; sending all together may result too often in a timeout
    for (TvShow tvShow : tvShows) {
      SyncShow show = toSyncShow(tvShow, false, episodesInTrakt);
      if (show == null) {
        continue;
      }

      try {
        SyncItems items = new SyncItems().shows(show);
        Response<SyncResponse> response = api.sync().addItemsToCollection(items).execute();
        if (!response.isSuccessful()) {
          LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
          return;
        }
        LOGGER.debug("Trakt add-to-library status: {}", tvShow.getTitle());
        printStatus(response.body());
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }
  }

  void syncTraktTvShowWatched(List<TvShow> tvShowsInTmm) {
    // create a local copy of the list
    List<TvShow> tvShows = new ArrayList<>(tvShowsInTmm);

    List<BaseShow> traktShows;
    try {
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
      Response<List<BaseShow>> response = api.sync().watchedShows(null).execute();
      if (!response.isSuccessful() && response.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        response = api.sync().watchedShows(null).execute();
      }
      if (!response.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
        return;
      }
      traktShows = response.body();
    }
    catch (Exception e) {
      LOGGER.error("failed syncing trakt: {}", e.getMessage());
      return;
    }

    LOGGER.info("You have {} TvShows marked as watched on Trakt.tv", traktShows.size());
    for (BaseShow traktShow : traktShows) {
      List<TvShow> matchingTmmTvShows = getTmmTvShowForTraktShow(tvShows, traktShow.show);

      for (TvShow tmmShow : matchingTmmTvShows) {
        // update show IDs from trakt
        boolean dirty = updateIDs(tmmShow, traktShow.show);

        // update watched date from trakt (show)
        if (traktShow.last_watched_at != null) {
          Date lastWatchedAt = DateTimeUtils.toDate(traktShow.last_watched_at.toInstant());
          if (!lastWatchedAt.equals(tmmShow.getLastWatched())) {
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking TvShow '{}' as watched on {} (was {})", tmmShow.getTitle(), lastWatchedAt, tmmShow.getLastWatched());
            tmmShow.setLastWatched(lastWatchedAt);
            // dirty = true; // we do not write date to NFO. But just mark for syncing back...
          }
        }

        // update collection date from trakt (episodes)
        for (BaseSeason bs : ListUtils.nullSafe(traktShow.seasons)) {
          for (BaseEpisode be : ListUtils.nullSafe(bs.episodes)) {
            List<TvShowEpisode> matchingEpisodes = tmmShow.getEpisode(MetadataUtil.unboxInteger(bs.number, -1),
                MetadataUtil.unboxInteger(be.number, -1));
            for (TvShowEpisode tmmEp : matchingEpisodes) {
              boolean epDirty = false;
              if (!tmmEp.isWatched()) {
                LOGGER.trace("Marking episode '{}' as watched", tmmEp.getTitle());
                tmmEp.setWatched(true);
                epDirty = true;
              }
              if (tmmEp.getPlaycount() != MetadataUtil.unboxInteger(be.plays)) {
                tmmEp.setPlaycount(MetadataUtil.unboxInteger(be.plays));
                dirty = true;
              }

              if (epDirty) {
                tmmEp.writeNFO();
                tmmEp.saveToDb();
              }

              if (be.last_watched_at != null) {
                Date lastWatchedAt = DateTimeUtils.toDate(be.last_watched_at.toInstant());
                if (!lastWatchedAt.equals(tmmEp.getLastWatched())) {
                  tmmEp.setLastWatched(lastWatchedAt);
                }
              }
            }
          }
        }

        if (dirty) {
          tmmShow.writeNFO();
          tmmShow.saveToDb();
        }
      }
    }

    // *****************************************************************************
    // 2) add all our shows to Trakt watched
    // *****************************************************************************
    LOGGER.info("Adding up to {} TvShows as watched on Trakt.tv", tvShows.size());
    // send show per show; sending all together may result too often in a timeout
    for (TvShow show : tvShows) {
      // get items to sync
      SyncShow sync = toSyncShow(show, true, new HashSet<>());
      if (sync == null) {
        continue;
      }

      try {
        SyncItems items = new SyncItems().shows(sync);
        Response<SyncResponse> response = api.sync().addItemsToWatchedHistory(items).execute();
        if (!response.isSuccessful()) {
          LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
          return;
        }
        LOGGER.debug("Trakt add-to-library status: {}", show.getTitle());
        printStatus(response.body());
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  void clearTraktTvShows() {
    // *****************************************************************************
    // 1) get ALL Trakt shows in collection / watched
    // *****************************************************************************
    List<BaseShow> traktCollection;
    List<BaseShow> traktWatched;
    try {
      // collection
      Response<List<BaseShow>> traktCollectionResponse = api.sync().collectionShows(null).execute();
      if (!traktCollectionResponse.isSuccessful() && traktCollectionResponse.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        traktCollectionResponse = api.sync().collectionShows(null).execute();
      }
      if (!traktCollectionResponse.isSuccessful()) {
        LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", traktCollectionResponse.code(), traktCollectionResponse.message());
        return;
      }
      traktCollection = traktCollectionResponse.body();

      // watched
      Response<List<BaseShow>> traktWatchedResponse = api.sync().watchedShows(null).execute();
      if (!traktWatchedResponse.isSuccessful() && traktWatchedResponse.code() == 401) {
        // try to re-auth
        traktTv.refreshAccessToken();
        traktWatchedResponse = api.sync().watchedShows(null).execute();
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

    LOGGER.info("You have {} shows in your Trakt.tv collection", traktCollection.size());
    LOGGER.info("You have {} shows watched", traktWatched.size());

    // *****************************************************************************
    // 2) remove every shows from the COLLECTION state
    // *****************************************************************************
    List<SyncShow> showToRemove = new ArrayList<>();
    for (BaseShow traktShow : traktCollection) {
      showToRemove.add(toSyncShow(traktShow));
    }
    if (!showToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().shows(showToRemove);
        Response<SyncResponse> response = api.sync().deleteItemsFromCollection(items).execute();
        if (!response.isSuccessful()) {
          LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
          return;
        }
        LOGGER.debug("removed {} shows from your trakt.tv collection", showToRemove.size());
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
        return;
      }
    }

    // *****************************************************************************
    // 3) remove every shows from the WATCHED state
    // *****************************************************************************
    showToRemove.clear();
    for (BaseShow traktShow : traktWatched) {
      showToRemove.add(toSyncShow(traktShow));
    }
    if (!showToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().shows(showToRemove);
        Response<SyncResponse> response = api.sync().deleteItemsFromWatchedHistory(items).execute();
        if (!response.isSuccessful()) {
          LOGGER.error("failed syncing trakt.tv: HTTP {} - '{}'", response.code(), response.message());
          return;
        }
        LOGGER.debug("removed {} shows from your trakt.tv watched", showToRemove.size());
      }
      catch (Exception e) {
        LOGGER.error("failed syncing trakt: {}", e.getMessage());
      }
    }
  }

  private List<TvShow> getTmmTvShowForTraktShow(List<TvShow> tmmTvShows, Show traktShow) {
    return tmmTvShows.stream().filter(tvShow -> matches(tvShow, traktShow)).collect(Collectors.toList());
  }

  private boolean matches(TvShow tmmShow, Show traktShow) {
    if (traktShow == null || traktShow.ids == null) {
      return false;
    }

    ShowIds ids = traktShow.ids;

    int traktId = MetadataUtil.unboxInteger(ids.trakt);
    if (traktId > 0 && traktId == tmmShow.getIdAsInt(Constants.TRAKT)) {
      return true;
    }

    if (StringUtils.isNotEmpty(ids.imdb) && ids.imdb.equals(tmmShow.getImdbId())) {
      return true;
    }

    int tmdbId = MetadataUtil.unboxInteger(ids.tmdb);
    if (tmdbId > 0 && tmdbId == tmmShow.getTmdbId()) {
      return true;
    }

    int tvdbId = MetadataUtil.unboxInteger(ids.tvdb);
    if (tvdbId > 0 && tvdbId == tmmShow.getIdAsInt(Constants.TVDB)) {
      return true;
    }

    // not used atm
    int tvrageId = MetadataUtil.unboxInteger(ids.tvrage);
    if (tvrageId > 0 && tvrageId == tmmShow.getIdAsInt("tvrage")) {
      return true;
    }

    return false;
  }

  private boolean updateIDs(TvShow tmmShow, Show traktShow) {
    boolean dirty = false;
    if (traktShow == null || traktShow.ids == null) {
      return dirty;
    }

    ShowIds ids = traktShow.ids;

    if (tmmShow.getIdAsString(Constants.IMDB).isEmpty() && !StringUtils.isEmpty(ids.imdb)) {
      tmmShow.setId(Constants.IMDB, ids.imdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(Constants.TMDB) == 0 && MetadataUtil.unboxInteger(ids.tmdb) > 0) {
      tmmShow.setId(Constants.TMDB, ids.tmdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(Constants.TRAKT) == 0 && MetadataUtil.unboxInteger(ids.trakt) > 0) {
      tmmShow.setId(Constants.TRAKT, ids.trakt);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(Constants.TVDB) == 0 && MetadataUtil.unboxInteger(ids.tvdb) > 0) {
      tmmShow.setId(Constants.TVDB, ids.tvdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt("tvrage") == 0 && MetadataUtil.unboxInteger(ids.tvrage) > 0) {
      tmmShow.setId("tvrage", ids.tvrage);
      dirty = true;
    }

    return dirty;
  }

  private SyncShow toSyncShow(TvShow tmmShow, boolean watched, Set<TvShowEpisode> episodesInTrakt) {
    boolean hasId = false;

    ShowIds ids = new ShowIds();
    if (MetadataUtil.isValidImdbId(tmmShow.getImdbId())) {
      ids.imdb = tmmShow.getImdbId();
      hasId = true;
    }

    if (tmmShow.getTmdbId() > 0) {
      ids.tmdb = tmmShow.getTmdbId();
      hasId = true;
    }

    if (tmmShow.getIdAsInt(Constants.TVDB) > 0) {
      ids.tvdb = tmmShow.getIdAsInt(Constants.TVDB);
      hasId = true;
    }

    if (tmmShow.getIdAsInt(Constants.TRAKT) > 0) {
      ids.trakt = tmmShow.getIdAsInt(Constants.TRAKT);
      hasId = true;
    }

    if (tmmShow.getIdAsInt("tvrage") > 0) {
      ids.tvrage = tmmShow.getIdAsInt("tvrage");
      hasId = true;
    }

    if (!hasId) {
      return null;
    }

    List<SyncSeason> syncSeasons = new ArrayList<>();
    boolean foundS = false;
    for (TvShowSeason tmmSeason : tmmShow.getSeasons()) {
      boolean foundEP = false;

      // since there can be multiple versions of the same episode, we force to take the first one for trakt sync AND combine all watched states vir
      // logical OR
      Map<String, SyncEpisode> syncEpisodeMap = new HashMap<>();
      for (TvShowEpisode tmmEp : tmmSeason.getEpisodes()) {
        if (tmmEp.getEpisode() < 0) {
          continue;
        }

        String episodeTag = "S" + tmmEp.getSeason() + "E" + tmmEp.getEpisode();

        // we have to decide what we send; trakt behaves differently when sending data to
        // sync collection and sync history.
        if (watched) {
          // sync history
          if (!syncEpisodeMap.containsKey(episodeTag) && tmmEp.isWatched() && tmmEp.getLastWatched() == null) {
            // watched in tmm and not in trakt -> sync
            OffsetDateTime watchedAt = OffsetDateTime.ofInstant(DateTimeUtils.toInstant(new Date()), ZoneId.systemDefault());
            syncEpisodeMap.put(episodeTag, toSyncEpisode(tmmEp).watchedAt(watchedAt));

            foundEP = true;
          }
        }
        else {
          // sync collection
          if (!episodesInTrakt.contains(tmmEp) && !syncEpisodeMap.containsKey(episodeTag)) {
            OffsetDateTime collectedAt = OffsetDateTime.ofInstant(DateTimeUtils.toInstant(tmmEp.getDateAdded()), ZoneId.systemDefault());
            syncEpisodeMap.put(episodeTag, toSyncEpisode(tmmEp).collectedAt(collectedAt));
            foundEP = true;
          }
        }
      }
      if (foundEP) {
        // do not send empty seasons
        foundS = true;
        syncSeasons.add(new SyncSeason().number(tmmSeason.getSeason()).episodes(new ArrayList<>(syncEpisodeMap.values())));
      }
    }

    if (foundS) {
      // we have at least one season/episode, so add it
      OffsetDateTime collectedAt = OffsetDateTime.ofInstant(DateTimeUtils.toInstant(tmmShow.getDateAdded()), ZoneId.systemDefault());
      return new SyncShow().id(ids).collectedAt(collectedAt).seasons(syncSeasons);
    }

    // if nothing added, do NOT send an empty show (to add all)
    return null;
  }

  private SyncShow toSyncShow(BaseShow baseShow) {
    // TODO: used only on clear() - so we don't need the episodes? TBC
    ArrayList<SyncSeason> ss = new ArrayList<>();
    for (BaseSeason baseSeason : baseShow.seasons) {
      ArrayList<SyncEpisode> se = new ArrayList<>();
      for (BaseEpisode baseEp : baseSeason.episodes) {
        se.add(new SyncEpisode().number(baseEp.number).collectedAt(baseEp.collected_at).watchedAt(baseEp.collected_at));
      }
      ss.add(new SyncSeason().number(baseSeason.number).episodes(se));
    }
    return new SyncShow().id(baseShow.show.ids).collectedAt(baseShow.last_collected_at).watchedAt(baseShow.last_watched_at).seasons(ss);
  }

  private SyncEpisode toSyncEpisode(TvShowEpisode episode) {
    SyncEpisode syncEpisode = new SyncEpisode();
    syncEpisode.number(episode.getEpisode());

    // also sync mediainfo
    syncEpisode.mediaType(getMediaType(episode.getMediaInfoSource()));
    syncEpisode.resolution(getResolution(episode.getMainVideoFile()));
    syncEpisode.hdr(getHdr(episode.getVideoHDRFormat()));
    syncEpisode.audio(getAudio(episode.getMediaInfoAudioCodec()));
    syncEpisode.audioChannels(getAudioChannels(episode.getMainVideoFile().getAudioChannelCount()));
    syncEpisode.is3d(episode.isVideoIn3D());

    return syncEpisode;
  }

  static boolean matchesMetadata(Metadata metadata, TvShowEpisode episode) {
    if (metadata == null) {
      return false;
    }

    if (metadata.is3d == null || metadata.is3d != episode.isVideoIn3D()) {
      return false;
    }

    if (metadata.audio != getAudio(episode.getMediaInfoAudioCodec())) {
      return false;
    }

    if (metadata.media_type != getMediaType(episode.getMediaInfoSource())) {
      return false;
    }

    if (metadata.resolution != getResolution(episode.getMainVideoFile())) {
      return false;
    }
    if (metadata.audio_channels == getAudioChannels(episode.getMainVideoFile().getAudioChannelCount())) {
      return false;
    }

    if (metadata.hdr != getHdr(episode.getVideoHDRFormat())) {
      return false;
    }

    return true;
  }
}
