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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;

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
    cmdList.add(Globals.settings.getMediaFramework());
    cmdList.add("-y");
    cmdList.add("-ss");
    cmdList.add(String.valueOf(second));
    cmdList.add("-i");
    cmdList.add(videoFile.toAbsolutePath().toString());
    cmdList.add("-frames:v");
    cmdList.add("1");
    cmdList.add(stillFile.toAbsolutePath().toString());

    return cmdList.toArray(new String[0]);
  }

  private static void executeCommand(String[] cmdline) throws IOException, InterruptedException {
    LOGGER.debug("Running command: {}", String.join(" ", cmdline));

    try {
      ProcessBuilder pb = new ProcessBuilder(cmdline).redirectErrorStream(true);
      final Process process = pb.start();

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); ByteArrayOutputStream errorStream = new ByteArrayOutputStream()) {

        new Thread(() -> {
          try {
            IOUtils.copy(process.getInputStream(), outputStream);
          }
          catch (IOException e) {
            LOGGER.debug("could not get output from the process", e);
          }
        }).start();

        new Thread(() -> {
          try {
            IOUtils.copy(process.getErrorStream(), errorStream);
          }
          catch (IOException e) {
            LOGGER.debug("could not get output from the process", e);
          }
        }).start();

        int processValue = process.waitFor();
        if (processValue != 0) {
          LOGGER.debug("error at FFmpeg: '{}", errorStream.toString(StandardCharsets.UTF_8));
          throw new IOException("could not create the still - code '" + processValue + "'");
        }
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
}
