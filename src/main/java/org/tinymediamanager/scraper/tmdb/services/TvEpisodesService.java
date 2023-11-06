package org.tinymediamanager.scraper.tmdb.services;

import java.util.Map;

import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.Credits;
import org.tinymediamanager.scraper.tmdb.entities.ExternalIds;
import org.tinymediamanager.scraper.tmdb.entities.Images;
import org.tinymediamanager.scraper.tmdb.entities.RatingObject;
import org.tinymediamanager.scraper.tmdb.entities.Status;
import org.tinymediamanager.scraper.tmdb.entities.TvEpisode;
import org.tinymediamanager.scraper.tmdb.entities.Videos;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface TvEpisodesService {

  /**
   * Get the primary information about a TV episode by combination of a season and episode number.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
  Call<TvEpisode> episode(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Path("episode_number") int tvShowEpisodeNumber,
      @Query("language") String language);

  /**
   * Get the primary information about a TV episode by combination of a season and episode number.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   */
  @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
  Call<TvEpisode> episode(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Path("episode_number") int tvShowEpisodeNumber,
      @Query("language") String language, @Query("append_to_response") AppendToResponse appendToResponse);

  /**
   * Get the primary information about a TV episode by combination of a season and episode number.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   * @param options
   *          <em>Optional.</em> parameters for the appended extra results.
   */
  @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
  Call<TvEpisode> episode(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Path("episode_number") int tvShowEpisodeNumber,
      @Query("language") String language, @Query("append_to_response") AppendToResponse appendToResponse, @QueryMap Map<String, String> options);

  /**
   * Get the TV episode credits by combination of season and episode number.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   */
  @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/credits")
  Call<Credits> credits(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Path("episode_number") int tvShowEpisodeNumber);

  /**
   * Get the external ids for a TV episode by combination of a season and episode number.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   */
  @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/external_ids")
  Call<ExternalIds> externalIds(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber,
      @Path("episode_number") int tvShowEpisodeNumber);

  /**
   * Get the images (episode stills) for a TV episode by combination of a season and episode number. Since episode stills don't have a language, this
   * call will always return all images.
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   */
  @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/images")
  Call<Images> images(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Path("episode_number") int tvShowEpisodeNumber);

  /**
   * Get the videos that have been added to a TV episode (teasers, clips, etc...)
   *
   * @param tvShowId
   *          A Tv Show TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/videos")
  Call<Videos> videos(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Path("episode_number") int tvShowEpisodeNumber,
      @Query("language") String language);

  /**
   * Rate a TV show.
   *
   * <b>Requires an active Session.</b>
   *
   * @param tvShowId
   *          TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   * @param body
   *          <em>Required.</em> A ReviewObject Object. Minimum value is 0.5 and Maximum 10.0, expected value is a number.
   */
  @POST("tv/{tv_id}/season/{season_number}/episode/{episode_number}/rating")
  Call<Status> addRating(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber, @Path("episode_number") int tvShowEpisodeNumber,
      @Body RatingObject body);

  /**
   * Remove your rating for a TV show.
   *
   * <b>Requires an active Session.</b>
   *
   * @param tvShowId
   *          TMDb id.
   * @param tvShowSeasonNumber
   *          TvSeason Number.
   * @param tvShowEpisodeNumber
   *          TvEpisode Number.
   */
  @DELETE("tv/{tv_id}/season/{season_number}/episode/{episode_number}/rating")
  Call<Status> deleteRating(@Path("tv_id") int tvShowId, @Path("season_number") int tvShowSeasonNumber,
      @Path("episode_number") int tvShowEpisodeNumber);

}
