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

package org.tinymediamanager.ui.movies;

import java.util.Locale;

import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.ui.ScraperInTable;

/**
 * the class {@link MovieScraper} is used to display the movie scraper in a table
 * 
 * @author Manuel Laggner
 */
public class MovieScraper extends ScraperInTable implements Comparable<MovieScraper> {
  private boolean defaultScraper = false;

  public MovieScraper(MediaScraper scraper) {
    super(scraper);
  }

  private boolean isKodiScraper() {
    return scraper.getName().startsWith("Kodi");
  }

  public Boolean getDefaultScraper() {
    return defaultScraper;
  }

  public void setDefaultScraper(Boolean newValue) {
    if (isEnabled()) {
      Boolean oldValue = this.defaultScraper;
      this.defaultScraper = newValue;
      firePropertyChange("defaultScraper", oldValue, newValue);
    }
  }

  public MediaScraper getMediaScraper() {
    return scraper;
  }

  @Override
  public int compareTo(MovieScraper o) {
    if (isKodiScraper() && !o.isKodiScraper()) {
      return 1;
    }

    if (!isKodiScraper() && o.isKodiScraper()) {
      return -1;
    }

    return scraper.getName().toLowerCase(Locale.ROOT).compareTo(o.scraper.getName().toLowerCase(Locale.ROOT));
  }
}
