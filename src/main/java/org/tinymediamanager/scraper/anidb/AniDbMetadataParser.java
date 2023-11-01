package org.tinymediamanager.scraper.anidb;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.util.StrgUtils;

/**
 * 
 * This class is responsible for parsing the XML response from AniDB into usable metadata.
 *
 * @see <a href="https://anidb.net/">https://anidb.net/</a>
 * @see <a href="https://wiki.anidb.net/API">https://wiki.anidb.net/API</a>
 * @see <a href="https://wiki.anidb.net/HTTP_API_Definition">https://wiki.anidb.net/HTTP_API_Definition</a>
 * @see AniDbMetadataProvider
 * @see AniDbTvShowMetadataProvider
 * @see AniDbMovieMetadataProvider
 * @author Manuel Laggner
 */
class AniDbMetadataParser {
  private static final Logger LOGGER       = LoggerFactory.getLogger(AniDbMetadataParser.class);
  private static final String IMAGE_SERVER = "http://img7.anidb.net/pics/anime/";

  private AniDbMetadataParser() {
    throw new IllegalAccessError();
  }

  /**
   * Example of XML returned from Animdb call:
   *
   * <pre>
   * {@code
   * <anime id="777" restricted="false">
   *     <episodecount>1</episodecount>
   *     <startdate>...</startdate>
   *     <titles>...</titles>
   *     <creators>...</creators>
   *     <description>...</description>
   *     <ratings>...</ratings>
   *     <picture>...</picture>
   *     <tags>...</tags>
   *     <characters>...</characters>
   *     <episodes>...</episodes>
   * </anime>
   * }
   * </pre>
   * <p>
   * NOTE: TMM does not use all of the data provided in the XML
   *
   * @param md
   * @param language
   *          Language of desired Title and Language to set on Artwork. This must match the {@code xml:lang} attribute exactly.
   * @param anime
   *          XML Element for {@code <anime></anime>} as shown in example above.
   * @param providerInfo
   *          The {@code MediaProviderInfo} that will provide this Metadata to TMM
   */
  static void fillAnimeMetadata(MediaMetadata md, String language, Element anime, MediaProviderInfo providerInfo) {
    for (Element e : anime.children()) {
      switch (e.tagName()) {
        case "startdate":
          fillDateMetadata(md, e);
          break;

        case "titles":
          fillTitleMetadata(md, language, e);
          break;

        case "description":
          md.setPlot(e.text());
          break;

        case "ratings":
          fillRatingsMetadata(md, e);
          break;

        case "tags":
          fillTagsMetadata(md, e, providerInfo.getConfig().getValueAsInteger("numberOfTags"),
              providerInfo.getConfig().getValueAsInteger("minimumTagsWeight"));
          break;

        case "picture":
          fillArtworkMetadata(md, language, e, providerInfo.getId());
          break;

        case "characters":
          fillActorsMetadata(md, e);
          break;

        default:
      }
    }

    md.addGenre(MediaGenres.ANIME);
  }

  /**
   * Example of XML returned from Animdb call:
   * 
   * <pre>
   * {@code <startdate>1989-07-15</startdate>}
   * </pre>
   *
   * @param md
   * @param startDate
   *          XML Element for {@code <startdate></startdate>} as shown in example above.
   */
  private static void fillDateMetadata(MediaMetadata md, Element startDate) {
    try {
      Date date = StrgUtils.parseDate(startDate.text());
      if (date == null)
        return;
      md.setReleaseDate(date);

      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      md.setYear(calendar.get(Calendar.YEAR));
    }
    catch (ParseException ex) {
      LOGGER.debug("could not parse date: {}", startDate.text());
    }
  }

