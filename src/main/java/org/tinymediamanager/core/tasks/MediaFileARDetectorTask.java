package org.tinymediamanager.core.tasks;

import org.tinymediamanager.core.entities.MediaFile;

public class MediaFileARDetectorTask extends ARDetectorTask {

  private final MediaFile mediaFile;

  public MediaFileARDetectorTask(MediaFile mediaFile) {
    this.mediaFile = mediaFile;
  }

  @Override
  protected void doInBackground() {
    analyze(mediaFile);
  }
}
