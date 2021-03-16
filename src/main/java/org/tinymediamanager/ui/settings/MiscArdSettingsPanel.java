package org.tinymediamanager.ui.settings;

import net.miginfocom.swing.MigLayout;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.AspectRatio;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.TmmFontHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.tinymediamanager.ui.TmmFontHelper.L2;

public class MiscArdSettingsPanel extends JPanel {

  private final Settings              settings              = Settings.getInstance();

  private JTextField                  tfSampleDuration;
  private JTextField                  tfSampleMinNumber;
  private JTextField                  tfSampleMaxGap;
  private JTextField                  tfIgnoreBeginning;
  private JTextField                  tfIgnoreEnd;
  private Map<String, JCheckBox>      customARCheckBoxes    = new LinkedHashMap<>();
  private ButtonGroup                 buttonGroupRound      = new ButtonGroup();
  private JRadioButton                rdbtnRoundNearest;
  private JRadioButton                rdbtnRoundUpToNext;
  private JTextField                  tfRoundThreshold;
  private ButtonGroup                 buttonGroupARUseMode  = new ButtonGroup();
  private JRadioButton                rdbtnMFMostFrequent;
  private JRadioButton                rdbtnMFWider;
  private JRadioButton                rdbtnMFHigher;
  private JTextField                  tfMFThreshold;

  public MiscArdSettingsPanel() {
    initComponents();
    initDataBindings();

    rdbtnRoundNearest.addActionListener(e -> tfRoundThreshold.setEnabled(false));
    rdbtnRoundUpToNext.addActionListener(e -> tfRoundThreshold.setEnabled(true));

    rdbtnMFMostFrequent.addActionListener(e -> tfMFThreshold.setEnabled(false));
    rdbtnMFWider.addActionListener(e -> tfMFThreshold.setEnabled(true));
    rdbtnMFHigher.addActionListener(e -> tfMFThreshold.setEnabled(true));

    ItemListener checkBoxListener = e -> checkCustomARChanges();
    for (Map.Entry<String, JCheckBox> entry : customARCheckBoxes.entrySet()) {
      if (settings.getCustomAspectRatios().contains(entry.getKey())) {
        entry.getValue().setSelected(true);
      }
      entry.getValue().addItemListener(checkBoxListener);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 1, insets 0",
      "[20lp!][20lp!][100lp][grow]",
      "[][][][][][10lp!][][][][][][10lp!][]"));

    // sample duration
    int row = 0;
    JLabel lblSampleDuration = new JLabel(TmmResourceBundle.getString("Settings.ard.sampleDuration"));
    add(lblSampleDuration, "cell 1 " + row + " 2 1, alignx right");

    tfSampleDuration = new JTextField(3);
    add(tfSampleDuration, "cell 3 " + row);
    lblSampleDuration.setLabelFor(tfSampleDuration);

    JLabel lblSampleDurationUnit = new JLabel("s");
    add(lblSampleDurationUnit, "cell 3 " + row);

    // sample min number
    row++;
    JLabel lblSampleMinNumber = new JLabel(TmmResourceBundle.getString("Settings.ard.sampleMinNumber"));
    add(lblSampleMinNumber, "cell 1 " + row + " 2 1,alignx right");

    tfSampleMinNumber = new JTextField(3);
    add(tfSampleMinNumber, "cell 3 " + row);
    lblSampleMinNumber.setLabelFor(tfSampleMinNumber);

    // sample max gap
    row++;
    JLabel lblSampleMaxGap = new JLabel(TmmResourceBundle.getString("Settings.ard.sampleMaxGap"));
    add(lblSampleMaxGap, "cell 1 " + row + " 2 1,alignx right");

    tfSampleMaxGap = new JTextField(3);
    add(tfSampleMaxGap, "cell 3 " + row);
    lblSampleMaxGap.setLabelFor(tfSampleMaxGap);

    JLabel lblSampleMaxMapUnit = new JLabel("s");
    add(lblSampleMaxMapUnit, "cell 3 " + row);

