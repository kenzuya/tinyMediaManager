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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

public class DebugDumpShowAction extends TmmAction {
  public DebugDumpShowAction() {
    putValue(NAME, TmmResourceBundle.getString("debug.entity.dump"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("debug.entity.dump.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    // do not handle multi/intermixed content...

    // check season first, since getSeasons() always returns episodes too
    final Set<TvShowSeason> selectedSeason = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects(true, true).getSeasons();
    if (!selectedSeason.isEmpty()) {
      for (TvShowSeason se : selectedSeason) {
        TvShowModuleManager.getInstance().dump(se);
      }
    }
    else {
      // we clicked on show/season
      final List<TvShowEpisode> selectedEpisodes = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();
      if (!selectedEpisodes.isEmpty()) {
        for (TvShowEpisode ep : selectedEpisodes) {
          TvShowModuleManager.getInstance().dump(ep);
        }
      }
      else {
        // recurse complete show
        final List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShows(true);
        if (selectedTvShows.isEmpty()) {
          JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
          return;
        }
        for (TvShow tvShow : selectedTvShows) {
          TvShowModuleManager.getInstance().dump(tvShow);
        }
      }
    }
  }
}
