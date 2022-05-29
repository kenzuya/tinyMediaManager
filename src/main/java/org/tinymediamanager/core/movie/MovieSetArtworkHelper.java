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
package org.tinymediamanager.core.movie;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IFileNaming;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.movie.filenaming.IMovieSetFileNaming;
import org.tinymediamanager.core.tasks.ImageCacheTask;
import org.tinymediamanager.core.tasks.MediaFileInformationFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.http.Url;

/**
 * The class MovieSetArtworkHelper. A helper class for managing movie set artwork
 *
 * @author Manuel Laggner
 */
public class MovieSetArtworkHelper {
  private static final Logger              LOGGER                      = LoggerFactory.getLogger(MovieSetArtworkHelper.class);

  private static final List<MediaFileType> SUPPORTED_ARTWORK_TYPES     = Arrays.asList(MediaFileType.POSTER, MediaFileType.FANART,
      MediaFileType.BANNER, MediaFileType.LOGO, MediaFileType.CLEARLOGO, MediaFileType.CLEARART, MediaFileType.THUMB, MediaFileType.DISC);
  private static final String[]            SUPPORTED_ARTWORK_FILETYPES = { "jpg", "png", "tbn", "webp" };

  private MovieSetArtworkHelper() {
    // hide default constructor for utility classes
  }

  /**
   * Update the artwork for a given {@link MovieSet}. This should be triggered after every {@link MovieSet} change like creating, adding movies,
   * removing movies
   * 
   * @param movieSet
   *          the movie set to update the artwork for
   */
  public static void updateArtwork(MovieSet movieSet) {
    // find artwork in the artwork dir
    findArtworkInArtworkFolder(movieSet);

    // find artwork in the movie dirs
    for (Movie movie : new ArrayList<>(movieSet.getMovies())) {
      findArtworkInMovieFolder(movieSet, movie);
    }
  }

  /**
   * cleanup the artwork for the given {@link MovieSet}. Move the artwork to the specified (settings) artwork folder or movie folder
   *
   * @param movieSet
   *          the {@link MovieSet} to do the cleanup for
   */
  public static void cleanupArtwork(MovieSet movieSet) {
    Path artworkFolder = getArtworkFolder();

    List<MediaFile> cleanup = new ArrayList<>();
    Set<MediaFile> needed = new TreeSet<>();

    // we can have 0..n different media files for every type (1 in every artwork folder type and 1 in every movie folder)
    // we will give any available image in our specified artwork folder priority over the ones from the movie files
    for (MediaFileType type : SUPPORTED_ARTWORK_TYPES) {
      // get the desired artwork filenames
      List<IMovieSetFileNaming> fileNamings = getFileNamingsForMediaFileType(type);
      if (fileNamings.isEmpty()) {
        continue;
      }

      List<MediaFile> mediaFiles = movieSet.getMediaFiles(type);
      cleanup.addAll(mediaFiles);

      // remove all 0 size files & not existing files
      mediaFiles = mediaFiles.stream().filter(mf -> {
        if (mf.getFilesize() == 0) {
          return false;
        }
        if (!Files.exists(mf.getFile())) {
          return false;
        }
        return true;
      }).collect(Collectors.toList());

      if (mediaFiles.isEmpty()) {
        continue;
      }

      // search all available folders (with our preference)
      MediaFile artworkFile = getArtworkFromMediaFiles(movieSet, mediaFiles, type, fileNamings);

      // now we _should_ have at least one artwork file; now distribute that to all other places (if needed)
      if (artworkFile != null) {
        // copy to the movie set artwork folder
        for (Path path : createArtworkPathsInArtworkFolder(movieSet, fileNamings, artworkFile.getExtension())) {
          try {
            // clone mf
            MediaFile newFile = new MediaFile(artworkFile);
            newFile.setFile(path);
            boolean ok = MovieRenamer.copyFile(artworkFile.getFileAsPath(), newFile.getFileAsPath());
            if (ok) {
              needed.add(newFile);
            }
            else {
              // not copied/exception... keep it for now...
              needed.add(artworkFile);
            }
          }
          catch (Exception e) {
            LOGGER.warn("could not write files", e);
          }
        }

        // copy to each movie folder
        for (IMovieSetFileNaming filenaming : fileNamings) {
          if (filenaming.getFolderLocation() != IMovieSetFileNaming.Location.MOVIE_FOLDER) {
            continue;
          }

          String filename = filenaming.getFilename("", artworkFile.getExtension());
          for (Movie movie : movieSet.getMovies()) {
            try {
              if (!movie.isMultiMovieDir()) {
                // clone mf
                MediaFile newFile = new MediaFile(artworkFile);
                newFile.setFile(movie.getPathNIO().resolve(filename));
                boolean ok = MovieRenamer.copyFile(artworkFile.getFileAsPath(), newFile.getFileAsPath());
                if (ok) {
                  needed.add(newFile);
                }
                else {
                  // not copied/exception... keep it for now...
                  needed.add(artworkFile);
                }
              }
              else {
                LOGGER.trace("not writing movie set artwork file to MMD - '{}'", movie.getPathNIO());
              }
            }
            catch (Exception e) {
              LOGGER.warn("could not write files", e);
            }
          }
        }
      }
    }

    // re-create the image cache on all new files
    if (!needed.isEmpty() && Settings.getInstance().isImageCache()) {
      ImageCacheTask task = new ImageCacheTask(new ArrayList<>(needed));
      TmmTaskManager.getInstance().addUnnamedTask(task);
    }

    // and assign it to the movie set
    cleanup.forEach(movieSet::removeFromMediaFiles);
    movieSet.addToMediaFiles(new ArrayList<>(needed));

    // now remove all unnecessary ones
    for (int i = cleanup.size() - 1; i >= 0; i--) {
      MediaFile cl = cleanup.get(i);

      // cleanup files which are not needed
      if (!needed.contains(cl)) {
        LOGGER.debug("Deleting {}", cl.getFileAsPath());
        Utils.deleteFileSafely(cl.getFileAsPath());
        // also cleanup the cache for deleted mfs
        ImageCache.invalidateCachedImage(cl);

        // also remove emtpy folders
        try {
          if ((artworkFolder != null && !artworkFolder.equals(cl.getFile().getParent())) && Utils.isFolderEmpty(cl.getFile().getParent())) {
            LOGGER.debug("Deleting empty Directory {}", cl.getFileAsPath().getParent());
            Files.delete(cl.getFileAsPath().getParent()); // do not use recursive her
          }
        }
        catch (IOException e) {
          LOGGER.warn("could not search for empty dir: {}", e.getMessage());
        }
      }
    }

    movieSet.saveToDb();
  }

