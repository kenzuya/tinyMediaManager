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
package org.tinymediamanager.thirdparty.yt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tinymediamanager.scraper.http.Url;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.extractor.Extractor;

/**
 * the class {@link YTExtractor} is used to extract YT data
 * 
 * @author Manuel Laggner
 */
class YTExtractor implements Extractor {
  private static final List<Pattern> YT_PLAYER_CONFIG_PATTERNS = Arrays.asList(Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;ytplayer"),
      Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;"), Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\;var meta"));
  private static final List<Pattern> YT_INITIAL_DATA_PATTERNS  = Arrays.asList(Pattern.compile("window\\[\"ytInitialData\"\\] = (\\{.*?\\});"),
      Pattern.compile("ytInitialData = (\\{.*?\\});"));

  private static final String        DEFAULT_ACCEPT_LANG       = "en-US,en;";
  private static final int           DEFAULT_RETRY_ON_FAILURE  = 3;

  private final Map<String, String>  requestProperties         = new HashMap<>();

  private int                        retryOnFailure            = DEFAULT_RETRY_ON_FAILURE;

  public YTExtractor() {
    setRequestProperty("Accept-language", DEFAULT_ACCEPT_LANG);
  }

  @Override
  public void setRequestProperty(String key, String value) {
    requestProperties.put(key, value);
  }

  @Override
  public void setRetryOnFailure(int retryOnFailure) {
    if (retryOnFailure < 0)
      throw new IllegalArgumentException("retry count should be > 0");
    this.retryOnFailure = retryOnFailure;
  }

  @Override
  public String extractYtPlayerConfig(String html) throws YoutubeException {
    for (Pattern pattern : YT_PLAYER_CONFIG_PATTERNS) {
      Matcher matcher = pattern.matcher(html);

      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    throw new YoutubeException.BadPageException("Could not parse web page");
  }

  @Override
  public String extractYtInitialData(String html) throws YoutubeException {
    for (Pattern pattern : YT_INITIAL_DATA_PATTERNS) {
      Matcher matcher = pattern.matcher(html);

      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    throw new YoutubeException.BadPageException("Could not parse web page");
  }

  @Override
  public String loadUrl(String urlAsString) throws YoutubeException {
    try {
      Url url = new Url(urlAsString);

      for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
        url.addHeader(entry.getKey(), entry.getValue());
      }

      return new String(url.getBytesWithRetry(retryOnFailure), StandardCharsets.UTF_8);
    }
    catch (Exception e) {
      throw new YoutubeException.VideoUnavailableException(e.getMessage());
    }
  }
}
