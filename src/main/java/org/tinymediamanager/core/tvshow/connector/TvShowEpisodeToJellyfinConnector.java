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

import java.util.List;

import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

/**
 * the class {@link TvShowEpisodeToJellyfinConnector} is used to write a most recent Jellyfin compatible NFO file
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeToJellyfinConnector extends TvShowEpisodeToKodiConnector {

  public TvShowEpisodeToJellyfinConnector(List<TvShowEpisode> episodes) {
    super(episodes);
  }

  @Override
  protected void addThumb(TvShowEpisode episode, TvShowEpisodeNfoParser.Episode parser) {
    // do not write any artwork urls
  }
}
