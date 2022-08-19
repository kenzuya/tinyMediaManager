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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * the class {@link TvShowFetchIdsAction} is used to get missing ids
 *
 * @author Manuel Laggner
 */
public class TvShowFetchIdsAction extends TmmAction {

  public TvShowFetchIdsAction() {
    putValue(LARGE_ICON_KEY, IconManager.BARCODE);
    putValue(SMALL_ICON, IconManager.BARCODE);
    putValue(NAME, TmmResourceBundle.getString("tvshow.fetchids"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects(false, false);

    Collection<TvShow> tvShows = selectedObjects.getTvShows();
    Collection<TvShowEpisode> episodes = selectedObjects.getEpisodesRecursive();

    if (tvShows.isEmpty() && episodes.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    TmmTaskManager.getInstance()
        .addUnnamedTask(
            new TmmTask(TmmResourceBundle.getString("tvshow.fetchids"), tvShows.size() + episodes.size(), TmmTaskHandle.TaskType.BACKGROUND_TASK) {

              @Override
              protected void doInBackground() {
                int i = 0;

                for (TvShow tvShow : tvShows) {
                  Map<String, Object> ids = new HashMap<>(tvShow.getIds());
                  MediaIdUtil.injectMissingIds(ids, MediaType.TV_SHOW);

                  if (ids.size() != tvShow.getIds().size()) {
                    ids.forEach((key, value) -> {
                      if (tvShow.getId(key) == null) {
                        tvShow.setId(key, value);
                      }
                    });
                    tvShow.saveToDb();
                    tvShow.writeNFO();
                  }

                  publishState(++i);
                  if (cancel) {
                    break;
                  }
                }

                for (TvShowEpisode episode : episodes) {
                  Map<String, Object> ids = new HashMap<>(episode.getIds());

                  // inject meta ids
                  ids.put(MediaMetadata.TVSHOW_IDS, episode.getTvShow().getIds());
                  ids.put(MediaMetadata.SEASON_NR, episode.getSeason());
                  ids.put(MediaMetadata.EPISODE_NR, episode.getEpisode());
                  MediaIdUtil.injectMissingIds(ids, MediaType.TV_EPISODE);

                  // remove meta ids
                  ids.remove(MediaMetadata.TVSHOW_IDS);
                  ids.remove(MediaMetadata.SEASON_NR);
                  ids.remove(MediaMetadata.EPISODE_NR);

                  if (ids.size() != episode.getIds().size()) {
                    ids.forEach((key, value) -> {
                      if (episode.getId(key) == null) {
                        episode.setId(key, value);
                      }
                    });
                    episode.saveToDb();
                    episode.writeNFO();
                  }

                  publishState(++i);
                  if (cancel) {
                    break;
                  }
                }
              }
            });
  }
}
