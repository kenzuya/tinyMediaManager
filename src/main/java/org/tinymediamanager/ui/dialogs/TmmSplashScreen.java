/*
 * Copyright 2012 - 2022 Manuel Laggner
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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.plaf.basic.BasicProgressBarUI;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.images.TmmSvgIcon;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TmmSplashScreen} is used to draw our own splash screen in swing
 * 
 * @author Manuel Laggner
 */
public class TmmSplashScreen extends JDialog {
  protected static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages");
  private final JProgressBar            progressBar;
  private final JLabel                  lblText;
  private final JLabel                  lblVersion;

  public TmmSplashScreen() throws URISyntaxException {
    ImageIcon splashscreen = new TmmSvgIcon(IconManager.class.getResource("images/svg/splashscreen.svg").toURI());

    {
      JLabel lblBackground = new JLabel(splashscreen);
      getContentPane().add(lblBackground, BorderLayout.CENTER);

      lblBackground.setLayout(new BorderLayout());

      JPanel panelSouth = new JPanel();
      panelSouth.setOpaque(false);
      lblBackground.add(panelSouth, BorderLayout.SOUTH);
      panelSouth.setLayout(new MigLayout("ins 20", "[grow,fill][]", "[][]"));

      progressBar = new JProgressBar();
      progressBar.setUI(new TmmSplashProgressBar());
      progressBar.setPreferredSize(new Dimension(200, 15));
      panelSouth.add(progressBar, "cell 0 0 2 1,growx");

      lblText = new JLabel(TmmResourceBundle.getString("splash.loading"));
      lblText.setForeground(new Color(134, 134, 134));
      panelSouth.add(lblText, "cell 0 1,growx , wmin 0");

      lblVersion = new JLabel("");
      lblVersion.setForeground(new Color(134, 134, 134));
      TmmFontHelper.changeFont(lblVersion, TmmFontHelper.L2);
      panelSouth.add(lblVersion, "cell 1 1,alignx right");
    }

    setModal(false);
    setUndecorated(true);
    setDefaultLookAndFeelDecorated(false);
    setBackground(new Color(0, 0, 0, 0));
    pack();
    setLocationRelativeTo(null);
  }

  /**
   * Set the version number
   * 
   * @param version
   *          the version number
   */
  public void setVersion(String version) {
    lblVersion.setText(version);
  }

  /**
   * Set the progress AND the text at the same time
   * 
   * @param progress
   *          the progress value. 0 - 100
   * @param text
   *          the text to be displayed below the progress
   */
  public void setProgress(int progress, String text) {
    setProgress(progress);
    setText(text);
  }

  /**
   * Set the progress
   * 
   * @param progress
   *          the progress value. 0 - 100
   */
  public void setProgress(int progress) {
    progressBar.setValue(progress);
  }

  /**
   * Set the text
   * 
   * @param text
   *          the text to be displayed below the progress
   */
  public void setText(String text) {
    try {
      lblText.setText(BUNDLE.getString(text));
    }
    catch (Exception e) {
      lblText.setText(text);
    }
  }

  class TmmSplashProgressBar extends BasicProgressBarUI {

    @Override
    public void paint(Graphics g, JComponent c) {
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int iStrokWidth = 2;
      g2d.setStroke(new BasicStroke(iStrokWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g2d.setColor(c.getForeground());
      g2d.setBackground(c.getBackground());
      int width = c.getWidth();
      int height = c.getHeight();
      RoundRectangle2D outline = new RoundRectangle2D.Double((iStrokWidth / 2), (iStrokWidth / 2), width - iStrokWidth, height - iStrokWidth, height,
          height);
      g2d.draw(outline);
      int iInnerHeight = height - (iStrokWidth * 4);
      int iInnerWidth = width - (iStrokWidth * 4);
      iInnerWidth = (int) Math.round(iInnerWidth * progressBar.getPercentComplete());
      int x = iStrokWidth * 2;
      int y = iStrokWidth * 2;
      Point2D start = new Point2D.Double(x, y);
      Point2D end = new Point2D.Double(x, y + iInnerHeight);
      float[] dist = { 0.0f, 0.25f, 1.0f };
      Color[] colors = { c.getForeground(), c.getForeground().brighter(), c.getBackground().darker() };
      LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
      g2d.setPaint(p);
      RoundRectangle2D fill = new RoundRectangle2D.Double(iStrokWidth * 2, iStrokWidth * 2, iInnerWidth, iInnerHeight, iInnerHeight, iInnerHeight);
      g2d.fill(fill);
      g2d.dispose();
    }

    @Override
    protected void installDefaults() {
      super.installDefaults();
      progressBar.setOpaque(false);
      progressBar.setBorderPainted(false);
    }
  }
}
