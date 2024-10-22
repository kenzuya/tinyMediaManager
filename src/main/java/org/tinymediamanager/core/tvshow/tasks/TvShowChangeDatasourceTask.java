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

package org.tinymediamanager.core.tvshow.tasks;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

/**
 * the class {@link TvShowChangeDatasourceTask} is used to change a Data source of a whole TV show including the move/copy of all files
 * 
 * @author Manuel Laggner
 */
public class TvShowChangeDatasourceTask extends TmmThreadPool {
  private static final Logger LOGGER          = LoggerFactory.getLogger(TvShowChangeDatasourceTask.class);

  private final String        datasource;
  private final List<TvShow>  tvShowsToChange = new ArrayList<>();

  public TvShowChangeDatasourceTask(List<TvShow> tvShowsToChange, String datasource) {
    super(TmmResourceBundle.getString("tvshow.changedatasource"));
    this.tvShowsToChange.addAll(tvShowsToChange);
    this.datasource = datasource;
  }

  @Override
  protected void doInBackground() {
    initThreadPool(1, "changeDataSource");
    start();

    for (TvShow tvShow : tvShowsToChange) {
      submitTask(new Worker(tvShow));
    }
    waitForCompletionOrCancel();
    LOGGER.info("Done changing data sources");
  }

  @Override
  public void callback(Object obj) {
    publishState((String) obj, progressDone);
  }

  private class Worker implements Runnable {
    private final TvShow tvShow;

    private Worker(TvShow tvShow) {
      this.tvShow = tvShow;
    }

    @Override
    public void run() {
      LOGGER.info("changing data source of TV show '{}' to '{}", tvShow.getTitle(), datasource);

      if (tvShow.getDataSource().equals(datasource)) {
        LOGGER.warn("old and new data source is the same");
        return;
      }
      moveTvShow();
    }

    private void moveTvShow() {
      Path srcDir = tvShow.getPathNIO();
      Path destDir = Paths.get(datasource, Paths.get(tvShow.getDataSource()).relativize(tvShow.getPathNIO()).toString());

      LOGGER.debug("moving TV show dir '{}' to '{}'", srcDir, destDir);

      try {
        boolean ok = Utils.moveDirectorySafe(srcDir, destDir);
        if (ok) {
          tvShow.setDataSource(datasource);
          tvShow.setPath(destDir.toAbsolutePath().toString());
          tvShow.updateMediaFilePath(srcDir, destDir);
          for (TvShowEpisode episode : new ArrayList<>(tvShow.getEpisodes())) {
            episode.setDataSource(datasource);
            episode.replacePathForRenamedTvShowRoot(srcDir, destDir);
            episode.updateMediaFilePath(srcDir, destDir);
            episode.saveToDb();

            // re-build the image cache afterwards in an own thread
            episode.cacheImages();
          }
          tvShow.saveToDb(); // since we moved already, save it

          // re-build the image cache afterwards in an own thread
          tvShow.cacheImages();
        }
        else {
          LOGGER.error("Could not move to destination '{}' - NOT changing datasource", destDir);
          MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, srcDir, "message.changedatasource.failedmove"));
        }
      }
      catch (Exception e) {
        LOGGER.error("error moving folder: ", e);
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, srcDir, "message.changedatasource.failedmove", new String[] { ":", e.getLocalizedMessage() }));
      }
    }
  }
}
