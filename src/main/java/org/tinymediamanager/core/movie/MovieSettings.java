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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.observablecollections.ObservableCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.LanguageStyle;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.TrailerSources;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.connector.MovieSetConnectors;
import org.tinymediamanager.core.movie.filenaming.MovieBannerNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieClearlogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieDiscartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieExtraFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieKeyartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieLogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.core.movie.filenaming.MoviePosterNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetBannerNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetClearartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetClearlogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetDiscartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetFanartNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetLogoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetNfoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetPosterNaming;
import org.tinymediamanager.core.movie.filenaming.MovieSetThumbNaming;
import org.tinymediamanager.core.movie.filenaming.MovieThumbNaming;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaArtwork.FanartSizes;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaArtwork.PosterSizes;
import org.tinymediamanager.scraper.entities.MediaLanguages;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * The Class MovieSettings.
 */
public final class MovieSettings extends AbstractSettings {
  private static final Logger            LOGGER                                 = LoggerFactory.getLogger(MovieSettings.class);
  private static final String            CONFIG_FILE                            = "movies.json";

  public static final String             DEFAULT_RENAMER_FOLDER_PATTERN         = "${title} ${- ,edition,} (${year})";
  public static final String             DEFAULT_RENAMER_FILE_PATTERN           = "${title} ${- ,edition,} (${year}) ${videoFormat} ${audioCodec}";

  private static MovieSettings           instance;

  /**
   * Constants mainly for events
   */
  public static final String             MOVIE_UI_FILTER_PRESETS                = "movieUiFilterPresets";
  public static final String             MOVIE_SET_UI_FILTER_PRESETS            = "movieSetUiFilterPresets";
  static final String                    MOVIE_DATA_SOURCE                      = "movieDataSource";
  static final String                    NFO_FILENAME                           = "nfoFilename";
  static final String                    POSTER_FILENAME                        = "posterFilename";
  static final String                    FANART_FILENAME                        = "fanartFilename";
  static final String                    EXTRAFANART_FILENAME                   = "extraFanartFilename";
  static final String                    BANNER_FILENAME                        = "bannerFilename";
  static final String                    CLEARART_FILENAME                      = "clearartFilename";
  static final String                    THUMB_FILENAME                         = "thumbFilename";
  static final String                    LOGO_FILENAME                          = "logoFilename";
  static final String                    CLEARLOGO_FILENAME                     = "clearlogoFilename";
  static final String                    DISCART_FILENAME                       = "discartFilename";
  static final String                    KEYART_FILENAME                        = "keyartFilename";
  static final String                    MOVIE_SET_POSTER_FILENAME              = "movieSetPosterFilename";
  static final String                    MOVIE_SET_FANART_FILENAME              = "movieSetFanartFilename";
  static final String                    MOVIE_SET_BANNER_FILENAME              = "movieSetBannerFilename";
  static final String                    MOVIE_SET_CLEARART_FILENAME            = "movieSetClearartFilename";
  static final String                    MOVIE_SET_THUMB_FILENAME               = "movieSetThumbFilename";
  static final String                    MOVIE_SET_LOGO_FILENAME                = "movieSetLogoFilename";
  static final String                    MOVIE_SET_CLEARLOGO_FILENAME           = "movieSetClearlogoFilename";
  static final String                    MOVIE_SET_DISCART_FILENAME             = "movieSetDiscartFilename";
  static final String                    TRAILER_FILENAME                       = "trailerFilename";
  static final String                    ARTWORK_SCRAPERS                       = "artworkScrapers";
  static final String                    TRAILER_SCRAPERS                       = "trailerScrapers";
  static final String                    SUBTITLE_SCRAPERS                      = "subtitleScrapers";
  static final String                    BAD_WORD                               = "badWord";
  static final String                    SKIP_FOLDER                            = "skipFolder";
  static final String                    CHECK_IMAGES_MOVIE                     = "checkImagesMovie";
  static final String                    CHECK_IMAGES_MOVIESET                  = "checkImagesMovieSet";

  final List<String>                     movieDataSources                       = ObservableCollections.observableList(new ArrayList<>());
  final List<MovieNfoNaming>             nfoFilenames                           = new ArrayList<>();

  // movie artwork
  final List<MoviePosterNaming>          posterFilenames                        = new ArrayList<>();
  final List<MovieFanartNaming>          fanartFilenames                        = new ArrayList<>();
  final List<MovieExtraFanartNaming>     extraFanartFilenames                   = new ArrayList<>();
  final List<MovieBannerNaming>          bannerFilenames                        = new ArrayList<>();
  final List<MovieClearartNaming>        clearartFilenames                      = new ArrayList<>();
  final List<MovieThumbNaming>           thumbFilenames                         = new ArrayList<>();
  final List<MovieClearlogoNaming>       clearlogoFilenames                     = new ArrayList<>();
  final List<MovieLogoNaming>            logoFilenames                          = new ArrayList<>();
  final List<MovieDiscartNaming>         discartFilenames                       = new ArrayList<>();
  final List<MovieKeyartNaming>          keyartFilenames                        = new ArrayList<>();

  final List<MovieTrailerNaming>         trailerFilenames                       = new ArrayList<>();
  final List<String>                     badWords                               = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                     artworkScrapers                        = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                     trailerScrapers                        = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                     subtitleScrapers                       = ObservableCollections.observableList(new ArrayList<>());
  final List<String>                     skipFolders                            = ObservableCollections.observableList(new ArrayList<>());

  // data sources / NFO settings
  boolean                                buildImageCacheOnImport                = false;
  MovieConnectors                        movieConnector                         = MovieConnectors.KODI;
  CertificationStyle                     certificationStyle                     = CertificationStyle.LARGE;
  boolean                                writeCleanNfo                          = false;
  DateField                              nfoDateAddedField                      = DateField.DATE_ADDED;
  MediaLanguages                         nfoLanguage                            = MediaLanguages.en;
  boolean                                createOutline                          = true;
  boolean                                outlineFirstSentence                   = false;
  boolean                                nfoWriteSingleStudio                   = false;

  // renamer
  boolean                                renameAfterScrape                      = false;
  boolean                                ardAfterScrape                         = false;
  boolean                                updateOnStart                          = false;
  String                                 renamerPathname                        = DEFAULT_RENAMER_FOLDER_PATTERN;
  String                                 renamerFilename                        = DEFAULT_RENAMER_FILE_PATTERN;
  boolean                                renamerPathnameSpaceSubstitution       = false;
  String                                 renamerPathnameSpaceReplacement        = "_";
  boolean                                renamerFilenameSpaceSubstitution       = false;
  String                                 renamerFilenameSpaceReplacement        = "_";
  String                                 renamerColonReplacement                = "-";
  boolean                                renamerNfoCleanup                      = false;
  boolean                                renamerCreateMoviesetForSingleMovie    = false;
  String                                 renamerFirstCharacterNumberReplacement = "#";
  boolean                                asciiReplacement                       = false;

  // meta data scraper
  String                                 movieScraper                           = Constants.TMDB;
  MediaLanguages                         scraperLanguage                        = MediaLanguages.en;
  CountryCode                            certificationCountry                   = CountryCode.US;
  String                                 releaseDateCountry                     = "";
  double                                 scraperThreshold                       = 0.75;
  boolean                                scraperFallback                        = false;
  final List<MovieScraperMetadataConfig> scraperMetadataConfig                  = new ArrayList<>();
  boolean                                capitalWordsInTitles                   = false;

