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

package org.tinymediamanager.core.movie.tasks;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.BasicTest;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;

/**
 * This class cannot run, since Settings() is STATIC<br>
 * run these test individually (for now)
 * 
 * @author Myron Boyle
 *
 */
public class MovieUpdateDatasourceTaskTest extends BasicTest {

  private static final int NUMBER_OF_EXPECTED_MOVIES = 71;
  private static final int NUMBER_OF_STACKED_MOVIES  = 12;
  private static final int NUMBER_OF_DISC_MOVIES     = 6;

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
    FileUtils.copyDirectory(new File("target/test-classes/testmovies"), new File(getSettingsFolder(), "testmovies"));
    MovieModuleManager.SETTINGS.addMovieDataSources(Paths.get(getSettingsFolder(), "/testmovies").toAbsolutePath().toString());
  }

  @After
  public void tearDownAfterTest() throws Exception {
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
    Utils.deleteDirectoryRecursive(Paths.get(getSettingsFolder(), "testmovies"));
    Files.delete(new File(getSettingsFolder(), "movies.db"));
  }

  @Test
  public void udsNew() throws Exception {
    MovieUpdateDatasourceTask task = new MovieUpdateDatasourceTask();
    task.run();

    showEntries();
  }

  private void showEntries() throws Exception {
    // wait until all movies have been added (let propertychanges finish)
    for (int i = 0; i < 20; i++) {
      if (MovieList.getInstance().getMovieCount() == NUMBER_OF_EXPECTED_MOVIES) {
        break;
      }

      // not all here yet? wait for a second
      System.out.println("waiting for 1000 ms");
      Thread.sleep(1000);
    }

    assertEqual("Amount of movies does not match!", NUMBER_OF_EXPECTED_MOVIES, MovieList.getInstance().getMovieCount());

    int stack = 0;
    int disc = 0;
    for (Movie m : MovieList.getInstance().getMovies()) {
      System.out.println(rpad(m.getTitle(), 30) + "(Disc:" + rpad(m.isDisc(), 5) + " Stack:" + rpad(m.isStacked(), 5) + " Multi:"
          + rpad(m.isMultiMovieDir(), 5) + ")\t" + m.getPathNIO());
      if (m.isStacked()) {
        stack++;
      }
      if (m.isDisc()) {
        disc++;
      }
    }
    assertEqual("Amount of stacked movies does not match!", NUMBER_OF_STACKED_MOVIES, stack);
    assertEqual("Amount of disc folders does not match!", NUMBER_OF_DISC_MOVIES, disc);
  }

  public static String rpad(Object s, int n) {
    return String.format("%1$-" + n + "s", s);
  }
}
