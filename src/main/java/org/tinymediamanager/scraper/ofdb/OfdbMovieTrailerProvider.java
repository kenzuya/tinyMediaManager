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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * the class {@link OfdbMovieTrailerProvider} is used to provide trailer for movies
 *
 * @author Manuel Laggner
 */
public class OfdbMovieTrailerProvider extends OfdbMetadataProvider implements IMovieTrailerProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OfdbMovieTrailerProvider.class);

  @Override
  protected String getSubId() {
    return "movie_trailer";
  }

  @Override
  public List<MediaTrailer> getTrailers(TrailerSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getTrailers(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    List<MediaTrailer> trailers = new ArrayList<>();
    if (!MediaIdUtil.isValidImdbId(options.getImdbId())) {
      LOGGER.debug("IMDB id not found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }
    /*
     * function getTrailerData(ci) { switch (ci) { case 'http://de.clip-1.filmtrailer.com/9507_31566_a_1.flv?log_var=72|491100001 -1|-' : return
     * '<b>Trailer 1</b><br><i>(small)</i><br><br>&raquo; 160px<br><br>Download:<br>&raquo; <a href=
     * "http://de.clip-1.filmtrailer.com/9507_31566_a_1.wmv?log_var=72|491100001-1|-" >wmv</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_31566_a_2.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 1</b><br><i>(medium)</i><br><br>&raquo;
     * 240px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_31566_a_2.wmv?log_var=72|491100001-1|-" >wmv</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_31566_a_3.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 1</b><br><i>(large)</i><br><br>&raquo;
     * 320px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_31566_a_3.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo;
     * <a href= "http://de.clip-1.filmtrailer.com/9507_31566_a_3.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
     * "http://de.clip-1.filmtrailer.com/9507_31566_a_3.webm?log_var=72|491100001-1|-" >webm</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_31566_a_4.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 1</b><br><i>(xlarge)</i><br><br>&raquo;
     * 400px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_31566_a_4.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo;
     * <a href= "http://de.clip-1.filmtrailer.com/9507_31566_a_4.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
     * "http://de.clip-1.filmtrailer.com/9507_31566_a_4.webm?log_var=72|491100001-1|-" >webm</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_31566_a_5.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 1</b><br><i>(xxlarge)</i><br><br>&raquo;
     * 640px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_31566_a_5.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo;
     * <a href= "http://de.clip-1.filmtrailer.com/9507_31566_a_5.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
     * "http://de.clip-1.filmtrailer.com/9507_31566_a_5.webm?log_var=72|491100001-1|-" >webm</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_39003_a_1.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 2</b><br><i>(small)</i><br><br>&raquo;
     * 160px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_1.wmv?log_var=72|491100001-1|-" >wmv</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_39003_a_2.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 2</b><br><i>(medium)</i><br><br>&raquo;
     * 240px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_2.wmv?log_var=72|491100001-1|-" >wmv</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_39003_a_3.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 2</b><br><i>(large)</i><br><br>&raquo;
     * 320px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_3.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo;
     * <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_3.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
     * "http://de.clip-1.filmtrailer.com/9507_39003_a_3.webm?log_var=72|491100001-1|-" >webm</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_39003_a_4.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 2</b><br><i>(xlarge)</i><br><br>&raquo;
     * 400px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_4.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo;
     * <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_4.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
     * "http://de.clip-1.filmtrailer.com/9507_39003_a_4.webm?log_var=72|491100001-1|-" >webm</a><br>'; case
     * 'http://de.clip-1.filmtrailer.com/9507_39003_a_5.flv?log_var=72|491100001 -1|-' : return '<b>Trailer 2</b><br><i>(xxlarge)</i><br><br>&raquo;
     * 640px<br><br>Download:<br>&raquo; <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_5.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo;
     * <a href= "http://de.clip-1.filmtrailer.com/9507_39003_a_5.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
     * "http://de.clip-1.filmtrailer.com/9507_39003_a_5.webm?log_var=72|491100001-1|-" >webm</a><br>'; } }
     */
    try {
      // search with IMDB
      Document doc = UrlUtil.parseDocumentFromUrl(getApiKey() + "/view.php?page=suchergebnis&Kat=IMDb&SText=" + options.getImdbId());

      Elements filme = doc.getElementsByAttributeValueMatching("href", "film\\/\\d+,");
      if (filme == null || filme.isEmpty()) {
        LOGGER.debug("found no search results");
        return trailers;
      }
      LOGGER.debug("found {} search results", filme.size()); // hopefully only one

      LOGGER.debug("get (trailer) details page");
      doc = UrlUtil.parseDocumentFromUrl(getApiKey() + "/" + StrgUtils.substr(filme.first().toString(), "href=\\\"(.*?)\\\""));

      // OLD STYLE
      // <b>Trailer 1</b><br><i>(xxlarge)</i><br><br>&raquo; 640px<br><br>Download:<br>&raquo; <a href=
      // "http://de.clip-1.filmtrailer.com/9507_31566_a_5.wmv?log_var=72|491100001-1|-" >wmv</a><br>&raquo; <a href=
      // "http://de.clip-1.filmtrailer.com/9507_31566_a_5.mp4?log_var=72|491100001-1|-" >mp4</a><br>&raquo; <a href=
      // "http://de.clip-1.filmtrailer.com/9507_31566_a_5.webm?log_var=72|491100001-1|-" >webm</a><br>
      Pattern regex = Pattern.compile("return '(.*?)';");
      Matcher m = regex.matcher(doc.toString());
      while (m.find()) {
        String s = m.group(1);
        String tname = StrgUtils.substr(s, "<b>(.*?)</b>");
        String tpix = StrgUtils.substr(s, "raquo; (.*?)x<br>");

        // url + format
        Pattern lr = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
        Matcher lm = lr.matcher(s);
        while (lm.find()) {
          String turl = lm.group(1);
          // String tformat = lm.group(2);
          MediaTrailer trailer = new MediaTrailer();
          trailer.setName(tname);
          // trailer.setQuality(tpix + " (" + tformat + ")");
          trailer.setQuality(tpix);
          trailer.setProvider("filmtrailer");
          trailer.setScrapedBy(getProviderInfo().getId());
          trailer.setUrl(turl);
          LOGGER.debug(trailer.toString());
          trailers.add(trailer);
        }
      }

      // NEW STYLE (additional!)
      // <div class="clips" id="clips2" style="display: none;">
      // <img src="images/flag_de.gif" align="left" vspace="3" width="18" height="12">&nbsp;
      // <img src="images/trailer_6.gif" align="top" vspace="1" width="16" height="16" alt="freigegeben ab 6 Jahren">&nbsp;
      // <i>Trailer 1:</i>
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_1.flv?log_var=67|491100001-1|-">&nbsp;small&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_2.flv?log_var=67|491100001-1|-">&nbsp;medium&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_3.flv?log_var=67|491100001-1|-">&nbsp;large&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_4.flv?log_var=67|491100001-1|-">&nbsp;xlarge&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_6584_a_5.flv?log_var=67|491100001-1|-">&nbsp;xxlarge&nbsp;</a> &nbsp;
      // <br>
      // <img src="images/flag_de.gif" align="left" vspace="3" width="18" height="12">&nbsp;
      // <img src="images/trailer_6.gif" align="top" vspace="1" width="16" height="16" alt="freigegeben ab 6 Jahren">&nbsp;
      // <i>Trailer 2:</i>
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_1.flv?log_var=67|491100001-1|-">&nbsp;small&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_2.flv?log_var=67|491100001-1|-">&nbsp;medium&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_3.flv?log_var=67|491100001-1|-">&nbsp;large&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_4.flv?log_var=67|491100001-1|-">&nbsp;xlarge&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_8244_a_5.flv?log_var=67|491100001-1|-">&nbsp;xxlarge&nbsp;</a> &nbsp;
      // <br>
      // <img src="images/flag_de.gif" align="left" vspace="3" width="18" height="12">&nbsp;
      // <img src="images/trailer_6.gif" align="top" vspace="1" width="16" height="16" alt="freigegeben ab 6 Jahren">&nbsp;
      // <i>Trailer 3:</i>
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_1.flv?log_var=67|491100001-1|-">&nbsp;small&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_2.flv?log_var=67|491100001-1|-">&nbsp;medium&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_3.flv?log_var=67|491100001-1|-">&nbsp;large&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_4.flv?log_var=67|491100001-1|-">&nbsp;xlarge&nbsp;</a> &nbsp;
      // <a href="http://de.clip-1.filmtrailer.com/2845_14749_a_5.flv?log_var=67|491100001-1|-">&nbsp;xxlarge&nbsp;</a> &nbsp;
      // <br>
      // <br>
      // </div>

      // new style size
      // 1 = 160 x 90 = small
      // 2 = 240 x 136 = medium
      // 3 = 320 x 180 = large
      // 4 = 400 x 226 = xlarge
      // 5 = 640 x 360 = xxlarge

      regex = Pattern.compile("<i>(.*?)</i>(.*?)<br>", Pattern.DOTALL); // get them as single trailer line
      m = regex.matcher(doc.getElementsByClass("clips").html());
      while (m.find()) {
        // LOGGER.info(doc.getElementsByClass("clips").html());
        // parse each line with 5 qualities
        String tname = m.group(1).trim();
        tname = tname.replaceFirst(":$", ""); // replace ending colon

        String urls = m.group(2);
        // url + format
        Pattern lr = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
        Matcher lm = lr.matcher(urls);
        while (lm.find()) {
          String turl = lm.group(1);
          String tpix = "";
          String tformat = lm.group(2).replaceAll("&nbsp;", "").trim();
          switch (tformat) {
            case "small":
              tpix = "90p";
              break;

            case "medium":
              tpix = "136p";
              break;

            case "large":
              tpix = "180p";
              break;

            case "xlarge":
              tpix = "226p";
              break;

            case "xxlarge":
              tpix = "360p";
              break;

            default:
              break;
          }
          MediaTrailer trailer = new MediaTrailer();
          trailer.setName(tname);
          // trailer.setQuality(tpix + " (" + tformat + ")");
          trailer.setQuality(tpix);
          trailer.setProvider("filmtrailer");
          trailer.setUrl(turl);
          trailer.setScrapedBy(getProviderInfo().getId());
          LOGGER.debug(trailer.toString());
          trailers.add(trailer);
        }
      }
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
    return trailers;
  }
}
