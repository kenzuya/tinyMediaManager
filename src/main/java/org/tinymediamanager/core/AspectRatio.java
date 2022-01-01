/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class AspectRatio {

  public static Map<Float, String> getDefaultValues() {
    LinkedHashMap<Float, String> predefinedValues = new LinkedHashMap<>();
    predefinedValues.put(1.33f, "4:3 (1.33:1)");
    predefinedValues.put(1.37f, "11:8 (1.37:1)");
    predefinedValues.put(1.43f, "IMAX (1.43:1)");
    predefinedValues.put(1.56f, "14:9 (1.56:1)");
    predefinedValues.put(1.66f, "5:3 (1.66:1)");
    predefinedValues.put(1.78f, "16:9 (1.78:1)");
    predefinedValues.put(1.85f, "Widescreen (1.85:1)");
    predefinedValues.put(1.90f, "Digital IMAX (1.90:1)");
    predefinedValues.put(2.00f, "18:9 (2.00:1)");
    predefinedValues.put(2.20f, "70mm (2.20:1)");
    predefinedValues.put(2.35f, "Anamorphic (2.35:1)");
    predefinedValues.put(2.40f, "Anamorphic widescreen (2.39:1 & 12:5)");
    predefinedValues.put(2.55f, "CinemaScope 55 (2.55:1)");
    predefinedValues.put(2.76f, "Ultra Panavision 70 (2.76:1)");
    return predefinedValues;
  }

}
