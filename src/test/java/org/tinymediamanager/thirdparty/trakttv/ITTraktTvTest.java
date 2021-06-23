package org.tinymediamanager.thirdparty.trakttv;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;

public class ITTraktTvTest extends BasicTest {

  private static final TraktTv t = TraktTv.getInstance();

  @BeforeClass
  public static void setup() {
    BasicTest.setup();
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    TmmModuleManager.getInstance().shutDown();
    MovieModuleManager.getInstance().shutDown();
    TvShowModuleManager.getInstance().shutDown();
  }

  @Test
  public void auth() {
    System.out.println();
  }

  @Test
  public void syncTraktMovieCollection() throws Exception {
    t.syncTraktMovieCollection(MovieModuleManager.getInstance().getMovieList().getMovies());
  }

  @Test
  public void syncTraktMovieWatched() throws Exception {
    t.syncTraktMovieWatched(MovieModuleManager.getInstance().getMovieList().getMovies());
  }

  @Test
  public void syncTraktTvShowCollection() throws Exception {
    t.syncTraktTvShowCollection(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
  }

  @Test
  public void syncTraktTvShowWatched() throws Exception {
    t.syncTraktTvShowWatched(TvShowModuleManager.getInstance().getTvShowList().getTvShows());
  }

  // @Test
  // public void getTvLib() {
  // List<TvShow> shows = t.getManager().userService().libraryShowsWatched(Settings.getInstance().getTraktUsername(), Extended.MIN);
  // System.out.println(shows.size());
  // }
  //
  // @Test
  // public void getGenres() {
  // List<Genre> mg = t.getManager().genreService().movies();
  // mg.addAll(t.getManager().genreService().shows());
  // for (Genre genre : mg) {
  // System.out.println(genre.name);
  // }
  // }

  @Test
  public void clearTvShows() throws Exception {
    t.clearTraktTvShows();
  }

  @Test
  public void clearMovies() throws Exception {
    t.clearTraktMovies();
  }
}
