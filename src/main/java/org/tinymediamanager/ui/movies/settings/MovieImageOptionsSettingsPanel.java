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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieScraperSettingsPanel} shows scraper options for the artwork scraper.
 *
 * @author Manuel Laggner
 */
class MovieImageOptionsSettingsPanel extends JPanel {
  private final MovieSettings       settings = MovieModuleManager.getInstance().getSettings();

  private JComboBox                 cbImagePosterSize;
  private JComboBox                 cbImageFanartSize;

  private JComboBox<MediaLanguages> cbScraperLanguage;
  private JCheckBox                 chckbxPreferLanguage;

  /**
   * Instantiates a new movie scraper settings panel.
   */
  MovieImageOptionsSettingsPanel() {
    // UI init
    initComponents();
    initDataBindings();
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[700lp,grow]", "[]"));

    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#advanced-options-1"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        JLabel lblScraperLanguage = new JLabel(TmmResourceBundle.getString("Settings.preferredLanguage"));
        panelOptions.add(lblScraperLanguage, "cell 1 0 2 1");

        cbScraperLanguage = new JComboBox(MediaLanguages.allValuesSorted());
        panelOptions.add(cbScraperLanguage, "cell 1 0");

        JLabel lblImageTmdbPosterSize = new JLabel(TmmResourceBundle.getString("image.poster.size"));
        panelOptions.add(lblImageTmdbPosterSize, "cell 1 1 2 1");

        cbImagePosterSize = new JComboBox(MediaArtwork.PosterSizes.values());
        panelOptions.add(cbImagePosterSize, "cell 1 1");

        JLabel lblImageTmdbFanartSize = new JLabel(TmmResourceBundle.getString("image.fanart.size"));
        panelOptions.add(lblImageTmdbFanartSize, "cell 1 2 2 1");

        cbImageFanartSize = new JComboBox(MediaArtwork.FanartSizes.values());
        panelOptions.add(cbImageFanartSize, "cell 1 2");

        chckbxPreferLanguage = new JCheckBox(TmmResourceBundle.getString("Settings.default.autoscrape.language"));
        panelOptions.add(chckbxPreferLanguage, "cell 1 3 2 1");
      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, MediaArtwork.PosterSizes> settingsBeanProperty_5 = BeanProperty.create("imagePosterSize");
    BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, MediaArtwork.PosterSizes, JComboBox, Object> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
        settings, settingsBeanProperty_5, cbImagePosterSize, jComboBoxBeanProperty);
    autoBinding_4.bind();
    //

    BeanProperty<MovieSettings, MediaArtwork.FanartSizes> settingsBeanProperty_6 = BeanProperty.create("imageFanartSize");
    AutoBinding<MovieSettings, MediaArtwork.FanartSizes, JComboBox, Object> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
        settings, settingsBeanProperty_6, cbImageFanartSize, jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    BeanProperty<MovieSettings, MediaLanguages> movieSettingsBeanProperty = BeanProperty.create("imageScraperLanguage");
    AutoBinding<MovieSettings, MediaLanguages, JComboBox, Object> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        movieSettingsBeanProperty, cbScraperLanguage, jComboBoxBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_10 = BeanProperty.create("imageLanguagePriority");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_10, chckbxPreferLanguage, jCheckBoxBeanProperty);
    autoBinding_11.bind();
    //
  }
}
