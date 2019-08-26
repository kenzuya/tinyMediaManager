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
package org.tinymediamanager.scraper.util.youtube.model;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.util.UrlUtil;
import org.tinymediamanager.scraper.util.youtube.model.formats.AudioFormat;
import org.tinymediamanager.scraper.util.youtube.model.formats.Format;
import org.tinymediamanager.scraper.util.youtube.model.formats.VideoFormat;
import org.tinymediamanager.scraper.util.youtube.model.quality.AudioQuality;
import org.tinymediamanager.scraper.util.youtube.model.quality.VideoQuality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents the parsed media information from the Youtube Parser for a given Youtube link
 *
 * @author Wolfgang Janes
 */
public class YoutubeMedia {

  private MediaDetails        videoDetails;
  private List<Format>        formats;
  private String              videoId;

  private static final Logger LOGGER       = LoggerFactory.getLogger(YoutubeMedia.class);
  private static final String CONFIG_START = "ytplayer.config = ";
  private static final String CONFIG_END   = ";ytplayer.load";
  private static final String ERROR        = "\"status\":\"ERROR\",\"reason\":\"";
  private static final String URL          = "https://www.youtube.com/watch?v=";

  public YoutubeMedia(String videoId) {
    this.videoId = videoId;
    this.videoDetails = new MediaDetails(videoId);
  }

  public MediaDetails getDetails() {
    return videoDetails;
  }

  public List<Format> getFormats() {
    return formats;
  }

  /**
   * Finds media information for video
   *
   * @param videoQuality
   *          the @{@link VideoQuality} to find
   * @param extension
   *          the @Link {@link Extension} to find
   * @return the video information
   */
  public VideoFormat findVideo(VideoQuality videoQuality, Extension extension) {
    for (Format format : formats) {
      if (format instanceof VideoFormat && ((VideoFormat) format).videoQuality() == videoQuality && format.extension().equals(extension)) {
        return ((VideoFormat) format);
      }
    }

    LOGGER.error("could not find video with quality {} and format {}", videoQuality.getText(), extension.getText());
    return null;
  }

  /**
   * Finds the best audio format with the given extension
   *
   * @param extension
   *          The @{@link Extension} to find
   * @return the audio information
   */
  public AudioFormat findBestAudio(Extension extension) {

    // search for all audio formats in the given quality order
    for (AudioQuality quality : Itag.getAudioQualityList()) {

      for (Format format : formats) {
        if (!(format instanceof AudioFormat)) {
          continue;
        }

        if (((AudioFormat) format).audioQuality() == quality && format.extension().equals(extension)) {
          return (AudioFormat) format;
        }
      }
    }

    // nothing found so far
    LOGGER.error("Could not find audio format for extension {}", extension.getText());
    return null;
  }

  /**
   * Parsing the Youtube Webpage from the given YoutubeID to get all the information for downloading audio and video
   *
   * @throws IOException
   *           any {@link IOException occurred while downloading}
   * @throws InterruptedException
   *           indicates that the thread has been interrupted
   */
  public void parseVideo() throws InterruptedException, IOException {

    ObjectMapper objectMapper = new ObjectMapper();

    // Load Page into String
    String page;
    page = UrlUtil.getStringFromUrl(URL + videoId);

    int start = page.indexOf(CONFIG_START);
    int end = page.indexOf(CONFIG_END);

    // Parse config from URL
    if (start == -1 || end == -1) {
      int errorIndex = page.indexOf(ERROR);
      if (errorIndex != -1) {
        String reason = page.substring(errorIndex + ERROR.length(), page.indexOf('\"', errorIndex + ERROR.length() + 1));
        LOGGER.error("Could not parse webpage: {} ", reason);
      }
      else {
        LOGGER.error("Could not parse webpage");
      }
    }

    // Get Video Details
    JsonNode cfgArgs = objectMapper.readTree(page.substring(start + CONFIG_START.length(), end)).get("args");
    videoDetails.setDetails(objectMapper.readTree(cfgArgs.get("player_response").asText()).get("videoDetails"));

    // Get Video / Audio Formats
    ArrayNode jsonAdaptiveFormats = parseFormats(cfgArgs.get("adaptive_fmts").asText());
    formats = new ArrayList<>(jsonAdaptiveFormats.size() + 1);

    for (int i = 0; i < jsonAdaptiveFormats.size(); i++) {
      JsonNode json = jsonAdaptiveFormats.get(i);
      try {
        Itag itag = Itag.findItag(json.get("itag").asInt(0));

        if (itag == null) {
          continue;
        }

        if (itag.isVideo()) {
          formats.add(new VideoFormat(json, itag));
        }
        else if (itag.isAudio()) {
          formats.add(new AudioFormat(json, itag));
        }
      }
      catch (Exception e) {
        LOGGER.error("Error while getting video and audioformats");
      }
    }
  }

  private ArrayNode parseFormats(String formats) {
    ArrayNode array = new ObjectMapper().createArrayNode();
    String splitBy = formats.substring(0, formats.indexOf('=') + 1);
    Pattern pattern = Pattern.compile("&" + splitBy + "|^" + splitBy + "|," + splitBy);
    for (String s : pattern.split(formats)) {
      if (StringUtils.isNotBlank(s)) {
        ObjectNode params = (splitQuery(splitBy + s));
        if (params.has("url")) {
          array.add(params);
        }
      }
    }
    return array;
  }

  private ObjectNode splitQuery(String requestString) {
    ObjectNode queryPairs = new ObjectMapper().createObjectNode();
    try {
      if (requestString != null) {
        String[] pairs = requestString.split("&");

        for (String pair : pairs) {
          String[] commaPairs = pair.split(",");
          for (String commaPair : commaPairs) {
            int idx = commaPair.indexOf('=');
            if (idx > 0) {
              queryPairs.put(URLDecoder.decode(commaPair.substring(0, idx), "UTF-8"), URLDecoder.decode(commaPair.substring(idx + 1), "UTF-8"));
            }
          }
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("Cannot split request string: {}", requestString);
    }
    return queryPairs;
  }
}
