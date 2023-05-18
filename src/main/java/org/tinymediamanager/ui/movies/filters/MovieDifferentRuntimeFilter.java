/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link MovieDifferentRuntimeFilter} is used to provide a filter for runtime difference between mediainfo data and scraped data
 * 
 * @author Wolfgang Janes
 */
public class MovieDifferentRuntimeFilter extends AbstractMovieUIFilter {

  private JSpinner spinner;

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.runtimedifference"));
  }

  @Override
  protected JComponent createFilterComponent() {
    spinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    return spinner;
  }

  @Override
  public String getId() {
    return "movieDifferentRuntime";
  }

  @Override
  public String getFilterValueAsString() {
    return spinner.getValue().toString();
  }

  @Override
  public void setFilterValue(Object value) {
    try {
      spinner.setValue(Integer.parseInt(value.toString()));
    }
    catch (Exception e) {
      // default value
      spinner.setValue(1);
    }
  }

  @Override
  public void clearFilter() {
    // default value
    spinner.setValue(1);
  }

  @Override
  public boolean accept(Movie movie) {

    int scrapedRuntimeInMinutes = movie.getRuntime();
    int mediaInfoRuntimeInMinutes = movie.getRuntimeFromMediaFilesInMinutes();

    if ((scrapedRuntimeInMinutes - mediaInfoRuntimeInMinutes >= (int) spinner.getValue())
        || (mediaInfoRuntimeInMinutes - scrapedRuntimeInMinutes >= (int) spinner.getValue())) {
      return true;
    }

    return false;
  }
}
