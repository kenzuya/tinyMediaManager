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
package org.tinymediamanager.ui.tvshows.dialogs;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CHARACTERART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.KEYART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.LOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;
import static org.tinymediamanager.ui.TmmUIHelper.createLinkForImage;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.MediaAiredStatus;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.ShadowLayerUI;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.TableSpinnerEditor;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.MediaIdTable;
import org.tinymediamanager.ui.components.MediaIdTable.MediaId;
import org.tinymediamanager.ui.components.MediaRatingTable;
import org.tinymediamanager.ui.components.PersonTable;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.combobox.AutoCompleteSupport;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.components.datepicker.DatePicker;
import org.tinymediamanager.ui.components.datepicker.YearSpinner;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.IdEditorDialog;
import org.tinymediamanager.ui.dialogs.ImageChooserDialog;
import org.tinymediamanager.ui.dialogs.PersonEditorDialog;
import org.tinymediamanager.ui.dialogs.RatingEditorDialog;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.renderer.LeftDotTableCellRenderer;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowEditor.
 * 
 * @author Manuel Laggner
 */
public class TvShowEditorDialog extends TmmDialog {
  private static final long                        serialVersionUID    = 3270218410302989845L;
  private static final String                      ORIGINAL_IMAGE_SIZE = "originalImageSize";

  private final TvShow                             tvShowToEdit;
  private final TvShowList                         tvShowList          = TvShowModuleManager.getInstance().getTvShowList();
  private final EventList<Person>                  actors;
  private final List<MediaGenres>                  genres              = ObservableCollections.observableList(new ArrayList<>());
  private final EventList<MediaId>                 ids;
  private final EventList<MediaRatingTable.Rating> ratings;
  private final List<String>                       tags                = ObservableCollections.observableList(new ArrayList<>());
  private final EventList<EpisodeEditorContainer>  episodes;
  private final List<MediaTrailer>                 trailers            = ObservableCollections.observableList(new ArrayList<>());
  private final int                                queueIndex;
  private final int                                queueSize;

  private List<String>                             extrafanarts        = null;
  private boolean                                  continueQueue       = true;
  private boolean                                  navigateBack        = false;

  /**
   * UI elements
   */
  private JTextField                               tfTitle;
  private YearSpinner                              spYear;
  private JTextArea                                taPlot;
  private TmmTable                                 tableActors;
  private ImageLabel                               lblPoster;
  private ImageLabel                               lblFanart;
  private ImageLabel                               lblBanner;
  private JSpinner                                 spRuntime;
  private JTextField                               tfStudio;
  private JList<MediaGenres>                       listGenres;
  private AutocompleteComboBox<MediaGenres>        cbGenres;
  private AutoCompleteSupport<MediaGenres>         cbGenresAutoCompleteSupport;
  private JSpinner                                 spRating;
  private JComboBox<MediaCertification>            cbCertification;
  private JComboBox<MediaAiredStatus>              cbStatus;

  private AutocompleteComboBox<String>             cbTags;
  private AutoCompleteSupport<String>              cbTagsAutoCompleteSupport;
  private JList<String>                            listTags;
  private JSpinner                                 spDateAdded;
  private DatePicker                               dpPremiered;
  private TmmTable                                 tableEpisodes;
  private JTextField                               tfSorttitle;
  private JTextArea                                taNote;

  private JTextField                               tfPoster;
  private JTextField                               tfFanart;
  private JTextField                               tfLogo;
  private JTextField                               tfClearLogo;
  private JTextField                               tfBanner;
  private JTextField                               tfClearArt;
  private JTextField                               tfThumb;

  private ImageLabel                               lblLogo;
  private ImageLabel                               lblClearlogo;
  private ImageLabel                               lblClearart;
  private ImageLabel                               lblThumb;
  private ImageLabel                               lblCharacterart;
  private ImageLabel                               lblKeyart;

  private TmmTable                                 tableIds;
  private TmmTable                                 tableRatings;
  private JTextField                               tfOriginalTitle;
  private JTextField                               tfCountry;
  private JTextField                               tfCharacterart;
  private JTextField                               tfKeyart;

  private TmmTable                                 tableTrailer;

