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
package org.tinymediamanager.scraper.thetvdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesExtendedResponse;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ListUtils;

import retrofit2.Response;

/**
 * the class {@link TheTvDbTvShowArtworkProvider} offer artwork for TV shows
 *
 * @author Manuel Laggner
 */
public class TheTvDbTvShowArtworkProvider extends TheTvDbMetadataProvider implements ITvShowArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TheTvDbTvShowArtworkProvider.class);

  @Override
  protected String getSubId() {
    return "tvshow_artwork";
  }

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = super.createMediaProviderInfo();

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().load();

    return info;
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    LOGGER.debug("getting artwork: {}", options);
    List<MediaArtwork> artwork = new ArrayList<>();

    if (options.getMediaType() == MediaType.TV_EPISODE) {
      try {
        // episode artwork has to be scraped via the meta data scraper
        TvShowEpisodeSearchAndScrapeOptions episodeSearchAndScrapeOptions = new TvShowEpisodeSearchAndScrapeOptions();
        episodeSearchAndScrapeOptions.setDataFromOtherOptions(options);
        if (options.getIds().get("tvShowIds") instanceof Map) {
          Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get("tvShowIds");
          episodeSearchAndScrapeOptions.setTvShowIds(tvShowIds);
        }
        MediaMetadata md = new TheTvDbTvShowMetadataProvider().getMetadata(episodeSearchAndScrapeOptions);
        return md.getMediaArt();
      }
      catch (MissingIdException e) {
        // no valid ID given - just do nothing
        return Collections.emptyList();
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }
    }

    // do we have an id from the options?
    Integer id = options.getIdAsInteger(getProviderInfo().getId());

    if (id == null || id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(getProviderInfo().getId());
    }

    // get artwork from thetvdb
    List<ArtworkBaseRecord> images = new ArrayList<>();
    try {
      // get all types of artwork we can get
      Response<SeriesExtendedResponse> response = tvdb.getSeriesService().getSeriesExtended(id).execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response.code(), response.message());
      }

      if (response.body() != null && response.body().data != null && response.body().data.artworks != null) {
        images.addAll(response.body().data.artworks);
      }
    }
    catch (Exception e) {
      LOGGER.error("failed to get artwork: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (ListUtils.isEmpty(images)) {
      return artwork;
    }

    // sort it
    images.sort(new ImageComparator(LanguageUtils.getIso3Language(options.getLanguage().toLocale())));

    // get base show artwork
    for (ArtworkBaseRecord image : images) {
      MediaArtwork ma = parseArtwork(image);

      if (ma == null) {
        continue;
      }

      if (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() == ma.getType()) {
        artwork.add(ma);
      }
    }

    return artwork;
  }

  /**********************************************************************
   * local helper classes
   **********************************************************************/
  private static class ImageComparator implements Comparator<ArtworkBaseRecord> {
    private final String preferredLangu;
    private final String english;

    private ImageComparator(String language) {
      preferredLangu = language;
      english = "eng";
    }

    /*
     * sort artwork: primary by language: preferred lang (ie de), en, others; then: score
     */
    @Override
    public int compare(ArtworkBaseRecord arg0, ArtworkBaseRecord arg1) {
      if (preferredLangu.equals(arg0.language) && !preferredLangu.equals(arg1.language)) {
        return -1;
      }

      // check if second image is preferred langu
      if (!preferredLangu.equals(arg0.language) && preferredLangu.equals(arg1.language)) {
        return 1;
      }

      // check if the first image is en
      if (english.equals(arg0.language) && !english.equals(arg1.language)) {
        return -1;
      }

      // check if the second image is en
      if (!english.equals(arg0.language) && english.equals(arg1.language)) {
        return 1;
      }

      int result = 0;

      if (arg0.score != null && arg1.score != null) {
        // swap arg0 and arg1 to sort reverse
        result = Long.compare(arg1.score, arg0.score);
      }

      // if the result is still 0, we need to compare by ID (returning a zero here will treat it as a duplicate and remove the previous one)
      if (result == 0) {
        result = Long.compare(arg0.id, arg1.id);
      }

      return result;
    }
  }
}
