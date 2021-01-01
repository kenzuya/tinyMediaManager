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

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.tinymediamanager.Globals;

/**
 * the class {@link FFmpeg} is used to access FFmpeg
 * 
 * @author Manuel Laggner/Wolfgang Janes
 */
public class FFmpeg {

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
    List<String> cmdListLinux = new LinkedList<>();

    cmdListLinux.add(Globals.settings.getMediaFramework());
    cmdListLinux.add("-y");
    cmdListLinux.add("-ss");
    cmdListLinux.add(String.valueOf(second));
    cmdListLinux.add("-i");
    cmdListLinux.add(videoFile.toAbsolutePath().toString());
    cmdListLinux.add("-frames:v");
    cmdListLinux.add("1");
    cmdListLinux.add(stillFile.toAbsolutePath().toString());

    return cmdListLinux.toArray(new String[0]);
  }

  private static void executeCommand(String[] cmdline) throws IOException, InterruptedException {
    Process p = Runtime.getRuntime().exec(cmdline);
    int processValue = p.waitFor();
    if (processValue != 0) {
      throw new IOException("could not create the still");
    }
  }
}
