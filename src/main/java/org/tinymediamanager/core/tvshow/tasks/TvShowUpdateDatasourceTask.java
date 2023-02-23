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
package org.tinymediamanager.core.tvshow.tasks;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static org.tinymediamanager.core.Utils.DISC_FOLDER_REGEX;
import static org.tinymediamanager.core.Utils.containsSkipFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractFileVisitor;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.tasks.MediaFileInformationFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser.EpisodeMatchingResult;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.connector.TvShowEpisodeNfoParser;
import org.tinymediamanager.core.tvshow.connector.TvShowNfoParser;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.thirdparty.VSMeta;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;

/**
 * The Class TvShowUpdateDataSourcesTask.
 * 
 * @author Manuel Laggner
 */

public class TvShowUpdateDatasourceTask extends TmmThreadPool {
  private static final Logger          LOGGER        = LoggerFactory.getLogger(TvShowUpdateDatasourceTask.class);

  // skip well-known, but unneeded folders (UPPERCASE)
  private static final List<String>    SKIP_FOLDERS  = Arrays.asList(".", "..", "CERTIFICATE", "$RECYCLE.BIN", "RECYCLER",
      "SYSTEM VOLUME INFORMATION", "@EADIR", "ADV_OBJ", "EXTRAS", "EXTRA", "EXTRATHUMB", "PLEX VERSIONS");

  // skip folders starting with a SINGLE "." or "._"
  private static final String          SKIP_REGEX    = "^[.][\\w@]+.*";

  private static long                  preDir        = 0;
  private static long                  postDir       = 0;
  private static long                  visFile       = 0;

  private final List<String>           dataSources;
  private final List<Pattern>          skipFolders   = new ArrayList<>();
  private final List<Path>             tvShowFolders = new ArrayList<>();
  private final TvShowList             tvShowList;
  private final Set<Path>              filesFound    = new HashSet<>();
  private final ReentrantReadWriteLock fileLock      = new ReentrantReadWriteLock();

  /**
   * Instantiates a new scrape task - to update all datasources
   * 
   */
  public TvShowUpdateDatasourceTask() {
    super(TmmResourceBundle.getString("update.datasource"));
    this.tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    this.dataSources = new ArrayList<>(TvShowModuleManager.getInstance().getSettings().getTvShowDataSource());

    init();
  }

  /**
   * Instantiates a new scrape task - to update a single datasource
   * 
   * @param datasource
   *          the data source to start the task for
   */
  public TvShowUpdateDatasourceTask(String datasource) {
    super(TmmResourceBundle.getString("update.datasource") + " (" + datasource + ")");
    this.tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    this.dataSources = Collections.singletonList(datasource);

    init();
  }

  /**
   * Instantiates a new scrape task - to update a single datasource
   *
   * @param datasources
   *          the data sources to start the task for
   */
  public TvShowUpdateDatasourceTask(Collection<String> datasources) {
    super(TmmResourceBundle.getString("update.datasource"));
    this.tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    this.dataSources = new ArrayList<>(datasources);

    init();
  }

  /**
   * Instantiates a new scrape task - to update given tv shows
   * 
   * @param tvShowFolders
   *          a list of TV show folders to start the task for
   */
  public TvShowUpdateDatasourceTask(List<Path> tvShowFolders) {
    super(TmmResourceBundle.getString("update.datasource"));
    this.tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    this.dataSources = new ArrayList<>(0);
    this.tvShowFolders.addAll(tvShowFolders);

    init();
  }

  private void init() {
    for (String skipFolder : TvShowModuleManager.getInstance().getSettings().getSkipFolder()) {
      try {
        Pattern pattern = Pattern.compile(skipFolder);
        skipFolders.add(pattern);
      }
      catch (Exception e) {
        try {
          LOGGER.debug("no valid skip pattern - '{}'", skipFolder);

          Pattern pattern = Pattern.compile(Pattern.quote(skipFolder));
          skipFolders.add(pattern);
        }
        catch (Exception ignored) {
          // just ignore
        }
      }
    }
  }

