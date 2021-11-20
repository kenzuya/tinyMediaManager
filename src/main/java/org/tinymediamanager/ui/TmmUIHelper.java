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
package org.tinymediamanager.ui;

import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.TmmOsUtils;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.thirdparty.TinyFileDialogs;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.dialogs.ImagePreviewDialog;
import org.tinymediamanager.ui.dialogs.RegexpInputDialog;
import org.tinymediamanager.ui.dialogs.UpdateDialog;
import org.tinymediamanager.ui.plaf.dark.TmmDarkLaf;
import org.tinymediamanager.ui.plaf.light.TmmLightLaf;
import org.tinymediamanager.updater.UpdateCheck;
import org.tinymediamanager.updater.UpdaterTask;

/**
 * The Class TmmUIHelper.
 * 
 * @author Manuel Laggner
 */
public class TmmUIHelper {
  private static final Logger           LOGGER = LoggerFactory.getLogger(TmmUIHelper.class);
  protected static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");

  private TmmUIHelper() {
    // private constructor for utility classes
  }

  public static Path selectDirectory(String title, String initialPath) {
    // are we forced to open the legacy file chooser?
    if ("true".equalsIgnoreCase(System.getProperty("tmm.legacy.filechooser"))) {
      return openJFileChooser(JFileChooser.DIRECTORIES_ONLY, title, initialPath, true, null, null);
    }

    // on macOS/OSX we simply use the AWT FileDialog
    if (SystemUtils.IS_OS_MAC) {
      try {
        // open directory chooser
        return openDirectoryDialog(title, initialPath);
      }
      catch (Exception | Error e) {
        LOGGER.warn("cannot open AWT directory chooser: {}", e.getMessage());
      }
      finally {
        // reset system property
        System.setProperty("apple.awt.fileDialogForDirectories", "false");
      }
    }
    else {
      // try to open with tinyfiledialogs
      try {
        // check if the initialPath is accessible
        if (StringUtils.isBlank(initialPath) || !Files.exists(Paths.get(initialPath))) {
          initialPath = System.getProperty("user.home");
        }
        return new TinyFileDialogs().chooseDirectory(title, Paths.get(initialPath));
      }
      catch (Exception | Error e) {
        LOGGER.error("could not call TinyFileDialogs - {}", e.getMessage());
      }
    }

    // open JFileChooser
    return openJFileChooser(JFileChooser.DIRECTORIES_ONLY, title, initialPath, true, null, null);
  }

  private static Path openDirectoryDialog(String title, String initialPath) throws Exception, Error {
    // set system property to choose directories
    System.setProperty("apple.awt.fileDialogForDirectories", "true");

    FileDialog chooser = new FileDialog(MainWindow.getFrame(), title);
    if (StringUtils.isNotBlank(initialPath)) {
      Path path = Paths.get(initialPath);
      if (Files.exists(path)) {
        chooser.setDirectory(path.toFile().getAbsolutePath());
      }
    }
    chooser.setVisible(true);

    // reset system property
    System.setProperty("apple.awt.fileDialogForDirectories", "false");

    if (StringUtils.isNotEmpty(chooser.getFile())) {
      return Paths.get(chooser.getDirectory(), chooser.getFile());
    }
    else {
      return null;
    }
  }

  private static Path openJFileChooser(int mode, String dialogTitle, String initialPath, boolean open, String filename,
      FileNameExtensionFilter filter) {
    JFileChooser fileChooser = null;

    if (StringUtils.isNotBlank(initialPath)) {
      Path path = Paths.get(initialPath);
      if (Files.exists(path)) {
        fileChooser = new JFileChooser(path.toFile());
      }
    }

    if (fileChooser == null) {
      fileChooser = new JFileChooser();
    }

    fileChooser.setFileSelectionMode(mode);
    fileChooser.setDialogTitle(dialogTitle);

    int result = -1;
    if (open) {
      result = fileChooser.showOpenDialog(MainWindow.getFrame());
    }
    else {
      if (StringUtils.isNotBlank(filename)) {
        fileChooser.setSelectedFile(new File(filename));
        fileChooser.setFileFilter(filter);
      }
      result = fileChooser.showSaveDialog(MainWindow.getFrame());
    }

    if (result == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile().toPath();
    }

    return null;
  }

