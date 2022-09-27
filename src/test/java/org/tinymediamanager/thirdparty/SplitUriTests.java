package org.tinymediamanager.thirdparty;

import org.junit.Assert;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;

public class SplitUriTests extends BasicTest {

  // TODO: delimit on " , "

  // C:\Users\mamk\Videos\Filme\Abyss, The\Abyss, The.avi
  // C:\Users\mamk\Videos\Filme\testDVD\VIDEO_TS\VIDEO_TS.IFO

  // =stack://C:\Users\mamk\Videos\Filme\Mulholland Drive\Mulholland Drive cd1.avi , C:\Users\mamk\Videos\Filme\Mulholland Drive\Mulholland Drive
  // cd2.avi

  // =stack://
  // zip://C%3a%5cUsers%5cmamk%5cVideos%5cFilme%5cAvatar%20-%20Aufbruch%20nach%20Pandora%20(2009).zip/Avatar - Aufbruch nach Pandora (2009)/Avatar -
  // Aufbruch nach Pandora (2009) (7.0) cd1.avi ,
  // zip://C%3a%5cUsers%5cmamk%5cVideos%5cFilme%5cAvatar%20-%20Aufbruch%20nach%20Pandora%20(2009).zip/Avatar - Aufbruch nach Pandora (2009)/Avatar -
  // Aufbruch nach Pandora (2009) (7.0) cd2.avi

  @Test
  public void testUris() {
    String movieFwd = "movie (2020)/movie.avi";
    String movieBack = "movie (2020)\\movie.avi";

    // schema, creds, host:port
    testUri("dav://user:pass@localhost:12345/videos/", movieFwd);

    // schema, creds
    testUri("smb://user:pass@localhost/videos/", movieFwd);

    // no schema, creds
    testUri("user:pass@localhost/videos/", movieFwd); // shall fail?

    // UPNP
    testUri("upnp://886fc236-b611-0730-0000-000017107649/1/videos/", movieFwd);

    // file
    testUri("file:///videos/", movieFwd);
    testUri("file://videos/", movieFwd);
    testUri("file:\\\\videos\\", movieBack, movieFwd); // 2 backslashes are smb!
    testUri("file:\\\\\\videos\\", movieBack, movieFwd);

    // local
    testUri("/videos/", movieFwd);
    testUri("c:\\videos\\", movieBack, movieFwd);

    // windows SMB
    testUri("\\\\localhost\\videos\\ ", movieBack, movieFwd);

    // testUri("",
    // "zip:///C%3a%5cUsers%5cmamk%5cVideos%5cFilme%5cAvatar%20-%20Aufbruch%20nach%20Pandora%20(2009).zip/Avatar - Aufbruch nach Pandora (2009)/Avatar
    // - Aufbruch nach Pandora (2009) (7.0) cd1.avi",
    // "dunno");
  }

  private void testUri(String ds, String movie) {
    testUri(ds, movie, movie);
  }

  private void testUri(String ds, String movie, String test) {
    // submit absolute
    String abs = ds + movie;
    SplitUri s = new SplitUri(ds, abs);
    // should be split again
    System.out.println(s);
    Assert.assertEquals(test, s.file);
  }

  @Test
  public void testUriMatching() {
    // enter a valid hostname, else it will take long ;)

    // same
    String s1 = "smb://localhost/public/TMM/testmovies/101 Dalmatiner/101 Dalmatiner #2.avi";
    String s2 = "\\\\127.0.0.1\\public\\TMM\\testmovies\\101 Dalmatiner\\101 Dalmatiner #2.avi";
    Assert.assertEquals(new SplitUri("smb://localhost/public/TMM/testmovies", s1), new SplitUri("\\\\127.0.0.1\\public\\TMM\\testmovies", s2));

    // no file
    s1 = "smb://192.168.1.10/Series/The Magicians (2015)/";
    s2 = "\\\\127.0.0.1\\Series\\The Magicians (2015)";
    Assert.assertEquals(new SplitUri("smb://192.168.1.10/Series", s1), new SplitUri("\\\\127.0.0.1\\Series", s2));

    // datasource only
    s1 = "smb://127.0.0.1/share";
    s2 = "\\\\127.0.0.1\\share";
    Assert.assertEquals(new SplitUri(s1, s1), new SplitUri(s2, s2));
    Assert.assertEquals(new SplitUri(s1, ""), new SplitUri(s2, ""));

    // other datasource
    s1 = "smb://localhost/public/TMM/testmovies/101 Dalmatiner/101 Dalmatiner #2.avi";
    s2 = "\\\\127.0.0.1\\public\\TMM\\newmovies\\101 Dalmatiner\\101 Dalmatiner #2.avi";
    Assert.assertEquals(new SplitUri("smb://localhost/public/TMM/testmovies/", s1), new SplitUri("\\\\127.0.0.1\\public\\TMM\\newmovies", s2));

    /////////////////////////////// NEGATIVE TESTS
    // wrong parent
    s1 = "smb://localhost/public/TMM/testmovies/101 Dalmatiner/101 Dalmatiner #2.avi";
    s2 = "\\\\127.0.0.1\\public\\TMM\\testmovies\\no Dalmatiner\\101 Dalmatiner #2.avi";
    Assert.assertNotEquals(new SplitUri("smb://localhost/public/TMM/testmovies/", s1), new SplitUri("\\\\127.0.0.1\\public\\TMM\\testmovies", s2));

    // no datasource
    s1 = "smb://192.168.1.10/Series/The Magicians (2015)/";
    s2 = "\\\\127.0.0.1\\Series\\The Magicians (2015)";
    Assert.assertNotEquals(new SplitUri("", s1), new SplitUri("", s2));
  }

}
