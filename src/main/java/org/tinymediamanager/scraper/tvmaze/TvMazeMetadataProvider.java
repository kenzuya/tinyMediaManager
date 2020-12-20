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
package org.tinymediamanager.scraper.tvmaze;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.tvmaze.service.Controller;

/**
 * The class @{@link TvMazeMetadataProvider} is a metadata provider for the site tvmaze.com
 *
 * @author Wolfgang Janes
 */
abstract class TvMazeMetadataProvider {

  public static final String      ID = "tvmaze";

  protected final MediaProviderInfo providerInfo;
  protected final Controller      controller;

  TvMazeMetadataProvider() {
    providerInfo = createMediaProviderInfo();
    controller = new Controller(false);
  }

  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = new MediaProviderInfo(ID, getSubId(), "tvmaze.com",
        "TVmaze is a community of TV lovers and dedicated contributors that discuss and help maintain tv information on the web.",
        TvMazeMetadataProvider.class.getResource("/org/tinymediamanager/scraper/tvmaze.png"));

    providerInfo.getConfig().load();
    return providerInfo;
  }

  /**
   * @param date
   *          The date from the scraper in String format
   * @return Year in int
   * @throws ParseException
   *           error parsing the date
   */
  int parseYear(String date) throws ParseException {
    Date year = new SimpleDateFormat("yyyy-MM-dd").parse(date);
    Calendar cal = new GregorianCalendar();
    cal.setTime(year);
    return cal.get(Calendar.YEAR);
  }

}
