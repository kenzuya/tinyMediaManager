/*
 * Copyright 2012 - 2021 Manuel Laggner
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

package org.tinymediamanager.core.movie.connector;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.w3c.dom.Element;

/**
 * the class MovieToMpMyVideoConnector is used to write a classic MediaPortal 1.x compatible NFO file for the MyVideo plugin
 *
 * @author Manuel Laggner
 */
public class MovieToMpMyVideoConnector extends MovieGenericXmlConnector {

  public MovieToMpMyVideoConnector(Movie movie) {
    super(movie);
  }

  @Override
  protected void addOwnTags() {
    // no own tags needed in this format
  }

  /**
   * the media portal fanart style<br />
   * <fanart><thumb>xxx</thumb></fanart>
   */
  @Override
  protected void addFanart() {
    Element fanart = document.createElement("fanart");

    String fanarUrl = movie.getArtworkUrl(MediaFileType.FANART);
    if (StringUtils.isNotBlank(fanarUrl)) {
      Element thumb = document.createElement("thumb");
      thumb.setTextContent(fanarUrl);
      fanart.appendChild(thumb);
    }

    root.appendChild(fanart);
  }

  /**
   * the mpaa style in <mpaa>xxx</mpaa>
   */
  @Override
  protected void addMpaa() {
    Element mpaa = document.createElement("mpaa");
    if (movie.getCertification() != null) {
      mpaa.setTextContent(CertificationStyle.formatCertification(movie.getCertification(), MovieModuleManager.SETTINGS.getCertificationStyle()));
    }
    root.appendChild(mpaa);
  }

  /**
   * also add the imdb id as <imdb>xxx</imdb>
   */
  @Override
  protected void addId() {
    Element id = document.createElement("imdb");
    id.setTextContent(movie.getImdbId());
    root.appendChild(id);
    // add the default <id>
    super.addId();
  }

  /**
   * countries are concatenated in a single <country>xxx</country> tag, separated by /
   */
  @Override
  protected void addCountry() {
    Element country = document.createElement("country");

    List<String> countries = ParserUtils.split(movie.getCountry());
    country.setTextContent(StringUtils.join(countries, " / "));

    root.appendChild(country);
  }

  /**
   * studios are concatenated in a single <studio>xxx</studio> tag, separated by /
   */
  @Override
  protected void addStudios() {
    Element studio = document.createElement("studio");

    List<String> studios = ParserUtils.split(movie.getProductionCompany());
    // if we just want to write one studio, we have to strip that out
    if (MovieSettings.getInstance().isNfoWriteSingleStudio()) {
      if (!studios.isEmpty()) {
        studio.setTextContent(studios.get(0));
      }
    }
    else {
      studio.setTextContent(StringUtils.join(studios, " / "));
    }

    root.appendChild(studio);
  }

  /**
   * credits are concatenated in a single <credits>xxx</credits> tag, separated by ,
   */
  @Override
  protected void addCredits() {
    Element credits = document.createElement("credits");
    credits.setTextContent(movie.getWritersAsString());
    root.appendChild(credits);
  }

  /**
   * directors are concatenated in a single <director>xxx</director> tag, separated by ,
   */
  @Override
  protected void addDirectors() {
    Element director = document.createElement("director");
    director.setTextContent(movie.getDirectorsAsString());
    root.appendChild(director);
  }

  /**
   * languages are print in the UI language in a single <language>xxx</language> tagseparated by |
   */
  @Override
  protected void addLanguages() {
    // prepare spoken language for MP - try to extract the iso codes to the UI language separated by a pipe
    List<String> languages = new ArrayList<>();
    for (String langu : ParserUtils.split(movie.getSpokenLanguages())) {
      String translated = LanguageUtils.getLocalizedLanguageNameFromLocalizedString(MovieModuleManager.SETTINGS.getNfoLanguage().toLocale(),
          langu.trim());
      languages.add(translated);
    }

    Element element = document.createElement("language");
    element.setTextContent(StringUtils.join(languages, '|'));
    root.appendChild(element);
  }
}
