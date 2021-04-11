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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

  private static List<String> createCommandforStill(Path videoFile, Path stillFile, int second) {
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

    return cmdList;
  }

  public static void muxVideoAndAudio(Path videoFile, Path audioFile, Path muxedFile) throws IOException, InterruptedException {
    executeCommand(createCommandforMux(videoFile, audioFile, muxedFile));
  }

  private static List<String> createCommandforMux(Path videoFile, Path audioFile, Path muxedFile) {
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

    return cmdList;
  }

  public static String scanDarkLevel(float position, Path videoFile) throws IOException, InterruptedException {
    return executeCommand(createCommandForScanDarkLevel(position, videoFile));
  }

  private static List<String> createCommandForScanDarkLevel(float position, Path videoFile) {
    List<String> cmdList = new ArrayList<>();
    cmdList.add(getFfmpegExecutable());
    cmdList.add("-hide_banner");
    cmdList.add("-an");
    cmdList.add("-dn");
    cmdList.add("-sn");
    cmdList.add("-ss");
    cmdList.add(Float.toString(position));
    cmdList.add("-i");
    cmdList.add(videoFile.toAbsolutePath().toString());
    cmdList.add("-vf");
    cmdList.add("signalstats,metadata=print");
    cmdList.add("-vframes");
    cmdList.add("1");
    cmdList.add("-f");
    cmdList.add("null");
    cmdList.add("pipe:1");
    return cmdList;
  }

  public static String scanSample(int start, int duration, int darkLevel, Path videoFile) throws IOException, InterruptedException {
    return executeCommand(createCommandForScanSample(start, duration, darkLevel, videoFile));
  }

  private static List<String> createCommandForScanSample(int start, int duration, int darkLevel, Path videoFile) {
    List<String> cmdList = new ArrayList<>();
    cmdList.add(getFfmpegExecutable());
    cmdList.add("-hide_banner");
    cmdList.add("-an");
    cmdList.add("-dn");
    cmdList.add("-sn");
    cmdList.add("-noaccurate_seek");
    cmdList.add("-t");
    cmdList.add(Integer.toString(duration));
    cmdList.add("-ss");
    cmdList.add(Integer.toString(start));
    cmdList.add("-i");
    cmdList.add(videoFile.toAbsolutePath().toString());
    cmdList.add("-vf");
    cmdList.add("cropdetect=" + Integer.toString(darkLevel) + ":2:0");
    cmdList.add("-f");
    cmdList.add("null");
    cmdList.add("pipe:1");
    return cmdList;
  }

  private static String executeCommand(List<String> cmdline) throws InterruptedException {
    LOGGER.debug("Running command: {}", String.join(" ", cmdline));

    try {
      ProcessBuilder pb = new ProcessBuilder(cmdline.toArray(new String[0])).redirectErrorStream(true);
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
        return outputStream.toString(StandardCharsets.UTF_8);
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

  public static boolean isAvailable() {
    FFmpegAddon fFmpegAddon = new FFmpegAddon();
    return ((Globals.settings.isUseInternalMediaFramework() && fFmpegAddon.isAvailable()) ||
            StringUtils.isNotEmpty(Globals.settings.getMediaFramework()));
  }
  private static String getFfmpegExecutable() {
    FFmpegAddon fFmpegAddon = new FFmpegAddon();

    if (Globals.settings.isUseInternalMediaFramework() && fFmpegAddon.isAvailable()) {
      return fFmpegAddon.getExecutablePath();
    }
    else {
      return Globals.settings.getMediaFramework();
    }
  }
}
