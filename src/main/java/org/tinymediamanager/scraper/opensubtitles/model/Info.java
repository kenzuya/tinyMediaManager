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
package org.tinymediamanager.scraper.opensubtitles.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author Myron Boyle
 */
public class Info {
  private final double          seconds;
  private final String          status;
  private final List<MovieInfo> movieInfo = new ArrayList<>();

  public Info(Map<String, Object> response) throws Exception {
    this.seconds = (Double) response.get("seconds");
    this.status = (String) response.get("status");

    Object[] data = (Object[]) response.get("data");
    if (data != null) {
      for (Object datum : data) {
        movieInfo.add(new MovieInfo(datum));
      }
    }
  }

  public List<MovieInfo> getMovieInfo() {
    return movieInfo;
  }

  public double getSeconds() {
    return seconds;
  }

  public String getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public static class MovieInfo {
    public final String id;
    public final String movieHash;
    public final double score;

    public final String movieKind;
    public final String movieTitle;
    public final String movieReleaseName;
    public final String subFormat;
    public final String subDownloadLink;
    public final int    subSumCD;
    public final Float  subRating;
    public final String zipDownloadLink;
    public final String season;
    public final String episode;

    @SuppressWarnings("unchecked")
    public MovieInfo(Object data) throws Exception {
      Map<String, Object> values = (Map<String, Object>) data;
      id = (String) values.get("IDSubtitleFile");
      movieHash = (String) values.get("MovieHash");
      score = (Double) values.get("Score");
      movieKind = (String) values.get("MovieKind");
      movieTitle = (String) values.get("MovieName");
      movieReleaseName = (String) values.get("MovieReleaseName");
      subFormat = (String) values.get("SubFormat");
      subDownloadLink = (String) values.get("SubDownloadLink");
      subSumCD = Integer.parseInt((String) values.get("SubSumCD"));
      subRating = Float.parseFloat((String) values.get("SubRating"));
      zipDownloadLink = (String) values.get("ZipDownloadLink");
      season = (String) values.get("SeriesSeason");
      episode = (String) values.get("SeriesEpisode");
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
  }
}
