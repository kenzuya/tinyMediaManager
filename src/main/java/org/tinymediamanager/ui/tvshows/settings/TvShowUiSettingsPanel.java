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
package org.tinymediamanager.ui.tvshows.settings;

import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.ACTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.COUNTRY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.DIRECTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.FILENAME;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.NOTE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCTION_COMPANY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.SPOKEN_LANGUAGE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.TAGS;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.WRITER;
import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.JHintLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowUiSettingsPanel} is used to show TV show related UI settings.
 * 
 * @author Manuel Laggner
 */
class TvShowUiSettingsPanel extends JPanel {
  private static final int                                         COL_COUNT = 7;

  private final TvShowSettings                                     settings  = TvShowModuleManager.getInstance().getSettings();
  private final ItemListener                                       checkBoxListener;
  private JCheckBox                                                chckbxShowMissingEpisodes;
  private AutocompleteComboBox<String>                             cbRating;
  private JCheckBox                                                chckbxShowMissingSpecials;
  private JCheckBox                                                chckbxTvShowTableTooltips;
  private JCheckBox                                                chckbxSeasonArtworkFallback;
  private JCheckBox                                                chckbxStoreFilter;

  private JCheckBox                                                chckbxNode;
  private JCheckBox                                                chckbxTitle;
  private JCheckBox                                                chckbxOriginalTitle;

  private JCheckBox                                                chckbxUniversalNote;
  private JCheckBox                                                chckbxUniversalFilename;
  private JCheckBox                                                chckbxUniversalTags;
  private JCheckBox                                                chckbxUniversalProductionCompany;
  private JCheckBox                                                chckbxUniversalCountry;
  private JCheckBox                                                chckbxUniversalLanguages;
  private JCheckBox                                                chckbxUniversalActors;
  private JCheckBox                                                chckbxUniversalDirectors;
  private JCheckBox                                                chckbxUniversalWriters;

  private final Map<TvShowScraperMetadataConfig, JCheckBox>        tvShowMetadataCheckBoxes;
  private final Map<TvShowEpisodeScraperMetadataConfig, JCheckBox> episodeMetadataCheckBoxes;
  private JHintCheckBox                                            chckbxTvShowDisplayAllMissingMetadata;
  private JHintCheckBox                                            chckbxEpisodeDisplayAllMissingMetadata;
  private JCheckBox                                                chckbxEpisodeSpecialsCheckMissingMetadata;
  private final Map<TvShowScraperMetadataConfig, JCheckBox>        tvShowArtworkCheckBoxes;
  private final Map<TvShowScraperMetadataConfig, JCheckBox>        seasonArtworkCheckBoxes;
  private final Map<TvShowEpisodeScraperMetadataConfig, JCheckBox> episodeArtworkCheckBoxes;
  private JHintCheckBox                                            chckbxTvShowDisplayAllMissingArtwork;
  private JHintCheckBox                                            chckbxSeasonDisplayAllMissingArtwork;
  private JHintCheckBox                                            chckbxEpisodeDisplayAllMissingArtwork;
  private JCheckBox                                                chckbxEpisodeSpecialsCheckMissingArtwork;
  private JCheckBox                                                chckbxTvShowPoster;
  private JCheckBox                                                chckbxTvShowFanart;
  private JCheckBox                                                chckbxTvShowBanner;
  private JCheckBox                                                chckbxTvShowThumb;
  private JCheckBox                                                chckbxTvShowClearlogo;
  private JCheckBox                                                chckbxSeasonPoster;
  private JCheckBox                                                chckbxEpisodePoster;
  private JCheckBox                                                chckbxEpisodeThumb;
  private JCheckBox                                                chckbxSeasonThumb;
  private JCheckBox                                                chckbxSeasonBanner;
  private JCheckBox                                                chckbxSeasonFanart;
  private JCheckBox                                                chckbxIncludeNotAired;

