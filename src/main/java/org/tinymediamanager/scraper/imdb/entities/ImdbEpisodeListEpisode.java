package org.tinymediamanager.scraper.imdb.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbEpisodeListEpisode {

  public String               id                   = "";
  public String               episode              = "";
  public String               season               = "";
  public String               titleText            = "";
  public String               plot                 = "";
  public double               aggregateRating      = 0.0;
  public int                  voteCount            = 0;
  public String               releaseDate          = "";
  public int                  releaseYear          = 0;
  public ImdbEpisodeListImage image                = null;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public class ImdbEpisodeListImage {

    public String               caption              = "";
    public String               url                  = "";
    public Integer              maxHeight            = 0;
    public Integer              maxWidth             = 0;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
      this.additionalProperties.put(name, value);
    }

    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
  }
}
