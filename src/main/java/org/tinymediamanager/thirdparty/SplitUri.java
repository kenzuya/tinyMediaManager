/*
 * Copyright 2012 - 2022 Manuel Laggner
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

import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.thirdparty.upnp.Upnp;

/**
 * Splits an URI (Kodi datasource, file, UNC, ...) in it's parameters<br>
 * <br>
 * <b>Type:</b><br>
 * LOCAL for local datasources<br>
 * UPNP for UPNP ones<br>
 * SMB/NFS/... Url schema for other remotes
 * 
 * @author Myron Boyle
 *
 */
public class SplitUri {
  private static final Logger       LOGGER     = LoggerFactory.getLogger(SplitUri.class);

  public String                     file       = "";
  public String                     datasource = "";
  public String                     label      = "";
  public String                     type       = "";
  public String                     ip         = "";
  public String                     hostname   = "";

  private final Map<String, String> lookup     = new HashMap<>();

  @SuppressWarnings("unused")
  private SplitUri() {
  }

  public SplitUri(String ds, String file) {
    this(ds, file, "", "");
  }

  /**
   * goal is, to normalize the string, so that we can URI parse it...
   * 
   * @param datasource
   *          the detected datasource
   * @param file
   * @param label
   * @param ipForLocal
   */
  public SplitUri(String datasource, String file, String label, String ipForLocal) {
    this.datasource = datasource;
    this.file = file;
    this.label = label;

    try {
      URI dsuri = parseToUri(datasource);
      // datasource is ONLY the path without server and anything...
      if (dsuri != null) {
        this.datasource = dsuri.getPath();
      }
    }
    catch (Exception e) {
      LOGGER.warn("Could not parse datasource: {}", datasource);
    }

    // remove datasource, and keep in original format (uri parsing with modified file)
    if (file.startsWith(datasource)) {
      this.file = file.substring(datasource.length());
    }

    URI u = null;
    try {
      u = parseToUri(file);
    }
    catch (Exception e) {
      // we might have an user:pass@server string, but without schema?
      // add one...m since url parser NEEDS one...
      if (!file.contains("://") && (file.contains("@") || file.contains(":"))) {
        file = "smb://" + file;
        try {
          u = parseToUri(file); // retry
        }
        catch (Exception e2) {
          LOGGER.warn("Could not parse URI: {}", file);
        }
      }
      else {
        LOGGER.warn("Could not parse URI: {}", file);
      }
    }

    if (u != null && !StringUtils.isBlank(u.getHost())) {
      if (file.startsWith("upnp")) {
        this.type = "UPNP";
        this.hostname = getMacFromUpnpUUID(u.getHost());

        UpnpService us = Upnp.getInstance().getUpnpService();
        if (us != null) {
          Registry registry = us.getRegistry();
          if (registry != null) {
            @SuppressWarnings("rawtypes")
            Device foundDevice = registry.getDevice(UDN.valueOf(u.getHost()), true);
            if (foundDevice != null) {
              this.ip = foundDevice.getDetails().getPresentationURI().getHost();
            }
          }
        }
      }
      else {
        this.type = u.getScheme().toUpperCase(Locale.ROOT);
        this.hostname = u.getHost();
        try {
          String ip = lookup.get(u.getHost());
          // cache
          if (ip == null) {
            InetAddress i = InetAddress.getByName(u.getHost());
            ip = i.getHostAddress();
            lookup.put(u.getHost(), ip);
          }
          this.ip = ip;
        }
        catch (Exception e) {
          LOGGER.warn("Could not lookup IP for {}: {}", u.getHost(), e.getMessage());
        }
      }
    }
    else {
      this.type = "LOCAL";
      if (ipForLocal.isEmpty()) {
        this.ip = "127.0.0.1";
        this.hostname = "localhost";
      }
      else {
        try {
          String tmp = lookup.get(ipForLocal);
          if (tmp == null) {
            InetAddress i = InetAddress.getByName(ipForLocal);
            this.ip = i.getHostAddress();
            this.hostname = i.getHostName();
            lookup.put(ipForLocal, ip);
            lookup.put(hostname, ip);
          }
        }
        catch (Exception e) {
          LOGGER.warn("Could not lookup hostname for {}: {}", ipForLocal, e.getMessage());
        }
      }
    }

    // convert forward & backslashes to same format
    this.datasource = clean(this.datasource);
    this.file = clean(this.file);
  }

  /**
   * trim leading & trailing slashes; and change backslashes to forward slashes
   * 
   * @param file
   * @return
   */
  private String clean(String file) {
    if (file == null || file.isEmpty()) {
      return "";
    }
    file = file.replaceAll("\\\\", "/");
    if (file.endsWith("/")) {
      file = file.substring(0, file.length() - 1);
    }
    if (file.startsWith("/")) {
      file = file.substring(1);
    }
    return file;
  }

  private URI parseToUri(String file) throws Exception {
    URI u = null;
    file = file.replaceAll("\\\\", "/"); // normalize all to slashes
    // are we an URI with scheme? Or just local file?
    if (file.contains("://")) {
      // Kodi sends the path 1:1 and not URI encoded...
      file = file.replace(" ", "%20"); // space in urls
      file = file.replace("#", "%23"); // hash sign in urls
      file = file.replace("+", "%2B"); // plus sign in file should be treated directly, not as "space" as in url
      u = URI.create(file);
    }
    else {
      file = file.trim(); // no whitespaces around
      u = Paths.get(file).toUri();
    }
    return u;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((file == null) ? 0 : file.hashCode());
    result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
    result = prime * result + ((ip == null) ? 0 : ip.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SplitUri other = (SplitUri) obj;

    if (datasource == null || datasource.isEmpty() || other.datasource == null || other.datasource.isEmpty()) {
      return false;
    }

    // 1: same? - step directly out
    if (file.equals(other.file) && datasource.equals(other.datasource)) {
      return true;
    }

    // 2: - check either matching IP or hostname
    if (file.equals(other.file) && ip != null && !ip.isEmpty() && ip.equals(other.ip)) {
      return true;
    }
    if (file.equals(other.file) && hostname != null && !hostname.isEmpty() && hostname.equalsIgnoreCase(other.hostname)) {
      return true;
    }

    // 3: file same, and at least the last folder of datasource
    Path ds = Paths.get(datasource);
    Path otherds = Paths.get(other.datasource);
    if (file.equals(other.file) && ds.getFileName().equals(otherds.getFileName())) {
      return true;
    }

    // 4: same path between DS and file
    if (file.equals(other.file)) {
      return true;
    }

    // 5: did not match? return false
    return false;
  }

  /**
   * gets the MAC from an upnp UUID string (= last 6 bytes reversed)<br>
   * like upnp://00113201-aac2-0011-c2aa-02aa01321100 -> 00113201AA02
   *
   * @param uuid
   * @return
   */
  private static String getMacFromUpnpUUID(String uuid) {
    String s = uuid.substring(uuid.lastIndexOf('-') + 1);
    StringBuilder result = new StringBuilder();
    for (int i = s.length() - 2; i >= 0; i = i - 2) {
      result.append(new StringBuilder(s.substring(i, i + 2)));
      result.append(i > 1 ? ":" : ""); // skip last
    }
    return result.toString().toUpperCase(Locale.ROOT);
  }

}
