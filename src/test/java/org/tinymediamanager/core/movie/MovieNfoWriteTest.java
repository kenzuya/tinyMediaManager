/*
 * Copyright 2012 - 2021 Manuel Laggner
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
package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.BasicTest;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieUpdateDatasourceTask;

public class MovieNfoWriteTest extends BasicTest {

  private static final int NUMBER_OF_EXPECTED_MOVIES = 6;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // MediaInfoUtils.loadMediaInfo(); // unneeded here for UDS. does not work on buildserver
    deleteSettingsFolder();
    Settings.getInstance(getSettingsFolder());
  }

  @Before
  public void setUpBeforeTest() throws Exception {
    setLicenseKey();

    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    // just a copy; we might have another movie test which uses these files
    FileUtils.copyDirectory(new File("target/test-classes/testmovies_nfo"), new File(getSettingsFolder(), "testmovies_nfo"));
    MovieModuleManager.SETTINGS.addMovieDataSources(Paths.get(getSettingsFolder(), "/testmovies_nfo").toAbsolutePath().toString());
  }

  // @After
  // public void tearDownAfterTest() throws Exception {
  // MovieModuleManager.getInstance().shutDown();
  // TmmModuleManager.getInstance().shutDown();
  // Utils.deleteDirectoryRecursive(Paths.get(getSettingsFolder(), "testmovies_nfo"));
  // Files.delete(new File(getSettingsFolder(), "movies.db"));
  // }

  private void loadMovies() throws Exception {
    MovieUpdateDatasourceTask task = new MovieUpdateDatasourceTask();
    task.run();

    // let the propertychangeevents finish
    Thread.sleep(2000);
  }

  @Test
  public void testDefault() throws Exception {
    // load movies
    loadMovies();

    assertThat(MovieList.getInstance().getMovies().size()).isEqualTo(NUMBER_OF_EXPECTED_MOVIES);

    // just rewrite the NFO file
    for (Movie movie : MovieList.getInstance().getMovies()) {
      movie.writeNFO();
    }

    // and now check the file names
    Path base = Paths.get(getSettingsFolder(), "testmovies_nfo");

    // bluray
    Path nfo = Paths.get(base.toString(), "BluRay", "BDMV", "index.nfo");
    assertThat(nfo.toFile().exists()).isTrue();

    // plain dvd files
    nfo = Paths.get(base.toString(), "DVD", "VIDEO_TS.nfo");
    assertThat(nfo.toFile().exists()).isTrue();

    // nested dvd files
    nfo = Paths.get(base.toString(), "DVDfolder", "VIDEO_TS", "VIDEO_TS.nfo");
    assertThat(nfo.toFile().exists()).isTrue();

    // single movie dir
    nfo = Paths.get(base.toString(), "Single", "movie.nfo");
    assertThat(nfo.toFile().exists()).isTrue();

    // multi movie dir
    nfo = Paths.get(base.toString(), "Multi1", "multifile1.nfo");
    assertThat(nfo.toFile().exists()).isTrue();
    nfo = Paths.get(base.toString(), "Multi1", "multifile2.nfo");
    assertThat(nfo.toFile().exists()).isTrue();
  }
}
