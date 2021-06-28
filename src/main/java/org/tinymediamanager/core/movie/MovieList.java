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

import static org.tinymediamanager.core.Constants.CERTIFICATION;
import static org.tinymediamanager.core.Constants.DECADE;
import static org.tinymediamanager.core.Constants.GENRE;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.TAGS;
import static org.tinymediamanager.core.Constants.YEAR;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.h2.mvstore.MVMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.MediaSource;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ObservableCopyOnWriteArrayList;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tasks.ImageCacheTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.fasterxml.jackson.databind.ObjectReader;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

/**
 * The Class MovieList.
 * 
 * @author Manuel Laggner
 */
public class MovieList extends AbstractModelObject {
  private static final Logger                            LOGGER             = LoggerFactory.getLogger(MovieList.class);
  private static MovieList                               instance;

  private final MovieSettings                            movieSettings;
  private final List<Movie>                              movieList;
  private final List<MovieSet>                           movieSetList;

  private final CopyOnWriteArrayList<Integer>            yearsInMovies;
  private final CopyOnWriteArrayList<String>             tagsInMovies;
  private final CopyOnWriteArrayList<MediaGenres>        genresInMovies;
  private final CopyOnWriteArrayList<String>             videoCodecsInMovies;
  private final CopyOnWriteArrayList<String>             videoContainersInMovies;
  private final CopyOnWriteArrayList<String>             audioCodecsInMovies;
  private final CopyOnWriteArrayList<MediaCertification> certificationsInMovies;
  private final CopyOnWriteArrayList<Double>             frameRatesInMovies;
  private final CopyOnWriteArrayList<Integer>            audioStreamsInMovies;
  private final CopyOnWriteArrayList<Integer>            subtitlesInMovies;
  private final CopyOnWriteArrayList<String>             audioLanguagesInMovies;
  private final CopyOnWriteArrayList<String>             subtitleLanguagesInMovies;
  private final CopyOnWriteArrayList<String>             decadeInMovies;
  private final CopyOnWriteArrayList<String>             hdrFormatInMovies;

  private final PropertyChangeListener                   movieListener;
  private final PropertyChangeListener                   movieSetListener;
  private final Comparator<MovieSet>                     movieSetComparator = new MovieSetComparator();

  /**
   * Instantiates a new movie list.
   */
  private MovieList() {
    // create all lists
    movieList = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(Movie.class));
    movieSetList = new ObservableCopyOnWriteArrayList<>();

    yearsInMovies = new CopyOnWriteArrayList<>();
    tagsInMovies = new CopyOnWriteArrayList<>();
    genresInMovies = new CopyOnWriteArrayList<>();
    videoCodecsInMovies = new CopyOnWriteArrayList<>();
    videoContainersInMovies = new CopyOnWriteArrayList<>();
    audioCodecsInMovies = new CopyOnWriteArrayList<>();
    certificationsInMovies = new CopyOnWriteArrayList<>();
    frameRatesInMovies = new CopyOnWriteArrayList<>();
    audioStreamsInMovies = new CopyOnWriteArrayList<>();
    subtitlesInMovies = new CopyOnWriteArrayList<>();
    audioLanguagesInMovies = new CopyOnWriteArrayList<>();
    subtitleLanguagesInMovies = new CopyOnWriteArrayList<>();
    decadeInMovies = new CopyOnWriteArrayList<>();
    hdrFormatInMovies = new CopyOnWriteArrayList<>();

    // movie listener: its used to always have a full list of all tags, codecs, years, ... used in tmm
    movieListener = evt -> {
      if (evt.getSource() instanceof Movie) {
        Movie movie = (Movie) evt.getSource();

        // do not update all list at the same time - could be a performance issue
        switch (evt.getPropertyName()) {
          case YEAR:
            updateYear(Collections.singletonList(movie));
            break;

          case CERTIFICATION:
            updateCertifications(Collections.singletonList(movie));
            break;

          case GENRE:
            updateGenres(Collections.singletonList(movie));
            break;

          case TAGS:
            updateTags(Collections.singletonList(movie));
            break;

          case MEDIA_FILES:
          case MEDIA_INFORMATION:
            updateMediaInformationLists(Collections.singletonList(movie));
            break;

          default:
            break;
        }
      }
    };

    movieSetListener = evt -> {
      switch (evt.getPropertyName()) {
        case Constants.ADDED_MOVIE:
        case Constants.REMOVED_MOVIE:
          firePropertyChange("movieInMovieSetCount", null, getMovieInMovieSetCount());
          break;

        default:
          break;
      }
    };

