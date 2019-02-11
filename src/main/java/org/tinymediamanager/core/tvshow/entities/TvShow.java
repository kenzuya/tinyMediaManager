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
package org.tinymediamanager.core.tvshow.entities;

import static org.tinymediamanager.core.Constants.ACTORS;
import static org.tinymediamanager.core.Constants.ADDED_EPISODE;
import static org.tinymediamanager.core.Constants.ADDED_SEASON;
import static org.tinymediamanager.core.Constants.CERTIFICATION;
import static org.tinymediamanager.core.Constants.COUNTRY;
import static org.tinymediamanager.core.Constants.EPISODE_COUNT;
import static org.tinymediamanager.core.Constants.FIRST_AIRED;
import static org.tinymediamanager.core.Constants.FIRST_AIRED_AS_STRING;
import static org.tinymediamanager.core.Constants.GENRE;
import static org.tinymediamanager.core.Constants.GENRES_AS_STRING;
import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.IMDB;
import static org.tinymediamanager.core.Constants.REMOVED_EPISODE;
import static org.tinymediamanager.core.Constants.RUNTIME;
import static org.tinymediamanager.core.Constants.SEASON_COUNT;
import static org.tinymediamanager.core.Constants.SORT_TITLE;
import static org.tinymediamanager.core.Constants.STATUS;
import static org.tinymediamanager.core.Constants.TAG;
import static org.tinymediamanager.core.Constants.TAGS_AS_STRING;
import static org.tinymediamanager.core.Constants.TITLE_SORTABLE;
import static org.tinymediamanager.core.Constants.TRAKT;
import static org.tinymediamanager.core.Constants.TVDB;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.entities.Rating;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowMediaFileComparator;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.connector.ITvShowConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToKodiConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToXbmcConnector;
import org.tinymediamanager.core.tvshow.filenaming.TvShowNfoNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowActorImageFetcherTask;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.Certification;
import org.tinymediamanager.scraper.entities.MediaAiredStatus;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCastMember;
import org.tinymediamanager.scraper.entities.MediaGenres;
import org.tinymediamanager.scraper.entities.MediaRating;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MapUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The Class TvShow.
 * 
 * @author Manuel Laggner
 */
public class TvShow extends MediaEntity implements IMediaInformation {
  private static final Logger                LOGGER                = LoggerFactory.getLogger(TvShow.class);
  private static final Comparator<MediaFile> MEDIA_FILE_COMPARATOR = new TvShowMediaFileComparator();

  private static final Pattern               seasonPosterPattern   = Pattern.compile("(?i)season([0-9]{1,4}|special)(-poster)?\\..{2,4}");
  private static final Pattern               seasonBannerPattern   = Pattern.compile("(?i)season([0-9]{1,4}|special)-banner\\..{2,4}");
  private static final Pattern               seasonThumbPattern    = Pattern.compile("(?i)season([0-9]{1,4}|special)-thumb\\..{2,4}");

  @JsonProperty
  private int                                runtime               = 0;
  @JsonProperty
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date                               firstAired            = null;
  @JsonProperty
  private MediaAiredStatus                   status                = MediaAiredStatus.UNKNOWN;
  @JsonProperty
  private String                             sortTitle             = "";
  @JsonProperty
  private Certification                      certification         = Certification.UNKNOWN;
  @JsonProperty
  private String                             country               = "";

  @JsonProperty
  private List<MediaGenres>                  genres                = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<String>                       tags                  = new CopyOnWriteArrayList<>();
  @JsonProperty
  private HashMap<Integer, String>           seasonPosterUrlMap    = new HashMap<>(0);
  @JsonProperty
  private HashMap<Integer, String>           seasonBannerUrlMap    = new HashMap<>(0);
  @JsonProperty
  private HashMap<Integer, String>           seasonThumbUrlMap     = new HashMap<>(0);
  @JsonProperty
  private List<Person>                       actors                = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<TvShowEpisode>                dummyEpisodes         = new CopyOnWriteArrayList<>();
  @JsonProperty
  private List<String>                       extraFanartUrls       = new CopyOnWriteArrayList<>();

  private List<TvShowEpisode>                episodes              = new CopyOnWriteArrayList<>();
  private HashMap<Integer, MediaFile>        seasonPosters         = new HashMap<>(0);
  private HashMap<Integer, MediaFile>        seasonBanners         = new HashMap<>(0);
  private HashMap<Integer, MediaFile>        seasonThumbs          = new HashMap<>(0);
  private List<TvShowSeason>                 seasons               = new CopyOnWriteArrayList<>();
  private String                             titleSortable         = "";
  private Date                               lastWatched           = null;

  private PropertyChangeListener             propertyChangeListener;

  /**
   * Instantiates a tv show. To initialize the propertychangesupport after loading
   */
  public TvShow() {
    // register for dirty flag listener
    super();

    // give tag events from episodes up to the TvShowList
    propertyChangeListener = evt -> {
      if ("tag".equals(evt.getPropertyName()) && evt.getSource() instanceof TvShowEpisode) {
        firePropertyChange(evt);
      }
    };
  }

  @Override
  protected Comparator<MediaFile> getMediaFileComparator() {
    return MEDIA_FILE_COMPARATOR;
  }

