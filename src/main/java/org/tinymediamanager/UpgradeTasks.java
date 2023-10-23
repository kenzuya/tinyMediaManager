/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieExtraFanartNaming;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowExtraFanartNaming;
import org.tinymediamanager.scraper.MediaMetadata;
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
      v = "4.0"; // set version for other updates
    }

    // ****************************************************
    // PLEASE MAKE THIS TO RUN MULTIPLE TIMES WITHOUT ERROR
    // NEEDED FOR NIGHTLY SNAPSHOTS ET ALL
    // SVN BUILD IS ALSO CONSIDERED AS LOWER !!!
    // ****************************************************

    // upgrade to v4 (OR DO THIS IF WE ARE INSIDE IDE)
    if (StrgUtils.compareVersion(v, "4.0") < 0) {
      LOGGER.info("Performing upgrade tasks to version 4.0");

      // transfer contents of extra.txt into the launcher-extra.yml
      Path extraTxt = Paths.get("extra.txt");
      if (Files.exists(extraTxt)) {
        try {
          List<String> jvmOpts = FileUtils.readLines(extraTxt.toFile(), StandardCharsets.UTF_8);

          LauncherExtraConfig extraConfig = LauncherExtraConfig.readFile(Paths.get(LauncherExtraConfig.LAUNCHER_EXTRA_YML).toFile());
          extraConfig.jvmOpts.addAll(jvmOpts);
          extraConfig.save();

          Files.deleteIfExists(extraTxt);
        }
        catch (Exception ignored) {
          // do nothing
        }
      }

      // delete old files
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerCMDUpd.exe"));
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerUpd.exe"));
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerCMD.sh"));
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerUpdater.sh"));
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerUpdaterCMD.sh"));
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerCMD-OSX.sh"));
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerOSX.sh"));
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerUpdaterCMD-OSX.sh"));
      Utils.deleteFileSafely(Paths.get("getdown.jar"));
    }

    if (StrgUtils.compareVersion(v, "4.0.2") < 0) {
      LOGGER.info("Performing upgrade tasks to version 4.0.2");
      Settings.getInstance().setIgnoreSSLProblems(true);
      Settings.getInstance().saveSettings();
    }

    if (StrgUtils.compareVersion(v, "4.1") < 0) {
      LOGGER.info("Performing upgrade tasks to version 4.1");
      // copy most common scraper settings to the new filename

      // Anidb
      Path config = Paths.get("data/scraper_anidb.conf");
      if (Files.exists(config)) {
        // copy to the tvshow file
        copyFileSilently(config, Paths.get("data/scraper_anidb_tvshow.conf"));
        Utils.deleteFileSafely(config);
      }

      // Fanart.tv
      config = Paths.get("data/scraper_fanarttv.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_fanarttv_movie_artwork.conf"));
        copyFileSilently(config, Paths.get("data/scraper_fanarttv_tvshow_artwork.conf"));
        Utils.deleteFileSafely(config);
      }

      // hd-trailers.net
      config = Paths.get("data/scraper_hd-trailers.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_hd-trailers_movie_trailer.conf"));
        Utils.deleteFileSafely(config);
      }

      // imdb
      config = Paths.get("data/scraper_imdb.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_imdb_movie.conf"));
        copyFileSilently(config, Paths.get("data/scraper_imdb_movie_artwork.conf"));
        copyFileSilently(config, Paths.get("data/scraper_imdb_tvshow.conf"));
        copyFileSilently(config, Paths.get("data/scraper_imdb_tvshow_artwork.conf"));
        Utils.deleteFileSafely(config);
      }

      // moviemeter
      config = Paths.get("data/scraper_moviemeter.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_moviemeter_movie.conf"));
        Utils.deleteFileSafely(config);
      }

      // mpdbtv
      config = Paths.get("data/scraper_mpdbtv.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_mpdbtv_movie.conf"));
        copyFileSilently(config, Paths.get("data/scraper_mpdbtv_movie_artwork.conf"));
        Utils.deleteFileSafely(config);
      }

      // ofdb
      config = Paths.get("data/scraper_ofdb.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_ofdb_movie.conf"));
        copyFileSilently(config, Paths.get("data/scraper_ofdb_movie_trailer.conf"));
        Utils.deleteFileSafely(config);
      }

      // omdb
      config = Paths.get("data/scraper_omdbapi.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_omdbapi_movie.conf"));
        Utils.deleteFileSafely(config);
      }

      // opnsubtitles
      config = Paths.get("data/scraper_opensubtitles.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_opensubtitles_movie_subtitle.conf"));
        copyFileSilently(config, Paths.get("data/scraper_opensubtitles_tvshow_subtitle.conf"));
        Utils.deleteFileSafely(config);
      }

      // thetvdb
      config = Paths.get("data/scraper_tvdb.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_tvdb_tvshow.conf"));
        copyFileSilently(config, Paths.get("data/scraper_tvdb_tvshow_artwork.conf"));
        Utils.deleteFileSafely(config);
      }

      // tmdb
      config = Paths.get("data/scraper_tmdb.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_tmdb_movie.conf"));
        copyFileSilently(config, Paths.get("data/scraper_tmdb_movie_artwork.conf"));
        copyFileSilently(config, Paths.get("data/scraper_tmdb_movie_trailer.conf"));
        copyFileSilently(config, Paths.get("data/scraper_tmdb_tvshow.conf"));
        copyFileSilently(config, Paths.get("data/scraper_tmdb_tvshow_artwork.conf"));
        copyFileSilently(config, Paths.get("data/scraper_tmdb_tvshow_trailer.conf"));
        Utils.deleteFileSafely(config);
      }

      // trakt
      config = Paths.get("data/scraper_trakt.conf");
      if (Files.exists(config)) {
        // copy to the movie_artwork/tvshow_artwork file
        copyFileSilently(config, Paths.get("data/scraper_trakt_movie.conf"));
        copyFileSilently(config, Paths.get("data/scraper_trakt_tvshow.conf"));
        Utils.deleteFileSafely(config);
      }
    }

    if (StrgUtils.compareVersion(v, "4.3") < 0) {
      LOGGER.info("Performing upgrade tasks to version 4.3");

      // we migrated from movie/tvshow settings to a global one
      // Movie/show settings are just kept for migration an can be removed at any time...
      if (MovieModuleManager.getInstance().getSettings().isArdAfterScrape() || TvShowModuleManager.getInstance().getSettings().isArdAfterScrape()) {
        Settings.getInstance().setArdEnabled(true);
        MovieModuleManager.getInstance().getSettings().setArdAfterScrape(false);
        TvShowModuleManager.getInstance().getSettings().setArdAfterScrape(false);
      }

      // delete all old ffmpeg addons
      final File[] files = Paths.get("native/addons").toFile().listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.getName().contains("ffmpeg")) {
            try {
              if (file.isFile()) {
                Files.delete(file.toPath());
              }
              else {
                Utils.deleteDirectoryRecursive(file.toPath());
              }
            }
            catch (Exception e) {
              LOGGER.debug("could not delete file '{}' - '{}'", file.getName(), e.getMessage());
            }
          }
        }
      }
    }

    if (StrgUtils.compareVersion(v, "4.3.4") < 0) {
      LOGGER.info("Performing upgrade tasks to version 4.3.4");
      Utils.deleteFileSafely(Paths.get(Globals.LOG_FOLDER).resolve("trace.log"));
    }

    /*
     * V5
     */
    if (StrgUtils.compareVersion(v, "5.0") < 0) {
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

  private static void copyFileSilently(Path source, Path destination) {
    try {
      FileUtils.copyFile(source.toFile(), destination.toFile());

    }
    catch (Exception e) {
      // ignore
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

        for (TvShowEpisode episode : tvShow.getEpisodes()) {
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

            episode.saveToDb();
          }
        }
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

  /**
   * performs some upgrade tasks from one version to another<br>
   * <b>make sure, this upgrade can run multiple times (= needed for nightlies!!!)
   *
   */
  public static void performUpgradeTasksAfterDatabaseLoadingV4() {
    MovieList movieList = MovieModuleManager.getInstance().getMovieList();
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    String v = "" + oldVersion;

    if (StringUtils.isBlank(v)) {
      v = "4.0"; // set version for other updates
    }

    // ****************************************************
    // PLEASE MAKE THIS TO RUN MULTIPLE TIMES WITHOUT ERROR
    // NEEDED FOR NIGHTLY SNAPSHOTS ET ALL
    // GIT BUILD IS ALSO CONSIDERED AS LOWER !!!
    // ****************************************************
    if (StrgUtils.compareVersion(v, "4.0.6") < 0) {
      // upgrade extrafanart settings
      if (MovieModuleManager.getInstance().getSettings().isImageExtraFanart()
          && MovieModuleManager.getInstance().getSettings().getExtraFanartFilenames().isEmpty()) {
        MovieModuleManager.getInstance().getSettings().addExtraFanartFilename(MovieExtraFanartNaming.FOLDER_EXTRAFANART);
        MovieModuleManager.getInstance().getSettings().saveSettings();
      }
      if (TvShowModuleManager.getInstance().getSettings().isImageExtraFanart()
          && TvShowModuleManager.getInstance().getSettings().getExtraFanartFilenames().isEmpty()) {
        TvShowModuleManager.getInstance().getSettings().addExtraFanartFilename(TvShowExtraFanartNaming.FOLDER_EXTRAFANART);
        TvShowModuleManager.getInstance().getSettings().saveSettings();
      }

      // update container formats
      for (Movie movie : movieList.getMovies()) {
        boolean dirty = false;

        for (MediaFile mediaFile : movie.getMediaFiles(MediaFileType.VIDEO)) {
          dirty = dirty || upgradeContainerFormat(mediaFile);
        }

        if (dirty) {
          movie.saveToDb();
          movie.firePropertyChange(MEDIA_INFORMATION, false, true);
        }
      }

      // update container formats
      for (TvShow tvShow : tvShowList.getTvShows()) {
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          boolean dirty = false;

          for (MediaFile mediaFile : episode.getMediaFiles(MediaFileType.VIDEO)) {
            dirty = dirty || upgradeContainerFormat(mediaFile);
          }

          if (dirty) {
            episode.saveToDb();
            episode.firePropertyChange(MEDIA_INFORMATION, false, true);
          }
        }
      }
    }

    if (StrgUtils.compareVersion(v, "4.1") < 0) {
      // remove invalid ratings
      for (Movie movie : movieList.getMovies()) {
        repairRatings(movie);
      }

      for (TvShow tvShow : tvShowList.getTvShows()) {
        repairRatings(tvShow);

        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          repairRatings(episode);
        }
      }

      // release date country
      if (StringUtils.isBlank(MovieModuleManager.getInstance().getSettings().getReleaseDateCountry())) {
        MovieModuleManager.getInstance().getSettings().setReleaseDateCountry(Locale.getDefault().getCountry());
      }
      if (StringUtils.isBlank(TvShowModuleManager.getInstance().getSettings().getReleaseDateCountry())) {
        TvShowModuleManager.getInstance().getSettings().setReleaseDateCountry(Locale.getDefault().getCountry());
      }
    }

    if (StrgUtils.compareVersion(v, "4.1.2") < 0) {
      // do not run the upgrade task in the git build, since it would always overwrite the values
      if (!ReleaseInfo.isGitBuild()) {
        {
          String folderPattern = MovieModuleManager.getInstance().getSettings().getRenamerPathname();
          if (folderPattern.contains("${hdr}")) {
            MovieModuleManager.getInstance().getSettings().setRenamerPathname(folderPattern.replace("${hdr}", "${hdrformat}"));
            MovieModuleManager.getInstance().getSettings().saveSettings();
          }

          String filenamePattern = MovieModuleManager.getInstance().getSettings().getRenamerFilename();
          if (filenamePattern.contains("${hdr}")) {
            MovieModuleManager.getInstance().getSettings().setRenamerFilename(filenamePattern.replace("${hdr}", "${hdrformat}"));
            MovieModuleManager.getInstance().getSettings().saveSettings();
          }

          filenamePattern = TvShowModuleManager.getInstance().getSettings().getRenamerFilename();
          if (filenamePattern.contains("${hdr}")) {
            TvShowModuleManager.getInstance().getSettings().setRenamerFilename(filenamePattern.replace("${hdr}", "${hdrformat}"));
            TvShowModuleManager.getInstance().getSettings().saveSettings();
          }
        }
      }
    }

    if (StrgUtils.compareVersion(v, "4.2.6") < 0) {
      // do not run the upgrade task in the git build, since it would always overwrite the values
      if (!ReleaseInfo.isGitBuild()) {
        // add MAKEMKV to skip folders
        if (!MovieModuleManager.getInstance().getSettings().getSkipFolder().contains("MAKEMKV")) {
          MovieModuleManager.getInstance().getSettings().addSkipFolder("MAKEMKV");
          MovieModuleManager.getInstance().getSettings().saveSettings();
        }
        if (!TvShowModuleManager.getInstance().getSettings().getSkipFolder().contains("MAKEMKV")) {
          TvShowModuleManager.getInstance().getSettings().addSkipFolder("MAKEMKV");
          TvShowModuleManager.getInstance().getSettings().saveSettings();
        }
      }
    }

    if (StrgUtils.compareVersion(v, "4.2.8") < 0) {
      // remove duplicate data sources
      Set<String> movieDataSources = new LinkedHashSet<>(MovieModuleManager.getInstance().getSettings().getMovieDataSource());
      if (movieDataSources.size() != MovieModuleManager.getInstance().getSettings().getMovieDataSource().size()) {
        MovieModuleManager.getInstance().getSettings().setMovieDataSources(movieDataSources);
        MovieModuleManager.getInstance().getSettings().saveSettings();
      }

      Set<String> tvShowDataSources = new LinkedHashSet<>(TvShowModuleManager.getInstance().getSettings().getTvShowDataSource());
      if (tvShowDataSources.size() != TvShowModuleManager.getInstance().getSettings().getTvShowDataSource().size()) {
        TvShowModuleManager.getInstance().getSettings().setTvShowDataSources(tvShowDataSources);
        TvShowModuleManager.getInstance().getSettings().saveSettings();
      }
    }

    if (StrgUtils.compareVersion(v, "4.3") < 0) {
      // replace imdbId with imdb
      for (Movie movie : movieList.getMovies()) {
        // fix imdb id
        Object value = movie.getId("imdbId");
        if (value != null && movie.getId(MediaMetadata.IMDB) == null) {
          movie.setId(MediaMetadata.IMDB, value);
        }
        movie.setId("imdbId", null);

        // round rating
        for (Map.Entry<String, MediaRating> entry : movie.getRatings().entrySet()) {
          entry.getValue().setRating(entry.getValue().getRating());
        }
        movie.saveToDb();
      }

      for (TvShow tvShow : tvShowList.getTvShows()) {
        // fix imdb id
        Object value = tvShow.getId("imdbId");
        if (value != null && tvShow.getId(MediaMetadata.IMDB) == null) {
          tvShow.setId(MediaMetadata.IMDB, value);
        }
        tvShow.setId("imdbId", null);

        // round rating
        for (Map.Entry<String, MediaRating> entry : tvShow.getRatings().entrySet()) {
          entry.getValue().setRating(entry.getValue().getRating());
        }
        tvShow.saveToDb();

        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          // fix imdb id
          value = episode.getId("imdbId");
          if (value != null && episode.getId(MediaMetadata.IMDB) == null) {
            episode.setId(MediaMetadata.IMDB, value);
          }
          episode.setId("imdbId", null);

          // round rating
          for (Map.Entry<String, MediaRating> entry : episode.getRatings().entrySet()) {
            entry.getValue().setRating(entry.getValue().getRating());
          }
          episode.saveToDb();
        }
      }
    }

    if (StrgUtils.compareVersion(v, "4.3.3") < 0) {
      // hide top250 column
      if (!ReleaseInfo.isGitBuild()) {
        TmmUILayoutStore.getInstance().hideNewColumn("movies.movieTable", "top250");
      }
    }

    if (StrgUtils.compareVersion(v, "4.3.9") < 0) {
      movieList.reevaluateMMD();

      // fix structures of invalid, mostly multi-episode files
      for (TvShow tvShow : tvShowList.getTvShows()) {
        Set<TvShowEpisode> cleanup = new HashSet<>();
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          List<MediaFile> mfs = episode.getMediaFiles(MediaFileType.VIDEO);
          if (mfs.size() > 1 && !episode.isDisc()) {
            // we have multiple videos on SAME episode
            // check if they have a stacking marker, else it would be invalid!
            for (MediaFile mf : mfs) {
              if (mf.getStacking() == 0) {
                cleanup.add(episode);
              }
            }
          }
        }
        for (TvShowEpisode ep : cleanup) {
          tvShow.removeEpisode(ep);
          LOGGER.info("Removed invalid episode '{}' (S{} E{}) from show /{}", ep.getTitle(), ep.getSeason(), ep.getEpisode(),
              tvShow.getPathNIO().getFileName());
        }
      }
    }

    if (StrgUtils.compareVersion(v, "4.3.9.1") < 0) {
      for (TvShow tvShow : tvShowList.getTvShows()) {
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          boolean dirty = false;
          for (MediaFile mf : episode.getMediaFiles(MediaFileType.GRAPHIC)) {
            mf.setType(MediaFileType.THUMB);
            dirty = true;
          }
          if (dirty) {
            episode.saveToDb();
          }
        }
      }
    }

    if (StrgUtils.compareVersion(v, "4.3.11") < 0) {
      // upgrade stacking information for subtitles
      for (Movie movie : movieList.getMovies()) {
        if (movie.isStacked()) {
          boolean dirty = false;
          for (MediaFile mf : movie.getMediaFiles(MediaFileType.SUBTITLE)) {
            String old = mf.getStackingMarker();
            mf.detectStackingInformation();
            if (!old.equals(mf.getStackingMarker())) {
              dirty = true;
            }
          }
          if (dirty) {
            movie.saveToDb();
            movie.firePropertyChange(MEDIA_INFORMATION, false, true);
          }
        }
      }
    }
  }

  private static boolean upgradeContainerFormat(MediaFile mediaFile) {
    switch (mediaFile.getContainerFormat().toLowerCase()) {
      case "video_ts", "mpeg-ps", "dvd-video" -> {
        mediaFile.setContainerFormat("DVD Video");
        return true;
      }
      case "bdav" -> {
        mediaFile.setContainerFormat("Blu-ray Video");
        return true;
      }
      case "matroska" -> {
        mediaFile.setContainerFormat("Matroska");
        return true;
      }
      case "mpeg-4" -> {
        mediaFile.setContainerFormat("MPEG-4");
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  private static void repairRatings(MediaEntity entity) {
    boolean dirty = false;

    Map<String, MediaRating> ratings = new HashMap<>(entity.getRatings());
    for (Map.Entry<String, MediaRating> entry : ratings.entrySet()) {
      MediaRating rating = entry.getValue();

      if (rating.getRating() < 0) {
        entity.removeRating(entry.getKey());
        dirty = true;
      }
      else if ("rottenTomatoes".equalsIgnoreCase(entry.getKey())) {
        entity.removeRating(entry.getKey());
        MediaRating mediaRating = new MediaRating("tomatometerallcritics", rating.getRating(), rating.getVotes(), rating.getMaxValue());
        entity.setRating(mediaRating);
        dirty = true;
      }
      else if ("metascore".equalsIgnoreCase(entry.getKey())) {
        entity.removeRating(entry.getKey());
        MediaRating mediaRating = new MediaRating("metacritic", rating.getRating(), rating.getVotes(), rating.getMaxValue());
        entity.setRating(mediaRating);
        dirty = true;
      }
    }

    if (dirty) {
      entity.saveToDb();
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
