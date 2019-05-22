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
package org.tinymediamanager.ui.tvshows.panels.tvshow;

import static org.tinymediamanager.core.Constants.BANNER;
import static org.tinymediamanager.core.Constants.FANART;
import static org.tinymediamanager.core.Constants.POSTER;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.Rating;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.StarRater;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.converter.RatingConverter;
import org.tinymediamanager.ui.converter.VoteCountConverter;
import org.tinymediamanager.ui.panels.MediaInformationLogosPanel;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowInformationPanel extends JPanel {
  private static final long           serialVersionUID = 1911808562993073590L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
  private StarRater                   panelRatingStars;
  private JLabel                      lblTvShowName;
  private JLabel                      lblRating;
  private JLabel                      lblVoteCount;
  private ImageLabel                  lblTvShowBackground;
  private JLabel                      lblFanartSize;
  private ImageLabel                  lblTvShowPoster;
  private JLabel                      lblPosterSize;
  private ImageLabel                  lblTvShowBanner;
  private JLabel                      lblBannerSize;
  private JTextArea                   tpOverview;
  private MediaInformationLogosPanel  panelLogos;

  private TvShowSelectionModel        tvShowSelectionModel;
  private JLabel                      lblOriginalTitle;

  /**
   * Instantiates a new tv show information panel.
   * 
   * @param tvShowSelectionModel
   *          the tv show selection model
   */
  public TvShowInformationPanel(TvShowSelectionModel tvShowSelectionModel) {
    this.tvShowSelectionModel = tvShowSelectionModel;

    initComponents();

    // beansbinding init
    initDataBindings();

    // manual coded binding
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection of a movie and change of a tv show
      if (source instanceof TvShowSelectionModel) {
        TvShowSelectionModel model = (TvShowSelectionModel) source;
        setFanart(model.getSelectedTvShow());
        setPoster(model.getSelectedTvShow());
        setBanner(model.getSelectedTvShow());
        panelLogos.setMediaInformationSource(model.getSelectedTvShow());
      }
      if (source instanceof TvShow || source instanceof MediaFile) {
        // if there is another change in the episode/media file, just update the logos to be sure
        TvShow tvShow = tvShowSelectionModel.getSelectedTvShow();
        if (tvShow != null) {
          panelLogos.setMediaInformationSource(tvShow);
        }
      }
      if (source instanceof TvShow && FANART.equals(property)) {
        TvShow tvShow = (TvShow) source;
        setFanart(tvShow);
      }
      if (source instanceof TvShow && POSTER.equals(property)) {
        TvShow tvShow = (TvShow) source;
        setPoster(tvShow);
      }
      if (source instanceof TvShow && BANNER.equals(property)) {
        TvShow tvShow = (TvShow) source;
        setBanner(tvShow);
      }
    };

    tvShowSelectionModel.addPropertyChangeListener(propertyChangeListener);

    // select first entry

  }

  private void initComponents() {
    setLayout(new MigLayout("", "[100lp:100lp,grow][300lp:300lp,grow 250]", "[grow]"));
    {
      JPanel panelLeft = new JPanel();
      panelLeft.setLayout(new ColumnLayout());
      add(panelLeft, "cell 0 0,grow");

      lblTvShowPoster = new ImageLabel(false, false, true);
      lblTvShowPoster.setDesiredAspectRatio(2 / 3f);
      lblTvShowPoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblTvShowPoster);
      lblTvShowPoster.enableLightbox();
      lblPosterSize = new JLabel(BUNDLE.getString("mediafiletype.poster")); //$NON-NLS-1$
      panelLeft.add(lblPosterSize);
      panelLeft.add(Box.createVerticalStrut(20));

      lblTvShowBackground = new ImageLabel(false, false, true);
      lblTvShowBackground.setDesiredAspectRatio(16 / 9f);
      lblTvShowBackground.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblTvShowBackground);
      lblTvShowBackground.enableLightbox();
      lblFanartSize = new JLabel(BUNDLE.getString("mediafiletype.fanart")); //$NON-NLS-1$
      panelLeft.add(lblFanartSize);
      panelLeft.add(Box.createVerticalStrut(20));

      lblTvShowBanner = new ImageLabel(false, false, true);
      lblTvShowBanner.setDesiredAspectRatio(25 / 8f);
      lblTvShowBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblTvShowBanner);
      lblTvShowBanner.enableLightbox();
      lblBannerSize = new JLabel(BUNDLE.getString("mediafiletype.banner")); //$NON-NLS-1$
      panelLeft.add(lblBannerSize);
    }
    {
      JPanel panelRight = new JPanel();
      add(panelRight, "cell 1 0,grow");
      panelRight.setLayout(new MigLayout("", "[450lp,grow]", "[][][shrink 0][][shrink 0][][shrink 0][][shrink 0][][]"));

      {
        lblTvShowName = new TmmLabel("", 1.33);
        panelRight.add(lblTvShowName, "cell 0 0,growx,wmin 0");
      }
      {
        lblOriginalTitle = new JLabel("");
        panelRight.add(lblOriginalTitle, "cell 0 1,growx,wmin 0");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 2,growx");
      }
      {
        JPanel panelDetails = new TvShowDetailsPanel(tvShowSelectionModel);
        panelRight.add(panelDetails, "cell 0 3,growx");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 4,growx");
      }
      {
        panelRatingStars = new StarRater(10, 1);
        panelRight.add(panelRatingStars, "flowx,cell 0 5,aligny center");
        panelRatingStars.setEnabled(false);

        lblRating = new JLabel("");
        panelRight.add(lblRating, "cell 0 5,aligny center");

        lblVoteCount = new JLabel("");
        panelRight.add(lblVoteCount, "cell 0 5,aligny center");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 6,growx");
      }
      {
        panelLogos = new MediaInformationLogosPanel();
        panelRight.add(panelLogos, "cell 0 7,growx");
      }
      {
        panelRight.add(new JSeparator(), "cell 0 8,growx");
      }
      {
        JLabel lblPlot = new TmmLabel(BUNDLE.getString("metatag.plot"));
        panelRight.add(lblPlot, "cell 0 9");
        TmmFontHelper.changeFont(lblPlot, Font.BOLD);

        JScrollPane scrollPaneOverview = new JScrollPane();
        panelRight.add(scrollPaneOverview, "cell 0 10,grow");

        tpOverview = new ReadOnlyTextArea();
        scrollPaneOverview.setViewportView(tpOverview);
      }
    }
  }

  private void setPoster(TvShow tvShow) {
    lblTvShowPoster.clearImage();
    lblTvShowPoster.setImagePath(tvShow.getArtworkFilename(MediaFileType.POSTER));
    Dimension posterSize = tvShow.getArtworkDimension(MediaFileType.POSTER);
    if (posterSize.width > 0 && posterSize.height > 0) {
      lblPosterSize.setText(BUNDLE.getString("mediafiletype.poster") + " - " + posterSize.width + "x" + posterSize.height); //$NON-NLS-1$
    }
    else {
      lblPosterSize.setText(BUNDLE.getString("mediafiletype.poster")); //$NON-NLS-1$
    }
  }

  private void setFanart(TvShow tvShow) {
    lblTvShowBackground.clearImage();
    lblTvShowBackground.setImagePath(tvShow.getArtworkFilename(MediaFileType.FANART));
    Dimension fanartSize = tvShow.getArtworkDimension(MediaFileType.FANART);
    if (fanartSize.width > 0 && fanartSize.height > 0) {
      lblFanartSize.setText(BUNDLE.getString("mediafiletype.fanart") + " - " + fanartSize.width + "x" + fanartSize.height); //$NON-NLS-1$
    }
    else {
      lblFanartSize.setText(BUNDLE.getString("mediafiletype.fanart")); //$NON-NLS-1$
    }
  }

  private void setBanner(TvShow tvShow) {
    lblTvShowBanner.clearImage();
    lblTvShowBanner.setImagePath(tvShow.getArtworkFilename(MediaFileType.BANNER));
    Dimension bannerSize = tvShow.getArtworkDimension(MediaFileType.BANNER);
    if (bannerSize.width > 0 && bannerSize.height > 0) {
      lblBannerSize.setText(BUNDLE.getString("mediafiletype.banner") + " - " + bannerSize.width + "x" + bannerSize.height); //$NON-NLS-1$
    }
    else {
      lblBannerSize.setText(BUNDLE.getString("mediafiletype.banner")); //$NON-NLS-1$
    }
  }

  protected void initDataBindings() {
    BeanProperty<TvShowSelectionModel, String> tvShowSelectionModelBeanProperty = BeanProperty.create("selectedTvShow.title");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowSelectionModel, String, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel,
        tvShowSelectionModelBeanProperty, lblTvShowName, jLabelBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<TvShowSelectionModel, String> tvShowSelectionModelBeanProperty_1 = BeanProperty.create("selectedTvShow.plot");
    BeanProperty<JTextArea, String> JTextAreaBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowSelectionModel, String, JTextArea, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel,
        tvShowSelectionModelBeanProperty_1, tpOverview, JTextAreaBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<TvShowSelectionModel, Float> tvShowSelectionModelBeanProperty_2 = BeanProperty.create("selectedTvShow.rating.ratingNormalized");
    BeanProperty<StarRater, Float> starRaterBeanProperty = BeanProperty.create("rating");
    AutoBinding<TvShowSelectionModel, Float, StarRater, Float> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel,
        tvShowSelectionModelBeanProperty_2, panelRatingStars, starRaterBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<TvShowSelectionModel, Rating> tvShowSelectionModelBeanProperty_5 = BeanProperty.create("selectedTvShow.rating");
    AutoBinding<TvShowSelectionModel, Rating, JLabel, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel,
        tvShowSelectionModelBeanProperty_5, lblRating, jLabelBeanProperty);
    autoBinding_3.setConverter(new RatingConverter());
    autoBinding_3.bind();
    //
    BeanProperty<TvShowSelectionModel, Integer> tvShowSelectionModelBeanProperty_3 = BeanProperty.create("selectedTvShow.rating.votes");
    AutoBinding<TvShowSelectionModel, Integer, JLabel, String> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel,
        tvShowSelectionModelBeanProperty_3, lblVoteCount, jLabelBeanProperty);
    autoBinding_4.setConverter(new VoteCountConverter());
    autoBinding_4.bind();
    //
    BeanProperty<TvShowSelectionModel, String> tvShowSelectionModelBeanProperty_4 = BeanProperty.create("selectedTvShow.originalTitle");
    AutoBinding<TvShowSelectionModel, String, JLabel, String> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, tvShowSelectionModel,
        tvShowSelectionModelBeanProperty_4, lblOriginalTitle, jLabelBeanProperty);
    autoBinding_5.bind();
  }
}
