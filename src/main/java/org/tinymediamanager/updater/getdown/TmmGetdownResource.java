/*
 * Copyright 2012 - 2019 Manuel Laggner
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

package org.tinymediamanager.updater.getdown;

import static com.threerings.getdown.Log.log;
import static org.tinymediamanager.updater.getdown.TmmGetdownApplication.UPDATE_FOLDER;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.zip.ZipFile;

import com.threerings.getdown.data.Resource;
import com.threerings.getdown.util.FileUtil;

public class TmmGetdownResource extends Resource {

  public TmmGetdownResource(String path, URL remote, File local, EnumSet<Attr> attrs) {
    super(path, remote, local, attrs);
    this._localNew = new File(UPDATE_FOLDER + File.separator + path);
    this._marker = new File(UPDATE_FOLDER + File.separator + ".getdown", path + "v");

    boolean unpack = attrs.contains(Attr.UNPACK);
    if (unpack && _isZip) {
      _unpacked = _localNew.getParentFile();
    }
    else if (unpack && _isPacked200Jar) {
      String dotJar = ".jar", lname = _localNew.getName();
      String uname = lname.substring(0, lname.lastIndexOf(dotJar) + dotJar.length());
      _unpacked = new File(_localNew.getParent(), uname);
    }
  }

  @Override
  public boolean isMarkedValid() {
    // make sure the markers parent is created
    File parent = new File(_marker.getParent());
    if (!parent.exists() && !parent.mkdirs()) {
      log.warning("Failed to create target directory for resource '" + _marker + "'.");
    }
    return super.isMarkedValid();
  }

  @Override
  public void install(boolean validate) throws IOException {
    // just unpack the downloaded files (w/o moving them to the new destination)
    applyAttrs();
  }

  @Override
  public void applyAttrs() throws IOException {
    if (shouldUnpack()) {
      unpack();
    }
    if (_attrs.contains(Attr.EXEC)) {
      FileUtil.makeExecutable(_localNew);
    }
  }

  @Override
  public void unpack() throws IOException {
    // sanity check
    if (!_isZip && !_isPacked200Jar) {
      throw new IOException("Requested to unpack non-jar file '" + _localNew + "'.");
    }
    if (_isZip) {
      try (ZipFile jar = new ZipFile(_localNew)) {
        FileUtil.unpackJar(jar, _unpacked, _attrs.contains(Attr.CLEAN));
      }
    }
    else {
      FileUtil.unpackPacked200Jar(_localNew, _unpacked);
    }
  }
}
