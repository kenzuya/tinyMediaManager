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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link TvShowSubtitleLanguageFilter} is a filter for subtitle languages for TV shows
 * 
 * @author Wolfgang Janes
 */
public class TvShowSubtitleLanguageFilter extends AbstractCheckComboBoxTvShowUIFilter<String> {
  private final TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

  public TvShowSubtitleLanguageFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildSubtitleLanguageArray();
    tvShowList.addPropertyChangeListener(Constants.SUBTITLE_LANGUAGES, evt -> SwingUtilities.invokeLater(this::buildSubtitleLanguageArray));

  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

    List<String> selectedItems = checkComboBox.getSelectedItems();

    for (TvShowEpisode episode : episodes) {
      List<String> epLang = episode.getMediaInfoSubtitleLanguageList();
      for (String sel : selectedItems) {
        if (invert ^ epLang.contains(sel)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.subtitlelanguage"));
  }

  @Override
  public String getId() {
    return "tvShowSubtitleLanguage";
  }

  private void buildSubtitleLanguageArray() {
    List<String> subtitles = new ArrayList<>(tvShowList.getSubtitleLanguagesInEpisodes());
    Collections.sort(subtitles);
    setValues(subtitles);
  }
}
