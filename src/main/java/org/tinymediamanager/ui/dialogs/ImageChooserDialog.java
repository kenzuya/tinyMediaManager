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

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.ImageSizeAndUrl;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.interfaces.IMediaArtworkProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.WrapLayout;
import org.tinymediamanager.ui.components.EnhancedTextField;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.SquareIconButton;

import net.miginfocom.swing.MigLayout;

/**
 * The Class ImageChooser. Let the user choose the right image for the media entity
 * 
 * @author Manuel Laggner
 */
public class ImageChooserDialog extends TmmDialog {
  private static final long         serialVersionUID = 8193355920006275933L;
  private static final Logger       LOGGER           = LoggerFactory.getLogger(ImageChooserDialog.class);
  private static final String       DIALOG_ID        = "imageChooser";

  private final Map<String, Object> ids;
  private final MediaArtworkType    type;
  private final MediaType           mediaType;
  private final ImageLabel          imageLabel;
  private final List<MediaScraper>  artworkScrapers;

  private final ButtonGroup         buttonGroup      = new ButtonGroup();
  private final List<JToggleButton> buttons          = new ArrayList<>();

  private JProgressBar              progressBar;
  private JLabel                    lblProgressAction;
  private JPanel                    panelImages;
  private LockableViewPort          viewport;
  private JTextField                tfImageUrl;

  private String                    openFolderPath   = null;
  private List<String>              extraThumbs      = null;
  private List<String>              extraFanarts     = null;
  private DownloadTask              task;

  private JLabel                    labelThumbs;
  private JButton                   btnMarkExtrathumbs;
  private JButton                   btnUnMarkExtrathumbs;

  private JLabel                    labelFanart;
  private JButton                   btnMarkExtrafanart;
  private JButton                   btnUnMarkExtrafanart;

  /**
   * Instantiates a new image chooser dialog.
   *
   * @param parent
   *          the parent of this dialog
   * @param ids
   *          the ids
   * @param type
   *          the type
   * @param artworkScrapers
   *          the artwork providers
   * @param imageLabel
   *          the image label
   * @param mediaType
   *          the media for for which artwork has to be chosen
   */
  public ImageChooserDialog(JDialog parent, final Map<String, Object> ids, MediaArtworkType type, List<MediaScraper> artworkScrapers,
      ImageLabel imageLabel, MediaType mediaType) {

    super(parent, "", DIALOG_ID);
    this.imageLabel = imageLabel;
    this.type = type;
    this.mediaType = mediaType;
    this.ids = ids;
    this.artworkScrapers = artworkScrapers;
    init();
  }

