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
package org.tinymediamanager.ui.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class UiSettingsPanel is used to display some UI related settings
 * 
 * @author Manuel Laggner
 */
class UiSettingsPanel extends JPanel {
  private static final long          serialVersionUID   = 6409982195347794360L;

  private static final Logger        LOGGER             = LoggerFactory.getLogger(UiSettingsPanel.class);
  private static final Integer[]     DEFAULT_FONT_SIZES = { 12, 14, 16, 18, 20, 22, 24, 26, 28 };

  private final Settings             settings           = Settings.getInstance();
  private final List<LocaleComboBox> locales            = new ArrayList<>();

  private JComboBox                  cbLanguage;
  private LinkTextArea               lblLinkTranslate;
  private JComboBox                  cbFontSize;
  private JComboBox                  cbFontFamily;
  private JLabel                     lblLanguageChangeHint;
  private JCheckBox                  chckbxStoreWindowPreferences;
  private JComboBox                  cbTheme;
  private JLabel                     lblThemeHint;
  private JCheckBox                  chckbxShowMemory;
  private JComboBox                  cbDatefield;
  private JCheckBox                  chckbxImageChooserLastFolder;
  private JCheckBox                  chckbxImageChooserEntityFolder;
  private JSpinner                   spUpdateInterval;
  private JCheckBox                  chckbxAutomaticUpdates;
  private JLabel                     lblUpdateHint;
  private CollapsiblePanel           collapsiblePanelUpdate;

