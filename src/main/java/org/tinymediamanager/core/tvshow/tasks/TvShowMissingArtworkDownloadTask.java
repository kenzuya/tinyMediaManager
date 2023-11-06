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

package org.tinymediamanager.core.tvshow.tasks;

import static org.tinymediamanager.scraper.entities.MediaType.TV_EPISODE;
import static org.tinymediamanager.scraper.entities.MediaType.TV_SHOW;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.*;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;

/**
 * the class TvShowMissingArtworkDownloadTask is used to download missing artwork for TV shows
 */
public class TvShowMissingArtworkDownloadTask extends TmmThreadPool {
  private static final Logger                            LOGGER = LoggerFactory.getLogger(TvShowMissingArtworkDownloadTask.class);

  private final Collection<TvShow>                       tvShows;
  private final Collection<TvShowEpisode>                episodes;
  private final TvShowSearchAndScrapeOptions             scrapeOptions;
  private final List<TvShowScraperMetadataConfig>        tvShowScraperMetadataConfig;
  private final List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig;

  public TvShowMissingArtworkDownloadTask(Collection<TvShow> tvShows, Collection<TvShowEpisode> episodes, TvShowSearchAndScrapeOptions scrapeOptions,
      List<TvShowScraperMetadataConfig> tvShowScraperMetadataConfig, List<TvShowEpisodeScraperMetadataConfig> episodeScraperMetadataConfig) {
    super(TmmResourceBundle.getString("task.missingartwork"));
    this.tvShows = new ArrayList<>(tvShows);
    this.episodes = new ArrayList<>(episodes);
    this.scrapeOptions = scrapeOptions;
    this.tvShowScraperMetadataConfig = tvShowScraperMetadataConfig;
    this.episodeScraperMetadataConfig = episodeScraperMetadataConfig;

    // add the episodes from the shows
    for (TvShow show : this.tvShows) {
      for (TvShowEpisode episode : new ArrayList<>(show.getEpisodes())) {
        if (!this.episodes.contains(episode)) {
          this.episodes.add(episode);
        }
      }
    }
  }

  @Override
  protected void doInBackground() {
    LOGGER.info("Getting missing artwork");

    initThreadPool(3, "scrapeMissingTvShowArtwork");
    start();

    for (TvShow show : tvShows) {
      if (cancel) {
        break;
      }
      if (TvShowArtworkHelper.hasMissingArtwork(show, tvShowScraperMetadataConfig)) {
        submitTask(new TvShowWorker(show, scrapeOptions));
      }
    }

    for (TvShowEpisode episode : episodes) {
      if (cancel) {
        break;
      }
      if (TvShowArtworkHelper.hasMissingArtwork(episode, episodeScraperMetadataConfig)) {
        submitTask(new TvShowEpisodeWorker(episode, scrapeOptions));
      }
    }

    waitForCompletionOrCancel();
    LOGGER.info("Done getting missing artwork");
  }

  @Override
  public void callback(Object obj) {
    publishState((String) obj, progressDone);
  }

  /****************************************************************************************
   * Helper classes
   ****************************************************************************************/
  private static class TvShowWorker implements Runnable {
    private final TvShow                        tvShow;
    private final ArtworkSearchAndScrapeOptions options;

    private TvShowWorker(TvShow tvShow, MediaSearchAndScrapeOptions scrapeOptions) {
      this.tvShow = tvShow;
      options = new ArtworkSearchAndScrapeOptions(TV_SHOW);
      options.setDataFromOtherOptions(scrapeOptions);
      options.setIds(Collections.emptyMap()); // force clearing of ids

      options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
      options.setLanguage(TvShowModuleManager.getInstance().getSettings().getScraperLanguage());
      options.setFanartSize(TvShowModuleManager.getInstance().getSettings().getImageFanartSize());
      options.setPosterSize(TvShowModuleManager.getInstance().getSettings().getImagePosterSize());
      options.setThumbSize(TvShowModuleManager.getInstance().getSettings().getImageThumbSize());
      for (Map.Entry<String, Object> entry : tvShow.getIds().entrySet()) {
        options.setId(entry.getKey(), entry.getValue().toString());
      }
    }

