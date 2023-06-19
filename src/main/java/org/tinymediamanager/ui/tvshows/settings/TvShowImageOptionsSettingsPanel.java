/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import java.awt.Dimension;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.filenaming.TvShowExtraFanartNaming;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowScraperSettingsPanel} shows scraper options for the meta data scraper.
 * 
 * @author Manuel Laggner
 */
class TvShowImageOptionsSettingsPanel extends JPanel {
  private final TvShowSettings      settings = TvShowModuleManager.getInstance().getSettings();

  private final ItemListener        checkBoxListener;

  private JCheckBox                 cbActorImages;
  private JSpinner                  spDownloadCountExtrafanart;
  private JCheckBox                 chckbxEnableExtrafanart;
  private JComboBox<MediaLanguages> cbScraperLanguage;
  private JComboBox                 cbImagePosterSize;
  private JComboBox                 cbImageFanartSize;
  private JCheckBox                 chckbxExtraFanart1;
  private JCheckBox                 chckbxExtraFanart2;
  private JCheckBox                 chckbxPreferLanguage;

  /**
   * Instantiates a new movie artwork options settings panel.
   */
  TvShowImageOptionsSettingsPanel() {
    checkBoxListener = e -> checkChanges();

    // UI init
    initComponents();
    initDataBindings();

    // further init
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(chckbxExtraFanart1);
    buttonGroup.add(chckbxExtraFanart2);

    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName())) {
        buildCheckBoxes();
      }
    });

    buildCheckBoxes();
  }

  private void buildCheckBoxes() {
    // initialize
    clearSelection(chckbxExtraFanart1, chckbxExtraFanart2);

    // extrafanart filenames
    for (TvShowExtraFanartNaming fanart : settings.getExtraFanartFilenames()) {
      switch (fanart) {
        case EXTRAFANART:
          chckbxExtraFanart1.setSelected(true);
          break;

        case FOLDER_EXTRAFANART:
          chckbxExtraFanart2.setSelected(true);
      }
    }

    // listen to changes of the checkboxes
    chckbxExtraFanart1.addItemListener(checkBoxListener);
    chckbxExtraFanart2.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.removeItemListener(checkBoxListener);
      checkBox.setSelected(false);
    }
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    // set poster filenames
    settings.clearExtraFanartFilenames();

    if (chckbxExtraFanart1.isSelected()) {
      settings.addExtraFanartFilename(TvShowExtraFanartNaming.EXTRAFANART);
    }
    if (chckbxExtraFanart2.isSelected()) {
      settings.addExtraFanartFilename(TvShowExtraFanartNaming.FOLDER_EXTRAFANART);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][15lp!][][][grow][]")); // 16lp ~ width
      // of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#advanced-options-1"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        JLabel lblScraperLanguage = new JLabel(TmmResourceBundle.getString("Settings.preferredLanguage"));
        panelOptions.add(lblScraperLanguage, "cell 1 0 2 1");

        cbScraperLanguage = new JComboBox(MediaLanguages.allValuesSorted());
        panelOptions.add(cbScraperLanguage, "cell 1 0 2 1");

        JLabel lblImageTmdbPosterSize = new JLabel(TmmResourceBundle.getString("image.poster.size"));
        panelOptions.add(lblImageTmdbPosterSize, "cell 1 1 2 1");

        cbImagePosterSize = new JComboBox(MediaArtwork.PosterSizes.values());
        panelOptions.add(cbImagePosterSize, "cell 1 1 2 1");

        JLabel lblImageTmdbFanartSize = new JLabel(TmmResourceBundle.getString("image.fanart.size"));
        panelOptions.add(lblImageTmdbFanartSize, "cell 1 2 2 1");

        cbImageFanartSize = new JComboBox(MediaArtwork.FanartSizes.values());
        panelOptions.add(cbImageFanartSize, "cell 1 2 2 1");

        chckbxPreferLanguage = new JCheckBox(TmmResourceBundle.getString("Settings.default.autoscrape.language"));
        panelOptions.add(chckbxPreferLanguage, "cell 1 3 2 1");

        cbActorImages = new JCheckBox(TmmResourceBundle.getString("Settings.actor.download"));
        panelOptions.add(cbActorImages, "cell 1 5 2 1");

        chckbxEnableExtrafanart = new JCheckBox(TmmResourceBundle.getString("Settings.enable.extrafanart"));
        panelOptions.add(chckbxEnableExtrafanart, "cell 1 6 2 1");

        JPanel panel = new JPanel();
        panelOptions.add(panel, "cell 2 7,growx");
        panel.setLayout(new MigLayout("insets 0", "[][20lp!][]", "[]"));

        chckbxExtraFanart1 = new JCheckBox("fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panel.add(chckbxExtraFanart1, "cell 0 0");

        chckbxExtraFanart2 = new JCheckBox("extrafanart/fanartX." + TmmResourceBundle.getString("Settings.artwork.extension"));
        panel.add(chckbxExtraFanart2, "cell 2 0");

        JLabel lblDownloadCount = new JLabel(TmmResourceBundle.getString("Settings.amount.autodownload"));
        panelOptions.add(lblDownloadCount, "cell 2 8");

        spDownloadCountExtrafanart = new JSpinner();
        spDownloadCountExtrafanart.setMinimumSize(new Dimension(60, 20));
        panelOptions.add(spDownloadCountExtrafanart, "cell 2 8");
      }
    }
  }

  protected void initDataBindings() {
    Property tvShowSettingsBeanProperty = BeanProperty.create("writeActorImages");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty, cbActorImages,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property tvShowSettingsBeanProperty_1 = BeanProperty.create("imageExtraFanartCount");
    Property jSpinnerBeanProperty_1 = BeanProperty.create("value");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_1,
        spDownloadCountExtrafanart, jSpinnerBeanProperty_1);
    autoBinding_3.bind();
    //
    Property tvShowSettingsBeanProperty_2 = BeanProperty.create("imageExtraFanart");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_2, chckbxEnableExtrafanart,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property jSpinnerBeanProperty = BeanProperty.create("enabled");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, chckbxEnableExtrafanart, jCheckBoxBeanProperty,
        spDownloadCountExtrafanart, jSpinnerBeanProperty);
    autoBinding_2.bind();
    //
    Property tvShowSettingsBeanProperty_3 = BeanProperty.create("imageScraperLanguage");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_3, cbScraperLanguage,
        jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property tvShowSettingsBeanProperty_4 = BeanProperty.create("imagePosterSize");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_4, cbImagePosterSize,
        jComboBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property tvShowSettingsBeanProperty_5 = BeanProperty.create("imageFanartSize");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_5, cbImageFanartSize,
        jComboBoxBeanProperty);
    autoBinding_7.bind();
    //
    Property jCheckBoxBeanProperty_2 = BeanProperty.create("enabled");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart, jCheckBoxBeanProperty, chckbxExtraFanart1,
        jCheckBoxBeanProperty_2);
    autoBinding_9.bind();
    //
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxEnableExtrafanart, jCheckBoxBeanProperty, chckbxExtraFanart2,
        jCheckBoxBeanProperty_2);
    autoBinding_10.bind();
    //
    Property tvShowSettingsBeanProperty_7 = BeanProperty.create("imageLanguagePriority");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_7, chckbxPreferLanguage,
        jCheckBoxBeanProperty);
    autoBinding_11.bind();
  }
}
