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
package org.tinymediamanager.thirdparty;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.addon.FFmpegAddon;

/**
 * the class {@link FFmpeg} is used to access FFmpeg
 * 
 * @author Manuel Laggner/Wolfgang Janes
 */
public class FFmpeg {
  private static final Logger LOGGER = LoggerFactory.getLogger(FFmpeg.class);

  private FFmpeg() {
    throw new IllegalAccessError();
  }

  /**
   * create a still of the given video file to the given path. The still is being taken at the given second of the video file
   * 
   * @param videoFile
   *          the video file to extract the still from
   * @param stillFile
   *          the destination file
   * @param second
   *          the second of the video file to get the still from
   * @throws IOException
   *           any {@link IOException} occurred
   * @throws InterruptedException
   *           being thrown if the thread has been interrupted
   */
  public static void createStill(Path videoFile, Path stillFile, int second) throws IOException, InterruptedException {
    executeCommand(createCommandforStill(videoFile, stillFile, second));
  }

  private static String[] createCommandforStill(Path videoFile, Path stillFile, int second) {
    List<String> cmdList = new ArrayList<>();
    cmdList.add(getFfmpegExecutable());
    cmdList.add("-y");
    cmdList.add("-ss");
    cmdList.add(String.valueOf(second));
    cmdList.add("-i");
    cmdList.add(videoFile.toAbsolutePath().toString());
    cmdList.add("-frames:v");
    cmdList.add("1");
    cmdList.add("-q:v");
    cmdList.add("2");
    cmdList.add(stillFile.toAbsolutePath().toString());

    return cmdList.toArray(new String[0]);
  }

  public static void muxVideoAndAudio(Path videoFile, Path audioFile, Path muxedFile) throws IOException, InterruptedException {
    executeCommand(createCommandforMux(videoFile, audioFile, muxedFile));
  }

  private static String[] createCommandforMux(Path videoFile, Path audioFile, Path muxedFile) {
    List<String> cmdList = new ArrayList<>();
    cmdList.add(getFfmpegExecutable());
    cmdList.add("-y");
    cmdList.add("-i");
    cmdList.add(videoFile.toAbsolutePath().toString());
    cmdList.add("-i");
    cmdList.add(audioFile.toAbsolutePath().toString());
    cmdList.add("-c");
    cmdList.add("copy");
    cmdList.add(muxedFile.toAbsolutePath().toString());

    return cmdList.toArray(new String[0]);
  }

  public static String getMetaData(Path videoFile) throws IOException, InterruptedException {
    return executeCommand(createCommandForMetaData(videoFile));
  }

  private static String[] createCommandForMetaData(Path videoFile) {
    List<String> cmdList = new ArrayList<>();
    cmdList.add(getFfprobeExecutable());
    cmdList.add("-v");
    cmdList.add("error");
    cmdList.add("-select_streams");
    cmdList.add("v:0");
    cmdList.add("-show_entries");
    cmdList.add("stream=width,height,sample_aspect_ratio:format=duration");
    cmdList.add("-of");
    cmdList.add("default=nw=1:nk=1");
    cmdList.add("-i");
    cmdList.add(videoFile.toAbsolutePath().toString());

    return cmdList.toArray(new String[0]);
  }

  private static String executeCommand(String[] cmdline) throws IOException, InterruptedException {
    LOGGER.debug("Running command: {}", String.join(" ", cmdline));

    try {
      ProcessBuilder pb = new ProcessBuilder(cmdline).redirectErrorStream(true);
      final Process process = pb.start();

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        new Thread(() -> {
          try {
            IOUtils.copy(process.getInputStream(), outputStream);
          }
          catch (IOException e) {
            LOGGER.debug("could not get output from the process", e);
          }
        }).start();

        int processValue = process.waitFor();
        if (processValue != 0) {
          LOGGER.debug("error at FFmpeg: '{}", outputStream.toString(StandardCharsets.UTF_8));
          throw new IOException("could not create the still - code '" + processValue + "'");
        }
        return outputStream.toString(Charset.defaultCharset());
      }
      finally {
        if (process != null) {
          process.destroy();
          // Process must be destroyed before closing streams, can't use try-with-resources,
          // as resources are closing when leaving try block, before finally
          IOUtils.close(process.getErrorStream());
        }
      }
    }
    catch (IOException e) {
      LOGGER.error("could not run FFmpeg", e);
      throw new RuntimeException("Failed to start process.", e);
    }
  }

  private static String getFfmpegExecutable() {
    String executable = "ffmpeg";
    if (SystemUtils.IS_OS_WINDOWS) {
      executable = "ffmpeg.exe";
    }
    return getFfmpegPath() + File.separator + executable;
  }

  private static String getFfprobeExecutable() {
    String executable = "ffprobe";
    if (SystemUtils.IS_OS_WINDOWS) {
      executable = "ffprobe.exe";
    }
    return getFfmpegPath() + File.separator + executable;
  }

  private static String getFfmpegPath() {
    FFmpegAddon fFmpegAddon = new FFmpegAddon();

    if (Globals.settings.isUseInternalMediaFramework() && fFmpegAddon.isAvailable()) {
      return fFmpegAddon.getExecutablePath();
    }
    else {
      return Globals.settings.getMediaFramework();
    }
  }
}
