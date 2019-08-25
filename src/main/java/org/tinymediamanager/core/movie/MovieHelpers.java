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

package org.tinymediamanager.core.movie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieTrailer;
import org.tinymediamanager.core.movie.tasks.MovieTrailerDownloadTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.Certification;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a collection of various helpers for the movie module
 *
 * @author Manuel Laggner
 */
public class MovieHelpers {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieHelpers.class);

  /**
   * Parses a given certification string for the localized country setup in setting.
   *
   * @param name
   *          certification string like "USA:R / UK:15 / Sweden:15"
   * @return the localized certification if found, else *ANY* language cert found
   */
  // <certification>USA:R / UK:15 / Sweden:15 / Spain:18 / South Korea:15 /
  // Singapore:NC-16 / Portugal:M/16 / Philippines:R-18 / Norway:15 / New
  // Zealand:M / Netherlands:16 / Malaysia:U / Malaysia:18PL / Ireland:18 /
  // Iceland:16 / Hungary:18 / Germany:16 / Finland:K-15 / Canada:18A /
  // Canada:18+ / Brazil:16 / Australia:M / Argentina:16</certification>

  public static Certification parseCertificationStringForMovieSetupCountry(String name) {
    Certification cert = Certification.UNKNOWN;
    name = name.trim();
    if (name.contains("/")) {
      // multiple countries
      String[] countries = name.split("/");
      // first try to find by setup CertLanguage
      for (String c : countries) {
        c = c.trim();
        if (c.contains(":")) {
          String[] cs = c.split(":");
          cert = Certification.getCertification(MovieModuleManager.SETTINGS.getCertificationCountry(), cs[1]);
          if (cert != Certification.UNKNOWN) {
            return cert;
          }
        }
        else {
          cert = Certification.getCertification(MovieModuleManager.SETTINGS.getCertificationCountry(), c);
          if (cert != Certification.UNKNOWN) {
            return cert;
          }
        }
      }
      // still not found localized cert? parse the name to find *ANY*
      // certificate
      for (String c : countries) {
        c = c.trim();
        if (c.contains(":")) {
          String[] cs = c.split(":");
          cert = Certification.findCertification(cs[1]);
          if (cert != Certification.UNKNOWN) {
            return cert;
          }
        }
        else {
          cert = Certification.findCertification(c);
          if (cert != Certification.UNKNOWN) {
            return cert;
          }
        }
      }
    }
    else {
      // no slash, so only one country
      if (name.contains(":")) {
        String[] cs = name.split(":");
        cert = Certification.getCertification(MovieModuleManager.SETTINGS.getCertificationCountry(), cs[1].trim());
      }
      else {
        // no country? try to find only by name
        cert = Certification.getCertification(MovieModuleManager.SETTINGS.getCertificationCountry(), name.trim());
      }
    }
    // still not found localized cert? parse the name to find *ANY* certificate
    if (cert == Certification.UNKNOWN) {
      cert = Certification.findCertification(name);
    }
    return cert;
  }

  /**
   * start the automatic trailer download for the given movie
   * 
   * @param movie
   *          the movie to start the trailer download for
   */
  public static void startAutomaticTrailerDownload(Movie movie) {
    // start movie trailer download?
    if (MovieModuleManager.SETTINGS.isUseTrailerPreference() && MovieModuleManager.SETTINGS.isAutomaticTrailerDownload()
        && movie.getMediaFiles(MediaFileType.TRAILER).isEmpty() && !movie.getTrailer().isEmpty()) {
      try {
        MovieTrailer trailer = movie.getTrailer().get(0);
        MovieTrailerDownloadTask task = new MovieTrailerDownloadTask(trailer, movie);
        TmmTaskManager.getInstance().addDownloadTask(task);
      }
      catch (Exception e) {
        LOGGER.error("could not start trailer download: " + e.getMessage());
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, movie, "message.scrape.movietrailerfailed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }
  }

  /**
   * Method to get a list of files with the given regular expression
   * @param path Path where these files should be searched
   * @param regexList list of regular expression
   * @return a list of files
   */
  public static List<File> getUnknownFilesbyRegex(String path, List<String> regexList) {

    List<File> filesToDelete = new ArrayList<>();

    filesToDelete.addAll(Arrays.asList(Objects.requireNonNull(FileSystems.getDefault().getPath(path).toFile().listFiles(filename -> {
      boolean accept = false;

      for( String regex : regexList ) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(filename.getName());
        if ( m.find() ) {
          accept = true;
        }
      }

      return accept;
    }))));


    return filesToDelete;
    }
  }
