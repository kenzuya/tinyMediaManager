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

package org.tinymediamanager.core;

/**
 * the enum DateAdded is used to control which date should be taken for various parts of tinyMediaManager
 * 
 * @author Manuel Laggner
 */
public enum DateField {
  DATE_ADDED("metatag.dateadded"), // dateAdded from MediaEntity
  FILE_CREATION_DATE("metatag.filecreationdate"), // fileCreation date
  FILE_LAST_MODIFIED_DATE("metatag.filelastmodifieddate"), // fileLastModified date
  RELEASE_DATE("metatag.releasedate"); // release date

  private final String description;

  DateField(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return TmmResourceBundle.getString(description);
  }
}
