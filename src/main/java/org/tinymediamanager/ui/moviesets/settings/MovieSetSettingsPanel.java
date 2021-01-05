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
package org.tinymediamanager.ui.moviesets.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieSetSettingsPanel is used for displaying some movie set related settings
 * 
 * @author Manuel Laggner
 */
public class MovieSetSettingsPanel extends JPanel {
  private static final long   serialVersionUID = -4173835431245178069L;

  private final MovieSettings settings         = MovieModuleManager.SETTINGS;

  private JCheckBox           chckbxShowMissingMovies;
  private JCheckBox           chckbxTvShowTableTooltips;

  private JCheckBox           chckbxCheckPoster;
  private JCheckBox           chckbxCheckFanart;
  private JCheckBox           chckbxCheckBanner;
  private JCheckBox           chckbxCheckClearart;
  private JCheckBox           chckbxCheckThumb;
  private JCheckBox           chckbxCheckLogo;
  private JCheckBox           chckbxCheckClearlogo;
  private JCheckBox           chckbxCheckDiscart;

  private final ItemListener  checkBoxListener;

  public MovieSetSettingsPanel() {
    checkBoxListener = e -> checkChanges();

    // UI initializations
    initComponents();
    initDataBindings();

    buildCheckBoxes();
  }

  private void checkChanges() {
    settings.clearCheckImagesMovieSet();
    if (chckbxCheckPoster.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.POSTER);
    }
    if (chckbxCheckFanart.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.BACKGROUND);
    }
    if (chckbxCheckBanner.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.BANNER);
    }
    if (chckbxCheckClearart.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.CLEARART);
    }
    if (chckbxCheckThumb.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.THUMB);
    }
    if (chckbxCheckLogo.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.LOGO);
    }
    if (chckbxCheckClearlogo.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.CLEARLOGO);
    }
    if (chckbxCheckDiscart.isSelected()) {
      settings.addCheckImagesMovieSet(MediaArtwork.MediaArtworkType.DISC);
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

    for (MediaArtwork.MediaArtworkType type : settings.getCheckImagesMovieSet()) {
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
    setLayout(new MigLayout("", "[600lp,grow]", "[][]"));
    {
      JPanel panelUiSettings = new JPanel();
      panelUiSettings.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][]")); // 16lp ~ width of the

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        chckbxShowMissingMovies = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.showmissingmovies"));
        panelUiSettings.add(chckbxShowMissingMovies, "cell 1 0 2 1");
      }
      {
        chckbxTvShowTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.movieset.showtabletooltips"));
        panelUiSettings.add(chckbxTvShowTableTooltips, "cell 1 1 2 1");
      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      add(collapsiblePanel, "cell 0 1,growx,wmin 0");
      {
        JLabel lblCheckImages = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
        panelMisc.add(lblCheckImages, "cell 1 0 2 1");

        JPanel panelCheckImages = new JPanel();
        panelCheckImages.setLayout(new MigLayout("hidemode 1, insets 0", "", ""));
        panelMisc.add(panelCheckImages, "cell 2 1");

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
  }

  protected void initDataBindings() {
    Property movieSettingsBeanProperty = BeanProperty.create("displayMovieSetMissingMovies");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxShowMissingMovies,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("showMovieSetTableTooltips");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1,
        chckbxTvShowTableTooltips, jCheckBoxBeanProperty);
    autoBinding_1.bind();
  }
}
