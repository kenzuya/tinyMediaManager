package org.tinymediamanager.thirdparty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;

import com.sun.jna.Platform;

public class MediaInfoUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfoUtils.class);

  private MediaInfoUtils() {
    // private constructor for utility classes
  }

  /**
   * load media info from /native/*
   */
  public static void loadMediaInfo() {
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

      // need that, since we cannot try and reload/unload a Class
      // MI does not load over UNC, so copy to temp
      if (System.getProperty("user.dir", "").startsWith("\\\\") || System.getProperty("user.dir", "").startsWith("//")) {
        LOGGER.debug("We're on a network UNC path!");
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "tmm");
        Path nativeDir = tmpDir.resolve(nativepath).toAbsolutePath();
        Utils.copyDirectoryRecursive(Paths.get(nativepath), nativeDir);

        System.setProperty("jna.library.path", nativeDir.toString()); // MI
        System.setProperty("org.lwjgl.librarypath", nativeDir.toString()); // nfd
        LOGGER.debug("Loading native libs from: {}", nativeDir);
      }
      else {
        System.setProperty("jna.library.path", nativepath); // MI
        System.setProperty("org.lwjgl.librarypath", nativepath); // nfd
        LOGGER.debug("Loading native libs from: {}", nativepath);
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
    catch (IOException e) {
      LOGGER.error("Could not load mediainfo", e);
    }
  }
}
