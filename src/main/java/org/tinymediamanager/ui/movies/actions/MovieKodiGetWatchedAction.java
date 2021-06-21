/*
 * Copyright 2012 - 2021 Manuel Laggner
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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * Action to trigger a Kodi library refresh on selected items
 * 
 * @author Myron Boyle
 */
public class MovieKodiGetWatchedAction extends TmmAction {
  private static final long serialVersionUID = -6731682301579049379L;

  public MovieKodiGetWatchedAction() {
    putValue(LARGE_ICON_KEY, IconManager.WATCHED_MENU);
    putValue(SMALL_ICON, IconManager.WATCHED_MENU);
    putValue(NAME, TmmResourceBundle.getString("kodi.rpc.getwatched"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = new ArrayList<>(MovieUIModule.getInstance().getSelectionModel().getSelectedMovies());

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    TmmTaskManager.getInstance()
        .addUnnamedTask(
            new TmmTask(TmmResourceBundle.getString("kodi.rpc.getwatched"), selectedMovies.size(), TmmTaskHandle.TaskType.BACKGROUND_TASK) {

              @Override
              protected void doInBackground() {
                KodiRPC kodiRPC = KodiRPC.getInstance();
                int i = 0;

                for (Movie movie : selectedMovies) {
                  kodiRPC.readWatchedState(movie);
                  publishState(++i);
                  if (cancel) {
                    return;
                  }
                }
              }
            });
  }
}