  /**
   * Example of XML returned from Animdb call:
   *
   * <pre>
   * {@code
   *     <titles>
   *         <title xml:lang="x-jat" type="main">Kidou Keisatsu Patlabor</title>
   *         <title xml:lang="ja" type="synonym">機動警察パトレイバー 劇場版</title>
   *         <title xml:lang="en" type="synonym">Patlabor Movie 1</title>
   *         <title xml:lang="en" type="synonym">Mobile Police Patlabor: The Movie</title>
   *         <title xml:lang="x-jat" type="synonym">Kidou Keisatsu Patlabor Gekijouban</title>
   *         <title xml:lang="ja" type="official">機動警察パトレイバー PATLABOR THE MOBILE POLICE</title>
   *         <title xml:lang="en" type="official">Patlabor the Movie</title>
   *         <title xml:lang="de" type="official">Patlabor - The Movie</title>
   *     </titles>
   * }
   * </pre>
   *
   * @param md
   * @param language
   *          The desired language of the Title. This must match the {@code xml:lang} attribute exactly.
   * @param titles
   *          XML Element for {@code <titles></titles>} as shown in example above.
   */
  private static void fillTitleMetadata(MediaMetadata md, String language, Element titles) {
    String titleEN = "";
    String titleScraperLangu = "";
    String titleMain = "";

    for (Element title : titles.children()) {
      // store first title if neither the requested one nor the english one available

      // do not work further with short titles
      if ("short".equals(title.attr("type"))) {
        continue;
      }

      // only use synonyms if there is no other/"official" translation
      boolean synonym = "synonym".equals(title.attr("type"));

      // main title aka original title
      if ("main".equalsIgnoreCase(title.attr("type"))) {
        if (!synonym || StringUtils.isBlank(titleMain)) {
          titleMain = title.text();
        }
      }

      // store the english one for fallback
      if ("en".equalsIgnoreCase(title.attr("xml:lang"))) {
        if (!synonym || StringUtils.isBlank(titleEN)) {
          titleEN = title.text();
        }
      }

      // search for the requested one
      if (language.equalsIgnoreCase(title.attr("xml:lang"))) {
        if (!synonym || StringUtils.isBlank(titleScraperLangu)) {
          titleScraperLangu = title.text();
        }
      }
    }

    if (StringUtils.isNotBlank(titleMain)) {
      md.setOriginalTitle(titleMain);
    }

    if (StringUtils.isNotBlank(titleScraperLangu)) {
      md.setTitle(titleScraperLangu);
    }
    else if (StringUtils.isNotBlank(titleEN)) {
      md.setTitle(titleEN);
    }
    else { // QUESTION: This gets set even if `titleMain` is blank. Is that ok?
      md.setTitle(titleMain);
    }
  }

  /**
   * Example of XML returned from Anidb call:
   * 
   * <pre>
   * {@code
   *     <ratings>
   *         <permanent count="1558">7.40</permanent>
   *         <temporary count="1562">7.53</temporary>
   *         <review count="1">8.00</review>
   *     </ratings>
   * }
   * </pre>
   * <p>
   * Note: Only the `temporary` rating is used.
   *
   * @param md
   * @param ratings
   *          XML Element for {@code <ratings></ratings>} as shown in example above.
   */
  private static void fillRatingsMetadata(MediaMetadata md, Element ratings) {
    for (Element rating : ratings.children()) {
      if ("temporary".equalsIgnoreCase(rating.tagName())) {
        try {
          MediaRating mediaRating = new MediaRating(MediaMetadata.ANIDB);
          mediaRating.setRating(Float.parseFloat(rating.text()));
          mediaRating.setVotes(Integer.parseInt(rating.attr("count")));
          mediaRating.setMaxValue(10);
          md.addRating(mediaRating);
          break;
        }
        catch (NumberFormatException ex) {
          LOGGER.debug("could not parse rating: {} - {}", rating.text(), rating.attr("count"));
        }
      }
    }
  }

