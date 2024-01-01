/*
 * Copyright 2012 - 2024 Manuel Laggner
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
package org.tinymediamanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.TmmUILayoutStore;

/**
 * The class UpdateTasks. To perform needed update tasks
 *
 * @author Manuel Laggner / Myron Boyle
 */
public class UpgradeTasks {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeTasks.class);

  private static String       oldVersion;

  private UpgradeTasks() {
    throw new IllegalAccessError();
  }

  public static void setOldVersion() {
    oldVersion = Settings.getInstance().getVersion();
  }

  public static String getOldVersion() {
    return oldVersion;
  }

  public static boolean isNewVersion() {
    return StrgUtils.compareVersion(oldVersion, ReleaseInfo.getVersion()) == 0;
  }

  public static void performUpgradeTasksBeforeDatabaseLoading() {
    String v = "" + oldVersion;
    if (StringUtils.isBlank(v)) {
      v = "5.0"; // set version for other updates
    }

    // ****************************************************
    // PLEASE MAKE THIS TO RUN MULTIPLE TIMES WITHOUT ERROR
    // NEEDED FOR NIGHTLY SNAPSHOTS ET ALL
    // SVN BUILD IS ALSO CONSIDERED AS LOWER !!!
    // ****************************************************

    if (StrgUtils.compareVersion(v, "5.0") < 0) {
      LOGGER.info("Performing upgrade tasks to version 5.0");
      // migrate wrong launcher-extra.yml
      Path wrongExtra = Paths.get(Globals.DATA_FOLDER, LauncherExtraConfig.LAUNCHER_EXTRA_YML);
      if (Files.exists(wrongExtra)) {
        Path correctExtra = Paths.get(Globals.CONTENT_FOLDER, LauncherExtraConfig.LAUNCHER_EXTRA_YML);
        try {
          Files.move(wrongExtra, correctExtra, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
          LOGGER.warn("Could not move launcher-extra.yml from {} to {}", wrongExtra, correctExtra);
        }
      }

      // remove LOGO from check artwork
      MovieModuleManager.getInstance().getSettings().removeMovieCheckArtwork(MovieScraperMetadataConfig.LOGO);
      TvShowModuleManager.getInstance().getSettings().removeTvShowCheckArtwork(TvShowScraperMetadataConfig.LOGO);
    }
  }

  /**
   * Each DB version can only be executed once!<br>
   * Do not make changes to existing versions, use a new number!
   */
  public static void performDbUpgradesForMovies() {
    MovieModuleManager module = MovieModuleManager.getInstance();
    MovieList movieList = module.getMovieList();
    if (module.getDbVersion() == 0) {
      module.setDbVersion(5000);
    }
    LOGGER.info("Current movie DB version: {}", module.getDbVersion());

    if (module.getDbVersion() < 5001) {
      LOGGER.info("performing upgrade to ver: {}", 5001);
      for (Movie movie : movieList.getMovies()) {
        // migrate logo to clearlogo
        for (MediaFile mf : movie.getMediaFiles(MediaFileType.LOGO)) {
          // remove
          movie.removeFromMediaFiles(mf);

          // change type
          mf.setType(MediaFileType.CLEARLOGO);

          // and add ad the end
          movie.addToMediaFiles(mf);
        }

        String logoUrl = movie.getArtworkUrl(MediaFileType.LOGO);
        if (StringUtils.isNotBlank(logoUrl)) {
          movie.removeArtworkUrl(MediaFileType.LOGO);
          String clearlogoUrl = movie.getArtworkUrl(MediaFileType.CLEARLOGO);
          if (StringUtils.isBlank(clearlogoUrl)) {
            movie.setArtworkUrl(logoUrl, MediaFileType.CLEARLOGO);
          }
        }

        movie.saveToDb();
      }

      module.setDbVersion(5001);
    }
  }

  /**
   * Each DB version can only be executed once!<br>
   * Do not make changes to existing versions, use a new number!
   */
  public static void performDbUpgradesForShows() {
    TvShowModuleManager module = TvShowModuleManager.getInstance();
    TvShowList tvShowList = module.getTvShowList();
    if (module.getDbVersion() == 0) {
      module.setDbVersion(5000);
    }
    LOGGER.info("Current tvshow DB version: {}", module.getDbVersion());

    if (module.getDbVersion() < 5001) {
      LOGGER.info("performing upgrade to ver: {}", 5001);
      for (TvShow tvShow : tvShowList.getTvShows()) {
        // migrate logo to clearlogo
        for (MediaFile mf : tvShow.getMediaFiles(MediaFileType.LOGO)) {
          // remove
          tvShow.removeFromMediaFiles(mf);

          // change type
          mf.setType(MediaFileType.CLEARLOGO);

          // and add ad the end
          tvShow.addToMediaFiles(mf);
        }

        String logoUrl = tvShow.getArtworkUrl(MediaFileType.LOGO);
        if (StringUtils.isNotBlank(logoUrl)) {
          tvShow.removeArtworkUrl(MediaFileType.LOGO);
          String clearlogoUrl = tvShow.getArtworkUrl(MediaFileType.CLEARLOGO);
          if (StringUtils.isBlank(clearlogoUrl)) {
            tvShow.setArtworkUrl(logoUrl, MediaFileType.CLEARLOGO);
          }
        }

        // migrate season artwork to the seasons
        List<MediaFile> seasonMediaFiles = tvShow.getMediaFiles(MediaFileType.SEASON_POSTER, MediaFileType.SEASON_BANNER, MediaFileType.SEASON_THUMB,
            MediaFileType.SEASON_FANART);
        for (MediaFile mf : seasonMediaFiles) {
          if (mf.getFilesize() != 0) {
            String foldername = tvShow.getPathNIO().relativize(mf.getFileAsPath().getParent()).toString();
            int season = TvShowHelpers.detectSeasonFromFileAndFolder(mf.getFilename(), foldername);

            if (season != Integer.MIN_VALUE) {
              TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(season);
              tvShowSeason.addToMediaFiles(mf);
            }
          }
          tvShow.removeFromMediaFiles(mf);
        }

        // link TV shows and seasons once again
        for (TvShowSeason season : tvShow.getSeasons()) {
          season.setTvShow(tvShow);
        }

        // save episodes (they are migrated while loading from database)
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          episode.saveToDb();
        }

        tvShow.saveToDb();
      }
      module.setDbVersion(5001);
    }

    // migrating EpisodeGroups from V4 / nightly V5
    if (module.getDbVersion() < 5002) {
      LOGGER.info("performing upgrade to ver: {}", 5002);
      for (TvShow tvShow : tvShowList.getTvShows()) {
        if (tvShow.getEpisodeGroup() == null || (tvShow.getEpisodeGroup() != null && tvShow.getEpisodeGroup().getEpisodeGroupType() == null)) {
          // v4 empty / old v5 - cannot read
          tvShow.setEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);
        }

        tvShow.getEpisodes().forEach(TvShowEpisode::saveToDb); // re-save
        tvShow.saveToDb();
      } // end foreach show

      module.setDbVersion(5002);
    }

    // if (module.getDbVersion() < 50xx) {
    // LOGGER.info("performing upgrade to ver: {}", 50xx);
    //
    // module.setDbVersion(50xx);
    // }
  }

  public static void upgradeEpisodeNumbers(TvShowEpisode episode) {
    // create season and EGs, if we read it in "old" style
    if (!episode.additionalProperties.isEmpty() && episode.getEpisodeNumbers().isEmpty()) {
      // V4 style
      int s = MetadataUtil.parseInt(episode.additionalProperties.get("season"), -2);
      int e = MetadataUtil.parseInt(episode.additionalProperties.get("episode"), -2);
      if (s > -2 && e > -2) {
        // also record -1/-1 episodes
        episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, s, e));
      }

      s = MetadataUtil.parseInt(episode.additionalProperties.get("dvdSeason"), -1);
      e = MetadataUtil.parseInt(episode.additionalProperties.get("dvdEpisode"), -1);
      if (s > -1 && e > -1) {
        episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DVD, s, e));
      }

      s = MetadataUtil.parseInt(episode.additionalProperties.get("displaySeason"), -1);
      e = MetadataUtil.parseInt(episode.additionalProperties.get("displayEpisode"), -1);
      if (s > -1 && e > -1) {
        episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DISPLAY, s, e));
      }
    }
  }

  /**
   * copy over data/settings from v4
   * 
   * @param path
   *          the path to the v4 data folder
   */
  public static void copyV4Data(Path path) {
    // close tmm internals
    TinyMediaManager.shutdown();

    // remove shutdown hook
    TmmUILayoutStore.getInstance().setSkipSaving(true);

    // data
    File[] files = path.toFile().listFiles();
    if (files != null) {
      for (File file : files) {
        try {
          Utils.copyFileSafe(file.toPath(), Paths.get(Globals.DATA_FOLDER, file.getName()), true);
        }
        catch (Exception e) {
          LOGGER.warn("could not copy file '{}' from v4 - '{}'", file.getName(), e.getMessage());
        }
      }
    }

    // try /cache too
    Path cache = path.getParent().resolve("cache");
    if (cache.toFile().exists() && cache.toFile().isDirectory()) {
      files = cache.toFile().listFiles();
      if (files != null) {
        for (File file : files) {
          try {
            if (file.isFile()) {
              Utils.copyFileSafe(file.toPath(), Paths.get(Globals.CACHE_FOLDER, file.getName()), true);
            }
            else if (file.isDirectory()) {
              Utils.copyDirectoryRecursive(file.toPath(), Paths.get(Globals.CACHE_FOLDER, file.getName()));
            }
          }
          catch (Exception e) {
            LOGGER.warn("could not copy file '{}' from v4 - '{}'", file.getName(), e.getMessage());
          }
        }
      }
    }

    // and copy over the launcher-extra.yml
    Path launcherExtra = path.getParent().resolve("launcher-extra.yml");
    if (launcherExtra.toFile().exists() && launcherExtra.toFile().isFile()) {
      try {
        Utils.copyFileSafe(launcherExtra, Paths.get(Globals.DATA_FOLDER, launcherExtra.toFile().getName()), true);
      }
      catch (Exception e) {
        LOGGER.warn("could not copy file '{}' from v4 - '{}'", launcherExtra.toFile().getName(), e.getMessage());
      }
    }

    // spawn our process
    ProcessBuilder pb = TmmOsUtils.getPBforTMMrestart();
    try {
      LOGGER.info("Going to execute: {}", pb.command());
      pb.start();
    }
    catch (Exception e) {
      LOGGER.error("Cannot spawn process:", e);
    }

    TinyMediaManager.shutdownLogger();

    System.exit(0);
  }
}
