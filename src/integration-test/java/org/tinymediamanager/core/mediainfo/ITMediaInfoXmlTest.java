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

package org.tinymediamanager.core.mediainfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tinymediamanager.core.MediaFileHelper.VIDEO_3D_HSBS;
import static org.tinymediamanager.core.MediaFileHelper.VIDEO_3D_SBS;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;

public class ITMediaInfoXmlTest extends BasicITest {

  @Test
  public void testVideofiles() {
    Path samplesFolder = getWorkFolder().resolve("samples");

    MediaFile mf = new MediaFile(samplesFolder.resolve("3D-FSBS.mkv"));
    mf.gatherMediaInformation();
    assertThat(mf.getVideo3DFormat()).isEqualTo(VIDEO_3D_SBS);

    mf = new MediaFile(samplesFolder.resolve("3D-HSBS.mkv"));
    mf.gatherMediaInformation();
    assertThat(mf.getVideo3DFormat()).isEqualTo(VIDEO_3D_HSBS);

    mf = new MediaFile(samplesFolder.resolve("3D-FTAB.mkv"));
    mf.gatherMediaInformation();
    assertThat(mf.getVideo3DFormat()).isEqualTo(MediaFileHelper.VIDEO_3D_TAB);

    mf = new MediaFile(samplesFolder.resolve("3D-HTAB.mkv"));
    mf.gatherMediaInformation();
    assertThat(mf.getVideo3DFormat()).isEqualTo(MediaFileHelper.VIDEO_3D_HTAB);
  }

  @Test
  public void testDvdFolder() {
    String path = "/media/data/movie_test_s/Spaceballs/VIDEO_TS";

    MediaFile mediaFile = new MediaFile(Paths.get(path));
    mediaFile.gatherMediaInformation();

    checkBaseMediaInfo(mediaFile);

    assertThat(mediaFile.getAudioStreams()).isNotEmpty();
    MediaFileAudioStream audioStream = mediaFile.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isGreaterThan(0);
    assertThat(audioStream.getBitrate()).isGreaterThan(0);
    assertThat(audioStream.getCodec()).isNotEmpty();
    assertThat(audioStream.getLanguage()).isNotEmpty();

    assertThat(mediaFile.getSubtitles()).isNotEmpty();
    MediaFileSubtitle subtitle = mediaFile.getSubtitles().get(0);
    assertThat(subtitle.getCodec()).isNotEmpty();
    assertThat(subtitle.getLanguage()).isNotEmpty();
  }

  @Test
  public void testDvdIso() {
    String path = "/media/data/movie_test_s/Sin.City/Sin.City.iso";

    MediaFile mediaFile = new MediaFile(Paths.get(path));
    mediaFile.gatherMediaInformation();

    checkBaseMediaInfo(mediaFile);

    assertThat(mediaFile.getAudioStreams()).isNotEmpty();
    MediaFileAudioStream audioStream = mediaFile.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isGreaterThan(0);
    assertThat(audioStream.getBitrate()).isGreaterThan(0);
    assertThat(audioStream.getCodec()).isNotEmpty();
    assertThat(audioStream.getLanguage()).isNotEmpty();

    assertThat(mediaFile.getSubtitles()).isNotEmpty();
    MediaFileSubtitle subtitle = mediaFile.getSubtitles().get(0);
    assertThat(subtitle.getCodec()).isNotEmpty();
    assertThat(subtitle.getLanguage()).isNotEmpty();
  }

