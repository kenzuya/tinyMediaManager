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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFileSubtitle;

import net.miginfocom.swing.MigLayout;

/**
 * this dialog is used for editing a subtitle
 *
 * @author Wolfgang Janes
 */
public class SubtitleEditorDialog extends TmmDialog {

    private static final String DIALOG_ID = "subtitleEditor";

    private JTextField tfLanguage;
    private JCheckBox cbForced;
    private MediaFileSubtitle mediaFileSubtitleToEdit;

    public SubtitleEditorDialog(Window owner, String title, MediaFileSubtitle mediaFileSubtitleToEdit) {
        super(owner,title,DIALOG_ID);
        this.mediaFileSubtitleToEdit = mediaFileSubtitleToEdit;
        initComponents();
    }


    private void initComponents() {
        JPanel panelContent = new JPanel();
        getContentPane().add(panelContent);
        panelContent.setLayout(new MigLayout("", "[][50lp:n,grow]", "[][]"));

        {
            JLabel lblLanguage = new JLabel(TmmResourceBundle.getString("metatag.language"));
            panelContent.add(lblLanguage,"cell 0 0, alignx trailing");

            tfLanguage = new JTextField();
            panelContent.add(tfLanguage,"cell 1 0, growx");
        }
        {
            JLabel lblForced = new JLabel(TmmResourceBundle.getString("metatag.forced"));
            panelContent.add(lblForced,"cell 0 1, alignx trailing");

            cbForced = new JCheckBox();
            panelContent.add(cbForced,"cell 1 1, growx");
        }
        {
            JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
            btnCancel.addActionListener(e -> setVisible(false));
            addButton(btnCancel);

            JButton btnOk = new JButton(TmmResourceBundle.getString("Button.save"));
            btnOk.addActionListener(e -> {

                mediaFileSubtitleToEdit.setForced(cbForced.isSelected());
                mediaFileSubtitleToEdit.setLanguage(tfLanguage.getText());

                setVisible(false);
            });
            addDefaultButton(btnOk);
        }
    }

}
