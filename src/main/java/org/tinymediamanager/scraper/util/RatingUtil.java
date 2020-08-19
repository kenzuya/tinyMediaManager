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
package org.tinymediamanager.scraper.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;

public class RatingUtil {

  private static final Map<String, MediaRating> IMDB_RATINGS = new HashMap<>();

  private RatingUtil() {
    // private constructor for utility classes
  }

  public static synchronized MediaRating getImdbRating(String imdbId) throws IOException, InterruptedException {
    if (!MetadataUtil.isValidImdbId(imdbId)) {
      return null;
    }

    if (IMDB_RATINGS.isEmpty()) {
      // no rating here yet
      OnDiskCachedUrl cachedUrl = new OnDiskCachedUrl("https://datasets.imdbws.com/title.ratings.tsv.gz", 1, TimeUnit.DAYS);

      try (InputStream httpInputStream = cachedUrl.getInputStream(); GzipCompressorInputStream in = new GzipCompressorInputStream(httpInputStream)) {
        String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        // read the file line by line
        String[] lines = content.split("\n");

        for (String line : lines) {
          try {
            String[] cols = line.split("\t");
            if (cols.length > 2 && MetadataUtil.isValidImdbId(cols[0])) {
              IMDB_RATINGS.put(cols[0], new MediaRating(MediaMetadata.IMDB, Float.parseFloat(cols[1]), Integer.parseInt(cols[2])));
            }
          }
          catch (Exception ingored) {
            // just ignore
          }
        }
      }
    }

    return IMDB_RATINGS.get(imdbId);
  }
}
