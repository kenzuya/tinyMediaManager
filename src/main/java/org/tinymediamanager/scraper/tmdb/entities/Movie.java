package org.tinymediamanager.scraper.tmdb.entities;

import java.util.List;

import org.tinymediamanager.scraper.tmdb.enumerations.Status;

public class Movie extends BaseMovie {

  public Collection           belongs_to_collection;
  public Long                 budget;
  public String               homepage;
  public String               imdb_id;
  public List<BaseCompany>    production_companies;
  public List<Country>        production_countries;
  public Long                 revenue;
  public Integer              runtime;
  public List<SpokenLanguage> spoken_languages;
  public Status               status;
  public String               tagline;

  // Following are used with append_to_response
  public AlternativeTitles    alternative_titles;
  public Changes              changes;
  public Keywords             keywords;
  public Images               images;
  public Translations         translations;
  public Credits              credits;
  public ExternalIds          external_ids;
  public ReleaseDatesResults  release_dates;
  public MovieResultsPage     similar;
  public MovieResultsPage     recommendations;
  public ReviewResultsPage    reviews;
  public Videos               videos;

}
