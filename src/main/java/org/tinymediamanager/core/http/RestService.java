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
package org.tinymediamanager.core.http;

import java.io.IOException;

import fi.iki.elonen.router.RouterNanoHTTPD;

class RestService extends RouterNanoHTTPD {
  RestService(int port) throws IOException {
    super(port);

    addMappings();
    start(30000, false);
  }

  @Override
  public void addMappings() {
    super.addMappings();

    addRoute("/api/command", CommandHandler.class);
  }
}
