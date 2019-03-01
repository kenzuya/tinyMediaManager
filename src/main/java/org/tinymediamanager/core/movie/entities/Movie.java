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
package org.tinymediamanager.core.movie.entities;

import static org.tinymediamanager.core.Constants.ACTORS;
import static org.tinymediamanager.core.Constants.CERTIFICATION;
import static org.tinymediamanager.core.Constants.COUNTRY;
import static org.tinymediamanager.core.Constants.DIRECTORS;
import static org.tinymediamanager.core.Constants.DIRECTORS_AS_STRING;
import static org.tinymediamanager.core.Constants.EDITION;
import static org.tinymediamanager.core.Constants.EDITION_AS_STRING;
import static org.tinymediamanager.core.Constants.GENRE;
import static org.tinymediamanager.core.Constants.GENRES_AS_STRING;
import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.IMDB;
import static org.tinymediamanager.core.Constants.MEDIA_SOURCE;
import static org.tinymediamanager.core.Constants.MOVIESET;
import static org.tinymediamanager.core.Constants.MOVIESET_TITLE;
import static org.tinymediamanager.core.Constants.PRODUCERS;
import static org.tinymediamanager.core.Constants.RELEASE_DATE;
import static org.tinymediamanager.core.Constants.RELEASE_DATE_AS_STRING;
import static org.tinymediamanager.core.Constants.RUNTIME;
import static org.tinymediamanager.core.Constants.SORT_TITLE;
import static org.tinymediamanager.core.Constants.SPOKEN_LANGUAGES;
import static org.tinymediamanager.core.Constants.TAG;
import static org.tinymediamanager.core.Constants.TAGS_AS_STRING;
import static org.tinymediamanager.core.Constants.TITLE_FOR_UI;
import static org.tinymediamanager.core.Constants.TITLE_SORTABLE;
import static org.tinymediamanager.core.Constants.TMDB;
import static org.tinymediamanager.core.Constants.TOP250;
import static org.tinymediamanager.core.Constants.TRAILER;
import static org.tinymediamanager.core.Constants.VIDEO_IN_3D;
import static org.tinymediamanager.core.Constants.WATCHED;
import static org.tinymediamanager.core.Constants.WRITERS;
import static org.tinymediamanager.core.Constants.WRITERS_AS_STRING;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IMediaInformation;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.MediaSource;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.entities.Rating;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieMediaFileComparator;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieTrailerQuality;
import org.tinymediamanager.core.movie.MovieTrailerSources;
import org.tinymediamanager.core.movie.connector.IMovieConnector;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.connector.MovieToKodiConnector;
import org.tinymediamanager.core.movie.connector.MovieToMediaportalConnector;
import org.tinymediamanager.core.movie.connector.MovieToXbmcConnector;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.core.movie.tasks.MovieActorImageFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.Certification;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCastMember;
import org.tinymediamanager.scraper.entities.MediaGenres;
import org.tinymediamanager.scraper.entities.MediaRating;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.exceptions.UnsupportedMediaTypeException;
import org.tinymediamanager.scraper.mediaprovider.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The main class for movies.
 * 
 * @author Manuel Laggner / Myron Boyle
 */
public class Movie extends MediaEntity implements IMediaInformation {
  @XmlTransient
  private static final Logger                   LOGGER                     = LoggerFactory.getLogger(Movie.class);
  private static final Comparator<MediaFile>    MEDIA_FILE_COMPARATOR      = new MovieMediaFileComparator();
  private static final Comparator<MovieTrailer> TRAILER_QUALITY_COMPARATOR = new MovieTrailer.QualityComparator();

  @JsonProperty
  private String                                sortTitle                  = "";
  @JsonProperty
  private String                                tagline                    = "";
  @JsonProperty
  private int                                   runtime                    = 0;
  @JsonProperty
  private boolean                               watched                    = false;
  @JsonProperty
  private boolean                               isDisc                     = false;
  @JsonProperty
  private String                                spokenLanguages            = "";
  @JsonProperty
  private boolean                               subtitles                  = false;
  @JsonProperty
  private String                                country                    = "";
  @JsonProperty
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date                                  releaseDate                = null;
  @JsonProperty
  private boolean                               multiMovieDir              = false;                               // we detected more movies in
                                                                                                                  // same folder
  @JsonProperty
  private int                                   top250                     = 0;
  @JsonProperty
  private MediaSource                           mediaSource                = MediaSource.UNKNOWN;                 // DVD, Bluray, etc
  @JsonProperty
  private boolean                               videoIn3D                  = false;
  @JsonProperty
  private Certification                         certification              = Certification.UNKNOWN;
  @JsonProperty
  private UUID                                  movieSetId;
  @JsonProperty
  private MovieEdition                          edition                    = MovieEdition.NONE;
  @JsonProperty
  private boolean                               stacked                    = false;
  @JsonProperty
  private boolean                               offline                    = false;

  @JsonProperty
  private List<MediaGenres>                     genres                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<String>                          tags                       = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<String>                          extraThumbs                = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<String>                          extraFanarts               = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<Person>                          actors                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<Person>                          producers                  = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<Person>                          directors                  = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<Person>                          writers                    = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<MovieTrailer>                    trailer                    = new CopyOnWriteArrayList<>();

  private MovieSet                              movieSet;
  private String                                titleSortable              = "";
  private String                                originalTitleSortable      = "";
  private Date                                  lastWatched                = null;

  /**
   * Instantiates a new movie. To initialize the propertychangesupport after loading
   */
  public Movie() {
    // register for dirty flag listener
    super();
  }

  /**
   * Overwrites all null/empty elements with "other" value (but might be also empty)<br>
   * For lists, check with 'contains' and add.<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   * 
   * @param other
   *          the movie to merge in
   */
  public void merge(Movie other) {
    merge(other, false);
  }

  /**
   * Overwrites all elements with "other" value<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the movie to merge in
   */
  public void forceMerge(Movie other) {
    merge(other, true);
  }

  void merge(Movie other, boolean force) {
    if (other == null) {
      return;
    }
    super.merge(other, force);

    setSortTitle(StringUtils.isEmpty(sortTitle) || force ? other.sortTitle : sortTitle);
    setTagline(StringUtils.isEmpty(tagline) || force ? other.tagline : tagline);
    setSpokenLanguages(StringUtils.isEmpty(spokenLanguages) || force ? other.spokenLanguages : spokenLanguages);
    setCountry(StringUtils.isEmpty(country) || force ? other.country : country);
    setWatched(!watched || force ? other.watched : watched);
    setRuntime(runtime == 0 || force ? other.runtime : runtime);
    setTop250(top250 == 0 || force ? other.top250 : top250);
    setReleaseDate(releaseDate == null || force ? other.releaseDate : releaseDate);
    setMovieSet(movieSet == null || force ? other.movieSet : movieSet);
    setMediaSource(mediaSource == MediaSource.UNKNOWN || force ? other.mediaSource : mediaSource);
    setCertification(certification == Certification.UNKNOWN || force ? other.certification : certification);
    setEdition(edition == MovieEdition.NONE || force ? other.edition : edition);

    // when force is set, clear the lists/maps and add all other values
    if (force) {
      genres.clear();
      actors.clear();
      producers.clear();
      directors.clear();
      writers.clear();
      tags.clear();
      trailer.clear();
      extraFanarts.clear();
      extraThumbs.clear();
    }

    setGenres(other.genres);
    setActors(other.actors);
    setProducers(other.producers);
    setDirectors(other.directors);
    setWriters(other.writers);
    setTags(other.tags);
    setExtraFanarts(other.extraFanarts);
    setExtraThumbs(other.extraThumbs);

    ArrayList<MovieTrailer> mergedTrailers = new ArrayList<>(trailer);
    ListUtils.mergeLists(mergedTrailers, other.trailer);
    setTrailers(mergedTrailers);
  }

