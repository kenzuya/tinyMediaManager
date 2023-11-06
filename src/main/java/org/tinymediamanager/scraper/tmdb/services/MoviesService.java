package org.tinymediamanager.scraper.tmdb.services;

import java.util.Map;

import org.tinymediamanager.scraper.tmdb.entities.AlternativeTitles;
import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.Credits;
import org.tinymediamanager.scraper.tmdb.entities.ExternalIds;
import org.tinymediamanager.scraper.tmdb.entities.Images;
import org.tinymediamanager.scraper.tmdb.entities.Keywords;
import org.tinymediamanager.scraper.tmdb.entities.Movie;
import org.tinymediamanager.scraper.tmdb.entities.MovieResultsPage;
import org.tinymediamanager.scraper.tmdb.entities.RatingObject;
import org.tinymediamanager.scraper.tmdb.entities.ReleaseDate;
import org.tinymediamanager.scraper.tmdb.entities.ReleaseDatesResults;
import org.tinymediamanager.scraper.tmdb.entities.ReviewResultsPage;
import org.tinymediamanager.scraper.tmdb.entities.Status;
import org.tinymediamanager.scraper.tmdb.entities.Translations;
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

public interface MoviesService {

  /**
   * Get the basic movie information for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("movie/{movie_id}")
  Call<Movie> summary(@Path("movie_id") int movieId, @Query("language") String language);

  /**
   * Get the basic movie information for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result. <b>Accepted Value(s):</b> alternative_titles, changes, credits, images,
   *          keywords, release_dates, videos, translations, recommendations, similar, reviews, lists
   */
  @GET("movie/{movie_id}")
  Call<Movie> summary(@Path("movie_id") int movieId, @Query("language") String language,
      @Query("append_to_response") AppendToResponse appendToResponse);

  /**
   * Get the basic movie information for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result. <b>Accepted Value(s):</b> alternative_titles, changes, credits, images,
   *          keywords, release_dates, videos, translations, recommendations, similar, reviews, lists
   * @param options
   *          <em>Optional.</em> parameters for the appended extra results.
   */
  @GET("movie/{movie_id}")
  Call<Movie> summary(@Path("movie_id") int movieId, @Query("language") String language,
      @Query("append_to_response") AppendToResponse appendToResponse, @QueryMap Map<String, String> options);

  /**
   * Get the alternative titles for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param country
   *          <em>Optional.</em> ISO 3166-1 code.
   */
  @GET("movie/{movie_id}/alternative_titles")
  Call<AlternativeTitles> alternativeTitles(@Path("movie_id") int movieId, @Query("country") String country);

  /**
   * Get the cast and crew information for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   */
  @GET("movie/{movie_id}/credits")
  Call<Credits> credits(@Path("movie_id") int movieId);

  /**
   * Get the external ids that we have stored for a movie.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("movie/{movie_id}/external_ids")
  Call<ExternalIds> externalIds(@Path("movie_id") int movieId, @Query("language") String language);

  /**
   * Get the images (posters and backdrops) for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("movie/{movie_id}/images")
  Call<Images> images(@Path("movie_id") int movieId, @Query("language") String language);

  /**
   * Get the plot keywords for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   */
  @GET("movie/{movie_id}/keywords")
  Call<Keywords> keywords(@Path("movie_id") int movieId);

  /**
   * Get the similar movies for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("movie/{movie_id}/similar")
  Call<MovieResultsPage> similar(@Path("movie_id") int movieId, @Query("page") Integer page, @Query("language") String language);

  /**
   * Get the recommendations for a particular movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("movie/{movie_id}/recommendations")
  Call<MovieResultsPage> recommendations(@Path("movie_id") int movieId, @Query("page") Integer page, @Query("language") String language);

  /**
   * Get the release dates, certifications and related information by country for a specific movie id.
   *
   * The results are keyed by iso_3166_1 code and contain a type value which on our system, maps to: {@link ReleaseDate#TYPE_PREMIERE},
   * {@link ReleaseDate#TYPE_THEATRICAL_LIMITED}, {@link ReleaseDate#TYPE_THEATRICAL}, {@link ReleaseDate#TYPE_DIGITAL},
   * {@link ReleaseDate#TYPE_PHYSICAL}, {@link ReleaseDate#TYPE_TV}
   *
   * @param movieId
   *          A Movie TMDb id.
   */
  @GET("movie/{movie_id}/release_dates")
  Call<ReleaseDatesResults> releaseDates(@Path("movie_id") int movieId);

