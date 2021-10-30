package org.tinymediamanager.scraper.tmdb.services;

import java.util.Map;

import org.tinymediamanager.scraper.tmdb.entities.AppendToResponse;
import org.tinymediamanager.scraper.tmdb.entities.Collection;
import org.tinymediamanager.scraper.tmdb.entities.Images;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface CollectionsService {

  /**
   * Get the basic collection information for a specific collection id.
   *
   * @param collectionId
   *          TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("collection/{collection_id}")
  Call<Collection> summary(@Path("collection_id") int collectionId, @Query("language") String language);

  /**
   * Get the basic collection information for a specific collection id.
   *
   * @param collectionId
   *          TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   */
  @GET("collection/{collection_id}")
  Call<Collection> summary(@Path("collection_id") int collectionId, @Query("language") String language,
      @Query("append_to_response") AppendToResponse appendToResponse);

  /**
   * Get the basic collection information for a specific collection id.
   *
   * @param collectionId
   *          TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   * @param appendToResponse
   *          <em>Optional.</em> extra requests to append to the result.
   * @param options
   *          <em>Optional.</em> parameters for the appended extra results.
   */
  @GET("collection/{collection_id}")
  Call<Collection> summary(@Path("collection_id") int collectionId, @Query("language") String language,
      @Query("append_to_response") AppendToResponse appendToResponse, @QueryMap Map<String, String> options);

  /**
   * Get the images (posters and backdrops) for a specific collection id.
   *
   * @param collectionId
   *          TMDb id.
   * @param language
   *          <em>Optional.</em> ISO 639-1 code.
   */
  @GET("collection/{collection_id}/images")
  Call<Images> images(@Path("collection_id") int collectionId, @Query("language") String language);
}
