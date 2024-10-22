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
package org.tinymediamanager.ui.moviesets.filters;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a movie set data source filter
 * 
 * @author Manuel Laggner
 */
public class MovieSetDatasourceFilter extends AbstractCheckComboBoxMovieSetUIFilter<String> {
  private MovieSettings movieSettings = MovieModuleManager.getInstance().getSettings();

  public MovieSetDatasourceFilter() {
    super();
    buildAndInstallDatasourceArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallDatasourceArray();
    movieSettings.addPropertyChangeListener(Constants.DATA_SOURCE, propertyChangeListener);
  }

  @Override
  public String getId() {
    return "movieSetDatasource";
  }

  @Override
  public boolean accept(MovieSet movieSet, List<Movie> movies) {
    List<String> datasources = checkComboBox.getSelectedItems();
    for (Movie movie : movies) {
      if (datasources.contains(movie.getDataSource())) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.datasource"));
  }

  private void buildAndInstallDatasourceArray() {
    setValues(new ArrayList<>(movieSettings.getMovieDataSource()));
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }
}
