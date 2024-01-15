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
package org.tinymediamanager.core.threading;

/**
 * the class {@link ThreadUtils}
 */
public class ThreadUtils {
  private ThreadUtils() {
    throw new IllegalAccessError();
  }

  /**
   * try to sleep for x ms, swallow any exceptions
   * 
   * @param millis
   *          the amount of milliseconds to sleep
   */
  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException ignored) {
      // just ignore that
    }
  }
}
