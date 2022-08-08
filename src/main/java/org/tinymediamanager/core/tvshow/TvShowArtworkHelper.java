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

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CHARACTERART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.KEYART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.LOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_FANART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_THUMB;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IFileNaming;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tasks.MediaEntityImageFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonBannerNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonFanartNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonPosterNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonThumbNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowExtraImageFetcherTask;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.thirdparty.VSMeta;

/**
 * The class TvShowArtworkHelper . A helper class for managing TV show artwork
 * 
 * @author Manuel Laggner
 */
public class TvShowArtworkHelper {
  private static final Logger  LOGGER        = LoggerFactory.getLogger(TvShowArtworkHelper.class);
  private static final Pattern INDEX_PATTERN = Pattern.compile(".*?(\\d+)$");

  private TvShowArtworkHelper() {
    // use private constructor for utility classes
  }

  /**
   * Manage downloading of the chosen artwork type
   * 
   * @param show
   *          the TV show for which artwork has to be downloaded
   * @param type
   *          the artwork type to be downloaded
   */
  public static void downloadArtwork(TvShow show, MediaFileType type) {
    // extra handling for extrafanart & extrathumbs
    if (type == MediaFileType.EXTRAFANART) {
      downloadExtraArtwork(show, type);
      return;
    }

    String url = show.getArtworkUrl(type);
    try {
      if (StringUtils.isBlank(url)) {
        return;
      }

      List<IFileNaming> fileNamings = new ArrayList<>();

      switch (type) {
        case FANART:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getFanartFilenames());
          break;

        case POSTER:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getPosterFilenames());
          break;

        case BANNER:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getBannerFilenames());
          break;

