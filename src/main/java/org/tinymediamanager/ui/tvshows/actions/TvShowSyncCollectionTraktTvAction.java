/*
 * Copyright 2012 - 2020 Manuel Laggner
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
import java.util.ResourceBundle;

import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;

/**
 * The class TvShowSyncTraktTvAction. To synchronize your TV show library with trakt.tv
 * 
 * @author Manuel Laggner
 */
public class TvShowSyncCollectionTraktTvAction extends TmmAction {
  private static final long           serialVersionUID = 6640292090443882545L;
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");

  public TvShowSyncCollectionTraktTvAction() {
    putValue(NAME, BUNDLE.getString("tvshow.synctrakt"));
    putValue(SHORT_DESCRIPTION, BUNDLE.getString("tvshow.synctrakt.desc"));
    putValue(SMALL_ICON, IconManager.MOVIE);
    putValue(LARGE_ICON_KEY, IconManager.MOVIE);
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(TvShowList.getInstance().getTvShows());
    task.setSyncCollection(true);

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