  public static Path selectFile(String title, String initialPath, FileNameExtensionFilter filter) {
    // are we forced to open the legacy file chooser?
    if ("true".equalsIgnoreCase(System.getProperty("tmm.legacy.filechooser"))) {
      return openJFileChooser(JFileChooser.FILES_ONLY, title, initialPath, true, null, filter);
    }

    // on macOS/OSX we simply use the AWT FileDialog
    if (SystemUtils.IS_OS_MAC) {
      try {
        // open file chooser
        return openFileDialog(title, initialPath, FileDialog.LOAD, null);
      }
      catch (Exception | Error e) {
        LOGGER.warn("cannot open AWT filechooser: {}", e.getMessage());
      }
    }
    else {
      try {
        // check if the initialPath is accessible
        if (StringUtils.isBlank(initialPath) || !Files.exists(Paths.get(initialPath))) {
          initialPath = System.getProperty("user.home");
        }

        String[] filterList = null;
        String filterDescription = null;

        if (filter != null) {
          List<String> extensions = new ArrayList<>();
          filterDescription = filter.getDescription();
          for (String extension : filter.getExtensions()) {
            extensions.add("*" + extension);
          }
          filterList = extensions.toArray(new String[0]);
        }

        return new TinyFileDialogs().openFile(title, Paths.get(initialPath), filterList, filterDescription);
      }
      catch (Exception | Error e) {
        LOGGER.error("could not call TinyFileDialogs - {}", e.getMessage());
      }
    }

    // open JFileChooser
    return openJFileChooser(JFileChooser.FILES_ONLY, title, initialPath, true, null, filter);
  }

  public static Path selectApplication(String title, String initialPath) {
    if (SystemUtils.IS_OS_MAC) {
      return selectDirectory(title, initialPath);
    }
    else if (SystemUtils.IS_OS_WINDOWS) {
      return selectFile(title, initialPath, new FileNameExtensionFilter(TmmResourceBundle.getString("tmm.executables"), ".exe"));
    }
    else {
      return selectFile(title, initialPath, null);
    }
  }

  private static Path openFileDialog(String title, String initialPath, int mode, String filename) throws Exception, Error {
    FileDialog chooser = new FileDialog(MainWindow.getFrame(), title, mode);
    if (StringUtils.isNotBlank(initialPath)) {
      Path path = Paths.get(initialPath);
      if (Files.exists(path)) {
        chooser.setDirectory(path.toFile().getAbsolutePath());
      }
    }
    if (mode == FileDialog.SAVE) {
      chooser.setFile(filename);
    }
    chooser.setVisible(true);

    if (StringUtils.isNotEmpty(chooser.getFile())) {
      return Paths.get(chooser.getDirectory(), chooser.getFile());
    }
    else {
      return null;
    }
  }

  public static Path saveFile(String title, String initialPath, String filename, FileNameExtensionFilter filter) {
    // are we forced to open the legacy file chooser?
    if ("true".equalsIgnoreCase(System.getProperty("tmm.legacy.filechooser"))) {
      return openJFileChooser(JFileChooser.FILES_ONLY, title, initialPath, false, filename, filter);
    }

    // on macOS/OSX we simply use the AWT FileDialog
    if (SystemUtils.IS_OS_MAC) {
      try {
        // open file chooser
        return openFileDialog(title, initialPath, FileDialog.SAVE, filename);
      }
      catch (Exception | Error e) {
        LOGGER.warn("cannot open AWT filechooser: {}", e.getMessage());
      }
    }
    else {
      // try to open with TinyFileDialogs
      try {
        String[] filterList = null;
        String filterDescription = null;

        if (filter != null) {
          List<String> extensions = new ArrayList<>();
          filterDescription = filter.getDescription();
          for (String extension : filter.getExtensions()) {
            extensions.add("*" + extension);
          }
          filterList = extensions.toArray(new String[0]);
        }

        return new TinyFileDialogs().saveFile(title, Paths.get(initialPath, filename), filterList, filterDescription);
      }
      catch (Exception | Error e) {
        LOGGER.error("could not call TinyFileDialogs - {}", e.getMessage());
      }
    }

    return openJFileChooser(JFileChooser.FILES_ONLY, title, initialPath, false, filename, filter);
  }

