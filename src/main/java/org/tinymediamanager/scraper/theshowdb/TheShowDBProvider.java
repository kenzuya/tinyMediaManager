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
package org.tinymediamanager.scraper.theshowdb;

import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.theshowdb.services.TheShowDBController;

/**
 * The Class @{@link TheShowDBProvider} is a metadata provider for the website theshowdb.com
 */
abstract class TheShowDBProvider {

  private static final String     ID = "theshowdb";
  private final MediaProviderInfo providerInfo;
  protected TheShowDBController   controller;

  TheShowDBProvider() {
    providerInfo = createMediaProviderInfo();
    controller = new TheShowDBController(false);
  }

  protected abstract String getSubId();

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "TheShowDB",
        "<html><h3>TheShowDB</h3><br />An open, crowd-sourced database of TV Shows from around the world, focusing on artwork</html>",
        TheShowDBProvider.class.getResource("/org/tinymediamanager/scraper/theshowdb.png"));

    info.getConfig().addText("apiKey", "1", true);
    info.getConfig().load();

    return info;
  }

  /**
   * @return the entered API Key
   */
  protected String getApiKey() {
    return providerInfo.getConfig().getValue("apiKey");
  }

  protected MediaProviderInfo getMediaProviderInfo() {
    return createMediaProviderInfo();
  }

}
