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
package org.tinymediamanager.ui.panels;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.WrapLayout;
import org.tinymediamanager.ui.components.NoBorderScrollPane;

import net.miginfocom.swing.MigLayout;

/**
 * The Image Panel is used to display all images for a MediaEntity
 * 
 * @author Manuel Laggner
 */
public class ImagePanel extends JPanel implements HierarchyListener {
  private static final long     serialVersionUID = -5344085698387374260L;
  private static final Logger   LOGGER           = LoggerFactory.getLogger(ImagePanel.class);

  private final List<MediaFile> mediaFiles;

  protected int                 maxWidth         = 300;
  protected int                 maxHeight        = 100;
  private ImageLoader           activeWorker     = null;

  /**
   * UI components
   */
  private final JPanel          panelImages;
  private final JScrollPane     scrollPane;

  public ImagePanel(List<MediaFile> mediaFiles) {
    this.mediaFiles = mediaFiles;
    setLayout(new MigLayout("", "[400lp,grow]", "[300lp,grow]"));

    scrollPane = new NoBorderScrollPane();
    scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    add(scrollPane, "cell 0 0,grow");

    panelImages = new JPanel();
    panelImages.setLayout(new WrapLayout(FlowLayout.LEFT));
    scrollPane.setViewportView(panelImages);
  }

  /**
   * Trigger to rebuild the panel
   */
  public void rebuildPanel() {
    if (activeWorker != null && !activeWorker.isDone()) {
      activeWorker.cancel(true);
    }

    panelImages.removeAll();
    panelImages.revalidate();
    scrollPane.repaint();

    // fetch image in separate worker -> performance
    activeWorker = new ImageLoader(mediaFiles);
    activeWorker.execute();
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public int getMaxHeight() {
    return maxHeight;
  }

  public void setMaxWidth(int maxWidth) {
    this.maxWidth = maxWidth;
  }

  public void setMaxHeight(int maxHeight) {
    this.maxHeight = maxHeight;
  }

  @Override
  public void hierarchyChanged(HierarchyEvent arg0) {
    if (isShowing() && panelImages.getComponents().length == 0 && !mediaFiles.isEmpty()) {
      // rebuild the panel
      rebuildPanel();
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    addHierarchyListener(this);
  }

  @Override
  public void removeNotify() {
    removeHierarchyListener(this);
    super.removeNotify();
  }

  /**
   * worker to load the images asynchrony
   */
  protected class ImageLoader extends SwingWorker<Void, ImageChunk> {
    private final List<MediaFile> mediaFiles;

    private ImageLoader(List<MediaFile> mediaFiles) {
      this.mediaFiles = mediaFiles;
    }

    @Override
    protected Void doInBackground() {
      for (MediaFile mediaFile : mediaFiles) {
        if (isShowing()) {
          if (isCancelled()) {
            return null;
          }
          try {
            Path file = ImageCache.getCachedFile(mediaFile);
            if (file == null) {
              file = mediaFile.getFileAsPath();
            }
            BufferedImage bufferedImage = ImageUtils.createImage(file);
            Point size = ImageUtils.calculateSize(maxWidth, maxHeight, bufferedImage.getWidth(), bufferedImage.getHeight(), true);
            BufferedImage img = Scalr.resize(bufferedImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, size.x, size.y, Scalr.OP_ANTIALIAS);
            bufferedImage = null;

            if (isCancelled()) {
              return null;
            }

            publish(new ImageChunk(mediaFile.getFileAsPath().toString(), mediaFile.getType(), img));
            img = null;
          }
          catch (Exception e) {
            LOGGER.trace("scaling image failed: {}", e.getMessage());
          }
        }
      }
      return null;
    }

    @Override
    protected void process(List<ImageChunk> chunks) {
      for (ImageChunk chunk : chunks) {
        try {
          if (isCancelled()) {
            return;
          }

          JPanel panelContainer = new JPanel(new BorderLayout());

          JLabel lblImageJLabel = new JLabel(new ImageIcon(chunk.image));
          lblImageJLabel.addMouseListener(new ImageLabelClickListener(chunk.pathToImage));
          panelContainer.add(lblImageJLabel, BorderLayout.CENTER);

          JLabel lblImageType = new JLabel(TmmResourceBundle.getString("mediafiletype." + chunk.type.name().toLowerCase(Locale.ROOT)));
          lblImageType.setHorizontalAlignment(JLabel.CENTER);
          panelContainer.add(lblImageType, BorderLayout.SOUTH);

          panelImages.add(panelContainer);
          panelImages.revalidate();
          scrollPane.repaint();
        }
        catch (Exception ignored) {
          // ignore
        }
      }
    }
  }

  protected static class ImageChunk {
    private final String        pathToImage;
    private final BufferedImage image;
    private final MediaFileType type;

    private ImageChunk(String path, MediaFileType type, BufferedImage image) {
      this.pathToImage = path;
      this.type = type;
      this.image = image;
    }
  }

  /*
   * click listener for creating a lightbox effect
   */
  private static class ImageLabelClickListener implements MouseListener {
    private final String pathToFile;

    private ImageLabelClickListener(String path) {
      this.pathToFile = path;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
      if (StringUtils.isNotBlank(pathToFile)) {
        MainWindow.getInstance().createLightbox(pathToFile, "");
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // not used
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // not used
    }

    @Override
    public void mousePressed(MouseEvent e) {
      // not used
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // not used
    }
  }
}
