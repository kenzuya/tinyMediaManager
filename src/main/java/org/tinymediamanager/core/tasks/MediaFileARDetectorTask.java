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

import org.tinymediamanager.core.entities.MediaFile;

public class MediaFileARDetectorTask extends ARDetectorTask {

  private final MediaFile mediaFile;

  public MediaFileARDetectorTask(MediaFile mediaFile) {
    super(TaskType.BACKGROUND_TASK);
    this.mediaFile = mediaFile;
  }

  @Override
  protected void doInBackground() {
    if (!canRun()) return;

    analyze(mediaFile);
  }
}
