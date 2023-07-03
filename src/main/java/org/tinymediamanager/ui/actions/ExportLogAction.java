/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.AbstractFileVisitor;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.license.ZipArchiveHelper;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

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
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tmm.exportlogs.desc"));
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
        MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        writeLogsFile(file.toFile());
        TmmProperties.getInstance().putProperty("exportlogs.path", file.toAbsolutePath().toString());
        MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }
    catch (Exception ex) {
      LOGGER.error("Could not write logs.zip: {}", ex.getMessage());
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, file != null ? file.toString() : "", "message.erroropenfile",
          new String[] { ":", ex.getLocalizedMessage() }));
    }
  }

  private void writeLogsFile(File file) throws Exception {
    ZipParameters zipParameters = createZipParameters();

    try (FileOutputStream os = new FileOutputStream(file); ZipOutputStream zos = ZipArchiveHelper.getInstance().createEncryptedZipOutputStream(os)) {
      // attach logs
      List<Path> logs = Utils.listFiles(Paths.get(Globals.LOG_FOLDER));
      for (Path logFile : logs) {
        try (FileInputStream in = new FileInputStream(logFile.toFile())) {
          zipParameters.setFileNameInZip("logs/" + logFile.getFileName());

          zos.putNextEntry(zipParameters);
          IOUtils.copy(in, zos);
          zos.closeEntry();
        }
        catch (Exception e) {
          LOGGER.warn("unable to attach {} - {}", logFile, e.getMessage());
        }
      }

      // attach config files, but not DB
      File[] data = new File(Globals.DATA_FOLDER).listFiles((directory, filename) -> {
        return !filename.matches(".*\\.db$") && !filename.matches(".*\\.lic$"); // not DB and license
      });
      if (data != null) {
        for (File dataFile : data) {
          try (FileInputStream in = new FileInputStream(dataFile)) {
            zipParameters.setFileNameInZip("data/" + dataFile.getName());

            zos.putNextEntry(zipParameters);
            IOUtils.copy(in, zos);
            zos.closeEntry();
          }
          catch (Exception e) {
            LOGGER.warn("unable to attach {} - {}", dataFile.getName(), e.getMessage());
          }
        }
      }

      // attach install folder structure
      try (InputStream in = generateFileListing()) {
        zipParameters.setFileNameInZip("install.txt");

        zos.putNextEntry(zipParameters);
        IOUtils.copy(in, zos);
        zos.closeEntry();
      }
      catch (Exception e) {
        LOGGER.warn("unable to attach install.txt - {}", e.getMessage());
      }
    }
  }

  private InputStream generateFileListing() {
    List<Path> filesAndFolders = new ArrayList<>();
    Path current = Paths.get(".");

    try {
      Files.walkFileTree(current, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new AbstractFileVisitor() {
        @Override
        public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
          // skip unneeded folders
          String foldername = file.getFileName().toString();
          if ("backup".equals(foldername) || "logs".equals(foldername) || "cache".equals(foldername) || "update".equals(foldername)
              || "templates".equals(foldername) || ".git".equals(foldername) || ".idea".equals(foldername) || ".settings".equals(foldername)) {
            return FileVisitResult.SKIP_SUBTREE;
          }

          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          filesAndFolders.add(file);
          return FileVisitResult.CONTINUE;
        }
      });
    }
    catch (Exception e) {
      LOGGER.warn("could not get a file listing: {}", e.getMessage());
    }

    Collections.sort(filesAndFolders);

    StringBuilder sb = new StringBuilder();

    for (Path path : filesAndFolders) {
      sb.append(current.relativize(path));
      sb.append("\t");
      sb.append(path.toFile().length());
      sb.append("\n");
    }

    return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  private ZipParameters createZipParameters() {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setEncryptFiles(true);

    return zipParameters;
  }
}
