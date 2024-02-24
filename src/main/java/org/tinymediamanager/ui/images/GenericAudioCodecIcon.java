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

import static org.tinymediamanager.ui.images.SvgIconHelper.getHexString;
import static org.tinymediamanager.ui.images.SvgIconHelper.setFill;

import javax.swing.UIManager;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.Text;
import com.kitfox.svg.animation.AnimationElement;

/**
 * the class {@link GenericAudioCodecIcon} is used to draw the aspect ratio icon
 * 
 * @author Manuel Laggner
 */
public class GenericAudioCodecIcon extends MediaInfoIcon {

  public GenericAudioCodecIcon(String text) throws Exception {
    super("audio/codec/audio_codec_generic.svg");
    setText(text);
  }

  private void setText(String text) {
    // get the text node from the root
    try {
      // clear cached SVG, because we want to modify the source
      getSvgUniverse().clear();

      SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
      Text textElement = (Text) diagram.getElement("text");

      textElement.addAttribute("x", AnimationElement.AT_XML, "141");
      textElement.appendText(text);
      setFill(getHexString(UIManager.getColor("Label.foreground")), diagram.getRoot());
      textElement.rebuild();
    }
    catch (Exception ex) {
      // ignored
    }
  }
}
