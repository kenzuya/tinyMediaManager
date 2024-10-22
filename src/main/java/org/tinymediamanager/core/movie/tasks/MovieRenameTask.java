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
package org.tinymediamanager.core.movie.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmThreadPool;

/**
 * The Class MovieRenameTask.
 * 
 * @author Manuel Laggner
 */
public class MovieRenameTask extends TmmThreadPool {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieRenameTask.class);

  private final List<Movie>   moviesToRename;

  /**
   * Instantiates a new movie rename task.
   * 
   * @param moviesToRename
   *          the movies to rename
   */
  public MovieRenameTask(List<Movie> moviesToRename) {
    super(TmmResourceBundle.getString("movie.rename"));
    this.moviesToRename = new ArrayList<>(moviesToRename);
  }

  @Override
  protected void doInBackground() {
    try {
      initThreadPool(1, "rename");
      start();

      List<MediaFile> imageFiles = new ArrayList<>();

      // rename movies
      for (Movie movie : moviesToRename) {
        if (cancel) {
          break;
        }
        submitTask(new RenameMovieTask(movie));
      }
      waitForCompletionOrCancel();

      if (cancel) {
        return;
      }

      for (Movie movie : moviesToRename) {
        imageFiles.addAll(movie.getMediaFiles().stream().filter(MediaFile::isGraphic).toList());
      }
      // re-build the image cache afterwards in an own thread
      if (Settings.getInstance().isImageCache() && !imageFiles.isEmpty()) {
        imageFiles.forEach(ImageCache::cacheImageAsync);
      }

      LOGGER.info("Done renaming movies)");
    }
    catch (Exception e) {
      LOGGER.error("Thread crashed", e);
      MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, "Settings.renamer", "message.renamer.threadcrashed"));
    }
  }

  /**
   * ThreadpoolWorker to work off ONE possible movie from root datasource directory
   * 
   * @author Myron Boyle
   * @version 1.0
   */
  private static class RenameMovieTask implements Callable<Object> {
    private final Movie movie;

    private RenameMovieTask(Movie movie) {
      this.movie = movie;
    }

    @Override
    public String call() {
      MovieRenamer.renameMovie(movie);
      return movie.getTitle();
    }
  }

  @Override
  public void callback(Object obj) {
    publishState((String) obj, progressDone);
  }
}