        case LOGO:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getLogoFilenames());
          break;

        case CLEARLOGO:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getClearlogoFilenames());
          break;

        case CHARACTERART:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getCharacterartFilenames());
          break;

        case CLEARART:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getClearartFilenames());
          break;

        case THUMB:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getThumbFilenames());
          break;

        case KEYART:
          fileNamings.addAll(TvShowModuleManager.getInstance().getSettings().getKeyartFilenames());
          break;

        default:
          return;
      }

      List<String> filenames = new ArrayList<>();
      for (IFileNaming naming : fileNamings) {
        String filename = naming.getFilename("", Utils.getArtworkExtensionFromUrl(url));

        if (StringUtils.isBlank(filename)) {
          continue;
        }

        filenames.add(filename);
      }

      if (!filenames.isEmpty()) {
        // get images in thread
        MediaEntityImageFetcherTask task = new MediaEntityImageFetcherTask(show, url, MediaFileType.getMediaArtworkType(type), filenames);
        TmmTaskManager.getInstance().addImageDownloadTask(task);
      }
    }
    finally {
      // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
      if (url.startsWith("file:")) {
        show.removeArtworkUrl(type);
      }
    }
  }

  /**
   * set & download missing artwork for the given TV show
   *
   * @param tvShow
   *          the TV show to set the artwork for
   * @param artwork
   *          a list of all artworks to be set
   */
  public static void downloadMissingArtwork(TvShow tvShow, List<MediaArtwork> artwork) {
    // sort artwork once again (langu/rating)
    artwork.sort(new MediaArtwork.MediaArtworkComparator(TvShowModuleManager.getInstance().getSettings().getScraperLanguage().name()));

    // poster
    if (tvShow.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.POSTER);
    }

    // fanart
    if (tvShow.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      setBestFanart(tvShow, artwork);
    }

    // logo
    if (tvShow.getMediaFiles(MediaFileType.LOGO).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.LOGO);
    }

    // clearlogo
    if (tvShow.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.CLEARLOGO);
    }

    // clearart
    if (tvShow.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.CLEARART);
    }

    // banner
    if (tvShow.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.BANNER);
    }

    // thumb
    if (tvShow.getMediaFiles(MediaFileType.THUMB).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.THUMB);
    }

    // discart
    if (tvShow.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.DISC);
    }

    // characterart
    if (tvShow.getMediaFiles(MediaFileType.CHARACTERART).isEmpty()) {
      setBestArtwork(tvShow, artwork, CHARACTERART);
    }

    // keyart
    if (tvShow.getMediaFiles(MediaFileType.KEYART).isEmpty()) {
      setBestArtwork(tvShow, artwork, KEYART);
    }

    for (TvShowSeason season : tvShow.getSeasons()) {
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_POSTER))) {
        for (MediaArtwork art : artwork.stream().filter(mediaArtwork -> mediaArtwork.getType() == SEASON_POSTER).collect(Collectors.toList())) {
          if (art.getSeason() == season.getSeason()) {
            tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_POSTER);
            downloadSeasonPoster(tvShow, art.getSeason());
            break;
          }
        }
      }
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_FANART))) {
        for (MediaArtwork art : artwork.stream().filter(mediaArtwork -> mediaArtwork.getType() == SEASON_FANART).collect(Collectors.toList())) {
          if (art.getSeason() == season.getSeason()) {
            tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_FANART);
            downloadSeasonFanart(tvShow, art.getSeason());
            break;
          }
        }
      }
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_BANNER))) {
        for (MediaArtwork art : artwork.stream().filter(mediaArtwork -> mediaArtwork.getType() == SEASON_BANNER).collect(Collectors.toList())) {
          if (art.getSeason() == season.getSeason()) {
            tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_BANNER);
            downloadSeasonBanner(tvShow, art.getSeason());
            break;
          }
        }
      }
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_THUMB))) {
        for (MediaArtwork art : artwork.stream().filter(mediaArtwork -> mediaArtwork.getType() == SEASON_THUMB).collect(Collectors.toList())) {
          if (art.getSeason() == season.getSeason()) {
            tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_THUMB);
            downloadSeasonThumb(tvShow, art.getSeason());
            break;
          }
        }
      }
    }

    // update DB
    tvShow.saveToDb();
  }

  /**
   * choose the best artwork for this tv show
   *
   * @param tvShow
   *          our tv show
   * @param artwork
   *          the artwork list
   * @param type
   *          the type to download
   */
  private static void setBestArtwork(TvShow tvShow, List<MediaArtwork> artwork, MediaArtworkType type) {
    for (MediaArtwork art : artwork) {
      if (art.getType() == type && StringUtils.isNotBlank(art.getDefaultUrl())) {
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.getMediaFileType(type));
        downloadArtwork(tvShow, MediaFileType.getMediaFileType(type));
        break;
      }
    }
  }

  /**
   * choose the best fanart for this tv show
   *
   * @param tvShow
   *          our tv show
   * @param artwork
   *          the artwork list
   */
  private static void setBestFanart(TvShow tvShow, List<MediaArtwork> artwork) {
    List<MediaArtwork> sortedArtwork = new ArrayList<>(artwork);

    // according to the kodi specifications the fanart _should_ be without any text on it - so we try to get the text-less image (in the right
    // resolution) first
    // https://kodi.wiki/view/Artwork_types#fanart
    MediaArtwork fanartWoText = null;
    for (MediaArtwork art : sortedArtwork) {
      if (art.getType() == MediaArtworkType.BACKGROUND && art.getLanguage().equals("")) {
        fanartWoText = art;
        break;
      }
    }

    if (fanartWoText != null) {
      sortedArtwork.add(0, fanartWoText);
    }

    for (MediaArtwork art : sortedArtwork) {
      if (art.getType() == BACKGROUND && StringUtils.isNotBlank(art.getDefaultUrl())) {
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.getMediaFileType(BACKGROUND));
        downloadArtwork(tvShow, MediaFileType.getMediaFileType(BACKGROUND));
        break;
      }
    }
  }

  /**
   * detect if there is missing artwork for the given TV show
   * 
   * @param tvShow
   *          the TV show to check artwork for
   * @return true/false
   */
  public static boolean hasMissingArtwork(TvShow tvShow, List<TvShowScraperMetadataConfig> config) {
    if (config.contains(TvShowScraperMetadataConfig.POSTER) && !TvShowModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.FANART) && !TvShowModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.BANNER) && !TvShowModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.DISCART) && !TvShowModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.LOGO) && !TvShowModuleManager.getInstance().getSettings().getLogoFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.LOGO).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.CLEARLOGO) && !TvShowModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.CLEARART) && !TvShowModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.THUMB) && !TvShowModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.THUMB).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.CHARACTERART)
        && !TvShowModuleManager.getInstance().getSettings().getCharacterartFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.CHARACTERART).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.KEYART) && !TvShowModuleManager.getInstance().getSettings().getKeyartFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.KEYART).isEmpty()) {
      return true;
    }
    if (config.contains(TvShowScraperMetadataConfig.EXTRAFANART) && TvShowModuleManager.getInstance().getSettings().isImageExtraFanart()
        && !TvShowModuleManager.getInstance().getSettings().getExtraFanartFilenames().isEmpty()
        && tvShow.getMediaFiles(MediaFileType.EXTRAFANART).isEmpty()) {
      return true;
    }

    for (TvShowSeason season : tvShow.getSeasons()) {
      if (config.contains(TvShowScraperMetadataConfig.SEASON_POSTER)
          && !TvShowModuleManager.getInstance().getSettings().getSeasonPosterFilenames().isEmpty()
          && StringUtils.isBlank(season.getArtworkFilename(SEASON_POSTER))) {
        return true;
      }
      if (config.contains(TvShowScraperMetadataConfig.SEASON_FANART)
          && !TvShowModuleManager.getInstance().getSettings().getSeasonFanartFilenames().isEmpty()
          && StringUtils.isBlank(season.getArtworkFilename(SEASON_FANART))) {
        return true;
      }
      if (config.contains(TvShowScraperMetadataConfig.SEASON_BANNER)
          && !TvShowModuleManager.getInstance().getSettings().getSeasonBannerFilenames().isEmpty()
          && StringUtils.isBlank(season.getArtworkFilename(SEASON_BANNER))) {
        return true;
      }
      if (config.contains(TvShowScraperMetadataConfig.SEASON_THUMB)
          && !TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames().isEmpty()
          && StringUtils.isBlank(season.getArtworkFilename(SEASON_THUMB))) {
        return true;
      }
    }

    return false;
  }

  /**
   * detect if there is missing artwork for the given episode
   * 
   * @param episode
   *          the episode to check artwork for
   * @return true/false
   */
  public static boolean hasMissingArtwork(TvShowEpisode episode, List<TvShowEpisodeScraperMetadataConfig> config) {
    return config.contains(TvShowEpisodeScraperMetadataConfig.THUMB)
        && !TvShowModuleManager.getInstance().getSettings().getEpisodeThumbFilenames().isEmpty()
        && episode.getMediaFiles(MediaFileType.THUMB).isEmpty();
  }

  public static void downloadSeasonArtwork(TvShow show, int season, MediaArtworkType artworkType) {
    switch (artworkType) {
      case SEASON_POSTER:
        downloadSeasonPoster(show, season);
        break;

      case SEASON_FANART:
        downloadSeasonFanart(show, season);
        break;

      case SEASON_BANNER:
        downloadSeasonBanner(show, season);
        break;

      case SEASON_THUMB:
        downloadSeasonThumb(show, season);
        break;

      default:
        break;
    }
  }

  /**
   * Download the season poster
   * 
   * @param show
   *          the TV show
   * @param season
   *          the season to download the poster for
   */
  private static void downloadSeasonPoster(TvShow show, int season) {
    String seasonPosterUrl = show.getSeasonArtworkUrl(season, SEASON_POSTER);

    TvShowSeason tvShowSeason = null;
    // try to get a season instance
    for (TvShowSeason s : show.getSeasons()) {
      if (s.getSeason() == season) {
        tvShowSeason = s;
        break;
      }
    }

    for (TvShowSeasonPosterNaming seasonPosterNaming : TvShowModuleManager.getInstance().getSettings().getSeasonPosterFilenames()) {
      String filename = seasonPosterNaming.getFilename(show, season, Utils.getArtworkExtensionFromUrl(seasonPosterUrl));
      if (StringUtils.isBlank(filename)) {
        LOGGER.warn("empty filename for artwork: {} - {}", seasonPosterNaming.name(), show); // NOSONAR
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
        continue;
      }

      Path destFile = show.getPathNIO().resolve(filename);

      // check if the parent exist and create if needed
      if (!Files.exists(destFile.getParent())) {
        try {
          Files.createDirectory(destFile.getParent());
        }
        catch (IOException e) {
          LOGGER.error("could not create folder: {} - {}", destFile.getParent(), e.getMessage());
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
          continue;
        }
      }

      SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(show, destFile, tvShowSeason, seasonPosterUrl, SEASON_POSTER);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (tvShowSeason != null && seasonPosterUrl.startsWith("file:")) {
      tvShowSeason.removeArtworkUrl(SEASON_POSTER);
    }
  }

  /**
   * Download the season fanart
   *
   * @param show
   *          the TV show
   * @param season
   *          the season to download the fanart for
   */
  private static void downloadSeasonFanart(TvShow show, int season) {
    String seasonFanartUrl = show.getSeasonArtworkUrl(season, SEASON_FANART);

    TvShowSeason tvShowSeason = null;
    // try to get a season instance
    for (TvShowSeason s : show.getSeasons()) {
      if (s.getSeason() == season) {
        tvShowSeason = s;
        break;
      }
    }

    for (TvShowSeasonFanartNaming seasonFanartNaming : TvShowModuleManager.getInstance().getSettings().getSeasonFanartFilenames()) {
      String filename = seasonFanartNaming.getFilename(show, season, Utils.getArtworkExtensionFromUrl(seasonFanartUrl));
      if (StringUtils.isBlank(filename)) {
        LOGGER.warn("empty filename for artwork: {} - {}", seasonFanartNaming.name(), show); // NOSONAR
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
        continue;
      }

      Path destFile = show.getPathNIO().resolve(filename);

      // check if the parent exist and create if needed
      if (!Files.exists(destFile.getParent())) {
        try {
          Files.createDirectory(destFile.getParent());
        }
        catch (IOException e) {
          LOGGER.error("could not create folder: {} - {}", destFile.getParent(), e.getMessage());
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
          continue;
        }
      }

      SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(show, destFile, tvShowSeason, seasonFanartUrl, SEASON_FANART);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (tvShowSeason != null && seasonFanartUrl.startsWith("file:")) {
      tvShowSeason.removeArtworkUrl(SEASON_FANART);
    }
  }

  /**
   * Download the season banner
   *
   * @param show
   *          the TV show
   * @param season
   *          the season to download the banner for
   */
  private static void downloadSeasonBanner(TvShow show, int season) {
    String seasonBannerUrl = show.getSeasonArtworkUrl(season, SEASON_BANNER);

    TvShowSeason tvShowSeason = null;
    // try to get a season instance
    for (TvShowSeason s : show.getSeasons()) {
      if (s.getSeason() == season) {
        tvShowSeason = s;
        break;
      }
    }

    for (TvShowSeasonBannerNaming seasonBannerNaming : TvShowModuleManager.getInstance().getSettings().getSeasonBannerFilenames()) {
      String filename = seasonBannerNaming.getFilename(show, season, Utils.getArtworkExtensionFromUrl(seasonBannerUrl));
      if (StringUtils.isBlank(filename)) {
        LOGGER.warn("empty filename for artwork: {} - {}", seasonBannerNaming.name(), show);
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
        continue;
      }

      Path destFile = Paths.get(show.getPathNIO() + File.separator + filename);

      // check if the parent exist and create if needed
      if (!Files.exists(destFile.getParent())) {
        try {
          Files.createDirectory(destFile.getParent());
        }
        catch (IOException e) {
          LOGGER.error("could not create folder: {} - {}", destFile.getParent(), e.getMessage());
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
          continue;
        }
      }

      SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(show, destFile, tvShowSeason, seasonBannerUrl, SEASON_BANNER);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (tvShowSeason != null && seasonBannerUrl.startsWith("file:")) {
      tvShowSeason.removeArtworkUrl(SEASON_BANNER);
    }
  }

  /**
   * Download the season thumb
   *
   * @param show
   *          the TV show
   * @param season
   *          the season to download the thumb for
   */
  private static void downloadSeasonThumb(TvShow show, int season) {
    String seasonThumbUrl = show.getSeasonArtworkUrl(season, SEASON_THUMB);

    TvShowSeason tvShowSeason = null;
    // try to get a season instance
    for (TvShowSeason s : show.getSeasons()) {
      if (s.getSeason() == season) {
        tvShowSeason = s;
        break;
      }
    }

    for (TvShowSeasonThumbNaming seasonThumbNaming : TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames()) {
      String filename = seasonThumbNaming.getFilename(show, season, Utils.getArtworkExtensionFromUrl(seasonThumbUrl));
      if (StringUtils.isBlank(filename)) {
        LOGGER.warn("empty filename for artwork: {} - {}", seasonThumbNaming.name(), show);
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
        continue;
      }

      Path destFile = show.getPathNIO().resolve(filename);

      // check if the parent exist and create if needed
      if (!Files.exists(destFile.getParent())) {
        try {
          Files.createDirectory(destFile.getParent());
        }
        catch (IOException e) {
          LOGGER.error("could not create folder: {} - {}", destFile.getParent(), e.getMessage());
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, show, "tvshow.seasondownload.failed"));
          continue;
        }
      }

      SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(show, destFile, tvShowSeason, seasonThumbUrl, SEASON_THUMB);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (tvShowSeason != null && seasonThumbUrl.startsWith("file:")) {
      tvShowSeason.removeArtworkUrl(SEASON_THUMB);
    }
  }

  private static class SeasonArtworkImageFetcher implements Runnable {
    private final TvShow           tvShow;
    private final TvShowSeason     tvShowSeason;
    private final MediaArtworkType artworkType;
    private final Path             destinationPath;
    private final String           filename;
    private final String           url;

    SeasonArtworkImageFetcher(TvShow show, Path destFile, TvShowSeason tvShowSeason, String url, MediaArtworkType type) {
      this.tvShow = show;
      this.destinationPath = destFile.getParent();
      this.filename = destFile.getFileName().toString();
      this.artworkType = type;
      this.tvShowSeason = tvShowSeason;
      this.url = url;
    }

    @Override
    public void run() {
      String oldFilename = "";

      if (tvShowSeason != null) {
        oldFilename = tvShow.getSeasonArtwork(tvShowSeason.getSeason(), artworkType);
        tvShowSeason.clearArtwork(artworkType);
      }

      LOGGER.debug("writing season artwork {}", filename);

      // fetch and store images
      try {
        Path destFile = ImageUtils.downloadImage(url, destinationPath, filename);

        // if the old filename differs from the new one (e.g. .jpg -> .png), remove the old one
        if (StringUtils.isNotBlank(oldFilename)) {
          Path oldFile = Paths.get(oldFilename);
          if (!oldFile.equals(destFile)) {
            ImageCache.invalidateCachedImage(oldFile);
            Utils.deleteFileSafely(oldFile);
          }
        }

        // invalidate image cache
        if (tvShowSeason != null) {
          MediaFile mf = new MediaFile(destFile, MediaFileType.getMediaFileType(artworkType));
          mf.gatherMediaInformation();
          tvShowSeason.setArtwork(mf);
        }

        // build up image cache
        ImageCache.invalidateCachedImage(destFile);
        ImageCache.cacheImageSilently(destFile);
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("fetch image {} - {}", this.url, e);
        // fallback
        if (tvShowSeason != null && !oldFilename.isEmpty()) {
          MediaFile mf = new MediaFile(Paths.get(oldFilename), MediaFileType.getMediaFileType(artworkType));
          mf.gatherMediaInformation();
          tvShowSeason.setArtwork(mf);
        }
        // build up image cache
        ImageCache.invalidateCachedImage(Paths.get(oldFilename));
        ImageCache.cacheImageSilently(Paths.get(oldFilename));
      }
      finally {
        tvShow.saveToDb();
      }
    }
  }

  /**
   * set the found artwork for the given {@link TvShow}
   *
   * @param tvShow
   *          the TV show to set the artwork for
   * @param artwork
   *          a list of all artworks to be set
   * @param config
   *          the config which artwork to set
   * @param overwrite
   *          should we overwrite existing artwork
   */
  public static void setArtwork(TvShow tvShow, List<MediaArtwork> artwork, List<TvShowScraperMetadataConfig> config, boolean overwrite) {
    if (!ScraperMetadataConfig.containsAnyArtwork(config)) {
      return;
    }

    // sort artwork once again (langu/rating)
    artwork.sort(new MediaArtwork.MediaArtworkComparator(TvShowModuleManager.getInstance().getSettings().getScraperLanguage().name()));

    // poster
    if (config.contains(TvShowScraperMetadataConfig.POSTER) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.POSTER)))) {
      setBestArtwork(tvShow, artwork, POSTER);
    }

    // fanart
    if (config.contains(TvShowScraperMetadataConfig.FANART) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.FANART)))) {
      setBestFanart(tvShow, artwork);
    }

    // banner
    if (config.contains(TvShowScraperMetadataConfig.BANNER) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.BANNER)))) {
      setBestArtwork(tvShow, artwork, BANNER);
    }

    // logo
    if (config.contains(TvShowScraperMetadataConfig.LOGO) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.LOGO)))) {
      setBestArtwork(tvShow, artwork, LOGO);
    }

    // clearlogo
    if (config.contains(TvShowScraperMetadataConfig.CLEARLOGO)
        && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.CLEARLOGO)))) {
      setBestArtwork(tvShow, artwork, CLEARLOGO);
    }

    // clearart
    if (config.contains(TvShowScraperMetadataConfig.CLEARART)
        && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.CLEARART)))) {
      setBestArtwork(tvShow, artwork, CLEARART);
    }

    // thumb
    if (config.contains(TvShowScraperMetadataConfig.THUMB) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.THUMB)))) {
      setBestArtwork(tvShow, artwork, THUMB);
    }

    // characterart
    if (config.contains(TvShowScraperMetadataConfig.CHARACTERART)) {
      setBestArtwork(tvShow, artwork, CHARACTERART);
    }

    // keyart
    if (config.contains(TvShowScraperMetadataConfig.KEYART) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.KEYART)))) {
      setBestArtwork(tvShow, artwork, KEYART);
    }

    // season poster
    if (config.contains(TvShowScraperMetadataConfig.SEASON_POSTER)) {
      HashMap<Integer, String> seasonPosters = new HashMap<>();
      for (MediaArtwork art : artwork) {
        if (art.getType() == MediaArtworkType.SEASON_POSTER && art.getSeason() >= 0) {
          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(tvShow.getSeasonArtwork(art.getSeason(), SEASON_POSTER))) {
            String url = seasonPosters.get(art.getSeason());
            if (StringUtils.isBlank(url)) {
              tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_POSTER);
              TvShowArtworkHelper.downloadSeasonArtwork(tvShow, art.getSeason(), SEASON_POSTER);
              seasonPosters.put(art.getSeason(), art.getDefaultUrl());
            }
          }
        }
      }
    }

    // season fanart
    if (config.contains(TvShowScraperMetadataConfig.SEASON_FANART)) {
      HashMap<Integer, String> seasonFanarts = new HashMap<>();
      for (MediaArtwork art : artwork) {
        if (art.getType() == SEASON_FANART && art.getSeason() >= 0) {
          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(tvShow.getSeasonArtwork(art.getSeason(), SEASON_FANART))) {
            String url = seasonFanarts.get(art.getSeason());
            if (StringUtils.isBlank(url)) {
              tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_FANART);
              TvShowArtworkHelper.downloadSeasonArtwork(tvShow, art.getSeason(), SEASON_FANART);
              seasonFanarts.put(art.getSeason(), art.getDefaultUrl());
            }
          }
        }
      }
    }

    // season banner
    if (config.contains(TvShowScraperMetadataConfig.SEASON_BANNER)) {
      HashMap<Integer, String> seasonBanners = new HashMap<>();
      for (MediaArtwork art : artwork) {
        if (art.getType() == MediaArtworkType.SEASON_BANNER && art.getSeason() >= 0) {
          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(tvShow.getSeasonArtwork(art.getSeason(), SEASON_BANNER))) {
            String url = seasonBanners.get(art.getSeason());
            if (StringUtils.isBlank(url)) {
              tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_BANNER);
              TvShowArtworkHelper.downloadSeasonArtwork(tvShow, art.getSeason(), SEASON_BANNER);
              seasonBanners.put(art.getSeason(), art.getDefaultUrl());
            }
          }
        }
      }
    }

    // season thumb
    if (config.contains(TvShowScraperMetadataConfig.SEASON_THUMB)) {
      HashMap<Integer, String> seasonThumbs = new HashMap<>();
      for (MediaArtwork art : artwork) {
        if (art.getType() == MediaArtworkType.SEASON_THUMB && art.getSeason() >= 0) {
          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(tvShow.getSeasonArtwork(art.getSeason(), SEASON_THUMB))) {
            String url = seasonThumbs.get(art.getSeason());
            if (StringUtils.isBlank(url)) {
              tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_THUMB);
              TvShowArtworkHelper.downloadSeasonArtwork(tvShow, art.getSeason(), SEASON_THUMB);
              seasonThumbs.put(art.getSeason(), art.getDefaultUrl());
            }
          }
        }
      }
    }

    // extrafanart
    if (config.contains(TvShowScraperMetadataConfig.EXTRAFANART)
        && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.EXTRAFANART)))) {
      List<String> extrafanarts = new ArrayList<>();
      if (TvShowModuleManager.getInstance().getSettings().isImageExtraFanart()
          && TvShowModuleManager.getInstance().getSettings().getImageExtraFanartCount() > 0) {
        for (MediaArtwork art : artwork) {
          // only get artwork in desired resolution
          if (art.getType() == MediaArtworkType.BACKGROUND) {
            extrafanarts.add(art.getDefaultUrl());
            if (extrafanarts.size() >= TvShowModuleManager.getInstance().getSettings().getImageExtraFanartCount()) {
              break;
            }
          }
        }
        tvShow.setExtraFanartUrls(extrafanarts);
        if (!extrafanarts.isEmpty()) {
          downloadArtwork(tvShow, MediaFileType.EXTRAFANART);
        }
      }
    }

    // update DB
    tvShow.saveToDb();
    tvShow.writeNFO(); // to get the artwork urls into the NFO
  }

  private static void downloadExtraArtwork(TvShow tvShow, MediaFileType type) {
    // get images in thread
    TvShowExtraImageFetcherTask task = new TvShowExtraImageFetcherTask(tvShow, type);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
  }

  /**
   * extract embedded artwork from a VSMETA file to the destinations specified in the settings
   *
   * @param tvShow
   *          the {@link TvShow} to assign the new {@link MediaFile}s to
   * @param vsMetaFile
   *          the VSMETA {@link MediaFile}
   * @param artworkType
   *          the {@link MediaArtworkType}
   * @return true if extraction was successful, false otherwise
   */
  public static boolean extractArtworkFromVsmeta(TvShow tvShow, MediaFile vsMetaFile, MediaArtworkType artworkType) {
    return extractArtworkFromVsmetaInternal(tvShow, vsMetaFile, artworkType);
  }

  /**
   * extract embedded artwork from a VSMETA file to the destinations specified in the settings
   *
   * @param tvShowEpisode
   *          the {@link TvShow} to assign the new {@link MediaFile}s to
   * @param vsMetaFile
   *          the VSMETA {@link MediaFile}
   * @param artworkType
   *          the {@link MediaArtworkType}
   * @return true if extraction was successful, false otherwise
   */
  public static boolean extractArtworkFromVsmeta(TvShowEpisode tvShowEpisode, MediaFile vsMetaFile, MediaArtworkType artworkType) {
    return extractArtworkFromVsmetaInternal(tvShowEpisode, vsMetaFile, artworkType);
  }

  private static boolean extractArtworkFromVsmetaInternal(MediaEntity mediaEntity, MediaFile vsMetaFile, MediaArtworkType artworkType) {
    VSMeta vsmeta = new VSMeta(vsMetaFile.getFileAsPath());
    List<? extends IFileNaming> fileNamings;
    byte[] bytes;

    if (mediaEntity instanceof TvShow) {
      switch (artworkType) {
        case POSTER:
          fileNamings = TvShowModuleManager.getInstance().getSettings().getPosterFilenames();
          bytes = vsmeta.getShowImageBytes();
          break;

        case BACKGROUND:
          fileNamings = TvShowModuleManager.getInstance().getSettings().getFanartFilenames();
          bytes = vsmeta.getBackdropBytes();
          break;

        default:
          return false;
      }
    }
    else if (mediaEntity instanceof TvShowEpisode && artworkType == THUMB) {
      fileNamings = TvShowModuleManager.getInstance().getSettings().getEpisodeThumbFilenames();
      bytes = vsmeta.getPosterBytes();
    }
    else {
      return false;
    }

    if (fileNamings.isEmpty() || bytes.length == 0) {
      return false;
    }

    // remove .ext.vsmeta
    String basename = FilenameUtils.getBaseName(FilenameUtils.getBaseName(vsMetaFile.getFilename()));

    for (IFileNaming fileNaming : fileNamings) {
      try {
        String filename = fileNaming.getFilename(basename, "jpg"); // need to force jpg here since we do know it better
        MediaFile mf = new MediaFile(vsMetaFile.getFileAsPath().getParent().resolve(filename), MediaFileType.getMediaFileType(artworkType));
        Files.write(mf.getFileAsPath(), bytes);
        mediaEntity.addToMediaFiles(mf);
      }
      catch (Exception e) {
        LOGGER.warn("could not extract VSMETA artwork: {}", e.getMessage());
      }
    }

    return true;
  }

  /**
   * parse out the last number of the filename which is the index of extra artwork
   *
   * @param filename
   *          the filename containing the index
   * @return the detected index or -1
   */
  public static int getIndexOfArtwork(String filename) {
    String basename = FilenameUtils.getBaseName(filename);
    Matcher matcher = INDEX_PATTERN.matcher(basename);
    if (matcher.find() && matcher.groupCount() == 1) {
      try {
        return Integer.parseInt(matcher.group(1));
      }
      catch (Exception e) {
        LOGGER.debug("could not parse index of '{}'- {}", filename, e.getMessage());
      }
    }

    return -1;
  }
}
