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

import org.tinymediamanager.core.tvshow.entities.TvShowSeason;

/**
 * the class {@link TvShowSeasonToEmbyConnector} allows us to write Emby style TV show NFO files
 * 
 * @author Manuel Laggner
 */
public class TvShowSeasonToEmbyConnector extends TvShowSeasonGenericXmlConnector {
  public TvShowSeasonToEmbyConnector(TvShowSeason tvShowSeason) {
    super(tvShowSeason);
  }

  @Override
  protected void addOwnTags() {
    // nothing needed yet
  }
}
