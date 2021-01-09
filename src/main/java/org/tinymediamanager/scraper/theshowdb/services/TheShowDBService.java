/*
 * Copyright 2012 - 2020 Manuel Laggner
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
package org.tinymediamanager.scraper.theshowdb.services;

import org.tinymediamanager.scraper.theshowdb.entities.Episode;
import org.tinymediamanager.scraper.theshowdb.entities.Episodes;
import org.tinymediamanager.scraper.theshowdb.entities.Season;
import org.tinymediamanager.scraper.theshowdb.entities.Show;
import org.tinymediamanager.scraper.theshowdb.entities.Shows;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

public interface TheShowDBService {

  //Search
  @GET("{apikey}/search_show.php")
  Call<Shows> searchShowByName(@Path("apikey") String apikey, @Query("s") String query);

  //List
  @GET("{apikey}/list_seasons.php")
  Call<List<Season>> listAllSeasonsByShowId(@Path("apikey") String apikey, @Query("id") String id);

  @GET("{apikey}/list_all_season_episodes.php")
  Call<List<Episode>> listAllEpisodesBySeasonId(@Path("apikey") String apikey, @Query("id") String id);

  @GET("{apikey}/list_all_show_episodes.php")
  Call<Episodes> listAllEpisodesByShowId(@Path("apikey") String apiKey, @Query("id") String id);

  //Lookup
  @GET("{apikey}/lookup_show.php")
  Call<Shows> lookupShowDetailsByShowId(@Path("apikey") String apikey, @Query("id") String id);

  @GET("{apikey}/lookup_show_and_seasons.php")
  Call<Show> lookupShowAndSeasonDetailsByShowId(@Path("apikey") String apikey, @Query("id") String id);

  @GET("{apikey}/lookup_season.php")
  Call<Season> lookupSeasonDetailsBySeasonId(@Path("apikey") String apikey, @Query("id") String id);

  @GET("{apikey}/lookup_episode.php")
  Call<Episode> lookupEpisodeDetailsByEpisodeId(@Path("apikey") String apikey, @Query("id") String id);

}
