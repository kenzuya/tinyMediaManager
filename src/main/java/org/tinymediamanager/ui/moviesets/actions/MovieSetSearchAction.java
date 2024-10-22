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
package org.tinymediamanager.ui.moviesets.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;
import org.tinymediamanager.ui.moviesets.dialogs.MovieSetChooserDialog;

/**
 * @author Manuel Laggner
 * 
 */
public class MovieSetSearchAction extends TmmAction {
  /**
   * Instantiates a new search movie set action.
   */
  public MovieSetSearchAction() {
    putValue(NAME, TmmResourceBundle.getString("movieset.search"));
    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
    putValue(SMALL_ICON, IconManager.SEARCH);
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movieset.search"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<MovieSet> selectedMovieSets = MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovieSets();

    if (selectedMovieSets.isEmpty()) {
      return;
    }

    for (MovieSet movieSet : selectedMovieSets) {
      MovieSetChooserDialog chooser = new MovieSetChooserDialog(movieSet, selectedMovieSets.size() > 1);
      if (!chooser.showDialog()) {
        break;
      }
    }
  }
}
