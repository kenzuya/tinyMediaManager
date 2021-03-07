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

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.event.ItemListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.thirdparty.trakttv.TvShowClearTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class TvShowSettingsPanel extends JPanel {
  private static final long            serialVersionUID = -675729644848101096L;

  private final TvShowSettings         settings         = TvShowModuleManager.SETTINGS;
  private final ItemListener           checkBoxListener;

  private JCheckBox                    chckbxImageCache;
  private JCheckBox                    chckbxExtractArtworkFromVsmeta;
  private JCheckBox                    chckbxTraktTv;
  private JButton                      btnClearTraktTvShows;
  private JCheckBox                    chckbxShowLogos;
  private JCheckBox                    chckbxShowMissingEpisodes;
  private JButton                      btnPresetKodi;
  private JButton                      btnPresetXbmc;
  private JButton                      btnPresetMediaPortal1;
  private JButton                      btnPresetMediaPortal2;
  private JButton                      btnPresetPlex;
  private AutocompleteComboBox<String> cbRating;
  private JCheckBox                    chckbxRenameAfterScrape;
  private JCheckBox                    chckbxAutoUpdateOnStart;
  private JCheckBox                    chckbxShowMissingSpecials;
  private JCheckBox                    chckbxTvShowTableTooltips;

  private JCheckBox                    chckbxTvShowCheckPoster;
  private JCheckBox                    chckbxTvShowCheckFanart;
  private JCheckBox                    chckbxTvShowCheckBanner;
  private JCheckBox                    chckbxTvShowCheckClearart;
  private JCheckBox                    chckbxTvShowCheckThumb;
  private JCheckBox                    chckbxTvShowCheckLogo;
  private JCheckBox                    chckbxTvShowCheckClearlogo;

  private JCheckBox                    chckbxTvShowSeasonCheckPoster;
  private JCheckBox                    chckbxTvShowSeasonCheckBanner;
  private JCheckBox                    chckbxTvShowSeasonCheckThumb;
  private JCheckBox                    chckbxTvShowEpisodeCheckThumb;
  private JCheckBox                    chckbxMetadataFromMediainfo;
  private JCheckBox                    chckbxTraktCollection;
  private JCheckBox                    chckbxTraktWatched;
  private JCheckBox                    chckbxTraktRating;
  private JCheckBox                    chckbxSeasonArtworkFallback;
  private JCheckBox                    chckbxStoreFilter;

  /**
   * Instantiates a new tv show settings panel.
   */
  TvShowSettingsPanel() {
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

    btnPresetXbmc.addActionListener(evt -> settings.setDefaultSettingsForXbmc());
    btnPresetKodi.addActionListener(evt -> settings.setDefaultSettingsForKodi());
    btnPresetMediaPortal1.addActionListener(evt -> settings.setDefaultSettingsForMediaPortal());
    btnPresetMediaPortal2.addActionListener(evt -> settings.setDefaultSettingsForMediaPortal());
    btnPresetPlex.addActionListener(evt -> settings.setDefaultSettingsForPlex());

    buildCheckBoxes();
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    settings.clearTvShowCheckImages();
    if (chckbxTvShowCheckPoster.isSelected()) {
      settings.addTvShowCheckImages(MediaArtwork.MediaArtworkType.POSTER);
    }
    if (chckbxTvShowCheckFanart.isSelected()) {
      settings.addTvShowCheckImages(MediaArtwork.MediaArtworkType.BACKGROUND);
    }
    if (chckbxTvShowCheckBanner.isSelected()) {
      settings.addTvShowCheckImages(MediaArtwork.MediaArtworkType.BANNER);
    }
    if (chckbxTvShowCheckClearart.isSelected()) {
      settings.addTvShowCheckImages(MediaArtwork.MediaArtworkType.CLEARART);
    }
    if (chckbxTvShowCheckThumb.isSelected()) {
      settings.addTvShowCheckImages(MediaArtwork.MediaArtworkType.THUMB);
    }
    if (chckbxTvShowCheckLogo.isSelected()) {
      settings.addTvShowCheckImages(MediaArtwork.MediaArtworkType.LOGO);
    }
    if (chckbxTvShowCheckClearlogo.isSelected()) {
      settings.addTvShowCheckImages(MediaArtwork.MediaArtworkType.CLEARLOGO);
    }

    settings.clearSeasonCheckImages();
    if (chckbxTvShowSeasonCheckPoster.isSelected()) {
      settings.addSeasonCheckImages(MediaArtwork.MediaArtworkType.SEASON_POSTER);
    }
    if (chckbxTvShowSeasonCheckBanner.isSelected()) {
      settings.addSeasonCheckImages(MediaArtwork.MediaArtworkType.SEASON_BANNER);
    }
    if (chckbxTvShowSeasonCheckThumb.isSelected()) {
      settings.addSeasonCheckImages(MediaArtwork.MediaArtworkType.SEASON_THUMB);
    }

    settings.clearEpisodeCheckImages();
    if (chckbxTvShowEpisodeCheckThumb.isSelected()) {
      settings.addEpisodeCheckImages(MediaArtwork.MediaArtworkType.THUMB);
    }
  }

  private void buildCheckBoxes() {
    chckbxTvShowCheckPoster.removeItemListener(checkBoxListener);
    chckbxTvShowCheckFanart.removeItemListener(checkBoxListener);
    chckbxTvShowCheckBanner.removeItemListener(checkBoxListener);
    chckbxTvShowCheckClearart.removeItemListener(checkBoxListener);
    chckbxTvShowCheckThumb.removeItemListener(checkBoxListener);
    chckbxTvShowCheckLogo.removeItemListener(checkBoxListener);
    chckbxTvShowCheckClearlogo.removeItemListener(checkBoxListener);
    clearSelection(chckbxTvShowCheckPoster, chckbxTvShowCheckFanart, chckbxTvShowCheckBanner, chckbxTvShowCheckClearart, chckbxTvShowCheckThumb,
        chckbxTvShowCheckLogo, chckbxTvShowCheckClearlogo);

    chckbxTvShowSeasonCheckPoster.removeItemListener(checkBoxListener);
    chckbxTvShowSeasonCheckBanner.removeItemListener(checkBoxListener);
    chckbxTvShowSeasonCheckThumb.removeItemListener(checkBoxListener);
    clearSelection(chckbxTvShowSeasonCheckPoster, chckbxTvShowSeasonCheckBanner, chckbxTvShowSeasonCheckThumb);

    chckbxTvShowEpisodeCheckThumb.removeItemListener(checkBoxListener);
    clearSelection(chckbxTvShowEpisodeCheckThumb);

    for (MediaArtwork.MediaArtworkType type : settings.getTvShowCheckImages()) {
      switch (type) {
        case POSTER:
          chckbxTvShowCheckPoster.setSelected(true);
          break;

        case BACKGROUND:
          chckbxTvShowCheckFanart.setSelected(true);
          break;

        case BANNER:
          chckbxTvShowCheckBanner.setSelected(true);
          break;

        case CLEARART:
          chckbxTvShowCheckClearart.setSelected(true);
          break;

        case THUMB:
          chckbxTvShowCheckThumb.setSelected(true);
          break;

        case LOGO:
          chckbxTvShowCheckLogo.setSelected(true);
          break;

        case CLEARLOGO:
          chckbxTvShowCheckClearlogo.setSelected(true);
          break;

        default:
          break;
      }
    }

    for (MediaArtwork.MediaArtworkType type : settings.getSeasonCheckImages()) {
      switch (type) {
        case SEASON_POSTER:
          chckbxTvShowSeasonCheckPoster.setSelected(true);
          break;

        case SEASON_BANNER:
          chckbxTvShowSeasonCheckBanner.setSelected(true);
          break;

        case SEASON_THUMB:
          chckbxTvShowSeasonCheckThumb.setSelected(true);
          break;

        default:
          break;
      }
    }

    for (MediaArtwork.MediaArtworkType type : settings.getEpisodeCheckImages()) {
      switch (type) {
        case THUMB:
          chckbxTvShowEpisodeCheckThumb.setSelected(true);
          break;

        default:
          break;
      }
    }

    chckbxTvShowCheckPoster.addItemListener(checkBoxListener);
    chckbxTvShowCheckFanart.addItemListener(checkBoxListener);
    chckbxTvShowCheckBanner.addItemListener(checkBoxListener);
    chckbxTvShowCheckClearart.addItemListener(checkBoxListener);
    chckbxTvShowCheckThumb.addItemListener(checkBoxListener);
    chckbxTvShowCheckLogo.addItemListener(checkBoxListener);
    chckbxTvShowCheckClearlogo.addItemListener(checkBoxListener);

    chckbxTvShowSeasonCheckPoster.addItemListener(checkBoxListener);
    chckbxTvShowSeasonCheckBanner.addItemListener(checkBoxListener);
    chckbxTvShowSeasonCheckThumb.addItemListener(checkBoxListener);

    chckbxTvShowEpisodeCheckThumb.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      panelUiSettings.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][][]")); // 16lp ~ width of the

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        chckbxStoreFilter = new JCheckBox(TmmResourceBundle.getString("Settings.movie.persistuifilter"));
        panelUiSettings.add(chckbxStoreFilter, "cell 1 0 2 1");
      }
      {
        chckbxShowLogos = new JCheckBox(TmmResourceBundle.getString("Settings.showlogos"));
        panelUiSettings.add(chckbxShowLogos, "cell 1 1 2 1");

        chckbxShowMissingEpisodes = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingepisodes"));
        panelUiSettings.add(chckbxShowMissingEpisodes, "cell 1 2 2 1");

        chckbxShowMissingSpecials = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.missingespecials"));
        panelUiSettings.add(chckbxShowMissingSpecials, "cell 2 3");

        JLabel lblRating = new JLabel(TmmResourceBundle.getString("Settings.preferredrating"));
        panelUiSettings.add(lblRating, "flowx,cell 1 4 2 1");

        cbRating = new AutocompleteComboBox(Arrays.asList("tvdb", "tmdb", "imdb", "trakt", "metascore", "rottenTomatoes", "anidb"));
        panelUiSettings.add(cbRating, "cell 1 4 2 1");

        chckbxTvShowTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.showtabletooltips"));
        panelUiSettings.add(chckbxTvShowTableTooltips, "cell 1 6 2 1");
      }
      {
        chckbxSeasonArtworkFallback = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.seasonartworkfallback"));
        panelUiSettings.add(chckbxSeasonArtworkFallback, "cell 1 7 2 1");
      }
    }
    {
      JPanel panelAutomaticTasks = new JPanel();
      panelAutomaticTasks.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][10lp!][]")); // 16lp ~ width of the

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

        chckbxTraktTv = new JCheckBox(TmmResourceBundle.getString("Settings.trakt"));
        panelAutomaticTasks.add(chckbxTraktTv, "cell 1 1 2 1");

        btnClearTraktTvShows = new JButton(TmmResourceBundle.getString("Settings.trakt.cleartvshows"));
        panelAutomaticTasks.add(btnClearTraktTvShows, "cell 1 1 2 1");

        chckbxTraktCollection = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.collection"));
        panelAutomaticTasks.add(chckbxTraktCollection, "cell 2 2");

        chckbxTraktWatched = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.watched"));
        panelAutomaticTasks.add(chckbxTraktWatched, "cell 2 3");

        chckbxTraktRating = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.rating"));
        panelAutomaticTasks.add(chckbxTraktRating, "cell 2 4");

        chckbxAutoUpdateOnStart = new JCheckBox(TmmResourceBundle.getString("Settings.tvshow.automaticupdate"));
        panelAutomaticTasks.add(chckbxAutoUpdateOnStart, "cell 1 6 2 1");

        JLabel lblAutomaticUpdateHint = new JLabel(IconManager.HINT);
        lblAutomaticUpdateHint.setToolTipText(TmmResourceBundle.getString("Settings.tvshow.automaticupdate.desc"));
        panelAutomaticTasks.add(lblAutomaticUpdateHint, "cell 1 6 2 1");
      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][]")); // 16lp ~ width of the

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

        JLabel lblCheckImages = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
        panelMisc.add(lblCheckImages, "cell 1 3 2 1");

        {
          JPanel panelCheckImages = new JPanel();
          panelCheckImages.setLayout(new MigLayout("hidemode 1, insets 0", "[][][][]", ""));
          panelMisc.add(panelCheckImages, "cell 2 4");

          JLabel lblTvShowCheckImages = new TmmLabel(TmmResourceBundle.getString("metatag.tvshow"));
          panelCheckImages.add(lblTvShowCheckImages, "cell 0 0");

          chckbxTvShowCheckPoster = new JCheckBox(TmmResourceBundle.getString("mediafiletype.poster"));
          panelCheckImages.add(chckbxTvShowCheckPoster, "cell 1 0");

          chckbxTvShowCheckFanart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.fanart"));
          panelCheckImages.add(chckbxTvShowCheckFanart, "cell 2 0");

          chckbxTvShowCheckBanner = new JCheckBox(TmmResourceBundle.getString("mediafiletype.banner"));
          panelCheckImages.add(chckbxTvShowCheckBanner, "cell 3 0");

          chckbxTvShowCheckClearart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.clearart"));
          panelCheckImages.add(chckbxTvShowCheckClearart, "cell 4 0");

          chckbxTvShowCheckThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
          panelCheckImages.add(chckbxTvShowCheckThumb, "cell 5 0");

          chckbxTvShowCheckLogo = new JCheckBox(TmmResourceBundle.getString("mediafiletype.logo"));
          panelCheckImages.add(chckbxTvShowCheckLogo, "cell 6 0");

          chckbxTvShowCheckClearlogo = new JCheckBox(TmmResourceBundle.getString("mediafiletype.clearlogo"));
          panelCheckImages.add(chckbxTvShowCheckClearlogo, "cell 7 0");

          JLabel lblTvShowSeasonCheckImages = new TmmLabel(TmmResourceBundle.getString("metatag.season"));
          panelCheckImages.add(lblTvShowSeasonCheckImages, "cell 0 1");

          chckbxTvShowSeasonCheckPoster = new JCheckBox(TmmResourceBundle.getString("mediafiletype.poster"));
          panelCheckImages.add(chckbxTvShowSeasonCheckPoster, "cell 1 1");

          chckbxTvShowSeasonCheckBanner = new JCheckBox(TmmResourceBundle.getString("mediafiletype.banner"));
          panelCheckImages.add(chckbxTvShowSeasonCheckBanner, "cell 2 1");

          chckbxTvShowSeasonCheckThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
          panelCheckImages.add(chckbxTvShowSeasonCheckThumb, "cell 3 1");

          JLabel lblTvShowEpisodeCheckImages = new TmmLabel(TmmResourceBundle.getString("metatag.episode"));
          panelCheckImages.add(lblTvShowEpisodeCheckImages, "cell 0 2");

          chckbxTvShowEpisodeCheckThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
          panelCheckImages.add(chckbxTvShowEpisodeCheckThumb, "cell 1 2");
        }
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
    Property tvShowSettingsBeanProperty_7 = BeanProperty.create("showLogosPanel");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_7, chckbxShowLogos,
        jCheckBoxBeanProperty);
    autoBinding_9.bind();
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
  }
}
