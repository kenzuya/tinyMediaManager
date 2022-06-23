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

package org.tinymediamanager.updater;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * the class {@link UpdateCheck} is used to check for a new update
 * 
 * @author Manuel Laggner, Myron Boyle
 */
public class UpdateCheck {
  private static final Logger LOGGER      = LoggerFactory.getLogger(UpdateCheck.class);

  private String              changelog   = "";
  private boolean             forceUpdate = false;

  public boolean isUpdateAvailable() {
    return isUpdateAvailable(false);
  }

  public boolean isUpdateAvailable(boolean useCache) {
    if (ReleaseInfo.isGitBuild()) {
      return false;
    }

    Path getdownFile = getFile("getdown.txt");
    Path digestFile = getFile("digest.txt");
    LOGGER.info("Checking for updates...");

    ArrayList<String> updateUrls = new ArrayList<>();
    try (Scanner scanner = new Scanner(getdownFile.toFile())) {
      while (scanner.hasNextLine()) {
        String[] kv = scanner.nextLine().split("=");
        if ("appbase".equals(kv[0].trim()) || "mirror".equals(kv[0].trim())) {
          updateUrls.add(kv[1].trim());
        }
      }

      boolean valid = false;
      String remoteDigest = "";
      String remoteUrl = "";
      // try to download from all our mirrors
      for (String uu : updateUrls) {
        if (!uu.endsWith("/")) {
          uu += '/';
        }

        String urlAsString = uu + "digest.txt";

        LOGGER.trace("Checking {}", uu);
        try {
          Url url;
          if (useCache) {
            url = new OnDiskCachedUrl(urlAsString, 12, TimeUnit.HOURS);
          }
          else {
            url = new Url(urlAsString);
          }
          remoteDigest = UrlUtil.getStringFromUrl(url);
          if (remoteDigest != null && remoteDigest.contains("tmm.jar")) {
            remoteDigest = remoteDigest.trim();
            valid = true; // bingo!
            remoteUrl = uu;
          }
        }
        catch (InterruptedException | InterruptedIOException e) {
          // do not swallow these Exceptions
          Thread.currentThread().interrupt();
        }
        catch (Exception e) {
          LOGGER.warn("Unable to download from url {} - {}", urlAsString, e.getMessage());
        }
        if (valid) {
          break; // no exception - step out :)
        }
      }

      if (!valid) {
        // we failed to download from all mirrors
        // last chance: throw ex and try really hardcoded mirror
        throw new IOException("Error downloading remote checksum information.");
      }

      // compare with our local
      String localDigest = "";
      try {
        localDigest = Utils.readFileToString(digestFile).trim();
      }
      catch (Exception ignored) {
        // ignored
      }
      if (!localDigest.equals(remoteDigest)) {
        LOGGER.info("Update needed...");

        String remoteGD = UrlUtil.getStringFromUrl(remoteUrl + "getdown.txt");
        if (remoteGD.contains("forceUpdate")) {
          forceUpdate = true;
        }

        // download changelog.txt for preview
        changelog = UrlUtil.getStringFromUrl(remoteUrl + "changelog.txt");
        return true;
      }
      else {
        LOGGER.info("Already up2date :)");
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("Update task failed badly! {}", e.getMessage());

      try {
        // try a hardcoded "backup url" for GD.txt, where we could specify a new location :)
        LOGGER.info("Trying fallback...");
        String fallback = "https://www.tinymediamanager.org";
        if (ReleaseInfo.isPreRelease()) {
          fallback += "/getdown_prerelease_v4.txt";
        }
        else if (ReleaseInfo.isNightly()) {
          fallback += "/getdown_nightly_v4.txt";
        }
        else {
          fallback += "/getdown_v4.txt";
        }

        String gd = UrlUtil.getStringFromUrl(fallback);
        if (StringUtils.isBlank(gd) || !gd.contains("appbase")) {
          throw new IOException("could not even download our fallback");
        }
        Utils.writeStringToFile(getdownFile, gd);
        return true;
      }
      catch (InterruptedException | InterruptedIOException e2) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e2) {
        LOGGER.error("Update fallback failed - {}", e2.getMessage());
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "Update check failed :(", e2.getMessage()));
      }
    }
    return false;
  }

  /**
   * get the changelog for the new update
   * 
   * @return the changelog
   */
  public String getChangelog() {
    return changelog;
  }

  /**
   * when forced, do not ask for confirmation dialog.
   *
   * @return true/false
   */
  public boolean isForcedUpdate() {
    return forceUpdate;
  }

  // FROM GETDOWN:
  // try reading data from our backup config file; thanks to funny windows
  // bullshit, we have to do this backup file fiddling in case we got screwed while
  // updating getdown.txt during normal operation
  private Path getFile(String file) {
    Path ret = Paths.get(file);
    if (!Files.exists(ret)) {
      ret = Paths.get(file + "_old");
    }
    return ret;
  }
}
