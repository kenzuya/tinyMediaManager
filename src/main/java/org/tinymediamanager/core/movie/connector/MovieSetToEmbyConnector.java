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

import org.tinymediamanager.core.movie.entities.MovieSet;
import org.w3c.dom.Element;

/**
 * this class is a general XML connector which suits as a base class for most xml based connectors
 *
 * @author Manuel Laggner
 */
public class MovieSetToEmbyConnector extends MovieSetGenericXmlConnector {
  public MovieSetToEmbyConnector(MovieSet movieSet) {
    super(movieSet);
  }

  @Override
  protected void addOwnTags() {
    // no own tags here

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

  @Override
  protected void addIds() {
    // write a <tmdbid> tag for emby
    Element tmdbid = document.createElement("tmdbid");
    if (movieSet.getTmdbId() > 0) {
      tmdbid.setTextContent(Integer.toString(movieSet.getTmdbId()));
    }
    root.appendChild(tmdbid);

    super.addIds();
  }
}