  // artwork scraper
  PosterSizes                            imagePosterSize                        = PosterSizes.LARGE;
  FanartSizes                            imageFanartSize                        = FanartSizes.LARGE;
  boolean                                imageExtraThumbs                       = false;
  boolean                                imageExtraThumbsResize                 = true;
  int                                    imageExtraThumbsSize                   = 300;
  int                                    imageExtraThumbsCount                  = 5;
  boolean                                imageExtraFanart                       = false;
  int                                    imageExtraFanartCount                  = 5;
  boolean                                scrapeBestImage                        = true;
  MediaLanguages                         imageScraperLanguage                   = MediaLanguages.en;
  boolean                                imageLanguagePriority                  = true;
  boolean                                writeActorImages                       = false;

  // trailer scraper
  boolean                                useTrailerPreference                   = true;
  boolean                                automaticTrailerDownload               = false;
  TrailerQuality                         trailerQuality                         = TrailerQuality.HD_720;
  TrailerSources                         trailerSource                          = TrailerSources.YOUTUBE;

  // subtitle scraper
  MediaLanguages                         subtitleScraperLanguage                = MediaLanguages.en;
  LanguageStyle                          subtitleLanguageStyle                  = LanguageStyle.ISO3T;
  boolean                                subtitleWithoutLanguageTag             = false;
  boolean                                subtitleForceBestMatch                 = false;

  // misc
  boolean                                runtimeFromMediaInfo                   = false;
  boolean                                includeExternalAudioStreams            = false;
  boolean                                syncTrakt                              = false;
  boolean                                syncTraktCollection                    = true;
  boolean                                syncTraktWatched                       = true;
  boolean                                syncTraktRating                        = true;
  boolean                                extractArtworkFromVsmeta               = false;
  boolean                                useMediainfoMetadata                   = false;

  boolean                                title                                  = true;
  boolean                                sortableTitle                          = false;
  boolean                                originalTitle                          = false;
  boolean                                sortableOriginalTitle                  = false;
  boolean                                sortTitle                              = false;
  boolean                                note                                   = false;

  // ui
  boolean                                showMovieTableTooltips                 = true;
  final List<String>                     ratingSources                          = ObservableCollections.observableList(new ArrayList<>());
  final List<MediaArtworkType>           checkImagesMovie                       = new ArrayList<>();
  boolean                                storeUiFilters                         = false;
  final List<UIFilters>                  uiFilters                              = new ArrayList<>();

  // movie sets
  MovieSetConnectors                     movieSetConnector                      = MovieSetConnectors.EMBY;
  final List<MovieSetNfoNaming>          movieSetNfoFilenames                   = new ArrayList<>();
  @JsonAlias("movieSetArtworkFolder")
  String                                 movieSetDataFolder                     = "";
  boolean                                scrapeBestImageMovieSet                = true;

  // movie set artwork
  final List<MovieSetPosterNaming>       movieSetPosterFilenames                = new ArrayList<>();
  final List<MovieSetFanartNaming>       movieSetFanartFilenames                = new ArrayList<>();
  boolean                                showMovieSetTableTooltips              = true;
  final List<MovieSetBannerNaming>       movieSetBannerFilenames                = new ArrayList<>();
  boolean                                displayMovieSetMissingMovies           = false;
  final List<MovieSetClearartNaming>     movieSetClearartFilenames              = new ArrayList<>();
  final List<MediaArtworkType>           checkImagesMovieSet                    = new ArrayList<>();
  final List<MovieSetThumbNaming>        movieSetThumbFilenames                 = new ArrayList<>();
  boolean                                storeMovieSetUiFilters                 = false;
  final List<MovieSetClearlogoNaming>    movieSetClearlogoFilenames             = new ArrayList<>();
  final List<UIFilters>                  movieSetUiFilters                      = new ArrayList<>();
  final List<MovieSetLogoNaming>         movieSetLogoFilenames                  = new ArrayList<>();
  final Map<String, List<UIFilters>>     movieUiFilterPresets                   = new HashMap<>();
  final List<MovieSetDiscartNaming>      movieSetDiscartFilenames               = new ArrayList<>();
  final Map<String, List<UIFilters>>     movieSetUiFilterPresets                = new HashMap<>();

  public MovieSettings() {
    super();

    // add default entries to the lists - they will be overwritten by jackson later
    addDefaultEntries();

    addPropertyChangeListener(evt -> setDirty());
  }

  private void addDefaultEntries() {
    // file names
    nfoFilenames.clear();
    addNfoFilename(MovieNfoNaming.FILENAME_NFO);

    posterFilenames.clear();
    addPosterFilename(MoviePosterNaming.FILENAME_POSTER);

    fanartFilenames.clear();
    addFanartFilename(MovieFanartNaming.FILENAME_FANART);

    extraFanartFilenames.clear();
    addExtraFanartFilename(MovieExtraFanartNaming.FILENAME_EXTRAFANART);

    bannerFilenames.clear();
    addBannerFilename(MovieBannerNaming.FILENAME_BANNER);

    clearartFilenames.clear();
    addClearartFilename(MovieClearartNaming.FILENAME_CLEARART);

    thumbFilenames.clear();
    addThumbFilename(MovieThumbNaming.FILENAME_LANDSCAPE);

    logoFilenames.clear();
    addLogoFilename(MovieLogoNaming.FILENAME_LOGO);

    clearlogoFilenames.clear();
    addClearlogoFilename(MovieClearlogoNaming.FILENAME_CLEARLOGO);

    discartFilenames.clear();
    addDiscartFilename(MovieDiscartNaming.FILENAME_DISCART);

    keyartFilenames.clear();
    addKeyartFilename(MovieKeyartNaming.FILENAME_KEYART);

    movieSetNfoFilenames.clear();
    addMovieSetNfoFilename(MovieSetNfoNaming.KODI_NFO);

    movieSetPosterFilenames.clear();
    addMovieSetPosterFilename(MovieSetPosterNaming.KODI_POSTER);

    movieSetFanartFilenames.clear();
    addMovieSetFanartFilename(MovieSetFanartNaming.KODI_FANART);

    movieSetBannerFilenames.clear();
    addMovieSetBannerFilename(MovieSetBannerNaming.KODI_BANNER);

    movieSetClearartFilenames.clear();
    addMovieSetClearartFilename(MovieSetClearartNaming.KODI_CLEARART);

    movieSetThumbFilenames.clear();
    addMovieSetThumbFilename(MovieSetThumbNaming.KODI_LANDSCAPE);

    movieSetLogoFilenames.clear();
    addMovieSetLogoFilename(MovieSetLogoNaming.KODI_LOGO);

    movieSetClearlogoFilenames.clear();
    addMovieSetClearlogoFilename(MovieSetClearlogoNaming.KODI_CLEARLOGO);

    movieSetDiscartFilenames.clear();
    addMovieSetDiscartFilename(MovieSetDiscartNaming.KODI_DISCART);

    trailerFilenames.clear();
    addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);

    // other settings
    setMovieConnector(MovieConnectors.KODI);
    setRenamerPathname(DEFAULT_RENAMER_FOLDER_PATTERN);
    setRenamerFilename(DEFAULT_RENAMER_FILE_PATTERN);
    setCertificationStyle(CertificationStyle.LARGE);

    // UI settings
    checkImagesMovie.clear();
    addCheckImagesMovie(MediaArtworkType.POSTER);
    addCheckImagesMovie(MediaArtworkType.BACKGROUND);

    ratingSources.clear();
    addRatingSource(MediaMetadata.IMDB);

    checkImagesMovieSet.clear();
    addCheckImagesMovieSet(MediaArtworkType.POSTER);
    addCheckImagesMovieSet(MediaArtworkType.BACKGROUND);

