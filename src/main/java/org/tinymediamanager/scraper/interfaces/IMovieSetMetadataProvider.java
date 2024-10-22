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
package org.tinymediamanager.scraper.interfaces;

import java.util.List;

import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

/**
 * The Interface {@link IMovieSetMetadataProvider}. All scrapers providing movie set meta data must implement this interface
 * 
 * @author Myron Boyle, Manuel Laggner
 * @since 3.0
 */
public interface IMovieSetMetadataProvider extends IMediaProvider {

  /**
   * Gets the meta data.
   * 
   * @param options
   *          the options
   * @return the meta data
   * @throws ScrapeException
   *           any exception which can be thrown while scraping
   * @throws MissingIdException
   *           indicates that there was no usable id to scrape
   * @throws NothingFoundException
   *           indicated that nothing has been found
   */
  MediaMetadata getMetadata(MovieSetSearchAndScrapeOptions options) throws ScrapeException, MissingIdException, NothingFoundException;

  /**
   * Search for media.
   * 
   * @param options
   *          the options
   * @return the list
   * @throws ScrapeException
   *           any exception which can be thrown while scraping
   */
  List<MediaSearchResult> search(MovieSetSearchAndScrapeOptions options) throws ScrapeException;
}
