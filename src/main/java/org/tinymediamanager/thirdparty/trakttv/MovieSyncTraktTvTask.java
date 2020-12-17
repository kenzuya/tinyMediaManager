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
package org.tinymediamanager.thirdparty.trakttv;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTask;

/**
 * Sync your data with trakt.tv
 * 
 * @author Manuel Laggner
 */
public class MovieSyncTraktTvTask extends TmmTask {
  private static final ResourceBundle BUNDLE         = ResourceBundle.getBundle("messages");

  private boolean                     syncCollection = false;
  private boolean                     syncWatched    = false;
  private final List<Movie>           movies         = new ArrayList<>();

  public MovieSyncTraktTvTask(List<Movie> movies) {
    super(BUNDLE.getString("trakt.sync"), 0, TaskType.BACKGROUND_TASK);
    this.movies.addAll(movies);
  }

  public void setSyncCollection(boolean value) {
    this.syncCollection = value;
  }

  public void setSyncWatched(boolean value) {
    this.syncWatched = value;
  }

  @Override
  protected void doInBackground() {
    TraktTv traktTV = TraktTv.getInstance();

    if (syncCollection) {
      publishState(BUNDLE.getString("trakt.sync.movie"), 0);
      traktTV.syncTraktMovieCollection(movies);
    }

    if (syncWatched) {
      publishState(BUNDLE.getString("trakt.sync.moviewatched"), 0);
      traktTV.syncTraktMovieWatched(movies);
    }
  }
}
