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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a genres filter for the TV show tree
 * 
 * @author Manuel Laggner
 */
public class TvShowGenreFilter extends AbstractCheckComboBoxTvShowUIFilter<MediaGenres> {

  public TvShowGenreFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildAndInstallMediaGenres();
    MediaGenres.addListener(evt -> SwingUtilities.invokeLater(this::buildAndInstallMediaGenres));
  }

  @Override
  public String getId() {
    return "tvShowGenre";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<MediaGenres> selectedItems = checkComboBox.getSelectedItems();

    // check for explicit empty search
    if (!invert && (selectedItems.isEmpty() && tvShow.getGenres().isEmpty())) {
      return true;
    }
    else if (invert && (selectedItems.isEmpty() && !tvShow.getGenres().isEmpty())) {
      return true;
    }

    // n:m match is kinda hard
    // if we want a "direct" search, the TV show entries must contain any of the given list
    // if we want a negative search, the TV show entries must not contain all of the given list
    return invert == Collections.disjoint(selectedItems, tvShow.getGenres());
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.genre"));
  }

  private void buildAndInstallMediaGenres() {
    setValues(MediaGenres.values());
  }

  @Override
  protected String parseTypeToString(MediaGenres type) throws Exception {
    return type.name();
  }

  @Override
  protected MediaGenres parseStringToType(String string) throws Exception {
    return MediaGenres.getGenre(string);
  }
}
