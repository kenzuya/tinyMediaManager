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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaSource;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.http.TmmHttpClient;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.TraktV2Interceptor;
import com.uwetrottmann.trakt5.entities.AccessToken;
import com.uwetrottmann.trakt5.entities.SyncErrors;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncStats;
import com.uwetrottmann.trakt5.enums.Audio;
import com.uwetrottmann.trakt5.enums.AudioChannels;
import com.uwetrottmann.trakt5.enums.Hdr;
import com.uwetrottmann.trakt5.enums.MediaType;
import com.uwetrottmann.trakt5.enums.Resolution;

import okhttp3.OkHttpClient;
import retrofit2.Response;

/**
 * Sync your collection and watched status with Trakt.tv<br>
 * Using best practice 2-way-sync according to http://trakt.tv/api-docs/sync<br>
 * https://github.com/UweTrottmann/trakt-java
 * 
 * @author Myron Boyle
 * 
 */

public class TraktTv {
  private static final Logger  LOGGER = LoggerFactory.getLogger(TraktTv.class);
  private static final TraktV2 API    = createTraktApi();
  private static TraktTv       instance;

  private static TraktV2 createTraktApi() {
    String clientId;
    String clientSecret;
    try {
      clientId = License.getInstance().getApiKey("trakt");
      clientSecret = License.getInstance().getApiKey("trakt.secret");
    }
    catch (Exception e) {
      clientId = "";
      clientSecret = "";
    }

    return new TraktV2(clientId, clientSecret, "urn:ietf:wg:oauth:2.0:oob") {
      // tell the trakt.tv api to use our OkHttp client

      @Override
      protected synchronized OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
        builder.addInterceptor(new TraktV2Interceptor(this));
        return builder.build();
      }
    };
  }

  public static synchronized TraktTv getInstance() {
    if (instance == null) {
      instance = new TraktTv();
    }
    return instance;
  }

  private TraktTv() {
  }

  public static Map<String, String> authenticateViaPin(String pin) throws IOException {
    Map<String, String> result = new HashMap<>();
    Response<AccessToken> response = API.exchangeCodeForAccessToken(pin);

    if (response.isSuccessful() && response.body() != null) {
      // get tokens
      String accessToken = response.body().access_token;
      String refreshToken = response.body().refresh_token;
      if (StringUtils.isNoneBlank(accessToken, refreshToken)) {
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
      }
    }

    return result;
  }

  /**
   * get a new accessToken with the refreshToken
   */
  public static void refreshAccessToken() throws IOException {
    if (StringUtils.isBlank(Globals.settings.getTraktRefreshToken())) {
      throw new IOException("no trakt.tv refresh token found");
    }

    Response<AccessToken> response = API.refreshToken(Globals.settings.getTraktRefreshToken())
        .refreshAccessToken(Globals.settings.getTraktRefreshToken());

    if (response.isSuccessful() && response.body() != null) {
      if (StringUtils.isNoneBlank(response.body().access_token, response.body().refresh_token)) {
        Globals.settings.setTraktAccessToken(response.body().access_token);
        Globals.settings.setTraktRefreshToken(response.body().refresh_token);
        API.accessToken(Globals.settings.getTraktAccessToken());
      }
    }
    else {
      throw new IOException("could not get trakt.tv refresh token (HTTP " + response.code() + ")");
    }
  }

  /**
   * do we have values for user/pass/api?!
   * 
   * @return true/false if trakt could be called
   */
  private boolean isEnabled() {
    if (StringUtils.isNoneBlank(Globals.settings.getTraktAccessToken(), Globals.settings.getTraktRefreshToken())) {
      // everything seems fine; also set the access token
      API.accessToken(Globals.settings.getTraktAccessToken());
      return true;
    }
    return false;
  }

  // @formatter:off
  // ███╗   ███╗ ██████╗ ██╗   ██╗██╗███████╗███████╗
  // ████╗ ████║██╔═══██╗██║   ██║██║██╔════╝██╔════╝
  // ██╔████╔██║██║   ██║██║   ██║██║█████╗  ███████╗
  // ██║╚██╔╝██║██║   ██║╚██╗ ██╔╝██║██╔══╝  ╚════██║
  // ██║ ╚═╝ ██║╚██████╔╝ ╚████╔╝ ██║███████╗███████║
  // ╚═╝     ╚═╝ ╚═════╝   ╚═══╝  ╚═╝╚══════╝╚══════╝
  // @formatter:on

  /**
   * Syncs Trakt.tv collection (specified movies)<br>
   * Gets all Trakt movies from collection, matches them to ours, and sends ONLY the new ones back to Trakt
   */
  public void syncTraktMovieCollection(List<Movie> moviesInTmm) {
    if (!isEnabled()) {
      return;
    }

    new TraktTvMovie(API).syncTraktMovieCollection(moviesInTmm);
  }

  /**
   * Syncs Trakt.tv "seen" flag (all gives you have already marked as watched)<br>
   * Gets all watched movies from Trakt, and sets the "watched" flag on TMM movies.<br>
   * Then update the remaining TMM movies on Trakt as 'seen'.
   */
  public void syncTraktMovieWatched(List<Movie> moviesInTmm) {
    if (!isEnabled()) {
      return;
    }

    new TraktTvMovie(API).syncTraktMovieWatched(moviesInTmm);
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  void clearTraktMovies() {
    if (!isEnabled()) {
      return;
    }

    new TraktTvMovie(API).clearTraktMovies();
  }

  // @formatter:off
  //  ████████╗██╗   ██╗███████╗██╗  ██╗ ██████╗ ██╗    ██╗███████╗
  //  ╚══██╔══╝██║   ██║██╔════╝██║  ██║██╔═══██╗██║    ██║██╔════╝
  //     ██║   ██║   ██║███████╗███████║██║   ██║██║ █╗ ██║███████╗
  //     ██║   ╚██╗ ██╔╝╚════██║██╔══██║██║   ██║██║███╗██║╚════██║
  //     ██║    ╚████╔╝ ███████║██║  ██║╚██████╔╝╚███╔███╔╝███████║
  //     ╚═╝     ╚═══╝  ╚══════╝╚═╝  ╚═╝ ╚═════╝  ╚══╝╚══╝ ╚══════╝
  // @formatter:on

  /**
   * Syncs Trakt.tv collection (gets all IDs & dates, and adds all TMM shows to Trakt)<br>
   * Do not send diffs, since this is too complicated currently :|
   */
  public void syncTraktTvShowCollection(List<TvShow> tvShowsInTmm) {
    if (!isEnabled()) {
      return;
    }

    new TraktTvTvShow(API).syncTraktTvShowCollection(tvShowsInTmm);
  }

  public void syncTraktTvShowWatched(List<TvShow> tvShowsInTmm) {
    if (!isEnabled()) {
      return;
    }

    new TraktTvTvShow(API).syncTraktTvShowWatched(tvShowsInTmm);
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  public void clearTraktTvShows() {
    if (!isEnabled()) {
      return;
    }

    new TraktTvTvShow(API).clearTraktTvShows();
  }

  // @formatter:off
  // ██╗   ██╗████████╗██╗██╗     ███████╗
  // ██║   ██║╚══██╔══╝██║██║     ██╔════╝
  // ██║   ██║   ██║   ██║██║     ███████╗
  // ██║   ██║   ██║   ██║██║     ╚════██║
  // ╚██████╔╝   ██║   ██║███████╗███████║
  //  ╚═════╝    ╚═╝   ╚═╝╚══════╝╚══════╝
  // @formatter:on
  static MediaType getMediaType(MediaSource mediaSource) {
    if (mediaSource == MediaSource.BLURAY || mediaSource == MediaSource.UHD_BLURAY) {
      return MediaType.BLURAY;
    }
    if (mediaSource == MediaSource.DVD || mediaSource == MediaSource.DVDSCR) {
      return MediaType.DVD;
    }
    if (mediaSource == MediaSource.HDDVD) {
      return MediaType.HDDVD;
    }
    if (mediaSource == MediaSource.VHS || mediaSource == MediaSource.D_VHS) {
      return MediaType.VHS;
    }
    if (mediaSource == MediaSource.LASERDISC) {
      return MediaType.LASERDISC;
    }
    return MediaType.DIGITAL;
  }

  static Resolution getResolution(MediaFile mediaFile) {
    switch (mediaFile.getVideoFormat()) {
      case MediaFileHelper.VIDEO_FORMAT_4320P:
      case MediaFileHelper.VIDEO_FORMAT_2160P:
        return Resolution.UHD_4K;

      case MediaFileHelper.VIDEO_FORMAT_1440P:
      case MediaFileHelper.VIDEO_FORMAT_1080P:
        return Resolution.HD_1080P;

      case MediaFileHelper.VIDEO_FORMAT_720P:
        return Resolution.HD_720P;

      case MediaFileHelper.VIDEO_FORMAT_576P:
      case MediaFileHelper.VIDEO_FORMAT_540P:
        return Resolution.SD_576P;

      case MediaFileHelper.VIDEO_FORMAT_480P:
      case MediaFileHelper.VIDEO_FORMAT_360P:
        return Resolution.SD_480P;

      default:
        return null;
    }
  }

  static Hdr getHdr(String hdr) {
    if ("hdr10+".equalsIgnoreCase(hdr)) {
      return Hdr.HDR10_PLUS;
    }
    if ("hdr".equalsIgnoreCase(hdr)) {
      return Hdr.HDR10;
    }
    if ("dolby vision".equalsIgnoreCase(hdr)) {
      return Hdr.DOLBY_VISION;
    }
    if ("hlg".equalsIgnoreCase(hdr)) {
      return Hdr.HLG;
    }
    return null;
  }

  static Audio getAudio(String audioCodec) {
    if ("DTSHD-MA".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS_MA;
    }
    if ("DTSHD-HRA".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS_HR;
    }
    if ("DTS-X".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS_X;
    }
    if ("Atmos".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_ATMOS;
    }
    if ("DTS".equalsIgnoreCase(audioCodec)) {
      return Audio.DTS;
    }
    if ("DTS-ES".equalsIgnoreCase(audioCodec)) {
      // DTS-ES is not (yet) supported. Use DTS for now
      return Audio.DTS;
    }
    if ("TrueHD".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_TRUEHD;
    }
    if ("EAC3".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_DIGITAL_PLUS;
    }
    if ("AC3".equalsIgnoreCase(audioCodec)) {
      return Audio.DOLBY_DIGITAL;
    }
    if ("MP2".equalsIgnoreCase(audioCodec)) {
      return Audio.MP2;
    }
    if ("MP3".equalsIgnoreCase(audioCodec)) {
      return Audio.MP3;
    }
    if ("OGG".equalsIgnoreCase(audioCodec)) {
      return Audio.OGG;
    }
    if ("WMA".equalsIgnoreCase(audioCodec)) {
      return Audio.WMA;
    }
    if ("AAC".equalsIgnoreCase(audioCodec)) {
      return Audio.AAC;
    }
    if ("FLAC".equalsIgnoreCase(audioCodec)) {
      return Audio.FLAC;
    }
    return null;
  }

  static AudioChannels getAudioChannels(int audioChannelCount) {
    switch (audioChannelCount) {
      case 1:
        return AudioChannels.CH1_0;

      case 2:
        return AudioChannels.CH2_0;

      case 3:
        return AudioChannels.CH2_1;

      case 4:
        return AudioChannels.CH3_1;

      case 5:
        return AudioChannels.CH4_1;

      case 6:
        return AudioChannels.CH5_1;

      case 7:
        return AudioChannels.CH6_1;

      case 8:
        return AudioChannels.CH7_1;

      case 10:
        return AudioChannels.CH9_1;

      case 11:
        return AudioChannels.CH10_1;

      default:
        return null;
    }
  }

  /**
   * prints some trakt response status
   * 
   * @param resp
   *          the response
   */
  static void printStatus(SyncResponse resp) {
    if (resp != null) {
      String info = getStatusString(resp.added);
      if (!info.isEmpty()) {
        LOGGER.debug("Added       : {}", info);
      }
      info = getStatusString(resp.existing);
      if (!info.isEmpty()) {
        LOGGER.debug("Existing    : {}", info);
      }
      info = getStatusString(resp.deleted);
      if (!info.isEmpty()) {
        LOGGER.debug("Deleted     : {}", info);
      }
      info = getStatusString(resp.not_found);
      if (!info.isEmpty()) {
        LOGGER.debug("Errors      : {}", info);
      }
    }
  }

  static String getStatusString(SyncStats ss) {
    if (ss == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(50);

    if (ss.movies != null && ss.movies > 0) {
      sb.append(ss.movies).append(" Movies ");
    }
    if (ss.shows != null && ss.shows > 0) {
      sb.append(ss.shows).append(" Shows ");
    }
    if (ss.seasons != null && ss.seasons > 0) {
      sb.append(ss.seasons).append(" Seasons ");
    }
    if (ss.episodes != null && ss.episodes > 0) {
      sb.append(ss.episodes).append(" Episodes");
    }

    return sb.toString();
  }

  static String getStatusString(SyncErrors ss) {
    if (ss == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(50);

    if (ss.movies != null && !ss.movies.isEmpty()) {
      sb.append(ss.movies.size()).append(" Movies ");
    }
    if (ss.shows != null && !ss.shows.isEmpty()) {
      sb.append(ss.shows.size()).append(" Shows ");
    }
    if (ss.seasons != null && !ss.seasons.isEmpty()) {
      sb.append(ss.seasons.size()).append(" Seasons ");
    }
    if (ss.episodes != null && !ss.episodes.isEmpty()) {
      sb.append(ss.episodes.size()).append(" Episodes");
    }

    return sb.toString();
  }
}
