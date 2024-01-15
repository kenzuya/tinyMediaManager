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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.dialogs.MovieEditorDialog;

/**
 * MovieEditAction - edit movies
 * 
 * @author Manuel Laggner
 */
public class MovieEditAction extends TmmAction {
  public MovieEditAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.edit"));
    putValue(LARGE_ICON_KEY, IconManager.EDIT);
    putValue(SMALL_ICON, IconManager.EDIT);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      return;
    }

    int selectedCount = selectedMovies.size();
    int index = 0;
    int selectedTab = 0;

    do {
      Movie movie = selectedMovies.get(index);
      MovieEditorDialog dialogMovieEditor = new MovieEditorDialog(movie, index, selectedCount, selectedTab);
      dialogMovieEditor.setVisible(true);
      selectedTab = dialogMovieEditor.getSelectedTab();

      if (!dialogMovieEditor.isContinueQueue()) {
        break;
      }

      if (dialogMovieEditor.isNavigateBack()) {
        index -= 1;
      }
      else {
        index += 1;
      }

    } while (index < selectedCount);
  }
}
