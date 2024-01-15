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

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.tasks.TvShowRenameTask;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class {@link TvShowRenameAction}. To rename TV shows/episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowRenameAction extends TmmAction {
  public TvShowRenameAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.rename"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.rename"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSelectionModel.SelectedObjects selectedObjects = TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects();

    if (selectedObjects.isLockedFound()) {
      TvShowSelectionModel.showLockedInformation();
    }

    if (selectedObjects.isEmpty()) {
      return;
    }

    // display warning and ask the user again
    if (Boolean.FALSE.equals(TmmProperties.getInstance().getPropertyAsBoolean("tvshow.hiderenamehint"))) {
      JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
      TmmFontHelper.changeFont(checkBox, L1);
      checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      Object[] params = { TmmResourceBundle.getString("tvshow.rename.desc"), checkBox };
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("tvshow.rename"),
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

      // the user don't want to show this dialog again
      if (checkBox.isSelected()) {
        TmmProperties.getInstance().putProperty("tvshow.hiderenamehint", String.valueOf(checkBox.isSelected()));
      }

      if (answer != JOptionPane.YES_OPTION) {
        return;
      }
    }

    // rename
    TmmThreadPool renameTask = new TvShowRenameTask(selectedObjects.getTvShows(), selectedObjects.getEpisodesRecursive());
    TmmTaskManager.getInstance().addMainTask(renameTask);
  }
}
