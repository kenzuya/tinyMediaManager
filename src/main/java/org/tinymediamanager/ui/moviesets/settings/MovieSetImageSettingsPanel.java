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
package org.tinymediamanager.ui.moviesets.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieImageSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class MovieSetImageSettingsPanel extends JPanel {
  private static final long           serialVersionUID = 7312645402037806284L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");
  private static final MovieSettings  SETTINGS         = MovieModuleManager.SETTINGS;

  private JCheckBox                   chckbxStoreMoviesetArtwork;
  private JTextField                  tfMovieSetArtworkFolder;
  private JCheckBox                   chckbxMovieSetArtwork;
  private JButton                     btnSelectFolder;
  private JCheckBox                   chckxKodiStyle;
  private JCheckBox                   chckxAutomatorStyle;

  /**
   * Instantiates a new movie image settings panel.
   */
  MovieSetImageSettingsPanel() {

    // UI init
    initComponents();
    initDataBindings();

    // further initializations
    btnSelectFolder.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("movieset.folderchooser.path");
      Path file = TmmUIHelper.selectDirectory(BUNDLE.getString("Settings.movieset.folderchooser"), path);
      if (file != null && Files.isDirectory(file)) {
        tfMovieSetArtworkFolder.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("movieset.folderchooser.path", file.toAbsolutePath().toString());
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[]"));
    {

      JPanel panelMovieSet = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][grow]"));

      JLabel lblTitle = new TmmLabel(BUNDLE.getString("Settings.movieset"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMovieSet, lblTitle, true);
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        chckbxMovieSetArtwork = new JCheckBox(BUNDLE.getString("Settings.movieset.store.movie"));
        panelMovieSet.add(chckbxMovieSetArtwork, "cell 1 0 2 1");

        chckbxStoreMoviesetArtwork = new JCheckBox(BUNDLE.getString("Settings.movieset.store"));
        panelMovieSet.add(chckbxStoreMoviesetArtwork, "cell 1 1 2 1");

        {

          JPanel panelFolderSettings = new JPanel();
          panelMovieSet.add(panelFolderSettings, "cell 2 2,grow");
          panelFolderSettings.setLayout(new MigLayout("insets 0", "[][grow]", "[][][]"));
          JLabel lblFoldername = new JLabel(BUNDLE.getString("Settings.movieset.foldername"));
          panelFolderSettings.add(lblFoldername, "cell 0 0,alignx right");

          tfMovieSetArtworkFolder = new JTextField();
          panelFolderSettings.add(tfMovieSetArtworkFolder, "flowx,cell 1 0");
          tfMovieSetArtworkFolder.setColumns(40);

          JLabel folderStyle = new JLabel(BUNDLE.getString("Settings.movieset.foldername.style"));
          panelFolderSettings.add(folderStyle, "cell 0 1,alignx right");

          chckxKodiStyle = new JCheckBox(BUNDLE.getString("Settings.movieset.foldername.kodi"));
          panelFolderSettings.add(chckxKodiStyle, "cell 1 1");

          chckxAutomatorStyle = new JCheckBox(BUNDLE.getString("Settings.movieset.foldername.automator"));
          panelFolderSettings.add(chckxAutomatorStyle, "cell 1 2");

          ButtonGroup buttonGroup = new ButtonGroup();
          buttonGroup.add(chckxKodiStyle);
          buttonGroup.add(chckxAutomatorStyle);

          btnSelectFolder = new JButton(BUNDLE.getString("Settings.movieset.buttonselect"));
          panelFolderSettings.add(btnSelectFolder, "cell 1 0");
        }
      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, String> settingsBeanProperty_12 = BeanProperty.create("movieSetArtworkFolder");
    BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding<MovieSettings, String, JTextField, String> autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, SETTINGS,
        settingsBeanProperty_12, tfMovieSetArtworkFolder, jTextFieldBeanProperty);
    autoBinding_16.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_13 = BeanProperty.create("enableMovieSetArtworkFolder");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, SETTINGS,
        settingsBeanProperty_13, chckbxStoreMoviesetArtwork, jCheckBoxBeanProperty);
    autoBinding_17.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_18 = BeanProperty.create("enableMovieSetArtworkMovieFolder");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_22 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, SETTINGS,
        settingsBeanProperty_18, chckbxMovieSetArtwork, jCheckBoxBeanProperty);
    autoBinding_22.bind();
    //
    BeanProperty<MovieSettings, Boolean> movieSettingsBeanProperty = BeanProperty.create("movieSetArtworkFolderStyleKodi");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, SETTINGS,
        movieSettingsBeanProperty, chckxKodiStyle, jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<MovieSettings, Boolean> movieSettingsBeanProperty_1 = BeanProperty.create("movieSetArtworkFolderStyleAutomator");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, SETTINGS,
        movieSettingsBeanProperty_1, chckxAutomatorStyle, jCheckBoxBeanProperty);
    autoBinding_1.bind();
  }
}
