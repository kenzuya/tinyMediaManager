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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.TmmAction;

/**
 * The {@link MovieAddDatasourceAction} - to directly add a new data source
 * 
 * @author Manuel Laggner
 */
public class MovieAddDatasourceAction extends TmmAction {
  private static final long serialVersionUID = -4417368111497702010L;

  public MovieAddDatasourceAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.datasource.add"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.datasource.add"));
    putValue(SMALL_ICON, IconManager.ADD);
    putValue(LARGE_ICON_KEY, IconManager.ADD);
  }

  @Override
  protected void processAction(ActionEvent e) {
    SwingUtilities.invokeLater(() -> {
      String path = TmmProperties.getInstance().getProperty("movie.datasource.path");
      Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("Settings.datasource.folderchooser"), path);
      if (file != null && Files.isDirectory(file)) {
        MovieModuleManager.SETTINGS.addMovieDataSources(file.toAbsolutePath().toString());
        MovieModuleManager.SETTINGS.saveSettings();
        TmmProperties.getInstance().putProperty("movie.datasource.path", file.toAbsolutePath().toString());
      }
    });
  }
}
