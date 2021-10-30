package org.tinymediamanager.scraper.tmdb.entities;

import java.util.Date;
import java.util.List;

import com.google.gson.JsonPrimitive;

public class Person extends BasePerson {

  public List<JsonPrimitive> also_known_as;
  public String              biography;
  public Date                birthday;
  public Date                deathday;
  public Integer             gender;
  public String              homepage;
  public String              imdb_id;
  public String              place_of_birth;

  public PersonExternalIds   external_ids;
  public PersonCredits       combined_credits;
  public PersonCredits       movie_credits;
  public PersonCredits       tv_credits;
  public PersonImages        images;
  public Changes             changes;

}
