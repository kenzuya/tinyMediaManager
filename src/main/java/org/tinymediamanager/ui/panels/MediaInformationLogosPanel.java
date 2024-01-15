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
package org.tinymediamanager.ui.panels;

import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.IMediaInformation;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.ui.images.AspectRatioIcon;
import org.tinymediamanager.ui.images.AudioChannelsIcon;
import org.tinymediamanager.ui.images.GenericVideoCodecIcon;
import org.tinymediamanager.ui.images.MediaInfoIcon;
import org.tinymediamanager.ui.images.VideoFormatIcon;

import net.miginfocom.swing.MigLayout;

/**
 * The class MediaInformationLogosPanel is used to display all media info related logos
 */
public class MediaInformationLogosPanel extends JPanel {
  private IMediaInformation mediaInformationSource;

  private final JLabel      lblVideoFormat;
  private final JLabel      lblAspectRatio;
  private final JLabel      lblVideoCodec;
  private final JLabel      lblVideo3d;
  private final JLabel      lblVideoHdr1;
  private final JLabel      lblVideoHdr2;
  private final JLabel      lblVideoHdr3;
  private final JLabel      lblAudioCodec;
  private final JLabel      lblAudioChannels;
  private final JLabel      lblSource;
  private final JLabel      lblHfr;

  public MediaInformationLogosPanel() {
    setLayout(new MigLayout("hidemode 3", "[][][][][][10lp][][][10lp][][]", "[]"));

    lblVideoFormat = new JLabel();
    add(lblVideoFormat, "cell 0 0, bottom");
    lblAspectRatio = new JLabel();
    add(lblAspectRatio, "cell 1 0, bottom");
    lblVideoCodec = new JLabel();
    add(lblVideoCodec, "cell 2 0, bottom");
    lblVideo3d = new JLabel();
    add(lblVideo3d, "cell 3 0, bottom");
    lblVideoHdr1 = new JLabel();
    add(lblVideoHdr1, "cell 4 0, bottom");
    lblVideoHdr2 = new JLabel();
    add(lblVideoHdr2, "cell 4 0, bottom");
    lblVideoHdr3 = new JLabel();
    add(lblVideoHdr1, "cell 4 0, bottom");

    lblAudioChannels = new JLabel();
    add(lblAudioChannels, "cell 6 0, bottom");
    lblAudioCodec = new JLabel();
    add(lblAudioCodec, "cell 7 0, bottom");

    lblSource = new JLabel();
    add(lblSource, "cell 9 0, bottom");

    lblHfr = new JLabel();
    add(lblHfr, "cell 10 0, bottom");

    // listen to UI changes to re-set the icons
  }

  @Override
  public void updateUI() {
    super.updateUI();

    if (mediaInformationSource != null) {
      updateIcons();
    }
  }

  public void setMediaInformationSource(IMediaInformation source) {
    this.mediaInformationSource = source;
    updateIcons();
  }

  private void updateIcons() {
    setIcon(lblVideoFormat, getVideoFormatIcon());
    setIcon(lblAspectRatio, getAspectRatioIcon());
    setIcon(lblVideoCodec, getVideoCodecIcon());
    setIcon(lblVideo3d, getVideo3dIcon());
    setIcon(lblVideoHdr1, getVideoHdrIcon(0));
    setIcon(lblVideoHdr2, getVideoHdrIcon(1));
    setIcon(lblVideoHdr3, getVideoHdrIcon(2));
    setIcon(lblAudioCodec, getAudioCodecIcon());
    setIcon(lblAudioChannels, getAudioChannelsIcon());
    setIcon(lblSource, getSourceIcon());
    setIcon(lblHfr, getHfrIcon());
  }

  private void setIcon(JLabel label, Icon icon) {
    label.setIcon(icon);
    if (icon != null) {
      label.setVisible(true);
    }
    else {
      label.setVisible(false);
    }
  }

