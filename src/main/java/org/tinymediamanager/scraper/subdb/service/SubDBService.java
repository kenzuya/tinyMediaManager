package org.tinymediamanager.scraper.subdb.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface SubDBService {

  @Headers({"User-Agent: SubDB/1.0 (TheSubDB-Scraper/0.1; http://gitlab.com/TinyMediaManager)"})
  
  @GET("/")
  Call<String> getData(@Query("action") String action, @Query("hash") String hash, @Query("language") String lang);

}
