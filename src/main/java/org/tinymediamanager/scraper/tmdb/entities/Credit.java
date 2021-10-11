package org.tinymediamanager.scraper.tmdb.entities;

import org.tinymediamanager.scraper.tmdb.enumerations.CreditType;
import org.tinymediamanager.scraper.tmdb.enumerations.MediaType;

public class Credit {

  public CreditType  credit_type;
  public String      department;
  public String      job;
  public CreditMedia media;
  public MediaType   media_type;
  public String      id;
  public BasePerson  person;

}