  public static void openFile(Path file) throws Exception {
    String fileType = "." + FilenameUtils.getExtension(file.getFileName().toString().toLowerCase(Locale.ROOT));
    String abs = file.toAbsolutePath().toString();

    if (StringUtils.isNotBlank(Settings.getInstance().getMediaPlayer()) && Settings.getInstance().getAllSupportedFileTypes().contains(fileType)) {
      if (SystemUtils.IS_OS_MAC) {
        exec(new String[] { "open", Settings.getInstance().getMediaPlayer(), "--args", abs });
      }
      else {
        exec(new String[] { Settings.getInstance().getMediaPlayer(), abs });
      }
    }
    else if (SystemUtils.IS_OS_WINDOWS) {
      // try to open directly

      try {
        Desktop.getDesktop().open(file.toFile());
      }
      catch (Exception e) {
        // use explorer directly - ship around access exceptions and the unresolved network bug
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6780505
        exec(new String[] { "explorer", abs });
      }
    }
    else if (SystemUtils.IS_OS_LINUX) {
      // try all different starters
      boolean started = false;
      try {
        exec(new String[] { "xdg-open", abs });
        started = true;
      }
      catch (IOException ignored) {
        // nothing to do here
      }

      if (!started) {
        try {
          exec(new String[] { "kde-open", abs });
          started = true;
        }
        catch (IOException ignored) {
          // nothing to do here
        }
      }

      if (!started) {
        try {
          exec(new String[] { "gnome-open", abs });
          started = true;
        }
        catch (IOException ignored) {
          // nothing to do here
        }
      }

      if (!started && Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(file.toFile());
      }
    }
    else if (Desktop.isDesktopSupported()) {
      Desktop.getDesktop().open(file.toFile());

    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * browse to the url
   *
   * @param url
   *          the url to browse
   * @throws Exception
   *           any exception occurred
   */
  public static void browseUrl(String url) throws Exception {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop().browse(new URI(url));
    }
    else if (SystemUtils.IS_OS_LINUX) {
      // try all different starters
      boolean started = false;
      try {
        exec(new String[] { "gnome-open", url });
        started = true;
      }
      catch (IOException ignored) {
        // no exception handling needed
      }

      if (!started) {
        try {
          exec(new String[] { "kde-open", url });
          started = true;
        }
        catch (IOException ignored) {
          // no exception handling needed
        }
      }

      if (!started) {
        try {
          exec(new String[] { "kde-open5", url });
          started = true;
        }
        catch (IOException ignored) {
          // no exception handling needed
        }
      }

      if (!started) {
        try {
          exec(new String[] { "xdg-open", url });
          started = true;
        }
        catch (IOException ignored) {
          // no exception handling needed
        }
      }
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * browse to the url without throwing any exception
   *
   * @param url
   *          the url to browse
   */
  public static void browseUrlSilently(String url) {
    try {
      browseUrl(url);
    }
    catch (Exception e) {
      LOGGER.error("could not open url '{}' - {}", url, e.getMessage());
      MessageManager.instance
          .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  /**
   * Executes a command line and discards the stdout and stderr of the spawned process.
   *
   * @param cmdline
   *          the command including all parameters
   * @throws IOException
   * @see {@link Runtime#exec(String[])}
   */
  private static void exec(String[] cmdline) throws IOException {
    Process p = Runtime.getRuntime().exec(cmdline);

    // The purpose of the following to threads is to read stdout and stderr from the processes, which are spawned in this class.
    // On some platforms (for sure on Linux) the process might block otherwise, because the internal buffers fill up.
    // MPV for example is quite verbose and blocks up after about 1 min.
    StreamRedirectThread stdoutReader = new StreamRedirectThread(p.getInputStream(), new NirvanaOutputStream());
    StreamRedirectThread stderrReader = new StreamRedirectThread(p.getErrorStream(), new NirvanaOutputStream());
    new Thread(stdoutReader).start();
    new Thread(stderrReader).start();
  }

  /**
   * This OutputStream discards all bytes written to it.
   */
  private static class NirvanaOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {
    }
  }

  /**
   * Reads from an InputStream and writes the contents directly to an OutputStream
   */
  public static class StreamRedirectThread implements Runnable {
    private final InputStream  in;
    private final OutputStream out;

    public StreamRedirectThread(InputStream in, OutputStream out) {
      super();
      this.in = in;
      this.out = out;
    }

    @Override
    public void run() {
      try {
        int length = -1;
        byte[] buffer = new byte[1024 * 1024];
        while (in != null && (length = in.read(buffer)) >= 0) {
          out.write(buffer, 0, length);
        }
      }
      catch (Exception e) {
        LOGGER.error("Couldn't redirect stream: {}", e.getLocalizedMessage());
      }
    }
  }

  public static LinkLabel createLinkForImage(LinkLabel linklabel, ImageLabel image) {
    linklabel.addActionListener(e -> {
      if (StringUtils.isNotBlank(image.getImagePath())) {
        ImagePreviewDialog dialog = new ImagePreviewDialog(Paths.get(image.getImagePath()));
        dialog.setVisible(true);
      }
      else {
        ImagePreviewDialog dialog = new ImagePreviewDialog(image.getImageUrl());
        dialog.setVisible(true);
      }
    });

    return linklabel;
  }

  /**
   * Given an ID, the method returns the web detail page of movie/show.
   * 
   * @param me
   *          the MediaEntity (movie/show) for generating correct links
   * @param id
   *          the ID to generate the url for (IMDB, TMDB, ...)
   * @return the https url for GUI, or empty
   */
  public static String createUrlForID(MediaEntity me, String id) {
    String url = "";
    String value = me.getIdAsString(id);
    if (value.isEmpty()) {
      return "";
    }

    // same url, regardless if movie or tv
    switch (id) {
      case Constants.IMDB:
        url = "https://www.imdb.com/title/" + value;
        break;

      case "anidb":
        url = "https://anidb.net/anime/" + value;
        break;

      case "moviemeter":
        url = "https://www.moviemeter.nl/film/" + value;
        break;

      case "mpdbtv":
        url = "https://mpdb.tv/movie/en_us/" + value;
        break;

      case "ofdb":
        url = "https://ssl.ofdb.de/film/" + value + "," + me.getTitle();
        break;

      case "omdbapi":
        url = "https://www.omdb.org/movie/" + value + "-" + me.getTitle();
        break;

      case "tvmaze":
        url = "https://www.tvmaze.com/shows/" + value;
        break;

      default:
        break;
    }

    if (me instanceof Movie) {
      switch (id) {
        case Constants.TRAKT:
          url = "https://trakt.tv/movies/" + value;
          break;

        case Constants.TMDB:
          url = "https://www.themoviedb.org/movie/" + value;
          break;

        case Constants.TVDB:
          url = "https://thetvdb.com/dereferrer/movie/" + value;
          break;

        default:
          break;
      }
    }
    else if (me instanceof TvShow || me instanceof TvShowEpisode) {
      switch (id) {
        case Constants.TRAKT:
          url = "https://trakt.tv/shows/" + value;
          break;

        case Constants.TMDB:
          url = "https://www.themoviedb.org/tv/" + value;
          break;

        case Constants.TVDB:
          url = "https://thetvdb.com/dereferrer/series/" + value;
          break;

        default:
          break;
      }
    }

    return url;
  }

  /**
   * Update UI of all application windows immediately. Invoke after changing anything in the LaF.
   */
  public static void updateUI() {
    // update all visible components
    for (Window w : Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(w);
    }

    // update icons
    IconManager.updateIcons();
  }

  public static void setTheme() throws Exception {

    switch (Settings.getInstance().getTheme()) {
      case "Dark":
        UIManager.setLookAndFeel(new TmmDarkLaf());
        break;

      case "Light":
      default:
        UIManager.setLookAndFeel(new TmmLightLaf());
        break;
    }
  }

  public static void checkForUpdate(int delayInSeconds, boolean showNoUpdateFoundMessage) {
    Runnable runnable = () -> {
      try {
        UpdateCheck updateCheck = new UpdateCheck();
        if (updateCheck.isUpdateAvailable()) {
          LOGGER.info("update available");

          // we might need this somewhen...
          if (updateCheck.isForcedUpdate()) {
            LOGGER.info("Updating (forced)...");
            // start the updater task
            TmmTaskManager.getInstance().addDownloadTask(new UpdaterTask());
            return;
          }

          // show whatsnewdialog with the option to update
          SwingUtilities.invokeLater(() -> {
            if (StringUtils.isNotBlank(updateCheck.getChangelog())) {
              UpdateDialog dialog = new UpdateDialog(updateCheck.getChangelog());
              dialog.setVisible(true);
            }
            else {
              // do the update without changelog popup
              Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
              int answer = JOptionPane.showOptionDialog(null, TmmResourceBundle.getString("tmm.update.message"),
                  TmmResourceBundle.getString("tmm.update.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
              if (answer == JOptionPane.YES_OPTION) {
                LOGGER.info("Updating...");

                // start the updater task
                TmmTaskManager.getInstance().addDownloadTask(new UpdaterTask());
              }
            }
          });
        }
        else {
          // no update found
          if (showNoUpdateFoundMessage) {
            JOptionPane.showMessageDialog(null, TmmResourceBundle.getString("tmm.update.notfound"), TmmResourceBundle.getString("tmm.update.title"),
                JOptionPane.INFORMATION_MESSAGE);
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("Update check failed - {}", e.getMessage());
      }
    };

    if (delayInSeconds > 0) {
      // update task start a few secs after GUI...
      Timer timer = new Timer(delayInSeconds * 1000, e -> runnable.run());
      timer.setRepeats(false);
      timer.start();
    }
    else {
      runnable.run();
    }
  }

  public static void restartWarningAfterV4Upgrade() {
    Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
    int confirm = JOptionPane.showOptionDialog(null, TmmResourceBundle.getString("tmm.upgrade.finished.desc"),
        TmmResourceBundle.getString("tmm.upgrade.finished"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
    if (confirm == JOptionPane.YES_OPTION) {
      MainWindow.getInstance().closeTmmAndStart(TmmOsUtils.getPBforTMMrestart());
    }
  }

  public static String showRegexpInputDialog(Window parent) {
    RegexpInputDialog inputDialog = new RegexpInputDialog(parent);
    inputDialog.setVisible(true);
    return inputDialog.getRegularExpression();
  }
}
