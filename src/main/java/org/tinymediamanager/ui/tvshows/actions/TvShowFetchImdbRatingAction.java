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

import static org.tinymediamanager.core.Constants.IMDB;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

public class TvShowFetchImdbRatingAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowFetchImdbRatingAction.class);

  public TvShowFetchImdbRatingAction() {
    putValue(LARGE_ICON_KEY, IconManager.RATING_BLUE);
    putValue(SMALL_ICON, IconManager.RATING_BLUE);
    putValue(NAME, TmmResourceBundle.getString("tvshow.refetchimdbrating"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isEmpty()) {
      return;
    }

    if (selectedObjects.isLockedFound()) {
      TvShowSelectionModel.showLockedInformation();
    }

    TmmTaskManager.getInstance()
        .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("tvshow.refetchimdbrating"),
            selectedObjects.getTvShows().size() + selectedObjects.getEpisodesRecursive().size(), TmmTaskHandle.TaskType.BACKGROUND_TASK) {

          @Override
          protected void doInBackground() {
            int i = 0;

            for (TvShow tvShow : selectedObjects.getTvShows()) {
              MediaRating rating = RatingProvider.getImdbRating(tvShow.getImdbId());
              if (rating != null) {
                tvShow.setRating(rating);
                tvShow.saveToDb();
                tvShow.writeNFO();
              }

              publishState(++i);
              if (cancel) {
                return;
              }
            }

            for (TvShowEpisode episode : selectedObjects.getEpisodesRecursive()) {
              MediaRating rating = RatingProvider.getImdbRating(episode.getIdAsString(IMDB));
              if (rating != null) {
                episode.setRating(rating);
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
