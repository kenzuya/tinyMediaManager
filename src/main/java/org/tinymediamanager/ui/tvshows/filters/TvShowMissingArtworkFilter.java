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
package org.tinymediamanager.ui.tvshows.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * This class implements a "missing artwork" filter for the TV show tree
 * 
 * @author Manuel Laggner
 */
public class TvShowMissingArtworkFilter extends AbstractCheckComboBoxTvShowUIFilter<TvShowMissingArtworkFilter.MetadataField> {

  private final TvShowList tvShowList;

  public TvShowMissingArtworkFilter() {
    super();
    tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));

    // initial filling
    List<MetadataField> values = new ArrayList<>();
    for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
      if (config.isArtwork()) {
        values.add(new MetadataField(config));
      }
    }
    setValues(values);
  }

  @Override
  public String getId() {
    return "tvShowMissingArtwork";
  }

  @Override
  protected String parseTypeToString(MetadataField type) throws Exception {
    return type.config.name();
  }

  @Override
  protected MetadataField parseStringToType(String string) throws Exception {
    try {
      return new MetadataField(TvShowScraperMetadataConfig.valueOf(string));
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<TvShowScraperMetadataConfig> tvShowValues = new ArrayList<>();
    List<TvShowEpisodeScraperMetadataConfig> episodeValues = new ArrayList<>();
    for (MetadataField metadataField : checkComboBox.getSelectedItems()) {
      tvShowValues.add(metadataField.config);
      if (metadataField.config == TvShowScraperMetadataConfig.THUMB) {
        episodeValues.add(TvShowEpisodeScraperMetadataConfig.THUMB);
      }
    }

    if (invert ^ !tvShowList.detectMissingFields(tvShow, tvShowValues).isEmpty()) {
      return true;
    }

    for (TvShowEpisode episode : episodes) {
      if (episode.isDummy()) {
        continue;
      }

      if (invert ^ !tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.missingartwork"));
  }

  public static class MetadataField {
    private final TvShowScraperMetadataConfig config;

    public MetadataField(TvShowScraperMetadataConfig config) {
      this.config = config;
    }

    @Override
    public String toString() {
      return config.getDescription();
    }
  }
}
