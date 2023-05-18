/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.tinymediamanager.TmmOsUtils;

/**
 * the interface {@link IAddon} is used to provide extra addons (external tools) for tinyMediaManager
 * 
 * @author Manuel Laggner
 */
public interface IAddon {
  /**
   * get the {@link Path} to the addon folder
   * 
   * @return a {@link Path} to the addon folder
   */
  default Path getAddonFolder() {
    return Paths.get(TmmOsUtils.getNativeFolderName()).resolve("addons").toAbsolutePath();
  }

  /**
   * get the addon name
   * 
   * @return the addon name
   */
  String getAddonName();

  /**
   * checks whether this addon is available or not
   * 
   * @return true/false
   */
  boolean isAvailable();

  /**
   * gets the executable filename
   * 
   * @return the filename of the executable
   */
  String getExecutableFilename();

  /**
   * get the full path to the executable
   * 
   * @return the full path to the executable
   */
  default String getExecutablePath() {
    return getAddonFolder().resolve(getExecutableFilename()).toAbsolutePath().toString();
  }
}
