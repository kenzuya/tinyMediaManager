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

package org.tinymediamanager.ui.tvshows.panels.episode;

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
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.LinkTextArea;
import org.tinymediamanager.ui.panels.MediaInformationPanel;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeSelectionModel;

/**
 * The Class TvShowEpisodeMediaInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeMediaInformationPanel extends MediaInformationPanel {
  private final TvShowEpisodeSelectionModel selectionModel;

  public TvShowEpisodeMediaInformationPanel(TvShowEpisodeSelectionModel model) {
    super();
    panelMediaFiles.installTmmUILayoutStore("tvshows.episode");

    this.selectionModel = model;

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection of an episode and change of media files
      if (source.getClass() != TvShowEpisodeSelectionModel.class) {
        return;
      }

      if ("selectedTvShowEpisode".equals(property) || MEDIA_INFORMATION.equals(property) || MEDIA_FILES.equals(property)
          || MEDIA_SOURCE.equals(property)) {
        fillVideoStreamDetails();
        buildAudioStreamDetails();
        buildSubtitleStreamDetails();

        // this does sometimes not work. simply wrap it
        try {
          mediaFileEventList.getReadWriteLock().writeLock().lock();
          mediaFileEventList.clear();
          mediaFileEventList.addAll(selectionModel.getSelectedTvShowEpisode().getMediaFiles());
        }
        catch (Exception ignored) {
          // nothing to do here
        }
        finally {
          mediaFileEventList.getReadWriteLock().writeLock().unlock();
        }
        panelMediaFiles.adjustColumns();
      }
    };

    selectionModel.addPropertyChangeListener(propertyChangeListener);
    initDataBindings();
  }

  @Override
  protected void fillVideoStreamDetails() {
    TvShowEpisode tvShowEpisode = selectionModel.getSelectedTvShowEpisode();
    List<MediaFile> mediaFiles = tvShowEpisode.getMediaFiles(MediaFileType.VIDEO);

    if (mediaFiles.isEmpty()) {
      return;
    }

    MediaFile mediaFile = tvShowEpisode.getMainVideoFile();

    int runtime = 0;
    for (MediaFile mf : mediaFiles) {
      runtime += mf.getDuration();
    }

    if (runtime == 0) {
      lblRuntime.setText("");
    }
    else {
      int minutes = (runtime / 60) % 60;
      int hours = (runtime / (60 * 60)) % 24;
      int seconds = runtime % 60;
      lblRuntime.setText(String.format("%dh %02dm %02ds", hours, minutes, seconds));
    }

    chckbxWatched.setSelected(tvShowEpisode.isWatched());

    lblVideoCodec.setText(mediaFile.getVideoCodec());
    if (mediaFile.getAspectRatio() > 0) {
      lblVideoResolution.setText(String.format("%s (%.2f:1)", mediaFile.getVideoResolution(), mediaFile.getAspectRatio()));
    }
    else {
      lblVideoResolution.setText(mediaFile.getVideoResolution());
    }
    lblVideoBitrate.setText(mediaFile.getVideoBitRateInKbps());
    lblVideoBitDepth.setText(mediaFile.getBitDepthString());
    lblSource.setText(tvShowEpisode.getMediaSource().toString());
    lblFrameRate.setText(String.format("%.2f fps", mediaFile.getFrameRate()));
    lblOriginalFilename.setText(tvShowEpisode.getOriginalFilename());
    lblHdrFormat.setText(mediaFile.getHdrFormat());
  }

  @Override
  protected void buildAudioStreamDetails() {
    try {
      audioStreamEventList.getReadWriteLock().writeLock().lock();
      audioStreamEventList.clear();

      TvShowEpisode tvShowEpisode = selectionModel.getSelectedTvShowEpisode();
      List<MediaFile> mediaFiles = tvShowEpisode.getMediaFilesContainingAudioStreams();

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

      TvShowEpisode tvShowEpisode = selectionModel.getSelectedTvShowEpisode();
      List<MediaFile> mediaFiles = tvShowEpisode.getMediaFilesContainingSubtitles();

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
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty = BeanProperty.create("selectedTvShowEpisode.path");
    BeanProperty<LinkTextArea, String> linkTextAreaBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, LinkTextArea, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ,
        selectionModel, tvShowEpisodeSelectionModelBeanProperty, this.lblPath, linkTextAreaBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_1 = BeanProperty
        .create("selectedTvShowEpisode.dateAddedAsString");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel,
        tvShowEpisodeSelectionModelBeanProperty_1, this.lblDateAdded, jLabelBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, Boolean> tvShowEpisodeSelectionModelBeanProperty_2 = BeanProperty
        .create("selectedTvShowEpisode.watched");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<TvShowEpisodeSelectionModel, Boolean, JCheckBox, Boolean> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ,
        selectionModel, tvShowEpisodeSelectionModelBeanProperty_2, this.chckbxWatched, jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_3 = BeanProperty
        .create("selectedTvShowEpisode.originalFilename");
    BeanProperty<JLabel, String> jLabelBeanProperty_1 = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel,
        tvShowEpisodeSelectionModelBeanProperty_3, this.lblOriginalFilename, jLabelBeanProperty_1);
    autoBinding_3.bind();
  }
}