    @Override
    public void run() {
      try {
        // set up scrapers
        List<MediaArtwork> artwork = new ArrayList<>();

        // scrape providers till one artwork has been found
        for (MediaScraper artworkScraper : options.getArtworkScrapers()) {
          ITvShowArtworkProvider artworkProvider = (ITvShowArtworkProvider) artworkScraper.getMediaProvider();
          try {
            artwork.addAll(artworkProvider.getArtwork(options));
          }
          catch (MissingIdException ignored) {
            LOGGER.debug("no id found for scraper {}", artworkProvider.getProviderInfo());
          }
          catch (NothingFoundException e) {
            LOGGER.debug("did not find artwork for '{}'", tvShow.getTitle());
          }
          catch (ScrapeException e) {
            LOGGER.error("getArtwork", e);
            MessageManager.instance.pushMessage(
                new Message(Message.MessageLevel.ERROR, tvShow, "message.scrape.tvshowartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
          }
        }

        // now set & download the artwork
        if (!artwork.isEmpty()) {
          TvShowArtworkHelper.downloadMissingArtwork(tvShow, artwork);
        }
      }
      catch (Exception e) {
        LOGGER.error("Thread crashed", e);
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowMissingArtwork", "message.scrape.threadcrashed",
            new String[] { ":", e.getLocalizedMessage() }));
      }
    }
  }

  private static class TvShowEpisodeWorker implements Runnable {
    private final TvShowEpisode                 episode;
    private final ArtworkSearchAndScrapeOptions options;

    private TvShowEpisodeWorker(TvShowEpisode episode, MediaSearchAndScrapeOptions scrapeOptions) {
      this.episode = episode;

      options = new ArtworkSearchAndScrapeOptions((TV_EPISODE));
      options.setDataFromOtherOptions(scrapeOptions);
      options.setIds(Collections.emptyMap()); // force clearing of IDs

      options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
      options.setLanguage(TvShowModuleManager.getInstance().getSettings().getScraperLanguage());
      options.setFanartSize(TvShowModuleManager.getInstance().getSettings().getImageFanartSize());
      options.setPosterSize(TvShowModuleManager.getInstance().getSettings().getImagePosterSize());
      options.setThumbSize(TvShowModuleManager.getInstance().getSettings().getImageThumbSize());
      options.setId(MediaMetadata.TVSHOW_IDS, episode.getTvShow().getIds());
      options.setId("mediaFile", episode.getMainFile());

      for (Map.Entry<String, Object> entry : episode.getIds().entrySet()) {
        options.setId(entry.getKey(), entry.getValue().toString());
      }

      options.setId(MediaMetadata.EPISODE_NR, episode.getEpisodeNumbers());
      options.setArtworkType(MediaArtwork.MediaArtworkType.THUMB);
    }

    @Override
    public void run() {
      try {
        List<MediaArtwork> artwork = new ArrayList<>();

        // scrape providers till one artwork has been found
        for (MediaScraper artworkScraper : options.getArtworkScrapers()) {
          ITvShowArtworkProvider artworkProvider = (ITvShowArtworkProvider) artworkScraper.getMediaProvider();
          try {
            artwork.addAll(artworkProvider.getArtwork(options));
            if (!artwork.isEmpty()) {
              break;
            }
          }
          catch (MissingIdException ignored) {
            LOGGER.debug("no id found for scraper {}", artworkProvider.getProviderInfo());
          }
          catch (NothingFoundException e) {
            LOGGER.debug("did not find artwork for '{}' - S{}/E{}", episode.getTvShow().getTitle(), episode.getSeason(), episode.getEpisode());
          }
          catch (ScrapeException e) {
            LOGGER.error("getArtwork", e);
            MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, episode, "message.scrape.tvshowartworkfailed",
                new String[] { ":", e.getLocalizedMessage() }));
          }
        }

        int preferredSizeOrder = TvShowModuleManager.getInstance().getSettings().getImageThumbSize().getOrder();
        List<MediaArtwork.ImageSizeAndUrl> sortedThumbs = TvShowArtworkHelper.sortArtworkUrls(artwork, MediaArtwork.MediaArtworkType.THUMB,
                preferredSizeOrder);

        if (!sortedThumbs.isEmpty()) {
          episode.setArtworkUrl(sortedThumbs.get(0).getUrl(), MediaFileType.THUMB);
          episode.downloadArtwork(MediaFileType.THUMB);

          episode.saveToDb();
          episode.writeNFO();
        }
      }
      catch (Exception e) {
        LOGGER.error("Thread crashed", e);
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowMissingArtwork", "message.scrape.threadcrashed",
            new String[] { ":", e.getLocalizedMessage() }));
      }
    }
  }
}