  /**
   * get a {@link Path} to the artwork folder
   * 
   * @return the {@link Path} to the artwork folder or null
   */
  private static Path getArtworkFolder() {
    String artworkFolder = MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder();
    if (StringUtils.isBlank(artworkFolder)) {
      return null;
    }
    return Paths.get(artworkFolder);
  }

  /**
   * Create the path to the artwork file path inside the artwork folder. If the artwork folder is not activated this may return null
   * 
   * @param movieSet
   *          the movie set to create the file path for
   * @param extension
   *          the extension of the artwork file
   * @param filenamings
   *          the file namings to create the paths for
   * @return a {@link Path} to the artwork file
   */
  private static List<Path> createArtworkPathsInArtworkFolder(MovieSet movieSet, List<IMovieSetFileNaming> filenamings, String extension) {
    Path artworkFolder = getArtworkFolder();
    if (artworkFolder == null) {
      return Collections.emptyList();
    }

    List<Path> paths = new ArrayList<>();
    String movieSetName = movieSet.getTitleForStorage();

    for (IMovieSetFileNaming fileNaming : filenamings) {
      if (fileNaming.getFolderLocation() == IMovieSetFileNaming.Location.KODI_STYLE_FOLDER) {
        // Kodi style: <movie set artwork folder>/<movie set name>/<artwork type>.ext
        paths.add(Paths.get(artworkFolder.toString(), movieSetName, fileNaming.getFilename(movieSetName, extension)));
      }
      else if (fileNaming.getFolderLocation() == IMovieSetFileNaming.Location.AUTOMATOR_STYLE_FOLDER) {
        // Artwork Automator style: <movie set artwork folder>/<movie set name>-<artwork type>.ext
        paths.add(Paths.get(artworkFolder.toString(), fileNaming.getFilename(movieSetName, extension)));
      }
    }

    return paths;
  }

