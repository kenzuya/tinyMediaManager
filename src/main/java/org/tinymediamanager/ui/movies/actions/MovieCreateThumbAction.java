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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * the class {@link MovieCreateThumbAction} trigger the thumb creation via FFmpeg
 * 
 * @author Wolfgang Janes
 */
public class MovieCreateThumbAction extends TmmAction {
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");
  private static final Logger         LOGGER = LoggerFactory.getLogger(MovieCreateThumbAction.class);

  public MovieCreateThumbAction() {

    putValue(NAME, BUNDLE.getString("movie.ffmpeg.createthumb"));
    putValue(SHORT_DESCRIPTION, BUNDLE.getString("movie.ffmpeg.createthumb.desc"));
    putValue(SMALL_ICON, IconManager.THUMB);
    putValue(LARGE_ICON_KEY, IconManager.THUMB);
  }

  @Override
  protected void processAction(ActionEvent e) {
    // check customizing; FFmpeg settings AND Thumb settings must be available
    if (StringUtils.isBlank(Globals.settings.getMediaFramework())) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), BUNDLE.getString("mediaframework.missingbinary"));
      return;
    }

    if (Globals.settings.getFfmpegPercentage() == 0) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), BUNDLE.getString("mediaframework.framevalue"));
      return;
    }

    if (MovieModuleManager.SETTINGS.getThumbFilenames().isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), BUNDLE.getString("movie.nothumbs"));
      return;
    }

    List<Movie> selectedMovies = new ArrayList<>(MovieUIModule.getInstance().getSelectionModel().getSelectedMovies());

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), BUNDLE.getString("tmm.nothingselected"));
      return;
    }

    Runnable runnable = () -> {
      for (Movie movie : selectedMovies) {
        if (!movie.isDisc()) {
          try {
            MovieArtworkHelper.createThumbWithFfmpeg(movie);
          }
          catch (Exception ex) {
            LOGGER.error("could not create FFmpeg thumb - {}", ex.getMessage());
          }
        }
      }
    };

    TmmTaskManager.getInstance().addUnnamedTask(runnable);
  }
}
