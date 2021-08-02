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
package org.tinymediamanager.scraper.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;

public class ITRatingUtilTest extends BasicTest {

  @BeforeClass
  public static void setup() {
    BasicTest.setup();
  }

  @Test
  public void testRatings() throws Exception {
    assertThat(RatingUtil.getImdbRating("tt5719388").getRating()).isPositive();
    assertThat(RatingUtil.getImdbRating("tt57193881111")).isNull();
  }
}
