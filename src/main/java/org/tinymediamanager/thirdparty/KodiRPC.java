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

package org.tinymediamanager.thirdparty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.jsonrpc.api.AbstractCall;
import org.tinymediamanager.jsonrpc.api.call.Application;
import org.tinymediamanager.jsonrpc.api.call.AudioLibrary;
import org.tinymediamanager.jsonrpc.api.call.Files;
import org.tinymediamanager.jsonrpc.api.call.System;
import org.tinymediamanager.jsonrpc.api.call.VideoLibrary;
import org.tinymediamanager.jsonrpc.api.model.ApplicationModel;
import org.tinymediamanager.jsonrpc.api.model.FilesModel;
import org.tinymediamanager.jsonrpc.api.model.GlobalModel;
import org.tinymediamanager.jsonrpc.api.model.ListModel;
import org.tinymediamanager.jsonrpc.api.model.VideoModel;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.EpisodeDetail;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.EpisodeFields;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.MovieDetail;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.MovieFields;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.TVShowDetail;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.TVShowFields;
import org.tinymediamanager.jsonrpc.config.HostConfig;
import org.tinymediamanager.jsonrpc.io.ApiCallback;
import org.tinymediamanager.jsonrpc.io.ApiException;
import org.tinymediamanager.jsonrpc.io.ConnectionListener;
import org.tinymediamanager.jsonrpc.io.JavaConnectionManager;
import org.tinymediamanager.jsonrpc.io.JsonApiRequest;
import org.tinymediamanager.jsonrpc.notification.AbstractEvent;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

public class KodiRPC {
  private static final Logger         LOGGER                   = LoggerFactory.getLogger(KodiRPC.class);
  private static KodiRPC              instance;

  private final JavaConnectionManager connectionManager        = new JavaConnectionManager();

  private final List<SplitUri>        videodatasources         = new ArrayList<>();
  private final List<String>          videodatasourcesAsString = new ArrayList<>();
  private final List<SplitUri>        audiodatasources         = new ArrayList<>();

  // TMM DbId-to-KodiId mappings
  private final Map<UUID, Integer>    moviemappings            = new HashMap<>();
  private final Map<UUID, Integer>    tvshowmappings           = new HashMap<>();

  private String                      kodiVersion              = "";

