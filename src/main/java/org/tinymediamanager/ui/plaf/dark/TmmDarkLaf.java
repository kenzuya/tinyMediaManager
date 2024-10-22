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
package org.tinymediamanager.ui.plaf.dark;

import org.tinymediamanager.ui.plaf.TmmLaf;

/**
 * the base class for the dark look and feel, mainly to inject loading of the right properties file
 *
 * @author Manuel Laggner
 */
public class TmmDarkLaf extends TmmLaf {
  public static boolean install() {
    return install(new TmmDarkLaf());
  }

  @Override
  public String getName() {
    return "Tmm Dark";
  }

  @Override
  public String getDescription() {
    return "Dark Look and Feel";
  }

  @Override
  public boolean isDark() {
    return true;
  }
}
