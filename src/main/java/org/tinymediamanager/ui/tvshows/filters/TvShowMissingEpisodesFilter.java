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
package org.tinymediamanager.ui.tvshows.filters;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a "missing episodes" filter for the TV show tree
 * 
 * @author Manuel Laggner
 */
public class TvShowMissingEpisodesFilter extends AbstractTvShowUIFilter {

  @Override
  public String getId() {
    return "tvShowMissingEpisodes";
  }

  @Override
  public String getFilterValueAsString() {
    return null;
  }

  @Override
  public void setFilterValue(Object value) {
    // nothing to do
  }

  @Override
  public void clearFilter() {
    // nothing to do
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    for (TvShowEpisode episode : episodes) {
      if (invert ^ episode.isDummy()) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("tvshowextendedsearch.missingepisodes"));
  }

  @Override
  protected JComponent createFilterComponent() {
    return null;
  }
}
