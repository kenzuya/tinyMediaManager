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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.tasks.TvShowSubtitleSearchAndDownloadTask;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowDownloadSubtitleDialog;

/**
 * The MovieSubtitleDownloadAction - download subtitles (via hash) for all selected movies
 * 
 * @author Manuel Laggner
 */
public class TvShowSubtitleDownloadAction extends TmmAction {
  public TvShowSubtitleDownloadAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.download.subtitle"));
    putValue(SMALL_ICON, IconManager.SUBTITLE);
    putValue(LARGE_ICON_KEY, IconManager.SUBTITLE);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.download.subtitle"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShowEpisode> episodes = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();

    if (episodes.isEmpty()) {
      return;
    }

    TvShowDownloadSubtitleDialog dialog = new TvShowDownloadSubtitleDialog(TmmResourceBundle.getString("tvshow.download.subtitle"));
    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);

    // do we want to scrape?
    if (dialog.shouldStartDownload()) {
      for (MediaLanguages language : dialog.getLanguages()) {
        TvShowSubtitleSearchAndDownloadTask task = new TvShowSubtitleSearchAndDownloadTask(episodes, dialog.getSubtitleScrapers(), language);
        task.setForceBestMatch(dialog.isForceBestMatch());
        TmmTaskManager.getInstance().addMainTask(task);
      }
    }
  }
}
