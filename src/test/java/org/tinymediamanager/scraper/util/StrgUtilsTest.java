package org.tinymediamanager.scraper.util;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;

public class StrgUtilsTest extends BasicTest {

  @Test
  public void testCompareVersion() {
    Assert.assertTrue(StrgUtils.compareVersion("2.7", "2.7-SNAPSHOT") > 0);
    Assert.assertTrue(StrgUtils.compareVersion("2.7-SNAPSHOT", "2.7") < 0);
    Assert.assertTrue(StrgUtils.compareVersion("2.7-SNAPSHOT", "2.7.1") < 0);
    Assert.assertTrue(StrgUtils.compareVersion("2.7.1", "2.7.2-SNAPSHOT") < 0);
    Assert.assertTrue(StrgUtils.compareVersion("2.6.9", "2.7-SNAPSHOT") < 0);
    Assert.assertTrue(StrgUtils.compareVersion("2.7.1-SNAPSHOT", "2.7.2-SNAPSHOT") < 0);

    Assert.assertTrue(StrgUtils.compareVersion("2.7.1", "2.7.1") == 0);
    // Assert.assertTrue(StrgUtils.compareVersion("SVN", "2.7.1") < 0); // dunno how to get actual version for comparison
    Assert.assertTrue(StrgUtils.compareVersion("2.7.1-SNAPSHOT", "2.7.1-SNAPSHOT") < 0); // same snapshot should be considered as lower!
    Assert.assertTrue(StrgUtils.compareVersion("SVN", "SVN") < 0); // dito for SVN
  }

  @Test
  public void hex() {
    Assert.assertEquals("6162636465666768", StrgUtils.bytesToHex("abcdefgh".getBytes()));
  }

  @Test
  public void parseDateTests() throws ParseException {
    Assert.assertNotNull(StrgUtils.parseDate("11 Oct. 2001"));
    Assert.assertNotNull(StrgUtils.parseDate("11 Okt. 2001"));
    Assert.assertNotNull(StrgUtils.parseDate("11 Dic. 2001"));
    Assert.assertNotNull(StrgUtils.parseDate("1 Okt. 2001"));
    Assert.assertNotNull(StrgUtils.parseDate("01 Okt. 2001"));
    Assert.assertNotNull(StrgUtils.parseDate("1. Oktober 2001"));
    Assert.assertNotNull(StrgUtils.parseDate("11 Okt..... 2001"));
    Assert.assertNotNull(StrgUtils.parseDate("2019-02-12"));
    Assert.assertNotNull(StrgUtils.parseDate("12-02-2019"));
    Assert.assertNotNull(StrgUtils.parseDate("2019-02-12 15:16"));
    Assert.assertNotNull(StrgUtils.parseDate("2019-02-12 15:16:13"));
    Assert.assertNotNull(StrgUtils.parseDate("2021-04-21T21:08:22.451Z"));
    Assert.assertNotNull(StrgUtils.parseDate("2014-12-25T09:31:55Z"));
  }
}
