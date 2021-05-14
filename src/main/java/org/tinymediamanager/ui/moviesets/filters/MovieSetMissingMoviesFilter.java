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
package org.tinymediamanager.ui.moviesets.filters;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a "missing movies" filter for the movie set show tree
 * 
 * @author Manuel Laggner
 */
public class MovieSetMissingMoviesFilter extends AbstractMovieSetUIFilter {

  @Override
  public String getId() {
    return "tvShowMissingEpisodes";
  }

  @Override
  public String getFilterValueAsString() {
    return null;
  }

  @Override
  public void setFilterValue(Object value) {
  }

  @Override
  protected boolean accept(MovieSet movieSet, List<Movie> movies) {
    for (Movie movie : movies) {
      if (movie instanceof MovieSet.MovieSetMovie) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.missingmovies"));
  }

  @Override
  protected JComponent createFilterComponent() {
    return null;
  }
}
