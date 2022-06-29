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
package org.tinymediamanager.core.tvshow;

import java.util.HashMap;
import java.util.Map;

import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaType;

/**
 * The class TvShowEpisodeSearchAndScrapeOptions is used to hold scrape and search related data for episodes
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeSearchAndScrapeOptions extends MediaSearchAndScrapeOptions {
  private final Map<String, Object> tvShowIds = new HashMap<>();

  /**
   * a minimal constructor for special cases. Do not forget to pass the {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids via
   * setTvShowIds()
   */
  public TvShowEpisodeSearchAndScrapeOptions() {
    super(MediaType.TV_EPISODE);
  }

  /**
   * the _main_ constructor to give all {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids to the options
   * 
   * @param tvShowIds
   */
  public TvShowEpisodeSearchAndScrapeOptions(final Map<String, Object> tvShowIds) {
    super(MediaType.TV_EPISODE);
    setTvShowIds(tvShowIds);
  }

  /**
   * copy constructor
   * 
   * @param original
   *          the original to copy
   */
  public TvShowEpisodeSearchAndScrapeOptions(TvShowEpisodeSearchAndScrapeOptions original) {
    super(original);
    setTvShowIds(original.tvShowIds);
  }

  @Override
  public void setDataFromOtherOptions(MediaSearchAndScrapeOptions original) {
    super.setDataFromOtherOptions(original);
    if (original instanceof TvShowEpisodeSearchAndScrapeOptions) {
      setTvShowIds(((TvShowEpisodeSearchAndScrapeOptions) original).tvShowIds);
    }
    if (original instanceof TvShowSearchAndScrapeOptions) {
      setTvShowIds(original.getIds());
    }
  }

  /**
   * get all set {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids
   * 
   * @return all set {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids
   */
  public Map<String, Object> getTvShowIds() {
    return tvShowIds;
  }

  /**
   * set the {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids for this options
   * 
   * @param tvShowIds
   *          a {@link Map} with the {@link org.tinymediamanager.core.tvshow.entities.TvShow} ids to set
   */
  public void setTvShowIds(final Map<String, Object> tvShowIds) {
    if (this.tvShowIds == null) { // can happen in the constructor // NOSONAR
      return;
    }

    this.tvShowIds.clear();

    if (tvShowIds == null) {
      return;
    }

    this.tvShowIds.putAll(tvShowIds);
    this.ids.put("tvShowIds", tvShowIds);
  }

  /**
   * create an instance of {@link TvShowSearchAndScrapeOptions} from this options for further usage
   * 
   * @return the created {@link TvShowSearchAndScrapeOptions}
   */
  public TvShowSearchAndScrapeOptions createTvShowSearchAndScrapeOptions() {
    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    // take all data from this
    options.setDataFromOtherOptions(this);
    // but the ids from the show
    options.setIds(tvShowIds);

    return options;
  }

  /**
   * Load default Settings.
   */
  public void loadDefaults() {
    // language
    language = TvShowModuleManager.getInstance().getSettings().getScraperLanguage();

    // metadata
    metadataScraper = TvShowModuleManager.getInstance().getTvShowList().getDefaultMediaScraper();

    // artwork
    artworkScrapers.addAll(TvShowModuleManager.getInstance().getTvShowList().getDefaultArtworkScrapers());

    // trailer
    trailerScrapers.addAll(TvShowModuleManager.getInstance().getTvShowList().getDefaultTrailerScrapers());

    // subtitle
    subtitleScrapers.addAll(TvShowModuleManager.getInstance().getTvShowList().getDefaultSubtitleScrapers());
  }
}
