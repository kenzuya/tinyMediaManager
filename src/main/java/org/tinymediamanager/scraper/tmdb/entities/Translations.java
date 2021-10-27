package org.tinymediamanager.scraper.tmdb.entities;

import java.util.List;

public class Translations {

  public static class Translation {

    public String iso_3166_1;
    public String iso_639_1;
    public String name;
    public String english_name;
    public Data   data;

    public static class Data {

      /**
       * Title for movies
       */
      public String title;
      /**
       * Title for tvshows/episodes
       */
      public String name;
      public String overview;
      public String homepage;
    }
  }

  public Integer           id;
  public List<Translation> translations;
}