  /**
   * Instantiates a new tv show editor dialog.
   *
   * @param tvShow
   *          the tv show
   * @param queueIndex
   *          the actual index in the queue
   * @param queueSize
   *          the queue size
   */
  public TvShowEditorDialog(TvShow tvShow, int queueIndex, int queueSize) {
    super(TmmResourceBundle.getString("tvshow.edit") + (queueSize > 1 ? " " + (queueIndex + 1) + "/" + queueSize : "") + "  < " + tvShow.getPathNIO()
        + " >", "tvShowEditor");

    this.tvShowToEdit = tvShow;
    this.queueIndex = queueIndex;
    this.queueSize = queueSize;
    ids = MediaIdTable.convertIdMapToEventList(tvShowToEdit.getIds());
    ratings = MediaRatingTable.convertRatingMapToEventList(tvShowToEdit.getRatings(), false);
    MediaRating userMediaRating = tvShowToEdit.getRating(MediaRating.USER);

    // creation of lists
    actors = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(Person.class));
    episodes = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()),
        GlazedLists.beanConnector(EpisodeEditorContainer.class));

    initComponents();
    bindingGroup = initDataBindings();

    {
      tfTitle.setText(tvShow.getTitle());
      tfOriginalTitle.setText(tvShow.getOriginalTitle());
      tfSorttitle.setText(tvShow.getSortTitle());
      taPlot.setText(tvShow.getPlot());
      lblPoster.setImagePath(tvShow.getArtworkFilename(MediaFileType.POSTER));
      lblFanart.setImagePath(tvShow.getArtworkFilename(MediaFileType.FANART));
      lblLogo.setImagePath(tvShow.getArtworkFilename(MediaFileType.LOGO));
      lblClearlogo.setImagePath(tvShow.getArtworkFilename(MediaFileType.CLEARLOGO));
      lblClearart.setImagePath(tvShow.getArtworkFilename(MediaFileType.CLEARART));
      lblThumb.setImagePath(tvShow.getArtworkFilename(MediaFileType.THUMB));
      lblBanner.setImagePath(tvShow.getArtworkFilename(MediaFileType.BANNER));
      lblCharacterart.setImagePath(tvShow.getArtworkFilename(MediaFileType.CHARACTERART));
      lblKeyart.setImagePath(tvShow.getArtworkFilename(MediaFileType.KEYART));
      tfPoster.setText(tvShow.getArtworkUrl(MediaFileType.POSTER));
      tfFanart.setText(tvShow.getArtworkUrl(MediaFileType.FANART));
      tfLogo.setText(tvShow.getArtworkUrl(MediaFileType.LOGO));
      tfClearLogo.setText(tvShow.getArtworkUrl(MediaFileType.CLEARLOGO));
      tfClearArt.setText(tvShow.getArtworkUrl(MediaFileType.CLEARART));
      tfThumb.setText(tvShow.getArtworkUrl(MediaFileType.THUMB));
      tfBanner.setText(tvShow.getArtworkUrl(MediaFileType.BANNER));
      tfCharacterart.setText(tvShow.getArtworkUrl(MediaFileType.CHARACTERART));
      tfKeyart.setText(tvShow.getArtworkUrl(MediaFileType.KEYART));

      tfStudio.setText(tvShow.getProductionCompany());
      tfCountry.setText(tvShow.getCountry());
      taNote.setText(tvShow.getNote());
      cbStatus.setSelectedItem(tvShow.getStatus());
      spRuntime.setValue(tvShow.getRuntime());
      int year = tvShow.getYear();
      spYear.setValue(year);
      spDateAdded.setValue(tvShow.getDateAdded());
      spRating.setModel(new SpinnerNumberModel(userMediaRating.getRating(), 0.0, 10.0, 0.1));

      for (Person origCast : tvShow.getActors()) {
        actors.add(new Person(origCast));
      }

      genres.addAll(tvShow.getGenres());
      tags.addAll(tvShowToEdit.getTags());
      if (TvShowModuleManager.getInstance().getSettings().isImageExtraFanart()) {
        extrafanarts = new ArrayList<>(tvShowToEdit.getExtraFanartUrls());
      }

      List<MediaCertification> availableCertifications = MediaCertification
          .getCertificationsforCountry(TvShowModuleManager.getInstance().getSettings().getCertificationCountry());
      if (!availableCertifications.contains(tvShowToEdit.getCertification())) {
        availableCertifications.add(0, tvShowToEdit.getCertification());
      }
      for (MediaCertification cert : availableCertifications) {
        cbCertification.addItem(cert);
      }
      cbCertification.setSelectedItem(tvShowToEdit.getCertification());

      List<TvShowEpisode> epl = new ArrayList<>(tvShowToEdit.getEpisodes());
      // custom sort per filename (just this time)
      // for unknown EPs (-1/-1) this is extremely useful to sort like on filesystem
      // and for already renamed ones, it makes no difference
      epl.sort(Comparator.comparing(s -> s.getMediaFiles(MediaFileType.VIDEO).get(0).getFile()));

      for (TvShowEpisode episode : epl) {
        EpisodeEditorContainer container = new EpisodeEditorContainer();
        container.tvShowEpisode = episode;
        container.dvdOrder = episode.isDvdOrder();
        container.season = episode.getSeason();
        container.episode = episode.getEpisode();
        episodes.add(container);
      }

      trailers.addAll(tvShow.getTrailer());

    }

    // adjust columnn titles - we have to do it this way - thx to windowbuilder pro
    tableEpisodes.getColumnModel().getColumn(1).setCellRenderer(new LeftDotTableCellRenderer());
    tableEpisodes.getColumnModel().getColumn(2).setCellEditor(new TableSpinnerEditor());
    tableEpisodes.getColumnModel().getColumn(3).setCellEditor(new TableSpinnerEditor());

    // adjust table columns
    TableColumnResizer.adjustColumnPreferredWidths(tableActors, 6);
    TableColumnResizer.adjustColumnPreferredWidths(tableEpisodes, 6);

    // adjust columnn titles - we have to do it this way - thx to windowbuilder pro
    tableTrailer.getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("metatag.nfo"));
    tableTrailer.getColumnModel().getColumn(1).setHeaderValue(TmmResourceBundle.getString("metatag.name"));
    tableTrailer.getColumnModel().getColumn(2).setHeaderValue(TmmResourceBundle.getString("metatag.source"));
    tableTrailer.getColumnModel().getColumn(3).setHeaderValue(TmmResourceBundle.getString("metatag.quality"));
    tableTrailer.getColumnModel().getColumn(4).setHeaderValue(TmmResourceBundle.getString("metatag.url"));

    // adjust table columns
    tableTrailer.getColumnModel().getColumn(0).setMaxWidth(55);
    tableTrailer.adjustColumnPreferredWidths(5);

    // implement listener to simulate button group
    tableTrailer.getModel().addTableModelListener(arg0 -> {
      // click on the checkbox
      if (arg0.getColumn() == 0) {
        int row = arg0.getFirstRow();
        MediaTrailer changedTrailer = trailers.get(row);
        // if flag inNFO was changed, change all other trailers flags
        if (changedTrailer.getInNfo()) {
          for (MediaTrailer trailer : trailers) {
            if (trailer != changedTrailer) {
              trailer.setInNfo(Boolean.FALSE);
            }
          }
        }
      }
    });
  }

  private void initComponents() {
    JTabbedPane tabbedPane = new TmmTabbedPane();

    // to draw the shadow beneath window frame, encapsulate the panel
    JLayer<JComponent> rootLayer = new JLayer(tabbedPane, new ShadowLayerUI()); // removed <> because this leads WBP to crash
    getContentPane().add(rootLayer, BorderLayout.CENTER);

    /**********************************************************************************
     * DetailsPanel 1
     **********************************************************************************/
    {
      JPanel details1Panel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), details1Panel);
      details1Panel.setLayout(new MigLayout("", "[][grow][50lp:75lp][][60lp:75lp][100lp:n][][25lp:n][200lp:250lp,grow]",
          "[][][][75lp:25%:25%,grow][][][pref!][][][][75lp:20%:20%,grow][50lp:50lp:100lp,grow 50]"));

      {
        JLabel lblTitle = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        details1Panel.add(lblTitle, "cell 0 0,alignx right");

        tfTitle = new JTextField();
        details1Panel.add(tfTitle, "cell 1 0 6 1,growx");
      }
      {
        lblPoster = new ImageLabel();
        lblPoster.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), POSTER,
                tvShowList.getDefaultArtworkScrapers(), lblPoster, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblPoster, tfPoster);
          }
        });
        lblPoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        details1Panel.add(new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster")), "cell 8 0");
        LinkLabel lblPosterSize = new LinkLabel();
        details1Panel.add(lblPosterSize, "cell 8 0");

        JButton btnDeletePoster = new FlatButton(IconManager.DELETE_GRAY);
        btnDeletePoster.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeletePoster.addActionListener(e -> {
          lblPoster.clearImage();
          tfPoster.setText("");
        });
        details1Panel.add(btnDeletePoster, "cell 8 0");

        details1Panel.add(lblPoster, "cell 8 1 1 6, grow");
        lblPoster.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblPosterSize, lblPoster, btnDeletePoster, MediaFileType.POSTER));
      }
      {
        JLabel lblOriginalTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.originaltitle"));
        details1Panel.add(lblOriginalTitleT, "cell 0 1,alignx right");

        tfOriginalTitle = new JTextField();
        details1Panel.add(tfOriginalTitle, "cell 1 1 6 1,growx");
      }
      {
        JLabel lblSortTitle = new TmmLabel(TmmResourceBundle.getString("metatag.sorttitle"));
        details1Panel.add(lblSortTitle, "cell 0 2,alignx right");

        tfSorttitle = new JTextField();
        details1Panel.add(tfSorttitle, "cell 1 2 6 1,growx");
      }
      {
        JLabel lblPlot = new TmmLabel(TmmResourceBundle.getString("metatag.plot"));
        details1Panel.add(lblPlot, "cell 0 3,alignx right,aligny top");

        JScrollPane scrollPanePlot = new JScrollPane();
        details1Panel.add(scrollPanePlot, "cell 1 3 6 1,grow");

        taPlot = new JTextArea();
        taPlot.setLineWrap(true);
        taPlot.setWrapStyleWord(true);
        taPlot.setForeground(UIManager.getColor("TextField.foreground"));
        scrollPanePlot.setViewportView(taPlot);
      }
      {
        JLabel lblYear = new TmmLabel(TmmResourceBundle.getString("metatag.year"));
        details1Panel.add(lblYear, "cell 0 4,alignx right");

        spYear = new YearSpinner();
        details1Panel.add(spYear, "cell 1 4,growx");
      }
      {
        JLabel lblpremiered = new TmmLabel(TmmResourceBundle.getString("metatag.premiered"));
        details1Panel.add(lblpremiered, "cell 3 4,alignx right");

        dpPremiered = new DatePicker(tvShowToEdit.getFirstAired());
        details1Panel.add(dpPremiered, "cell 4 4 2 1,growx");
      }
      {
        JLabel lblStudio = new TmmLabel(TmmResourceBundle.getString("metatag.studio"));
        details1Panel.add(lblStudio, "cell 0 5,alignx right");

        tfStudio = new JTextField();
        details1Panel.add(tfStudio, "cell 1 5 6 1,growx");
      }
      {
        JLabel lblCountryT = new TmmLabel(TmmResourceBundle.getString("metatag.country"));
        details1Panel.add(lblCountryT, "cell 0 6,alignx trailing");

        tfCountry = new JTextField();
        details1Panel.add(tfCountry, "cell 1 6 6 1,growx");
      }
      {
        JLabel lblRuntime = new TmmLabel(TmmResourceBundle.getString("metatag.runtime"));
        details1Panel.add(lblRuntime, "cell 0 7,alignx right");

        spRuntime = new JSpinner();
        details1Panel.add(spRuntime, "flowx,cell 1 7,growx");

        JLabel lblMin = new TmmLabel(TmmResourceBundle.getString("metatag.minutes"));
        details1Panel.add(lblMin, "cell 1 7");
      }
      {
        JLabel lblStatus = new TmmLabel(TmmResourceBundle.getString("metatag.status"));
        details1Panel.add(lblStatus, "cell 3 7,alignx right");

        cbStatus = new JComboBox(MediaAiredStatus.values());
        details1Panel.add(cbStatus, "cell 4 7,growx");
      }
      {
        JLabel lblCertification = new TmmLabel(TmmResourceBundle.getString("metatag.certification"));
        details1Panel.add(lblCertification, "cell 0 8,alignx right");

        cbCertification = new JComboBox();
        details1Panel.add(cbCertification, "cell 1 8,growx");
      }
      {
        JLabel lblRating = new TmmLabel(TmmResourceBundle.getString("metatag.userrating"));
        details1Panel.add(lblRating, "cell 0 9,alignx right");

        spRating = new JSpinner();
        details1Panel.add(spRating, "cell 1 9,growx");
      }
      {
        JLabel lblRatingsT = new TmmLabel(TmmResourceBundle.getString("metatag.ratings"));
        details1Panel.add(lblRatingsT, "flowy,cell 0 10,alignx right,aligny top");

        JScrollPane scrollPaneRatings = new JScrollPane();
        details1Panel.add(scrollPaneRatings, "cell 1 10 4 1,grow");

        tableRatings = new MediaRatingTable(ratings);
        tableRatings.configureScrollPane(scrollPaneRatings);
      }
      {
        lblFanart = new ImageLabel();
        lblFanart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblFanart.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), BACKGROUND,
                tvShowList.getDefaultArtworkScrapers(), lblFanart, MediaType.TV_SHOW);

            dialog.bindExtraFanarts(extrafanarts);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblFanart, tfFanart);
          }
        });
        details1Panel.add(new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart")), "cell 8 8");

        LinkLabel lblFanartSize = new LinkLabel();
        details1Panel.add(lblFanartSize, "cell 8 8");

        JButton btnDeleteFanart = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteFanart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteFanart.addActionListener(e -> {
          lblFanart.clearImage();
          tfFanart.setText("");
        });
        details1Panel.add(btnDeleteFanart, "cell 8 8");

        details1Panel.add(lblFanart, "cell 8 9 1 4,grow");
        lblFanart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblFanartSize, lblFanart, btnDeleteFanart, MediaFileType.FANART));
      }

      JButton btnAddRating = new SquareIconButton(new AddRatingAction());
      details1Panel.add(btnAddRating, "cell 0 10,alignx right,aligny top");

      JButton btnRemoveRating = new SquareIconButton(new RemoveRatingAction());
      details1Panel.add(btnRemoveRating, "cell 0 10,alignx right,aligny top");

      {
        JLabel lblNoteT = new TmmLabel(TmmResourceBundle.getString("metatag.note"));
        details1Panel.add(lblNoteT, "cell 0 11,alignx right,aligny top");

        JScrollPane scrollPane = new JScrollPane();
        details1Panel.add(scrollPane, "cell 1 11 6 1,grow,wmin 0");

        taNote = new JTextArea();
        taNote.setLineWrap(true);
        taNote.setWrapStyleWord(true);
        taNote.setForeground(UIManager.getColor("TextField.foreground"));
        scrollPane.setViewportView(taNote);
      }
    }

    /**********************************************************************************
     * DetailsPanel 2
     **********************************************************************************/
    {
      JPanel details2Panel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.details2"), details2Panel);

      details2Panel
          .setLayout(new MigLayout("", "[][150lp:400lp,grow][20lp:n][][150lp:300lp,grow]", "[][100lp:150lp,grow][20lp:n][100lp:150lp,grow][][grow]"));
      {
        JLabel lblActors = new TmmLabel(TmmResourceBundle.getString("metatag.actors"));
        details2Panel.add(lblActors, "flowy,cell 0 0 1 2,alignx right,aligny top");

        tableActors = new PersonTable(actors);

        JScrollPane scrollPaneActors = new JScrollPane();
        tableActors.configureScrollPane(scrollPaneActors);
        details2Panel.add(scrollPaneActors, "cell 1 0 1 2,grow");

        JButton btnAddActor = new SquareIconButton(new AddActorAction());
        details2Panel.add(btnAddActor, "cell 0 0,alignx right");

        JButton btnRemoveActor = new SquareIconButton(new RemoveActorAction()); // $NON-NLS-1$
        details2Panel.add(btnRemoveActor, "cell 0 0,alignx right,aligny top");

        JButton btnMoveActorUp = new SquareIconButton(new MoveActorUpAction());
        details2Panel.add(btnMoveActorUp, "cell 0 0,alignx right");

        JButton btnMoveActorDown = new SquareIconButton(new MoveActorDownAction());
        details2Panel.add(btnMoveActorDown, "cell 0 0,alignx right,aligny top");
      }
      {
        JLabel lblDateAdded = new TmmLabel(TmmResourceBundle.getString("metatag.dateadded"));
        details2Panel.add(lblDateAdded, "cell 3 0,alignx right");

        spDateAdded = new JSpinner(new SpinnerDateModel());
        details2Panel.add(spDateAdded, "cell 4 0");
      }
      {
        JLabel lblIds = new TmmLabel("Ids");
        details2Panel.add(lblIds, "flowy,cell 3 1,alignx right,aligny top");

        tableIds = new MediaIdTable(ids);

        JScrollPane scrollPaneIds = new JScrollPane();
        tableIds.configureScrollPane(scrollPaneIds);
        details2Panel.add(scrollPaneIds, "cell 4 1,grow");

        JButton btnAddId = new SquareIconButton(new AddIdAction());
        details2Panel.add(btnAddId, "cell 3 1,alignx right");

        JButton btnRemoveId = new SquareIconButton(new RemoveIdAction());
        details2Panel.add(btnRemoveId, "cell 3 1,alignx right,aligny top");
      }
      {
        JLabel lblGenres = new TmmLabel(TmmResourceBundle.getString("metatag.genre"));
        details2Panel.add(lblGenres, "flowy,cell 0 3,alignx right,aligny top");

        JScrollPane scrollPaneGenres = new JScrollPane();
        details2Panel.add(scrollPaneGenres, "cell 1 3,grow");

        listGenres = new JList<>();
        scrollPaneGenres.setViewportView(listGenres);

        JButton btnAddGenre = new SquareIconButton(new AddGenreAction());
        details2Panel.add(btnAddGenre, "cell 0 3,alignx right");

        JButton btnRemoveGenre = new SquareIconButton(new RemoveGenreAction());
        details2Panel.add(btnRemoveGenre, "cell 0 3,alignx right,aligny top");

        JButton btnMoveGenreUp = new SquareIconButton(new MoveGenreUpAction());
        details2Panel.add(btnMoveGenreUp, "cell 0 3,alignx right,aligny top");

        JButton btnMoveGenreDown = new SquareIconButton(new MoveGenreDownAction());
        details2Panel.add(btnMoveGenreDown, "cell 0 3,alignx right,aligny top");

        cbGenres = new AutocompleteComboBox(MediaGenres.values());
        cbGenresAutoCompleteSupport = cbGenres.getAutoCompleteSupport();
        InputMap im = cbGenres.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object enterAction = im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        cbGenres.getActionMap().put(enterAction, new AddGenreAction());
        details2Panel.add(cbGenres, "cell 1 4,growx");
      }
      {
        JLabel lblTags = new TmmLabel(TmmResourceBundle.getString("metatag.tags"));
        details2Panel.add(lblTags, "flowy,cell 3 3,alignx right,aligny top");

        JScrollPane scrollPaneTags = new JScrollPane();
        details2Panel.add(scrollPaneTags, "cell 4 3,grow");
        listTags = new JList();
        scrollPaneTags.setViewportView(listTags);

        JButton btnAddTag = new SquareIconButton(new AddTagAction());
        details2Panel.add(btnAddTag, "cell 3 3,alignx right");

        JButton btnRemoveTag = new SquareIconButton(new RemoveTagAction());
        details2Panel.add(btnRemoveTag, "cell 3 3,alignx right,aligny top");

        JButton btnMoveTagUp = new SquareIconButton(new MoveTagUpAction());
        details2Panel.add(btnMoveTagUp, "cell 3 3,alignx right,aligny top");

        JButton btnMoveTagDown = new SquareIconButton(new MoveTagDownAction());
        details2Panel.add(btnMoveTagDown, "cell 3 3,alignx right,aligny top");

        cbTags = new AutocompleteComboBox<>(tvShowList.getTagsInTvShows());
        cbTagsAutoCompleteSupport = cbTags.getAutoCompleteSupport();
        InputMap im = cbTags.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object enterAction = im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        cbTags.getActionMap().put(enterAction, new AddTagAction());
        details2Panel.add(cbTags, "cell 4 4,growx");
      }
    }

    /**********************************************************************************
     * local artwork pane
     **********************************************************************************/
    {
      JPanel artworkPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.extraartwork"), null, artworkPanel, null);
      artworkPanel.setLayout(new MigLayout("", "[25%:35%:35%,grow][20lp:n][15%:25%:25%,grow][20lp:n][15%:25%:25%,grow]",
          "[][100lp:30%:30%,grow][20lp:n][][100lp:30%:30%,grow][20lp:n][][100lp:30%:30%,grow]"));

      {
        JLabel lblClearlogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearlogo"));

        artworkPanel.add(lblClearlogoT, "cell 0 0");

        LinkLabel lblClearlogoSize = new LinkLabel();
        artworkPanel.add(lblClearlogoSize, "cell 0 0");

        JButton btnDeleteClearLogo = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteClearLogo.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteClearLogo.addActionListener(e -> {
          lblClearlogo.clearImage();
          tfClearLogo.setText("");
        });
        artworkPanel.add(btnDeleteClearLogo, "cell 0 0");

        lblClearlogo = new ImageLabel();
        lblClearlogo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblClearlogo.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), CLEARLOGO,
                tvShowList.getDefaultArtworkScrapers(), lblClearlogo, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblClearlogo, tfClearLogo);
          }
        });
        artworkPanel.add(lblClearlogo, "cell 0 1,grow");
        lblClearlogo.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblClearlogoSize, lblClearlogo, btnDeleteClearLogo, MediaFileType.CLEARLOGO));
      }
      {
        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        artworkPanel.add(lblBannerT, "cell 2 0 3 1");

        LinkLabel lblBannerSize = new LinkLabel();
        artworkPanel.add(lblBannerSize, "cell 2 0 3 1");

        JButton btnDeleteBanner = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteBanner.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteBanner.addActionListener(e -> {
          lblBanner.clearImage();
          tfBanner.setText("");
        });
        artworkPanel.add(btnDeleteBanner, "cell 2 0 3 1");

        lblBanner = new ImageLabel();
        lblBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblBanner.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), BANNER,
                tvShowList.getDefaultArtworkScrapers(), lblBanner, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblBanner, tfBanner);

          }
        });
        artworkPanel.add(lblBanner, "cell 2 1 3 1,grow");
        lblBanner.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblBannerSize, lblBanner, btnDeleteBanner, MediaFileType.BANNER));
      }
      {
        JLabel lblClearartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearart"));
        artworkPanel.add(lblClearartT, "cell 2 3");

        LinkLabel lblClearartSize = new LinkLabel();
        artworkPanel.add(lblClearartSize, "cell 2 3");

        JButton btnDeleteClearart = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteClearart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteClearart.addActionListener(e -> {
          lblClearart.clearImage();
          tfClearArt.setText("");
        });
        artworkPanel.add(btnDeleteClearart, "cell 2 3");

        lblClearart = new ImageLabel();
        lblClearart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblClearart.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), CLEARART,
                tvShowList.getDefaultArtworkScrapers(), lblClearart, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblClearart, tfClearArt);
          }
        });
        artworkPanel.add(lblClearart, "cell 2 4,grow");
        lblClearart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblClearartSize, lblClearart, btnDeleteClearart, MediaFileType.CLEARART));
      }
      {
        JLabel lblLogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.logo"));
        artworkPanel.add(lblLogoT, "cell 0 3");

        LinkLabel lblLogoSize = new LinkLabel();
        artworkPanel.add(lblLogoSize, "cell 0 3");

        JButton btnDeleteLogo = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteLogo.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteLogo.addActionListener(e -> {
          lblLogo.clearImage();
          tfLogo.setText("");
        });
        artworkPanel.add(btnDeleteLogo, "cell 0 3");

        lblLogo = new ImageLabel();
        lblLogo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblLogo.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), LOGO,
                tvShowList.getDefaultArtworkScrapers(), lblLogo, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblLogo, tfLogo);
          }
        });
        artworkPanel.add(lblLogo, "cell 0 4,grow");
        lblLogo.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblLogoSize, lblLogo, btnDeleteLogo, MediaFileType.LOGO));
      }
      {
        JLabel lblKeyartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.keyart"));
        artworkPanel.add(lblKeyartT, "cell 4 3");

        LinkLabel lblKeyartSize = new LinkLabel();
        artworkPanel.add(lblKeyartSize, "cell 4 3");

        JButton btnDeleteKeyart = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteKeyart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteKeyart.addActionListener(e -> {
          lblKeyart.clearImage();
          tfKeyart.setText("");
        });
        artworkPanel.add(btnDeleteKeyart, "cell 4 3");

        lblKeyart = new ImageLabel();
        lblKeyart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblKeyart.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), KEYART,
                tvShowList.getDefaultArtworkScrapers(), lblKeyart, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblKeyart, tfKeyart);
          }
        });
        artworkPanel.add(lblKeyart, "cell 4 4 1 4,grow");
        lblKeyart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblKeyartSize, lblKeyart, btnDeleteKeyart, MediaFileType.KEYART));
      }
      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        artworkPanel.add(lblThumbT, "cell 0 6");

        LinkLabel lblThumbSize = new LinkLabel();
        artworkPanel.add(lblThumbSize, "cell 0 6");

        JButton btnDeleteThumb = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteThumb.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteThumb.addActionListener(e -> {
          lblThumb.clearImage();
          tfThumb.setText("");
        });
        artworkPanel.add(btnDeleteThumb, "cell 0 6");

        lblThumb = new ImageLabel();
        lblThumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblThumb.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), THUMB,
                tvShowList.getDefaultArtworkScrapers(), lblThumb, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblThumb, tfThumb);
          }
        });
        artworkPanel.add(lblThumb, "cell 0 7,grow");
        lblThumb.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblThumbSize, lblThumb, btnDeleteThumb, MediaFileType.THUMB));
      }
      {
        JLabel lblCharacterartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.characterart"));
        artworkPanel.add(lblCharacterartT, "cell 2 6");

        LinkLabel lblCharacterartSize = new LinkLabel();
        artworkPanel.add(lblCharacterartSize, "cell 2 6");

        JButton btnDeleteCharacterart = new FlatButton(IconManager.DELETE_GRAY);
        btnDeleteCharacterart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteCharacterart.addActionListener(e -> {
          lblCharacterart.clearImage();
          tfCharacterart.setText("");
        });
        artworkPanel.add(btnDeleteCharacterart, "cell 2 6");

        lblCharacterart = new ImageLabel();
        lblCharacterart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblCharacterart.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            ImageChooserDialog dialog = new ImageChooserDialog(TvShowEditorDialog.this, new HashMap<>(tvShowToEdit.getIds()), CHARACTERART,
                tvShowList.getDefaultArtworkScrapers(), lblCharacterart, MediaType.TV_SHOW);

            if (Settings.getInstance().isImageChooserUseEntityFolder()) {
              dialog.setOpenFolderPath(tvShowToEdit.getPathNIO().toAbsolutePath().toString());
            }

            dialog.setLocationRelativeTo(MainWindow.getInstance());
            dialog.setVisible(true);
            updateArtworkUrl(lblCharacterart, tfCharacterart);
          }
        });
        artworkPanel.add(lblCharacterart, "cell 2 7, grow");
        lblCharacterart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblCharacterartSize, lblCharacterart, btnDeleteCharacterart, MediaFileType.CHARACTERART));
      }
    }

    /**********************************************************************************
     * artwork urls
     **********************************************************************************/
    {
      JPanel artworkPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("edit.artwork"), null, artworkPanel, null);
      artworkPanel.setLayout(new MigLayout("", "[][grow]", "[][][][][][][][][]"));
      {
        JLabel lblPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
        artworkPanel.add(lblPosterT, "cell 0 0,alignx right");

        tfPoster = new JTextField();
        artworkPanel.add(tfPoster, "cell 1 0,growx");
      }
      {
        JLabel lblFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
        artworkPanel.add(lblFanartT, "cell 0 1,alignx right");

        tfFanart = new JTextField();
        artworkPanel.add(tfFanart, "cell 1 1,growx");
      }
      {
        JLabel lblLogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.logo"));
        artworkPanel.add(lblLogoT, "cell 0 2,alignx right");

        tfLogo = new JTextField();
        artworkPanel.add(tfLogo, "cell 1 2,growx");
      }
      {
        JLabel lblClearLogoT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearlogo"));
        artworkPanel.add(lblClearLogoT, "cell 0 3,alignx right");

        tfClearLogo = new JTextField();
        artworkPanel.add(tfClearLogo, "cell 1 3,growx");
      }
      {
        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        artworkPanel.add(lblBannerT, "cell 0 4,alignx right");

        tfBanner = new JTextField();
        artworkPanel.add(tfBanner, "cell 1 4,growx");
      }
      {
        JLabel lblClearArtT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.clearart"));
        artworkPanel.add(lblClearArtT, "cell 0 5,alignx right");

        tfClearArt = new JTextField();
        artworkPanel.add(tfClearArt, "cell 1 5,growx");
      }
      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        artworkPanel.add(lblThumbT, "cell 0 6,alignx right");

        tfThumb = new JTextField();
        artworkPanel.add(tfThumb, "cell 1 6,growx");
      }
      {
        JLabel lblCharacterartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.characterart"));
        artworkPanel.add(lblCharacterartT, "cell 0 7,alignx trailing");

        tfCharacterart = new JTextField();
        artworkPanel.add(tfCharacterart, "cell 1 7,growx");
      }
      {
        JLabel lblKeyartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.keyart"));
        artworkPanel.add(lblKeyartT, "cell 0 8,alignx trailing");

        tfKeyart = new JTextField();
        artworkPanel.add(tfKeyart, "cell 1 8,growx");
      }
    }

    /**********************************************************************************
     * episode pane
     **********************************************************************************/
    {
      JPanel episodesPanel = new JPanel();

      tabbedPane.addTab(TmmResourceBundle.getString("metatag.episodes"), episodesPanel);
      episodesPanel.setLayout(new MigLayout("", "[][200lp:450lp,grow]", "[][100lp:200lp,grow]"));
      {
        JButton btnCloneEpisode = new SquareIconButton(new CloneEpisodeAction());
        episodesPanel.add(btnCloneEpisode, "cell 0 0");
      }
      {
        tableEpisodes = new TmmTable(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(episodes), new EpisodeTableFormat()));

        JScrollPane scrollPaneEpisodes = new JScrollPane();
        tableEpisodes.configureScrollPane(scrollPaneEpisodes);
        episodesPanel.add(scrollPaneEpisodes, "cell 1 0 1 2,grow");
      }
      {
        JButton btnRemoveEpisode = new SquareIconButton(new RemoveEpisodeAction());
        episodesPanel.add(btnRemoveEpisode, "cell 0 1,aligny top");
      }
    }

    /**********************************************************************************
     * Trailer Panel
     **********************************************************************************/
    {
      JPanel trailerPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("Settings.trailer"), null, trailerPanel, null);
      trailerPanel.setLayout(new MigLayout("", "[][grow]", "[100lp:250lp,grow][grow 200]"));

      {
        JLabel lblTrailer = new TmmLabel(TmmResourceBundle.getString("metatag.trailer"));
        trailerPanel.add(lblTrailer, "flowy,cell 0 0,alignx right,aligny top");

        JButton btnAddTrailer = new SquareIconButton(new AddTrailerAction());
        trailerPanel.add(btnAddTrailer, "cell 0 0,alignx right,aligny top");

        JButton btnRemoveTrailer = new SquareIconButton(new RemoveTrailerAction());
        trailerPanel.add(btnRemoveTrailer, "cell 0 0,alignx right,aligny top");

        JButton btnPlayTrailer = new SquareIconButton(new PlayTrailerAction());
        trailerPanel.add(btnPlayTrailer, "cell 0 0,alignx right,aligny top");

        JScrollPane scrollPaneTrailer = new JScrollPane();
        trailerPanel.add(scrollPaneTrailer, "cell 1 0,grow");
        tableTrailer = new TmmTable();
        tableTrailer.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tableTrailer.configureScrollPane(scrollPaneTrailer);
      }
    }

    /**********************************************************************************
     * button pane
     **********************************************************************************/
    {
      if (queueSize > 1) {
        JButton btnAbort = new JButton(new AbortAction());
        addButton(btnAbort);
        if (queueIndex > 0) {
          JButton backButton = new JButton(new NavigateBackAction());
          addButton(backButton);
        }
      }

      JButton cancelButton = new JButton(new CancelAction());
      addButton(cancelButton);

      JButton okButton = new JButton(new OKAction());
      addDefaultButton(okButton);
    }
  }

  private void updateArtworkUrl(ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isNotBlank(imageLabel.getImageUrl())) {
      textField.setText(imageLabel.getImageUrl());
    }
  }

  private class OKAction extends AbstractAction {
    private static final long serialVersionUID = 6699599213348390696L;

    OKAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.ok"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.change"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      tvShowToEdit.setTitle(tfTitle.getText());
      tvShowToEdit.setOriginalTitle(tfOriginalTitle.getText());
      tvShowToEdit.setSortTitle(tfSorttitle.getText());
      tvShowToEdit.setYear((Integer) spYear.getValue());
      tvShowToEdit.setPlot(taPlot.getText());
      tvShowToEdit.setRuntime((Integer) spRuntime.getValue());

      // sync of media ids
      // first round -> add existing ids
      for (MediaId id : ids) {
        // only process non empty ids
        // changed; if empty/0/null value gets set, it is removed in setter ;)
        // if (StringUtils.isAnyBlank(id.key, id.value)) {
        // continue;
        // }
        // first try to cast it into an Integer
        try {
          Integer value = Integer.parseInt(id.value);
          // cool, it is an Integer
          tvShowToEdit.setId(id.key, value);
        }
        catch (NumberFormatException ex) {
          // okay, we set it as a String
          tvShowToEdit.setId(id.key, id.value);
        }
      }
      // second round -> remove deleted ids
      List<String> removeIds = new ArrayList<>();
      for (Entry<String, Object> entry : tvShowToEdit.getIds().entrySet()) {
        MediaId id = new MediaId(entry.getKey());
        if (!ids.contains(id)) {
          removeIds.add(entry.getKey());
        }
      }
      for (String id : removeIds) {
        // set a null value causes to fire the right events
        tvShowToEdit.setId(id, null);
      }

      Object certification = cbCertification.getSelectedItem();
      if (certification instanceof MediaCertification) {
        tvShowToEdit.setCertification((MediaCertification) certification);
      }

      // process artwork
      processArtwork(MediaFileType.POSTER, lblPoster, tfPoster);
      processArtwork(MediaFileType.FANART, lblFanart, tfFanart);
      processArtwork(MediaFileType.LOGO, lblLogo, tfLogo);
      processArtwork(MediaFileType.CLEARLOGO, lblClearlogo, tfClearLogo);
      processArtwork(MediaFileType.BANNER, lblBanner, tfBanner);
      processArtwork(MediaFileType.CLEARART, lblClearart, tfClearArt);
      processArtwork(MediaFileType.THUMB, lblThumb, tfThumb);
      processArtwork(MediaFileType.CHARACTERART, lblCharacterart, tfCharacterart);
      processArtwork(MediaFileType.KEYART, lblKeyart, tfKeyart);

      // set extrafanarts
      if (extrafanarts != null && (extrafanarts.size() != tvShowToEdit.getExtraFanartUrls().size()
          || !extrafanarts.containsAll(tvShowToEdit.getExtraFanartUrls()) || !tvShowToEdit.getExtraFanartUrls().containsAll(extrafanarts))) {
        tvShowToEdit.setExtraFanartUrls(extrafanarts);
        tvShowToEdit.downloadArtwork(MediaFileType.EXTRAFANART);
      }

      tvShowToEdit.setProductionCompany(tfStudio.getText());
      tvShowToEdit.setCountry(tfCountry.getText());
      tvShowToEdit.setNote(taNote.getText());
      tvShowToEdit.setGenres(genres);

      // remove cast to avoid merging
      tvShowToEdit.removeActors();
      tvShowToEdit.setActors(actors);

      tvShowToEdit.setTags(tags);
      tvShowToEdit.setDateAdded((Date) spDateAdded.getValue());
      tvShowToEdit.setFirstAired(dpPremiered.getDate());

      tvShowToEdit.setStatus((MediaAiredStatus) cbStatus.getSelectedItem());

      // user rating
      Map<String, MediaRating> newRatings = new HashMap<>();

      if ((double) spRating.getValue() > 0) {
        newRatings.put(MediaRating.USER, new MediaRating(MediaRating.USER, (double) spRating.getValue(), 1, 10));
      }

      // other ratings
      for (MediaRatingTable.Rating rating : TvShowEditorDialog.this.ratings) {
        if (StringUtils.isNotBlank(rating.key) && rating.value > 0) {
          newRatings.put(rating.key, new MediaRating(rating.key, rating.value, rating.votes, rating.maxValue));
        }
      }
      tvShowToEdit.setRatings(newRatings);

      // adapt episodes according to the episode table (in a 2 way sync)
      // remove episodes
      for (int i = tvShowToEdit.getEpisodeCount() - 1; i >= 0; i--) {
        boolean found = false;
        TvShowEpisode episode = tvShowToEdit.getEpisodes().get(i);
        for (EpisodeEditorContainer container : episodes) {
          if (container.tvShowEpisode == episode) {
            found = true;
            break;
          }
        }

        if (!found) {
          tvShowToEdit.removeEpisode(episode);
        }
      }

      // add episodes
      for (EpisodeEditorContainer container : episodes) {
        boolean found = false;
        boolean shouldStore = false;

        if (container.dvdOrder != container.tvShowEpisode.isDvdOrder()) {
          container.tvShowEpisode.setDvdOrder(container.dvdOrder);
          shouldStore = true;
        }

        if (container.episode != container.tvShowEpisode.getEpisode()) {
          if (container.dvdOrder) {
            container.tvShowEpisode.setDvdEpisode(container.episode);
          }
          else {
            container.tvShowEpisode.setAiredEpisode(container.episode);
          }

          shouldStore = true;
        }

        if (container.season != container.tvShowEpisode.getSeason()) {
          if (container.dvdOrder) {
            container.tvShowEpisode.setDvdSeason(container.season);
          }
          else {
            container.tvShowEpisode.setAiredSeason(container.season);
          }
          shouldStore = true;
        }

        for (TvShowEpisode episode : tvShowToEdit.getEpisodes()) {
          if (container.tvShowEpisode == episode) {
            found = true;
            break;
          }
        }

        if (!found) {
          container.tvShowEpisode.writeNFO();
          container.tvShowEpisode.saveToDb();
          tvShowToEdit.addEpisode(container.tvShowEpisode);
        }
        else if (shouldStore) {
          container.tvShowEpisode.writeNFO();
          container.tvShowEpisode.saveToDb();
        }
      }

      tvShowToEdit.removeAllTrailers();
      tvShowToEdit.addToTrailer(trailers);

      tvShowToEdit.writeNFO();
      tvShowToEdit.saveToDb();

      if (TvShowModuleManager.getInstance().getSettings().getSyncTrakt()) {
        TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(Collections.singletonList(tvShowToEdit));
        task.setSyncCollection(TvShowModuleManager.getInstance().getSettings().getSyncTraktCollection());
        task.setSyncWatched(TvShowModuleManager.getInstance().getSettings().getSyncTraktWatched());
        task.setSyncRating(TvShowModuleManager.getInstance().getSettings().getSyncTraktRating());

        TmmTaskManager.getInstance().addUnnamedTask(task);
      }

      setVisible(false);
    }
  }

  private void processArtwork(MediaFileType type, ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isAllBlank(imageLabel.getImagePath(), imageLabel.getImageUrl())
        && StringUtils.isNotBlank(tvShowToEdit.getArtworkFilename(type))) {
      // artwork has been explicitly deleted
      tvShowToEdit.deleteMediaFiles(type);
    }

    if (StringUtils.isNotEmpty(textField.getText()) && !textField.getText().equals(tvShowToEdit.getArtworkUrl(type))) {
      // artwork url and textfield do not match -> redownload
      tvShowToEdit.setArtworkUrl(textField.getText(), type);
      tvShowToEdit.downloadArtwork(type);
    }
    else if (StringUtils.isEmpty(textField.getText())) {
      // remove the artwork url
      tvShowToEdit.removeArtworkUrl(type);
    }
    else {
      // they match, but check if there is a need to download the artwork
      if (StringUtils.isBlank(tvShowToEdit.getArtworkFilename(type))) {
        tvShowToEdit.downloadArtwork(type);
      }
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = -4617793684152607277L;

    CancelAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.cancel"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("edit.discard"));
      putValue(SMALL_ICON, IconManager.CANCEL_INV);
      putValue(LARGE_ICON_KEY, IconManager.CANCEL_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }

  private class AddRatingAction extends AbstractAction {
    private static final long serialVersionUID = 2903255414533349267L;

    private AddRatingAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("rating.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaRatingTable.Rating rating = new MediaRatingTable.Rating("");
      // default values
      rating.maxValue = 10;
      rating.votes = 1;

      RatingEditorDialog dialog = new RatingEditorDialog(SwingUtilities.getWindowAncestor(tableActors), TmmResourceBundle.getString("rating.add"),
          rating);
      dialog.setVisible(true);

      if (StringUtils.isNotBlank(rating.key) && rating.value > 0 && rating.maxValue > 0 && rating.votes > 0) {
        ratings.add(rating);
      }
    }
  }

  private class RemoveRatingAction extends AbstractAction {
    private static final long serialVersionUID = -7079821950827356996L;

    private RemoveRatingAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("rating.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableRatings.getSelectedRow();
      if (row > -1) {
        row = tableRatings.convertRowIndexToModel(row);
        ratings.remove(row);
      }
    }
  }

  private class AddActorAction extends AbstractAction {
    private static final long serialVersionUID = -5879601617842300526L;

    AddActorAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.actor.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Person actor = new Person(ACTOR, TmmResourceBundle.getString("cast.actor.unknown"), TmmResourceBundle.getString("cast.role.unknown"));
      PersonEditorDialog dialog = new PersonEditorDialog(SwingUtilities.getWindowAncestor(tableActors), TmmResourceBundle.getString("cast.actor.add"),
          actor);
      dialog.setVisible(true);

      if (StringUtils.isNotBlank(actor.getName()) && !actor.getName().equals(TmmResourceBundle.getString("cast.actor.unknown"))) {
        if (actor.getRole().equals(TmmResourceBundle.getString("cast.role.unknown"))) {
          actor.setRole("");
        }
        actors.add(0, actor);
      }
    }
  }

  private class RemoveActorAction extends AbstractAction {
    private static final long serialVersionUID = 6970920169867315771L;

    RemoveActorAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("cast.actor.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableActors.getSelectedRow();
      if (row > -1) {
        row = tableActors.convertRowIndexToModel(row);
        actors.remove(row);
      }
    }
  }

  private class MoveActorUpAction extends AbstractAction {
    private static final long serialVersionUID = 5775423424097844658L;

    MoveActorUpAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.moveactorup"));
      putValue(SMALL_ICON, IconManager.ARROW_UP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableActors.getSelectedRow();
      if (row > 0) {
        Collections.rotate(actors.subList(row - 1, row + 1), 1);
        tableActors.getSelectionModel().setSelectionInterval(row - 1, row - 1);
      }
    }
  }

  private class MoveActorDownAction extends AbstractAction {
    private static final long serialVersionUID = -6564146895819191932L;

    MoveActorDownAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.moveactordown"));
      putValue(SMALL_ICON, IconManager.ARROW_DOWN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableActors.getSelectedRow();
      if (row < actors.size() - 1) {
        Collections.rotate(actors.subList(row, row + 2), -1);
        tableActors.getSelectionModel().setSelectionInterval(row + 1, row + 1);
      }
    }
  }

  private class AddGenreAction extends AbstractAction {
    private static final long serialVersionUID = 6666302391216952247L;

    AddGenreAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("genre.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaGenres newGenre = null;
      Object item = cbGenres.getSelectedItem();

      // check, if text is selected (from auto completion), in this case we just
      // remove the selection
      Component editorComponent = cbGenres.getEditor().getEditorComponent();
      if (editorComponent instanceof JTextField) {
        JTextField tf = (JTextField) editorComponent;
        String selectedText = tf.getSelectedText();
        if (selectedText != null) {
          tf.setSelectionStart(0);
          tf.setSelectionEnd(0);
          tf.setCaretPosition(tf.getText().length());
          return;
        }
      }

      // genre
      if (item instanceof MediaGenres) {
        newGenre = (MediaGenres) item;
      }

      // newly created genre?
      if (item instanceof String) {
        newGenre = MediaGenres.getGenre((String) item);
      }

      // add genre if it is not already in the list
      if (newGenre != null && !genres.contains(newGenre)) {
        genres.add(newGenre);

        // set text combobox text input to ""
        if (editorComponent instanceof JTextField) {
          cbGenresAutoCompleteSupport.setFirstItem(null);
          cbGenres.setSelectedIndex(0);
          cbGenresAutoCompleteSupport.removeFirstItem();
        }
      }
    }
  }

  private class RemoveGenreAction extends AbstractAction {
    private static final long serialVersionUID = -5459615776560234688L;

    RemoveGenreAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("genre.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      for (MediaGenres genre : listGenres.getSelectedValuesList()) {
        genres.remove(genre);
      }
    }
  }

  private class MoveGenreUpAction extends AbstractAction {
    private static final long serialVersionUID = -6855661707692602266L;

    MoveGenreUpAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movegenreup"));
      putValue(SMALL_ICON, IconManager.ARROW_UP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = listGenres.getSelectedIndex();
      if (row > 0) {
        Collections.rotate(genres.subList(row - 1, row + 1), 1);
        listGenres.getSelectionModel().setSelectionInterval(row - 1, row - 1);
      }
    }
  }

  private class MoveGenreDownAction extends AbstractAction {
    private static final long serialVersionUID = -1135108943010008069L;

    MoveGenreDownAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movegenredown"));
      putValue(SMALL_ICON, IconManager.ARROW_DOWN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = listGenres.getSelectedIndex();
      if (row < genres.size() - 1) {
        Collections.rotate(genres.subList(row, row + 2), -1);
        listGenres.getSelectionModel().setSelectionInterval(row + 1, row + 1);
      }
    }
  }

  /**
   * Shows the dialog and returns whether the work on the queue should be continued.
   *
   * @return true, if successful
   */
  public boolean showDialog() {
    setVisible(true);
    return continueQueue;
  }

  private class AddTagAction extends AbstractAction {
    private static final long serialVersionUID = 9160043031922897785L;

    AddTagAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tag.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String newTag = (String) cbTags.getSelectedItem();
      if (StringUtils.isBlank(newTag)) {
        return;
      }

      // check, if text is selected (from auto completion), in this case we just
      // remove the selection
      Component editorComponent = cbTags.getEditor().getEditorComponent();
      if (editorComponent instanceof JTextField) {
        JTextField tf = (JTextField) editorComponent;
        String selectedText = tf.getSelectedText();
        if (selectedText != null) {
          tf.setSelectionStart(0);
          tf.setSelectionEnd(0);
          tf.setCaretPosition(tf.getText().length());
          return;
        }
      }

      // search if this tag already has been added
      boolean tagFound = false;
      for (String tag : tags) {
        if (tag.equals(newTag)) {
          tagFound = true;
          break;
        }
      }

      // add tag
      if (!tagFound) {
        tags.add(newTag);

        // set text combobox text input to ""
        if (editorComponent instanceof JTextField) {
          cbTagsAutoCompleteSupport.setFirstItem("");
          cbTags.setSelectedIndex(0);
          cbTagsAutoCompleteSupport.removeFirstItem();
        }
      }
    }
  }

  private class AddIdAction extends AbstractAction {
    private static final long serialVersionUID = 2903255414553349267L;

    AddIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaId mediaId = new MediaId();
      IdEditorDialog dialog = new IdEditorDialog(SwingUtilities.getWindowAncestor(tableIds), TmmResourceBundle.getString("id.add"), mediaId,
          ScraperType.TV_SHOW);
      dialog.setVisible(true);

      if (StringUtils.isNoneBlank(mediaId.key, mediaId.value)) {
        ids.add(mediaId);
      }
    }
  }

  private class RemoveIdAction extends AbstractAction {
    private static final long serialVersionUID = -7079826950827356996L;

    RemoveIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableIds.getSelectedRow();
      if (row > -1) {
        row = tableIds.convertRowIndexToModel(row);
        ids.remove(row);
      }
    }
  }

  private class RemoveTagAction extends AbstractAction {
    private static final long serialVersionUID = -1580945350962234235L;

    RemoveTagAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tag.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      for (String tag : listTags.getSelectedValuesList()) {
        tags.remove(tag);
      }
    }
  }

  private class MoveTagUpAction extends AbstractAction {
    private static final long serialVersionUID = -6855661707692602266L;

    MoveTagUpAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movetagup"));
      putValue(SMALL_ICON, IconManager.ARROW_UP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = listTags.getSelectedIndex();
      if (row > 0) {
        Collections.rotate(tags.subList(row - 1, row + 1), 1);
        listTags.getSelectionModel().setSelectionInterval(row - 1, row - 1);
      }
    }
  }

  private class MoveTagDownAction extends AbstractAction {
    private static final long serialVersionUID = -1135108943010008069L;

    MoveTagDownAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.edit.movetagdown"));
      putValue(SMALL_ICON, IconManager.ARROW_DOWN_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = listTags.getSelectedIndex();
      if (row < tags.size() - 1) {
        Collections.rotate(tags.subList(row, row + 2), -1);
        listTags.getSelectionModel().setSelectionInterval(row + 1, row + 1);
      }
    }
  }

  private class AbortAction extends AbstractAction {
    private static final long serialVersionUID = -7652218354710642510L;

    AbortAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.abortqueue"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.edit.abortqueue.desc"));
      putValue(SMALL_ICON, IconManager.STOP_INV);
      putValue(LARGE_ICON_KEY, IconManager.STOP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      continueQueue = false;
      setVisible(false);
    }
  }

  private class NavigateBackAction extends AbstractAction {
    private static final long serialVersionUID = -1652218154720642310L;

    private NavigateBackAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.back"));
      putValue(SMALL_ICON, IconManager.BACK_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      navigateBack = true;
      setVisible(false);
    }
  }

  private static class EpisodeEditorContainer extends AbstractModelObject {
    TvShowEpisode tvShowEpisode;
    int           season;
    int           episode;
    boolean       dvdOrder = false;

    public String getEpisodeTitle() {
      return tvShowEpisode.getTitle();
    }

    public String getMediaFilename() {
      List<MediaFile> mfs = tvShowEpisode.getMediaFiles(MediaFileType.VIDEO);
      if (mfs != null && !mfs.isEmpty()) {
        return mfs.get(0).getFile().toString();
      }
      else {
        return "";
      }
    }

    public int getEpisode() {
      return episode;
    }

    public void setEpisode(int newValue) {
      int oldValue = this.episode;
      this.episode = newValue;
      firePropertyChange("episode", oldValue, newValue);
    }

    public int getSeason() {
      return season;
    }

    public void setSeason(int newValue) {
      int oldValue = this.season;
      this.season = newValue;
      firePropertyChange("season", oldValue, newValue);
    }

    public boolean isDvdOrder() {
      return this.dvdOrder;
    }

    public void setDvdOrder(boolean newValue) {
      boolean oldValue = this.dvdOrder;
      this.dvdOrder = newValue;
      firePropertyChange("dvdOrder", oldValue, newValue);
    }
  }

  @Override
  public void dispose() {
    if (dpPremiered != null) {
      dpPremiered.cleanup();
    }

    super.dispose();
  }

  public boolean isContinueQueue() {
    return continueQueue;
  }

  public boolean isNavigateBack() {
    return navigateBack;
  }

  private class CloneEpisodeAction extends AbstractAction {
    private static final long serialVersionUID = -3255090541823134232L;

    CloneEpisodeAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshowepisode.clone"));
      putValue(SMALL_ICON, IconManager.COPY_INV);
      putValue(LARGE_ICON_KEY, IconManager.COPY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      int row = tableEpisodes.getSelectedRow();
      if (row > -1) {
        row = tableEpisodes.convertRowIndexToModel(row);
        EpisodeEditorContainer origContainer = episodes.get(row);
        EpisodeEditorContainer newContainer = new EpisodeEditorContainer();
        newContainer.tvShowEpisode = new TvShowEpisode(origContainer.tvShowEpisode);
        newContainer.tvShowEpisode.setTitle(origContainer.tvShowEpisode.getTitle() + " (clone)");
        newContainer.episode = -1;
        newContainer.season = newContainer.tvShowEpisode.getSeason();
        episodes.add(row + 1, newContainer);
      }
    }
  }

  private class RemoveEpisodeAction extends AbstractAction {
    private static final long serialVersionUID = -8233854057648972649L;

    RemoveEpisodeAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshowepisode.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableEpisodes.getSelectedRow();
      if (row > -1) {
        row = tableEpisodes.convertRowIndexToModel(row);
        episodes.remove(row);
      }
    }
  }

  private static class EpisodeTableFormat extends TmmTableFormat<EpisodeEditorContainer> implements WritableTableFormat<EpisodeEditorContainer> {
    EpisodeTableFormat() {
      /*
       * title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.title"), "name", EpisodeEditorContainer::getEpisodeTitle, String.class);
      col.setColumnResizeable(true);
      addColumn(col);

      /*
       * MF name
       */
      col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", EpisodeEditorContainer::getMediaFilename, String.class);
      col.setColumnResizeable(true);
      addColumn(col);

      /*
       * season
       */
      col = new Column(TmmResourceBundle.getString("metatag.season"), "season", EpisodeEditorContainer::getSeason, Integer.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * episode
       */
      col = new Column(TmmResourceBundle.getString("metatag.episode"), "episode", EpisodeEditorContainer::getEpisode, Integer.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * DVD order
       */
      col = new Column(TmmResourceBundle.getString("metatag.dvdorder"), "name", EpisodeEditorContainer::isDvdOrder, Boolean.class);
      col.setColumnResizeable(false);
      addColumn(col);
    }

    @Override
    public boolean isEditable(EpisodeEditorContainer episodeEditorContainer, int i) {
      switch (i) {
        case 2:
        case 3:
        case 4:
          return true;

        default:
          return false;
      }
    }

    @Override
    public EpisodeEditorContainer setColumnValue(EpisodeEditorContainer episodeEditorContainer, Object o, int i) {
      switch (i) {
        case 2:
          episodeEditorContainer.setSeason((Integer) o);
          break;

        case 3:
          episodeEditorContainer.setEpisode((Integer) o);
          break;

        case 4:
          episodeEditorContainer.setDvdOrder((Boolean) o);
          break;

        default:
          break;
      }
      return episodeEditorContainer;
    }
  }

  protected BindingGroup initDataBindings() {
    JListBinding<MediaGenres, List<MediaGenres>, JList> jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ, genres, listGenres);
    jListBinding.bind();
    //
    JListBinding<String, List<String>, JList> jListBinding_1 = SwingBindings.createJListBinding(UpdateStrategy.READ, tags, listTags);
    jListBinding_1.bind();
    //

    JTableBinding<MediaTrailer, List<MediaTrailer>, JTable> jTableBinding_2 = SwingBindings.createJTableBinding(AutoBinding.UpdateStrategy.READ,
        trailers, tableTrailer);
    //
    BeanProperty<MediaTrailer, Boolean> trailerBeanProperty = BeanProperty.create("inNfo");
    jTableBinding_2.addColumnBinding(trailerBeanProperty).setColumnClass(Boolean.class);
    //
    BeanProperty<MediaTrailer, String> trailerBeanProperty_1 = BeanProperty.create("name");
    jTableBinding_2.addColumnBinding(trailerBeanProperty_1);
    //
    BeanProperty<MediaTrailer, String> trailerBeanProperty_2 = BeanProperty.create("provider");
    jTableBinding_2.addColumnBinding(trailerBeanProperty_2);
    //
    BeanProperty<MediaTrailer, String> trailerBeanProperty_3 = BeanProperty.create("quality");
    jTableBinding_2.addColumnBinding(trailerBeanProperty_3);
    //
    BeanProperty<MediaTrailer, String> trailerBeanProperty_4 = BeanProperty.create("url");
    jTableBinding_2.addColumnBinding(trailerBeanProperty_4);
    //
    jTableBinding_2.bind();
    BindingGroup bindingGroup = new BindingGroup();
    //
    bindingGroup.addBinding(jListBinding);
    bindingGroup.addBinding(jListBinding_1);
    bindingGroup.addBinding(jTableBinding_2);

    return bindingGroup;
  }

  private void setImageSizeAndCreateLink(LinkLabel lblSize, ImageLabel imageLabel, JButton buttonDelete, MediaFileType type) {
    createLinkForImage(lblSize, imageLabel);

    // image has been deleted
    if (imageLabel.getOriginalImageSize().width == 0 && imageLabel.getOriginalImageSize().height == 0) {
      lblSize.setText("");
      lblSize.setVisible(false);
      buttonDelete.setVisible(false);
      return;
    }

    Dimension dimension = tvShowToEdit.getArtworkDimension(type);
    if (dimension.width == 0 && dimension.height == 0) {
      lblSize.setText(imageLabel.getOriginalImageSize().width + "x" + imageLabel.getOriginalImageSize().height);
    }
    else {
      lblSize.setText(dimension.width + "x" + dimension.height);
    }

    lblSize.setVisible(true);
    buttonDelete.setVisible(true);
  }

  private class AddTrailerAction extends AbstractAction {
    private static final long serialVersionUID = -4446154040952056823L;

    public AddTrailerAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("trailer.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaTrailer trailer = new MediaTrailer();
      trailer.setName("unknown");
      trailer.setProvider("unknown");
      trailer.setQuality("unknown");
      trailer.setUrl("http://");
      trailers.add(0, trailer);
    }
  }

  private class RemoveTrailerAction extends AbstractAction {
    private static final long serialVersionUID = -6956921050689930101L;

    public RemoveTrailerAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("trailer.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableTrailer.getSelectedRow();
      if (row > -1) {
        row = tableTrailer.convertRowIndexToModel(row);
        trailers.remove(row);
      }
    }
  }

  /**
   * Play the selected Trailer
   */
  private class PlayTrailerAction extends AbstractAction {

    public PlayTrailerAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("trailer.play"));
      putValue(SMALL_ICON, IconManager.PLAY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableTrailer.getSelectedRow();
      if (row > -1) {
        row = tableTrailer.convertRowIndexToModel(row);
        MediaTrailer selectedTrailer = trailers.get(row);

        String url = selectedTrailer.getUrl();
        try {
          TmmUIHelper.browseUrl(url);
        }
        catch (Exception ex) {
          MessageManager.instance
              .pushMessage(new Message(Message.MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", ex.getLocalizedMessage() }));
        }
      }
      else {
        // Now Row selected
        JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      }
    }
  }

}
