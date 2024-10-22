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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.dialogs.MovieCreateOfflineDialog;

/**
 * MovieCreateOfflineAction - create a new offline movie
 * 
 * @author Manuel Laggner
 */
public class MovieCreateOfflineAction extends TmmAction {
  public MovieCreateOfflineAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.createoffline"));
    putValue(LARGE_ICON_KEY, IconManager.ADD);
    putValue(SMALL_ICON, IconManager.ADD);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.createoffline"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    MovieCreateOfflineDialog dialog = new MovieCreateOfflineDialog();
    dialog.pack();
    dialog.setVisible(true);
  }
}