  /**
   * find the artwork from the artwork folder or movie folders
   *
   * @param movieSet
   *          the {@link MovieSet} to find the artwork for
   * @param mediaFiles
   *          the {@link MediaFile}s to search the artwork for
   * @param type
   *          the {@link MediaFileType} to search if the file name search fails
   * @param fileNamings
   *          a list of all available {@link IMovieSetFileNaming}s for this artwork type
   * @return the {@link MediaFile} in the preferred artwork folder or null
   */
  private static MediaFile getArtworkFromMediaFiles(MovieSet movieSet, List<MediaFile> mediaFiles, MediaFileType type,
      List<IMovieSetFileNaming> fileNamings) {
    Path artworkFolder = getArtworkFolder();
    if (artworkFolder != null) {
      // try to resolve via the filename
      for (IMovieSetFileNaming fileNaming : fileNamings) {
        for (MediaFile mediaFile : mediaFiles) {
          // first try to use our old logic
          String movieSetName = MovieRenamer.replaceInvalidCharacters(movieSet.getTitle());

          // also remove illegal separators
          movieSetName = MovieRenamer.replacePathSeparators(movieSetName);

          if (isMediaFileInArtworkFolder(movieSetName, artworkFolder, fileNaming, mediaFile)) {
            return mediaFile;
          }

          // second, try the Kodi style
          movieSetName = movieSet.getTitleForStorage();

          if (isMediaFileInArtworkFolder(movieSetName, artworkFolder, fileNaming, mediaFile)) {
            return mediaFile;
          }
        }
      }
    }

    // not found via filename? maybe the name changed or only in movie folders -> search for the type
    for (MediaFile mediaFile : mediaFiles) {
      if (mediaFile.getType() == type) {
        return mediaFile;
      }
    }

    return null;
  }

  private static boolean isMediaFileInArtworkFolder(String cleanMovieSetName, Path artworkFolder, IMovieSetFileNaming fileNaming,
      MediaFile mediaFile) {
    if (fileNaming.getFolderLocation() == IMovieSetFileNaming.Location.KODI_STYLE_FOLDER) {
      // Kodi style: <movie set artwork folder>/<movie set name>/<artwork type>.ext
      Path path = Paths.get(artworkFolder.toString(), cleanMovieSetName, fileNaming.getFilename(cleanMovieSetName, ""));
      return mediaFile.getFileAsPath().toAbsolutePath().startsWith(path);
    }
    else if (fileNaming.getFolderLocation() == IMovieSetFileNaming.Location.AUTOMATOR_STYLE_FOLDER) {
      // Artwork Automator style: <movie set artwork folder>/<movie set name>-<artwork type>.ext
      Path path = Paths.get(artworkFolder.toString(), fileNaming.getFilename(cleanMovieSetName, ""));
      return mediaFile.getFileAsPath().toAbsolutePath().startsWith(path);
    }
    else {
      return mediaFile.getFilename().contains(fileNaming.getFilename("", ""));
    }
  }

