package org.tinymediamanager.scraper.tmdb.entities;

import java.util.List;

public class Credits {

  public Integer          id;

  public List<CastMember> cast;
  public List<CrewMember> crew;
  public List<CastMember> guest_stars;

}
