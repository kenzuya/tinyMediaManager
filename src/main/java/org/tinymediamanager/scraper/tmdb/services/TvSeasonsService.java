package org.tinymediamanager.scraper.tmdb.services;

import java.util.Map;

import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.Credits;
import org.tinymediamanager.scraper.tmdb.entities.Images;
import org.tinymediamanager.scraper.tmdb.entities.Translations;
import org.tinymediamanager.scraper.tmdb.entities.TvSeason;
import org.tinymediamanager.scraper.tmdb.entities.TvSeasonExternalIds;
import org.tinymediamanager.scraper.tmdb.entities.Videos;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface TvSeasonsService {

  /**
   * Get the primary information about a TV season by its season number.
   *
   * @param tvShowId
   *          A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/season/{season_number}")
  Call<TvSeason> season(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Query("language") String language);

  /**
   * Get the primary information about a TV season by its season number.
   *
   * @param tvShowId
   *          A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   */
  @GET("tv/{tv_id}/season/{season_number}")
  Call<TvSeason> season(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Query("language") String language,
      @Query("append_to_response") AppendToResponse appendToResponse);

  /**
   * Get the primary information about a TV season by its season number.
   *
   * @param tvShowId
   *          A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   * @param options
   *          <em>Optional.</em> parameters for the appended extra results.
   */
  @GET("tv/{tv_id}/season/{season_number}")
  Call<TvSeason> season(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Query("language") String language,
      @Query("append_to_response") AppendToResponse appendToResponse, @QueryMap Map<String, String> options);

  /**
   * Get the cast and crew credits for a TV season by season number.
   *
   * @param tvShowId
   *          A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   */
  @GET("tv/{tv_id}/season/{season_number}/credits")
  Call<Credits> credits(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber);

  /**
   * Get the external ids that we have stored for a TV season by season number.
   *
   * @param tvShowId
   *          A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/season/{season_number}/external_ids")
  Call<TvSeasonExternalIds> externalIds(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber,
      @Query("language") String language);

  /**
   * Get the images (posters) that we have stored for a TV season by season number.
   *
   * @param tvShowId
   *          A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/season/{season_number}/images")
  Call<Images> images(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Query("language") String language);

  /**
   * Get the videos that have been added to a TV season (trailers, teasers, etc...)
   *
   * @param tvShowId
   *          A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/season/{season_number}/videos")
  Call<Videos> videos(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Query("language") String language);

  /**
   * Get the translations that have been added to a TV season
   *
   * @param tvShowId           A Tv Show TvSeason TMDb id.
   * @param tvShowSeasonNumber TvSeason Number.
   */
  @GET("tv/{tv_id}/season/{season_number}/translations")
  Call<Translations> translations(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber);
}
