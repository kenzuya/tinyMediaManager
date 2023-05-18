/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieReloadMediaInformationTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * MovieMediaInformationAction - gather MediaInformation for movies
 * 
 * @author Manuel Laggner
 */
public class MovieMediaInformationAction extends TmmAction {
  private static final long           serialVersionUID = 4927466975489852998L;


  public MovieMediaInformationAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.updatemediainfo"));
    putValue(SMALL_ICON, IconManager.MEDIAINFO);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.updatemediainfo"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // get data of all files within all selected movies
    TmmThreadPool task = new MovieReloadMediaInformationTask(selectedMovies);
    TmmTaskManager.getInstance().addMainTask(task);
  }
}
