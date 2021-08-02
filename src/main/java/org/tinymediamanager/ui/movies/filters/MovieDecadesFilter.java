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
package org.tinymediamanager.ui.movies.filters;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for filtering movies by decades
 *
 * @author Wolfgang Janes
 */
public class MovieDecadesFilter extends AbstractCheckComboBoxMovieUIFilter<String> {
  private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();

  public MovieDecadesFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildDecades();
    PropertyChangeListener propertyChangeListener = evt -> buildDecades();
    movieList.addPropertyChangeListener(Constants.DECADE, propertyChangeListener);
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("metatag.decade"));
  }

  @Override
  public String getId() {
    return "movieDecades";
  }

  @Override
  public boolean accept(Movie movie) {
    List<String> selectedItems = checkComboBox.getSelectedItems();
    return selectedItems.contains(movie.getDecadeShort());
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  private void buildDecades() {
    List<String> decades = new ArrayList<>(movieList.getDecadeInMovies());
    Collections.sort(decades);
    setValues(decades);
  }
}
