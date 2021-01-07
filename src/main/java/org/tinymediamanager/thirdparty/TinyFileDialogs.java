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
package org.tinymediamanager.thirdparty;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

/**
 * the class {@link TinyFileDialogs} is the wrapper to the native part of tinyfiledialogs
 * 
 * @author Manuel Laggner
 */
public class TinyFileDialogs {

  public Path chooseDirectory(String title, Path defaultPath) throws Exception {
    if (TinyFileDialogsLibrary.INSTANCE == null) {
      throw new Exception("Could not access tinyfiledialogs");
    }

    String initialPath = null;

    if (defaultPath != null) {
      initialPath = defaultPath.toAbsolutePath().toString();
      if (defaultPath.toFile().isDirectory()) {
        initialPath += File.separator;
      }
    }

    String path = TinyFileDialogsLibrary.INSTANCE.tinyfd_selectFolderDialog(title, initialPath);
    if (StringUtils.isBlank(path)) {
      return null;
    }

    return Paths.get(path);
  }

  public Path openFile(String title, Path defaultFile, String[] filterPatterns, String singleFilterDescription) throws Exception {
    if (TinyFileDialogsLibrary.INSTANCE == null) {
      throw new Exception("Could not access tinyfiledialogs");
    }

    String initialFile = null;

    if (defaultFile != null) {
      initialFile = defaultFile.toAbsolutePath().toString();
      if (defaultFile.toFile().isDirectory()) {
        initialFile += File.separator;
      }
    }

    int filterPatternCount = 0;
    if (filterPatterns != null) {
      filterPatternCount = filterPatterns.length;
    }

    String path = TinyFileDialogsLibrary.INSTANCE.tinyfd_openFileDialog(title, initialFile, filterPatternCount, filterPatterns,
        singleFilterDescription, 0);
    if (StringUtils.isBlank(path)) {
      return null;
    }

    return Paths.get(path);
  }

  public Path saveFile(String title, Path defaultFile, String[] filterPatterns, String singleFilterDescription) throws Exception {
    if (TinyFileDialogsLibrary.INSTANCE == null) {
      throw new Exception("Could not access tinyfiledialogs");
    }

    String initialFile = defaultFile != null ? defaultFile.toAbsolutePath().toString() : null;
    int filterPatternCount = 0;
    if (filterPatterns != null) {
      filterPatternCount = filterPatterns.length;
    }

    String path = TinyFileDialogsLibrary.INSTANCE.tinyfd_saveFileDialog(title, initialFile, filterPatternCount, filterPatterns,
        singleFilterDescription);
    if (StringUtils.isBlank(path)) {
      return null;
    }

    return Paths.get(path);
  }
}
