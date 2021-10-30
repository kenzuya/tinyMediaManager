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
package org.tinymediamanager.thirdparty.yt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.tinymediamanager.scraper.http.Url;

import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoStreamDownload;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.downloader.response.ResponseImpl;

class DownloaderImpl implements Downloader {
  @Override
  public Response<String> downloadWebpage(RequestWebpage requestWebpage) {
    try {
      Url url = new Url(requestWebpage.getDownloadUrl());

      if (requestWebpage.getHeaders() != null) {
        for (Map.Entry<String, String> entry : requestWebpage.getHeaders().entrySet()) {
          url.addHeader(entry.getKey(), entry.getValue());
        }
      }

      String body;
      if (requestWebpage.getMaxRetries() != null) {
        body = new String(url.getBytesWithRetry(requestWebpage.getMaxRetries()), StandardCharsets.UTF_8);
      }
      else {
        body = new String(url.getBytes(), StandardCharsets.UTF_8);
      }
      return ResponseImpl.from(body);
    }
    catch (Exception e) {
      return ResponseImpl.error(e);
    }
  }

  @Override
  public Response<File> downloadVideoAsFile(RequestVideoFileDownload requestVideoFileDownload) {
    // we handle that internally
    return null;
  }

  @Override
  public Response<Void> downloadVideoAsStream(RequestVideoStreamDownload requestVideoStreamDownload) {
    // we handle that internally
    return null;
  }
}
