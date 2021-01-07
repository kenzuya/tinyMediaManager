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
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowArtworkHelper;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * the class {@link TvShowEpisodeCreateThumbAction} trigger the thumb creation via FFmpeg
 * 
 * @author Wolfgang Janes
 */
public class TvShowEpisodeCreateThumbAction extends TmmAction {
  
  private static final Logger         LOGGER = LoggerFactory.getLogger(TvShowEpisodeCreateThumbAction.class);

  public TvShowEpisodeCreateThumbAction() {

    putValue(NAME, TmmResourceBundle.getString("tvshowepisode.ffmpeg.createthumb"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshowepisode.ffmpeg.createthumb.desc"));
    putValue(SMALL_ICON, IconManager.THUMB);
    putValue(LARGE_ICON_KEY, IconManager.THUMB);
  }

  @Override
  protected void processAction(ActionEvent e) {
    // check customizing; FFmpeg settings AND Thumb settings must be available
    if (StringUtils.isBlank(Globals.settings.getMediaFramework())) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("mediaframework.missingbinary"));
      return;
    }

    if (Globals.settings.getFfmpegPercentage() == 0) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("mediaframework.framevalue"));
      return;
    }

    if (MovieModuleManager.SETTINGS.getThumbFilenames().isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tvshowepisode.nothumbs"));
      return;
    }

    List<TvShowEpisode> selectedEpisodes = new ArrayList<>(TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes());

    if (selectedEpisodes.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    Runnable runnable = () -> {
      for (TvShowEpisode episode : selectedEpisodes) {
        if (!episode.isDisc()) {
          try {
            TvShowArtworkHelper.createThumbWithFfmpeg(episode);
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
