package org.tinymediamanager.youtube;

import org.junit.Assert;
import org.junit.Test;
import org.tinymediamanager.scraper.util.youtube.model.Itag;
import org.tinymediamanager.scraper.util.youtube.model.MediaDetails;
import org.tinymediamanager.scraper.util.youtube.model.YoutubeMedia;
import org.tinymediamanager.scraper.util.youtube.model.formats.AudioVideoFormat;
import org.tinymediamanager.scraper.util.youtube.model.formats.Format;

public class ITYtDownloaderTest {

  private static final String videoId = "I0iBBzyiRes";            // ANGEL HAS FALLEN Trailer German Deutsch
  private static YoutubeMedia media   = new YoutubeMedia(videoId);

  @Test
  public void A_ParsingDataTest() throws Exception {
    String videoId = "8BeLUQRomPA"; // ANGEL HAS FALLEN Trailer German Deutsch
    YoutubeMedia media = new YoutubeMedia(videoId);
    media.parseVideo();
    MediaDetails details = media.getDetails();

    // Video Details
    Assert.assertEquals(details.getVideoId(), "8BeLUQRomPA");
    Assert.assertEquals(details.getTitle(), "ANGEL HAS FALLEN Trailer German Deutsch (2019) Exklusiv");
    Assert.assertEquals(details.getAuthor(), "KinoCheck");
    Assert.assertEquals(details.getLengthSeconds(), 79);

  }

  @Test
  public void A_ParsingDataTestSignature() throws Exception {
    String videoId = "kJQP7kiw5Fk"; // Luis Fonsi - Despacito ft. Daddy Yankee
    YoutubeMedia media = new YoutubeMedia(videoId);
    media.parseVideo();
    MediaDetails details = media.getDetails();

    // Video Details
    Assert.assertEquals(details.getVideoId(), "kJQP7kiw5Fk");
    Assert.assertEquals(details.getTitle(), "Luis Fonsi - Despacito ft. Daddy Yankee");

  }

  @Test
  public void saltTest() throws Exception {
    String videoId = "LbkQTB-OJsk"; // SALT

    YoutubeMedia media = new YoutubeMedia(videoId);
    media.parseVideo();
    MediaDetails details = media.getDetails();

    // Video details
    Assert.assertEquals(details.getVideoId(), "LbkQTB-OJsk");
    Assert.assertEquals(details.getTitle(), "Watch the new SALT trailer, starring Angelina Jolie");

    // check if we have the desired codec
    Format itag22 = null;
    for (Format format : media.getFormats()) {
      if (format instanceof AudioVideoFormat && format.itag() == Itag.I_22) {
        itag22 = format;
        break;
      }
    }

    Assert.assertNotNull(itag22);
  }
}
