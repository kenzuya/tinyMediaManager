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
package org.tinymediamanager.core.movie.tasks;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tasks.DownloadTask;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
  private final String providerId;

  public MovieSubtitleDownloadTask(String url, Path videoFilePath, String languageTag, Movie movie, String providerId) {
    super(url, movie.getPathNIO().resolve(FilenameUtils.getBaseName(videoFilePath.getFileName().toString()) + "." + languageTag));
    this.movie = movie;
    this.languageTag = languageTag;
    this.videoFilePath = videoFilePath;
    this.providerId = providerId;
  }

  @Override
  protected void doInBackground() {
    // let the DownloadTask handle the whole download
    // set special UserAgent for SubDB Scraping
    if (providerId.equals("thesubdb")) {
      setSpecialUserAgent("SubDB/1.0 (TheSubDB-Scraper/0.1; http://gitlab.com/TinyMediaManager)");
    }
    super.doInBackground();

    MediaFile mf = new MediaFile(file);
    Path old = mf.getFileAsPath();

    if (mf.getType() != MediaFileType.SUBTITLE) {
      String basename = FilenameUtils.getBaseName(videoFilePath.toString()) + "." + languageTag;

      // try to decompress
      try (FileInputStream fis = new FileInputStream(file.toFile()); ZipInputStream is = new ZipInputStream(fis)) {
        byte[] buffer = new byte[1024];

        // get the zipped file list entry
        ZipEntry ze = is.getNextEntry();

        while (ze != null) {
          String zipEntryFilename = ze.getName();
          String extension = FilenameUtils.getExtension(zipEntryFilename).toLowerCase(Locale.ROOT);

          // check is that is a valid file type
          if (!Globals.settings.getSubtitleFileType().contains("." + extension) && !"idx".equals(extension)) {
            ze = is.getNextEntry();
            continue;
          }

          Path destination = file.getParent().resolve(basename + "." + extension);
          try (FileOutputStream os = new FileOutputStream(destination.toFile())) {

            int len;
            while ((len = is.read(buffer)) > 0) {
              os.write(buffer, 0, len);
            }

            mf = new MediaFile(destination);

            // only take the first subtitle
            break;
          }
        }
        is.closeEntry();
      }
      catch (Exception e) {
        LOGGER.debug("could not extract subtitle: {}", e.getMessage());
      }
    }

    //from subdb we do not get a zip file
    //so we have to rename the downloaded file to .srt format
    if (providerId.equals("thesubdb")) {
      Path newFile = file.resolveSibling(file.getFileName() + ".srt");
      boolean ok = false;
      try {
        ok = Utils.moveFileSafe(file, newFile);
      } catch (IOException e) {
        LOGGER.debug("could not rename file: {}", e.getMessage());
      }
      if (ok) {
        mf = new MediaFile(newFile);
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

}
