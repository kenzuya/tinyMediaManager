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
package org.tinymediamanager.scraper.thetvdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.scraper.thetvdb.entities.AllSeriesResponse;
import org.tinymediamanager.scraper.thetvdb.entities.EpisodeBaseResponse;
import org.tinymediamanager.scraper.thetvdb.entities.EpisodeExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonBaseResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeasonType;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesBaseResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesEpisodesResponse;
import org.tinymediamanager.scraper.thetvdb.entities.SeriesExtendedResponse;
import org.tinymediamanager.scraper.thetvdb.service.Controller;

public class ITTheTvDbApiTest extends BasicTest {
  private static Controller controller;

  @BeforeClass
  public static void setUp() throws Exception {
    setLicenseKey();
    TheTvDbMetadataProvider md = new TheTvDbTvShowMetadataProvider();

    controller = new Controller(true);
    controller.setAuthToken(md.getAuthToken());
  }

  @Test
  public void testAllSeries() throws Exception {
    AllSeriesResponse response = controller.getSeriesService().getAllSeries(1).execute().body();
    assertThat(response.data).isNotEmpty();
  }

  @Test
  public void testSeriesBase() throws Exception {
    SeriesBaseResponse response = controller.getSeriesService().getSeriesBase(79335).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.id).isEqualTo(79335);
    assertThat(response.data.name).isEqualTo("Psych");
  }

  @Test
  public void testSeriesExtended() throws Exception {
    SeriesExtendedResponse response = controller.getSeriesService().getSeriesExtended(79335).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.id).isEqualTo(79335);
    assertThat(response.data.name).isEqualTo("Psych");
  }

  @Test
  public void testSeriesEpisodes() throws Exception {
    SeriesEpisodesResponse response = controller.getSeriesService().getSeriesEpisodes(79335, SeasonType.DEFAULT, 0).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.series).isNotNull();
    assertThat(response.data.series.id).isEqualTo(79335);
    assertThat(response.data.series.name).isEqualTo("Psych");
    assertThat(response.data.episodes).isNotEmpty();
  }

  @Test
  public void testSeasonsBase() throws Exception {
    SeasonBaseResponse response = controller.getSeasonsService().getSeasonBase(16284).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.number).isEqualTo(1);
    assertThat(response.data.seriesId).isEqualTo(79335);
  }

  @Test
  public void testSeasonsExtended() throws Exception {
    SeasonExtendedResponse response = controller.getSeasonsService().getSeasonExtended(16284).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.number).isEqualTo(1);
    assertThat(response.data.seriesId).isEqualTo(79335);
    assertThat(response.data.episodes).isNotEmpty();
    assertThat(response.data.episodes.get(0).episodeNumber).isEqualTo(1);
    assertThat(response.data.episodes.get(0).seasonNumber).isEqualTo(1);
  }

  @Test
  public void testEpisodesBase() throws Exception {
    EpisodeBaseResponse response = controller.getEpisodesService().getEpisodeBase(307497).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.episodeNumber).isEqualTo(1);
    assertThat(response.data.seasonNumber).isEqualTo(1);
  }

  @Test
  public void testEpisodesExtended() throws Exception {
    EpisodeExtendedResponse response = controller.getEpisodesService().getEpisodeExtended(307497).execute().body();
    assertThat(response.data).isNotNull();
    assertThat(response.data.episodeNumber).isEqualTo(1);
    assertThat(response.data.seasonNumber).isEqualTo(1);
  }
}
