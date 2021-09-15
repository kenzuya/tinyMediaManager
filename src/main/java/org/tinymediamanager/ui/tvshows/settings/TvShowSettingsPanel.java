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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.TvShowSettingsDefaults;
import org.tinymediamanager.thirdparty.trakttv.TvShowClearTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.JHintLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class TvShowSettingsPanel extends JPanel {
  private static final long                                        serialVersionUID = -675729644848101096L;
  private static final int                                         COL_COUNT        = 7;

  private final TvShowSettings                                     settings         = TvShowModuleManager.getInstance().getSettings();
  private final ItemListener                                       checkBoxListener;

  private JCheckBox                                                chckbxImageCache;
  private JCheckBox                                                chckbxExtractArtworkFromVsmeta;
  private JCheckBox                                                chckbxTraktTv;
  private JButton                                                  btnClearTraktTvShows;
  private JCheckBox                                                chckbxShowMissingEpisodes;
  private JButton                                                  btnPresetKodi;
  private JButton                                                  btnPresetXbmc;
  private JButton                                                  btnPresetMediaPortal1;
  private JButton                                                  btnPresetMediaPortal2;
  private JButton                                                  btnPresetPlex;
  private AutocompleteComboBox<String>                             cbRating;
  private JCheckBox                                                chckbxRenameAfterScrape;
  private JCheckBox                                                chckbxARDAfterScrape;
  private JCheckBox                                                chckbxAutoUpdateOnStart;
  private JCheckBox                                                chckbxShowMissingSpecials;
  private JCheckBox                                                chckbxTvShowTableTooltips;

  private JCheckBox                                                chckbxMetadataFromMediainfo;
  private JCheckBox                                                chckbxTraktCollection;
  private JCheckBox                                                chckbxTraktWatched;
  private JCheckBox                                                chckbxTraktRating;
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

  /**
   * Instantiates a new tv show settings panel.
   */
  TvShowSettingsPanel() {
    tvShowMetadataCheckBoxes = new LinkedHashMap<>();
    episodeMetadataCheckBoxes = new LinkedHashMap<>();
    tvShowArtworkCheckBoxes = new LinkedHashMap<>();
    seasonArtworkCheckBoxes = new LinkedHashMap<>();
    episodeArtworkCheckBoxes = new LinkedHashMap<>();
    checkBoxListener = e -> checkChanges();

    // UI initializations
    initComponents();
    initDataBindings();

    // logic initializations
    btnClearTraktTvShows.addActionListener(e -> {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int confirm = JOptionPane.showOptionDialog(null, TmmResourceBundle.getString("Settings.trakt.cleartvshows.hint"),
          TmmResourceBundle.getString("Settings.trakt.cleartvshows"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
      if (confirm == JOptionPane.YES_OPTION) {
        TmmTask task = new TvShowClearTraktTvTask();
        TmmTaskManager.getInstance().addUnnamedTask(task);
      }
    });

    btnPresetXbmc.addActionListener(evt -> TvShowSettingsDefaults.setDefaultSettingsForXbmc());
    btnPresetKodi.addActionListener(evt -> TvShowSettingsDefaults.setDefaultSettingsForKodi());
    btnPresetMediaPortal1.addActionListener(evt -> TvShowSettingsDefaults.setDefaultSettingsForMediaPortal());
    btnPresetMediaPortal2.addActionListener(evt -> TvShowSettingsDefaults.setDefaultSettingsForMediaPortal());
    btnPresetPlex.addActionListener(evt -> TvShowSettingsDefaults.setDefaultSettingsForPlex());

    buildCheckBoxes();
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
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
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      // 16lp ~ width of the checkbox
      panelUiSettings
          .setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][][][][10lp!][][grow][10lp!][][][10lp!][][]"));

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        JLabel lblTvShowFilter = new JLabel(TmmResourceBundle.getString("Settings.tvshowquickfilter"));
        panelUiSettings.add(lblTvShowFilter, "cell 1 0 2 1");

        chckbxNode = new JCheckBox(TmmResourceBundle.getString("metatag.node"));
        panelUiSettings.add(chckbxNode, "flowx,cell 2 1");

        chckbxTitle = new JCheckBox(TmmResourceBundle.getString("metatag.title"));
        panelUiSettings.add(chckbxTitle, "cell 2 1");

        chckbxOriginalTitle = new JCheckBox(TmmResourceBundle.getString("metatag.originaltitle"));
        panelUiSettings.add(chckbxOriginalTitle, "cell 2 1");
      }
      {
        JHintLabel lblUniversalFilter = new JHintLabel(TmmResourceBundle.getString("filter.universal"));
        lblUniversalFilter.setHintIcon(IconManager.HINT);
        lblUniversalFilter.setToolTipText(TmmResourceBundle.getString("filter.universal.hint"));
        panelUiSettings.add(lblUniversalFilter, "cell 1 2 2 1");

        chckbxUniversalNote = new JCheckBox(TmmResourceBundle.getString("metatag.note"));
        panelUiSettings.add(chckbxUniversalNote, "cell 2 3");

        chckbxUniversalFilename = new JCheckBox(TmmResourceBundle.getString("metatag.filename"));
        panelUiSettings.add(chckbxUniversalFilename, "cell 2 3");

        chckbxUniversalTags = new JCheckBox(TmmResourceBundle.getString("metatag.tags"));
        panelUiSettings.add(chckbxUniversalTags, "cell 2 3");

        chckbxUniversalProductionCompany = new JCheckBox(TmmResourceBundle.getString("metatag.production"));
        panelUiSettings.add(chckbxUniversalProductionCompany, "cell 2 3");

        chckbxUniversalCountry = new JCheckBox(TmmResourceBundle.getString("metatag.country"));
        panelUiSettings.add(chckbxUniversalCountry, "cell 2 3");

        chckbxUniversalLanguages = new JCheckBox(TmmResourceBundle.getString("metatag.spokenlanguages"));
        panelUiSettings.add(chckbxUniversalLanguages, "cell 2 3");

        chckbxUniversalActors = new JCheckBox(TmmResourceBundle.getString("metatag.actors"));
        panelUiSettings.add(chckbxUniversalActors, "cell 2 4");

        chckbxUniversalDirectors = new JCheckBox(TmmResourceBundle.getString("metatag.directors"));
        panelUiSettings.add(chckbxUniversalDirectors, "cell 2 4");

        chckbxUniversalWriters = new JCheckBox(TmmResourceBundle.getString("metatag.writers"));
        panelUiSettings.add(chckbxUniversalWriters, "cell 2 4");
      }

      {
        chckbxStoreFilter = new JCheckBox(TmmResourceBundle.getString("Settings.movie.persistuifilter"));
        panelUiSettings.add(chckbxStoreFilter, "cell 1 5 2 1");
      }

      {
        chckbxShowMissingEpisodes = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingepisodes"));
        panelUiSettings.add(chckbxShowMissingEpisodes, "cell 1 6 2 1");

        chckbxShowMissingSpecials = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingespecials"));
        panelUiSettings.add(chckbxShowMissingSpecials, "cell 2 7");

        JLabel lblRating = new JLabel(TmmResourceBundle.getString("Settings.preferredrating"));
        panelUiSettings.add(lblRating, "flowx,cell 1 8 2 1");

        cbRating = new AutocompleteComboBox(Arrays.asList("tvdb", "tmdb", "imdb", "trakt", "metascore", "rottenTomatoes", "anidb"));
        panelUiSettings.add(cbRating, "cell 1 8 2 1");

        {
          JLabel lblCheckMetadata = new JLabel(TmmResourceBundle.getString("Settings.checkmetadata"));
          panelUiSettings.add(lblCheckMetadata, "cell 1 10 2 1");

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
          chckbxTvShowDisplayAllMissingMetadata = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkmetadata.displayall"));
          chckbxTvShowDisplayAllMissingMetadata.setToolTipText(TmmResourceBundle.getString("Settings.checkmetadata.displayall.desc"));
          chckbxTvShowDisplayAllMissingMetadata.setHintIcon(IconManager.HINT);
          panelCheckMetadata.add(chckbxTvShowDisplayAllMissingMetadata, gbc.clone());

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
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
          chckbxEpisodeDisplayAllMissingMetadata = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkmetadata.displayall"));
          chckbxEpisodeDisplayAllMissingMetadata.setToolTipText(TmmResourceBundle.getString("Settings.checkmetadata.displayall.desc"));
          chckbxEpisodeDisplayAllMissingMetadata.setHintIcon(IconManager.HINT);
          panelCheckMetadata.add(chckbxEpisodeDisplayAllMissingMetadata, gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          chckbxEpisodeSpecialsCheckMissingMetadata = new JCheckBox(TmmResourceBundle.getString("tvshowepisode.checkmetadata.specials"));
          panelCheckMetadata.add(chckbxEpisodeSpecialsCheckMissingMetadata, gbc.clone());

          panelUiSettings.add(panelCheckMetadata, "cell 2 11");
        }
        {
          JLabel lblCheckArtwork = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
          panelUiSettings.add(lblCheckArtwork, "cell 1 13 2 1");

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
            if (value.isArtwork()) {
              addMetadataCheckbox(panelCheckArtwork, value, tvShowArtworkCheckBoxes, gbc);
            }
          }

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckArtwork.add(Box.createVerticalStrut(5), gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          chckbxTvShowDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
          chckbxTvShowDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
          chckbxTvShowDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
          panelCheckArtwork.add(chckbxTvShowDisplayAllMissingArtwork, gbc.clone());

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckArtwork.add(Box.createVerticalStrut(10), gbc.clone());

          // Season
          gbc.gridx = 0;
          gbc.gridy++;

          JLabel lblSeason = new TmmLabel(TmmResourceBundle.getString("metatag.season"));
          panelCheckArtwork.add(lblSeason, gbc.clone());

          gbc.gridx++;

          for (TvShowScraperMetadataConfig value : TvShowScraperMetadataConfig.values()) {
            if (value.isArtwork() && value.name().startsWith("SEASON")) {
              addMetadataCheckbox(panelCheckArtwork, value, seasonArtworkCheckBoxes, gbc);
            }
          }

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
          panelCheckArtwork.add(Box.createVerticalStrut(5), gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          chckbxSeasonDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
          chckbxSeasonDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
          chckbxSeasonDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
          panelCheckArtwork.add(chckbxSeasonDisplayAllMissingArtwork, gbc.clone());

          // spacer
          gbc.gridx = 0;
          gbc.gridy++;
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
          chckbxEpisodeDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
          chckbxEpisodeDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
          chckbxEpisodeDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
          panelCheckArtwork.add(chckbxEpisodeDisplayAllMissingArtwork, gbc.clone());

          gbc.gridx = 1;
          gbc.gridy++;
          chckbxEpisodeSpecialsCheckMissingArtwork = new JCheckBox(TmmResourceBundle.getString("tvshowepisode.checkartwork.specials"));
          panelCheckArtwork.add(chckbxEpisodeSpecialsCheckMissingArtwork, gbc.clone());

          panelUiSettings.add(panelCheckArtwork, "cell 2 14");
        }

        chckbxTvShowTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.showtabletooltips"));
        panelUiSettings.add(chckbxTvShowTableTooltips, "cell 1 16 2 1");
      }
      {
        chckbxSeasonArtworkFallback = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.seasonartworkfallback"));
        panelUiSettings.add(chckbxSeasonArtworkFallback, "cell 1 17 2 1");
      }
    }
    {
      JPanel panelAutomaticTasks = new JPanel();
      panelAutomaticTasks.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][10lp!][][][10lp!][]")); // 16lp ~ width of
                                                                                                                                  // the

      JLabel lblAutomaticTasksT = new TmmLabel(TmmResourceBundle.getString("Settings.automatictasks"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAutomaticTasks, lblAutomaticTasksT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#automatic-tasks"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        chckbxRenameAfterScrape = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.automaticrename"));
        panelAutomaticTasks.add(chckbxRenameAfterScrape, "cell 1 0 2 1");

        JLabel lblAutomaticRenameHint = new JLabel(IconManager.HINT);
        lblAutomaticRenameHint.setToolTipText(TmmResourceBundle.getString("Settings.tvshow.automaticrename.desc"));
        panelAutomaticTasks.add(lblAutomaticRenameHint, "cell 1 0 2 1");

        chckbxARDAfterScrape = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.automaticard"));
        panelAutomaticTasks.add(chckbxARDAfterScrape, "cell 1 1 2 1");

        JLabel lblAutomaticARDHint = new JLabel(IconManager.HINT);
        lblAutomaticARDHint.setToolTipText(TmmResourceBundle.getString("Settings.tvshow.automaticard.desc"));
        panelAutomaticTasks.add(lblAutomaticARDHint, "cell 1 1 2 1");

        chckbxTraktTv = new JCheckBox(TmmResourceBundle.getString("Settings.trakt"));
        panelAutomaticTasks.add(chckbxTraktTv, "cell 1 2 2 1");

        btnClearTraktTvShows = new JButton(TmmResourceBundle.getString("Settings.trakt.cleartvshows"));
        panelAutomaticTasks.add(btnClearTraktTvShows, "cell 1 2 2 1");

        chckbxTraktCollection = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.collection"));
        panelAutomaticTasks.add(chckbxTraktCollection, "cell 2 4");

        chckbxTraktWatched = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.watched"));
        panelAutomaticTasks.add(chckbxTraktWatched, "cell 2 4");

        chckbxTraktRating = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.rating"));
        panelAutomaticTasks.add(chckbxTraktRating, "cell 2 5");

        chckbxAutoUpdateOnStart = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.automaticupdate"));
        panelAutomaticTasks.add(chckbxAutoUpdateOnStart, "cell 1 7 2 1");

        JLabel lblAutomaticUpdateHint = new JLabel(IconManager.HINT);
        lblAutomaticUpdateHint.setToolTipText(TmmResourceBundle.getString("Settings.tvshow.automaticupdate.desc"));
        panelAutomaticTasks.add(lblAutomaticUpdateHint, "cell 1 7 2 1");
      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#misc-settings"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");

      chckbxMetadataFromMediainfo = new JCheckBox(TmmResourceBundle.getString("Settings.usemediainfometadata"));
      panelMisc.add(chckbxMetadataFromMediainfo, "cell 1 0 2 1");
      {
        chckbxExtractArtworkFromVsmeta = new JCheckBox(TmmResourceBundle.getString("Settings.extractartworkfromvsmeta"));
        panelMisc.add(chckbxExtractArtworkFromVsmeta, "cell 1 1 2 1");

        chckbxImageCache = new JCheckBox(TmmResourceBundle.getString("Settings.imagecacheimport"));
        panelMisc.add(chckbxImageCache, "cell 1 2 2 1");

        JLabel lblBuildImageCacheHint = new JLabel(IconManager.HINT);
        lblBuildImageCacheHint.setToolTipText(TmmResourceBundle.getString("Settings.imagecacheimporthint"));
        panelMisc.add(lblBuildImageCacheHint, "cell 1 2 2 1");
      }
    }
    {
      JPanel panelPresets = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][15lp][][][grow]", "[]"));

      JLabel lblPresets = new TmmLabel(TmmResourceBundle.getString("Settings.preset"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelPresets, lblPresets, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#media-center-presets"));
      add(collapsiblePanel, "cell 0 6,growx,wmin 0");
      {

        {
          JLabel lblPresetHintT = new JLabel(TmmResourceBundle.getString("Settings.preset.desc"));
          panelPresets.add(lblPresetHintT, "cell 1 0 3 1");
        }
        {
          btnPresetKodi = new JButton("Kodi v17+");
          panelPresets.add(btnPresetKodi, "cell 2 1,growx");

          btnPresetXbmc = new JButton("XBMC/Kodi <v17");
          panelPresets.add(btnPresetXbmc, "cell 3 1,growx");

          btnPresetMediaPortal1 = new JButton("MediaPortal 1.x");
          panelPresets.add(btnPresetMediaPortal1, "cell 2 2,growx");

          btnPresetMediaPortal2 = new JButton("MediaPortal 2.x");
          panelPresets.add(btnPresetMediaPortal2, "cell 3 2,growx");

          btnPresetPlex = new JButton("Plex");
          panelPresets.add(btnPresetPlex, "cell 2 3,growx");
        }
      }
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
    Property settingsBeanProperty = BeanProperty.create("syncTrakt");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, chckbxTraktTv,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property tvShowSettingsBeanProperty = BeanProperty.create("buildImageCacheOnImport");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty, chckbxImageCache,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("displayMissingEpisodes");
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
    Property tvShowSettingsBeanProperty_5 = BeanProperty.create("renameAfterScrape");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_5, chckbxRenameAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_6.bind();
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
    Property tvShowSettingsBeanProperty_8 = BeanProperty.create("extractArtworkFromVsmeta");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_8,
        chckbxExtractArtworkFromVsmeta, jCheckBoxBeanProperty);
    autoBinding_10.bind();
    //
    Property tvShowSettingsBeanProperty_11 = BeanProperty.create("updateOnStart");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_11,
        chckbxAutoUpdateOnStart, jCheckBoxBeanProperty);
    autoBinding_11.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("showTvShowTableTooltips");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1,
        chckbxTvShowTableTooltips, jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property tvShowSettingsBeanProperty_9 = BeanProperty.create("useMediainfoMetadata");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_9,
        chckbxMetadataFromMediainfo, jCheckBoxBeanProperty);
    autoBinding_12.bind();
    //
    AutoBinding autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktTv, jCheckBoxBeanProperty, chckbxTraktCollection,
        jCheckBoxBeanProperty_1);
    autoBinding_13.bind();
    //
    AutoBinding autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktTv, jCheckBoxBeanProperty, chckbxTraktWatched,
        jCheckBoxBeanProperty_1);
    autoBinding_14.bind();
    //
    AutoBinding autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktTv, jCheckBoxBeanProperty, chckbxTraktRating,
        jCheckBoxBeanProperty_1);
    autoBinding_15.bind();
    //
    Property tvShowSettingsBeanProperty_10 = BeanProperty.create("syncTraktCollection");
    AutoBinding autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_10, chckbxTraktCollection,
        jCheckBoxBeanProperty);
    autoBinding_16.bind();
    //
    Property tvShowSettingsBeanProperty_12 = BeanProperty.create("syncTraktWatched");
    AutoBinding autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_12, chckbxTraktWatched,
        jCheckBoxBeanProperty);
    autoBinding_17.bind();
    //
    Property tvShowSettingsBeanProperty_13 = BeanProperty.create("syncTraktRating");
    AutoBinding autoBinding_18 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_13, chckbxTraktRating,
        jCheckBoxBeanProperty);
    autoBinding_18.bind();
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
    Property tvShowSettingsBeanProperty_20 = BeanProperty.create("ardAfterScrape");
    AutoBinding autoBinding_20 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_20, chckbxARDAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_20.bind();
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
  }
}
