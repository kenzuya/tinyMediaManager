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
package org.tinymediamanager.core.tvshow;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.CustomNullStringSerializerProvider;
import org.tinymediamanager.core.ITmmModule;
import org.tinymediamanager.core.NullKeySerializer;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The class TvShowModuleManager. Used to manage the tv show module
 * 
 * @author Manuel Laggner
 */
public class TvShowModuleManager implements ITmmModule {

  public static final TvShowSettings   SETTINGS     = TvShowSettings.getInstance();

  private static final String          MODULE_TITLE = "TV show management";
  private static final String          TV_SHOW_DB   = "tvshows.db";
  private static final Logger          LOGGER       = LoggerFactory.getLogger(TvShowModuleManager.class);
  private static final int             COMMIT_DELAY = 2000;

  private static TvShowModuleManager   instance;

  private final List<String>           startupMessages;
  private final Map<MediaEntity, Long> pendingChanges;
  private final ReentrantReadWriteLock lock;

  private boolean                      enabled;
  private MVStore                      mvStore;
  private ObjectWriter                 tvShowObjectWriter;
  private ObjectReader                 tvShowObjectReader;
  private ObjectWriter                 episodeObjectWriter;
  private ObjectReader                 episodeObjectReader;

  private MVMap<UUID, String>          tvShowMap;
  private MVMap<UUID, String>          episodeMap;

  private Timer                        databaseTimer;

  private TvShowModuleManager() {
    enabled = false;
    startupMessages = new ArrayList<>();
    pendingChanges = new HashMap<>();
    lock = new ReentrantReadWriteLock();
  }

  public static TvShowModuleManager getInstance() {
    if (instance == null) {
      instance = new TvShowModuleManager();
    }
    return instance;
  }

  @Override
  public String getModuleTitle() {
    return MODULE_TITLE;
  }

