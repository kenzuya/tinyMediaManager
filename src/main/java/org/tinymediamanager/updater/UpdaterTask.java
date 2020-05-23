/*
 * Copyright 2012 - 2019 Manuel Laggner
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
package org.tinymediamanager.updater;

import static org.tinymediamanager.updater.getdown.TmmGetdownApplication.UPDATE_FOLDER;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinymediamanager.TmmOsUtils;
import org.tinymediamanager.core.UTF8Control;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.updater.getdown.TmmGetdownApplication;
import org.tinymediamanager.updater.getdown.TmmGetdownDownloader;

import com.threerings.getdown.data.Application;
import com.threerings.getdown.data.EnvConfig;
import com.threerings.getdown.data.Resource;
import com.threerings.getdown.net.Downloader;
import com.threerings.getdown.util.ProgressObserver;

/**
 * UpdaterTasks checks if there's a new update for TMM
 * 
 * @author Myron Boyle
 */
public class UpdaterTask extends TmmTask {
  private static final Logger         LOGGER = LoggerFactory.getLogger(UpdaterTask.class);
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages", new UTF8Control());

  public UpdaterTask() {
    super(BUNDLE.getString("task.updater.prepare"), 100, TaskType.BACKGROUND_TASK);
  }

  @Override
  public void doInBackground() {
    try {
      List<EnvConfig.Note> notes = new ArrayList<>();

      String[] args = { "." };
      EnvConfig envc = EnvConfig.create(args, notes);
      Application app = new TmmGetdownApplication(envc);

      // reset logger from jul to slf4j
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();

      // first parse our application deployment file
      try {
        app.init(true);
      }
      catch (IOException ioe) {
        // no worries - if getdown.txt is missing the following code recovers by re-downloading it
        app.attemptRecovery(e -> {
        });
        // and re-initalize
        app.init(true);

      }

      ProgressObserver progobs = this::publishState;

      // we will clean the checksum update folder at startup (this will force to re-download partially downloaded files)
      Utils.deleteDirectoryRecursive(Paths.get(UPDATE_FOLDER));
      Path updateFolder = Paths.get(UPDATE_FOLDER);
      updateFolder.toFile().mkdirs();

      // we create this tracking counter here so that we properly note the first time through
      // the update process whether we previously had validated resources (which means this
      // is not a first time install); we may, in the course of updating, wipe out our
      // validation markers and revalidate which would make us think we were doing a fresh
      // install if we didn't specifically remember that we had validated resources the first
      // time through
      int[] alreadyValid = new int[1];

      // we'll keep track of all the resources we unpack
      Set<Resource> unpacked = new HashSet<>();

      Set<Resource> toDownload = new HashSet<>();
      Set<Resource> toInstallResources = new HashSet<>();
      app.verifyMetadata(e -> {
      });
      app.verifyResources(progobs, alreadyValid, unpacked, toInstallResources, toDownload);

      if (!toDownload.isEmpty()) {
        setTaskName(BUNDLE.getString("task.update"));
        // we have resources to download, also note them as to-be-installed
        toInstallResources.addAll(toDownload);

        // redownload any that are corrupt or invalid...
        LOGGER.info("{} of {} files require update ({} assumed valid).", toDownload.size(), app.getAllActiveResources().size(), alreadyValid[0]);

        download(toDownload, app);

        // and prepare for installation
        for (Resource resource : toInstallResources) {
          resource.install(true);
        }

        // all files downloaded -> popup to inform the user (if we're in a UI environment)
        if (!GraphicsEnvironment.isHeadless()) {
          SwingUtilities.invokeLater(() -> {
            int decision = JOptionPane.showConfirmDialog(MainWindow.getActiveInstance(), BUNDLE.getString("tmm.updater.restart.desc"),
                BUNDLE.getString("tmm.updater.restart"), JOptionPane.YES_NO_OPTION);
            if (decision == JOptionPane.YES_OPTION) {
              MainWindow.getActiveInstance().closeTmmAndStart(TmmOsUtils.getPBforTMMrestart());
            }
          });
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("could not download update: {}", e.getMessage());
    }
  }

  /**
   * Called if the application is determined to require resource downloads.
   */
  private void download(Collection<Resource> resources, Application app) throws IOException {
    Downloader dl = new TmmGetdownDownloader() {
      @Override
      protected void downloadProgress(int percent, long remaining) {
        super.downloadProgress(percent, remaining);
        publishState(percent);
      }
    };

    if (dl.download(resources, app.maxConcurrentDownloads())) {
      // download completed; mark the completed download by putting a .ready file into the /update folder
      Files.createFile(Paths.get(UPDATE_FOLDER, ".ready"));
    }
  }
}
