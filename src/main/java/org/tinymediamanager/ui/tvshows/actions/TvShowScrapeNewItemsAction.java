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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.KeyStroke;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.tasks.TvShowEpisodeScrapeTask;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowChooserDialog;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowScrapeMetadataDialog;

/**
 * The class TvShowScrapeNewItemsAction. Scrape all new items
 *
 * @author Manuel Laggner
 */
public class TvShowScrapeNewItemsAction extends TmmAction {
  private static final long serialVersionUID = -3365542777082781952L;

  public TvShowScrapeNewItemsAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.scrape.newitems"));
    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
    putValue(SMALL_ICON, IconManager.SEARCH);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShow> newTvShows = new ArrayList<>();
    Map<TvShow, List<TvShowEpisode>> newEpisodes = new HashMap<>();

    // all new TV shows
    for (TvShow tvShow : new ArrayList<>(TvShowList.getInstance().getTvShows())) {
      // if there is at least one new episode and no scraper id we assume the TV show is new
      if (tvShow.isNewlyAdded() && !tvShow.isScraped()) {
        newTvShows.add(tvShow);
        continue;
      }

      // else: check every episode if there is a new episode
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        if (episode.isNewlyAdded() && !episode.isScraped()) {
          List<TvShowEpisode> episodes = newEpisodes.computeIfAbsent(tvShow, k -> new ArrayList<>());
          episodes.add(episode);
        }
      }
    }

    // whereas tv show scraping has to run in foreground
    if (!newTvShows.isEmpty()) {
      int count = newTvShows.size();
      int index = 0;

      do {
        TvShow tvShow = newTvShows.get(index);
        TvShowChooserDialog chooser = new TvShowChooserDialog(tvShow, index, count);
        chooser.setVisible(true);

        if (!chooser.isContinueQueue()) {
          break;
        }

        if (chooser.isNavigateBack()) {
          index -= 1;
        }
        else {
          index += 1;
        }

      } while (index < count);
    }

    // scrape new episodes
    for (Map.Entry<TvShow, List<TvShowEpisode>> entry : newEpisodes.entrySet()) {
      TvShow tvShow = entry.getKey();

      // so for the known ones, we can directly start scraping
      if (StringUtils.isNoneBlank(tvShow.getLastScraperId(), tvShow.getLastScrapeLanguage())) {
        // automatically scrape
        TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
        options.loadDefaults();
        options.setMetadataScraper(MediaScraper.getMediaScraperById(tvShow.getLastScraperId(), ScraperType.TV_SHOW));
        options.setLanguage(MediaLanguages.valueOf(tvShow.getLastScrapeLanguage()));

        List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = TvShowModuleManager.SETTINGS.getEpisodeScraperMetadataConfig();

        TvShowEpisodeScrapeTask task = new TvShowEpisodeScrapeTask(entry.getValue(), options, episodeScraperMetadataConfig);
        TmmTaskManager.getInstance().addUnnamedTask(task);
      }
      else {
        // otherwise we need to show the dialog
        TvShowScrapeMetadataDialog dialog = TvShowScrapeMetadataDialog
            .createEpisodeScrapeDialog(TmmResourceBundle.getString("tvshowepisode.scrape") + " - \"" + tvShow.getTitle() + "\"");
        dialog.setLocationRelativeTo(MainWindow.getInstance());
        dialog.setVisible(true);

        // do we want to scrape?
        if (!dialog.shouldStartScrape()) {
          return;
        }

        // get options from dialog
        TvShowEpisodeSearchAndScrapeOptions options = dialog.getTvShowEpisodeSearchAndScrapeOptions();
        List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig = dialog.getTvShowEpisodeScraperMetadataConfig();

        // scrape
        TvShowEpisodeScrapeTask task = new TvShowEpisodeScrapeTask(entry.getValue(), options, episodeScraperMetadataConfig);
        TmmTaskManager.getInstance().addUnnamedTask(task);
      }
    }
  }
}