  /**
   * find and assign movie set artwork in the artwork folder
   * 
   * @param movieSet
   *          the movie set to search artwork for
   */
  private static void findArtworkInArtworkFolder(MovieSet movieSet) {
    String artworkFolder = MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder();
    if (StringUtils.isBlank(artworkFolder)) {
      return;
    }

    // here we have 2 kinds of file names in the movie set artwork folder:
    // a) the movie set artwork automator style: <artwork folder>/<movie set name>-<artwork type>.ext
    // b) Artwork Beef style: <artwork folder>/<movie set name>/<artwork type>.ext

    // a)
    for (MediaFileType type : SUPPORTED_ARTWORK_TYPES) {
      for (String fileType : SUPPORTED_ARTWORK_FILETYPES) {
        // old tmm style
        String movieSetName = MovieRenamer.replaceInvalidCharacters(movieSet.getTitle());

        // also remove illegal separators
        movieSetName = MovieRenamer.replacePathSeparators(movieSetName);

        String artworkFileName = movieSetName + "-" + type.name().toLowerCase(Locale.ROOT) + "." + fileType;
        Path artworkFile = Paths.get(artworkFolder, artworkFileName);
        if (Files.exists(artworkFile)) {
          // add this artwork to the media files
          MediaFile mediaFile = new MediaFile(artworkFile, type);
          TmmTaskManager.getInstance().addUnnamedTask(new MediaFileInformationFetcherTask(mediaFile, movieSet, false));
          movieSet.addToMediaFiles(mediaFile);
        }

        // Kodi style
        movieSetName = movieSet.getTitleForStorage();

        artworkFileName = movieSetName + "-" + type.name().toLowerCase(Locale.ROOT) + "." + fileType;
        artworkFile = Paths.get(artworkFolder, artworkFileName);
        if (Files.exists(artworkFile)) {
          // add this artwork to the media files
          MediaFile mediaFile = new MediaFile(artworkFile, type);
          TmmTaskManager.getInstance().addUnnamedTask(new MediaFileInformationFetcherTask(mediaFile, movieSet, false));
          movieSet.addToMediaFiles(mediaFile);
        }
      }
    }

    // b)
    for (MediaFileType type : SUPPORTED_ARTWORK_TYPES) {
      for (String fileType : SUPPORTED_ARTWORK_FILETYPES) {
        // old tmm style
        String movieSetName = MovieRenamer.replaceInvalidCharacters(movieSet.getTitle());

        // also remove illegal separators
        movieSetName = MovieRenamer.replacePathSeparators(movieSetName);

        String artworkFileName = type.name().toLowerCase(Locale.ROOT) + "." + fileType;
        Path artworkFile = Paths.get(artworkFolder, movieSetName, artworkFileName);
        if (Files.exists(artworkFile)) {
          // add this artwork to the media files
          MediaFile mediaFile = new MediaFile(artworkFile, type);
          TmmTaskManager.getInstance().addUnnamedTask(new MediaFileInformationFetcherTask(mediaFile, movieSet, false));
          movieSet.addToMediaFiles(mediaFile);
        }

        // Kodi style
        movieSetName = movieSet.getTitleForStorage();

        artworkFileName = type.name().toLowerCase(Locale.ROOT) + "." + fileType;
        artworkFile = Paths.get(artworkFolder, movieSetName, artworkFileName);
        if (Files.exists(artworkFile)) {
          // add this artwork to the media files
          MediaFile mediaFile = new MediaFile(artworkFile, type);
          TmmTaskManager.getInstance().addUnnamedTask(new MediaFileInformationFetcherTask(mediaFile, movieSet, false));
          movieSet.addToMediaFiles(mediaFile);
        }
      }
    }
  }

  /**
   * find and assign movie set artwork in the movie folder
   * 
   * @param movieSet
   *          the movie set to set the artwork for
   * @param movie
   *          the movie to look for the movie set artwork
   */
  private static void findArtworkInMovieFolder(MovieSet movieSet, Movie movie) {
    for (MediaFileType type : SUPPORTED_ARTWORK_TYPES) {
      // only if there is not yet any artwork assigned
      if (!movieSet.getMediaFiles(type).isEmpty()) {
        continue;
      }

      for (String fileType : SUPPORTED_ARTWORK_FILETYPES) {
        String artworkFileName = "movieset-" + type.name().toLowerCase(Locale.ROOT) + "." + fileType;
        Path artworkFile = movie.getPathNIO().resolve(artworkFileName);
        if (Files.exists(artworkFile)) {
          // add this artwork to the media files
          MediaFile mediaFile = new MediaFile(artworkFile, type);
          TmmTaskManager.getInstance().addUnnamedTask(new MediaFileInformationFetcherTask(mediaFile, movieSet, false));
          movieSet.addToMediaFiles(mediaFile);
        }
      }
    }
  }

