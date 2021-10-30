package org.tinymediamanager.scraper.tmdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Keywords {

  public Integer           id;

  @SerializedName(value = "keywords", alternate = { "results" })
  public List<BaseKeyword> keywords;

}
