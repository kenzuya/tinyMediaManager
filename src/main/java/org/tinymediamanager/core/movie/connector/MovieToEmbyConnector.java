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
package org.tinymediamanager.core.movie.connector;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.MediaMetadata;
import org.w3c.dom.Element;

/**
 * the class {@link MovieToEmbyConnector} is used to write a most recent Emby compatible NFO file
 *
 * @author Manuel Laggner
 */
public class MovieToEmbyConnector extends MovieToKodiConnector {

  public MovieToEmbyConnector(Movie movie) {
    super(movie);
  }

  @Override
  protected void addOwnTags() {
    super.addOwnTags();

    addLockdata();
  }

  /**
   * write the <lockdata> tag for Emby<br />
   * This will be protect the NFO from being modified by emby
   */
  protected void addLockdata() {
    Element lockdata = document.createElement("lockdata");
    lockdata.setTextContent("true");

    root.appendChild(lockdata);
  }

  /**
   * add directors in <director>xxx</director> tags (mulitple)
   */
  @Override
  protected void addDirectors() {
    for (Person director : movie.getDirectors()) {
      Element element = document.createElement("director");

      // imdb id
      String imdbId = director.getIdAsString(MediaMetadata.IMDB);
      if (StringUtils.isNotBlank(imdbId)) {
        element.setAttribute("imdbid", imdbId);
      }

      // tmdb id
      int tmdbid = director.getIdAsInt(MediaMetadata.TMDB);
      if (tmdbid > 0) {
        element.setAttribute("tmdbid", String.valueOf(tmdbid));
      }

      element.setTextContent(director.getName());
      root.appendChild(element);
    }
  }
}
