/*
 * Copyright 2012 - 2019 Manuel Laggner
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

package org.tinymediamanager.core.mediainfo;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.thirdparty.MediaInfo;

import com.sun.jna.Platform;

public class MediaInfoUtils {
  public static final boolean USE_LIBMEDIAINFO = useMediaInfo();

  private static final Logger LOGGER           = LoggerFactory.getLogger(MediaInfoUtils.class);

  /**
   * checks if we should use libMediaInfo
   * 
   * @return true/false
   */
  private static boolean useMediaInfo() {
    return Boolean.parseBoolean(System.getProperty("tmm.uselibmediainfo", "true"));
  }

  private MediaInfoUtils() {
    // private constructor for utility classes
  }

  /**
   * load media info from /native/*
   */
  public static void loadMediaInfo() {
    if (!USE_LIBMEDIAINFO) {
      return;
    }

    try {
      String miv;
      String nativepath = "native/";

      // windows
      if (Platform.isWindows()) {
        nativepath += "windows";
      }
      // linux
      else if (Platform.isLinux()) {
        nativepath += "linux";
      }
      // osx
      else if (Platform.isMac()) {
        nativepath += "mac";
      }

      Path tmmNativeDir = Paths.get(nativepath).toAbsolutePath();

      try {
        // copy and load the native libs to the temp dir to avoid unforseeable issues
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "tmm");
        Path nativeDir = tmpDir.resolve(nativepath).toAbsolutePath();
        Utils.copyDirectoryRecursive(tmmNativeDir, nativeDir);

        System.setProperty("jna.library.path", nativeDir.toString()); // MI
        LOGGER.debug("Loading native libs from: {}", nativeDir);
      }
      catch (Exception e) {
        // not possible somehow -> load directly from tmm folder
        LOGGER.info("could not copy native libs to the temp folder -> try to load from install dir");
        System.setProperty("jna.library.path", tmmNativeDir.toString()); // MI
        LOGGER.debug("Loading native libs from: {}", tmmNativeDir);
      }

      miv = MediaInfo.version(); // load class

      if (!StringUtils.isEmpty(miv)) {
        LOGGER.info("Using {}", miv);
      }
      else {
        LOGGER.error("could not load MediaInfo!");
        if (Platform.isLinux()) {
          LOGGER.error("Please try do install the library from your distribution");
        }
      }

    }
    catch (Exception e) {
      LOGGER.error("Could not load mediainfo", e);
    }
  }
}
