/*
 * Copyright 2012 - 2023 Manuel Laggner
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

import java.awt.Window;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link RegexpInputDialog} is a plain input Dialog which validates the input to be a valid regular expression
 * 
 * @author Manuel Laggner
 */
public class RegexpInputDialog extends TmmDialog {
  private static final long   serialVersionUID = 123315882962746712L;
  private static final String DIALOG_ID        = "regexpInput";

  private String              regularExpression;

  private JTextField          tfExpression;
  private JButton             btnOk;
  private JLabel              lblInvalidExpression;
  private JLabel              lblDescription;

  /** Creates the reusable dialog. */
  public RegexpInputDialog(Window owner) {
    super(owner, TmmResourceBundle.getString("tmm.regexp"), DIALOG_ID);

    initComponents();

    lblInvalidExpression.setVisible(false);
    btnOk.setEnabled(false);

    tfExpression.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        checkExpression();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        checkExpression();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        checkExpression();
      }

      private void checkExpression() {
        String text = tfExpression.getText();
        if (StringUtils.isBlank(text)) {
          lblInvalidExpression.setVisible(false);
          btnOk.setEnabled(false);
        }
        else {
          try {
            Pattern.compile(text);
            lblInvalidExpression.setVisible(false);
            btnOk.setEnabled(true);
          }
          catch (Exception e) {
            lblInvalidExpression.setVisible(true);
            btnOk.setEnabled(false);
          }
        }
      }
    });
  }

  private void initComponents() {
    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent);
    panelContent.setLayout(new MigLayout("", "[300lp,grow]", "[][][]"));
    {
      lblDescription = new JLabel(TmmResourceBundle.getString("tmm.regexp"));
      panelContent.add(lblDescription, "cell 0 0");
    }
    {
      tfExpression = new JTextField();
      panelContent.add(tfExpression, "cell 0 1,growx");
      tfExpression.setColumns(10);
    }
    {
      lblInvalidExpression = new JLabel(TmmResourceBundle.getString("tmm.regexp.invalid"));
      panelContent.add(lblInvalidExpression, "cell 0 2");
    }
    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.addActionListener(e -> {
        regularExpression = null;
        setVisible(false);
      });
      addButton(btnCancel);

      btnOk = new JButton(TmmResourceBundle.getString("Button.ok"));
      btnOk.addActionListener(e -> {
        regularExpression = tfExpression.getText();
        setVisible(false);
      });
      addDefaultButton(btnOk);
    }
  }

  /**
   * Returns null if the typed string was invalid; otherwise, returns the string as the user entered it.
   */
  public String getRegularExpression() {
    return regularExpression;
  }
}
