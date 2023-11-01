package org.tinymediamanager.scraper.rating.services;

import org.tinymediamanager.scraper.rating.entities.MdbListRatingEntity;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MdbListService {

    // Get Ratings via IMDB ID
    @GET("/api")
    Call<MdbListRatingEntity> getRatingsByImdbId(@Query("apikey") String apiKey, @Query("i") String imdbid);

    // Get Ratings via Trakt ID
    @GET("/api")
    Call<MdbListRatingEntity> getRatingsByTraktId(@Query("apikey") String apiKey, @Query("t") String imdbid);

    //Get Ratings via TMDB ID
    @GET("/api")
    Call<MdbListRatingEntity> getRatingsByTmdbId(@Query("apikey") String apiKey, @Query("tm") String imdbid);

    //Get Ratings via TVDB ID
    @GET("/api")
    Call<MdbListRatingEntity> getRatingsByTvdbId(@Query("apikey") String apiKey, @Query("tv") String imdbid);

}
