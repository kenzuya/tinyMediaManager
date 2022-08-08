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
package org.tinymediamanager.ui.moviesets.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.dialogs.MovieExporterDialog;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

/**
 * The {@link MovieSetExportMovieAction} - to export all selected movies via a template
 * 
 * @author Manuel Laggner
 */
public class MovieSetExportMovieAction extends TmmAction {
  private static final long serialVersionUID = -6731682301579049379L;

  public MovieSetExportMovieAction() {
    putValue(LARGE_ICON_KEY, IconManager.EXPORT);
    putValue(SMALL_ICON, IconManager.EXPORT);
    putValue(NAME, TmmResourceBundle.getString("movie.export"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> movies = new ArrayList<>(MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovies(true));

    if (movies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // export selected movies
    MovieExporterDialog dialog = new MovieExporterDialog(movies);
    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);
  }
}
