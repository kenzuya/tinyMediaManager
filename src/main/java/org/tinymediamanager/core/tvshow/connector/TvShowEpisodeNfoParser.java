/*
 * Copyright 2012 - 2024 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tinymediamanager.core.tvshow.connector;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.DIRECTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.movie.MovieHelpers;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.tinymediamanager.scraper.util.StrgUtils;

/**
 * The class TvShowEpisodeNfoParser is used to parse all types of NFO/XML files for episodes
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeNfoParser {
  public List<Episode> episodes = new ArrayList<>();

  /**
   * create a new instance by parsing the document
   *
   * @param document
   *          the document returned by JSOUP.parse()
   */
  private TvShowEpisodeNfoParser(Document document) {
    document.outputSettings().prettyPrint(false);

    // first check if there is a valid root object
    Elements elements = document.select("episodedetails");
    if (!elements.isEmpty()) {
      // parse Kodi style
      for (Element element : elements) {
        Episode episode = new Episode(element);
        if (StringUtils.isNotBlank(episode.title)) {
          episodes.add(episode);
        }
      }

      return;
    }

    elements = document.select("recording");
    if (!elements.isEmpty()) {
      // parse nextpvr style
      Episode episode = new Episode(elements.get(0));

      if (StringUtils.isNotBlank(episode.title)) {
        episodes.add(episode);
      }

      return;
    }

  }

  /**
   * parse the given file
   *
   * @param path
   *          the path to the NFO/XML to be parsed
   * @return a new instance of the parser class
   * @throws Exception
   *           any exception if parsing fails
   */
  public static TvShowEpisodeNfoParser parseNfo(Path path) throws Exception {
    try (InputStream is = Files.newInputStream(path)) {
      return new TvShowEpisodeNfoParser(Jsoup.parse(is, "UTF-8", "", Parser.xmlParser()));
    }
  }

  /**
   * parse the xml content
   *
   * @param content
   *          the content of the NFO/XML to be parsed
   * @return a new instance of the parser class
   * @throws Exception
   *           any exception if parsing fails
   */
  public static TvShowEpisodeNfoParser parseNfo(String content) {
    return new TvShowEpisodeNfoParser(Jsoup.parse(content, "", Parser.xmlParser()));
  }

  /**
   * determines whether this was a valid NFO or not<br />
   * we use several fields which should be filled in a valid NFO for decision
   *
   * @return true/false
   */
  public boolean isValidNfo() {
    if (episodes.isEmpty()) {
      return false;
    }

    Episode episode = episodes.get(0);
    if (StringUtils.isBlank(episode.title)) {
      return false;
    }

    // having multiple episodes in NFO, then the episode# IS mandatory
    if (episodes.size() > 1 && episode.episode < 0) {
      return false;
    }

    return true;
  }

  public List<TvShowEpisode> toTvShowEpisodes() {
    List<TvShowEpisode> episodes = new ArrayList<>();

    for (Episode episode : this.episodes) {
      episodes.add(episode.toTvShowEpisode());
    }

    return episodes;
  }

  public static class Episode {
    private static final Logger       LOGGER              = LoggerFactory.getLogger(Episode.class);
    private static final List<String> IGNORE              = Arrays.asList("set", "status");

    private final Element             root;
    private final List<String>        supportedElements   = new ArrayList<>();

    public String                     title               = "";
    public String                     originaltitle       = "";
    public String                     showTitle           = "";
    public int                        season              = -1;
    public int                        episode             = -1;
    public int                        displayseason       = -1;
    public int                        displayepisode      = -1;
    public String                     plot                = "";
    public int                        runtime             = 0;
    public MediaCertification         certification       = MediaCertification.UNKNOWN;
    public Date                       releaseDate         = null;
    public boolean                    watched             = false;
    public int                        playcount           = 0;
    public MediaSource                source              = MediaSource.UNKNOWN;
    public String                     userNote            = "";
    public String                     originalFileName    = "";

    public Map<String, Object>        ids                 = new HashMap<>();
    public Map<String, Rating>        ratings             = new HashMap<>();

    public List<String>               thumbs              = new ArrayList<>();
    public List<MediaGenres>          genres              = new ArrayList<>();
    public List<String>               studios             = new ArrayList<>();
    public List<String>               tags                = new ArrayList<>();
    public List<Person>               actors              = new ArrayList<>();
    public List<Person>               directors           = new ArrayList<>();
    public List<Person>               credits             = new ArrayList<>();
    public List<MediaEpisodeNumber>   episodeNumbers      = new ArrayList<>();

    public List<String>               unsupportedElements = new ArrayList<>();

    /* some xbmc related tags we parse, but do not use internally */
    public int                        year                = 0;
    public int                        top250              = 0;
    public String                     outline             = "";
    public String                     tagline             = "";
    public String                     trailer             = "";
    public Fileinfo                   fileinfo            = null;
    public String                     epbookmark          = "";
    public Date                       lastplayed          = null;
    public String                     code                = "";
    public Date                       dateadded           = null;
    public String                     episodenumberend    = "";

    private Episode(Element root) {
      this.root = root;

      if ("episodedetails".equalsIgnoreCase(root.tagName())) {
        parseKodiStyle();
      }
      else if ("recording".equalsIgnoreCase(root.tagName())) {
        parseNextpvrStyle();
      }
    }

    private void parseKodiStyle() {
      // parse all supported fields
      parseTag(Episode::parseTitle);
      parseTag(Episode::parseOriginalTitle);
      parseTag(Episode::parseShowTitle);
      parseTag(Episode::parseSeason);
      parseTag(Episode::parseEpisode);
      parseTag(Episode::parseDisplaySeason);
      parseTag(Episode::parseDisplayEpisode);
      parseTag(Episode::parseRatingAndVotes);
      parseTag(Episode::parseYear);
      parseTag(Episode::parseTop250);
      parseTag(Episode::parsePlot);
      parseTag(Episode::parseOutline);
      parseTag(Episode::parseTagline);
      parseTag(Episode::parseRuntime);
      parseTag(Episode::parseThumbs);
      parseTag(Episode::parseCertification);
      parseTag(Episode::parseIds);
      parseTag(Episode::parseReleaseDate);
      parseTag(Episode::parseWatchedAndPlaycount);
      parseTag(Episode::parseGenres);
      parseTag(Episode::parseStudios);
      parseTag(Episode::parseCredits);
      parseTag(Episode::parseDirectors);
      parseTag(Episode::parseTags);
      parseTag(Episode::parseActors);
      parseTag(Episode::parseFileinfo);
      parseTag(Episode::parseSource);
      parseTag(Episode::parseTrailer);

      parseTag(Episode::parseEpbookmark);
      parseTag(Episode::parseLastplayed);
      parseTag(Episode::parseCode);
      parseTag(Episode::parseEpisodenumberend);
      parseTag(Episode::parseDateadded);
      parseTag(Episode::parseOriginalFilename);
      parseTag(Episode::parseUserNote);
      parseTag(Episode::parseEpisodeGroups);

      // MUST BE THE LAST ONE!
      parseTag(Episode::findUnsupportedElements);
    }

    private void parseNextpvrStyle() {
      parseTag(Episode::parseSubtitle);
      parseTag(Episode::parseDescription);
      parseTag(Episode::parseCertificationInRating);
      parseTag(Episode::parseOriginalAirDate);
      parseTag(Episode::parseGenres);

      // fix the case where the title is in the description
      if (StringUtils.isBlank(title) && StringUtils.isNotBlank(plot)) {
        title = plot;
      }
    }

    /**
     * parse the tag in a save way
     *
     * @param function
     *          the parsing function to be executed
     */
    private Void parseTag(Function<Episode, Void> function) {
      try {
        function.apply(this);
      }
      catch (Exception e) {
        LOGGER.warn("problem parsing tag (line {}) - {}", e.getStackTrace()[0].getLineNumber(), e.getMessage());
      }

      return null;
    }

    private Element getSingleElement(Element parent, String tag) {
      Elements elements = parent.select(parent.tagName() + " > " + tag);
      if (elements.size() != 1) {
        return null;
      }
      return elements.get(0);
    }

    /**
     * the title usually comes in the title tag
     */
    private Void parseTitle() {
      supportedElements.add("title");

      Element element = getSingleElement(root, "title");
      if (element != null) {
        title = element.ownText();
      }

      return null;
    }

    /**
     * the title in the nextpvr xml comes in the subtitle tag
     */
    private Void parseSubtitle() {
      supportedElements.add("subtitle");

      Element element = getSingleElement(root, "subtitle");
      if (element != null) {
        title = element.ownText();
      }

      return null;
    }

    /**
     * the title usually comes in the original title tag
     */
    private Void parseOriginalTitle() {
      supportedElements.add("originaltitle");

      Element element = getSingleElement(root, "originaltitle");
      if (element != null) {
        originaltitle = element.ownText();
      }

      return null;
    }

    /**
     * the show title usually comes in the showtitle tag
     */
    private Void parseShowTitle() {
      supportedElements.add("showtitle");

      Element element = getSingleElement(root, "showtitle");
      if (element != null) {
        showTitle = element.ownText();
      }

      return null;
    }

    /**
     * the season usually comes in the season tag
     */
    private Void parseSeason() {
      supportedElements.add("season");

      Element element = getSingleElement(root, "season");
      if (element != null) {
        try {
          season = Integer.parseInt(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * the episode usually comes in the episode tag
     */
    private Void parseEpisode() {
      supportedElements.add("episode");

      Element element = getSingleElement(root, "episode");
      if (element != null) {
        try {
          episode = Integer.parseInt(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * the displayseason usually comes in the displayseason tag
     */
    private Void parseDisplaySeason() {
      supportedElements.add("displayseason");

      Element element = getSingleElement(root, "displayseason");
      if (element != null) {
        try {
          displayseason = Integer.parseInt(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * the displayepisode usually comes in the displayepisode tag
     */
    private Void parseDisplayEpisode() {
      supportedElements.add("displayepisode");

      Element element = getSingleElement(root, "displayepisode");
      if (element != null) {
        try {
          displayepisode = Integer.parseInt(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * rating and votes are either in<br />
     * - two separate fields: rating, votes (old style) or<br />
     * - in a nested ratings field (new style)
     */
    private Void parseRatingAndVotes() {
      supportedElements.add("rating");
      supportedElements.add("userrating");
      supportedElements.add("ratings");
      supportedElements.add("votes");

      // old style
      // <rating>6.5</rating>
      // <votes>846</votes>
      Element element = getSingleElement(root, "rating");
      if (element != null) {
        Rating r = new Rating();
        r.id = Rating.DEFAULT;
        try {
          r.rating = Float.parseFloat(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
        element = getSingleElement(root, "votes");
        if (element != null) {
          try {
            r.votes = MetadataUtil.parseInt(element.ownText()); // replace thousands separator
          }
          catch (Exception ignored) {
          }
        }
        if (r.rating > 0) {
          ratings.put(r.id, r);
        }
      }

      // user rating
      // <userrating>8</userrating>
      element = getSingleElement(root, "userrating");
      if (element != null) {
        try {
          Rating r = new Rating();
          r.id = Rating.USER;
          r.rating = Float.parseFloat(element.ownText());
          if (r.rating > 0) {
            ratings.put(r.id, r);
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      // new style
      // <ratings>
      // <rating name="default" max="10" default="true"> <value>5.800000</value> <votes>2100</votes> </rating>
      // <rating name="imdb"> <value>8.9</value> <votes>12345</votes> </rating>
      // </ratings>
      element = getSingleElement(root, "ratings");
      if (element != null) {
        for (Element ratingChild : element.select(element.tagName() + " > rating")) {
          Rating r = new Rating();
          // name
          r.id = ratingChild.attr("name");

          // Kodi writes tmdb votes as "themoviedb"
          if ("themoviedb".equals(r.id)) {
            r.id = MediaMetadata.TMDB;
          }
          // cleanup
          else if ("rottenTomatoes".equals(r.id)) {
            r.id = "tomatometerallcritics";
          }
          else if ("metascore".equals(r.id)) {
            r.id = MediaMetadata.METACRITIC;
          }

          // maxvalue
          try {
            r.maxValue = MetadataUtil.parseInt(ratingChild.attr("max"));
          }
          catch (NumberFormatException ignored) {
          }

          for (Element child : ratingChild.children()) {
            // value & votes
            switch (child.tagName()) {
              case "value":
                try {
                  r.rating = Float.parseFloat(child.ownText());
                }
                catch (NumberFormatException ignored) {
                }
                break;

              case "votes":
                try {
                  r.votes = MetadataUtil.parseInt(child.ownText());
                }
                catch (Exception ignored) {
                }
                break;
            }
          }

          if (StringUtils.isNotBlank(r.id) && r.rating > 0) {
            ratings.put(r.id, r);
          }
        }
      }

      return null;
    }

    /**
     * the year usually comes in the year tag as an integer
     */
    private Void parseYear() {
      supportedElements.add("year");

      Element element = getSingleElement(root, "year");
      if (element != null) {
        try {
          year = MetadataUtil.parseInt(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * the top250 usually comes in the top250 tag as an integer (or empty)
     */
    private Void parseTop250() {
      supportedElements.add("top250");

      Element element = getSingleElement(root, "top250");
      if (element != null) {
        try {
          top250 = MetadataUtil.parseInt(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * the plot usually comes in the plot tag as an integer (or empty)
     */
    private Void parsePlot() {
      supportedElements.add("plot");

      Element element = getSingleElement(root, "plot");
      if (element != null) {
        plot = element.wholeText();
      }

      return null;
    }

    /**
     * the plot could also come in a description tag (nextpvr)
     */
    private Void parseDescription() {
      supportedElements.add("description");

      Element element = getSingleElement(root, "description");
      if (element != null) {
        plot = element.wholeText();
      }

      return null;
    }

    /**
     * the outline usually comes in the outline tag as an integer (or empty)
     */
    private Void parseOutline() {
      supportedElements.add("outline");

      Element element = getSingleElement(root, "outline");
      if (element != null) {
        outline = element.wholeText();
      }

      return null;
    }

    /**
     * the tagline usually comes in the tagline tag as an integer (or empty)
     */
    private Void parseTagline() {
      supportedElements.add("tagline");

      Element element = getSingleElement(root, "tagline");
      if (element != null) {
        tagline = element.wholeText();
      }

      return null;
    }

    /**
     * the runtime usually comes in the runtime tag as an integer
     */
    private Void parseRuntime() {
      supportedElements.add("runtime");

      Element element = getSingleElement(root, "runtime");
      if (element != null) {
        try {
          runtime = MetadataUtil.parseInt(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * the thumb usually comes in a thumb tag
     */
    private Void parseThumbs() {
      supportedElements.add("thumb");

      Element element = getSingleElement(root, "thumb");
      if (element != null && element.ownText().matches("https?://.*")) {
        thumbs.add(element.ownText());
      }

      return null;
    }

    /**
     * certification will come in the certification or mpaa tag<br />
     * - kodi has both tags filled, but certification has a much more clear format<br />
     * - mediaportal has only mpaa filled
     */
    private Void parseCertification() {
      supportedElements.add("certification");
      supportedElements.add("mpaa");

      Element element = getSingleElement(root, "certification");
      if (element == null || StringUtils.isBlank(element.ownText())) {
        element = getSingleElement(root, "mpaa");
      }
      if (element != null) {
        certification = MovieHelpers.parseCertificationStringForMovieSetupCountry(element.ownText());
      }

      return null;
    }

    /**
     * certification from nextpvr can come in a rating tag which breaks handling of Kodi ratings - so handle this alone
     */
    private Void parseCertificationInRating() {
      supportedElements.add("rating");

      Element element = getSingleElement(root, "rating");
      if (element != null) {
        certification = MovieHelpers.parseCertificationStringForMovieSetupCountry(element.ownText());
      }

      return null;
    }

    /**
     * ids can be stored either in the<br />
     * - id tag (imdbID) or<br />
     * - imdb tag (imdbId) or<br />
     * - tmdbId tag (tmdb Id> or<br />
     * - uniqueid tag (new kodi style multiple ids) or<br />
     * - in a special nested tag (tmm store)
     */
    private Void parseIds() {
      supportedElements.add("id");
      supportedElements.add("imdb");
      supportedElements.add("tmdbid");
      supportedElements.add("ids");
      supportedElements.add("uniqueid");

      // id tag
      Element element = getSingleElement(root, "id");
      if (element != null) {
        try {
          ids.put(MediaMetadata.TVDB, MetadataUtil.parseInt(element.ownText()));
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      // uniqueid
      element = getSingleElement(root, "uniqueid");
      if (element != null && ids.get(MediaMetadata.TVDB) == null) {
        try {
          ids.put(MediaMetadata.TVDB, MetadataUtil.parseInt(element.ownText()));
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      // uniqueid tag
      Elements elements = root.select(root.tagName() + " > uniqueid");
      for (Element id : elements) {
        try {
          String key = id.attr("type");
          String value = id.ownText();
          if (StringUtils.isNoneBlank(key, value)) {
            // special handling for TVDB: <uniqueid type="unknown"..
            if ("unknown".equals(key) && ids.get(MediaMetadata.TVDB) == null) {
              try {
                ids.put(MediaMetadata.TVDB, MetadataUtil.parseInt(value));
              }
              catch (Exception e) {
                // store as string
                ids.put(key, value);
              }
            }
            else {
              // check whether the id is an integer
              try {
                ids.put(key, MetadataUtil.parseInt(value));
              }
              catch (Exception e) {
                // store as string
                ids.put(key, value);
              }
            }
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      // imdb id and pattern check
      element = getSingleElement(root, "imdb");
      if (element != null && MediaIdUtil.isValidImdbId(element.ownText())) {
        ids.put(MediaMetadata.IMDB, element.ownText());
      }

      // tmdbId tag
      element = getSingleElement(root, "tmdbId");
      if (element != null) {
        try {
          ids.put(MediaMetadata.TMDB, MetadataUtil.parseInt(element.ownText()));
        }
        catch (NumberFormatException ignored) {
        }
      }
      // iterate over our internal id store (old JAXB style)
      element = getSingleElement(root, "ids");
      if (element != null) {
        Elements children = element.select(element.tagName() + " > entry");
        for (Element entry : children) {
          Element key = getSingleElement(entry, "key");
          Element value = getSingleElement(entry, "value");

          if (key == null || value == null) {
            continue;
          }

          if (StringUtils.isNoneBlank(key.ownText(), value.ownText())) {
            // check whether the id is an integer
            try {
              ids.put(key.ownText(), MetadataUtil.parseInt(value.ownText()));
            }
            catch (Exception e) {
              // store as string
              ids.put(key.ownText(), value.ownText());
            }
          }
        }
      }
      // iterate over our internal id store (new style)
      element = getSingleElement(root, "ids");
      if (element != null) {
        Elements children = element.children();
        for (Element entry : children) {
          if (StringUtils.isNoneBlank(entry.tagName(), entry.ownText())) {
            // check whether the id is an integer
            try {
              ids.put(entry.tagName(), MetadataUtil.parseInt(entry.ownText()));
            }
            catch (Exception e) {
              // store as string
              ids.put(entry.tagName(), entry.ownText());
            }
          }
        }
      }

      return null;
    }

    /**
     * the release date is usually in the premiered tag
     */
    private Void parseReleaseDate() {
      supportedElements.add("aired");
      supportedElements.add("premiered");

      Element element = getSingleElement(root, "aired");
      if (element != null) {
        // parse a date object out of the string
        try {
          Date date = StrgUtils.parseDate(element.ownText());
          if (date != null) {
            releaseDate = date;
          }
        }
        catch (ParseException ignored) {
        }
      }
      // also look if there is an premiered date
      if (releaseDate == null) {
        element = getSingleElement(root, "premiered");
        if (element != null) {
          // parse a date object out of the string
          try {
            Date date = StrgUtils.parseDate(element.ownText());
            if (date != null) {
              releaseDate = date;
            }
          }
          catch (ParseException ignored) {
          }
        }
      }

      return null;
    }

    /**
     * the release date from nextpvr is in an original_air_date tag
     */
    private Void parseOriginalAirDate() {
      supportedElements.add("original_air_date");

      Element element = getSingleElement(root, "original_air_date");
      if (element != null) {
        // parse a date object out of the string
        try {
          Date date = StrgUtils.parseDate(element.ownText());
          if (date != null) {
            releaseDate = date;
          }
        }
        catch (ParseException ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * parse the watched flag (watched tag) and playcount (playcount tag) together
     */
    private Void parseWatchedAndPlaycount() {
      supportedElements.add("watched");
      supportedElements.add("playcount");

      Element element = getSingleElement(root, "watched");
      if (element != null) {
        try {
          watched = Boolean.parseBoolean(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      element = getSingleElement(root, "playcount");
      if (element != null) {
        try {
          playcount = MetadataUtil.parseInt(element.ownText());
          if (playcount > 0) {
            // tests have proven that Kodi is setting the playcount only, if the episode has been watched
            watched = true;
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * parse the genres tags<br />
     * - kodi has multiple genre tags<br />
     * - mediaportal as a nested genres tag
     */
    private Void parseGenres() {
      supportedElements.add("genres");
      supportedElements.add("genre");

      Elements elements = null;
      Element element = getSingleElement(root, "genres");
      if (element != null) {
        // nested genre tags
        elements = element.select(element.tagName() + " > genre");
      }
      else {
        // direct/multiple genre tags in movie root
        elements = root.select(root.tagName() + " > genre");
      }

      if (elements != null && !elements.isEmpty()) {
        for (Element genre : elements) {
          if (StringUtils.isNotBlank(genre.ownText())) {
            // old style - single tag with delimiter
            String[] split = StringEscapeUtils.unescapeXml(genre.ownText()).split("[/&]");
            for (String sp : split) {
              genres.add(MediaGenres.getGenre(sp.strip()));
            }
          }
        }
      }

      return null;
    }

    /**
     * studios come in two different flavors<br />
     * - kodi has multiple studio tags<br />
     * - mediaportal has all studios (comma separated) in one studio tag
     */
    private Void parseStudios() {
      supportedElements.add("studio");

      Elements elements = root.select(root.tagName() + " > studio");
      // if there is exactly one studio tag, split the studios at the comma
      if (elements.size() == 1) {
        try {
          studios.addAll(Arrays.asList(elements.get(0).ownText().split("\\s*[,\\/]\\s*"))); // split on , or / and remove whitespace around)
        }
        catch (Exception ignored) {
          // ignored
        }
      }
      else {
        for (Element element : elements) {
          if (StringUtils.isNotBlank(element.ownText())) {
            studios.add(element.ownText());
          }
        }
      }

      return null;
    }

    /**
     * credits come in two different flavors<br />
     * - kodi has multiple credits tags<br />
     * - mediaportal has all credits (comma separated) in one credits tag
     */
    private Void parseCredits() {
      supportedElements.add("credits");

      Elements elements = root.select(root.tagName() + " > credits");
      // if there is exactly one credits tag, split the credits at the comma
      if (elements.size() == 1) {
        try {
          Element element = elements.get(0);

          // split on , or / and remove whitespace around
          List<String> creditsNames = ParserUtils.split(element.ownText());

          for (String credit : creditsNames) {
            Person person = new Person();
            person.name = credit;

            // parse IDs, only if exactly one director is in the tag
            if (creditsNames.size() == 1) {
              if (StringUtils.isNotBlank(element.attr("tmdbid"))) {
                person.tmdbId = element.attr("tmdbid");
              }

              if (StringUtils.isNotBlank(element.attr("imdbid"))) {
                person.imdbId = element.attr("imdbid");
              }

              if (StringUtils.isNotBlank(element.attr("tvdbid"))) {
                person.tvdbId = element.attr("tvdbid");
              }
            }

            credits.add(person);
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }
      else {
        for (Element element : elements) {
          if (StringUtils.isNotBlank(element.ownText())) {
            Person person = new Person();
            person.name = element.ownText();

            if (StringUtils.isNotBlank(element.attr("tmdbid"))) {
              person.tmdbId = element.attr("tmdbid");
            }

            if (StringUtils.isNotBlank(element.attr("imdbid"))) {
              person.imdbId = element.attr("imdbid");
            }

            if (StringUtils.isNotBlank(element.attr("tvdbid"))) {
              person.tvdbId = element.attr("tvdbid");
            }

            credits.add(person);
          }
        }
      }

      return null;
    }

    /**
     * directors come in two different flavors<br />
     * - kodi has multiple director tags<br />
     * - mediaportal has all directors (comma separated) in one director tag
     */
    private Void parseDirectors() {
      supportedElements.add("director");

      Elements elements = root.select(root.tagName() + " > director");
      // if there is exactly one director tag, split the directors at the comma
      if (elements.size() == 1) {
        try {
          Element element = elements.get(0);

          // split on , or / and remove whitespace around
          List<String> directorNames = ParserUtils.split(element.ownText());

          for (String director : directorNames) {
            Person person = new Person();
            person.name = director;

            // parse IDs, only if exactly one director is in the tag
            if (directorNames.size() == 1) {
              if (StringUtils.isNotBlank(element.attr("tmdbid"))) {
                person.tmdbId = element.attr("tmdbid");
              }

              if (StringUtils.isNotBlank(element.attr("imdbid"))) {
                person.imdbId = element.attr("imdbid");
              }

              if (StringUtils.isNotBlank(element.attr("tvdbid"))) {
                person.tvdbId = element.attr("tvdbid");
              }
            }

            directors.add(person);
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }
      else {
        for (Element element : elements) {
          if (StringUtils.isNotBlank(element.ownText())) {
            Person person = new Person();
            person.name = element.ownText();

            if (StringUtils.isNotBlank(element.attr("tmdbid"))) {
              person.tmdbId = element.attr("tmdbid");
            }

            if (StringUtils.isNotBlank(element.attr("imdbid"))) {
              person.imdbId = element.attr("imdbid");
            }

            if (StringUtils.isNotBlank(element.attr("tvdbid"))) {
              person.tvdbId = element.attr("tvdbid");
            }

            directors.add(person);
          }
        }
      }

      return null;
    }

    /**
     * tags usually come in a tag tag
     */
    private Void parseTags() {
      supportedElements.add("tag");

      Elements elements = root.select(root.tagName() + " > tag");
      for (Element element : elements) {
        if (StringUtils.isNotBlank(element.ownText())) {
          tags.add(element.ownText());
        }
      }

      return null;
    }

    /**
     * actors usually come as multiple actor tags in the root with three child tags:<br />
     * - name<br />
     * - role<br />
     * - thumb
     */
    private Void parseActors() {
      supportedElements.add("actor");

      Elements elements = root.select(root.tagName() + " > actor");
      for (Element element : elements) {
        Person actor = new Person();
        for (Element child : element.children()) {
          switch (child.tagName()) {
            case "name":
              actor.name = child.ownText();
              break;

            case "role":
              actor.role = child.ownText();
              break;

            case "thumb":
              actor.thumb = child.ownText();
              break;

            case "profile":
              actor.profile = child.ownText();
              break;

            case "tmdbid":
              actor.tmdbId = child.ownText();
              break;

            case "tvdbid":
              actor.tvdbId = child.ownText();
              break;

            case "imdbid":
              actor.imdbId = child.ownText();
              break;

            case "type":
              if (child.ownText().equals("GuestStar")) {
                actor.guestStar = true;
              }
              break;

            default:
              break;
          }
        }
        if (StringUtils.isNotBlank(actor.name)) {
          actors.add(actor);
        }
      }

      return null;
    }

    /**
     * parse file information.
     */
    private Void parseFileinfo() {
      supportedElements.add("fileinfo");

      Element element = getSingleElement(root, "fileinfo");
      if (element != null) {
        // there is a fileinfo tag available - look if there is also a streamdetails tag
        element = getSingleElement(element, "streamdetails");
        if (element != null) {
          // available; parse out everything
          fileinfo = new Fileinfo();

          for (Element child : element.children()) {
            switch (child.tagName().toLowerCase(Locale.ROOT)) {
              case "video":
                Video video = parseVideo(child);
                if (video != null) {
                  fileinfo.videos.add(video);
                }
                break;

              case "audio":
                Audio audio = parseAudio(child);
                if (audio != null) {
                  fileinfo.audios.add(audio);
                }
                break;

              case "subtitle":
                Subtitle subtitle = parseSubtitle(child);
                if (subtitle != null) {
                  fileinfo.subtitles.add(subtitle);
                }
                break;
            }
          }
        }
      }

      return null;
    }

    private Video parseVideo(Element element) {
      Video video = new Video();
      for (Element child : element.children()) {
        switch (child.tagName().toLowerCase(Locale.ROOT)) {
          case "codec":
            video.codec = child.ownText();
            break;

          case "aspect":
            try {
              video.aspect = Float.parseFloat(child.ownText());
            }
            catch (NumberFormatException ignored) {
            }
            break;

          case "width":
            try {
              video.width = MetadataUtil.parseInt(child.ownText());
            }
            catch (NumberFormatException ignored) {
            }
            break;

          case "height":
            try {
              video.height = MetadataUtil.parseInt(child.ownText());
            }
            catch (NumberFormatException ignored) {
            }
            break;

          case "durationinseconds":
            try {
              video.durationinseconds = MetadataUtil.parseInt(child.ownText());
            }
            catch (NumberFormatException ignored) {
            }
            break;

          case "hdrtype":
            video.hdrtype = child.ownText();
            break;

          case "stereomode":
            video.stereomode = child.ownText();
            break;
        }
      }

      // if there is at least the codec, we return the created object
      if (StringUtils.isNotBlank(video.codec)) {
        return video;
      }

      return null;
    }

    private Audio parseAudio(Element element) {
      Audio audio = new Audio();
      for (Element child : element.children()) {
        switch (child.tagName().toLowerCase(Locale.ROOT)) {
          case "codec":
            audio.codec = child.ownText();
            break;

          case "language":
            audio.language = child.ownText();
            break;

          case "channels":
            try {
              audio.channels = MetadataUtil.parseInt(child.ownText());
            }
            catch (NumberFormatException ignored) {
            }
        }
      }

      // if there is at least the codec, we return the created object
      if (StringUtils.isNotBlank(audio.codec)) {
        return audio;
      }
      return null;
    }

    private Subtitle parseSubtitle(Element element) {
      Subtitle subtitle = new Subtitle();
      for (Element child : element.children()) {
        switch (child.tagName().toLowerCase(Locale.ROOT)) {
          case "language":
            subtitle.language = child.ownText();
            break;
        }
      }

      // if there is at least the codec, we return the created object
      if (StringUtils.isNotBlank(subtitle.language)) {
        return subtitle;
      }
      return null;
    }

    /**
     * the media source is usually in the source tag
     */
    private Void parseSource() {
      supportedElements.add("source");

      Element element = getSingleElement(root, "source");
      if (element != null) {
        try {
          source = MediaSource.getMediaSource(element.ownText());
        }
        catch (Exception ignored) {
          // ignored
        }
      }

      return null;
    }

    private Void parseOriginalFilename() {
      supportedElements.add("original_filename");

      Element element = getSingleElement(root, "original_filename");

      if (element != null) {
        originalFileName = element.ownText();
      }
      return null;
    }

    /**
     * a trailer is usually in the trailer tag
     */
    private Void parseTrailer() {
      supportedElements.add("trailer");

      Element element = getSingleElement(root, "trailer");
      if (element != null) {
        // the trailer can come as a plain http link or prepared for kodi

        // try to parse out youtube trailer plugin
        Pattern pattern = Pattern.compile("plugin://plugin.video.youtube/\\?action=play_video&videoid=(.*)$");
        Matcher matcher = pattern.matcher(element.ownText());
        if (matcher.matches()) {
          trailer = "http://www.youtube.com/watch?v=" + matcher.group(1);
        }
        else {
          pattern = Pattern.compile("plugin://plugin.video.hdtrailers_net/video/.*\\?/(.*)$");
          matcher = pattern.matcher(element.ownText());
          if (matcher.matches()) {
            try {
              trailer = URLDecoder.decode(matcher.group(1), "UTF-8");
            }
            catch (UnsupportedEncodingException ignored) {
            }
          }
        }

        // pure http link
        if (StringUtils.isNotBlank(element.ownText()) && element.ownText().matches("https?://.*")) {
          trailer = element.ownText();
        }
      }

      return null;
    }

    /**
     * find epbookmark for xbmc related nfos
     */
    private Void parseEpbookmark() {
      supportedElements.add("epbookmark");

      Element element = getSingleElement(root, "epbookmark");
      if (element != null) {
        epbookmark = element.ownText();
      }

      return null;
    }

    /**
     * find lastplayed for xbmc related nfos
     */
    private Void parseLastplayed() {
      supportedElements.add("lastplayed");

      Element element = getSingleElement(root, "lastplayed");
      if (element != null) {
        // parse a date object out of the string
        try {
          Date date = StrgUtils.parseDate(element.ownText());
          if (date != null) {
            lastplayed = date;
          }
        }
        catch (ParseException ignored) {
          // ignored
        }
      }

      return null;
    }

    /**
     * find code for xbmc related nfos
     */
    private Void parseCode() {
      supportedElements.add("code");

      Element element = getSingleElement(root, "code");
      if (element != null) {
        code = element.ownText();
      }

      return null;
    }

    /**
     * find episodenumberend for emby relates nfos
     */
    private Void parseEpisodenumberend() {
      supportedElements.add("episodenumberend");

      Element element = getSingleElement(root, "episodenumberend");
      if (element != null) {
        episodenumberend = element.ownText();
      }

      return null;
    }

    /**
     * find dateadded for xbmc related nfos
     */
    private Void parseDateadded() {
      supportedElements.add("dateadded");

      Element element = getSingleElement(root, "dateadded");
      if (element != null) {
        // parse a date object out of the string
        try {
          Date date = StrgUtils.parseDate(element.ownText());
          if (date != null) {
            dateadded = date;
          }
        }
        catch (ParseException ignored) {
        }
      }

      return null;
    }

    /**
     * find and store all unsupported tags
     */
    private Void findUnsupportedElements() {
      // just ignore further elements
      supportedElements.add("lockdata");

      // get all children of the root
      for (Element element : root.children()) {
        if (!IGNORE.contains(element.tagName()) && !supportedElements.contains(element.tagName())) {
          String elementText = element.toString().replaceAll(">\\r?\\n\\s*<", "><");
          unsupportedElements.add(elementText);
        }
      }

      return null;
    }

    /**
     * the user note is usually in the user_note tag
     */
    private Void parseUserNote() {
      supportedElements.add("user_note");

      Element element = getSingleElement(root, "user_note");
      if (element != null) {
        userNote = element.ownText();
      }
      return null;
    }

    /**
     * the episode groups are in the episode_group tag
     */
    private Void parseEpisodeGroups() {
      supportedElements.add("episode_groups");

      Element element = getSingleElement(root, "episode_groups");
      if (element != null) {
        for (Element group : element.children()) {
          try {
            MediaEpisodeGroup.EpisodeGroupType episodeGroupType = MediaEpisodeGroup.EpisodeGroupType.valueOf(group.attr("id"));
            MediaEpisodeNumber episodeNumber = new MediaEpisodeNumber(new MediaEpisodeGroup(episodeGroupType, group.attr("name")),
                Integer.parseInt(group.attr("season")), Integer.parseInt(group.attr("episode")));

            if (episodeNumber.containsAnyNumber()) {
              episodeNumbers.add(episodeNumber);
            }
          }
          catch (Exception ignored) {
            // nothing to do
          }
        }
      }
      return null;
    }

    /**
     * morph this instance to a TvShowEpisode object
     *
     * @return the TvShowEpisode Object
     */
    public TvShowEpisode toTvShowEpisode() {
      TvShowEpisode episode = new TvShowEpisode();
      episode.setTitle(title);
      episode.setOriginalTitle(originaltitle);

      // do we have episode group information
      if (!episodeNumbers.isEmpty()) {
        for (MediaEpisodeNumber episodeNumber : episodeNumbers) {
          episode.setEpisode(episodeNumber);
        }
      }
      else {
        // no - just add the S/E from the old style NFO
        episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, this.season, this.episode));
        episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_DISPLAY, this.displayseason, this.displayepisode));
      }

      for (Map.Entry<String, TvShowEpisodeNfoParser.Rating> entry : ratings.entrySet()) {
        TvShowEpisodeNfoParser.Rating r = entry.getValue();
        episode.setRating(new MediaRating(r.id, r.rating, r.votes, r.maxValue));
      }

      episode.setYear(year);
      episode.setFirstAired(releaseDate);
      if (dateadded != null) {
        // set when in NFO, else use constructor date
        episode.setDateAdded(dateadded);
      }
      episode.setPlot(plot);

      if (!thumbs.isEmpty()) {
        episode.setArtworkUrl(thumbs.get(0), MediaFileType.THUMB);
      }

      for (Map.Entry<String, Object> entry : ids.entrySet()) {
        episode.setId(entry.getKey(), entry.getValue());
      }

      String studio = StringUtils.join(studios, " / ");
      if (studio == null) {
        episode.setProductionCompany("");
      }
      else {
        episode.setProductionCompany(studio);
      }

      episode.setWatched(watched);
      episode.setPlaycount(playcount);
      episode.setLastWatched(lastplayed);
      episode.setMediaSource(source);

      List<org.tinymediamanager.core.entities.Person> newActors = new ArrayList<>();
      for (Person actor : actors) {
        org.tinymediamanager.core.entities.Person tmmActor = morphPerson(ACTOR, actor);
        if (!newActors.contains(tmmActor)) {
          newActors.add(tmmActor);
        }
      }
      episode.addToActors(newActors);

      List<org.tinymediamanager.core.entities.Person> newDirectors = new ArrayList<>();
      for (Person director : directors) {
        if (StringUtils.isBlank(director.role)) {
          director.role = "Director";
        }
        newDirectors.add(morphPerson(DIRECTOR, director));
      }
      episode.addToDirectors(newDirectors);

      List<org.tinymediamanager.core.entities.Person> newWriters = new ArrayList<>();
      for (Person writer : credits) {
        if (StringUtils.isBlank(writer.role)) {
          writer.role = "Writer";
        }
        newWriters.add(morphPerson(WRITER, writer));
      }
      episode.addToWriters(newWriters);

      episode.addToTags(tags);

      episode.setOriginalFilename(originalFileName);
      episode.setNote(userNote);

      return episode;
    }

    private org.tinymediamanager.core.entities.Person morphPerson(org.tinymediamanager.core.entities.Person.Type type, Person nfoPerson) {
      org.tinymediamanager.core.entities.Person person = new org.tinymediamanager.core.entities.Person(type);

      person.setName(nfoPerson.name);
      person.setRole(nfoPerson.role);
      person.setThumbUrl(nfoPerson.thumb);
      person.setProfileUrl(nfoPerson.profile);

      if (nfoPerson.guestStar) {
        person.setType(org.tinymediamanager.core.entities.Person.Type.GUEST);
      }

      int tmdbId = MetadataUtil.parseInt(nfoPerson.tmdbId, 0);
      if (tmdbId > 0) {
        person.setId(MediaMetadata.TMDB, tmdbId);
      }

      if (StringUtils.isNotBlank(nfoPerson.imdbId)) {
        person.setId(MediaMetadata.IMDB, nfoPerson.imdbId);
      }

      int tvdbId = MetadataUtil.parseInt(nfoPerson.tvdbId, 0);
      if (tvdbId > 0) {
        person.setId(MediaMetadata.TVDB, tvdbId);
      }

      return person;
    }
  }

  /*
   * entity classes
   */
  public static class Rating {
    public static final String DEFAULT  = "default";
    public static final String USER     = "user";

    public String              id       = "";
    public float               rating   = 0;
    public int                 votes    = 0;
    public int                 maxValue = 10;
  }

  public static class Person {
    public String  name      = "";
    public String  role      = "";
    public String  thumb     = "";
    public boolean guestStar = false;
    public String  profile   = "";
    public String  tmdbId    = "";
    public String  imdbId    = "";
    public String  tvdbId    = "";
  }

  public static class Fileinfo {
    public List<Video>    videos    = new ArrayList<>();
    public List<Audio>    audios    = new ArrayList<>();
    public List<Subtitle> subtitles = new ArrayList<>();
  }

  public static class Video {
    public String codec      = "";
    public float  aspect     = 0f;
    public int    width      = 0;
    public int    height     = 0;
    public int    durationinseconds;
    public String hdrtype    = "";
    public String stereomode = "";
  }

  public static class Audio {
    public String codec    = "";
    public String language = "";
    public int    channels = 0;
  }

  public static class Subtitle {
    public String language;
  }
}
