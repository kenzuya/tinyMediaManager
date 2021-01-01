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

package org.tinymediamanager;

import static org.tinymediamanager.ui.TmmUIHelper.checkForUpdate;
import static org.tinymediamanager.ui.TmmUIHelper.restartWarningAfterV4Upgrade;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.ELProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.cli.TinyMediaManagerCLI;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.mediainfo.MediaInfoUtils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.tasks.MovieUpdateDatasourceTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.tasks.TvShowUpdateDatasourceTask;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.thirdparty.upnp.Upnp;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmTaskbar;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.TmmUILogCollector;
import org.tinymediamanager.ui.dialogs.MessageDialog;
import org.tinymediamanager.ui.dialogs.WhatsNewDialog;
import org.tinymediamanager.ui.images.LogoCircle;
import org.tinymediamanager.ui.wizard.TinyMediaManagerWizard;

import com.sun.jna.Platform;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;

/**
 * The Class TinyMediaManager.
 * 
 * @author Manuel Laggner
 */
public class TinyMediaManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(TinyMediaManager.class);

  /**
   * The main method.
   * 
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {
    // should we change the log level for the console?
    setConsoleLogLevel();

    // simple parse command line
    if (args != null && args.length > 0) {
      LOGGER.debug("TMM started with: {}", Arrays.toString(args));
      if (!TinyMediaManagerCLI.checkArgs(args)) {
        shutdownLogger();
        System.exit(0);
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
        System.exit(0);
      }
    }

    // check if we have write permissions to this folder
    try {
      RandomAccessFile f = new RandomAccessFile("access.test", "rw");
      f.close();
      Files.deleteIfExists(Paths.get("access.test"));
    }
    catch (Exception e2) {
      String msg = "Cannot write to TMM directory, have no rights - exiting.";
      if (!GraphicsEnvironment.isHeadless()) {
        JOptionPane.showMessageDialog(null, msg);
      }
      else {
        System.out.println(msg); // NOSONAR
      }
      shutdownLogger();
      System.exit(1);
    }

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

    LOGGER.info("=====================================================");
    LOGGER.info("=== tinyMediaManager (c) 2012-2020 Manuel Laggner ===");
    LOGGER.info("=====================================================");
    LOGGER.info("tmm.version      : {}", ReleaseInfo.getRealVersion());
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
    debugCharacterEncoding("current encoding : ");

    // set GUI default language
    Locale.setDefault(Utils.getLocaleFromLanguage(Globals.settings.getLanguage()));
    LOGGER.info("System language  : {}_{}", System.getProperty("user.language"), System.getProperty("user.country"));
    LOGGER.info("GUI language     : {}_{}", Locale.getDefault().getLanguage(), Locale.getDefault().getCountry());
    LOGGER.info("Scraper language : {}", MovieModuleManager.SETTINGS.getScraperLanguage());
    LOGGER.info("TV Scraper lang  : {}", TvShowModuleManager.SETTINGS.getScraperLanguage());

    // start EDT
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        boolean newVersion = !Globals.settings.isCurrentVersion(); // same snapshots/git considered as "new", for upgrades
        try {
          Thread.setDefaultUncaughtExceptionHandler(new Log4jBackstop());
          if (!GraphicsEnvironment.isHeadless()) {
            Thread.currentThread().setName("main");
            TmmTaskbar.setImage(new LogoCircle().getImage());
          }
          else {
            Thread.currentThread().setName("headless");
            LOGGER.debug("starting without GUI...");
          }
          Toolkit tk = Toolkit.getDefaultToolkit();
          tk.addAWTEventListener(TmmUILayoutStore.getInstance(), AWTEvent.WINDOW_EVENT_MASK);
          if (!GraphicsEnvironment.isHeadless()) {
            setLookAndFeel();
          }
          doStartupTasks();

          // suppress logging messages from betterbeansbinding
          org.jdesktop.beansbinding.util.logging.Logger.getLogger(ELProperty.class.getName()).setLevel(java.util.logging.Level.SEVERE);

          // init ui logger
          TmmUILogCollector.init();

          LOGGER.info("=====================================================");
          // init splash
          SplashScreen splash = null;
          if (!GraphicsEnvironment.isHeadless()) {
            splash = SplashScreen.getSplashScreen();
          }
          Graphics2D g2 = null;
          if (splash != null) {
            g2 = splash.createGraphics();
            if (g2 != null) {
              Font font = new Font("Dialog", Font.PLAIN, 11);
              g2.setFont(font);
              g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
              g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
              g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            }
            else {
              LOGGER.debug("got no graphics from splash");
            }
          }
          else {
            LOGGER.debug("no splash found");
          }

          if (g2 != null) {
            updateProgress(g2, "starting tinyMediaManager", 0);
            splash.update();
          }
          LOGGER.info("starting tinyMediaManager");

          // upgrade check
          UpgradeTasks.setOldVersion();
          if (newVersion) {
            LOGGER.info("Upgrade from " + UpgradeTasks.getOldVersion() + " to " + ReleaseInfo.getVersion());
            if (g2 != null) {
              updateProgress(g2, "upgrading to new version", 10);
              splash.update();
            }
            UpgradeTasks.performUpgradeTasksBeforeDatabaseLoading(); // do the upgrade tasks for the old version
            Globals.settings.setCurrentVersion();
            Globals.settings.saveSettings();
          }

          // proxy settings
          if (Globals.settings.useProxy()) {
            LOGGER.info("setting proxy");
            Globals.settings.setProxy();
          }

          // MediaInfo /////////////////////////////////////////////////////
          if (g2 != null) {
            updateProgress(g2, "loading MediaInfo libs", 20);
            splash.update();
          }

          MediaInfoUtils.loadMediaInfo();

          // load modules //////////////////////////////////////////////////
          if (g2 != null) {
            updateProgress(g2, "loading movie module", 30);
            splash.update();
          }
          TmmModuleManager.getInstance().startUp();
          TmmModuleManager.getInstance().registerModule(MovieModuleManager.getInstance());
          TmmModuleManager.getInstance().enableModule(MovieModuleManager.getInstance());

          if (g2 != null) {
            updateProgress(g2, "loading TV show module", 40);
            splash.update();
          }
          TmmModuleManager.getInstance().registerModule(TvShowModuleManager.getInstance());
          TmmModuleManager.getInstance().enableModule(TvShowModuleManager.getInstance());

          if (g2 != null) {
            updateProgress(g2, "loading plugins", 50);
            splash.update();
          }
          // just instantiate static - will block (takes a few secs)
          MediaProviders.loadMediaProviders();

          if (Globals.settings.isNewConfig()) {
            // add/set default scrapers
            MovieModuleManager.SETTINGS.setDefaultScrapers();
            TvShowModuleManager.SETTINGS.setDefaultScrapers();
          }

          if (g2 != null) {
            updateProgress(g2, "starting services", 60);
            splash.update();
          }
          Upnp u = Upnp.getInstance();
          if (Globals.settings.isUpnpShareLibrary()) {
            u.startWebServer();
            u.createUpnpService();
            u.startMediaServer();
          }
          if (Globals.settings.isUpnpRemotePlay()) {
            u.createUpnpService();
            u.sendPlayerSearchRequest();
            u.startWebServer();
          }
          try {
            // TODO: async?
            KodiRPC.getInstance().connect();
          }
          catch (Exception e) {
            // catch all, to not kill JVM on any other exceptions!
            LOGGER.error(e.getMessage());
          }

          // do upgrade tasks after database loading
          if (newVersion) {
            if (g2 != null) {
              updateProgress(g2, "upgrading database to new version", 70);
              splash.update();
            }
            UpgradeTasks.performUpgradeTasksAfterDatabaseLoading();
          }

          // launch application ////////////////////////////////////////////
          if (g2 != null) {
            updateProgress(g2, "loading ui", 80);
            splash.update();
          }
          if (!GraphicsEnvironment.isHeadless()) {

            MainWindow window = MainWindow.getInstance();

            // finished ////////////////////////////////////////////////////
            if (g2 != null) {
              updateProgress(g2, "finished starting :)", 100);
              splash.update();
            }

            TmmUILayoutStore.getInstance().loadSettings(window);
            window.setVisible(true);

            // wizard for new user
            if (Globals.settings.isNewConfig()) {
              TinyMediaManagerWizard wizard = new TinyMediaManagerWizard();
              wizard.setVisible(true);
            }
            else if (!Boolean.parseBoolean(System.getProperty("tmm.noupdate"))) {
              // if the wizard is not run, check for an update
              // this has a simple reason: the wizard lets you do some settings only once: if you accept the update WHILE the wizard is showing, the
              // wizard will no more appear
              // the same goes for the scraping AFTER the wizard has been started.. in this way the update check is only being done at the next
              // startup
              checkForUpdate();
            }

            // show changelog
            if (newVersion && !ReleaseInfo.getVersion().equals(UpgradeTasks.getOldVersion())) {
              // special case nightly/git: if same snapshot version, do not display changelog
              showChangelog();
            }

            // did we just upgrade to v4?
            if (newVersion && UpgradeTasks.getOldVersion().startsWith("3")) {
              restartWarningAfterV4Upgrade();
            }

            // If auto update on start for movies data sources is enable, execute it
            if (MovieModuleManager.SETTINGS.isUpdateOnStart()) {
              TmmThreadPool task = new MovieUpdateDatasourceTask();
              TmmTaskManager.getInstance().addMainTask(task);
            }

            // If auto update on start for TV shows data sources is enable, execute it
            if (TvShowModuleManager.SETTINGS.isUpdateOnStart()) {
              TmmThreadPool task = new TvShowUpdateDatasourceTask();
              TmmTaskManager.getInstance().addMainTask(task);
            }
          }
          else {
            TinyMediaManagerCLI.start(args);
            // wait for other tmm threads (artwork download et all)
            while (TmmTaskManager.getInstance().poolRunning()) {
              try {
                Thread.sleep(2000);
              }
              catch (InterruptedException e) {
                // ignored, just shut down gracefully
              }
            }

            LOGGER.info("bye bye");
            try {
              // send shutdown signal
              TmmTaskManager.getInstance().shutdown();
              // save unsaved settings
              TmmModuleManager.getInstance().saveSettings();
              // hard kill
              TmmTaskManager.getInstance().shutdownNow();
              // close database connection
              TmmModuleManager.getInstance().shutDown();
            }
            catch (Exception ex) {
              LOGGER.warn(ex.getMessage());
            }
            shutdownLogger();
            System.exit(0);
          }
        }
        catch (IllegalStateException e) {
          LOGGER.error("IllegalStateException", e);
          if (!GraphicsEnvironment.isHeadless() && e.getMessage().contains("file is locked")) {
            ResourceBundle bundle = ResourceBundle.getBundle("messages");
            MessageDialog dialog = new MessageDialog(null, TmmResourceBundle.getString("tmm.problemdetected"));
            dialog.setImage(IconManager.ERROR);
            dialog.setText(TmmResourceBundle.getString("tmm.nostart"));
            dialog.setDescription(TmmResourceBundle.getString("tmm.nostart.instancerunning"));
            dialog.setResizable(true);
            dialog.pack();
            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
          }
          shutdownLogger();
          System.exit(1);
        }
        catch (Exception e) {
          LOGGER.error("Exception while start of tmm", e);
          if (!GraphicsEnvironment.isHeadless()) {
            MessageDialog.showExceptionWindow(e);
          }
          shutdownLogger();
          System.exit(1);
        }
      }

      /**
       * Update progress on splash screen.
       * 
       * @param text
       *          the text
       */
      private void updateProgress(Graphics2D g2, String text, int progress) {
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(50, 350, 230, 100);
        g2.setPaintMode();

        // paint text
        g2.setColor(new Color(134, 134, 134));
        g2.drawString(text + "...", 51, 390);
        int l = g2.getFontMetrics().stringWidth(ReleaseInfo.getRealVersion()); // bound right
        g2.drawString(ReleaseInfo.getRealVersion(), 277 - l, 443);

        // paint progess bar
        g2.setColor(new Color(20, 20, 20));
        g2.fillRoundRect(51, 400, 227, 6, 6, 6);

        g2.setColor(new Color(134, 134, 134));
        g2.fillRoundRect(51, 400, 227 * progress / 100, 6, 6, 6);
        LOGGER.debug("Startup (" + progress + "%) " + text);

        // Object oldAAValue = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        // g2.setComposite(AlphaComposite.Clear);
        // g2.fillRect(20, 200, 480, 305);
        // g2.setPaintMode();
        //
        // g2.setColor(new Color(51, 153, 255));
        // g2.fillRect(22, 272, 452 * progress / 100, 21);
        //
        // g2.setColor(Color.black);
        // g2.drawString(text + "...", 23, 310);
        // int l = g2.getFontMetrics().stringWidth(ReleaseInfo.getRealVersion()); // bound right
        // g2.drawString(ReleaseInfo.getRealVersion(), 480 - l, 325);
        // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAAValue);
        // LOGGER.debug("Startup (" + progress + "%) " + text);
      }

      /**
       * Does some tasks at startup
       */
      private void doStartupTasks() {
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
          File desktop = new File(TmmOsUtils.DESKTOP_FILE);
          if (!desktop.exists()) {
            TmmOsUtils.createDesktopFileForLinux(desktop);
          }
        }
      }

      private void showChangelog() {
        // read the changelog
        WhatsNewDialog.showChangelog();
      }
    });
  }

  public static void setLookAndFeel() {
    // load font settings
    try {
      // sanity check
      Font font = Font.decode(Globals.settings.getFontFamily());
      Font savedFont = new Font(font.getFamily(), font.getStyle(), Globals.settings.getFontSize());

      UIManager.put("defaultFont", savedFont);
    }
    catch (Exception e) {
      LOGGER.warn("could not set default font - {}", e.getMessage());
    }

    try {
      TmmUIHelper.setTheme();
      // decrease the tooltip timeout
      ToolTipManager.sharedInstance().setInitialDelay(300);
    }
    catch (Exception e) {
      LOGGER.error("Failed to initialize LaF - {}", e.getMessage());
    }
  }

  public static void shutdownLogger() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    // dump the traces into a file tmm_trace.zip
    Appender appender = loggerContext.getLogger("ROOT").getAppender("INMEMORY");
    if (appender instanceof InMemoryAppender) {
      File file = new File("logs/tmm_trace.zip");
      try (FileOutputStream os = new FileOutputStream(file);
          ZipOutputStream zos = new ZipOutputStream(os);
          InputStream is = new ByteArrayInputStream(((InMemoryAppender) appender).getLog().getBytes())) {

        ZipEntry ze = new ZipEntry("trace.log");
        zos.putNextEntry(ze);

        IOUtils.copy(is, zos);
        zos.closeEntry();
      }
      catch (Exception e) {
        LOGGER.warn("could not store traces log file: {}", e.getMessage());
      }
    }

    loggerContext.stop();
  }

  public static void setConsoleLogLevel() {
    String loglevelAsString = System.getProperty("tmm.consoleloglevel", "");
    Level level;

    switch (loglevelAsString) {
      case "ERROR":
        level = Level.TRACE;
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
    Appender consoleAppender = lc.getLogger("ROOT").getAppender("CONSOLE");
    if (consoleAppender instanceof ConsoleAppender) {
      // and set a filter to drop messages beneath the given level
      ThresholdLoggerFilter filter = new ThresholdLoggerFilter(level);
      filter.start();
      consoleAppender.clearAllFilters();
      consoleAppender.addFilter(filter);
    }
  }

  /**
   * debug various JVM character settings
   */
  private static void debugCharacterEncoding(String text) {
    String defaultCharacterEncoding = System.getProperty("file.encoding");
    byte[] bArray = { 'w' };
    InputStream is = new ByteArrayInputStream(bArray);
    InputStreamReader reader = new InputStreamReader(is);
    LOGGER.info(text + defaultCharacterEncoding + " | " + reader.getEncoding() + " | " + Charset.defaultCharset());
  }
}
