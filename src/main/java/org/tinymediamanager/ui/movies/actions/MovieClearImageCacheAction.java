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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * MovieClearImageCacheAction - clear image cache for selected movies
 * 
 * @author Manuel Laggner
 */
public class MovieClearImageCacheAction extends TmmAction {
  public MovieClearImageCacheAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.clearimagecache"));
    putValue(LARGE_ICON_KEY, "");
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies(true);

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // get data of all files within all selected movies
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (Movie movie : selectedMovies) {
      ImageCache.clearImageCacheForMediaEntity(movie);
    }
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
}
