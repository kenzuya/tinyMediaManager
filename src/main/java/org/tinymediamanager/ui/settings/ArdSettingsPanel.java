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
package org.tinymediamanager.ui.settings;

import net.miginfocom.swing.MigLayout;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.ArdSettings;
import org.tinymediamanager.core.AspectRatio;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.stream.Collectors;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

public class ArdSettingsPanel extends JPanel {

  private final Settings              settings              = Settings.getInstance();

  private JSlider                     sliderDetectionMode;
  private Map<String, JCheckBox>      customARCheckBoxes    = new LinkedHashMap<>();
  private ButtonGroup                 buttonGroupRound      = new ButtonGroup();
  private JRadioButton                rdbtnRoundNearest;
  private JRadioButton                rdbtnRoundUpToNext;
  private ButtonGroup                 buttonGroupARUseMode  = new ButtonGroup();
  private JRadioButton                rdbtnMFDisabled;
  private JRadioButton                rdbtnMFHigher;
  private JRadioButton                rdbtnMFWider;

  private int                         previousMode          = -1;
  private int                         previousMFMode        = -1;

  public ArdSettingsPanel() {
    initComponents();
    initDataBindings();

    this.previousMode = settings.getArdMode().ordinal();
    sliderDetectionMode.setValue(settings.getArdMode().ordinal());
    sliderDetectionMode.addChangeListener(e -> {
      if (!sliderDetectionMode.getValueIsAdjusting()) {
        switch (sliderDetectionMode.getValue()) {
          case 0: // fast
          case 1: // default
            rdbtnMFDisabled.setEnabled(false);
            rdbtnMFWider.setEnabled(false);
            rdbtnMFHigher.setEnabled(false);

            this.previousMFMode = this.previousMode == 2 ? settings.getArdMFMode() : this.previousMFMode;
            buttonGroupARUseMode.clearSelection();
            break;
          case 2: // accurate
            rdbtnMFDisabled.setEnabled(true);
            rdbtnMFWider.setEnabled(true);
            rdbtnMFHigher.setEnabled(true);
            rdbtnMFDisabled.setSelected(this.previousMFMode == 0);
            rdbtnMFHigher.setSelected(this.previousMFMode == 1);
            rdbtnMFWider.setSelected(this.previousMFMode == 2);
            break;
        }
        settings.setArdMode(ArdSettings.Mode.values()[sliderDetectionMode.getValue()]);
        this.previousMode = sliderDetectionMode.getValue();
      }
    });


    ItemListener checkBoxListener = e -> checkCustomARChanges();
    for (Map.Entry<String, JCheckBox> entry : customARCheckBoxes.entrySet()) {
      if (settings.getCustomAspectRatios().contains(entry.getKey())) {
        entry.getValue().setSelected(true);
      }
      entry.getValue().addItemListener(checkBoxListener);
    }

    if (settings.isArdRoundUp()) {
      rdbtnRoundUpToNext.setSelected(true);
    } else {
      rdbtnRoundNearest.setSelected(true);
    }

    boolean isAccurate = sliderDetectionMode.getValue() == 2;
    previousMFMode = settings.getArdMFMode();
    rdbtnMFDisabled.addChangeListener(e -> { if (rdbtnMFDisabled.isSelected()) settings.setArdMFMode(0); });
    rdbtnMFDisabled.setSelected(isAccurate && settings.getArdMFMode() == 0);
    rdbtnMFDisabled.setEnabled(isAccurate);
    rdbtnMFHigher.addChangeListener(e -> { if (rdbtnMFHigher.isSelected()) settings.setArdMFMode(1); });
    rdbtnMFHigher.setSelected(isAccurate && settings.getArdMFMode() == 1);
    rdbtnMFHigher.setEnabled(isAccurate);
    rdbtnMFWider.addChangeListener(e -> { if (rdbtnMFWider.isSelected()) settings.setArdMFMode(2); });
    rdbtnMFWider.setSelected(isAccurate && settings.getArdMFMode() == 2);
    rdbtnMFWider.setEnabled(isAccurate);
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));

    JPanel panelArdSettings = new JPanel(new MigLayout("hidemode 1, insets 0",
                                                       "[20lp!][20lp!][500lp][grow]",
                                                       "[][10lp!][][][][][10lp!][][][][]"));
    JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("Settings.ard"), H3);
    CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelArdSettings, lblLanguageT, true);
    collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#ard"));
    add(collapsiblePanel, "cell 0 0,growx, wmin 0");

    {
      int row = 0;

      JLabel lblDetectionMode = new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.hint"));
      panelArdSettings.add(lblDetectionMode, "cell 1 " + row + ", span, aligny top");

      sliderDetectionMode = new JSlider(0, 2, 1);
      sliderDetectionMode.setMajorTickSpacing(1);
      Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
      labelTable.put(0, new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.fast")));
      labelTable.put(1, new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.default")));
      labelTable.put(2, new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.accurate")));
      sliderDetectionMode.setLabelTable(labelTable);
      sliderDetectionMode.setPaintLabels(true);
      sliderDetectionMode.setPaintTicks(true);
      lblDetectionMode.setLabelFor(sliderDetectionMode);
      panelArdSettings.add(sliderDetectionMode, "cell 1 " + row + ", span");

      // custom aspect ratios
      row++;
      row++;
      JLabel lblAspectRatiosDesc = new JLabel(TmmResourceBundle.getString("Settings.ard.roundAspectRatios.hint"));
      panelArdSettings.add(lblAspectRatiosDesc, "cell 1 " + row + ", span");

      row++;
      JPanel panelCustomAspectRatios = new JPanel();
      panelCustomAspectRatios.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.anchor = GridBagConstraints.LINE_START;
      gbc.ipadx = 30;
      panelArdSettings.add(panelCustomAspectRatios, "cell 1 " + row + ", span");

      Map<Float, String> customAspectRatios = AspectRatio.getDefaultValues();
      int yOne = 0;
      int yTwo = 0;
      for (Map.Entry<Float, String> customAR : customAspectRatios.entrySet()) {
        JCheckBox checkBox = new JCheckBox(customAR.getValue());
        customARCheckBoxes.put(customAR.getKey().toString(), checkBox);

        if (customAR.getKey() < 1.9f) {
          gbc.gridx = 0;
          gbc.gridy = yOne++;
        } else {
          gbc.gridx = 1;
          gbc.gridy = yTwo++;
        }
        panelCustomAspectRatios.add(checkBox, gbc);
        gbc.gridx++;
      }

      // round nearest
      row++;
      rdbtnRoundNearest = new JRadioButton(TmmResourceBundle.getString("Settings.ard.roundNearest"));
      buttonGroupRound.add(rdbtnRoundNearest);
      panelArdSettings.add(rdbtnRoundNearest, "cell 1 " + row + " 2 1,aligny bottom");

      // round up
      row++;
      rdbtnRoundUpToNext = new JRadioButton(TmmResourceBundle.getString("Settings.ard.roundUpToNext"));
      buttonGroupRound.add(rdbtnRoundUpToNext);
      panelArdSettings.add(rdbtnRoundUpToNext, "cell 1 " + row + " 2 1");

      row++;
      row++;
      JLabel lblMultiFormat = new JLabel(TmmResourceBundle.getString("Settings.ard.multiformat.hint"));
      panelArdSettings.add(lblMultiFormat, "cell 1 " + row + " 3 1");

      // MF disabled
      row++;
      rdbtnMFDisabled = new JRadioButton(TmmResourceBundle.getString("Settings.ard.multiformat.disabled"));
      buttonGroupARUseMode.add(rdbtnMFDisabled);
      panelArdSettings.add(rdbtnMFDisabled, "cell 1 " + row + ",growx");

      // MF most higher
      row++;
      rdbtnMFHigher = new JRadioButton(TmmResourceBundle.getString("Settings.ard.multiformat.useHigher"));
      buttonGroupARUseMode.add(rdbtnMFHigher);
      panelArdSettings.add(rdbtnMFHigher, "cell 1 " + row + ",growx");

      // MF wider
      row++;
      rdbtnMFWider = new JRadioButton(TmmResourceBundle.getString("Settings.ard.multiformat.useWider"));
      buttonGroupARUseMode.add(rdbtnMFWider);
      panelArdSettings.add(rdbtnMFWider, "cell 1 " + row + ",growx");
    }
  }

  private void initDataBindings() {
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");

    // round up
    Property ardRoundUpBeanProperty = BeanProperty.create("ardRoundUp");
    AutoBinding autoBinding_ard_roundUp = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings,
      ardRoundUpBeanProperty,
      rdbtnRoundUpToNext,
      jCheckBoxBeanProperty);
    autoBinding_ard_roundUp.bind();
  }

  private void checkCustomARChanges() {
    Set<String> customARs = new LinkedHashSet<>();

    for (Map.Entry<String, JCheckBox> entry : customARCheckBoxes.entrySet()) {
      if (entry.getValue().isSelected()) {
        customARs.add(entry.getKey());
      }
    }

    settings.setCustomAspectRatios(customARs.stream().collect(Collectors.toList()));
  }
}
