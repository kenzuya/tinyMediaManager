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
package org.tinymediamanager.addon;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;

/**
 * the class {@link YtDlpAddon} provides native integration of yt-dlp
 * 
 * @author Manuel Laggner
 */
public class YtDlpAddon implements IAddon {
  private static final String ADDON_NAME = "yt-dlp";

  @Override
  public String getAddonName() {
    return ADDON_NAME;
  }

  @Override
  public boolean isAvailable() {
    Path addonFolder = getUserAddonFolder();

    if (!Files.exists(addonFolder)) {
      return false;
    }

    if (Files.exists(addonFolder.resolve(getExecutableFilename()))) {
      return true;
    }

    return false;
  }

  @Override
  public String getExecutableFilename() {
    if (SystemUtils.IS_OS_WINDOWS) {
      return "yt-dlp.exe";
    }
    return "yt-dlp";
  }

  /**
   * get the full path to the executable
   *
   * @return the full path to the executable
   */
  @Override
  public String getExecutablePath() {
    return getUserAddonFolder().resolve(getExecutableFilename()).toAbsolutePath().toString();
  }
}
