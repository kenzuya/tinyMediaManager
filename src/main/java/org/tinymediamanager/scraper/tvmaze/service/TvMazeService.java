/*
 * Copyright 2012 - 2024 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.tvmaze.service;

import java.util.List;

import org.tinymediamanager.scraper.tvmaze.entities.Cast;
import org.tinymediamanager.scraper.tvmaze.entities.Episode;
import org.tinymediamanager.scraper.tvmaze.entities.Image;
import org.tinymediamanager.scraper.tvmaze.entities.Season;
import org.tinymediamanager.scraper.tvmaze.entities.Show;
import org.tinymediamanager.scraper.tvmaze.entities.Shows;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TvMazeService {

  @GET("/search/shows")
  Call<List<Shows>> showSearch(@Query("q") String query);

  @GET("/shows/{id}")
  Call<Show> show_main_information(@Path("id") int id);

  @GET("/shows/{id}/seasons")
  Call<List<Season>> seasonList(@Path("id") int id);

  @GET("/shows/{id}/episodes")
  Call<List<Episode>> episodeList(@Path("id") int id);

  @GET("/shows/{id}/episodebynumber")
  Call<Episode> episode(@Path("id") int id, @Query("season") int season, @Query("number") int episode);

  @GET("/shows/{id}/images")
  Call<List<Image>> imagesList(@Path("id") int id);

  @GET("/shows/{id}/cast")
  Call<List<Cast>> castList(@Path("id") int id);

}
