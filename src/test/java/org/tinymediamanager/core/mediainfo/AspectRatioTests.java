package org.tinymediamanager.core.mediainfo;

import java.nio.file.Paths;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaFile;

public class AspectRatioTests {
  private static final Logger LOGGER = LoggerFactory.getLogger(AspectRatioTests.class);

  @Test
  public void AR169() {
    MediaFile mf = new MediaFile(Paths.get("src/test/resources/aspectRatios/720x572_AR_16x9.avi"));
    mf.gatherMediaInformation();
    log(mf);
  }

  @Test
  public void ARUncropped() {
    MediaFile mf = new MediaFile(Paths.get("src/test/resources/aspectRatios/AR 1.85_1 - uncropped 16x9.avi"));
    mf.gatherMediaInformation();
    log(mf);
  }

  @Test
  public void DARandCrop() {
    MediaFile mf = new MediaFile(Paths.get("src/test/resources/aspectRatios/DAR and a crop flag - AR is 1.66_1.avi"));
    mf.gatherMediaInformation();
    log(mf);
  }

  @Test
  public void unusualDAR() {
    MediaFile mf = new MediaFile(Paths.get("src/test/resources/aspectRatios/unusual DAR flags, actual AR is 1.6_1.avi"));
    mf.gatherMediaInformation();
    log(mf);
  }

  private void log(MediaFile mf) {
    LOGGER.info("AR: {}  Resolution: {}", mf.getAspectRatio(), mf.getVideoResolution());
    LOGGER.debug("************************************");
  }
}
