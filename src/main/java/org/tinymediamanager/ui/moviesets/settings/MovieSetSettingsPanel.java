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

import java.util.ResourceBundle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
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
  private static final long           serialVersionUID = -4173835431245178069L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");

  private final MovieSettings         settings         = MovieModuleManager.SETTINGS;

  private JCheckBox                   chckbxShowMissingMovies;
  private JCheckBox                   chckbxTvShowTableTooltips;

  public MovieSetSettingsPanel() {
    initComponents();
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      panelUiSettings.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp][grow]", "[][]")); // 16lp ~ width of the

      JLabel lblUiSettings = new TmmLabel(BUNDLE.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/tvshows/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        chckbxShowMissingMovies = new JCheckBox(BUNDLE.getString("Settings.movieset.showmissingmovies"));
        panelUiSettings.add(chckbxShowMissingMovies, "cell 1 0 2 1");
      }
      {
        chckbxTvShowTableTooltips = new JCheckBox(BUNDLE.getString("Settings.movieset.showtabletooltips"));
        panelUiSettings.add(chckbxTvShowTableTooltips, "cell 1 1 2 1");
      }
    }
    initDataBindings();
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