    // ignore beginning
    row++;
    JLabel lblIgnoreBeginning = new JLabel(TmmResourceBundle.getString("Settings.ard.ignoreBeginning"));
    add(lblIgnoreBeginning, "cell 1 " + row + " 2 1,alignx right");

    tfIgnoreBeginning = new JTextField(3);
    add(tfIgnoreBeginning, "cell 3 " + row);
    lblIgnoreBeginning.setLabelFor(tfIgnoreBeginning);

    JLabel lblSampleIgnoreBeginningUnit = new JLabel("%");
    add(lblSampleIgnoreBeginningUnit, "cell 3 " + row);

    // ignore end
    row++;
    JLabel lblIgnoreEnd = new JLabel(TmmResourceBundle.getString("Settings.ard.ignoreEnd"));
    add(lblIgnoreEnd, "cell 1 " + row + " 2 1,alignx right");

    tfIgnoreEnd = new JTextField(3);
    add(tfIgnoreEnd, "cell 3 " + row);
    lblIgnoreEnd.setLabelFor(tfIgnoreEnd);

    JLabel lblSampleIgnoreEndUnit = new JLabel("%");
    add(lblSampleIgnoreEndUnit, "cell 3 " + row);

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

    // round threshold
    row++;
    JLabel lblRoundThreshold = new JLabel(TmmResourceBundle.getString("Settings.ard.roundThreshold"));
    add(lblRoundThreshold, "cell 1 " + row + " 2 1,alignx right");

    tfRoundThreshold = new JTextField(3);
    tfRoundThreshold.setEnabled(settings.isArdRoundUp());
    add(tfRoundThreshold, "cell 3 " + row);
    lblRoundThreshold.setLabelFor(tfRoundThreshold);

    row++;
    row++;
    JLabel lblMultiFormat = new JLabel(TmmResourceBundle.getString("Settings.ard.multiformat.hint"));
    add(lblMultiFormat, "cell 1 " + row + " 3 1");

    // MF most frequent
    row++;
    rdbtnMFMostFrequent = new JRadioButton(TmmResourceBundle.getString("Settings.ard.useMostFrequent"));
    buttonGroupARUseMode.add(rdbtnMFMostFrequent);
    add(rdbtnMFMostFrequent, "cell 1 " + row + ",growx");

    // MF wider
    row++;
    rdbtnMFWider = new JRadioButton(TmmResourceBundle.getString("Settings.ard.useWider"));
    buttonGroupARUseMode.add(rdbtnMFWider);
    add(rdbtnMFWider, "cell 1 " + row + ",growx");

    // MF most higher
    row++;
    rdbtnMFHigher = new JRadioButton(TmmResourceBundle.getString("Settings.ard.useHigher"));
    buttonGroupARUseMode.add(rdbtnMFHigher);
    add(rdbtnMFHigher, "cell 1 " + row + ",growx");

    row++;
    JLabel lblMultiFormatHint = new JLabel(TmmResourceBundle.getString("Settings.ard.multiformathint.hint"));
    TmmFontHelper.changeFont(lblMultiFormatHint, L2);
    add(lblMultiFormatHint, "cell 2 " + row + " 3 1");

    // MF threshold
    row++;
    JLabel lblMFThreshold = new JLabel(TmmResourceBundle.getString("Settings.ard.mfThreshold"));
    add(lblMFThreshold, "cell 1 " + row + " 2 1,alignx right");

    tfMFThreshold = new JTextField(3);
    tfMFThreshold.setEnabled(!settings.isArdMFMostFrequent());
    add(tfMFThreshold, "cell 3 " + row);

