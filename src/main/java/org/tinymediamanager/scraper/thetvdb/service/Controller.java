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
package org.tinymediamanager.scraper.thetvdb.service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.thetvdb.entities.LoginRequestRecord;
import org.tinymediamanager.scraper.thetvdb.entities.LoginResponse;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Controller {
  private static final Logger LOGGER       = LoggerFactory.getLogger(Controller.class);
  private static final String API_BASE_URL = "https://api4.thetvdb.com/v4/";

  private final boolean       debug;

  private Retrofit            restAdapter;
  private String              authToken;

  public Controller() {
    this(false);
  }

  /**
   * setting up the retrofit object with further debugging options if needed
   *
   * @param debug
   *          true or false
   */
  public Controller(boolean debug) {
    this.debug = debug;
  }

  private Retrofit getRestAdapter() {
    if (restAdapter == null) {
      OkHttpClient.Builder builder = TmmHttpClient.newBuilderWithForcedCache(15, TimeUnit.MINUTES);
      if (debug) {
        builder.addInterceptor(new HttpLoggingInterceptor(LOGGER::debug).setLevel(HttpLoggingInterceptor.Level.BODY));
      }
      builder.addInterceptor(chain -> {
        Request original = chain.request();
        Request.Builder request = original.newBuilder().method(original.method(), original.body());
        request.addHeader("Authorization", "Bearer " + authToken);
        return chain.proceed(request.build());
      });
      restAdapter = new Retrofit.Builder().client(builder.build())
          .baseUrl(API_BASE_URL)
          .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()))
          .build();
    }

    return restAdapter;
  }

  private static GsonBuilder getGsonBuilder() {
    GsonBuilder builder = new GsonBuilder();
    // class types
    builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> {
      try {
        return json.getAsInt();
      }
      catch (NumberFormatException e) {
        return -1;
      }
    });
    builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
    return builder;
  }

  public static String login(String apiKey) throws Exception {
    if (StringUtils.isBlank(apiKey)) {
      return null;
    }

    OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
    Retrofit retrofit = new Retrofit.Builder().client(builder.build())
        .baseUrl(API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()))
        .build();
    LoginService loginService = retrofit.create(LoginService.class);

    LoginRequestRecord data = new LoginRequestRecord();
    data.apikey = apiKey;
    data.pin = "";

    Response<LoginResponse> response = loginService.login(data).execute();
    String token = null;

    if (response.isSuccessful()) {
      token = response.body().data.token;
    }

    if (StringUtils.isBlank(token)) {
      throw new HttpException(403, "Unauthorized");
    }

    return token;
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  /**
   * get an instance of the {@link ConfigService}
   * 
   * @return an instance of the {@link ConfigService}
   */
  public ConfigService getConfigService() {
    return getRestAdapter().create(ConfigService.class);
  }

  /**
   * get an instance of the {@link SearchService}
   * 
   * @return an instance of the {@link SearchService}
   */
  public SearchService getSearchService() {
    return getRestAdapter().create(SearchService.class);
  }

  /**
   * get an instance of the {@link SeriesService}
   *
   * @return an instance of the {@link SeriesService}
   */
  public SeriesService getSeriesService() {
    return getRestAdapter().create(SeriesService.class);
  }

  /**
   * get an instance of the {@link EpisodesService}
   *
   * @return an instance of the {@link EpisodesService}
   */
  public EpisodesService getEpisodesService() {
    return getRestAdapter().create(EpisodesService.class);
  }

  /**
   * get an instance of the {@link SeasonsService}
   *
   * @return an instance of the {@link SeasonsService}
   */
  public SeasonsService getSeasonsService() {
    return getRestAdapter().create(SeasonsService.class);
  }

  /**
   * get an instance of the {@link MoviesService}
   *
   * @return an instance of the {@link MoviesService}
   */
  public MoviesService getMoviesService() {
    return getRestAdapter().create(MoviesService.class);
  }
}
