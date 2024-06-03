/*
 * Copyright 2012 - 2020 Manuel Laggner
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.UpgradeTasks;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;

/**
 * the class {@link TvShowUpgradeTasks} is used to perform actions on {@link TvShow}s, {@link TvShowSeason}s and {@link TvShowEpisode}s
 *
 * @author Manuel Laggner
 */
public class TvShowUpgradeTasks extends UpgradeTasks {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowUpgradeTasks.class);

  public TvShowUpgradeTasks() {
    super();
  }

  /**
   * Each DB version can only be executed once!<br>
   * Do not make changes to existing versions, use a new number!
   */
  @Override
  public void performDbUpgrades() {
    TvShowModuleManager module = TvShowModuleManager.getInstance();
    TvShowList tvShowList = module.getTvShowList();
    if (module.getDbVersion() == 0) {
      module.setDbVersion(5000);
    }

    LOGGER.info("Current tvshow DB version: {}", module.getDbVersion());

    if (module.getDbVersion() < 5001) {
      LOGGER.info("performing upgrade to ver: {}", 5001);
      for (TvShow tvShow : tvShowList.getTvShows()) {
        // migrate logo to clearlogo
        for (MediaFile mf : tvShow.getMediaFiles(MediaFileType.LOGO)) {
          // remove
          tvShow.removeFromMediaFiles(mf);
          // change type
          mf.setType(MediaFileType.CLEARLOGO);
          // and add ad the end
          tvShow.addToMediaFiles(mf);
        }

        String logoUrl = tvShow.getArtworkUrl(MediaFileType.LOGO);
        if (StringUtils.isNotBlank(logoUrl)) {
          tvShow.removeArtworkUrl(MediaFileType.LOGO);
          String clearlogoUrl = tvShow.getArtworkUrl(MediaFileType.CLEARLOGO);
          if (StringUtils.isBlank(clearlogoUrl)) {
            tvShow.setArtworkUrl(logoUrl, MediaFileType.CLEARLOGO);
          }
        }

        // migrate season artwork to the seasons
        List<MediaFile> seasonMediaFiles = tvShow.getMediaFiles(MediaFileType.SEASON_POSTER, MediaFileType.SEASON_BANNER, MediaFileType.SEASON_THUMB,
            MediaFileType.SEASON_FANART);
        for (MediaFile mf : seasonMediaFiles) {
          if (mf.getFilesize() != 0) {
            String foldername = tvShow.getPathNIO().relativize(mf.getFileAsPath().getParent()).toString();
            int season = TvShowHelpers.detectSeasonFromFileAndFolder(mf.getFilename(), foldername);
            if (season != Integer.MIN_VALUE) {
              TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(season);
              tvShowSeason.addToMediaFiles(mf);
            }
          }
          tvShow.removeFromMediaFiles(mf);
        }

        // link TV shows and seasons once again
        for (TvShowSeason season : tvShow.getSeasons()) {
          season.setTvShow(tvShow);
        }
        // save episodes (they are migrated while loading from database)
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          registerForSaving(episode);
        }

        registerForSaving(tvShow);
      }
      module.setDbVersion(5001);
    }

    // migrating EpisodeGroups from V4 / nightly V5
    if (module.getDbVersion() < 5002) {
      LOGGER.info("performing upgrade to ver: {}", 5002);
      for (TvShow tvShow : tvShowList.getTvShows()) {
        if (tvShow.getEpisodeGroup() == null || (tvShow.getEpisodeGroup() != null && tvShow.getEpisodeGroup().getEpisodeGroupType() == null)) {
          // v4 empty / old v5 - cannot read
          tvShow.setEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);
        }
        tvShow.getEpisodes().forEach(this::registerForSaving); // re-save
        registerForSaving(tvShow);
      } // end foreach show

      module.setDbVersion(5002);
    }

    // migrate folder.ext in TV shows to seasons
    if (module.getDbVersion() < 5003) {
      for (TvShow tvShow : tvShowList.getTvShows()) {
        List<MediaFile> toRemove = new ArrayList<>();

        for (MediaFile mf : tvShow.getMediaFiles(MediaFileType.POSTER)) {
          if (!mf.getPath().equals(tvShow.getPath())) {
            // probably season poster
            mf.setType(MediaFileType.SEASON_POSTER);

            String foldername = tvShow.getPathNIO().relativize(mf.getFileAsPath().getParent()).toString();
            int season = TvShowHelpers.detectSeasonFromFileAndFolder(mf.getFilename(), foldername);
            if (season != Integer.MIN_VALUE) {
              TvShowSeason tvShowSeason = tvShow.getOrCreateSeason(season);
              tvShowSeason.addToMediaFiles(mf);
              toRemove.add(mf);
            }
          }
        } // end foreach mediafile

        if (!toRemove.isEmpty()) {
          toRemove.forEach(tvShow::removeFromMediaFiles);
          registerForSaving(tvShow);
        }
      } // end foreach show
      module.setDbVersion(5003);
    }

    // fix ratings
    if (module.getDbVersion() < 5004) {
      LOGGER.info("performing upgrade to ver: {}", 5004);
      for (TvShow tvShow : tvShowList.getTvShows()) {
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          if (fixRatings(episode)) {
            registerForSaving(episode);
          }
        }
        if (fixRatings(tvShow)) {
          registerForSaving(tvShow);
        }
      }
      module.setDbVersion(5004);
    }

    saveAll();
  }

  @Override
  protected void saveAll() {
    for (MediaEntity mediaEntity : entitiesToSave) {
      if (mediaEntity instanceof TvShow tvShow) {
        TvShowModuleManager.getInstance().persistTvShow(tvShow);
      }
      else if (mediaEntity instanceof TvShowSeason season) {
        TvShowModuleManager.getInstance().persistSeason(season);
      }
      else if (mediaEntity instanceof TvShowEpisode episode) {
        TvShowModuleManager.getInstance().persistEpisode(episode);
      }
    }
  }
}
