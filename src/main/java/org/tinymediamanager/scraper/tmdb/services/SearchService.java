package org.tinymediamanager.scraper.tmdb.services;

import org.tinymediamanager.scraper.tmdb.entities.CollectionResultsPage;
import org.tinymediamanager.scraper.tmdb.entities.MovieResultsPage;
import org.tinymediamanager.scraper.tmdb.entities.TvShowResultsPage;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SearchService {

  /**
   * Search for collections.
   *
   * @see <a href="https://developers.themoviedb.org/3/search/search-collections">Documentation</a>
   */
  @GET("search/collection")
  Call<CollectionResultsPage> collection(@Query("query") String query, @Query("page") Integer page, @Query("language") String language);

  /**
   * Search for movies.
   *
   * @see <a href="https://developers.themoviedb.org/3/search/search-movies">Documentation</a>
   */
  @GET("search/movie")
  Call<MovieResultsPage> movie(@Query("query") String query, @Query("page") Integer page, @Query("language") String language,
      @Query("region") String region, @Query("include_adult") Boolean includeAdult, @Query("year") Integer year,
      @Query("primary_release_year") Integer primaryReleaseYear);

  /**
   * Search for TV shows.
   *
   * @see <a href="https://developers.themoviedb.org/3/search/search-tv-shows">Documentation</a>
   */
  @GET("search/tv")
  Call<TvShowResultsPage> tv(@Query("query") String query, @Query("page") Integer page, @Query("language") String language,
      @Query("first_air_date_year") Integer firstAirDateYear, @Query("include_adult") Boolean includeAdult);
}
