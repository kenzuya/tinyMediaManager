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
package org.tinymediamanager.scraper.http;

import org.apache.commons.lang3.StringUtils;

/**
 * This class is holding the proxy settings. After changing the proxy settings, a new HTTP-Client is being created automatically
 * 
 * @author Manuel Laggner
 * @since 1.0
 */
public class ProxySettings {
  public static final ProxySettings INSTANCE = new ProxySettings();

  private String                    host     = "";
  private int                       port     = 0;
  private String                    username = "";
  private String                    password = "";

  /**
   * Set the actual proxy settings
   * 
   * @param host
   *          the proy host
   * @param port
   *          the proxy port
   * @param username
   *          the proxy username (if needed)
   * @param password
   *          the proxy password (if needed)
   */
  public static void setProxySettings(String host, int port, String username, String password) {
    INSTANCE.host = host == null ? "" : host;
    INSTANCE.port = port;
    INSTANCE.username = username == null ? "" : username;
    INSTANCE.password = password == null ? "" : password;

    TmmHttpClient.recreateHttpClient();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public boolean useProxy() {
    return StringUtils.isNotBlank(INSTANCE.host);
  }
}
