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

package org.tinymediamanager.scraper.opensubtitles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.util.RingBuffer;

/**
 * The class {@link OpenSubtitlesConnectionCounter} is a helper class to count the connection and throttle if needed
 *
 * @author Manuel Laggner
 */
class OpenSubtitlesConnectionCounter {
  private static final Logger           LOGGER   = LoggerFactory.getLogger(OpenSubtitlesConnectionCounter.class);
  private static final RingBuffer<Long> COUNTER  = new RingBuffer<>(40);
  private static final int              DURATION = 10500;

  private OpenSubtitlesConnectionCounter() {
    // hide constructor for utility classes
  }

  static synchronized void trackConnections() throws InterruptedException {
    long currentTime = System.currentTimeMillis();
    if (COUNTER.count() == COUNTER.maxSize()) {
      long oldestConnection = COUNTER.getTailItem();
      if (oldestConnection > (currentTime - DURATION)) {
        LOGGER.debug("connection limit reached, throttling {}", COUNTER);
        do {
          OpenSubtitlesConnectionCounter.class.wait(DURATION - (currentTime - oldestConnection));
          currentTime = System.currentTimeMillis();
        } while (oldestConnection > (currentTime - DURATION));
      }
    }

    currentTime = System.currentTimeMillis();
    COUNTER.add(currentTime);
  }
}
