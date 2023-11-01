package org.tinymediamanager.scraper.rating.services;

import java.io.IOException;
import java.util.Date;

import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.rating.entities.MdbListRatingEntity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.internal.bind.DateTypeAdapter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MdbListController {

  private final Retrofit retrofit;

  public MdbListController() {
    OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
    builder.addInterceptor(chain -> {
      Request request = chain.request();
      Response response = chain.proceed(request);
      return response;
    });
    retrofit = buildRetrofitInstance(builder.build());

  }

  private GsonBuilder getGsonBuilder() {
    GsonBuilder builder = new GsonBuilder().setLenient();
    // class types
    builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> {
      try {
        return json.getAsInt();
      } catch (NumberFormatException e) {
        return 0;
      }
    });
    builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
    return builder;
  }

  private Retrofit buildRetrofitInstance(OkHttpClient client) {
    return new Retrofit.Builder().client(client)
            .baseUrl("https://mdblist.com/")
            .addConverterFactory(GsonConverterFactory.create(getGsonBuilder().create()))
            .build();
  }

  private MdbListService getService() {
    return retrofit.create(MdbListService.class);
  }

  public retrofit2.Response<MdbListRatingEntity> getRatingsByImdbId(String apikey, String imdbId) throws IOException {
    return getService().getRatingsByImdbId(apikey, imdbId).execute();
  }

  public retrofit2.Response<MdbListRatingEntity> getRatingsByTraktId(String apikey, String traktId) throws IOException {
    return getService().getRatingsByTraktId(apikey, traktId).execute();
  }

  public retrofit2.Response<MdbListRatingEntity> getRatingsByTmdbId(String apikey, String tmdbId) throws IOException {
    return getService().getRatingsByTmdbId(apikey, tmdbId).execute();
  }

  public retrofit2.Response<MdbListRatingEntity> getRatingsByTvdbId(String apikey, String tvdbId) throws IOException {
    return getService().getRatingsByTvdbId(apikey, tvdbId).execute();
  }

}
