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
package org.tinymediamanager.scraper.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang3.StringUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;

public class RatingUtil {
  private static final Logger          LOGGER  = LoggerFactory.getLogger(RatingUtil.class);
  private static final String          IMDB_DB = "imdb_ratings.db";

  private static MVStore               mvStore;
  private static MVMap<String, String> ratingMap;

  private RatingUtil() {
    throw new IllegalAccessError();
  }

  public static synchronized MediaRating getImdbRating(String imdbId) {
    if (!MetadataUtil.isValidImdbId(imdbId)) {
      return null;
    }

    if (mvStore == null) {
      // no rating here yet
      initImdbRatings();
    }

    String line = ratingMap.get(imdbId);
    if (StringUtils.isNotBlank(line)) {
      String[] cols = line.split("\t");
      if (cols.length > 2 && MetadataUtil.isValidImdbId(cols[0])) {
        try {
          return new MediaRating(MediaMetadata.IMDB, Float.parseFloat(cols[1]), Integer.parseInt(cols[2]));
        }
        catch (Exception ignored) {
          // ignored
        }
      }
    }

    return null;
  }

  private static void initImdbRatings() {
    Path databaseFile = Paths.get(Globals.CACHE_FOLDER, IMDB_DB);
    try {
      mvStore = new MVStore.Builder().fileName(databaseFile.toString()).compressHigh().autoCommitDisabled().open();
      ratingMap = mvStore.openMap("ratings");

      Url cachedUrl = new OnDiskCachedUrl("https://datasets.imdbws.com/title.ratings.tsv.gz", 1, TimeUnit.DAYS);

      try (InputStream httpInputStream = cachedUrl.getInputStream()) {
        // performance hack: even if we re-zip the same file we get a different file size
        // so, we do store the file size in the map and only if the file size changes, we refill the map
        if (!String.valueOf(cachedUrl.getContentLength()).equals(ratingMap.get("length"))) {
          try (GzipCompressorInputStream in = new GzipCompressorInputStream(httpInputStream)) {
            ratingMap.clear();
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // read the file line by line
            String[] lines = content.split("\n");

            for (String line : lines) {
              try {
                String[] cols = line.split("\t");
                if (cols.length > 2 && MetadataUtil.isValidImdbId(cols[0])) {
                  ratingMap.put(cols[0], line);
                }
              }
              catch (Exception e) {
                // just ignore
                LOGGER.debug("could not store rating - {}", e.getMessage());
              }
            }
          }
          ratingMap.put("length", String.valueOf(cachedUrl.getContentLength()));
          mvStore.commit();
        }
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      // look if the file is locked by another process (rethrow rather than delete the db file)
      LOGGER.warn("could not create IMDB ratings database - {}", e.getMessage());
    }
  }

  public synchronized static void shutdown() {
    if (mvStore != null) {
      mvStore.compactMoveChunks();
      mvStore.close();
    }
  }
}