  private void init() {
    switch (type) {
      case BACKGROUND:
        setTitle(TmmResourceBundle.getString("image.choose.fanart"));
        break;

      case POSTER:
        setTitle(TmmResourceBundle.getString("image.choose.poster"));
        break;

      case BANNER:
        setTitle(TmmResourceBundle.getString("image.choose.banner"));
        break;

      case SEASON_POSTER:
        Object season = ids.get("tvShowSeason");
        if (season != null) {
          setTitle(TmmResourceBundle.getString("image.choose.season") + " - " + TmmResourceBundle.getString("metatag.season") + " " + season);
        }
        else {
          setTitle(TmmResourceBundle.getString("image.choose.season"));
        }
        break;

      case SEASON_BANNER:
        season = ids.get("tvShowSeason");
        if (season != null) {
          setTitle(TmmResourceBundle.getString("image.choose.season.banner") + " - " + TmmResourceBundle.getString("metatag.season") + " " + season);
        }
        else {
          setTitle(TmmResourceBundle.getString("image.choose.season.banner"));
        }
        break;

      case SEASON_THUMB:
        season = ids.get("tvShowSeason");
        if (season != null) {
          setTitle(TmmResourceBundle.getString("image.choose.season.thumb") + " - " + TmmResourceBundle.getString("metatag.season") + " " + season);
        }
        else {
          setTitle(TmmResourceBundle.getString("image.choose.season.thumb"));
        }
        break;

      case CLEARART:
        setTitle(TmmResourceBundle.getString("image.choose.clearart"));
        break;

      case DISC:
        setTitle(TmmResourceBundle.getString("image.choose.disc"));
        break;

      case LOGO:
        setTitle(TmmResourceBundle.getString("image.choose.logo"));
        break;

      case CLEARLOGO:
        setTitle(TmmResourceBundle.getString("image.choose.clearlogo"));
        break;

      case CHARACTERART:
        setTitle(TmmResourceBundle.getString("image.choose.characterart"));
        break;

      case THUMB:
        setTitle(TmmResourceBundle.getString("image.choose.thumb"));
        break;

      case KEYART:
        setTitle(TmmResourceBundle.getString("image.choose.keyart"));
        break;
    }

    /* UI components */
    JPanel contentPanel = new JPanel();
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new MigLayout("hidemode 1", "[850lp,grow][]", "[500lp,grow][shrink 0][][][]"));
    {
      JScrollPane scrollPane = new NoBorderScrollPane();
      viewport = new LockableViewPort();
      scrollPane.setViewport(viewport);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      contentPanel.add(scrollPane, "cell 0 0 2 1,grow");
      {
        panelImages = new JPanel();
        viewport.setView(panelImages);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panelImages.setLayout(new WrapLayout(FlowLayout.LEFT));
      }
    }
    {
      JSeparator separator = new JSeparator();
      contentPanel.add(separator, "cell 0 1 2 1,growx");
    }
    {
      tfImageUrl = new EnhancedTextField(TmmResourceBundle.getString("image.inserturl"));
      contentPanel.add(tfImageUrl, "cell 0 2,growx");
      tfImageUrl.setColumns(10);
      JButton btnAddImage = new JButton(TmmResourceBundle.getString("image.downloadimage"));
      btnAddImage.addActionListener(e -> {
        if (StringUtils.isNotBlank(tfImageUrl.getText())) {
          downloadAndPreviewImage(tfImageUrl.getText());
        }
      });
      contentPanel.add(btnAddImage, "cell 1 2");

    }

    // add buttons to select/deselect all extrafanarts/extrathumbs
    if (type == BACKGROUND) {
      labelThumbs = new JLabel("Extrathumbs:");
      contentPanel.add(labelThumbs, "flowx,cell 0 3");
      labelThumbs.setVisible(false);

      btnMarkExtrathumbs = new SquareIconButton(IconManager.CHECK_ALL);
      contentPanel.add(btnMarkExtrathumbs, "cell 0 3");
      btnMarkExtrathumbs.setVisible(false);
      btnMarkExtrathumbs.setToolTipText(TmmResourceBundle.getString("image.extrathumbs.markall"));
      btnMarkExtrathumbs.addActionListener(arg0 -> {
        for (JToggleButton button : buttons) {
          if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox) {
            JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrathumb");
            chkbx.setSelected(true);
          }
        }
      });

