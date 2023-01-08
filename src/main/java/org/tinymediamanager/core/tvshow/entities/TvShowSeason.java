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
package org.tinymediamanager.core.tvshow.entities;

import static org.tinymediamanager.core.Constants.ADDED_EPISODE;
import static org.tinymediamanager.core.Constants.FIRST_AIRED;
import static org.tinymediamanager.core.Constants.HAS_NFO_FILE;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.REMOVED_EPISODE;
import static org.tinymediamanager.core.Constants.SEASON;
import static org.tinymediamanager.core.Constants.TV_SHOW;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.connector.ITvShowSeasonConnector;
import org.tinymediamanager.core.tvshow.connector.TvShowSeasonToEmbyConnector;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonNfoNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Class TvShowSeason.
 * 
 * @author Manuel Laggner
 */
public class TvShowSeason extends MediaEntity implements Comparable<TvShowSeason> {

  @JsonProperty
  private UUID                      tvShowDbId = null;

  @JsonProperty
  private final int                 season;

  private TvShow                    tvShow     = null;
  private final List<TvShowEpisode> episodes   = new CopyOnWriteArrayList<>();

  private PropertyChangeListener    listener;

  /**
   * Instantiates a new tv show episode. To initialize the propertychangesupport after loading
   */
  public TvShowSeason(@JsonProperty("season") int season) {
    // register for dirty flag listener
    super();

    this.season = season;

    init();
  }

  public TvShowSeason(int season, @NotNull TvShow tvShow) {
    this(season);

    this.tvShow = tvShow;
    this.tvShowDbId = tvShow.getDbId();
  }

  /**
   * copy constructor
   * 
   * @param source
   *          the other {@link TvShowSeason} to copy
   */
  public TvShowSeason(TvShowSeason source) {
    super.merge(source);

    this.tvShow = source.tvShow;
    this.tvShowDbId = source.tvShowDbId;

    this.season = source.season;

    init();
  }

  private void init() {
    listener = evt -> {
      if (evt.getSource()instanceof TvShowEpisode episode) {
        switch (evt.getPropertyName()) {
          case MEDIA_FILES -> firePropertyChange(MEDIA_FILES, null, evt.getNewValue());
          case SEASON -> {
            if (episode.getSeason() != season) {
              removeEpisode(episode);
            }
          }
          case FIRST_AIRED -> firePropertyChange(FIRST_AIRED, null, evt.getNewValue());
        }
      }
    };
  }

  public int getSeason() {
    return season;
  }

  @Override
  public MediaFile getMainFile() {
    return MediaFile.EMPTY_MEDIAFILE;
  }

  /**
   * checks whether the parent {@link TvShow} is locked
   * 
   * @return true/false
   */
  @Override
  public boolean isLocked() {
    return getTvShow() == null || getTvShow().isLocked();
  }

  @Override
  public MediaRating getRating() {
    return MediaMetadata.EMPTY_RATING;
  }

  public void setTvShow(TvShow newValue) {
    TvShow oldValue = this.tvShow;
    this.tvShow = newValue;
    this.tvShowDbId = newValue.getDbId();
    firePropertyChange(TV_SHOW, oldValue, newValue);
  }

  public TvShow getTvShow() {
    return tvShow;
  }

  public UUID getTvShowDbId() {
    return tvShowDbId;
  }

  public synchronized void addEpisode(TvShowEpisode episode) {
    // do not add twice
    if (episode == null || episodes.contains(episode)) {
      return;
    }

    // when adding a new episode, check:
    for (TvShowEpisode e : episodes) {
      // - if that is a dummy episode; do not add it if a the real episode is available
      if (episode.isDummy() && ((episode.getEpisode() == e.getEpisode() && episode.getSeason() == e.getSeason())
          || (episode.getDvdEpisode() > 0 && episode.getDvdEpisode() == e.getDvdEpisode() && episode.getDvdSeason() == e.getDvdSeason()))) {
        return;
      }
      // - if that is a real episode; remove the corresponding dummy episode if available
      if (!episode.isDummy() && e.isDummy() && ((episode.getEpisode() == e.getEpisode() && episode.getSeason() == e.getSeason())
          || (episode.getDvdEpisode() > 0 && episode.getDvdEpisode() == e.getDvdEpisode() && episode.getDvdSeason() == e.getDvdSeason()))) {
        tvShow.removeEpisode(e);
      }
    }

    episodes.add(episode);
    episodes.sort(TvShowEpisode::compareTo);
    episode.addPropertyChangeListener(listener);
    firePropertyChange(ADDED_EPISODE, null, episodes);
    firePropertyChange(FIRST_AIRED, null, getFirstAired());
  }

  public void removeEpisode(TvShowEpisode episode) {
    episodes.remove(episode);
    episode.removePropertyChangeListener(listener);
    firePropertyChange(REMOVED_EPISODE, null, episodes);
    firePropertyChange(FIRST_AIRED, null, getFirstAired());
  }

