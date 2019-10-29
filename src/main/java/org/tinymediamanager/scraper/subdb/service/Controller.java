package org.tinymediamanager.scraper.subdb.service;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;

public class Controller {
  
  private static final Logger LOGGER   = LoggerFactory.getLogger(Controller.class);
  private Retrofit            retrofit;

  public Controller() {
    this(false,false);
  }

  /**
   * setting up the retrofit object with further debugging options if needed
   * 
   * @param debug
   *          true or false
   */
  public Controller(boolean debug, boolean sandbox ) {
    OkHttpClient.Builder builder = TmmHttpClient.newBuilder();
    if (debug) {
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor(s -> LOGGER.debug(s));
      logging.setLevel(Level.BODY); // BASIC?!
      builder.addInterceptor(logging);
    }
    
    if (sandbox) { 
      retrofit = buildRetrofitInstance(builder.build(),false);
    } else {
      retrofit = buildRetrofitInstance(builder.build(),true);
    }
  }

  /**
   * Returns the created Retrofit Service
   * 
   * @return retrofit object
   */
  private SubDBService getService() {
    return retrofit.create(SubDBService.class);
  }

  /**
   * Builder Class for retrofit Object
   * 
   * @param client
   *          the http client
   * @return a new retrofit object.
   */
  private Retrofit buildRetrofitInstance(OkHttpClient client, boolean sandbox ) {
    if (sandbox) {
      return new Retrofit.Builder().client(client).baseUrl("http://api.thesubdb.com")
          .addConverterFactory(ScalarsConverterFactory.create()).build();
    } else {
      return new Retrofit.Builder().client(client).baseUrl("http://sandbox.thesubdb.com")
          .addConverterFactory(ScalarsConverterFactory.create()).build();
    }
  }

  public String getSubtitles(String hash) throws IOException {
    return getService().getData("search",hash,null).execute().body();
  }

  public String getUrl() {
    return retrofit.baseUrl().url().toString();
  }
  

}