  /**
   * Initialize after loading.
   */
  @Override
  public void initializeAfterLoading() {
    super.initializeAfterLoading();

    // remove empty tag and null values
    Utils.removeEmptyStringsFromList(tags);

    // load dummy episodes
    for (TvShowEpisode episode : dummyEpisodes) {
      if (episode.getSeason() == 0 && !TvShowModuleManager.SETTINGS.isDisplayMissingSpecials()) {
        continue;
      }
      episode.setTvShow(this);
      if (TvShowModuleManager.SETTINGS.isDisplayMissingEpisodes()) {
        addToSeason(episode);
      }
    }

    // create season artwork maps
    for (MediaFile mf : getMediaFiles(MediaFileType.SEASON_POSTER)) {
      if (mf.getFilename().startsWith("season-specials-poster")) {
        seasonPosters.put(0, mf);
      }
      else {
        // parse out the season from the name
        Matcher matcher = seasonPosterPattern.matcher(mf.getFilename());
        if (matcher.matches()) {
          try {
            int season = Integer.parseInt(matcher.group(1));
            seasonPosters.put(season, mf);
          }
          catch (Exception ignored) {
          }
        }
      }
    }
    for (MediaFile mf : getMediaFiles(MediaFileType.SEASON_BANNER)) {
      if (mf.getFilename().startsWith("season-specials-banner")) {
        seasonBanners.put(0, mf);
      }
      else {
        // parse out the season from the name
        Matcher matcher = seasonBannerPattern.matcher(mf.getFilename());
        if (matcher.matches()) {
          try {
            int season = Integer.parseInt(matcher.group(1));
            seasonBanners.put(season, mf);
          }
          catch (Exception ignored) {
          }
        }
      }
    }
    for (MediaFile mf : getMediaFiles(MediaFileType.SEASON_THUMB)) {
      if (mf.getFilename().startsWith("season-specials-thumb")) {
        seasonThumbs.put(0, mf);
      }
      else {
        // parse out the season from the name
        Matcher matcher = seasonThumbPattern.matcher(mf.getFilename());
        if (matcher.matches()) {
          try {
            int season = Integer.parseInt(matcher.group(1));
            seasonThumbs.put(season, mf);
          }
          catch (Exception ignored) {
          }
        }
      }
    }

    for (TvShowEpisode episode : episodes) {
      episode.addPropertyChangeListener(propertyChangeListener);
    }
  }

  /**
   * Overwrites all null/empty elements with "other" value (but might be empty also)<br>
   * For lists, check with 'contains' and add.<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the other Tv show to merge in
   */
  public void merge(TvShow other) {
    merge(other, false);
  }

  /**
   * Overwrites all elements with "other" value<br>
   * Do NOT merge path, dateAdded, scraped, mediaFiles and other crucial properties!
   *
   * @param other
   *          the other TV show to merge in
   */
  public void forceMerge(TvShow other) {
    merge(other, true);
  }

  void merge(TvShow other, boolean force) {
    if (other == null) {
      return;
    }

    super.merge(other, force);

    setSortTitle(StringUtils.isEmpty(sortTitle) || force ? other.sortTitle : sortTitle);
    setRuntime(runtime == 0 || force ? other.runtime : runtime);
    setFirstAired(firstAired == null || force ? other.firstAired : firstAired);
    setStatus(status == MediaAiredStatus.UNKNOWN || force ? other.status : status);
    setCertification(certification == Certification.NOT_RATED || force ? other.certification : certification);
    setCountry(StringUtils.isEmpty(country) || force ? other.country : country);

    // when force is set, clear the lists/maps and add all other values
    if (force) {
      genres.clear();
      tags.clear();
      actors.clear();
      extraFanartUrls.clear();

      seasonPosterUrlMap.clear();
      seasonBannerUrlMap.clear();
      seasonThumbUrlMap.clear();
    }

    setGenres(other.genres);
    setTags(other.tags);
    setActors(other.actors);
    setExtraFanartUrls(other.extraFanartUrls);

    for (Integer season : other.seasonPosterUrlMap.keySet()) {
      if (!seasonPosterUrlMap.containsKey(season)) {
        seasonPosterUrlMap.put(season, other.seasonPosterUrlMap.get(season));
      }
    }
    for (Integer season : other.seasonBannerUrlMap.keySet()) {
      if (!seasonBannerUrlMap.containsKey(season)) {
        seasonBannerUrlMap.put(season, other.seasonBannerUrlMap.get(season));
      }
    }
    for (Integer season : other.seasonThumbUrlMap.keySet()) {
      if (!seasonThumbUrlMap.containsKey(season)) {
        seasonThumbUrlMap.put(season, other.seasonThumbUrlMap.get(season));
      }
    }

    // get ours, and merge other values
    for (TvShowEpisode ep : episodes) {
      TvShowEpisode otherEP = other.getEpisode(ep.getSeason(), ep.getEpisode());
      ep.merge(otherEP, force);
    }

    // get others, and simply add
    for (TvShowEpisode otherEp : other.getEpisodes()) {
      TvShowEpisode ourEP = getEpisode(otherEp.getSeason(), otherEp.getEpisode()); // do not do a contains check!
      if (ourEP == null) {
        TvShowEpisode clone = new TvShowEpisode(otherEp);
        clone.setTvShow(this); // yes!
        addEpisode(clone);
      }
    }
  }

