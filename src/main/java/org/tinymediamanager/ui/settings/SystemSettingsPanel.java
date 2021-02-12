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

import static org.tinymediamanager.ui.TmmFontHelper.H3;
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.LauncherExtraConfig;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.TmmLabel;

import com.sun.jna.Platform;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MiscSettingsPanel.
 * 
 * @author Manuel Laggner
 */
class SystemSettingsPanel extends JPanel {
  private static final long    serialVersionUID = 500841588272296493L;

  private static final Logger  LOGGER           = LoggerFactory.getLogger(SystemSettingsPanel.class);
  private static final Pattern MEMORY_PATTERN   = Pattern.compile("-Xmx([0-9]*)(.)");

  private final Settings       settings         = Settings.getInstance();

  private JTextField           tfProxyHost;
  private JTextField           tfProxyPort;
  private JTextField           tfProxyUsername;
  private JPasswordField       tfProxyPassword;
  private JTextField           tfMediaPlayer;
  private JTextField           tfMediaFramework;
  private JButton              btnSearchMediaPlayer;
  private JButton              btnSearchFFMpegBinary;
  private JSlider              sliderMemory;
  private JLabel               lblMemory;
  private JCheckBox            chckbxIgnoreSSLProblems;
  private JSpinner             spMaximumDownloadThreads;

