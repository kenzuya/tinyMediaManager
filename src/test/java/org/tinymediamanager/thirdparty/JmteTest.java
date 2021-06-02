package org.tinymediamanager.thirdparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tinymediamanager.core.jmte.JmteUtils.morphTemplate;

import java.nio.file.Paths;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter.TemplateType;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieExporter;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.tvshow.TvShowExporter;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowRenamer;

public class JmteTest extends BasicTest {

  @BeforeClass
  public static void init() throws Exception {
    BasicTest.setup();

    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();

    createFakeMovie("Movie 1");
    createFakeMovie("Another Movie");
    createFakeMovie("Cool Movie");

    createFakeShow("Best Show");
    createFakeShow("THE show");
    createFakeShow("Show 3");
  }

  @AfterClass
  public static void shutdown() throws Exception {
    TvShowModuleManager.getInstance().shutDown();
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void testAllMovieTemplates() throws Exception {
    MovieList ml = MovieModuleManager.getInstance().getMovieList();
    for (ExportTemplate t : MovieExporter.findTemplates(TemplateType.MOVIE)) {
      System.out.println("\nTEMPLATE: " + t.getPath());
      MovieExporter ex = new MovieExporter(Paths.get(t.getPath()));
      ex.export(ml.getMovies(), Paths.get(getSettingsFolder(), t.getName()));
    }
  }

  @Test
  public void testAllTvShowTemplates() throws Exception {
    TvShowList tv = TvShowModuleManager.getInstance().getTvShowList();
    for (ExportTemplate t : TvShowExporter.findTemplates(TemplateType.TV_SHOW)) {
      System.out.println("\nTEMPLATE: " + t.getPath());
      TvShowExporter ex = new TvShowExporter(Paths.get(t.getPath()));
      ex.export(tv.getTvShows(), Paths.get(getSettingsFolder(), t.getName()));
    }
  }

  @Test
  public void testMovieReplacements() throws Exception {
    Map<String, String> tokenMap = MovieRenamer.getTokenMap();

    assertThat(morphTemplate("${movie.title}", tokenMap)).isEqualTo("${movie.title}");
    assertThat(morphTemplate("${title}", tokenMap)).isEqualTo("${movie.title}");
    assertThat(morphTemplate("${title[2]}", tokenMap)).isEqualTo("${movie.title[2]}");
    assertThat(morphTemplate("${movie.title[2]}", tokenMap)).isEqualTo("${movie.title[2]}");
    assertThat(morphTemplate("${if title}${title[2]}${end}", tokenMap)).isEqualTo("${if movie.title}${movie.title[2]}${end}");
    assertThat(morphTemplate("${foreach genres genre}${genre}${end}", tokenMap)).isEqualTo("${foreach movie.genres genre}${genre}${end}");
    assertThat(morphTemplate("${title;lower}", tokenMap)).isEqualTo("${movie.title;lower}");
  }

  @Test
  public void testTvShowReplacements() throws Exception {
    Map<String, String> tokenMap = TvShowRenamer.getTokenMap();

    assertThat(morphTemplate("${episode.title}", tokenMap)).isEqualTo("${episode.title}");
    assertThat(morphTemplate("${title}", tokenMap)).isEqualTo("${episode.title}");
    assertThat(morphTemplate("${title[2]}", tokenMap)).isEqualTo("${episode.title[2]}");
    assertThat(morphTemplate("${episode.title[2]}", tokenMap)).isEqualTo("${episode.title[2]}");
    assertThat(morphTemplate("${if title}${title[2]}${end}", tokenMap)).isEqualTo("${if episode.title}${episode.title[2]}${end}");
    assertThat(morphTemplate("${foreach audioCodecList codec}${codec}${end}", tokenMap))
        .isEqualTo("${foreach episode.mediaInfoAudioCodecList codec}${codec}${end}");
    assertThat(morphTemplate("${title;lower}", tokenMap)).isEqualTo("${episode.title;lower}");
  }
}
