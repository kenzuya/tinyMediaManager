/*
 * Copyright 2012 - 2024 Manuel Laggner
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

package org.tinymediamanager.scraper.opensubtitles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.opensubtitles.model.Info;

public class ITOpensubtitlesMetadataProviderTest extends BasicITest {
  private static final String SERVICE = "http://api.opensubtitles.org/xml-rpc";

  @Test
  public void testSearchByMovieHash() {
    try {
      OpenSubtitlesSubtitleProvider os = new OpenSubtitlesMovieSubtitleProvider();

      File parentDir = new File("");
      File[] subDirs = parentDir.listFiles();

      for (File subDir : subDirs) {
        File[] movieDirs = subDir.listFiles();
        for (File movieDir : movieDirs) {
          File[] files = movieDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              if (name.endsWith(".avi") || name.endsWith(".mkv") || name.endsWith(".mp4")) {
                return true;
              }
              return false;
            }
          });
          if (files == null) {
            continue;
          }

          for (File file : files) {
            SubtitleSearchAndScrapeOptions options = new SubtitleSearchAndScrapeOptions(MediaType.MOVIE);
            MediaFile mediaFile = new MediaFile(file.toPath());
            mediaFile.gatherMediaInformation();

            options.setMediaFile(mediaFile);
            options.setLanguage(MediaLanguages.de);
            List<SubtitleSearchResult> results = os.search(options);
            if (!results.isEmpty()) {
              System.out.println("Subtitle for hash found: " + results.get(0).getUrl());
            }
          }
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSearchByTitle() {
    try {
      OpenSubtitlesSubtitleProvider mp = new OpenSubtitlesMovieSubtitleProvider();
      SubtitleSearchAndScrapeOptions options = new SubtitleSearchAndScrapeOptions(MediaType.MOVIE);
      options.setSearchQuery("The Matrix");
      options.setLanguage(MediaLanguages.de);
      List<SubtitleSearchResult> results = mp.search(options);
      assertThat(results).isNotEmpty();
      assertThat(results.size()).isGreaterThanOrEqualTo(11);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testLogin() throws Exception {
    try {
      TmmXmlRpcClient client = new TmmXmlRpcClient(new URL(SERVICE));

      client.setUserAgent(new OpenSubtitlesMovieSubtitleProvider().getApiKey());

      Map<String, Object> result = (Map<String, Object>) client.call("LogIn",
          new Object[] { "", "", "", new OpenSubtitlesMovieSubtitleProvider().getApiKey() });
      assertThat(result).isNotEmpty();
      assertThat((String) result.get("token")).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSearchMovieSubtitles() {
    try {
      // login
      TmmXmlRpcClient client = new TmmXmlRpcClient(new URL(SERVICE));

      client.setUserAgent(new OpenSubtitlesMovieSubtitleProvider().getApiKey());

      Map<String, Object> result = (Map<String, Object>) client.call("LogIn",
          new Object[] { "", "", "", new OpenSubtitlesMovieSubtitleProvider().getApiKey() });
      assertThat(result).isNotEmpty();
      assertThat((String) result.get("token")).isNotEmpty();
      String token = (String) result.get("token");

      // search with query
      Map<String, Object> mapQuery = new HashMap<>();
      mapQuery.put("query", "The Matrix");
      mapQuery.put("sublanguageid", "ger");

      Object[] arrayQuery = new Object[] { mapQuery };
      result = (Map<String, Object>) client.call("SearchSubtitles", new Object[] { token, arrayQuery });
      assertThat(result).isNotEmpty();
      Info info = new Info(result);
      assertThat(info.getSeconds()).isGreaterThan(0);
      assertThat(info.getStatus()).isNotEmpty();
      assertThat(info.getMovieInfo()).isNotEmpty();
      assertThat(info.getMovieInfo().get(0).movieKind).isEqualTo("movie");
      assertThat(info.getMovieInfo().get(0).subFormat).isNotEmpty();
      assertThat(info.getMovieInfo().get(0).subDownloadLink).isNotEmpty();

      // search with hash
      mapQuery = new HashMap<>();
      mapQuery.put("moviebytesize", String.valueOf(1478618924));
      mapQuery.put("moviehash", "c00e58454d238c53");
      mapQuery.put("sublanguageid", "ger");

      arrayQuery = new Object[] { mapQuery };
      result = (Map<String, Object>) client.call("SearchSubtitles", new Object[] { token, arrayQuery });
      assertThat(result).isNotEmpty();
      info = new Info(result);
      assertThat(info.getSeconds()).isGreaterThan(0);
      assertThat(info.getStatus()).isNotEmpty();
      assertThat(info.getMovieInfo().size()).isEqualTo(1);
      assertThat(info.getMovieInfo().get(0).movieKind).isEqualTo("movie");
      assertThat(info.getMovieInfo().get(0).subFormat).isNotEmpty();
      assertThat(info.getMovieInfo().get(0).subDownloadLink).isNotEmpty();

      // search with query and valid movie hash -> works
      mapQuery = new HashMap<>();
      mapQuery.put("query", "X-Men Erste Entscheidung");
      mapQuery.put("moviebytesize", String.valueOf(1478618924));
      mapQuery.put("moviehash", "c00e58454d238c53");
      mapQuery.put("sublanguageid", "ger");

      arrayQuery = new Object[] { mapQuery };
      result = (Map<String, Object>) client.call("SearchSubtitles", new Object[] { token, arrayQuery });
      assertThat(result).isNotEmpty();
      info = new Info(result);
      assertThat(info.getSeconds()).isGreaterThan(0);
      assertThat(info.getStatus()).isNotEmpty();
      assertThat(info.getMovieInfo().size()).isEqualTo(1);
      assertThat(info.getMovieInfo().get(0).movieKind).isEqualTo("movie");
      assertThat(info.getMovieInfo().get(0).subFormat).isNotEmpty();
      assertThat(info.getMovieInfo().get(0).subDownloadLink).isNotEmpty();

      // search with IMDB Id
      mapQuery = new HashMap<>();
      mapQuery.put("imdbid", "0103064");
      mapQuery.put("sublanguageid", "ger");

      arrayQuery = new Object[] { mapQuery };
      result = (Map<String, Object>) client.call("SearchSubtitles", new Object[] { token, arrayQuery });
      assertThat(result).isNotEmpty();
      info = new Info(result);
      assertThat(info.getSeconds()).isGreaterThan(0);
      assertThat(info.getStatus()).isNotEmpty();
      assertThat(info.getMovieInfo().size()).isGreaterThanOrEqualTo(1);
      assertThat(info.getMovieInfo().get(0).movieKind).isEqualTo("movie");
      assertThat(info.getMovieInfo().get(0).subFormat).isNotEmpty();
      assertThat(info.getMovieInfo().get(0).subDownloadLink).isNotEmpty();

      // search with query and invalid movie hash - does not work due to OpenSubtitle.org logic (see pseudocode)
      // source:
      // if(defined($moviehash) and defined($moviebytesize)) {
      // search by $moviehash and $moviebytesize
      // } elseif (defined($tag)) {
      // search by $tag
      // } elseif (defined($imdbid)) {
      // search by $imdbid
      // } elseif (defined($query)) {
      // fulltext search by $query
      // } else {
      // empty result
      // }
      mapQuery = new HashMap<>();
      mapQuery.put("query", "The Matrix");
      mapQuery.put("moviebytesize", String.valueOf(1475618927));
      mapQuery.put("moviehash", "c00e59454d238c52");
      mapQuery.put("imdbid", "0103064");
      mapQuery.put("sublanguageid", "ger");

      arrayQuery = new Object[] { mapQuery };
      result = (Map<String, Object>) client.call("SearchSubtitles", new Object[] { token, arrayQuery });
      assertThat(result).isNotEmpty();
      info = new Info(result);
      assertThat(info.getSeconds()).isGreaterThan(0);
      assertThat(info.getStatus()).isNotEmpty();
      assertThat(info.getMovieInfo()).isEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void testSearchEpisodeSubtitles() {
    try {
      // login
      TmmXmlRpcClient client = new TmmXmlRpcClient(new URL(SERVICE));

      client.setUserAgent(new OpenSubtitlesMovieSubtitleProvider().getApiKey());

      Map<String, Object> result = (Map<String, Object>) client.call("LogIn",
          new Object[] { "", "", "", new OpenSubtitlesMovieSubtitleProvider().getApiKey() });
      assertThat(result).isNotEmpty();
      assertThat((String) result.get("token")).isNotEmpty();
      String token = (String) result.get("token");

      // search with query
      Map<String, Object> mapQuery = new HashMap<>();
      mapQuery.put("imdbid", "0944947"); // note without leading tt
      mapQuery.put("season", "1");
      mapQuery.put("episode", "1");
      mapQuery.put("sublanguageid", "ger");

      Object[] arrayQuery = new Object[] { mapQuery };
      result = (Map<String, Object>) client.call("SearchSubtitles", new Object[] { token, arrayQuery });
      assertThat(result).isNotEmpty();
      Info info = new Info(result);
      assertThat(info.getSeconds()).isGreaterThan(0);
      assertThat(info.getStatus()).isNotEmpty();
      assertThat(info.getMovieInfo()).isNotEmpty();
      assertThat(info.getMovieInfo().get(0).movieKind).isEqualTo("episode");
      assertThat(info.getMovieInfo().get(0).season).isEqualTo("1");
      assertThat(info.getMovieInfo().get(0).episode).isEqualTo("1");
      assertThat(info.getMovieInfo().get(0).subFormat).isNotEmpty();
      assertThat(info.getMovieInfo().get(0).subDownloadLink).isNotEmpty();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
}
