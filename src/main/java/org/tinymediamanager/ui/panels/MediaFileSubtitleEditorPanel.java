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
package org.tinymediamanager.ui.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTableFormat;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link MediaFileSubtitleEditorPanel} is used to add/edit subtitles
 * 
 * @author Manuel Laggner
 */
public class MediaFileSubtitleEditorPanel extends JPanel implements IModalPopupPanel {

  private final JComboBox<LocaleContainer>      cbLanguage;
  private final JCheckBox                       chkbxForced;
  private final JCheckBox                       chkbxSdh;
  private final JTextField                      tfFormat;
  private final JTextField                      tfTitle;

  private final JButton                         btnClose;
  private final JButton                         btnCancel;

  private final TmmTableFormat.StringComparator stringComparator;

  private boolean                               cancel = false;

  public MediaFileSubtitleEditorPanel(MediaFileSubtitle subtitle) {
    super();

    stringComparator = new TmmTableFormat.StringComparator();

    List<LocaleContainer> languages = new ArrayList<>();
    for (Locale locale : Locale.getAvailableLocales()) {
      LocaleContainer localeContainer = new LocaleContainer(locale);
      if (!languages.contains(localeContainer)) {
        languages.add(localeContainer);
      }
    }
    languages.sort((o1, o2) -> stringComparator.compare(o1.toString(), o2.toString()));

    {
      setLayout(new MigLayout("", "[][][300lp,grow]", "[][][][][]"));
      {
        JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
        add(lblLanguageT, "cell 0 0,alignx trailing");

        cbLanguage = new JComboBox(languages.toArray());
        add(cbLanguage, "cell 1 0 2 1");
      }
      {
        JLabel lblForcedT = new TmmLabel(TmmResourceBundle.getString("metatag.forced"));
        add(lblForcedT, "cell 0 1,alignx trailing");

        chkbxForced = new JCheckBox();
        add(chkbxForced, "cell 1 1, growx");
      }
      {
        JLabel lblSdhT = new TmmLabel(TmmResourceBundle.getString("metatag.sdh"));
        add(lblSdhT, "cell 0 2,alignx trailing");

        chkbxSdh = new JCheckBox();
        add(chkbxSdh, "cell 1 2, growx");
      }
      {
        JLabel lblFormatT = new TmmLabel(TmmResourceBundle.getString("metatag.format"));
        add(lblFormatT, "cell 0 3,alignx trailing");

        tfFormat = new JTextField();
        tfFormat.setColumns(8);
        add(tfFormat, "cell 1 3, growx");
      }
      {
        JLabel lblTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        add(lblTitleT, "cell 0 4,alignx trailing");

        tfTitle = new JTextField();
        tfTitle.setColumns(30);
        add(tfTitle, "cell 1 4 2 1, growx");
      }
      {
        btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
        btnCancel.addActionListener(e -> {
          cancel = true;
          setVisible(false);
        });

        btnClose = new JButton(TmmResourceBundle.getString("Button.save"));
        btnClose.addActionListener(e -> {
          subtitle.setCodec(tfFormat.getText());
          subtitle.setForced(chkbxForced.isSelected());
          subtitle.setSdh(chkbxSdh.isSelected());

          Object obj = cbLanguage.getSelectedItem();
          if (obj instanceof LocaleContainer localeContainer) {
            subtitle.setLanguage(localeContainer.iso3);
          }

          subtitle.setTitle(tfTitle.getText());

          setVisible(false);
        });
      }
    }

    tfFormat.setText(subtitle.getCodec());
    chkbxForced.setSelected(subtitle.isForced());
    chkbxSdh.setSelected(subtitle.isSdh());
    cbLanguage.setSelectedItem(new LocaleContainer(LocaleUtils.toLocale(subtitle.getLanguage())));
    tfTitle.setText(subtitle.getTitle());

    // set focus to the first combobox
    SwingUtilities.invokeLater(cbLanguage::requestFocus);
  }

  @Override
  public JComponent getContent() {
    return this;
  }

  @Override
  public JButton getCloseButton() {
    return btnClose;
  }

  @Override
  public JButton getCancelButton() {
    return btnCancel;
  }

  @Override
  public boolean isCancelled() {
    return cancel;
  }

  private static class LocaleContainer {
    private final Locale locale;
    private final String iso3;
    private final String description;

    public LocaleContainer(@NotNull Locale locale) {
      this.locale = locale;
      this.iso3 = LanguageUtils.getIso3Language(locale);
      this.description = StringUtils.isNotBlank(locale.getDisplayLanguage()) ? locale.getDisplayLanguage() + " (" + this.iso3 + ")" : "";
    }

    @Override
    public String toString() {
      return description;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LocaleContainer that = (LocaleContainer) o;
      return iso3.equals(that.iso3);
    }

    @Override
    public int hashCode() {
      return Objects.hash(iso3);
    }
  }
}
