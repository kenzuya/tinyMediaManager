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
package org.tinymediamanager.core.tasks;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.EmptyFileException;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmThreadPool;

/**
 * The Class ImageCacheTask. Cache a bunch of images in a separate task
 * 
 * @author Manuel Laggner
 */
public class ImageCacheTask extends TmmThreadPool {
  private static final Logger         LOGGER       = LoggerFactory.getLogger(ImageCacheTask.class);

  private final Collection<MediaFile> filesToCache = new ArrayList<>();

  @Override
  public void callback(Object obj) {
    publishState(progressDone);
  }

  public ImageCacheTask(Collection<MediaFile> files) {
    super(TmmResourceBundle.getString("tmm.rebuildimagecache"));
    filesToCache.addAll(files);
  }

  @Override
  protected void doInBackground() {
    if (!Settings.getInstance().isImageCache()) {
      return;
    }

    // distribute the work over all available cores
    int threadCount = Runtime.getRuntime().availableProcessors() - 1;
    if (threadCount < 2) {
      threadCount = 2;
    }

    initThreadPool(threadCount, "imageCache");

    for (MediaFile fileToCache : filesToCache) {
      if (cancel) {
        return;
      }
      submitTask(new CacheTask(fileToCache));
    }
    waitForCompletionOrCancel();
  }

  private static class CacheTask implements Callable<Object> {
    private final MediaFile fileToCache;

    CacheTask(MediaFile fileToCache) {
      this.fileToCache = fileToCache;
    }

    @Override
    public Object call() {
      try {
        // sleep 50ms to let the system calm down from a previous task
        Thread.sleep(50);
        ImageCache.cacheImage(fileToCache);
      }
      catch (EmptyFileException e) {
        LOGGER.debug("failed to cache file (file is empty): {}", fileToCache);
      }
      catch (FileNotFoundException e) {
        // silently ignore
      }
      catch (Exception e) {
        LOGGER.warn("failed to cache file: {} - {}", fileToCache.getFile(), e.getMessage());
      }
      return null;
    }
  }
}
