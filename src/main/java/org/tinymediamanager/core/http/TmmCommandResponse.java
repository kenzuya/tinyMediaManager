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
package org.tinymediamanager.core.http;

/**
 * the command response to send to the API caller
 * 
 * @author Manuel Laggner
 */
public class TmmCommandResponse {
  private int    responseCode;
  private String responseMessage;

  public TmmCommandResponse(int responseCode, String responseMessage) {
    this.responseCode = responseCode;
    this.responseMessage = responseMessage;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public TmmCommandResponse setResponseCode(int responseCode) {
    this.responseCode = responseCode;
    return this;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public TmmCommandResponse setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
    return this;
  }
}