    movieSettings = MovieModuleManager.SETTINGS;
  }

  /**
   * Gets the single instance of MovieList.
   * 
   * @return single instance of MovieList
   */
  public static synchronized MovieList getInstance() {
    if (MovieList.instance == null) {
      MovieList.instance = new MovieList();
    }
    return MovieList.instance;
  }

  /**
   * Adds the movie.
   * 
   * @param movie
   *          the movie
   */
  public void addMovie(Movie movie) {
    if (!movieList.contains(movie)) {
      int oldValue = movieList.size();
      movieList.add(movie);

      updateLists(Collections.singletonList(movie));
      movie.addPropertyChangeListener(movieListener);
      firePropertyChange("movies", null, movieList);
      firePropertyChange("movieCount", oldValue, movieList.size());
    }
  }

  /**
   * Removes the datasource.
   * 
   * @param datasource
   *          the path
   */
  void removeDatasource(String datasource) {
    if (StringUtils.isEmpty(datasource)) {
      return;
    }

    List<Movie> moviesToRemove = new ArrayList<>();
    Path path = Paths.get(datasource);
    for (int i = movieList.size() - 1; i >= 0; i--) {
      Movie movie = movieList.get(i);
      if (path.equals(Paths.get(movie.getDataSource()))) {
        moviesToRemove.add(movie);
      }
    }

    removeMovies(moviesToRemove);
  }

  /**
   * exchanges the given datasource in the entities/database with a new one
   */
  void exchangeDatasource(String oldDatasource, String newDatasource) {
    Path oldPath = Paths.get(oldDatasource);
    List<Movie> moviesToChange = movieList.stream().filter(movie -> oldPath.equals(Paths.get(movie.getDataSource()))).collect(Collectors.toList());
    List<MediaFile> imagesToCache = new ArrayList<>();

    for (Movie movie : moviesToChange) {
      Path oldMoviePath = movie.getPathNIO();
      Path newMoviePath;

      try {
        // try to _cleanly_ calculate the relative path
        newMoviePath = Paths.get(newDatasource, Paths.get(movie.getDataSource()).relativize(oldMoviePath).toString());
      }
      catch (Exception e) {
        // if that fails (maybe migrate from windows to linux/macos), just try a simple string replacement
        newMoviePath = Paths.get(newDatasource, FilenameUtils.separatorsToSystem(movie.getPath().replace(movie.getDataSource(), "")));
      }

      movie.setDataSource(newDatasource);
      movie.setPath(newMoviePath.toAbsolutePath().toString());
      movie.updateMediaFilePath(oldMoviePath, newMoviePath);
      movie.saveToDb(); // since we moved already, save it

      // re-build the image cache afterwards in an own thread
      imagesToCache.addAll(movie.getImagesToCache());
    }

    if (!imagesToCache.isEmpty()) {
      ImageCacheTask task = new ImageCacheTask(imagesToCache);
      TmmTaskManager.getInstance().addUnnamedTask(task);
    }
  }

  /**
   * Gets the unscraped movies.
   * 
   * @return the unscraped movies
   */
  public List<Movie> getUnscrapedMovies() {
    List<Movie> unscrapedMovies = new ArrayList<>();
    for (Movie movie : movieList) {
      if (!movie.isScraped()) {
        unscrapedMovies.add(movie);
      }
    }
    return unscrapedMovies;
  }

  /**
   * Gets the new movies or movies with new files
   * 
   * @return the new movies
   */
  public List<Movie> getNewMovies() {
    List<Movie> newMovies = new ArrayList<>();
    for (Movie movie : movieList) {
      if (movie.isNewlyAdded()) {
        newMovies.add(movie);
      }
    }
    return newMovies;
  }

  /**
   * Gets a list of used genres.
   * 
   * @return MediaGenres list
   */
  public Collection<MediaGenres> getUsedGenres() {
    return Collections.unmodifiableList(genresInMovies);
  }

  /**
   * remove given movies from the database
   * 
   * @param movies
   *          list of movies to remove
   */
  public void removeMovies(List<Movie> movies) {
    if (movies == null || movies.isEmpty()) {
      return;
    }
    Set<MovieSet> modifiedMovieSets = new HashSet<>();
    int oldValue = movieList.size();

    // remove in inverse order => performance
    for (int i = movies.size() - 1; i >= 0; i--) {
      Movie movie = movies.get(i);
      movieList.remove(movie);
      if (movie.getMovieSet() != null) {
        MovieSet movieSet = movie.getMovieSet();

        movieSet.removeMovie(movie, false);
        modifiedMovieSets.add(movieSet);
        movie.setMovieSet(null);
      }
      try {
        MovieModuleManager.getInstance().removeMovieFromDb(movie);
      }
      catch (Exception e) {
        LOGGER.error("Error removing movie from DB: {}", e.getMessage());
      }
    }

    firePropertyChange("movies", null, movieList);
    firePropertyChange("movieCount", oldValue, movieList.size());
  }

  /**
   * delete the given movies from the database and physically
   * 
   * @param movies
   *          list of movies to delete
   */
  public void deleteMovies(List<Movie> movies) {
    if (movies == null || movies.isEmpty()) {
      return;
    }
    Set<MovieSet> modifiedMovieSets = new HashSet<>();
    int oldValue = movieList.size();

    // remove in inverse order => performance
    for (int i = movies.size() - 1; i >= 0; i--) {
      Movie movie = movies.get(i);
      movie.deleteFilesSafely();
      movieList.remove(movie);
      if (movie.getMovieSet() != null) {
        MovieSet movieSet = movie.getMovieSet();
        movieSet.removeMovie(movie, false);
        modifiedMovieSets.add(movieSet);
        movie.setMovieSet(null);
      }
      try {
        MovieModuleManager.getInstance().removeMovieFromDb(movie);
      }
      catch (Exception e) {
        LOGGER.error("Error removing movie from DB: {}", e.getMessage());
      }
    }

    firePropertyChange("movies", null, movieList);
    firePropertyChange("movieCount", oldValue, movieList.size());
  }

  /**
   * Gets the movies.
   * 
   * @return the movies
   */
  public List<Movie> getMovies() {
    return movieList;
  }

  /**
   * Load movies from database.
   */
  void loadMoviesFromDatabase(MVMap<UUID, String> movieMap) {
    // load movies
    ObjectReader movieObjectReader = MovieModuleManager.getInstance().getMovieObjectReader();

    for (UUID uuid : new ArrayList<>(movieMap.keyList())) {
      String json = "";
      try {
        json = movieMap.get(uuid);
        Movie movie = movieObjectReader.readValue(json);
        movie.setDbId(uuid);

        // sanity check: only movies with a video file are valid
        if (movie.getMediaFiles(MediaFileType.VIDEO).isEmpty()) {
          // no video file? drop it
          LOGGER.info("movie \"{}\" without video file - dropping", movie.getTitle());
          movieMap.remove(uuid);
        }

        // for performance reasons we add movies directly
        movieList.add(movie);
      }
      catch (Exception e) {
        LOGGER.warn("problem decoding movie json string: {}", e.getMessage());
        LOGGER.info("dropping corrupt movie: {}", json);
        movieMap.remove(uuid);
      }
    }
    LOGGER.info("found {} movies in database", movieList.size());
  }

  void loadMovieSetsFromDatabase(MVMap<UUID, String> movieSetMap) {
    // load movie sets
    ObjectReader movieSetObjectReader = MovieModuleManager.getInstance().getMovieSetObjectReader();

    for (UUID uuid : new ArrayList<>(movieSetMap.keyList())) {
      try {
        MovieSet movieSet = movieSetObjectReader.readValue(movieSetMap.get(uuid));
        movieSet.setDbId(uuid);
        // for performance reasons we add movies sets directly
        movieSetList.add(movieSet);
      }
      catch (Exception e) {
        LOGGER.warn("problem decoding movie set json string: {}", e.getMessage());
        LOGGER.info("dropping corrupt movie set");
        movieSetMap.remove(uuid);
      }
    }

    LOGGER.info("found {} movieSets in database", movieSetList.size());
  }

  void initDataAfterLoading() {
    // remove invalid movies which have no VIDEO files
    checkAndCleanupMediaFiles();

    // initialize movies/movie sets (e.g. link with each others)
    // updateLists is slow here calling for a bunch of movies, so we do the work directly
    for (Movie movie : movieList) {
      movie.initializeAfterLoading();
      movie.addPropertyChangeListener(movieListener);
    }

    updateLists(movieList);

    for (MovieSet movieSet : movieSetList) {
      movieSet.initializeAfterLoading();
    }
  }

  public void persistMovie(Movie movie) {
    // remove this movie from the database
    try {
      MovieModuleManager.getInstance().persistMovie(movie);
    }
    catch (Exception e) {
      LOGGER.error("failed to persist movie: {} - {}", movie.getTitle(), e.getMessage());
    }
  }

  public void removeMovieFromDb(Movie movie) {
    // remove this movie from the database
    try {
      MovieModuleManager.getInstance().removeMovieFromDb(movie);
    }
    catch (Exception e) {
      LOGGER.error("failed to remove movie: {}", movie.getTitle());
    }
  }

  public void persistMovieSet(MovieSet movieSet) {
    // remove this movie set from the database
    try {
      MovieModuleManager.getInstance().persistMovieSet(movieSet);
    }
    catch (Exception e) {
      LOGGER.error("failed to persist movie set: {}", movieSet.getTitle());
    }
  }

  public void removeMovieSetFromDb(MovieSet movieSet) {
    // remove this movie set from the database
    try {
      MovieModuleManager.getInstance().removeMovieSetFromDb(movieSet);
    }
    catch (Exception e) {
      LOGGER.error("failed to remove movie set: {}", movieSet.getTitle());
    }
  }

  public MovieSet lookupMovieSet(UUID uuid) {
    for (MovieSet movieSet : movieSetList) {
      if (movieSet.getDbId().equals(uuid)) {
        return movieSet;
      }
    }
    return null;
  }

  public Movie lookupMovie(UUID uuid) {
    for (Movie movie : movieList) {
      if (movie.getDbId().equals(uuid)) {
        return movie;
      }
    }
    return null;
  }

  /**
   * Gets the movie by path.
   * 
   * @param path
   *          the path
   * @return the movie by path
   */
  public synchronized Movie getMovieByPath(Path path) {

    for (Movie movie : movieList) {
      if (movie.getPathNIO().compareTo(path.toAbsolutePath()) == 0) {
        LOGGER.debug("Ok, found already existing movie '{}' in DB (path: {})", movie.getTitle(), path);
        return movie;
      }
    }

    return null;
  }

  /**
   * Gets a list of movies by same path.
   * 
   * @param path
   *          the path
   * @return the movie list
   */
  public synchronized List<Movie> getMoviesByPath(Path path) {
    ArrayList<Movie> movies = new ArrayList<>();
    for (Movie movie : movieList) {
      if (movie.getPathNIO().compareTo(path) == 0) {
        movies.add(movie);
      }
    }
    return movies;
  }

  /**
   * Search for a movie with the default settings.
   * 
   * @param searchTerm
   *          the search term
   * @param year
   *          the year of the movie (if available, otherwise <= 0)
   * @param ids
   *          a map of all available ids of the movie or null if no id based search is requested
   * @param metadataScraper
   *          the media scraper
   * @throws ScrapeException
   *           any {@link ScrapeException} occurred while searching
   * @return the list
   */
  public List<MediaSearchResult> searchMovie(String searchTerm, int year, Map<String, Object> ids, MediaScraper metadataScraper)
      throws ScrapeException {
    return searchMovie(searchTerm, year, ids, metadataScraper, movieSettings.getScraperLanguage());
  }

  /**
   * Search movie with the chosen language.
   * 
   * @param searchTerm
   *          the search term
   * @param year
   *          the year of the movie (if available, otherwise <= 0)
   * @param ids
   *          a map of all available ids of the movie or null if no id based search is requested
   * @param mediaScraper
   *          the media scraper
   * @param language
   *          the language to search with
   * @throws ScrapeException
   *           any {@link ScrapeException} occurred while searching
   * @return the list
   */
  public List<MediaSearchResult> searchMovie(String searchTerm, int year, Map<String, Object> ids, MediaScraper mediaScraper, MediaLanguages language)
      throws ScrapeException {

    if (mediaScraper == null || !mediaScraper.isEnabled()) {
      return Collections.emptyList();
    }

    Set<MediaSearchResult> sr = new TreeSet<>();
    IMovieMetadataProvider provider = (IMovieMetadataProvider) mediaScraper.getMediaProvider();

    // set what we have, so the provider could chose from all :)
    MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    options.setLanguage(language);
    options.setCertificationCountry(MovieModuleManager.SETTINGS.getCertificationCountry());
    options.setReleaseDateCountry(MovieModuleManager.SETTINGS.getReleaseDateCountry());
    options.setMetadataScraper(mediaScraper);

    if (ids != null) {
      options.setIds(ids);
    }

    if (!searchTerm.isEmpty()) {
      String query = searchTerm.toLowerCase(Locale.ROOT);

      if (MetadataUtil.isValidImdbId(query)) {
        options.setImdbId(query);
      }
      else if (query.startsWith("imdb:")) {
        String imdbId = query.replace("imdb:", "");
        if (MetadataUtil.isValidImdbId(imdbId)) {
          options.setImdbId(imdbId);
        }
      }
      else if (query.startsWith("tmdb:")) {
        try {
          int tmdbId = Integer.parseInt(query.replace("tmdb:", ""));
          if (tmdbId > 0) {
            options.setTmdbId(tmdbId);
          }
        }
        catch (Exception e) {
          // ignored
        }
      }
      options.setSearchQuery(searchTerm);
    }

    if (year > 0) {
      options.setSearchYear(year);
    }

    LOGGER.info("=====================================================");
    LOGGER.info("Searching with scraper: {}", provider.getProviderInfo().getId());
    LOGGER.info("options: {}", options);
    LOGGER.info("=====================================================");
    sr.addAll(provider.search(options));

    // if result is empty, try all scrapers
    if (sr.isEmpty() && movieSettings.isScraperFallback()) {
      for (MediaScraper ms : getAvailableMediaScrapers()) {
        if (provider.getProviderInfo().equals(ms.getMediaProvider().getProviderInfo())
            || ms.getMediaProvider().getProviderInfo().getName().startsWith("Kodi") || !ms.getMediaProvider().isActive()) {
          continue;
        }
        LOGGER.info("no result yet - trying alternate scraper: {}", ms.getName());
        try {
          LOGGER.info("=====================================================");
          LOGGER.info("Searching with alternate scraper: '{}', '{}'", ms.getMediaProvider().getId(), provider.getProviderInfo().getVersion());
          LOGGER.info("options: {}", options);
          LOGGER.info("=====================================================");
          sr.addAll(((IMovieMetadataProvider) ms.getMediaProvider()).search(options));
        }
        catch (ScrapeException e) {
          LOGGER.error("searchMovieFallback", e);
          // just swallow those errors here
        }

        if (!sr.isEmpty()) {
          break;
        }
      }
    }

    return new ArrayList<>(sr);
  }

  public List<MediaScraper> getAvailableMediaScrapers() {
    List<MediaScraper> availableScrapers = MediaScraper.getMediaScrapers(ScraperType.MOVIE);
    availableScrapers.sort(new MovieMediaScraperComparator());
    return availableScrapers;
  }

  public MediaScraper getDefaultMediaScraper() {
    MediaScraper scraper = MediaScraper.getMediaScraperById(movieSettings.getMovieScraper(), ScraperType.MOVIE);
    if (scraper == null || !scraper.isEnabled()) {
      scraper = MediaScraper.getMediaScraperById(Constants.TMDB, ScraperType.MOVIE);
    }
    return scraper;
  }

  public MediaScraper getMediaScraperById(String providerId) {
    return MediaScraper.getMediaScraperById(providerId, ScraperType.MOVIE);
  }

  /**
   * get all available artwork scrapers.
   * 
   * @return the artwork scrapers
   */
  public List<MediaScraper> getAvailableArtworkScrapers() {
    List<MediaScraper> availableScrapers = MediaScraper.getMediaScrapers(ScraperType.MOVIE_ARTWORK);
    // we can use the MovieMediaScraperComparator here too, since TMDB should also be first
    availableScrapers.sort(new MovieMediaScraperComparator());
    return availableScrapers;
  }

  /**
   * get all specified artwork scrapers
   * 
   * @param providerIds
   *          a list of all specified scraper ids
   * @return the specified artwork scrapers
   */
  public List<MediaScraper> getArtworkScrapers(List<String> providerIds) {
    List<MediaScraper> artworkScrapers = new ArrayList<>();

    for (String providerId : providerIds) {
      if (StringUtils.isBlank(providerId)) {
        continue;
      }
      MediaScraper artworkScraper = MediaScraper.getMediaScraperById(providerId, ScraperType.MOVIE_ARTWORK);
      if (artworkScraper != null) {
        artworkScrapers.add(artworkScraper);
      }
    }

    return artworkScrapers;
  }

  /**
   * get all default (specified via settings) artwork scrapers
   * 
   * @return the specified artwork scrapers
   */
  public List<MediaScraper> getDefaultArtworkScrapers() {
    List<MediaScraper> defaultScrapers = getArtworkScrapers(movieSettings.getArtworkScrapers());
    return defaultScrapers.stream().filter(MediaScraper::isActive).collect(Collectors.toList());
  }

  /**
   * all available trailer scrapers.
   * 
   * @return the trailer scrapers
   */
  public List<MediaScraper> getAvailableTrailerScrapers() {
    List<MediaScraper> availableScrapers = MediaScraper.getMediaScrapers(ScraperType.MOVIE_TRAILER);
    // we can use the MovieMediaScraperComparator here too, since TMDB should also be first
    availableScrapers.sort(new MovieMediaScraperComparator());
    return availableScrapers;
  }

  /**
   * get all default (specified via settings) trailer scrapers
   * 
   * @return the specified trailer scrapers
   */
  public List<MediaScraper> getDefaultTrailerScrapers() {
    List<MediaScraper> defaultScrapers = getTrailerScrapers(movieSettings.getTrailerScrapers());
    return defaultScrapers.stream().filter(MediaScraper::isActive).collect(Collectors.toList());
  }

  /**
   * get all specified trailer scrapers.
   * 
   * @param providerIds
   *          the scrapers
   * @return the trailer providers
   */
  public List<MediaScraper> getTrailerScrapers(List<String> providerIds) {
    List<MediaScraper> trailerScrapers = new ArrayList<>();

    for (String providerId : providerIds) {
      if (StringUtils.isBlank(providerId)) {
        continue;
      }
      MediaScraper trailerScraper = MediaScraper.getMediaScraperById(providerId, ScraperType.MOVIE_TRAILER);
      if (trailerScraper != null) {
        trailerScrapers.add(trailerScraper);
      }
    }

    return trailerScrapers;
  }

  /**
   * all available subtitle scrapers.
   *
   * @return the subtitle scrapers
   */
  public List<MediaScraper> getAvailableSubtitleScrapers() {
    List<MediaScraper> availableScrapers = MediaScraper.getMediaScrapers(ScraperType.MOVIE_SUBTITLE);
    availableScrapers.sort(new MovieMediaScraperComparator());
    return availableScrapers;
  }

  /**
   * get all default (specified via settings) subtitle scrapers
   *
   * @return the specified subtitle scrapers
   */
  public List<MediaScraper> getDefaultSubtitleScrapers() {
    List<MediaScraper> defaultScrapers = getSubtitleScrapers(movieSettings.getSubtitleScrapers());
    return defaultScrapers.stream().filter(MediaScraper::isActive).collect(Collectors.toList());
  }

  /**
   * get all specified subtitle scrapers.
   *
   * @param providerIds
   *          the scrapers
   * @return the subtitle scrapers
   */
  public List<MediaScraper> getSubtitleScrapers(List<String> providerIds) {
    List<MediaScraper> subtitleScrapers = new ArrayList<>();

    for (String providerId : providerIds) {
      if (StringUtils.isBlank(providerId)) {
        continue;
      }
      MediaScraper subtitleScraper = MediaScraper.getMediaScraperById(providerId, ScraperType.MOVIE_SUBTITLE);
      if (subtitleScraper != null) {
        subtitleScrapers.add(subtitleScraper);
      }
    }

    return subtitleScrapers;
  }

  /**
   * Gets the movie count.
   * 
   * @return the movie count
   */
  public int getMovieCount() {
    return movieList.size();
  }

  /**
   * Gets the movie set count.
   * 
   * @return the movie set count
   */
  public int getMovieSetCount() {
    return movieSetList.size();
  }

  /**
   * Gets the movie in movie set count.
   *
   * @return the movie in movie set count
   */
  public int getMovieInMovieSetCount() {
    int count = 0;
    for (MovieSet movieSet : movieSetList) {
      count += movieSet.getMovies().size();
    }
    return count;
  }

  private void updateLists(Collection<Movie> movies) {
    updateYear(movies);
    updateDecades(movies);
    updateTags(movies);
    updateGenres(movies);
    updateCertifications(movies);
    updateMediaInformationLists(movies);
  }

  /**
   * Update year in movies
   *
   * @param movies
   *          all movies to update
   */
  private void updateYear(Collection<Movie> movies) {
    Set<Integer> years = new HashSet<>();
    movies.forEach(movie -> years.add(movie.getYear()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(yearsInMovies, years)) {
      firePropertyChange(YEAR, null, yearsInMovies);
    }
  }

  /**
   * Update decades in movies
   *
   * @param movies
   *          all movies to update
   */
  private void updateDecades(Collection<Movie> movies) {
    Set<String> decades = new HashSet<>();
    movies.forEach(movie -> decades.add(movie.getDecadeShort()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(decadeInMovies, decades)) {
      firePropertyChange(DECADE, null, decadeInMovies);
    }
  }

  /**
   * Update genres used in movies.
   *
   * @param movies
   *          all movies to update
   */
  private void updateGenres(Collection<Movie> movies) {
    Set<MediaGenres> genres = new HashSet<>();
    movies.forEach(movie -> genres.addAll(movie.getGenres()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(genresInMovies, genres)) {
      firePropertyChange(GENRE, null, genresInMovies);
    }
  }

  /**
   * Update tags used in movies.
   *
   * @param movies
   *          all movies to update
   */
  private void updateTags(Collection<Movie> movies) {
    Set<String> tags = new HashSet<>();
    movies.forEach(movie -> tags.addAll(movie.getTags()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(tagsInMovies, tags)) {
      firePropertyChange(TAGS, null, tagsInMovies);
    }
  }

  /**
   * Update media information used in movies.
   *
   * @param movies
   *          all movies to update
   */
  private void updateMediaInformationLists(Collection<Movie> movies) {
    Set<String> videoCodecs = new HashSet<>();
    Set<Double> frameRates = new HashSet<>();
    Map<String, String> videoContainers = new HashMap<>();
    Set<String> audioCodecs = new HashSet<>();
    Set<Integer> audioStreamCount = new HashSet<>();
    Set<Integer> subtitleCount = new HashSet<>();
    Set<String> audioLanguages = new HashSet<>();
    Set<String> subtitleLanguages = new HashSet<>();
    Set<String> hdrFormat = new HashSet<>();

    // get Subtitle language from video files and subtitle files
    for (Movie movie : movies) {
      for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO, MediaFileType.SUBTITLE)) {
        // subtitle language
        if (!mf.getSubtitleLanguagesList().isEmpty()) {
          for (String lang : mf.getSubtitleLanguagesList()) {
            subtitleLanguages.add(lang);
          }
        }
      }
    }

    for (Movie movie : movies) {
      for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO)) {
        // video codec
        if (StringUtils.isNotBlank(mf.getVideoCodec())) {
          videoCodecs.add(mf.getVideoCodec());
        }

        // frame rate
        if (mf.getFrameRate() > 0) {
          frameRates.add(mf.getFrameRate());
        }

        // video container
        if (StringUtils.isNotBlank(mf.getContainerFormat())) {
          videoContainers.putIfAbsent(mf.getContainerFormat().toLowerCase(Locale.ROOT), mf.getContainerFormat());
        }

        // audio codec
        for (MediaFileAudioStream audio : mf.getAudioStreams()) {
          if (StringUtils.isNotBlank(audio.getCodec())) {
            audioCodecs.add(audio.getCodec());
          }
        }
        // audio streams
        if (!mf.getAudioStreams().isEmpty()) {
          audioStreamCount.add(mf.getAudioStreams().size());
        }

        // subtitles
        if (!mf.getSubtitles().isEmpty()) {
          subtitleCount.add(mf.getSubtitles().size());
        }

        // audio language
        if (!mf.getAudioLanguagesList().isEmpty()) {
          for (String lang : mf.getAudioLanguagesList()) {
            audioLanguages.add(lang);
          }
        }

        // HDR Format
        if (!mf.getHdrFormat().isEmpty()) {
          hdrFormat.add(mf.getHdrFormat());
        }
      }
    }

    // video codecs
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(videoCodecsInMovies, videoCodecs)) {
      firePropertyChange(Constants.VIDEO_CODEC, null, videoCodecsInMovies);
    }

    // frame rate
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(frameRatesInMovies, frameRates)) {
      firePropertyChange(Constants.FRAME_RATE, null, frameRatesInMovies);
    }

    // video container
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(videoContainersInMovies, videoContainers.values())) {
      firePropertyChange(Constants.VIDEO_CONTAINER, null, videoContainersInMovies);
    }

    // audio codec
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioCodecsInMovies, audioCodecs)) {
      firePropertyChange(Constants.AUDIO_CODEC, null, audioCodecsInMovies);
    }

    // audio streams
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioStreamsInMovies, audioStreamCount)) {
      firePropertyChange(Constants.AUDIOSTREAMS_COUNT, null, audioStreamsInMovies);
    }

    // subtitles
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(subtitlesInMovies, subtitleCount)) {
      firePropertyChange(Constants.SUBTITLES_COUNT, null, subtitlesInMovies);
    }

    // audio languages
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(audioLanguagesInMovies, audioLanguages)) {
      firePropertyChange(Constants.AUDIO_LANGUAGES, null, audioLanguagesInMovies);
    }

    // subtitle languages
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(subtitleLanguagesInMovies, subtitleLanguages)) {
      firePropertyChange(Constants.SUBTITLE_LANGUAGES, null, subtitleLanguagesInMovies);
    }

    // HDR Format
    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(hdrFormatInMovies, hdrFormat)) {
      firePropertyChange(Constants.HDR_FORMAT, null, hdrFormatInMovies);
    }
  }

  /**
   * Update certifications used in movies.
   *
   * @param movies
   *          all movies to update
   */
  private void updateCertifications(Collection<Movie> movies) {
    Set<MediaCertification> certifications = new HashSet<>();
    movies.forEach(movie -> certifications.add(movie.getCertification()));

    if (ListUtils.addToCopyOnWriteArrayListIfAbsent(certificationsInMovies, certifications)) {
      firePropertyChange(Constants.CERTIFICATION, null, certificationsInMovies);
    }
  }

  /**
   * get a {@link Set} of all years in movies
   * 
   * @return a {@link Set} of all years
   */
  public Collection<Integer> getYearsInMovies() {
    return yearsInMovies;
  }

  /**
   * get a {@link Set} of all decades in movies
   *
   * @return a {@link Set} of all decades
   */
  public Collection<String> getDecadeInMovies() {
    return decadeInMovies;
  }

  /**
   * get a {@link Set} of all tags in movies.
   *
   * @return a {@link Set} of all tags
   */
  public Collection<String> getTagsInMovies() {
    return tagsInMovies;
  }

  public Collection<String> getVideoCodecsInMovies() {
    return videoCodecsInMovies;
  }

  public Collection<String> getVideoContainersInMovies() {
    return videoContainersInMovies;
  }

  public Collection<String> getAudioCodecsInMovies() {
    return audioCodecsInMovies;
  }

  public Collection<MediaCertification> getCertificationsInMovies() {
    return certificationsInMovies;
  }

  public Collection<Double> getFrameRatesInMovies() {
    return frameRatesInMovies;
  }

  public Collection<Integer> getAudioStreamsInMovies() {
    return audioStreamsInMovies;
  }

  public Collection<Integer> getSubtitlesInMovies() {
    return subtitlesInMovies;
  }

  public Collection<String> getAudioLanguagesInMovies() {
    return audioLanguagesInMovies;
  }

  public Collection<String> getSubtitleLanguagesInMovies() {
    return subtitleLanguagesInMovies;
  }

  public Collection<String> getHDRFormatInMovies() {
    return hdrFormatInMovies;
  }

  /**
   * Search duplicates.
   */
  public void searchDuplicates() {
    Map<String, Movie> imdbDuplicates = new HashMap<>();
    Map<Integer, Movie> tmdbDuplicates = new HashMap<>();

    for (Movie movie : movieList) {
      movie.clearDuplicate();

      // imdb duplicate search only works with given imdbid
      if (StringUtils.isNotEmpty(movie.getImdbId())) {
        // is there a movie with this imdbid sotred?
        String imdbId = movie.getImdbId();
        if (imdbDuplicates.containsKey(imdbId)) {
          // yes - set duplicate flag on both movies
          movie.setDuplicate();
          Movie movie2 = imdbDuplicates.get(imdbId);
          movie2.setDuplicate();
        }
        else {
          // no, store movie
          imdbDuplicates.put(imdbId, movie);
        }
      }

      // tmdb duplicate search only works with with given tmdb id
      int tmdbId = movie.getTmdbId();
      if (tmdbId > 0) {
        // is there a movie with this tmdbid sotred?
        if (tmdbDuplicates.containsKey(tmdbId)) {
          // yes - set duplicate flag on both movies
          movie.setDuplicate();
          Movie movie2 = tmdbDuplicates.get(tmdbId);
          movie2.setDuplicate();
        }
        else {
          // no, store movie
          tmdbDuplicates.put(tmdbId, movie);
        }
      }
    }
  }

  /**
   * Gets the movie set list.
   * 
   * @return the movieSetList
   */
  public List<MovieSet> getMovieSetList() {
    return movieSetList;
  }

  /**
   * get the movie set list in a sorted order
   * 
   * @return the movie set list (sorted)
   */
  public List<MovieSet> getSortedMovieSetList() {
    List<MovieSet> sortedMovieSets = new ArrayList<>(getMovieSetList());
    sortedMovieSets.sort(movieSetComparator);
    return sortedMovieSets;
  }

  /**
   * Adds the movie set.
   * 
   * @param movieSet
   *          the movie set
   */
  public void addMovieSet(MovieSet movieSet) {
    int oldValue = movieSetList.size();
    this.movieSetList.add(movieSet);
    movieSet.addPropertyChangeListener(movieSetListener);
    firePropertyChange(Constants.ADDED_MOVIE_SET, null, movieSet);
    firePropertyChange("movieSetCount", oldValue, movieSetList.size());
    firePropertyChange("movieInMovieSetCount", oldValue, getMovieInMovieSetCount());
  }

  /**
   * Removes the movie set.
   * 
   * @param movieSet
   *          the movie set
   */
  public void removeMovieSet(MovieSet movieSet) {
    int oldValue = movieSetList.size();
    movieSet.removeAllMovies();
    movieSet.removePropertyChangeListener(movieSetListener);

    try {
      // remove artwork
      MovieSetArtworkHelper.removeMovieSetArtwork(movieSet);
      movieSetList.remove(movieSet);
      MovieModuleManager.getInstance().removeMovieSetFromDb(movieSet);
    }
    catch (Exception e) {
      LOGGER.error("Error removing movie set from DB: {}", e.getMessage());
    }

    firePropertyChange(Constants.REMOVED_MOVIE_SET, null, movieSet);
    firePropertyChange("movieSetCount", oldValue, movieSetList.size());
    firePropertyChange("movieInMovieSetCount", oldValue, getMovieInMovieSetCount());
  }

  private MovieSet findMovieSet(String title, int tmdbId) {
    // first search by tmdbId
    if (tmdbId > 0) {
      for (MovieSet movieSet : movieSetList) {
        if (movieSet.getTmdbId() == tmdbId) {
          return movieSet;
        }
      }
    }

    // search for the movieset by name
    for (MovieSet movieSet : movieSetList) {
      if (movieSet.getTitle().equals(title)) {
        return movieSet;
      }
    }

    return null;
  }

  public synchronized MovieSet getMovieSet(String title, int tmdbId) {
    MovieSet movieSet = findMovieSet(title, tmdbId);

    if (movieSet == null && StringUtils.isNotBlank(title)) {
      movieSet = new MovieSet(title);
      if (tmdbId > 0) {
        movieSet.setTmdbId(tmdbId);
      }
      movieSet.saveToDb();
      addMovieSet(movieSet);
    }

    return movieSet;
  }

  /**
   * check if there are movies without (at least) one VIDEO mf
   */
  private void checkAndCleanupMediaFiles() {
    List<Movie> moviesToRemove = new ArrayList<>();
    for (Movie movie : movieList) {
      List<MediaFile> mfs = movie.getMediaFiles(MediaFileType.VIDEO);
      if (mfs.isEmpty()) {
        // mark movie for removal
        moviesToRemove.add(movie);
      }
    }

    if (!moviesToRemove.isEmpty()) {
      removeMovies(moviesToRemove);
      LOGGER.warn("movies without VIDEOs detected");

      // and push a message
      // also delay it so that the UI has time to start up
      Thread thread = new Thread(() -> {
        try {
          Thread.sleep(15000);
        }
        catch (Exception ignored) {
        }
        Message message = new Message(MessageLevel.SEVERE, "tmm.movies", "message.database.corrupteddata");
        MessageManager.instance.pushMessage(message);
      });
      thread.start();
    }
  }

  /**
   * invalidate the title sortable upon changes to the sortable prefixes
   */
  public void invalidateTitleSortable() {
    for (Movie movie : new ArrayList<>(movieList)) {
      movie.clearTitleSortable();
    }
  }

  /**
   * create a new offline movie with the given title in the specified data source
   * 
   * @param title
   *          the given title
   * @param datasource
   *          the data source to create the offline movie in
   */
  public void addOfflineMovie(String title, String datasource) {
    addOfflineMovie(title, datasource, MediaSource.UNKNOWN);
  }

  /**
   * create a new offline movie with the given title in the specified data source
   * 
   * @param title
   *          the given title
   * @param datasource
   *          the data source to create the offline movie in
   * @param mediaSource
   *          the media source to be set for the offline movie
   */
  public void addOfflineMovie(String title, String datasource, MediaSource mediaSource) {
    // first crosscheck if the data source is in our settings
    if (!movieSettings.getMovieDataSource().contains(datasource)) {
      return;
    }

    // check if there is already an identical stub folder
    int i = 1;
    Path stubFolder = Paths.get(datasource, title);
    while (Files.exists(stubFolder)) {
      stubFolder = Paths.get(datasource, title + "(" + i++ + ")");
    }

    Path stubFile = stubFolder.resolve(title + ".disc");

    // create the stub file
    try {
      Files.createDirectory(stubFolder);
      Files.createFile(stubFile);
    }
    catch (IOException e) {
      LOGGER.error("could not create stub file - {}", e.getMessage());
      return;
    }

    // create a movie and set it as MF
    MediaFile mf = new MediaFile(stubFile);
    mf.gatherMediaInformation();
    Movie movie = new Movie();

    movie.setTitle(title);
    movie.setPath(stubFolder.toAbsolutePath().toString());
    movie.setDataSource(datasource);
    movie.setMediaSource(mediaSource);
    movie.setDateAdded(new Date());
    movie.addToMediaFiles(mf);
    movie.setOffline(true);
    movie.setNewlyAdded(true);

    try {
      addMovie(movie);
      movie.saveToDb();
    }
    catch (Exception e) {
      try {
        Utils.deleteDirectoryRecursive(stubFolder);
      }
      catch (Exception e1) {
        LOGGER.debug("could not delete stub folder - {}", e1.getMessage());
      }
      throw e;
    }
  }

  /**
   * get all titles from TV shows (mainly for the showlink feature)
   *
   * @return a {@link List} of all TV show titles
   */
  public List<String> getTvShowTitles() {
    List<String> tvShowTitles = new ArrayList<>();
    TvShowList.getInstance().getTvShows().forEach(tvShow -> tvShowTitles.add(tvShow.getTitle()));
    tvShowTitles.sort(Comparator.naturalOrder());
    return tvShowTitles;
  }

  private static class MovieSetComparator implements Comparator<MovieSet> {
    @Override
    public int compare(MovieSet o1, MovieSet o2) {
      if (o1 == null || o2 == null || o1.getTitleSortable() == null || o2.getTitleSortable() == null) {
        return 0;
      }
      return o1.getTitleSortable().compareToIgnoreCase(o2.getTitleSortable());
    }
  }

  private static class MovieMediaScraperComparator implements Comparator<MediaScraper> {
    @Override
    public int compare(MediaScraper o1, MediaScraper o2) {
      return o1.getId().compareTo(o2.getId());
    }
  }
}
