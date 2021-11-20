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
package org.tinymediamanager.core.tvshow.connector;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.w3c.dom.Element;

/**
 * the class {@link TvShowEpisodeToEmbyConnector} is used to write a most recent Emby compatible NFO file
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeToEmbyConnector extends TvShowEpisodeToKodiConnector {

  public TvShowEpisodeToEmbyConnector(List<TvShowEpisode> episodes) {
    super(episodes);
  }

  @Override
  protected void addOwnTags(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    super.addOwnTags(episode, parser);

    addLockdata(episode, parser);

    // write highest episode number to tag, if multi-episode (only allowed on same season)
    // write em for all, since Emby only reads the first entry... and we don't care on reading ;)
    if (episode.isMultiEpisode()) {
      TvShowEpisode highest = new TvShowEpisode();
      for (TvShowEpisode tvShowEpisode : episodes) {
        if (tvShowEpisode.getSeason() > highest.getSeason()) {
          highest = tvShowEpisode;
        }
        if (tvShowEpisode.getSeason() == highest.getSeason() && tvShowEpisode.getEpisode() > highest.getEpisode()) {
          highest = tvShowEpisode;
        }
      }
      addEpisodeNumberEnd(highest, parser);
    }
  }

  /**
   * write the <episodenumberend> tag for Emby<br />
   * in case of a multi-episode, this will mark the highest episode number on all entries...<br>
   * see https://gitlab.com/tinyMediaManager/tinyMediaManager/-/issues/1444
   */
  protected void addEpisodeNumberEnd(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element episodenumberend = document.createElement("episodenumberend");
    episodenumberend.setTextContent(String.valueOf(episode.getEpisode()));

    root.appendChild(episodenumberend);
  }

  /**
   * write the <lockdata> tag for Emby<br />
   * This will be protect the NFO from being modified by emby
   */
  protected void addLockdata(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    Element lockdata = document.createElement("lockdata");
    lockdata.setTextContent("true");

    root.appendChild(lockdata);
  }

  /**
   * add directors in <director>xxx</director> tags (mulitple)
   */
  @Override
  protected void addDirectors(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    for (Person director : episode.getDirectors()) {
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
