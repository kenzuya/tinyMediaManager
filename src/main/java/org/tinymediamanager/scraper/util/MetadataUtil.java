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
package org.tinymediamanager.scraper.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;

/**
 * The class MetadataUtil. Here are some helper utils for managing meta data
 * 
 * @author Manuel Laggner
 * @since 1.0
 */
public class MetadataUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUtil.class);

  private MetadataUtil() {
    throw new IllegalAccessError();
  }

  /**
   * Return the best score for a title when compared to the search string. It uses 2 passes to find the best match. the first pass uses the matchTitle
   * as is, and the second pass uses the matchTitle will non search characters removed.
   * 
   * @param searchTitle
   *          the search title
   * @param matchTitle
   *          the match title
   * @return the best out of the 2 scored attempts
   */
  public static float calculateScore(String searchTitle, String matchTitle) {
    if (StringUtils.isAnyBlank(searchTitle, matchTitle)) {
      return 0;
    }

    float score1 = Similarity.compareStrings(searchTitle, matchTitle);
    float score2 = Similarity.compareStrings(searchTitle, removeNonSearchCharacters(matchTitle));
    float score3 = 0;
    if (searchTitle != null && searchTitle.matches(".* \\d{4}$")) { // ends with space+year
      score3 = Similarity.compareStrings(searchTitle.replaceFirst(" \\d{4}$", ""), matchTitle);
    }

    return Math.max(score1, Math.max(score3, score2));
  }

  /**
   * calculate a penalty for the score if the year from the search differs with the year of the result
   * 
   * @param searchYear
   *          the year from the search request
   * @param resultYear
   *          the year from the search result
   * @return the penalty 0...0.11 (0 for no year difference or no search year; 0.11 for the maximum difference of >100 years)
   */
  public static float calculateYearPenalty(int searchYear, int resultYear) {
    if (searchYear <= 1900) {
      // do not calculate a penalty if there is no year in the search given
      return 0;
    }

    if (resultYear == 0) {
      // no year in the result (due to incomplete data at the data provider?) - return a maximum of 0.11
      return 0.11f;
    }

    // calculate the year difference and adopt it to a max of 0.2 (the maximum difference of 100 years = 0.11)
    int diff = Math.abs(searchYear - resultYear);
    if (diff == 0) {
      return 0;
    }

    if (diff > 100) {
      return 0.11f;
    }

    return 0.01f + (diff / 1000.0f);
  }

  /**
   * Parses the running time.
   * 
   * @param in
   *          the in
   * @param regex
   *          the regex
   * @return the string
   */
  public static String parseRunningTime(String in, String regex) {
    if (in == null || regex == null) {
      return null;
    }
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(in);
    if (m.find()) {
      return m.group(1);
    }
    else {
      LOGGER.warn("Could not find Running Time in {}; using Regex: {}", in, regex);
      return null;
    }
  }

  /**
   * For the purposes of searching it will, keep only alpha numeric characters and '&.
   * 
   * @param s
   *          the s
   * @return the string
   */
  public static String removeNonSearchCharacters(String s) {
    if (s == null) {
      return null;
    }
    return s.replaceAll("[\\\\[\\\\]_.:|]", " ");
  }

  /**
   * parse a String for its integer value<br />
   * this method can parse normal integer values (e.g. 2001) as well as the style with digit separators (e.g. 2.001 or 2,001 or 2 001)
   *
   * @param intAsString
   *          the String to be parsed
   * @return the integer
   * @throws NumberFormatException
   *           an exception if none of the parsing methods worked
   */
  public static int parseInt(String intAsString) throws NumberFormatException, NullPointerException {
    // first try to parse that with the interal parsing logic
    try {
      return Integer.parseInt(intAsString);
    }
    catch (NumberFormatException e) {
      // did not work; try to remove digit separators
      // since we do not know for which locale the separators has been written, remove . and , and all whitespaces
      return Integer.parseInt(intAsString.replaceAll("[,\\.\\s]*", ""));
    }
  }

  public static int parseInt(Object intObj, int defaultValue) {
    if (intObj == null) {
      return defaultValue;
    }
    return parseInt(intObj.toString());
  }

  /**
   * parse a String for its integer value with a default value<br />
   * this method can parse normal integer values (e.g. 2001) as well as the style with digit separators (e.g. 2.001 or 2,001 or 2 001)
   *
   * @param intAsString
   *          the String to be parsed
   * @param defaultValue
   *          the default value to return
   * @return the integer
   */
  public static int parseInt(String intAsString, int defaultValue) {
    if (StringUtils.isBlank(intAsString)) {
      return defaultValue;
    }

    // first try to parse that with the interal parsing logic
    try {
      return parseInt(intAsString);
    }
    catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * safe unboxing of the given {@link Integer} or else return 0
   * 
   * @param original
   *          the integer to unbox
   * @return the int value of the {@link Integer} or 0 if null
   */
  public static int unboxInteger(Integer original) {
    return unboxInteger(original, 0);
  }

  /**
   * safe unboxing of the given {@link Integer} or else return the default value
   *
   * @param original
   *          the integer to unbox
   * @param defaultValue
   *          the default value if unboxing fails
   * @return the int value of the {@link Integer} or the default value if null
   */
  public static int unboxInteger(Integer original, int defaultValue) {
    return Optional.ofNullable(original).orElse(defaultValue);
  }

  /**
   * safe unboxing of the given {@link Long} or else return the default value
   *
   * @param original
   *          the long to unbox
   * @return the int value of the {@link Long} or the default value if null
   */
  public static long unboxLong(Long original) {
    return unboxLong(original, 0);
  }

  /**
   * safe unboxing of the given {@link Long} or else return the default value
   *
   * @param original
   *          the long to unbox
   * @param defaultValue
   *          the default value if unboxing fails
   * @return the int value of the {@link Long} or the default value if null
   */
  public static long unboxLong(Long original, long defaultValue) {
    return Optional.ofNullable(original).orElse(defaultValue);
  }

  /**
   * safe unboxing of the given {@link Float} or else return 0
   *
   * @param original
   *          the float to unbox
   * @return the int value of the {@link Float} or 0 if null
   */
  public static float unboxFloat(Float original) {
    return Optional.ofNullable(original).orElse(0f);
  }

  /**
   * safe unboxing of the given {@link Double} or else return 0
   *
   * @param original
   *          the double to unbox
   * @return the int value of the {@link Double} or 0 if null
   */
  public static double unboxDouble(Double original) {
    return Optional.ofNullable(original).orElse(0d);
  }

  /**
   * get the movie set id for the given movie id(s)
   * 
   * @return the movie set id or 0
   */
  public static int getMovieSetId(Map<String, Object> ids) {
    try {
      MovieSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
      options.setCertificationCountry(MovieModuleManager.getInstance().getSettings().getCertificationCountry());
      options.setReleaseDateCountry(MovieModuleManager.getInstance().getSettings().getReleaseDateCountry());
      options.setIds(new HashMap<>(ids));

      MediaMetadata mediaMetadata = ((IMovieMetadataProvider) MediaScraper.getMediaScraperById(MediaMetadata.TMDB, ScraperType.MOVIE)
          .getMediaProvider()).getMetadata(options);
      if (mediaMetadata.getIdAsInt(MediaMetadata.TMDB_SET) > 0) {
        return mediaMetadata.getIdAsInt(MediaMetadata.TMDB_SET);
      }
    }
    catch (Exception e) {
      LOGGER.debug("could not get movie set id - '{}'", e.getMessage());
    }
    return 0;
  }
}
