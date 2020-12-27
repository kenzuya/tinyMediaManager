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
package org.tinymediamanager.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import org.mp4parser.Container;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.FragmentedMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;

/**
 * The TMM Muxer class for muxing an audio and an video file in MP4 format
 *
 * @author Wolfgang Janes
 */
public class TmmMuxer {
  private final Path audioFile;
  private final Path videoFile;

  public TmmMuxer(Path audio, Path video) {
    audioFile = audio;
    videoFile = video;
  }

  /**
   * merge the video and audio stream into the given destination
   * 
   * @param destination
   *          the path to the desired destination
   * @throws IOException
   *           any {@link IOException } thrown while processing
   */
  public void mergeAudioVideo(Path destination) throws IOException {
    Movie video = null;
    Movie audio = null;
    Movie movie = null;

    try {
      video = MovieCreator.build(videoFile.toAbsolutePath().toString());
      audio = MovieCreator.build(audioFile.toAbsolutePath().toString());

      movie = new Movie();
      movie.addTrack(video.getTracks().get(0));
      movie.addTrack(audio.getTracks().get(0));

      Container mp4file = new FragmentedMp4Builder().build(movie);
      try (FileChannel fc = new FileOutputStream(destination.toFile()).getChannel()) {
        mp4file.writeContainer(fc);
      }
    }
    finally {
      // and close all tracks
      closeTracks(video);
      closeTracks(audio);
      closeTracks(movie);
    }
  }

  private void closeTracks(Movie movie) {
    if (movie == null) {
      return;
    }
    for (Track track : movie.getTracks()) {
      try {
        track.close();
      }
      catch (Exception e) {
        // ignore
      }
    }
  }
}
