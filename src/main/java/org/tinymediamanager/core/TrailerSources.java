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
package org.tinymediamanager.core;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * The enum MovieTrailerSources
 *
 * @author Manuel Laggner
 */
@Deprecated
public enum TrailerSources {

  //@formatter:off
  YOUTUBE("Youtube", Collections.singletonList("youtube")),
  IMDB("IMDb", Collections.singletonList("imdb")),
  APPLE("Apple", Collections.singletonList("apple")),
  AOL("Aol", Collections.singletonList("aol")),
  HDTRAILERS("HD Trailers", Collections.singletonList("hdtrailers"));  // @formatter:on

  private final String       displayText;
  private final List<String> possibleSources;

  TrailerSources(String text, List<String> sources) {
    this.displayText = text;
    this.possibleSources = sources;
  }

  public boolean containsSource(String source) {
    if (StringUtils.isBlank(source)) {
      return false;
    }
    for (String s : possibleSources) {
      if (source.equalsIgnoreCase(s)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return this.displayText;
  }
}
