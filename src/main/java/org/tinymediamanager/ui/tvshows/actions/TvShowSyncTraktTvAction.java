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

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;

/**
 * The class {@link TvShowSyncTraktTvAction}. To synchronize your TV show library with trakt.tv (collection and watched)
 * 
 * @author Manuel Laggner
 */
public class TvShowSyncTraktTvAction extends TmmAction {
  private static final long           serialVersionUID = 6640292090443882545L;
  

  public TvShowSyncTraktTvAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.synctrakt.complete"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.synctrakt.complete.desc"));
    putValue(SMALL_ICON, IconManager.SYNC);
    putValue(LARGE_ICON_KEY, IconManager.SYNC);
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(TvShowList.getInstance().getTvShows());
    task.setSyncCollection(true);
    task.setSyncWatched(true);

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