  /**
   * set the found artwork for the given movie
   *
   * @param movieSet
   *          the movie set to set the artwork for
   * @param artwork
   *          a list of all artworks to be set
   * @param config
   *          the config which artwork to set
   */
  public static void setArtwork(MovieSet movieSet, List<MediaArtwork> artwork, List<MovieSetScraperMetadataConfig> config) {
    if (!ScraperMetadataConfig.containsAnyArtwork(config)) {
      return;
    }

    // poster
    if (config.contains(MovieSetScraperMetadataConfig.POSTER)) {
      setBestPoster(movieSet, artwork);
    }

    // fanart
    if (config.contains(MovieSetScraperMetadataConfig.FANART)) {
      setBestFanart(movieSet, artwork);
    }

    // works now for single & multimovie
    if (config.contains(MovieSetScraperMetadataConfig.LOGO)) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.LOGO);
    }
    if (config.contains(MovieSetScraperMetadataConfig.CLEARLOGO)) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.CLEARLOGO);
    }
    if (config.contains(MovieSetScraperMetadataConfig.CLEARART)) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.CLEARART);
    }
    if (config.contains(MovieSetScraperMetadataConfig.BANNER)) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.BANNER);
    }
    if (config.contains(MovieSetScraperMetadataConfig.THUMB)) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.THUMB);
    }
    if (config.contains(MovieSetScraperMetadataConfig.DISCART)) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.DISC);
    }

    // update DB
    movieSet.saveToDb();
    movieSet.writeNFO();
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
   * @param language
   *          the preferred language
   * @return the found artwork or null
   */
  private static List<MediaArtwork> sortArtwork(List<MediaArtwork> artwork, MediaArtwork.MediaArtworkType type, int sizeOrder, String language) {
    List<MediaArtwork> sortedArtwork = new ArrayList<>();

    // the right language and the right resolution
    for (MediaArtwork art : artwork) {
      if (!sortedArtwork.contains(art) && art.getType() == type && art.getLanguage().equals(language) && art.getSizeOrder() == sizeOrder) {
        sortedArtwork.add(art);
      }
    }

    // the right language and down to 2 resolutions lower (if language has priority)
    if (MovieModuleManager.getInstance().getSettings().isImageLanguagePriority()) {
      int counter = 1;
      int newOrder = sizeOrder;
      while (counter <= 2) {
        newOrder = newOrder / 2;

        for (MediaArtwork art : artwork) {
          if (art.getType() == type && art.getLanguage().equals(language) && art.getSizeOrder() == newOrder) {
            sortedArtwork.add(art);
          }
        }
        counter++;
      }
    }

    // the right resolution
    for (MediaArtwork art : artwork) {
      // only get artwork in desired resolution
      if (!sortedArtwork.contains(art) && art.getType() == type && art.getSizeOrder() == sizeOrder) {
        sortedArtwork.add(art);
      }
    }

    // down to 2 resolutions lower
    int counter = 1;
    int newOrder = sizeOrder;
    while (counter <= 2) {
      newOrder = newOrder / 2;

      for (MediaArtwork art : artwork) {
        if (!sortedArtwork.contains(art) && art.getType() == type && art.getSizeOrder() == newOrder) {
          sortedArtwork.add(art);
        }
      }
      counter++;
    }

    // the first we find
    if (sortedArtwork.isEmpty() && !artwork.isEmpty()) {
      for (MediaArtwork art : artwork) {
        if (!sortedArtwork.contains(art) && art.getType() == type) {
          sortedArtwork.add(art);
        }
      }
    }

    return sortedArtwork;
  }

  /*
   * find the "best" poster in the list of artwork, assign it to the movie and download it
   */
  private static void setBestPoster(MovieSet movieSet, List<MediaArtwork> artwork) {
    int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImagePosterSize().getOrder();
    String preferredLanguage = MovieModuleManager.getInstance().getSettings().getImageScraperLanguage().getLanguage();

    // sort artwork due to our preferences
    List<MediaArtwork> sortedPosters = sortArtwork(artwork, MediaArtwork.MediaArtworkType.POSTER, preferredSizeOrder, preferredLanguage);

    // assign and download the poster
    if (!sortedPosters.isEmpty()) {
      MediaArtwork foundPoster = sortedPosters.get(0);
      movieSet.setArtworkUrl(foundPoster.getDefaultUrl(), MediaFileType.POSTER);
    }
  }

  /*
   * find the "best" fanart in the list of artwork, assign it to the movie set and download it
   */
  private static void setBestFanart(MovieSet movieSet, List<MediaArtwork> artwork) {
    int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder();
    String preferredLanguage = MovieModuleManager.getInstance().getSettings().getImageScraperLanguage().getLanguage();

    // sort artwork due to our preferences
    List<MediaArtwork> sortedFanarts = sortArtwork(artwork, MediaArtwork.MediaArtworkType.BACKGROUND, preferredSizeOrder, preferredLanguage);

    // assign and download the fanart
    if (!sortedFanarts.isEmpty()) {
      MediaArtwork foundfanart = sortedFanarts.get(0);
      movieSet.setArtworkUrl(foundfanart.getDefaultUrl(), MediaFileType.FANART);
    }
  }

  /**
   * choose the best artwork for this movieSet
   *
   * @param movieSet
   *          our movie set
   * @param artwork
   *          the artwork list
   * @param type
   *          the type to download
   */
  private static void setBestArtwork(MovieSet movieSet, List<MediaArtwork> artwork, MediaArtwork.MediaArtworkType type) {
    // sort artwork due to our preferences
    int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder();
    String preferredLanguage = MovieModuleManager.getInstance().getSettings().getImageScraperLanguage().getLanguage();
    List<MediaArtwork> sortedArtworks = sortArtwork(artwork, type, preferredSizeOrder, preferredLanguage);

    for (MediaArtwork art : sortedArtworks) {
      if (art.getType() == type) {
        movieSet.setArtworkUrl(art.getDefaultUrl(), MediaFileType.getMediaFileType(type));
        break;
      }
    }
  }

  /**
   * Manage downloading of the chosen artwork type
   *
   * @param movieSet
   *          the movie for which artwork has to be downloaded
   * @param type
   *          the type of artwork to be downloaded
   */
  public static void downloadArtwork(MovieSet movieSet, MediaFileType type) {
    String url = movieSet.getArtworkUrl(type);
    if (StringUtils.isBlank(url)) {
      return;
    }
    try {
      // get image in thread
      MovieSetImageFetcherTask task = new MovieSetImageFetcherTask(movieSet, url, type);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }
    finally {
      // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
      if (url.startsWith("file:")) {
        movieSet.removeArtworkUrl(type);
      }
    }
  }

  /**
   * (re)write the artwork to the movie folders of the given movies
   * 
   * @param movieSet
   *          the parent movie set
   * @param movies
   *          the movies to write the artwork to
   */
  public static void writeImagesToMovieFolder(MovieSet movieSet, List<Movie> movies) {
    for (MediaFileType type : SUPPORTED_ARTWORK_TYPES) {
      List<IMovieSetFileNaming> fileNamings = getFileNamingsForMediaFileType(type);
      for (IMovieSetFileNaming fileNaming : fileNamings) {
        if (fileNaming.getFolderLocation() != IMovieSetFileNaming.Location.MOVIE_FOLDER) {
          continue;
        }

        String url = movieSet.getArtworkUrl(type);
        if (StringUtils.isBlank(url)) {
          continue;
        }

        // get image in thread
        MovieSetImageFetcherTask task = new MovieSetImageFetcherTask(movieSet, url, type, Collections.singletonList(fileNaming), movies);
        TmmTaskManager.getInstance().addImageDownloadTask(task);
      }
    }
  }

  /**
   * strip out the movie set artwork from a movie folder
   *
   * @param movie
   *          the movie to strip out the movie set artwork
   */
  public static void cleanMovieSetArtworkInMovieFolder(Movie movie) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(movie.getPathNIO())) {
      for (Path entry : stream) {
        Matcher matcher = MediaFileHelper.MOVIESET_ARTWORK_PATTERN.matcher(entry.getFileName().toString());
        if (matcher.find()) {
          Utils.deleteFileSafely(entry);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("remove movie set artwork: {}", e.getMessage());
    }
  }

  /**
   * remove the whole artwork for the given {@link MovieSet}
   *
   * @param movieSet
   *          the movie set to remove the artwork for
   */
  public static void removeMovieSetArtwork(MovieSet movieSet) {
    for (MediaFile mediaFile : movieSet.getMediaFiles()) {
      if (!mediaFile.isGraphic()) {
        continue;
      }
      Utils.deleteFileSafely(mediaFile.getFile());
    }

    // and also remove any empty subfolders from the artwork folder
    if (StringUtils.isBlank(MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder())) {
      return;
    }

    Path movieSetArtworkFolder = Paths.get(MovieModuleManager.getInstance().getSettings().getMovieSetDataFolder());
    List<Path> subfolders;
    try (Stream<Path> stream = Files.walk(movieSetArtworkFolder, 1)) {
      subfolders = stream.filter(Files::isDirectory).collect(Collectors.toList());
    }
    catch (Exception e) {
      LOGGER.warn("could not clean movie set artwork subfolders - '{}'", e.getMessage());
      return;
    }

    for (Path path : subfolders) {
      if (path.equals(movieSetArtworkFolder)) {
        continue;
      }
      try {
        Utils.deleteEmptyDirectoryRecursive(path);
      }
      catch (Exception e) {
        LOGGER.warn("could not clean empty subfolder '{}' - '{}'", path, e.getMessage());
      }
    }
  }

  public static boolean hasMissingArtwork(MovieSet movieSet) {
    if (!MovieModuleManager.getInstance().getSettings().getPosterFilenames().isEmpty() && movieSet.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      return true;
    }
    if (!MovieModuleManager.getInstance().getSettings().getFanartFilenames().isEmpty() && movieSet.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      return true;
    }
    if (!MovieModuleManager.getInstance().getSettings().getBannerFilenames().isEmpty() && movieSet.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      return true;
    }
    if (!MovieModuleManager.getInstance().getSettings().getDiscartFilenames().isEmpty() && movieSet.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      return true;
    }
    if (!MovieModuleManager.getInstance().getSettings().getLogoFilenames().isEmpty() && movieSet.getMediaFiles(MediaFileType.LOGO).isEmpty()) {
      return true;
    }
    if (!MovieModuleManager.getInstance().getSettings().getClearlogoFilenames().isEmpty()
        && movieSet.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      return true;
    }
    if (!MovieModuleManager.getInstance().getSettings().getClearartFilenames().isEmpty()
        && movieSet.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      return true;
    }
    return !MovieModuleManager.getInstance().getSettings().getThumbFilenames().isEmpty() && movieSet.getMediaFiles(MediaFileType.THUMB).isEmpty();
  }

  /**
   * get the missing artwork for the given movie set
   * 
   * @param movieSet
   *          the movie set to get the artwork for
   * @param artwork
   *          a list with available artwork
   */
  public static void getMissingArtwork(MovieSet movieSet, List<MediaArtwork> artwork) {
    // poster
    if (movieSet.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      setBestPoster(movieSet, artwork);
    }

    // fanart
    if (movieSet.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      setBestFanart(movieSet, artwork);
    }

    // logo
    if (movieSet.getMediaFiles(MediaFileType.LOGO).isEmpty()) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.LOGO);
    }

    // clearlogo
    if (movieSet.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.CLEARLOGO);
    }

    // clearart
    if (movieSet.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.CLEARART);
    }

    // banner
    if (movieSet.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.BANNER);
    }

    // thumb
    if (movieSet.getMediaFiles(MediaFileType.THUMB).isEmpty()) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.THUMB);
    }

    // discart
    if (movieSet.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      setBestArtwork(movieSet, artwork, MediaArtwork.MediaArtworkType.DISC);
    }

    // update DB
    movieSet.saveToDb();
  }

  private static class MovieSetImageFetcherTask implements Runnable {
    private final MovieSet                  movieSet;
    private final String                    urlToArtwork;
    private final MediaFileType             type;

    private final List<IMovieSetFileNaming> fileNamings         = new ArrayList<>();
    private final List<Movie>               movies              = new ArrayList<>();
    private final List<MediaFile>           writtenArtworkFiles = new ArrayList<>();

    /**
     * This constructor is needed to write a kind of artwork to the configured locations (or cache dir if nothing specified)
     *
     * @param movieSet
     *          the movie set
     * @param url
     *          the url to the artwork
     * @param type
     *          the media file type to store the artwork for
     */
    private MovieSetImageFetcherTask(MovieSet movieSet, String url, MediaFileType type) {
      this(movieSet, url, type, movieSet.getMovies());
    }

    /**
     * This constructor is needed to (re)write a kind of artwork to the given list of movies
     * 
     * @param movieSet
     *          the movie set
     * @param url
     *          the url to the artwork
     * @param type
     *          the media file type to store the artwork for
     * @param movies
     *          the list of movies to write the artwork to
     */
    private MovieSetImageFetcherTask(MovieSet movieSet, String url, MediaFileType type, List<Movie> movies) {
      this(movieSet, url, type, getFileNamingsForMediaFileType(type), movies);
    }

    /**
     * This constructor is needed to (re)write a kind of artwork to the given list of movies
     *
     * @param movieSet
     *          the movie set
     * @param url
     *          the url to the artwork
     * @param type
     *          the media file type to store the artwork for
     * @param fileNamings
     *          all file namings to write the artwork for
     * @param movies
     *          the list of movies to write the artwork to
     */
    private MovieSetImageFetcherTask(MovieSet movieSet, String url, MediaFileType type, List<IMovieSetFileNaming> fileNamings, List<Movie> movies) {
      this.movieSet = movieSet;
      this.urlToArtwork = url;
      this.type = type;
      this.fileNamings.addAll(fileNamings);
      this.movies.addAll(movies);
    }

    @Override
    public void run() {
      // first, fetch image
      try {
        Url url = new Url(urlToArtwork);
        try (InputStream is = url.getInputStream()) {
          // do not use UrlUtil.getByteArrayFromUrl because we may need the HTTP headers for artwork type detection
          byte[] bytes = IOUtils.toByteArray(is);

          String extension = Utils.getArtworkExtensionFromContentType(url.getHeader("content-type"));
          if (StringUtils.isBlank(extension)) {
            extension = Utils.getArtworkExtensionFromUrl(urlToArtwork);
          }

          // and then write it to the desired files
          movieSet.removeAllMediaFiles(type);

          if (!fileNamings.isEmpty()) {
            writeImageToArtworkFolder(bytes, extension);
            writeImageToMovieFolders(bytes, extension);
          }
          else {
            writeImageToCacheFolder(bytes);
          }

          // add all written media files to the movie set
          movieSet.addToMediaFiles(writtenArtworkFiles);
          movieSet.saveToDb();
        }
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("fetch image: {} - {}", urlToArtwork, e.getMessage());
      }
    }

    private void writeImageToArtworkFolder(byte[] bytes, String extension) {
      List<Path> paths = createArtworkPathsInArtworkFolder(movieSet, fileNamings, extension);

      for (Path path : paths) {
        // check if folder exists
        if (!Files.exists(path.getParent())) {
          try {
            Files.createDirectories(path.getParent());
          }
          catch (IOException e) {
            LOGGER.warn("could not create directory '{}' - {} ", path.getParent(), e.getMessage());
          }
        }

        // write files
        try {
          writeImage(bytes, path);

          MediaFile artwork = new MediaFile(path, type);
          artwork.gatherMediaInformation();
          writtenArtworkFiles.add(artwork);

          ImageCache.invalidateCachedImage(artwork);
          ImageCache.cacheImageSilently(artwork);
        }
        catch (Exception e) {
          LOGGER.warn("could not write file", e);
        }
      }
    }

    private void writeImageToMovieFolders(byte[] bytes, String extension) {
      // check for empty strings or movies
      if (movies.isEmpty()) {
        return;
      }

      List<IMovieSetFileNaming> movieFileNamings = fileNamings.stream()
          .filter(fileNaming -> fileNaming.getFolderLocation() == IMovieSetFileNaming.Location.MOVIE_FOLDER)
          .collect(Collectors.toList());

      for (IMovieSetFileNaming fileNaming : movieFileNamings) {
        String filename = fileNaming.getFilename("", extension);

        // write image for all movies
        for (Movie movie : movies) {
          try {
            if (!movie.isMultiMovieDir()) {
              Path imageFile = movie.getPathNIO().resolve(filename);
              writeImage(bytes, imageFile);

              MediaFile artwork = new MediaFile(imageFile, type);
              artwork.gatherMediaInformation();
              writtenArtworkFiles.add(artwork);

              ImageCache.invalidateCachedImage(artwork);
              ImageCache.cacheImageSilently(artwork);
            }
          }
          catch (Exception e) {
            LOGGER.warn("could not write files", e);
          }
        }
      }
    }

    private void writeImageToCacheFolder(byte[] bytes) {
      String filename = ImageCache.getMD5WithSubfolder(urlToArtwork);

      try {
        writeImage(bytes, ImageCache.getCacheDir().resolve(filename + ".jpg"));
      }
      catch (Exception e) {
        LOGGER.warn("error in image fetcher", e);
      }
    }

    private void writeImage(byte[] bytes, Path pathAndFilename) throws IOException {
      try (FileOutputStream outputStream = new FileOutputStream(pathAndFilename.toFile()); InputStream is = new ByteArrayInputStream(bytes)) {
        IOUtils.copy(is, outputStream);
        outputStream.flush();
        try {
          outputStream.getFD().sync(); // wait until file has been completely written
        }
        catch (Exception e) {
          // empty here -> just not let the thread crash
        }
      }
    }
  }

  /**
   * get all configured {@link IFileNaming}s for the given {@link MediaFileType}
   *
   * @param type
   *          the file type
   * @return a list of all configured file namings
   */
  private static List<IMovieSetFileNaming> getFileNamingsForMediaFileType(MediaFileType type) {
    List<IMovieSetFileNaming> fileNamings = new ArrayList<>(0);

    switch (type) {
      case FANART:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetFanartFilenames());
        break;

      case POSTER:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetPosterFilenames());
        break;

      case LOGO:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetLogoFilenames());
        break;

      case CLEARLOGO:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetClearlogoFilenames());
        break;

      case BANNER:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetBannerFilenames());
        break;

      case CLEARART:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetClearartFilenames());
        break;

      case THUMB:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetThumbFilenames());
        break;

      case DISC:
        fileNamings.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetDiscartFilenames());
        break;

      default:
        break;
    }

    return fileNamings;
  }
}