    JLabel lblMFThresholdUnit = new JLabel("%");
    add(lblMFThresholdUnit, "cell 3 " + row);
  }

  private void initDataBindings() {
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");

    // sample duration
    Property ardSampleDurationBeanProperty = BeanProperty.create("ardSampleDuration");
    Property jTextFieldBeanProperty_ard_sampleDuration = BeanProperty.create("text");
    AutoBinding autoBinding_ard_sampleDuration = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings,
      ardSampleDurationBeanProperty,
      tfSampleDuration,
      jTextFieldBeanProperty_ard_sampleDuration);
    autoBinding_ard_sampleDuration.bind();

    // sample min number
    Property ardSampleMinNumberBeanProperty = BeanProperty.create("ardSampleMinNumber");
    Property jTextFieldBeanProperty_ard_sampleMinNumber = BeanProperty.create("text");
    AutoBinding autoBinding_ard_sampleMinNumber = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings,
      ardSampleMinNumberBeanProperty,
      tfSampleMinNumber,
      jTextFieldBeanProperty_ard_sampleMinNumber);
    autoBinding_ard_sampleMinNumber.bind();

    // sample max gap
    Property ardSampleMaxGapBeanProperty = BeanProperty.create("ardSampleMaxGap");
    Property jTextFieldBeanProperty_ard_sampleMaxGap = BeanProperty.create("text");
    AutoBinding autoBinding_ard_sampleMaxGap = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings,
      ardSampleMaxGapBeanProperty,
      tfSampleMaxGap,
      jTextFieldBeanProperty_ard_sampleMaxGap);
    autoBinding_ard_sampleMaxGap.bind();

    // sample ignore beginning
    Property ardIgnoreBeginningBeanProperty = BeanProperty.create("ardIgnoreBeginning");
    Property jTextFieldBeanProperty_ard_ignoreBeginning = BeanProperty.create("text");
    AutoBinding autoBinding_ard_ignoreBeginning = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings,
      ardIgnoreBeginningBeanProperty,
      tfIgnoreBeginning,
      jTextFieldBeanProperty_ard_ignoreBeginning);
    autoBinding_ard_ignoreBeginning.bind();

    // sample ignore end
    Property ardIgnoreEndBeanProperty = BeanProperty.create("ardIgnoreEnd");
    Property jTextFieldBeanProperty_ard_ignoreEnd = BeanProperty.create("text");
    AutoBinding autoBinding_ard_ignoreEnd = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings,
      ardIgnoreEndBeanProperty,
      tfIgnoreEnd,
      jTextFieldBeanProperty_ard_ignoreEnd);
    autoBinding_ard_ignoreEnd.bind();

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

    // round threshold
    Property ardRoundThresholdBeanProperty = BeanProperty.create("ardRoundThreshold");
    Property jTextFieldBeanProperty_ard_roundThreshold = BeanProperty.create("text");
    AutoBinding autoBinding_ard_roundThreshold = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
      settings,
      ardRoundThresholdBeanProperty,
      tfRoundThreshold,
      jTextFieldBeanProperty_ard_roundThreshold);
    autoBinding_ard_roundThreshold.bind();

    // MF round most frequent
    Property ardMFRoundMostFrequentBeanProperty = BeanProperty.create("ardMFMostFrequent");
    AutoBinding autoBinding_ard_mfRoundMostFrequent = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
      settings,
      ardMFRoundMostFrequentBeanProperty,
      rdbtnMFMostFrequent,
      jCheckBoxBeanProperty);
    autoBinding_ard_mfRoundMostFrequent.bind();

    // MF round wider
    Property ardMFRoundWiderBeanProperty = BeanProperty.create("ardMFWider");
    AutoBinding autoBinding_ard_mfRoundWider = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
      settings,
      ardMFRoundWiderBeanProperty,
      rdbtnMFWider,
      jCheckBoxBeanProperty);
    autoBinding_ard_mfRoundWider.bind();

    // MF round most frequent
    Property ardMFRoundHigherBeanProperty = BeanProperty.create("ardMFHigher");
    AutoBinding autoBinding_ard_mfRoundHigher = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
      settings,
      ardMFRoundHigherBeanProperty,
      rdbtnMFHigher,
      jCheckBoxBeanProperty);
    autoBinding_ard_mfRoundHigher.bind();

    // MF threshold
    Property ardMFThresholdBeanProperty = BeanProperty.create("ardMFThreshold");
    Property jTextFieldBeanProperty_ard_mfThreshold = BeanProperty.create("text");
    AutoBinding autoBinding_ard_mfThreshold = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
      settings,
      ardMFThresholdBeanProperty,
      tfMFThreshold,
      jTextFieldBeanProperty_ard_mfThreshold);
    autoBinding_ard_mfThreshold.bind();
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
