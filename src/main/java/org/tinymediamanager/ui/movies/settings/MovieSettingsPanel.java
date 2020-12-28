/*
 * Copyright 2012 - 2020 Manuel Laggner
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
package org.tinymediamanager.ui.movies.settings;

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
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.MovieTextMatcherList;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.thirdparty.trakttv.MovieClearTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieSettingsPanel is used for displaying some movie related settings
 * 
 * @author Manuel Laggner
 */
public class MovieSettingsPanel extends JPanel {
  private static final long            serialVersionUID = -4173835431245178069L;

  

  private final MovieSettings          settings         = MovieModuleManager.SETTINGS;

  private JButton                      btnClearTraktData;
  private JCheckBox                    chckbxTraktSync;
  private JCheckBox                    chckbxRenameAfterScrape;
  private JCheckBox                    chckbxAutoUpdateOnStart;
  private JCheckBox                    chckbxBuildImageCache;
  private JCheckBox                    chckbxExtractArtworkFromVsmeta;
  private JCheckBox                    chckbxRuntimeFromMi;
  private JCheckBox                    chckbxShowLogos;
  private JButton                      btnPresetKodi;
  private JButton                      btnPresetXbmc;
  private JButton                      btnPresetMediaPortal1;
  private JButton                      btnPresetMediaPortal2;
  private JButton                      btnPresetPlex;
  private JCheckBox                    chckbxPersonalRatingFirst;
  private AutocompleteComboBox<String> cbRating;
  private JCheckBox                    chckbxIncludeExternalAudioStreams;
  private JCheckBox                    chckbxMovieTableTooltips;

  private JCheckBox                    chckbxCheckPoster;
  private JCheckBox                    chckbxCheckFanart;
  private JCheckBox                    chckbxCheckBanner;
  private JCheckBox                    chckbxCheckClearart;
  private JCheckBox                    chckbxCheckThumb;
  private JCheckBox                    chckbxCheckLogo;
  private JCheckBox                    chckbxCheckClearlogo;
  private JCheckBox                    chckbxCheckDiscart;

  private JCheckBox                    chckbxTitle;
  private JCheckBox                    chckbxSortableTitle;
  private JCheckBox                    chckbxOriginalTitle;
  private JCheckBox                    chckbxSortableOriginalTitle;
  private JCheckBox                    chckbxSortTitle;

  private final ItemListener           checkBoxListener;

