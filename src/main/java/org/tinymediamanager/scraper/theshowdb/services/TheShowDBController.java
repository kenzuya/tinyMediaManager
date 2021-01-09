/*
 * Copyright 2012 - 2020 Manuel Laggner
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
package org.tinymediamanager.scraper.theshowdb.services;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.theshowdb.entities.Episode;
import org.tinymediamanager.scraper.theshowdb.entities.Episodes;
import org.tinymediamanager.scraper.theshowdb.entities.Season;
import org.tinymediamanager.scraper.theshowdb.entities.Show;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.tinymediamanager.scraper.theshowdb.entities.Shows;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TheShowDBController {

  private static final Logger LOGGER = LoggerFactory.getLogger(TheShowDBController.class);
  Retrofit                    retrofit;

  /**
   * setting up the retrofit object with further debugging options if needed
   * 
   * @param debug
   *          true or false
   */
  public TheShowDBController(boolean debug) {
    OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
    if (debug) {
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor(LOGGER::debug);
      logging.setLevel(HttpLoggingInterceptor.Level.BODY); // BASIC?!
      builder.addInterceptor(logging);
    }
    retrofit = buildRetrofitInstance(builder.build());
  }

  private GsonBuilder getGsonBuilder() {
    GsonBuilder builder = new GsonBuilder();
    // class types
    builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> {
      try {
        return json.getAsInt();
      }
      catch (NumberFormatException e) {
        return 0;
      }
    });
    builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
    return builder;
  }

  /**
   * Builder Class for retrofit Object
   *
   * @param client
   *          the http client
   * @return a new retrofit object.
   */
  private Retrofit buildRetrofitInstance(OkHttpClient client) {
    return new Retrofit.Builder().client(client).baseUrl("https://www.theshowdb.com/api/json/v1/")
        .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create())).build();
  }

  /**
   * Returns the created Retrofit Service
   *
   * @return retrofit object
   */
  private TheShowDBService getService() {
    return retrofit.create(TheShowDBService.class);
  }

  public Shows getShowByName(String apiKey, String query) throws IOException {
    return getService().searchShowByName(apiKey, query).execute().body();
  }

  public List<Season> getAllSeasonsByShowId(String apikey, String id) throws IOException {
    return getService().listAllSeasonsByShowId(apikey, id).execute().body();
  }

  public List<Episode> getAllEpisodesBySeasonId(String apikey, String id) throws IOException {
    return getService().listAllEpisodesBySeasonId(apikey, id).execute().body();
  }

  public Episodes getAllEpisodesByShowId(String apikey, String id) throws IOException {
    return getService().listAllEpisodesByShowId(apikey, id).execute().body();
  }

  public Shows getShowDetailsByShowId(String apikey, String id) throws IOException {
    return getService().lookupShowDetailsByShowId(apikey, id).execute().body();
  }

  public Season getSeasonDetailsBySeasonId(String apikey, String id) throws IOException {
    return getService().lookupSeasonDetailsBySeasonId(apikey, id).execute().body();
  }

  public Episode getEpisodeDetailsByEpisodeId(String apikey, String id) throws IOException {
    return getService().lookupEpisodeDetailsByEpisodeId(apikey, id).execute().body();
  }

}
