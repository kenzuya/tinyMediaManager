/*
 * Copyright 2012 - 2021 Manuel Laggner
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaTrailer;

/**
 * A task for downloading trailers
 *
 * @author Manuel Laggner
 */
public abstract class TrailerDownloadTask extends DownloadTask {

  private final MediaTrailer mediaTrailer;

  protected TrailerDownloadTask(MediaTrailer trailer) {
    super(TmmResourceBundle.getString("trailer.download") + " - " + trailer.getName(), trailer.getUrl());
    this.mediaTrailer = trailer;

    setTaskDescription(trailer.getName());
  }

  @Override
  protected void doInBackground() {
    if (!isFeatureEnabled()) {
      return;
    }
    super.doInBackground();
  }

  @Override
  protected String getSpecialUserAgent() {
    if ("apple".equalsIgnoreCase(mediaTrailer.getProvider())) {
      return "QuickTime";
    }
    return null;
  }

  @Override
  protected void checkDownloadedFile() throws IOException {
    super.checkDownloadedFile();

    BasicFileAttributes view = Files.readAttributes(tempFile, BasicFileAttributes.class);

    // the trailer must not be smaller than 1MB
    if (view.size() < 1000000) {
      // just cancel - cleanup is in DownloadTask
      cancel = true;
    }
  }
}
