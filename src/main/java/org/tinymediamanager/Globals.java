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
package org.tinymediamanager;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

/**
 * The Class Globals. used to hold global information/fields for the whole application
 * 
 * @author Manuel Laggner
 */
public final class Globals {
  private static final boolean DEBUG           = Boolean.parseBoolean(System.getProperty("tmm.debug", "false"));
  private static final boolean SELF_UPDATEABLE = (!Boolean.parseBoolean(System.getProperty("tmm.noupdate")) && !Files.exists(Paths.get(".managed")))
      ? true
      : false;

  public static final String   DATA_FOLDER;
  public static final String   CACHE_FOLDER;
  public static final String   BACKUP_FOLDER;
  public static final String   LOG_FOLDER;

  static {
    // first we look for a dedicated folder property
    // after that we look for tmm.contentfolder
    String dataFolder = System.getProperty("tmm.datafolder");
    String cacheFolder = System.getProperty("tmm.cachefolder");
    String backupFolder = System.getProperty("tmm.backupfolder");
    String logFolder = System.getProperty("tmm.logfolder");

    // always filled!
    String contentFolder = System.getProperty("tmm.contentfolder");
    if (StringUtils.isBlank(contentFolder)) {
      if (Files.exists(Paths.get(".userdir")) || !isTmmDirWritable()) {
        // userdir
        contentFolder = TmmOsUtils.getUserDir().toString();
      }
      else {
        // portable - current folder
        contentFolder = ".";
      }
    }

    // data
    if (StringUtils.isNotBlank(dataFolder)) {
      DATA_FOLDER = dataFolder;
    }
    else {
      DATA_FOLDER = contentFolder + "/data";
    }

    // cache
    if (StringUtils.isNotBlank(cacheFolder)) {
      CACHE_FOLDER = cacheFolder;
    }
    else {
      CACHE_FOLDER = contentFolder + "/cache";
    }

    // backup
    if (StringUtils.isNotBlank(backupFolder)) {
      BACKUP_FOLDER = backupFolder;
    }
    else {
      BACKUP_FOLDER = contentFolder + "/backup";
    }

    // logs
    if (StringUtils.isNotBlank(logFolder)) {
      LOG_FOLDER = logFolder;
    }
    else {
      LOG_FOLDER = contentFolder + "/logs";
    }
  }

  private Globals() {
    throw new IllegalAccessError();
  }

  private static boolean isTmmDirWritable() {
    try {
      RandomAccessFile f = new RandomAccessFile("access.test", "rw");
      f.close();
      Files.deleteIfExists(Paths.get("access.test"));
      return true;
    }
    catch (Exception e) {
      // ignore
    }
    return false;
  }

  /**
   * are we in our internal debug mode?
   * 
   * @return true/false
   */
  public static boolean isDebug() {
    return DEBUG;
  }

  /**
   * Are we running from a webstart instance?
   * 
   * @return true/false
   */
  public static boolean isRunningJavaWebStart() {
    boolean hasJNLP = false;
    try {
      Class.forName("javax.jnlp.ServiceManager");
      hasJNLP = true;
    }
    catch (ClassNotFoundException ex) {
      hasJNLP = false;
    }
    return hasJNLP;
  }

  /**
   * Are we running on a jetty webswing instance?
   * 
   * @return true/false
   */
  public static boolean isRunningWebSwing() {
    return System.getProperty("webswing.classPath") != null;
  }

  public static boolean isSelfUpdateable() {
    return SELF_UPDATEABLE;
  }
}
