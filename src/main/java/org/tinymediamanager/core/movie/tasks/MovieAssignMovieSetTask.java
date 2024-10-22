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
package org.tinymediamanager.core.movie.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieArtworkHelper;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;

/**
 * The class MovieAssignMovieSetTask. A task to assign the movie set to the given movies
 * 
 * @author Manuel Laggner
 */
public class MovieAssignMovieSetTask extends TmmThreadPool {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieAssignMovieSetTask.class);

  private final List<Movie>   moviesToScrape;

  public MovieAssignMovieSetTask(List<Movie> moviesToScrape) {
    super(TmmResourceBundle.getString("movie.assignmovieset"));
    this.moviesToScrape = new ArrayList<>(moviesToScrape);
  }

  @Override
  protected void doInBackground() {
    initThreadPool(1, "scrape");
    start();

    for (Movie movie : moviesToScrape) {
      submitTask(new Worker(movie));
    }
    waitForCompletionOrCancel();
    LOGGER.info("Done assigning movies to movie sets");
  }

  private static class Worker implements Runnable {
    private final MovieList movieList = MovieModuleManager.getInstance().getMovieList();
    private final Movie     movie;

    public Worker(Movie movie) {
      this.movie = movie;
    }

    @Override
    public void run() {
      if (movie.getMovieSet() != null) {
        return;
      }
      try {
        MediaScraper movieScraper = MediaScraper.getMediaScraperById(MediaMetadata.TMDB, ScraperType.MOVIE);
        MediaScraper movieSetScraper = MediaScraper.getMediaScraperById(MediaMetadata.TMDB, ScraperType.MOVIE_SET);

        MovieSearchAndScrapeOptions movieOptions = new MovieSearchAndScrapeOptions();
        movieOptions.setLanguage(MovieModuleManager.getInstance().getSettings().getScraperLanguage());
        movieOptions.setCertificationCountry(MovieModuleManager.getInstance().getSettings().getCertificationCountry());
        movieOptions.setReleaseDateCountry(MovieModuleManager.getInstance().getSettings().getReleaseDateCountry());

        for (Entry<String, Object> entry : movie.getIds().entrySet()) {
          movieOptions.setId(entry.getKey(), entry.getValue().toString());
        }

        MediaMetadata md = ((IMovieMetadataProvider) movieScraper.getMediaProvider()).getMetadata(movieOptions);
        int collectionId = 0;
        try {
          collectionId = (int) md.getId(MediaMetadata.TMDB_SET);
        }
        catch (Exception e) {
          LOGGER.warn("Could not parse collectionId: {}", md.getId(MediaMetadata.TMDB_SET));
        }

        if (collectionId > 0) {
          String collectionName = md.getCollectionName();
          MovieSet movieSet = movieList.getMovieSet(collectionName, collectionId);
          if (movieSet != null && movieSet.getTmdbId() == 0) {
            movieSet.setTmdbId(collectionId);
            // get movieset metadata
            try {
              MovieSetSearchAndScrapeOptions movieSetOptions = new MovieSetSearchAndScrapeOptions();
              movieSetOptions.setTmdbId(collectionId);
              movieSetOptions.setLanguage(MovieModuleManager.getInstance().getSettings().getScraperLanguage());

              MediaMetadata info = ((IMovieSetMetadataProvider) movieSetScraper.getMediaProvider()).getMetadata(movieSetOptions);
              if (info != null && StringUtils.isNotBlank(info.getTitle())) {
                movieSet.setTitle(info.getTitle());
                movieSet.setPlot(info.getPlot());
                if (!info.getMediaArt(MediaArtworkType.POSTER).isEmpty()) {
                  int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImagePosterSize().getOrder();
                  List<MediaArtwork.ImageSizeAndUrl> sortedPosters = MovieArtworkHelper.sortArtworkUrls(info.getMediaArt(), MediaArtworkType.POSTER,
                      preferredSizeOrder);
                  if (!sortedPosters.isEmpty()) {
                    movieSet.setArtworkUrl(sortedPosters.get(0).getUrl(), MediaFileType.POSTER);
                  }
                }
                if (!info.getMediaArt(MediaArtworkType.BACKGROUND).isEmpty()) {
                  int preferredSizeOrder = MovieModuleManager.getInstance().getSettings().getImageFanartSize().getOrder();
                  List<MediaArtwork.ImageSizeAndUrl> sortedFanarts = MovieArtworkHelper.sortArtworkUrls(info.getMediaArt(),
                      MediaArtworkType.BACKGROUND, preferredSizeOrder);
                  if (!sortedFanarts.isEmpty()) {
                    movieSet.setArtworkUrl(sortedFanarts.get(0).getUrl(), MediaFileType.FANART);
                  }
                }
              }
            }
            catch (MissingIdException | NothingFoundException e) {
              LOGGER.debug("could not fetch movie set data: {}", e.getMessage());
            }
            catch (ScrapeException e) {
              LOGGER.error("getMovieSet", e);
              MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, movie, "message.scrape.metadatamoviesetfailed",
                  new String[] { ":", e.getLocalizedMessage() }));
            }
          }

          // add movie to movieset
          if (movieSet != null) {
            // first remove from "old" movieset
            movie.setMovieSet(null);

            // add to new movieset
            movie.setMovieSet(movieSet);
            movieSet.insertMovie(movie);
            movie.writeNFO();
            movie.saveToDb();
            movieSet.saveToDb();
          }
        }
      }
      catch (MissingIdException | NothingFoundException e) {
        LOGGER.debug("could not fetch movie data: {}", e.getMessage());
      }
      catch (ScrapeException e) {
        LOGGER.error("getMovieSet", e);
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, movie, "message.scrape.metadatamoviesetfailed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }
  }

  @Override
  public void callback(Object obj) {
    publishState((String) obj, progressDone);
  }
}
