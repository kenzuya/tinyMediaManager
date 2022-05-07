package org.tinymediamanager.scraper.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.tinymediamanager.core.BasicTest;

public class MetadataUtilTest extends BasicTest {

  @Test
  public void testParseInt() {
    assertThat(MetadataUtil.parseInt("2000")).isEqualTo(2000);
    assertThat(MetadataUtil.parseInt("2.000")).isEqualTo(2000);
    assertThat(MetadataUtil.parseInt("2,000")).isEqualTo(2000);
    assertThat(MetadataUtil.parseInt("2 000")).isEqualTo(2000);
  }
}
