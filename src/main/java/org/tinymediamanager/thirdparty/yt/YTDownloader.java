/*
 * Copyright 2012 - 2023 Manuel Laggner
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
package org.tinymediamanager.thirdparty.yt;

import org.tinymediamanager.license.TmmFeature;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeDownloader;

/**
 * the class {@link YTDownloader} is used to pass the YT download over our HTTP client
 * 
 * @author Manuel Laggner
 */
public class YTDownloader extends YoutubeDownloader implements TmmFeature {
  public YTDownloader() throws ScrapeException {
    super(new Config.Builder().build(), new DownloaderImpl());
  }
}
