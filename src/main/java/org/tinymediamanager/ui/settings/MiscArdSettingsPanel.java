package org.tinymediamanager.ui.settings;

import net.miginfocom.swing.MigLayout;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.AspectRatio;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.stream.Collectors;

public class MiscArdSettingsPanel extends JPanel {

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

  public MiscArdSettingsPanel() {
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
        settings.setArdMode(Settings.ArdMode.values()[sliderDetectionMode.getValue()]);
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
    setLayout(new MigLayout("hidemode 1, insets 0",
      "[20lp!][20lp!][500lp][grow]",
      "[][10lp!][][][][][10lp!][][][][]"));

    int row = 0;

    JLabel lblDetectionMode = new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.hint"));
    add(lblDetectionMode, "cell 1 " + row + ", span, aligny top");

    sliderDetectionMode = new JSlider(0,2,1);
    sliderDetectionMode.setMajorTickSpacing(1);
    Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
    labelTable.put(0, new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.fast")));
    labelTable.put(1, new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.default")));
    labelTable.put(2, new JLabel(TmmResourceBundle.getString("Settings.ard.detectionMode.accurate")));
    sliderDetectionMode.setLabelTable(labelTable);
    sliderDetectionMode.setPaintLabels(true);
    sliderDetectionMode.setPaintTicks(true);
    lblDetectionMode.setLabelFor(sliderDetectionMode);
    add(sliderDetectionMode, "cell 1 " + row + ", span");

    // custom aspect ratios
    row++;
    row++;
    JLabel lblAspectRatiosDesc = new JLabel(TmmResourceBundle.getString("Settings.ard.roundAspectRatios.hint"));
    add(lblAspectRatiosDesc, "cell 1 " + row + ", span");

    row++;
    JPanel panelCustomAspectRatios = new JPanel();
    panelCustomAspectRatios.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.ipadx = 10;
    add(panelCustomAspectRatios, "cell 1 " + row + ", span");

    Map<Float, String> customAspectRatios = AspectRatio.getDefaultValues();
    for (Map.Entry<Float, String> customAR : customAspectRatios.entrySet()) {
      JCheckBox checkBox = new JCheckBox(customAR.getValue());
      customARCheckBoxes.put(customAR.getKey().toString(), checkBox);

      if (gbc.gridx >= 4) {
        gbc.gridx = 0;
        gbc.gridy++;
      }
      panelCustomAspectRatios.add(checkBox, gbc);
      gbc.gridx++;
    }

    // round nearest
    row++;
    rdbtnRoundNearest = new JRadioButton(TmmResourceBundle.getString("Settings.ard.roundNearest"));
    buttonGroupRound.add(rdbtnRoundNearest);
    add(rdbtnRoundNearest, "cell 1 " + row + " 2 1,aligny bottom");

    // round up
    row++;
    rdbtnRoundUpToNext = new JRadioButton(TmmResourceBundle.getString("Settings.ard.roundUpToNext"));
    buttonGroupRound.add(rdbtnRoundUpToNext);
    add(rdbtnRoundUpToNext, "cell 1 " + row + " 2 1");

    row++;
    row++;
    JLabel lblMultiFormat = new JLabel(TmmResourceBundle.getString("Settings.ard.multiformat.hint"));
    add(lblMultiFormat, "cell 1 " + row + " 3 1");

    // MF disabled
    row++;
    rdbtnMFDisabled = new JRadioButton(TmmResourceBundle.getString("Settings.ard.multiformat.disabled"));
    buttonGroupARUseMode.add(rdbtnMFDisabled);
    add(rdbtnMFDisabled, "cell 1 " + row + ",growx");

    // MF most higher
    row++;
    rdbtnMFHigher = new JRadioButton(TmmResourceBundle.getString("Settings.ard.multiformat.useHigher"));
    buttonGroupARUseMode.add(rdbtnMFHigher);
    add(rdbtnMFHigher, "cell 1 " + row + ",growx");

    // MF wider
    row++;
    rdbtnMFWider = new JRadioButton(TmmResourceBundle.getString("Settings.ard.multiformat.useWider"));
    buttonGroupARUseMode.add(rdbtnMFWider);
    add(rdbtnMFWider, "cell 1 " + row + ",growx");
  }

  private void initDataBindings() {
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");

    // round nearest
    Property ardRoundNearestBeanProperty = BeanProperty.create("ardRoundNearest");
    AutoBinding autoBinding_ard_roundNearest = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings,
      ardRoundNearestBeanProperty,
      rdbtnRoundNearest,
      jCheckBoxBeanProperty);
    autoBinding_ard_roundNearest.bind();

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
