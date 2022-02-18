package org.tinymediamanager.scraper.util;

import org.junit.Assert;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;

public class TvUtilsTest extends BasicTest {

  @Test
  public void testEpisodeNumber() {
    Assert.assertEquals(-1, TvUtils.getEpisodeNumber(0)); // 0 episode not possible
    Assert.assertEquals(-1, TvUtils.getEpisodeNumber((Object[]) null));

    Assert.assertEquals(2, TvUtils.getEpisodeNumber(2.2));
    Assert.assertEquals(2, TvUtils.getEpisodeNumber(2.2f));
    Assert.assertEquals(2, TvUtils.getEpisodeNumber(2));
    Assert.assertEquals(2, TvUtils.getEpisodeNumber("2"));
    Assert.assertEquals(2, TvUtils.getEpisodeNumber(2));

    Assert.assertEquals(2, TvUtils.getEpisodeNumber("", null, -1, 0, (float) -2.2, -2.2, 2));
  }

  @Test
  public void testSeasonNumber() {
    Assert.assertEquals(0, TvUtils.getSeasonNumber(0)); // 0 season IS possible
    Assert.assertEquals(-1, TvUtils.getSeasonNumber((Object[]) null));

    Assert.assertEquals(2, TvUtils.getSeasonNumber(2.2));
    Assert.assertEquals(2, TvUtils.getSeasonNumber(2.2f));
    Assert.assertEquals(2, TvUtils.getSeasonNumber(2));
    Assert.assertEquals(2, TvUtils.getSeasonNumber("2"));
    Assert.assertEquals(2, TvUtils.getSeasonNumber(2));

    Assert.assertEquals(2, TvUtils.getSeasonNumber("", null, -1, 2, (float) -2.2, -2.2, 0));
  }

  @Test
  public void testIntParsing() {
    Assert.assertEquals(2, TvUtils.parseInt(2.2));
    Assert.assertEquals(0, TvUtils.parseInt(0));
    Assert.assertEquals(0, TvUtils.parseInt((Object[]) null));

    Assert.assertEquals(2, TvUtils.parseInt(2.2f));
    Assert.assertEquals(2, TvUtils.parseInt(2));
    Assert.assertEquals(2, TvUtils.parseInt("2"));
    Assert.assertEquals(2, TvUtils.parseInt(2));

    Assert.assertEquals(2, TvUtils.parseInt("", null, -1, 0, (float) -2.2, -2.2, 2));
  }

}
