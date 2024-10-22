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

package org.tinymediamanager.core.movie.connector;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.w3c.dom.Element;

/**
 * the class MovieToMpLegacyConnector is used to write a classic MediaPortal 1.x compatible NFO file. This is the legacy format (dunno is anyone uses
 * this any more)
 *
 * @author Manuel Laggner
 */
public class MovieToMpLegacyConnector extends MovieGenericXmlConnector {

  public MovieToMpLegacyConnector(Movie movie) {
    super(movie);
  }

  @Override
  protected void addOwnTags() {
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
   * no MPAA tag
   */
  @Override
  protected void addMpaa() {
    // no MPAA tag
  }

  /**
   * genres are nested in a genre tag<br />
   * <genres><genre>xxx</genre></genres>
   */
  @Override
  protected void addGenres() {
    Element genres = document.createElement("genres");

    for (MediaGenres mediaGenre : movie.getGenres()) {
      Element genre = document.createElement("genre");
      genre.setTextContent(mediaGenre.getLocalizedName(MovieModuleManager.getInstance().getSettings().getNfoLanguage()));
      genres.appendChild(genre);
    }

    root.appendChild(genres);
  }

  /**
   * countries are concatenated in a single <country>xxx</country> tag
   */
  @Override
  protected void addCountry() {
    Element country = document.createElement("country");
    country.setTextContent(movie.getCountry());
    root.appendChild(country);
  }

  /**
   * studios are concatenated in a single <studio>xxx</studio> tag
   */
  @Override
  protected void addStudios() {
    Element studio = document.createElement("studio");
    // if we just want to write one studio, we have to strip that out
    if (MovieModuleManager.getInstance().getSettings().isNfoWriteSingleStudio()) {
      List<String> studios = ParserUtils.split(movie.getProductionCompany());
      if (!studios.isEmpty()) {
        studio.setTextContent(studios.get(0));
      }
    }
    else {
      studio.setTextContent(movie.getProductionCompany());
    }
    root.appendChild(studio);
  }

  /**
   * credits are concatenated in a single <credits>xxx</credits> tag
   */
  @Override
  protected void addCredits() {
    Element credits = document.createElement("credits");
    credits.setTextContent(movie.getWritersAsString());
    root.appendChild(credits);
  }

  /**
   * directors are concatenated in a single <director>xxx</director> tag
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
      String translated = LanguageUtils.getLocalizedLanguageNameFromLocalizedString(MovieModuleManager.getInstance().getSettings().getNfoLanguage(),
          langu.strip());
      languages.add(translated);
    }

    Element element = document.createElement("languages");
    element.setTextContent(StringUtils.join(languages, '|'));
    root.appendChild(element);
  }
}
