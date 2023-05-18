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
package org.tinymediamanager.thirdparty.trakttv;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.tvshow.entities.TvShow;

/**
 * Sync your data with trakt.tv
 * 
 * @author Manuel Laggner
 */
public class TvShowSyncTraktTvTask extends TmmTask {
  private static final Logger LOGGER         = LoggerFactory.getLogger(TvShowSyncTraktTvTask.class);

  private final List<TvShow>  tvShows        = new ArrayList<>();

  private boolean             syncCollection = false;
  private boolean             syncWatched    = false;
  private boolean             syncRating     = false;

  public TvShowSyncTraktTvTask(List<TvShow> tvShows) {
    super(TmmResourceBundle.getString("trakt.sync"), 0, TaskType.BACKGROUND_TASK);
    this.tvShows.addAll(tvShows);
  }

  public void setSyncCollection(boolean value) {
    this.syncCollection = value;
  }

  public void setSyncWatched(boolean value) {
    this.syncWatched = value;
  }

  public void setSyncRating(boolean value) {
    this.syncRating = value;
  }

  @Override
  protected void doInBackground() {
    if (!isFeatureEnabled()) {
      return;
    }

    TraktTv traktTV = TraktTv.getInstance();

    if (syncCollection) {
      publishState(TmmResourceBundle.getString("trakt.sync.tvshow"), 0);
      try {
        traktTV.syncTraktTvShowCollection(tvShows);
      }
      catch (Exception e) {
        LOGGER.error("Could not sync to trakt - '{}'", e.getMessage());
      }
    }

    if (syncWatched) {
      publishState(TmmResourceBundle.getString("trakt.sync.tvshowwatched"), 0);
      try {
        traktTV.syncTraktTvShowWatched(tvShows);
      }
      catch (Exception e) {
        LOGGER.error("Could not sync to trakt - '{}'", e.getMessage());
      }
    }

    if (syncRating) {
      publishState(TmmResourceBundle.getString("trakt.sync.tvshowrating"), 0);
      try {
        traktTV.syncTraktTvShowRating(tvShows);
      }
      catch (Exception e) {
        LOGGER.error("Could not sync to trakt - '{}'", e.getMessage());
      }
    }
  }
}
