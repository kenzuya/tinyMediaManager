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

package org.tinymediamanager.addon;

import static org.tinymediamanager.addon.AddonManager.getAddonFolder;

import org.tinymediamanager.core.Utils;

/**
 * the interface {@link IAddon} is used to provide extra addons (external tools) for tinyMediaManager
 * 
 * @author Manuel Laggner
 */
public interface IAddon {
  /**
   * get the addon name
   * 
   * @return the addon name
   */
  String getAddonName();

  /**
   * get the full addon name (including OS/arch)
   * 
   * @return the full name of the addon including OS/arch
   */
  String getFullAddonName();

  /**
   * checks whether this addon is available or not
   * 
   * @return true/false
   */
  boolean isAvailable();

  /**
   * get the installed version of this addon
   * 
   * @return a {@link String} containing the installed version (or an empty {@link String} is not installed)
   */
  default String getVersion() {
    if (isAvailable()) {
      try {
        return Utils.readFileToString(getAddonFolder().resolve(getAddonName() + ".v"));
      }
      catch (Exception e) {
        return "";
      }
    }
    return "";
  }
}