  @Override
  public void startUp() {
    // configure JSON
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
    objectMapper.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
    objectMapper.configure(MapperFeature.AUTO_DETECT_SETTERS, false);
    objectMapper.configure(MapperFeature.AUTO_DETECT_FIELDS, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setTimeZone(TimeZone.getDefault());
    objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
    objectMapper.setSerializerProvider(new CustomNullStringSerializerProvider());
    objectMapper.getSerializerProvider().setNullKeySerializer(new NullKeySerializer());

    tvShowObjectWriter = objectMapper.writerFor(TvShow.class);
    tvShowObjectReader = objectMapper.readerFor(TvShow.class);
    episodeObjectWriter = objectMapper.writerFor(TvShowEpisode.class);
    episodeObjectReader = objectMapper.readerFor(TvShowEpisode.class);

    // open database
    openDatabaseAndLoadTvShows();
    enabled = true;

    TimerTask databaseWriteTask = new TimerTask() {
      @Override
      public void run() {
        writePendingChanges();
      }
    };
    databaseTimer = new Timer();
    databaseTimer.schedule(databaseWriteTask, COMMIT_DELAY, COMMIT_DELAY);
  }

  /**
   * open the database<BR/>
   * 1. try to open the actual one<BR/>
   * 2. try to open from backups<BR/>
   * 3. open a new one
   */
  private void openDatabaseAndLoadTvShows() {
    Path databaseFile = Paths.get(Globals.settings.getSettingsFolder(), TV_SHOW_DB);
    try {
      loadDatabase(databaseFile);
      return;
    }
    catch (Exception e) {
      // look if the file is locked by another process (rethrow rather than delete the db file)
      if (e instanceof IllegalStateException && e.getMessage().contains("file is locked")) {
        throw e;
      }

      if (mvStore != null && !mvStore.isClosed()) {
        mvStore.close();
      }
      LOGGER.error("Could not open database file: {}", e.getMessage());
    }

    try {
      Utils.deleteFileSafely(Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
      Utils.moveFileSafe(databaseFile, Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
    }
    catch (Exception e) {
      LOGGER.error("Could not move corrupted database to '{}' - '{}", TV_SHOW_DB + ".corrupted", e.getMessage());
    }

    LOGGER.info("try to restore the database from the backups");

    // get backups
    List<Path> backups = Utils.listFiles(Paths.get(Globals.BACKUP_FOLDER));
    backups.sort(Comparator.reverseOrder());

    // load movies.db from the backup
    boolean first = true;
    for (Path backup : backups) {
      if (!backup.getFileName().toString().startsWith("data.")) {
        continue;
      }

      // but not the first one which contains the damaged database
      if (first) {
        first = false;
        continue;
      }

      try {
        Utils.unzipFile(backup, Paths.get("/", "data", TV_SHOW_DB), databaseFile);
        loadDatabase(databaseFile);
        startupMessages.add(TmmResourceBundle.getString("tvshow.loaddb.failed.restore"));

        return;
      }
      catch (Exception e) {
        if (mvStore != null && !mvStore.isClosed()) {
          mvStore.close();
        }
        LOGGER.error("Could not open database file from backup: {}", e.getMessage());
      }
    }

    LOGGER.info("starting over with an empty database file");

    try {
      Utils.deleteFileSafely(databaseFile);
      loadDatabase(databaseFile);
      startupMessages.add(TmmResourceBundle.getString("tvshow.loaddb.failed"));
    }
    catch (Exception e1) {
      LOGGER.error("could not move old database file and create a new one: {}", e1.getMessage());
    }
  }

  private void loadDatabase(Path databaseFile) {
    Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
      private int counter = 0;

      @Override
      public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof IllegalStateException) {
          // wait up to 10 times, then try to recover
          if (counter < 10) {
            counter++;
            return;
          }

          LOGGER.error("database corruption detected - try to recover");

          // try to in-memory fix the DB
          mvStore.close();

          try {
            Utils.deleteFileSafely(Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
            Utils.moveFileSafe(databaseFile, Paths.get(Globals.BACKUP_FOLDER, TV_SHOW_DB + ".corrupted"));
          }
          catch (Exception e1) {
            LOGGER.error("Could not move corrupted database to '{}' - '{}", TV_SHOW_DB + ".corrupted", e1.getMessage());
          }

          mvStore = new MVStore.Builder().fileName(databaseFile.toString())
              .compressHigh()
              .autoCommitBufferSize(512)
              .backgroundExceptionHandler(this)
              .open();
          mvStore.setAutoCommitDelay(2000); // 2 sec
          mvStore.setRetentionTime(0);
          mvStore.setReuseSpace(true);
          mvStore.setCacheSize(8);

          tvShowMap = mvStore.openMap("tvshows");
          episodeMap = mvStore.openMap("episodes");

          for (TvShow tvShow : TvShowList.getInstance().getTvShows()) {
            persistTvShow(tvShow);

            for (TvShowEpisode episode : tvShow.getEpisodes()) {
              persistEpisode(episode);
            }
          }

          counter = 0;
        }
      }
    };

    mvStore = new MVStore.Builder().fileName(databaseFile.toString())
        .compressHigh()
        .autoCommitBufferSize(512)
        .backgroundExceptionHandler(exceptionHandler)
        .open();
    mvStore.setAutoCommitDelay(2000); // 2 sec
    mvStore.setRetentionTime(0);
    mvStore.setReuseSpace(true);
    mvStore.setCacheSize(8);

    tvShowMap = mvStore.openMap("tvshows");
    episodeMap = mvStore.openMap("episodes");

    TvShowList.getInstance().loadTvShowsFromDatabase(tvShowMap);
    TvShowList.getInstance().loadEpisodesFromDatabase(episodeMap);
    TvShowList.getInstance().initDataAfterLoading();
  }

  @Override
  public synchronized void shutDown() throws Exception {
    if (!isEnabled()) {
      return;
    }

    enabled = false;

    databaseTimer.cancel();

    // write pending changes
    if (mvStore != null && !mvStore.isClosed()) {
      writePendingChanges(true);

      mvStore.compactMoveChunks();
      mvStore.close();
    }

    if (Globals.settings.isDeleteTrashOnExit()) {
      for (String ds : SETTINGS.getTvShowDataSource()) {
        Path file = Paths.get(ds, Constants.BACKUP_FOLDER);
        Utils.deleteDirectoryRecursive(file);
      }
    }
  }

  private void writePendingChanges() {
    writePendingChanges(false);
  }

  private synchronized void writePendingChanges(boolean force) {
    if (force) {
      // force write - wait until the lock is released
      lock.writeLock().lock();
    }
    else {
      // lock if there is no other task running
      if (!lock.writeLock().tryLock()) {
        return;
      }
    }

    try {
      Map<MediaEntity, Long> pending = new HashMap<>(pendingChanges);

      long now = System.currentTimeMillis();

      for (Map.Entry<MediaEntity, Long> entry : pending.entrySet()) {
        if (force || entry.getValue() < (now - COMMIT_DELAY)) {
          try {
            if (entry.getKey() instanceof TvShow) {
              // store TV show
              TvShow tvShow = (TvShow) entry.getKey();

              // only diffs
              String oldValue = tvShowMap.get(tvShow.getDbId());
              String newValue = tvShowObjectWriter.writeValueAsString(tvShow);
              if (!StringUtils.equals(oldValue, newValue)) {
                tvShowMap.put(tvShow.getDbId(), newValue);
              }
            }
            else if (entry.getKey() instanceof TvShowEpisode) {
              // store episode
              TvShowEpisode episode = (TvShowEpisode) entry.getKey();

              // only diffs
              String oldValue = episodeMap.get(episode.getDbId());
              String newValue = episodeObjectWriter.writeValueAsString(episode);
              if (!StringUtils.equals(oldValue, newValue)) {
                episodeMap.put(episode.getDbId(), newValue);
              }
            }
          }
          catch (Exception e) {
            LOGGER.warn("could not store '{}' - '{}'", entry.getKey().getClass().getName(), e.getMessage());
          }
          finally {
            pendingChanges.remove(entry.getKey());
          }
        }
      }
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * dumps a whole TV show to logfile
   * 
   * @param tvshow
   *          the TV show to dump the data for
   */
  public void dump(TvShow tvshow) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode node = mapper.readValue(tvShowMap.get(tvshow.getDbId()), ObjectNode.class);

      ArrayNode episodes = JsonNodeFactory.instance.arrayNode();
      for (TvShowEpisode ep : tvshow.getEpisodes()) {
        ObjectNode epNode = mapper.readValue(episodeMap.get(ep.getDbId()), ObjectNode.class);
        episodes.add(epNode);
        // TODO: dump EP IDs !!!
      }
      node.set("episodes", episodes);

      String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
      LOGGER.info("Dumping TvShow: {}\n{}", tvshow.getDbId(), s);
    }
    catch (Exception e) {
      LOGGER.error("Cannot parse JSON!", e);
    }
  }

  void persistTvShow(TvShow tvShow) {
    // write movie to DB
    try {
      lock.writeLock().lock();
      pendingChanges.put(tvShow, System.currentTimeMillis());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void removeTvShowFromDb(TvShow tvShow) {
    try {
      lock.writeLock().lock();
      pendingChanges.remove(tvShow);
      tvShowMap.remove(tvShow.getDbId());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void persistEpisode(TvShowEpisode episode) {
    try {
      lock.writeLock().lock();
      pendingChanges.put(episode, System.currentTimeMillis());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  void removeEpisodeFromDb(TvShowEpisode episode) {
    try {
      lock.writeLock().lock();
      pendingChanges.remove(episode);
      episodeMap.remove(episode.getDbId());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void initializeDatabase() {
    Utils.deleteFileSafely(Paths.get(Settings.getInstance().getSettingsFolder(), TV_SHOW_DB));
  }

  @Override
  public void saveSettings() {
    SETTINGS.saveSettings();
  }

  @Override
  public List<String> getStartupMessages() {
    return startupMessages;
  }

  public ObjectReader getTvShowObjectReader() {
    return tvShowObjectReader;
  }

  public ObjectReader getEpisodeObjectReader() {
    return episodeObjectReader;
  }
}
