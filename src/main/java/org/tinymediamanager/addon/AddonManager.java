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
package org.tinymediamanager.addon;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * the class {@link AddonManager} is used to manage addons for tinyMediaManager (version check, download, ..)
 * 
 * @author Manuel Laggner
 */
public class AddonManager {
  private static final Logger LOGGER         = LoggerFactory.getLogger(AddonManager.class);

  private static final String REPOSITORY_URL = "https://gitlab.com/api/v4/projects/9945251/packages/maven/org/tinymediamanager/addon/";

  private AddonManager() {
    throw new IllegalAccessError();
  }

  static String getOsName() {
    if (SystemUtils.IS_OS_MAC) {
      return "macos";
    }
    else if (SystemUtils.IS_OS_WINDOWS) {
      return "windows";
    }
    else {
      return "linux";
    }
  }

  static String getOsArch() {
    String arch = SystemUtils.OS_ARCH;

    if ("arm".equalsIgnoreCase(arch)) {
      return "armv7";
    }
    else {
      return arch;
    }
  }

  static Path getAddonFolder() {
    return Paths.get("native", "addons");
  }

  public static String getLatestVersionForAddon(IAddon addon) {
    String metadataXml;

    try {
      Url url = new OnDiskCachedUrl(REPOSITORY_URL + addon.getFullAddonName() + "/maven-metadata.xml", 1, TimeUnit.DAYS);
      metadataXml = UrlUtil.getStringFromUrl(url);
    }
    catch (Exception e) {
      LOGGER.error("could not load addon metadata - '{}'", e.getMessage());
      return "";
    }

    if (StringUtils.isBlank(metadataXml)) {
      return "";
    }

    // parse the metadata via jsoup
    Document document = Jsoup.parse(metadataXml, "", Parser.xmlParser());
    Elements elements = document.select("metadata > versioning > release");
    if (elements.isEmpty()) {
      return "";
    }

    return elements.get(0).text();
  }

  public static void downloadLatestVersionForAddon(IAddon addon) throws IOException, InterruptedException {
    downloadLatestVersionForAddon(addon, getAddonFolder());
  }

  static void downloadLatestVersionForAddon(IAddon addon, Path targetFolder) throws IOException, InterruptedException {
    String version = getLatestVersionForAddon(addon);

    Path tempFile = null;
    Path target = targetFolder.resolve(addon.getAddonName());
    try {
      String urlAsString = REPOSITORY_URL + addon.getFullAddonName() + "/" + version + "/" + addon.getFullAddonName() + "-" + version + ".tar.br";
      Url url = new Url(urlAsString);

      String filename = FilenameUtils.getName(urlAsString);
      try {
        // create a temp file/folder inside the temp folder or tmm folder
        Path tempFolder = Paths.get(Utils.getTempFolder());
        if (!Files.exists(tempFolder)) {
          Files.createDirectory(tempFolder);
        }
        tempFile = tempFolder.resolve(filename + ".part"); // multi episode same file
      }
      catch (Exception e) {
        LOGGER.debug("could not write to temp folder: {}", e.getMessage());

        // could not create the temp folder somehow - put the files into the entity dir
        tempFile = target.resolveSibling(filename + ".part"); // multi episode same file
      }

      try (InputStream is = url.getInputStream(); FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
        IOUtils.copy(is, outputStream);
        Utils.flushFileOutputStreamToDisk(outputStream);
      }

      // check if the file has been downloaded
      if (!Files.exists(tempFile) || Files.size(tempFile) == 0) {
        // cleanup the file
        FileUtils.deleteQuietly(tempFile.toFile());
        throw new IOException("0byte file downloaded: " + filename);
      }

      // delete new destination if existing
      Utils.deleteDirectoryRecursive(target);

      // extract the temp file to the expected filename
      Utils.unpackBrotli(tempFile.toFile(), target.toFile());

      // unpacking should have worked - put the version file also to the target
      Utils.writeStringToFile(targetFolder.resolve(addon.getAddonName() + ".v"), version);
    }
    finally {
      // remove temp file
      if (tempFile != null && Files.exists(tempFile)) {
        Utils.deleteFileSafely(tempFile);
      }
    }
  }
}
