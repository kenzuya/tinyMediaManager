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
package org.tinymediamanager.core.movie;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;

/**
 * The enum MovieSetScraperMetadataConfig is used to control which fields will be set after scraping
 * 
 * @author Manuel Laggner
 */
public enum MovieSetScraperMetadataConfig implements ScraperMetadataConfig {
  // meta data
  ID(Type.METADATA),
  TITLE(Type.METADATA),
  PLOT(Type.METADATA),
  RATING(Type.METADATA),

  // artwork
  POSTER(Type.ARTWORK),
  FANART(Type.ARTWORK),
  BANNER(Type.ARTWORK),
  CLEARART(Type.ARTWORK),
  THUMB(Type.ARTWORK),
  LOGO(Type.ARTWORK),
  CLEARLOGO(Type.ARTWORK),
  DISCART(Type.ARTWORK, "mediafiletype.disc");

  private final Type   type;
  private final String description;
  private final String tooltip;

  MovieSetScraperMetadataConfig(Type type) {
    this(type, null, null);
  }

  MovieSetScraperMetadataConfig(Type type, String description) {
    this(type, description, null);
  }

  MovieSetScraperMetadataConfig(Type type, String description, String tooltip) {
    this.type = type;
    this.description = description;
    this.tooltip = tooltip;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getDescription() {
    if (StringUtils.isBlank(description)) {
      try {
        if (type == Type.ARTWORK) {
          return TmmResourceBundle.getString("mediafiletype." + name().toLowerCase(Locale.ROOT));
        }
        else {
          return TmmResourceBundle.getString("metatag." + name().toLowerCase(Locale.ROOT));
        }
      }
      catch (Exception ignored) {
        // just not crash
      }
    }
    else {
      try {
        return TmmResourceBundle.getString(description);
      }
      catch (Exception ignored) {
        // just not crash
      }
    }
    return "";
  }

  @Override
  public String getToolTip() {
    if (StringUtils.isBlank(tooltip)) {
      return null;
    }
    try {
      return TmmResourceBundle.getString(tooltip);
    }
    catch (Exception ignored) {
      // just not crash
    }
    return null;
  }
}
