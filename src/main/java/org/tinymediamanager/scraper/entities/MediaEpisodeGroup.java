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

package org.tinymediamanager.scraper.entities;

import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * this enum is used to set different episode groups
 *
 * @author Manuel Laggner
 */
public class MediaEpisodeGroup implements Comparable<MediaEpisodeGroup> {

  public enum EpisodeGroup {
    AIRED,
    DVD,
    ABSOLUTE,
    ALTERNATE,
    DISPLAY
  }

  public static final MediaEpisodeGroup DEFAULT_AIRED    = new MediaEpisodeGroup(EpisodeGroup.AIRED);
  public static final MediaEpisodeGroup DEFAULT_DVD      = new MediaEpisodeGroup(EpisodeGroup.DVD);
  public static final MediaEpisodeGroup DEFAULT_ABSOLUTE = new MediaEpisodeGroup(EpisodeGroup.ABSOLUTE);
  public static final MediaEpisodeGroup DEFAULT_DISPLAY  = new MediaEpisodeGroup(EpisodeGroup.DISPLAY);

  @JsonProperty
  private final EpisodeGroup            episodeGroup;
  @JsonProperty
  private final String                  name;

  public MediaEpisodeGroup(String name) {
    this(EpisodeGroup.AIRED, name);
  }

  public MediaEpisodeGroup(EpisodeGroup episodeGroup) {
    this(episodeGroup, "");
  }

  @JsonCreator
  public MediaEpisodeGroup(@NotNull @JsonProperty("episodeGroup") EpisodeGroup episodeGroup, @JsonProperty("name") String name) {
    this.episodeGroup = episodeGroup;
    this.name = name != null ? name : "";
  }

  public EpisodeGroup getEpisodeGroup() {
    return episodeGroup;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    String localizedEnumName = TmmResourceBundle.getString("episodeGroup." + episodeGroup.name().toLowerCase(Locale.ROOT));
    if (StringUtils.isNotBlank(name)) {
      return name + " (" + localizedEnumName + ")";
    }
    else {
      return localizedEnumName;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MediaEpisodeGroup that = (MediaEpisodeGroup) o;
    return episodeGroup == that.episodeGroup && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(episodeGroup, name);
  }

  @Override
  public int compareTo(@NotNull MediaEpisodeGroup o) {
    int result = Integer.compare(episodeGroup.ordinal(), o.episodeGroup.ordinal());

    if (result == 0) {
      result = name.compareTo(o.name);
    }

    return result;
  }
}
