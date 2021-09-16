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
package org.tinymediamanager.scraper.tmdb;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.TmdbInterceptor;
import com.uwetrottmann.tmdb2.entities.Configuration;
import com.uwetrottmann.tmdb2.entities.Genre;
import com.uwetrottmann.tmdb2.entities.Translations;
import com.uwetrottmann.tmdb2.entities.Translations.Translation;

import okhttp3.OkHttpClient;

/**
 * The Class TmdbMetadataProvider. A meta data, artwork and trailer provider for the site themoviedb.org
 *
 * @author Manuel Laggner
 */
abstract class TmdbMetadataProvider implements IMediaProvider {
  static final String             ID = "tmdb";

  // Use primary translations, not just our internal MediaLanguages (we need the country!)
  // https://api.themoviedb.org/3/configuration/primary_translations?api_key=XXXX
  // And keep on duplicate languages the main country on first position!
  protected static final String[] PT = new String[] { "ar-AE", "ar-SA", "be-BY", "bg-BG", "bn-BD", "ca-ES", "ch-GU", "cs-CZ", "da-DK", "de-DE",
      "el-GR", "en-US", "en-AU", "en-CA", "en-GB", "eo-EO", "es-ES", "es-MX", "eu-ES", "fr-FR", "fa-IR", "fi-FI", "fr-CA", "gl-ES", "he-IL", "hi-IN",
      "hu-HU", "id-ID", "it-IT", "ja-JP", "ka-GE", "kn-IN", "ko-KR", "lt-LT", "ml-IN", "nb-NO", "nl-NL", "no-NO", "pl-PL", "pt-BR", "pt-PT", "ro-RO",
      "ru-RU", "si-LK", "sk-SK", "sl-SI", "sr-RS", "sv-SE", "ta-IN", "te-IN", "th-TH", "tr-TR", "uk-UA", "vi-VN", "zh-CN", "zh-HK", "zh-TW" };

  private final MediaProviderInfo providerInfo;

  protected Tmdb                  api;
  protected Configuration         configuration;
  protected String                artworkBaseUrl;

  TmdbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "themoviedb.org",
        "<html><h3>The Movie Database (TMDb)</h3><br />The largest free movie database maintained by the community. It provides metadata and artwork<br />in many different languages. Thus it is the first choice for non english users<br /><br />Available languages: multiple</html>",
        TmdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/themoviedb_org.svg"));
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getConfig().getValue("apiKey"));
  }

  // thread safe initialization of the API
  protected synchronized void initAPI() throws ScrapeException {

    // create a new instance of the tmdb api
    if (api == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        String userApiKey = providerInfo.getConfig().getValue("apiKey");
        api = new Tmdb(StringUtils.isNotBlank(userApiKey) ? userApiKey : getApiKey()) {
          // tell the tmdb api to use our OkHttp client

          @Override
          protected synchronized OkHttpClient okHttpClient() {
            OkHttpClient.Builder builder = TmmHttpClient.newBuilder(true);
            builder.connectTimeout(30, TimeUnit.SECONDS);
            builder.writeTimeout(30, TimeUnit.SECONDS);
            builder.readTimeout(30, TimeUnit.SECONDS);
            builder.addInterceptor(new TmdbInterceptor(this));
            return builder.build();
          }
        };

        configuration = api.configurationService().configuration().execute().body();
        if (configuration == null) {
          throw new Exception("Invalid TMDB API key");
        }
        artworkBaseUrl = configuration.images.secure_base_url;
      }
      catch (Exception e) {
        getLogger().error("could not initialize the API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        api = null;
        throw new ScrapeException(e);
      }
    }

    String userApiKey = providerInfo.getConfig().getValue("apiKey");

    // check if the API should change from current key to user key
    if (StringUtils.isNotBlank(userApiKey)) {
      api.apiKey(userApiKey);
    }

    // check if the API should change from current key to tmm key
    if (StringUtils.isBlank(userApiKey)) {
      api.apiKey(getApiKey());
    }
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  protected abstract Logger getLogger();

  /**
   * tries to find correct title & overview from all the translations<br>
   * everything can be null/empty
   *
   * @param translations
   *          the translations to search in
   * @param locale
   *          the locale to search for
   * @return the found translation or null
   */
  protected Translation getTranslationForLocale(Translations translations, Locale locale) {
    Translation ret = null;

    if (translations != null && translations.translations != null && !translations.translations.isEmpty()) {
      for (Translation tr : translations.translations) {
        // check with language AND country
        if (tr.iso_639_1.equals(locale.getLanguage()) && tr.iso_3166_1.equals(locale.getCountry())) {
          ret = tr;
          break;
        }
      }

      if (ret == null) {
        // did not find exact translation, check again with language OR country
        switch (locale.toString()) {
          // exceptions are here es_MX, pt_BR and fr_CA which are completely different
          case "es_MX":
          case "pt_BR":
          case "fr_CA":
            // do nothing
            break;

          default:
            for (Translation tr : translations.translations) {
              if (tr.iso_639_1.equals(locale.getLanguage()) || tr.iso_3166_1.equals(locale.getCountry())) {
                ret = tr;
                break;
              }
            }
            break;
        }
      }
    }

    return ret;
  }

  /**
   * 0 is title(movie) or name(show)<br>
   * 1 is overview<br>
   * both may be empty, but never null
   *
   * @param translations
   *          the translations to search in
   * @param locale
   *          the locale to search for
   * @return the title or null
   */
  protected String[] getValuesFromTranslation(Translations translations, Locale locale) {
    String[] ret = new String[] { "", "" };

    Translation tr = getTranslationForLocale(translations, locale);
    if (tr == null || tr.data == null) {
      return ret;
    }

    if (!StringUtils.isEmpty(tr.data.title)) {
      ret[0] = tr.data.title; // movie
    }
    if (!StringUtils.isEmpty(tr.data.name)) {
      ret[0] = tr.data.name; // show
    }

    if (!StringUtils.isEmpty(tr.data.overview)) {
      ret[1] = tr.data.overview;
    }

    return ret;
  }

  /**
   * Maps scraper Genres to internal TMM genres
   */
  static MediaGenres getTmmGenre(Genre genre) {
    if (genre == null || MetadataUtil.unboxInteger(genre.id) == 0) {
      return null;
    }

    MediaGenres g = null;
    switch (genre.id) {
      case 28:
      case 10759:
        g = MediaGenres.ACTION;
        break;

      case 12:
        g = MediaGenres.ADVENTURE;
        break;

      case 16:
        g = MediaGenres.ANIMATION;
        break;

      case 35:
        g = MediaGenres.COMEDY;
        break;

      case 80:
        g = MediaGenres.CRIME;
        break;

      case 105:
        g = MediaGenres.DISASTER;
        break;

      case 99:
        g = MediaGenres.DOCUMENTARY;
        break;

      case 18:
        g = MediaGenres.DRAMA;
        break;

      case 82:
        g = MediaGenres.EASTERN;
        break;

      case 2916:
        g = MediaGenres.EROTIC;
        break;

      case 10751:
        g = MediaGenres.FAMILY;
        break;

      case 10750:
        g = MediaGenres.FAN_FILM;
        break;

      case 14:
        g = MediaGenres.FANTASY;
        break;

      case 10753:
        g = MediaGenres.FILM_NOIR;
        break;

      case 10769:
        g = MediaGenres.FOREIGN;
        break;

      case 36:
        g = MediaGenres.HISTORY;
        break;

      case 10595:
        g = MediaGenres.HOLIDAY;
        break;

      case 27:
        g = MediaGenres.HORROR;
        break;

      case 10756:
        g = MediaGenres.INDIE;
        break;

      case 10402:
        g = MediaGenres.MUSIC;
        break;

      case 22:
        g = MediaGenres.MUSICAL;
        break;

      case 9648:
        g = MediaGenres.MYSTERY;
        break;

      case 10754:
        g = MediaGenres.NEO_NOIR;
        break;

      case 10763:
        g = MediaGenres.NEWS;
        break;

      case 10764:
        g = MediaGenres.REALITY_TV;
        break;

      case 1115:
        g = MediaGenres.ROAD_MOVIE;
        break;

      case 10749:
        g = MediaGenres.ROMANCE;
        break;

      case 878:
      case 10765:
        g = MediaGenres.SCIENCE_FICTION;
        break;

      case 10755:
        g = MediaGenres.SHORT;
        break;

      case 10766:
        g = MediaGenres.SOAP;
        break;

      case 9805:
        g = MediaGenres.SPORT;
        break;

      case 10758:
        g = MediaGenres.SPORTING_EVENT;
        break;

      case 10757:
        g = MediaGenres.SPORTS_FILM;
        break;

      case 10748:
        g = MediaGenres.SUSPENSE;
        break;

      case 10767:
        g = MediaGenres.TALK_SHOW;
        break;

      case 10770:
        g = MediaGenres.TV_MOVIE;
        break;

      case 53:
        g = MediaGenres.THRILLER;
        break;

      case 10752:
      case 10768:
        g = MediaGenres.WAR;
        break;

      case 37:
        g = MediaGenres.WESTERN;
        break;
    }
    if (g == null) {
      g = MediaGenres.getGenre(genre.name);
    }
    return g;
  }

  /**
   * tmdb works better if we send a "real" language tag (containing language AND country); since we have only the language tag we do the same hack as
   * described in the tmdb api (By default, a bare ISO-639-1 language will default to its matching pair, ie. pt-PT - source
   * https://developers.themoviedb.org/3/getting-started/languages), but without the bug they have ;)
   * 
   * @param language
   *          the {@link MediaLanguages} to parse
   * @return a {@link String} containing the language and country code
   */
  static String getRequestLanguage(MediaLanguages language) {
    String name = language.name();

    Locale locale;

    if (name.length() > 2) {
      // language tag is longer than 2 characters -> we have the country
      locale = language.toLocale();
    }
    else {
      // try to get the right locale with the language tag in front an end (e.g. de-DE)
      locale = new Locale(name, name.toUpperCase(Locale.ROOT));
      // now check if the resulting locale is valid
      if (!LocaleUtils.isAvailableLocale(locale)) {
        // no => fallback to default
        locale = language.toLocale();
      }
    }

    if (locale == null) {
      return null;
    }

    return locale.toLanguageTag();
  }
}
