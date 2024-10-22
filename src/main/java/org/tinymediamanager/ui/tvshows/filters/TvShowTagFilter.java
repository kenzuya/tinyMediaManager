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

import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTableFormat;

/**
 * This class implements a tag filter for the TV show tree
 * 
 * @author Manuel Laggner
 */
public class TvShowTagFilter extends AbstractCheckComboBoxTvShowUIFilter<String> {
  private final TmmTableFormat.StringComparator comparator;

  private final TvShowList                      tvShowList = TvShowModuleManager.getInstance().getTvShowList();
  private final Set<String>                     oldTags    = new HashSet<>();

  public TvShowTagFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    comparator = new TmmTableFormat.StringComparator();
    buildAndInstallTagsArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallTagsArray();
    tvShowList.addPropertyChangeListener(Constants.TAGS, propertyChangeListener);
  }

  @Override
  public String getId() {
    return "tvShowTag";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<String> tags = checkComboBox.getSelectedItems();

    // check for explicit empty search
    if (!invert && (tags.isEmpty() && tvShow.getTags().isEmpty())) {
      return true;
    }
    else if (invert && (tags.isEmpty() && !tvShow.getTags().isEmpty())) {
      return true;
    }

    for (TvShowEpisode episode : episodes) {
      if (!invert && (tags.isEmpty() && episode.getTags().isEmpty())) {
        return true;
      }
      else if (invert && (tags.isEmpty() && !episode.getTags().isEmpty())) {
        return true;
      }
    }

    // search tags of the show
    for (String tag : tags) {
      boolean containsTags = tvShow.getTags().contains(tag);
      if (!invert && containsTags) {
        return true;
      }
      else if (invert && containsTags) {
        return false;
      }

      for (TvShowEpisode episode : episodes) {
        if (invert ^ episode.getTags().contains(tag)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.tag"));
  }

  private void buildAndInstallTagsArray() {
    // do it lazy because otherwise there is too much UI overhead
    // also use a set for faster lookups
    boolean dirty = false;
    Set<String> tags = new HashSet<>(tvShowList.getTagsInTvShows());
    tags.addAll(tvShowList.getTagsInEpisodes());

    if (oldTags.size() != tags.size()) {
      dirty = true;
    }

    if (!oldTags.containsAll(tags) || !tags.containsAll(oldTags)) {
      dirty = true;
    }

    if (dirty) {
      oldTags.clear();
      oldTags.addAll(tags);

      setValues(ListUtils.asSortedList(tags, comparator));
    }
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }
}
