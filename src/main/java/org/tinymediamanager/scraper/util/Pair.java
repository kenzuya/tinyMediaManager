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
package org.tinymediamanager.scraper.util;

/**
 * A Pair is simple container containing 2 paired values and types.
 * 
 * @param <First>
 *          the generic type
 * @param <Second>
 *          the generic type
 * @author seans
 */
public class Pair<First, Second> {

  private First  first;
  private Second second;

  public Pair(First f, Second s) {
    this.first = f;
    this.second = s;
  }

  public First first() {
    return first;
  }

  public Second second() {
    return second;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Pair other = (Pair) obj;
    if (first == null) {
      if (other.first != null)
        return false;
    }
    else if (!first.equals(other.first))
      return false;
    if (second == null) {
      if (other.second != null)
        return false;
    }
    else if (!second.equals(other.second))
      return false;
    return true;
  }
}