  public List<TvShowEpisode> getEpisodes() {
    return this.episodes.stream().filter(episode -> !episode.isDummy()).toList();
  }

  /**
   * get the firstAired of the first episode here
   * 
   * @return the first aired date of the first episode or null
   */
  public Date getFirstAired() {
    Date firstAired = null;

    for (TvShowEpisode episode : episodes) {
      if (firstAired == null) {
        firstAired = episode.getFirstAired();
      }
      else if (episode.getFirstAired() != null && firstAired.after(episode.getFirstAired())) {
        firstAired = episode.getFirstAired();
      }
    }

    return firstAired;
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
      if (!episode.isDummy() && !episode.getHasSubtitles()) {
        subtitles = false;
        break;
      }
    }

    return subtitles;
  }

  /**
   * Checks if all episodes of that season have artwork assigned
   *
   * @return true if artwork is available
   */
  public Boolean getHasEpisodeImages() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy() && !tvShowList.detectMissingArtwork(episode).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if all episodes of that season have valis metadata
   *
   * @return true if all chosen metadata fields are filled
   */
  public Boolean getHasEpisodeMetadata() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy() && !tvShowList.detectMissingMetadata(episode).isEmpty()) {
        return false;
      }
    }

    return true;
  }

  public boolean isDummy() {
    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy()) {
        return false;
      }
    }
    return true;
  }

  public List<TvShowEpisode> getEpisodesForDisplay() {
    return episodes;
  }

  public long getVideoFilesize() {
    long filesize = 0;
    for (TvShowEpisode episode : episodes) {
      filesize += episode.getVideoFilesize();
    }
    return filesize;
  }

  @Override
  public long getTotalFilesize() {
    long filesize = 0;
    for (TvShowEpisode episode : episodes) {
      filesize += episode.getTotalFilesize();
    }
    return filesize;
  }

  @Override
  public List<MediaFile> getMediaFiles() {
    Set<MediaFile> unique = new LinkedHashSet<>(super.getMediaFiles());

    for (TvShowEpisode episode : episodes) {
      unique.addAll(episode.getMediaFiles());
    }

    return new ArrayList<>(unique);
  }

  public List<MediaFile> getMediaFiles(MediaFileType type) {
    Set<MediaFile> unique = new LinkedHashSet<>(super.getMediaFiles(type));

    for (TvShowEpisode episode : episodes) {
      unique.addAll(episode.getMediaFiles(type));
    }

    return new ArrayList<>(unique);
  }

  @Override
  public boolean isNewlyAdded() {
    for (TvShowEpisode episode : episodes) {
      if (episode.isNewlyAdded()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void saveToDb() {
    TvShowModuleManager.getInstance().getTvShowList().persistSeason(this);
  }

  public void writeNfo() {
    ITvShowSeasonConnector connector;

    // only write the NFO if there is at least one episode existing (or the setting activated)
    if (!episodes.isEmpty() || TvShowModuleManager.getInstance().getSettings().isCreateMissingSeasonItems()) {
      List<TvShowSeasonNfoNaming> nfoNamings = TvShowModuleManager.getInstance().getSettings().getSeasonNfoFilenames();
      if (!nfoNamings.isEmpty()) {
        connector = switch (TvShowModuleManager.getInstance().getSettings().getTvShowConnector()) {
          default -> new TvShowSeasonToEmbyConnector(this);
        };

        connector.write(nfoNamings);

        firePropertyChange(HAS_NFO_FILE, false, true);
      }
    }
  }

  @Override
  public void callbackForWrittenArtwork(MediaArtworkType type) {
    // nothing to do
  }

  @Override
  protected Comparator<MediaFile> getMediaFileComparator() {
    return null;
  }

  /**
   * Gets the images to cache.
   *
   * @return the images to cache
   */
  @Override
  public List<MediaFile> getImagesToCache() {
    // get files to cache
    return getMediaFiles().stream().filter(MediaFile::isGraphic).toList();
  }

  public Object getValueForMetadata(TvShowScraperMetadataConfig metadataConfig) {
    return switch (metadataConfig) {
      case TITLE -> getTitle();
      case SEASON_POSTER -> getArtworkFilename(MediaFileType.SEASON_POSTER);
      case SEASON_FANART -> getArtworkFilename(MediaFileType.SEASON_FANART);
      case SEASON_BANNER -> getArtworkFilename(MediaFileType.SEASON_BANNER);
      case SEASON_THUMB -> getArtworkFilename(MediaFileType.SEASON_THUMB);
      default -> null;
    };

  }

  @Override
  public int compareTo(TvShowSeason o) {
    if (getTvShow() != o.getTvShow()) {
      return getTvShow().getTitle().compareTo(o.getTvShow().getTitle());
    }
    return Integer.compare(getSeason(), o.getSeason());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TvShowSeason that = (TvShowSeason) o;
    return season == that.season && Objects.equals(tvShow, that.tvShow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tvShow, season);
  }
}
