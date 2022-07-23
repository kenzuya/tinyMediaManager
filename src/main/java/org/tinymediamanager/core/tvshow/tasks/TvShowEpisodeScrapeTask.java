/*
 * Copyright 2012 - 2022 Manuel Laggner
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

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;

/**
 * The Class TvShowEpisodeScrapeTask.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeScrapeTask extends TmmTask {

  private static final Logger                            LOGGER = LoggerFactory.getLogger(TvShowEpisodeScrapeTask.class);

  private final List<TvShowEpisode>                      episodes;
  private final TvShowEpisodeSearchAndScrapeOptions      scrapeOptions;
  private final List<TvShowEpisodeScraperMetadataConfig> config;
  private final boolean                                  overwrite;

  /**
   * Instantiates a new tv show episode scrape task.
   * 
   * @param episodes
   *          the episodes to scrape
   * @param options
   *          the scraper options to use
   */
  public TvShowEpisodeScrapeTask(List<TvShowEpisode> episodes, TvShowEpisodeSearchAndScrapeOptions options,
      List<TvShowEpisodeScraperMetadataConfig> config, boolean overwrite) {
    super(TmmResourceBundle.getString("tvshow.scraping"), episodes.size(), TaskType.BACKGROUND_TASK);
    this.episodes = episodes;
    this.scrapeOptions = options;
    this.config = config;
    this.overwrite = overwrite;
  }

  @Override
  public void doInBackground() {
    MediaScraper mediaScraper = scrapeOptions.getMetadataScraper();

    if (!mediaScraper.isEnabled()) {
      return;
    }

    for (TvShowEpisode episode : episodes) {
      // only scrape if at least one ID is available
      if (episode.getTvShow().getIds().isEmpty()) {
        LOGGER.info("we cannot scrape (no ID): {} - {}", episode.getTvShow().getTitle(), episode.getTitle());
        continue;
      }

      TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions(scrapeOptions);

      MediaMetadata md = new MediaMetadata(mediaScraper.getMediaProvider().getProviderInfo().getId());
      md.setScrapeOptions(options);
      md.setReleaseDate(episode.getFirstAired());
      options.setMetadata(md);
      options.setIds(episode.getIds());

      if (episode.isDvdOrder()) {
        options.setId(MediaMetadata.SEASON_NR_DVD, String.valueOf(episode.getDvdSeason()));
        options.setId(MediaMetadata.EPISODE_NR_DVD, String.valueOf(episode.getDvdEpisode()));
      }
      else {
        options.setId(MediaMetadata.SEASON_NR, String.valueOf(episode.getAiredSeason()));
        options.setId(MediaMetadata.EPISODE_NR, String.valueOf(episode.getAiredEpisode()));
      }

      options.setTvShowIds(episode.getTvShow().getIds());

      try {
        LOGGER.info("=====================================================");
        LOGGER.info("Scrape metadata with scraper: {}", mediaScraper.getMediaProvider().getProviderInfo().getId());
        LOGGER.info(options.toString());
        LOGGER.info("=====================================================");
        MediaMetadata metadata = ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getMetadata(options);

        // also inject other ids
        MediaIdUtil.injectMissingIds(metadata.getIds(), MediaType.TV_EPISODE);

        // also fill other ratings if ratings are requested
        if (TvShowModuleManager.getInstance().getSettings().isFetchAllRatings() && config.contains(TvShowEpisodeScraperMetadataConfig.RATING)) {
          Map<String, Object> ids = new HashMap<>(metadata.getIds());
          ids.put("tvShowIds", episode.getTvShow().getIds());

          for (MediaRating rating : ListUtils.nullSafe(RatingProvider.getRatings(ids, MediaType.TV_EPISODE))) {
            if (!metadata.getRatings().contains(rating)) {
              metadata.addRating(rating);
            }
          }
        }

        if (StringUtils.isNotBlank(metadata.getTitle())) {
          episode.setMetadata(metadata, config, overwrite);
          episode.setLastScraperId(scrapeOptions.getMetadataScraper().getId());
          episode.setLastScrapeLanguage(scrapeOptions.getLanguage().name());
        }

        // scrape artwork if wanted
        if (ScraperMetadataConfig.containsAnyArtwork(config)) {
          List<MediaArtwork> artworks = getArtwork(episode, metadata, options);

          // thumb
          if (config.contains(TvShowEpisodeScraperMetadataConfig.THUMB)
              && (overwrite || StringUtils.isBlank(episode.getArtworkFilename(MediaFileType.THUMB)))) {
            for (MediaArtwork art : artworks) {
              if (art.getType() == THUMB && StringUtils.isNotBlank(art.getDefaultUrl())) {
                episode.setArtworkUrl(art.getDefaultUrl(), MediaFileType.THUMB);
                episode.downloadArtwork(MediaFileType.THUMB);
                break;
              }
            }
          }
        }
      }
      catch (MissingIdException e) {
        LOGGER.warn("missing id for scrape");
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, episode, "scraper.error.missingid"));
      }
      catch (NothingFoundException ignored) {
        LOGGER.debug("nothing found");
      }
      catch (ScrapeException e) {
        LOGGER.error("searchMovieFallback", e);
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, episode, "message.scrape.metadataepisodefailed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    if (TvShowModuleManager.getInstance().getSettings().getSyncTrakt()) {
      Set<TvShow> tvShows = new HashSet<>();
      for (TvShowEpisode episode : episodes) {
        tvShows.add(episode.getTvShow());
      }
      TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(new ArrayList<>(tvShows));
      task.setSyncCollection(TvShowModuleManager.getInstance().getSettings().getSyncTraktCollection());
      task.setSyncWatched(TvShowModuleManager.getInstance().getSettings().getSyncTraktWatched());
      task.setSyncRating(TvShowModuleManager.getInstance().getSettings().getSyncTraktRating());

      TmmTaskManager.getInstance().addUnnamedTask(task);
    }
  }

  /**
   * Gets the artwork.
   *
   * @param episode
   *          the {@link TvShowEpisode} to get the artwork for
   * @param metadata
   *          already scraped {@link MediaMetadata}
   * @param scrapeOptions
   *          the {@link TvShowEpisodeSearchAndScrapeOptions} to use for atwork scraping
   * @return the artwork
   */
  private List<MediaArtwork> getArtwork(TvShowEpisode episode, MediaMetadata metadata, TvShowEpisodeSearchAndScrapeOptions scrapeOptions) {
    List<MediaArtwork> artwork = new ArrayList<>();

    ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.TV_EPISODE);
    options.setDataFromOtherOptions(scrapeOptions);
    options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
    options.setMetadata(metadata);

    for (Map.Entry<String, Object> entry : episode.getIds().entrySet()) {
      options.setId(entry.getKey(), entry.getValue().toString());
    }

    // scrape providers till one artwork has been found
    for (MediaScraper artworkScraper : scrapeOptions.getArtworkScrapers()) {
      ITvShowArtworkProvider artworkProvider = (ITvShowArtworkProvider) artworkScraper.getMediaProvider();
      try {
        artwork.addAll(artworkProvider.getArtwork(options));
      }
      catch (MissingIdException ignored) {
        LOGGER.debug("no id avaiable for scraper {}", artworkScraper.getId());
      }
      catch (ScrapeException e) {
        LOGGER.error("getArtwork", e);
        MessageManager.instance.pushMessage(
            new Message(Message.MessageLevel.ERROR, episode, "message.scrape.tvshowartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }
    return artwork;
  }
}
