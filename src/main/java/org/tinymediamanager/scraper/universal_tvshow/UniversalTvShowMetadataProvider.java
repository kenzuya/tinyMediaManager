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
package org.tinymediamanager.scraper.universal_tvshow;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.config.MediaProviderConfig;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTmdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTvdbMetadataProvider;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * This is a metadata provider which is highly configurable and combines the results of various other providers
 *
 * @author Manuel Laggner
 */
public class UniversalTvShowMetadataProvider implements ITvShowMetadataProvider {
  public static final String                                ID                  = "universal_tvshow";

  private static final String                               UNDEFINED           = "-";
  private static final String                               SEARCH              = "search";
  private static final String                               RATINGS             = "ratings";
  private static final Logger                               LOGGER              = LoggerFactory.getLogger(UniversalTvShowMetadataProvider.class);
  private static final Map<String, ITvShowMetadataProvider> COMPATIBLE_SCRAPERS = new HashMap<>();
  private static final ExecutorService                      EXECUTOR            = Executors.newFixedThreadPool(4);

  private final MediaProviderInfo                           providerInfo;

  public UniversalTvShowMetadataProvider() {
    providerInfo = createMediaProviderInfo();
    init();
  }

  private MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, ID, "Universal TV show scraper",
        "<html><h3>Universal TV show scraper</h3><br />A meta scraper which allows to collect data from several other scrapers</html>",
        UniversalTvShowMetadataProvider.class.getResource("/org/tinymediamanager/scraper/tmm_logo.svg"));
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled();
  }

  public static void addProvider(ITvShowMetadataProvider provider) {
    // called for each plugin implementing that interface
    if (!provider.getId().equals(ID) && !COMPATIBLE_SCRAPERS.containsKey(provider.getId())
        && (provider instanceof ITvShowTvdbMetadataProvider || provider instanceof ITvShowImdbMetadataProvider)
        || provider instanceof ITvShowTmdbMetadataProvider) {
      COMPATIBLE_SCRAPERS.put(provider.getId(), provider);
    }
  }

  private void init() {
    MediaProviderConfig config = providerInfo.getConfig();

    List<String> compatibleScraperIds = new ArrayList<>(COMPATIBLE_SCRAPERS.keySet());
    compatibleScraperIds.add(0, UNDEFINED); // no scraper

    config.addLabel("searchLabel", "Button.search");
    config.addSelect(SEARCH, compatibleScraperIds, UNDEFINED);

    // use the right key to let reflection work
    // getter in MediaMetadata must be get + Key (first letter upper case)
    config.addLabel("tvShowLabel", "tmm.tvshow");
    config.addSelect("title", "metatag.title", compatibleScraperIds, UNDEFINED);
    config.addSelect("originalTitle", "metatag.originaltitle", compatibleScraperIds, UNDEFINED);
    config.addSelect("year", "metatag.year", compatibleScraperIds, UNDEFINED);
    config.addSelect("releaseDate", "metatag.releasedate", compatibleScraperIds, UNDEFINED);
    config.addSelect("plot", "metatag.plot", compatibleScraperIds, UNDEFINED);
    config.addSelect("runtime", "metatag.runtime", compatibleScraperIds, UNDEFINED);
    config.addSelect(RATINGS, "metatag.rating", compatibleScraperIds, UNDEFINED);
    config.addSelect("genres", "metatag.genre", compatibleScraperIds, UNDEFINED);
    config.addSelect("certifications", "metatag.certification", compatibleScraperIds, UNDEFINED);
    config.addSelect("productionCompanies", "metatag.studio", compatibleScraperIds, UNDEFINED);
    config.addSelect("castMembers", "metatag.cast", compatibleScraperIds, UNDEFINED);
    config.addSelect("spokenLanguages", "metatag.spokenlanguages", compatibleScraperIds, UNDEFINED);
    config.addSelect("countries", "metatag.country", compatibleScraperIds, UNDEFINED);
    config.addSelect("tags", "metatag.tags", compatibleScraperIds, UNDEFINED);
    config.addSelect("mediaArt", "metatag.artwork", compatibleScraperIds, UNDEFINED);
    config.addSelect("status", "metatag.status", compatibleScraperIds, UNDEFINED);

    config.addLabel("episodeLabel", "metatag.episode");
    config.addSelect("episodes", "metatag.episodes", compatibleScraperIds, UNDEFINED);
    config.addSelect("episodeTitle", "metatag.title", compatibleScraperIds, UNDEFINED);
    config.addSelect("episodePlot", "metatag.plot", compatibleScraperIds, UNDEFINED);
    config.addSelect("episodeCastMembers", "metatag.cast", compatibleScraperIds, UNDEFINED);
    config.addSelect("episodeRatings", "metatag.rating", compatibleScraperIds, UNDEFINED);
    config.addSelect("episodeMediaArt", "metatag.artwork", compatibleScraperIds, UNDEFINED);

    config.load();
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    SortedSet<MediaSearchResult> results = new TreeSet<>();

    ITvShowMetadataProvider mp = COMPATIBLE_SCRAPERS.get(providerInfo.getConfig().getValue(SEARCH));
    if (mp == null) {
      return results;
    }

    try {
      for (MediaSearchResult result : mp.search(options)) {
        result.setProviderId(providerInfo.getId());
        results.add(result);
      }
    }
    catch (ScrapeException e) {
      LOGGER.warn("Could not call search method of {} - {}", mp.getProviderInfo().getId(), e.getMessage());
      throw e;
    }

    return results;
  }

  private Set<ITvShowMetadataProvider> getRelevantMetadataProviders(boolean episode) {
    Set<ITvShowMetadataProvider> metadataProviders = new HashSet<>();
    for (Map.Entry<String, String> entry : providerInfo.getConfig().getConfigKeyValuePairs().entrySet()) {
      if ((episode && entry.getKey().startsWith("episode"))
          || (!episode && !entry.getKey().startsWith("episode")) && !UNDEFINED.equals(entry.getValue())) {
        ITvShowMetadataProvider mp = COMPATIBLE_SCRAPERS.get(entry.getValue());
        if (mp != null) {
          metadataProviders.add(mp);
        }
      }
    }
    return metadataProviders;
  }

  private Map<String, MediaMetadata> getTvShowMetadataMap(Set<ITvShowMetadataProvider> metadataProviders, TvShowSearchAndScrapeOptions options) {
    // inject missing IDs
    Map<String, MediaMetadata> metadataMap = injectMissingIds(metadataProviders, options);

    // start the workers to get the metadata from the different providers
    ExecutorCompletionService<MediaMetadata> completionService = new ExecutorCompletionService<>(EXECUTOR);
    List<Future<MediaMetadata>> futures = new ArrayList<>();
    for (ITvShowMetadataProvider mp : metadataProviders) {
      // look into the cache - maybe we do not need to call it again
      if (metadataMap.get(mp.getProviderInfo().getId()) == null) {
        futures.add(completionService.submit(new TvShowMetadataProviderWorker(mp, options)));
      }
    }

    // wait for all workers to finish
    for (Future<MediaMetadata> future : futures) {
      try {
        MediaMetadata mediaMetadata = future.get();
        if (mediaMetadata != null) {
          metadataMap.put(mediaMetadata.getProviderId(), mediaMetadata);
        }
      }
      catch (Exception e) {
        LOGGER.warn("Could not get a result from scraper: {}", e.getMessage());
      }
    }
    return metadataMap;
  }

  private Map<String, MediaMetadata> getTvShowEpisodeMetadataMap(Set<ITvShowMetadataProvider> metadataProviders,
      TvShowEpisodeSearchAndScrapeOptions options) {
    // inject missing tv show IDs
    TvShowSearchAndScrapeOptions tvShowSearchAndScrapeOptions = options.createTvShowSearchAndScrapeOptions();
    injectMissingIds(metadataProviders, tvShowSearchAndScrapeOptions);

    // mix in the options from tvShowSearchAndScrapeOptions
    for (Map.Entry<String, Object> entry : tvShowSearchAndScrapeOptions.getIds().entrySet()) {
      if (!options.getTvShowIds().containsKey(entry.getKey())) {
        options.getTvShowIds().put(entry.getKey(), entry.getValue());
      }
    }

    // inject missing episode IDs
    Map<String, MediaMetadata> metadataMap = injectMissingIds(metadataProviders, options);

    // start the workers to get the metadata from the different providers
    ExecutorCompletionService<MediaMetadata> completionService = new ExecutorCompletionService<>(EXECUTOR);
    List<Future<MediaMetadata>> futures = new ArrayList<>();
    for (ITvShowMetadataProvider mp : metadataProviders) {
      // look into the cache - maybe we do not need to call it again
      if (metadataMap.get(mp.getProviderInfo().getId()) == null) {
        futures.add(completionService.submit(new TvShowEpisodeMetadataProviderWorker(mp, options)));
      }
    }

    // wait for all workers to finish
    for (Future<MediaMetadata> future : futures) {
      try {
        MediaMetadata mediaMetadata = future.get();
        if (mediaMetadata != null) {
          metadataMap.put(mediaMetadata.getProviderId(), mediaMetadata);
        }
      }
      catch (Exception e) {
        LOGGER.warn("Could not get a result from scraper: {}", e.getMessage());
      }
    }
    return metadataMap;
  }

  private Map<String, MediaMetadata> injectMissingIds(Set<ITvShowMetadataProvider> metadataProviders, TvShowSearchAndScrapeOptions options) {
    // check if we have all needed IDs
    // this scraper supports scraping via TVDB id, TMDB id and IMDB id; but not all used providers support all of them
    // we may need to get the missing ones
    String imdbId = options.getImdbId();
    int tmdbId = options.getTmdbId();
    int tvdbId = options.getIdAsInt(MediaMetadata.TVDB);

    Map<String, MediaMetadata> metadataMap = new HashMap<>();

    for (ITvShowMetadataProvider mp : metadataProviders) {
      if (mp instanceof ITvShowImdbMetadataProvider && MetadataUtil.isValidImdbId(imdbId)) {
        // everything is good ;)
        continue;
      }
      if (mp instanceof ITvShowTmdbMetadataProvider && tmdbId > 0) {
        // everything is good ;)
        continue;
      }
      if (mp instanceof ITvShowTvdbMetadataProvider && tvdbId > 0) {
        // everything is good ;)
        continue;
      }

      // we've come here, so we have not the needed ID
      // TMDB offers scraping by all of them and returns them all too (if available)
      if (tmdbId > 0 || MetadataUtil.isValidImdbId(imdbId) || tvdbId > 0) {
        // try to get the meta data via TMDB
        // anything cached?
        MediaMetadata md = metadataMap.get(MediaMetadata.TMDB);
        if (md == null) {
          try {
            ITvShowMetadataProvider tmdb = COMPATIBLE_SCRAPERS.get(MediaMetadata.TMDB);
            if (tmdb != null) {
              md = tmdb.getMetadata(options);
              if (md != null) {
                // cache the result for later usage
                metadataMap.put(MediaMetadata.TMDB, md);
              }
            }
          }
          catch (Exception e) {
            LOGGER.warn("Could not get a result from scraper: {}", e.getMessage());
          }
        }
        // we got a response - parse out TVDB id, TMDB id and IMDB id if needed
        if (md != null) {
          if (tmdbId == 0) {
            Object id = md.getId(MediaMetadata.TMDB);
            if (id != null) {
              try {

                if (id instanceof Integer) {
                  // all good
                  tmdbId = (int) id;
                }
                else {
                  tmdbId = Integer.parseInt(id.toString());
                }
              }
              catch (Exception ignored) {
                LOGGER.trace("could not parse tmdb id: - {}", md.getId(MediaMetadata.TMDB));
              }
            }
          }
          if (!MetadataUtil.isValidImdbId(imdbId) && MetadataUtil.isValidImdbId((String) md.getId(MediaMetadata.IMDB))) {
            imdbId = (String) md.getId(MediaMetadata.IMDB);
          }
          if (tvdbId == 0) {
            Object id = md.getId(MediaMetadata.TVDB);
            if (id != null) {
              try {
                if (id instanceof Integer) {
                  // all good
                  tvdbId = (int) id;
                }
                else {
                  tvdbId = Integer.parseInt(id.toString());
                }
              }
              catch (Exception ignored) {
                LOGGER.trace("could not parse tvdb id: - {}", md.getId(MediaMetadata.TVDB));
              }
            }
          }
          break;
        }
      }
    }

    // inject the found TVDB id, TMDB id and IMDB id into the search options
    if (MetadataUtil.isValidImdbId(imdbId)) {
      options.setImdbId(imdbId);
    }
    if (tmdbId > 0) {
      options.setTmdbId(tmdbId);
    }
    if (tvdbId > 0) {
      options.setId(MediaMetadata.TVDB, String.valueOf(tvdbId));
    }

    return metadataMap;
  }

  @Override
  public MediaMetadata getMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() - {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    MediaMetadata md = new MediaMetadata(providerInfo.getId());

    // check which scrapers should be used
    Set<ITvShowMetadataProvider> metadataProviders = getRelevantMetadataProviders(false);
    if (metadataProviders.isEmpty()) {
      return md;
    }

    // call all scrapers in different workers and wait for them to finish
    Map<String, MediaMetadata> metadataMap = getTvShowMetadataMap(metadataProviders, options);

    // and now assign the values per reflection
    assignTvShowResults(md, metadataMap);

    if (md.getIds().isEmpty()) {
      throw new NothingFoundException();
    }

    return md;
  }

  private void assignTvShowResults(MediaMetadata md, Map<String, MediaMetadata> metadataMap) {
    // take all ids we can get
    for (Map.Entry<String, MediaMetadata> entry : metadataMap.entrySet()) {
      for (Map.Entry<String, Object> id : entry.getValue().getIds().entrySet()) {
        md.setId(id.getKey(), id.getValue());
      }
    }

    // assign the requested metadata
    for (Map.Entry<String, String> entry : providerInfo.getConfig().getConfigKeyValuePairs().entrySet()) {
      if (!entry.getKey().startsWith("episode") && !SEARCH.equals(entry.getKey()) && !UNDEFINED.equals(entry.getValue())) {
        // all specified fields should be filled from the desired scraper
        MediaMetadata mediaMetadata = metadataMap.get(entry.getValue());
        if (mediaMetadata != null) {
          try {
            Method getter = new PropertyDescriptor(entry.getKey(), MediaMetadata.class).getReadMethod();
            Method setter = new PropertyDescriptor(entry.getKey(), MediaMetadata.class).getWriteMethod();

            setter.invoke(md, getter.invoke(mediaMetadata));
          }
          catch (Exception e) {
            LOGGER.warn("Problem assigning {} - {}", entry.getKey(), e.getMessage());
          }
        }

        // last but not least we take all ratings we got ;) the more the better
        if (RATINGS.equals(entry.getKey())) {
          for (Map.Entry<String, MediaMetadata> mediaMetadataEntry : metadataMap.entrySet()) {
            // do not process the desired scraper again
            if (mediaMetadataEntry.getKey().equals(entry.getValue())) {
              continue;
            }
            for (MediaRating rating : mediaMetadataEntry.getValue().getRatings()) {
              if (!md.getRatings().contains(rating)) {
                md.addRating(rating);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public MediaMetadata getMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() - {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    MediaMetadata md = new MediaMetadata(providerInfo.getId());

    // check which scrapers should be used
    Set<ITvShowMetadataProvider> metadataProviders = getRelevantMetadataProviders(true);
    if (metadataProviders.isEmpty()) {
      return md;
    }

    // call all scrapers in different workers and wait for them to finish
    Map<String, MediaMetadata> metadataMap = getTvShowEpisodeMetadataMap(metadataProviders, options);

    // and now assign the values per reflection
    assignEpisodeResults(md, metadataMap);

    return md;
  }

  private void assignEpisodeResults(MediaMetadata md, Map<String, MediaMetadata> metadataMap) {
    // take all ids we can get
    for (Map.Entry<String, MediaMetadata> entry : metadataMap.entrySet()) {
      for (Map.Entry<String, Object> id : entry.getValue().getIds().entrySet()) {
        md.setId(id.getKey(), id.getValue());
      }
    }

    // assign the requested metadata
    for (Map.Entry<String, String> entry : providerInfo.getConfig().getConfigKeyValuePairs().entrySet()) {
      if (entry.getKey().startsWith("episode") && !UNDEFINED.equals(entry.getValue())) {
        MediaMetadata mediaMetadata = metadataMap.get(entry.getValue());

        if (mediaMetadata == null) {
          continue;
        }

        // "common" data from the same scraper
        if ("episodes".equals(entry.getKey())) {
          md.setSeasonNumber(mediaMetadata.getSeasonNumber());
          md.setEpisodeNumber(mediaMetadata.getEpisodeNumber());
          md.setDisplaySeasonNumber(mediaMetadata.getDisplaySeasonNumber());
          md.setDisplayEpisodeNumber(mediaMetadata.getDisplayEpisodeNumber());
          md.setDvdSeasonNumber(mediaMetadata.getDvdSeasonNumber());
          md.setDvdEpisodeNumber(mediaMetadata.getDvdEpisodeNumber());
          md.setAbsoluteNumber(mediaMetadata.getAbsoluteNumber());
          md.setReleaseDate(mediaMetadata.getReleaseDate());
        }
        else {
          String episodeField = Introspector.decapitalize(entry.getKey().replace("episode", ""));
          // all specified fields should be filled from the desired scraper
          try {
            Method getter = new PropertyDescriptor(episodeField, MediaMetadata.class).getReadMethod();
            Method setter = new PropertyDescriptor(episodeField, MediaMetadata.class).getWriteMethod();

            setter.invoke(md, getter.invoke(mediaMetadata));
          }
          catch (Exception e) {
            LOGGER.warn("Problem assigning {} - {}", episodeField, e.getMessage());
          }

          // last but not least we take all ratings we got ;) the more the better
          if (RATINGS.equals(episodeField)) {
            for (Map.Entry<String, MediaMetadata> mediaMetadataEntry : metadataMap.entrySet()) {
              // do not process the desired scraper again
              if (mediaMetadataEntry.getKey().equals(entry.getValue())) {
                continue;
              }
              for (MediaRating rating : mediaMetadataEntry.getValue().getRatings()) {
                if (!md.getRatings().contains(rating)) {
                  md.addRating(rating);
                }
              }
            }
          }
        }
      }
    }
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList() - {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    List<MediaMetadata> episodeList = new ArrayList<>();

    ITvShowMetadataProvider metadataProvider = COMPATIBLE_SCRAPERS.get(providerInfo.getConfig().getConfigKeyValuePairs().get("episodes"));
    if (metadataProvider != null) {
      // get all relevant ids (if not present)
      injectMissingIds(Collections.singleton(metadataProvider), options);

      episodeList.addAll(metadataProvider.getEpisodeList(options));
    }

    return episodeList;
  }

  /**
   * inject missing ids for episode data
   * 
   * @param metadataProviders
   *          the meta data providers
   * @param options
   *          the scraping options
   */
  private Map<String, MediaMetadata> injectMissingIds(Set<ITvShowMetadataProvider> metadataProviders, TvShowEpisodeSearchAndScrapeOptions options) {
    // check if we have all needed IDs
    // this scraper supports scraping via TVDB id, TMDB id and IMDB id; but not all used providers support all of them
    // we may need to get the missing ones
    String imdbId = options.getImdbId();
    int tmdbId = options.getTmdbId();
    int tvdbId = options.getIdAsInt(MediaMetadata.TVDB);

    Map<String, MediaMetadata> metadataMap = new HashMap<>();

    for (ITvShowMetadataProvider mp : metadataProviders) {
      if (mp instanceof ITvShowImdbMetadataProvider && MetadataUtil.isValidImdbId(imdbId)) {
        // everything is good ;)
        continue;
      }
      if (mp instanceof ITvShowTmdbMetadataProvider && tmdbId > 0) {
        // everything is good ;)
        continue;
      }
      if (mp instanceof ITvShowTvdbMetadataProvider && tvdbId > 0) {
        // everything is good ;)
        continue;
      }

      // we've come here, so we have not the needed ID
      // TMDB offers scraping by all of them and returns them all too (if available)
      if (tmdbId > 0 || MetadataUtil.isValidImdbId(imdbId) || tvdbId > 0) {
        // try to get the meta data via TMDB
        // anything cached?
        MediaMetadata md = metadataMap.get(MediaMetadata.TMDB);
        if (md == null) {
          try {
            ITvShowMetadataProvider tmdb = COMPATIBLE_SCRAPERS.get(MediaMetadata.TMDB);
            if (tmdb != null) {
              md = tmdb.getMetadata(options);
              if (md != null) {
                // cache the result for later usage
                metadataMap.put(MediaMetadata.TMDB, md);
              }
            }
          }
          catch (Exception e) {
            LOGGER.warn("Could not get a result from scraper: {}", e.getMessage());
          }
        }
        // we got a response - parse out TVDB id, TMDB id and IMDB id if needed
        if (md != null) {
          if (tmdbId == 0) {
            Object id = md.getId(MediaMetadata.TMDB);
            if (id != null) {
              try {

                if (id instanceof Integer) {
                  // all good
                  tmdbId = (int) id;
                }
                else {
                  tmdbId = Integer.parseInt(id.toString());
                }
              }
              catch (Exception ignored) {
                LOGGER.trace("could not parse tmdb id: - {}", md.getId(MediaMetadata.TMDB));
              }
            }
          }
          if (!MetadataUtil.isValidImdbId(imdbId) && MetadataUtil.isValidImdbId((String) md.getId(MediaMetadata.IMDB))) {
            imdbId = (String) md.getId(MediaMetadata.IMDB);
          }
          if (tvdbId == 0) {
            Object id = md.getId(MediaMetadata.TVDB);
            if (id != null) {
              try {
                if (id instanceof Integer) {
                  // all good
                  tvdbId = (int) id;
                }
                else {
                  tvdbId = Integer.parseInt(id.toString());
                }
              }
              catch (Exception ignored) {
                LOGGER.trace("could not parse tvdb id: - {}", md.getId(MediaMetadata.TVDB));
              }
            }
          }
          break;
        }
      }
    }

    // inject the found TVDB id, TMDB id and IMDB id into the search options
    if (MetadataUtil.isValidImdbId(imdbId)) {
      options.setImdbId(imdbId);
    }
    if (tmdbId > 0) {
      options.setTmdbId(tmdbId);
    }
    if (tvdbId > 0) {
      options.setId(MediaMetadata.TVDB, String.valueOf(tvdbId));
    }

    return metadataMap;
  }

  /****************************************************************************
   * local helper classes
   ****************************************************************************/
  protected static class TvShowMetadataProviderWorker implements Callable<MediaMetadata> {
    private final ITvShowMetadataProvider      metadataProvider;
    private final TvShowSearchAndScrapeOptions mediaScrapeOptions;

    TvShowMetadataProviderWorker(ITvShowMetadataProvider metadataProvider, TvShowSearchAndScrapeOptions mediaScrapeOptions) {
      this.metadataProvider = metadataProvider;
      this.mediaScrapeOptions = mediaScrapeOptions;
    }

    @Override
    public MediaMetadata call() throws Exception {
      return metadataProvider.getMetadata(mediaScrapeOptions);
    }
  }

  protected static class TvShowEpisodeMetadataProviderWorker implements Callable<MediaMetadata> {
    private final ITvShowMetadataProvider             metadataProvider;
    private final TvShowEpisodeSearchAndScrapeOptions mediaScrapeOptions;

    TvShowEpisodeMetadataProviderWorker(ITvShowMetadataProvider metadataProvider, TvShowEpisodeSearchAndScrapeOptions mediaScrapeOptions) {
      this.metadataProvider = metadataProvider;
      this.mediaScrapeOptions = mediaScrapeOptions;
    }

    @Override
    public MediaMetadata call() throws Exception {
      return metadataProvider.getMetadata(mediaScrapeOptions);
    }
  }
}
