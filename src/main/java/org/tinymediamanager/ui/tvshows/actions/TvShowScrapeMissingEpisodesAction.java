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

package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.tasks.TvShowScrapeTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowScrapeMissingEpisodesAction. To update the information of missing episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowScrapeMissingEpisodesAction extends TmmAction {
  public TvShowScrapeMissingEpisodesAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.scrape.missingepisodes"));
    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
    putValue(SMALL_ICON, IconManager.SEARCH);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShowsRecursive();

    if (selectedTvShows.isEmpty()) {
      return;
    }

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.loadDefaults();

    TvShowScrapeTask.TvShowScrapeParams tvShowScrapeParams = new TvShowScrapeTask.TvShowScrapeParams(selectedTvShows, options, new ArrayList<>(),
        new ArrayList<>());
    tvShowScrapeParams.setDoSearch(false);

    TmmThreadPool scrapeTask = new TvShowScrapeTask(tvShowScrapeParams);
    TmmTaskManager.getInstance().addMainTask(scrapeTask);
  }
}
