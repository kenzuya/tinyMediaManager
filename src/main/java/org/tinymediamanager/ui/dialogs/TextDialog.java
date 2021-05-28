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
package org.tinymediamanager.ui.dialogs;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.components.NoBorderScrollPane;

import net.miginfocom.swing.MigLayout;

/**
 * Dialog Class for showing a text
 */
public class TextDialog extends TmmDialog {

  private JTextArea taText;

  public TextDialog(String text) {
    super(TmmResourceBundle.getString("mediafiletype.text"), "text");
    setBounds(5, 5, 1000, 590);

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);
    panelContent.setLayout(new MigLayout("", "[600lp,grow]", "[400lp,grow]"));

    JScrollPane scrollPane = new NoBorderScrollPane();
    panelContent.add(scrollPane, "cell 0 0,grow");

    taText = new JTextArea();
    scrollPane.setViewportView(taText);
    taText.setEditable(false);
    taText.setWrapStyleWord(true);
    taText.setLineWrap(true);
    taText.setText(text);

      JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);
    }
  }

