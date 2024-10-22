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
package org.tinymediamanager.ui.images;

import java.awt.Dimension;

/**
 * the class {@link AspectRatioIcon} is used to draw the aspect ratio icon
 * 
 * @author Manuel Laggner
 */
public class AspectRatioIcon extends MediaInfoIcon {

  public AspectRatioIcon(String text) throws Exception {
    super("video/aspect_ratio.svg");
    setPreferredSize(new Dimension(32, 32));
    setText(text, 100);
  }
}