  UiSettingsPanel() {
    LocaleComboBox actualLocale = null;
    Locale settingsLang = Utils.getLocaleFromLanguage(Settings.getInstance().getLanguage());
    for (Locale l : Utils.getLanguages()) {
      LocaleComboBox localeComboBox = new LocaleComboBox(l);
      locales.add(localeComboBox);
      if (l.equals(settingsLang)) {
        actualLocale = localeComboBox;
      }
    }
    Collections.sort(locales);

    // ui init
    initComponents();
    initDataBindings();

    // data init
    if (actualLocale != null) {
      cbLanguage.setSelectedItem(actualLocale);
    }

    cbFontFamily.setSelectedItem(Settings.getInstance().getFontFamily());
    int index = cbFontFamily.getSelectedIndex();
    if (index < 0) {
      cbFontFamily.setSelectedItem("Dialog");
      index = cbFontFamily.getSelectedIndex();
    }
    if (index < 0) {
      cbFontFamily.setSelectedIndex(0);
    }
    cbFontSize.setSelectedItem(Settings.getInstance().getFontSize());
    index = cbFontSize.getSelectedIndex();
    if (index < 0) {
      cbFontSize.setSelectedIndex(0);
    }
    cbTheme.setSelectedItem(Settings.getInstance().getTheme());
    index = cbTheme.getSelectedIndex();
    if (index < 0) {
      cbTheme.setSelectedIndex(0);
    }

    lblLinkTranslate.addActionListener(arg0 -> {
      try {
        TmmUIHelper.browseUrl(lblLinkTranslate.getText());
      }
      catch (Exception e) {
        LOGGER.error(e.getMessage());
        MessageManager.instance.pushMessage(
            new Message(MessageLevel.ERROR, lblLinkTranslate.getText(), "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));//$NON-NLS-2$
      }
    });

    ActionListener actionListener = e -> SwingUtilities.invokeLater(this::checkChanges);
    cbLanguage.addActionListener(actionListener);
    cbFontFamily.addActionListener(actionListener);
    cbFontSize.addActionListener(actionListener);
    cbTheme.addActionListener(actionListener);
    chckbxAutomaticUpdates.addActionListener(actionListener);

    Settings.getInstance().addPropertyChangeListener(evt -> {
      switch (evt.getPropertyName()) {
        case "theme":
          if (!Settings.getInstance().getTheme().equals(cbTheme.getSelectedItem())) {
            cbTheme.setSelectedItem(Settings.getInstance().getTheme());
          }
          break;

        case "fontSize":
          if (cbFontSize.getSelectedItem() != null && Settings.getInstance().getFontSize() != (Integer) cbFontSize.getSelectedItem()) {
            cbFontSize.setSelectedItem(Settings.getInstance().getFontSize());
          }
          break;

        case "fontFamily":
          if (!Settings.getInstance().getFontFamily().equals(cbFontFamily.getSelectedItem())) {
            cbFontFamily.setSelectedItem(Settings.getInstance().getFontFamily());
          }
          break;
      }
    });

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(chckbxImageChooserLastFolder);
    buttonGroup.add(chckbxImageChooserEntityFolder);

    if (settings.isImageChooserUseEntityFolder()) {
      chckbxImageChooserEntityFolder.setSelected(true);
    }

    chckbxImageChooserLastFolder.addActionListener(actionListener);
    chckbxImageChooserEntityFolder.addActionListener(actionListener);

    if (!chckbxAutomaticUpdates.isSelected()) {
      lblUpdateHint.setText(TmmResourceBundle.getString("Settings.updatecheck.hint"));
    }

    // hide update related settings if we tmm.noupdate has been set
    if (Boolean.parseBoolean(System.getProperty("tmm.noupdate"))) {
      collapsiblePanelUpdate.setVisible(false);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void initComponents() {
    setLayout(new MigLayout("hidemode 1", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][][15lp!][][grow]"));
    {
      JPanel panelLanguage = new JPanel();
      panelLanguage.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("Settings.language"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelLanguage, lblLanguageT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#ui-language"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        cbLanguage = new JComboBox(locales.toArray());
        panelLanguage.add(cbLanguage, "cell 1 0 2 1");
      }
      {
        final JLabel lblLanguageHint = new JLabel(TmmResourceBundle.getString("tmm.helptranslate"));
        panelLanguage.add(lblLanguageHint, "cell 1 1 2 1");
      }
      {
        lblLinkTranslate = new LinkTextArea("https://www.reddit.com/r/tinyMediaManager/comments/kt2iyq/basic_information/");
        panelLanguage.add(lblLinkTranslate, "cell 1 2 2 1, grow, wmin 0");
      }
      {
        lblLanguageChangeHint = new JLabel("");
        TmmFontHelper.changeFont(lblLanguageChangeHint, Font.BOLD);
        panelLanguage.add(lblLanguageChangeHint, "cell 0 3 3 1");
      }
    }

    {
      JPanel panelTheme = new JPanel();
      panelTheme.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblThemeT = new TmmLabel(TmmResourceBundle.getString("Settings.uitheme"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelTheme, lblThemeT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#ui-theme"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        cbTheme = new JComboBox(new String[] { "Light", "Dark" });
        panelTheme.add(cbTheme, "cell 1 0 2 1");
      }
      {
        lblThemeHint = new JLabel("");
        TmmFontHelper.changeFont(lblThemeHint, Font.BOLD);
        panelTheme.add(lblThemeHint, "cell 0 1 3 1");
      }
    }

    {
      JPanel panelFont = new JPanel();
      panelFont.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][][grow]", "[][][]")); // 16lp ~ width of the

      JLabel lblFontT = new TmmLabel(TmmResourceBundle.getString("Settings.font"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelFont, lblFontT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#font"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");
      {
        JLabel lblFontFamilyT = new JLabel(TmmResourceBundle.getString("Settings.fontfamily"));
        panelFont.add(lblFontFamilyT, "cell 1 0");
      }
      {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        cbFontFamily = new JComboBox(env.getAvailableFontFamilyNames());
        panelFont.add(cbFontFamily, "cell 2 0");
      }
      {
        JLabel lblFontSizeT = new JLabel(TmmResourceBundle.getString("Settings.fontsize"));
        panelFont.add(lblFontSizeT, "cell 1 1");
      }
      {
        cbFontSize = new JComboBox(DEFAULT_FONT_SIZES);
        panelFont.add(cbFontSize, "cell 2 1");
      }
      {
        JTextArea tpFontHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.fonts.hint"));
        panelFont.add(tpFontHint, "cell 1 2 2 1,growx");
      }
    }

    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][10lp!][][][][10lp!][][]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#misc-settings"));
      add(collapsiblePanel, "cell 0 6,growx,wmin 0");
      {
        JLabel lblDatefield = new JLabel(TmmResourceBundle.getString("Settings.datefield"));
        panelMisc.add(lblDatefield, "cell 1 0 2 1");

        cbDatefield = new JComboBox(DateField.values());
        panelMisc.add(cbDatefield, "cell 1 0");

        JLabel lblDatefieldHint = new JLabel(TmmResourceBundle.getString("Settings.datefield.desc"));
        panelMisc.add(lblDatefieldHint, "cell 2 1");
      }
      {
        JLabel lblImageChooserDefaultFolderT = new JLabel(TmmResourceBundle.getString("Settings.imagechooser.folder"));
        panelMisc.add(lblImageChooserDefaultFolderT, "cell 1 3 2 1");

        chckbxImageChooserLastFolder = new JCheckBox(TmmResourceBundle.getString("Settings.imagechooser.last"));
        panelMisc.add(chckbxImageChooserLastFolder, "cell 2 4");

        chckbxImageChooserEntityFolder = new JCheckBox(TmmResourceBundle.getString("Settings.imagechooser.entity"));
        panelMisc.add(chckbxImageChooserEntityFolder, "cell 2 5");
      }
      {
        chckbxStoreWindowPreferences = new JCheckBox(TmmResourceBundle.getString("Settings.storewindowpreferences"));
        panelMisc.add(chckbxStoreWindowPreferences, "cell 1 7 2 1");
      }
      {
        chckbxShowMemory = new JCheckBox(TmmResourceBundle.getString("Settings.showmemory"));
        panelMisc.add(chckbxShowMemory, "cell 1 8 2 1");
      }
    }
    {
      JPanel panelUpdate = new JPanel();
      panelUpdate.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][]")); // 16lp ~ width of the

      JLabel lblUpdateT = new TmmLabel(TmmResourceBundle.getString("Settings.update"), H3);
      collapsiblePanelUpdate = new CollapsiblePanel(panelUpdate, lblUpdateT, true);
      collapsiblePanelUpdate.addExtraTitleComponent(new DocsButton("/settings#update"));
      add(collapsiblePanelUpdate, "cell 0 8,growx,wmin 0");

      {
        chckbxAutomaticUpdates = new JCheckBox(TmmResourceBundle.getString("Settings.updatecheck"));
        panelUpdate.add(chckbxAutomaticUpdates, "cell 1 0 2 1");
      }
      {
        JLabel lblUpdateInterval = new JLabel(TmmResourceBundle.getString("Settings.updatecheck.interval"));
        panelUpdate.add(lblUpdateInterval, "flowx,cell 2 1");

        spUpdateInterval = new JSpinner();
        spUpdateInterval.setModel(new SpinnerNumberModel(1, 1, 30, 1));
        panelUpdate.add(spUpdateInterval, "cell 2 1");
      }
      {
        lblUpdateHint = new JLabel("");
        TmmFontHelper.changeFont(lblUpdateHint, Font.BOLD);
        panelUpdate.add(lblUpdateHint, "cell 1 2 2 1");
      }
    }
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    LocaleComboBox loc = (LocaleComboBox) cbLanguage.getSelectedItem();
    if (loc != null) {
      Locale locale = loc.loc;
      Locale actualLocale = Utils.getLocaleFromLanguage(Settings.getInstance().getLanguage());
      if (!locale.equals(actualLocale)) {
        Settings.getInstance().setLanguage(locale.toString());
        lblLanguageChangeHint.setText(TmmResourceBundle.getString("Settings.languagehint"));
      }
    }

    // theme
    String theme = (String) cbTheme.getSelectedItem();
    if (!Settings.getInstance().getTheme().equals(theme)) {
      Settings.getInstance().setTheme(theme);
      try {
        TmmUIHelper.setTheme();
        TmmUIHelper.updateUI();
      }
      catch (Exception e) {
        lblThemeHint.setText(TmmResourceBundle.getString("Settings.uitheme.hint"));
      }
    }

    // fonts
    String fontFamily = (String) cbFontFamily.getSelectedItem();
    Integer fontSize = (Integer) cbFontSize.getSelectedItem();
    if ((fontFamily != null && !fontFamily.equals(Settings.getInstance().getFontFamily()))
        || (fontSize != null && fontSize != Settings.getInstance().getFontSize())) {
      Settings.getInstance().setFontFamily(fontFamily);
      Settings.getInstance().setFontSize(fontSize);

      Font font = UIManager.getFont("defaultFont");
      Font newFont = new Font(fontFamily, font.getStyle(), fontSize);
      UIManager.put("defaultFont", newFont);

      TmmUIHelper.updateUI();
    }

    // image chooser folder
    settings.setImageChooserUseEntityFolder(chckbxImageChooserEntityFolder.isSelected());

    // update
    if (chckbxAutomaticUpdates.isSelected()) {
      lblUpdateHint.setText("");
    }
    else {
      lblUpdateHint.setText(TmmResourceBundle.getString("Settings.updatecheck.hint"));
    }
  }

  /**
   * Helper class for customized toString() method, to get the Name in localized language.
   */
  private static class LocaleComboBox implements Comparable<LocaleComboBox> {
    private final Locale       loc;
    private final List<Locale> countries;

    LocaleComboBox(Locale loc) {
      this.loc = loc;
      countries = LocaleUtils.countriesByLanguage(loc.getLanguage().toLowerCase(Locale.ROOT));
    }

    public Locale getLocale() {
      return loc;
    }

    @Override
    public String toString() {
      // display country name if needed
      // not needed when language == country
      if (loc.getLanguage().equalsIgnoreCase(loc.getCountry())) {
        return loc.getDisplayLanguage(loc);
      }

      // special exceptions (which do not have language == country)
      if (loc.toString().equals("en_US")) {
        return loc.getDisplayLanguage(loc);
      }

      // not needed, when this language is only in one country
      if (countries.size() == 1) {
        return loc.getDisplayLanguage(loc);
      }

      // output country if available
      if (StringUtils.isNotBlank(loc.getDisplayCountry(loc))) {
        return loc.getDisplayLanguage(loc) + " (" + loc.getDisplayCountry(loc) + ")";
      }

      return loc.getDisplayLanguage(loc);
    }

    @Override
    public int compareTo(LocaleComboBox o) {
      return toString().toLowerCase(Locale.ROOT).compareTo(o.toString().toLowerCase(Locale.ROOT));
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("storeWindowPreferences");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, chckbxStoreWindowPreferences,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("showMemory");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, chckbxShowMemory,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("dateField");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, cbDatefield,
        jComboBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property jSpinnerBeanProperty = BeanProperty.create("enabled");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxAutomaticUpdates, jCheckBoxBeanProperty, spUpdateInterval,
        jSpinnerBeanProperty);
    autoBinding_3.bind();
    //
    Property settingsBeanProperty_3 = BeanProperty.create("enableAutomaticUpdate");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_3, chckbxAutomaticUpdates,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property settingsBeanProperty_4 = BeanProperty.create("automaticUpdateInterval");
    Property jSpinnerBeanProperty_1 = BeanProperty.create("value");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_4, spUpdateInterval,
        jSpinnerBeanProperty_1);
    autoBinding_5.bind();
  }
}
