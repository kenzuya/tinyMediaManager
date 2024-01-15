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
package org.tinymediamanager.ui.movies.filters;

import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * this class is used for a video bit depth movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieVideoBitdepthFilter extends AbstractNumberMovieFilter {

  public MovieVideoBitdepthFilter() {
    super();

    // display the size with M at the end
    spinnerLow.setEditor(prepareNumberEditor(spinnerLow, "#0 bit"));
    spinnerHigh.setEditor(prepareNumberEditor(spinnerHigh, "#0 bit"));
  }

  @Override
  public String getId() {
    return "movieVideoBitdepth";
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(0, 0, 99, 1);
  }

  @Override
  public boolean accept(Movie movie) {
    return matchInt(movie.getMainVideoFile().getBitDepth());
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.bitdepth"));
  }
}
