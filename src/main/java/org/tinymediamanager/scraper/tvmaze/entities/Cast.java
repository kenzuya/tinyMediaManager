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

import com.google.gson.annotations.SerializedName;

public class Cast {

  @SerializedName("person")
  public Person    person;
  @SerializedName("character")
  public Character character;

  public class Person {

    @SerializedName("id")
    public int     id;
    @SerializedName("url")
    public String  url;
    @SerializedName("name")
    public String  name;
    @SerializedName("country")
    public Country country;
    @SerializedName("birthdate")
    public String  birthdate;
    @SerializedName("image")
    public Image   image;
  }

  public class Character {

    @SerializedName("id")
    public int    id;
    @SerializedName("url")
    public String url;
    @SerializedName("name")
    public String name;
    @SerializedName("image")
    public Image  image;
  }

  public class Country {
    @SerializedName("name")
    public String name;
  }

  public class Image {
    @SerializedName("medium")
    public String medium;

    @SerializedName("original")
    public String original;
  }

}