  /**
   * Get the reviews for a particular movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param page
   *          <em>Optional.</em> Minimum value is 1, expected value is an integer.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("movie/{movie_id}/reviews")
  Call<ReviewResultsPage> reviews(@Path("movie_id") int movieId, @Query("page") Integer page, @Query("language") String language);

  /**
   * Get the translations for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   */
  @GET("movie/{movie_id}/translations")
  Call<Translations> translations(@Path("movie_id") int movieId);

  /**
   * Get the videos (trailers, teasers, clips, etc...) for a specific movie id.
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("movie/{movie_id}/videos")
  Call<Videos> videos(@Path("movie_id") int movieId, @Query("language") String language);

  /**
   * Get a list of the availabilities per country by provider.
   *
   * Please note: In order to use this data you must attribute the source of the data as JustWatch.
   *
   * @see <a href="https://developers.themoviedb.org/3/movies/get-movie-watch-providers">Documentation</a>
   */
  @GET("movie/{movie_id}/watch/providers")
  Call<WatchProviders> watchProviders(@Path("movie_id") int movieId);

  /**
   * Get the latest movie id.
   */
  @GET("movie/latest")
  Call<Movie> latest();

  /**
   * Get a list of movies in theatres. This is a release type query that looks for all movies that have a release type of 2 or 3 within the specified
   * date range.
   *
   * You can optionally specify a region parameter which will narrow the search to only look for theatrical release dates within the specified
   * country.
   *
   * @see <a href="https://developers.themoviedb.org/3/movies/get-now-playing">Documentation</a>
   */
  @GET("movie/now_playing")
  Call<MovieResultsPage> nowPlaying(@Query("page") Integer page, @Query("language") String language, @Query("region") String region);

  /**
   * Get a list of the current popular movies on TMDb. This list updates daily.
   *
   * @see <a href="https://developers.themoviedb.org/3/movies/get-popular-movies">Documentation</a>
   */
  @GET("movie/popular")
  Call<MovieResultsPage> popular(@Query("page") Integer page, @Query("language") String language, @Query("region") String region);

  /**
   * Get the top rated movies on TMDb.
   *
   * @see <a href="https://developers.themoviedb.org/3/movies/get-top-rated-movies">Documentation</a>
   */
  @GET("movie/top_rated")
  Call<MovieResultsPage> topRated(@Query("page") Integer page, @Query("language") String language, @Query("region") String region);

  /**
   * Get a list of upcoming movies in theatres. This is a release type query that looks for all movies that have a release type of 2 or 3 within the
   * specified date range.
   *
   * You can optionally specify a region prameter which will narrow the search to only look for theatrical release dates within the specified country.
   *
   * @see <a href="https://developers.themoviedb.org/3/movies/get-upcoming">Documentation</a>
   */
  @GET("movie/upcoming")
  Call<MovieResultsPage> upcoming(@Query("page") Integer page, @Query("language") String language, @Query("region") String region);

  /**
   * Sets the Rating for the movie with the specified id.
   *
   * <b>Requires an active Session.</b>
   *
   * @param movieId
   *          A Movie TMDb id.
   * @param body
   *          <em>Required.</em> A ReviewObject Object. Minimum value is 0.5 and Maximum 10.0, expected value is a number.
   */
  @POST("movie/{movie_id}/rating")
  Call<Status> addRating(@Path("movie_id") Integer movieId, @Body RatingObject body);

  /**
   * Deletes the Rating for the movie with the specified id.
   *
   * <b>Requires an active Session.</b>
   *
   * @param movieId
   *          A Movie TMDb id.
   */
  @DELETE("movie/{movie_id}/rating")
  Call<Status> deleteRating(@Path("movie_id") Integer movieId);

}