  /**
   * Instantiates a new general settings panel.
   */
  SystemSettingsPanel() {

    initComponents();

    initDataBindings();

    initMemorySlider();

    // data init
    btnSearchMediaPlayer.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("chooseplayer.path");
      Path file = TmmUIHelper.selectApplication(TmmResourceBundle.getString("Button.chooseplayer"), path);
      if (file != null && (Utils.isRegularFile(file) || Platform.isMac())) {
        tfMediaPlayer.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("chooseplayer.path", file.getParent().toString());
      }
    });

    btnSearchFFMpegBinary.addActionListener(arg0 -> {
      String path = TmmProperties.getInstance().getProperty("chooseffmpeg.path");
      Path file = TmmUIHelper.selectFile(TmmResourceBundle.getString("Button.chooseffmpeglocation"), path, null);
      if (file != null && (Utils.isRegularFile(file) || Platform.isMac())) {
        tfMediaFramework.setText(file.toAbsolutePath().toString());
        TmmProperties.getInstance().putProperty("chooseffmpeg.path", file.getParent().toString());
      }
    });
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelMediaPlayer = new JPanel();
      panelMediaPlayer.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("Settings.mediaplayer"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMediaPlayer, lblLanguageT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#media-player"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        tfMediaPlayer = new JTextField();
        panelMediaPlayer.add(tfMediaPlayer, "cell 1 0 2 1");
        tfMediaPlayer.setColumns(35);

        btnSearchMediaPlayer = new JButton(TmmResourceBundle.getString("Button.chooseplayer"));
        panelMediaPlayer.add(btnSearchMediaPlayer, "cell 1 0");

        JTextArea tpMediaPlayer = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.mediaplayer.hint"));
        panelMediaPlayer.add(tpMediaPlayer, "cell 1 1 2 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMediaPlayer, L2);
      }
    }
    {
      JPanel panelMediaFramework = new JPanel();
      panelMediaFramework.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][][300lp][]", "[][]"));
      JLabel lblMediaFrameworkT = new TmmLabel(TmmResourceBundle.getString("Settings.mediaframework"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMediaFramework, lblMediaFrameworkT, true);
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");
      {
        tfMediaFramework = new JTextField();
        panelMediaFramework.add(tfMediaFramework, "cell 1 0 2 1");
        tfMediaFramework.setColumns(35);

        btnSearchFFMpegBinary = new JButton(TmmResourceBundle.getString("Button.chooseffmpeglocation"));
        panelMediaFramework.add(btnSearchFFMpegBinary, "cell 1 0");

        JTextArea tpFFMpegLocation = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.mediaframework.hint"));
        panelMediaFramework.add(tpFFMpegLocation, "cell 1 1 2 1, growx");
        TmmFontHelper.changeFont(tpFFMpegLocation, L2);

      }
    }
    {
      JPanel panelMemory = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][][300lp][grow]", ""));

      JLabel lblMemoryT = new TmmLabel(TmmResourceBundle.getString("Settings.memoryborder"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMemory, lblMemoryT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#memory-settings"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");
      {
        lblMemoryT = new JLabel(TmmResourceBundle.getString("Settings.memory"));
        panelMemory.add(lblMemoryT, "cell 1 0,aligny top");

        sliderMemory = new JSlider();
        sliderMemory.setPaintLabels(true);
        sliderMemory.setPaintTicks(true);
        sliderMemory.setSnapToTicks(true);
        sliderMemory.setMajorTickSpacing(512);
        sliderMemory.setMinorTickSpacing(128);
        sliderMemory.setMinimum(256);
        if (Platform.is64Bit()) {
          sliderMemory.setMaximum(2560);
        }
        else {
          sliderMemory.setMaximum(1536);
        }
        sliderMemory.setValue(512);
        panelMemory.add(sliderMemory, "cell 2 0,growx,aligny top");

        lblMemory = new JLabel("512");
        panelMemory.add(lblMemory, "cell 3 0,aligny top");

        JLabel lblMb = new JLabel("MB");
        panelMemory.add(lblMb, "cell 3 0,aligny top");

        JTextArea tpMemoryHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.memory.hint"));
        panelMemory.add(tpMemoryHint, "cell 1 1 3 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMemoryHint, L2);
      }
    }
    {
      JPanel panelProxy = new JPanel();
      panelProxy.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][][grow]", "[][][][]")); // 16lp ~ width of the

      JLabel lblProxyT = new TmmLabel(TmmResourceBundle.getString("Settings.proxy"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelProxy, lblProxyT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#proxy-settings"));
      add(collapsiblePanel, "cell 0 6,growx,wmin 0");
      {
        JLabel lblProxyHostT = new JLabel(TmmResourceBundle.getString("Settings.proxyhost"));
        panelProxy.add(lblProxyHostT, "cell 1 0,alignx right");

        tfProxyHost = new JTextField();
        panelProxy.add(tfProxyHost, "cell 2 0");
        tfProxyHost.setColumns(20);
        lblProxyHostT.setLabelFor(tfProxyHost);

        JLabel lblProxyPortT = new JLabel(TmmResourceBundle.getString("Settings.proxyport"));
        panelProxy.add(lblProxyPortT, "cell 1 1,alignx right");
        lblProxyPortT.setLabelFor(tfProxyPort);

        tfProxyPort = new JTextField();
        panelProxy.add(tfProxyPort, "cell 2 1");
        tfProxyPort.setColumns(20);

        JLabel lblProxyUserT = new JLabel(TmmResourceBundle.getString("Settings.proxyuser"));
        panelProxy.add(lblProxyUserT, "cell 1 2,alignx right");
        lblProxyUserT.setLabelFor(tfProxyUsername);

        tfProxyUsername = new JTextField();
        panelProxy.add(tfProxyUsername, "cell 2 2");
        tfProxyUsername.setColumns(20);

        JLabel lblProxyPasswordT = new JLabel(TmmResourceBundle.getString("Settings.proxypass"));
        panelProxy.add(lblProxyPasswordT, "cell 1 3,alignx right");
        lblProxyPasswordT.setLabelFor(tfProxyPassword);

        tfProxyPassword = new JPasswordField();
        tfProxyPassword.setColumns(20);
        panelProxy.add(tfProxyPassword, "cell 2 3");
      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][grow]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#misc-settings-1"));
      add(collapsiblePanel, "cell 0 8,growx,wmin 0");
      {
        JLabel lblParallelDownloadCountT = new JLabel(TmmResourceBundle.getString("Settings.paralleldownload"));
        panelMisc.add(lblParallelDownloadCountT, "cell 1 0 2 1");

        spMaximumDownloadThreads = new JSpinner();
        spMaximumDownloadThreads.setMinimumSize(new Dimension(60, 20));
        panelMisc.add(spMaximumDownloadThreads, "cell 1 0 2 1");

        chckbxIgnoreSSLProblems = new JCheckBox(TmmResourceBundle.getString("Settings.ignoressl"));
        panelMisc.add(chckbxIgnoreSSLProblems, "cell 1 1 2 1");

        JTextPane tpSSLHint = new ReadOnlyTextPane();
        tpSSLHint.setText(TmmResourceBundle.getString("Settings.ignoressl.desc"));
        TmmFontHelper.changeFont(tpSSLHint, L2);
        panelMisc.add(tpSSLHint, "cell 2 2,grow");
      }
    }
  }

  private void initMemorySlider() {
    Path file = Paths.get(LauncherExtraConfig.LAUNCHER_EXTRA_YML);
    int maxMemory = 512;
    if (Files.exists(file)) {
      // parse out memory option from extra.txt
      try {
        LauncherExtraConfig extraConfig = LauncherExtraConfig.readFile(file.toFile());
        Matcher matcher = MEMORY_PATTERN.matcher(String.join("\n", extraConfig.jvmOpts));
        if (matcher.find()) {
          maxMemory = Integer.parseInt(matcher.group(1));
          String dimension = matcher.group(2);
          if ("k".equalsIgnoreCase(dimension)) {
            maxMemory /= 1024;
          }
          if ("g".equalsIgnoreCase(dimension)) {
            maxMemory *= 1024;
          }
        }
      }
      catch (Exception e) {
        maxMemory = 512;
      }
    }

    sliderMemory.setValue(maxMemory);

    // add a listener to write the actual memory state to extra.txt
    addHierarchyListener(new HierarchyListener() {
      private boolean oldState = false;

      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        if (oldState != isShowing()) {
          oldState = isShowing();
          if (!isShowing()) {
            writeMemorySettings();
          }
        }
      }
    });
  }

  private void writeMemorySettings() {
    int memoryAmount = sliderMemory.getValue();

    Path file = Paths.get(LauncherExtraConfig.LAUNCHER_EXTRA_YML);
    try {
      LauncherExtraConfig extraConfig = LauncherExtraConfig.readFile(file.toFile());

      // delete any old memory setting
      extraConfig.jvmOpts.removeIf(option -> MEMORY_PATTERN.matcher(option).find());

      // set the new one if it differs from the default
      if (memoryAmount != 512) {
        extraConfig.jvmOpts.add("-Xmx" + memoryAmount + "m");
      }

      // and re-write the settings
      extraConfig.save();
    }
    catch (Exception e) {
      LOGGER.warn("Could not write memory settings - {}", e.getMessage());
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("proxyHost");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, tfProxyHost,
        jTextFieldBeanProperty);
    autoBinding.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("proxyPort");
    Property jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, tfProxyPort,
        jTextFieldBeanProperty_1);
    autoBinding_1.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("proxyUsername");
    Property jTextFieldBeanProperty_2 = BeanProperty.create("text");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, tfProxyUsername,
        jTextFieldBeanProperty_2);
    autoBinding_2.bind();
    //
    Property settingsBeanProperty_3 = BeanProperty.create("proxyPassword");
    Property jPasswordFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_3, tfProxyPassword,
        jPasswordFieldBeanProperty);
    autoBinding_3.bind();
    //
    Property settingsBeanProperty_6 = BeanProperty.create("mediaPlayer");
    Property jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_6, tfMediaPlayer,
        jTextFieldBeanProperty_3);
    autoBinding_9.bind();
    //
    Property settingsBeanProperty_7 = BeanProperty.create("mediaFramework");
    Property jTextFieldBeanProperty_4 = BeanProperty.create("text");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_7, tfMediaFramework,
        jTextFieldBeanProperty_4);
    autoBinding_10.bind();
    //
    Property jSliderBeanProperty = BeanProperty.create("value");
    Property jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ, sliderMemory, jSliderBeanProperty, lblMemory, jLabelBeanProperty);
    autoBinding_11.bind();
    //
    Property settingsBeanProperty_4 = BeanProperty.create("ignoreSSLProblems");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_4, chckbxIgnoreSSLProblems,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property settingsBeanProperty_5 = BeanProperty.create("maximumDownloadThreads");
    Property jSpinnerBeanProperty = BeanProperty.create("value");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_5, spMaximumDownloadThreads,
        jSpinnerBeanProperty);
    autoBinding_5.bind();
  }
}
