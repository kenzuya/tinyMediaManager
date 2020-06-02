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
package org.tinymediamanager.ui.moviesets.settings;

import java.util.ResourceBundle;

import javax.swing.JPanel;

import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;

/**
 * The class MovieSetSettingsPanel is used for displaying some movie set related settings
 * 
 * @author Manuel Laggner
 */
public class MovieSetSettingsPanel extends JPanel {
  private static final long           serialVersionUID = -4173835431245178069L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");

  private final MovieSettings         settings         = MovieModuleManager.SETTINGS;

  public MovieSetSettingsPanel() {

  }

  private void initComponents() {

  }

  protected void initDataBindings() {

  }
}
