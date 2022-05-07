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

package org.tinymediamanager.scraper.davestrailer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;

public class ITDavesTrailerPageProviderTest extends BasicITest {

  @Test
  public void testScrapeTrailerOneTrailerRow() {
    IMovieTrailerProvider mp;
    try {
      mp = new DavesTrailerPageProvider();
      TrailerSearchAndScrapeOptions options = new TrailerSearchAndScrapeOptions(MediaType.MOVIE);
      MediaMetadata md = new MediaMetadata("foo");
      md.setTitle("Dune");
      options.setId(MediaMetadata.IMDB, "tt1160419");
      options.setMetadata(md);

      List<MediaTrailer> trailers = mp.getTrailers(options);
      assertThat(trailers).isNotNull();

    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testScrapeTrailerMoreTrailerRows() {
    IMovieTrailerProvider mp;
    try {
      mp = new DavesTrailerPageProvider();
      TrailerSearchAndScrapeOptions options = new TrailerSearchAndScrapeOptions(MediaType.MOVIE);
      MediaMetadata md = new MediaMetadata("foo");
      md.setTitle("Bears");
      options.setId(MediaMetadata.IMDB, "tt3890160");
      options.setMetadata(md);

      List<MediaTrailer> trailers = mp.getTrailers(options);
      assertThat(trailers).isNotNull();

    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
