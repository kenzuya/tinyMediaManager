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

package org.tinymediamanager.ui.moviesets;

import java.awt.FontMetrics;
import java.util.Date;

import javax.swing.ImageIcon;

import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.renderer.DateTableCellRenderer;

/**
 * The MovieInMovieSetTableFormat. Used as definition for the movie table in the movie set module
 *
 * @author Manuel Laggner
 */
public class MovieInMovieSetTableFormat extends TmmTableFormat<Movie> {

  public MovieInMovieSetTableFormat() {

    FontMetrics fontMetrics = getFontMetrics();

    /*
     * title
     */
    Column col = new Column(TmmResourceBundle.getString("metatag.title"), "title", Movie::getTitleSortable, String.class);
    addColumn(col);

    /*
     * year
     */
    col = new Column(TmmResourceBundle.getString("metatag.year"), "year", MediaEntity::getYear, Movie.class);
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("2000") + getCellPadding());
    addColumn(col);

    /*
     * date added
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded"), "dateadded", MediaEntity::getDateAddedForUi, Date.class);
    col.setColumnResizeable(false);
    col.setCellRenderer(new DateTableCellRenderer());
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
      // ignore
    }
    addColumn(col);

    /*
     * watched
     */
    col = new Column(TmmResourceBundle.getString("metatag.watched"), "watched", movie -> getCheckIcon(movie.isWatched()), ImageIcon.class);
    col.setHeaderIcon(IconManager.WATCHED);
    col.setColumnResizeable(false);
    addColumn(col);
  }
}
