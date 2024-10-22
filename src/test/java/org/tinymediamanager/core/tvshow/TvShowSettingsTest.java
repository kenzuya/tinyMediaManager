/*
 * Copyright 2012 - 2024 Manuel Laggner
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

package org.tinymediamanager.core.tvshow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.tinymediamanager.core.Utils;

public class TvShowSettingsTest extends BasicTvShowTest {

  @Test
  public void testTvShowSettings() {
    try {
      TvShowSettings settings = TvShowSettings.getInstance();
      assertThat(settings).isNotNull();
      settings.setAsciiReplacement(true);
      Thread.sleep(1000); // sleep here because the dirty listener is async
      settings.saveSettings();

      // cannot re-instantiate settings - need to check plain file
      String config = Utils.readFileToString(getSettingsFolder().resolve(TvShowModuleManager.getInstance().getSettings().getConfigFilename()));
      assertTrue(config.contains("\"asciiReplacement\" : true"));
    }
    catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }
}
