/*
 * Copyright 2012 - 2019 Manuel Laggner
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
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;

import hu.kazocsaba.imageviewer.ImageViewer;

/**
 * The class ImagePreviewDialog. To display a preview of the image in the image chooser
 * 
 * @author Manuel Laggner
 */
public class ImagePreviewDialog extends TmmDialog {
  private static final long                serialVersionUID = -7479476493187235867L;
  private static final Logger              LOGGER           = LoggerFactory.getLogger(ImagePreviewDialog.class);

  private String                           imageUrl;
  private Path                             imagePath;
  private SwingWorker<BufferedImage, Void> worker;
  private ImageViewer                      imgViewer        = new ImageViewer();

  private JLabel                           lblLoadingInfo;

  public ImagePreviewDialog(String urlToImage) {
    super("", "imagePreview");
    this.imageUrl = urlToImage;

    lblLoadingInfo = new JLabel(BUNDLE.getString("image.download")); //$NON-NLS-1$
    lblLoadingInfo.setBorder(new EmptyBorder(10, 10, 10, 10));
    TmmFontHelper.changeFont(lblLoadingInfo, 1.5f);
    getContentPane().add(lblLoadingInfo, BorderLayout.CENTER);

    setBottomPanel(new JPanel());

    worker = new ImageFetcher();
    worker.execute();
  }

  public ImagePreviewDialog(Path pathToImage) {
    super("", "imagePreview");
    this.imagePath = pathToImage;

    lblLoadingInfo = new JLabel(BUNDLE.getString("image.download")); //$NON-NLS-1$
    lblLoadingInfo.setBorder(new EmptyBorder(10, 10, 10, 10));
    TmmFontHelper.changeFont(lblLoadingInfo, 1.5f);
    getContentPane().add(lblLoadingInfo, BorderLayout.CENTER);

    setBottomPanel(new JPanel());

    worker = new ImageFetcherPath();
    worker.execute();

  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      // get max screen size on multi screen setups
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      int width = gd.getDisplayMode().getWidth();
      int height = gd.getDisplayMode().getHeight();
      setMaximumSize(new Dimension(width, height));

      pack();
      setLocationRelativeTo(MainWindow.getActiveInstance());
      super.setVisible(true);
    }
    else {
      if (worker != null && !worker.isDone()) {
        worker.cancel(true);
      }
      super.setVisible(false);
      dispose();
    }
  }

  /***************************************************************************
   * helper classes
   **************************************************************************/
  protected class ImageFetcher extends SwingWorker<BufferedImage, Void> {
    @Override
    protected BufferedImage doInBackground() {
      try {
        Url url = new Url(imageUrl);
        return ImageUtils.createImage(url.getBytesWithRetry(5));
      }
      catch (Exception e) {
        LOGGER.warn("fetch image: " + e.getMessage());
        return null;
      }
    }

    @Override
    protected void done() {
      try {
        BufferedImage image = get();
        if (image == null) {
          lblLoadingInfo.setText(BUNDLE.getString("image.download.failed")); //$NON-NLS-1$
          pack();
          return;
        }
        imgViewer.setImage(image);
        JComponent comp = imgViewer.getComponent();

        getContentPane().removeAll();
        getContentPane().add(comp, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(MainWindow.getActiveInstance());
      }
      catch (Exception e) {
        LOGGER.warn("fetch image: " + e.getMessage());
      }
    }
  }

  protected class ImageFetcherPath extends SwingWorker<BufferedImage, Void> {
    @Override
    protected BufferedImage doInBackground() {
      try {
        return ImageUtils.createImage(imagePath);
      }
      catch (Exception e) {
        LOGGER.warn("fetch image: " + e.getMessage());
        return null;
      }
    }

    @Override
    protected void done() {
      try {
        BufferedImage image = get();
        if (image == null) {
          lblLoadingInfo.setText(BUNDLE.getString("image.download.failed")); //$NON-NLS-1$
          pack();
          return;
        }
        imgViewer.setImage(image);
        JComponent comp = imgViewer.getComponent();

        getContentPane().removeAll();
        getContentPane().add(comp, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(MainWindow.getActiveInstance());
      }
      catch (Exception e) {
        LOGGER.warn("fetch image: " + e.getMessage());
      }
    }

  }
}

