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
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.license.License;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;

import net.miginfocom.swing.MigLayout;

/**
 * this class offers the link to purchase tinyMediaManager and lets the user enter the license code
 * 
 * @author Manuel Laggner
 */
public class UnlockDialog extends TmmDialog {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnlockDialog.class);

  public UnlockDialog() {
    super(MainWindow.getInstance(), BUNDLE.getString("tmm.unlock.desc"), "unlockDialog");

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);
    panelContent.setLayout(new MigLayout("", "[grow][400lp]", "[][][][10lp:n][][][100lp:150lp,grow][grow]"));
    {
      JTextArea taLicenseHint = new ReadOnlyTextArea(BUNDLE.getString("tmm.license.hint1") + "\n\n" + BUNDLE.getString("tmm.license.hint2"));
      taLicenseHint.setLineWrap(true);
      panelContent.add(taLicenseHint, "cell 0 0 2 1,grow");

      JButton btnOpenPaddle = new JButton(BUNDLE.getString("tmm.license.buy"));
      btnOpenPaddle.addActionListener(e -> {
        String url = StringEscapeUtils.unescapeHtml4("https://www.tinymediamanager.org/purchase/");
        try {
          TmmUIHelper.browseUrl(url);
        }
        catch (Exception e1) {
          LOGGER.error("FAQ", e1);
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e1.getLocalizedMessage() }));
        }
      });
      panelContent.add(btnOpenPaddle, "cell 0 1 2 1");
    }
    {
      JSeparator separator = new JSeparator();
      panelContent.add(separator, "cell 0 3 2 1,growx");
    }
    {
      JLabel lblEnterLicenseCodeT = new JLabel(BUNDLE.getString("tmm.license.code"));
      panelContent.add(lblEnterLicenseCodeT, "cell 0 5");

      JTextArea taLicenseCode = new JTextArea();
      taLicenseCode.setLineWrap(true);

      JScrollPane scrollPane = new JScrollPane(taLicenseCode);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      panelContent.add(scrollPane, "cell 1 5 1 2,grow");

      JButton btnUnlock = new JButton(BUNDLE.getString("tmm.license.unlock"));
      btnUnlock.addActionListener(arg0 -> {
        try {
          License.getInstance().setLicenseCode(taLicenseCode.getText());

          if (License.getInstance().isValidLicense()) {
            // if we're reaching this, the license code was valid!

            // persist the license code
            Utils.writeStringToFile(Paths.get(Globals.DATA_FOLDER, "tmm.lic"), taLicenseCode.getText());

            JOptionPane.showMessageDialog(UnlockDialog.this, BUNDLE.getString("tmm.license.thanks"));
            JOptionPane.showMessageDialog(UnlockDialog.this, BUNDLE.getString("tmm.license.restart"));
            setVisible(false);
          }
          else {
            JOptionPane.showMessageDialog(UnlockDialog.this, BUNDLE.getString("tmm.license.invalid"));
          }
        }
        catch (Exception e) {
          JOptionPane.showMessageDialog(UnlockDialog.this, BUNDLE.getString("tmm.license.invalid"));
        }
      });
      panelContent.add(btnUnlock, "cell 1 7");

    }
    {
      JButton btnClose = new JButton(BUNDLE.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);
    }

  }
}
