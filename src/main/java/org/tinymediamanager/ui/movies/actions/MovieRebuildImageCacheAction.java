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

import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tasks.ImageCacheTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * MovieRebuildImageCacheAction - rebuild the image cache for selected movie(s)
 * 
 * @author Manuel Laggner
 */
public class MovieRebuildImageCacheAction extends TmmAction {
  private static final long serialVersionUID = -5089957097690621345L;

  public MovieRebuildImageCacheAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.rebuildimagecache"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.rebuildimagecache"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    if (!Settings.getInstance().isImageCache()) {
      JOptionPane.showMessageDialog(null, TmmResourceBundle.getString("tmm.imagecache.notactivated"));
      return;
    }

    List<Movie> selectedMovies = new ArrayList<>(MovieUIModule.getInstance().getSelectionModel().getSelectedMovies());

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    List<MediaFile> imageFiles = new ArrayList<>();

    // get data of all files within all selected movies
    for (Movie movie : selectedMovies) {
      imageFiles.addAll(movie.getImagesToCache());
      ImageCache.clearImageCacheForMediaEntity(movie);
    }

    ImageCacheTask task = new ImageCacheTask(imageFiles);
    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
