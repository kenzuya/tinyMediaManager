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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.thirdparty.trakttv.MovieSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * The class {@link MovieSyncSelectedTraktTvAction}. To synchronize all selected movies with trakt.tv (collection and watched)
 * 
 * @author Manuel Laggner
 */
public class MovieSyncSelectedTraktTvAction extends TmmAction {
  public MovieSyncSelectedTraktTvAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.synctrakt.selected"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.synctrakt.selected.desc"));
    putValue(SMALL_ICON, IconManager.SYNC);
    putValue(LARGE_ICON_KEY, IconManager.SYNC);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    MovieSyncTraktTvTask task = new MovieSyncTraktTvTask(selectedMovies);
    task.setSyncCollection(true);
    task.setSyncWatched(true);

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
