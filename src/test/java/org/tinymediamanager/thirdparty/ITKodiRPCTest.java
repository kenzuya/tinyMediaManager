package org.tinymediamanager.thirdparty;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.jsonrpc.api.model.VideoModel.MovieDetail;
import org.tinymediamanager.jsonrpc.config.HostConfig;
import org.tinymediamanager.jsonrpc.io.ApiException;

public class ITKodiRPCTest extends BasicTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ITKodiRPCTest.class);
  // *************************************************************************************
  // you need to enable Kodi -> remote control from OTHER machines (to open TCP port 9090)
  // and you need to enable webserver -> some calls are POSTed (not async)
  // *************************************************************************************

  @BeforeClass
  public static void setup() {
    BasicTest.setup();
  }

  @Test
  public void events() {
    KodiRPC.getInstance();
    while (true) {
      // do nothing, just wait for events...
    }
  }

  @Test
  public void getVersion() {
    System.out.println(KodiRPC.getInstance().getVersion());
  }

  @Test
  public void getMappings() {
    KodiRPC.getInstance().getAndSetMovieMappings();
  }

  @Test
  public void getAllMoviesSYNC() {
    List<MovieDetail> movies = KodiRPC.getInstance().getAllMoviesSYNC();
    if (movies == null) {
      LOGGER.error("no movies found");
    }
    else {
      LOGGER.info("found " + movies.size() + " movies");
      for (MovieDetail res : movies) {
        for (String c : res.country) {
          System.out.println(c);
        }
        LOGGER.debug(res.toString());
      }
    }
  }

  @Test
  public void getDataSources() {
    for (SplitUri ds : KodiRPC.getInstance().getVideoDataSources()) {
      System.out.println(ds);
    }

    KodiRPC.getInstance().getDataSources();
  }

  @Test
  public void getAllTvShows() {
    KodiRPC.getInstance().getAllTvShows();
  }

  @BeforeClass
  public static void setUp() {
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    // Upnp.getInstance().createUpnpService();
    // Upnp.getInstance().sendPlayerSearchRequest();
    try {
      HostConfig config = new HostConfig("127.0.0.1", 8080, "kodi", "kodi");
      KodiRPC.getInstance().connect(config);
    }
    catch (ApiException e) {
      System.err.println(e.getMessage());
      Assert.fail(e.getMessage());
    }
  }

  @AfterClass
  public static void tearDown() throws Exception {
    Thread.sleep(10000); // wait a bit - async
    KodiRPC.getInstance().disconnect();
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
    Thread.sleep(200); // wait a bit - async
  }
}
