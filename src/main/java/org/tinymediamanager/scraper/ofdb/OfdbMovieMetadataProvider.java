/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.scraper.ofdb;

import java.io.InterruptedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * the class {@link OfdbMovieMetadataProvider} is used to gather meta data from ofdb
 * 
 * @author Manuel Laggner
 */
public class OfdbMovieMetadataProvider extends OfdbMetadataProvider implements IMovieMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OfdbMovieMetadataProvider.class);

  @Override
  protected String getSubId() {
    return "movie";
  }

  /*
   * <meta property="og:title" content="Bourne Vermaächtnis, Das (2012)" /> <meta property="og:type" content="movie" /> <meta property="og:url"
   * content="http://www.ofdb.de/film/226745,Das-Bourne-Vermächtnis" /> <meta property="og:image" content="http://img.ofdb.de/film/226/226745.jpg" />
   * <meta property="og:site_name" content="OFDb" /> <meta property="fb:app_id" content="198140443538429" /> <script
   * src="http://www.ofdb.de/jscripts/vn/immer_oben.js" type="text/javascript"></script>
   */
  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata() {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    // we have 3 entry points here
    // a) getMetadata has been called with an ofdbId
    // b) getMetadata has been called with an imdbId
    // c) getMetadata has been called from a previous search

    String detailUrl = "";

    // case a)
    String id = options.getIdAsString(getId());

    if (StringUtils.isNotBlank(id)) {
      try {
        detailUrl = getApiKey() + "/view.php?page=film&fid=" + id;
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }
    }

    // case b)
    if (options.getSearchResult() == null && StringUtils.isNotBlank(options.getIdAsString(MediaMetadata.IMDB))) {
      try {
        SortedSet<MediaSearchResult> results = search(options);
        if (!results.isEmpty()) {
          options.setSearchResult(results.first());
          detailUrl = options.getSearchResult().getUrl();
        }
      }
      catch (Exception e) {
        LOGGER.warn("failed IMDB search: {}", e.getMessage());
      }
    }

    // case c)
    if (options.getSearchResult() != null) {
      detailUrl = options.getSearchResult().getUrl();
    }

    // we can only work further if we got a search result on ofdb.de
    if (StringUtils.isBlank(detailUrl)) {
      LOGGER.warn("We did not get any useful movie url");
      throw new MissingIdException(MediaMetadata.IMDB, getProviderInfo().getId());
    }

    MediaMetadata md = new MediaMetadata(getId());
    md.setScrapeOptions(options);

    // generic Elements used all over
    Elements el = null;
    String ofdbId = StrgUtils.substr(detailUrl, "film\\/(\\d+),");
    if (StringUtils.isBlank(ofdbId)) {
      ofdbId = StrgUtils.substr(detailUrl, "fid=(\\d+)");
    }

    Document doc = null;
    LOGGER.trace("get details page: {}", detailUrl);
    try {
      doc = UrlUtil.parseDocumentFromUrl(detailUrl);
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("could not fetch detail url: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (doc.getAllElements().size() < 10) {
      throw new ScrapeException(new Exception("we did not receive a valid web page"));
    }

    // parse details

    // IMDB ID "http://www.imdb.com/Title?1194173"
    el = doc.getElementsByAttributeValueContaining("href", "imdb.com");
    if (!el.isEmpty()) {
      md.setId(MediaMetadata.IMDB, "tt" + StrgUtils.substr(el.first().attr("href"), "\\?(\\d+)"));
    }

    // title / year
    // <meta property="og:title" content="Bourne Vermächtnis, Das (2012)" />
    el = doc.getElementsByAttributeValue("property", "og:title");
    if (!el.isEmpty()) {
      String[] ty = parseTitle(el.first().attr("content"));
      md.setTitle(StrgUtils.removeCommonSortableName(ty[0]));
      try {
        md.setYear(Integer.parseInt(ty[1]));
      }
      catch (Exception ignored) {
        // the default value is just fine
      }
    }
    // another year position
    if (md.getYear() == 0) {
      // <a href="view.php?page=blaettern&Kat=Jahr&Text=2012">2012</a>
      el = doc.getElementsByAttributeValueContaining("href", "Kat=Jahr");
      try {
        md.setYear(Integer.parseInt(el.first().text()));
      }
      catch (Exception ignored) {
        // the default value is just fine
      }
    }

    // original title (has to be searched with a regexp)
    // <tr valign="top">
    // <td nowrap=""><font class="Normal" face="Arial,Helvetica,sans-serif"
    // size="2">Originaltitel:</font></td>
    // <td>&nbsp;&nbsp;</td>
    // <td width="99%"><font class="Daten" face="Arial,Helvetica,sans-serif"
    // size="2"><b>Brave</b></font></td>
    // </tr>
    String originalTitle = StrgUtils.substr(doc.body().html(), "(?s)Originaltitel.*?<b>(.*?)</b>");
    if (!originalTitle.isEmpty()) {
      md.setOriginalTitle(StrgUtils.removeCommonSortableName(originalTitle));
    }

    // Production: <a href="view.php?page=blaettern&amp;Kat=Land&amp;Text=Argentinien">Argentinien</a>
    el = doc.getElementsByAttributeValueContaining("href", "Kat=Land");
    for (Element g : el) {
      md.addCountry(g.ownText());
    }

    // Genre: <a href="view.php?page=genre&Genre=Action">Action</a>
    el = doc.getElementsByAttributeValueContaining("href", "page=genre");
    for (Element g : el) {
      md.addGenre(getTmmGenre(g.text()));
    }

    // rating
    // <div itemtype="http://schema.org/AggregateRating" itemscope="" itemprop="aggregateRating">
    // Note: <span itemprop="ratingValue">8.74</span><meta itemprop="worstRating" content="1"><meta itemprop="bestRating" content="10">
    // &nbsp;•&nbsp;&nbsp;Stimmen: <span itemprop="ratingCount">2187</span>
    // &nbsp;•&nbsp;&nbsp;Platz: 19 &nbsp;•&nbsp;&nbsp;Ihre Note: --</div>
    try {
      MediaRating rating = new MediaRating("odfb");
      el = doc.getElementsByAttributeValue("itemprop", "ratingValue");
      if (!el.isEmpty()) {
        String r = el.text();
        if (!r.isEmpty()) {
          rating.setRating(Float.parseFloat(r));
          rating.setMaxValue(10);
        }
      }
      el = doc.getElementsByAttributeValue("itemprop", "ratingCount");
      if (!el.isEmpty()) {
        String r = el.text();
        if (!r.isEmpty()) {
          rating.setVotes(Integer.parseInt(r));
        }
      }
      md.addRating(rating);
    }
    catch (Exception e) {
      LOGGER.trace("could not parse rating: {}", e.getMessage());
    }

    // get PlotLink; open url and parse
    // <a href="plot/22523,31360,Die-Bourne-Identität"><b>[mehr]</b></a>
    LOGGER.trace("parse plot");
    el = doc.getElementsByAttributeValueMatching("href", "plot\\/\\d+,");
    if (!el.isEmpty()) {
      try {
        String plotUrl = getApiKey() + "/" + el.first().attr("href");
        Document plot = UrlUtil.parseDocumentFromUrl(plotUrl);

        Elements block = plot.getElementsByClass("Blocksatz"); // first
        // "Blocksatz" is plot
        String p = block.first().text(); // remove all html stuff
        p = p.substring(p.indexOf("Mal gelesen") + 12); // remove "header"
        md.setPlot(p);
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("failed to get plot page: {}", e.getMessage());
      }
    }

    doc = null;
    try {
      String movieDetail = getApiKey() + "/view.php?page=film_detail&fid=" + ofdbId;
      LOGGER.trace("parse movie detail: {}", movieDetail);
      doc = UrlUtil.parseDocumentFromUrl(movieDetail);
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("failed to get detail page: {}", e.getMessage());
    }

    if (doc != null) {
      parseCast(doc.getElementsContainingOwnText("Regie"), Person.Type.DIRECTOR, md);
      parseCast(doc.getElementsContainingOwnText("Darsteller"), Person.Type.ACTOR, md);
      parseCast(doc.getElementsContainingOwnText("Stimme/Sprecher"), Person.Type.ACTOR, md);
      parseCast(doc.getElementsContainingOwnText("Synchronstimme (deutsch)"), Person.Type.ACTOR, md);
      parseCast(doc.getElementsContainingOwnText("Drehbuchautor(in)"), Person.Type.WRITER, md);
      parseCast(doc.getElementsContainingOwnText("Produzent(in)"), Person.Type.PRODUCER, md);
    }

    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    SortedSet<MediaSearchResult> results = new TreeSet<>();

    String searchQuery = options.getSearchQuery();
    String imdb = "";
    Elements filme = null;
    Exception savedException = null;
    /*
     * Kat = All | Titel | Person | DTitel | OTitel | Regie | Darsteller | Song | Rolle | EAN| IMDb | Google
     * http://www.ofdb.de//view.php?page=suchergebnis &Kat=xxxxxxxxx&SText=yyyyyyyyyyy
     */
    // 1. search with imdbId
    if (StringUtils.isNotEmpty(options.getImdbId())) {
      try {
        imdb = options.getImdbId();
        LOGGER.debug("search with imdbId: {}", imdb);

        Document doc = UrlUtil.parseDocumentFromUrl(getApiKey() + "/view.php?page=suchergebnis&Kat=IMDb&SText=" + imdb);

        // only look for movie links
        filme = doc.getElementsByAttributeValueMatching("href", "film\\/\\d+,");
        LOGGER.debug("found {} search results", filme.size());
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("failed to search for imdb Id {}: {}", imdb, e.getMessage());
        savedException = e;
      }
    }

    // 2. search for search string
    if ((filme == null || filme.isEmpty()) && StringUtils.isNotBlank(options.getSearchQuery())) {
      try {
        String searchString = getApiKey() + "/view.php?page=suchergebnis&Kat=All&SText="
            + URLEncoder.encode(cleanSearch(searchQuery), StandardCharsets.UTF_8);
        LOGGER.debug("search for everything: {}", searchQuery);

        Document doc = UrlUtil.parseDocumentFromUrl(searchString);

        // only look for movie links
        filme = doc.getElementsByAttributeValueMatching("href", "film\\/\\d+,");
        LOGGER.debug("found {} search results", filme.size());
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("failed to search for: {} - {}", searchQuery, e.getMessage());
        savedException = e;
      }
    }

    // if there has been a saved exception and we did not find anything - throw the exception
    if ((filme == null || filme.isEmpty()) && savedException != null) {
      throw new ScrapeException(savedException);
    }

    if (filme == null || filme.isEmpty()) {
      LOGGER.debug("nothing found :(");
      return results;
    }

    // <a href="film/22523,Die-Bourne-Identität"
    // onmouseover="Tip('<img src=&quot;images/film/22/22523.jpg&quot;
    // width=&quot;120&quot; height=&quot;170&quot;>',SHADOW,true)">Bourne
    // Identität, Die<font size="1"> / Bourne Identity, The</font> (2002)</a>
    HashSet<String> foundResultUrls = new HashSet<>();
    for (Element a : filme) {
      try {
        MediaSearchResult sr = new MediaSearchResult(getId(), MediaType.MOVIE);
        if (StringUtils.isNotEmpty(imdb)) {
          sr.setIMDBId(imdb);
        }
        sr.setId(StrgUtils.substr(a.toString(), "film\\/(\\d+),")); // OFDB ID
        sr.setTitle(StringEscapeUtils.unescapeHtml4(StrgUtils.removeCommonSortableName(StrgUtils.substr(a.toString(), ".*>(.*?)(\\[.*?\\])?<font"))));
        LOGGER.debug("found movie {}", sr.getTitle());
        sr.setOriginalTitle(StringEscapeUtils.unescapeHtml4(StrgUtils.removeCommonSortableName(StrgUtils.substr(a.toString(), ".*> / (.*?)</font"))));
        try {
          sr.setYear(Integer.parseInt(StrgUtils.substr(a.toString(), "font> \\((.*?)\\)<\\/a")));
        }
        catch (Exception e) {
          LOGGER.trace("could not parse year: {}", e.getMessage());
        }

        sr.setUrl(getApiKey() + "/" + StrgUtils.substr(a.toString(), "href=\\\"(.*?)\\\""));
        sr.setPosterUrl(getApiKey() + "/images" + StrgUtils.substr(a.toString(), "images(.*?)\\&quot"));

        // check if it has at least a title and url
        if (StringUtils.isBlank(sr.getTitle()) || StringUtils.isBlank(sr.getUrl())) {
          continue;
        }

        // OFDB could provide linke twice - check if that has been already added
        if (foundResultUrls.contains(sr.getUrl())) {
          continue;
        }
        foundResultUrls.add(sr.getUrl());

        if (StringUtils.isNotBlank(sr.getIMDBId()) && imdb.equals(sr.getIMDBId())) {
          // perfect match
          sr.setScore(1);
        }
        else {
          // compare score based on names
          sr.calculateScore(options);
        }
        results.add(sr);
      }
      catch (Exception e) {
        LOGGER.warn("error parsing movie result: {}", e.getMessage());
      }
    }

    return results;
  }

  /**
   * return a 2 element array. 0 = title; 1=date
   * <p>
   * parses the title in the format Title YEAR or Title (YEAR)
   *
   * @param title
   *          the title
   * @return the string[]
   */
  private String[] parseTitle(String title) {
    String v[] = { "", "" };
    if (title == null)
      return v;

    Pattern p = Pattern.compile("(.*)\\s+\\(?([0-9]{4})\\)?", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(title);
    if (m.find()) {
      v[0] = m.group(1);
      v[1] = m.group(2);
    }
    else {
      v[0] = title;
    }
    return v;
  }

  // parse actors
  // find the header
  // go up until TR table row
  // get next TR for casts entries
  private void parseCast(Elements el, Person.Type type, MediaMetadata md) {
    if (el != null && !el.isEmpty()) {
      Element castEl = null;
      for (Element element : el) {
        if (!element.tagName().equals("option")) { // we get more, just do not take the optionbox
          castEl = element;
        }
      }
      if (castEl == null) {
        LOGGER.debug("meh, no {} found", type.name());
        return;
      }
      // walk up to table TR...
      while (!((castEl == null) || (castEl.tagName().equalsIgnoreCase("tr")))) {
        castEl = castEl.parent();
      }
      // ... and take the next table row ^^
      Element tr = castEl.nextElementSibling();

      if (tr != null) {
        for (Element a : tr.getElementsByAttributeValue("valign", "middle")) {
          String act = a.toString();
          String aname = StrgUtils.substr(act, "alt=\"(.*?)\"");
          if (!aname.isEmpty()) {
            Person cm = new Person(type);
            cm.setName(aname);
            String id = StrgUtils.substr(act, "id=(.*?)[^\"]\">");
            if (!id.isEmpty()) {
              cm.setId(getId(), id);
              // thumb
              // http://www.ofdb.de/thumbnail.php?cover=images%2Fperson%2F7%2F7689.jpg&size=6
              // fullsize ;) http://www.ofdb.de/images/person/7/7689.jpg
              try {
                String imgurl = URLDecoder.decode(StrgUtils.substr(act, "images%2Fperson%2F(.*?)&amp;size"), "UTF-8");
                if (!imgurl.isEmpty()) {
                  imgurl = getApiKey() + "/images/person/" + imgurl;
                }
                cm.setThumbUrl(imgurl);
              }
              catch (Exception e) {
                LOGGER.trace("could not parse thumb url: {}", e.getMessage());
              }
              // profile path
              Element profileAnchor = a.getElementsByAttributeValueStarting("href", "view.php?page=person").first();
              if (profileAnchor != null) {
                cm.setProfileUrl(getApiKey() + profileAnchor.attr("href"));
              }
            }
            String arole = StrgUtils.substr(act, "\\.\\.\\. (.*?)</font>").replaceAll("<[^>]*>", "");
            cm.setRole(arole);

            md.addCastMember(cm);
          }
        }
      }
    }
  }

  /*
   * Maps scraper Genres to internal TMM genres
   */
  private MediaGenres getTmmGenre(String genre) {
    MediaGenres g = null;
    if (genre.isEmpty()) {
      return g;
    }
    // @formatter:off
    else if (genre.equals("Abenteuer")) {
      g = MediaGenres.ADVENTURE;
    } else if (genre.equals("Action")) {
      g = MediaGenres.ACTION;
    } else if (genre.equals("Amateur")) {
      g = MediaGenres.INDIE;
    } else if (genre.equals("Animation")) {
      g = MediaGenres.ANIMATION;
    } else if (genre.equals("Anime")) {
      g = MediaGenres.ANIMATION;
    } else if (genre.equals("Biographie")) {
      g = MediaGenres.BIOGRAPHY;
    } else if (genre.equals("Dokumentation")) {
      g = MediaGenres.DOCUMENTARY;
    } else if (genre.equals("Drama")) {
      g = MediaGenres.DRAMA;
    } else if (genre.equals("Eastern")) {
      g = MediaGenres.EASTERN;
    } else if (genre.equals("Erotik")) {
      g = MediaGenres.EROTIC;
    } else if (genre.equals("Essayfilm")) {
      g = MediaGenres.INDIE;
    } else if (genre.equals("Experimentalfilm")) {
      g = MediaGenres.INDIE;
    } else if (genre.equals("Fantasy")) {
      g = MediaGenres.FANTASY;
    } else if (genre.equals("Grusel")) {
      g = MediaGenres.HORROR;
    } else if (genre.equals("Hardcore")) {
      g = MediaGenres.EROTIC;
    } else if (genre.equals("Heimatfilm")) {
      g = MediaGenres.TV_MOVIE;
    } else if (genre.equals("Historienfilm")) {
      g = MediaGenres.HISTORY;
    } else if (genre.equals("Horror")) {
      g = MediaGenres.HORROR;
    } else if (genre.equals("Kampfsport")) {
      g = MediaGenres.SPORT;
    } else if (genre.equals("Katastrophen")) {
      g = MediaGenres.DISASTER;
    } else if (genre.equals("Kinder-/Familienfilm")) {
      g = MediaGenres.FAMILY;
    } else if (genre.equals("Komödie")) {
      g = MediaGenres.COMEDY;
    } else if (genre.equals("Krieg")) {
      g = MediaGenres.WAR;
    } else if (genre.equals("Krimi")) {
      g = MediaGenres.CRIME;
    } else if (genre.equals("Kurzfilm")) {
      g = MediaGenres.SHORT;
    } else if (genre.equals("Liebe/Romantik")) {
      g = MediaGenres.ROMANCE;
    } else if (genre.equals("Mondo")) {
      g = MediaGenres.DOCUMENTARY;
    } else if (genre.equals("Musikfilm")) {
      g = MediaGenres.MUSIC;
    } else if (genre.equals("Mystery")) {
      g = MediaGenres.MYSTERY;
    } else if (genre.equals("Science-Fiction")) {
      g = MediaGenres.SCIENCE_FICTION;
    } else if (genre.equals("Serial")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("Sex")) {
      g = MediaGenres.EROTIC;
    } else if (genre.equals("Splatter")) {
      g = MediaGenres.HORROR;
    } else if (genre.equals("Sportfilm")) {
      g = MediaGenres.SPORT;
    } else if (genre.equals("Stummfilm")) {
      g = MediaGenres.SILENT_MOVIE;
    } else if (genre.equals("TV-Film")) {
      g = MediaGenres.TV_MOVIE;
    } else if (genre.equals("TV-Mini-Serie")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("TV-Pilotfilm")) {
      g = MediaGenres.TV_MOVIE;
    } else if (genre.equals("TV-Serie")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("Thriller")) {
      g = MediaGenres.THRILLER;
    } else if (genre.equals("Tierfilm")) {
      g = MediaGenres.ANIMAL;
    } else if (genre.equals("Webserie")) {
      g = MediaGenres.SERIES;
    } else if (genre.equals("Western")) {
      g = MediaGenres.WESTERN;
    }
    // @formatter:on
    if (g == null) {
      g = MediaGenres.getGenre(genre);
    }
    return g;
  }
}