  @Test
  public void test3dBlurayIso() {
    String path = "/media/data/movie_test_s/The.Secret.Life.of.Pets.2.2019.3D/pets2.3d-bluray.iso";

    MediaFile mediaFile = new MediaFile(Paths.get(path));
    mediaFile.gatherMediaInformation();

    checkBaseMediaInfo(mediaFile);

    // also check 3D flag
    assertThat(mediaFile.getVideo3DFormat()).isNotEmpty();

    assertThat(mediaFile.getAudioStreams()).isNotEmpty();
    MediaFileAudioStream audioStream = mediaFile.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isGreaterThan(0);
    assertThat(audioStream.getBitrate()).isGreaterThan(0);
    assertThat(audioStream.getCodec()).isNotEmpty();
    // assertThat(audioStream.getLanguage()).isNotEmpty(); not possible atm

    assertThat(mediaFile.getSubtitles()).isNotEmpty();
    MediaFileSubtitle subtitle = mediaFile.getSubtitles().get(0);
    assertThat(subtitle.getCodec()).isNotEmpty();
    // assertThat(subtitle.getLanguage()).isNotEmpty(); not possible atm
  }

  @Test
  public void testBlurayFolder() {
    String path = "/media/data/movie_test_s/Zogg.2018/BDMV";

    MediaFile mediaFile = new MediaFile(Paths.get(path));
    mediaFile.gatherMediaInformation();

    checkBaseMediaInfo(mediaFile);

    assertThat(mediaFile.getAudioStreams()).isNotEmpty();
    MediaFileAudioStream audioStream = mediaFile.getAudioStreams().get(1); // no bitrate in steam 0
    assertThat(audioStream.getAudioChannels()).isGreaterThan(0);
    assertThat(audioStream.getBitrate()).isGreaterThan(0);
    assertThat(audioStream.getCodec()).isNotEmpty();
    assertThat(audioStream.getLanguage()).isNotEmpty();

    assertThat(mediaFile.getSubtitles()).isNotEmpty();
    MediaFileSubtitle subtitle = mediaFile.getSubtitles().get(0);
    assertThat(subtitle.getCodec()).isNotEmpty();
    assertThat(subtitle.getLanguage()).isNotEmpty();
  }

  @Test
  public void testHdDvdFolder() {
    String path = "/media/data/movie_test_s/Michael.Walz/HVDVD_TS";

    MediaFile mediaFile = new MediaFile(Paths.get(path));
    mediaFile.gatherMediaInformation();

    // extra checks here since we do not have a real hd-dvd iso for testing
    assertThat(mediaFile.getFilesize()).isGreaterThan(0);
    assertThat(mediaFile.getDuration()).isGreaterThan(0);

    assertThat(mediaFile.getVideoWidth()).isGreaterThan(0);
    assertThat(mediaFile.getVideoHeight()).isGreaterThan(0);
    assertThat(mediaFile.getVideoCodec()).isNotEmpty();
    assertThat(mediaFile.getOverallBitRate()).isGreaterThan(0);

    assertThat(mediaFile.getAudioStreams()).isNotEmpty();
    MediaFileAudioStream audioStream = mediaFile.getAudioStreams().get(0);
    assertThat(audioStream.getAudioChannels()).isGreaterThan(0);
    assertThat(audioStream.getBitrate()).isGreaterThan(0);
    assertThat(audioStream.getCodec()).isNotEmpty();
    // assertThat(audioStream.getLanguage()).isNotEmpty();

    // assertThat(mediaFile.getSubtitles()).isNotEmpty();
    // MediaFileSubtitle subtitle = mediaFile.getSubtitles().get(0);
    // assertThat(subtitle.getCodec()).isNotEmpty();
    // assertThat(subtitle.getLanguage()).isNotEmpty();
  }

  private void checkBaseMediaInfo(MediaFile mediaFile) {
    assertThat(mediaFile.getFilesize()).isGreaterThan(0);
    assertThat(mediaFile.getDuration()).isGreaterThan(0);

    assertThat(mediaFile.getVideoWidth()).isGreaterThan(0);
    assertThat(mediaFile.getVideoHeight()).isGreaterThan(0);
    assertThat(mediaFile.getVideoCodec()).isNotEmpty();
    assertThat(mediaFile.getOverallBitRate()).isGreaterThan(0);
  }
}
