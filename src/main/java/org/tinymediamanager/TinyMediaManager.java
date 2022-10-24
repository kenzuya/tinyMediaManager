/*
 * Copyright 2012 - 2022 Manuel Laggner
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

package org.tinymediamanager;

import static org.tinymediamanager.ui.TmmUIHelper.checkForUpdate;
import static org.tinymediamanager.ui.TmmUIHelper.restartWarningAfterV4Upgrade;
import static org.tinymediamanager.ui.TmmUIHelper.setLookAndFeel;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jdesktop.beansbinding.ELProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.cli.TinyMediaManagerCLI;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.http.TmmHttpServer;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettingsDefaults;
import org.tinymediamanager.core.movie.tasks.MovieUpdateDatasourceTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettingsDefaults;
import org.tinymediamanager.core.tvshow.tasks.TvShowUpdateDatasourceTask;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.thirdparty.upnp.Upnp;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmTaskbar;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.TmmUILogCollector;
import org.tinymediamanager.ui.dialogs.MessageDialog;
import org.tinymediamanager.ui.dialogs.TmmSplashScreen;
import org.tinymediamanager.ui.dialogs.WhatsNewDialog;
import org.tinymediamanager.ui.images.LogoCircle;
import org.tinymediamanager.ui.wizard.TinyMediaManagerWizard;

import com.sun.jna.Platform;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;

/**
 * The Class TinyMediaManager.
 * 
 * @author Manuel Laggner
 */
public final class TinyMediaManager {
  private static final Logger LOGGER       = LoggerFactory.getLogger(TinyMediaManager.class);

  private TmmSplashScreen     splashScreen = null;
  private final boolean       headless;
  private boolean             newVersion   = false;

  private TinyMediaManager() {
    headless = GraphicsEnvironment.isHeadless();
  }

