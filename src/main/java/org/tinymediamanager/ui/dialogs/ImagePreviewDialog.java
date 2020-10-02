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

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.TmmAction;

import net.miginfocom.swing.MigLayout;

/**
 * The class ImagePreviewDialog. To display a preview of the image in the image chooser
 * 
 * @author Manuel Laggner
 */
public class ImagePreviewDialog extends TmmDialog {
  private static final long           serialVersionUID = -7479476493187235867L;
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");
  private static final Logger         LOGGER           = LoggerFactory.getLogger(ImagePreviewDialog.class);

  private String                      imageUrl;
  private String                      imagePath;

  private JLabel                      image;
  private byte[]                      originalImageBytes;

  public ImagePreviewDialog(String urlToImage) {
    super(BUNDLE.getString("image.show"), "imagePreview");
    init();

    this.imageUrl = urlToImage;
  }

  public ImagePreviewDialog(Path pathToImage) {
    super(BUNDLE.getString("image.show"), "imagePreview");
    init();

    this.imagePath = pathToImage.toString();
  }

  private void init() {
    JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new SaveToDiskAction());

    {
      JPanel imagePanel = new JPanel();
      imagePanel.setLayout(new MigLayout("", "[300lp,grow]", "[300lp,grow]"));
      JScrollPane scrollPane = new JScrollPane(imagePanel);
      getContentPane().add(scrollPane);

      image = new JLabel();
      image.setHorizontalAlignment(SwingConstants.CENTER);

      image.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent mouseEvent) {
          if (mouseEvent.isPopupTrigger()) {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
          }
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
          if (mouseEvent.isPopupTrigger()) {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
          }
        }
      });

      imagePanel.add(image, "cell 0 0, grow, center");
    }
    {
      JButton closeButton = new JButton(BUNDLE.getString("Button.close"));
      closeButton.addActionListener(e -> setVisible(false));
      addDefaultButton(closeButton);
    }
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      // get max screen size on multi screen setups
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      int width = gd.getDisplayMode().getWidth();
      int height = gd.getDisplayMode().getHeight();
      setMaximumSize(new Dimension(width, height));

      // run async to avoid strange loading artefacts
      SwingWorker<Void, Void> worker = new SwingWorker<>() {
        @Override
        protected Void doInBackground() throws Exception {
          if (StringUtils.isNotBlank(imagePath)) {
            Path file = Paths.get(imagePath);
            originalImageBytes = Files.readAllBytes(file);
            image.setIcon(new ImageIcon(originalImageBytes));
          }
          else if (StringUtils.isNotBlank(imageUrl)) {
            try {
              Url url = new Url(imageUrl);
              originalImageBytes = url.getBytesWithRetry(5);
              image.setIcon(new ImageIcon(originalImageBytes));
            }
            catch (Exception e) {
              LOGGER.error("could not load image - {}", e.getMessage());
            }
          }
          return null;
        }
      };
      worker.execute();

      pack();
      setLocationRelativeTo(MainWindow.getInstance());
      super.setVisible(true);
    }
    else {
      super.setVisible(false);
      dispose();
    }
  }

  private class SaveToDiskAction extends TmmAction {

    private SaveToDiskAction() {
      putValue(LARGE_ICON_KEY, IconManager.EXPORT);
      putValue(SMALL_ICON, IconManager.EXPORT);
      putValue(NAME, BUNDLE.getString("image.savetodisk"));
    }

    @Override
    protected void processAction(ActionEvent e) {
      // open save to dialog
      Path file;
      try {
        String filename = "";
        if (StringUtils.isNotBlank(imagePath)) {
          filename = FilenameUtils.getBaseName(imagePath);
        }
        else if (StringUtils.isNotBlank(imageUrl)) {
          filename = FilenameUtils.getBaseName(imageUrl);
        }
        file = TmmUIHelper.saveFile(BUNDLE.getString("image.savetodisk"), "", filename, new FileNameExtensionFilter("Image files", ".jpg", ".png"));
        if (file != null) {
          try (FileOutputStream os = new FileOutputStream(file.toFile())) {
            IOUtils.write(originalImageBytes, os);
          }
        }
      }
      catch (Exception ex) {
        LOGGER.error("Could not save image file: {}", ex.getMessage());
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, "", "message.erroropenfile", new String[] { ":", ex.getLocalizedMessage() }));
      }
    }
  }
}
