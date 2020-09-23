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
package org.tinymediamanager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.sun.jna.Platform;

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
    oldVersion = Globals.settings.getVersion();
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
    if (v.startsWith("3")) {
      // create a backup from v3 data
      createV3Backup();
    }

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
      Utils.deleteFileSafely(Paths.get("tinyMediaManagerCMD.exe"));
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

  }

  /**
   * performs some upgrade tasks from one version to another<br>
   * <b>make sure, this upgrade can run multiple times (= needed for nightlies!!!)
   *
   */
  public static void performUpgradeTasksAfterDatabaseLoading() {
    MovieList movieList = MovieList.getInstance();
    TvShowList tvShowList = TvShowList.getInstance();

    String v = "" + oldVersion;

    if (StringUtils.isBlank(v)) {
      v = "4.0"; // set version for other updates
    }

    // ****************************************************
    // PLEASE MAKE THIS TO RUN MULTIPLE TIMES WITHOUT ERROR
    // NEEDED FOR NIGHTLY SNAPSHOTS ET ALL
    // GIT BUILD IS ALSO CONSIDERED AS LOWER !!!
    // ****************************************************
  }

  /**
   * rename downloaded files (getdown.jar, ...)
   */
  public static void renameDownloadedFiles() {
    // OSX launcher
    if (Platform.isMac()) {
      File file = new File("macOS/MacOS/tinyMediaManager");
      if (file.exists() && file.length() > 0) {
        File cur = new File("../../MacOS/tinyMediaManager");
        try {
          FileUtils.copyFile(file, cur);
          cur.setExecutable(true);
        }
        catch (IOException e) {
          LOGGER.error("Could not update MacOS/tinyMediaManager");
        }
      }
    }

    // legacy OSX launcher (need for a smooth transition from v3)
    if (Platform.isMac()) {
      File file = new File("macOS/MacOS/JavaApplicationStub");
      if (file.exists() && file.length() > 0) {
        File cur = new File("../../MacOS/JavaApplicationStub");
        try {
          FileUtils.copyFile(file, cur);
          cur.setExecutable(true);
        }
        catch (IOException e) {
          LOGGER.error("Could not update MacOS/JavaApplicationStub");
        }
      }
    }

    // OSX Info.plist
    if (Platform.isMac()) {
      File file = new File("macOS/Info.plist");
      if (file.exists() && file.length() > 0) {
        File cur = new File("../../Info.plist");
        try {
          FileUtils.copyFile(file, cur);
        }
        catch (IOException e) {
          LOGGER.error("Could not update Info.plist");
        }
      }
    }

    // OSX tmm.icns
    if (Platform.isMac()) {
      File file = new File("macOS/Resources/tmm.icns");
      if (file.exists() && file.length() > 0) {
        File cur = new File("../tmm.icns");
        try {
          FileUtils.copyFile(file, cur);
        }
        catch (IOException e) {
          LOGGER.error("Could not update tmm.icns");
        }
      }
    }
  }

  private static void createV3Backup() {
    Path backup = Paths.get(Globals.BACKUP_FOLDER);
    Path dataFolder = Paths.get(Settings.getInstance().getSettingsFolder());
    try {
      if (!Files.exists(backup)) {
        Files.createDirectory(backup);
      }

      backup = backup.resolve("v3-backup.zip");
      if (!Files.exists(backup)) {
        Utils.createZip(backup, dataFolder); // just put in main dir
      }
    }
    catch (IOException e) {
      LOGGER.error("Could not backup file {}: {}", dataFolder, e.getMessage());
    }

  }
}
