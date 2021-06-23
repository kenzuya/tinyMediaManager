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
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.REMOVED_EPISODE;
import static org.tinymediamanager.core.Constants.RUNTIME;
import static org.tinymediamanager.core.Constants.SEASON;
import static org.tinymediamanager.core.Constants.SEASON_COUNT;
import static org.tinymediamanager.core.Constants.SORT_TITLE;
import static org.tinymediamanager.core.Constants.STATUS;
import static org.tinymediamanager.core.Constants.SUBTITLES;
import static org.tinymediamanager.core.Constants.TAGS;
import static org.tinymediamanager.core.Constants.TITLE_SORTABLE;
import static org.tinymediamanager.core.Constants.TMDB;
import static org.tinymediamanager.core.Constants.TRAILER;
import static org.tinymediamanager.core.Constants.TRAKT;
import static org.tinymediamanager.core.Constants.TVDB;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IMediaInformation;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.MediaSource;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.TrailerSources;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskChain;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.TvShowEpisodeAndSeasonParser;
import org.tinymediamanager.core.tvshow.TvShowMediaFileComparator;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.connector.ITvShowConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToKodiConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowToXbmcConnector;
import org.tinymediamanager.core.tvshow.filenaming.TvShowNfoNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowTrailerNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowARDetectorTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowActorImageFetcherTask;
import org.tinymediamanager.core.tvshow.tasks.TvShowRenameTask;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
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
  private static final Logger                   LOGGER                     = LoggerFactory.getLogger(TvShow.class);
  private static final Comparator<MediaFile>    MEDIA_FILE_COMPARATOR      = new TvShowMediaFileComparator();

  private static final Pattern                  SEASON_NUMBER              = Pattern.compile("(?i)season([0-9]{1,4}).*");
  private static final Pattern                  SEASON_FOLDER_NUMBER       = Pattern.compile("(?i).*([0-9]{1,4}).*");

  @JsonProperty
  private int                                   runtime                    = 0;
  @JsonProperty
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date                                  firstAired                 = null;
  @JsonProperty
  private MediaAiredStatus                      status                     = MediaAiredStatus.UNKNOWN;
  @JsonProperty
  private String                                sortTitle                  = "";
  @JsonProperty
  private MediaCertification                    certification              = MediaCertification.UNKNOWN;
  @JsonProperty
  private String                                country                    = "";

  @JsonProperty
  private final List<MediaGenres>               genres                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final Map<Integer, String>            seasonTitleMap             = new HashMap<>(0);
  @JsonProperty
  private final Map<Integer, String>            seasonPosterUrlMap         = new HashMap<>(0);
  @JsonProperty
  private final Map<Integer, String>            seasonBannerUrlMap         = new HashMap<>(0);
  @JsonProperty
  private final Map<Integer, String>            seasonThumbUrlMap          = new HashMap<>(0);
  @JsonProperty
  private final List<Person>                    actors                     = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<TvShowEpisode>             dummyEpisodes              = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<String>                    extraFanartUrls            = new CopyOnWriteArrayList<>();
  @JsonProperty
  private final List<MediaTrailer>              trailer                    = new CopyOnWriteArrayList<>();

  private final List<TvShowEpisode>             episodes                   = new CopyOnWriteArrayList<>();
  private final Map<Integer, MediaFile>         seasonPosters              = new HashMap<>(0);
  private final Map<Integer, MediaFile>         seasonBanners              = new HashMap<>(0);
  private final Map<Integer, MediaFile>         seasonThumbs               = new HashMap<>(0);
  private final List<TvShowSeason>              seasons                    = new CopyOnWriteArrayList<>();
  private String                                titleSortable              = "";
  private Date                                  lastWatched                = null;

  private final PropertyChangeListener          propertyChangeListener;

  private static final Comparator<MediaTrailer> TRAILER_QUALITY_COMPARATOR = new MediaTrailer.QualityComparator();

  /**
   * Instantiates a tv show. To initialize the propertychangesupport after loading
   */
  public TvShow() {
    // register for dirty flag listener
    super();

    propertyChangeListener = evt -> {
      if (evt.getSource() instanceof TvShowEpisode) {
        TvShowEpisode episode = (TvShowEpisode) evt.getSource();

        switch (evt.getPropertyName()) {
          case TAGS:
          case MEDIA_INFORMATION:
          case MEDIA_FILES:
          case SUBTITLES:
          case "hasSubtitles":
            firePropertyChange(evt);
            break;

          case SEASON:
            // remove from any season which is not the desired season
            for (TvShowSeason season : seasons) {
              if (season.getEpisodes().contains(episode) && season.getSeason() != episode.getSeason()) {
                season.removeEpisode(episode);
              }
            }
            addToSeason(episode);
            break;
        }
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

    // load dummy episodes
    for (TvShowEpisode episode : dummyEpisodes) {
      episode.setTvShow(this);
      if (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isDisplayMissingSpecials()) {
        continue;
      }
      if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
        addToSeason(episode);
      }
    }

    // create season artwork maps
    for (MediaFile mf : getMediaFiles(MediaFileType.SEASON_POSTER, MediaFileType.SEASON_BANNER, MediaFileType.SEASON_THUMB)) {
      // do not process 0 byte files
      if (mf.getFilesize() == 0) {
        continue;
      }

      int season = Integer.MIN_VALUE;
      try {
        if (mf.getFilename().startsWith("season-specials")) {
          season = 0;
        }
        else if (mf.getFilename().startsWith("season-all")) {
          season = -1;
        }
        else {
          // parse out the season from the name
          Matcher matcher = SEASON_NUMBER.matcher(mf.getFilename());
          if (matcher.matches()) {
            season = Integer.parseInt(matcher.group(1));
          }

          // try to parse out the season from the parent
          if (season == Integer.MIN_VALUE) {
            matcher = SEASON_NUMBER.matcher(mf.getFileAsPath().getParent().toString());
            if (matcher.matches()) {
              season = Integer.parseInt(matcher.group(1));
            }
          }
          if (season == Integer.MIN_VALUE) {
            matcher = SEASON_FOLDER_NUMBER.matcher(mf.getFileAsPath().getParent().toString());
            if (matcher.matches()) {
              season = Integer.parseInt(matcher.group(1));
            }
          }

        }

        if (season == Integer.MIN_VALUE) {
          throw new IllegalStateException("did not find a season number");
        }
        else {
          switch (mf.getType()) {
            case SEASON_BANNER:
              seasonBanners.put(season, mf);
              break;

            case SEASON_POSTER:
              seasonPosters.put(season, mf);
              break;

            case SEASON_THUMB:
              seasonThumbs.put(season, mf);
              break;

            default:
              break;
          }
        }
      }
      catch (Exception e) {
        LOGGER.warn("could not parse season number: {} MF: {}", e.getMessage(), mf.getFileAsPath().toAbsolutePath());
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
    setCertification(certification == MediaCertification.NOT_RATED || force ? other.certification : certification);
    setCountry(StringUtils.isEmpty(country) || force ? other.country : country);

    // when force is set, clear the lists/maps and add all other values
    if (force) {
      genres.clear();
      actors.clear();
      extraFanartUrls.clear();

      seasonTitleMap.clear();
      seasonPosterUrlMap.clear();
      seasonBannerUrlMap.clear();
      seasonThumbUrlMap.clear();
    }

    setGenres(other.genres);
    setActors(other.actors);
    setExtraFanartUrls(other.extraFanartUrls);

    for (Integer season : other.seasonTitleMap.keySet()) {
      seasonTitleMap.putIfAbsent(season, other.seasonTitleMap.get(season));
    }
    for (Integer season : other.seasonPosterUrlMap.keySet()) {
      seasonPosterUrlMap.putIfAbsent(season, other.seasonPosterUrlMap.get(season));
    }
    for (Integer season : other.seasonBannerUrlMap.keySet()) {
      seasonBannerUrlMap.putIfAbsent(season, other.seasonBannerUrlMap.get(season));
    }
    for (Integer season : other.seasonThumbUrlMap.keySet()) {
      seasonThumbUrlMap.putIfAbsent(season, other.seasonThumbUrlMap.get(season));
    }

    // get ours, and merge other values
    for (TvShowEpisode ep : episodes) {
      TvShowEpisode otherEP = other.getEpisode(ep.getSeason(), ep.getEpisode()).stream().findFirst().orElse(null);
      ep.merge(otherEP, force);
    }

    // get others, and simply add
    for (TvShowEpisode otherEp : other.getEpisodes()) {
      TvShowEpisode ourEP = getEpisode(otherEp.getSeason(), otherEp.getEpisode()).stream().findFirst().orElse(null); // do not do a contains check!
      if (ourEP == null) {
        TvShowEpisode clone = new TvShowEpisode(otherEp);
        clone.setTvShow(this); // yes!
        addEpisode(clone);
      }
    }
  }

  @Override
  public void addToMediaFiles(MediaFile mediaFile) {
    super.addToMediaFiles(mediaFile);

    // if we have added a trailer, also update the trailer list
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      mixinLocalTrailers();
    }
  }

  @Override
  public void setTitle(String newValue) {
    super.setTitle(newValue);

    String oldValue = this.titleSortable;
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
  public MediaRating getRating() {
    MediaRating mediaRating = ratings.get(TvShowModuleManager.getInstance().getSettings().getPreferredRating());

    if (mediaRating == null) {
      mediaRating = MediaMetadata.EMPTY_RATING;
    }

    return mediaRating;
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
    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      for (TvShowEpisode episode : dummyEpisodes) {
        episode.setTvShow(this);

        if (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isDisplayMissingSpecials()) {
          continue;
        }

        TvShowSeason season = getSeasonForEpisode(episode);

        // also fire the event there was no episode for that dummy yet
        boolean found = false;
        for (TvShowEpisode e : season.getEpisodesForDisplay()) {
          if ((e.getSeason() == episode.getSeason() && e.getEpisode() == episode.getEpisode())
              || (e.getDvdSeason() == episode.getDvdSeason() && e.getDvdEpisode() == episode.getDvdEpisode())) {
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
    if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()) {
      // build up a set which holds a string representing the S/E indicator
      Set<String> availableEpisodes = new HashSet<>();

      for (TvShowEpisode episode : episodes) {
        if (episode.getSeason() > -1 && episode.getEpisode() > -1) {
          availableEpisodes.add("A" + episode.getSeason() + "." + episode.getEpisode());
        }
        if (episode.getDvdSeason() > -1 && episode.getDvdEpisode() > -1) {
          availableEpisodes.add("D" + episode.getSeason() + "." + episode.getEpisode());
        }
      }

      // and now mix in unavailable ones
      for (TvShowEpisode episode : getDummyEpisodes()) {
        if (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isDisplayMissingSpecials()) {
          continue;
        }
        if (!availableEpisodes.contains("A" + episode.getSeason() + "." + episode.getEpisode())
            && !availableEpisodes.contains("D" + episode.getDvdSeason() + "." + episode.getDvdEpisode())) {
          episodes.add(episode);
        }
      }
    }

    return episodes;
  }

  /**
   * get all episodes for the given season
   *
   * @param season
   *          the season to get all episodes for
   * @return a {@link List} of all episodes
   */
  public List<TvShowEpisode> getEpisodesForSeason(int season) {
    return episodes.stream().filter(episode -> episode.getSeason() == season).collect(Collectors.toList());
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
          if (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isDisplayMissingSpecials()) {
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
      String seasonTitle = seasonTitleMap.get(episode.getSeason());
      if (StringUtils.isNotBlank(seasonTitle)) {
        season.setTitle(seasonTitle);
      }
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
    if (!episodes.isEmpty()) {
      for (int i = episodes.size() - 1; i >= 0; i--) {
        TvShowEpisode episode = episodes.get(i);
        removeEpisode(episode);
        TvShowModuleManager.getInstance().getTvShowList().removeEpisodeFromDb(episode);
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
      episode.removePropertyChangeListener(propertyChangeListener);
      removeFromSeason(episode);
      episodes.remove(episode);
      TvShowModuleManager.getInstance().getTvShowList().removeEpisodeFromDb(episode);
      saveToDb();

      firePropertyChange(REMOVED_EPISODE, null, episode);
      firePropertyChange(EPISODE_COUNT, oldValue, episodes.size());

      // and mix in the dummy one again
      // check if there is no other episode available (e.g. exchanged media file)
      if (TvShowModuleManager.getInstance().getSettings().isDisplayMissingEpisodes()
          && ListUtils.isEmpty(getEpisode(episode.getSeason(), episode.getEpisode()))) {
        for (TvShowEpisode dummy : dummyEpisodes) {
          if (dummy.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isDisplayMissingSpecials()) {
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
      episode.removePropertyChangeListener(propertyChangeListener);
      episode.deleteFilesSafely();
      removeFromSeason(episode);
      episodes.remove(episode);
      TvShowModuleManager.getInstance().getTvShowList().removeEpisodeFromDb(episode);
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
   * Adds the given genres
   *
   * @param newGenres
   *          a {@link Collection} with the new genres
   */
  public void addToGenres(Collection<MediaGenres> newGenres) {
    Set<MediaGenres> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (MediaGenres genre : ListUtils.nullSafe(newGenres)) {
      if (genre == null || genres.contains(genre)) {
        continue;
      }
      newItems.add(genre);
    }

    if (newItems.isEmpty()) {
      return;
    }

    genres.addAll(newItems);
    firePropertyChange(GENRE, null, newGenres);
    firePropertyChange(GENRES_AS_STRING, null, newGenres);
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
  public void setMetadata(MediaMetadata metadata, List<TvShowScraperMetadataConfig> config) {
    // check against null metadata (e.g. aborted request)
    if (metadata == null) {
      LOGGER.error("metadata was null");
      return;
    }

    // check if metadata has at least an id (aka it is not empty)
    if (metadata.getIds().isEmpty()) {
      LOGGER.warn("wanted to save empty metadata for {}", getTitle());
      return;
    }

    // populate ids

    // here we have two flavors:
    // a) we did a search, so all existing ids should be different to to new ones -> remove old ones
    // b) we did just a scrape (probably with another scraper). we should have at least one id in the TV show which matches the ids from the metadata
    // ->
    // merge ids

    // search for existing ids
    boolean matchFound = false;
    for (Map.Entry<String, Object> entry : metadata.getIds().entrySet()) {
      if (entry.getValue() != null && entry.getValue().equals(getId(entry.getKey()))) {
        matchFound = true;
        break;
      }
    }

    if (!matchFound) {
      // clear the old ids to set only the new ones
      ids.clear();
    }

    setIds(metadata.getIds());

    if (config.contains(TvShowScraperMetadataConfig.TITLE)) {
      // Capitalize first letter of original title if setting is set!
      if (TvShowModuleManager.getInstance().getSettings().getCapitalWordsInTitles()) {
        setTitle(WordUtils.capitalize(metadata.getTitle()));
      }
      else {
        setTitle(metadata.getTitle());
      }
    }

    if (config.contains(TvShowScraperMetadataConfig.ORIGINAL_TITLE)) {
      // Capitalize first letter of original title if setting is set!
      if (TvShowModuleManager.getInstance().getSettings().getCapitalWordsInTitles()) {
        setOriginalTitle(WordUtils.capitalize(metadata.getOriginalTitle()));
      }
      else {
        setOriginalTitle(metadata.getOriginalTitle());
      }
    }

    if (config.contains(TvShowScraperMetadataConfig.PLOT)) {
      setPlot(metadata.getPlot());
    }

    if (config.contains(TvShowScraperMetadataConfig.YEAR)) {
      setYear(metadata.getYear());
    }

    if (config.contains(TvShowScraperMetadataConfig.RATING)) {
      Map<String, MediaRating> newRatings = new HashMap<>();

      if (matchFound) {
        // only update new ratings, but let the old ones survive
        newRatings.putAll(getRatings());
      }

      for (MediaRating mediaRating : metadata.getRatings()) {
        newRatings.put(mediaRating.getId(), mediaRating);
      }

      setRatings(newRatings);
    }

    if (config.contains(TvShowScraperMetadataConfig.AIRED)) {
      setFirstAired(metadata.getReleaseDate());
    }

    if (config.contains(TvShowScraperMetadataConfig.STATUS)) {
      setStatus(metadata.getStatus());
    }

    if (config.contains(TvShowScraperMetadataConfig.RUNTIME)) {
      setRuntime(metadata.getRuntime());
    }

    if (config.contains(TvShowScraperMetadataConfig.COUNTRY)) {
      setCountry(StringUtils.join(metadata.getCountries(), ", "));
    }

    if (config.contains(TvShowScraperMetadataConfig.STUDIO)) {
      setProductionCompany(StringUtils.join(metadata.getProductionCompanies(), ", "));
    }

    if (config.contains(TvShowScraperMetadataConfig.CERTIFICATION)) {
      if (!metadata.getCertifications().isEmpty()) {
        setCertification(metadata.getCertifications().get(0));
      }
    }

    if (config.contains(TvShowScraperMetadataConfig.ACTORS)) {
      setActors(metadata.getCastMembers(Person.Type.ACTOR));
      writeActorImages();
    }

    if (config.contains(TvShowScraperMetadataConfig.GENRES)) {
      setGenres(metadata.getGenres());
    }

    if (config.contains(TvShowScraperMetadataConfig.TAGS)) {
      removeAllTags();
      addToTags(metadata.getTags());
    }

    if (config.contains(TvShowScraperMetadataConfig.SEASON_NAMES)) {
      // only take _non common_ season names
      for (Map.Entry<Integer, String> entry : metadata.getSeasonNames().entrySet()) {
        Matcher matcher = TvShowEpisodeAndSeasonParser.SEASON_PATTERN.matcher(entry.getValue());
        if (!matcher.find()) {
          seasonTitleMap.put(entry.getKey(), entry.getValue());
        }
      }
    }

    // set scraped
    setScraped(true);

    // update DB
    writeNFO();
    saveToDb();

    postProcess(config);
  }

  /**
   * Sets the artwork.
   *
   * @param artwork
   *          the artwork
   * @param config
   *          the config
   */
  public void setArtwork(List<MediaArtwork> artwork, List<TvShowScraperMetadataConfig> config) {
    TvShowArtworkHelper.setArtwork(this, artwork, config);
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

    List<TvShowNfoNaming> nfoNamings = TvShowModuleManager.getInstance().getSettings().getNfoFilenames();
    if (nfoNamings.isEmpty()) {
      return;
    }

    switch (TvShowModuleManager.getInstance().getSettings().getTvShowConnector()) {
      case XBMC:
      case MEDIAPORTAL:
        connector = new TvShowToXbmcConnector(this);
        break;

      case KODI:
      case JELLYFIN:
      case EMBY:
      case PLEX:
      default:
        connector = new TvShowToKodiConnector(this);
        break;
    }

    connector.write(nfoNamings);

    firePropertyChange(HAS_NFO_FILE, false, true);
  }

  private void postProcess(List<TvShowScraperMetadataConfig> config) {
    TmmTaskChain taskChain = new TmmTaskChain();

    // detect AR of the TV show if that has been chosen in the settings
    if (TvShowModuleManager.getInstance().getSettings().isArdAfterScrape()) {
      taskChain.add(new TvShowARDetectorTask(this.episodes));
    }

    // rename the TV show if that has been chosen in the settings
    if (TvShowModuleManager.getInstance().getSettings().isRenameAfterScrape()) {
      taskChain.add(new TvShowRenameTask(Collections.singletonList(this), null, true));
    }

    // write actor images after possible rename (to have a good folder structure)
    if (ScraperMetadataConfig.containsAnyCast(config) && TvShowModuleManager.getInstance().getSettings().isWriteActorImages()) {
      taskChain.add(new TmmTask(TmmResourceBundle.getString("tvshow.downloadactorimages"), 1, TmmTaskHandle.TaskType.BACKGROUND_TASK) {
        @Override
        protected void doInBackground() {
          writeActorImages();
        }
      });
    }

    taskChain.run();
  }

  /**
   * Gets the checks for nfo file.
   *
   * @return the checks for nfo file
   */
  public Boolean getHasNfoFile() {
    List<MediaFile> nfos = getMediaFiles(MediaFileType.NFO);
    return nfos != null && !nfos.isEmpty();
  }

  /**
   * Gets the checks for trailer.
   *
   * @return the checks for trailer
   */
  public Boolean getHasTrailer() {
    if (trailer != null && !trailer.isEmpty()) {
      return true;
    }

    // check if there is a mediafile (trailer)
    if (!getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
      return true;
    }

    return false;
  }

  /**
   * Check if Tv show has a Music Theme File
   * 
   * @return the check for the musictheme file
   */
  public Boolean getHasMusicTheme() {
    return (!getMediaFiles(MediaFileType.THEME).isEmpty());
  }

  /**
   * Gets the check mark for images. What to be checked is configurable
   *
   * @return the checks for images
   */
  public Boolean getHasImages() {
    for (MediaArtworkType type : TvShowModuleManager.getInstance().getSettings().getTvShowCheckImages()) {
      if (StringUtils.isBlank(getArtworkFilename(MediaFileType.getMediaFileType(type)))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if all seasons and episodes of that TV show have artwork assigned
   *
   * @return true if artwork is available
   */
  public Boolean getHasSeasonAndEpisodeImages() {
    for (TvShowSeason season : seasons) {
      if (!season.getHasImages() || !season.getHasEpisodeImages()) {
        return false;
      }
    }
    return true;
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
   * Sets the TraktTv id.
   *
   * @param newValue
   *          the new TraktTV id
   */
  public void setTraktId(int newValue) {
    this.setId(TRAKT, newValue);
  }

  /**
   * Gets the TMDB id.
   *
   * @return the TTMDB id
   */
  public int getTmdbId() {
    return this.getIdAsInt(TMDB);
  }

  /**
   * Sets the TMDB id.
   *
   * @param newValue
   *          the new TMDB id
   */
  public void setTmdbId(int newValue) {
    this.setId(TMDB, newValue);
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
    return TmmDateFormat.MEDIUM_DATE_FORMAT.format(firstAired);
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
   * adds the given actors
   *
   * @param newActors
   *          a {@link Collection} of all actors to be added
   */
  public void addToActors(Collection<Person> newActors) {
    Set<Person> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (Person person : ListUtils.nullSafe(newActors)) {
      if (person == null || actors.contains(person)) {
        continue;
      }
      if (person.getType() != Person.Type.ACTOR) {
        return;
      }

      newItems.add(person);
    }

    if (newItems.isEmpty()) {
      return;
    }

    actors.addAll(newItems);
    firePropertyChange(ACTORS, null, actors);
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
    mergePersons(actors, newActors);
    firePropertyChange(ACTORS, null, this.getActors());
  }

  /**
   * Gets the certifications.
   *
   * @return the certifications
   */
  @Override
  public MediaCertification getCertification() {
    return certification;
  }

  /**
   * Sets the certifications.
   *
   * @param newValue
   *          the new certifications
   */
  public void setCertification(MediaCertification newValue) {
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
    boolean episodeFound = false;
    boolean watched = true;

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy()) {
        episodeFound = true;
        watched = watched && episode.isWatched();
      }
    }

    // at least 1 non-dummy found -> pass the collected watched state
    if (episodeFound) {
      return watched;
    }

    // only dummy episodes -> return false
    return false;
  }

  /**
   * checks if all episode has subtitles
   *
   * @return true, is all episodes have subtitles
   */
  public boolean hasEpisodeSubtitles() {
    boolean subtitles = true;

    for (TvShowEpisode episode : episodes) {
      if (!episode.getHasSubtitles()) {
        subtitles = false;
        break;
      }
    }

    return subtitles;
  }

  public Date getLastWatched() {
    return lastWatched;
  }

  public void setLastWatched(Date lastWatched) {
    this.lastWatched = lastWatched;
  }

  /**
   * Gets the trailers
   *
   * @return the trailers
   */
  public List<MediaTrailer> getTrailer() {
    return this.trailer;
  }

  /**
   * Adds the trailer.
   *
   * @param newTrailers
   *          a {@link Collection} of trailers to be added
   */
  public void addToTrailer(Collection<MediaTrailer> newTrailers) {
    Set<MediaTrailer> newItems = new LinkedHashSet<>();

    // do not accept duplicates or null values
    for (MediaTrailer trailer : ListUtils.nullSafe(newTrailers)) {
      if (trailer == null || this.trailer.contains(trailer)) {
        continue;
      }
      newItems.add(trailer);
    }

    if (newItems.isEmpty()) {
      return;
    }

    trailer.addAll(newItems);
    firePropertyChange(TRAILER, null, trailer);
  }

  /**
   * Removes the all trailers.
   */
  public void removeAllTrailers() {
    trailer.clear();
    firePropertyChange(TRAILER, null, trailer);
  }

  public void setTrailers(List<MediaTrailer> trailers) {
    MediaTrailer preferredTrailer = null;
    removeAllTrailers();

    List<MediaTrailer> newItems = new ArrayList<>();

    // set preferred trailer
    if (TvShowModuleManager.getInstance().getSettings().isUseTrailerPreference()) {
      TrailerQuality desiredQuality = TvShowModuleManager.getInstance().getSettings().getTrailerQuality();
      TrailerSources desiredSource = TvShowModuleManager.getInstance().getSettings().getTrailerSource();

      // search for quality and provider
      for (MediaTrailer trailer : trailers) {
        if (desiredQuality.containsQuality(trailer.getQuality()) && desiredSource.containsSource(trailer.getProvider())) {
          trailer.setInNfo(Boolean.TRUE);
          preferredTrailer = trailer;
          break;
        }
      }

      // search for quality
      if (preferredTrailer == null) {
        for (MediaTrailer trailer : trailers) {
          if (desiredQuality.containsQuality(trailer.getQuality())) {
            trailer.setInNfo(Boolean.TRUE);
            preferredTrailer = trailer;
            break;
          }
        }
      }

      // if not yet one has been found; sort by quality descending and take the first one which is lower or equal to the desired quality
      if (preferredTrailer == null) {
        List<MediaTrailer> sortedTrailers = new ArrayList<>(trailers);
        sortedTrailers.sort(TRAILER_QUALITY_COMPARATOR);
        for (MediaTrailer trailer : sortedTrailers) {
          if (desiredQuality.ordinal() >= TrailerQuality.getTrailerQuality(trailer.getQuality()).ordinal()) {
            trailer.setInNfo(Boolean.TRUE);
            preferredTrailer = trailer;
            break;
          }
        }
      }
    } // end if MovieModuleManager.getInstance().getSettings().isUseTrailerPreference()

    // if not yet one has been found; sort by quality descending and take the first one
    if (preferredTrailer == null && !trailers.isEmpty()) {
      List<MediaTrailer> sortedTrailers = new ArrayList<>(trailers);
      sortedTrailers.sort(TRAILER_QUALITY_COMPARATOR);
      preferredTrailer = sortedTrailers.get(0);
      preferredTrailer.setInNfo(Boolean.TRUE);
    }

    // add trailers
    if (preferredTrailer != null) {
      newItems.add(preferredTrailer);
    }
    for (MediaTrailer trailer : trailers) {
      // preferred trailer has already been added
      if (preferredTrailer != null && preferredTrailer == trailer) {
        continue;
      }

      // if still no preferred trailer has been set, then mark the first one
      if (preferredTrailer == null && this.trailer.isEmpty() && !trailer.getUrl().startsWith("file")) {
        trailer.setInNfo(Boolean.TRUE);
      }

      newItems.add(preferredTrailer);
    }

    addToTrailer(newItems);

    // mix in local trailers
    mixinLocalTrailers();
  }

  /**
   * all supported TRAILER names. (without path, without extension!)
   *
   * @param trailer
   *          trailer naming enum
   * @return the associated trailer filename
   */
  public String getTrailerFilename(TvShowTrailerNaming trailer) {
    // basename is the TV show title itself
    return FilenameUtils.removeExtension(trailer.getFilename(getTitle(), "ext"));
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

  /**
   * <b>PHYSICALLY</b> deletes all {@link MediaFile}s of the given type for the given season
   *
   * @param artworkType
   *          the {@link MediaArtworkType} for all {@link MediaFile}s to delete
   */
  public void deleteSeasonArtworkFiles(int season, MediaArtworkType artworkType) {
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
      mf.deleteSafely(getDataSource());
      removeFromMediaFiles(mf);
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
  @Override
  public List<MediaFile> getImagesToCache() {
    // get files to cache
    List<MediaFile> filesToCache = new ArrayList<>();

    for (MediaFile mf : getMediaFiles()) {
      if (mf.isGraphic()) {
        filesToCache.add(mf);
      }
    }

    if (TvShowModuleManager.getInstance().getSettings().isWriteActorImages()) {
      filesToCache.addAll(listActorFiles());
    }

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
    TvShowModuleManager.getInstance().getTvShowList().persistTvShow(this);
  }

  @Override
  public void deleteFromDb() {
    // remove this TV show from the database
    TvShowModuleManager.getInstance().getTvShowList().removeTvShow(this);
  }

  public List<TvShowEpisode> getEpisode(final int season, final int episode) {
    if (season == -1 || episode == -1) {
      return Collections.emptyList();
    }

    return this.episodes.stream().filter(e -> e.getSeason() == season && e.getEpisode() == episode).collect(Collectors.toList());
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
      if (StringUtils.isNotBlank(plot) && year > 0 && ListUtils.isNotEmpty(genres) && ListUtils.isNotEmpty(actors)) {
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
    if (!TvShowModuleManager.getInstance().getSettings().isWriteActorImages()) {
      return;
    }

    TvShowActorImageFetcherTask task = new TvShowActorImageFetcherTask(this);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
  }

  /**
   * add a season title to the internal season title map
   *
   * @param season
   *          the season to set the title for
   * @param title
   *          the title
   */
  public void addSeasonTitle(int season, String title) {
    if (StringUtils.isNotBlank(title)) {
      seasonTitleMap.put(season, title);
    }
    else {
      seasonTitleMap.remove(season);
    }

    firePropertyChange("seasonTitle", null, seasonTitleMap);
  }

  /**
   * get a map containing all set season titles
   *
   * @return a map containing all season titles
   */
  public Map<Integer, String> getSeasonTitles() {
    return seasonTitleMap;
  }

  /**
   * @return list of actor images on filesystem
   */
  private List<MediaFile> listActorFiles() {
    if (!Files.exists(getPathNIO().resolve(Person.ACTOR_DIR))) {
      return Collections.emptyList();
    }

    List<MediaFile> fileNames = new ArrayList<>();

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(getPathNIO().resolve(Person.ACTOR_DIR))) {
      for (Path path : directoryStream) {
        if (Utils.isRegularFile(path)) {
          // only get graphics
          MediaFile mf = new MediaFile(path);
          if (mf.isGraphic()) {
            fileNames.add(mf);
          }
        }
      }
    }
    catch (IOException e) {
      LOGGER.debug("Cannot get actors: {}", getPathNIO().resolve(Person.ACTOR_DIR));
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
  public MediaFile getMainFile() {
    return getMainVideoFile();
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
    return Collections.emptyList();
  }

  @Override
  public String getMediaInfoAudioChannels() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioChannelList() {
    return Collections.emptyList();
  }

  @Override
  public String getMediaInfoAudioLanguage() {
    return "";
  }

  @Override
  public List<String> getMediaInfoAudioLanguageList() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getMediaInfoSubtitleLanguageList() {
    return Collections.emptyList();
  }

  @Override
  public String getMediaInfoContainerFormat() {
    return "";
  }

  @Override
  public int getMediaInfoVideoBitDepth() {
    return 0;
  }

  @Override
  public MediaSource getMediaInfoSource() {
    return MediaSource.UNKNOWN;
  }

  @Override
  public long getVideoFilesize() {
    long filesize = 0;
    for (TvShowEpisode episode : episodes) {
      for (MediaFile mf : episode.getMediaFiles(MediaFileType.VIDEO)) {
        filesize += mf.getFilesize();
      }
    }
    return filesize;
  }

  @Override
  protected void fireAddedEventForMediaFile(MediaFile mediaFile) {
    super.fireAddedEventForMediaFile(mediaFile);

    // TV show related media file types
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      firePropertyChange(TRAILER, false, true);
    }
  }

  @Override
  protected void fireRemoveEventForMediaFile(MediaFile mediaFile) {
    super.fireRemoveEventForMediaFile(mediaFile);

    // TV show related media file types
    if (mediaFile.getType() == MediaFileType.TRAILER) {
      firePropertyChange(TRAILER, true, false);
    }
  }

  private void mixinLocalTrailers() {
    // remove local ones
    for (int i = trailer.size() - 1; i >= 0; i--) {
      MediaTrailer mediaTrailer = trailer.get(i);
      if ("downloaded".equalsIgnoreCase(mediaTrailer.getProvider())) {
        trailer.remove(i);
      }
    }

    // add local trailers (in the front)!
    for (MediaFile mf : getMediaFiles(MediaFileType.TRAILER)) {
      LOGGER.debug("adding local trailer {}", mf.getFilename());
      MediaTrailer mt = new MediaTrailer();
      mt.setName(mf.getFilename());
      mt.setProvider("downloaded");
      mt.setQuality(mf.getVideoFormat());
      mt.setInNfo(false);
      mt.setUrl(mf.getFile().toUri().toString());
      trailer.add(0, mt);
      firePropertyChange(TRAILER, null, trailer);
    }
  }

  @Override
  public void callbackForGatheredMediainformation(MediaFile mediaFile) {
    super.callbackForGatheredMediainformation(mediaFile);

    if (mediaFile.getType() == MediaFileType.TRAILER) {
      // re-write the trailer list
      mixinLocalTrailers();
    }
  }
}
