package org.tinymediamanager.core.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.thirdparty.FFmpeg;

import java.nio.file.Path;

public class ARDetectorTask implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ARDetectorTask.class);

  private final Path path;

  public ARDetectorTask(Path path) {
    this.path = path;
  }

  @Override
  public void run() {
    getMetaData();
  }

  private void getMetaData() {
    try {
      String result = FFmpeg.getMetaData(this.path);
      if (StringUtils.isNotEmpty(result)) {
        String[] metaData = result.split("\\s");
        if (metaData != null && metaData.length == 4) {
          double videoWidth = Double.valueOf(metaData[0]);
          double videoHeight = Double.valueOf(metaData[1]);
          double arReported = videoWidth / videoHeight;
          String[] sampleARValues = metaData[2].split(":");
          double sampleAR = 0d;
          if (Double.valueOf(sampleARValues[0]) > 0 && Double.valueOf(sampleARValues[1]) > 0) {
            sampleAR = Double.valueOf(sampleARValues[0]) / Double.valueOf(sampleARValues[1]);
          }
          if (sampleAR > 0.5d) sampleAR = 1d;

          double arEncoded = Math.round(arReported * sampleAR * 1E6) / 1E6;
          int duration = Double.valueOf(metaData[3]).intValue();
          LOGGER.info("Video duration (hh:mm:ss): " + duration);
          if (duration == 0) {
            LOGGER.warn("Video duration 0 seconds. Skipping this video.");
          }

        }
      }
    } catch (Exception ex) {
      LOGGER.error("Error scanning file: " + path.toString());
    }
  }
}