  /**
   * Example of XML returned from Animdb call:
   *
   * <pre>
   * {@code
   *     <tags>
   *         <tag id="308" parentid="2611" infobox="true" weight="400" localspoiler="false" globalspoiler="false" verified="true" update="2017-12-17">
   *             <name>detective</name>
   *             <description>A detective is an investigator, generally either a member of a law enforcement agency or as an individual working in the capacity of private investigators, tasked with solving crimes and other mysteries, such as disappearances, by examining and evaluating clues and records in order to solve the mystery; for a crime, this would mean uncovering the criminal`s identity and/or whereabouts. In some police systems, a detective position is achieved by passing a written test after a person completes the requirements for being a police officer; in others, detectives are college graduates who join directly from civilian life without first serving as uniformed officers.
   *                 Source: Wikipedia</description>
   *             <picurl>210906.jpg</picurl>
   *         </tag>
   *         <tag id="2274" parentid="2638" weight="0" localspoiler="false" globalspoiler="false" verified="true" update="2018-03-30">
   *             <name>robot</name>
   *             <description>A robot is an automatically guided machine, able to do tasks on its own. Another common characteristic is that by its appearance or movements, a robot often conveys a sense that it has intent or agency of its own.
   *                 Not to be confused with android (human-like robot).</description>
   *             <picurl>36563.jpg</picurl>
   *         </tag>
   *     <tags>
   * }
   * </pre>
   * <p>
   * NOTE: TMM does not use all of the data provided in these tags
   *
   * @param md
   * @param tags
   *          XML Element for {@code <tags></tags>}> as shown in example above.
   * @param maxTags
   * @param minWeight
   */
  private static void fillTagsMetadata(MediaMetadata md, Element tags, Integer maxTags, Integer minWeight) {

    for (Element tag : tags.children()) {
      Element name = tag.getElementsByTag("name").first();
      int weight = 0;
      try {
        weight = Integer.parseInt(tag.attr("weight"));
      }
      catch (Exception ex) {
        LOGGER.trace("Could not parse tags weight: {}", ex.getMessage());
      }
      if (name != null && weight >= minWeight) {
        md.addTag(name.text());
        if (md.getTags().size() >= maxTags) {
          break;
        }
      }
    }
  }

  /**
   * Example of XML returned from Animdb call:
   *
   * <pre>
   * {@code <picture>83834.jpg</picture>}
   * </pre>
   *
   * @param md
   * @param language
   *          Language to set on Artwork.
   * @param picture
   *          XML Element for {@code <picture></picture>} as shown in example above.
   * @param providerId
   */
  private static void fillArtworkMetadata(MediaMetadata md, String language, Element picture, String providerId) {
    // Poster
    MediaArtwork ma = new MediaArtwork(providerId, MediaArtwork.MediaArtworkType.POSTER);
    ma.setPreviewUrl(IMAGE_SERVER + picture.text());
    ma.setOriginalUrl(IMAGE_SERVER + picture.text());
    ma.setLanguage(language);
    ma.addImageSize(0, 0, IMAGE_SERVER + picture.text(), 0); // no size given in the API
    md.addMediaArt(ma);
  }

  /**
   * Example of XML returned from Animdb call:
   * 
   * <pre>
   * {@code
   *     <characters>
   *         <character id="9011" type="main character in" update="2013-08-10">
   *             <rating votes="114">7.83</rating>
   *             <name>Izumi Noa</name>
   *             <gender>female</gender>
   *             <charactertype id="1">Character</charactertype>
   *             <description>Born in Tomakomai, Hokkaido in 1978, she is the pilot of the first Ingram in
   *             Unit 2...</description>
   *             <picture>141281.jpg</picture>
   *             <seiyuu id="3940" picture="54324.jpg">Tominaga Miina</seiyuu>
   *         </character>
   *         <character id="9016" type="secondary cast in" update="2013-08-13">
   *             <rating votes="203">9.25</rating>
   *             <name>Gotou Kiichi</name>
   *             <gender>male</gender>
   *             <charactertype id="1">Character</charactertype>
   *             <description>Gotou is the Captain of Unit 2 and was born in the Taito Ward in Tokyo.
   *             Although he....</description>
   *             <picture>141450.jpg</picture>
   *             <seiyuu id="3383" picture="24325.jpg">Oobayashi Ryuusuke</seiyuu>
   *         </character>
   *     </characters>
   *   }
   * </pre>
   * <p>
   * NOTE: Animdb's {@code name} field maps to TMM's {@code role} field, while Anidb's {@code seiyuu} text maps to TMM's {@code name}. This is b/c
   * seiyuu means voice actor in Japanese.
   * <p>
   * NOTE: TMM does not use all of the data provided in these tags
   * </p>
   *
   * @param md
   * @param characters
   *          XML Element for {@code <characters></characters>} as shown in example above.
   */
  private static void fillActorsMetadata(MediaMetadata md, Element characters) {
    for (Element character : characters.children()) {
      Person member = new Person(Person.Type.ACTOR);
      for (Element characterInfo : character.children()) {
        // NOTE: Animdb's name field maps to TMM's role field while Anidb's seiyuu text maps to TMM's name
        if ("name".equalsIgnoreCase(characterInfo.tagName())) {
          member.setRole(characterInfo.text());
        }
        if ("seiyuu".equalsIgnoreCase(characterInfo.tagName())) {
          member.setName(characterInfo.text());
          String image = characterInfo.attr("picture");
          if (StringUtils.isNotBlank(image)) {
            member.setThumbUrl("http://img7.anidb.net/pics/anime/" + image);
          }
        }
      }
      md.addCastMember(member);
    }
  }

