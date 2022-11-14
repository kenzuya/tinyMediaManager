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
package org.tinymediamanager.ui.components;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.WritableTableFormat;

/**
 * The class MediaIdTable is used to display / edit media ids
 *
 * @author Manuel Laggner
 */
public class MediaIdTable extends TmmTable {
  private final Map<String, Object> idMap;
  private final EventList<MediaId>  idList;

  /**
   * this constructor is used to display the ids
   *
   * @param ids
   *          a map containing the ids
   */
  public MediaIdTable(Map<String, Object> ids) {
    this.idMap = ids;
    this.idList = convertIdMapToEventList(idMap);
    setModel(new TmmTableModel<>(idList, new MediaIdTableFormat(false)));
    setTableHeader(null);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
  }

  public MediaIdTable(EventList<MediaId> ids) {
    this.idMap = null;
    this.idList = ids;
    setModel(new TmmTableModel<>(idList, new MediaIdTableFormat(true)));
    setTableHeader(null);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
  }

  public static EventList<MediaId> convertIdMapToEventList(Map<String, Object> idMap) {
    EventList<MediaId> idList = new BasicEventList<>();
    for (Entry<String, Object> entry : idMap.entrySet()) {
      MediaId id = new MediaId();
      id.key = entry.getKey();
      try {
        id.value = entry.getValue().toString();
      }
      catch (Exception e) {
        id.value = "";
      }
      idList.add(id);
    }

    return idList;
  }

  public static class MediaId {
    public String key;
    public String value;

    public MediaId() {
    }

    public MediaId(String key) {
      this.key = key;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(19, 31).append(key).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MediaId) || obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      MediaId other = (MediaId) obj;
      return StringUtils.equals(key, other.key);
    }
  }

  private static class MediaIdTableFormat extends TmmTableFormat<MediaId> implements WritableTableFormat<MediaId> {
    private final boolean editable;

    public MediaIdTableFormat(boolean editable) {
      this.editable = editable;

      /*
       * key
       */
      Column col = new Column("", "key", mediaId -> mediaId.key, String.class);
      addColumn(col);

      /*
       * value
       */
      col = new Column("", "value", mediaId -> mediaId.value, String.class);
      addColumn(col);
    }

    @Override
    public boolean isEditable(MediaId arg0, int arg1) {
      return editable && arg1 == 1;
    }

    @Override
    public MediaId setColumnValue(MediaId arg0, Object arg1, int arg2) {
      if (arg0 == null || arg1 == null) {
        return null;
      }
      switch (arg2) {
        case 0:
          arg0.key = arg1.toString();
          break;

        case 1:
          arg0.value = arg1.toString();
          break;
      }
      return arg0;
    }
  }
}
