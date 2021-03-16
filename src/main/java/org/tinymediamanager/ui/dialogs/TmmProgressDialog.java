package org.tinymediamanager.ui.dialogs;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public abstract class TmmProgressDialog extends TmmDialog {

  private JProgressBar              progressBar;
  private JLabel                    lblProgressAction;

  public TmmProgressDialog(String title, String id) {
    super(title, id);
    init();
  }

  public TmmProgressDialog(JFrame owner, String title, String id) {
    super(owner, title, id);
    init();
  }

  public TmmProgressDialog(JDialog owner, String title, String id) {
    super(owner, title, id);
    init();
  }

  public TmmProgressDialog(Window owner, String title, String id) {
    super(owner, title, id);
    init();
  }

  private void init() {
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new MigLayout("", "[][grow]", "[]"));

    progressBar = new JProgressBar();
    progressBar.setVisible(false);
    infoPanel.add(progressBar, "cell 0 0");

    lblProgressAction = new JLabel("");
    infoPanel.add(lblProgressAction, "cell 1 0");

    setBottomInformationPanel(infoPanel);
  }

  public void startProgress(String description) {
    startProgress(description, -1);
  }

  public void startProgress(String description, int max) {
    lblProgressAction.setText(description);
    progressBar.setVisible(true);
    if (max <= 0) {
      progressBar.setIndeterminate(true);
    } else {
      progressBar.setIndeterminate(false);
      progressBar.setMinimum(0);
      progressBar.setMaximum(max);
    }
  }

  public void setProgressValue(int value) {
    progressBar.setValue(value);
  }

  public void stopProgress() {
    lblProgressAction.setText("");
    progressBar.setVisible(false);
  }
}