  /**
   * get the right icon for the video format
   *
   * @return the icon or null
   */
  private Icon getVideoFormatIcon() {
    String videoFormat = Utils.cleanFilename(mediaInformationSource.getMediaInfoVideoFormat());

    // a) return null if the Format is empty
    if (StringUtils.isBlank(videoFormat)) {
      return null;
    }

    try {
      return new VideoFormatIcon(videoFormat);
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * get the right icon for the aspect ratio
   *
   * @return the icon or null
   */
  private Icon getAspectRatioIcon() {
    float aspectRatio = mediaInformationSource.getMediaInfoAspectRatio();
    if (aspectRatio == 0) {
      return null;
    }

    try {
      return new AspectRatioIcon(String.format(Locale.US, "%.2f", aspectRatio));
    }
    catch (Exception ignored) {
      // ignore
    }
    return null;
  }

  /**
   * get the right icon for the video codec
   *
   * @return the icon or null
   */
  private Icon getVideoCodecIcon() {
    String videoCodec = Utils.cleanFilename(mediaInformationSource.getMediaInfoVideoCodec());

    // a) return null if the Format is empty
    if (StringUtils.isBlank(videoCodec)) {
      return null;
    }

    try {
      return new MediaInfoIcon("video/codec/" + videoCodec.toLowerCase(Locale.ROOT) + ".svg");
    }
    catch (Exception e) {
      try {
        // nothing found so far - maybe it is just a "generic" codec; try to return the generic one
        return new GenericVideoCodecIcon(videoCodec);
      }
      catch (Exception e1) {
        return null;
      }
    }
  }

  /**
   * get the right icon for the audio codec
   *
   * @return the icon or null
   */
  private Icon getAudioCodecIcon() {
    String audioCodec = Utils.cleanFilename(mediaInformationSource.getMediaInfoAudioCodec());

    // a) return null if the codec is empty
    if (StringUtils.isBlank(audioCodec)) {
      return null;
    }

    try {
      return new MediaInfoIcon("audio/codec/" + audioCodec.toLowerCase(Locale.ROOT) + ".svg");
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * get the right icon for the audio channels
   *
   * @return the icon or null
   */
  private Icon getAudioChannelsIcon() {
    int audioChannelsInt;
    try {
      audioChannelsInt = Integer.parseInt(mediaInformationSource.getMediaInfoAudioChannels().replace("ch", ""));
    }
    catch (NumberFormatException ignored) {
      return null;
    }

    if (audioChannelsInt == 0) {
      return null;
    }

    String text = MediaFileHelper.audioChannelInDotNotation(audioChannelsInt);
    try {
      return new AudioChannelsIcon(text);
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * get the right icon for 3D
   *
   * @return the icon or null
   */
  private Icon getVideo3dIcon() {
    // a) return null if the video is not in 3D
    if (!mediaInformationSource.isVideoIn3D()) {
      return null;
    }

    try {
      return new MediaInfoIcon("video/3d.svg");
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * get the right icon for HDR
   *
   * @return the icon or null
   */
  private Icon getVideoHdrIcon(int num) {
    String hdrFormat = mediaInformationSource.getVideoHDRFormat();
    // a) return null if the video is not in 3D
    if (StringUtils.isBlank(hdrFormat)) {
      return null;
    }

    String[] split = hdrFormat.split(", ");
    int i = 0;
    for (String hdr : split) {
      if (num == i) {
        try {
          return new MediaInfoIcon("video/" + hdr.toLowerCase(Locale.ROOT).replace(" ", "_") + ".svg");
        }
        catch (Exception e) {
          try {
            return new GenericVideoCodecIcon(hdr);
          }
          catch (Exception e1) {
            return null;
          }
        }
      }
      i++;
    }

    return null;
  }

  /**
   * get the media source
   *
   * @return the icon or null
   */
  private Icon getSourceIcon() {
    MediaSource source = mediaInformationSource.getMediaInfoSource();

    // a) return null if the source is empty
    if (source == MediaSource.UNKNOWN) {
      return null;
    }

    try {
      return new MediaInfoIcon("source/" + source.name().toLowerCase(Locale.ROOT) + ".svg");
    }
    catch (Exception e) {
      return null;
    }
  }

  private Icon getHfrIcon() {
    double framerate = mediaInformationSource.getMediaInfoFrameRate();
    try {
      return (framerate >= 48.0) ? new MediaInfoIcon("video/hfr.svg") : null;
    }
    catch (Exception e) {
      return null;
    }
  }
}
