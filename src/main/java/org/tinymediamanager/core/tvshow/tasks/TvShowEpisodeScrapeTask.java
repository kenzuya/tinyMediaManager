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
package org.tinymediamanager.core.tvshow.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
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

  /**
   * Instantiates a new tv show episode scrape task.
   * 
   * @param episodes
   *          the episodes to scrape
   * @param options
   *          the scraper options to use
   */
  public TvShowEpisodeScrapeTask(List<TvShowEpisode> episodes, TvShowEpisodeSearchAndScrapeOptions options,
      List<TvShowEpisodeScraperMetadataConfig> config) {
    super(TmmResourceBundle.getString("tvshow.scraping"), episodes.size(), TaskType.BACKGROUND_TASK);
    this.episodes = episodes;
    this.scrapeOptions = options;
    this.config = config;
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
      options.setTvShowIds(episode.getTvShow().getIds());

      MediaMetadata md = new MediaMetadata(mediaScraper.getMediaProvider().getProviderInfo().getId());
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

      try {
        LOGGER.info("=====================================================");
        LOGGER.info("Scrape metadata with scraper: {}", mediaScraper.getMediaProvider().getProviderInfo().getId());
        LOGGER.info(options.toString());
        LOGGER.info("=====================================================");
        MediaMetadata metadata = ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getMetadata(options);
        if (StringUtils.isNotBlank(metadata.getTitle())) {
          episode.setMetadata(metadata, config);
          episode.setLastScraperId(scrapeOptions.getMetadataScraper().getId());
          episode.setLastScrapeLanguage(scrapeOptions.getLanguage().name());
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

    if (TvShowModuleManager.SETTINGS.getSyncTrakt()) {
      Set<TvShow> tvShows = new HashSet<>();
      for (TvShowEpisode episode : episodes) {
        tvShows.add(episode.getTvShow());
      }
      TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(new ArrayList<>(tvShows));
      task.setSyncCollection(TvShowModuleManager.SETTINGS.getSyncTraktCollection());
      task.setSyncWatched(TvShowModuleManager.SETTINGS.getSyncTraktWatched());
      task.setSyncRating(TvShowModuleManager.SETTINGS.getSyncTraktRating());

      TmmTaskManager.getInstance().addUnnamedTask(task);
    }
  }
}