  @Override
  protected Comparator<MediaFile> getMediaFileComparator() {
    return MEDIA_FILE_COMPARATOR;
  }

  /**
   * checks if this movie has been scraped.<br>
   * On a fresh DB, just reading local files, everything is again "unscraped". <br>
   * detect minimum of filled values as "scraped"
   * 
   * @return isScraped
   */
  @Override
  public boolean isScraped() {
    return scraped || getHasMetadata();
  }

  /**
   * Gets the sort title.
   * 
   * @return the sort title
   */
  public String getSortTitle() {
    return sortTitle;
  }

  /**
   * Sets the sort title.
   * 
   * @param newValue
   *          the new sort title
   */
  public void setSortTitle(String newValue) {
    String oldValue = this.sortTitle;
    this.sortTitle = newValue;
    firePropertyChange(SORT_TITLE, oldValue, newValue);
  }

  /**
   * Returns the sortable variant of title<br>
   * eg "The Bourne Legacy" -> "Bourne Legacy, The".
   * 
   * @return the title in its sortable format
   */
  public String getTitleSortable() {
    if (StringUtils.isBlank(titleSortable)) {
      titleSortable = Utils.getSortableName(getTitle());
    }
    return titleSortable;
  }

  /**
   * Returns the sortable variant of the original title<br>
   * eg "The Bourne Legacy" -> "Bourne Legacy, The".
   *
   * @return the original title in its sortable format
   */
  public String getOriginalTitleSortable() {
    if (StringUtils.isBlank(originalTitleSortable)) {
      originalTitleSortable = Utils.getSortableName(getOriginalTitle());
    }
    return originalTitleSortable;
  }

  public void clearTitleSortable() {
    titleSortable = "";
    originalTitleSortable = "";
  }

  /**
   * Gets the checks for nfo file.
   * 
   * @return the checks for nfo file
   */
  public Boolean getHasNfoFile() {
    List<MediaFile> mf = getMediaFiles(MediaFileType.NFO);

    return mf != null && mf.size() > 0;
  }

  /**
   * doe we have basic metadata filled?<br>
   * like plot and year to take another fields into account always produces false positives (there are documentaries out there, which do not have
   * actors or either a producer in the meta data DBs..)
   * 
   * @return true/false
   */
  public Boolean getHasMetadata() {
    return !plot.isEmpty() && !(year == 0);
  }

  /**
   * Gets the check mark for images.<br>
   * Assumes true, but when PosterFilename is set and we do not have a poster, return false<br>
   * same for fanarts.
   * 
   * @return the checks for images
   */
  public Boolean getHasImages() {
    if (!MovieModuleManager.SETTINGS.getPosterFilenames().isEmpty() && StringUtils.isEmpty(getArtworkFilename(MediaFileType.POSTER))) {
      return false;
    }

    if (!MovieModuleManager.SETTINGS.getFanartFilenames().isEmpty() && StringUtils.isEmpty(getArtworkFilename(MediaFileType.FANART))) {
      return false;
    }

    return true;
  }

  /**
   * Gets the checks for trailer.
   * 
   * @return the checks for trailer
   */
  public Boolean getHasTrailer() {
    if (trailer != null && trailer.size() > 0) {
      return true;
    }

    // check if there is a mediafile (trailer)
    if (!getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
      return true;
    }

    return false;
  }

  /**
   * Gets the title for ui.
   * 
   * @return the title for ui
   */
  public String getTitleForUi() {
    String titleForUi = title;
    if (year > 0) {
      titleForUi += " (" + year + ")";
    }
    return titleForUi;
  }

  /**
   * Initialize after loading.
   */
  @Override
  public void initializeAfterLoading() {
    super.initializeAfterLoading();

    // remove empty tag and null values
    Utils.removeEmptyStringsFromList(tags);

    // link with movie set
    if (movieSetId != null) {
      movieSet = MovieList.getInstance().lookupMovieSet(movieSetId);
    }
  }

  /**
   * Gets the trailers
   * 
   * @return the trailers
   */
  public List<MovieTrailer> getTrailer() {
    return this.trailer;
  }

  /**
   * Adds the trailer.
   * 
   * @param obj
   *          the obj
   */
  public void addTrailer(MovieTrailer obj) {
    trailer.add(obj);
    firePropertyChange(TRAILER, null, trailer);
  }

  /**
   * Removes the all trailers.
   */
  public void removeAllTrailers() {
    trailer.clear();
    firePropertyChange(TRAILER, null, trailer);
  }

  /**
   * Adds the to tags.
   * 
   * @param newTag
   *          the new tag
   */
  public void addToTags(String newTag) {
    if (StringUtils.isBlank(newTag)) {
      return;
    }

    // do not accept duplicates
    if (tags.contains(newTag)) {
      return;
    }

    tags.add(newTag);
    firePropertyChange(TAG, null, newTag);
    firePropertyChange(TAGS_AS_STRING, null, newTag);
  }

  /**
   * Removes the from tags.
   * 
   * @param removeTag
   *          the remove tag
   */
  public void removeFromTags(String removeTag) {
    tags.remove(removeTag);
    firePropertyChange(TAG, null, removeTag);
    firePropertyChange(TAGS_AS_STRING, null, removeTag);
  }

  /**
   * Sets the tags.
   * 
   * @param newTags
   *          the new tags
   */
  @JsonSetter
  public void setTags(List<String> newTags) {
    // two way sync of tags
    ListUtils.mergeLists(tags, newTags);
    Utils.removeEmptyStringsFromList(tags);

    firePropertyChange(TAG, null, newTags);
    firePropertyChange(TAGS_AS_STRING, null, newTags);
  }

  /**
   * Gets the tag as string.
   * 
   * @return the tag as string
   */
  public String getTagsAsString() {
    StringBuilder sb = new StringBuilder();
    for (String tag : tags) {
      if (!StringUtils.isEmpty(sb)) {
        sb.append(", ");
      }
      sb.append(tag);
    }
    return sb.toString();
  }

  /**
   * Gets the tags.
   * 
   * @return the tags
   */
  public List<String> getTags() {
    return this.tags;
  }

  /**
   * Remove all Tags from List
   */
  public void removeAllTags() {
    tags.clear();
    firePropertyChange(TAG, null, tags);
    firePropertyChange(TAGS_AS_STRING, null, tags);

  }
  /** has movie local (or any mediafile inline) subtitles? */
  public boolean hasSubtitles() {
    if (this.subtitles) {
      return true; // local ones found
    }

    if (getMediaFiles(MediaFileType.SUBTITLE).size() > 0) {
      return true;
    }

    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      if (mf.hasSubtitles()) {
        return true;
      }
    }