  /**
   * Instantiates a new tv show settings panel.
   */
  TvShowUiSettingsPanel() {
    tvShowMetadataCheckBoxes = new LinkedHashMap<>();
    episodeMetadataCheckBoxes = new LinkedHashMap<>();
    tvShowArtworkCheckBoxes = new LinkedHashMap<>();
    seasonArtworkCheckBoxes = new LinkedHashMap<>();
    episodeArtworkCheckBoxes = new LinkedHashMap<>();
    checkBoxListener = e -> checkChanges();

    // UI initializations
    initComponents();
    initDataBindings();

    buildCheckBoxes();
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    // show artwork
    List<MediaFileType> tvShowArtworkTypes = new ArrayList<>();
    if (chckbxTvShowPoster.isSelected()) {
      tvShowArtworkTypes.add(MediaFileType.POSTER);
    }
    if (chckbxTvShowFanart.isSelected()) {
      tvShowArtworkTypes.add(MediaFileType.FANART);
    }
    if (chckbxTvShowBanner.isSelected()) {
      tvShowArtworkTypes.add(MediaFileType.BANNER);
    }
    if (chckbxTvShowThumb.isSelected()) {
      tvShowArtworkTypes.add(MediaFileType.THUMB);
    }
    if (chckbxTvShowClearlogo.isSelected()) {
      tvShowArtworkTypes.add(MediaFileType.CLEARLOGO);
    }
    settings.setShowTvShowArtworkTypes(tvShowArtworkTypes);

    List<MediaFileType> seasonArtworkTypes = new ArrayList<>();
    if (chckbxSeasonPoster.isSelected()) {
      seasonArtworkTypes.add(MediaFileType.SEASON_POSTER);
    }
    if (chckbxSeasonFanart.isSelected()) {
      seasonArtworkTypes.add(MediaFileType.SEASON_FANART);
    }
    if (chckbxSeasonBanner.isSelected()) {
      seasonArtworkTypes.add(MediaFileType.SEASON_BANNER);
    }
    if (chckbxSeasonThumb.isSelected()) {
      seasonArtworkTypes.add(MediaFileType.SEASON_THUMB);
    }
    settings.setShowSeasonArtworkTypes(seasonArtworkTypes);

    List<MediaFileType> episodeArtworkTypes = new ArrayList<>();
    if (chckbxEpisodePoster.isSelected()) {
      episodeArtworkTypes.add(MediaFileType.SEASON_POSTER);
    }
    if (chckbxEpisodeThumb.isSelected()) {
      episodeArtworkTypes.add(MediaFileType.THUMB);
    }
    settings.setShowEpisodeArtworkTypes(episodeArtworkTypes);

    // universal filter
    List<AbstractSettings.UniversalFilterFields> universalFilterFields = new ArrayList<>();
    if (chckbxUniversalNote.isSelected()) {
      universalFilterFields.add(NOTE);
    }
    if (chckbxUniversalFilename.isSelected()) {
      universalFilterFields.add(FILENAME);
    }
    if (chckbxUniversalTags.isSelected()) {
      universalFilterFields.add(TAGS);
    }
    if (chckbxUniversalCountry.isSelected()) {
      universalFilterFields.add(COUNTRY);
    }
    if (chckbxUniversalProductionCompany.isSelected()) {
      universalFilterFields.add(PRODUCTION_COMPANY);
    }
    if (chckbxUniversalLanguages.isSelected()) {
      universalFilterFields.add(SPOKEN_LANGUAGE);
    }
    if (chckbxUniversalActors.isSelected()) {
      universalFilterFields.add(ACTOR);
    }
    if (chckbxUniversalWriters.isSelected()) {
      universalFilterFields.add(WRITER);
    }
    if (chckbxUniversalDirectors.isSelected()) {
      universalFilterFields.add(DIRECTOR);
    }
    settings.setUniversalFilterFields(universalFilterFields);

    // TV show
    // metadata
    settings.clearTvShowCheckMetadata();
    for (Map.Entry<TvShowScraperMetadataConfig, JCheckBox> entry : tvShowMetadataCheckBoxes.entrySet()) {
      TvShowScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addTvShowCheckMetadata(key);
      }
    }

