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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowChangeToDvdOrderAction. To change to the dvd order after import
 * 
 * @author Manuel Laggner
 */
public class TvShowChangeToDvdOrderAction extends TmmAction {
  private static final long           serialVersionUID = 8457297935386064655L;


  public TvShowChangeToDvdOrderAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.changetodvdorder"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.changeorder.desc"));
    putValue(LARGE_ICON_KEY, IconManager.EDIT);
    putValue(SMALL_ICON, IconManager.EDIT);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Object> selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();
    Set<TvShowEpisode> selectedEpisodes = new HashSet<>();

    for (Object obj : selectedObjects) {
      // display tv show editor
      if (obj instanceof TvShow) {
        TvShow tvShow = (TvShow) obj;
        selectedEpisodes.addAll(tvShow.getEpisodes());
      }
      if (obj instanceof TvShowSeason) {
        TvShowSeason season = (TvShowSeason) obj;
        selectedEpisodes.addAll(season.getEpisodes());
      }
      // display tv episode editor
      if (obj instanceof TvShowEpisode) {
        TvShowEpisode tvShowEpisode = (TvShowEpisode) obj;
        selectedEpisodes.add(tvShowEpisode);
      }
    }

    if (selectedEpisodes.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (TvShowEpisode episode : selectedEpisodes) {
      if (!episode.isDvdOrder()) {
        episode.setDvdOrder(true);
        episode.setDvdSeason(episode.getAiredSeason());
        episode.setDvdEpisode(episode.getAiredEpisode());
        episode.setAiredEpisode(-1);
        episode.setAiredSeason(-1);
        episode.writeNFO();
        episode.saveToDb();
      }
    }
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
}
