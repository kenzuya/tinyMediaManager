/*
 * Copyright 2012 - 2024 Manuel Laggner
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.config.MediaProviderConfig;
import org.tinymediamanager.scraper.config.MediaProviderConfigObject;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.TmmCheckComboBox;

import com.google.gson.Gson;

import net.miginfocom.swing.MigLayout;

/**
 * The class MediaScraperConfigurationPanel is used to display configurable scraper options
 * 
 * @author Manuel Laggner
 */
public class MediaScraperConfigurationPanel extends JPanel {
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");                         // stick to resourcebundle here
  private static final Logger         LOGGER = LoggerFactory.getLogger(MediaScraperConfigurationPanel.class);

  private final IMediaProvider        mediaProvider;
  private final JPanel                configPanel;

  private boolean                     dirty  = false;

  public MediaScraperConfigurationPanel(IMediaProvider mediaProvider) {
    this.mediaProvider = mediaProvider;

    setLayout(new BorderLayout());

    JPanel panelHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
    add(panelHead, BorderLayout.NORTH);

    JLabel lblScraperOptions = new TmmLabel(TmmResourceBundle.getString("Settings.scraper.options"), 1.2);
    panelHead.add(lblScraperOptions);

    configPanel = createConfigPanel();
    add(configPanel, BorderLayout.CENTER);

    // add a listener to determine when to save the settings
    addAncestorListener(new AncestorListener() {
      @Override
      public void ancestorRemoved(AncestorEvent event) {
        // check if anything has been changed
        if (dirty) {
          saveSettings();
        }
      }

      @Override
      public void ancestorMoved(AncestorEvent event) {
        // nothing needed here
      }

      @Override
      public void ancestorAdded(AncestorEvent event) {
        // nothing needed here
      }
    });
  }

