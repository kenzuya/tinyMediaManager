package org.tinymediamanager.core.mediainfo;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.entities.MediaFile;

// http://blog.ampedsoftware.com/2016/03/14/introduction-to-aspect-ratio/ 

public class AspectRatioTests extends BasicTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AspectRatioTests.class);

  @Before
  public void setup() throws Exception {
    super.setup();
    copyResourceFolderToWorkFolder("aspectRatios");
  }

  @Test
  public void AR169() {
    MediaFile mf = new MediaFile(getWorkFolder().resolve("aspectRatios/720x572_AR_16x9.avi"));
    mf.gatherMediaInformation();
    log(mf);
    assertEqual(1.78f, mf.getAspectRatio());
  }

  @Test
  public void ARUncropped() {
    MediaFile mf = new MediaFile(getWorkFolder().resolve("aspectRatios/AR 1.85_1 - uncropped 16x9.avi"));
    mf.gatherMediaInformation();
    log(mf);
    assertEqual(1.85f, mf.getAspectRatio());
  }

  @Test
  public void DARandCrop() {
    MediaFile mf = new MediaFile(getWorkFolder().resolve("aspectRatios/DAR and a crop flag - AR is 1.66_1.avi"));
    mf.gatherMediaInformation();
    log(mf);
    assertEqual(1.66f, mf.getAspectRatio());
  }

  @Test
  public void unusualDAR() {
    MediaFile mf = new MediaFile(getWorkFolder().resolve("aspectRatios/unusual DAR flags, actual AR is 1.6_1.avi"));
    mf.gatherMediaInformation();
    log(mf);
    assertEqual(1.66f, mf.getAspectRatio());
  }

  private void log(MediaFile mf) {
    LOGGER.info("AR: {}  Resolution: {}", mf.getAspectRatio(), mf.getVideoResolution());
    LOGGER.debug("************************************");
  }
}
