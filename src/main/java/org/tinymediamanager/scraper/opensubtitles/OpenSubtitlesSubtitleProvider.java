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

package org.tinymediamanager.scraper.opensubtitles;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.SubtitleSearchAndScrapeOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.opensubtitles.model.Info;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.Similarity;

/**
 * OpensubtitlesMetadataProvider provides subtitle scraping from OpenSubtitles.org
 *
 * @author Myron Boyle, Manuel Laggner
 */
abstract class OpenSubtitlesSubtitleProvider implements IMediaProvider {
  public static final String       ID              = "opensubtitles";

  private static final String      SERVICE         = "http://api.opensubtitles.org/xml-rpc";
  private static final int         HASH_CHUNK_SIZE = 64 * 1024;

  protected static TmmXmlRpcClient client          = null;

  private final MediaProviderInfo  providerInfo;

  private String                   sessionToken    = "";
  private String                   username        = "";
  private String                   password        = "";

  protected OpenSubtitlesSubtitleProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected abstract Logger getLogger();

  private MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "OpenSubtitles.org",
        "<html><h3>OpenSubtitles.org</h3><br />A subtitle scraper for OpenSubtitles.org</html>",
        OpenSubtitlesSubtitleProvider.class.getResource("/org/tinymediamanager/scraper/opensubtitles_org.png"));

    // configure/load settings
    info.getConfig().addText("username", "");
    info.getConfig().addText("password", "", true);

    info.getConfig().load();

    return info;
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }

  private synchronized void initAPI() throws ScrapeException {

    if (client == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }

      try {
        client = new TmmXmlRpcClient(new URL(SERVICE));
      }
      catch (MalformedURLException e) {
        getLogger().error("cannot create XmlRpcClient", e);
        throw new ScrapeException(e);
      }
    }

    // API key check
    try {
      client.setUserAgent(getApiKey());
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }

  public List<SubtitleSearchResult> search(SubtitleSearchAndScrapeOptions options) throws ScrapeException {
    // lazy initialization of the api
    initAPI();

    List<SubtitleSearchResult> results = new ArrayList<>();

    // first try: search with moviehash & filesize
    if (options.getFile() != null && options.getFile().exists() && options.getFile().length() > 0) {
      File file = options.getFile();
      long fileSize = file.length();
      String hash = computeOpenSubtitlesHash(file);

      getLogger().debug("searching subtitle for {}", file);
      getLogger().debug("moviebytesize: {}; moviehash: {}", fileSize, hash);

      Map<String, Object> mapQuery = new HashMap<>();
      mapQuery.put("moviebytesize", String.valueOf(fileSize));
      mapQuery.put("moviehash", hash);
      mapQuery.put("sublanguageid", getLanguageCode(options.getLanguage().toLocale()));
      try {
        OpenSubtitlesConnectionCounter.trackConnections();
        Object[] arrayQuery = { mapQuery };
        Info info = new Info((Map<String, Object>) methodCall("SearchSubtitles", arrayQuery));

        for (Info.MovieInfo movieInfo : info.getMovieInfo()) {
          // hash search will give a 100% perfect match
          SubtitleSearchResult result = morphSearchResult(movieInfo);
          result.setScore(1.0f);

          results.add(result);
        }

        getLogger().debug("found {} results", info.getMovieInfo().size());
      }
      catch (TmmXmlRpcException e) {
        switch (e.statusCode) {
          // forbidden/unauthorized
          case HttpURLConnection.HTTP_FORBIDDEN:
          case HttpURLConnection.HTTP_UNAUTHORIZED:
            throw new ScrapeException(new Exception("Access to Opensubtitles was not successfull (HTTP " + e.statusCode + ")"));

          // rate limit exceeded?
          case 429:
          case 407:
            throw new ScrapeException(new Exception("Rate limit exceeded (HTTP " + e.statusCode + ")"));

          // unspecified error:
          default:
            throw new ScrapeException(e.getCause());
        }
      }
      catch (Exception e) {
        getLogger().error("Could not search subtitle - {}", e.getMessage());
      }
    }

    // second try: search by IMDB Id
    if (results.isEmpty() && MediaIdUtil.isValidImdbId(options.getImdbId())) {
      Map<String, Object> mapQuery = new HashMap<>();

      getLogger().debug("searching subtitle for imdb id: {}", options.getImdbId());
      // use IMDB Id without leading tt
      mapQuery.put("imdbid", options.getImdbId().replace("tt", ""));
      mapQuery.put("sublanguageid", getLanguageCode(options.getLanguage().toLocale()));

      if (options.getEpisode() > -1) {
        mapQuery.put("episode", String.valueOf(options.getEpisode()));
      }
      if (options.getSeason() > -1) {
        mapQuery.put("season", String.valueOf(options.getSeason()));
      }

      try {
        OpenSubtitlesConnectionCounter.trackConnections();
        Object[] arrayQuery = { mapQuery };
        Info info = new Info((Map<String, Object>) methodCall("SearchSubtitles", arrayQuery));

        for (Info.MovieInfo movieInfo : info.getMovieInfo()) {
          SubtitleSearchResult result = morphSearchResult(movieInfo);
          // degrade maximal search score of imdb search to 0.9
          result.setScore(0.9f);

          results.add(result);
        }

        getLogger().debug("found {} results", info.getMovieInfo().size());
      }
      catch (TmmXmlRpcException e) {
        switch (e.statusCode) {
          // forbidden/unauthorized
          case HttpURLConnection.HTTP_FORBIDDEN:
          case HttpURLConnection.HTTP_UNAUTHORIZED:
            throw new ScrapeException(new Exception("Access to Opensubtitles was not successfull (HTTP " + e.statusCode + ")"));

          // rate limit exceeded?
          case 429:
          case 407:
            throw new ScrapeException(new Exception("Rate limit exceeded (HTTP " + e.statusCode + ")"));

          // service unavailable
          case 503:
            throw new ScrapeException(new HttpException(503, "Service unavailable (HTTP 503))"));

          // unspecified error:
          default:
            throw new ScrapeException(e.getCause());
        }
      }
      catch (Exception e) {
        getLogger().error("Could not search subtitle.", e);
      }
    }

    // third try: search by query
    if (results.isEmpty() && StringUtils.isNotBlank(options.getSearchQuery())) {
      Map<String, Object> mapQuery = new HashMap<>();

      getLogger().debug("serching subtitle for query: {}", options.getSearchQuery());

      mapQuery.put("query", options.getSearchQuery());
      mapQuery.put("sublanguageid", getLanguageCode(options.getLanguage().toLocale()));
      try {
        OpenSubtitlesConnectionCounter.trackConnections();
        Object[] arrayQuery = { mapQuery };
        Info info = new Info((Map<String, Object>) methodCall("SearchSubtitles", arrayQuery));
        for (Info.MovieInfo movieInfo : info.getMovieInfo()) {
          // degrade maximal search score of title search to 0.8
          float score = 0.8f * Similarity.compareStrings(options.getSearchQuery(), movieInfo.movieTitle);

          SubtitleSearchResult result = morphSearchResult(movieInfo);
          result.setScore(score);

          results.add(result);
        }

        getLogger().debug("found {} results", info.getMovieInfo().size());
      }
      catch (TmmXmlRpcException e) {
        switch (e.statusCode) {
          // forbidden/unauthorized
          case HttpURLConnection.HTTP_FORBIDDEN:
          case HttpURLConnection.HTTP_UNAUTHORIZED:
            throw new ScrapeException(new Exception("Access to Opensubtitles was not successfull (HTTP " + e.statusCode + ")"));

          // rate limit exceeded?
          case 429:
          case 407:
            throw new ScrapeException(new Exception("Rate limit exceeded (HTTP " + e.statusCode + ")"));

          // unspecified error:
          default:
            throw new ScrapeException(e);
        }
      }
      catch (Exception e) {
        getLogger().error("Could not search subtitle.", e);
      }
    }

    Collections.sort(results);
    Collections.reverse(results);

    return results;
  }

  private SubtitleSearchResult morphSearchResult(Info.MovieInfo movieInfo) {
    SubtitleSearchResult result = new SubtitleSearchResult(providerInfo.getId());
    result.setId(movieInfo.id);
    result.setTitle(movieInfo.movieTitle);
    result.setReleaseName(movieInfo.movieReleaseName);
    result.setUrl(movieInfo.zipDownloadLink);
    result.setRating(movieInfo.subRating);
    result.setStackCount(movieInfo.subSumCD);
    result.setScore((float) movieInfo.score);

    return result;
  }

  /**
   * calls the specific method with params...
   *
   * @param method
   *          the method
   * @param params
   *          the params
   * @return return value
   * @throws TmmXmlRpcException
   * @throws ScrapeException
   */
  private Object methodCall(String method, Object params) throws TmmXmlRpcException, ScrapeException {
    startSession();
    Object response = null;
    if (StringUtils.isNotBlank(sessionToken)) {
      if (params != null) {
        response = client.call(method, sessionToken, params);
      }
      else {
        response = client.call(method, sessionToken);
      }
    }
    else {
      getLogger().warn("Have no session - seems the startSession() did not work successfully");
    }
    return response;
  }

  /**
   * opensubtitles need sometimes not ISO 639.2B - this method maps the exceptions
   *
   * @param locale
   *          the language top be converted
   * @return the string accepted by opensubtitles
   */
  private String getLanguageCode(Locale locale) {
    // default ISO 639.2B
    String languageCode = LanguageUtils.getIso3BLanguage(locale.getLanguage());

    // and now the exceptions
    // greek: gre -> ell
    if ("gre".equals(languageCode)) {
      languageCode = "ell";
    }
    // pt_BR -> pob
    if ("pt_BR".equalsIgnoreCase(locale.toString())) {
      languageCode = "pob";
    }

    return languageCode;
  }

  /**
   * This function should be always called when starting communication with OSDb server to identify user, specify application and start a new session
   * (either registered user or anonymous). If user has no account, blank username and password should be used.
   */
  @SuppressWarnings("unchecked")
  private synchronized void startSession() throws ScrapeException {
    if ((providerInfo.getConfig().getValue("username") != null && !username.equals(providerInfo.getConfig().getValue("username")))
        || (providerInfo.getConfig().getValue("password") != null && !password.equals(providerInfo.getConfig().getValue("password")))) {
      username = providerInfo.getConfig().getValue("username");
      password = providerInfo.getConfig().getValue("password");
      sessionToken = "";
    }

    if (StringUtils.isBlank(sessionToken)) {
      try {
        OpenSubtitlesConnectionCounter.trackConnections();

        Map<String, Object> response = (Map<String, Object>) client.call("LogIn", username, password, "", getApiKey());
        sessionToken = (String) response.get("token");
        getLogger().debug("Login OK");
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }
    }
  }

  /**
   * Returns OpenSubtitle hash or empty string if error
   *
   * @param file
   *          the file to compute the hash
   * @return hash
   */
  private String computeOpenSubtitlesHash(File file) {
    long size = file.length();
    long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);

    try (FileInputStream is = new FileInputStream(file); FileChannel fileChannel = is.getChannel()) {
      // do not use FileChannel.map() here because it is not releasing resources
      ByteBuffer buf = ByteBuffer.allocate((int) chunkSizeForFile);
      fileChannel.read(buf);
      long head = computeOpenSubtitlesHashForChunk(buf);

      fileChannel.read(buf, Math.max(size - HASH_CHUNK_SIZE, 0));
      long tail = computeOpenSubtitlesHashForChunk(buf);

      return String.format("%016x", size + head + tail);
    }
    catch (Exception e) {
      getLogger().error("Error computing OpenSubtitles hash", e);
    }
    return "";
  }

  private static long computeOpenSubtitlesHashForChunk(ByteBuffer buffer) {

    buffer.rewind();
    LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
    long hash = 0;

    while (longBuffer.hasRemaining()) {
      hash += longBuffer.get();
    }

    return hash;
  }
}