  public MovieSettingsPanel() {

    checkBoxListener = e -> checkChanges();

    // UI initializations
    initComponents();
    initDataBindings();

    // logic initializations
    btnClearTraktData.addActionListener(e -> {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int confirm = JOptionPane.showOptionDialog(null, TmmResourceBundle.getString("Settings.trakt.clearmovies.hint"),
          TmmResourceBundle.getString("Settings.trakt.clearmovies"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
      if (confirm == JOptionPane.YES_OPTION) {
        TmmTask task = new MovieClearTraktTvTask();
        TmmTaskManager.getInstance().addUnnamedTask(task);
      }
    });

    btnPresetXbmc.addActionListener(evt -> settings.setDefaultSettingsForXbmc());
    btnPresetKodi.addActionListener(evt -> settings.setDefaultSettingsForKodi());
    btnPresetMediaPortal1.addActionListener(evt -> settings.setDefaultSettingsForMediaPortal1());
    btnPresetMediaPortal2.addActionListener(evt -> settings.setDefaultSettingsForMediaPortal2());
    btnPresetPlex.addActionListener(evt -> settings.setDefaultSettingsForPlex());

    buildCheckBoxes();
  }

  private void checkChanges() {
    settings.clearCheckImagesMovie();
    if (chckbxCheckPoster.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.POSTER);
    }
    if (chckbxCheckFanart.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.BACKGROUND);
    }
    if (chckbxCheckBanner.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.BANNER);
    }
    if (chckbxCheckClearart.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.CLEARART);
    }
    if (chckbxCheckThumb.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.THUMB);
    }
    if (chckbxCheckLogo.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.LOGO);
    }
    if (chckbxCheckClearlogo.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.CLEARLOGO);
    }
    if (chckbxCheckDiscart.isSelected()) {
      settings.addCheckImagesMovie(MediaArtwork.MediaArtworkType.DISC);
    }
  }

  private void buildCheckBoxes() {
    chckbxCheckPoster.removeItemListener(checkBoxListener);
    chckbxCheckFanart.removeItemListener(checkBoxListener);
    chckbxCheckBanner.removeItemListener(checkBoxListener);
    chckbxCheckClearart.removeItemListener(checkBoxListener);
    chckbxCheckThumb.removeItemListener(checkBoxListener);
    chckbxCheckLogo.removeItemListener(checkBoxListener);
    chckbxCheckClearlogo.removeItemListener(checkBoxListener);
    chckbxCheckDiscart.removeItemListener(checkBoxListener);
    clearSelection(chckbxCheckPoster, chckbxCheckFanart, chckbxCheckBanner, chckbxCheckClearart, chckbxCheckThumb, chckbxCheckLogo,
        chckbxCheckClearlogo, chckbxCheckDiscart);

    for (MediaArtwork.MediaArtworkType type : settings.getCheckImagesMovie()) {
      switch (type) {
        case POSTER:
          chckbxCheckPoster.setSelected(true);
          break;
        case BACKGROUND:
          chckbxCheckFanart.setSelected(true);
          break;

        case BANNER:
          chckbxCheckBanner.setSelected(true);
          break;

        case CLEARART:
          chckbxCheckClearart.setSelected(true);
          break;

        case THUMB:
          chckbxCheckThumb.setSelected(true);
          break;

        case LOGO:
          chckbxCheckLogo.setSelected(true);
          break;

        case CLEARLOGO:
          chckbxCheckClearlogo.setSelected(true);
          break;

        case DISC:
          chckbxCheckDiscart.setSelected(true);
          break;

        default:
          break;
      }
    }

    chckbxCheckPoster.addItemListener(checkBoxListener);
    chckbxCheckFanart.addItemListener(checkBoxListener);
    chckbxCheckBanner.addItemListener(checkBoxListener);
    chckbxCheckClearart.addItemListener(checkBoxListener);
    chckbxCheckThumb.addItemListener(checkBoxListener);
    chckbxCheckLogo.addItemListener(checkBoxListener);
    chckbxCheckClearlogo.addItemListener(checkBoxListener);
    chckbxCheckDiscart.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkbox : checkBoxes) {
      checkbox.setSelected(false);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      panelUiSettings.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp][grow]", "[][][][][][]")); // 16lp ~ width of the

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        chckbxShowLogos = new JCheckBox(TmmResourceBundle.getString("Settings.showlogos"));
        panelUiSettings.add(chckbxShowLogos, "cell 1 0 2 1");
      }
      {
        JLabel lblRating = new JLabel(TmmResourceBundle.getString("Settings.preferredrating"));
        panelUiSettings.add(lblRating, "cell 1 1 2 1");

        cbRating = new AutocompleteComboBox(Arrays.asList("imdb", "tmdb", "metascore", "rottenTomatoes"));
        panelUiSettings.add(cbRating, "cell 1 1 2 1");
      }
      {
        chckbxPersonalRatingFirst = new JCheckBox(TmmResourceBundle.getString("Settings.personalratingfirst"));
        panelUiSettings.add(chckbxPersonalRatingFirst, "cell 2 2 2 1");
      }
      {
        JLabel lblMovieFilter = new JLabel(TmmResourceBundle.getString("Settings.movietitlefilter"));
        panelUiSettings.add(lblMovieFilter, "cell 1 3 2 1");
        chckbxTitle = new JCheckBox(MovieTextMatcherList.TITLE.toString());
        panelUiSettings.add(chckbxTitle, "cell 2 4");
        chckbxSortableTitle = new JCheckBox(MovieTextMatcherList.TITLE_SORTABLE.toString());
        panelUiSettings.add(chckbxSortableTitle, "cell 2 4");
        JLabel lblSortableTitleHint = new JLabel(IconManager.HINT);
        lblSortableTitleHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.renamer.${titleSortable}"));
        panelUiSettings.add(lblSortableTitleHint, "cell 2 4");
        chckbxOriginalTitle = new JCheckBox(MovieTextMatcherList.ORIGINAL_TITLE.toString());
        panelUiSettings.add(chckbxOriginalTitle, "cell 2 4");
        chckbxSortableOriginalTitle = new JCheckBox(MovieTextMatcherList.ORIGINAL_TITLE_SORTABLE.toString());
        panelUiSettings.add(chckbxSortableOriginalTitle, "cell 2 4");
        JLabel lblSortableOriginalTitleHint = new JLabel(IconManager.HINT);
        lblSortableOriginalTitleHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.renamer.${titleSortable}"));
        panelUiSettings.add(lblSortableOriginalTitleHint, "cell 2 4");
        chckbxSortTitle = new JCheckBox(MovieTextMatcherList.SORTED_TITLE.toString());
        panelUiSettings.add(chckbxSortTitle, "cell 2 4");
      }
      {
        chckbxMovieTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.movie.showtabletooltips"));
        panelUiSettings.add(chckbxMovieTableTooltips, "cell 1 5 2 1");
      }
    }
    {
      JPanel panelAutomaticTasks = new JPanel();
      panelAutomaticTasks.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp][grow]", "")); // 16lp ~ width of the

      JLabel lblAutomaticTasksT = new TmmLabel(TmmResourceBundle.getString("Settings.automatictasks"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAutomaticTasks, lblAutomaticTasksT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#automatic-tasks"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        chckbxRenameAfterScrape = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticrename"));
        panelAutomaticTasks.add(chckbxRenameAfterScrape, "cell 1 0 2 1");

        JLabel lblAutomaticRenameHint = new JLabel(IconManager.HINT);
        lblAutomaticRenameHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticrename.desc"));
        panelAutomaticTasks.add(lblAutomaticRenameHint, "cell 1 0 2 1");

        chckbxTraktSync = new JCheckBox(TmmResourceBundle.getString("Settings.trakt"));
        panelAutomaticTasks.add(chckbxTraktSync, "cell 1 1 2 1");

        btnClearTraktData = new JButton(TmmResourceBundle.getString("Settings.trakt.clearmovies"));
        panelAutomaticTasks.add(btnClearTraktData, "cell 1 1 2 1");

        chckbxAutoUpdateOnStart = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticupdate"));
        panelAutomaticTasks.add(chckbxAutoUpdateOnStart, "cell 1 2 2 1");

        JLabel lblAutomaticUpdateHint = new JLabel(IconManager.HINT);
        lblAutomaticUpdateHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticupdate.desc"));
        panelAutomaticTasks.add(lblAutomaticUpdateHint, "cell 1 2 2 1");

      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp][grow]", "")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#misc-settings"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");
      {
        chckbxExtractArtworkFromVsmeta = new JCheckBox(TmmResourceBundle.getString("Settings.extractartworkfromvsmeta"));
        panelMisc.add(chckbxExtractArtworkFromVsmeta, "cell 1 0 2 1");

        chckbxBuildImageCache = new JCheckBox(TmmResourceBundle.getString("Settings.imagecacheimport"));
        panelMisc.add(chckbxBuildImageCache, "cell 1 1 2 1");

        JLabel lblBuildImageCacheHint = new JLabel(IconManager.HINT);
        lblBuildImageCacheHint.setToolTipText(TmmResourceBundle.getString("Settings.imagecacheimporthint"));
        panelMisc.add(lblBuildImageCacheHint, "cell 1 1 2 1");

        chckbxRuntimeFromMi = new JCheckBox(TmmResourceBundle.getString("Settings.runtimefrommediafile"));
        panelMisc.add(chckbxRuntimeFromMi, "cell 1 2 2 1");

        chckbxIncludeExternalAudioStreams = new JCheckBox(TmmResourceBundle.getString("Settings.includeexternalstreamsinnfo"));
        panelMisc.add(chckbxIncludeExternalAudioStreams, "cell 1 3 2 1");
      }
      {
        JLabel lblCheckImages = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
        panelMisc.add(lblCheckImages, "cell 1 4 2 1");

        JPanel panelCheckImages = new JPanel();
        panelCheckImages.setLayout(new MigLayout("hidemode 1, insets 0", "", ""));
        panelMisc.add(panelCheckImages, "cell 2 5");

        chckbxCheckPoster = new JCheckBox(TmmResourceBundle.getString("mediafiletype.poster"));
        panelCheckImages.add(chckbxCheckPoster, "cell 0 0");

        chckbxCheckFanart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.fanart"));
        panelCheckImages.add(chckbxCheckFanart, "cell 1 0");

        chckbxCheckBanner = new JCheckBox(TmmResourceBundle.getString("mediafiletype.banner"));
        panelCheckImages.add(chckbxCheckBanner, "cell 2 0");

        chckbxCheckClearart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.clearart"));
        panelCheckImages.add(chckbxCheckClearart, "cell 3 0");

        chckbxCheckThumb = new JCheckBox(TmmResourceBundle.getString("mediafiletype.thumb"));
        panelCheckImages.add(chckbxCheckThumb, "cell 4 0");

        chckbxCheckLogo = new JCheckBox(TmmResourceBundle.getString("mediafiletype.logo"));
        panelCheckImages.add(chckbxCheckLogo, "cell 5 0");

        chckbxCheckClearlogo = new JCheckBox(TmmResourceBundle.getString("mediafiletype.clearlogo"));
        panelCheckImages.add(chckbxCheckClearlogo, "cell 6 0");

        chckbxCheckDiscart = new JCheckBox(TmmResourceBundle.getString("mediafiletype.disc"));
        panelCheckImages.add(chckbxCheckDiscart, "cell 7 0");
      }
    }
    {
      JPanel panelPresets = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][15lp][][][grow]", "[]"));

      JLabel lblPresets = new TmmLabel(TmmResourceBundle.getString("Settings.preset"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelPresets, lblPresets, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#media-center-presets"));
      add(collapsiblePanel, "cell 0 6,growx,wmin 0");
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

  protected void initDataBindings() {
    Property movieSettingsBeanProperty_1 = BeanProperty.create("renameAfterScrape");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, chckbxRenameAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("syncTrakt");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxTraktSync,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("buildImageCacheOnImport");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3, chckbxBuildImageCache,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_4 = BeanProperty.create("runtimeFromMediaInfo");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_4, chckbxRuntimeFromMi,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property movieSettingsBeanProperty_7 = BeanProperty.create("preferPersonalRating");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_7,
        chckbxPersonalRatingFirst, jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property movieSettingsBeanProperty_8 = BeanProperty.create("preferredRating");
    Property autocompleteComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_8, cbRating,
        autocompleteComboBoxBeanProperty);
    autoBinding_8.bind();
    //
    Property movieSettingsBeanProperty_9 = BeanProperty.create("includeExternalAudioStreams");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_9,
        chckbxIncludeExternalAudioStreams, jCheckBoxBeanProperty);
    autoBinding_9.bind();
    //
    Property movieSettingsBeanProperty_10 = BeanProperty.create("showLogosPanel");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_10, chckbxShowLogos,
        jCheckBoxBeanProperty);
    autoBinding_10.bind();
    //
    Property movieSettingsBeanProperty_11 = BeanProperty.create("extractArtworkFromVsmeta");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_11,
        chckbxExtractArtworkFromVsmeta, jCheckBoxBeanProperty);
    autoBinding_11.bind();
    //
    Property movieSettingsBeanProperty_12 = BeanProperty.create("updateOnStart");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_12,
        chckbxAutoUpdateOnStart, jCheckBoxBeanProperty);
    autoBinding_12.bind();
    //
    Property movieSettingsBeanProperty_13 = BeanProperty.create("title");
    AutoBinding autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_13, chckbxTitle,
        jCheckBoxBeanProperty);
    autoBinding_13.bind();
    //
    Property movieSettingsBeanProperty_14 = BeanProperty.create("sortableTitle");
    AutoBinding autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_14, chckbxSortableTitle,
        jCheckBoxBeanProperty);
    autoBinding_14.bind();
    //
    Property movieSettingsBeanProperty_15 = BeanProperty.create("originalTitle");
    AutoBinding autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_15, chckbxOriginalTitle,
        jCheckBoxBeanProperty);
    autoBinding_15.bind();
    //
    Property movieSettingsBeanProperty_16 = BeanProperty.create("sortableOriginalTitle");
    AutoBinding autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_16,
        chckbxSortableOriginalTitle, jCheckBoxBeanProperty);
    autoBinding_16.bind();
    //
    Property movieSettingsBeanProperty_17 = BeanProperty.create("sortTitle");
    AutoBinding autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_17, chckbxSortTitle,
        jCheckBoxBeanProperty);
    autoBinding_17.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("showMovieTableTooltips");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxMovieTableTooltips,
        jCheckBoxBeanProperty);
    autoBinding.bind();
  }
}