  /**
   * <pre>
   * {@code
   *    <episodes>
   *         <episode id="49040" update="2013-05-12">
   *             <epno type="1">1</epno>
   *             <length>25</length>
   *             <airdate>2006-03-25</airdate>
   *             <rating votes="1">5.74</rating>
   *             <title xml:lang="ja">地下世界</title>
   *             <title xml:lang="en">Inner World</title>
   *             <title xml:lang="x-jat">Chika Sekai</title>
   *         </episode>
   *         <episode id="49042" update="2013-05-12">
   *             <epno type="1">2</epno>
   *             <length>25</length>
   *             <airdate>2006-03-25</airdate>
   *             <rating votes="1">5.74</rating>
   *             <title xml:lang="ja">アラクナ城へ</title>
   *             <title xml:lang="en">Never Give Up</title>
   *             <title xml:lang="x-jat">Arachna-jou e</title>
   *         </episode>
   *    </episodes>
   * }
   * </pre>
   *
   * <p>
   * NOTE: The episode number's {@code type}, for example {@code <epno type="1"}, uses a 1 for a normal episode and 2 to indicate a special.
   * </p>
   *
   * @param episodes
   *
   * @return
   */
  static List<AniDbEpisode> parseEpisodes(@Nullable Element episodes) {
    if (episodes == null)
      return new ArrayList<>();

    return episodes.children()
        .stream()
        .filter(e -> e.tagName().equals("episode"))
        .map(AniDbMetadataParser::parseEpisode)
        .filter(Objects::nonNull)
        .toList();
  }

  private static AniDbEpisode parseEpisode(Element episodeElement) {
    AniDbEpisode.Builder builder = new AniDbEpisode.Builder();
    try {
      builder.id(Integer.parseInt(episodeElement.attr("id")));
    }
    catch (NumberFormatException ignored) {
      // ignored
    }

    for (Element episodeInfo : episodeElement.children()) {
      if ("epno".equalsIgnoreCase(episodeInfo.tagName())) {
        String type = episodeInfo.attr("type");
        try {
          // looks like anidb is storing anything in a single season, so put
          if ("1".equals(type)) {
            // 1 to season, if type = 1
            builder.season(1);
            builder.episode(Integer.parseInt(episodeInfo.text()));
          }
          else if ("2".equals(type)) {
            // 2 is a special
            builder.season(0);
            builder.episode(Integer.parseInt(episodeInfo.text().replaceAll("[^0-9]+", "")));
          }
          else {
            // no valid type -> no valid episode
            return null;
          }
        }
        catch (NumberFormatException ignored) {
          // ignored
        }
        continue;
      }

      if ("length".equalsIgnoreCase(episodeInfo.tagName())) {
        try {
          builder.runtime(Integer.parseInt(episodeInfo.text()));
        }
        catch (NumberFormatException ignored) {
          // ignored
        }
        continue;
      }

      if ("airdate".equalsIgnoreCase(episodeInfo.tagName())) {
        try {
          builder.airdate(StrgUtils.parseDate(episodeInfo.text()));
        }
        catch (Exception ignored) {
          // ignored
        }
        continue;
      }

      if ("rating".equalsIgnoreCase(episodeInfo.tagName())) {
        try {
          builder.rating(Float.parseFloat(episodeInfo.text()));
          builder.votes(Integer.parseInt(episodeInfo.attr("votes")));
        }
        catch (NumberFormatException ignored) {
          // ignored
        }
        continue;
      }

      if ("title".equalsIgnoreCase(episodeInfo.tagName())) {
        try {
          builder.titles(episodeInfo.attr("xml:lang").toLowerCase(Locale.ROOT), episodeInfo.text());
        }
        catch (Exception ignored) {
          // ignored
        }
        continue;
      }

      if ("summary".equalsIgnoreCase(episodeInfo.tagName())) {
        builder.summary(episodeInfo.text());
      }
    }

    return builder.build();
  }
}
