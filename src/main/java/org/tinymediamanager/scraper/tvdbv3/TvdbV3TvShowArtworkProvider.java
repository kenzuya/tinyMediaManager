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
package org.tinymediamanager.scraper.tvdbv3;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.ALL;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.uwetrottmann.thetvdb.entities.Language;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResult;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResultResponse;
import com.uwetrottmann.thetvdb.entities.SeriesImagesQueryParam;
import com.uwetrottmann.thetvdb.entities.SeriesImagesQueryParamResponse;

import retrofit2.Response;

/**
 * the class {@link TvdbV3TvShowArtworkProvider} offer artwork for TV shows - legacy v3 API!!
 *
 * @author Manuel Laggner
 */
public class TvdbV3TvShowArtworkProvider extends TvdbV3MetadataProvider implements ITvShowArtworkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvdbV3TvShowArtworkProvider.class);

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
        if (options.getIds().get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
          Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get(MediaMetadata.TVSHOW_IDS);
          episodeSearchAndScrapeOptions.setTvShowIds(tvShowIds);
        }
        MediaMetadata md = new TvdbV3TvShowMetadataProvider().getMetadata(episodeSearchAndScrapeOptions);
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
    Integer id = options.getIdAsInteger(MediaMetadata.TVDB);

    if (id == null || id == 0) {
      LOGGER.warn("no id available");
      throw new MissingIdException(MediaMetadata.TVDB);
    }

    // get artwork from thetvdb
    List<SeriesImageQueryResult> images = new ArrayList<>();
    try {
      // get all types of artwork we can get
      Response<SeriesImagesQueryParamResponse> response = tvdb.series().imagesQueryParams(id).execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response.code(), response.message());
      }
      for (SeriesImagesQueryParam param : response.body().data) {
        if (options.getArtworkType() == ALL || ("fanart".equals(param.keyType) && options.getArtworkType() == BACKGROUND)
            || ("poster".equals(param.keyType) && options.getArtworkType() == POSTER)
            || ("season".equals(param.keyType) && options.getArtworkType() == SEASON_POSTER)
            || ("seasonwide".equals(param.keyType) && options.getArtworkType() == SEASON_BANNER)
            || ("series".equals(param.keyType) && options.getArtworkType() == BANNER)) {

          try {
            Response<SeriesImageQueryResultResponse> httpResponse = tvdb.series().imagesQuery(id, param.keyType, null, null, null).execute();
            if (!httpResponse.isSuccessful()) {
              throw new HttpException(httpResponse.code(), httpResponse.message());
            }
            images.addAll(httpResponse.body().data);
          }
          catch (Exception e) {
            LOGGER.error("could not get artwork from tvdb: {}", e.getMessage());
          }
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("failed to get artwork: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (images.isEmpty()) {
      return artwork;
    }

    // sort it
    images.sort(new ImageComparator(options.getLanguage().getLanguage()));

    // build output
    for (SeriesImageQueryResult image : images) {
      MediaArtwork ma = null;

      // set artwork type
      switch (image.keyType) {
        case "fanart":
          ma = new MediaArtwork(getProviderInfo().getId(), BACKGROUND);
          break;

        case "poster":
          ma = new MediaArtwork(getProviderInfo().getId(), POSTER);
          break;

        case "season":
          ma = new MediaArtwork(getProviderInfo().getId(), SEASON_POSTER);
          try {
            ma.setSeason(Integer.parseInt(image.subKey));
          }
          catch (Exception e) {
            LOGGER.trace("could not parse season: {}", image.subKey);
          }
          break;

        case "seasonwide":
          ma = new MediaArtwork(getProviderInfo().getId(), SEASON_BANNER);
          try {
            ma.setSeason(Integer.parseInt(image.subKey));
          }
          catch (Exception e) {
            LOGGER.trace("could not parse season: {}", image.subKey);
          }
          break;

        case "series":
          ma = new MediaArtwork(getProviderInfo().getId(), BANNER);
          break;

        default:
          continue;
      }

      // extract image sizes
      if (StringUtils.isNotBlank(image.resolution)) {
        try {
          Pattern pattern = Pattern.compile("([0-9]{3,4})x([0-9]{3,4})");
          Matcher matcher = pattern.matcher(image.resolution);
          if (matcher.matches() && matcher.groupCount() > 1) {
            int width = Integer.parseInt(matcher.group(1));
            int height = Integer.parseInt(matcher.group(2));
            int sizeOrder = getSizeOrder(ma.getType(), width);

            ma.addImageSize(width, height, ARTWORK_URL + image.fileName, sizeOrder);
          }
        }
        catch (Exception e) {
          LOGGER.debug("could not extract size from artwork: {}", image.resolution);
        }
      }

      ma.setOriginalUrl(ARTWORK_URL + image.fileName);
      if (StringUtils.isNotBlank(image.thumbnail)) {
        ma.setPreviewUrl(ARTWORK_URL + image.thumbnail);
      }
      else {
        ma.setPreviewUrl(ma.getOriginalUrl());
      }

      if (StringUtils.isBlank(image.language)) {
        // no text
        ma.setLanguage("");
      }
      else {
        ma.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(image.language));
      }

      artwork.add(ma);
    }

    return artwork;
  }

  /**********************************************************************
   * local helper classes
   **********************************************************************/
  private class ImageComparator implements Comparator<SeriesImageQueryResult> {
    private int preferredLangu = 0;
    private int english        = 0;

    private ImageComparator(String language) {
      for (Language lang : tvdbLanguages) {
        if (language.equals(lang.abbreviation)) {
          preferredLangu = lang.id;
        }
        if ("en".equals(lang.abbreviation)) {
          english = lang.id;
        }
      }
    }

    /*
     * sort artwork: primary by language: preferred lang (ie de), en, others; then: score
     */
    @Override
    public int compare(SeriesImageQueryResult arg0, SeriesImageQueryResult arg1) {
      // check if first image is preferred langu
      int languageId0 = MetadataUtil.unboxInteger(arg0.languageId, -1);
      int languageId1 = MetadataUtil.unboxInteger(arg1.languageId, -1);

      if (languageId0 == preferredLangu && languageId1 != preferredLangu) {
        return -1;
      }

      // check if second image is preferred langu
      if (languageId0 != preferredLangu && languageId1 == preferredLangu) {
        return 1;
      }

      // check if the first image is en
      if (languageId0 == english && languageId1 != english) {
        return -1;
      }

      // check if the second image is en
      if (languageId0 != english && languageId1 == english) {
        return 1;
      }

      int result = 0;

      if (arg0.ratingsInfo != null && arg1.ratingsInfo != null) {
        int ratingCount0 = MetadataUtil.unboxInteger(arg0.ratingsInfo.count);
        int ratingCount1 = MetadataUtil.unboxInteger(arg1.ratingsInfo.count);

        // swap arg0 and arg1 to sort reverse
        result = Integer.compare(ratingCount1, ratingCount0);
      }

      // if the result is still 0, we need to compare by ID (returning a zero here will treat it as a duplicate and remove the previous one)
      if (result == 0) {
        result = Integer.compare(arg1.id, arg0.id);
      }

      return result;
    }
  }
}