  private JPanel createConfigPanel() {
    JPanel panel = new JPanel(new MigLayout("gapy 2lp", "[][20lp!][200lp,grow]", ""));

    int row = 0;

    // build up the panel for being displayed in the popup
    MediaProviderConfig config = mediaProvider.getProviderInfo().getConfig();
    for (Entry<String, MediaProviderConfigObject> entry : config.getConfigObjects().entrySet()) {
      if (!entry.getValue().isVisible()) {
        continue;
      }

      // label
      // try different ways to get a meaningful key description
      String keyDescription = getStringFromBundle("scraper." + mediaProvider.getProviderInfo().getId() + "." + entry.getKey());
      if (StringUtils.isBlank(keyDescription)) {
        keyDescription = getStringFromBundle(entry.getValue().getKeyDescription());
      }
      if (StringUtils.isBlank(keyDescription)) {
        keyDescription = entry.getValue().getKeyDescription();
      }

      if (entry.getValue().getType() == MediaProviderConfigObject.ConfigType.LABEL) {
        // if we have a LABEL here, use a bold style label
        panel.add(new TmmLabel(keyDescription), "cell 0 " + row + " 3 1, gaptop 10lp");
      }
      else {
        panel.add(new JLabel(keyDescription), "cell 0 " + row);
      }

      JComponent comp = null;
      switch (entry.getValue().getType()) {
        case LABEL:
          // just use a Label here
          break;

        case BOOL:
          // display as checkbox
          JCheckBox checkbox = new JCheckBox();
          checkbox.setSelected(entry.getValue().getValueAsBool());
          checkbox.addActionListener(e -> dirty = true);
          comp = checkbox;
          break;

        case INTEGER:
          // display as a spinner
          JSpinner spinner = new JSpinner(new SpinnerNumberModel(entry.getValue().getValueAsInteger().intValue(), 0, Integer.MAX_VALUE, 1));
          spinner.addChangeListener(e -> dirty = true);
          // make the spinner smaller
          ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(5);
          comp = spinner;
          break;

        case SELECT:
        case SELECT_INDEX:
          // display as combobox
          JComboBox<String> combobox = new JComboBox<>(entry.getValue().getPossibleValues().toArray(new String[0]));
          combobox.setSelectedItem(entry.getValue().getValueAsString());
          combobox.addActionListener(e -> dirty = true);
          comp = combobox;
          break;

        case MULTI_SELECT:
          // display as check-combobox
          TmmCheckComboBox<String> checkComboBox = new TmmCheckComboBox<>(entry.getValue().getPossibleValues().toArray(new String[0]));
          String valuesAsString = entry.getValue().getValueAsString();
          if (StringUtils.isNotBlank(valuesAsString)) {
            String[] values = new Gson().fromJson(valuesAsString, String[].class);
            checkComboBox.setSelectedItems(values);
          }
          checkComboBox.addActionListener(e -> dirty = true);
          comp = checkComboBox;
          break;

        default:
          // display as text
          JTextField tf;
          if (entry.getValue().isEncrypt()) {
            tf = new JPasswordField(config.getValue(entry.getKey()));
          }
          else {
            tf = new JTextField(config.getValue(entry.getKey()));
          }

          tf.setColumns(20);
          tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
              dirty = true;
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
              dirty = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
              dirty = true;
            }
          });
          comp = tf;
          break;
      }

      if (comp != null) {
        comp.putClientProperty(entry.getKey(), entry.getKey());
        panel.add(comp, "cell 2 " + row);
      }

      // add a hint if a long text has been found
      try {
        String desc = getStringFromBundle("scraper." + mediaProvider.getProviderInfo().getId() + "." + entry.getKey() + ".desc");
        if (StringUtils.isNotBlank(desc)) {
          JLabel lblHint = new JLabel(IconManager.HINT);
          lblHint.setToolTipText(desc);
          panel.add(lblHint, "cell 2 " + row);
        }
      }
      catch (Exception e) {
        LOGGER.debug("failed to add a hint: {}", e.getMessage());
      }
      row++;
    }
    return panel;
  }

  /**
   * get the String from the bundle w/o throwing an exception
   * 
   * @param key
   *          the key to get the String for
   * @return the desired String or an empty String
   */
  private String getStringFromBundle(String key) {
    if (mediaProvider.getProviderInfo().getResourceBundle() != null) {
      try {
        return mediaProvider.getProviderInfo().getResourceBundle().getString(key);
      }
      catch (Exception ignored) {
        // an exception is thrown here if no string in the resources has been found -> silently ignore
      }
    }

    try {
      return BUNDLE.getString(key);
    }
    catch (Exception ignored) {
      // an exception if thrown here is no string in the resources has been found -> silently ignore
    }

    return "";
  }

  private void saveSettings() {
    Map<String, MediaProviderConfigObject> config = mediaProvider.getProviderInfo().getConfig().getConfigObjects();
    // transfer the items from the components to the config
    for (Entry<String, MediaProviderConfigObject> entry : config.entrySet()) {
      for (Component comp : configPanel.getComponents()) {
        // get the right component for this setting
        if (!(comp instanceof JComponent)) {
          continue;
        }

        Object param = ((JComponent) comp).getClientProperty(entry.getKey());
        if (!entry.getKey().equals(param)) {
          continue;
        }

        // parse the value and write it back to the new config
        try {
          if (comp instanceof JCheckBox) {
            mediaProvider.getProviderInfo().getConfig().setValue(entry.getKey(), ((JCheckBox) comp).isSelected());
          }
          else if (comp instanceof TmmCheckComboBox) {
            mediaProvider.getProviderInfo().getConfig().setValue(entry.getKey(), ((TmmCheckComboBox) comp).getSelectedItems().toString());
          }
          else if (comp instanceof JComboBox) {
            mediaProvider.getProviderInfo().getConfig().setValue(entry.getKey(), ((JComboBox) comp).getSelectedItem().toString());
          }
          else if (comp instanceof JSpinner) {
            mediaProvider.getProviderInfo().getConfig().setValue(entry.getKey(), (Integer) (((JSpinner) comp).getValue()));
          }
          else {
            mediaProvider.getProviderInfo().getConfig().setValue(entry.getKey(), ((JTextField) comp).getText());
          }
        }
        catch (Exception ignored) {
          // ignored
        }
      }
    }

    mediaProvider.getProviderInfo().getConfig().save();
  }
}
