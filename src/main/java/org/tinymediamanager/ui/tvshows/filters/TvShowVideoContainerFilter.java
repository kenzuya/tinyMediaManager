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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a video container filter for the TV show tree
 * 
 * @author Manuel Laggner
 */
public class TvShowVideoContainerFilter extends AbstractCheckComboBoxTvShowUIFilter<String> {
  private final TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

  public TvShowVideoContainerFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildAndInstallContainerArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallContainerArray();
    tvShowList.addPropertyChangeListener(Constants.VIDEO_CONTAINER, propertyChangeListener);
  }

  @Override
  public String getId() {
    return "tvShowVideoContainer";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<String> selectedValues = checkComboBox.getSelectedItems();

    // search container in the episodes
    for (TvShowEpisode episode : episodes) {
      String container = episode.getMediaInfoContainerFormat();

      for (String value : selectedValues) {
        if (invert ^ value.equalsIgnoreCase(container)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.container"));
  }

  private void buildAndInstallContainerArray() {
    List<String> containers = new ArrayList<>(tvShowList.getVideoContainersInEpisodes());
    Collections.sort(containers);

    setValues(containers);
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
