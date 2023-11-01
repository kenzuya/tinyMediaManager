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
package org.tinymediamanager.scraper.opensubtitles_com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.interfaces.IMovieSubtitleProvider;

public class OpenSubtitlesComMovieSubtitleProvider extends OpenSubtitlesComSubtitleProvider implements IMovieSubtitleProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSubtitlesComMovieSubtitleProvider.class);

  @Override
  protected String getSubId() {
    return "movie_subtitle";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
}
