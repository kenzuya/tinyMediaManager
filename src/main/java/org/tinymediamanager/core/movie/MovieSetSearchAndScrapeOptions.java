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
package org.tinymediamanager.core.movie;

import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaType;

/**
 * The class MovieSetSearchAndScrapeOptions is used to hold scrape and search related data for movie sets
 *
 * @author Manuel Laggner
 */
public class MovieSetSearchAndScrapeOptions extends MediaSearchAndScrapeOptions {

  public MovieSetSearchAndScrapeOptions() {
    super(MediaType.MOVIE_SET);
  }

  /**
   * copy constructor
   *
   * @param original
   *          the original to copy
   */
  public MovieSetSearchAndScrapeOptions(MovieSetSearchAndScrapeOptions original) {
    super(original);
  }

  /**
   * Load default Settings.
   */
  public void loadDefaults() {
    language = MovieModuleManager.getInstance().getSettings().getScraperLanguage();

    // metadata
    metadataScraper = MovieModuleManager.getInstance().getMovieList().getDefaultMediaScraper();

    // artwork
    artworkScrapers.addAll(MovieModuleManager.getInstance().getMovieList().getDefaultArtworkScrapers());

    // trailer
    trailerScrapers.addAll(MovieModuleManager.getInstance().getMovieList().getDefaultTrailerScrapers());

    // subtitle
    subtitleScrapers.addAll(MovieModuleManager.getInstance().getMovieList().getDefaultSubtitleScrapers());
  }
}
