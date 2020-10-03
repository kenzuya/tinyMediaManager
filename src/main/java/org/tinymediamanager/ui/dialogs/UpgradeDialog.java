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
package org.tinymediamanager.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;

import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;

import net.miginfocom.swing.MigLayout;

/**
 * The class WhatsNewDialog. Used to show the user a list of changelogs after each upgrade
 * 
 * @author Manuel Laggner
 */
public class UpgradeDialog extends TmmDialog {
  private static final long serialVersionUID = -4071143363981892283L;
  private JButton           btnUpgrade;
  private JCheckBox         chckbxAccept;

  public UpgradeDialog() {
    super(BUNDLE.getString("tmm.upgradev4"), "upgradev4");
    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[600lp,grow]", "[][][][shrink 0][400lp,grow][shrink 0][]"));

      JLabel lblTinymediamanagerV = new JLabel("tinyMediaManager v4");
      TmmFontHelper.changeFont(lblTinymediamanagerV, TmmFontHelper.H1, Font.BOLD);
      panelContent.add(lblTinymediamanagerV, "cell 0 0");

      JTextPane tpTop = new ReadOnlyTextPane(BUNDLE.getString("tmm.upgradev4.desc"));
      panelContent.add(tpTop, "cell 0 1,grow");

      LinkLabel lblLinkLabel = new LinkLabel("https://www.tinymediamanager.org/blog/version-4-0/");
      lblLinkLabel.addActionListener(arg0 -> {
        try {
          TmmUIHelper.browseUrl("https://www.tinymediamanager.org/blog/version-4-0/");
        }
        catch (Exception ignored) {
        }
      });
      panelContent.add(lblLinkLabel, "cell 0 2, grow");

      JSeparator separator = new JSeparator();
      panelContent.add(separator, "cell 0 3,growx");

      JScrollPane scrollPane = new JScrollPane();
      panelContent.add(scrollPane, "cell 0 4,grow");

      JTextPane tpChangelog = new ReadOnlyTextPane(changelogV4());
      scrollPane.setViewportView(tpChangelog);

      JSeparator separator_1 = new JSeparator();
      panelContent.add(separator_1, "cell 0 5,growx");

      chckbxAccept = new JCheckBox(BUNDLE.getString("tmm.upgradev4.accept"));
      panelContent.add(chckbxAccept, "flowx,cell 0 6");

      btnUpgrade = new JButton(BUNDLE.getString("tmm.upgradev4"));
      btnUpgrade.addActionListener(e -> {
        // exchange the getdown path
        try {
          Path path = Paths.get("getdown.txt");

          String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
          content = content.replace("tinymediamanager.org/v3/build/", "tinymediamanager.org/v4/build/");
          Files.write(path, content.getBytes(StandardCharsets.UTF_8));
          MainWindow.getActiveInstance().checkForUpdate();
          setVisible(false);
        }
        catch (Exception ex) {
          // ignored
        }
      });

      Component horizontalGlue = Box.createHorizontalGlue();
      panelContent.add(horizontalGlue, "cell 0 6,growx");
      panelContent.add(btnUpgrade, "cell 0 6");
    }
    {
      JButton btnClose = new JButton(BUNDLE.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);
    }
    initDataBindings();
  }

  protected void initDataBindings() {
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    BeanProperty<JButton, Boolean> jButtonBeanProperty = BeanProperty.create("enabled");
    AutoBinding<JCheckBox, Boolean, JButton, Boolean> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxAccept,
        jCheckBoxBeanProperty, btnUpgrade, jButtonBeanProperty);
    autoBinding.bind();
  }

  private String changelogV4() {
    try {
      return IOUtils.resourceToString("/changelogv4.txt", Charset.defaultCharset());
    }
    catch (Exception e) {
      // ignored
    }
    return "";
  }
}
