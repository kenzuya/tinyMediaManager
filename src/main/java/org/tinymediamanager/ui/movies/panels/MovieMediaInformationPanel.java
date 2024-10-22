/*
 * Copyright 2012 - 2024 Manuel Laggner
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
package org.tinymediamanager.ui.movies.panels;

import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;
import static org.tinymediamanager.core.Constants.MEDIA_SOURCE;

import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.panels.MediaInformationPanel;

/**
 * the class {@link MovieMediaInformationPanel} holds all media information data for movies
 * 
 * @author Manuel Laggner
 */
public class MovieMediaInformationPanel extends MediaInformationPanel {
  private final MovieSelectionModel movieSelectionModel;

  /**
   * Instantiates a new movie media information panel.
   * 
   * @param model
   *          the model
   */
  public MovieMediaInformationPanel(MovieSelectionModel model) {
    super();
    this.movieSelectionModel = model;
    panelMediaFiles.installTmmUILayoutStore(getName() + ".table");

    initDataBindings();

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != MovieSelectionModel.class) {
        return;
      }

      // react on selection of a movie and change of media files
      if ("selectedMovie".equals(property) || MEDIA_INFORMATION.equals(property) || MEDIA_SOURCE.equals(property) || MEDIA_FILES.equals(property)) {
        fillVideoStreamDetails();
        buildAudioStreamDetails();
        buildSubtitleStreamDetails();

        // this does sometimes not work. simply wrap it
        try {
          mediaFileEventList.getReadWriteLock().writeLock().lock();
          mediaFileEventList.clear();
          mediaFileEventList.addAll(movieSelectionModel.getSelectedMovie().getMediaFiles());
        }
        catch (Exception ignored) {
          // ignored
        }
        finally {
          mediaFileEventList.getReadWriteLock().writeLock().unlock();
          panelMediaFiles.adjustColumns();
        }
      }
    };

    movieSelectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  @Override
  public String getName() {
    return "movie.mediainformation";
  }

  @Override
  protected void fillVideoStreamDetails() {
    Movie movie = movieSelectionModel.getSelectedMovie();
    List<MediaFile> mediaFiles = movie.getMediaFiles(MediaFileType.VIDEO);

    if (mediaFiles.isEmpty()) {
      return;
    }

    MediaFile mediaFile = movie.getMainVideoFile();

    int runtime = movie.getRuntimeFromMediaFilesInSeconds();

    if (runtime == 0) {
      lblRuntime.setText("");
    }
    else {
      int minutes = (runtime / 60) % 60;
      int hours = (runtime / (60 * 60)) % 24;
      int seconds = runtime % 60;
      lblRuntime.setText(String.format("%dh %02dm %02ds", hours, minutes, seconds));
    }

    chckbxWatched.setSelected(movie.isWatched());

    lblVideoCodec.setText(mediaFile.getVideoCodec());
    if (mediaFile.getAspectRatio() > 0) {
      lblVideoResolution.setText(String.format("%s (%.2f:1)", mediaFile.getVideoResolution(), mediaFile.getAspectRatio()));
    }
    else {
      lblVideoResolution.setText(mediaFile.getVideoResolution());
    }
    lblVideoBitrate.setText(mediaFile.getVideoBitRateInKbps());
    lblVideoBitDepth.setText(mediaFile.getBitDepthString());
    lblSource.setText(movie.getMediaSource().toString());
    lblFrameRate.setText(String.format("%.2f fps", mediaFile.getFrameRate()));
    lblOriginalFilename.setText(movie.getOriginalFilename());
    lblHdrFormat.setText(mediaFile.getHdrFormat());
  }

  @Override
  protected void buildAudioStreamDetails() {
    try {
      audioStreamEventList.getReadWriteLock().writeLock().lock();
      audioStreamEventList.clear();

      Movie movie = movieSelectionModel.getSelectedMovie();
      List<MediaFile> mediaFiles = movie.getMediaFilesContainingAudioStreams();

      for (MediaFile mediaFile : mediaFiles) {
        for (int i = 0; i < mediaFile.getAudioStreams().size(); i++) {
          MediaFileAudioStream audioStream = mediaFile.getAudioStreams().get(i);

          AudioStreamContainer container = new AudioStreamContainer();
          container.audioStream = audioStream;

          if (mediaFile.getType() == MediaFileType.VIDEO) {
            container.source = TmmResourceBundle.getString("metatag.internal");
          }
          else {
            container.source = TmmResourceBundle.getString("metatag.external");
          }

          audioStreamEventList.add(container);
        }
      }
    }
    catch (Exception ignored) {
      // ignored
    }
    finally {
      audioStreamEventList.getReadWriteLock().writeLock().unlock();
      tableAudioStreams.adjustColumnPreferredWidths(6);
    }
  }

  @Override
  protected void buildSubtitleStreamDetails() {
    try {
      subtitleEventList.getReadWriteLock().writeLock().lock();
      subtitleEventList.clear();

      Movie movie = movieSelectionModel.getSelectedMovie();
      List<MediaFile> mediaFiles = movie.getMediaFilesContainingSubtitles();

      for (MediaFile mediaFile : mediaFiles) {
        for (int i = 0; i < mediaFile.getSubtitles().size(); i++) {
          MediaFileSubtitle subtitle = mediaFile.getSubtitles().get(i);

          SubtitleContainer container = new SubtitleContainer();
          container.subtitle = subtitle;

          if (mediaFile.getType() == MediaFileType.VIDEO) {
            container.source = TmmResourceBundle.getString("metatag.internal");
          }
          else {
            container.source = TmmResourceBundle.getString("metatag.external");
          }

          subtitleEventList.add(container);
        }
      }
    }
    catch (Exception ignored) {
      // ignored
    }
    finally {
      subtitleEventList.getReadWriteLock().writeLock().unlock();
      tableSubtitles.adjustColumnPreferredWidths(6);
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSelectionModel, Boolean> movieSelectionModelBeanProperty = BeanProperty.create("selectedMovie.watched");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSelectionModel, Boolean, JCheckBox, Boolean> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel,
        movieSelectionModelBeanProperty, chckbxWatched, jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<MovieSelectionModel, String> movieSelectionModelBeanProperty_1 = BeanProperty.create("selectedMovie.dateAddedAsString");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<MovieSelectionModel, String, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel,
        movieSelectionModelBeanProperty_1, lblDateAdded, jLabelBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<MovieSelectionModel, String> movieSelectionModelBeanProperty_2 = BeanProperty.create("selectedMovie.path");
    BeanProperty<LinkTextArea, String> linkTextAreaBeanProperty = BeanProperty.create("text");
    AutoBinding<MovieSelectionModel, String, LinkTextArea, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ,
        movieSelectionModel, movieSelectionModelBeanProperty_2, lblPath, linkTextAreaBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<MovieSelectionModel, String> movieSelectionModelBeanProperty_3 = BeanProperty.create("selectedMovie.originalFilename");
    BeanProperty<JLabel, String> jLabelBeanProperty_2 = BeanProperty.create("text");
    AutoBinding<MovieSelectionModel, String, JLabel, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, movieSelectionModel,
        movieSelectionModelBeanProperty_3, lblOriginalFilename, jLabelBeanProperty_2);
    autoBinding_3.bind();
  }
}