    scraperMetadataConfig.addAll(Arrays.asList(MovieScraperMetadataConfig.values()));
  }

  @Override
  protected ObjectWriter createObjectWriter() {
    return objectMapper.writerFor(MovieSettings.class);
  }

  /**
   * Gets the single instance of MovieSettings.
   *
   * @return single instance of MovieSettings
   */
  static synchronized MovieSettings getInstance() {
    return getInstance(Settings.getInstance().getSettingsFolder());
  }

  /**
   * Override our settings folder (defaults to "data")<br>
   * <b>Should only be used for unit testing et all!</b><br>
   *
   * @return single instance of MovieSettings
   */
  static synchronized MovieSettings getInstance(String folder) {
    if (instance == null) {
      instance = (MovieSettings) getInstance(folder, CONFIG_FILE, MovieSettings.class);
    }
    return instance;
  }

  @Override
  public String getConfigFilename() {
    return CONFIG_FILE;
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * the tmm defaults
   */
  @Override
  protected void writeDefaultSettings() {
    addDefaultEntries();

    // set default languages based on java instance
    String defaultLang = Locale.getDefault().getLanguage();
    CountryCode cc = CountryCode.getByCode(defaultLang.toUpperCase(Locale.ROOT));
    if (cc != null) {
      setCertificationCountry(cc);
      setReleaseDateCountry(Locale.getDefault().getCountry());
    }
    for (MediaLanguages ml : MediaLanguages.values()) {
      if (ml.name().equals(defaultLang)) {
        setScraperLanguage(ml);
      }
    }
    saveSettings();
  }

  public void addMovieDataSources(String path) {
    if (!movieDataSources.contains(path)) {
      movieDataSources.add(path);
      firePropertyChange(MOVIE_DATA_SOURCE, null, movieDataSources);
      firePropertyChange(Constants.DATA_SOURCE, null, movieDataSources);
    }
  }

  public void removeMovieDataSources(String path) {
    MovieList movieList = MovieModuleManager.getInstance().getMovieList();
    movieList.removeDatasource(path);
    movieDataSources.remove(path);
    firePropertyChange(MOVIE_DATA_SOURCE, null, movieDataSources);
    firePropertyChange(Constants.DATA_SOURCE, null, movieDataSources);
  }

  public void exchangeMovieDatasource(String oldDatasource, String newDatasource) {
    int index = movieDataSources.indexOf(oldDatasource);
    if (index > -1) {
      movieDataSources.remove(oldDatasource);
      movieDataSources.add(index, newDatasource);
      MovieModuleManager.getInstance().getMovieList().exchangeDatasource(oldDatasource, newDatasource);
    }
    firePropertyChange(MOVIE_DATA_SOURCE, null, movieDataSources);
    firePropertyChange(Constants.DATA_SOURCE, null, movieDataSources);
  }

  public List<String> getMovieDataSource() {
    return movieDataSources;
  }

  public void swapMovieDataSource(int pos1, int pos2) {
    String tmp = movieDataSources.get(pos1);
    movieDataSources.set(pos1, movieDataSources.get(pos2));
    movieDataSources.set(pos2, tmp);
    firePropertyChange(MOVIE_DATA_SOURCE, null, movieDataSources);
    firePropertyChange(Constants.DATA_SOURCE, null, movieDataSources);
  }

  public void addNfoFilename(MovieNfoNaming filename) {
    if (!nfoFilenames.contains(filename)) {
      nfoFilenames.add(filename);
      firePropertyChange(NFO_FILENAME, null, nfoFilenames);
    }
  }

  public void clearNfoFilenames() {
    nfoFilenames.clear();
    firePropertyChange(NFO_FILENAME, null, nfoFilenames);
  }

  public List<MovieNfoNaming> getNfoFilenames() {
    return new ArrayList<>(this.nfoFilenames);
  }

  public void addTrailerFilename(MovieTrailerNaming filename) {
    if (!trailerFilenames.contains(filename)) {
      trailerFilenames.add(filename);
      firePropertyChange(TRAILER_FILENAME, null, trailerFilenames);
    }
  }

  public void clearTrailerFilenames() {
    trailerFilenames.clear();
    firePropertyChange(TRAILER_FILENAME, null, trailerFilenames);
  }

  public List<MovieTrailerNaming> getTrailerFilenames() {
    return new ArrayList<>(this.trailerFilenames);
  }

  public void addPosterFilename(MoviePosterNaming filename) {
    if (!posterFilenames.contains(filename)) {
      posterFilenames.add(filename);
      firePropertyChange(POSTER_FILENAME, null, posterFilenames);
    }
  }

  public void clearPosterFilenames() {
    posterFilenames.clear();
    firePropertyChange(POSTER_FILENAME, null, posterFilenames);
  }

  public List<MoviePosterNaming> getPosterFilenames() {
    return new ArrayList<>(this.posterFilenames);
  }

  public void addFanartFilename(MovieFanartNaming filename) {
    if (!fanartFilenames.contains(filename)) {
      fanartFilenames.add(filename);
      firePropertyChange(FANART_FILENAME, null, fanartFilenames);
    }
  }

  public void clearFanartFilenames() {
    fanartFilenames.clear();
    firePropertyChange(FANART_FILENAME, null, fanartFilenames);
  }

  public List<MovieFanartNaming> getFanartFilenames() {
    return new ArrayList<>(this.fanartFilenames);
  }

  public void addExtraFanartFilename(MovieExtraFanartNaming filename) {
    if (!extraFanartFilenames.contains(filename)) {
      extraFanartFilenames.add(filename);
      firePropertyChange(EXTRAFANART_FILENAME, null, extraFanartFilenames);
    }
  }

  public void clearExtraFanartFilenames() {
    extraFanartFilenames.clear();
    firePropertyChange(EXTRAFANART_FILENAME, null, extraFanartFilenames);
  }

  public List<MovieExtraFanartNaming> getExtraFanartFilenames() {
    return new ArrayList<>(this.extraFanartFilenames);
  }

  public void addBannerFilename(MovieBannerNaming filename) {
    if (!bannerFilenames.contains(filename)) {
      bannerFilenames.add(filename);
      firePropertyChange(BANNER_FILENAME, null, bannerFilenames);
    }
  }

  public void clearBannerFilenames() {
    bannerFilenames.clear();
    firePropertyChange(BANNER_FILENAME, null, bannerFilenames);
  }

  public List<MovieBannerNaming> getBannerFilenames() {
    return new ArrayList<>(this.bannerFilenames);
  }

  public void addClearartFilename(MovieClearartNaming filename) {
    if (!clearartFilenames.contains(filename)) {
      clearartFilenames.add(filename);
      firePropertyChange(CLEARART_FILENAME, null, clearartFilenames);
    }
  }

  public void clearClearartFilenames() {
    clearartFilenames.clear();
    firePropertyChange(CLEARART_FILENAME, null, clearartFilenames);
  }

  public List<MovieClearartNaming> getClearartFilenames() {
    return new ArrayList<>(this.clearartFilenames);
  }

  public void addThumbFilename(MovieThumbNaming filename) {
    if (!thumbFilenames.contains(filename)) {
      thumbFilenames.add(filename);
      firePropertyChange(THUMB_FILENAME, null, thumbFilenames);
    }
  }

  public void clearThumbFilenames() {
    thumbFilenames.clear();
    firePropertyChange(THUMB_FILENAME, null, thumbFilenames);
  }

  public List<MovieThumbNaming> getThumbFilenames() {
    return new ArrayList<>(this.thumbFilenames);
  }

  public void addLogoFilename(MovieLogoNaming filename) {
    if (!logoFilenames.contains(filename)) {
      logoFilenames.add(filename);
      firePropertyChange(LOGO_FILENAME, null, logoFilenames);
    }
  }

  public void clearLogoFilenames() {
    logoFilenames.clear();
    firePropertyChange(LOGO_FILENAME, null, logoFilenames);
  }

  public List<MovieLogoNaming> getLogoFilenames() {
    return new ArrayList<>(this.logoFilenames);
  }

  public void addClearlogoFilename(MovieClearlogoNaming filename) {
    if (!clearlogoFilenames.contains(filename)) {
      clearlogoFilenames.add(filename);
      firePropertyChange(CLEARLOGO_FILENAME, null, clearlogoFilenames);
    }
  }

  public void clearClearlogoFilenames() {
    clearlogoFilenames.clear();
    firePropertyChange(CLEARLOGO_FILENAME, null, clearlogoFilenames);
  }

  public List<MovieClearlogoNaming> getClearlogoFilenames() {
    return new ArrayList<>(this.clearlogoFilenames);
  }

  public void addDiscartFilename(MovieDiscartNaming filename) {
    if (!discartFilenames.contains(filename)) {
      discartFilenames.add(filename);
      firePropertyChange(DISCART_FILENAME, null, discartFilenames);
    }
  }

  public void clearDiscartFilenames() {
    discartFilenames.clear();
    firePropertyChange(DISCART_FILENAME, null, discartFilenames);
  }

  public List<MovieDiscartNaming> getDiscartFilenames() {
    return new ArrayList<>(this.discartFilenames);
  }

  public void clearCheckImagesMovie() {
    checkImagesMovie.clear();
    firePropertyChange(CHECK_IMAGES_MOVIE, null, checkImagesMovie);
  }

  public List<MediaArtworkType> getCheckImagesMovie() {
    return new ArrayList<>(this.checkImagesMovie);
  }

  public void addCheckImagesMovie(MediaArtworkType type) {
    if (!checkImagesMovie.contains(type)) {
      checkImagesMovie.add(type);
      firePropertyChange(CHECK_IMAGES_MOVIE, null, checkImagesMovie);
    }
  }

  public void clearCheckImagesMovieSet() {
    checkImagesMovieSet.clear();
    firePropertyChange(CHECK_IMAGES_MOVIESET, null, checkImagesMovieSet);
  }

  public List<MediaArtworkType> getCheckImagesMovieSet() {
    return new ArrayList<>(this.checkImagesMovieSet);
  }

  public void addCheckImagesMovieSet(MediaArtworkType type) {
    if (!checkImagesMovieSet.contains(type)) {
      checkImagesMovieSet.add(type);
      firePropertyChange(CHECK_IMAGES_MOVIESET, null, checkImagesMovieSet);
    }
  }

  public void addKeyartFilename(MovieKeyartNaming filename) {
    if (!keyartFilenames.contains(filename)) {
      keyartFilenames.add(filename);
      firePropertyChange(KEYART_FILENAME, null, keyartFilenames);
    }
  }

  public void clearKeyartFilenames() {
    keyartFilenames.clear();
    firePropertyChange(KEYART_FILENAME, null, keyartFilenames);
  }

  public List<MovieKeyartNaming> getKeyartFilenames() {
    return keyartFilenames;
  }

  public PosterSizes getImagePosterSize() {
    return imagePosterSize;
  }

  public void setImagePosterSize(PosterSizes newValue) {
    PosterSizes oldValue = this.imagePosterSize;
    this.imagePosterSize = newValue;
    firePropertyChange("imagePosterSize", oldValue, newValue);
  }

  public FanartSizes getImageFanartSize() {
    return imageFanartSize;
  }

  public void setImageFanartSize(FanartSizes newValue) {
    FanartSizes oldValue = this.imageFanartSize;
    this.imageFanartSize = newValue;
    firePropertyChange("imageFanartSize", oldValue, newValue);
  }

  public boolean isImageExtraThumbs() {
    return imageExtraThumbs;
  }

  public boolean isImageExtraThumbsResize() {
    return imageExtraThumbsResize;
  }

  public int getImageExtraThumbsSize() {
    return imageExtraThumbsSize;
  }

  public void setImageExtraThumbsResize(boolean newValue) {
    boolean oldValue = this.imageExtraThumbsResize;
    this.imageExtraThumbsResize = newValue;
    firePropertyChange("imageExtraThumbsResize", oldValue, newValue);
  }

  public void setImageExtraThumbsSize(int newValue) {
    int oldValue = this.imageExtraThumbsSize;
    this.imageExtraThumbsSize = newValue;
    firePropertyChange("imageExtraThumbsSize", oldValue, newValue);
  }

  public int getImageExtraThumbsCount() {
    return imageExtraThumbsCount;
  }

  public void setImageExtraThumbsCount(int newValue) {
    int oldValue = this.imageExtraThumbsCount;
    this.imageExtraThumbsCount = newValue;
    firePropertyChange("imageExtraThumbsCount", oldValue, newValue);
  }

  public int getImageExtraFanartCount() {
    return imageExtraFanartCount;
  }

  public void setImageExtraFanartCount(int newValue) {
    int oldValue = this.imageExtraFanartCount;
    this.imageExtraFanartCount = newValue;
    firePropertyChange("imageExtraFanartCount", oldValue, newValue);
  }

  public boolean isImageExtraFanart() {
    return imageExtraFanart;
  }

  public void setImageExtraThumbs(boolean newValue) {
    boolean oldValue = this.imageExtraThumbs;
    this.imageExtraThumbs = newValue;
    firePropertyChange("imageExtraThumbs", oldValue, newValue);
  }

  public void setImageExtraFanart(boolean newValue) {
    boolean oldValue = this.imageExtraFanart;
    this.imageExtraFanart = newValue;
    firePropertyChange("imageExtraFanart", oldValue, newValue);
  }

  public String getMovieSetDataFolder() {
    return movieSetDataFolder;
  }

  public void setMovieSetDataFolder(String newValue) {
    String oldValue = this.movieSetDataFolder;
    this.movieSetDataFolder = newValue;
    firePropertyChange("movieSetDataFolder", oldValue, newValue);
  }

  public MovieConnectors getMovieConnector() {
    return movieConnector;
  }

  public void setMovieConnector(MovieConnectors newValue) {
    MovieConnectors oldValue = this.movieConnector;
    this.movieConnector = newValue;
    firePropertyChange("movieConnector", oldValue, newValue);
  }

  public String getRenamerPathname() {
    return renamerPathname;
  }

  public void setRenamerPathname(String newValue) {
    String oldValue = this.renamerPathname;
    this.renamerPathname = newValue;
    firePropertyChange("renamerPathname", oldValue, newValue);
  }

  public String getRenamerFilename() {
    return renamerFilename;
  }

  public void setRenamerFilename(String newValue) {
    String oldValue = this.renamerFilename;
    this.renamerFilename = newValue;
    firePropertyChange("renamerFilename", oldValue, newValue);
  }

  public boolean isRenamerPathnameSpaceSubstitution() {
    return renamerPathnameSpaceSubstitution;
  }

  public void setRenamerPathnameSpaceSubstitution(boolean newValue) {
    boolean oldValue = this.renamerPathnameSpaceSubstitution;
    this.renamerPathnameSpaceSubstitution = newValue;
    firePropertyChange("renamerPathnameSpaceSubstitution", oldValue, newValue);
  }

  public boolean isRenamerFilenameSpaceSubstitution() {
    return renamerFilenameSpaceSubstitution;
  }

  @JsonProperty(value = "renamerSpaceSubstitution")
  public void setRenamerFilenameSpaceSubstitution(boolean newValue) {
    boolean oldValue = this.renamerFilenameSpaceSubstitution;
    this.renamerFilenameSpaceSubstitution = newValue;
    firePropertyChange("renamerFilenameSpaceSubstitution", oldValue, newValue);
  }

  public void setRenameAfterScrape(boolean newValue) {
    boolean oldValue = this.renameAfterScrape;
    this.renameAfterScrape = newValue;
    firePropertyChange("renameAfterScrape", oldValue, newValue);
  }

  public boolean isRenameAfterScrape() {
    return this.renameAfterScrape;
  }

  public void setArdAfterScrape(boolean newValue) {
    boolean oldValue = this.ardAfterScrape;
    this.ardAfterScrape = newValue;
    firePropertyChange("ardAfterScrape", oldValue, newValue);
  }

  public boolean isArdAfterScrape() {
    return this.ardAfterScrape;
  }

  public boolean isUpdateOnStart() {
    return this.updateOnStart;
  }

  public void setUpdateOnStart(boolean newValue) {
    boolean oldValue = this.updateOnStart;
    this.updateOnStart = newValue;
    firePropertyChange("updateOnStart", oldValue, newValue);
  }

  public String getRenamerPathnameSpaceReplacement() {
    return renamerPathnameSpaceReplacement;
  }

  public void setRenamerPathnameSpaceReplacement(String newValue) {
    String oldValue = this.renamerPathnameSpaceReplacement;
    this.renamerPathnameSpaceReplacement = newValue;
    firePropertyChange("renamerPathnameSpaceReplacement", oldValue, newValue);
  }

  @JsonProperty(value = "renamerSpaceReplacement")
  public String getRenamerFilenameSpaceReplacement() {
    return renamerFilenameSpaceReplacement;
  }

  public void setRenamerFilenameSpaceReplacement(String newValue) {
    String oldValue = this.renamerFilenameSpaceReplacement;
    this.renamerFilenameSpaceReplacement = newValue;
    firePropertyChange("renamerFilenameSpaceReplacement", oldValue, newValue);
  }

  public String getRenamerColonReplacement() {
    return renamerColonReplacement;
  }

  public void setRenamerColonReplacement(String newValue) {
    String oldValue = this.renamerColonReplacement;
    this.renamerColonReplacement = newValue;
    firePropertyChange("renamerColonReplacement", oldValue, newValue);
  }

  public String getRenamerFirstCharacterNumberReplacement() {
    return renamerFirstCharacterNumberReplacement;
  }

  public void setRenamerFirstCharacterNumberReplacement(String newValue) {
    String oldValue = this.renamerFirstCharacterNumberReplacement;
    this.renamerFirstCharacterNumberReplacement = newValue;
    firePropertyChange("renamerFirstCharacterNumberReplacement", oldValue, newValue);
  }

  public String getMovieScraper() {
    if (StringUtils.isBlank(movieScraper)) {
      return Constants.TMDB;
    }
    return movieScraper;
  }

  public void setMovieScraper(String newValue) {
    String oldValue = this.movieScraper;
    this.movieScraper = newValue;
    firePropertyChange("movieScraper", oldValue, newValue);
  }

  public void addMovieArtworkScraper(String newValue) {
    if (!artworkScrapers.contains(newValue)) {
      artworkScrapers.add(newValue);
      firePropertyChange(ARTWORK_SCRAPERS, null, artworkScrapers);
    }
  }

  public void removeMovieArtworkScraper(String newValue) {
    if (artworkScrapers.contains(newValue)) {
      artworkScrapers.remove(newValue);
      firePropertyChange(ARTWORK_SCRAPERS, null, artworkScrapers);
    }
  }

  public List<String> getArtworkScrapers() {
    return artworkScrapers;
  }

  public boolean isScrapeBestImage() {
    return scrapeBestImage;
  }

  public void setScrapeBestImage(boolean newValue) {
    boolean oldValue = this.scrapeBestImage;
    this.scrapeBestImage = newValue;
    firePropertyChange("scrapeBestImage", oldValue, newValue);
  }

  public boolean isScrapeBestImageMovieSet() {
    return scrapeBestImageMovieSet;
  }

  public void setScrapeBestImageMovieSet(boolean newValue) {
    boolean oldValue = this.scrapeBestImageMovieSet;
    this.scrapeBestImageMovieSet = newValue;
    firePropertyChange("scrapeBestImageMovieSet", oldValue, newValue);
  }

  public void addMovieTrailerScraper(String newValue) {
    if (!trailerScrapers.contains(newValue)) {
      trailerScrapers.add(newValue);
      firePropertyChange(TRAILER_SCRAPERS, null, trailerScrapers);
    }
  }

  public void removeMovieTrailerScraper(String newValue) {
    if (trailerScrapers.contains(newValue)) {
      trailerScrapers.remove(newValue);
      firePropertyChange(TRAILER_SCRAPERS, null, trailerScrapers);
    }
  }

  public List<String> getTrailerScrapers() {
    return trailerScrapers;
  }

  public void addMovieSubtitleScraper(String newValue) {
    if (!subtitleScrapers.contains(newValue)) {
      subtitleScrapers.add(newValue);
      firePropertyChange(SUBTITLE_SCRAPERS, null, subtitleScrapers);
    }
  }

  public void removeMovieSubtitleScraper(String newValue) {
    if (subtitleScrapers.contains(newValue)) {
      subtitleScrapers.remove(newValue);
      firePropertyChange(SUBTITLE_SCRAPERS, null, subtitleScrapers);
    }
  }

  public List<String> getSubtitleScrapers() {
    return subtitleScrapers;
  }

  public void addSkipFolder(String newValue) {
    if (!skipFolders.contains(newValue)) {
      skipFolders.add(newValue);
      firePropertyChange(SKIP_FOLDER, null, skipFolders);
    }
  }

  public void removeSkipFolder(String newValue) {
    if (skipFolders.contains(newValue)) {
      skipFolders.remove(newValue);
      firePropertyChange(SKIP_FOLDER, null, skipFolders);
    }
  }

  public List<String> getSkipFolder() {
    return skipFolders;
  }

  public Map<String, List<UIFilters>> getMovieUiFilterPresets() {
    return movieUiFilterPresets;
  }

  public void setMovieUiFilterPresets(Map<String, List<UIFilters>> newValues) {
    movieUiFilterPresets.clear();
    movieUiFilterPresets.putAll(newValues);
    firePropertyChange(MOVIE_UI_FILTER_PRESETS, null, movieUiFilterPresets);
  }

  public Map<String, List<UIFilters>> getMovieSetUiFilterPresets() {
    return movieSetUiFilterPresets;
  }

  public void setMovieSetUiFilterPresets(Map<String, List<UIFilters>> newValues) {
    movieSetUiFilterPresets.clear();
    movieSetUiFilterPresets.putAll(newValues);
    firePropertyChange(MOVIE_SET_UI_FILTER_PRESETS, null, movieSetUiFilterPresets);
  }

  public boolean isWriteActorImages() {
    return writeActorImages;
  }

  public void setWriteActorImages(boolean newValue) {
    boolean oldValue = this.writeActorImages;
    this.writeActorImages = newValue;
    firePropertyChange("writeActorImages", oldValue, newValue);
  }

  public MediaLanguages getScraperLanguage() {
    return scraperLanguage;
  }

  public void setScraperLanguage(MediaLanguages newValue) {
    MediaLanguages oldValue = this.scraperLanguage;
    this.scraperLanguage = newValue;
    firePropertyChange("scraperLanguage", oldValue, newValue);
  }

  public MediaLanguages getSubtitleScraperLanguage() {
    return subtitleScraperLanguage;
  }

  public void setSubtitleScraperLanguage(MediaLanguages newValue) {
    MediaLanguages oldValue = this.subtitleScraperLanguage;
    this.subtitleScraperLanguage = newValue;
    firePropertyChange("subtitleScraperLanguage", oldValue, newValue);
  }

  public CountryCode getCertificationCountry() {
    return certificationCountry;
  }

  public void setCertificationCountry(CountryCode newValue) {
    CountryCode oldValue = this.certificationCountry;
    certificationCountry = newValue;
    firePropertyChange("certificationCountry", oldValue, newValue);
  }

  public String getReleaseDateCountry() {
    return releaseDateCountry;
  }

  public void setReleaseDateCountry(String newValue) {
    String oldValue = this.releaseDateCountry;
    this.releaseDateCountry = newValue;
    firePropertyChange("releaseDateCountry", oldValue, newValue);
  }

  public double getScraperThreshold() {
    return scraperThreshold;
  }

  public void setScraperThreshold(double newValue) {
    double oldValue = this.scraperThreshold;
    scraperThreshold = newValue;
    firePropertyChange("scraperThreshold", oldValue, newValue);
  }

  public boolean isRenamerNfoCleanup() {
    return renamerNfoCleanup;
  }

  public void setRenamerNfoCleanup(boolean newValue) {
    boolean oldValue = this.renamerNfoCleanup;
    this.renamerNfoCleanup = newValue;
    firePropertyChange("renamerNfoCleanup", oldValue, newValue);
  }

  public boolean isBuildImageCacheOnImport() {
    return buildImageCacheOnImport;
  }

  public void setBuildImageCacheOnImport(boolean newValue) {
    boolean oldValue = this.buildImageCacheOnImport;
    this.buildImageCacheOnImport = newValue;
    firePropertyChange("buildImageCacheOnImport", oldValue, newValue);
  }

  public boolean isRenamerCreateMoviesetForSingleMovie() {
    return renamerCreateMoviesetForSingleMovie;
  }

  public void setRenamerCreateMoviesetForSingleMovie(boolean newValue) {
    boolean oldValue = this.renamerCreateMoviesetForSingleMovie;
    this.renamerCreateMoviesetForSingleMovie = newValue;
    firePropertyChange("renamerCreateMoviesetForSingleMovie", oldValue, newValue);
  }

  public boolean isRuntimeFromMediaInfo() {
    return runtimeFromMediaInfo;
  }

  public void setRuntimeFromMediaInfo(boolean newValue) {
    boolean oldValue = this.runtimeFromMediaInfo;
    this.runtimeFromMediaInfo = newValue;
    firePropertyChange("runtimeFromMediaInfo", oldValue, newValue);
  }

  public boolean isExtractArtworkFromVsmeta() {
    return extractArtworkFromVsmeta;
  }

  public void setExtractArtworkFromVsmeta(boolean newValue) {
    boolean oldValue = this.extractArtworkFromVsmeta;
    this.extractArtworkFromVsmeta = newValue;
    firePropertyChange("extractArtworkFromVsmeta", oldValue, newValue);
  }

  public boolean isUseMediainfoMetadata() {
    return useMediainfoMetadata;
  }

  public void setUseMediainfoMetadata(boolean newValue) {
    boolean oldValue = this.useMediainfoMetadata;
    this.useMediainfoMetadata = newValue;
    firePropertyChange("useMediainfoMetadata", oldValue, newValue);
  }

  public void setTitle(boolean newValue) {
    boolean oldValue = this.title;
    this.title = newValue;
    firePropertyChange("title", oldValue, newValue);
  }

  public void setSortableTitle(boolean newValue) {
    boolean oldValue = this.sortableTitle;
    this.sortableTitle = newValue;
    firePropertyChange("sortableTitle", oldValue, newValue);
  }

  public void setOriginalTitle(boolean newValue) {
    boolean oldValue = this.originalTitle;
    this.originalTitle = newValue;
    firePropertyChange("originalTitle", oldValue, newValue);
  }

  public void setSortableOriginalTitle(boolean newValue) {
    boolean oldValue = this.sortableOriginalTitle;
    this.sortableOriginalTitle = newValue;
    firePropertyChange("sortableOriginalTitle", oldValue, newValue);
  }

  public void setSortTitle(boolean newValue) {
    boolean oldValue = this.sortTitle;
    this.sortTitle = newValue;
    firePropertyChange("sortTitle", oldValue, newValue);
  }

  public void setNote(boolean newValue) {
    boolean oldValue = this.note;
    this.note = newValue;
    firePropertyChange("note", oldValue, newValue);
  }

  public boolean getTitle() {
    return this.title;
  }

  public boolean getSortableTitle() {
    return this.sortableTitle;
  }

  public boolean getOriginalTitle() {
    return this.originalTitle;
  }

  public boolean getSortableOriginalTitle() {
    return this.sortableOriginalTitle;
  }

  public boolean getSortTitle() {
    return this.sortTitle;
  }

  public boolean getNote() {
    return this.note;
  }

  public boolean isIncludeExternalAudioStreams() {
    return includeExternalAudioStreams;
  }

  public void setIncludeExternalAudioStreams(boolean newValue) {
    boolean oldValue = this.includeExternalAudioStreams;
    this.includeExternalAudioStreams = newValue;
    firePropertyChange("includeExternalAudioStreams", oldValue, newValue);
  }

  public boolean isAsciiReplacement() {
    return asciiReplacement;
  }

  public void setAsciiReplacement(boolean newValue) {
    boolean oldValue = this.asciiReplacement;
    this.asciiReplacement = newValue;
    firePropertyChange("asciiReplacement", oldValue, newValue);
  }

  public void addBadWord(String badWord) {
    if (!badWords.contains(badWord.toLowerCase(Locale.ROOT))) {
      badWords.add(badWord.toLowerCase(Locale.ROOT));
      firePropertyChange(BAD_WORD, null, badWords);
    }
  }

  public void removeBadWord(String badWord) {
    badWords.remove(badWord.toLowerCase(Locale.ROOT));
    firePropertyChange(BAD_WORD, null, badWords);
  }

  public List<String> getBadWord() {
    // convert to lowercase for easy contains checking
    ListIterator<String> iterator = badWords.listIterator();
    while (iterator.hasNext()) {
      iterator.set(iterator.next().toLowerCase(Locale.ROOT));
    }
    return badWords;
  }

  public boolean isScraperFallback() {
    return scraperFallback;
  }

  public void setScraperFallback(boolean newValue) {
    boolean oldValue = this.scraperFallback;
    this.scraperFallback = newValue;
    firePropertyChange("scraperFallback", oldValue, newValue);
  }

  public boolean isUseTrailerPreference() {
    return useTrailerPreference;
  }

  public void setUseTrailerPreference(boolean newValue) {
    boolean oldValue = this.useTrailerPreference;
    this.useTrailerPreference = newValue;
    firePropertyChange("useTrailerPreference", oldValue, newValue);
    // also influences the automatic trailer download
    firePropertyChange("automaticTrailerDownload", oldValue, newValue);
  }

  public boolean isAutomaticTrailerDownload() {
    // only available if the trailer preference is set
    return useTrailerPreference && automaticTrailerDownload;
  }

  public void setAutomaticTrailerDownload(boolean newValue) {
    boolean oldValue = this.automaticTrailerDownload;
    this.automaticTrailerDownload = newValue;
    firePropertyChange("automaticTrailerDownload", oldValue, newValue);
  }

  public TrailerQuality getTrailerQuality() {
    return trailerQuality;
  }

  public void setTrailerQuality(TrailerQuality newValue) {
    TrailerQuality oldValue = this.trailerQuality;
    this.trailerQuality = newValue;
    firePropertyChange("trailerQuality", oldValue, newValue);
  }

  public TrailerSources getTrailerSource() {
    return trailerSource;
  }

  public void setTrailerSource(TrailerSources newValue) {
    TrailerSources oldValue = this.trailerSource;
    this.trailerSource = newValue;
    firePropertyChange("trailerSource", oldValue, newValue);
  }

  public void setSyncTrakt(boolean newValue) {
    boolean oldValue = this.syncTrakt;
    this.syncTrakt = newValue;
    firePropertyChange("syncTrakt", oldValue, newValue);
  }

  public boolean getSyncTrakt() {
    return syncTrakt;
  }

  public void setSyncTraktCollection(boolean newValue) {
    boolean oldValue = this.syncTraktCollection;
    this.syncTraktCollection = newValue;
    firePropertyChange("syncTraktCollection", oldValue, newValue);
  }

  public boolean getSyncTraktCollection() {
    return syncTraktCollection;
  }

  public void setSyncTraktWatched(boolean newValue) {
    boolean oldValue = this.syncTraktWatched;
    this.syncTraktWatched = newValue;
    firePropertyChange("syncTraktWatched", oldValue, newValue);
  }

  public boolean getSyncTraktWatched() {
    return syncTraktWatched;
  }

  public void setSyncTraktRating(boolean newValue) {
    boolean oldValue = this.syncTraktRating;
    this.syncTraktRating = newValue;
    firePropertyChange("syncTraktRating", oldValue, newValue);
  }

  public boolean getSyncTraktRating() {
    return syncTraktRating;
  }

  public List<String> getRatingSources() {
    return ratingSources;
  }

  public void setRatingSources(List<String> newValue) {
    ratingSources.clear();
    ratingSources.addAll(newValue);
    firePropertyChange("ratingSources", null, ratingSources);
  }

  public void addRatingSource(String ratingSource) {
    if (!ratingSources.contains(ratingSource)) {
      ratingSources.add(ratingSource);
      firePropertyChange("ratingSources", null, ratingSources);
    }
  }

  public void removeRatingSource(String ratingSource) {
    if (ratingSources.remove(ratingSource)) {
      firePropertyChange("ratingSources", null, ratingSources);
    }
  }

  public void swapRatingSources(int pos1, int pos2) {
    String tmp = ratingSources.get(pos1);
    ratingSources.set(pos1, ratingSources.get(pos2));
    ratingSources.set(pos2, tmp);
    firePropertyChange("ratingSources", null, ratingSources);
  }

  public MediaLanguages getImageScraperLanguage() {
    return imageScraperLanguage;
  }

  public void setImageScraperLanguage(MediaLanguages newValue) {
    MediaLanguages oldValue = this.imageScraperLanguage;
    this.imageScraperLanguage = newValue;
    firePropertyChange("imageScraperLanguage", oldValue, newValue);
  }

  public boolean isImageLanguagePriority() {
    return imageLanguagePriority;
  }

  public void setImageLanguagePriority(boolean newValue) {
    boolean oldValue = this.imageLanguagePriority;
    this.imageLanguagePriority = newValue;
    firePropertyChange("imageLanguagePriority", oldValue, newValue);
  }

  public CertificationStyle getCertificationStyle() {
    return certificationStyle;
  }

  public void setCertificationStyle(CertificationStyle newValue) {
    CertificationStyle oldValue = this.certificationStyle;
    this.certificationStyle = newValue;
    firePropertyChange("certificationStyle", oldValue, newValue);
  }

  public LanguageStyle getSubtitleLanguageStyle() {
    return subtitleLanguageStyle;
  }

  public void setSubtitleLanguageStyle(LanguageStyle newValue) {
    LanguageStyle oldValue = this.subtitleLanguageStyle;
    this.subtitleLanguageStyle = newValue;
    firePropertyChange("subtitleLanguageStyle", oldValue, newValue);
  }

  public boolean isSubtitleWithoutLanguageTag() {
    return subtitleWithoutLanguageTag;
  }

  public void setSubtitleWithoutLanguageTag(boolean newValue) {
    boolean oldValue = this.subtitleWithoutLanguageTag;
    this.subtitleWithoutLanguageTag = newValue;
    firePropertyChange("subtitleWithoutLanguageTag", oldValue, newValue);
  }

  public boolean isSubtitleForceBestMatch() {
    return subtitleForceBestMatch;
  }

  public void setSubtitleForceBestMatch(boolean newValue) {
    boolean oldValue = this.subtitleForceBestMatch;
    this.subtitleForceBestMatch = newValue;
    firePropertyChange("subtitleForceBestMatch", oldValue, newValue);
  }

  public List<MovieScraperMetadataConfig> getScraperMetadataConfig() {
    return scraperMetadataConfig;
  }

  public void setScraperMetadataConfig(List<MovieScraperMetadataConfig> newValues) {
    scraperMetadataConfig.clear();
    scraperMetadataConfig.addAll(newValues);
    firePropertyChange("scraperMetadataConfig", null, scraperMetadataConfig);
  }

  public boolean isWriteCleanNfo() {
    return writeCleanNfo;
  }

  public void setWriteCleanNfo(boolean newValue) {
    boolean oldValue = writeCleanNfo;
    this.writeCleanNfo = newValue;
    firePropertyChange("writeCleanNfo", oldValue, newValue);
  }

  public DateField getNfoDateAddedField() {
    return nfoDateAddedField;
  }

  public void setNfoDateAddedField(DateField newValue) {
    DateField oldValue = nfoDateAddedField;
    this.nfoDateAddedField = newValue;
    firePropertyChange("nfoDateAddedField", oldValue, newValue);
  }

  public boolean isNfoWriteSingleStudio() {
    return nfoWriteSingleStudio;
  }

  public void setNfoWriteSingleStudio(boolean newValue) {
    boolean oldValue = nfoWriteSingleStudio;
    nfoWriteSingleStudio = newValue;
    firePropertyChange("nfoWriteSingleStudio", oldValue, newValue);
  }

  public MediaLanguages getNfoLanguage() {
    return nfoLanguage;
  }

  public void setNfoLanguage(MediaLanguages newValue) {
    MediaLanguages oldValue = nfoLanguage;
    this.nfoLanguage = newValue;
    firePropertyChange("nfoLanguage", oldValue, newValue);
  }

  public boolean isCreateOutline() {
    return createOutline;
  }

  public void setCreateOutline(boolean newValue) {
    boolean oldValue = this.createOutline;
    this.createOutline = newValue;
    firePropertyChange("createOutline", oldValue, newValue);
  }

  public boolean isOutlineFirstSentence() {
    return outlineFirstSentence;
  }

  public void setOutlineFirstSentence(boolean newValue) {
    boolean oldValue = this.outlineFirstSentence;
    this.outlineFirstSentence = newValue;
    firePropertyChange("outlineFirstSentence", oldValue, newValue);
  }

  public boolean getCapitalWordsInTitles() {
    return capitalWordsInTitles;
  }

  public void setCapitalWordsInTitles(boolean newValue) {
    boolean oldValue = this.capitalWordsInTitles;
    this.capitalWordsInTitles = newValue;
    firePropertyChange("capitalWordsInTitles", oldValue, newValue);
  }

  public MovieSetConnectors getMovieSetConnector() {
    return movieSetConnector;
  }

  public void setMovieSetConnector(MovieSetConnectors newValue) {
    MovieSetConnectors oldValue = this.movieSetConnector;
    this.movieSetConnector = newValue;
    firePropertyChange("movieSetConnector", oldValue, newValue);
  }

  public void addMovieSetNfoFilename(MovieSetNfoNaming filename) {
    if (!movieSetNfoFilenames.contains(filename)) {
      movieSetNfoFilenames.add(filename);
      firePropertyChange("movieSetNfoFilenames", null, movieSetNfoFilenames);
    }
  }

  public void clearMovieSetNfoFilenames() {
    movieSetNfoFilenames.clear();
    firePropertyChange("movieSetNfoFilenames", null, movieSetNfoFilenames);
  }

  public List<MovieSetNfoNaming> getMovieSetNfoFilenames() {
    return new ArrayList<>(this.movieSetNfoFilenames);
  }

  public void addMovieSetPosterFilename(MovieSetPosterNaming filename) {
    if (!movieSetPosterFilenames.contains(filename)) {
      movieSetPosterFilenames.add(filename);
      firePropertyChange(MOVIE_SET_POSTER_FILENAME, null, movieSetPosterFilenames);
    }
  }

  public void clearMovieSetPosterFilenames() {
    movieSetPosterFilenames.clear();
    firePropertyChange(MOVIE_SET_POSTER_FILENAME, null, movieSetPosterFilenames);
  }

  public List<MovieSetPosterNaming> getMovieSetPosterFilenames() {
    return new ArrayList<>(this.movieSetPosterFilenames);
  }

  public void addMovieSetFanartFilename(MovieSetFanartNaming filename) {
    if (!movieSetFanartFilenames.contains(filename)) {
      movieSetFanartFilenames.add(filename);
      firePropertyChange(MOVIE_SET_FANART_FILENAME, null, movieSetFanartFilenames);
    }
  }

  public void clearMovieSetFanartFilenames() {
    movieSetFanartFilenames.clear();
    firePropertyChange(MOVIE_SET_FANART_FILENAME, null, movieSetFanartFilenames);
  }

  public List<MovieSetFanartNaming> getMovieSetFanartFilenames() {
    return new ArrayList<>(this.movieSetFanartFilenames);
  }

  public void addMovieSetBannerFilename(MovieSetBannerNaming filename) {
    if (!movieSetBannerFilenames.contains(filename)) {
      movieSetBannerFilenames.add(filename);
      firePropertyChange(MOVIE_SET_BANNER_FILENAME, null, movieSetBannerFilenames);
    }
  }

  public void clearMovieSetBannerFilenames() {
    movieSetBannerFilenames.clear();
    firePropertyChange(MOVIE_SET_BANNER_FILENAME, null, movieSetBannerFilenames);
  }

  public List<MovieSetBannerNaming> getMovieSetBannerFilenames() {
    return new ArrayList<>(this.movieSetBannerFilenames);
  }

  public void addMovieSetClearartFilename(MovieSetClearartNaming filename) {
    if (!movieSetClearartFilenames.contains(filename)) {
      movieSetClearartFilenames.add(filename);
      firePropertyChange(MOVIE_SET_CLEARART_FILENAME, null, movieSetClearartFilenames);
    }
  }

  public void clearMovieSetClearartFilenames() {
    movieSetClearartFilenames.clear();
    firePropertyChange(MOVIE_SET_CLEARART_FILENAME, null, movieSetClearartFilenames);
  }

  public List<MovieSetClearartNaming> getMovieSetClearartFilenames() {
    return new ArrayList<>(this.movieSetClearartFilenames);
  }

  public void addMovieSetThumbFilename(MovieSetThumbNaming filename) {
    if (!movieSetThumbFilenames.contains(filename)) {
      movieSetThumbFilenames.add(filename);
      firePropertyChange(MOVIE_SET_THUMB_FILENAME, null, movieSetThumbFilenames);
    }
  }

  public void clearMovieSetThumbFilenames() {
    movieSetThumbFilenames.clear();
    firePropertyChange(MOVIE_SET_THUMB_FILENAME, null, movieSetThumbFilenames);
  }

  public List<MovieSetThumbNaming> getMovieSetThumbFilenames() {
    return new ArrayList<>(this.movieSetThumbFilenames);
  }

  public void addMovieSetLogoFilename(MovieSetLogoNaming filename) {
    if (!movieSetLogoFilenames.contains(filename)) {
      movieSetLogoFilenames.add(filename);
      firePropertyChange(MOVIE_SET_LOGO_FILENAME, null, movieSetLogoFilenames);
    }
  }

  public void clearMovieSetLogoFilenames() {
    movieSetLogoFilenames.clear();
    firePropertyChange(MOVIE_SET_LOGO_FILENAME, null, movieSetLogoFilenames);
  }

  public List<MovieSetLogoNaming> getMovieSetLogoFilenames() {
    return new ArrayList<>(this.movieSetLogoFilenames);
  }

  public void addMovieSetClearlogoFilename(MovieSetClearlogoNaming filename) {
    if (!movieSetClearlogoFilenames.contains(filename)) {
      movieSetClearlogoFilenames.add(filename);
      firePropertyChange(MOVIE_SET_CLEARLOGO_FILENAME, null, movieSetClearlogoFilenames);
    }
  }

  public void clearMovieSetClearlogoFilenames() {
    movieSetClearlogoFilenames.clear();
    firePropertyChange(MOVIE_SET_CLEARLOGO_FILENAME, null, movieSetClearlogoFilenames);
  }

  public List<MovieSetClearlogoNaming> getMovieSetClearlogoFilenames() {
    return new ArrayList<>(this.movieSetClearlogoFilenames);
  }

  public void addMovieSetDiscartFilename(MovieSetDiscartNaming filename) {
    if (!movieSetDiscartFilenames.contains(filename)) {
      movieSetDiscartFilenames.add(filename);
      firePropertyChange(MOVIE_SET_DISCART_FILENAME, null, movieSetDiscartFilenames);
    }
  }

  public void clearMovieSetDiscartFilenames() {
    movieSetDiscartFilenames.clear();
    firePropertyChange(MOVIE_SET_DISCART_FILENAME, null, movieSetDiscartFilenames);
  }

  public List<MovieSetDiscartNaming> getMovieSetDiscartFilenames() {
    return new ArrayList<>(this.movieSetDiscartFilenames);
  }

  public boolean isShowMovieTableTooltips() {
    return showMovieTableTooltips;
  }

  public void setShowMovieTableTooltips(boolean newValue) {
    boolean oldValue = showMovieTableTooltips;
    showMovieTableTooltips = newValue;
    firePropertyChange("showMovieTableTooltips", oldValue, newValue);
  }

  public boolean isShowMovieSetTableTooltips() {
    return showMovieSetTableTooltips;
  }

  public void setShowMovieSetTableTooltips(boolean newValue) {
    boolean oldValue = showMovieSetTableTooltips;
    showMovieSetTableTooltips = newValue;
    firePropertyChange("showMovieSetTableTooltips", oldValue, newValue);
  }

  public boolean isDisplayMovieSetMissingMovies() {
    return displayMovieSetMissingMovies;
  }

  public void setDisplayMovieSetMissingMovies(boolean newValue) {
    boolean oldValue = this.displayMovieSetMissingMovies;
    this.displayMovieSetMissingMovies = newValue;
    firePropertyChange("displayMovieSetMissingMovies", oldValue, newValue);
  }

  public void setUiFilters(List<UIFilters> filters) {
    uiFilters.clear();
    uiFilters.addAll(filters);
    firePropertyChange("uiFilters", null, uiFilters);
  }

  public List<UIFilters> getUiFilters() {
    if (storeUiFilters) {
      return uiFilters;
    }
    return new ArrayList<>();
  }

  public void setStoreUiFilters(boolean newValue) {
    boolean oldValue = this.storeUiFilters;
    this.storeUiFilters = newValue;
    firePropertyChange("storeUiFilters", oldValue, newValue);
  }

  public boolean isStoreUiFilters() {
    return storeUiFilters;
  }

  public void setMovieSetUiFilters(List<UIFilters> filters) {
    movieSetUiFilters.clear();
    movieSetUiFilters.addAll(filters);
    firePropertyChange("movieSetUiFilters", null, movieSetUiFilters);
  }

  public List<UIFilters> getMovieSetUiFilters() {
    if (storeUiFilters) {
      return movieSetUiFilters;
    }
    return new ArrayList<>();
  }

  public void setStoreMovieSetUiFilters(boolean newValue) {
    boolean oldValue = this.storeMovieSetUiFilters;
    this.storeMovieSetUiFilters = newValue;
    firePropertyChange("storeMovieSetUiFilters", oldValue, newValue);
  }

  public boolean isStoreMovieSetUiFilters() {
    return storeMovieSetUiFilters;
  }


}
