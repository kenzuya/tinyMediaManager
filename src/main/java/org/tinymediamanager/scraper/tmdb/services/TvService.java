package org.tinymediamanager.scraper.tmdb.services;

import java.util.Map;

import org.tinymediamanager.scraper.tmdb.entities.AlternativeTitles;
import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.ContentRatings;
import org.tinymediamanager.scraper.tmdb.entities.Credits;
import org.tinymediamanager.scraper.tmdb.entities.ExternalIds;
import org.tinymediamanager.scraper.tmdb.entities.Images;
import org.tinymediamanager.scraper.tmdb.entities.Keywords;
import org.tinymediamanager.scraper.tmdb.entities.RatingObject;
import org.tinymediamanager.scraper.tmdb.entities.Status;
import org.tinymediamanager.scraper.tmdb.entities.Translations;
import org.tinymediamanager.scraper.tmdb.entities.TvShow;
import org.tinymediamanager.scraper.tmdb.entities.TvShowResultsPage;
import org.tinymediamanager.scraper.tmdb.entities.Videos;
import org.tinymediamanager.scraper.tmdb.entities.WatchProviders;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface TvService {

  /**
   * Get the primary information about a TV series by id.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}")
  Call<TvShow> tv(@Path("tv_id") int tvShowId, @Query("language") String language);

  /**
   * Get the primary information about a TV series by id.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   */
  @GET("tv/{tv_id}")
  Call<TvShow> tv(@Path("tv_id") int tvShowId, @Query("language") String language, @Query("append_to_response") AppendToResponse appendToResponse);

  /**
   * Get the primary information about a TV series by id.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   * @param options
   *          <em>Optional.</em> parameters for the appended extra results.
   */
  @GET("tv/{tv_id}")
  Call<TvShow> tv(@Path("tv_id") int tvShowId, @Query("language") String language, @Query("append_to_response") AppendToResponse appendToResponse,
      @QueryMap Map<String, String> options);

  /**
   * Get the alternative titles for a specific show ID.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   */
  @GET("tv/{tv_id}/alternative_titles")
  Call<AlternativeTitles> alternativeTitles(@Path("tv_id") int tvShowId);

  /**
   * Get the cast and crew information about a TV series. Just like the website, we pull this information from the last season of the series.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/credits")
  Call<Credits> credits(@Path("tv_id") int tvShowId, @Query("language") String language);

  /**
   * Get the content ratings for a specific TV show.
   *
   * @param tmbdId
   *          A Tv Show TMDb id.
   */
  @GET("tv/{tv_id}/content_ratings")
  Call<ContentRatings> content_ratings(@Path("tv_id") int tmbdId);

  /**
   * Get the external ids that we have stored for a TV series.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/external_ids")
  Call<ExternalIds> externalIds(@Path("tv_id") int tvShowId, @Query("language") String language);

  /**
   * Get the images (posters and backdrops) for a TV series.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/images")
  Call<Images> images(@Path("tv_id") int tvShowId, @Query("language") String language);

  /**
   * Get the plot keywords for a specific TV show id.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   */
  @GET("tv/{tv_id}/keywords")
  Call<Keywords> keywords(@Path("tv_id") int tvShowId);

  /**
   * Get the list of TV show recommendations for this item.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/recommendations")
  Call<TvShowResultsPage> recommendations(@Path("tv_id") int tvShowId, @Query("page") Integer page, @Query("language") String language);

  /**
   * Get the similar TV shows for a specific tv id.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/similar")
  Call<TvShowResultsPage> similar(@Path("tv_id") int tvShowId, @Query("page") Integer page, @Query("language") String language);

  /**
   * Get a list of the translations that exist for a TV show.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/translations")
  Call<Translations> translations(@Path("tv_id") int tvShowId, @Query("language") String language);

  /**
   * Get the videos that have been added to a TV series (trailers, opening credits, etc...)
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/videos")
  Call<Videos> videos(@Path("tv_id") int tvShowId, @Query("language") String language);

  /**
   * Get a list of the availabilities per country by provider.
   *
   * Please note: In order to use this data you must attribute the source of the data as JustWatch.
   *
   * @see <a href="https://developers.themoviedb.org/3/tv/get-tv-watch-providers">Documentation</a>
   */
  @GET("tv/{tv_id}/watch/providers")
  Call<WatchProviders> watchProviders(@Path("tv_id") int tvShowId);

  /**
   * Get the latest TV show id.
   */
  @GET("tv/latest")
  Call<TvShow> latest();

  /**
   * Get the list of TV shows that are currently on the air. This query looks for any TV show that has an episode with an air date in the next 7 days.
   *
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/on_the_air")
  Call<TvShowResultsPage> onTheAir(@Query("page") Integer page, @Query("language") String language);

  /**
   * Get the list of TV shows that air today. Without a specified timezone, this query defaults to EST (Eastern Time UTC-05:00).
   *
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/airing_today")
  Call<TvShowResultsPage> airingToday(@Query("page") Integer page, @Query("language") String language);

  /**
   * Get the list of top rated TV shows. By default, this list will only include TV shows that have 2 or more votes. This list refreshes every day.
   *
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/top_rated")
  Call<TvShowResultsPage> topRated(@Query("page") Integer page, @Query("language") String language);

  /**
   * Get the list of popular TV shows. This list refreshes every day.
   *
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/popular")
  Call<TvShowResultsPage> popular(@Query("page") Integer page, @Query("language") String language);

  /**
   * Rate a TV show.
   *
   * <b>Requires an active Session.</b>
   *
   * @param tvShowId
   *          TMDb id.
   * @param body
   *          <em>Required.</em> A ReviewObject Object. Minimum value is 0.5 and Maximum 10.0, expected value is a number.
   */
  @POST("tv/{tv_id}/rating")
  Call<Status> addRating(@Path("tv_id") Integer tvShowId, @Body RatingObject body);

  /**
   * Remove your rating for a TV show.
   *
   * <b>Requires an active Session.</b>
   *
   * @param tvShowId
   *          TMDb id.
   */
  @DELETE("tv/{tv_id}/rating")
  Call<Status> deleteRating(@Path("tv_id") Integer tvShowId);

}
