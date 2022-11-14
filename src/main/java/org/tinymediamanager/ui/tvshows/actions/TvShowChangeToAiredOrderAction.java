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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowChangeToAiredOrderAction. To change to the aired order after import
 * 
 * @author Manuel Laggner
 */
public class TvShowChangeToAiredOrderAction extends TmmAction {
  public TvShowChangeToAiredOrderAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.changefromdvdorder"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.changeorder.desc"));
    putValue(LARGE_ICON_KEY, IconManager.EDIT);
    putValue(SMALL_ICON, IconManager.EDIT);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShowEpisode> selectedEpisodes = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();

    if (selectedEpisodes.isEmpty()) {
      return;
    }

    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (TvShowEpisode episode : selectedEpisodes) {
      if (episode.isDvdOrder()) {
        episode.setDvdOrder(false);
        episode.setAiredSeason(episode.getDvdSeason());
        episode.setAiredEpisode(episode.getDvdEpisode());
        episode.setDvdEpisode(-1);
        episode.setDvdSeason(-1);
        episode.writeNFO();
        episode.saveToDb();
      }
    }
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
}
