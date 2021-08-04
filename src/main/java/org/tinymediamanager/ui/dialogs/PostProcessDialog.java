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
import java.awt.event.ActionEvent;
import java.nio.file.Path;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.SystemUtils;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link PostProcessDialog} is the abstract parent for all post process settings pages
 *
 * @author Wolfgang Janes
 */
public abstract class PostProcessDialog extends TmmDialog {

  protected PostProcess      process       = null;

  protected final JTextField tfProcessName = new JTextField();
  protected final JTextField tfPath        = new JTextField();
  protected final JTextField tfCommand     = new JTextField();

  public PostProcessDialog() {
    super(TmmResourceBundle.getString("Settings.addpostprocess"), "addPostProcess");

    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[][][]", "[][][]"));

      // Name
      JLabel lblProcessName = new JLabel(TmmResourceBundle.getString("Settings.processname"));
      panelContent.add(lblProcessName, "cell 0 0,alignx right");

      tfProcessName.setColumns(20);
      panelContent.add(tfProcessName, "cell 1 0");

      // Path
      JLabel lblPath = new JLabel(TmmResourceBundle.getString("metatag.path"));
      panelContent.add(lblPath, "cell 0 1, alignx right");

      tfPath.setColumns(30);
      panelContent.add(tfPath, "cell 1 1");

      // Button Search Path
      JButton btnChoosePostProcessPath = new JButton(TmmResourceBundle.getString("Button.choosefile"));
      panelContent.add(btnChoosePostProcessPath, "cell 2 1");
      btnChoosePostProcessPath.addActionListener(e -> {
        String path = TmmProperties.getInstance().getProperty("postprocess.path");
        Path file = TmmUIHelper.selectFile(TmmResourceBundle.getString("Button.choosefile"), path, null);
        if (file != null && (Utils.isRegularFile(file) || SystemUtils.IS_OS_MAC)) {
          tfPath.setText(file.toAbsolutePath().toString());
          TmmProperties.getInstance().putProperty("postprocess.path", file.getParent().toString());
        }
      });

      // Command
      JLabel lblCommand = new JLabel(TmmResourceBundle.getString("Settings.commandname"));
      panelContent.add(lblCommand, "cell 0 2, alignx right");

      tfCommand.setColumns(30);
      panelContent.add(tfCommand, "cell 1 2");

    }
    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setAction(new PostProcessDialog.CancelAction());
      addButton(btnCancel);

      JButton btnSave = new JButton(TmmResourceBundle.getString("Button.save"));
      btnSave.setAction(new PostProcessDialog.SaveAction());
      addDefaultButton(btnSave);
    }

  }

  public void setProcess(PostProcess process) {
    this.process = process;
    this.tfProcessName.setText(process.getName());
    this.tfPath.setText(process.getPath());
    this.tfCommand.setText(process.getCommand());
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = -8416641526799936831L;

    CancelAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.cancel"));
      putValue(SMALL_ICON, IconManager.CANCEL_INV);
      putValue(LARGE_ICON_KEY, IconManager.CANCEL_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }

  private class SaveAction extends AbstractAction {
    private static final long serialVersionUID = 1740130137146252281L;

    SaveAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.save"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      save();
    }
  }

  protected abstract void save();

}