    // artwork
    settings.clearTvShowCheckArtwork();
    for (Map.Entry<TvShowScraperMetadataConfig, JCheckBox> entry : tvShowArtworkCheckBoxes.entrySet()) {
      TvShowScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addTvShowCheckArtwork(key);
      }
    }

    // season
    // artwork
    settings.clearSeasonCheckArtwork();
    for (Map.Entry<TvShowScraperMetadataConfig, JCheckBox> entry : seasonArtworkCheckBoxes.entrySet()) {
      TvShowScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addSeasonCheckArtwork(key);
      }
    }

    // episode
    // metadata
    settings.clearEpisodeCheckMetadata();
    for (Map.Entry<TvShowEpisodeScraperMetadataConfig, JCheckBox> entry : episodeMetadataCheckBoxes.entrySet()) {
      TvShowEpisodeScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addEpisodeCheckMetadata(key);
      }
    }

    // artwork
    settings.clearEpisodeCheckArtwork();
    for (Map.Entry<TvShowEpisodeScraperMetadataConfig, JCheckBox> entry : episodeArtworkCheckBoxes.entrySet()) {
      TvShowEpisodeScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addEpisodeCheckArtwork(key);
      }
    }
  }

  private void buildCheckBoxes() {
    // show artwork
    for (MediaFileType artworkType : settings.getShowTvShowArtworkTypes()) {
      switch (artworkType) {
        case POSTER:
          chckbxTvShowPoster.setSelected(true);
          break;

        case FANART:
          chckbxTvShowFanart.setSelected(true);
          break;

        case BANNER:
          chckbxTvShowBanner.setSelected(true);
          break;

        case THUMB:
          chckbxTvShowThumb.setSelected(true);
          break;

        case CLEARLOGO:
          chckbxTvShowClearlogo.setSelected(true);
      }
    }

    for (MediaFileType artworkType : settings.getShowSeasonArtworkTypes()) {
      switch (artworkType) {
        case SEASON_POSTER:
        case POSTER:
          chckbxSeasonPoster.setSelected(true);
          break;

        case SEASON_FANART:
        case FANART:
          chckbxSeasonFanart.setSelected(true);
          break;

        case SEASON_BANNER:
        case BANNER:
          chckbxSeasonBanner.setSelected(true);
          break;

        case SEASON_THUMB:
        case THUMB:
          chckbxSeasonThumb.setSelected(true);
          break;
      }
    }

    for (MediaFileType artworkType : settings.getShowEpisodeArtworkTypes()) {
      switch (artworkType) {
        case SEASON_POSTER:
        case POSTER:
          chckbxEpisodePoster.setSelected(true);
          break;

        case THUMB:
          chckbxEpisodeThumb.setSelected(true);
          break;
      }
    }

    // universal filter
    for (AbstractSettings.UniversalFilterFields filterField : settings.getUniversalFilterFields()) {
      switch (filterField) {
        case NOTE:
          chckbxUniversalNote.setSelected(true);
          break;

        case FILENAME:
          chckbxUniversalFilename.setSelected(true);
          break;

        case TAGS:
          chckbxUniversalTags.setSelected(true);
          break;

        case PRODUCTION_COMPANY:
          chckbxUniversalProductionCompany.setSelected(true);
          break;

        case COUNTRY:
          chckbxUniversalCountry.setSelected(true);
          break;

        case SPOKEN_LANGUAGE:
          chckbxUniversalLanguages.setSelected(true);
          break;

        case ACTOR:
          chckbxUniversalActors.setSelected(true);
          break;

        case DIRECTOR:
          chckbxUniversalDirectors.setSelected(true);
          break;

        case WRITER:
          chckbxUniversalWriters.setSelected(true);
          break;
      }
    }

    // set the checkbox listener at the end!
    chckbxTvShowPoster.addItemListener(checkBoxListener);
    chckbxTvShowFanart.addItemListener(checkBoxListener);
    chckbxTvShowBanner.addItemListener(checkBoxListener);
    chckbxTvShowThumb.addItemListener(checkBoxListener);
    chckbxTvShowClearlogo.addItemListener(checkBoxListener);
    chckbxSeasonPoster.addItemListener(checkBoxListener);
    chckbxSeasonFanart.addItemListener(checkBoxListener);
    chckbxSeasonBanner.addItemListener(checkBoxListener);
    chckbxSeasonThumb.addItemListener(checkBoxListener);
    chckbxEpisodePoster.addItemListener(checkBoxListener);
    chckbxEpisodeThumb.addItemListener(checkBoxListener);
    chckbxUniversalNote.addItemListener(checkBoxListener);
    chckbxUniversalFilename.addItemListener(checkBoxListener);
    chckbxUniversalTags.addItemListener(checkBoxListener);
    chckbxUniversalProductionCompany.addItemListener(checkBoxListener);
    chckbxUniversalCountry.addItemListener(checkBoxListener);
    chckbxUniversalLanguages.addItemListener(checkBoxListener);
    chckbxUniversalActors.addItemListener(checkBoxListener);
    chckbxUniversalDirectors.addItemListener(checkBoxListener);
    chckbxUniversalWriters.addItemListener(checkBoxListener);

    // TV Show
    // metadata
    for (TvShowScraperMetadataConfig value : settings.getTvShowCheckMetadata()) {
      JCheckBox checkBox = tvShowMetadataCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    for (JCheckBox checkBox : tvShowMetadataCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }

    // artwork
    for (TvShowScraperMetadataConfig value : settings.getTvShowCheckArtwork()) {
      JCheckBox checkBox = tvShowArtworkCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    for (JCheckBox checkBox : tvShowArtworkCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }

    // season
    // artwork
    for (TvShowScraperMetadataConfig value : settings.getSeasonCheckArtwork()) {
      JCheckBox checkBox = seasonArtworkCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    for (JCheckBox checkBox : seasonArtworkCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }

    // episode
    // metadata
    for (TvShowEpisodeScraperMetadataConfig value : settings.getEpisodeCheckMetadata()) {
      JCheckBox checkBox = episodeMetadataCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    for (JCheckBox checkBox : episodeMetadataCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }

    // artwork
    for (TvShowEpisodeScraperMetadataConfig value : settings.getEpisodeCheckArtwork()) {
      JCheckBox checkBox = episodeArtworkCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    for (JCheckBox checkBox : episodeArtworkCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      // 16lp ~ width of the checkbox
      panelUiSettings.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][10lp!][][grow][10lp!][][][10lp!][][]"));

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");

      {
        JLabel lblNewLabel = new JLabel(TmmResourceBundle.getString("Settings.showartworktypes"));
        panelUiSettings.add(lblNewLabel, "cell 1 0 2 1");

        JPanel panelShowArtwork = new JPanel();
        panelUiSettings.add(panelShowArtwork, "cell 2 1,grow");
        panelShowArtwork.setLayout(new MigLayout("", "[][][][][][]", "[][][]"));

        JLabel lblTvShowArtwork = new JLabel(TmmResourceBundle.getString("metatag.tvshow"));
        panelShowArtwork.add(lblTvShowArtwork, "flowy,cell 0 0");

        chckbxTvShowPoster = new JCheckBox(TmmResourceBundle.getString("mediafiletype.poster"));
        panelShowArtwork.add(chckbxTvShowPoster, "cell 1 0");

        chckbxTvShowFanart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.fanart"));
        panelShowArtwork.add(chckbxTvShowFanart, "cell 2 0");

        chckbxTvShowBanner = new JCheckBox(TmmResourceBundle.getString("mediafiletype.banner"));
        panelShowArtwork.add(chckbxTvShowBanner, "cell 3 0");

        chckbxTvShowThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
        panelShowArtwork.add(chckbxTvShowThumb, "cell 4 0");

        chckbxTvShowClearlogo = new JCheckBox(TmmResourceBundle.getString("mediafiletype.clearlogo"));
        panelShowArtwork.add(chckbxTvShowClearlogo, "cell 5 0");

        JLabel lblSeasonArtwork = new JLabel(TmmResourceBundle.getString("metatag.season"));
        panelShowArtwork.add(lblSeasonArtwork, "flowy,cell 0 1");

        chckbxSeasonPoster = new JCheckBox(TmmResourceBundle.getString("mediafiletype.poster"));
        panelShowArtwork.add(chckbxSeasonPoster, "cell 1 1");

        chckbxSeasonFanart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.fanart"));
        panelShowArtwork.add(chckbxSeasonFanart, "cell 2 1");

        chckbxSeasonBanner = new JCheckBox(TmmResourceBundle.getString("mediafiletype.banner"));
        panelShowArtwork.add(chckbxSeasonBanner, "cell 3 1");

        chckbxSeasonThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
        panelShowArtwork.add(chckbxSeasonThumb, "cell 4 1");

        JLabel lblEpisodeArtwork = new JLabel(TmmResourceBundle.getString("metatag.episode"));
        panelShowArtwork.add(lblEpisodeArtwork, "cell 0 2");

        chckbxEpisodePoster = new JCheckBox(TmmResourceBundle.getString("mediafiletype.season_poster"));
        panelShowArtwork.add(chckbxEpisodePoster, "cell 1 2");

        chckbxEpisodeThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
        panelShowArtwork.add(chckbxEpisodeThumb, "cell 2 2");
      }
      {
        chckbxShowMissingEpisodes = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingepisodes"));
        panelUiSettings.add(chckbxShowMissingEpisodes, "cell 1 2 2 1");

        chckbxShowMissingSpecials = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingespecials"));
        panelUiSettings.add(chckbxShowMissingSpecials, "cell 2 3");

        chckbxIncludeNotAired = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingnotaired"));
        panelUiSettings.add(chckbxIncludeNotAired, "cell 2 4");

        chckbxTvShowTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.showtabletooltips"));
        panelUiSettings.add(chckbxTvShowTableTooltips, "cell 1 5 2 1");

        {
          JLabel lblCheckMetadata = new JLabel(TmmResourceBundle.getString("Settings.checkmetadata"));
          panelUiSettings.add(lblCheckMetadata, "cell 1 7 2 1");

          JPanel panelCheckMetadata = new JPanel(new GridBagLayout());

          GridBagConstraints gbc = new GridBagConstraints();
          gbc.gridx = 0;
          gbc.gridy = 0;
          gbc.anchor = GridBagConstraints.LINE_START;
          gbc.ipadx = 10;

          // TV show
          JLabel lblTvShow = new TmmLabel(TmmResourceBundle.getString("metatag.tvshow"));
          panelCheckMetadata.add(lblTvShow, gbc.clone());

          gbc.gridx++;

          // Metadata
          for (TvShowScraperMetadataConfig value : TvShowScraperMetadataConfig.values()) {
            if (value.isMetaData()) {
              addMetadataCheckbox(panelCheckMetadata, value, tvShowMetadataCheckBoxes, gbc);
            }
          }

          // cast
          gbc.gridx = 1;
          gbc.gridy++;
          for (TvShowScraperMetadataConfig value : TvShowScraperMetadataConfig.values()) {
            if (value.isCast()) {
              addMetadataCheckbox(panelCheckMetadata, value, tvShowMetadataCheckBoxes, gbc);
            }
          }

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckMetadata.add(Box.createVerticalStrut(5), gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          gbc.gridwidth = COL_COUNT;
          chckbxTvShowDisplayAllMissingMetadata = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkmetadata.displayall"));
          chckbxTvShowDisplayAllMissingMetadata.setToolTipText(TmmResourceBundle.getString("Settings.checkmetadata.displayall.desc"));
          chckbxTvShowDisplayAllMissingMetadata.setHintIcon(IconManager.HINT);
          panelCheckMetadata.add(chckbxTvShowDisplayAllMissingMetadata, gbc.clone());

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          gbc.gridwidth = 1;
          panelCheckMetadata.add(Box.createVerticalStrut(10), gbc.clone());

          // Episode
          gbc.gridx = 0;
          gbc.gridy++;

          JLabel lblEpisode = new TmmLabel(TmmResourceBundle.getString("metatag.episode"));
          panelCheckMetadata.add(lblEpisode, gbc.clone());

          gbc.gridx++;

          // Metadata
          for (TvShowEpisodeScraperMetadataConfig value : TvShowEpisodeScraperMetadataConfig.values()) {
            if (value.isMetaData()) {
              addMetadataCheckbox(panelCheckMetadata, value, episodeMetadataCheckBoxes, gbc);
            }
          }

          // cast
          gbc.gridx = 1;
          gbc.gridy++;
          for (TvShowEpisodeScraperMetadataConfig value : TvShowEpisodeScraperMetadataConfig.values()) {
            if (value.isCast()) {
              addMetadataCheckbox(panelCheckMetadata, value, episodeMetadataCheckBoxes, gbc);
            }
          }

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckMetadata.add(Box.createVerticalStrut(5), gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          gbc.gridwidth = COL_COUNT;
          chckbxEpisodeDisplayAllMissingMetadata = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkmetadata.displayall"));
          chckbxEpisodeDisplayAllMissingMetadata.setToolTipText(TmmResourceBundle.getString("Settings.checkmetadata.displayall.desc"));
          chckbxEpisodeDisplayAllMissingMetadata.setHintIcon(IconManager.HINT);
          panelCheckMetadata.add(chckbxEpisodeDisplayAllMissingMetadata, gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          gbc.gridwidth = COL_COUNT;
          chckbxEpisodeSpecialsCheckMissingMetadata = new JCheckBox(TmmResourceBundle.getString("tvshowepisode.checkmetadata.specials"));
          panelCheckMetadata.add(chckbxEpisodeSpecialsCheckMissingMetadata, gbc.clone());

          panelUiSettings.add(panelCheckMetadata, "cell 2 8");
        }
        {
          JLabel lblCheckArtwork = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
          panelUiSettings.add(lblCheckArtwork, "cell 1 10 2 1");

          JPanel panelCheckArtwork = new JPanel(new GridBagLayout());

          GridBagConstraints gbc = new GridBagConstraints();
          gbc.gridx = 0;
          gbc.gridy = 0;
          gbc.anchor = GridBagConstraints.LINE_START;
          gbc.ipadx = 10;

          // TV show
          JLabel lblTvShow = new TmmLabel(TmmResourceBundle.getString("metatag.tvshow"));
          panelCheckArtwork.add(lblTvShow, gbc.clone());

          gbc.gridx++;

          for (TvShowScraperMetadataConfig value : TvShowScraperMetadataConfig.values()) {
            if (value.isArtwork() && !value.name().startsWith("SEASON_")) {
              addMetadataCheckbox(panelCheckArtwork, value, tvShowArtworkCheckBoxes, gbc);
            }
          }

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckArtwork.add(Box.createVerticalStrut(5), gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          gbc.gridwidth = COL_COUNT;
          chckbxTvShowDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
          chckbxTvShowDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
          chckbxTvShowDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
          panelCheckArtwork.add(chckbxTvShowDisplayAllMissingArtwork, gbc.clone());

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          gbc.gridwidth = 1;
          panelCheckArtwork.add(Box.createVerticalStrut(10), gbc.clone());

          // Season
          gbc.gridx = 0;
          gbc.gridy++;

          JLabel lblSeason = new TmmLabel(TmmResourceBundle.getString("metatag.season"));
          panelCheckArtwork.add(lblSeason, gbc.clone());

          gbc.gridx++;

          for (TvShowScraperMetadataConfig value : TvShowScraperMetadataConfig.values()) {
            if (value.isArtwork() && value.name().startsWith("SEASON_")) {
              addMetadataCheckbox(panelCheckArtwork, value, seasonArtworkCheckBoxes, gbc);
            }
          }

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckArtwork.add(Box.createVerticalStrut(5), gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          gbc.gridwidth = COL_COUNT;
          chckbxSeasonDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
          chckbxSeasonDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
          chckbxSeasonDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
          panelCheckArtwork.add(chckbxSeasonDisplayAllMissingArtwork, gbc.clone());

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          gbc.gridwidth = 1;
          panelCheckArtwork.add(Box.createVerticalStrut(10), gbc.clone());

          // Episode
          gbc.gridx = 0;
          gbc.gridy++;

          JLabel lblEpisode = new TmmLabel(TmmResourceBundle.getString("metatag.episode"));
          panelCheckArtwork.add(lblEpisode, gbc.clone());

          gbc.gridx++;

          for (TvShowEpisodeScraperMetadataConfig value : TvShowEpisodeScraperMetadataConfig.values()) {
            if (value.isArtwork()) {
              addMetadataCheckbox(panelCheckArtwork, value, episodeArtworkCheckBoxes, gbc);
            }
          }

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckArtwork.add(Box.createVerticalStrut(5), gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          gbc.gridwidth = COL_COUNT;
          chckbxEpisodeDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
          chckbxEpisodeDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
          chckbxEpisodeDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
          panelCheckArtwork.add(chckbxEpisodeDisplayAllMissingArtwork, gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          gbc.gridwidth = COL_COUNT;
          chckbxEpisodeSpecialsCheckMissingArtwork = new JCheckBox(TmmResourceBundle.getString("tvshowepisode.checkartwork.specials"));
          panelCheckArtwork.add(chckbxEpisodeSpecialsCheckMissingArtwork, gbc.clone());

          panelUiSettings.add(panelCheckArtwork, "cell 2 11");
        }
      }
      {
        chckbxSeasonArtworkFallback = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.seasonartworkfallback"));
        panelUiSettings.add(chckbxSeasonArtworkFallback, "cell 1 13 2 1");
      }

      JLabel lblRating = new JLabel(TmmResourceBundle.getString("Settings.preferredrating"));
      panelUiSettings.add(lblRating, "cell 1 14 2 1");

      cbRating = new AutocompleteComboBox(Arrays.asList(MediaMetadata.TVDB, MediaMetadata.TMDB, MediaMetadata.IMDB, MediaMetadata.TRAKT_TV,
          "metascore", "rottenTomatoes", MediaMetadata.ANIDB));
      panelUiSettings.add(cbRating, "cell 1 14 2 1");
    }
    {
      JPanel panelFilter = new JPanel();
      panelFilter.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][10lp!][]")); // 16lp ~ width of
                                                                                                                   // the

      JLabel lblAutomaticTasksT = new TmmLabel(TmmResourceBundle.getString("Settings.filters"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelFilter, lblAutomaticTasksT, true);
      JLabel lblTvShowFilter = new JLabel(TmmResourceBundle.getString("Settings.tvshowquickfilter"));
      panelFilter.add(lblTvShowFilter, "cell 1 0 2 1");

      chckbxNode = new JCheckBox(TmmResourceBundle.getString("metatag.node"));
      panelFilter.add(chckbxNode, "flowx,cell 2 1");

      chckbxTitle = new JCheckBox(TmmResourceBundle.getString("metatag.title"));
      panelFilter.add(chckbxTitle, "cell 2 1");
      {

        chckbxOriginalTitle = new JCheckBox(TmmResourceBundle.getString("metatag.originaltitle"));
        panelFilter.add(chckbxOriginalTitle, "cell 2 1");
      }
      JHintLabel lblUniversalFilter = new JHintLabel(TmmResourceBundle.getString("filter.universal"));
      panelFilter.add(lblUniversalFilter, "cell 1 2 2 1");
      lblUniversalFilter.setHintIcon(IconManager.HINT);
      lblUniversalFilter.setToolTipText(TmmResourceBundle.getString("filter.universal.hint"));

      chckbxUniversalNote = new JCheckBox(TmmResourceBundle.getString("metatag.note"));
      panelFilter.add(chckbxUniversalNote, "flowx,cell 2 3");

      chckbxUniversalFilename = new JCheckBox(TmmResourceBundle.getString("metatag.filename"));
      panelFilter.add(chckbxUniversalFilename, "cell 2 3");

      chckbxUniversalTags = new JCheckBox(TmmResourceBundle.getString("metatag.tags"));
      panelFilter.add(chckbxUniversalTags, "cell 2 3");

      chckbxUniversalProductionCompany = new JCheckBox(TmmResourceBundle.getString("metatag.production"));
      panelFilter.add(chckbxUniversalProductionCompany, "cell 2 3");

      chckbxUniversalCountry = new JCheckBox(TmmResourceBundle.getString("metatag.country"));
      panelFilter.add(chckbxUniversalCountry, "cell 2 3");

      chckbxUniversalLanguages = new JCheckBox(TmmResourceBundle.getString("metatag.spokenlanguages"));
      panelFilter.add(chckbxUniversalLanguages, "cell 2 3");

      chckbxUniversalActors = new JCheckBox(TmmResourceBundle.getString("metatag.actors"));
      panelFilter.add(chckbxUniversalActors, "flowx,cell 2 4");

      chckbxUniversalDirectors = new JCheckBox(TmmResourceBundle.getString("metatag.directors"));
      panelFilter.add(chckbxUniversalDirectors, "cell 2 4");
      {

        chckbxUniversalWriters = new JCheckBox(TmmResourceBundle.getString("metatag.writers"));
        panelFilter.add(chckbxUniversalWriters, "cell 2 4");
      }

      {
        chckbxStoreFilter = new JCheckBox(TmmResourceBundle.getString("Settings.movie.persistuifilter"));
        panelFilter.add(chckbxStoreFilter, "cell 1 6 2 1");
      }
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#filter"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
    }

  }

  private <E extends ScraperMetadataConfig> void addMetadataCheckbox(JPanel panel, E config, Map<E, JCheckBox> map, GridBagConstraints gbc) {
    JCheckBox checkBox;
    if (StringUtils.isNotBlank(config.getToolTip())) {
      checkBox = new JHintCheckBox(config.getDescription());
      checkBox.setToolTipText(config.getToolTip());
      ((JHintCheckBox) checkBox).setHintIcon(IconManager.HINT);
    }
    else {
      checkBox = new JCheckBox(config.getDescription());
    }
    map.put(config, checkBox);

    if (gbc.gridx >= COL_COUNT) {
      gbc.gridx = 1;
      gbc.gridy++;
    }
    panel.add(checkBox, gbc.clone());

    gbc.gridx++;
  }

  protected void initDataBindings() {
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("displayMissingEpisodes");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_2,
        chckbxShowMissingEpisodes, jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property tvShowSettingsBeanProperty_3 = BeanProperty.create("preferredRating");
    Property autocompleteComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_3, cbRating,
        autocompleteComboBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxShowMissingEpisodes, jCheckBoxBeanProperty,
        chckbxShowMissingSpecials, jCheckBoxBeanProperty_1);
    autoBinding_7.bind();
    //
    Property tvShowSettingsBeanProperty_6 = BeanProperty.create("displayMissingSpecials");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_6,
        chckbxShowMissingSpecials, jCheckBoxBeanProperty);
    autoBinding_8.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("showTvShowTableTooltips");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1,
        chckbxTvShowTableTooltips, jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property tvShowSettingsBeanProperty_14 = BeanProperty.create("seasonArtworkFallback");
    AutoBinding autoBinding_19 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_14,
        chckbxSeasonArtworkFallback, jCheckBoxBeanProperty);
    autoBinding_19.bind();
    //
    Property tvShowSettingsBeanProperty_4 = BeanProperty.create("storeUiFilters");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_4, chckbxStoreFilter,
        jCheckBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property tvShowSettingsBeanProperty_21 = BeanProperty.create("node");
    AutoBinding autoBinding_21 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_21, chckbxNode,
        jCheckBoxBeanProperty);
    autoBinding_21.bind();
    //
    Property tvShowSettingsBeanProperty_22 = BeanProperty.create("title");
    AutoBinding autoBinding_22 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_22, chckbxTitle,
        jCheckBoxBeanProperty);
    autoBinding_22.bind();
    //
    Property tvShowSettingsBeanProperty_23 = BeanProperty.create("originalTitle");
    AutoBinding autoBinding_23 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_23, chckbxOriginalTitle,
        jCheckBoxBeanProperty);
    autoBinding_23.bind();
    //
    Property tvShowSettingsBeanProperty_15 = BeanProperty.create("tvShowDisplayAllMissingMetadata");
    AutoBinding autoBinding_25 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_15,
        chckbxTvShowDisplayAllMissingMetadata, jCheckBoxBeanProperty);
    autoBinding_25.bind();
    //
    Property tvShowSettingsBeanProperty_16 = BeanProperty.create("episodeDisplayAllMissingMetadata");
    AutoBinding autoBinding_26 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_16,
        chckbxEpisodeDisplayAllMissingMetadata, jCheckBoxBeanProperty);
    autoBinding_26.bind();
    //
    Property tvShowSettingsBeanProperty_17 = BeanProperty.create("tvShowDisplayAllMissingArtwork");
    AutoBinding autoBinding_27 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_17,
        chckbxTvShowDisplayAllMissingArtwork, jCheckBoxBeanProperty);
    autoBinding_27.bind();
    //
    Property tvShowSettingsBeanProperty_18 = BeanProperty.create("seasonDisplayAllMissingArtwork");
    AutoBinding autoBinding_28 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_18,
        chckbxSeasonDisplayAllMissingArtwork, jCheckBoxBeanProperty);
    autoBinding_28.bind();
    //
    Property tvShowSettingsBeanProperty_19 = BeanProperty.create("episodeDisplayAllMissingArtwork");
    AutoBinding autoBinding_29 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_19,
        chckbxEpisodeDisplayAllMissingArtwork, jCheckBoxBeanProperty);
    autoBinding_29.bind();
    //
    Property tvShowSettingsBeanProperty_7 = BeanProperty.create("episodeSpecialsCheckMissingMetadata");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_7,
        chckbxEpisodeSpecialsCheckMissingMetadata, jCheckBoxBeanProperty);
    autoBinding_9.bind();
    //
    Property tvShowSettingsBeanProperty_24 = BeanProperty.create("episodeSpecialsCheckMissingArtwork");
    AutoBinding autoBinding_24 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_24,
        chckbxEpisodeSpecialsCheckMissingArtwork, jCheckBoxBeanProperty);
    autoBinding_24.bind();
    //
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxShowMissingEpisodes, jCheckBoxBeanProperty, chckbxIncludeNotAired,
        jCheckBoxBeanProperty_1);
    autoBinding.bind();
    //
    Property tvShowSettingsBeanProperty = BeanProperty.create("displayMissingNotAired");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty, chckbxIncludeNotAired,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
  }
}
