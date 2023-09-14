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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.entities.Episode;
import com.uwetrottmann.thetvdb.entities.EpisodeResponse;
import com.uwetrottmann.thetvdb.entities.Language;
import com.uwetrottmann.thetvdb.entities.LanguagesResponse;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;

import okhttp3.OkHttpClient;
import retrofit2.Response;

/**
 * The Class {@link TvdbV3MetadataProvider} - legacy v3 API!!
 *
 * @author Manuel Laggner
 */
abstract class TvdbV3MetadataProvider implements IMediaProvider {
  private static final String     ID                = "tvdbv3";

  protected static final String   ARTWORK_URL       = "https://artworks.thetvdb.com/banners/";
  protected static final String   FALLBACK_LANGUAGE = "fallbackLanguage";

  private final MediaProviderInfo providerInfo;

  protected final List<Language>  tvdbLanguages;
  protected TheTvdb               tvdb;

  TvdbV3MetadataProvider() {
    providerInfo = createMediaProviderInfo();
    tvdbLanguages = new ArrayList<>();
  }

  protected abstract Logger getLogger();

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "thetvdb.com - legacy API v3",
        "<html><h3>The TVDB</h3><br />An open database for television fans. This scraper is able to scrape TV series metadata and artwork.<br /><b>Legacy API v3.</b> This API will be turned off in the first half of 2022.</html>",
        TvdbV3MetadataProvider.class.getResource("/org/tinymediamanager/scraper/thetvdbv3_com.svg"));
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getUserApiKey());
  }

  protected synchronized void initAPI() throws ScrapeException {

    if (tvdb == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        tvdb = new TheTvdb(getApiKey()) {
          // tell the tvdb api to use our OkHttp client
          private OkHttpClient okHttpClient;

          @Override
          protected synchronized OkHttpClient okHttpClient() {
            if (this.okHttpClient == null) {
              OkHttpClient.Builder builder = TmmHttpClient.newBuilder(true); // with cache
              this.setOkHttpClientDefaults(builder);
              this.okHttpClient = builder.build();
            }

            return this.okHttpClient;
          }
        };
        Response<LanguagesResponse> httpResponse = tvdb.languages().allAvailable().execute();
        if (!httpResponse.isSuccessful()) {
          throw new HttpException(httpResponse.code(), httpResponse.message());
        }
        LanguagesResponse response = httpResponse.body();
        if (response == null) {
          throw new Exception("Could not connect to TVDB");
        }
        tvdbLanguages.clear();
        tvdbLanguages.addAll(response.data);
      }
      catch (Exception e) {
        getLogger().warn("could not initialize API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        tvdb = null;
        throw new ScrapeException(e);
      }
    }

    String userApiKey = providerInfo.getUserApiKey();

    // check if the API should change from current key to user key
    if (StringUtils.isNotBlank(userApiKey)) {
      tvdb.apiKey(userApiKey);
    }

    // check if the API should change from current key to tmm key
    if (StringUtils.isBlank(userApiKey)) {
      tvdb.apiKey(getApiKey());
    }
  }

  protected void fillFallbackLanguages(String language, String fallbackLanguage, Series serie) throws IOException {
    // check with fallback language
    Response<SeriesResponse> httpResponse;
    if ((StringUtils.isEmpty(serie.seriesName) || StringUtils.isEmpty(serie.overview)) && !fallbackLanguage.equals(language)) {
      getLogger().trace("Getting show in fallback language {}", fallbackLanguage);
      httpResponse = tvdb.series().series(serie.id, fallbackLanguage).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      serie.seriesName = StringUtils.isEmpty(serie.seriesName) ? httpResponse.body().data.seriesName : serie.seriesName;
      serie.overview = StringUtils.isEmpty(serie.overview) ? httpResponse.body().data.overview : serie.overview;
    }

    // STILL empty? check with EN language...
    if ((StringUtils.isEmpty(serie.seriesName) || StringUtils.isEmpty(serie.overview)) && !fallbackLanguage.equals("en") && !language.equals("en")) {
      getLogger().trace("Getting show in fallback language {}", "en");
      httpResponse = tvdb.series().series(serie.id, "en").execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      serie.seriesName = StringUtils.isEmpty(serie.seriesName) ? httpResponse.body().data.seriesName : serie.seriesName;
      serie.overview = StringUtils.isEmpty(serie.overview) ? httpResponse.body().data.overview : serie.overview;
    }
  }

  protected void fillFallbackLanguages(String language, String fallbackLanguage, Episode episode) throws IOException {
    // check with fallback language
    Response<EpisodeResponse> httpResponse;
    if ((StringUtils.isEmpty(episode.episodeName) || StringUtils.isEmpty(episode.overview)) && !fallbackLanguage.equals(language)) {
      getLogger().trace("Getting episode S{}E{} in fallback language {}", episode.airedSeason, episode.airedEpisodeNumber, fallbackLanguage);
      httpResponse = tvdb.episodes().get(episode.id, fallbackLanguage).execute();
      if (!httpResponse.isSuccessful()) {
        throw new HttpException(httpResponse.code(), httpResponse.message());
      }
      episode.episodeName = StringUtils.isEmpty(episode.episodeName) ? httpResponse.body().data.episodeName : episode.episodeName;
      episode.overview = StringUtils.isEmpty(episode.overview) ? httpResponse.body().data.overview : episode.overview;
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
}
