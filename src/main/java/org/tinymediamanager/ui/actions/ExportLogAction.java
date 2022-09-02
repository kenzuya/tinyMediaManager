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

package org.tinymediamanager.ui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.TmmUIHelper;

/**
 * the class {@link ExportLogAction} is used to prepare debugging logs
 *
 * @author Manuel Laggner
 */
public class ExportLogAction extends TmmAction {
  private static final Logger LOGGER           = LoggerFactory.getLogger(ExportLogAction.class);
  private static final long   serialVersionUID = -1578568721825387890L;

  public ExportLogAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.exportlogs"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    // open the log download window
    Path file = null;
    try {
      String path = TmmProperties.getInstance().getProperty("exportlogs.path", "");
      file = TmmUIHelper.saveFile(TmmResourceBundle.getString("BugReport.savelogs"), path, "tmm_logs.zip",
          new FileNameExtensionFilter("Zip files", ".zip"));
      if (file != null) {
        writeLogsFile(file.toFile());
        TmmProperties.getInstance().putProperty("exportlogs.path", file.toAbsolutePath().toString());
      }
    }
    catch (Exception ex) {
      LOGGER.error("Could not write logs.zip: {}", ex.getMessage());
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, file != null ? file.toString() : "", "message.erroropenfile",
          new String[] { ":", ex.getLocalizedMessage() }));
    }
  }

  private void writeLogsFile(File file) throws Exception {
    try (FileOutputStream os = new FileOutputStream(file); ZipOutputStream zos = new ZipOutputStream(os)) {

      // attach logs
      List<Path> logs = Utils.listFiles(Paths.get(Globals.LOG_FOLDER));
      if (logs != null) {
        for (Path logFile : logs) {
          try (FileInputStream in = new FileInputStream(logFile.toFile())) {
            ZipEntry ze = new ZipEntry("logs/" + logFile.getFileName());
            zos.putNextEntry(ze);
            IOUtils.copy(in, zos);
            zos.closeEntry();
          }
          catch (Exception e) {
            LOGGER.warn("unable to attach {} - {}", logFile, e.getMessage());
          }
        }
      }

      // attach config files, but not DB
      File[] data = new File(Globals.DATA_FOLDER).listFiles((directory, filename) -> {
        return !filename.matches(".*\\.db$") && !filename.contains("tmm.lic"); // not DB and license
      });
      if (data != null) {
        for (File dataFile : data) {
          try (FileInputStream in = new FileInputStream(dataFile)) {

            ZipEntry ze = new ZipEntry("data/" + dataFile.getName());
            zos.putNextEntry(ze);

            IOUtils.copy(in, zos);
            zos.closeEntry();
          }
          catch (Exception e) {
            LOGGER.warn("unable to attach {} - {}", dataFile.getName(), e.getMessage());
          }
        }
      }
    }
  }
}
