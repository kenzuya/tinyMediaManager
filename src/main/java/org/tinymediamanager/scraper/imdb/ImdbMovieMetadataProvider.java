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
package org.tinymediamanager.scraper.imdb;

import static org.tinymediamanager.scraper.imdb.ImdbParser.INCLUDE_SHORT;
import static org.tinymediamanager.scraper.imdb.ImdbParser.INCLUDE_TV_MOVIE;
import static org.tinymediamanager.scraper.imdb.ImdbParser.INCLUDE_TV_SERIES;
import static org.tinymediamanager.scraper.imdb.ImdbParser.INCLUDE_VIDEOGAME;
import static org.tinymediamanager.scraper.imdb.ImdbParser.LOCAL_RELEASE_DATE;
import static org.tinymediamanager.scraper.imdb.ImdbParser.MAX_KEYWORD_COUNT;
import static org.tinymediamanager.scraper.imdb.ImdbParser.SCRAPE_COLLETION_INFO;
import static org.tinymediamanager.scraper.imdb.ImdbParser.SCRAPE_KEYWORDS_PAGE;
import static org.tinymediamanager.scraper.imdb.ImdbParser.SCRAPE_LANGUAGE_NAMES;
import static org.tinymediamanager.scraper.imdb.ImdbParser.SCRAPE_UNCREDITED_ACTORS;
import static org.tinymediamanager.scraper.imdb.ImdbParser.USE_TMDB_FOR_MOVIES;

import java.util.SortedSet;

import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;

/**
 * the class {@link ImdbMovieMetadataProvider} provides meta data for movies
 *
 * @author Manuel Laggner
 */
public class ImdbMovieMetadataProvider extends ImdbMetadataProvider implements IMovieMetadataProvider, IMovieImdbMetadataProvider {

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = super.createMediaProviderInfo();

    // configure/load settings
    providerInfo.getConfig().addBoolean(INCLUDE_TV_MOVIE, true);
    providerInfo.getConfig().addBoolean(INCLUDE_SHORT, false);
    providerInfo.getConfig().addBoolean(INCLUDE_TV_SERIES, false);
    providerInfo.getConfig().addBoolean(INCLUDE_VIDEOGAME, false);
    providerInfo.getConfig().addBoolean(USE_TMDB_FOR_MOVIES, false);
    providerInfo.getConfig().addBoolean(SCRAPE_COLLETION_INFO, false);
    providerInfo.getConfig().addBoolean(LOCAL_RELEASE_DATE, true);
    providerInfo.getConfig().addBoolean(SCRAPE_UNCREDITED_ACTORS, true);
    providerInfo.getConfig().addBoolean(SCRAPE_LANGUAGE_NAMES, true);
    providerInfo.getConfig().addBoolean(SCRAPE_KEYWORDS_PAGE, false);
    providerInfo.getConfig().addInteger(MAX_KEYWORD_COUNT, 10);

    providerInfo.getConfig().load();

    return providerInfo;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    return (new ImdbMovieParser(this, executor)).search(options);
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    return (new ImdbMovieParser(this, executor)).getMovieMetadata(options);
  }
}
