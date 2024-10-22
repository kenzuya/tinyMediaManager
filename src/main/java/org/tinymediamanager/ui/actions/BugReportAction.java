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
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.dialogs.BugReportDialog;

/**
 * The BugReportAction to send bug reports directly from tmm
 * 
 * @author Manuel Laggner
 */
public class BugReportAction extends TmmAction {
  public BugReportAction() {
    putValue(NAME, TmmResourceBundle.getString("BugReport"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("BugReport"));
    putValue(SMALL_ICON, IconManager.BUG);
    putValue(LARGE_ICON_KEY, IconManager.BUG);
  }

  @Override
  protected void processAction(ActionEvent e) {
    JDialog dialog = new BugReportDialog();
    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.pack();
    dialog.setVisible(true);
  }
}
