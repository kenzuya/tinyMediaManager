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
package org.tinymediamanager.core;

import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaCertification;

/**
 * The enum CertificationStyle represents all certification styles which we support writing
 *
 * @author Manuel Laggner
 */
public enum CertificationStyle {
  SHORT, // FSK 16
  SHORT_MPAA, // short for all but the term "Rated" in front of all US certs
  MEDIUM, // DE: FSK 16
  MEDIUM_FULL, // Germany: FSK 16
  LARGE, // DE:FSK 16 / DE:FSK16 / DE:16 / DE:ab 16
  LARGE_FULL, // Germany:FSK 16 / Germany:FSK16 / Germany:16 / Germany:ab 16
  TECHNICAL; // DE_FSK16

  /**
   * format the certification based on the given style
   * 
   * @param cert
   *          the certification to format
   * @param style
   *          the style how the certification should be formatted
   * @return the formatted certification style
   */
  public static String formatCertification(MediaCertification cert, CertificationStyle style) {
    if (cert == MediaCertification.UNKNOWN) {
      return "";
    }

    switch (style) {
      case SHORT:
        return cert.getName();

      case SHORT_MPAA:
        if (cert.getCountry() == CountryCode.US) {
          return MediaCertification.getMPAAString(cert);
        }
        return cert.getName();

      case MEDIUM:
        return cert.getCountry().getAlpha2() + ": " + cert.getName();

      case MEDIUM_FULL:
        return cert.getCountry().getName() + ": " + cert.getName();

      case LARGE:
        return MediaCertification.generateCertificationStringWithAlternateNames(cert);

      case LARGE_FULL:
        return MediaCertification.generateCertificationStringWithAlternateNames(cert, true);

      case TECHNICAL:
        return cert.name();

      default:
        return "";
    }
  }
}