  @Override
  public void setTitle(String newValue) {
    String oldValue = this.title;
    super.setTitle(newValue);

    oldValue = this.titleSortable;
    titleSortable = "";
    firePropertyChange(TITLE_SORTABLE, oldValue, titleSortable);
  }

  /**
   * Returns the sortable variant of title<br>
   * eg "The Big Bang Theory" -> "Big Bang Theory, The".
   * 
   * @return the title in its sortable format
   */
  public String getTitleSortable() {
    if (StringUtils.isEmpty(titleSortable)) {
      titleSortable = Utils.getSortableName(this.getTitle());
    }
    return titleSortable;
  }

  public void clearTitleSortable() {
    titleSortable = "";
  }

  public String getSortTitle() {
    return sortTitle;
  }

  public void setSortTitle(String newValue) {
    String oldValue = this.sortTitle;
    this.sortTitle = newValue;
    firePropertyChange(SORT_TITLE, oldValue, newValue);
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
    if (TvShowModuleManager.SETTINGS.getPreferPersonalRating()) {
      rating = ratings.get(Rating.USER);
    }

    // the default rating
    if (rating == null) {
      rating = ratings.get(TvShowModuleManager.SETTINGS.getPreferredRating());
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
   * Gets the episodes.
   * 
   * @return the episodes
   */
  public List<TvShowEpisode> getEpisodes() {
    return episodes;
  }

  /**
   * Adds the episode.
   * 
   * @param episode
   *          the episode
   */
  public void addEpisode(TvShowEpisode episode) {
    int oldValue = episodes.size();
    episodes.add(episode);
    episode.addPropertyChangeListener(propertyChangeListener);
    addToSeason(episode);

    episodes.sort(TvShowEpisode::compareTo);

    firePropertyChange(ADDED_EPISODE, null, episode);
    firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());
  }

  public List<TvShowEpisode> getDummyEpisodes() {
    return dummyEpisodes;
  }

  public void setDummyEpisodes(List<TvShowEpisode> dummyEpisodes) {
    this.dummyEpisodes.clear();
    this.dummyEpisodes.addAll(dummyEpisodes);

    // also mix in the episodes if activated
    if (TvShowModuleManager.SETTINGS.isDisplayMissingEpisodes()) {
      for (TvShowEpisode episode : dummyEpisodes) {
        episode.setTvShow(this);

        if (episode.getSeason() == 0 && !TvShowModuleManager.SETTINGS.isDisplayMissingSpecials()) {
          continue;
        }

        TvShowSeason season = getSeasonForEpisode(episode);

        // also fire the event there was no episode for that dummy yet
        boolean found = false;
        for (TvShowEpisode e : season.getEpisodesForDisplay()) {
          if (e.getSeason() == episode.getSeason() && e.getEpisode() == episode.getEpisode()) {
            found = true;
            break;
          }
        }

        if (found) {
          continue;
        }

        season.addEpisode(episode);
        firePropertyChange(ADDED_EPISODE, null, episode);
      }
    }

    this.dummyEpisodes.sort(TvShowEpisode::compareTo);

    firePropertyChange("dummyEpisodes", null, dummyEpisodes);
    firePropertyChange(EPISODE_COUNT, 0, episodes.size());
  }

  /**
   * build a list of <br>
   * a) available episodes along with<br>
   * b) missing episodes <br>
   * for display in the TV show list
   *
   * @return a list of _all_ episodes
   */
  public List<TvShowEpisode> getEpisodesForDisplay() {
    List<TvShowEpisode> episodes = new ArrayList<>(getEpisodes());

    // mix in unavailable episodes if the user wants to
    if (TvShowModuleManager.SETTINGS.isDisplayMissingEpisodes()) {
      // build up a set which holds a string representing the S/E indicator
      Set<String> availableEpisodes = new HashSet<>();

      for (TvShowEpisode episode : episodes) {
        availableEpisodes.add(episode.getSeason() + "." + episode.getEpisode());
      }

      // and now mix in unavailable ones
      for (TvShowEpisode episode : getDummyEpisodes()) {
        if (episode.getSeason() == 0 && !TvShowModuleManager.SETTINGS.isDisplayMissingSpecials()) {
          continue;
        }
        if (!availableEpisodes.contains(episode.getSeason() + "." + episode.getEpisode())) {
          episodes.add(episode);
        }
      }
    }

    return episodes;
  }

  /**
   * Gets the episode count.
   * 
   * @return the episode count
   */
  public int getEpisodeCount() {
    return episodes.size();
  }

  /**
   * Gets the dummy episode count
   * 
   * @return the dummy episode count
   */
  public int getDummyEpisodeCount() {
    int count = 0;
    for (TvShowSeason season : seasons) {
      for (TvShowEpisode episode : season.getEpisodesForDisplay()) {
        if (episode.isDummy()) {
          if (episode.getSeason() == 0 && !TvShowModuleManager.SETTINGS.isDisplayMissingSpecials()) {
            continue;
          }
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Adds the to season.
   * 
   * @param episode
   *          the episode
   */
  private void addToSeason(TvShowEpisode episode) {
    TvShowSeason season = getSeasonForEpisode(episode);
    season.addEpisode(episode);
  }

  private void removeFromSeason(TvShowEpisode episode) {
    TvShowSeason season = getSeasonForEpisode(episode);
    season.removeEpisode(episode);
  }

  /**
   * Gets the season for episode.
   * 
   * @param episode
   *          the episode
   * @return the season for episode
   */
  public synchronized TvShowSeason getSeasonForEpisode(TvShowEpisode episode) {
    TvShowSeason season = null;

    // search for an existing season
    for (TvShowSeason s : seasons) {
      if (s.getSeason() == episode.getSeason()) {
        season = s;
        break;
      }
    }

    // no one found - create one
    if (season == null) {
      int oldValue = seasons.size();
      season = new TvShowSeason(episode.getSeason(), this);
      seasons.add(season);
      firePropertyChange(ADDED_SEASON, null, season);
      firePropertyChange(SEASON_COUNT, oldValue, seasons.size());
    }

    return season;
  }

  public int getSeasonCount() {
    int count = 0;
    for (TvShowSeason season : seasons) {
      if (!season.isDummy()) {
        count++;
      }
    }
    return count;
  }

  /**
   * gets the season object for the given season number or null
   *
   * @param seasonNumber
   *          the season number
   * @return the TvShowSeason object or null
   */
  public TvShowSeason getSeason(int seasonNumber) {
    for (TvShowSeason season : seasons) {
      if (season.getSeason() == seasonNumber) {
        return season;
      }
    }
    return null;
  }

  /**
   * remove all episodes from this tv show.
   */
  public void removeAllEpisodes() {
    int oldValue = episodes.size();
    if (episodes.size() > 0) {
      for (int i = episodes.size() - 1; i >= 0; i--) {
        TvShowEpisode episode = episodes.get(i);
        episodes.remove(episode);
        episode.removePropertyChangeListener(propertyChangeListener);
        TvShowList.getInstance().removeEpisodeFromDb(episode);
      }
    }

    firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());
  }

  /**
   * Removes the episode.
   * 
   * @param episode
   *          the episode
   */
  public void removeEpisode(TvShowEpisode episode) {
    if (episodes.contains(episode)) {
      int oldValue = episodes.size();
      episodes.remove(episode);
      episode.removePropertyChangeListener(propertyChangeListener);
      removeFromSeason(episode);
      TvShowList.getInstance().removeEpisodeFromDb(episode);
      saveToDb();

      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());

      // and mix in the dummy one again
      if (TvShowModuleManager.SETTINGS.isDisplayMissingEpisodes()) {
        for (TvShowEpisode dummy : dummyEpisodes) {
          if (dummy.getSeason() == 0 && !TvShowModuleManager.SETTINGS.isDisplayMissingSpecials()) {
            continue;
          }
          if (dummy.getSeason() == episode.getSeason() && dummy.getEpisode() == episode.getEpisode()) {
            addToSeason(dummy);
            firePropertyChange(ADDED_EPISODE, null, dummy);
            break;
          }
        }
      }
    }
    else if (dummyEpisodes.contains(episode)) {
      // just fire the event for updating the UI
      removeFromSeason(episode);
      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(EPISODE_COUNT, 0, episodes.size());
    }
  }

  /**
   * Removes an episode from tmm and deletes it from the data source
   * 
   * @param episode
   *          the episode to be removed
   */
  public void deleteEpisode(TvShowEpisode episode) {
    if (episodes.contains(episode)) {
      int oldValue = episodes.size();
      episode.deleteFilesSafely();
      episodes.remove(episode);
      episode.removePropertyChangeListener(propertyChangeListener);
      removeFromSeason(episode);
      TvShowList.getInstance().removeEpisodeFromDb(episode);
      saveToDb();

      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());
    }
  }

  /**
   * Gets the seasons.
   * 
   * @return the seasons
   */
  public List<TvShowSeason> getSeasons() {
    return seasons;
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
   * Sets the metadata.
   * 
   * @param metadata
   *          the new metadata
   * @param config
   *          the config
   */
  public void setMetadata(MediaMetadata metadata, TvShowScraperMetadataConfig config) {
    // check against null metadata (e.g. aborted request)
    if (metadata == null) {
      LOGGER.error("metadata was null");
      return;
    }

    // check if metadata has at least a name
    if (StringUtils.isEmpty(metadata.getTitle())) {
      LOGGER.warn("wanted to save empty metadata for " + getTitle());
      return;
    }

    // populate ids
    for (Entry<String, Object> entry : metadata.getIds().entrySet()) {
      setId(entry.getKey(), entry.getValue().toString());
    }

    // if option is set capitalize the first letter of each word
    // in title and original title
    if (config.isTitle()) {
      if (TvShowModuleManager.SETTINGS.getCapitalWordsInTitles()) {
        setTitle(WordUtils.capitalize(metadata.getTitle()));
        setOriginalTitle(WordUtils.capitalize(metadata.getOriginalTitle()));
      }
      else {
        setTitle(metadata.getTitle());
        setOriginalTitle(metadata.getOriginalTitle());
      }
    }

    if (config.isPlot()) {
      setPlot(metadata.getPlot());
    }

    if (config.isYear()) {
      setYear(metadata.getYear());
    }

    if (config.isRating()) {
      clearRatings();
      for (MediaRating mediaRating : metadata.getRatings()) {
        setRating(new Rating(mediaRating));
      }
    }

    if (config.isAired()) {
      setFirstAired(metadata.getReleaseDate());
    }

    if (config.isStatus()) {
      setStatus(metadata.getStatus());
    }

    if (config.isRuntime()) {
      setRuntime(metadata.getRuntime());
    }

    if (config.isCountry()) {
      setCountry(StringUtils.join(metadata.getCountries(), ", "));
    }

    if (config.isStudio()) {
      setProductionCompany(StringUtils.join(metadata.getProductionCompanies(), ", "));
    }

    if (config.isCast()) {

      List<Person> actors = new ArrayList<>();

      for (MediaCastMember member : metadata.getCastMembers()) {
        switch (member.getType()) {
          case ACTOR:
            actors.add(new Person(member));
            break;

          default:
            break;
        }
      }
      setActors(actors);
      writeActorImages();
    }

    if (config.isCertification()) {
      if (metadata.getCertifications().size() > 0) {
        setCertification(metadata.getCertifications().get(0));
      }
    }

    if (config.isGenres()) {
      setGenres(metadata.getGenres());
    }

    // set scraped
    setScraped(true);

    // update DB
    writeNFO();
    saveToDb();

    // rename the TV show if that has been chosen in the settings
    if (TvShowModuleManager.SETTINGS.isRenameAfterScrape()) {
      TvShowRenamer.renameTvShowRoot(this); // rename root and season artwork and update ShowMFs
    }
  }

  /**
   * Sets the artwork.
   * 
   * @param artwork
   *          the artwork
   * @param config
   *          the config
   */
  public void setArtwork(List<MediaArtwork> artwork, TvShowScraperMetadataConfig config) {
    if (config.isArtwork()) {
      TvShowArtworkHelper.setArtwork(this, artwork);
    }
  }

  /**
   * download the specified type of artwork for this TV show
   * 
   * @param type
   *          the chosen artwork type to be downloaded
   */
  public void downloadArtwork(MediaFileType type) {
    TvShowArtworkHelper.downloadArtwork(this, type);
  }

  /**
   * download season artwork
   * 
   * @param season
   *          the season to download the artwork for
   * @param artworkType
   *          the artwork type to download
   */
  public void downloadSeasonArtwork(int season, MediaArtworkType artworkType) {
    TvShowArtworkHelper.downloadSeasonArtwork(this, season, artworkType);
  }

  /**
   * Write nfo.
   */
  public void writeNFO() {
    ITvShowConnector connector;

    List<TvShowNfoNaming> nfoNamings = TvShowModuleManager.SETTINGS.getNfoFilenames();
    if (nfoNamings.isEmpty()) {
      return;
    }

    switch (TvShowModuleManager.SETTINGS.getTvShowConnector()) {
      case KODI:
        connector = new TvShowToKodiConnector(this);
        break;

      case XBMC:
      default:
        connector = new TvShowToXbmcConnector(this);
        break;
    }

    connector.write(nfoNamings);

    firePropertyChange(HAS_NFO_FILE, false, true);
  }

  /**
   * Gets the checks for nfo file.
   * 
   * @return the checks for nfo file
   */
  public Boolean getHasNfoFile() {
    List<MediaFile> nfos = getMediaFiles(MediaFileType.NFO);
    return nfos != null && nfos.size() > 0;
  }

  /**
   * Gets the checks for images.
   * 
   * @return the checks for images
   */
  public Boolean getHasImages() {
    return StringUtils.isNotBlank(getArtworkFilename(MediaFileType.POSTER)) && StringUtils.isNotBlank(getArtworkFilename(MediaFileType.FANART))
        && StringUtils.isNotBlank(getArtworkFilename(MediaFileType.BANNER));
  }

  /**
   * Checks if all seasaons and episodes of that TV show have artwork assigned
   *
   * @return true if artwork is available
   */
  public Boolean getHasSeasonAndEpisodeImages() {
    boolean images = true;
    for (TvShowSeason season : seasons) {
      if (!season.getHasImages() || !season.getHasEpisodeImages()) {
        images = false;
        break;
      }
    }
    return images;
  }

  /**
   * Checks if all episodes of that season have a NFO file
   *
   * @return true if NFO files are available
   */
  public Boolean getHasEpisodeNfoFiles() {
    boolean nfo = true;
    for (TvShowEpisode episode : episodes) {
      if (!episode.getHasNfoFile()) {
        nfo = false;
        break;
      }
    }
    return nfo;
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
   * Sets the imdb id.
   * 
   * @param newValue
   *          the new imdb id
   */
  public void setImdbId(String newValue) {
    this.setId(IMDB, newValue);
  }

  /**
   * Gets the tvdb id.
   * 
   * @return the tvdb id
   */
  public String getTvdbId() {
    return this.getIdAsString(TVDB);
  }

  /**
   * Sets the tvdb id.
   * 
   * @param newValue
   *          the new tvdb id
   */
  public void setTvdbId(String newValue) {
    this.setId(TVDB, newValue);
  }

  /**
   * Gets the TraktTV id.
   * 
   * @return the TraktTV id
   */
  public int getTraktId() {
    return this.getIdAsInt(TRAKT);
  }

  /**
   * Sets the TvRage id.
   * 
   * @param newValue
   *          the new TraktTV id
   */
  public void setTraktId(int newValue) {
    this.setId(TRAKT, newValue);
  }

  /**
   * first aired date.
   * 
   * @return the date
   */
  public Date getFirstAired() {
    return firstAired;
  }

  @JsonIgnore
  public void setFirstAired(Date newValue) {
    Date oldValue = this.firstAired;
    this.firstAired = newValue;
    firePropertyChange(FIRST_AIRED, oldValue, newValue);
    firePropertyChange(FIRST_AIRED_AS_STRING, oldValue, newValue);
  }

  /**
   * first aired date as yyyy-mm-dd<br>
   * https://xkcd.com/1179/ :P
   * 
   * @return the date or empty string
   */
  public String getFirstAiredFormatted() {
    if (this.firstAired == null) {
      return "";
    }
    return new SimpleDateFormat("yyyy-MM-dd").format(this.firstAired);
  }

  /**
   * Gets the first aired as a string, formatted in the system locale.
   * 
   * @return the first aired as string
   */
  public String getFirstAiredAsString() {
    if (this.firstAired == null) {
      return "";
    }
    return SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(firstAired);
  }

  /**
   * convenient method to set the first aired date (parsed from string).
   * 
   * @param aired
   *          the new first aired
   */
  public void setFirstAired(String aired) {
    try {
      setFirstAired(StrgUtils.parseDate(aired));
    }
    catch (ParseException ignored) {
    }
  }

  /**
   * Gets the status.
   * 
   * @return the status
   */
  public MediaAiredStatus getStatus() {
    return status;
  }

  /**
   * Sets the status.
   * 
   * @param newValue
   *          the new status
   */
  public void setStatus(MediaAiredStatus newValue) {
    MediaAiredStatus oldValue = this.status;
    this.status = newValue;
    firePropertyChange(STATUS, oldValue, newValue);
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

    for (String tag : tags) {
      if (tag.equals(newTag)) {
        return;
      }
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
   * Gets the runtime.
   * 
   * @return the runtime
   */
  public int getRuntime() {
    return runtime;
  }

  /**
   * Sets the runtime.
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
   * Adds the actor.
   * 
   * @param obj
   *          the obj
   */
  public void addActor(Person obj) {
    actors.add(obj);
    firePropertyChange(ACTORS, null, this.getActors());
  }

  /**
   * Gets the actors.
   * 
   * @return the actors
   */
  public List<Person> getActors() {
    return this.actors;
  }

  /**
   * Removes the actor.
   * 
   * @param obj
   *          the obj
   */
  public void removeActor(Person obj) {
    actors.remove(obj);

    firePropertyChange(ACTORS, null, this.getActors());
  }

  /**
   * Sets the actors.
   * 
   * @param newActors
   *          the new actors
   */
  @JsonSetter
  public void setActors(List<Person> newActors) {
    // two way sync of actors
    ListUtils.mergeLists(actors, newActors);
    firePropertyChange(ACTORS, null, this.getActors());
  }

  /**
   * Gets the certifications.
   * 
   * @return the certifications
   */
  @Override
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
   * get the country
   * 
   * @return the countries in which this TV show has been produced
   */
  public String getCountry() {
    return country;
  }

  /**
   * set the country
   * 
   * @param newValue
   *          the country in which this TV show has been produced
   */
  public void setCountry(String newValue) {
    String oldValue = this.country;
    this.country = newValue;
    firePropertyChange(COUNTRY, oldValue, newValue);
  }

  /**
   * <p>
   * Uses <code>ReflectionToStringBuilder</code> to generate a <code>toString</code> for the specified object.
   * </p>
   * 
   * @return the String result
   * @see ReflectionToStringBuilder#toString()
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  /**
   * get all episodes to scraper (with season or ep number == -1)
   * 
   * @return a list of all episodes to scrape
   */
  public List<TvShowEpisode> getEpisodesToScrape() {
    List<TvShowEpisode> episodes = new ArrayList<>();
    for (TvShowEpisode episode : this.episodes) {
      if (episode.getFirstAired() != null || (episode.getSeason() > -1 && episode.getEpisode() > -1)) {
        episodes.add(episode);
      }
    }
    return episodes;
  }

  /**
   * Checks if all episodes are watched.
   * 
   * @return true, if all episodes are watched
   */
  public boolean isWatched() {
    boolean watched = true;

    for (TvShowEpisode episode : episodes) {
      if (!episode.isWatched()) {
        watched = false;
        break;
      }
    }

    return watched;
  }

  public Date getLastWatched() {
    return lastWatched;
  }

  public void setLastWatched(Date lastWatched) {
    this.lastWatched = lastWatched;
  }

  /**
   * Sets the season artwork url.
   *
   * @param season
   *          the season
   * @param url
   *          the url
   * @param artworkType
   *          the artwork type
   */
  public void setSeasonArtworkUrl(int season, String url, MediaArtworkType artworkType) {
    switch (artworkType) {
      case SEASON_POSTER:
        seasonPosterUrlMap.put(season, url);
        break;

      case SEASON_BANNER:
        seasonBannerUrlMap.put(season, url);
        break;

      case SEASON_THUMB:
        seasonThumbUrlMap.put(season, url);
        break;

      default:
        return;
    }
  }

  /**
   * Gets the season artwork url.
   *
   * @param season
   *          the season
   * @param artworkType
   *          the artwork type
   * @return the season artwork url or an empty string
   */
  public String getSeasonArtworkUrl(int season, MediaArtworkType artworkType) {
    String url = null;

    switch (artworkType) {
      case SEASON_POSTER:
        url = seasonPosterUrlMap.get(season);
        break;

      case SEASON_BANNER:
        url = seasonBannerUrlMap.get(season);
        break;

      case SEASON_THUMB:
        url = seasonThumbUrlMap.get(season);
        break;

      default:
        break;
    }

    if (StringUtils.isBlank(url)) {
      return "";
    }

    return url;
  }

  /**
   * get all season artwork urls
   * 
   * @param artworkType
   *          the artwork type to get the artwork for
   * @return a map containing all available season artworks for the given type
   *
   */
  public Map<Integer, String> getSeasonArtworkUrls(MediaArtworkType artworkType) {
    switch (artworkType) {
      case SEASON_POSTER:
        return MapUtils.sortByKey(seasonPosterUrlMap);

      case SEASON_BANNER:
        return MapUtils.sortByKey(seasonBannerUrlMap);

      case SEASON_THUMB:
        return MapUtils.sortByKey(seasonThumbUrlMap);

      default:
        return new HashMap<>(0);
    }
  }

  /**
   * Gets the season artwork.
   * 
   * @param season
   *          the season
   * @param artworkType
   *          the artwork type
   * @return the season artwork
   *
   */
  public String getSeasonArtwork(int season, MediaArtworkType artworkType) {
    MediaFile artworkFile = null;
    switch (artworkType) {
      case SEASON_POSTER:
        artworkFile = seasonPosters.get(season);
        break;

      case SEASON_BANNER:
        artworkFile = seasonBanners.get(season);
        break;

      case SEASON_THUMB:
        artworkFile = seasonThumbs.get(season);
        break;

      default:
        break;

    }

    if (artworkFile != null) {
      return artworkFile.getFile().toString();
    }
    return "";
  }

  /**
   * get all season artwork filenames
   *
   * @param artworkType
   *          the artwork type to get the artwork for
   * @return a map containing all available season artwork filenames for the given type
   *
   */
  public Map<Integer, MediaFile> getSeasonArtworks(MediaArtworkType artworkType) {
    switch (artworkType) {
      case SEASON_POSTER:
        return MapUtils.sortByKey(seasonPosters);

      case SEASON_BANNER:
        return MapUtils.sortByKey(seasonBanners);

      case SEASON_THUMB:
        return MapUtils.sortByKey(seasonThumbs);

      default:
        return new HashMap<>(0);
    }
  }

  Dimension getSeasonArtworkSize(int season, MediaArtworkType type) {
    MediaFile artworkFile = null;
    switch (type) {
      case SEASON_POSTER:
        artworkFile = seasonPosters.get(season);
        break;

      case SEASON_BANNER:
        artworkFile = seasonBanners.get(season);
        break;

      case SEASON_THUMB:
        artworkFile = seasonThumbs.get(season);
        break;

      default:
        break;

    }

    if (artworkFile != null) {
      return new Dimension(artworkFile.getVideoWidth(), artworkFile.getVideoHeight());
    }

    return new Dimension(0, 0);
  }

  /**
   * Sets the season artwork.
   * 
   * @param season
   *          the season
   * @param artworkType
   *          the artwork type
   * @param file
   *          the file
   */
  public void setSeasonArtwork(int season, MediaArtworkType artworkType, Path file) {
    MediaFile mf = new MediaFile(file, MediaFileType.getMediaFileType(artworkType));
    setSeasonArtwork(season, mf);
  }

  /**
   * Sets the season artwork.
   * 
   * @param season
   *          the season
   * @param mf
   *          the media file
   */
  public void setSeasonArtwork(int season, MediaFile mf) {
    MediaFile oldMf = null;

    MediaArtworkType artworkType = MediaFileType.getMediaArtworkType(mf.getType());

    // check if that MF is already in our show
    switch (artworkType) {
      case SEASON_POSTER:
        oldMf = seasonPosters.get(season);
        break;

      case SEASON_BANNER:
        oldMf = seasonBanners.get(season);
        break;

      case SEASON_THUMB:
        oldMf = seasonThumbs.get(season);
        break;

      default:
        return;
    }

    if (oldMf != null && oldMf.equals(mf)) {
      // it is there - do not add it again
      return;
    }

    mf.gatherMediaInformation();
    addToMediaFiles(mf);

    // add it
    switch (artworkType) {
      case SEASON_POSTER:
        seasonPosters.put(season, mf);
        break;

      case SEASON_BANNER:
        seasonBanners.put(season, mf);
        break;

      case SEASON_THUMB:
        seasonThumbs.put(season, mf);
        break;

      default:
        break;
    }
  }

  void clearSeasonArtwork(int season, MediaArtworkType artworkType) {
    MediaFile mf = null;
    switch (artworkType) {
      case SEASON_POSTER:
        mf = seasonPosters.get(season);
        seasonPosters.remove(season);
        break;

      case SEASON_BANNER:
        mf = seasonBanners.get(season);
        seasonBanners.remove(season);
        break;

      case SEASON_THUMB:
        mf = seasonThumbs.get(season);
        seasonThumbs.remove(season);
        break;

      default:
        return;
    }

    if (mf != null) {
      removeFromMediaFiles(mf);
    }
  }

  void clearSeasonArtworkUrl(int season, MediaArtworkType artworkType) {
    switch (artworkType) {
      case SEASON_POSTER:
        seasonPosterUrlMap.remove(season);
        break;

      case SEASON_BANNER:
        seasonBannerUrlMap.remove(season);
        break;

      case SEASON_THUMB:
        seasonThumbUrlMap.remove(season);
        break;

      default:
        return;
    }
  }

  /**
   * Gets the extra fanart urls.
   *
   * @return the extra fanart urls
   */
  public List<String> getExtraFanartUrls() {
    return extraFanartUrls;
  }

  /**
   * Sets the extra fanart urls.
   *
   * @param extraFanartUrls
   *          the new extra fanart urls
   */
  @JsonSetter
  public void setExtraFanartUrls(List<String> extraFanartUrls) {
    ListUtils.mergeLists(this.extraFanartUrls, extraFanartUrls);
  }

  /**
   * Gets the media files of all episodes.<br>
   * (without the TV show MFs like poster/banner/...)
   * 
   * @return the media files
   */
  public List<MediaFile> getEpisodesMediaFiles() {
    List<MediaFile> mediaFiles = new ArrayList<>();
    for (TvShowEpisode episode : this.episodes) {
      for (MediaFile mf : episode.getMediaFiles()) {
        if (!mediaFiles.contains(mf)) {
          mediaFiles.add(mf);
        }
      }
    }
    return mediaFiles;
  }

  /**
   * Gets the images to cache.
   * 
   * @return the images to cache
   */
  public List<Path> getImagesToCache() {
    // get files to cache
    List<Path> filesToCache = new ArrayList<>();

    for (MediaFile mf : getMediaFiles()) {
      if (mf.isGraphic()) {
        filesToCache.add(mf.getFileAsPath());
      }
    }

    filesToCache.addAll(listActorFiles());

    for (TvShowEpisode episode : new ArrayList<>(this.episodes)) {
      filesToCache.addAll(episode.getImagesToCache());
    }

    return filesToCache;
  }

  @Override
  public synchronized void callbackForWrittenArtwork(MediaArtworkType type) {
  }

  @Override
  public void saveToDb() {
    // update/insert this TV show to the database
    TvShowList.getInstance().persistTvShow(this);
  }

  @Override
  public void deleteFromDb() {
    // remove this TV show from the database
    TvShowList.getInstance().removeTvShow(this);
  }

  public TvShowEpisode getEpisode(int season, int episode) {
    TvShowEpisode ep = null;

    for (TvShowEpisode e : new ArrayList<>(this.episodes)) {
      if (e.getSeason() == season && e.getEpisode() == episode) {
        ep = e;
        break;
      }
    }
    return ep;
  }

  /**
   * check if one of the tv shows episode is newly added
   * 
   * @return true/false
   */
  public boolean hasNewlyAddedEpisodes() {
    for (TvShowEpisode episode : new ArrayList<>(this.episodes)) {
      if (episode.isNewlyAdded()) {
        return true;
      }
    }
    return false;
  }

  /**
   * checks if this TV show has been scraped.<br>
   * On a fresh DB, just reading local files, everything is again "unscraped". <br>
   * detect minimum of filled values as "scraped"
   * 
   * @return isScraped
   */
  @Override
  public boolean isScraped() {
    if (!scraped) {
      if (!plot.isEmpty() && !(year == 0) && !(genres == null || genres.size() == 0) && !(actors == null || actors.size() == 0)) {
        return true;
      }
    }
    return scraped;
  }

  /**
   * Write actor images.
   */
  public void writeActorImages() {
    // check if actor images shall be written
    if (!TvShowModuleManager.SETTINGS.isWriteActorImages()) {
      return;
    }

    TvShowActorImageFetcherTask task = new TvShowActorImageFetcherTask(this);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
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

  /**
   * <b>PHYSICALLY</b> deletes a complete TV show by moving it to datasource backup folder<br>
   * DS\.backup\&lt;moviename&gt;
   */
  public boolean deleteFilesSafely() {
    return Utils.deleteDirectorySafely(getPathNIO(), getDataSource());
  }

  @Override
  public MediaFile getMainVideoFile() {
    return new MediaFile();
  }

  @Override
  public String getMediaInfoVideoFormat() {
    return "";
  }

  @Override
  public String getMediaInfoVideoResolution() {
    return "";
  }

  @Override
  public float getMediaInfoAspectRatio() {
    return 0;
  }

  @Override
  public String getMediaInfoVideoCodec() {
    return "";
  }

  @Override
  public double getMediaInfoFrameRate() {
    return 0;
  }

  @Override
  public String getVideoHDRFormat() {
    return "";
  }

  @Override
  public boolean isVideoIn3D() {
    return false;
  }

  @Override
  public String getMediaInfoAudioCodec() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioCodecList() {
    return new ArrayList<>();
  }

  @Override
  public String getMediaInfoAudioChannels() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioChannelList() {
    return new ArrayList<>();
  }

  @Override
  public String getMediaInfoAudioLanguage() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioLanguageList() {
    return new ArrayList<>();
  }

  @Override
  public String getMediaInfoContainerFormat() {
    return "";
  }

  @Override
  public MediaSource getMediaInfoSource() {
    return MediaSource.UNKNOWN;
  }
}
