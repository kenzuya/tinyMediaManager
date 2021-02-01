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

import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.trakttv.TraktTv;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.TmmLabel;

import net.miginfocom.swing.MigLayout;

/**
 * The class ExternalServicesSettingsPanel. Handle all settings for the external services
 * 
 * @author Manuel Laggner
 */
class ExternalServicesSettingsPanel extends JPanel {
  private static final long serialVersionUID = 7266564870819511988L;

  private JButton           btnGetTraktPin;
  private JButton           btnTestTraktConnection;
  private JLabel            lblTraktStatus;

  ExternalServicesSettingsPanel() {
    // UI init
    initComponents();

    // data init
    if (License.getInstance().isValidLicense()
        && StringUtils.isNoneBlank(Globals.settings.getTraktAccessToken(), Globals.settings.getTraktRefreshToken())) {
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
    }
    else {
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
    }

    btnGetTraktPin.addActionListener(e -> getTraktPin());
    btnGetTraktPin.setEnabled(License.getInstance().isValidLicense());
    btnTestTraktConnection.addActionListener(e -> {
      try {
        TraktTv.getInstance().refreshAccessToken();
        JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.testconnection.good"),
            TmmResourceBundle.getString("Settings.trakt.testconnection"), JOptionPane.INFORMATION_MESSAGE);
      }
      catch (Exception e1) {
        JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.testconnection.bad"),
            TmmResourceBundle.getString("Settings.trakt.testconnection"), JOptionPane.ERROR_MESSAGE);
      }
    });
    btnTestTraktConnection.setEnabled(License.getInstance().isValidLicense());
  }

  private void getTraktPin() {
    // open the pin url in a browser
    try {
      TmmUIHelper.browseUrl("https://trakt.tv/pin/799");
    }
    catch (Exception e1) {
      // browser could not be opened, show a dialog box
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.fallback"),
          TmmResourceBundle.getString("Settings.trakt.getpin"), JOptionPane.INFORMATION_MESSAGE);
    }

    // let the user insert the pin
    String pin = JOptionPane.showInputDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.entercode"));

    // try to get the tokens
    String accessToken = "";
    String refreshToken = "";
    try {
      Map<String, String> tokens = TraktTv.getInstance().authenticateViaPin(pin);
      accessToken = tokens.get("accessToken") == null ? "" : tokens.get("accessToken");
      refreshToken = tokens.get("refreshToken") == null ? "" : tokens.get("refreshToken");
    }
    catch (Exception ignored) {
    }

    Globals.settings.setTraktAccessToken(accessToken);
    Globals.settings.setTraktRefreshToken(refreshToken);

    if (StringUtils.isNoneBlank(Globals.settings.getTraktAccessToken(), Globals.settings.getTraktRefreshToken())) {
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
    }
    else {
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.problem"),
          TmmResourceBundle.getString("Settings.trakt.getpin"), JOptionPane.ERROR_MESSAGE);
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[]"));
    {
      JPanel panelTrakt = new JPanel();
      panelTrakt.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblTraktT = new TmmLabel(TmmResourceBundle.getString("Settings.trakt"), H3);

      if (!License.getInstance().isValidLicense()) {
        lblTraktT.setText("*PRO* " + lblTraktT.getText());
      }

      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelTrakt, lblTraktT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#trakttv"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        lblTraktStatus = new JLabel("");
        panelTrakt.add(lblTraktStatus, "cell 1 0 2 1");
      }
      {
        btnGetTraktPin = new JButton(TmmResourceBundle.getString("Settings.trakt.getpin"));
        panelTrakt.add(btnGetTraktPin, "cell 1 1 2 1");

        btnTestTraktConnection = new JButton(TmmResourceBundle.getString("Settings.trakt.testconnection"));
        panelTrakt.add(btnTestTraktConnection, "cell 1 1");
      }
    }
  }
}
