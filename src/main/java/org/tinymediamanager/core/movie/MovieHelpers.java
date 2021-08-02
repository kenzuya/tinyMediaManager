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

package org.tinymediamanager.core.movie;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.core.movie.tasks.MovieTrailerDownloadTask;
import org.tinymediamanager.core.tasks.TrailerDownloadTask;
import org.tinymediamanager.core.tasks.YTDownloadTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.MediaCertification;

/**
 * a collection of various helpers for the movie module
 *
 * @author Manuel Laggner
 */
public class MovieHelpers {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieHelpers.class);

  private MovieHelpers() {
    // private constructors for helper classes
  }

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
  public static MediaCertification parseCertificationStringForMovieSetupCountry(String name) {
    MediaCertification cert = MediaCertification.UNKNOWN;
    name = name.trim();
    if (name.contains("/")) {
      // multiple countries
      String[] countries = name.split("/");
      // first try to find by setup CertLanguage
      for (String c : countries) {
        c = c.trim();
        if (c.contains(":")) {
          String[] cs = c.split(":");
          cert = MediaCertification.getCertification(MovieModuleManager.getInstance().getSettings().getCertificationCountry(), cs[1]);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
        else {
          cert = MediaCertification.getCertification(MovieModuleManager.getInstance().getSettings().getCertificationCountry(), c);
          if (cert != MediaCertification.UNKNOWN) {
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
          cert = MediaCertification.findCertification(cs[1]);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
        else {
          cert = MediaCertification.findCertification(c);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
      }
    }
    else {
      // no slash, so only one country
      if (name.contains(":")) {
        String[] cs = name.split(":");
        cert = MediaCertification.getCertification(MovieModuleManager.getInstance().getSettings().getCertificationCountry(), cs[1].trim());
      }
      else {
        // no country? try to find only by name
        cert = MediaCertification.getCertification(MovieModuleManager.getInstance().getSettings().getCertificationCountry(), name.trim());
      }
    }
    // still not found localized cert? parse the name to find *ANY* certificate
    if (cert == MediaCertification.UNKNOWN) {
      cert = MediaCertification.findCertification(name);
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
    if (MovieModuleManager.getInstance().getSettings().isUseTrailerPreference()
        && MovieModuleManager.getInstance().getSettings().isAutomaticTrailerDownload()
        && movie.getMediaFiles(MediaFileType.TRAILER).isEmpty() && !movie.getTrailer().isEmpty()) {
      downloadBestTrailer(movie);
    }
  }

  /**
   * download the best trailer for the given movie
   *
   * @param movie
   *          the movie to download the trailer for
   */
  public static void downloadBestTrailer(Movie movie) {
    if (!movie.getTrailer().isEmpty()) {
      TmmTask task = new MovieTrailerDownloadTask(movie);
      TmmTaskManager.getInstance().addDownloadTask(task);
    }
  }

  /**
   * download the given trailer for the given movie
   *
   * @param movie
   *          the movie to download the trailer for
   * @param trailer
   *          the trailer to download
   */
  public static void downloadTrailer(Movie movie, MediaTrailer trailer) {
    if (StringUtils.isBlank(trailer.getUrl()) || !trailer.getUrl().startsWith("http")) {
      return;
    }

    // get the right file name
    List<MovieTrailerNaming> trailernames = new ArrayList<>();
    if (movie.isMultiMovieDir()) {
      // in a MMD we can only use this naming
      trailernames.add(MovieTrailerNaming.FILENAME_TRAILER);
    }
    else {
      trailernames = MovieModuleManager.getInstance().getSettings().getTrailerFilenames();
    }

    // hmm.. at the moment we can only download ONE trailer, so both patterns won't work
    // just take the first one (or the default if there is no entry whyever)
    String filename;
    if (!trailernames.isEmpty()) {
      filename = movie.getTrailerFilename(trailernames.get(0));
    }
    else {
      filename = movie.getTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);
    }

    try {
      Matcher matcher = Utils.YOUTUBE_PATTERN.matcher(trailer.getUrl());
      if (matcher.matches()) {
        YTDownloadTask task = new YTDownloadTask(trailer, MovieModuleManager.getInstance().getSettings().getTrailerQuality()) {
          @Override
          protected Path getDestinationWoExtension() {
            return movie.getPathNIO().resolve(filename);
          }

          @Override
          protected MediaEntity getMediaEntityToAdd() {
            return movie;
          }
        };
        TmmTaskManager.getInstance().addDownloadTask(task);
      }
      else {
        TrailerDownloadTask task = new TrailerDownloadTask(trailer) {
          @Override
          protected Path getDestinationWoExtension() {
            return movie.getPathNIO().resolve(filename);
          }

          @Override
          protected MediaEntity getMediaEntityToAdd() {
            return movie;
          }
        };
        TmmTaskManager.getInstance().addDownloadTask(task);
      }
    }
    catch (Exception e) {
      LOGGER.error("could not start trailer download: {}", e.getMessage());
      MessageManager.instance
          .pushMessage(new Message(Message.MessageLevel.ERROR, movie, "message.scrape.trailerfailed", new String[] { ":", e.getLocalizedMessage() }));
    }
  }

}
