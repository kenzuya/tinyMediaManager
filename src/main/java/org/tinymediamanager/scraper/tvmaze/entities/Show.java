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
package org.tinymediamanager.scraper.tvmaze.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * The Show Class for Retrofit
 */
public class Show {

  @SerializedName("id")
  public int id;
  @SerializedName("url")
  public String url;
  @SerializedName("name")
  public String title;
  @SerializedName("type")
  public String type;
  @SerializedName("language")
  public String language;
  @SerializedName("genres")
  public List<String> genres;
  @SerializedName("status")
  public String status;
  @SerializedName("runtime")
  public int runtime;
  @SerializedName("premiered")
  public String premiered;
  @SerializedName("rating")
  public Rating rating;
  @SerializedName("externals")
  public TvShowIds tvShowIds;
  @SerializedName("image")
  public Image image;
  @SerializedName("summary")
  public String summary;

  /**
   * Rating
   */
  public static class Rating {

    @SerializedName("average")
    public double average;

  }

  /**
   * TvShow Id's
   */
  public static class TvShowIds {

    @SerializedName("tvrage")
    public int tvrage;
    @SerializedName("thetvdb")
    public int thetvdb;
    @SerializedName("imdb")
    public String imdb;

  }

  public static class Image {
    @SerializedName("medium")
    public String medium;

    @SerializedName("original")
    public String original;
  }

}


