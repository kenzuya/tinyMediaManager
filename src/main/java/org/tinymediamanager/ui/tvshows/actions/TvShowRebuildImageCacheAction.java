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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tasks.ImageCacheTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowRebuildImageCacheAction. To rebuild the image cache selected TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowRebuildImageCacheAction extends TmmAction {
  private static final long serialVersionUID = 3452373237085274937L;

  public TvShowRebuildImageCacheAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.rebuildimagecache"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.rebuildimagecache"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    if (!Settings.getInstance().isImageCache()) {
      JOptionPane.showMessageDialog(null, TmmResourceBundle.getString("tmm.imagecache.notactivated"));
      return;
    }

    List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShows();
    List<TvShowEpisode> selectedEpisodes = new ArrayList<>();

    // add all episodes which are not part of a selected tv show
    for (Object obj : TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects()) {
      if (obj instanceof TvShowEpisode) {
        TvShowEpisode episode = (TvShowEpisode) obj;
        if (!selectedTvShows.contains(episode.getTvShow())) {
          selectedEpisodes.add(episode);
        }
      }
    }

    if (selectedEpisodes.isEmpty() && selectedTvShows.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    Set<MediaFile> imageFiles = new HashSet<>();

    // get data of all files within all selected TV shows/episodes
    for (TvShow tvShow : selectedTvShows) {
      imageFiles.addAll(tvShow.getImagesToCache());
      ImageCache.clearImageCacheForMediaEntity(tvShow);
    }

    for (TvShowEpisode episode : selectedEpisodes) {
      imageFiles.addAll(episode.getImagesToCache());
      ImageCache.clearImageCacheForMediaEntity(episode);
    }

    ImageCacheTask task = new ImageCacheTask(imageFiles.stream().distinct().collect(Collectors.toList()));
    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