  private KodiRPC() {
    connectionManager.registerConnectionListener(new ConnectionListener() {

      @Override
      public void notificationReceived(AbstractEvent event) {
        LOGGER.debug("Event received: {}", event);
      }

      @Override
      public void disconnected() {
        LOGGER.info("Event: Disconnected");
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.INFO, "Kodi disconnected"));
      }

      @Override
      public void connected() {
        LOGGER.info("Event: Connected to {}", connectionManager.getHostConfig().getAddress());
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.INFO, "Kodi connected"));
      }
    });
  }

  public static synchronized KodiRPC getInstance() {
    if (instance == null) {
      instance = new KodiRPC();
    }

    return instance;
  }

  public boolean isConnected() {
    return connectionManager.isConnected();
  }

  // -----------------------------------------------------------------------------------

  /**
   * gets the Kodi version (cached on connect)
   * 
   * @return
   */
  public String getVersion() {
    return "Kodi " + kodiVersion;
  }

  // -----------------------------------------------------------------------------------

  public void cleanVideoLibrary() {
    final VideoLibrary.Clean call = new VideoLibrary.Clean(true);
    sendWoResponse(call);
  }

  public void scanVideoLibrary() {
    final VideoLibrary.Scan call = new VideoLibrary.Scan(null, true);
    sendWoResponse(call);
  }

  public void scanVideoLibrary(String dir) {
    final VideoLibrary.Scan call = new VideoLibrary.Scan(dir, true);
    sendWoResponse(call);
  }

  public List<SplitUri> getVideoDataSources() {
    return this.videodatasources;
  }

  public List<String> getVideoDataSourcesAsString() {
    return this.videodatasourcesAsString;
  }

  private void getAndSetVideoDataSources() {
    final Files.GetSources call = new Files.GetSources(FilesModel.Media.VIDEO); // movies + tv !!!

    this.videodatasources.clear();
    this.videodatasourcesAsString.clear();

    send(call);
    if (call.getResults() != null && !call.getResults().isEmpty()) {
      try {
        for (ListModel.SourceItem res : call.getResults()) {
          LOGGER.debug("Kodi datasource: {}", res.file);
          this.videodatasourcesAsString.add(res.file);

          SplitUri s = new SplitUri(res.file, "", res.label, connectionManager.getHostConfig().getAddress());
          this.videodatasources.add(s);
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not process Kodi RPC response - '{}'", e.getMessage());
      }

      // sort by length (longest first)
      Comparator<String> c = Comparator.comparingInt(String::length).thenComparing(Comparator.reverseOrder());
      this.videodatasourcesAsString.sort(c);
    }
  }

  private String detectDatasource(String file) {
    for (String ds : this.videodatasourcesAsString) {
      if (file.startsWith(ds)) {
        return ds;
      }
    }
    return "";
  }

  /**
   * builds the moviemappings: DBid -> Kodi ID
   */
  protected void getAndSetMovieMappings() {
    final VideoLibrary.GetMovies call = new VideoLibrary.GetMovies(MovieFields.FILE, MovieFields.IMDBNUMBER, MovieFields.TITLE, MovieFields.YEAR);
    send(call);

    if (call.getResults() != null && !call.getResults().isEmpty()) {
      // cache our lookup maps
      Map<SplitUri, Movie> tmmFiles = prepareMovieFileMap(MovieModuleManager.getInstance().getMovieList().getMovies());
      Map<String, Movie> imdbIds = prepareMovieImdbIdMap(MovieModuleManager.getInstance().getMovieList().getMovies());
      Map<String, List<Movie>> titles = prepareMovieTitleMap(MovieModuleManager.getInstance().getMovieList().getMovies());

      LOGGER.debug("TMM {} movies", tmmFiles.size());

      // iterate over all Kodi resources
      try {
        for (MovieDetail res : call.getResults()) {
          Movie foundMovie = findMatchingMovie(res, tmmFiles, imdbIds, titles);
          if (foundMovie != null) {
            moviemappings.put(foundMovie.getDbId(), res.movieid);
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not process Kodi RPC response - '{}'", e.getMessage());
      }

      LOGGER.debug("mapped {} movies", moviemappings.size());
    }
  }

  private Map<SplitUri, Movie> prepareMovieFileMap(List<Movie> movies) {
    Map<SplitUri, Movie> fileMap = new HashMap<>();

    for (Movie movie : movies) {
      MediaFile main = movie.getMainVideoFile();
      if (movie.isDisc()) {
        // Kodi RPC sends what we call the main disc identifier
        for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO)) {

          // append MainDiscIdentifier to our folder MF
          if (mf.getFilename().equalsIgnoreCase(MediaFileHelper.VIDEO_TS)) {
            fileMap.put(new SplitUri(movie.getDataSource(), mf.getFileAsPath().resolve("VIDEO_TS.IFO").toString()), movie);
          }
          else if (mf.getFilename().equalsIgnoreCase(MediaFileHelper.HVDVD_TS)) {
            fileMap.put(new SplitUri(movie.getDataSource(), mf.getFileAsPath().resolve("HV000I01.IFO").toString()), movie);
          }
          else if (mf.getFilename().equalsIgnoreCase(MediaFileHelper.BDMV)) {
            fileMap.put(new SplitUri(movie.getDataSource(), mf.getFileAsPath().resolve("index.bdmv").toString()), movie);
          }
          else if (mf.isMainDiscIdentifierFile()) {
            // just add MainDiscIdentifier
            fileMap.put(new SplitUri(movie.getDataSource(), mf.getFileAsPath().toString()), movie);
          }
        }
      }
      else {
        fileMap.put(new SplitUri(movie.getDataSource(), main.getFileAsPath().toString()), movie);
      }
    }

    return fileMap;
  }

  private Map<String, Movie> prepareMovieImdbIdMap(List<Movie> movies) {
    Map<String, Movie> imdbIdMap = new HashMap<>();

    for (Movie movie : movies) {
      if (MediaIdUtil.isValidImdbId(movie.getImdbId())) {
        imdbIdMap.put(movie.getImdbId(), movie);
      }
    }

    return imdbIdMap;
  }

  private Map<String, List<Movie>> prepareMovieTitleMap(List<Movie> movies) {
    Map<String, List<Movie>> titleMap = new HashMap<>();

    for (Movie movie : movies) {
      if (StringUtils.isNotBlank(movie.getTitle())) {
        List<Movie> moviesForTitle = titleMap.computeIfAbsent(movie.getTitle(), k -> new ArrayList<>());
        moviesForTitle.add(movie);
      }
    }

    return titleMap;
  }

  private Movie findMatchingMovie(MovieDetail movieDetail, Map<SplitUri, Movie> tmmFiles, Map<String, Movie> imdbIdMap,
      Map<String, List<Movie>> titleMap) {
    // first -> try to match the split uri
    if (movieDetail.file.startsWith("stack")) {
      String[] files = movieDetail.file.split(" , ");
      for (String s : files) {
        s = s.replaceFirst("^stack://", "");
        String ds = detectDatasource(s);
        SplitUri sp = new SplitUri(ds, s, movieDetail.label, connectionManager.getHostConfig().getAddress()); // generate clean object

        for (Map.Entry<SplitUri, Movie> entry : tmmFiles.entrySet()) {
          SplitUri tmmsp = entry.getKey();
          if (sp.equals(tmmsp)) {
            return entry.getValue();
          }
        }
      }
    }
    else {
      String ds = detectDatasource(movieDetail.file);
      SplitUri kodi = new SplitUri(ds, movieDetail.file, movieDetail.label, connectionManager.getHostConfig().getAddress()); // generate clean object

      for (Map.Entry<SplitUri, Movie> entry : tmmFiles.entrySet()) {
        SplitUri tmm = entry.getKey();
        if (kodi.equals(tmm)) {
          return entry.getValue();
        }
      }
    }

    // try to match by imdb id
    if (MediaIdUtil.isValidImdbId(movieDetail.imdbnumber)) {
      Movie foundMovie = imdbIdMap.get(movieDetail.imdbnumber);
      if (foundMovie != null) {
        return foundMovie;
      }
    }

    // try to match by title/year
    if (StringUtils.isNotBlank(movieDetail.title)) {
      List<Movie> foundMovies = titleMap.get(movieDetail.imdbnumber);

      if (ListUtils.isNotEmpty(foundMovies)) {
        // a) check title AND year
        for (Movie movie : foundMovies) {
          if (movieDetail.title.equalsIgnoreCase(movie.getTitle()) && movieDetail.year == movie.getYear()) {
            return movie;
          }
        }

        // b) take the first title match
        return foundMovies.get(0);
      }
    }

    return null;
  }

  /**
   * builds the show/episode mappings: DBid -> Kodi ID
   */
  protected void getAndSetTvShowMappings() {
    final VideoLibrary.GetTVShows tvShowCall = new VideoLibrary.GetTVShows(TVShowFields.FILE, TVShowFields.IMDBNUMBER, TVShowFields.TITLE,
        TVShowFields.YEAR);
    send(tvShowCall);

    if (tvShowCall.getResults() != null && !tvShowCall.getResults().isEmpty()) {
      // cache our lookup maps
      Map<SplitUri, TvShow> tmmFiles = prepareTvShowFileMap(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
      Map<String, TvShow> idMap = prepareTvShowIdMap(TvShowModuleManager.getInstance().getTvShowList().getTvShows());

      LOGGER.debug("TMM {} shows", tmmFiles.size());

      // iterate over all Kodi shows
      try {
        for (TVShowDetail show : tvShowCall.getResults()) {
          TvShow foundTvShow = findMatchingTvShow(show, tmmFiles, idMap);
          if (foundTvShow != null) {
            tvshowmappings.put(foundTvShow.getDbId(), show.tvshowid);
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not process Kodi RPC response - '{}'", e.getMessage());
      }

      LOGGER.debug("mapped {} shows", tvshowmappings.size());
    }
  }

  private Map<SplitUri, TvShow> prepareTvShowFileMap(List<TvShow> tvShows) {
    Map<SplitUri, TvShow> fileMap = new HashMap<>();

    for (TvShow show : tvShows) {
      fileMap.put(new SplitUri(show.getDataSource(), show.getPathNIO().toString()), show); // folder
    }

    return fileMap;
  }

  private Map<String, TvShow> prepareTvShowIdMap(List<TvShow> tvShows) {
    Map<String, TvShow> idMap = new HashMap<>();

    for (TvShow tvShow : tvShows) {
      if (MediaIdUtil.isValidImdbId(tvShow.getImdbId())) {
        idMap.put(tvShow.getImdbId(), tvShow);
      }
      if (MetadataUtil.parseInt(tvShow.getTvdbId(), 0) > 0) {
        idMap.put(tvShow.getTvdbId(), tvShow);
      }
    }

    return idMap;
  }

  private TvShow findMatchingTvShow(TVShowDetail tvShowDetail, Map<SplitUri, TvShow> tmmFiles, Map<String, TvShow> idMap) {
    // first -> try to match the split uri
    if (tvShowDetail.file.startsWith("stack")) {
      String[] files = tvShowDetail.file.split(" , ");
      for (String s : files) {
        s = s.replaceFirst("^stack://", "");
        String ds = detectDatasource(s);
        SplitUri sp = new SplitUri(ds, s, tvShowDetail.label, connectionManager.getHostConfig().getAddress()); // generate clean object

        for (Map.Entry<SplitUri, TvShow> entry : tmmFiles.entrySet()) {
          SplitUri tmmsp = entry.getKey();
          if (sp.equals(tmmsp)) {
            return entry.getValue();
          }
        }
      }
    }
    else {
      String ds = detectDatasource(tvShowDetail.file);
      SplitUri kodi = new SplitUri(ds, tvShowDetail.file, tvShowDetail.label, connectionManager.getHostConfig().getAddress()); // generate clean
                                                                                                                               // object

      for (Map.Entry<SplitUri, TvShow> entry : tmmFiles.entrySet()) {
        SplitUri tmm = entry.getKey();
        if (kodi.equals(tmm)) {
          return entry.getValue();
        }
      }
    }

    // try to match by imdb id
    if (MediaIdUtil.isValidImdbId(tvShowDetail.imdbnumber) || MetadataUtil.parseInt(tvShowDetail.imdbnumber, 0) > 0) {
      TvShow tvShow = idMap.get(tvShowDetail.imdbnumber);
      if (tvShow != null) {
        return tvShow;
      }
    }

    return null;
  }

  public void refreshFromNfo(Movie movie) {
    Integer kodiID = moviemappings.get(movie.getDbId());

    if (kodiID != null) {
      List<MediaFile> nfo = movie.getMediaFiles(MediaFileType.NFO);
      if (!nfo.isEmpty()) {
        LOGGER.info("Refreshing from NFO: {}", nfo.get(0).getFileAsPath());
      }
      else {
        LOGGER.error("No NFO file found to refresh! {}", movie.getTitle());
        // we do NOT return here, maybe Kodi will do something even w/o nfo...
      }

      final VideoLibrary.RefreshMovie call = new VideoLibrary.RefreshMovie(kodiID, false); // always refresh from NFO
      sendWoResponse(call);
    }
    else {
      LOGGER.error("Unable to refresh - could not map '{}' to Kodi library! {}", movie.getTitle(), movie.getDbId());
    }
  }

  public void refreshFromNfo(TvShow tvShow) {
    Integer kodiID = tvshowmappings.get(tvShow.getDbId());

    if (kodiID != null) {
      List<MediaFile> nfo = tvShow.getMediaFiles(MediaFileType.NFO);
      if (!nfo.isEmpty()) {
        LOGGER.info("Refreshing from NFO: {}", nfo.get(0).getFileAsPath());
      }
      else {
        LOGGER.error("No NFO file found to refresh! {}", tvShow.getTitle());
        // we do NOT return here, maybe Kodi will do something even w/o nfo...
      }

      final VideoLibrary.RefreshTVShow call = new VideoLibrary.RefreshTVShow(kodiID, false, true); // always refresh from NFO, recursive
      sendWoResponse(call);
    }
    else {
      LOGGER.error("Unable to refresh - could not map '{}' to Kodi library! {}", tvShow.getTitle(), tvShow.getDbId());
    }
  }

  public void refreshFromNfo(TvShowEpisode episode) {
    Integer kodiID = getEpisodeId(episode);

    if (kodiID != null) {
      List<MediaFile> nfo = episode.getMediaFiles(MediaFileType.NFO);
      if (!nfo.isEmpty()) {
        LOGGER.info("Refreshing from NFO: {}", nfo.get(0).getFileAsPath());
      }
      else {
        LOGGER.error("No NFO file found to refresh! {}", episode.getTitle());
        // we do NOT return here, maybe Kodi will do something even w/o nfo...
      }

      final VideoLibrary.RefreshEpisode call = new VideoLibrary.RefreshEpisode(kodiID, false); // always refresh from NFO
      sendWoResponse(call);
    }
    else {
      LOGGER.error("Unable to refresh - could not map '{}' to Kodi library! {}", episode.getTitle(), episode.getDbId());
    }
  }

  public void readWatchedState(Movie movie) {
    Integer kodiID = moviemappings.get(movie.getDbId());

    if (kodiID != null) {
      final VideoLibrary.GetMovieDetails call = new VideoLibrary.GetMovieDetails(kodiID, VideoModel.BaseDetail.PLAYCOUNT);
      send(call);
      if (call.getResult() != null && call.getResult().playcount != null) {
        movie.setPlaycount(call.getResult().playcount);
        if (call.getResult().playcount > 0) {
          movie.setWatched(true);
        }
      }
    }
    else {
      LOGGER.error("Unable get playcount - could not map '{}' to Kodi library! {}", movie.getTitle(), movie.getDbId());
    }
  }

  public void readWatchedState(TvShowEpisode episode) {
    Integer kodiID = getEpisodeId(episode);

    if (kodiID != null) {
      final VideoLibrary.GetEpisodeDetails call = new VideoLibrary.GetEpisodeDetails(kodiID, VideoModel.BaseDetail.PLAYCOUNT);
      send(call);
      if (call.getResult() != null && call.getResult().playcount != null) {
        episode.setPlaycount(call.getResult().playcount);
        if (call.getResult().playcount > 0) {
          episode.setWatched(true);
        }
      }
    }
    else {
      LOGGER.error("Unable get playcount - could not map '{}' to Kodi library! {}", episode.getTitle(), episode.getDbId());
    }
  }

  private Integer getEpisodeId(TvShowEpisode episode) {
    Integer tvShowId = tvshowmappings.get(episode.getTvShowDbId());
    if (tvShowId == null) {
      return null;
    }

    final VideoLibrary.GetEpisodes episodeCall = new VideoLibrary.GetEpisodes(tvShowId, EpisodeFields.FILE, EpisodeFields.SEASON,
        EpisodeFields.EPISODE, EpisodeFields.TITLE);
    send(episodeCall);

    if (episodeCall.getResults() != null && !episodeCall.getResults().isEmpty()) {
      return findMatchingEpisode(episode, episodeCall.getResults());
    }
    return null;
  }

  private Integer findMatchingEpisode(TvShowEpisode episode, List<EpisodeDetail> episodeDetails) {
    MediaFile main = episode.getMainVideoFile();

    SplitUri splitUri = null;
    if (episode.isDisc()) {
      // Kodi RPC sends only those disc files
      for (MediaFile mf : episode.getMediaFiles(MediaFileType.VIDEO)) {
        if (mf.getFilename().equalsIgnoreCase("VIDEO_TS.IFO") || mf.getFilename().equalsIgnoreCase("INDEX.BDMV")) {
          splitUri = new SplitUri(episode.getDataSource(), mf.getFileAsPath().toString());
        }
      }
    }
    else {
      splitUri = new SplitUri(episode.getDataSource(), main.getFileAsPath().toString());
    }

    if (splitUri != null) {
      for (EpisodeDetail episodeDetail : episodeDetails) {
        // first -> try to match the split uri
        if (episodeDetail.file.startsWith("stack")) {
          String[] files = episodeDetail.file.split(" , ");
          for (String s : files) {
            s = s.replaceFirst("^stack://", "");
            String ds = detectDatasource(s);
            SplitUri sp = new SplitUri(ds, s, episodeDetail.label, connectionManager.getHostConfig().getAddress()); // generate clean object

            if (sp.equals(splitUri)) {
              return episodeDetail.episodeid;
            }
          }
        }
        else {
          String ds = detectDatasource(episodeDetail.file);
          SplitUri kodi = new SplitUri(ds, episodeDetail.file, episodeDetail.label, connectionManager.getHostConfig().getAddress()); // generate clean
          // object

          if (kodi.equals(splitUri)) {
            return episodeDetail.episodeid;
          }
        }
      }
    }

    // try to match by season/episode
    if (episode.getSeason() > -1 && episode.getEpisode() > -1) {
      for (EpisodeDetail episodeDetail : episodeDetails) {
        if (episodeDetail.season == episode.getSeason() && episodeDetail.episode == episode.getEpisode()) {
          return episodeDetail.episodeid;
        }
      }
    }

    return null;
  }

  // -----------------------------------------------------------------------------------

  public void cleanAudioLibrary() {
    final AudioLibrary.Clean call = new AudioLibrary.Clean(true);
    sendWoResponse(call);
  }

  public void scanAudioLibrary() {
    final AudioLibrary.Scan call = new AudioLibrary.Scan(null);
    sendWoResponse(call);
  }

  public void scanAudioLibrary(String dir) {
    final AudioLibrary.Scan call = new AudioLibrary.Scan(dir);
    sendWoResponse(call);
  }

  public List<SplitUri> getAudioDataSources() {
    return this.audiodatasources;
  }

  private void getAndSetAudioDataSources() {
    final Files.GetSources call = new Files.GetSources(FilesModel.Media.MUSIC);
    this.audiodatasources.clear();

    send(call);

    if (call.getResults() != null && !call.getResults().isEmpty()) {
      try {
        for (ListModel.SourceItem res : call.getResults()) {
          this.audiodatasources.add(new SplitUri(res.file, res.file, res.label, connectionManager.getHostConfig().getAddress()));
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not process Kodi RPC response - '{}'", e.getMessage());
      }
    }
  }

  // -----------------------------------------------------------------------------------

  /**
   * Kodi version
   */
  public String getKodiVersion() {
    final Application.GetProperties call = new Application.GetProperties("version");
    send(call);
    try {
      ApplicationModel.PropertyValue res = call.getResult();
      int maj = res.version.major;
      int min = res.version.minor;
      return maj + "." + min;
    }
    catch (Exception ignored) {
      // just ignore
    }
    return "";
  }

  /**
   * quit remote Kodi instance
   */
  public void quitApplication() {
    final Application.Quit call = new Application.Quit();
    sendWoResponse(call);
  }

  /**
   * Toggles mute on/off
   */
  public void muteApplication() {
    final Application.GetProperties props = new Application.GetProperties("muted");
    send(props); // get current
    if (props.getResults() != null && !props.getResults().isEmpty()) {
      final Application.SetMute call = new Application.SetMute(new GlobalModel.Toggle(!props.getResult().muted));
      sendWoResponse(call); // toggle true/false
    }
  }

  /**
   * set volume 0-100
   * 
   * @param vol
   */
  public void setVolume(int vol) {
    final Application.SetVolume call = new Application.SetVolume(vol);
    sendWoResponse(call);
  }

  // -----------------------------------------------------------------------------------

  public void SystemEjectOpticalDrive() {
    final System.EjectOpticalDrive call = new System.EjectOpticalDrive();
    sendWoResponse(call);
  }

  public void SystemHibernate() {
    final System.EjectOpticalDrive call = new System.EjectOpticalDrive();
    sendWoResponse(call);
  }

  public void SystemShutdown() {
    final System.Shutdown call = new System.Shutdown();
    sendWoResponse(call);
  }

  public void SystemReboot() {
    final System.Reboot call = new System.Reboot();
    sendWoResponse(call);
  }

  public void SystemSuspend() {
    final System.Suspend call = new System.Suspend();
    sendWoResponse(call);
  }

  // -----------------------------------------------------------------------------------

  /**
   * Sends a call to Kodi and waits for the response.<br />
   * Call getResult() / getResults() afterwards
   * 
   * @param call
   *          the call to send
   */
  public void send(AbstractCall<?> call) {
    if (!isConnected()) {
      LOGGER.warn("Cannot send RPC call - not connected");
      return;
    }
    try {
      call.setResponse(JsonApiRequest.execute(connectionManager.getHostConfig(), call.getRequest()));
    }
    catch (ApiException e) {
      LOGGER.error("Error calling Kodi: {}", e.getMessage());
    }
  }

  /**
   * Sends the call to Kodi without waiting for a response (fire and forget)
   * 
   * @param call
   *          the call to send
   */
  public void sendWoResponse(AbstractCall<?> call) {
    if (!isConnected()) {
      LOGGER.warn("Cannot send RPC call - not connected");
      return;
    }

    new Thread(() -> {
      try {
        JsonApiRequest.execute(connectionManager.getHostConfig(), call.getRequest());
      }
      catch (ApiException e) {
        LOGGER.error("Error calling Kodi: {}", e.getMessage());
      }
    }).start();
  }

  /**
   * Connect to Kodi with specified TCP port
   * 
   * @param config
   *          Host configuration
   * @throws Exception
   *           Throws {@link Exception} when something goes wrong with the initialization of the API.
   */
  public void connect(HostConfig config) throws Exception {
    if (isConnected()) {
      connectionManager.disconnect();
    }

    new Thread(() -> {
      try {
        LOGGER.info("Connecting...");
        connectionManager.connect(config);

        if (isConnected()) {
          this.kodiVersion = getKodiVersion();
          getAndSetVideoDataSources();
          getAndSetAudioDataSources();
          getAndSetMovieMappings();
          getAndSetTvShowMappings();
        }
      }
      catch (Exception e) {
        LOGGER.error("Error connecting to Kodi - '{}'", e.getMessage());
      }
    }).start();
  }

  public void connect() {
    Settings s = Settings.getInstance();
    if (s.getKodiHost().isEmpty()) {
      return;
    }

    try {
      connect(new HostConfig(s.getKodiHost(), s.getKodiHttpPort(), s.getKodiTcpPort(), s.getKodiUsername(), s.getKodiPassword()));
    }
    catch (Exception cex) {
      LOGGER.error("Error connecting to Kodi instance! {}", cex.getMessage());
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "KodiRPC", "Could not connect to Kodi: " + cex.getMessage()));
    }
  }

  public void disconnect() {
    connectionManager.disconnect();
  }

  public void getDataSources() {
    Settings.getInstance();
    final Files.GetSources f = new Files.GetSources(FilesModel.Media.VIDEO); // movies + tv !!!
    connectionManager.call(f, new ApiCallback<>() {

      @Override
      public void onResponse(AbstractCall<ListModel.SourceItem> call) {
        LOGGER.info("found " + call.getResults().size() + " sources");

        LOGGER.info("--- KODI DATASOURCES ---");
        for (ListModel.SourceItem res : call.getResults()) {
          LOGGER.debug(res.file + " - " + new SplitUri(res.file, "", res.label, connectionManager.getHostConfig().getAddress()));
        }

        LOGGER.info("--- TMM DATASOURCES ---");
        for (String ds : MovieModuleManager.getInstance().getSettings().getMovieDataSource()) {
          LOGGER.info(ds + " - " + new SplitUri(ds, ""));
        }
        for (String ds : TvShowModuleManager.getInstance().getSettings().getTvShowDataSource()) {
          LOGGER.info(ds + " - " + new SplitUri(ds, ""));
        }
      }

      @Override
      public void onError(int code, String message, String hint) {
        LOGGER.error("Error {}: {}", code, message);
      }
    });
  }

  public void updateMovieMappings() {
    if (isConnected()) {
      getAndSetMovieMappings();
    }
  }

  public void updateTvShowMappings() {
    if (isConnected()) {
      getAndSetTvShowMappings();
    }
  }

  /**
   * @return json movie list or NULL
   */
  public List<MovieDetail> getAllMoviesSYNC() {
    final VideoLibrary.GetMovies call = new VideoLibrary.GetMovies(MovieFields.FILE);
    send(call);
    return call.getResults();
  }

  public void getAllMoviesASYNC() {
    // MovieFields.values.toArray(new String[0]) // all values
    final VideoLibrary.GetMovies vl = new VideoLibrary.GetMovies(MovieFields.FILE); // ID & label are always set; just add additional
    connectionManager.call(vl, new ApiCallback<>() {

      @Override
      public void onResponse(AbstractCall<MovieDetail> call) {
        LOGGER.info("found " + call.getResults().size() + " movies");
        for (MovieDetail res : call.getResults()) {
          LOGGER.debug(res.toString());
        }
      }

      @Override
      public void onError(int code, String message, String hint) {
        LOGGER.error("Error {}: {}", code, message);
      }
    });
  }

  /**
   * Forces Kodi to reload movie from NFO
   * 
   * @param movie
   */
  public void triggerReload(Movie movie) {
    // MovieFields.values.toArray(new String[0]) // all values
    final VideoLibrary.GetMovies vl = new VideoLibrary.GetMovies(MovieFields.FILE); // ID & label are always set; just add additional
    connectionManager.call(vl, new ApiCallback<>() {

      @Override
      public void onResponse(AbstractCall<MovieDetail> call) {
        LOGGER.info("found " + call.getResults().size() + " movies");
        for (MovieDetail res : call.getResults()) {
          LOGGER.debug(res.toString());
        }
      }

      @Override
      public void onError(int code, String message, String hint) {
        LOGGER.error("Error {}: {}", code, message);
      }
    });
  }

  public void getAllTvShows() {
    final VideoLibrary.GetTVShows vl = new VideoLibrary.GetTVShows();
    connectionManager.call(vl, new ApiCallback<>() {

      @Override
      public void onResponse(AbstractCall<TVShowDetail> call) {
        LOGGER.info("found " + call.getResults().size() + " shows");
        for (TVShowDetail res : call.getResults()) {
          LOGGER.debug(res.toString());
        }
      }

      @Override
      public void onError(int code, String message, String hint) {
        LOGGER.error("Error {}: {}", code, message);
      }
    });
  }
}
