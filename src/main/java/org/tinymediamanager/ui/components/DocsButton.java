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
package org.tinymediamanager.ui.components;

import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.DocsAction;

/**
 * the class DocsButton is used to provide a navigation directly to the online docs for the given sub-path
 *
 * @author Manuel Laggner
 */
public class DocsButton extends FlatButton {

  public DocsButton(String subPath) {
    super(IconManager.HELP, (event) -> TmmUIHelper.browseUrlSilently(DocsAction.DOCS_URL + subPath));
  }
}
