package org.tinymediamanager.scraper.tmdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AlternativeTitles {

  @SerializedName(value = "titles", alternate = { "results" })
  public List<AlternativeTitle> titles;

  public Integer                id;

}
