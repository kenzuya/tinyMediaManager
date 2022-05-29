/*
 * Copyright 2012 - 2022 Manuel Laggner
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
import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

public class DebugDumpMovieSetAction extends TmmAction {
  private static final long serialVersionUID = -8473181347332963044L;

  public DebugDumpMovieSetAction() {
    putValue(NAME, TmmResourceBundle.getString("debug.entity.dump"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("debug.entity.dump.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<MovieSet> selectedMovieSets = new ArrayList<>(MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovieSets());

    if (selectedMovieSets.isEmpty()) {
      return;
    }

    for (MovieSet ms : selectedMovieSets) {
      MovieModuleManager.getInstance().dump(ms);
    }
  }
}
