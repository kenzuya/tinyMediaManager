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

package org.tinymediamanager.ui.tvshows.panels.episode;

import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.panels.MediaInformationPanel;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeSelectionModel;

/**
 * The Class TvShowEpisodeMediaInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeMediaInformationPanel extends MediaInformationPanel {
  private static final long           serialVersionUID = 2513029074142934502L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  private TvShowEpisodeSelectionModel selectionModel;

  public TvShowEpisodeMediaInformationPanel(TvShowEpisodeSelectionModel model) {
    super();

    this.selectionModel = model;

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection of a movie and change of media files
      if ((source.getClass() == TvShowEpisodeSelectionModel.class && "selectedTvShowEpisode".equals(property))
          || MEDIA_INFORMATION.equals(property)) {
        fillVideoStreamDetails();
        buildAudioStreamDetails();
        buildSubtitleStreamDetails();
      }
      if ((source.getClass() == TvShowEpisodeSelectionModel.class && "selectedTvShowEpisode".equals(property))
          || (source.getClass() == TvShowEpisode.class && MEDIA_FILES.equals(property))) {
        // this does sometimes not work. simply wrap it
        try {
          mediaFileEventList.getReadWriteLock().writeLock().lock();
          mediaFileEventList.clear();
          mediaFileEventList.addAll(selectionModel.getSelectedTvShowEpisode().getMediaFiles());
        }
        catch (Exception ignored) {
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
  protected MediaEntity getMediaEntity() {
    return selectionModel.getSelectedTvShowEpisode();
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
      int minutes = (int) (runtime / 60) % 60;
      int hours = (int) (runtime / (60 * 60)) % 24;
      lblRuntime.setText(hours + "h " + String.format("%02d", minutes) + "m");
    }

    chckbxWatched.setSelected(tvShowEpisode.isWatched());

    lblVideoCodec.setText(mediaFile.getVideoCodec());
    lblVideoResolution.setText(mediaFile.getVideoResolution());
    lblVideoBitrate.setText(mediaFile.getBiteRateInKbps());
    lblVideoBitDepth.setText(mediaFile.getBitDepthString());
    lblSource.setText(tvShowEpisode.getMediaSource().toString());
    lblFrameRate.setText(String.format("%.2f fps", mediaFile.getFrameRate()));
  }

  @Override
  protected void buildAudioStreamDetails() {
    audioStreamEventList.clear();

    TvShowEpisode tvShowEpisode = selectionModel.getSelectedTvShowEpisode();
    List<MediaFile> mediaFiles = tvShowEpisode.getMediaFilesContainingAudioStreams();

    for (MediaFile mediaFile : mediaFiles) {
      for (int i = 0; i < mediaFile.getAudioStreams().size(); i++) {
        MediaFileAudioStream audioStream = mediaFile.getAudioStreams().get(i);

        AudioStreamContainer container = new AudioStreamContainer();
        container.audioStream = audioStream;

        if (mediaFile.getType() == MediaFileType.VIDEO) {
          container.source = BUNDLE.getString("metatag.internal"); //$NON-NLS-1$
        }
        else {
          container.source = BUNDLE.getString("metatag.external"); //$NON-NLS-1$
        }

        audioStreamEventList.add(container);
      }
    }
  }

  @Override
  protected void buildSubtitleStreamDetails() {
    subtitleEventList.clear();

    TvShowEpisode tvShowEpisode = selectionModel.getSelectedTvShowEpisode();
    List<MediaFile> mediaFiles = tvShowEpisode.getMediaFilesContainingSubtitles();

    for (MediaFile mediaFile : mediaFiles) {
      for (int i = 0; i < mediaFile.getSubtitles().size(); i++) {
        MediaFileSubtitle subtitle = mediaFile.getSubtitles().get(i);

        SubtitleContainer container = new SubtitleContainer();
        container.subtitle = subtitle;

        if (mediaFile.getType() == MediaFileType.VIDEO) {
          container.source = BUNDLE.getString("metatag.internal"); //$NON-NLS-1$
        }
        else {
          container.source = BUNDLE.getString("metatag.external"); //$NON-NLS-1$
        }

        subtitleEventList.add(container);
      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty = BeanProperty.create("selectedTvShowEpisode.path");
    BeanProperty<LinkLabel, String> linkLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, LinkLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, selectionModel,
        tvShowEpisodeSelectionModelBeanProperty, this.lblPath, linkLabelBeanProperty);
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
  }
}
