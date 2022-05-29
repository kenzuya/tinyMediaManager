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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

/**
 * {@link MovieSetRewriteNfoAction} - to rewrite the NFOs from all selected movie sets
 * 
 * @author Manuel Laggner
 */
public class MovieSetRewriteNfoAction extends TmmAction {
  private static final long serialVersionUID = 1848573591741154631L;

  public MovieSetRewriteNfoAction() {
    putValue(NAME, TmmResourceBundle.getString("movieset.rewritenfo"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<MovieSet> selectedMovieSets = MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovieSets();

    if (MovieModuleManager.getInstance().getSettings().getMovieSetNfoFilenames().isEmpty()) {
      // no movie set file name specified - abort
      return;
    }

    if (selectedMovieSets.isEmpty()) {
      return;
    }

    TmmTaskManager.getInstance()
        .addUnnamedTask(
            new TmmTask(TmmResourceBundle.getString("movieset.rewritenfo"), selectedMovieSets.size(), TmmTaskHandle.TaskType.BACKGROUND_TASK) {
              @Override
              protected void doInBackground() {
                int i = 0;

                for (MovieSet movieSet : selectedMovieSets) {
                  movieSet.writeNFO();
                  movieSet.saveToDb();
                  publishState(++i);
                  if (cancel) {
                    break;
                  }
                }
              }
            });
  }
}
