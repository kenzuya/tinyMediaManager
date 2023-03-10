package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.tinymediamanager.core.entities.Person;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbCast {

  public ImdbName             name                 = null;
  public List<ImdbCharacter>  characters           = new ArrayList<>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  public Person toTmm(Person.Type type) {
    if (name == null || name.nameText == null || name.nameText.text.isEmpty()) {
      return null;
    }

    Person p = new Person(type);
    p.setId("imdb", name.id);
    p.setName(name.nameText.text);
    if (characters != null) {
      String chars = characters.stream().map(character -> character.name).collect(Collectors.joining(" / "));
      p.setRole(chars);
    }

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
