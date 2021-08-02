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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.event.ItemListener;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieScraperSettingsPanel} is used to display NFO related settings.
 * 
 * @author Manuel Laggner
 */
class MovieScraperNfoSettingsPanel extends JPanel {
  private static final long                    serialVersionUID = -299825914193235308L;

  private final MovieSettings                  settings         = MovieModuleManager.getInstance().getSettings();
  private final ItemListener                   checkBoxListener;
  private final ItemListener                   comboBoxListener;

  private JComboBox<MovieConnectors>           cbNfoFormat;
  private JCheckBox                            cbMovieNfoFilename1;
  private JCheckBox                            cbMovieNfoFilename2;
  private JComboBox<CertificationStyleWrapper> cbCertificationStyle;
  private JCheckBox                            chckbxWriteCleanNfo;
  private JComboBox<MediaLanguages>            cbNfoLanguage;
  private JComboBox<DateField>                 cbDatefield;
  private JHintCheckBox                        chckbxCreateOutline;
  private JCheckBox                            chckbxOutlineFirstSentence;
  private JCheckBox                            chckbxSingleStudio;

  /**
   * Instantiates a new movie scraper settings panel.
   */
  MovieScraperNfoSettingsPanel() {
    checkBoxListener = e -> checkChanges();
    comboBoxListener = e -> checkChanges();

    // UI init
    initComponents();
    initDataBindings();

    // data init
    // set default certification style when changing NFO style
    cbNfoFormat.addItemListener(e -> {
      if (cbNfoFormat.getSelectedItem() == MovieConnectors.MP) {
        for (int i = 0; i < cbCertificationStyle.getItemCount(); i++) {
          CertificationStyleWrapper wrapper = cbCertificationStyle.getItemAt(i);
          if (wrapper.style == CertificationStyle.TECHNICAL) {
            cbCertificationStyle.setSelectedItem(wrapper);
            break;
          }
        }
      }
      else if (cbNfoFormat.getSelectedItem() == MovieConnectors.XBMC) {
        for (int i = 0; i < cbCertificationStyle.getItemCount(); i++) {
          CertificationStyleWrapper wrapper = cbCertificationStyle.getItemAt(i);
          if (wrapper.style == CertificationStyle.LARGE) {
            cbCertificationStyle.setSelectedItem(wrapper);
            break;
          }
        }
      }
    });

    // implement checkBoxListener for preset events
    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName()) || "wizard".equals(evt.getPropertyName())) {
        buildCheckBoxes();
        buildComboBoxes();
      }
    });

    buildCheckBoxes();
    buildComboBoxes();
  }

  private void buildCheckBoxes() {
    cbMovieNfoFilename1.removeItemListener(checkBoxListener);
    cbMovieNfoFilename2.removeItemListener(checkBoxListener);

    clearSelection(cbMovieNfoFilename1, cbMovieNfoFilename2);

    // NFO filenames
    List<MovieNfoNaming> movieNfoFilenames = settings.getNfoFilenames();
    if (movieNfoFilenames.contains(MovieNfoNaming.FILENAME_NFO)) {
      cbMovieNfoFilename1.setSelected(true);
    }
    if (movieNfoFilenames.contains(MovieNfoNaming.MOVIE_NFO)) {
      cbMovieNfoFilename2.setSelected(true);
    }

    cbMovieNfoFilename1.addItemListener(checkBoxListener);
    cbMovieNfoFilename2.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
    }
  }

  private void buildComboBoxes() {
    cbCertificationStyle.removeItemListener(comboBoxListener);
    cbCertificationStyle.removeAllItems();

    // certification examples
    for (CertificationStyle style : CertificationStyle.values()) {
      CertificationStyleWrapper wrapper = new CertificationStyleWrapper();
      wrapper.style = style;
      cbCertificationStyle.addItem(wrapper);
      if (style == settings.getCertificationStyle()) {
        cbCertificationStyle.setSelectedItem(wrapper);
      }
    }

    cbCertificationStyle.addItemListener(comboBoxListener);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelNfo = new JPanel();
      panelNfo.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][][][][][]")); // 16lp ~ width of the

      JLabel lblNfoT = new TmmLabel(TmmResourceBundle.getString("Settings.nfo"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelNfo, lblNfoT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#nfo-settings"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        JLabel lblNfoFormat = new JLabel(TmmResourceBundle.getString("Settings.nfoFormat"));
        panelNfo.add(lblNfoFormat, "cell 1 0 2 1");

        cbNfoFormat = new JComboBox(MovieConnectors.values());
        panelNfo.add(cbNfoFormat, "cell 1 0 2 1");

        JButton docsButton = new DocsButton("/movies/nfo-formats");
        panelNfo.add(docsButton, "cell 1 0 2 1");

        {
          JPanel panelNfoFormat = new JPanel();
          panelNfo.add(panelNfoFormat, "cell 1 1 2 1");
          panelNfoFormat.setLayout(new MigLayout("insets 0", "[][]", "[][]"));

          JLabel lblNfoFileNaming = new JLabel(TmmResourceBundle.getString("Settings.nofFileNaming"));
          panelNfoFormat.add(lblNfoFileNaming, "cell 0 0");

          cbMovieNfoFilename1 = new JCheckBox(TmmResourceBundle.getString("Settings.moviefilename") + ".nfo");
          panelNfoFormat.add(cbMovieNfoFilename1, "cell 1 0");

          cbMovieNfoFilename2 = new JCheckBox("movie.nfo");
          panelNfoFormat.add(cbMovieNfoFilename2, "cell 1 1");
        }

        chckbxWriteCleanNfo = new JCheckBox(TmmResourceBundle.getString("Settings.writecleannfo"));
        panelNfo.add(chckbxWriteCleanNfo, "cell 1 2 2 1");

        JLabel lblNfoDatefield = new JLabel(TmmResourceBundle.getString("Settings.dateadded"));
        panelNfo.add(lblNfoDatefield, "cell 1 4 2 1");

        cbDatefield = new JComboBox(DateField.values());
        panelNfo.add(cbDatefield, "cell 1 4 2 1");

        JLabel lblNfoLanguage = new JLabel(TmmResourceBundle.getString("Settings.nfolanguage"));
        panelNfo.add(lblNfoLanguage, "cell 1 5 2 1");

        cbNfoLanguage = new JComboBox(MediaLanguages.valuesSorted());
        panelNfo.add(cbNfoLanguage, "cell 1 5 2 1");

        JLabel lblNfoLanguageDesc = new JLabel(TmmResourceBundle.getString("Settings.nfolanguage.desc"));
        panelNfo.add(lblNfoLanguageDesc, "cell 2 6");

        JLabel lblCertificationStyle = new JLabel(TmmResourceBundle.getString("Settings.certificationformat"));
        panelNfo.add(lblCertificationStyle, "flowx,cell 1 7 2 1");

        cbCertificationStyle = new JComboBox();
        panelNfo.add(cbCertificationStyle, "cell 1 7 2 1,wmin 0");

        chckbxCreateOutline = new JHintCheckBox(TmmResourceBundle.getString("Settings.createoutline"));
        chckbxCreateOutline.setToolTipText(TmmResourceBundle.getString("Settings.createoutline.hint"));
        chckbxCreateOutline.setHintIcon(IconManager.HINT);
        panelNfo.add(chckbxCreateOutline, "cell 1 8 2 1");

        chckbxOutlineFirstSentence = new JCheckBox(TmmResourceBundle.getString("Settings.outlinefirstsentence"));
        panelNfo.add(chckbxOutlineFirstSentence, "cell 2 9");

        chckbxSingleStudio = new JCheckBox(TmmResourceBundle.getString("Settings.singlestudio"));
        panelNfo.add(chckbxSingleStudio, "cell 1 10 2 1");
      }
    }
  }

  /**
   * check changes of checkboxes
   */
  private void checkChanges() {
    // set NFO filenames
    settings.clearNfoFilenames();
    if (cbMovieNfoFilename1.isSelected()) {
      settings.addNfoFilename(MovieNfoNaming.FILENAME_NFO);
    }
    if (cbMovieNfoFilename2.isSelected()) {
      settings.addNfoFilename(MovieNfoNaming.MOVIE_NFO);
    }

    CertificationStyleWrapper wrapper = (CertificationStyleWrapper) cbCertificationStyle.getSelectedItem();
    if (wrapper != null && settings.getCertificationStyle() != wrapper.style) {
      settings.setCertificationStyle(wrapper.style);
    }
  }

  /*
   * helper for displaying the combobox with an example
   */
  private static class CertificationStyleWrapper {
    private CertificationStyle style;

    @Override
    public String toString() {
      String bundleTag = TmmResourceBundle.getString("Settings.certification." + style.name().toLowerCase(Locale.ROOT));
      return bundleTag.replace("{}", CertificationStyle.formatCertification(MediaCertification.DE_FSK16, style));
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty_11 = BeanProperty.create("movieConnector");
    Property jComboBoxBeanProperty_1 = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_11, cbNfoFormat,
        jComboBoxBeanProperty_1);
    autoBinding_9.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("writeCleanNfo");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, chckbxWriteCleanNfo,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty_1 = BeanProperty.create("nfoLanguage");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, cbNfoLanguage,
        jComboBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("createOutline");
    Property jHintCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxCreateOutline,
        jHintCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("outlineFirstSentence");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3,
        chckbxOutlineFirstSentence, jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxCreateOutline, jHintCheckBoxBeanProperty,
        chckbxOutlineFirstSentence, jCheckBoxBeanProperty_1);
    autoBinding_4.bind();
    //
    Property movieSettingsBeanProperty_4 = BeanProperty.create("nfoDateAddedField");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_4, cbDatefield,
        jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property movieSettingsBeanProperty_5 = BeanProperty.create("nfoWriteSingleStudio");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_5, chckbxSingleStudio,
        jCheckBoxBeanProperty);
    autoBinding_6.bind();
  }
}
