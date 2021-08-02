package org.tinymediamanager.core.tvshow;

import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.TmmModuleManager;

public class TvShowExportTest extends BasicTest {

  @BeforeClass
  public static void init() throws Exception {
    BasicTest.setup();

    TmmModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();

    createFakeShow("ExportShow");
    createFakeShow("ExportShow 2");
  }

  @AfterClass
  public static void shutdown() throws Exception {
    TvShowModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void testList() throws Exception {
    TvShowList list = TvShowModuleManager.getInstance().getTvShowList();

    TvShowExporter exporter = new TvShowExporter(Paths.get("templates", "TvShowDetailExampleXml"));
    exporter.export(list.getTvShows(), Paths.get(getSettingsFolder(), "TvShowDetailExampleXml"));
  }
}
