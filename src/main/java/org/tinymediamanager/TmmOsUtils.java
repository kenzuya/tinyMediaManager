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
package org.tinymediamanager;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * The class TmmOsUtils. Utility class for OS specific tasks
 * 
 * @author Manuel Laggner
 */
public class TmmOsUtils {
  private static final Logger LOGGER       = LoggerFactory.getLogger(TmmOsUtils.class);

  public static final String  DESKTOP_FILE = "tinyMediaManager.desktop";

  private TmmOsUtils() {
    // hide public constructor for utility classes
  }

  /**
   * create a .desktop file for linux and unix (not osx)
   * 
   * @param desktop
   *          .desktop file
   */
  public static void createDesktopFileForLinux(File desktop) {
    if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
      return;
    }

    // get the path in a safe way
    String path = new File(TinyMediaManager.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
    path = URLDecoder.decode(path, StandardCharsets.UTF_8);

    StringBuilder sb = new StringBuilder(60);
    sb.append("[Desktop Entry]\n");
    sb.append("Type=Application\n");
    sb.append("Name=tinyMediaManager\n");
    sb.append("Path=");
    sb.append(path);
    sb.append('\n');
    sb.append("Exec=/bin/sh -c \"");
    sb.append(path);
    sb.append("/tinyMediaManager\"\n");
    sb.append("Icon=");
    sb.append(path);
    sb.append("/tmm.png\n");
    sb.append("Categories=AudioVideo;Video;Database;Java;");
    sb.append("\n");

    try (FileWriterWithEncoding writer = new FileWriterWithEncoding(desktop, UrlUtil.UTF_8)) {
      writer.write(sb.toString());
      if (!desktop.setExecutable(true)) {
        LOGGER.warn("could not make {} executable", desktop.getName());
      }
    }
    catch (IOException e) {
      LOGGER.warn(e.getMessage());
    }
  }

  /**
   * create a ProcessBuilder for restarting TMM
   *
   * @return the process builder
   */
  public static ProcessBuilder getPBforTMMrestart() {
    Path tmmExecutable;
    ProcessBuilder pb;

    if (SystemUtils.IS_OS_WINDOWS) {
      tmmExecutable = Paths.get("tinyMediaManager.exe");
      pb = new ProcessBuilder("cmd", "/c", "start", tmmExecutable.toAbsolutePath().getFileName().toString());
    }
    else if (SystemUtils.IS_OS_MAC) {
      tmmExecutable = Paths.get("../../MacOS/tinyMediaManager");
      pb = new ProcessBuilder("nohup", "/bin/sh", "-c", "./" + tmmExecutable.toAbsolutePath().getFileName().toString());
    }
    else {
      tmmExecutable = Paths.get("tinyMediaManager");
      pb = new ProcessBuilder("nohup", "/bin/sh", "-c", "./" + tmmExecutable.toAbsolutePath().getFileName().toString());
    }

    pb.directory(tmmExecutable.toAbsolutePath().getParent().toAbsolutePath().toFile());
    pb.redirectOutput(new File(SystemUtils.IS_OS_WINDOWS ? "NUL" : "/dev/null")).redirectErrorStream(true);
    return pb;
  }
}
