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

package org.tinymediamanager.core.tvshow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.tasks.TrailerDownloadTask;
import org.tinymediamanager.core.tasks.YTDownloadTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.filenaming.TvShowTrailerNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowTrailerDownloadTask;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.imdb.ImdbTvShowTrailerProvider;

/**
 * a collection of various helpers for the TV show module
 *
 * @author Manuel Laggner
 */
public class TvShowHelpers {
  private static final Logger  LOGGER               = LoggerFactory.getLogger(TvShow.class);

  private static final Pattern SEASON_NUMBER        = Pattern.compile("(?i)season(\\d+).*");
  private static final Pattern SEASON_FOLDER_NUMBER = Pattern.compile("(?i).*(\\d+).*");

  private TvShowHelpers() {
    throw new IllegalAccessError();
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
  public static MediaCertification parseCertificationStringForTvShowSetupCountry(String name) {
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
          cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), cs[1]);
          if (cert != MediaCertification.UNKNOWN) {
            return cert;
          }
        }
        else {
          cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), c);
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
        cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), cs[1].trim());
        if (cert == MediaCertification.UNKNOWN) {
          cert = MediaCertification.findCertification(cs[1].trim());
        }
      }
      else {
        // no country? try to find only by name
        cert = MediaCertification.getCertification(TvShowModuleManager.getInstance().getSettings().getCertificationCountry(), name.trim());
      }
    }
    // still not found localized cert? parse the name to find *ANY* certificate
    if (cert == MediaCertification.UNKNOWN) {
      cert = MediaCertification.findCertification(name);
    }
    return cert;
  }

  /**
   * try to detect the TV show folder by comparing the paths of the media files
   *
   * @param tvShow
   *          the TV show to analyze
   * @param season
   *          the season for what we would like to have the season folder
   * @return the path to the season folder relative to the TV show folder or the default season folder name from the renamer settings
   */
  public static String detectSeasonFolder(TvShow tvShow, int season) {
    List<String> subPaths = new ArrayList<>();

    Path tvShowPath = tvShow.getPathNIO();
    List<TvShowEpisode> episodes = tvShow.getEpisodesForSeason(season);

    try {
      // compare all episodes for the given season
      for (TvShowEpisode episode : episodes) {
        Path videoFilePath = episode.getMainVideoFile().getFileAsPath().getParent();

        // split up the relative path into its path junks
        Path relativePath = tvShowPath.relativize(videoFilePath);
        int subfolders = relativePath.getNameCount();

        for (int i = 1; i <= subfolders; i++) {
          subPaths.add(relativePath.subpath(0, i).toString());
        }
      }
    }
    catch (Exception e) {
      LOGGER.debug("could not extract season folder: {}", e.getMessage());
    }

    if (subPaths.isEmpty()) {
      return "";
    }

    // group them
    Map<String, Long> subPathCounts = subPaths.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));

    // take the highest count
    Map.Entry<String, Long> entry = subPathCounts.entrySet().stream().max(Comparator.comparing(Map.Entry::getValue)).get(); // NOSONAR

    // if there are at least 80% of all episodes having this subfolder, take it
    if (entry.getValue() >= 0.8 * episodes.size()) {
      return entry.getKey();
    }

    // just fake an episode here, since the real foldername can only be generated out of the episode
    // create a dummy episode to inject the season number
    TvShowEpisode episode = new TvShowEpisode();
    episode.setSeason(season);

    return TvShowRenamer.getSeasonFoldername(tvShow, episode);
  }

  /**
   * start the automatic trailer download for the given movie
   *
   * @param tvShow
   *          the TV show to start the trailer download for
   */
  public static void startAutomaticTrailerDownload(TvShow tvShow) {
    // start movie trailer download?
    if (TvShowModuleManager.getInstance().getSettings().isUseTrailerPreference()
        && TvShowModuleManager.getInstance().getSettings().isAutomaticTrailerDownload() && tvShow.getMediaFiles(MediaFileType.TRAILER).isEmpty()
        && !tvShow.getTrailer().isEmpty()) {
      downloadBestTrailer(tvShow);
    }
  }

  /**
   * download the best trailer for the given TV show
   *
   * @param tvShow
   *          the TV show to download the trailer for
   */
  public static void downloadBestTrailer(TvShow tvShow) {
    if (!tvShow.getTrailer().isEmpty()) {
      TmmTask task = new TvShowTrailerDownloadTask(tvShow);
      TmmTaskManager.getInstance().addDownloadTask(task);
    }
  }

  /**
   * download the given trailer for the given TV show
   *
   * @param tvshow
   *          the TV show to download the trailer for
   * @param trailer
   *          the trailer to download
   */
  public static void downloadTrailer(TvShow tvshow, MediaTrailer trailer) {
    if (StringUtils.isBlank(trailer.getUrl()) || !trailer.getUrl().startsWith("http")) {
      if (StringUtils.isBlank(trailer.getId())) {
        LOGGER.debug("Could not download trailer: no url/id {}", trailer);
        return;
      }
      // we have an ID - lets check if it is a known one:
      String id = trailer.getId();
      if (!id.matches("vi\\d+")) { // IMDB
        LOGGER.debug("Could not download trailer: id not known {}", trailer);
        return;
      }

      // IMD trailer ID
      ImdbTvShowTrailerProvider tp = new ImdbTvShowTrailerProvider();
      String url = tp.getUrlForId(trailer);
      if (url.isEmpty()) {
        LOGGER.debug("Could not download trailer: could not construct url from id {}", trailer);
        return;
      }
      trailer.setUrl(url);
    }

    // get the right file name
    List<TvShowTrailerNaming> trailernames = TvShowModuleManager.getInstance().getSettings().getTrailerFilenames();

    // hmm.. at the moment we can only download ONE trailer, so both patterns won't work
    // just take the first one (or the default if there is no entry whyever)
    String filename;
    if (!trailernames.isEmpty()) {
      filename = tvshow.getTrailerFilename(trailernames.get(0));
    }
    else {
      filename = tvshow.getTrailerFilename(TvShowTrailerNaming.TVSHOW_TRAILER);
    }

    try {
      Matcher matcher = Utils.YOUTUBE_PATTERN.matcher(trailer.getUrl());
      if (matcher.matches()) {
        YTDownloadTask task = new YTDownloadTask(trailer, TvShowModuleManager.getInstance().getSettings().getTrailerQuality()) {
          @Override
          protected Path getDestinationWoExtension() {
            return tvshow.getPathNIO().resolve(filename);
          }

          @Override
          protected MediaEntity getMediaEntityToAdd() {
            return tvshow;
          }
        };
        TmmTaskManager.getInstance().addDownloadTask(task);
      }
      else {
        TrailerDownloadTask task = new TrailerDownloadTask(trailer) {

          @Override
          protected Path getDestinationWoExtension() {
            return tvshow.getPathNIO().resolve(filename);
          }

          @Override
          protected MediaEntity getMediaEntityToAdd() {
            return tvshow;
          }
        };
        TmmTaskManager.getInstance().addDownloadTask(task);
      }
    }
    catch (Exception e) {
      LOGGER.error("could not start trailer download: {}", e.getMessage());
      MessageManager.instance.pushMessage(
          new Message(Message.MessageLevel.ERROR, tvshow, "message.scrape.trailerfailed", new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  /**
   * detect the season number of season-xxx named files
   * 
   * @param filename
   *          the filename
   * @param foldername
   *          the folder name
   * @return the season number OR Integer.MIN_VALUE if not detectable
   */
  public static int detectSeasonFromFileAndFolder(String filename, String foldername) {
    int season = Integer.MIN_VALUE;

    if (filename.startsWith("season-specials") || foldername.equalsIgnoreCase("specials")) {
      season = 0;
    }
    else if (filename.startsWith("season-all")) {
      season = -1;
    }
    else {
      // parse out the season from the name
      Matcher matcher = SEASON_NUMBER.matcher(filename);
      if (matcher.matches()) {
        season = Integer.parseInt(matcher.group(1));
      }

      // try to parse out the season from the parent
      if (season == Integer.MIN_VALUE) {
        matcher = SEASON_NUMBER.matcher(foldername);
        if (matcher.matches()) {
          season = Integer.parseInt(matcher.group(1));
        }
      }
      if (season == Integer.MIN_VALUE) {
        matcher = SEASON_FOLDER_NUMBER.matcher(foldername);
        if (matcher.matches()) {
          season = Integer.parseInt(matcher.group(1));
        }
      }
    }

    // mitigation for corrupt ones
    if (season == Integer.MAX_VALUE) {
      season = Integer.MIN_VALUE;
    }

    return season;
  }
}
