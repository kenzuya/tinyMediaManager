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

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowRemoveAction. To remove selected elements
 * 
 * @author Manuel Laggner
 */
public class TvShowRemoveAction extends TmmAction {
  private static final long serialVersionUID = -2355545751433709417L;

  public TvShowRemoveAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.remove"));
    putValue(SMALL_ICON, IconManager.DELETE);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.remove"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Object> selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // display warning and ask the user again
    if (Boolean.FALSE.equals(TmmProperties.getInstance().getPropertyAsBoolean("tvshow.hideremovehint"))) {
      JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
      TmmFontHelper.changeFont(checkBox, L1);
      checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      Object[] params = { TmmResourceBundle.getString("tvshow.remove.desc"), checkBox };
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("tvshow.remove"),
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

      // the user don't want to show this dialog again
      if (checkBox.isSelected()) {
        TmmProperties.getInstance().putProperty("tvshow.hideremovehint", String.valueOf(checkBox.isSelected()));
      }

      if (answer != JOptionPane.YES_OPTION) {
        return;
      }
    }

    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (Object obj : selectedObjects) {
      // remove a whole TV show
      if (obj instanceof TvShow) {
        TvShow tvShow = (TvShow) obj;
        TvShowModuleManager.getInstance().getTvShowList().removeTvShow(tvShow);
      }
      // remove seasons
      if (obj instanceof TvShowSeason) {
        TvShowSeason season = (TvShowSeason) obj;
        List<TvShowEpisode> episodes = new ArrayList<>(season.getEpisodes());
        for (TvShowEpisode episode : episodes) {
          season.getTvShow().removeEpisode(episode);
        }
      }
      // remove episodes
      if (obj instanceof TvShowEpisode) {
        TvShowEpisode tvShowEpisode = (TvShowEpisode) obj;
        tvShowEpisode.getTvShow().removeEpisode(tvShowEpisode);
      }
    }
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
}
