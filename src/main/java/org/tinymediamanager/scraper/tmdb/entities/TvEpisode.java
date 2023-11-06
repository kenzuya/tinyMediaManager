package org.tinymediamanager.scraper.tmdb.entities;

import java.util.List;

public class TvEpisode extends BaseTvEpisode {

  public List<CrewMember> crew;

  public List<CastMember> guest_stars;

  public Images           images;
  public ExternalIds      external_ids;
  public Credits          credits;
  public Videos           videos;
  public Translations     translations;

}