      btnUnMarkExtrathumbs = new SquareIconButton(IconManager.CLEAR_ALL);
      contentPanel.add(btnUnMarkExtrathumbs, "cell 0 3");
      btnUnMarkExtrathumbs.setVisible(false);
      btnUnMarkExtrathumbs.setToolTipText(TmmResourceBundle.getString("image.extrathumbs.unmarkall"));
      btnUnMarkExtrathumbs.addActionListener(arg0 -> {
        for (JToggleButton button : buttons) {
          if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox) {
            JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrathumb");
            chkbx.setSelected(false);
          }
        }
      });

      labelFanart = new JLabel("Extrafanart:");
      contentPanel.add(labelFanart, "flowx,cell 0 4");
      labelFanart.setVisible(false);

      btnMarkExtrafanart = new SquareIconButton(IconManager.CHECK_ALL);
      contentPanel.add(btnMarkExtrafanart, "cell 0 4");
      btnMarkExtrafanart.setVisible(false);
      btnMarkExtrafanart.setToolTipText(TmmResourceBundle.getString("image.extrafanart.markall"));
      btnMarkExtrafanart.addActionListener(arg0 -> {
        for (JToggleButton button : buttons) {
          if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox) {
            JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrafanart");
            chkbx.setSelected(true);
          }
        }
      });

      btnUnMarkExtrafanart = new SquareIconButton(IconManager.CLEAR_ALL);
      contentPanel.add(btnUnMarkExtrafanart, "cell 0 4");
      btnUnMarkExtrafanart.setVisible(false);
      btnUnMarkExtrafanart.setToolTipText(TmmResourceBundle.getString("image.extrafanart.unmarkall"));
      btnUnMarkExtrafanart.addActionListener(arg0 -> {
        for (JToggleButton button : buttons) {
          if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox) {
            JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrafanart");
            chkbx.setSelected(false);
          }
        }
      });
    }

    {
      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new MigLayout("", "[][grow]", "[]"));

      progressBar = new JProgressBar();
      infoPanel.add(progressBar, "cell 0 0");

      lblProgressAction = new JLabel("");
      infoPanel.add(lblProgressAction, "cell 1 0");

      setBottomInformationPanel(infoPanel);
    }
    {
      JButton cancelButton = new JButton(TmmResourceBundle.getString("Button.cancel"));
      Action actionCancel = new CancelAction();
      cancelButton.setAction(actionCancel);
      cancelButton.setActionCommand("Cancel");
      addButton(cancelButton);

      JButton btnAddFile = new JButton(TmmResourceBundle.getString("Button.addfile"));
      Action actionLocalFile = new LocalFileChooseAction();
      btnAddFile.setAction(actionLocalFile);
      addButton(btnAddFile);

      JButton okButton = new JButton(TmmResourceBundle.getString("Button.ok"));
      Action actionOK = new OkAction();
      okButton.setAction(actionOK);
      okButton.setActionCommand("OK");
      addDefaultButton(okButton);
    }

    task = new DownloadTask(ids, artworkScrapers);
    task.execute();
  }

  public void bindExtraThumbs(List<String> extraThumbs) {
    if (type != BACKGROUND) {
      return;
    }

    this.extraThumbs = extraThumbs;

    if (extraThumbs != null) {
      labelThumbs.setVisible(true);
      btnMarkExtrathumbs.setVisible(true);
      btnUnMarkExtrathumbs.setVisible(true);
    }
    else {
      labelThumbs.setVisible(false);
      btnMarkExtrathumbs.setVisible(false);
      btnUnMarkExtrathumbs.setVisible(false);
    }
  }

  public void bindExtraFanarts(List<String> extraFanarts) {
    if (type != BACKGROUND) {
      return;
    }

    this.extraFanarts = extraFanarts;

    if (extraFanarts != null) {
      labelFanart.setVisible(true);
      btnMarkExtrafanart.setVisible(true);
      btnUnMarkExtrafanart.setVisible(true);
    }
    else {
      labelFanart.setVisible(false);
      btnMarkExtrafanart.setVisible(false);
      btnUnMarkExtrafanart.setVisible(false);
    }
  }

  public void setOpenFolderPath(String openFolderPath) {
    this.openFolderPath = openFolderPath;
  }

  private void startProgressBar(String description) {
    lblProgressAction.setText(description);
    progressBar.setVisible(true);
    progressBar.setIndeterminate(true);
  }

  private void stopProgressBar() {
    lblProgressAction.setText("");
    progressBar.setVisible(false);
    progressBar.setIndeterminate(false);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void addImage(BufferedImage originalImage, final MediaArtwork artwork) {
    Point size = null;

    GridBagLayout gbl = new GridBagLayout();

    switch (type) {
      case BACKGROUND:
      case CLEARART:
      case THUMB:
      case DISC:
      case CHARACTERART:
        size = ImageUtils.calculateSize(300, 150, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

      case BANNER:
      case LOGO:
      case CLEARLOGO:
        size = ImageUtils.calculateSize(300, 100, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

      case POSTER:
      case KEYART:
      default:
        size = ImageUtils.calculateSize(150, 250, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

    }

    gbl.columnWeights = new double[] { Double.MIN_VALUE };
    gbl.rowWeights = new double[] { Double.MIN_VALUE };
    JPanel imagePanel = new JPanel();
    imagePanel.setLayout(gbl);

    int row = 0;

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    JToggleButton button = new JToggleButton();
    button.setBackground(Color.white);
    button.setMargin(new Insets(10, 10, 10, 10));
    if (artwork.isAnimated()) {
      button.setText("<html><img width=\"" + size.x + "\" height=\"" + size.y + "\" src='" + artwork.getPreviewUrl() + "'/></html>");
      button.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
    }
    else {
      ImageIcon imageIcon = new ImageIcon(
          Scalr.resize(originalImage, Scalr.Method.BALANCED, Scalr.Mode.AUTOMATIC, size.x, size.y, Scalr.OP_ANTIALIAS));
      button.setIcon(imageIcon);
    }
    button.putClientProperty("MediaArtwork", artwork);

    buttonGroup.add(button);
    buttons.add(button);
    imagePanel.add(button, gbc);

    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = ++row;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.LAST_LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 5, 0, 5);

    JComboBox cb = null;
    if (!artwork.getImageSizes().isEmpty()) {
      cb = new JComboBox(artwork.getImageSizes().toArray());
    }
    else {
      cb = new JComboBox(new String[] { originalImage.getWidth() + "x" + originalImage.getHeight() });
    }
    button.putClientProperty("MediaArtworkSize", cb);
    imagePanel.add(cb, gbc);

    /* show image button */
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = ++row;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 3;
    gbc.insets = new Insets(0, 5, 0, 5);

    LinkLabel lblShowImage = new LinkLabel(TmmResourceBundle.getString("image.showoriginal"));
    lblShowImage.addActionListener(e -> {
      ImagePreviewDialog dialog = new ImagePreviewDialog(artwork.getOriginalUrl());
      dialog.setVisible(true);
    });
    imagePanel.add(lblShowImage, gbc);

    // should we provide an option for extrathumbs
    if (type == BACKGROUND && extraThumbs != null) {
      gbc = new GridBagConstraints();
      gbc.gridx = 1;
      gbc.gridy = row;
      gbc.anchor = GridBagConstraints.LINE_END;
      gbc.insets = new Insets(0, 5, 0, 5);
      JLabel label = new JLabel("Extrathumb");
      imagePanel.add(label, gbc);

      gbc = new GridBagConstraints();
      gbc.gridx = 2;
      gbc.gridy = row;
      gbc.anchor = GridBagConstraints.LINE_START;
      gbc.insets = new Insets(0, 5, 0, 5);
      JCheckBox chkbx = new JCheckBox();
      button.putClientProperty("MediaArtworkExtrathumb", chkbx);
      imagePanel.add(chkbx, gbc);
    }

    // should we provide an option for extrafanart
    if (type == BACKGROUND && extraFanarts != null) {
      gbc = new GridBagConstraints();
      gbc.gridx = 1;
      gbc.gridy = ++row;
      gbc.anchor = GridBagConstraints.LINE_END;
      gbc.insets = new Insets(0, 5, 0, 5);
      JLabel label = new JLabel("Extrafanart");
      imagePanel.add(label, gbc);

      gbc = new GridBagConstraints();
      gbc.gridx = 2;
      gbc.gridy = row;
      gbc.anchor = GridBagConstraints.LINE_START;
      gbc.insets = new Insets(0, 5, 0, 5);
      JCheckBox chkbx = new JCheckBox();
      button.putClientProperty("MediaArtworkExtrafanart", chkbx);
      imagePanel.add(chkbx, gbc);
    }

    panelImages.add(imagePanel);

    viewport.setLocked(true);
    ImageChooserDialog.this.revalidate();
    SwingUtilities.invokeLater(() -> viewport.setLocked(false));
  }

  private void downloadAndPreviewImage(String url) {
    Runnable task = () -> {
      try {
        final MediaArtwork art;
        switch (type) {
          case BANNER:
            art = new MediaArtwork("", MediaArtworkType.BANNER);
            break;

          case CLEARART:
            art = new MediaArtwork("", MediaArtworkType.CLEARART);
            break;

          case DISC:
            art = new MediaArtwork("", MediaArtworkType.DISC);
            break;

          case BACKGROUND:
            art = new MediaArtwork("", BACKGROUND);
            break;

          case LOGO:
            art = new MediaArtwork("", MediaArtworkType.LOGO);
            break;

          case CLEARLOGO:
            art = new MediaArtwork("", MediaArtworkType.CLEARLOGO);
            break;

          case CHARACTERART:
            art = new MediaArtwork("", MediaArtworkType.CHARACTERART);
            break;

          case POSTER:
            art = new MediaArtwork("", MediaArtworkType.POSTER);
            break;

          case SEASON_POSTER:
            art = new MediaArtwork("", MediaArtworkType.SEASON_POSTER);
            break;

          case SEASON_BANNER:
            art = new MediaArtwork("", MediaArtworkType.SEASON_BANNER);
            break;

          case SEASON_THUMB:
            art = new MediaArtwork("", MediaArtworkType.SEASON_THUMB);
            break;

          case THUMB:
            art = new MediaArtwork("", MediaArtworkType.THUMB);
            break;

          case KEYART:
            art = new MediaArtwork("", MediaArtworkType.KEYART);
            break;

          default:
            return;
        }
        art.setDefaultUrl(url);
        art.setPreviewUrl(url);
        art.setOriginalUrl(url);

        Url url1 = new Url(art.getPreviewUrl());
        final BufferedImage bufferedImage = ImageUtils.createImage(url1.getBytesWithRetry(5));

        SwingUtilities.invokeLater(() -> {
          addImage(bufferedImage, art);
          bufferedImage.flush();
        });
        tfImageUrl.setText("");
      }
      catch (Exception e) {
        LOGGER.error("could not download manually entered image url: {} - {}", tfImageUrl.getText(), e.getMessage());
      }
    };
    task.run();
  }

  /**
   * call a new image chooser dialog without extrathumbs and extrafanart usage.<br />
   * this method also checks if there are valid IDs for scraping
   *
   * @param parent
   *          the parent of this dialog
   * @param ids
   *          the ids
   * @param type
   *          the type
   * @param artworkScrapers
   *          the artwork providers
   * @param mediaType
   *          the media for for which artwork has to be chosen
   * @param defaultPath
   *          the default path to open
   */
  public static String chooseImage(JDialog parent, final Map<String, Object> ids, MediaArtworkType type, List<MediaScraper> artworkScrapers,
      MediaType mediaType, String defaultPath) {
    return chooseImage(parent, ids, type, artworkScrapers, null, null, mediaType, defaultPath);
  }

  /**
   * call a new image chooser dialog with extrathumbs and extrafanart usage.<br />
   * this method also checks if there are valid IDs for scraping
   *
   * @param parent
   *          the parent of this dialog
   * @param ids
   *          the ids
   * @param type
   *          the type
   * @param artworkScrapers
   *          the artwork providers
   * @param extraThumbs
   *          the extra thumbs
   * @param extraFanarts
   *          the extra fanarts
   * @param mediaType
   *          the media for for which artwork has to be chosen
   * @param defaultPath
   *          the default path to open
   */
  public static String chooseImage(JDialog parent, final Map<String, Object> ids, MediaArtworkType type, List<MediaScraper> artworkScrapers,
      List<String> extraThumbs, List<String> extraFanarts, MediaType mediaType, String defaultPath) {
    if (ids.isEmpty()) {
      return "";
    }

    ImageLabel lblImage = new ImageLabel();
    ImageChooserDialog dialog = new ImageChooserDialog(parent, ids, type, artworkScrapers, lblImage, mediaType);

    dialog.bindExtraThumbs(extraThumbs);
    dialog.bindExtraFanarts(extraFanarts);
    dialog.setOpenFolderPath(defaultPath);

    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);
    return lblImage.getImageUrl();
  }

  private class OkAction extends AbstractAction {
    private static final long serialVersionUID = -1255049344169945137L;

    public OkAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.ok"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("image.seteselected"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaArtwork artwork = null;
      ImageSizeAndUrl resolution = null;

      // get selected button
      for (JToggleButton button : buttons) {
        if (button.isSelected()) {
          Object clientProperty = button.getClientProperty("MediaArtwork");
          if (clientProperty instanceof MediaArtwork) {
            artwork = (MediaArtwork) clientProperty;
            clientProperty = button.getClientProperty("MediaArtworkSize");
            // try to get the size
            if (clientProperty instanceof JComboBox) {
              @SuppressWarnings("rawtypes")
              JComboBox cb = (JComboBox) clientProperty;
              if (cb.getSelectedItem() instanceof ImageSizeAndUrl) {
                resolution = (ImageSizeAndUrl) cb.getSelectedItem();
              }
            }
            break;
          }
        }
      }

      // nothing selected
      if (artwork == null) {
        JOptionPane.showMessageDialog(null, TmmResourceBundle.getString("image.noneselected"));
        return;
      }

      if (artwork != null) {
        imageLabel.clearImage();
        if (resolution != null) {
          imageLabel.setImageUrl(resolution.getUrl());
        }
        else {
          imageLabel.setImageUrl(artwork.getDefaultUrl());
        }
      }

      // extrathumbs
      if (type == BACKGROUND && extraThumbs != null) {
        processExtraThumbs();
      }

      // extrafanart
      if (type == BACKGROUND && extraFanarts != null) {
        processExtraFanart();
      }

      task.cancel(true);
      setVisible(false);
    }

    /**
     * Process extra thumbs.
     */
    private void processExtraThumbs() {
      extraThumbs.clear();
      // get extrathumbs
      for (JToggleButton button : buttons) {
        if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox
            && button.getClientProperty("MediaArtwork") instanceof MediaArtwork
            && button.getClientProperty("MediaArtworkSize") instanceof JComboBox) {
          JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrathumb");
          if (chkbx.isSelected()) {
            MediaArtwork artwork = (MediaArtwork) button.getClientProperty("MediaArtwork");
            @SuppressWarnings("rawtypes")
            JComboBox cb = (JComboBox) button.getClientProperty("MediaArtworkSize");
            if (cb.getSelectedItem() instanceof ImageSizeAndUrl) {
              ImageSizeAndUrl size = (ImageSizeAndUrl) cb.getSelectedItem();
              if (size != null) {
                extraThumbs.add(size.getUrl());
              }
              else {
                extraThumbs.add(artwork.getDefaultUrl());
              }
            }
            else if (cb.getSelectedItem() instanceof String) {
              extraThumbs.add(artwork.getDefaultUrl());
            }
          }
        }
      }
    }

    /**
     * Process extra fanart.
     */
    private void processExtraFanart() {
      extraFanarts.clear();
      // get extrafanart
      for (JToggleButton button : buttons) {
        if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox
            && button.getClientProperty("MediaArtwork") instanceof MediaArtwork
            && button.getClientProperty("MediaArtworkSize") instanceof JComboBox) {
          JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrafanart");
          if (chkbx.isSelected()) {
            MediaArtwork artwork = (MediaArtwork) button.getClientProperty("MediaArtwork");
            @SuppressWarnings("rawtypes")
            JComboBox cb = (JComboBox) button.getClientProperty("MediaArtworkSize");
            if (cb.getSelectedItem() instanceof ImageSizeAndUrl) {
              ImageSizeAndUrl size = (ImageSizeAndUrl) cb.getSelectedItem();
              if (size != null) {
                extraFanarts.add(size.getUrl());
              }
              else {
                extraFanarts.add(artwork.getDefaultUrl());
              }
            }
            else if (cb.getSelectedItem() instanceof String) {
              extraFanarts.add(artwork.getDefaultUrl());
            }
          }
        }
      }
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = 403327079655572423L;

    public CancelAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.cancel"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("Button.cancel"));
      putValue(SMALL_ICON, IconManager.CANCEL_INV);
      putValue(LARGE_ICON_KEY, IconManager.CANCEL_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      task.cancel(true);
      setVisible(false);
    }
  }

  private class DownloadTask extends SwingWorker<Void, DownloadChunk> {
    private final Map<String, Object> ids;
    private final List<MediaScraper>  artworkScrapers;
    private boolean                   imagesFound = false;

    public DownloadTask(Map<String, Object> ids, List<MediaScraper> artworkScrapers) {
      this.ids = ids;
      this.artworkScrapers = artworkScrapers;
    }

    @Override
    public Void doInBackground() {
      if (ids.isEmpty()) {
        JOptionPane.showMessageDialog(null, TmmResourceBundle.getString("image.download.noid"));
        return null;
      }

      SwingUtilities.invokeLater(() -> {
        startProgressBar(TmmResourceBundle.getString("image.download.progress"));
      });

      if (artworkScrapers == null || artworkScrapers.isEmpty()) {
        return null;
      }

      // open a thread pool to parallel download the images
      ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 10, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
      pool.allowCoreThreadTimeOut(true);
      ExecutorCompletionService<DownloadChunk> service = new ExecutorCompletionService<>(pool);

      // get images from all artwork providers
      for (MediaScraper scraper : artworkScrapers) {
        try {
          IMediaArtworkProvider artworkProvider = (IMediaArtworkProvider) scraper.getMediaProvider();

          ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(mediaType);
          if (mediaType == MediaType.MOVIE || mediaType == MediaType.MOVIE_SET) {
            options.setLanguage(MovieModuleManager.SETTINGS.getImageScraperLanguage());
            options.setFanartSize(MovieModuleManager.SETTINGS.getImageFanartSize());
            options.setPosterSize(MovieModuleManager.SETTINGS.getImagePosterSize());
          }
          else if (mediaType == MediaType.TV_SHOW || mediaType == MediaType.TV_EPISODE) {
            options.setLanguage(TvShowModuleManager.SETTINGS.getScraperLanguage());
          }
          else {
            continue;
          }
          switch (type) {
            case POSTER:
              options.setArtworkType(MediaArtworkType.POSTER);
              break;

            case BACKGROUND:
              options.setArtworkType(BACKGROUND);
              break;

            case BANNER:
              options.setArtworkType(MediaArtworkType.BANNER);
              break;

            case SEASON_POSTER:
              options.setArtworkType(MediaArtworkType.SEASON_POSTER);
              break;

            case SEASON_BANNER:
              options.setArtworkType(MediaArtworkType.SEASON_BANNER);
              break;

            case SEASON_THUMB:
              options.setArtworkType(MediaArtworkType.SEASON_THUMB);
              break;

            case CLEARART:
              options.setArtworkType(MediaArtworkType.CLEARART);
              break;

            case DISC:
              options.setArtworkType(MediaArtworkType.DISC);
              break;

            case LOGO:
              options.setArtworkType(MediaArtworkType.LOGO);
              break;

            case CLEARLOGO:
              options.setArtworkType(MediaArtworkType.CLEARLOGO);
              break;

            case CHARACTERART:
              options.setArtworkType(MediaArtworkType.CHARACTERART);
              break;

            case KEYART:
              options.setArtworkType(MediaArtworkType.KEYART);
              break;

            case THUMB:
              options.setArtworkType(MediaArtworkType.THUMB);
              break;
          }

          // populate ids
          options.setIds(ids);

          // get the artwork
          List<MediaArtwork> artwork = artworkProvider.getArtwork(options);
          if (artwork == null || artwork.isEmpty()) {
            continue;
          }

          // display all images
          for (MediaArtwork art : artwork) {
            if (isCancelled()) {
              return null;
            }
            if (art.getPreviewUrl().isEmpty()) {
              continue;
            }

            Callable<DownloadChunk> callable = () -> {
              Url url = new Url(art.getPreviewUrl());
              DownloadChunk chunk = new DownloadChunk();
              chunk.artwork = art;
              try {
                chunk.image = ImageUtils.createImage(url.getBytesWithRetry(5));
              }
              catch (Exception e) {
                // ignore, return empty chunk
              }
              return chunk;
            };

            service.submit(callable);
          }
        }

        catch (ScrapeException e) {
          LOGGER.error("getArtwork", e);
        }
        catch (MissingIdException e) {
          LOGGER.debug("could not fetch artwork: {}", e.getIds());
        }
        catch (Exception e) {
          if (e instanceof InterruptedException || e instanceof InterruptedIOException) { // NOSONAR
            // shutdown the pool
            pool.getQueue().clear();
            pool.shutdownNow();

            return null;
          }
          LOGGER.error("could not process artwork downloading - {}", e.getMessage());
        }
      } // end foreach scraper

      // wait for all downloads to finish
      pool.shutdown();
      while (true) {
        try {
          final Future<DownloadChunk> future = service.poll(1, TimeUnit.SECONDS);
          if (future != null) {
            DownloadChunk dc = future.get();
            if (dc.image != null) {
              publish(dc);
              imagesFound = true;
            }
          }
          else if (pool.isTerminated()) {
            // no result got and the pool is terminated -> we're finished
            break;
          }
        }
        catch (InterruptedException e) { // NOSONAR
          return null;
        }
        catch (ExecutionException e) {
          LOGGER.error("ThreadPool imageChooser: Error getting result! - {}", e.getMessage());
        }
      }

      return null;
    }

    @Override
    protected void process(List<DownloadChunk> chunks) {
      for (DownloadChunk chunk : chunks) {
        addImage(chunk.image, chunk.artwork);
      }
    }

    @Override
    public void done() {
      if (!imagesFound) {
        JLabel lblNothingFound = new JLabel(TmmResourceBundle.getString("image.download.nothingfound"));
        TmmFontHelper.changeFont(lblNothingFound, 1.33);
        panelImages.add(lblNothingFound);
        panelImages.validate();
        panelImages.getParent().validate();
      }
      SwingUtilities.invokeLater(ImageChooserDialog.this::stopProgressBar);
    }
  }

  private static class DownloadChunk {
    private BufferedImage image;
    private MediaArtwork  artwork;
  }

  private class LocalFileChooseAction extends AbstractAction {
    private static final long serialVersionUID = -1178325861474276709L;

    public LocalFileChooseAction() {
      putValue(NAME, TmmResourceBundle.getString("image.choose.file"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("image.choose.file"));
      putValue(SMALL_ICON, IconManager.FILE_OPEN_INV);
      putValue(LARGE_ICON_KEY, IconManager.FILE_OPEN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String path;
      if (StringUtils.isNotBlank(openFolderPath)) {
        path = openFolderPath;
      }
      else {
        path = TmmProperties.getInstance().getProperty(DIALOG_ID + ".path");
      }

      Path file = TmmUIHelper.selectFile(TmmResourceBundle.getString("image.choose"), path,
          new FileNameExtensionFilter("Image files", ".jpg", ".jpeg", ".png", ".bmp", ".gif", ".tbn"));
      if (file != null && Utils.isRegularFile(file)) {
        String fileName = file.toAbsolutePath().toString();
        imageLabel.clearImage();
        imageLabel.setImageUrl("file:/" + fileName);
        task.cancel(true);
        TmmProperties.getInstance().putProperty(DIALOG_ID + ".path", file.getParent().toString());
        setVisible(false);
      }
    }
  }

  private static final class LockableViewPort extends JViewport {

    private boolean locked = false;

    @Override
    public void setViewPosition(Point p) {
      if (locked) {
        return;
      }
      super.setViewPosition(p);
    }

    public boolean isLocked() {
      return locked;
    }

    public void setLocked(boolean locked) {
      this.locked = locked;
    }
  }
}
