/*
 * Copyright 2012 - 2024 Manuel Laggner
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
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * Sync your data with trakt.tv
 * 
 * @author Manuel Laggner
 */
public class MovieSyncTraktTvTask extends TmmTask {
  private static final Logger LOGGER         = LoggerFactory.getLogger(MovieSyncTraktTvTask.class);

  private final List<Movie>   movies         = new ArrayList<>();

  private boolean             syncCollection = false;
  private boolean             syncWatched    = false;
  private boolean             syncRating     = false;

  public MovieSyncTraktTvTask(List<Movie> movies) {
    super(TmmResourceBundle.getString("trakt.sync"), 0, TaskType.BACKGROUND_TASK);
    this.movies.addAll(movies);
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

    // check if there is a need to sync
    // without _any_ scraped movies no sync is needed
    if (!syncNeeded(movies)) {
      return;
    }

    TraktTv traktTV = TraktTv.getInstance();

    if (syncCollection) {
      publishState(TmmResourceBundle.getString("trakt.sync.movie"), 0);
      try {
        traktTV.syncTraktMovieCollection(movies);
      }
      catch (Exception e) {
        LOGGER.error("Could not sync to trakt - '{}'", e.getMessage());
      }
    }

    if (syncWatched) {
      publishState(TmmResourceBundle.getString("trakt.sync.moviewatched"), 0);
      try {
        traktTV.syncTraktMovieWatched(movies);
      }
      catch (Exception e) {
        LOGGER.error("Could not sync to trakt - '{}'", e.getMessage());
      }
    }

    if (syncRating) {
      publishState(TmmResourceBundle.getString("trakt.sync.movierating"), 0);
      try {
        traktTV.syncTraktMovieRating(movies);
      }
      catch (Exception e) {
        LOGGER.error("Could not sync to trakt - '{}'", e.getMessage());
      }
    }
  }

  private boolean syncNeeded(List<Movie> movies) {
    for (Movie movie : movies) {
      if (movie.getTmdbId() > 0) {
        return true;
      }
      if (movie.getTraktId() > 0) {
        return true;
      }
      if (MediaIdUtil.isValidImdbId(movie.getImdbId())) {
        return true;
      }
    }

    return false;
  }
}
