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

import org.tinymediamanager.license.TmmFeature;

/**
 * this exception is used to indicate that the feature is not enabled
 * 
 * @author Manuel Laggner
 */
public class FeatureNotEnabledException extends Exception {

  public FeatureNotEnabledException(TmmFeature feature) {
    super(feature.getFeatureName());
  }

}
