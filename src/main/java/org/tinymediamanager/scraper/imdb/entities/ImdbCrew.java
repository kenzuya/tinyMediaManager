package org.tinymediamanager.scraper.imdb.entities;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tinymediamanager.core.entities.Person;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbCrew {

  public ImdbName             name                 = null;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  public Person toTmm(Person.Type type) {
    if (name == null && name.nameText == null && name.nameText.text.isEmpty()) {
      return null;
    }
    Person p = new Person(type);
    p.setId("imdb", name.id);
    p.setName(name.nameText.text);
    if (name.primaryImage != null) {
      p.setThumbUrl(name.primaryImage.url);
    }
    p.setProfileUrl("https://www.imdb.com/name/" + name.id);
    return p;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
