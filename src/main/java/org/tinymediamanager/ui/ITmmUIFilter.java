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
package org.tinymediamanager.ui;

import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * The interface ITmmUIFilter is a generic interface for all types of UI filters inside the tinyMediaManager UI
 * 
 * @author Manuel Laggner
 */
public interface ITmmUIFilter<E> {
  String FILTER_CHANGED = "filterChanged";

  /**
   * the filter state
   */
  enum FilterState {
    ACTIVE,
    ACTIVE_NEGATIVE,
    INACTIVE
  }

  /**
   * the filter option
   */
  enum FilterOption {
    /**
     * LT - lower than
     */
    LT,

    /**
     * LE - lower equals
     */
    LE,

    /**
     * EQ - equals
     */
    EQ,

    /**
     * GE - greater equals
     */
    GE,

    /**
     * GT - greater than
     */
    GT,

    /**
     * BT - between
     */
    BT;

    @Override
    public String toString() {
      String string = "";

      switch (this) {
        case LT -> string = "<";
        case LE -> string = "≤";
        case EQ -> string = "=";
        case GE -> string = "≥";
        case GT -> string = ">";
        case BT -> string = "[]";
      }

      return string;
    }
  }

  /**
   * get the id of this filter. Used for storing/loading filters
   * 
   * @return the id of this filter
   */
  String getId();

  /**
   * Get the JCheckBox for enabling/disabling the filter
   * 
   * @return the JCheckBox to enable/disable the filter
   */
  JCheckBox getCheckBox();

  /**
   * Get the JLabel for the filter name
   * 
   * @return the JLabel with the filter name
   */
  JLabel getLabel();

  /**
   * get the filter option {@link JComboBox}
   * 
   * @return the filter option {@link JComboBox} - only created if there getFilterComponent is not null!
   */
  JComboBox<FilterOption> getFilterOptionComboBox();

  /**
   * Get the filter component for extended filtering or null if it is not needed
   * 
   * @return the component for extended filtering (e.g. JComboBox for JTextfield) or null
   */
  JComponent getFilterComponent();

  /**
   * get the filter value
   * 
   * @return the filter value
   */
  String getFilterValueAsString();

  /**
   * set the filter value
   */
  void setFilterValue(Object value);

  /**
   * clear all filter values
   */
  void clearFilter();

  /**
   * get the filter state
   * 
   * @return the filter state (ACTIVE, ACTIVE_NEGATIVE, INACTIVE)
   */
  FilterState getFilterState();

  /**
   * set the filter state (ACTIVE, ACTIVE_NEGATIVE, INACTIVE)
   * 
   * @param state
   *          the state
   */
  void setFilterState(FilterState state);

  /**
   * get the filter option
   *
   * @return the filter option (LT, LE, EQ, GE, GT)
   */
  FilterOption getFilterOption();

  /**
   * set the filter option
   * 
   * @param filterOption
   *          the filter option (LT, LE, EQ, GE, GT)
   */
  void setFilterOption(FilterOption filterOption);

  /**
   * Returns whether the specified object is accepted by this filter or not.
   *
   * @param object
   *          object to process
   * @return true if the specified object is accepted by this filter, false otherwise
   */
  boolean accept(E object);

  /**
   * Adds the property change listener.
   * 
   * @param listener
   *          the listener
   */
  void addPropertyChangeListener(PropertyChangeListener listener);

  /**
   * Adds the property change listener.
   * 
   * @param propertyName
   *          the property name
   * @param listener
   *          the listener
   */
  void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

  /**
   * Removes the property change listener.
   * 
   * @param listener
   *          the listener
   */
  void removePropertyChangeListener(PropertyChangeListener listener);

  /**
   * Removes the property change listener.
   * 
   * @param propertyName
   *          the property name
   * @param listener
   *          the listener
   */
  void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);
}
