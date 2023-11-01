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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowEditorDialog;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowEpisodeEditorDialog;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowSeasonEditorDialog;

/**
 * The Class TvShowEditAction. To edit TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowEditAction extends TmmAction {
  public TvShowEditAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.edittvshowepisodes"));
    putValue(LARGE_ICON_KEY, IconManager.EDIT);
    putValue(SMALL_ICON, IconManager.EDIT);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.edittvshowepisodes"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Object> selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedTreeObjects();

    List<Object> selectedObjectWoLocked = selectedObjects.stream().filter(obj -> {
      if (obj instanceof TvShow && !((TvShow) obj).isLocked()) {
        return true;
      }
      else if (obj instanceof TvShowSeason && !((TvShowSeason) obj).isLocked()) {
        return true;
      }
      else if (obj instanceof TvShowEpisode && !((TvShowEpisode) obj).isLocked()) {
        return true;
      }
      return false;
    }).collect(Collectors.toList());

    if (selectedObjects.size() != selectedObjectWoLocked.size()) {
      TvShowSelectionModel.showLockedInformation();
    }

    if (selectedObjectWoLocked.isEmpty()) {
      return;
    }

    int selectedCount = selectedObjectWoLocked.size();
    int index = 0;
    int selectedShowTab = 0;
    int selectedSeasonTab = 0;
    int selectedEpisodeTab = 0;

    do {
      Object obj = selectedObjectWoLocked.get(index);

      // display tv show editor
      if (obj instanceof TvShow) {
        TvShow tvShow = (TvShow) obj;
        TvShowEditorDialog editor = new TvShowEditorDialog(tvShow, index, selectedCount, selectedShowTab);
        editor.setVisible(true);
        selectedShowTab = editor.getSelectedTab();
        if (!editor.isContinueQueue()) {
          break;
        }

        if (editor.isNavigateBack()) {
          index -= 1;
        }
        else {
          index += 1;
        }
      }

      // change season poster
      if (obj instanceof TvShowSeason) {
        TvShowSeason season = (TvShowSeason) obj;
        TvShowSeasonEditorDialog editor = new TvShowSeasonEditorDialog(season, index, selectedCount, selectedSeasonTab);
        editor.setVisible(true);
        selectedSeasonTab = editor.getSelectedTab();
        if (!editor.isContinueQueue()) {
          break;
        }

        if (editor.isNavigateBack()) {
          index -= 1;
        }
        else {
          index += 1;
        }
      }

      // display tv episode editor
      if (obj instanceof TvShowEpisode tvShowEpisode) {
        TvShowEpisodeEditorDialog editor = new TvShowEpisodeEditorDialog(tvShowEpisode, index, selectedCount, selectedEpisodeTab);
        editor.setVisible(true);
        selectedEpisodeTab = editor.getSelectedTab();
        if (!editor.isContinueQueue()) {
          break;
        }

        if (editor.isNavigateBack()) {
          index -= 1;
        }
        else {
          index += 1;
        }
      }
    } while (index < selectedCount);
  }
}
