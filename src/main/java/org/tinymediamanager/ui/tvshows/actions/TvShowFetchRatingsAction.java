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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowFetchRatingsDialog;

/**
 * the class {@link TvShowFetchRatingsAction} is used to download ratings from various sources
 *
 * @author Manuel Laggner
 */
public class TvShowFetchRatingsAction extends TmmAction {

  public TvShowFetchRatingsAction() {
    putValue(LARGE_ICON_KEY, IconManager.RATING_BLUE);
    putValue(SMALL_ICON, IconManager.RATING_BLUE);
    putValue(NAME, TmmResourceBundle.getString("tvshow.fetchratings"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
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

    TvShowFetchRatingsDialog dialog = new TvShowFetchRatingsDialog();
    dialog.setVisible(true);

    List<RatingProvider.RatingSource> sources = dialog.getSelectedRatingSources();

    if (!sources.isEmpty()) {
      TmmTaskManager.getInstance()
          .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("tvshow.fetchratings"), tvShows.size() + episodes.size(),
              TmmTaskHandle.TaskType.BACKGROUND_TASK) {

            @Override
            protected void doInBackground() {
              int i = 0;

              // TV shows
              for (TvShow tvShow : tvShows) {
                List<MediaRating> ratings = RatingProvider.getRatings(tvShow.getIds(), sources, MediaType.TV_SHOW);
                ratings.forEach(tvShow::setRating);
                if (!ratings.isEmpty()) {
                  tvShow.saveToDb();
                  tvShow.writeNFO();
                }

                publishState(++i);
                if (cancel) {
                  break;
                }
              }

              // episodes
              for (TvShowEpisode episode : episodes) {
                Map<String, Object> ids = new HashMap<>(episode.getIds());
                ids.put("tvShowIds", episode.getTvShow().getIds());
                ids.put(MediaMetadata.SEASON_NR, episode.getSeason());
                ids.put(MediaMetadata.EPISODE_NR, episode.getEpisode());
                List<MediaRating> ratings = RatingProvider.getRatings(ids, sources, MediaType.TV_EPISODE);
                ratings.forEach(episode::setRating);
                if (!ratings.isEmpty()) {
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
}
