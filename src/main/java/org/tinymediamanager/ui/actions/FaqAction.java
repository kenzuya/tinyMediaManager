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
package org.tinymediamanager.ui.actions;

import java.awt.event.ActionEvent;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;

/**
 * The FaqAction to redirect to the FAQ page
 * 
 * @author Manuel Laggner
 */
public class FaqAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(FaqAction.class);

  public FaqAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.faq"));
    putValue(SMALL_ICON, IconManager.HELP);
  }

  @Override
  protected void processAction(ActionEvent e) {
    String url = StringEscapeUtils.unescapeHtml4("https://www.tinymediamanager.org/help/faq");
    try {
      TmmUIHelper.browseUrl(url);
    }
    catch (Exception e1) {
      LOGGER.error("FAQ", e1);
      MessageManager.instance
          .pushMessage(new Message(MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", e1.getLocalizedMessage() }));
    }
  }
}
