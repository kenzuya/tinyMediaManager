/*
 * Copyright 2012 - 2020 Manuel Laggner
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

import static org.tinymediamanager.ui.TmmUIHelper.checkForUpdate;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import org.tinymediamanager.license.License;

/**
 * The CheckForUpdateAction is used to trigger an update check
 * 
 * @author Manuel Laggner
 */
public class CheckForUpdateAction extends TmmAction {
  private static final long           serialVersionUID = 3046686017542572465L;
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");

  public CheckForUpdateAction() {
    putValue(NAME, BUNDLE.getString("tmm.updater.check"));
    putValue(SHORT_DESCRIPTION, BUNDLE.getString("tmm.updater.check"));
    setEnabled(License.getInstance().isValidLicense());

    License.getInstance().addEventListener(() -> setEnabled(License.getInstance().isValidLicense()));
  }

  @Override
  protected void processAction(ActionEvent e) {
    checkForUpdate();
  }
}
