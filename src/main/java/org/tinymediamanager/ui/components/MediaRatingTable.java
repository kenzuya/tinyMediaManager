/*
 * Copyright 2012 - 2023 Manuel Laggner
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
package org.tinymediamanager.ui.components;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.TableColumn;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.ui.NumberCellEditor;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.WritableTableFormat;

/**
 * The class MediaRatingTable is used to display / edit ratings
 *
 * @author Manuel Laggner
 */
public class MediaRatingTable extends TmmTable {
  private static final long              serialVersionUID = 8010732881277204728L;



  private final Map<String, MediaRating> ratingMap;
  private final EventList<Rating>        ratingList;

  /**
   * this constructor is used to display the ratings
   *
   * @param ratings
   *          a map containing the ratings
   */
  public MediaRatingTable(Map<String, MediaRating> ratings) {
    this.ratingMap = ratings;
    this.ratingList = convertRatingMapToEventList(ratingMap, true);
    setModel(new TmmTableModel<>(ratingList, new MediaRatingTableFormat(false)));
    setTableHeader(null);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
  }

  /**
   * this constructor is used to edit the ratings
   *
   * @param ratings
   *          an eventlist containing the ratings
   */
  public MediaRatingTable(EventList<Rating> ratings) {
    this.ratingMap = null;
    this.ratingList = ratings;
    setModel(new TmmTableModel<>(ratingList, new MediaRatingTableFormat(true)));
    // setTableHeader(null);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

    // value column
    TableColumn column = getColumnModel().getColumn(1);
    column.setCellEditor(new NumberCellEditor(3, 2));

    // maxValue column
    column = getColumnModel().getColumn(2);
    column.setCellEditor(new NumberCellEditor(3, 0));

    // votes column
    column = getColumnModel().getColumn(3);
    column.setCellEditor(new NumberCellEditor(10, 0));
  }

  public static EventList<Rating> convertRatingMapToEventList(Map<String, org.tinymediamanager.core.entities.MediaRating> idMap,
      boolean withUserRating) {
    EventList<Rating> idList = new BasicEventList<>();
    for (Entry<String, org.tinymediamanager.core.entities.MediaRating> entry : idMap.entrySet()) {
      if (org.tinymediamanager.core.entities.MediaRating.USER.equals(entry.getKey()) && !withUserRating) {
        continue;
      }

      Rating id = new Rating(entry.getKey());
      org.tinymediamanager.core.entities.MediaRating mediaRating = entry.getValue();

      id.value = mediaRating.getRating();
      id.votes = mediaRating.getVotes();
      id.maxValue = mediaRating.getMaxValue();

      idList.add(id);
    }

    return idList;
  }

  public static EventList<Rating> convertRatingMapToEventList(List<org.tinymediamanager.core.entities.MediaRating> ratings) {
    EventList<Rating> idList = new BasicEventList<>();
    for (org.tinymediamanager.core.entities.MediaRating rating : ratings) {
      Rating id = new Rating(rating.getId());
      org.tinymediamanager.core.entities.MediaRating mediaRating = rating;

      id.value = mediaRating.getRating();
      id.votes = mediaRating.getVotes();
      id.maxValue = mediaRating.getMaxValue();

      idList.add(id);
    }

    return idList;
  }

  public static class Rating {
    public String key;
    public float  value;
    public int    maxValue;
    public int    votes;

    public Rating(String key) {
      this.key = key;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(13, 31).append(key).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Rating)) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      Rating other = (Rating) obj;
      return StringUtils.equals(key, other.key);
    }
  }

  private class MediaRatingTableFormat extends TmmTableFormat<Rating> implements WritableTableFormat<Rating> {
    private final boolean editable;

    MediaRatingTableFormat(boolean editable) {
      this.editable = editable;

      /*
       * source
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.rating.source"), "source", rating -> rating.key, String.class);
      addColumn(col);

      /*
       * rating
       */
      col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", rating -> rating.value, Float.class);
      addColumn(col);

      /*
       * maxvalue
       */
      col = new Column(TmmResourceBundle.getString("metatag.rating.maxvalue"), "maxvalue", rating -> rating.maxValue, Integer.class);
      addColumn(col);

      /*
       * votes
       */
      col = new Column(TmmResourceBundle.getString("metatag.rating.votes"), "votes", rating -> rating.votes, Integer.class);
      addColumn(col);
    }

    @Override
    public boolean isEditable(Rating arg0, int arg1) {
      return editable;
    }

    @Override
    public Rating setColumnValue(Rating arg0, Object arg1, int arg2) {
      if (arg0 == null || arg1 == null) {
        return null;
      }
      switch (arg2) {
        case 0:
          arg0.key = arg1.toString();
          break;

        case 1:
          try {
            arg0.value = (float) arg1;
          }
          catch (Exception ignored) {
          }
          break;

        case 2:
          try {
            arg0.maxValue = (int) arg1;
          }
          catch (Exception ignored) {
          }
          break;

        case 3:
          try {
            arg0.votes = (int) arg1;
          }
          catch (Exception ignored) {
          }
          break;
      }
      return arg0;
    }
  }
}
