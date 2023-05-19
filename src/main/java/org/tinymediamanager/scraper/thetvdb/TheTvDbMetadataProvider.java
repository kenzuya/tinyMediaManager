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
package org.tinymediamanager.scraper.thetvdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.PRODUCER;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_THUMB;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkBaseRecord;
import org.tinymediamanager.scraper.thetvdb.entities.ArtworkTypeRecord;
import org.tinymediamanager.scraper.thetvdb.entities.Character;
import org.tinymediamanager.scraper.thetvdb.entities.SearchResultResponse;
import org.tinymediamanager.scraper.util.LanguageUtils;

import retrofit2.Response;

/**
 * The Class TheTvDbMetadataProvider.
 *
 * @author Manuel Laggner
 */
abstract class TheTvDbMetadataProvider implements IMediaProvider {
  private static final Logger                   LOGGER            = LoggerFactory.getLogger(TheTvDbMetadataProvider.class);
  private static final String                   ID                = "tvdb";

  protected static final String                 FALLBACK_LANGUAGE = "fallbackLanguage";
  protected static final Pattern                ID_PATTERN        = Pattern.compile("\\d{3,}");

  private final MediaProviderInfo               providerInfo;
  private final Map<Integer, ArtworkTypeRecord> artworkTypes      = new HashMap<>();

  protected TheTvDbController                   tvdb;

  TheTvDbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  protected abstract Logger getLogger();

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "thetvdb.com",
        "<html><h3>The TVDB</h3><br />An open database for television fans. This scraper is able to scrape TV series metadata and artwork</html>",
        TheTvDbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/thetvdb_com.svg"), 30);
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getConfig().getValue("apiKey"));
  }

  String getAuthToken() {
    String userApiKey = providerInfo.getConfig().getValue("apiKey");
    String userPin = providerInfo.getConfig().getValue("pin");

    if (StringUtils.isNotBlank(userApiKey)) {
      // always remember to avoid a force-reinit at every call
      tvdb.setUserApiKey(userApiKey);
      tvdb.setUserPin(userPin);

      try {
        return TheTvDbController.login(userApiKey, userPin);
      }
      catch (Exception e) {
        LOGGER.warn("could not logon with the user entered key - '{}'", e.getMessage());
      }
    }

    return getApiKey();
  }

  protected synchronized void initAPI() throws ScrapeException {

    // check if the API should change from current key to another
    if (tvdb != null) {
      String userApiKey = providerInfo.getConfig().getValue("apiKey");
      String userPin = providerInfo.getConfig().getValue("pin");
      if (StringUtils.isNotBlank(userApiKey) && (!userApiKey.equals(tvdb.getUserApiKey()) || !userPin.equals(tvdb.getUserPin()))) {
        // force re-initialization with new key
        tvdb = null;
      }
      else if (StringUtils.isBlank(userApiKey) && StringUtils.isNotBlank(tvdb.getUserApiKey())) {
        // force re-initialization with new key
        tvdb = null;
      }
    }

    if (tvdb == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        tvdb = new TheTvDbController();
        tvdb.setAuthToken(getAuthToken());

        artworkTypes.clear();

        for (ArtworkTypeRecord artworkTypeRecord : Objects.requireNonNull(tvdb.getConfigService().getArtworkTypes().execute().body()).data) {
          if (artworkTypeRecord.width > 0 && artworkTypeRecord.height > 0) {
            artworkTypes.put(artworkTypeRecord.id, artworkTypeRecord);
          }
        }
      }
      catch (Exception e) {
        getLogger().warn("could not initialize API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        tvdb = null;
        throw new ScrapeException(e);
      }
    }
  }

  /**
   * try to strip out the year from the title
   * 
   * @param title
   *          the title to strip out the year
   * @param year
   *          the year to compare
   * @return the cleaned title or the original title if there is nothing to clean
   */
  protected String clearYearFromTitle(String title, int year) {
    return title.replaceAll("\\(" + year + "\\)$", "").trim();
  }

  /**
   * get the {@link ArtworkTypeRecord} for the given id
   * 
   * @param id
   *          the id to get the artwork type for
   * @return the {@link ArtworkTypeRecord} or null
   */
  protected ArtworkTypeRecord getArtworkType(Integer id) {
    if (id == null) {
      return null;
    }

    return artworkTypes.get(id);
  }

  /**
   * parse the localized text from the given texts
   * 
   * @param desiredLanguage
   *          the desired {@link MediaLanguages} to get the text in
   * @param localizedTexts
   *          a {@link Map} of translated texts
   * @return the translated text or an empty {@link String}
   */
  protected String parseLocalizedText(MediaLanguages desiredLanguage, Map<String, String> localizedTexts) {
    if (localizedTexts == null) {
      return "";
    }

    String iso3LanguageTag = LanguageUtils.getIso3Language(desiredLanguage.toLocale());

    // pt-BR is pt at tvdb...
    if ("pob".equals(iso3LanguageTag)) {
      iso3LanguageTag = "pt";
    }

    String text = localizedTexts.get(iso3LanguageTag);
    if (StringUtils.isNotBlank(text)) {
      return text;
    }

    return "";
  }

  protected List<Person> parseCastMembers(List<Character> characters) {
    if (characters == null) {
      return Collections.emptyList();
    }

    // sort by the field sort
    characters.sort(Comparator.comparingInt(o -> o.sort));

    List<Person> members = new ArrayList<>();

    for (Character character : characters) {
      Person member;

      switch (character.type) {

        case 1:
          member = new Person(DIRECTOR);
          break;

        case 2:
          member = new Person(WRITER);
          break;

        case 3:
        case 4:
          member = new Person(ACTOR);
          member.setRole(character.name);
          break;

        case 7:
          member = new Person(PRODUCER);
          break;

        default:
          continue;
      }

      member.setId(getId(), character.peopleId);
      member.setName(character.personName);

      if (StringUtils.isNotBlank(character.image)) {
        member.setThumbUrl(character.image);
      }
      if (StringUtils.isNotBlank(character.url)) {
        member.setProfileUrl(character.url);
      }

      members.add(member);
    }

    return members;
  }

  /**
   * parse the {@link ArtworkBaseRecord} and morph it to {@link MediaArtwork}
   * 
   * @param image
   *          the {@link ArtworkBaseRecord} from TVDB
   * @return the parsed {@link MediaArtwork}
   */
  protected MediaArtwork parseArtwork(ArtworkBaseRecord image) {
    if (image.id == null) {
      return null;
    }

    MediaArtwork ma = null;

    // set artwork type
    switch (image.type) {
      case 1:
      case 16:
        ma = new MediaArtwork(getProviderInfo().getId(), BANNER);
        break;

      case 2:
      case 14:
        ma = new MediaArtwork(getProviderInfo().getId(), POSTER);
        break;

      case 3:
      case 15:
        ma = new MediaArtwork(getProviderInfo().getId(), BACKGROUND);
        break;

      case 6:
        ma = new MediaArtwork(getProviderInfo().getId(), SEASON_BANNER);
        break;

      case 7:
        ma = new MediaArtwork(getProviderInfo().getId(), SEASON_POSTER);
        break;

      case 8:
        ma = new MediaArtwork(getProviderInfo().getId(), SEASON_THUMB);
        break;

      case 11:
      case 12:
        ma = new MediaArtwork(getProviderInfo().getId(), THUMB);
        break;

      default:
        return null;
    }

    // extract image sizes
    ArtworkTypeRecord artworkType = getArtworkType(image.type);
    if (artworkType != null) {
      int width = artworkType.width;
      int height = artworkType.height;
      ma.addImageSize(width, height, image.image);

      // set image size
      switch (ma.getType()) {
        case POSTER:
          if (width >= 1000) {
            ma.setSizeOrder(MediaArtwork.PosterSizes.LARGE.getOrder());
          }
          else if (width >= 500) {
            ma.setSizeOrder(MediaArtwork.PosterSizes.BIG.getOrder());
          }
          else if (width >= 342) {
            ma.setSizeOrder(MediaArtwork.PosterSizes.MEDIUM.getOrder());
          }
          else {
            ma.setSizeOrder(MediaArtwork.PosterSizes.SMALL.getOrder());
          }
          break;

        case BACKGROUND:
          if (width >= 3840) {
            ma.setSizeOrder(MediaArtwork.FanartSizes.XLARGE.getOrder());
          }
          if (width >= 1920) {
            ma.setSizeOrder(MediaArtwork.FanartSizes.LARGE.getOrder());
          }
          else if (width >= 1280) {
            ma.setSizeOrder(MediaArtwork.FanartSizes.MEDIUM.getOrder());
          }
          else {
            ma.setSizeOrder(MediaArtwork.FanartSizes.SMALL.getOrder());
          }
          break;

        default:
          break;
      }
    }

    // set size for banner & season poster (resolution not in api)
    if (ma.getType() == SEASON_BANNER || ma.getType() == SEASON_POSTER) {
      ma.setSizeOrder(MediaArtwork.FanartSizes.LARGE.getOrder());
    }
    else if (ma.getType() == BANNER) {
      ma.setSizeOrder(MediaArtwork.FanartSizes.MEDIUM.getOrder());
    }

    ma.setDefaultUrl(image.image);
    ma.setOriginalUrl(image.image);
    if (StringUtils.isNotBlank(image.thumbnail)) {
      ma.setPreviewUrl(image.thumbnail);
    }
    else {
      ma.setPreviewUrl(ma.getDefaultUrl());
    }

    if (StringUtils.isBlank(image.language)) {
      // no text
      ma.setLanguage("-");
    }
    else {
      ma.setLanguage(LanguageUtils.getIso2LanguageFromLocalizedString(image.language));
    }

    // get the season number
    if ((ma.getType() == SEASON_BANNER || ma.getType() == SEASON_POSTER || ma.getType() == SEASON_THUMB) && image.season != null) {
      ma.setSeason(image.season);
    }

    return ma;
  }

  protected MediaSearchResult morphMediaMetadataToSearchResult(MediaMetadata md, MediaType type) {
    MediaSearchResult searchResult = new MediaSearchResult(getId(), type);
    searchResult.setTitle(md.getTitle());
    searchResult.setYear(md.getYear());
    searchResult.setIds(md.getIds());
    searchResult.setMetadata(md);
    for (MediaArtwork artwork : md.getMediaArt()) {
      if (artwork.getType() == MediaArtwork.MediaArtworkType.POSTER) {
        searchResult.setPosterUrl(artwork.getDefaultUrl());
        break;
      }
    }
    searchResult.setScore(1);

    return searchResult;
  }

  protected int getTvdbIdViaImdbId(String imdbId) {
    // try to get it via service call
    try {
      Response<SearchResultResponse> httpResponse = tvdb.getSearchService().getSearch(imdbId, imdbId).execute();
      if (httpResponse.isSuccessful() && httpResponse.body() != null) {
        return Integer.parseInt(httpResponse.body().data.get(0).tvdbId);
      }
    }
    catch (Exception e) {
      getLogger().debug("could not fetch TVDB via IMDB - '{}'", e.getMessage());
    }

    return 0;
  }
}
