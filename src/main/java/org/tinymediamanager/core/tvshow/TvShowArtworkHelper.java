/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CHARACTERART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.KEYART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_FANART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_THUMB;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.tinymediamanager.core.tvshow.tasks.TvShowExtraImageFetcherTask;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
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
    throw new IllegalAccessError();
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
      setBestPoster(tvShow, artwork);
    }

    // fanart
    if (tvShow.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      setBestFanart(tvShow, artwork);
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
      for (MediaArtwork art : artwork.stream().filter(mediaArtwork -> mediaArtwork.getSeason() == season.getSeason()).toList()) {
        if (StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_POSTER))) {
          season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_POSTER);
          downloadSeasonArtwork(season, TvShowModuleManager.getInstance().getSettings().getSeasonPosterFilenames(), MediaFileType.SEASON_POSTER);
        }
        else if (StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_FANART))) {
          season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_FANART);
          downloadSeasonArtwork(season, TvShowModuleManager.getInstance().getSettings().getSeasonFanartFilenames(), MediaFileType.SEASON_FANART);
        }
        else if (StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_BANNER))) {
          season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_BANNER);
          downloadSeasonArtwork(season, TvShowModuleManager.getInstance().getSettings().getSeasonBannerFilenames(), MediaFileType.SEASON_BANNER);
        }
        else if (StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_THUMB))) {
          season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_THUMB);
          downloadSeasonArtwork(season, TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames(), MediaFileType.SEASON_THUMB);
        }
      }
    }

    // update DB
    tvShow.saveToDb();
  }

  private static void setBestPoster(TvShow tvShow, List<MediaArtwork> artwork) {
    int preferredSizeOrder = TvShowModuleManager.getInstance().getSettings().getImagePosterSize().getOrder();

    // sort artwork due to our preferences
    List<MediaArtwork> sortedPosters = sortArtwork(artwork, MediaArtworkType.POSTER, preferredSizeOrder);

    // assign and download the poster
    if (!sortedPosters.isEmpty()) {
      MediaArtwork foundPoster = sortedPosters.get(0);
      tvShow.setArtworkUrl(foundPoster.getDefaultUrl(), MediaFileType.POSTER);

      downloadArtwork(tvShow, MediaFileType.POSTER);
    }
  }

  /**
   * sort the artwork according to our preferences: <br/>
   * 1) the right language and the right resolution<br/>
   * 2) the right language and down to 2 resolutions lower (if language has priority)<br/>
   * 3) the right resolution<br/>
   * 4) down to 2 resolutions lower<br/>
   * 5) the first we find
   *
   * @param artwork
   *          the artworks to search in
   * @param type
   *          the artwork type
   * @param sizeOrder
   *          the preferred size
   * @return the found artwork or null
   */
  private static List<MediaArtwork> sortArtwork(List<MediaArtwork> artwork, MediaArtworkType type, int sizeOrder) {
    List<MediaArtwork> sortedArtwork = new ArrayList<>();

    if (artwork.isEmpty()) {
      return sortedArtwork;
    }

    List<MediaLanguages> languages = TvShowModuleManager.getInstance().getSettings().getImageScraperLanguages();

    // get the artwork in the chosen language priority
    for (MediaLanguages language : languages) {
      // the right language and the right resolution
      for (MediaArtwork art : artwork) {
        if (!sortedArtwork.contains(art) && art.getType() == type && art.getLanguage().equals(language.getLanguage())
                && art.getSizeOrder() == sizeOrder) {
          sortedArtwork.add(art);
        }
      }
    }

    // do we want to take other resolution artwork?
    if (TvShowModuleManager.getInstance().getSettings().isImageScraperOtherResolutions()) {
      int newOrder = MediaArtwork.MAX_IMAGE_SIZE_ORDER;
      while (newOrder > 1) {
        newOrder = newOrder / 2;
        for (MediaLanguages language : languages) {
          // the right language and the right resolution
          for (MediaArtwork art : artwork) {
            if (!sortedArtwork.contains(art) && art.getType() == type && art.getLanguage().equals(language.getLanguage())
                    && art.getSizeOrder() == sizeOrder) {
              sortedArtwork.add(art);
            }
          }
        }
      }
    }

    // should we fall back to _any_ artwork?
    if (TvShowModuleManager.getInstance().getSettings().isImageScraperFallback()) {
      for (MediaArtwork art : artwork) {
        if (!sortedArtwork.contains(art) && art.getType() == type) {
          sortedArtwork.add(art);
        }
      }
    }

    return sortedArtwork;
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
    int preferredSizeOrder = TvShowModuleManager.getInstance().getSettings().getImageFanartSize().getOrder();

    // sort artwork due to our preferences
    List<MediaArtwork> sortedArtwork = new ArrayList<>(artwork);

    // according to the kodi specifications the fanart _should_ be without any text on it - so we try to get the text-less image (in the right
    // resolution) first
    // https://kodi.wiki/view/Artwork_types#fanart
    MediaArtwork fanartWoText = null;
    for (MediaArtwork art : sortedArtwork) {
      if (art.getType() == MediaArtworkType.BACKGROUND && art.getLanguage().equals("-")) {
        fanartWoText = art;
        break;
      }
    }

    // sort artwork due to our preferences
    List<MediaArtwork> sortedFanarts = sortArtwork(artwork, MediaArtworkType.BACKGROUND, preferredSizeOrder);

    if (fanartWoText != null) {
      sortedFanarts.add(0, fanartWoText);
    }

    // assign and download the fanart
    if (!sortedFanarts.isEmpty()) {
      MediaArtwork foundfanart = sortedFanarts.get(0);
      tvShow.setArtworkUrl(foundfanart.getDefaultUrl(), MediaFileType.FANART);
      downloadArtwork(tvShow, MediaFileType.FANART);
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
          && StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_POSTER))) {
        return true;
      }
      if (config.contains(TvShowScraperMetadataConfig.SEASON_FANART)
          && !TvShowModuleManager.getInstance().getSettings().getSeasonFanartFilenames().isEmpty()
          && StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_FANART))) {
        return true;
      }
      if (config.contains(TvShowScraperMetadataConfig.SEASON_BANNER)
          && !TvShowModuleManager.getInstance().getSettings().getSeasonBannerFilenames().isEmpty()
          && StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_BANNER))) {
        return true;
      }
      if (config.contains(TvShowScraperMetadataConfig.SEASON_THUMB)
          && !TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames().isEmpty()
          && StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_THUMB))) {
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

  /**
   * download the {@link TvShowSeason} artwork
   * 
   * @param tvShowSeason
   *          the {@link TvShowSeason} to download the artwork for
   * @param artworkType
   *          the {@link MediaFileType} to download
   */
  public static void downloadSeasonArtwork(TvShowSeason tvShowSeason, MediaFileType artworkType) {
    switch (artworkType) {
      case SEASON_POSTER ->
        downloadSeasonArtwork(tvShowSeason, TvShowModuleManager.getInstance().getSettings().getSeasonPosterFilenames(), artworkType);
      case SEASON_FANART ->
        downloadSeasonArtwork(tvShowSeason, TvShowModuleManager.getInstance().getSettings().getSeasonFanartFilenames(), artworkType);
      case SEASON_BANNER ->
        downloadSeasonArtwork(tvShowSeason, TvShowModuleManager.getInstance().getSettings().getSeasonBannerFilenames(), artworkType);
      case SEASON_THUMB ->
        downloadSeasonArtwork(tvShowSeason, TvShowModuleManager.getInstance().getSettings().getSeasonThumbFilenames(), artworkType);
    }
  }

  /**
   * Download the season artwork
   * 
   * @param tvShowSeason
   *          the TV show season
   * @param fileNamings
   *          all {@link ITvShowSeasonFileNaming}s for this artwork/file type
   * @param mediaFileType
   *          the artwork/file type
   */
  private static void downloadSeasonArtwork(TvShowSeason tvShowSeason, List<? extends ITvShowSeasonFileNaming> fileNamings,
      MediaFileType mediaFileType) {

    // only write artwork if there is at least one episode in the season or the setting activated
    if (!tvShowSeason.getEpisodes().isEmpty() || TvShowModuleManager.getInstance().getSettings().isCreateMissingSeasonItems()) {
      String seasonArtworkUrl = tvShowSeason.getArtworkUrl(mediaFileType);

      for (ITvShowSeasonFileNaming fileNaming : fileNamings) {
        String filename = fileNaming.getFilename(tvShowSeason, Utils.getArtworkExtensionFromUrl(seasonArtworkUrl));
        if (StringUtils.isBlank(filename)) {
          LOGGER.warn("empty filename for artwork: {} - {}", fileNaming.name(), tvShowSeason); // NOSONAR
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, tvShowSeason, "tvshow.seasondownload.failed"));
          continue;
        }

        Path destFile = tvShowSeason.getTvShow().getPathNIO().resolve(filename);

        // check if the parent exist and create if needed
        if (!Files.exists(destFile.getParent())) {
          try {
            Files.createDirectory(destFile.getParent());
          }
          catch (IOException e) {
            LOGGER.error("could not create folder: {} - {}", destFile.getParent(), e.getMessage());
            MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, tvShowSeason, "tvshow.seasondownload.failed"));
            continue;
          }
        }

        SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(tvShowSeason, destFile, seasonArtworkUrl, mediaFileType);
        TmmTaskManager.getInstance().addImageDownloadTask(task);
      }

      // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
      if (seasonArtworkUrl.startsWith("file:")) {
        tvShowSeason.removeArtworkUrl(mediaFileType);
      }
    }
  }

  private static class SeasonArtworkImageFetcher implements Runnable {
    private final TvShowSeason  tvShowSeason;
    private final MediaFileType mediaFileType;
    private final Path          destinationPath;
    private final String        filename;
    private final String        url;

    SeasonArtworkImageFetcher(TvShowSeason tvShowSeason, Path destFile, String url, MediaFileType mediaFileType) {
      this.tvShowSeason = tvShowSeason;
      this.destinationPath = destFile.getParent();
      this.filename = destFile.getFileName().toString();
      this.mediaFileType = mediaFileType;
      this.url = url;
    }

    @Override
    public void run() {
      String oldFilename = tvShowSeason.getArtworkFilename(mediaFileType);
      Path oldFile = Paths.get(oldFilename);

      LOGGER.debug("writing season artwork {}", filename);

      // fetch and store images
      try {
        Path destFile = ImageUtils.downloadImage(url, destinationPath, filename);

        // if the old filename differs from the new one (e.g. .jpg -> .png), remove the old one
        if (StringUtils.isNotBlank(oldFilename)) {
          if (!oldFile.equals(destFile)) {
            ImageCache.invalidateCachedImage(oldFile);
            Utils.deleteFileSafely(oldFile);
          }
        }

        tvShowSeason.setArtwork(destFile, mediaFileType);

        // build up image cache
        ImageCache.invalidateCachedImage(destFile);
        ImageCache.cacheImageSilently(destFile);
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("fetch image {} - {}", this.url, e.getMessage());
        // fallback
        if (!oldFilename.isEmpty()) {
          tvShowSeason.setArtwork(oldFile, mediaFileType);
        }
        // build up image cache
        ImageCache.invalidateCachedImage(oldFile);
        ImageCache.cacheImageSilently(oldFile);
      }
      finally {
        tvShowSeason.getTvShow().saveToDb();
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
      setBestPoster(tvShow, artwork);
    }

    // fanart
    if (config.contains(TvShowScraperMetadataConfig.FANART) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.FANART)))) {
      setBestFanart(tvShow, artwork);
    }

    // banner
    if (config.contains(TvShowScraperMetadataConfig.BANNER) && (overwrite || StringUtils.isBlank(tvShow.getArtworkFilename(MediaFileType.BANNER)))) {
      setBestArtwork(tvShow, artwork, BANNER);
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
      for (MediaArtwork art : artwork) {
        if (art.getType() == SEASON_POSTER && art.getSeason() >= 0) {
          TvShowSeason season = tvShow.getOrCreateSeason(art.getSeason());

          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_POSTER))) {
            season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_POSTER);
            downloadSeasonArtwork(season, MediaFileType.SEASON_POSTER);
          }
        }
      }
    }

    // season fanart
    if (config.contains(TvShowScraperMetadataConfig.SEASON_FANART)) {
      for (MediaArtwork art : artwork) {
        if (art.getType() == SEASON_FANART && art.getSeason() >= 0) {
          TvShowSeason season = tvShow.getOrCreateSeason(art.getSeason());

          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_FANART))) {
            season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_FANART);
            downloadSeasonArtwork(season, MediaFileType.SEASON_FANART);
          }
        }
      }
    }

    // season banner
    if (config.contains(TvShowScraperMetadataConfig.SEASON_BANNER)) {
      for (MediaArtwork art : artwork) {
        if (art.getType() == SEASON_BANNER && art.getSeason() >= 0) {
          TvShowSeason season = tvShow.getOrCreateSeason(art.getSeason());

          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_BANNER))) {
            season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_BANNER);
            downloadSeasonArtwork(season, MediaFileType.SEASON_BANNER);
          }
        }
      }
    }

    // season thumb
    if (config.contains(TvShowScraperMetadataConfig.SEASON_THUMB)) {
      for (MediaArtwork art : artwork) {
        if (art.getType() == SEASON_THUMB && art.getSeason() >= 0) {
          TvShowSeason season = tvShow.getOrCreateSeason(art.getSeason());

          // check if there is already an artwork for this season
          if (overwrite || StringUtils.isBlank(season.getArtworkFilename(MediaFileType.SEASON_THUMB))) {
            season.setArtworkUrl(art.getDefaultUrl(), MediaFileType.SEASON_THUMB);
            downloadSeasonArtwork(season, MediaFileType.SEASON_THUMB);
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
