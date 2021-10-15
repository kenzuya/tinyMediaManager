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

import java.awt.GridBagConstraints;
import java.util.Map;

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
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.TvShowSettingsDefaults;
import org.tinymediamanager.thirdparty.trakttv.TvShowClearTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class TvShowSettingsPanel extends JPanel {
  private static final long    serialVersionUID = -675729644848101096L;
  private static final int     COL_COUNT        = 7;

  private final TvShowSettings settings         = TvShowModuleManager.getInstance().getSettings();

  private JCheckBox            chckbxImageCache;
  private JCheckBox            chckbxExtractArtworkFromVsmeta;
  private JCheckBox            chckbxTraktTv;
  private JButton              btnClearTraktTvShows;
  private JButton              btnPresetKodi;
  private JButton              btnPresetXbmc;
  private JButton              btnPresetMediaPortal1;
  private JButton              btnPresetMediaPortal2;
  private JButton              btnPresetPlex;
  private JCheckBox            chckbxRenameAfterScrape;
  private JCheckBox            chckbxARDAfterScrape;
  private JCheckBox            chckbxAutoUpdateOnStart;

  private JCheckBox            chckbxMetadataFromMediainfo;
  private JCheckBox            chckbxTraktCollection;
  private JCheckBox            chckbxTraktWatched;
  private JCheckBox            chckbxTraktRating;

  /**
   * Instantiates a new tv show settings panel.
   */
  TvShowSettingsPanel() {
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
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelAutomaticTasks = new JPanel();
      // 16lp ~ width of the checkbox
      panelAutomaticTasks.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][10lp!][][][10lp!][]"));

      JLabel lblAutomaticTasksT = new TmmLabel(TmmResourceBundle.getString("Settings.automatictasks"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAutomaticTasks, lblAutomaticTasksT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#automatic-tasks"));
      JPanel panelPresets = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][15lp][][][grow]", "[]"));

      JLabel lblPresets = new TmmLabel(TmmResourceBundle.getString("Settings.preset"), H3);
      CollapsiblePanel collapsiblePanel_1 = new CollapsiblePanel(panelPresets, lblPresets, true);
      collapsiblePanel_1.addExtraTitleComponent(new DocsButton("/tvshows/settings#media-center-presets"));
      add(collapsiblePanel_1, "cell 0 0,growx,wmin 0");

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
      {
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
    Property tvShowSettingsBeanProperty_5 = BeanProperty.create("renameAfterScrape");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_5, chckbxRenameAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_6.bind();
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
    Property tvShowSettingsBeanProperty_9 = BeanProperty.create("useMediainfoMetadata");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_9,
        chckbxMetadataFromMediainfo, jCheckBoxBeanProperty);
    autoBinding_12.bind();
    //
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
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
    Property tvShowSettingsBeanProperty_20 = BeanProperty.create("ardAfterScrape");
    AutoBinding autoBinding_20 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, tvShowSettingsBeanProperty_20, chckbxARDAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_20.bind();
  }
}
