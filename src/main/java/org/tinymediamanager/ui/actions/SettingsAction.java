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
package org.tinymediamanager.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.dialogs.SettingsDialog;

/**
 * The class SettingsAction. To display the settings dialog
 * 
 * @author Manuel Laggner
 */
public class SettingsAction extends TmmAction {
  public SettingsAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.settings"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    JDialog settingsDialog = SettingsDialog.getInstance();
    settingsDialog.setVisible(true);
  }
}