  private void launch(String[] args) {
    // read the license code
    Path license = Paths.get(Globals.DATA_FOLDER, "tmm.lic");
    if (Files.exists(license)) {
      try {
        License.getInstance().setLicenseCode(Utils.readFileToString(license));
      }
      catch (Exception e) {
        LOGGER.warn("unable to decode license file - {}", e.getMessage());
      }
    }

    // load settings and set default locale
    Locale.setDefault(Utils.getLocaleFromLanguage(Settings.getInstance().getLanguage()));
    newVersion = !Settings.getInstance().isCurrentVersion(); // same snapshots/git considered as "new", for upgrades

    printLogHeader();

    if (!headless) {
      // GUI mode - start on EDT
      EventQueue.invokeLater(() -> {
        try {
          splashScreen = new TmmSplashScreen();
          splashScreen.setVersion(ReleaseInfo.getHumanVersion());
          splashScreen.setVisible(true);
        }
        catch (Exception e) {
          LOGGER.error("could not initialize splash - {}", e.getMessage());
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
          @Override
          protected Void doInBackground() {
            try {
              Thread.currentThread().setName("main");
              TmmTaskbar.setImage(new LogoCircle(512).getImage());

              Toolkit tk = Toolkit.getDefaultToolkit();
              tk.addAWTEventListener(TmmUILayoutStore.getInstance(), AWTEvent.WINDOW_EVENT_MASK);
              setLookAndFeel();

              // init ui logger
              TmmUILogCollector.init();

              // main startup
              startup();

              // launch application ////////////////////////////////////////////
              updateProgress("splash.ui", 80);

              SwingUtilities.invokeLater(() -> {
                MainWindow window = MainWindow.getInstance();

                // finished ////////////////////////////////////////////////////
                updateProgress("finished starting :)", 100);

                splashScreen.setVisible(false);

                TmmUILayoutStore.getInstance().loadSettings(window);
                window.setVisible(true);
                LOGGER.info("UI loaded");

                // wizard for new user
                if (Settings.getInstance().isNewConfig()) {
                  TinyMediaManagerWizard wizard = new TinyMediaManagerWizard();
                  wizard.setLocationRelativeTo(null); // center
                  wizard.setVisible(true);
                }
                else if (Globals.isSelfUpdateable()) {
                  // if the wizard is not run, check for an update
                  // this has a simple reason: the wizard lets you do some settings only once: if you accept the update WHILE the wizard is
                  // showing, the
                  // wizard will no more appear
                  // the same goes for the scraping AFTER the wizard has been started.. in this way the update check is only being done at the
                  // next
                  // startup
                  checkForUpdate(5);
                }

                // show changelog
                if (newVersion && !ReleaseInfo.getVersion().equals(UpgradeTasks.getOldVersion())) {
                  // special case nightly/git: if same snapshot version, do not display changelog
                  SwingUtilities.invokeLater(WhatsNewDialog::showChangelog);
                }

                // did we just upgrade to v4?
                if (newVersion && UpgradeTasks.getOldVersion().startsWith("3")) {
                  restartWarningAfterV4Upgrade();
                }

                // is the license about to running out?
                if (License.getInstance().isValidLicense()) {
                  LocalDate validUntil = License.getInstance().validUntil();
                  if (validUntil != null && validUntil.minus(7, ChronoUnit.DAYS).isBefore(LocalDate.now())) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window, TmmResourceBundle.getString("tmm.renewlicense")
                        .replace("{}", TmmDateFormat.MEDIUM_DATE_FORMAT.format(Date.valueOf(validUntil)))));
                  }
                }

                // If auto update on start for movies data sources is enable, execute it
                if (MovieModuleManager.getInstance().getSettings().isUpdateOnStart()) {
                  TmmThreadPool task = new MovieUpdateDatasourceTask();
                  TmmTaskManager.getInstance().addMainTask(task);
                }

                // If auto update on start for TV shows data sources is enable, execute it
                if (TvShowModuleManager.getInstance().getSettings().isUpdateOnStart()) {
                  TmmThreadPool task = new TvShowUpdateDatasourceTask();
                  TmmTaskManager.getInstance().addMainTask(task);
                }
              });
            }
            catch (IllegalStateException e) {
              LOGGER.error("IllegalStateException", e);
              if (e.getMessage().contains("file is locked")) {
                JOptionPane.showMessageDialog(null,
                    TmmResourceBundle.getString("tmm.nostart") + "\n" + TmmResourceBundle.getString("tmm.nostart.instancerunning"),
                    TmmResourceBundle.getString("tmm.nostart"), JOptionPane.ERROR_MESSAGE, new LogoCircle());
              }
              shutdownLogger();
              System.exit(1);
            }
            catch (Exception e) {
              LOGGER.error("Exception while start of tmm", e);
              MessageDialog.showExceptionWindow(e);
              shutdownLogger();
              System.exit(1);
            }

            return null;
          }
        };

        worker.execute();
      });
    }
    else {
      // console mode - start directly
      Thread.currentThread().setName("headless");
      LOGGER.debug("starting without GUI...");

      try {
        startup();
      }
      catch (IllegalStateException e) {
        LOGGER.error("IllegalStateException", e);
        shutdownLogger();
        System.exit(1);
      }
      catch (Exception e) {
        LOGGER.error("Exception while start of tmm", e);
        shutdownLogger();
        System.exit(1);
      }

      // should we change the log level for the console? CLI version
      setConsoleLogLevel();

      TinyMediaManagerCLI.start(args);
      // wait for other tmm threads (artwork download etall)
      while (TmmTaskManager.getInstance().poolRunning()) {
        try {
          Thread.sleep(1000);
        }
        catch (InterruptedException e) {
          // ignored
        }
      }

      LOGGER.info("bye bye");
      try {
        shutdown();
        // shutdown the logger
        shutdownLogger();
      }
      catch (Exception ex) {
        LOGGER.warn(ex.getMessage());
      }
      System.exit(0);
    }
  }

  private void printLogHeader() {
    LOGGER.info("=======================================================");
    LOGGER.info("=== tinyMediaManager (c) 2012 - 2022 Manuel Laggner ===");
    LOGGER.info("=======================================================");
    LOGGER.info("tmm.version      : {}", ReleaseInfo.getRealVersion());
    if (!ReleaseInfo.isGitBuild()) {
      LOGGER.info("tmm.build        : {}", ReleaseInfo.getRealBuildDate());
    }
    LOGGER.info("os.name          : {}", System.getProperty("os.name"));
    LOGGER.info("os.version       : {}", System.getProperty("os.version"));
    LOGGER.info("os.arch          : {}", System.getProperty("os.arch"));
    LOGGER.info("java.version     : {}", System.getProperty("java.version"));
    LOGGER.info("java.maxMem      : {} MiB", Runtime.getRuntime().maxMemory() / 1024 / 1024);

    if (Globals.isRunningJavaWebStart()) {
      LOGGER.info("java.webstart    : true");
    }
    if (Globals.isRunningWebSwing()) {
      LOGGER.info("java.webswing    : true");
    }

    // START character encoding debug
    byte[] bArray = { 'w' };
    LOGGER.info("current encoding : {} | {} | {}", System.getProperty("file.encoding"),
        new InputStreamReader(new ByteArrayInputStream(bArray)).getEncoding(), Charset.defaultCharset());

    // set GUI default language
    LOGGER.info("System language  : {}_{}", System.getProperty("user.language"), System.getProperty("user.country"));
    LOGGER.info("GUI language     : {}_{}", Locale.getDefault().getLanguage(), Locale.getDefault().getCountry());
    LOGGER.info("=====================================================");
    LOGGER.info("starting tinyMediaManager");
  }

  /**
   * main startup routine
   */
  private void startup() throws Exception {
    // pre-startup tasks
    doPreStartupTasks();

    // suppress logging messages from betterbeansbinding
    org.jdesktop.beansbinding.util.logging.Logger.getLogger(ELProperty.class.getName()).setLevel(java.util.logging.Level.SEVERE);

    // upgrade tasks
    doUpgradeTasks();

    // native libs / initialization / webserver
    loadInternals();

    // modules
    loadModules();

    // plugins
    loadPlugins();

    // services
    loadServices();

    // post-startup tasks
    doPostStartupTasks();
  }

  private void doUpgradeTasks() {
    UpgradeTasks.setOldVersion();
    if (newVersion) {
      LOGGER.info("Upgrade from '{}' to '{}'", UpgradeTasks.getOldVersion(), ReleaseInfo.getVersion());
      updateProgress("splash.upgrade", 10);
      UpgradeTasks.performUpgradeTasksBeforeDatabaseLoading(); // do the upgrade tasks for the old version
      Settings.getInstance().setCurrentVersion();
      Settings.getInstance().saveSettings();
    }
  }

  private void loadInternals() {
    updateProgress("splash.internals", 20);

    TmmOsUtils.loadNativeLibs();

    // various initializations of classes
    MediaGenres.init();
    LanguageUtils.init();

    // init http server
    if (Settings.getInstance().isEnableHttpServer()) {
      try {
        // no need for start, because after creation the server is
        // automatically started
        TmmHttpServer.getInstance();
      }
      catch (Exception e) {
        LOGGER.error("could not start webserver: {}", e.getMessage());
      }
    }
  }

  private void loadModules() throws Exception {
    // load modules //////////////////////////////////////////////////
    updateProgress("splash.movie", 30);
    TmmModuleManager.getInstance().startUp();

    // register the shutdown handler
    Runtime.getRuntime().addShutdownHook(new Thread(() -> TmmModuleManager.getInstance().shutDown()));

    TmmModuleManager.getInstance().registerModule(MovieModuleManager.getInstance());
    TmmModuleManager.getInstance().enableModule(MovieModuleManager.getInstance());

    updateProgress("splash.tvshow", 50);
    TmmModuleManager.getInstance().registerModule(TvShowModuleManager.getInstance());
    TmmModuleManager.getInstance().enableModule(TvShowModuleManager.getInstance());
  }

  private void loadPlugins() {
    updateProgress("splash.plugins", 60);
    // just instantiate static - will block (takes a few secs)
    MediaProviders.loadMediaProviders();

    if (Settings.getInstance().isNewConfig()) {
      // add/set default scrapers
      MovieSettingsDefaults.setDefaultScrapers();
      TvShowSettingsDefaults.setDefaultScrapers();
    }
  }

  private void loadServices() {
    updateProgress("splash.services", 70);
    try {
      if (Settings.getInstance().isUpnpShareLibrary()) {
        Upnp u = Upnp.getInstance();
        u.startWebServer();
        u.createUpnpService();
        u.startMediaServer();
      }
    }
    catch (Exception e) {
      LOGGER.error("Could not start UPNP - '{}'", e.getMessage());
    }
    try {
      if (Settings.getInstance().isUpnpRemotePlay()) {
        Upnp u = Upnp.getInstance();
        u.createUpnpService();
        u.sendPlayerSearchRequest();
        u.startWebServer();
      }
    }
    catch (Exception e) {
      LOGGER.error("Could not start UPNP - '{}'", e.getMessage());
    }

    try {
      KodiRPC.getInstance().connect();
    }
    catch (Exception e) {
      // catch all, to not kill JVM on any other exceptions!
      LOGGER.error(e.getMessage());
    }
  }

  private void doPreStartupTasks() {
    // rename downloaded files
    UpgradeTasks.renameDownloadedFiles();

    // clean old log files
    Utils.cleanOldLogs();

    // create a backup of the /data folder and keep last 5 copies
    Path db = Paths.get(Settings.getInstance().getSettingsFolder());
    Utils.createBackupFile(db);
    Utils.deleteOldBackupFile(db, 5);

    // check if a .desktop file exists
    if (Platform.isLinux()) {
      if (!TmmOsUtils.existsDesktopFileForLinux()) {
        Path desktopFile = Paths.get(System.getProperty("user.home"), ".local", "share", "applications", "tinyMediaManager.desktop").toAbsolutePath();
        if (Files.isWritable(desktopFile.getParent())) {
          TmmOsUtils.createDesktopFileForLinux(desktopFile.toFile());
        }
        else {
          TmmOsUtils.createDesktopFileForLinux(new File(TmmOsUtils.DESKTOP_FILE));
        }
      }
    }
  }

  private void doPostStartupTasks() {
    // do upgrade tasks after database loading
    if (newVersion) {
      updateProgress("splash.upgrade2", 80);
      UpgradeTasks.performUpgradeTasksAfterDatabaseLoading();
    }
  }

  /**
   * Update progress on splash screen.
   *
   * @param text
   *          the text
   */
  private void updateProgress(String text, int progress) {
    if (splashScreen == null) {
      return;
    }

    splashScreen.setProgress(progress, text);
  }

  /**
   * The main method.
   * 
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(new Log4jBackstop());

    // simple parse command line
    if (args != null && args.length > 0) {
      LOGGER.debug("TMM started with: {}", Arrays.toString(args));
      if (!TinyMediaManagerCLI.checkArgs(args)) {
        shutdownLogger();
        System.exit(1);
      }
      System.setProperty("java.awt.headless", "true");
    }
    else {
      // no cmd params found, but if we are headless - display syntax
      String head = System.getProperty("java.awt.headless");
      if (head != null && head.equals("true")) {
        LOGGER.info("TMM started 'headless', and without params -> displaying syntax ");
        TinyMediaManagerCLI.printHelp();
        shutdownLogger();
        System.exit(1);
      }

      // should we change the log level for the console?
      // in GUI mode we set that directly. in CLI mode we set that after startup
      setConsoleLogLevel();
    }

    TinyMediaManager tinyMediaManager = new TinyMediaManager();
    tinyMediaManager.launch(args);

  }

  public static void shutdown() {
    // send shutdown signal
    TmmTaskManager.getInstance().shutdown();
    // save unsaved settings
    TmmModuleManager.getInstance().saveSettings();
    // hard kill
    TmmTaskManager.getInstance().shutdownNow();
    // close dabatbases
    TmmModuleManager.getInstance().shutDown();
  }

  public static void shutdownLogger() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.stop();
  }

  public static void setConsoleLogLevel() {
    String loglevelAsString = System.getProperty("tmm.consoleloglevel", "");
    Level level;

    switch (loglevelAsString) {
      case "OFF":
        level = null;
        break;

      case "ERROR":
        level = Level.ERROR;
        break;

      case "WARN":
        level = Level.WARN;
        break;

      case "INFO":
        level = Level.INFO;
        break;

      case "DEBUG":
        level = Level.DEBUG;
        break;

      case "TRACE":
        level = Level.TRACE;
        break;

      default:
        return;
    }

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    // get the console appender
    Appender<ILoggingEvent> consoleAppender = lc.getLogger("ROOT").getAppender("CONSOLE");
    if (consoleAppender instanceof ConsoleAppender) {
      if (level == null) {
        consoleAppender.stop();
      }
      else {
        // and set a filter to drop messages beneath the given level
        ThresholdLoggerFilter filter = new ThresholdLoggerFilter(level);
        filter.start();
        consoleAppender.clearAllFilters();
        consoleAppender.addFilter(filter);
      }
    }
  }
}
