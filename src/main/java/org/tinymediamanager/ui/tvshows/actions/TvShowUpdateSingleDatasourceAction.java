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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;

import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.tasks.TvShowUpdateDatasourceTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;

/**
 * The class TvShowUpdateSingleDatasourceAction. Update a single data source
 * 
 * @author Manuel Laggner
 */
public class TvShowUpdateSingleDatasourceAction extends TmmAction {
  private final String datasource;

  public TvShowUpdateSingleDatasourceAction(String datasource) {
    this.datasource = datasource;
    putValue(NAME, datasource);
    putValue(LARGE_ICON_KEY, IconManager.REFRESH);
    putValue(SMALL_ICON, IconManager.REFRESH);
  }

  @Override
  protected void processAction(ActionEvent e) {
    TmmThreadPool task = new TvShowUpdateDatasourceTask(datasource);
    TmmTaskManager.getInstance().addMainTask(task);
  }
}