    return false;
  }

  /** set subtitles */
  public void setSubtitles(boolean sub) {
    this.subtitles = sub;
  }

  /**
   * Gets the imdb id.
   * 
   * @return the imdb id
   */
  public String getImdbId() {
    return this.getIdAsString(IMDB);
  }

  /**
   * Gets the tmdb id.
   * 
   * @return the tmdb id
   */
  public int getTmdbId() {
    return this.getIdAsInt(TMDB);
  }

  /**
   * Sets the tmdb id.
   * 
   * @param newValue
   *          the new tmdb id
   */
  public void setTmdbId(int newValue) {
    this.setId(TMDB, newValue);
  }

  /**
   * Gets the runtime in minutes
   * 
   * @return the runtime
   */
  public int getRuntime() {
    int runtimeFromMi = getRuntimeFromMediaFilesInMinutes();
    if (MovieModuleManager.SETTINGS.isRuntimeFromMediaInfo() && runtimeFromMi > 0) {
      return runtimeFromMi;
    }
    return runtime == 0 ? runtimeFromMi : runtime;
  }

  /**
   * Gets the tagline.
   * 
   * @return the tagline
   */
  public String getTagline() {
    return tagline;
  }

  /**
   * Checks for file.
   * 
   * @param filename
   *          the filename
   * @return true, if successful
   */
  public boolean hasFile(String filename) {
    if (StringUtils.isEmpty(filename)) {
      return false;
    }

    for (MediaFile file : new ArrayList<>(getMediaFiles())) {
      if (filename.compareTo(file.getFilename()) == 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets the extra thumbs.
   * 
   * @return the extra thumbs
   */
  public List<String> getExtraThumbs() {
    return extraThumbs;
  }

  /**
   * Sets the extra thumbs.
   * 
   * @param extraThumbs
   *          the new extra thumbs
   */
  @JsonSetter
  public void setExtraThumbs(List<String> extraThumbs) {
    ListUtils.mergeLists(this.extraThumbs, extraThumbs);
  }

  /**
   * Gets the extra fanarts.
   * 
   * @return the extra fanarts
   */
  public List<String> getExtraFanarts() {
    return extraFanarts;
  }

  /**
   * Sets the extra fanarts.
   * 
   * @param extraFanarts
   *          the new extra fanarts
   */
  @JsonSetter
  public void setExtraFanarts(List<String> extraFanarts) {
    ListUtils.mergeLists(this.extraFanarts, extraFanarts);
  }

  /**
   * Sets the imdb id.
   * 
   * @param newValue
   *          the new imdb id
   */
  public void setImdbId(String newValue) {
    this.setId(IMDB, newValue);
  }

  /**
   * Sets the metadata.
   * 
   * @param metadata
   *          the new metadata
   * @param config
   *          the config
   */
  public void setMetadata(MediaMetadata metadata, MovieScraperMetadataConfig config) {
    if (metadata == null) {
      LOGGER.error("metadata was null");
      return;
    }

    // check if metadata has at least a name
    if (StringUtils.isEmpty(metadata.getTitle())) {
      LOGGER.warn("wanted to save empty metadata for " + getTitle());
      return;
    }

    setIds(metadata.getIds());

    // set chosen metadata
    if (config.isTitle()) {
      // Capitalize first letter of title if setting is set!
      if (MovieModuleManager.SETTINGS.getCapitalWordsInTitles()) {
        setTitle(WordUtils.capitalize(metadata.getTitle()));
      }
      else {
        setTitle(metadata.getTitle());
      }
    }

    if (config.isOriginalTitle()) {
      // Capitalize first letter of original title if setting is set!
      if (MovieModuleManager.SETTINGS.getCapitalWordsInTitles()) {
        setOriginalTitle(WordUtils.capitalize(metadata.getOriginalTitle()));
      }
      else {
        setOriginalTitle(metadata.getOriginalTitle());
      }
    }

    if (config.isTagline()) {
      setTagline(metadata.getTagline());
    }

    if (config.isPlot()) {
      setPlot(metadata.getPlot());
    }

    if (config.isYear()) {
      setYear(metadata.getYear());
      setReleaseDate(metadata.getReleaseDate());
    }

    if (config.isRating()) {
      clearRatings();
      for (MediaRating mediaRating : metadata.getRatings()) {
        setRating(new Rating(mediaRating));
      }
      setTop250(metadata.getTop250());
    }

    if (config.isRuntime()) {
      setRuntime(metadata.getRuntime());
    }

    setSpokenLanguages(StringUtils.join(metadata.getSpokenLanguages(), ", "));

    // certifications
    if (config.isCertification()) {
      if (metadata.getCertifications() != null && metadata.getCertifications().size() > 0) {
        setCertification(metadata.getCertifications().get(0));
      }
    }

    // country
    if (config.isCountry()) {
      setCountry(StringUtils.join(metadata.getCountries(), ", "));
    }

    // studio
    if (config.isStudio()) {
      setProductionCompany(StringUtils.join(metadata.getProductionCompanies(), ", "));
    }

    // cast
    if (config.isCast()) {

      List<Person> actors = new ArrayList<>();
      List<Person> producers = new ArrayList<>();
      List<Person> directors = new ArrayList<>();
      List<Person> writers = new ArrayList<>();

      for (MediaCastMember member : metadata.getCastMembers()) {
        switch (member.getType()) {
          case ACTOR:
            actors.add(new Person(member));
            break;

          case DIRECTOR:
            directors.add(new Person(member));
            break;

          case WRITER:
            writers.add(new Person(member));
            break;

          case PRODUCER:
            producers.add(new Person(member));
            break;

          default:
            break;
        }
      }
      setActors(actors);
      setDirectors(directors);
      setWriters(writers);
      setProducers(producers);
    }

    // genres
    if (config.isGenres()) {
      setGenres(metadata.getGenres());
    }

    // tags
    if (config.isTags()) {
      for (String tag : metadata.getTags()) {
        addToTags(tag);
      }
    }

    // set scraped
    setScraped(true);

    // create MovieSet
    if (config.isCollection()) {
      int col = 0;
      try {
        col = (int) metadata.getId(MediaMetadata.TMDB_SET);
      }
      catch (Exception ignored) {
      }
      if (col != 0) {
        MovieSet movieSet = MovieList.getInstance().getMovieSet(metadata.getCollectionName(), col);
        if (movieSet != null && movieSet.getTmdbId() == 0) {
          movieSet.setTmdbId(col);
          // get movieset metadata
          try {
            List<MediaScraper> sets = MediaScraper.getMediaScrapers(ScraperType.MOVIE_SET);
            if (sets != null && sets.size() > 0) {
              MediaScraper first = sets.get(0); // just get first
              IMovieSetMetadataProvider mp = ((IMovieSetMetadataProvider) first.getMediaProvider());
              MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE_SET);
              options.setTmdbId(col);
              options.setLanguage(MovieModuleManager.SETTINGS.getScraperLanguage().toLocale());
              options.setCountry(MovieModuleManager.SETTINGS.getCertificationCountry());

              MediaMetadata info = mp.getMetadata(options);
              if (info != null && StringUtils.isNotBlank(info.getTitle())) {
                movieSet.setTitle(info.getTitle());
                movieSet.setPlot(info.getPlot());

                if (!info.getMediaArt(MediaArtworkType.POSTER).isEmpty()) {
                  movieSet.setArtworkUrl(info.getMediaArt(MediaArtworkType.POSTER).get(0).getDefaultUrl(), MediaFileType.POSTER);
                }
                if (!info.getMediaArt(MediaArtworkType.BACKGROUND).isEmpty()) {
                  movieSet.setArtworkUrl(info.getMediaArt(MediaArtworkType.BACKGROUND).get(0).getDefaultUrl(), MediaFileType.FANART);
                }
              }
            }
          }
          catch (ScrapeException e) {
            LOGGER.error("getMovieSet", e);
            MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, this, "message.scrape.metadatamoviesetfailed",
                new String[] { ":", e.getLocalizedMessage() }));
          }
          catch (MissingIdException | UnsupportedMediaTypeException | NothingFoundException ignored) {
          }
        }

        // add movie to movieset
        if (movieSet != null) {
          // first remove from "old" movieset
          setMovieSet(null);

          // add to new movieset
          // movieSet.addMovie(this);
          setMovieSet(movieSet);
          movieSet.insertMovie(this);
          movieSet.saveToDb();
        }
      }
    }

    // update DB
    writeNFO();
    saveToDb();

    // rename the movie if that has been chosen in the settings
    if (MovieModuleManager.SETTINGS.isRenameAfterScrape()) {
      MovieRenamer.renameMovie(this);
    }

    // write actor images after possible rename (to have a good folder structure)
    if (config.isCast()) {
      writeActorImages();
    }
  }

  /**
   * Sets the trailers; first one is "inNFO" if not a local one.
   * 
   * @param trailers
   *          the new trailers
   */
  @JsonSetter
  public void setTrailers(List<MovieTrailer> trailers) {
    MovieTrailer preferredTrailer = null;
    removeAllTrailers();

    // set preferred trailer
    if (MovieModuleManager.SETTINGS.isUseTrailerPreference()) {
      MovieTrailerQuality desiredQuality = MovieModuleManager.SETTINGS.getTrailerQuality();
      MovieTrailerSources desiredSource = MovieModuleManager.SETTINGS.getTrailerSource();

      // search for quality and provider
      for (MovieTrailer trailer : trailers) {
        if (desiredQuality.containsQuality(trailer.getQuality()) && desiredSource.containsSource(trailer.getProvider())) {
          trailer.setInNfo(Boolean.TRUE);
          preferredTrailer = trailer;
          break;
        }
      }

      // search for quality
      if (preferredTrailer == null) {
        for (MovieTrailer trailer : trailers) {
          if (desiredQuality.containsQuality(trailer.getQuality())) {
            trailer.setInNfo(Boolean.TRUE);
            preferredTrailer = trailer;
            break;
          }
        }
      }

      // if not yet one has been found; sort by quality descending and take the first one which is lower or equal to the desired quality
      if (preferredTrailer == null) {
        List<MovieTrailer> sortedTrailers = new ArrayList<>(trailers);
        sortedTrailers.sort(TRAILER_QUALITY_COMPARATOR);
        for (MovieTrailer trailer : sortedTrailers) {
          if (desiredQuality.ordinal() >= MovieTrailerQuality.getMovieTrailerQuality(trailer.getQuality()).ordinal()) {
            trailer.setInNfo(Boolean.TRUE);
            preferredTrailer = trailer;
            break;
          }
        }
      }
    } // end if MovieModuleManager.SETTINGS.isUseTrailerPreference()

    // if not yet one has been found; sort by quality descending and take the first one
    if (preferredTrailer == null && !trailers.isEmpty()) {
      List<MovieTrailer> sortedTrailers = new ArrayList<>(trailers);
      sortedTrailers.sort(TRAILER_QUALITY_COMPARATOR);
      preferredTrailer = sortedTrailers.get(0);
      preferredTrailer.setInNfo(Boolean.TRUE);
    }

    // add trailers
    if (preferredTrailer != null) {
      addTrailer(preferredTrailer);
    }
    for (MovieTrailer trailer : trailers) {
      // preferred trailer has already been added
      if (preferredTrailer != null && preferredTrailer == trailer) {
        continue;
      }

      // if still no preferred trailer has been set, then mark the first one
      if (preferredTrailer == null && this.trailer.size() == 0 && !trailer.getUrl().startsWith("file")) {
        trailer.setInNfo(Boolean.TRUE);
      }

      addTrailer(trailer);
    }
  }

  /**
   * Sets the artwork.
   * 
   * @param md
   *          the md
   * @param config
   *          the config
   */
  public void setArtwork(MediaMetadata md, MovieScraperMetadataConfig config) {
    setArtwork(md.getMediaArt(MediaArtworkType.ALL), config);
  }

  /**
   * Sets the artwork.
   * 
   * @param artwork
   *          the artwork
   * @param config
   *          the config
   */
  public void setArtwork(List<MediaArtwork> artwork, MovieScraperMetadataConfig config) {
    if (config.isArtwork()) {
      MovieArtworkHelper.setArtwork(this, artwork);
    }
  }

  @Override
  public void setTitle(String newValue) {
    String oldValue = this.title;
    super.setTitle(newValue);

    firePropertyChange(TITLE_FOR_UI, oldValue, newValue);

    oldValue = this.titleSortable;
    titleSortable = "";
    firePropertyChange(TITLE_SORTABLE, oldValue, titleSortable);
  }

  @Override
  public void setOriginalTitle(String newValue) {
    String oldValue = this.originalTitle;
    super.setOriginalTitle(newValue);

    firePropertyChange(TITLE_FOR_UI, oldValue, newValue);

    oldValue = this.originalTitleSortable;
    originalTitleSortable = "";
    firePropertyChange(TITLE_SORTABLE, oldValue, originalTitleSortable);
  }

  /**
   * Sets the runtime in minutes
   * 
   * @param newValue
   *          the new runtime
   */
  public void setRuntime(int newValue) {
    int oldValue = this.runtime;
    this.runtime = newValue;
    firePropertyChange(RUNTIME, oldValue, newValue);
  }

  /**
   * Sets the tagline.
   * 
   * @param newValue
   *          the new tagline
   */
  public void setTagline(String newValue) {
    String oldValue = this.tagline;
    this.tagline = newValue;
    firePropertyChange("tagline", oldValue, newValue);
  }

  /**
   * Sets the year.
   * 
   * @param newValue
   *          the new year
   */
  @Override
  public void setYear(int newValue) {
    int oldValue = year;
    super.setYear(newValue);

    firePropertyChange(TITLE_FOR_UI, oldValue, newValue);
  }

  /**
   * all XBMC supported NFO names. (without path!)
   * 
   * @param nfo
   *          the nfo
   * @return the nfo filename
   */
  public String getNfoFilename(MovieNfoNaming nfo) {
    List<MediaFile> mfs = getMediaFiles(MediaFileType.VIDEO);
    if (mfs != null && mfs.size() > 0) {
      String name = mfs.get(0).getFilename();
      if (isStacked()) {
        // when movie IS stacked, remove stacking marker, else keep it!
        name = Utils.cleanStackingMarkers(name);
      }
      return getNfoFilename(nfo, name);
    }
    else {
      return getNfoFilename(nfo, ""); // no video files
    }
  }

  /**
   * all XBMC supported NFO names. (without path!)
   * 
   * @param nfo
   *          the nfo filenaming
   * @param newMovieFilename
   *          the new/desired movie filename (stacking marker should already be set correct here!)
   * @return the nfo filename
   */
  public String getNfoFilename(MovieNfoNaming nfo, String newMovieFilename) {
    String filename = "";
    switch (nfo) {
      case FILENAME_NFO:
        if (isDisc()) {
          // if filename is activated, we generate them accordingly MF(1)
          // but if disc, fixate this
          if (Files.exists(getPathNIO().resolve("VIDEO_TS.ifo")) || Files.exists(getPathNIO().resolve("VIDEO_TS"))) {
            filename = "VIDEO_TS.nfo";
          }
          else if (Files.exists(getPathNIO().resolve("HVDVD_TS.ifo")) || Files.exists(getPathNIO().resolve("HVDVD_TS"))) {
            filename = "HVDVD_TS.nfo";
          }
          else if (Files.exists(getPathNIO().resolve("index.bdmv"))) {
            filename = "index.nfo";
          }
          else if (Files.exists(getPathNIO().resolve("BDMV"))) {
            filename = "BDMV.nfo";
          }
        }
        else {
          String movieFilename = FilenameUtils.getBaseName(newMovieFilename);
          filename += movieFilename + ".nfo";
        }
        break;
      case MOVIE_NFO:
        filename += "movie.nfo";
        break;
      case DISC_NFO:
        if (isDisc()) {
          Path dir = getPathNIO().resolve("VIDEO_TS");
          if (Files.isDirectory(dir)) {
            filename = dir.resolve("VIDEO_TS.nfo").toString();
          }
          dir = getPathNIO().resolve("HVDVD_TS");
          if (Files.isDirectory(dir)) {
            filename = dir.resolve("HVDVD_TS.nfo").toString();
          }
          dir = getPathNIO().resolve("BDMV");
          if (Files.isDirectory(dir)) {
            filename = dir.resolve("index.nfo").toString();
          }
        }
        break;
      default:
        filename = "";
        break;
    }
    // LOGGER.trace("getNfoFilename: '" + newMovieFilename + "' / " + nfo + " -> '" + filename + "'");
    return filename;
  }

  /**
   * all supported TRAILER names. (without path!)
   * 
   * @param trailer
   *          trailer naming enum
   * @return the associated trailer filename
   */
  public String getTrailerFilename(MovieTrailerNaming trailer) {
    List<MediaFile> mfs = getMediaFiles(MediaFileType.VIDEO);
    if (mfs != null && mfs.size() > 0) {
      String name = mfs.get(0).getFilename();
      if (isStacked()) {
        // when movie IS stacked, remove stacking marker, else keep it!
        name = Utils.cleanStackingMarkers(name);
      }
      return getTrailerFilename(trailer, name);
    }
    else {
      return getTrailerFilename(trailer, ""); // no video files
    }
  }

  /**
   * all supported TRAILE names. (without path!)
   * 
   * @param trailer
   *          trailer naming enum
   * @param newMovieFilename
   *          the new/desired movie filename (stacking marker should already be set correct here!)
   * @return the associated trailer filename <b>(WITHOUT EXTENSION!!!!)</b>
   */
  public String getTrailerFilename(MovieTrailerNaming trailer, String newMovieFilename) {
    String filename = "";
    switch (trailer) {
      case FILENAME_TRAILER:
        if (isDisc()) {
          filename += "movie-trailer"; // TODO: assume, or same naming as NFO?
        }
        else {
          String movieFilename = FilenameUtils.getBaseName(newMovieFilename);
          filename = movieFilename + "-trailer";
        }
        break;
      case MOVIE_TRAILER:
        filename += "movie-trailer";
        break;
      default:
        filename = "";
        break;
    }
    return filename;
  }

  /**
   * get trailer name (w/o extension)<br>
   * &lt;moviefile&gt;-trailer.ext
   * 
   * @return the trailer basename
   */
  public String getTrailerBasename() {
    List<MediaFile> mfs = getMediaFiles(MediaFileType.VIDEO);
    if (mfs != null && mfs.size() > 0) {
      return FilenameUtils.getBaseName(Utils.cleanStackingMarkers(mfs.get(0).getFilename()));
    }
    return null;
  }

  /**
   * download the specified type of artwork for this movie
   *
   * @param type
   *          the chosen artwork type to be downloaded
   */
  public void downloadArtwork(MediaFileType type) {
    MovieArtworkHelper.downloadArtwork(this, type);
  }

  /**
   * Write actor images.
   */
  public void writeActorImages() {
    // check if actor images shall be written
    if (!MovieModuleManager.SETTINGS.isWriteActorImages() || isMultiMovieDir()) {
      return;
    }

    MovieActorImageFetcherTask task = new MovieActorImageFetcherTask(this);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
  }

  /**
   * Write nfo.
   */
  public void writeNFO() {
    if (MovieModuleManager.SETTINGS.getNfoFilenames().isEmpty()) {
      LOGGER.info("Not writing any NFO file, because NFO filename preferences were empty...");
      return;
    }

    IMovieConnector connector = null;

    switch (MovieModuleManager.SETTINGS.getMovieConnector()) {
      case MP:
        connector = new MovieToMediaportalConnector(this);
        break;

      case KODI:
        connector = new MovieToKodiConnector(this);
        break;

      case XBMC:
      default:
        connector = new MovieToXbmcConnector(this);
    }

    if (connector != null) {
      List<MovieNfoNaming> naming = MovieModuleManager.SETTINGS.getNfoFilenames();
      if (isMultiMovieDir()) {
        // fixate
        naming = new ArrayList<>();
        naming.add(MovieNfoNaming.FILENAME_NFO);
      }
      connector.write(naming);
      firePropertyChange(HAS_NFO_FILE, false, true);
    }
  }

  /**
   * Gets the genres.
   * 
   * @return the genres
   */
  public List<MediaGenres> getGenres() {
    return genres;
  }

  /**
   * Adds the genre.
   * 
   * @param newValue
   *          the new value
   */
  public void addGenre(MediaGenres newValue) {
    if (!genres.contains(newValue)) {
      genres.add(newValue);
      firePropertyChange(GENRE, null, newValue);
      firePropertyChange(GENRES_AS_STRING, null, newValue);
    }
  }

  /**
   * Sets the genres.
   * 
   * @param newGenres
   *          the new genres
   */
  @JsonSetter
  public void setGenres(List<MediaGenres> newGenres) {
    // two way sync of genres
    ListUtils.mergeLists(genres, newGenres);

    firePropertyChange(GENRE, null, genres);
    firePropertyChange(GENRES_AS_STRING, null, genres);
  }

  /**
   * Removes the genre.
   * 
   * @param genre
   *          the genre
   */
  public void removeGenre(MediaGenres genre) {
    if (genres.contains(genre)) {
      genres.remove(genre);
      firePropertyChange(GENRE, null, genre);
      firePropertyChange(GENRES_AS_STRING, null, genre);
    }
  }

  /**
   * Remove all genres from list
   */
  public void removeAllGenres() {
    genres.clear();
    firePropertyChange(GENRE,null, genres);
    firePropertyChange(GENRES_AS_STRING, null, genres);
  }

  /**
   * Gets the certifications.
   * 
   * @return the certifications
   */
  public Certification getCertification() {
    return certification;
  }

  /**
   * Sets the certifications.
   * 
   * @param newValue
   *          the new certifications
   */
  public void setCertification(Certification newValue) {
    this.certification = newValue;
    firePropertyChange(CERTIFICATION, null, newValue);
  }

  /**
   * get the "main" rating
   *
   * @return the main (preferred) rating
   */
  @Override
  public Rating getRating() {
    Rating rating = null;

    // the user rating
    if (MovieModuleManager.SETTINGS.getPreferPersonalRating()) {
      rating = ratings.get(Rating.USER);
    }

    // the default rating
    if (rating == null) {
      rating = ratings.get(MovieModuleManager.SETTINGS.getPreferredRating());
    }

    // then the default one (either NFO or DEFAULT)
    if (rating == null) {
      rating = ratings.get(Rating.NFO);
    }
    if (rating == null) {
      rating = ratings.get(Rating.DEFAULT);
    }

    // is there any rating?
    if (rating == null && !ratings.isEmpty()) {
      for (Rating r : ratings.values()) {
        rating = r;
        break;
      }
    }

    // last but not least a non null value
    if (rating == null) {
      rating = new Rating();
    }

    return rating;
  }

  /**
   * Gets the checks for rating.
   * 
   * @return the checks for rating
   */
  public boolean getHasRating() {
    return !ratings.isEmpty() || scraped;
  }

  /**
   * Gets the genres as string.
   * 
   * @return the genres as string
   */
  public String getGenresAsString() {
    StringBuilder sb = new StringBuilder();
    for (MediaGenres genre : genres) {
      if (!StringUtils.isEmpty(sb)) {
        sb.append(", ");
      }
      sb.append(genre != null ? genre.getLocalizedName() : "null");
    }
    return sb.toString();
  }

  /**
   * Checks if is watched.
   * 
   * @return true, if is watched
   */
  public boolean isWatched() {
    return watched;
  }

  /**
   * Sets the watched.
   * 
   * @param newValue
   *          the new watched
   */
  public void setWatched(boolean newValue) {
    boolean oldValue = this.watched;
    this.watched = newValue;
    firePropertyChange(WATCHED, oldValue, newValue);
  }

  /**
   * Checks if this movie is in a folder with other movies and not in an own folder<br>
   * so disable everything except renaming
   * 
   * @return true, if in datasource root
   */
  public boolean isMultiMovieDir() {
    return multiMovieDir;
  }

  /**
   * Sets the flag, that the movie is not in an own folder<br>
   * so disable everything except renaming
   * 
   * @param multiDir
   *          true/false
   */
  public void setMultiMovieDir(boolean multiDir) {
    this.multiMovieDir = multiDir;
  }

  /**
   * <p>
   * Uses <code>ReflectionToStringBuilder</code> to generate a <code>toString</code> for the specified object.
   * </p>
   * 
   * @return the String result
   * @see ReflectionToStringBuilder#toString(Object)
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  /**
   * Gets the movie set.
   * 
   * @return the movieset
   */
  public MovieSet getMovieSet() {
    return movieSet;
  }

  /**
   * Sets the movie set.
   * 
   * @param newValue
   *          the new movie set
   */
  public void setMovieSet(MovieSet newValue) {
    MovieSet oldValue = this.movieSet;
    this.movieSet = newValue;

    if (newValue == null) {
      movieSetId = null;
    }
    else {
      movieSetId = newValue.getDbId();
    }

    firePropertyChange(MOVIESET, oldValue, newValue);
    firePropertyChange(MOVIESET_TITLE, oldValue, newValue);
  }

  public void movieSetTitleChanged() {
    firePropertyChange(MOVIESET_TITLE, null, "");
  }

  public String getMovieSetTitle() {
    if (movieSet != null) {
      return movieSet.getTitle();
    }
    return "";
  }

  /**
   * Removes the from movie set.
   */
  public void removeFromMovieSet() {
    if (movieSet != null) {
      movieSet.removeMovie(this, true);
    }
    setMovieSet(null);
  }

  /**
   * is this a disc movie folder (video_ts / bdmv)?.
   * 
   * @return true, if is disc
   */
  public boolean isDisc() {
    return isDisc;
  }

  /**
   * is this a disc movie folder (video_ts / bdmv)?.
   * 
   * @param isDisc
   *          the new disc
   */
  public void setDisc(boolean isDisc) {
    this.isDisc = isDisc;
  }

  public int getMediaInfoVideoBitrate() {
    List<MediaFile> videos = getMediaFiles(MediaFileType.VIDEO);
    if (videos.size() > 0) {
      MediaFile mediaFile = videos.get(0);
      return mediaFile.getOverallBitRate();
    }

    return 0;
  }

  /**
   * Gets the media info audio codec (i.e mp3) and channels (i.e. 6 at 5.1 sound)
   */
  public String getMediaInfoAudioCodecAndChannels() {
    List<MediaFile> videos = getMediaFiles(MediaFileType.VIDEO);
    if (videos.size() > 0) {
      MediaFile mediaFile = videos.get(0);
      return mediaFile.getAudioCodec() + "_" + mediaFile.getAudioChannels();
    }

    return "";
  }

  public void setSpokenLanguages(String newValue) {
    String oldValue = this.spokenLanguages;
    this.spokenLanguages = newValue;
    firePropertyChange(SPOKEN_LANGUAGES, oldValue, newValue);
  }

  public String getSpokenLanguages() {
    return this.spokenLanguages;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String newValue) {
    String oldValue = this.country;
    this.country = newValue;
    firePropertyChange(COUNTRY, oldValue, newValue);
  }

  public MediaSource getMediaSource() {
    return mediaSource;
  }

  public void setMediaSource(MediaSource newValue) {
    MediaSource oldValue = this.mediaSource;
    this.mediaSource = newValue;
    firePropertyChange(MEDIA_SOURCE, oldValue, newValue);
  }

  /**
   * Gets the images to cache.
   */
  public List<Path> getImagesToCache() {
    // image files
    List<Path> filesToCache = new ArrayList<>();
    for (MediaFile mf : getMediaFiles()) {
      if (mf.isGraphic()) {
        filesToCache.add(mf.getFileAsPath());
      }
    }

    // getting all scraped actors (= possible to cache)
    // and having never ever downloaded any pic is quite slow.
    // (Many invalid cache requests and exists() checks)
    // Better get a listing of existent actor images directly!
    if (!isMultiMovieDir()) {
      // and only for normal movies - MMD should not have .actors folder!
      filesToCache.addAll(listActorFiles());
    } // check against actors and trigger a download? - NO, only via scrape/missingImagesTask

    return filesToCache;
  }

  /**
   * @return list of actor images on filesystem
   */
  private List<Path> listActorFiles() {
    List<Path> fileNames = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(getPathNIO().resolve(Person.ACTOR_DIR))) {
      for (Path path : directoryStream) {
        if (Utils.isRegularFile(path)) {
          // only get graphics
          MediaFile mf = new MediaFile(path);
          if (mf.isGraphic()) {
            fileNames.add(path.toAbsolutePath());
          }
        }
      }
    }
    catch (IOException e) {
      LOGGER.warn("Cannot get actors: " + getPathNIO().resolve(Person.ACTOR_DIR));
    }
    return fileNames;
  }

  public List<MediaFile> getMediaFilesContainingAudioStreams() {
    List<MediaFile> mediaFilesWithAudioStreams = new ArrayList<>(1);

    // get the audio streams from the first video file
    List<MediaFile> videoFiles = getMediaFiles(MediaFileType.VIDEO);
    if (videoFiles.size() > 0) {
      MediaFile videoFile = videoFiles.get(0);
      mediaFilesWithAudioStreams.add(videoFile);
    }

    // get all extra audio streams
    mediaFilesWithAudioStreams.addAll(getMediaFiles(MediaFileType.AUDIO));

    return mediaFilesWithAudioStreams;
  }

  public List<MediaFile> getMediaFilesContainingSubtitles() {
    List<MediaFile> mediaFilesWithSubtitles = new ArrayList<>(1);

    for (MediaFile mediaFile : getMediaFiles(MediaFileType.VIDEO, MediaFileType.SUBTITLE)) {
      if (mediaFile.hasSubtitles()) {
        mediaFilesWithSubtitles.add(mediaFile);
      }
    }

    return mediaFilesWithSubtitles;
  }

  public int getRuntimeFromMediaFiles() {
    int runtime = 0;
    for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO)) {
      runtime += mf.getDuration();
    }
    return runtime;
  }

  public int getRuntimeFromMediaFilesInMinutes() {
    return getRuntimeFromMediaFiles() / 60;
  }

  public Date getReleaseDate() {
    return releaseDate;
  }

  @JsonIgnore
  public void setReleaseDate(Date newValue) {
    Date oldValue = this.releaseDate;
    this.releaseDate = newValue;
    firePropertyChange(RELEASE_DATE, oldValue, newValue);
    firePropertyChange(RELEASE_DATE_AS_STRING, oldValue, newValue);
  }

  /**
   * release date as yyyy-mm-dd<br>
   * https://xkcd.com/1179/ :P
   */
  public String getReleaseDateFormatted() {
    if (this.releaseDate == null) {
      return "";
    }
    return new SimpleDateFormat("yyyy-MM-dd").format(this.releaseDate);
  }

  /**
   * Gets the first aired as a string, formatted in the system locale.
   */
  public String getReleaseDateAsString() {
    if (this.releaseDate == null) {
      return "";
    }
    return TmmDateFormat.SHORT_DATE_FORMAT.format(releaseDate);
  }

  /**
   * convenient method to set the release date (parsed from string).
   */
  public void setReleaseDate(String dateAsString) {
    try {
      setReleaseDate(StrgUtils.parseDate(dateAsString));
    }
    catch (ParseException ignored) {
    }
  }

  public Date getLastWatched() {
    return lastWatched;
  }

  public void setLastWatched(Date lastWatched) {
    this.lastWatched = lastWatched;
  }

  @Override
  public void saveToDb() {
    // update/insert this movie to the database
    MovieList.getInstance().persistMovie(this);
  }

  @Override
  public void deleteFromDb() {
    // remove this movie from the database
    MovieList.getInstance().removeMovieFromDb(this);
  }

  @Override
  public synchronized void callbackForWrittenArtwork(MediaArtworkType type) {
    if (MovieModuleManager.SETTINGS.getMovieConnector() == MovieConnectors.MP) {
      writeNFO();
    }
  }

  /**
   * get all video files for that movie
   *
   * @return a list of all video files
   */
  public List<MediaFile> getVideoFiles() {
    return getMediaFiles(MediaFileType.VIDEO);
  }

  /**
   * gets the basename (without stacking)
   * 
   * @return the video base name (without stacking)
   */
  public String getVideoBasenameWithoutStacking() {
    MediaFile mf = getMediaFiles(MediaFileType.VIDEO).get(0);
    return FilenameUtils.getBaseName(mf.getFilenameWithoutStacking());
  }

  public int getTop250() {
    return top250;
  }

  public void setVideoIn3D(boolean newValue) {
    boolean oldValue = this.videoIn3D;
    this.videoIn3D = newValue;
    firePropertyChange(VIDEO_IN_3D, oldValue, newValue);
  }

  public boolean isVideoIn3D() {
    String video3DFormat = "";
    List<MediaFile> videos = getMediaFiles(MediaFileType.VIDEO);
    if (videos.size() > 0) {
      MediaFile mediaFile = videos.get(0);
      video3DFormat = mediaFile.getVideo3DFormat();
    }

    return videoIn3D || StringUtils.isNotBlank(video3DFormat);
  }

  public void setTop250(int newValue) {
    int oldValue = this.top250;
    this.top250 = newValue;
    firePropertyChange(TOP250, oldValue, newValue);
  }

  /**
   * add an actor
   *
   * @param actor
   *          the actor to be added
   */
  public void addActor(Person actor) {
    if (actor.getType() != Person.Type.ACTOR) {
      return;
    }

    actors.add(actor);
    firePropertyChange(ACTORS, null, this.getActors());
  }

  /**
   * remove the given actor.
   *
   * @param actor
   *          the actor to be removed
   */
  public void removeActor(Person actor) {
    actors.remove(actor);
    firePropertyChange(ACTORS, null, this.getActors());
  }

  /**
   * set the actors. This will do a two way sync of the list to be added and the given list, to do not re-add existing actors (better for binding)
   *
   * @param newActors
   *          the new actors to be set
   */
  @JsonSetter
  public void setActors(List<Person> newActors) {
    // two way sync of actors
    ListUtils.mergeLists(actors, newActors);
    firePropertyChange(ACTORS, null, this.getActors());
  }

  /**
   * get the actors
   *
   * @return the actors
   */
  public List<Person> getActors() {
    return this.actors;
  }

  /**
   * add a producer
   *
   * @param producer
   *          the producer to be added
   */
  public void addProducer(Person producer) {
    if (producer.getType() != Person.Type.PRODUCER) {
      return;
    }

    producers.add(producer);
    firePropertyChange(PRODUCERS, null, producers);
  }

  /**
   * remove the given producer
   *
   * @param producer
   *          the producer to be removed
   */
  public void removeProducer(Person producer) {
    producers.remove(producer);
    firePropertyChange(PRODUCERS, null, producers);
  }

  /**
   * set the producers. This will do a two way sync of the list to be added and the given list, to do not re-add existing producers (better for
   * binding)
   *
   * @param newProducers
   *          the new producers to be set
   */
  @JsonSetter
  public void setProducers(List<Person> newProducers) {
    // two way sync of producers
    ListUtils.mergeLists(producers, newProducers);
    firePropertyChange(PRODUCERS, null, producers);
  }

  /**
   * get the producers
   *
   * @return the producers
   */
  public List<Person> getProducers() {
    return this.producers;
  }

  /**
   * add a director
   *
   * @param director
   *          the director to be added
   */
  public void addDirector(Person director) {
    if (director.getType() != Person.Type.DIRECTOR) {
      return;
    }

    directors.add(director);
    firePropertyChange(DIRECTORS, null, this.getDirectors());
    firePropertyChange(DIRECTORS_AS_STRING, null, this.getDirectorsAsString());
  }

  /**
   * remove the given director.
   *
   * @param director
   *          the director to be removed
   */
  public void removeDirector(Person director) {
    directors.remove(director);
    firePropertyChange(DIRECTORS, null, this.getDirectors());
    firePropertyChange(DIRECTORS_AS_STRING, null, this.getDirectorsAsString());
  }

  /**
   * Sets the directors.
   *
   * @param newDirectors
   *          the new directors
   */
  @JsonSetter
  public void setDirectors(List<Person> newDirectors) {
    // two way sync of directors
    ListUtils.mergeLists(directors, newDirectors);

    firePropertyChange(DIRECTORS, null, this.getDirectors());
    firePropertyChange(DIRECTORS_AS_STRING, null, this.getDirectorsAsString());
  }

  /**
   * get the directors.
   *
   * @return the directors
   */
  public List<Person> getDirectors() {
    return directors;
  }

  /**
   * get the directors as string
   *
   * @return a string containing all directors; separated by ,
   */
  public String getDirectorsAsString() {
    List<String> directorNames = new ArrayList<>();
    for (Person director : directors) {
      directorNames.add(director.getName());
    }
    return StringUtils.join(directorNames, ", ");
  }

  /**
   * add a writer
   *
   * @param writer
   *          the writer to be added
   */
  public void addWriter(Person writer) {
    if (writer.getType() != Person.Type.WRITER) {
      return;
    }

    writers.add(writer);
    firePropertyChange(WRITERS, null, this.getWriters());
    firePropertyChange(WRITERS_AS_STRING, null, this.getWritersAsString());
  }

  /**
   * remove the given writer.
   *
   * @param writer
   *          the writer to be removed
   */
  public void removeWriter(Person writer) {
    writers.remove(writer);
    firePropertyChange(WRITERS, null, this.getWriters());
    firePropertyChange(WRITERS_AS_STRING, null, this.getWritersAsString());
  }

  /**
   * Sets the writers.
   *
   * @param newWriters
   *          the new writers
   */
  @JsonSetter
  public void setWriters(List<Person> newWriters) {
    // two way sync of writers
    ListUtils.mergeLists(writers, newWriters);

    firePropertyChange(WRITERS, null, this.getWriters());
    firePropertyChange(WRITERS_AS_STRING, null, this.getWritersAsString());
  }

  /**
   * Gets the writers.
   *
   * @return the writers
   */
  public List<Person> getWriters() {
    return writers;
  }

  /**
   * get the writers as string
   *
   * @return a string containing all writers; separated by ,
   */
  public String getWritersAsString() {
    List<String> writerNames = new ArrayList<>();
    for (Person writer : writers) {
      writerNames.add(writer.getName());
    }
    return StringUtils.join(writerNames, ", ");
  }

  /**
   * Is the movie "stacked" (more than one video file)
   * 
   * @return true if the movie is stacked; false otherwise
   */
  public boolean isStacked() {
    return stacked;
  }

  public void setStacked(boolean stacked) {
    this.stacked = stacked;
  }

  /**
   * ok, we might have detected some stacking MFs.<br>
   * But if we only have ONE video file, reset stacking markers in this case<br>
   * eg: "Harry Potter 7 - Part 1" is not stacked<br>
   * CornerCase: what if HP7 has more files...?
   */
  public void reEvaluateStacking() {
    List<MediaFile> mfs = getMediaFiles(MediaFileType.VIDEO);
    if (mfs.size() > 1 && !isDisc()) {
      // ok, more video files means stacking (if not a disc folder)
      this.setStacked(true);
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO, MediaFileType.SUBTITLE)) {
        mf.detectStackingInformation();
      }
    }
    else {
      // only ONE video? remove any stacking markers from MFs
      this.setStacked(false);
      for (MediaFile mf : getMediaFiles(MediaFileType.VIDEO, MediaFileType.AUDIO, MediaFileType.SUBTITLE)) {
        mf.removeStackingInformation();
      }
    }
  }

  /**
   * <b>PHYSICALLY</b> deletes a complete Movie by moving it to datasource backup folder<br>
   * DS\.backup\&lt;moviename&gt;
   */
  public boolean deleteFilesSafely() {
    // backup
    if (isMultiMovieDir()) {
      boolean ok = true;
      for (MediaFile mf : getMediaFiles()) {
        if (!mf.deleteSafely(getDataSource())) {
          ok = false;
        }
      }
      return ok;
    }
    else {
      return Utils.deleteDirectorySafely(getPathNIO(), getDataSource());
    }
  }

  @Override
  public MediaFile getMainVideoFile() {
    MediaFile vid = getBiggestMediaFile(MediaFileType.VIDEO);
    if (vid != null) {
      return vid;
    }
    // cannot happen - movie MUST always have a video file
    return new MediaFile();
  }

  @Override
  public String getMediaInfoVideoResolution() {
    return getMainVideoFile().getVideoResolution();
  }

  @Override
  public String getMediaInfoVideoFormat() {
    return getMainVideoFile().getVideoFormat();
  }

  @Override
  public String getMediaInfoVideoCodec() {
    return getMainVideoFile().getVideoCodec();
  }

  @Override
  public double getMediaInfoFrameRate() {
    return getMainVideoFile().getFrameRate();
  }

  @Override
  public float getMediaInfoAspectRatio() {
    return getMainVideoFile().getAspectRatio();
  }

  @Override
  public String getMediaInfoAudioCodec() {
    return getMainVideoFile().getAudioCodec();
  }

  @Override
  public List<String> getMediaInfoAudioCodecList() {
    return getMainVideoFile().getAudioCodecList();
  }

  @Override
  public String getMediaInfoAudioChannels() {
    return getMainVideoFile().getAudioChannels();
  }

  @Override
  public List<String> getMediaInfoAudioChannelList() {
    return getMainVideoFile().getAudioChannelsList();
  }

  @Override
  public String getMediaInfoAudioLanguage() {
    return getMainVideoFile().getAudioLanguage();
  }

  @Override
  public List<String> getMediaInfoAudioLanguageList() {
    return getMainVideoFile().getAudioLanguagesList();
  }

  @Override
  public String getMediaInfoContainerFormat() {
    return getMainVideoFile().getContainerFormat();
  }

  @Override
  public MediaSource getMediaInfoSource() {
    return getMediaSource();
  }

  public String getVideo3DFormat() {
    MediaFile mediaFile = getMainVideoFile();
    if (StringUtils.isNotBlank(mediaFile.getVideo3DFormat())) {
      return mediaFile.getVideo3DFormat();
    }

    if (isVideoIn3D()) { // no MI info, but flag set from user
      return "3D";
    }

    return "";
  }

  @Override
  public String getVideoHDRFormat() {
    return getMainVideoFile().isHDR() ? "HDR" : "";
  }

  public MovieEdition getEdition() {
    return edition;
  }

  public String getEditionAsString() {
    return edition.toString();
  }

  public void setOffline(boolean newValue) {
    boolean oldValue = this.offline;
    this.offline = newValue;
    firePropertyChange("offline", oldValue, newValue);
  }

  public boolean isOffline() {
    return offline;
  }

  public void setEdition(MovieEdition newValue) {
    MovieEdition oldValue = this.edition;
    this.edition = newValue;
    firePropertyChange(EDITION, oldValue, newValue);
    firePropertyChange(EDITION_AS_STRING, oldValue, newValue);
  }
}
