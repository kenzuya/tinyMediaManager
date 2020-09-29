/*
 * Copyright 2012 - 2020 Manuel Laggner
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
package org.tinymediamanager.core.movie.tasks;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tasks.DownloadTask;

/**
 * This class handles the download and additional unpacking of a subtitle
 * 
 * @author Manuel Laggner
 */
public class MovieSubtitleDownloadTask extends DownloadTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieSubtitleDownloadTask.class);

  private final Movie         movie;
  private final String        languageTag;
  private final Path          videoFilePath;

  public MovieSubtitleDownloadTask(String url, Path videoFilePath, String languageTag, Movie movie) {
    super(url, movie.getPathNIO().resolve(FilenameUtils.getBaseName(videoFilePath.getFileName().toString()) + "." + languageTag));
    this.movie = movie;
    this.languageTag = languageTag;
    this.videoFilePath = videoFilePath;
  }

  @Override
  protected void doInBackground() {
    // let the DownloadTask handle the whole download
    super.doInBackground();

    MediaFile mf = new MediaFile(file);
    Path old = mf.getFileAsPath();

    if (mf.getType() != MediaFileType.SUBTITLE) {
      // try to decompress
      try (FileInputStream fis = new FileInputStream(file.toFile()); ZipInputStream is = new ZipInputStream(fis)) {

        // get the zipped file list entry
        ZipEntry ze = is.getNextEntry();

        // we prefer well known subtitle file formats, but also remember .txt files
        SubtitleEntry firstSubtitle = null;
        SubtitleEntry firstTxt = null;

        while (ze != null) {
          String extension = FilenameUtils.getExtension(ze.getName()).toLowerCase(Locale.ROOT);

          // check is that is a valid file type
          if (Globals.settings.getSubtitleFileType().contains("." + extension) || "idx".equals(extension)) {
            firstSubtitle = new SubtitleEntry(extension, is.readAllBytes());
          }

          if (firstTxt == null && "txt".equals(extension)) {
            firstTxt = new SubtitleEntry(extension, is.readAllBytes());
          }

          ze = is.getNextEntry();
        }

        if (firstSubtitle != null) {
          mf = copySubtitleFile(firstSubtitle);
        }
        else if (firstTxt != null) {
          mf = copySubtitleFile(firstTxt);
        }

        is.closeEntry();
      }
      catch (Exception e) {
        LOGGER.debug("could not extract subtitle: {}", e.getMessage());
      }
      finally {
        Utils.deleteFileSafely(file);
      }
    }
    if (!old.equals(mf.getFileAsPath())) {
      // if it not the same (zip vs sub) - delete ZIP
      Utils.deleteFileSafely(old);
    }

    mf.gatherMediaInformation();
    movie.removeFromMediaFiles(mf); // remove old (possibly same) file
    movie.addToMediaFiles(mf); // add file, but maybe with other MI values
    movie.saveToDb();
  }

  private MediaFile copySubtitleFile(SubtitleEntry entry) throws IOException {
    String basename = FilenameUtils.getBaseName(videoFilePath.toString()) + "." + languageTag;

    String extension;
    if ("txt".equals(entry.extension)) {
      extension = "srt";
    }
    else {
      extension = entry.extension;
    }

    Path destination = file.getParent().resolve(basename + "." + extension);
    try (FileOutputStream os = new FileOutputStream(destination.toFile())) {
      IOUtils.write(entry.buffer, os);
      return new MediaFile(destination);
    }
  }

  private static class SubtitleEntry {
    private final String extension;
    private final byte[] buffer;

    public SubtitleEntry(String extenstion, byte[] buffer) {
      this.extension = extenstion;
      this.buffer = buffer;
    }
  }
}