  @Override
  public void doInBackground() {
    // check if there is at least one DS to update
    Utils.removeEmptyStringsFromList(dataSources);
    if (dataSources.isEmpty() && tvShowFolders.isEmpty()) {
      LOGGER.info("no datasource to update");
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.nonespecified"));
      return;
    }

    resetCounters();

    try {
      StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      start();

      // get existing show folders
      List<Path> existing = new ArrayList<>();
      for (TvShow show : tvShowList.getTvShows()) {
        existing.add(show.getPathNIO());
      }

      // here we have 2 ways of updating:
      // - per datasource -> update ds / remove orphaned / update MFs
      // - per TV show -> udpate TV show / update MFs
      if (tvShowFolders.isEmpty()) {
        // update selected data sources
        for (String ds : dataSources) {
          // check the special case, that the data source is also an ignore folder
          if (skipFolders.contains(ds)) {
            LOGGER.debug("datasource '{}' is also a skipfolder - skipping", ds);
            continue;
          }

          LOGGER.info("Start UDS on datasource: {}", ds);
          initThreadPool(3, "update");
          setTaskName(TmmResourceBundle.getString("update.datasource") + " '" + ds + "'");
          publishState();

          Path dsAsPath = Paths.get(ds);

          // first of all check if the DS is available; we can take the
          // Files.exist here:
          // if the DS exists (and we have access to read it): Files.exist =
          // true
          if (!Files.exists(dsAsPath)) {
            // error - continue with next datasource
            LOGGER.warn("Datasource not available/empty {}", ds);
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable", new String[] { ds }));
            continue;
          }
          publishState();

          List<Path> newTvShowDirs = new ArrayList<>();
          List<Path> existingTvShowDirs = new ArrayList<>();
          List<Path> rootList = listFilesAndDirs(dsAsPath);

          // when there is _nothing_ found in the ds root, it might be offline -
          // skip further processing
          // not in Windows since that won't happen there
          if (rootList.isEmpty() && !SystemUtils.IS_OS_WINDOWS) {
            // error - continue with next datasource
            MessageManager.instance
                .pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable", new String[] { ds }));
            continue;
          }

          for (Path path : rootList) {
            if (Files.isDirectory(path)) {

              // additional datasource/A/show sub dirs!
              if (path.getFileName().toString().length() == 1) {
                List<Path> subList = listFilesAndDirs(path);
                for (Path sub : subList) {
                  if (Files.isDirectory(sub)) {
                    if (existing.contains(sub)) {
                      existingTvShowDirs.add(sub);
                    }
                    else {
                      newTvShowDirs.add(sub);
                    }
                  }
                }
              }

              // normal datasource/show folder
              else {
                if (existing.contains(path)) {
                  existingTvShowDirs.add(path);
                }
                else {
                  newTvShowDirs.add(path);
                }
              }
            }
            else {
              // File in root folder - not possible for TV datasource (at least, for videos ;)
              String ext = FilenameUtils.getExtension(path.getFileName().toString()).toLowerCase(Locale.ROOT);
              if (Settings.getInstance().getVideoFileType().contains("." + ext)) {
                MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.episodeinroot",
                    new String[] { path.getFileName().toString() }));
              }
            }
          }

          for (Path subdir : newTvShowDirs) {
            submitTask(new FindTvShowTask(subdir, dsAsPath.toAbsolutePath()));
          }
          for (Path subdir : existingTvShowDirs) {
            submitTask(new FindTvShowTask(subdir, dsAsPath.toAbsolutePath()));
          }
          waitForCompletionOrCancel();

          // print stats
          LOGGER.info("FilesFound: {}", filesFound.size());
          LOGGER.info("tvShowsFound: {}", tvShowList.getTvShowCount());
          LOGGER.info("episodesFound: {}", tvShowList.getEpisodeCount());
          LOGGER.debug("PreDir: {}", preDir);
          LOGGER.debug("PostDir: {}", postDir);
          LOGGER.debug("VisFile: {}", visFile);

          if (cancel) {
            break;
          }

          cleanupDatasource(ds);
          waitForCompletionOrCancel();
          if (cancel) {
            break;
          }
        } // end foreach datasource
      }
      else {
        initThreadPool(3, "update");
        // update selected TV shows
        for (Path path : tvShowFolders) {
          // first of all check if the DS is available; we can take the
          // Files.exist here:
          // if the DS exists (and we have access to read it): Files.exist = true
          if (!Files.exists(path)) {
            // error - continue with next datasource
            LOGGER.warn("Datasource not available/empty - {}", path.toAbsolutePath());
            MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "update.datasource.unavailable",
                new String[] { path.toAbsolutePath().toString() }));
            continue;
          }
          submitTask(new FindTvShowTask(path, path.getParent().toAbsolutePath()));
        }
        waitForCompletionOrCancel();

        // print stats
        LOGGER.info("FilesFound: {}", filesFound.size());
        LOGGER.info("tvShowsFound: {}", tvShowList.getTvShowCount());
        LOGGER.info("episodesFound: {}", tvShowList.getEpisodeCount());
        LOGGER.debug("PreDir: {}", preDir);
        LOGGER.debug("PostDir: {}", postDir);
        LOGGER.debug("VisFile: {}", visFile);

        if (!cancel) {
          cleanupShows();
          waitForCompletionOrCancel();
        }
      }

      if (cancel) {
        return;
      }

      // map Kodi entries
      if (StringUtils.isNotBlank(Settings.getInstance().getKodiHost())) {
        // call async to avoid slowdown of UDS
        TmmTaskManager.getInstance().addUnnamedTask(() -> KodiRPC.getInstance().updateTvShowMappings());
      }

      LOGGER.info("getting Mediainfo...");

      initThreadPool(1, "mediainfo");
      setTaskName(TmmResourceBundle.getString("update.mediainfo"));
      setTaskDescription(null);
      setProgressDone(0);
      // gather MediaInformation for ALL shows - TBD
      if (!cancel) {
        if (tvShowFolders.isEmpty()) {
          // get MI for selected DS
          for (int i = tvShowList.getTvShows().size() - 1; i >= 0; i--) {
            if (cancel) {
              break;
            }
            TvShow tvShow = tvShowList.getTvShows().get(i);
            if (dataSources.contains(tvShow.getDataSource())) {
              gatherMediaInformationForUngatheredMediaFiles(tvShow);
            }
          }
        }
        else {
          // get MI for selected TV shows
          for (int i = tvShowList.getTvShows().size() - 1; i >= 0; i--) {
            if (cancel) {
              break;
            }
            TvShow tvShow = tvShowList.getTvShows().get(i);
            if (tvShowFolders.contains(tvShow.getPathNIO())) {
              gatherMediaInformationForUngatheredMediaFiles(tvShow);
            }
          }
        }
        waitForCompletionOrCancel();
      }

      if (cancel) {
        return;
      }

      if (TvShowModuleManager.getInstance().getSettings().getSyncTrakt()) {
        TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
        task.setSyncCollection(TvShowModuleManager.getInstance().getSettings().getSyncTraktCollection());
        task.setSyncWatched(TvShowModuleManager.getInstance().getSettings().getSyncTraktWatched());
        task.setSyncRating(TvShowModuleManager.getInstance().getSettings().getSyncTraktRating());

        TmmTaskManager.getInstance().addUnnamedTask(task);
      }

      stopWatch.stop();
      LOGGER.info("Done updating datasource :) - took {}", stopWatch);

      resetCounters();
    }
    catch (

    Exception e) {
      LOGGER.error("Thread crashed", e);
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "update.datasource", "message.update.threadcrashed"));
    }
  }

  private void cleanupShows() {
    setTaskName(TmmResourceBundle.getString("update.cleanup"));
    setTaskDescription(null);
    setProgressDone(0);
    setWorkUnits(0);
    publishState();

    LOGGER.info("removing orphaned movies/files...");
    for (int i = tvShowList.getTvShows().size() - 1; i >= 0; i--) {
      if (cancel) {
        break;
      }
      TvShow tvShow = tvShowList.getTvShows().get(i);

      // check only Tv shows matching datasource
      if (!tvShowFolders.contains(tvShow.getPathNIO())) {
        continue;
      }

      if (!Files.exists(tvShow.getPathNIO())) {
        tvShowList.removeTvShow(tvShow);
      }
      else {
        cleanup(tvShow);
      }
    }
  }

  private void cleanupDatasource(String datasource) {
    setTaskName(TmmResourceBundle.getString("update.cleanup"));
    setTaskDescription(null);
    setProgressDone(0);
    setWorkUnits(0);
    publishState();
    LOGGER.info("removing orphaned tv shows/files...");

    for (int i = tvShowList.getTvShows().size() - 1; i >= 0; i--) {
      if (cancel) {
        break;
      }
      TvShow tvShow = tvShowList.getTvShows().get(i);

      // check only Tv shows matching datasource
      if (!Paths.get(datasource).toAbsolutePath().equals(Paths.get(tvShow.getDataSource()).toAbsolutePath())) {
        continue;
      }

      if (!Files.exists(tvShow.getPathNIO())) {
        tvShowList.removeTvShow(tvShow);
      }
      else {
        cleanup(tvShow);
      }
    }
  }

  private void cleanup(TvShow tvShow) {
    boolean dirty = false;
    if (!tvShow.isNewlyAdded() || tvShow.hasNewlyAddedEpisodes()) {
      // check and delete all not found MediaFiles
      List<MediaFile> mediaFiles = new ArrayList<>(tvShow.getMediaFiles());
      for (MediaFile mf : mediaFiles) {
        fileLock.readLock().lock();
        boolean fileFound = filesFound.contains(mf.getFileAsPath());
        fileLock.readLock().unlock();

        if (!fileFound) {
          LOGGER.debug("removing orphaned file: {}", mf.getFileAsPath());
          tvShow.removeFromMediaFiles(mf);
          // and remove the image cache
          if (mf.isGraphic()) {
            ImageCache.invalidateCachedImage(mf);

            // special handling for season artwork
            tvShow.removeSeasonArtwork(mf);
          }

          dirty = true;
        }
      }
      List<TvShowEpisode> episodes = new ArrayList<>(tvShow.getEpisodes());
      for (TvShowEpisode episode : episodes) {
        mediaFiles = new ArrayList<>(episode.getMediaFiles());
        for (MediaFile mf : mediaFiles) {
          fileLock.readLock().lock();
          boolean fileFound = filesFound.contains(mf.getFileAsPath());
          fileLock.readLock().unlock();

          if (!fileFound) {
            LOGGER.debug("removing orphaned file: {}", mf.getFileAsPath());
            episode.removeFromMediaFiles(mf);
            // and remove the image cache
            if (mf.isGraphic()) {
              ImageCache.invalidateCachedImage(mf);
            }
            dirty = true;
          }
        }
        // lets have a look if there is at least one video file for this episode
        List<MediaFile> mfs = episode.getMediaFiles(MediaFileType.VIDEO);
        if (mfs.isEmpty()) {
          tvShow.removeEpisode(episode);
          dirty = true;
        }
      }
    }

    if (dirty) {
      tvShow.saveToDb();
    }
  }

  /*
   * detect which mediafiles has to be parsed and start a thread to do that
   */
  private void gatherMediaInformationForUngatheredMediaFiles(TvShow tvShow) {
    // get mediainfo for tv show (fanart/poster..)
    for (MediaFile mf : tvShow.getMediaFiles()) {
      if (StringUtils.isBlank(mf.getContainerFormat())) {
        submitTask(new MediaFileInformationFetcherTask(mf, tvShow, false));
      }
      else {
        // // did the file dates/size change?
        if (MediaFileHelper.gatherFileInformation(mf)) {
          // okay, something changed with that movie file - force fetching mediainfo
          submitTask(new MediaFileInformationFetcherTask(mf, tvShow, true));
        }
      }
    }

    // get mediainfo for all episodes within this tv show
    for (TvShowEpisode episode : new ArrayList<>(tvShow.getEpisodes())) {
      for (MediaFile mf : episode.getMediaFiles()) {
        if (StringUtils.isBlank(mf.getContainerFormat())) {
          submitTask(new MediaFileInformationFetcherTask(mf, episode, false));
        }
        else {
          // at least update the file dates
          if (MediaFileHelper.gatherFileInformation(mf)) {
            // okay, something changed with that movie file - force fetching mediainfo
            submitTask(new MediaFileInformationFetcherTask(mf, episode, true));
          }
        }
      }
    }
  }

  /**
   * The Class FindTvShowTask.
   * 
   * @author Manuel Laggner
   */
  private class FindTvShowTask implements Callable<Object> {
    private final Path showDir;
    private final Path datasource;
    private final long uniqueId;

    /**
     * Instantiates a new find tv show task.
     * 
     * @param showDir
     *          the subdir
     * @param datasource
     *          the datasource
     */
    public FindTvShowTask(Path showDir, Path datasource) {
      this.showDir = showDir;
      this.datasource = datasource;
      this.uniqueId = TmmTaskManager.getInstance().GLOB_THRD_CNT.incrementAndGet();
    }

    @Override
    public String call() throws Exception {
      String name = Thread.currentThread().getName();
      if (!name.contains("-G")) {
        name = name + "-G0";
      }
      name = name.replaceAll("\\-G\\d+", "-G" + uniqueId);
      Thread.currentThread().setName(name);

      if (showDir.getFileName().toString().matches(SKIP_REGEX)) {
        LOGGER.debug("Skipping dir: {}", showDir);
        return "";
      }

      Set<Path> allFiles = getAllFilesRecursive(showDir, Integer.MAX_VALUE);
      if (allFiles == null || allFiles.isEmpty()) {
        LOGGER.info("skip empty directory: {}", showDir);
        return "";
      }

      if (cancel) {
        return null;
      }

      LOGGER.debug("start parsing {}", showDir);

      fileLock.writeLock().lock();
      filesFound.add(showDir.toAbsolutePath()); // our global cache
      filesFound.addAll(allFiles); // our global cache
      fileLock.writeLock().unlock();

      // convert to MFs (we need it anyways at the end)
      List<MediaFile> mfs = new ArrayList<>();
      for (Path file : allFiles) {
        if (!file.getFileName().toString().matches(SKIP_REGEX)) {
          MediaFile mf = new MediaFile(file);

          // now check posters: if the poster is in s subfolder of the TV show, we assume it is a season poster
          // otherwise just add it as generic graphic
          if (mf.getType() == MediaFileType.POSTER && !mf.getFileAsPath().getParent().equals(showDir)) {
            if (mf.getFileAsPath().getParent().getParent().equals(showDir)) {
              // a direct subfolder of the TV show dir
              mf.setType(MediaFileType.SEASON_POSTER);
            }
            else {
              // somewhere else beneath the TV show folder
              mf.setType(MediaFileType.GRAPHIC);
            }
          }

          mfs.add(mf);
        }
      }
      allFiles.clear();

      if (getMediaFiles(mfs, MediaFileType.VIDEO).isEmpty()) {
        LOGGER.info("no video file found in directory {}", showDir);
        return "";
      }

      // ******************************
      // STEP 1 - get (or create) TvShow object
      // ******************************
      TvShow tvShow = tvShowList.getTvShowByPath(showDir);

      if (tvShow != null && tvShow.isLocked()) {
        LOGGER.info("TV show '{}' found in uds, but is locked", tvShow.getPath());
        return "";
      }

      // SHOW_NFO or SEASON_BANNER
      MediaFile showNFO = new MediaFile(showDir.resolve("tvshow.nfo"), MediaFileType.NFO); // fixate
      if (tvShow == null) {
        // tvShow did not exist - try to parse a NFO file in parent folder
        if (Files.exists(showNFO.getFileAsPath())) {
          try {
            TvShowNfoParser parser = TvShowNfoParser.parseNfo(showNFO.getFileAsPath());
            tvShow = parser.toTvShow();
          }
          catch (Exception e) {
            LOGGER.warn("problem parsing NFO: {}", e.getMessage());
          }
        }
        if (tvShow == null) {
          // create new one
          tvShow = new TvShow();
        }

        if (StringUtils.isBlank(tvShow.getTitle()) || tvShow.getYear() <= 0) {
          // we have a tv show object, but without title or year; try to parse that our of the folder/filename
          String[] ty = ParserUtils.detectCleanTitleAndYear(showDir.getFileName().toString(),
              TvShowModuleManager.getInstance().getSettings().getBadWord());
          if (StringUtils.isBlank(tvShow.getTitle()) && StringUtils.isNotBlank(ty[0])) {
            tvShow.setTitle(ty[0]);
          }
          if (tvShow.getYear() <= 0 && !ty[1].isEmpty()) {
            try {
              tvShow.setYear(Integer.parseInt(ty[1]));
            }
            catch (Exception e) {
              LOGGER.trace("could not parse int: {}", e.getMessage());
            }
          }
        }

        // was NFO, but parsing exception. try to find at least imdb id within
        if ((tvShow.getImdbId().isEmpty() || tvShow.getTmdbId() == 0) && Files.exists(showNFO.getFileAsPath())) {
          try {
            String content = Utils.readFileToString(showNFO.getFileAsPath());
            String imdb = ParserUtils.detectImdbId(content);
            if (!imdb.isEmpty()) {
              LOGGER.debug("| Found IMDB id: {}", imdb);
              tvShow.setImdbId(imdb);
            }

            String tmdb = StrgUtils.substr(content, "themoviedb\\.org\\/tv\\/(\\d+)");
            if (tvShow.getTmdbId() == 0 && !tmdb.isEmpty()) {
              LOGGER.debug("| Found TMDB id: {}", tmdb);
              tvShow.setTmdbId(MetadataUtil.parseInt(tmdb, 0));
            }
          }
          catch (IOException e) {
            LOGGER.warn("| couldn't read NFO {}", showNFO);
          }
        }

        tvShow.setPath(showDir.toAbsolutePath().toString());
        tvShow.setDataSource(datasource.toString());
        tvShow.setNewlyAdded(true);
        tvShowList.addTvShow(tvShow);
      }

      // ******************************
      // STEP 2 - get all video MFs and get or create episodes
      // ******************************
      Set<Path> discFolders = new HashSet<>();
      for (MediaFile mf : getMediaFiles(mfs, MediaFileType.VIDEO)) {
        if (cancel) {
          return null;
        }

        // build an array of MFs, which might be in same episode
        List<MediaFile> epFiles = new ArrayList<>();

        if (mf.isDiscFile()) {
          // find EP root folder, and do not walk lower than showDir!
          Path discRoot = mf.getFileAsPath().toAbsolutePath(); // folder
          if (!discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
            discRoot = discRoot.getParent();
          }
          if (discFolders.contains(discRoot)) {
            // we already parsed one disc file (which adds all other videos), so
            // break here already
            continue;
          }
          discFolders.add(discRoot);
          // add all known files starting with same discRootDir
          for (MediaFile em : mfs) {
            if (em.getFileAsPath().startsWith(discRoot)) {
              if (em.getType() != MediaFileType.UNKNOWN) {
                epFiles.add(em);
              }
            }
          }
        }
        else {
          // normal episode file - get all same named files (in same directory!)
          String basename = Utils.cleanStackingMarkers(FilenameUtils.getBaseName(mf.getFilename()));
          basename = showDir.relativize(mf.getFileAsPath().getParent()) + "/" + basename;
          LOGGER.trace("UDS: basename - {}", basename);
          for (MediaFile em : mfs) {
            String emBasename = FilenameUtils.getBaseName(em.getFilename());
            emBasename = showDir.relativize(em.getFileAsPath().getParent()) + "/" + emBasename;
            String epNameRegexp = Pattern.quote(basename) + "[\\s.,_-].*";
            // same named files or thumb files
            if (emBasename.equals(basename) || emBasename.matches(epNameRegexp)) {
              // we found some graphics named like the episode - define them as thumb here
              if (em.getType() == MediaFileType.GRAPHIC) {
                em.setType(MediaFileType.THUMB);
              }
              // we found a video file which is named like the EP file?!
              // declare it as extras
              // NOOOO - we might have 2 video files!!!
              // if (!em.getFilename().equals(mf.getFilename()) && em.getType() == MediaFileType.VIDEO) {
              // em.setType(MediaFileType.EXTRA);
              // }
              epFiles.add(em);
              LOGGER.trace("UDS: found matching MF - {}", em.getFile());
            }
          }
        }

        // ******************************
        // STEP 2.1 - is this file already assigned to another episode?
        // ******************************
        List<TvShowEpisode> episodes = TvShowList.getTvEpisodesByFile(tvShow, mf.getFile());
        if (episodes.isEmpty()) {

          // ******************************
          // STEP 2.1.1 - parse EP NFO (has precedence over files)
          // ******************************

          // meta data from VSMETA files
          MediaFile meta = getMediaFile(epFiles, MediaFileType.VSMETA);
          TvShowEpisode vsMetaEP = null;
          if (meta != null) {
            VSMeta vsmeta = new VSMeta(meta.getFileAsPath());
            vsmeta.parseFile();
            vsMetaEP = vsmeta.getTvShowEpisode();
          }

          // meta data from XML files
          TvShowEpisode xmlEP = null;
          for (MediaFile xmlMf : epFiles) {
            if ("xml".equalsIgnoreCase(xmlMf.getExtension())) {
              try {
                TvShowEpisodeNfoParser nfoParser = TvShowEpisodeNfoParser.parseNfo(xmlMf.getFileAsPath());
                List<TvShowEpisode> epsInXml = nfoParser.toTvShowEpisodes();
                if (!epsInXml.isEmpty()) {
                  xmlEP = epsInXml.get(0);
                }
              }
              catch (Exception e) {
                // ignored
              }
            }
          }

          // drop all unknown EP files
          epFiles = epFiles.stream().filter(mediaFile -> mediaFile.getType() != MediaFileType.UNKNOWN).collect(Collectors.toList());

          MediaFile epNfo = getMediaFile(epFiles, MediaFileType.NFO);
          if (epNfo != null) {
            LOGGER.debug("found episode NFO - try to parse '{}'", showDir.relativize(epNfo.getFileAsPath()));
            List<TvShowEpisode> episodesInNfo = new ArrayList<>();

            try {
              TvShowEpisodeNfoParser parser = TvShowEpisodeNfoParser.parseNfo(epNfo.getFileAsPath());
              if (parser.isValidNfo()) {
                episodesInNfo.addAll(parser.toTvShowEpisodes());
              }
            }
            catch (Exception e) {
              LOGGER.debug("could not parse episode NFO: {}", e.getMessage());
            }

            // did we find any episodes in the NFO?
            if (!episodesInNfo.isEmpty()) {
              // these have priority!
              for (TvShowEpisode episode : episodesInNfo) {
                episode.setPath(mf.getPath());
                episode.setTvShow(tvShow);

                if (episode.getMediaSource() == MediaSource.UNKNOWN) {
                  episode.setMediaSource(MediaSource.parseMediaSource(mf.getFilename()));
                }
                episode.setNewlyAdded(true);

                // remember the filename the first time the movie gets added to tmm
                if (StringUtils.isBlank(episode.getOriginalFilename())) {
                  episode.setOriginalFilename(mf.getFilename());
                }

                episode.addToMediaFiles(epFiles); // all found EP MFs

                if (mf.isDiscFile()) {
                  episode.setDisc(true);

                  // set correct EP path in case of disc files
                  Path discRoot = mf.getFileAsPath().toAbsolutePath(); // folder
                  if (!discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                    discRoot = discRoot.getParent();
                  }
                  episode.setPath(discRoot.getParent().toString());
                }

                if (episodesInNfo.size() > 1) {
                  episode.setMultiEpisode(true);
                }
                else {
                  episode.setMultiEpisode(false);
                }
                episode.merge(vsMetaEP); // merge VSmeta infos
                episode.merge(xmlEP); // merge XML infos

                episode.saveToDb();
                tvShow.addEpisode(episode);
              }
              continue; // with next video MF
            }
          } // end parse NFO

          // ******************************
          // STEP 2.1.2 - no NFO? try to parse episode/season
          // ******************************
          String relativePath = showDir.relativize(mf.getFileAsPath()).toString();
          EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser.detectEpisodeFromFilenameAlternative(relativePath, tvShow.getTitle());

          // second check: is the detected episode (>-1; season >-1) already in
          // tmm and any valid stacking markers found?
          if (result.episodes.size() == 1 && result.season > -1 && result.stackingMarkerFound) {
            // get any assigned episode
            List<TvShowEpisode> eps = tvShow.getEpisode(result.season, result.episodes.get(0));
            if (!eps.isEmpty()) {
              // okay, at least one existing episode found.. just check if there is the same base name without stacking markers
              boolean found = false;
              for (TvShowEpisode ep : eps) {
                // need to call Utils.cleanStackingMarkers() because the MF stacking markers aren't detected yet
                String episodeBasenameWoStackingMarker = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(ep.getMainVideoFile().getFilename()));
                String mfBasenameWoStackingMarker = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mf.getFilename()));

                if (episodeBasenameWoStackingMarker.equals(mfBasenameWoStackingMarker)) {
                  if (ep.getMediaSource() == MediaSource.UNKNOWN) {
                    ep.setMediaSource(MediaSource.parseMediaSource(ep.getMainVideoFile().getFilename()));
                  }

                  ep.setNewlyAdded(true);

                  // remember the filename the first time the movie gets added to tmm
                  if (StringUtils.isBlank(ep.getOriginalFilename())) {
                    ep.setOriginalFilename(mf.getFilename());
                  }

                  ep.addToMediaFiles(mf);
                  found = true;
                  break;
                }
              }
              if (found) {
                continue;
              }
            }
          }
          if (!result.episodes.isEmpty()) {
            // something found with the season detection?
            for (int ep : result.episodes) {
              TvShowEpisode episode = new TvShowEpisode();
              episode.setDvdOrder(TvShowModuleManager.getInstance().getSettings().isDvdOrder());
              episode.setEpisode(ep);
              episode.setSeason(result.season);
              episode.setFirstAired(result.date);
              if (result.name.isEmpty()) {
                result.name = FilenameUtils.getBaseName(mf.getFilename());
              }
              episode.setTitle(TvShowEpisodeAndSeasonParser.cleanEpisodeTitle(result.name, tvShow.getTitle()));
              episode.setPath(mf.getPath());
              episode.setTvShow(tvShow);
              episode.addToMediaFiles(epFiles); // all found EP MFs

              // try to parse the imdb id from the filename
              if (!MediaIdUtil.isValidImdbId(episode.getImdbId())) {
                episode.setId(Constants.IMDB, ParserUtils.detectImdbId(mf.getFileAsPath().toString()));
              }
              // try to parse the Tmdb id from the filename
              if (episode.getTmdbId().isEmpty()) {
                episode.setId(Constants.TMDB, ParserUtils.detectTmdbId(mf.getFileAsPath().toString()));
              }
              if (episode.getMediaSource() == MediaSource.UNKNOWN) {
                episode.setMediaSource(MediaSource.parseMediaSource(mf.getFilename()));
              }
              episode.setNewlyAdded(true);

              // remember the filename the first time the movie gets added to tmm
              if (StringUtils.isBlank(episode.getOriginalFilename())) {
                episode.setOriginalFilename(mf.getFilename());
              }

              if (mf.isDiscFile()) {
                episode.setDisc(true);

                // set correct EP path in case of disc files
                Path discRoot = mf.getFileAsPath().toAbsolutePath(); // folder
                if (!discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                  discRoot = discRoot.getParent();
                }
                episode.setPath(discRoot.getParent().toString());
              }

              if (result.episodes.size() > 1) {
                episode.setMultiEpisode(true);
              }
              else {
                episode.setMultiEpisode(false);
              }
              episode.merge(vsMetaEP); // merge VSmeta infos

              // force title from xml
              if (xmlEP != null && StringUtils.isNotBlank(xmlEP.getTitle())) {
                episode.merge(xmlEP); // merge XML infos
                episode.setTitle(xmlEP.getTitle());
              }

              episode.saveToDb();
              tvShow.addEpisode(episode);
            }
          }
          else {
            // ******************************
            // STEP 2.1.3 - episode detection found nothing - simply add this
            // video as -1/-1
            // ******************************
            TvShowEpisode episode = new TvShowEpisode();
            episode.setDvdOrder(TvShowModuleManager.getInstance().getSettings().isDvdOrder());
            episode.setEpisode(-1);
            episode.setSeason(-1);
            episode.setPath(mf.getPath());

            if (mf.isDiscFile()) {
              episode.setDisc(true);

              // set correct EP path in case of disc files
              Path discRoot = mf.getFileAsPath().toAbsolutePath(); // folder
              if (!discRoot.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
                discRoot = discRoot.getParent();
              }
              episode.setPath(discRoot.getParent().toString());
            }

            episode.setTitle(TvShowEpisodeAndSeasonParser.cleanEpisodeTitle(FilenameUtils.getBaseName(mf.getFilename()), tvShow.getTitle()));
            episode.setTvShow(tvShow);
            episode.setFirstAired(result.date); // maybe found
            episode.addToMediaFiles(epFiles); // all found EP MFs

            // try to parse the imdb id from the filename
            if (!MediaIdUtil.isValidImdbId(episode.getImdbId())) {
              episode.setId(Constants.IMDB, ParserUtils.detectImdbId(mf.getFileAsPath().toString()));
            }
            // try to parse the Tmdb id from the filename
            if (episode.getTmdbId().isEmpty()) {
              episode.setId(Constants.TMDB, ParserUtils.detectTmdbId(mf.getFileAsPath().toString()));
            }
            if (episode.getMediaSource() == MediaSource.UNKNOWN) {
              episode.setMediaSource(MediaSource.parseMediaSource(mf.getFilename()));
            }
            episode.setNewlyAdded(true);

            // remember the filename the first time the movie gets added to tmm
            if (StringUtils.isBlank(episode.getOriginalFilename())) {
              episode.setOriginalFilename(mf.getFilename());
            }

            episode.merge(vsMetaEP); // merge VSmeta infos

            // force title from xml
            if (xmlEP != null && StringUtils.isNotBlank(xmlEP.getTitle())) {
              episode.merge(xmlEP); // merge XML infos
              episode.setTitle(xmlEP.getTitle());
            }

            episode.saveToDb();
            tvShow.addEpisode(episode);
          }
        } // end creation of new episodes
        else {
          // ******************************
          // STEP 2.2 - video MF was already found in DB - just add all
          // non-video MFs
          // ******************************
          for (TvShowEpisode episode : episodes) {
            episode.addToMediaFiles(epFiles); // add all (dupes will be
                                              // filtered)
            episode.setDisc(mf.isDiscFile());
            if (episodes.size() > 1) {
              episode.setMultiEpisode(true);
            }
            else {
              episode.setMultiEpisode(false);
            }
            episode.saveToDb();
          }
        }
      } // end for all video MFs loop

      // ******************************
      // STEP 3 - now we have a working show/episode object
      // remove all used episode MFs, rest must be show MFs ;)
      // ******************************
      mfs.removeAll(tvShow.getEpisodesMediaFiles()); // remove EP files

      // tvShow.addToMediaFiles(mfs); // add remaining
      // not so fast - try to parse S/E from remaining first!
      for (MediaFile mf : mfs) {
        // a season poster/fanart/banner/thumb does not belong to any episode
        if (mf.getType() == MediaFileType.SEASON_POSTER || mf.getType() == MediaFileType.SEASON_FANART || mf.getType() == MediaFileType.SEASON_BANNER
            || mf.getType() == MediaFileType.SEASON_THUMB) {
          continue;
        }

        String relativePath = showDir.relativize(mf.getFileAsPath()).toString();
        EpisodeMatchingResult result = TvShowEpisodeAndSeasonParser.detectEpisodeFromFilenameAlternative(relativePath, tvShow.getTitle());
        if (result.season > 0 && !result.episodes.isEmpty()) {
          for (int epnr : result.episodes) {
            // get any assigned episode
            List<TvShowEpisode> eps = tvShow.getEpisode(result.season, epnr);
            if (eps.size() == 1) {
              // just one episode for that S/E found -> we can blindly assign it
              eps.get(0).addToMediaFiles(mf);
            }
            else if (eps.size() > 1) {
              for (TvShowEpisode ep : eps) {
                String episodeBasenameWoStackingMarker = FilenameUtils.getBaseName(Utils.cleanStackingMarkers(ep.getMainVideoFile().getFilename()));
                // okay, at least one existing episode found.. just check if there is the same base name without stacking markers
                if (FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mf.getFilename())).startsWith(episodeBasenameWoStackingMarker)) {
                  ep.addToMediaFiles(mf);
                  break;
                }
                // or if the mf is in a subfolder with the base name of the video file
                if (episodeBasenameWoStackingMarker.equals(mf.getFileAsPath().getParent().getFileName().toString())) {
                  ep.addToMediaFiles(mf);
                  break;
                }
              }
            }
          }
        }
      }
      mfs.removeAll(tvShow.getEpisodesMediaFiles()); // remove EP files
      tvShow.addToMediaFiles(mfs); // now add remaining

      // fill season artwork map
      for (MediaFile mf : getMediaFiles(mfs, MediaFileType.SEASON_POSTER, MediaFileType.SEASON_FANART, MediaFileType.SEASON_BANNER,
          MediaFileType.SEASON_THUMB)) {
        try {
          String foldername = tvShow.getPathNIO().relativize(mf.getFileAsPath().getParent()).toString();
          int season = TvShowHelpers.detectSeasonFromFileAndFolder(mf.getFilename(), foldername);
          if (season == Integer.MIN_VALUE) {
            throw new IllegalStateException("did not find a season number");
          }
          else {
            LOGGER.debug("found {} - {}", mf.getType(), mf.getFileAsPath());
            tvShow.setSeasonArtwork(season, mf);
          }
        }
        catch (Exception e) {
          LOGGER.warn("could not parse season number: {} MF: {}", e.getMessage(), mf.getFileAsPath().toAbsolutePath());
        }
      }

      // re-evaluate stacking markers & disc folders
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        episode.reEvaluateDiscfolder();
        episode.reEvaluateStacking();
        episode.saveToDb();
      }

      // if there is missing artwork AND we do have a VSMETA file, we probably can extract an artwork from there
      if (!TvShowModuleManager.getInstance().getSettings().isExtractArtworkFromVsmeta()) {
        // TV show
        boolean missingTvShowPosters = tvShow.getMediaFiles(MediaFileType.POSTER).isEmpty();
        boolean missingTvShowFanarts = tvShow.getMediaFiles(MediaFileType.FANART).isEmpty();

        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          List<MediaFile> episodeVsmetas = episode.getMediaFiles(MediaFileType.VSMETA);
          if (episodeVsmetas.isEmpty()) {
            continue;
          }

          if (episode.getMediaFiles(MediaFileType.THUMB).isEmpty()
              && !TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames().isEmpty()) {
            LOGGER.debug("extracting episode THUMBs from VSMETA for {}", episode.getMainFile().getFileAsPath());
            boolean ok = TvShowArtworkHelper.extractArtworkFromVsmeta(episode, episodeVsmetas.get(0), MediaArtwork.MediaArtworkType.THUMB);
            if (ok) {
              episode.saveToDb();
            }
          }

          if (missingTvShowFanarts && !TvShowModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()) {
            LOGGER.debug("extracting TV show FANARTs from VSMETA for {}", episode.getMainFile().getFileAsPath());
            boolean ok = TvShowArtworkHelper.extractArtworkFromVsmeta(tvShow, episodeVsmetas.get(0), MediaArtwork.MediaArtworkType.BACKGROUND);
            if (ok) {
              missingTvShowFanarts = false;
            }
          }

          if (missingTvShowPosters && !TvShowModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()) {
            LOGGER.debug("extracting TV show POSTERs from VSMETA for {}", episode.getMainFile().getFileAsPath());
            boolean ok = TvShowArtworkHelper.extractArtworkFromVsmeta(tvShow, episodeVsmetas.get(0), MediaArtwork.MediaArtworkType.POSTER);
            if (ok) {
              missingTvShowPosters = false;
            }
          }
        }
      }

      tvShow.saveToDb();

      return showDir.getFileName().toString();
    }

    /**
     * gets mediaFile of specific type
     *
     * @param mfs
     *          the MF list to search
     * @param types
     *          the MediaFileTypes
     * @return MF or NULL
     */
    private MediaFile getMediaFile(List<MediaFile> mfs, MediaFileType... types) {
      MediaFile mf = null;
      for (MediaFile mediaFile : mfs) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (match) {
          mf = new MediaFile(mediaFile);
        }
      }
      return mf;
    }

    /**
     * gets all mediaFiles of specific type
     *
     * @param mfs
     *          the MF list to search
     * @param types
     *          the MediaFileTypes
     * @return list of matching MFs
     */
    private List<MediaFile> getMediaFiles(List<MediaFile> mfs, MediaFileType... types) {
      List<MediaFile> mf = new ArrayList<>();
      for (MediaFile mediaFile : mfs) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (match) {
          mf.add(new MediaFile(mediaFile));
        }
      }
      return mf;
    }

    /**
     * returns all MFs NOT matching specified type
     *
     * @param mfs
     *          array to search
     * @param types
     *          MF types to exclude
     * @return list of matching MFs
     */
    private List<MediaFile> getMediaFilesExceptType(List<MediaFile> mfs, MediaFileType... types) {
      List<MediaFile> mf = new ArrayList<>();
      for (MediaFile mediaFile : mfs) {
        boolean match = false;
        for (MediaFileType type : types) {
          if (mediaFile.getType().equals(type)) {
            match = true;
          }
        }
        if (!match) {
          mf.add(new MediaFile(mediaFile));
        }
      }
      return mf;
    }

    /**
     * gets all files recursive
     * 
     * @param path
     *          the folder to search for
     * @param deep
     *          how much levels to search for
     * @return a {@link Set} of all found {@link Path}s
     */
    private Set<Path> getAllFilesRecursive(Path path, int deep) {
      Path folder = path.toAbsolutePath();
      AllFilesRecursive visitor = new AllFilesRecursive();
      try {
        Files.walkFileTree(folder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), deep, visitor);
      }
      catch (IOException e) {
        // can not happen, since we've overridden visitFileFailed, which throws no exception ;)
      }
      return visitor.fFound;
    }
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the
    // text is never right
    publishState(progressDone);
  }

  /**
   * simple NIO File.listFiles() replacement<br>
   * returns all files & folders in specified dir (NOT recursive)
   *
   * @param directory
   *          the folder to list the items for
   * @return list of files&folders
   */
  private List<Path> listFilesAndDirs(Path directory) {
    List<Path> fileNames = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
      for (Path path : directoryStream) {
        if (isInSkipFolder(path)) {
          LOGGER.debug("Skipping: {}", path);
        }
        else {
          fileNames.add(path.toAbsolutePath());
        }
      }
    }
    catch (IOException e) {
      LOGGER.error("error on listFilesAndDirs", e);
      LOGGER.debug("falling back to the alternate coding");
      fileNames = listFilesAndDirs2(directory);
    }
    return fileNames;
  }

  /**
   * check if the given folder is a skip folder
   *
   * @param dir
   *          the folder to check
   * @return true/false
   */
  private boolean isInSkipFolder(Path dir) {
    if (dir == null || dir.getFileName() == null) {
      return false;
    }

    String dirName = dir.getFileName().toString();
    String dirNameUppercase = dirName.toUpperCase(Locale.ROOT);
    String fullPath = dir.toAbsolutePath().toString();

    // hard coded skip folders
    if (SKIP_FOLDERS.contains(dirNameUppercase) || dirName.matches(SKIP_REGEX)) {
      return true;
    }

    // skip folders from regexp
    for (Pattern pattern : skipFolders) {
      Matcher matcher = pattern.matcher(dirName);
      if (matcher.matches()) {
        return true;
      }

      // maybe the regexp is a full path
      if (pattern.toString().replace("\\Q", "").replace("\\E", "").equals(fullPath)) {
        return true;
      }
    }

    return false;
  }

  /**
   * simple NIO File.listFiles() replacement<br>
   * returns all folders in specified dir (NOT recursive)
   * 
   * @param directory
   *          the folder to list the items for
   * @return list of files&folders
   */
  private List<Path> listFilesAndDirs2(Path directory) {
    List<Path> fileNames = new ArrayList<>();
    try (Stream<Path> directoryStream = Files.walk(directory, 1, FileVisitOption.FOLLOW_LINKS)) {
      List<Path> allElements = directoryStream.filter(Files::isDirectory).collect(Collectors.toList());
      for (Path path : allElements) {
        if (directory.toAbsolutePath().equals(path.toAbsolutePath())) {
          continue;
        }
        String fn = path.getFileName().toString().toUpperCase(Locale.ROOT);
        if (!SKIP_FOLDERS.contains(fn) && !fn.matches(SKIP_REGEX) && !skipFolders.contains(path.toFile().getAbsolutePath())) {
          fileNames.add(path.toAbsolutePath());
        }
        else {
          LOGGER.debug("Skipping: {}", path);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("error on listFilesAndDirs2", e);
    }
    return fileNames;
  }

  private class AllFilesRecursive extends AbstractFileVisitor {
    private final Set<Path> fFound = new HashSet<>();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      if (cancel) {
        return TERMINATE;
      }

      incVisFile();

      if (file.getFileName() == null) {
        return CONTINUE;
      }

      String filename = file.getFileName().toString();
      String path = "";
      if (file.getParent() != null && file.getParent().getFileName() != null) {
        path = file.getParent().getFileName().toString();
      }

      // in a disc folder we only accept NFO files
      if (Utils.isRegularFile(attr) && path.matches(DISC_FOLDER_REGEX)) {
        if (FilenameUtils.getExtension(filename).equalsIgnoreCase("nfo")) {
          fFound.add(file.toAbsolutePath());
        }
        return CONTINUE;
      }

      // check if we're in dirty disc folder
      if (MediaFileHelper.isMainDiscIdentifierFile(filename)) {
        fFound.add(file.toAbsolutePath());
        return CONTINUE;
      }

      if (Utils.isRegularFile(attr) && !filename.matches(SKIP_REGEX)) {
        fFound.add(file.toAbsolutePath());
        return CONTINUE;
      }

      return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      if (cancel) {
        return TERMINATE;
      }

      incPreDir();

      // getFilename returns null on DS root!
      if (dir.getFileName() != null && (isInSkipFolder(dir) || containsSkipFile(dir))) {
        LOGGER.debug("Skipping dir: {}", dir);
        return SKIP_SUBTREE;
      }

      // add the disc folder itself (clean disc folder)
      if (dir.getFileName() != null && dir.getFileName().toString().matches(DISC_FOLDER_REGEX)) {
        fFound.add(dir.toAbsolutePath());
        return CONTINUE;
      }

      // don't go below a disc folder
      if (dir.getParent() != null && dir.getParent().getFileName() != null && dir.getParent().getFileName().toString().matches(DISC_FOLDER_REGEX)) {
        return SKIP_SUBTREE;
      }

      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      if (cancel) {
        return TERMINATE;
      }

      incPostDir();

      return CONTINUE;
    }
  }

  private static void resetCounters() {
    visFile = 0;
    preDir = 0;
    postDir = 0;
  }

  /**
   * synchronized increment of visFile
   */
  private static synchronized void incVisFile() {
    visFile++;
  }

  /**
   * synchronized increment of preDir
   */
  private static synchronized void incPreDir() {
    preDir++;
  }

  /**
   * synchronized increment of postDir
   */
  private static synchronized void incPostDir() {
    postDir++;
  }
}
