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
package org.tinymediamanager.scraper.util.youtube.model.formats;

import java.util.Locale;

import org.tinymediamanager.scraper.util.youtube.YoutubeHelper;
import org.tinymediamanager.scraper.util.youtube.model.Itag;
import org.tinymediamanager.scraper.util.youtube.model.quality.AudioQuality;
import org.tinymediamanager.scraper.util.youtube.model.quality.VideoQuality;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Audio & video format
 *
 * @author Manuel Laggner
 */
public class AudioVideoFormat extends Format {

  private final Integer      averageBitrate;
  private final Integer      audioSampleRate;
  private final AudioQuality audioQuality;
  private final String       qualityLabel;
  private final Integer      width;
  private final Integer      height;
  private final VideoQuality videoQuality;

  public AudioVideoFormat(JsonNode json, Itag itag) {
    super(json, itag);
    audioSampleRate = YoutubeHelper.getInt(json, "audioSampleRate");
    averageBitrate = YoutubeHelper.getInt(json, "averageBitrate");
    qualityLabel = YoutubeHelper.getString(json, "qualityLabel");
    width = YoutubeHelper.getInt(json, "width");
    height = YoutubeHelper.getInt(json, "height");

    VideoQuality videoQuality = null;
    if (json.has("quality")) {
      try {
        videoQuality = VideoQuality.valueOf(json.get("quality").asText().toUpperCase(Locale.ROOT));
      }
      catch (IllegalArgumentException ignore) {
      }
    }
    this.videoQuality = videoQuality;

    AudioQuality audioQuality = null;
    if (json.has("audioQuality")) {
      String[] split = json.get("audioQuality").asText().split("_");
      String quality = split[split.length - 1].toUpperCase(Locale.ROOT);
      try {
        audioQuality = AudioQuality.valueOf(quality);
      }
      catch (IllegalArgumentException ignore) {
      }
    }
    this.audioQuality = audioQuality;
  }

  @Override
  public String type() {
    return "audioVideo";
  }

  public VideoQuality videoQuality() {
    return videoQuality != null ? videoQuality : itag.videoQuality();
  }

  public String qualityLabel() {
    return qualityLabel;
  }

  public Integer width() {
    return width;
  }

  public Integer height() {
    return height;
  }

  public Integer averageBitrate() {
    return averageBitrate;
  }

  public AudioQuality audioQuality() {
    return audioQuality != null ? audioQuality : itag.audioQuality();
  }

  public Integer audioSampleRate() {
    return audioSampleRate;
  }
}
