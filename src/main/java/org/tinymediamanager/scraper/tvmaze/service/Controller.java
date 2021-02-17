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
package org.tinymediamanager.scraper.tvmaze.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.tvmaze.entities.Cast;
import org.tinymediamanager.scraper.tvmaze.entities.Episode;
import org.tinymediamanager.scraper.tvmaze.entities.Image;
import org.tinymediamanager.scraper.tvmaze.entities.Season;
import org.tinymediamanager.scraper.tvmaze.entities.Show;
import org.tinymediamanager.scraper.tvmaze.entities.Shows;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Controller {

  private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
  Retrofit                    retrofit;
  private String              apiKey;

  /**
   * setting up the retrofit object with further debugging options if needed
   *
   * @param debug
   *          true or false
   */
  public Controller(String apiKey, boolean debug) {
    this.apiKey = apiKey;
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
    return new Retrofit.Builder().client(client).baseUrl(apiKey).addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create())).build();
  }

  /**
   * Returns the created Retrofit Service
   *
   * @return retrofit object
   */
  private TvMazeService getService() {
    return retrofit.create(TvMazeService.class);
  }

  public List<Shows> getTvShowSearchResults(String query) throws IOException {
    return getService().showSearch(query).execute().body();
  }

  public Show getMainInformation(int id) throws IOException {
    return getService().show_main_information(id).execute().body();
  }

  public List<Season> getSeasons(int id) throws IOException {
    return getService().seasonList(id).execute().body();
  }

  public List<Episode> getEpisodes(int id) throws IOException {
    return getService().episodeList(id).execute().body();
  }

  public List<Image> getImages(int id) throws IOException {
    return getService().imagesList(id).execute().body();
  }

  public List<Cast> getCast(int id) throws IOException {
    return getService().castList(id).execute().body();
  }

}
